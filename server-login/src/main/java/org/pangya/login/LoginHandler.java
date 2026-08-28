package org.pangya.login;

import org.pangya.db.LoginRepository;
import org.pangya.network.AppConfig;
import org.pangya.network.redis.SessionKeyStore;
import org.pangya.network.session.PlayerContext;
import org.pangya.network.session.Session;
import org.pangya.protocol.login.LoginPackets;
import org.pangya.protocol.login.ServerInfo;
import org.pangya.protocol.packet.PacketReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Pattern;

/**
 * JP {@code LoginServer.requestLogin} + {@code packet_func_ls.succes_login} / {@code packet003}.
 */
public final class LoginHandler {

    private static final Logger log = LoggerFactory.getLogger(LoginHandler.class);
    private static final Pattern INVALID_ID =
            Pattern.compile(".*[\\^$&,\\\\?`´~|\"@#¨'%*!\\\\].*");

    private final AppConfig config;
    private final LoginRepository repo;
    private final SessionKeyStore redis;

    public LoginHandler(AppConfig config, LoginRepository repo, SessionKeyStore redis) {
        this.config = config;
        this.repo = repo;
        this.redis = redis;
    }

    public void onPacket(Session session, byte[] plaintext) {
        if (plaintext.length < 2) {
            return;
        }
        PacketReader reader = new PacketReader(plaintext);
        int opcode = reader.opcode();
        switch (opcode) {
            case LoginPackets.CLIENT_CONNECT -> requestLogin(session, reader);
            case LoginPackets.CLIENT_SELECT_GS -> selectGameServer(session, reader);
            default -> log.debug("unhandled login opcode 0x{}", Integer.toHexString(opcode));
        }
    }

    private void requestLogin(Session session, PacketReader reader) {
        LoginPackets.LoginData data = LoginPackets.readLoginData(reader);
        if (data.id() == null || data.id().length() < 2 || INVALID_ID.matcher(data.id()).matches()) {
            session.send(LoginPackets.pacote001Option(LoginPackets.OPT_BAD_ID_OR_PASS));
            session.disconnect();
            return;
        }
        if (data.password() == null || data.password().length() < 2) {
            session.send(LoginPackets.pacote001Option(LoginPackets.OPT_BAD_ID_OR_PASS));
            session.disconnect();
            return;
        }
        if (config.maintenance()) {
            session.send(LoginPackets.pacote001Option(LoginPackets.OPT_MAINTENANCE));
            session.setAuthorized(false);
            return;
        }
        // JP hashes MD5 then overwrites with the password the client sent.
        String pass = data.password();
        if (repo.isBannedIp(session.ip())) {
            session.send(LoginPackets.pacote001Option(LoginPackets.OPT_REGION_BAN));
            session.disconnect();
            return;
        }
        var uidOpt = repo.verifyId(data.id());
        if (uidOpt.isEmpty() || !repo.verifyPass(uidOpt.get(), pass)) {
            session.send(LoginPackets.pacote001Option(LoginPackets.OPT_BAD_ID_OR_PASS));
            session.disconnect();
            return;
        }
        long uid = uidOpt.get();
        var info = repo.playerInfo(uid).orElse(null);
        if (info == null) {
            session.send(LoginPackets.pacote001Option(LoginPackets.OPT_BAD_ID_OR_PASS));
            session.disconnect();
            return;
        }
        PlayerContext pi = session.player();
        pi.uid = info.uid();
        pi.id = info.id();
        pi.nickname = info.nickname();
        pi.capability = info.capability();
        pi.level = info.level();
        pi.idState = info.idState();
        pi.blockTime = info.blockTimeSeconds();

        if (pi.idState != 0) {
            sendBlock(session, pi);
            session.disconnect();
            return;
        }
        if (!repo.isFirstLoginDone(uid)) {
            session.setAuthorized(true);
            session.send(LoginPackets.pacote00F(1, pi.id));
            session.send(LoginPackets.pacote001Option(LoginPackets.OPT_FIRST_LOGIN));
            return;
        }
        if (!repo.isFirstSetDone(uid)) {
            session.setAuthorized(true);
            session.send(LoginPackets.pacote00F(1, pi.id));
            session.send(LoginPackets.pacote001Option(LoginPackets.OPT_FIRST_SET));
            return;
        }
        if (repo.isLogon(uid)) {
            session.setAuthorized(true);
            session.send(LoginPackets.pacote001Option(LoginPackets.OPT_ALREADY_ON_GS));
            return;
        }
        successLogin(session);
    }

    private void successLogin(Session session) {
        PlayerContext pi = session.player();
        pi.loginState = 1;
        session.setAuthorized(true);
        String authKey = repo.generateAuthKeyLogin(pi.uid);
        pi.authKeyLogin = authKey;
        try {
            redis.putLoginKey(pi.uid, authKey);
            redis.putPlayerIp(pi.uid, session.ip());
        } catch (RuntimeException e) {
            log.warn("redis session key failed uid={}: {}", pi.uid, e.toString());
        }
        try {
            repo.registerPlayerLogin(pi.uid, session.ip(), config.uid());
        } catch (RuntimeException e) {
            log.warn("register login failed uid={}: {}", pi.uid, e.toString());
        }
        List<ServerInfo> games = toInfo(repo.serverList(1));
        List<ServerInfo> msns = toInfo(repo.serverList(3));
        String[] macros = repo.macros(pi.uid);
        session.send(LoginPackets.pacote010(authKey));
        String accessCode = repo.generateWebKey(pi.uid);
        session.send(LoginPackets.pacote001Success(pi.id, pi.uid, pi.capability, accessCode, pi.nickname));
        session.send(LoginPackets.pacote002(games));
        session.send(LoginPackets.pacote009(msns));
        session.send(LoginPackets.pacote006(macros));
        log.info("player logged id={} uid={}", pi.id, pi.uid);
    }

    private void selectGameServer(Session session, PacketReader reader) {
        int serverUid = reader.u32();
        if (serverUid <= 0 || !session.authorized()) {
            return;
        }
        boolean known = repo.serverList(1).stream().anyMatch(s -> s.uid() == serverUid);
        if (!known) {
            log.warn("select gs unknown uid={}", serverUid);
            return;
        }
        repo.registerLogonServer(session.player().uid, serverUid);
        String key = repo.generateAuthKeyGame(session.player().uid, serverUid);
        try {
            redis.putGameKey(session.player().uid, serverUid, key);
        } catch (RuntimeException e) {
            log.warn("redis game key failed: {}", e.toString());
        }
        session.send(LoginPackets.pacote003(key, 0));
    }

    private static void sendBlock(Session session, PlayerContext pi) {
        // C# bit flags on IDState; non-zero without known bits still blocks via option 0x0C.
        if (pi.blockTime > 0 || pi.blockTime == -1) {
            int hours = pi.blockTime == -1 ? LoginPackets.BLOCK_TIME_UNDER_ONE_HOUR
                    : Math.max(LoginPackets.BLOCK_TIME_UNDER_ONE_HOUR, pi.blockTime / 3600);
            session.send(LoginPackets.pacote001BlockTime(hours));
        } else {
            session.send(LoginPackets.pacote001Option(LoginPackets.OPT_BLOCK_FOREVER));
        }
    }

    static ServerInfo toInfo(LoginRepository.ServerListRow row) {
        ServerInfo s = new ServerInfo();
        s.name = row.name();
        s.uid = row.uid();
        s.ip = row.ip();
        s.port = row.port();
        s.maxUser = row.maxUser();
        s.currUser = row.currUser();
        s.property = row.property();
        s.angelicWings = row.angelicWings();
        s.eventFlag = row.eventFlag();
        s.eventMap = row.eventMap();
        s.appRate = row.appRate();
        s.scratchRate = row.scratchRate();
        s.imgNo = row.imgNo();
        return s;
    }

    static List<ServerInfo> toInfo(List<LoginRepository.ServerListRow> rows) {
        return rows.stream().map(LoginHandler::toInfo).toList();
    }
}
