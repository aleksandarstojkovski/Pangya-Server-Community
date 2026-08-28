package org.pangya.game;

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

/**
 * C# {@code GameService.authCmd*} / base {@code Server.authCmdInfoPlayerOnline}
 * from Auth via {@code unit_auth_server_connect}.
 */
public final class GameAuthHandler {

    private static final Logger log = LoggerFactory.getLogger(GameAuthHandler.class);

    private final AppConfig config;
    private final LoginRepository repo;
    private final SessionManager sessions;
    private final AuthOutbound authOut;
    private final GameHandler game;

    public GameAuthHandler(
            AppConfig config,
            LoginRepository repo,
            SessionManager sessions,
            AuthOutbound authOut,
            GameHandler game) {
        this.config = config;
        this.repo = repo;
        this.sessions = sessions;
        this.authOut = authOut;
        this.game = game;
    }

    public void onAuthPacket(int opcode, PacketReader body) {
        switch (opcode) {
            case AuthS2s.AUTH_SHUTDOWN -> game.authShutdown(body.i32());
            case AuthS2s.AUTH_BROADCAST_NOTICE -> game.authBroadcastNotice(body.pstr());
            case AuthS2s.AUTH_BROADCAST_TICKER -> {
                String nick = body.pstr();
                String msg = body.remaining() >= 2 ? body.pstr() : "";
                game.authBroadcastTicker(nick, msg);
            }
            case AuthS2s.AUTH_BROADCAST_CUBE_WIN_RARE -> {
                int option = body.u32();
                String msg = body.remaining() >= 2 ? body.pstr() : "";
                game.authBroadcastCubeWinRare(msg, option);
            }
            case AuthS2s.AUTH_DISCONNECT_PLAYER -> authDisconnectPlayer(body);
            case AuthS2s.AUTH_NEW_MAIL -> {
                AuthS2s.AuthNewMailRequest req = AuthS2s.readAuthNewMail(body);
                game.authNewMailArrived(req.playerUid(), req.mailId());
            }
            case AuthS2s.AUTH_NEW_RATE -> {
                AuthS2s.AuthNewRateRequest req = AuthS2s.readAuthNewRate(body);
                game.authNewRate(req.tipo(), req.qntd());
            }
            case AuthS2s.AUTH_RELOAD_SYSTEM -> game.authReloadGlobalSystem(body.u32());
            case AuthS2s.AUTH_INFO_PLAYER_ONLINE -> authInfoPlayerOnline(body);
            case AuthS2s.AUTH_CONFIRM_PLAYER_INFO -> authConfirmSendInfoPlayerOnline(body);
            default -> log.debug("unhandled auth packet 0x{}", Integer.toHexString(opcode));
        }
    }

    /** C# {@code GameService.authCmdDisconnectPlayer}. */
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
            log.debug("auth disconnect uid={} not on game server, cleared DB logon", req.playerUid());
        }
        authOut.sendConfirmDisconnectPlayer(req.serverUid(), req.playerUid());
    }

    /** C# base {@code Server.authCmdInfoPlayerOnline}. */
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
            log.debug(
                    "auth info player online uid={} reqServer={} option={}",
                    req.playerUid(),
                    req.reqServerUid(),
                    info.option());
        } catch (RuntimeException e) {
            authOut.sendInfoPlayerOnline(req.reqServerUid(), AuthS2s.AuthServerPlayerInfo.offline(req.playerUid()));
            log.warn("auth info player online uid={} failed: {}", req.playerUid(), e.toString());
        }
    }

    /** C# {@code GameService.authCmdConfirmSendInfoPlayerOnline}. */
    private void authConfirmSendInfoPlayerOnline(PacketReader body) {
        AuthS2s.AuthConfirmPlayerInfo req = AuthS2s.readAuthConfirmPlayerInfo(body);
        Session target = sessions.findByUid(req.uid());
        if (target == null) {
            log.debug("auth confirm send info uid={} reqServer={} not connected", req.uid(), req.reqServerUid());
            return;
        }
        if (!authOut.isLive()) {
            return;
        }
        PlayerContext pi = target.player();
        AuthS2s.AuthServerPlayerInfo info =
                AuthS2s.AuthServerPlayerInfo.online(pi.uid, pi.id, target.ip());
        authOut.sendInfoPlayerOnline(config.uid(), info);
        log.debug("auth confirm send info uid={} reqServer={}", req.uid(), req.reqServerUid());
    }

    /** C# INI {@code OPTION.SAME_ID_LOGIN}; default 0 (disconnect on auth command). */
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
