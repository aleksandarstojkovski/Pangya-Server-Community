package org.pangya.game;

import org.pangya.db.InventoryRepository;
import org.pangya.db.LoginRepository;
import org.pangya.network.AppConfig;
import org.pangya.network.redis.SessionKeyStore;
import org.pangya.network.session.PlayerContext;
import org.pangya.network.session.Session;
import org.pangya.network.session.SessionManager;
import org.pangya.protocol.game.GamePackets;
import org.pangya.protocol.packet.PacketReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * GB {@code GameServer.requestLogin} + channel enter + {@code Channel.requestMakeRoom}
 * ({@code Room.getInfo().ToArray()}) + start-game flags + Practice leave.
 */
public final class GameHandler {

    private static final Logger log = LoggerFactory.getLogger(GameHandler.class);

    private final AppConfig config;
    private final LoginRepository repo;
    private final InventoryRepository inventory;
    private final SessionKeyStore redis;
    private final SessionManager sessions;
    private final List<GamePackets.ChannelInfo> channels;
    private final AtomicInteger nextRoom = new AtomicInteger(1);
    private final ConcurrentHashMap<Integer, GameRoom> rooms = new ConcurrentHashMap<>();

    public GameHandler(
            AppConfig config,
            LoginRepository repo,
            InventoryRepository inventory,
            SessionKeyStore redis,
            SessionManager sessions,
            List<GamePackets.ChannelInfo> channels) {
        this.config = config;
        this.repo = repo;
        this.inventory = inventory;
        this.redis = redis;
        this.sessions = sessions;
        this.channels = List.copyOf(channels);
    }

    public static List<GamePackets.ChannelInfo> loadChannels(AppConfig config) {
        List<GamePackets.ChannelInfo> out = new ArrayList<>();
        byte id = 0;
        for (Map<String, Object> row : config.list("channels")) {
            GamePackets.ChannelInfo c = new GamePackets.ChannelInfo();
            c.name = config.nestedFrom(row, "name", "Channel");
            c.maxUser = (short) config.nestedIntFrom(row, "maxUser", 500);
            c.id = id++;
            c.flag = config.nestedIntFrom(row, "flag", 0);
            c.flag2 = config.nestedIntFrom(row, "flag2", 0);
            out.add(c);
        }
        if (out.isEmpty()) {
            GamePackets.ChannelInfo fallback = new GamePackets.ChannelInfo();
            fallback.name = "Channel (Rookies)";
            fallback.maxUser = 500;
            fallback.id = 0;
            out.add(fallback);
        }
        return out;
    }

    public void onPacket(Session session, byte[] plaintext) {
        if (plaintext.length < 2) {
            return;
        }
        PacketReader reader = new PacketReader(plaintext);
        int opcode = reader.opcode();
        switch (opcode) {
            case GamePackets.CLIENT_REQUEST_LOGIN -> requestLogin(session, reader);
            case GamePackets.CLIENT_ENTER_CHANNEL -> enterChannel(session, reader);
            case GamePackets.CLIENT_REQUEST_CREATE_ROOM -> createRoom(session, reader);
            case GamePackets.CLIENT_REQUEST_START_GAME -> startGame(session);
            case GamePackets.CLIENT_EXIT_ROOM -> leaveRoom(session);
            case GamePackets.CLIENT_LEAVE_PRACTICE -> leavePractice(session);
            default -> log.debug("unhandled game opcode 0x{}", Integer.toHexString(opcode));
        }
    }

    private void requestLogin(Session session, PacketReader reader) {
        try {
            GamePackets.GameLogin data = GamePackets.readLogin(reader);
            PlayerContext pi = session.player();
            pi.id = data.id() == null ? "" : data.id();
            pi.uid = data.uid() & 0xffff_ffffL;

            if (data.packetVersion() == 0) {
                session.send(GamePackets.loginAckU32(GamePackets.ACK_INVALID_VERSION));
                session.setAuthorized(false);
                return;
            }
            if (pi.uid == 0) {
                session.send(GamePackets.loginAckU32(GamePackets.ACK_LOGIN_FAIL));
                session.setAuthorized(false);
                return;
            }
            if (data.clientVersion() == null || data.clientVersion().isEmpty()) {
                session.send(GamePackets.loginAckU32(GamePackets.ACK_INVALID_VERSION));
                session.setAuthorized(false);
                return;
            }
            if (data.authKeyLogin() == null || data.authKeyLogin().isEmpty()) {
                session.send(GamePackets.loginAckU32(GamePackets.ACK_SECURITY_KEY));
                session.setAuthorized(false);
                return;
            }
            if (data.authKeyGame() == null || data.authKeyGame().isEmpty()) {
                session.send(GamePackets.loginAckU32(GamePackets.ACK_INVALID_VERSION));
                session.setAuthorized(false);
                return;
            }
            if (pi.id.isEmpty() || pi.id.length() >= 0x40) {
                session.send(GamePackets.loginAckU32(GamePackets.ACK_INVALID_ID));
                session.setAuthorized(false);
                return;
            }
            if (repo.isBannedIp(session.ip())) {
                session.send(GamePackets.loginAckU32(GamePackets.ACK_GENERIC_ERROR));
                session.setAuthorized(false);
                session.disconnect();
                return;
            }

            var info = repo.playerInfo(pi.uid).orElse(null);
            if (info == null || info.uid() <= 0) {
                session.send(GamePackets.loginAckU32(GamePackets.ACK_INVALID_ID));
                session.setAuthorized(false);
                return;
            }
            if (!Objects.equals(info.id(), pi.id)) {
                session.send(GamePackets.loginAckU32(GamePackets.ACK_INVALID_ID));
                session.setAuthorized(false);
                return;
            }
            pi.nickname = info.nickname();
            pi.capability = info.capability();
            pi.level = info.level();
            pi.idState = info.idState();
            pi.blockTime = info.blockTimeSeconds();

            if (pi.idState != 0) {
                session.send(GamePackets.loginAckU32(GamePackets.ACK_GENERIC_ERROR));
                session.setAuthorized(false);
                session.disconnect();
                return;
            }

            int decryptedVersion = GamePackets.xorPacketVersion(data.packetVersion());
            if (decryptedVersion != config.packetVersion()) {
                log.warn("packet version mismatch uid={} server={} client={}",
                        pi.uid, config.packetVersion(), decryptedVersion);
                session.send(GamePackets.loginAckU32(GamePackets.ACK_INVALID_VERSION));
                session.setAuthorized(false);
                return;
            }

            if (!keyMatches(data.authKeyLogin(), redis.getLoginKey(pi.uid), repo.loadAuthKeyLogin(pi.uid).orElse(null))) {
                session.send(GamePackets.loginAckU32(GamePackets.ACK_SECURITY_KEY));
                session.setAuthorized(false);
                return;
            }
            if (!keyMatches(
                    data.authKeyGame(),
                    redis.getGameKey(pi.uid, config.uid()),
                    repo.loadAuthKeyGame(pi.uid, config.uid()).orElse(null))) {
                session.send(GamePackets.loginAckU32(GamePackets.ACK_SECURITY_KEY));
                session.setAuthorized(false);
                return;
            }

            sessions.disconnectOthersWithUid(pi.uid, session);
            session.setAuthorized(true);
            pi.authKeyLogin = data.authKeyLogin();
            try {
                repo.registerLogonServer(pi.uid, config.uid());
            } catch (RuntimeException e) {
                log.warn("register game logon failed uid={}: {}", pi.uid, e.toString());
            }

            // C# LoginTask.sendCompleteData: pacote044 principal + warehouse + chars +
            // caddies + equip + mascots + channel list. Remaining dump packets are S4+.
            session.send(GamePackets.loginOkPrincipal(
                    config.clientVersion(),
                    config.version(),
                    session.oid(),
                    pi.id,
                    pi.nickname,
                    pi.capability,
                    (int) pi.uid,
                    pi.level,
                    config.property()));
            var warehouse = inventory.warehouse(pi.uid);
            var characters = inventory.characters(pi.uid);
            var caddies = inventory.caddies(pi.uid);
            session.send(GamePackets.warehouse(warehouse));
            session.send(GamePackets.characters(characters));
            session.send(GamePackets.caddies(caddies));
            session.send(GamePackets.userEquip(inventory.userEquip(pi.uid)));
            session.send(GamePackets.mascots(inventory.mascots(pi.uid)));
            session.send(GamePackets.channelList(channels));
            for (byte[] extra : GamePackets.loginDumpTail(
                    (int) pi.uid, inventory.pang(pi.uid), inventory.cookie(pi.uid), pi.level,
                    inventory.cards(pi.uid))) {
                session.send(extra);
            }
            log.info("game login id={} uid={}", pi.id, pi.uid);
        } catch (RuntimeException e) {
            log.warn("game login failed: {}", e.toString());
            session.setAuthorized(false);
            session.send(GamePackets.loginAckU32(GamePackets.ACK_GENERIC_ERROR));
            session.disconnect();
        }
    }

    private void enterChannel(Session session, PacketReader reader) {
        if (!session.authorized() || reader.remaining() < 1) {
            return;
        }
        int channelId = reader.u8();
        GamePackets.ChannelInfo found = findChannel(channelId);
        if (found == null) {
            session.send(GamePackets.channelEnter(GamePackets.CHANNEL_NOT_FOUND));
            return;
        }
        if (found.currUser >= found.maxUser && found.maxUser > 0) {
            session.send(GamePackets.channelEnter(GamePackets.CHANNEL_FULL));
            return;
        }
        PlayerContext pi = session.player();
        if (pi.channelId == channelId) {
            session.send(GamePackets.channelEnter(GamePackets.CHANNEL_ENTER_OK));
            return;
        }
        adjustChannelCount(pi.channelId, -1);
        pi.channelId = channelId;
        found.currUser++;
        session.send(GamePackets.channelEnter(GamePackets.CHANNEL_ENTER_OK));
    }

    private void createRoom(Session session, PacketReader reader) {
        if (!session.authorized() || session.player().channelId < 0) {
            session.send(GamePackets.roomCreateFailed(GamePackets.CREATE_ROOM_FAILED));
            return;
        }
        GamePackets.CreateRoom room = GamePackets.readCreateRoom(reader);
        if (room.tipo() < 0 || room.tipo() > GamePackets.TIPO_MAX) {
            session.send(GamePackets.roomCreateFailed(GamePackets.CREATE_ROOM_FAILED));
            return;
        }
        if (room.tipo() == GamePackets.TIPO_PRACTICE
                && (room.maxPlayer() > 1 || room.password() == null || room.password().isEmpty())) {
            session.send(GamePackets.roomCreateFailed(GamePackets.CREATE_ROOM_FAILED));
            return;
        }
        int number = nextRoom.getAndIncrement() & 0xffff;
        PlayerContext pi = session.player();
        GameRoom created = new GameRoom(room, number, (int) pi.uid, config.ratePang(), config.rateExp());
        rooms.put(number, created);
        pi.roomNumber = number;
        pi.inPractice = room.tipo() == GamePackets.TIPO_PRACTICE;
        session.send(GamePackets.roomEntered(created.info));
        log.info("room {} tipo={} uid={}", number, room.tipo(), pi.uid);
    }

    private void startGame(Session session) {
        if (!session.authorized()) {
            return;
        }
        PlayerContext pi = session.player();
        GameRoom room = rooms.get(pi.roomNumber);
        if (room == null) {
            session.send(GamePackets.startGameFailed(GamePackets.START_GAME_NOT_READY));
            return;
        }
        if (room.info.master != (int) pi.uid) {
            session.send(GamePackets.startGameFailed(GamePackets.START_GAME_NOT_READY));
            return;
        }
        if (room.info.numPlayer < 2 && !GamePackets.allowsSoloStart(room.tipo)) {
            session.send(GamePackets.startGameFailed(GamePackets.START_GAME_NOT_READY));
            return;
        }
        if (room.inGame) {
            session.send(GamePackets.startGameFailed(GamePackets.START_GAME_NOT_READY));
            return;
        }
        room.inGame = true;
        room.info.state = 0;
        session.send(GamePackets.startGameFlag());
        session.send(GamePackets.startGameFlag2());
        session.send(GamePackets.pangRate(room.info.ratePang));
        log.info("start game room {} tipo={} uid={}", room.info.numero, room.tipo, pi.uid);
    }

    private void leaveRoom(Session session) {
        PlayerContext pi = session.player();
        if (!session.authorized() || pi.roomNumber < 0) {
            return;
        }
        rooms.remove(pi.roomNumber);
        pi.inPractice = false;
        pi.roomNumber = -1;
    }

    private void leavePractice(Session session) {
        if (!session.authorized() || !session.player().inPractice) {
            return;
        }
        leaveRoom(session);
    }

    private GamePackets.ChannelInfo findChannel(int id) {
        for (GamePackets.ChannelInfo c : channels) {
            if ((c.id & 0xff) == id) {
                return c;
            }
        }
        return null;
    }

    private void adjustChannelCount(int channelId, int delta) {
        GamePackets.ChannelInfo c = findChannel(channelId);
        if (c != null) {
            int next = c.currUser + delta;
            c.currUser = (short) Math.max(0, next);
        }
    }

    static boolean keyMatches(String presented, String redisKey, String sqlKey) {
        if (presented == null || presented.isEmpty()) {
            return false;
        }
        return presented.equals(redisKey) || presented.equals(sqlKey);
    }
}
