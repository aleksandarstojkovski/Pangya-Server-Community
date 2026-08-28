package org.pangya.ranking;

import org.pangya.db.LoginRepository;
import org.pangya.network.session.PlayerContext;
import org.pangya.network.session.Session;
import org.pangya.network.session.SessionManager;
import org.pangya.protocol.packet.PacketReader;
import org.pangya.protocol.ranking.RankingPackets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GB {@code RankingServer.requestLogin} + {@code sendFirstPage}. Auth confirm is skipped when
 * {@code auth.enabled=false} (tests); live compose still registers with Auth like Login/Game.
 */
public final class RankingHandler {

    private static final Logger log = LoggerFactory.getLogger(RankingHandler.class);

    private final LoginRepository repo;
    private final SessionManager sessions;

    public RankingHandler(LoginRepository repo, SessionManager sessions) {
        this.repo = repo;
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
            session.send(RankingPackets.firstPageOk(data.menu(), data.item(), data.term(), data.classType()));
            log.info("ranking login id={} uid={}", pi.id, pi.uid);
        } catch (RuntimeException e) {
            log.warn("ranking login failed: {}", e.toString());
            session.send(RankingPackets.firstPageError(1));
            session.disconnect();
        }
    }
}
