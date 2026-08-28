package org.pangya.ranking;

import org.pangya.db.LoginRepository;
import org.pangya.db.RankRepository;
import org.pangya.network.session.PlayerContext;
import org.pangya.network.session.Session;
import org.pangya.network.session.SessionManager;
import org.pangya.protocol.packet.PacketReader;
import org.pangya.protocol.ranking.RankingPackets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * GB {@code RankingServer.requestLogin} + {@code sendFirstPage} + {@code requestPlayerInfo}.
 * Registry rows come from {@code pangya_rank_atual} (C# {@code ProcGetRankRegistryInfo}).
 */
public final class RankingHandler {

    private static final Logger log = LoggerFactory.getLogger(RankingHandler.class);
    private static final int PAGE_SIZE = 12;

    private final LoginRepository repo;
    private final RankRepository ranks;
    private final SessionManager sessions;

    public RankingHandler(LoginRepository repo, RankRepository ranks, SessionManager sessions) {
        this.repo = repo;
        this.ranks = ranks;
        this.sessions = sessions;
    }

    public void onPacket(Session session, byte[] plaintext) {
        if (plaintext.length < 2) {
            return;
        }
        PacketReader reader = new PacketReader(plaintext);
        int opcode = reader.opcode();
        switch (opcode) {
            case RankingPackets.CLIENT_CONNECT -> requestLogin(session, reader);
            case RankingPackets.CLIENT_REQUEST_PLAYER_INFO -> playerInfo(session, reader);
            default -> log.debug("unhandled ranking opcode 0x{}", Integer.toHexString(opcode));
        }
    }

    private void requestLogin(Session session, PacketReader reader) {
        try {
            RankingPackets.Login data = RankingPackets.readLogin(reader);
            if (data.uid() == 0 || data.id() == null || data.id().isEmpty()) {
                session.send(RankingPackets.firstPageError(1));
                return;
            }
            if (repo.isBannedIp(session.ip())) {
                session.send(RankingPackets.firstPageError(1));
                session.disconnect();
                return;
            }
            var info = repo.playerInfo(data.uid() & 0xffff_ffffL).orElse(null);
            if (info == null || !data.id().equals(info.id())) {
                session.send(RankingPackets.firstPageError(1));
                session.disconnect();
                return;
            }
            if (info.idState() != 0) {
                session.send(RankingPackets.firstPageError(1));
                session.disconnect();
                return;
            }
            PlayerContext pi = session.player();
            pi.uid = info.uid();
            pi.id = info.id();
            pi.nickname = info.nickname();
            sessions.disconnectOthersWithUid(pi.uid, session);
            session.setAuthorized(true);

            int page = Math.max(1, data.page());
            List<RankRepository.RegistryRow> all = ranks.registry().stream()
                    .filter(r -> r.menu() == data.menu() && r.item() == data.item())
                    .toList();
            int pages = all.isEmpty() ? 0 : (all.size() + PAGE_SIZE - 1) / PAGE_SIZE;
            List<RankRepository.RegistryRow> slice = ranks.page(data.menu(), data.item(), page);
            List<RankingPackets.RegistryRow> wire = slice.stream()
                    .map(r -> new RankingPackets.RegistryRow(
                            r.uid(), r.currentPosition(), r.lastPosition(), r.value()))
                    .toList();
            session.send(RankingPackets.firstPage(
                    data.menu(), data.item(), data.term(), data.classType(),
                    wire, all.isEmpty() ? 0 : page, pages));
            log.info("ranking login id={} uid={} rows={}", pi.id, pi.uid, wire.size());
        } catch (RuntimeException e) {
            log.warn("ranking login failed: {}", e.toString());
            session.send(RankingPackets.firstPageError(1));
            session.disconnect();
        }
    }

    private void playerInfo(Session session, PacketReader reader) {
        if (!session.authorized()) {
            return;
        }
        RankingPackets.PlayerInfoRequest req = RankingPackets.readPlayerInfo(reader);
        var snap = ranks.playerSnapshot(req.uid() & 0xffff_ffffL).orElse(null);
        if (snap == null) {
            session.send(RankingPackets.playerFullInfoError());
            return;
        }
        List<RankingPackets.RegistryRow> overall = ranks.overallForPlayer(snap.uid()).stream()
                .map(r -> new RankingPackets.RegistryRow(
                        r.uid(), r.currentPosition(), r.lastPosition(), r.value()))
                .toList();
        byte[] character = ranks.character(snap.uid()).map(c -> c.toArray()).orElse(new byte[0]);
        session.send(RankingPackets.playerFullInfo(
                new RankingPackets.PlayerInfo(snap.uid(), snap.id(), snap.nickname(), snap.level()),
                character,
                overall));
    }
}
