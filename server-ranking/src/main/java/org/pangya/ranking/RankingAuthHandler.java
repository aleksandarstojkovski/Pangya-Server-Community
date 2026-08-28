package org.pangya.ranking;

import org.pangya.db.LoginRepository;
import org.pangya.network.AppConfig;
import org.pangya.network.auth.AuthOutbound;
import org.pangya.network.session.PlayerContext;
import org.pangya.network.session.Session;
import org.pangya.network.session.SessionManager;
import org.pangya.protocol.auth.AuthS2s;
import org.pangya.protocol.packet.PacketReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.IntConsumer;

/**
 * C# base {@code Server.authCmdInfoPlayerOnline} + child disconnect for Ranking (tipo=4).
 * Ranking source is not in the JP reference tree; behavior matches Game/Messenger minus {@code 0x0C}.
 */
public final class RankingAuthHandler {

    private static final Logger log = LoggerFactory.getLogger(RankingAuthHandler.class);

    private final AppConfig config;
    private final LoginRepository repo;
    private final SessionManager sessions;
    private final AuthOutbound authOut;
    private IntConsumer shutdownScheduler = sec -> log.warn("auth shutdown {} sec (no scheduler wired)", sec);

    public RankingAuthHandler(AppConfig config, LoginRepository repo, SessionManager sessions, AuthOutbound authOut) {
        this.config = config;
        this.repo = repo;
        this.sessions = sessions;
        this.authOut = authOut;
    }

    void setShutdownScheduler(IntConsumer scheduler) {
        this.shutdownScheduler = scheduler == null ? sec -> {} : scheduler;
    }

    public void onAuthPacket(int opcode, PacketReader body) {
        switch (opcode) {
            case AuthS2s.AUTH_SHUTDOWN -> shutdownScheduler.accept(body.i32());
            case AuthS2s.AUTH_DISCONNECT_PLAYER -> authDisconnectPlayer(body);
            case AuthS2s.AUTH_INFO_PLAYER_ONLINE -> authInfoPlayerOnline(body);
            default -> log.debug("unhandled auth packet 0x{}", Integer.toHexString(opcode));
        }
    }

    private void authDisconnectPlayer(PacketReader body) {
        AuthS2s.AuthDisconnectRequest req = AuthS2s.readAuthDisconnect(body);
        Session target = sessions.findByUid(req.playerUid());
        if (target != null) {
            if (req.force() == 1 || !sameIdLoginEnabled()) {
                target.disconnect();
                log.info("auth disconnect uid={} server={} force={}", req.playerUid(), req.serverUid(), req.force());
            }
        } else {
            repo.registerPlayerLogon(req.playerUid(), 1);
            log.debug("auth disconnect uid={} not on ranking server, cleared DB logon", req.playerUid());
        }
        authOut.sendConfirmDisconnectPlayer(req.serverUid(), req.playerUid());
    }

    private void authInfoPlayerOnline(PacketReader body) {
        AuthS2s.AuthInfoPlayerOnlineRequest req = AuthS2s.readAuthInfoPlayerOnline(body);
        try {
            Session target = sessions.findByUid(req.playerUid());
            AuthS2s.AuthServerPlayerInfo info;
            if (target != null) {
                PlayerContext pi = target.player();
                info = AuthS2s.AuthServerPlayerInfo.online(pi.uid, pi.id, target.ip());
            } else {
                info = AuthS2s.AuthServerPlayerInfo.offline(req.playerUid());
            }
            authOut.sendInfoPlayerOnline(req.reqServerUid(), info);
        } catch (RuntimeException e) {
            authOut.sendInfoPlayerOnline(req.reqServerUid(), AuthS2s.AuthServerPlayerInfo.offline(req.playerUid()));
            log.warn("auth info player online uid={} failed: {}", req.playerUid(), e.toString());
        }
    }

    private boolean sameIdLoginEnabled() {
        Object v = config.section("server").get("sameIdLogin");
        if (v instanceof Boolean b) {
            return b;
        }
        if (v instanceof Number n) {
            return n.intValue() == 1;
        }
        if (v != null) {
            return "1".equals(v.toString());
        }
        return false;
    }
}
