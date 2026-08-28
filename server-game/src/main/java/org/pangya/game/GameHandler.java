package org.pangya.game;

import org.pangya.db.InventoryRepository;
import org.pangya.db.LoginRepository;
import org.pangya.network.AppConfig;
import org.pangya.network.redis.SessionKeyStore;
import org.pangya.network.session.PlayerContext;
import org.pangya.network.session.Session;
import org.pangya.network.session.SessionManager;
import org.pangya.protocol.game.GamePackets;
import org.pangya.protocol.login.ServerInfo;
import org.pangya.protocol.packet.PacketIo;
import org.pangya.protocol.packet.PacketReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * JP {@code GameServer.requestLogin} + channel enter + {@code Channel.requestMakeRoom}
 * ({@code Room.getInfo().ToArray()}) + start-game flags + Practice leave.
 */
public final class GameHandler {

    private static final Logger log = LoggerFactory.getLogger(GameHandler.class);
    /** C# {@code requestCheckNick} forbidden-character class. */
    private static final Pattern NICK_BAD = Pattern.compile(".*[\\^$&,\\\\?`´~|\"@#¨'%*!].*");

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
            case GamePackets.CLIENT_REQUEST_JOIN_ROOM -> joinRoom(session, reader);
            case GamePackets.CLIENT_REQUEST_START_GAME -> startGame(session);
            case GamePackets.CLIENT_EXIT_ROOM -> leaveRoom(session);
            case GamePackets.CLIENT_REQUEST_BANISH -> banish(session, reader);
            case GamePackets.CLIENT_REQUEST_USERINFO_OFFLINE -> requestUserInfoOffline(session, reader);
            case GamePackets.CLIENT_LEAVE_PRACTICE -> leavePractice(session);
            case GamePackets.CLIENT_LOAD_OK -> finishLoadHole(session);
            case GamePackets.CLIENT_HOLE_INFO -> initHole(session, reader);
            case GamePackets.CLIENT_SHOT -> initShot(session);
            case GamePackets.CLIENT_CAMERA -> changeMira(session, reader);
            case GamePackets.CLIENT_CLICK -> changeBarSpace(session, reader);
            case GamePackets.CLIENT_POWER_SHOT -> activePowerShot(session, reader);
            case GamePackets.CLIENT_CLUB -> changeClub(session, reader);
            case GamePackets.CLIENT_USE_ITEM -> { }
            case GamePackets.CLIENT_EMOTICON -> changeTyping(session, reader);
            case GamePackets.CLIENT_DROP -> moveBall(session, reader);
            case GamePackets.CLIENT_TIMECHECK -> { }
            case GamePackets.CLIENT_LOADING_INFO -> loadPercent(session, reader);
            case GamePackets.CLIENT_TEAMCHAT -> teamChat(session, reader);
            case GamePackets.CLIENT_ALLOW_WHISPER -> allowWhisper(session, reader);
            case GamePackets.CLIENT_SHOT_RESULT -> syncShot(session, reader);
            case GamePackets.CLIENT_SHOT_ACK -> finishShot(session);
            case GamePackets.CLIENT_REQUEST_EQUIP_ITEM -> equipItem(session, reader);
            case GamePackets.CLIENT_REQUEST_BUY_ITEM -> buyItem(session, reader);
            case GamePackets.CLIENT_LOUNGE_STATE -> loungeState(session);
            case GamePackets.CLIENT_ENTER_LOBBY -> enterLobby(session);
            case GamePackets.CLIENT_LEAVE_LOBBY -> leaveLobby(session);
            case GamePackets.CLIENT_CHAT -> chat(session, reader);
            case GamePackets.CLIENT_SET_READY -> setReady(session, reader);
            case GamePackets.CLIENT_CHANGE_ROOM_INFO -> changeRoomInfo(session, reader);
            case GamePackets.CLIENT_KEEPALIVE -> { }
            case GamePackets.CLIENT_WHISPER -> whisper(session, reader);
            case GamePackets.CLIENT_REQUEST_CASH -> requestCash(session);
            case GamePackets.CLIENT_REQUEST_USERINFO -> requestPlayerInfo(session, reader);
            case GamePackets.CLIENT_UPDATE_MACRO -> updateMacros(session, reader);
            case GamePackets.CLIENT_REQUEST_SERVER_LIST -> requestServerList(session);
            case GamePackets.CLIENT_REQUEST_RANK -> requestRank(session);
            case GamePackets.CLIENT_CHANGE_TEAM -> changeTeam(session, reader);
            case GamePackets.CLIENT_REQUEST_DETAIL_ROOM_INFO -> requestRoomDetail(session, reader);
            case GamePackets.CLIENT_INVITE -> invite(session, reader);
            case GamePackets.CLIENT_CHECK_INVITE -> { }
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

            // JP LoginTask.sendCompleteData: 0x44, chars 0x70, caddies 0x71,
            // warehouse 0x73, mascots 0xE1, equip 0x72, channel 0x4D, then tail.
            session.send(GamePackets.loginOkPrincipal(
                    config.clientVersion(),
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
            session.send(GamePackets.characters(characters));
            session.send(GamePackets.caddies(caddies));
            session.send(GamePackets.warehouse(warehouse));
            session.send(GamePackets.mascots(inventory.mascots(pi.uid)));
            session.send(GamePackets.userEquip(inventory.userEquip(pi.uid)));
            session.send(GamePackets.channelList(channels));
            for (byte[] extra : GamePackets.loginDumpTail(
                    (int) pi.uid,
                    inventory.pang(pi.uid),
                    inventory.cookie(pi.uid),
                    pi.level,
                    inventory.cards(pi.uid),
                    inventory.counters(pi.uid),
                    inventory.achievements(pi.uid))) {
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
        GameRoom created = new GameRoom(room, number, (int) pi.uid, config.ratePang(), config.rateExp(), pi.channelId);
        created.addPlayer(session);
        created.putPlayerInfo(session, makePlayerInfo(session, created));
        rooms.put(number, created);
        pi.roomNumber = number;
        pi.inPractice = room.tipo() == GamePackets.TIPO_PRACTICE;
        pi.place = 0;
        sendRoomEnterPackets(session, created);
        sendLobbyRoomInfo(created, GamePackets.ROOM_LIST_ADD);
        sendLobbyPlayerInfo(session, GamePackets.LOBBY_USER_UPDATE);
        log.info("room {} tipo={} uid={}", number, room.tipo(), pi.uid);
    }

    private void joinRoom(Session session, PacketReader reader) {
        if (!session.authorized() || session.player().channelId < 0) {
            session.send(GamePackets.roomCreateFailed(GamePackets.CREATE_ROOM_FAILED));
            return;
        }
        GamePackets.JoinRoom req = GamePackets.readJoinRoom(reader);
        GameRoom room = rooms.get(req.numero() & 0xffff);
        if (room == null || room.inGame) {
            session.send(GamePackets.roomCreateFailed(GamePackets.CREATE_ROOM_FAILED));
            return;
        }
        if (room.info.senhaFlag == 0 && !room.info.password.equals(req.password() == null ? "" : req.password())) {
            session.send(GamePackets.roomCreateFailed(GamePackets.CREATE_ROOM_FAILED));
            return;
        }
        if (!room.addPlayer(session)) {
            session.send(GamePackets.roomCreateFailed(GamePackets.CREATE_ROOM_FAILED));
            return;
        }
        PlayerContext pi = session.player();
        pi.roomNumber = room.info.numero;
        pi.inPractice = room.tipo == GamePackets.TIPO_PRACTICE;
        pi.place = 0;
        room.putPlayerInfo(session, makePlayerInfo(session, room));
        sendRoomEnterPackets(session, room);
        sendLobbyRoomInfo(room, GamePackets.ROOM_LIST_UPDATE);
        sendLobbyPlayerInfo(session, GamePackets.LOBBY_USER_UPDATE);
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
        room.startMillis = System.currentTimeMillis();
        room.course = new GameCourse(room.info);
        room.broadcast(GamePackets.startGameFlag());
        room.broadcast(GamePackets.startGameFlag2());
        room.broadcast(GamePackets.pangRate(room.info.ratePang));
        if (GamePackets.usesTourneyInitialData(room.tipo)) {
            // C# TourneyBase.sendInitialData: 0x76 then per-player 0x52.
            room.broadcast(GamePackets.gameInitTourney(room.info.tipoShow));
            for (var member : room.snapshot()) {
                member.send(GamePackets.course(room.info, room.course.holes, room.course.seed));
            }
        } else if (GamePackets.usesVersusInitialData(room.tipo)) {
            List<GamePackets.VersusPlayer> dump = new ArrayList<>();
            for (var member : room.snapshot()) {
                dump.add(versusPlayer(member));
            }
            room.broadcast(GamePackets.gameInitVersus(room.info.tipoShow, dump));
            for (var member : room.snapshot()) {
                member.send(GamePackets.course(room.info, room.course.holes, room.course.seed));
                member.send(GamePackets.mascotSeed(room.course.seed));
            }
        }
        log.info("start game room {} tipo={} uid={}", room.info.numero, room.tipo, pi.uid);
    }

    private void initHole(Session session, PacketReader reader) {
        GameRoom room = inGameRoom(session);
        if (room == null || room.course == null) {
            return;
        }
        GamePackets.InitHole hole = GamePackets.readInitHole(reader);
        GamePackets.HoleInfo info = room.course.find(hole.numero());
        if (info == null) {
            log.warn("init hole missing numero={} room={}", hole.numero(), room.info.numero);
            return;
        }
        GameRoom.PlayerShot shot = room.shots.computeIfAbsent(session.oid(), id -> new GameRoom.PlayerShot());
        shot.hole = hole.numero();
        shot.x = hole.teeX();
        shot.z = hole.teeZ();
        if (GamePackets.usesTourneyInitialData(room.tipo)) {
            session.send(GamePackets.weather(info.weather()));
            session.send(GamePackets.wind(info.wind(), 0, info.degree(), 1));
            int elapsed = (int) Math.max(0, System.currentTimeMillis() - room.startMillis);
            session.send(GamePackets.remainTime(elapsed));
        }
    }

    private void finishLoadHole(Session session) {
        // C# TourneyBase.requestFinishLoadHole sets a flag; first-hole Practice sends nothing extra.
        inGameRoom(session);
    }

    private void initShot(Session session) {
        // C# TourneyBase.requestInitShot stores ShotDataEx and does not reply.
        inGameRoom(session);
    }

    /**
     * VersusBase {@code game_broadcast}; TourneyBase {@code session_send} to the actor.
     */
    private void replyInGame(GameRoom room, Session session, byte[] pkt) {
        if (GamePackets.usesVersusInitialData(room.tipo)) {
            room.broadcast(pkt);
        } else {
            session.send(pkt);
        }
    }

    private void changeMira(Session session, PacketReader reader) {
        GameRoom room = inGameRoom(session);
        if (room == null || reader.remaining() < 4) {
            return;
        }
        replyInGame(room, session, GamePackets.camera(session.oid(), reader.f32()));
    }

    private void changeBarSpace(Session session, PacketReader reader) {
        // C# Versus/Tourney store bar state; timeout {@code 0x5C} is not sent until 3 misses.
        if (inGameRoom(session) != null && reader.remaining() >= 5) {
            reader.u8();
            reader.f32();
        }
    }

    private void activePowerShot(Session session, PacketReader reader) {
        GameRoom room = inGameRoom(session);
        if (room == null || reader.remaining() < 1) {
            return;
        }
        replyInGame(room, session, GamePackets.powerShot(session.oid(), reader.u8()));
    }

    private void changeClub(Session session, PacketReader reader) {
        GameRoom room = inGameRoom(session);
        if (room == null || reader.remaining() < 1) {
            return;
        }
        replyInGame(room, session, GamePackets.club(session.oid(), reader.u8()));
    }

    private void changeTyping(Session session, PacketReader reader) {
        GameRoom room = inGameRoom(session);
        if (room == null || reader.remaining() < 2) {
            return;
        }
        replyInGame(room, session, GamePackets.typing(session.oid(), reader.i16()));
    }

    private void moveBall(Session session, PacketReader reader) {
        GameRoom room = inGameRoom(session);
        if (room == null || reader.remaining() < 12) {
            return;
        }
        replyInGame(room, session, GamePackets.moveBall(reader.f32(), reader.f32(), reader.f32()));
    }

    private void loadPercent(Session session, PacketReader reader) {
        GameRoom room = inGameRoom(session);
        if (room == null || reader.remaining() < 1 || !GamePackets.usesVersusInitialData(room.tipo)) {
            return;
        }
        room.broadcast(GamePackets.loadPercent(session.oid(), reader.u8()));
    }

    private void teamChat(Session session, PacketReader reader) {
        if (!session.authorized() || reader.remaining() < 2) {
            return;
        }
        String msg = reader.pstr();
        if (msg == null || msg.isEmpty()) {
            return;
        }
        GameRoom room = rooms.get(session.player().roomNumber);
        if (room == null || (room.tipo != GamePackets.TIPO_MATCH && room.tipo != GamePackets.TIPO_GUILD_BATTLE)) {
            return;
        }
        GamePackets.PlayerRoomInfo self = room.playerInfo(session);
        if (self == null) {
            return;
        }
        int team = self.stateFlag & GamePackets.PLAYER_TEAM_BIT;
        String nick = session.player().nickname == null ? "" : session.player().nickname;
        byte[] pkt = GamePackets.teamChat(nick, msg);
        for (Session member : room.snapshot()) {
            GamePackets.PlayerRoomInfo info = room.playerInfo(member);
            if (info != null && (info.stateFlag & GamePackets.PLAYER_TEAM_BIT) == team) {
                member.send(pkt);
            }
        }
    }

    private void allowWhisper(Session session, PacketReader reader) {
        if (!session.authorized() || reader.remaining() < 1) {
            return;
        }
        int whisper = reader.u8();
        if (whisper > 1) {
            return;
        }
        session.player().whisper = whisper;
    }

    private void syncShot(Session session, PacketReader reader) {
        GameRoom room = inGameRoom(session);
        if (room == null || reader.remaining() != GamePackets.SHOT_SYNC_BYTES) {
            return;
        }
        byte[] encrypted = reader.readBytes(GamePackets.SHOT_SYNC_BYTES);
        GamePackets.ShotSync sync = GamePackets.readShotSync(GamePackets.xorRoomKey(encrypted, room.info.key));
        GameRoom.PlayerShot shot = room.shots.computeIfAbsent(session.oid(), id -> new GameRoom.PlayerShot());
        shot.x = sync.x();
        shot.z = sync.z();
        shot.shotState = sync.shotState();
        shot.tempo = sync.tempo() & 0xffff;
        int hole = shot.hole == 0 ? 1 : shot.hole;
        room.broadcast(GamePackets.syncShot(sync.oid(), hole, shot.x, shot.z, shot.shotState, shot.tempo));
    }

    private void finishShot(Session session) {
        GameRoom room = inGameRoom(session);
        if (room == null) {
            return;
        }
        session.send(GamePackets.endShot(session.oid()));
    }

    private void equipItem(Session session, PacketReader reader) {
        if (!session.authorized() || reader.remaining() < 1) {
            return;
        }
        int type = reader.u8();
        long uid = session.player().uid;
        int err = GamePackets.EQUIP_OK;
        byte[] extra = new byte[0];
        try {
            switch (type) {
                case 0 -> { // C# CharacterInfo.ToRead — parts without IFF validation.
                    if (reader.remaining() < GamePackets.CHARACTER_INFO_BYTES) {
                        err = 1;
                    } else {
                        GamePackets.CharacterInfo ci = GamePackets.CharacterInfo.read(reader);
                        if (ci.id == 0) {
                            err = 1;
                        } else if (inventory.characters(uid).stream().noneMatch(c -> c.id == ci.id)) {
                            err = 2;
                        } else {
                            inventory.updateCharacterParts(uid, ci);
                            extra = ci.toArray();
                        }
                    }
                }
                case 1 -> { // Caddie id
                    int id = reader.remaining() >= 4 ? reader.i32() : 0;
                    if (id != 0 && inventory.caddies(uid).stream().noneMatch(c -> c.id == id)) {
                        err = 2;
                    } else {
                        inventory.equipCaddie(uid, id);
                        extra = i32le(id);
                    }
                }
                case 3 -> { // Ball typeid + clubset id
                    int ball = reader.remaining() >= 4 ? reader.i32() : 0;
                    int club = reader.remaining() >= 4 ? reader.i32() : 0;
                    boolean ballOk = ball == 0
                            || inventory.warehouse(uid).stream().anyMatch(w -> w.typeid == ball);
                    boolean clubOk = club == 0
                            || inventory.warehouse(uid).stream().anyMatch(w -> w.id == club);
                    if (!ballOk || !clubOk) {
                        err = 2;
                    } else {
                        inventory.equipBallAndClub(uid, ball, club);
                        extra = concat(u32le(ball), i32le(club));
                    }
                }
                case 5 -> { // Character id
                    int id = reader.remaining() >= 4 ? reader.i32() : 0;
                    if (inventory.characters(uid).stream().noneMatch(c -> c.id == id)) {
                        err = 2;
                    } else {
                        inventory.equipCharacter(uid, id);
                        extra = i32le(id);
                    }
                }
                case 8 -> { // Mascot id
                    int id = reader.remaining() >= 4 ? reader.i32() : 0;
                    var found = inventory.mascots(uid).stream().filter(m -> m.id == id).findFirst();
                    if (id != 0 && found.isEmpty()) {
                        err = 2;
                    } else {
                        inventory.equipMascot(uid, id);
                        extra = found.map(m -> m.toArray()).orElse(new byte[GamePackets.MASCOT_INFO_BYTES]);
                    }
                }
                default -> extra = new byte[0];
            }
        } catch (RuntimeException e) {
            log.warn("equip type={} uid={} failed: {}", type, uid, e.toString());
            err = 1;
            extra = new byte[0];
        }
        session.send(GamePackets.equipAck(err, type, extra));
    }

    private void buyItem(Session session, PacketReader reader) {
        if (!session.authorized()) {
            return;
        }
        try {
            GamePackets.BuyRequest req = GamePackets.readBuyRequest(reader);
            if (req.items().isEmpty()) {
                session.send(GamePackets.buyFailed(GamePackets.BUY_FAIL_EMPTY));
                return;
            }
            List<GamePackets.BoughtItem> bought = new ArrayList<>();
            long pang = 0;
            long cookie = 0;
            long pangSpent = 0;
            long cookieSpent = 0;
            for (GamePackets.BuyItem item : req.items()) {
                InventoryRepository.ShopBuyResult result = inventory.buyShopItem(
                        session.player().uid, item.typeid(), item.qntd(), item.pang(), item.cookie());
                if (result.code() != 0) {
                    session.send(GamePackets.buyFailed(result.code()));
                    return;
                }
                bought.add(new GamePackets.BoughtItem(
                        result.typeid(), result.itemId(), 0, 0, result.qntdDep()));
                pang = result.pang();
                cookie = result.cookie();
                pangSpent += result.pangSpent();
                cookieSpent += result.cookieSpent();
            }
            if (pangSpent > 0) {
                session.send(GamePackets.pangSpent(pang, pangSpent));
            }
            if (cookieSpent > 0) {
                session.send(GamePackets.cookieBalance(cookie));
            }
            session.send(GamePackets.buyNewItems(bought, pang, cookie));
            session.send(GamePackets.buyOk(pang, cookie));
        } catch (RuntimeException e) {
            log.warn("buy item uid={} failed: {}", session.player().uid, e.toString());
            session.send(GamePackets.buyFailed(GamePackets.BUY_FAIL_GENERIC));
        }
    }

    private void loungeState(Session session) {
        if (!session.authorized()) {
            return;
        }
        GameRoom room = rooms.get(session.player().roomNumber);
        if (room == null || room.tipo != GamePackets.TIPO_LOUNGE) {
            return;
        }
        room.broadcast(GamePackets.loungeState(session.oid()));
    }

    private GameRoom inGameRoom(Session session) {
        if (!session.authorized()) {
            return null;
        }
        GameRoom room = rooms.get(session.player().roomNumber);
        if (room == null || !room.inGame) {
            return null;
        }
        return room;
    }

    private void leaveRoom(Session session) {
        PlayerContext pi = session.player();
        if (!session.authorized() || pi.roomNumber < 0) {
            return;
        }
        GameRoom room = rooms.get(pi.roomNumber);
        boolean destroyed = false;
        GameRoom leftover = null;
        GamePackets.PlayerRoomInfo leaver = room == null ? null : room.playerInfo(session);
        if (room != null) {
            boolean wasMaster = room.info.master == (int) pi.uid;
            room.removePlayer(session);
            if (room.info.numPlayer <= 0) {
                rooms.remove(pi.roomNumber);
                destroyed = true;
            } else if (wasMaster && !room.inGame) {
                Session next = room.electMaster();
                if (next != null) {
                    room.broadcast(GamePackets.decisionRoomMaster(next.oid(), 0));
                }
            }
            leftover = room;
        } else {
            rooms.remove(pi.roomNumber);
        }
        pi.inPractice = false;
        pi.roomNumber = -1;
        pi.place = 0;
        if (leftover != null && !destroyed) {
            leftover.broadcast(GamePackets.roomUpdate(leftover.info));
            int base = GamePackets.usesCompactPlayerRoomInfo(leftover.tipo) ? 0x100 : 0;
            GamePackets.PlayerRoomInfo left = leaver;
            if (left == null) {
                left = new GamePackets.PlayerRoomInfo();
                left.oid = session.oid();
            }
            leftover.broadcast(GamePackets.roomPlayers(base + 2, List.of(left)));
        }
        if (leftover != null) {
            sendLobbyRoomInfo(leftover, destroyed ? GamePackets.ROOM_LIST_REMOVE : GamePackets.ROOM_LIST_UPDATE);
        }
        sendLobbyPlayerInfo(session, GamePackets.LOBBY_USER_UPDATE);
        session.send(GamePackets.exitRoomAck(-1));
    }

    /**
     * C# {@code Channel.requestKickPlayerOfRoom}: master kicks uid via
     * {@code leaveRoomMultiPlayer(kick, 3)} → {@code 0x4C} -1.
     */
    private void banish(Session session, PacketReader reader) {
        if (!session.authorized() || reader.remaining() < 4) {
            return;
        }
        long uid = reader.u32() & 0xffff_ffffL;
        PlayerContext pi = session.player();
        GameRoom room = rooms.get(pi.roomNumber);
        if (room == null || room.info.master != (int) pi.uid) {
            return;
        }
        if (room.inGame && (pi.capability & 4) == 0) {
            return;
        }
        Session kick = room.findByUid(uid);
        if (kick == null) {
            return;
        }
        leaveRoom(kick);
    }

    /**
     * C# {@code Channel.requestCheckNick} → {@code pacote0A1}. Found nick is
     * error 0 + uid + MemberInfoEx; anything else is error 2.
     */
    private void requestUserInfoOffline(Session session, PacketReader reader) {
        if (!session.authorized() || reader.remaining() < 1) {
            return;
        }
        reader.u8();
        String nick = reader.remaining() >= 2 ? reader.pstr() : "";
        if (nick.isEmpty()
                || nick.length() < 4
                || nick.indexOf(' ') >= 0
                || NICK_BAD.matcher(nick).matches()) {
            session.send(GamePackets.userInfoOfflineMissing());
            return;
        }
        var info = repo.playerInfoByNick(nick).orElse(null);
        if (info == null || info.uid() == 0) {
            session.send(GamePackets.userInfoOfflineMissing());
            return;
        }
        Session online = sessions.findByUid(info.uid());
        int oid = online == null ? 0 : online.oid();
        int sala = 0xffff;
        if (online != null && online.player().roomNumber >= 0) {
            sala = online.player().roomNumber & 0xffff;
        }
        session.send(GamePackets.userInfoOffline(
                (int) info.uid(),
                GamePackets.memberInfoExPublic(oid, info.id(), info.nickname(), info.capability(), sala)));
    }

    private void leavePractice(Session session) {
        if (!session.authorized() || !session.player().inPractice) {
            return;
        }
        leaveRoom(session);
    }

    private void sendRoomEnterPackets(Session joiner, GameRoom room) {
        room.broadcast(GamePackets.roomUpdate(room.info));
        joiner.send(GamePackets.roomEntered(room.info));
        int base = GamePackets.usesCompactPlayerRoomInfo(room.tipo) ? 0x100 : 0;
        List<GamePackets.PlayerRoomInfo> all = room.playerInfoSnapshot();
        GamePackets.PlayerRoomInfo self = room.playerInfo(joiner);
        room.broadcast(GamePackets.roomPlayers(base, all));
        if (self != null) {
            room.broadcast(GamePackets.roomPlayers(base + 1, List.of(self)));
        }
    }

    private GamePackets.PlayerRoomInfo makePlayerInfo(Session session, GameRoom room) {
        PlayerContext pi = session.player();
        GamePackets.PlayerRoomInfo pri = new GamePackets.PlayerRoomInfo();
        pri.oid = session.oid();
        pri.nickname = pi.nickname == null ? "" : pi.nickname;
        pri.position = room.snapshot().indexOf(session) + 1;
        pri.capability = pi.capability;
        pri.uid = (int) pi.uid;
        pri.level = pi.level;
        pri.place = 10;
        if (room.info.master == (int) pi.uid) {
            pri.stateFlag |= GamePackets.PLAYER_MASTER_BIT | GamePackets.PLAYER_READY_BIT;
        }
        if (pri.position > 0) {
            pri.stateFlag |= (pri.position - 1) % 2;
        }
        GamePackets.UserEquip equip = inventory.userEquip(pi.uid);
        System.arraycopy(equip.skinTypeid, 0, pri.skin, 0, Math.min(6, equip.skinTypeid.length));
        pri.skin[4] = 0;
        pri.title = equip.skinTypeid.length > 5 ? equip.skinTypeid[5] : 0;
        for (GamePackets.CharacterInfo c : inventory.characters(pi.uid)) {
            if (c.id == equip.characterId || pri.character == null) {
                pri.character = c;
                pri.charTypeid = c.typeid;
                if (c.id == equip.characterId) {
                    break;
                }
            }
        }
        for (GamePackets.MascotInfo m : inventory.mascots(pi.uid)) {
            if (m.id == equip.mascotId) {
                pri.mascotTypeid = m.typeid;
                break;
            }
        }
        return pri;
    }

    private GamePackets.VersusPlayer versusPlayer(Session session) {
        PlayerContext pi = session.player();
        GamePackets.UserEquip equip = inventory.userEquip(pi.uid);
        byte[] character = new byte[GamePackets.CHARACTER_INFO_BYTES];
        byte[] caddie = new byte[GamePackets.CADDIE_INFO_BYTES];
        byte[] clubset = new byte[GamePackets.CLUBSET_INFO_BYTES];
        byte[] mascot = new byte[GamePackets.MASCOT_INFO_BYTES];
        for (GamePackets.CharacterInfo c : inventory.characters(pi.uid)) {
            if (c.id == equip.characterId) {
                character = c.toArray();
                break;
            }
        }
        for (GamePackets.CaddieInfo c : inventory.caddies(pi.uid)) {
            if (c.id == equip.caddieId) {
                caddie = c.toArray();
                break;
            }
        }
        for (GamePackets.WarehouseItem w : inventory.warehouse(pi.uid)) {
            if (w.id == equip.clubsetId) {
                clubset = GamePackets.ClubSetInfo.fromWarehouse(w).toArray();
                break;
            }
        }
        for (GamePackets.MascotInfo m : inventory.mascots(pi.uid)) {
            if (m.id == equip.mascotId) {
                mascot = m.toArray();
                break;
            }
        }
        List<GamePackets.CardInfo> cards = inventory.cards(pi.uid);
        return new GamePackets.VersusPlayer(
                GamePackets.memberInfoExPublic(session.oid(), pi.id, pi.nickname, pi.capability),
                (int) pi.uid,
                GamePackets.userInfoPublic(pi.level),
                equip.toArray(),
                character,
                caddie,
                clubset,
                mascot,
                cards);
    }

    private void enterLobby(Session session) {
        if (!session.authorized() || session.player().channelId < 0) {
            return;
        }
        PlayerContext pi = session.player();
        if (pi.inLobby) {
            return;
        }
        pi.inLobby = true;
        GamePackets.PlayerLobbyInfo self = makeLobbyInfo(session);
        List<GamePackets.PlayerLobbyInfo> lobby = lobbyPlayerInfos(pi.channelId);
        if (lobby.isEmpty()) {
            lobby = List.of(self);
        }
        session.send(GamePackets.lobbyUsers(GamePackets.LOBBY_USER_CLEAR, List.of(lobby.getFirst())));
        session.send(GamePackets.lobbyUsers(GamePackets.LOBBY_USER_LIST, lobby));
        session.send(GamePackets.roomList(GamePackets.ROOM_LIST_FULL, visibleRoomArrays(pi.channelId)));
        broadcastChannel(pi.channelId, GamePackets.lobbyUsers(GamePackets.LOBBY_USER_JOIN, List.of(self)));
        session.send(GamePackets.enterLobbyAck());
    }

    private void leaveLobby(Session session) {
        if (!session.authorized()) {
            return;
        }
        PlayerContext pi = session.player();
        if (pi.roomNumber >= 0) {
            leaveRoom(session);
        }
        GamePackets.PlayerLobbyInfo info = makeLobbyInfo(session);
        pi.inLobby = false;
        if (pi.channelId >= 0) {
            broadcastChannel(pi.channelId, GamePackets.lobbyUsers(GamePackets.LOBBY_USER_LEAVE, List.of(info)));
        }
        session.send(GamePackets.leaveLobbyAck());
    }

    private void chat(Session session, PacketReader reader) {
        if (!session.authorized()) {
            return;
        }
        String nick = reader.remaining() >= 2 ? reader.pstr() : "";
        String msg = reader.remaining() >= 2 ? reader.pstr() : "";
        if (nick.isEmpty() || msg.isEmpty()) {
            return;
        }
        PlayerContext pi = session.player();
        String from = pi.nickname == null || pi.nickname.isEmpty() ? nick : pi.nickname;
        byte[] packet = GamePackets.chat(GamePackets.CHAT_NORMAL, from, msg);
        GameRoom room = rooms.get(pi.roomNumber);
        if (room != null) {
            room.broadcast(packet);
        } else if (pi.channelId >= 0) {
            broadcastChannel(pi.channelId, packet);
        }
    }

    private void setReady(Session session, PacketReader reader) {
        if (!session.authorized() || reader.remaining() < 1) {
            return;
        }
        int ready = reader.u8();
        GameRoom room = rooms.get(session.player().roomNumber);
        if (room == null) {
            return;
        }
        GamePackets.PlayerRoomInfo pri = room.playerInfo(session);
        if (pri == null) {
            return;
        }
        if (ready == 0) {
            pri.stateFlag |= GamePackets.PLAYER_READY_BIT;
        } else {
            pri.stateFlag &= ~GamePackets.PLAYER_READY_BIT;
        }
        room.putPlayerInfo(session, pri);
        room.broadcast(GamePackets.readyState(session.oid(), ready));
    }

    private void changeRoomInfo(Session session, PacketReader reader) {
        if (!session.authorized()) {
            return;
        }
        PlayerContext pi = session.player();
        GameRoom room = rooms.get(pi.roomNumber);
        if (room == null) {
            return;
        }
        if (room.info.master != (int) pi.uid) {
            return;
        }
        if (!room.applyInfoChange(reader)) {
            return;
        }
        room.broadcast(GamePackets.roomUpdate(room.info));
        sendLobbyRoomInfo(room, GamePackets.ROOM_LIST_UPDATE);
    }

    private GamePackets.PlayerLobbyInfo makeLobbyInfo(Session session) {
        PlayerContext pi = session.player();
        GamePackets.PlayerLobbyInfo info = new GamePackets.PlayerLobbyInfo();
        info.uid = (int) pi.uid;
        info.oid = session.oid();
        info.salaNumero = pi.roomNumber >= 0 ? pi.roomNumber : 0xFFFF;
        info.nick = pi.nickname == null ? "" : pi.nickname;
        info.level = pi.level;
        info.capability = pi.capability;
        info.teamPoint = 1000;
        info.nickDisplay = "@NT_" + info.nick;
        GamePackets.UserEquip equip = inventory.userEquip(pi.uid);
        info.title = equip.skinTypeid.length > 5 ? equip.skinTypeid[5] : 0;
        return info;
    }

    private List<GamePackets.PlayerLobbyInfo> lobbyPlayerInfos(int channelId) {
        List<GamePackets.PlayerLobbyInfo> out = new ArrayList<>();
        for (Session other : sessions.snapshot()) {
            PlayerContext pi = other.player();
            if (other.authorized() && pi.inLobby && pi.channelId == channelId) {
                out.add(makeLobbyInfo(other));
            }
        }
        return out;
    }

    private List<byte[]> visibleRoomArrays(int channelId) {
        List<byte[]> out = new ArrayList<>();
        for (GameRoom room : rooms.values()) {
            if (room.channelId == channelId && !room.hiddenFromLobby()) {
                out.add(room.info.toArray());
            }
        }
        return out;
    }

    private void sendLobbyRoomInfo(GameRoom room, int option) {
        if (room.hiddenFromLobby()) {
            return;
        }
        broadcastChannel(room.channelId, GamePackets.roomList(option, List.of(room.info.toArray())));
    }

    private void sendLobbyPlayerInfo(Session session, int option) {
        PlayerContext pi = session.player();
        if (!pi.inLobby || pi.channelId < 0) {
            return;
        }
        broadcastChannel(pi.channelId, GamePackets.lobbyUsers(option, List.of(makeLobbyInfo(session))));
    }

    private void broadcastChannel(int channelId, byte[] packet) {
        if (channelId < 0) {
            return;
        }
        for (Session other : sessions.snapshot()) {
            if (other.authorized() && other.player().channelId == channelId) {
                other.send(packet);
            }
        }
    }

    private void whisper(Session session, PacketReader reader) {
        if (!session.authorized()) {
            return;
        }
        String nick = reader.remaining() >= 2 ? reader.pstr() : "";
        String msg = reader.remaining() >= 2 ? reader.pstr() : "";
        if (nick.isEmpty() || msg.isEmpty()) {
            return;
        }
        Session target = sessions.findByNickname(nick);
        if (target == null || target.player().whisper != 1) {
            session.send(GamePackets.chatOffline(nick));
            return;
        }
        String from = session.player().nickname == null ? "" : session.player().nickname;
        String to = target.player().nickname == null ? nick : target.player().nickname;
        session.send(GamePackets.whisper(GamePackets.WHISPER_FROM, to, msg));
        target.send(GamePackets.whisper(GamePackets.WHISPER_TO, from, msg));
    }

    private void requestCash(Session session) {
        if (!session.authorized()) {
            return;
        }
        session.send(GamePackets.cookieBalance(inventory.cookie(session.player().uid)));
    }

    private void requestPlayerInfo(Session session, PacketReader reader) {
        if (!session.authorized() || reader.remaining() < 5) {
            return;
        }
        int uid = reader.u32();
        int season = reader.u8();
        var info = repo.playerInfo(uid & 0xffff_ffffL).orElse(null);
        if (info == null) {
            session.send(GamePackets.playerInfoAck(GamePackets.PLAYER_INFO_OK, 0, 0));
            return;
        }
        boolean viewerGm = (session.player().capability & 4) != 0;
        boolean targetGm = (info.capability() & 4) != 0;
        if (uid != (int) session.player().uid && !viewerGm && targetGm) {
            session.send(GamePackets.playerInfoAck(GamePackets.PLAYER_INFO_NO_GM, season, uid));
            return;
        }
        Session online = sessions.findByUid(info.uid());
        int oid = online == null ? 0 : online.oid();
        int sala = 0xffff;
        if (online != null && online.player().roomNumber >= 0) {
            sala = online.player().roomNumber & 0xffff;
        }
        GamePackets.UserEquip equip = inventory.userEquip(info.uid());
        GamePackets.CharacterInfo character = null;
        for (GamePackets.CharacterInfo c : inventory.characters(info.uid())) {
            if (c.id == equip.characterId || character == null) {
                character = c;
                if (c.id == equip.characterId) {
                    break;
                }
            }
        }
        for (byte[] pkt : GamePackets.playerInfoDump(
                (int) info.uid(),
                season,
                oid,
                sala,
                info.id(),
                info.nickname(),
                info.capability(),
                info.level(),
                character,
                equip)) {
            session.send(pkt);
        }
        session.send(GamePackets.playerInfoAck(GamePackets.PLAYER_INFO_OK, season, (int) info.uid()));
    }

    private void updateMacros(Session session, PacketReader reader) {
        if (!session.authorized() || reader.remaining() < GamePackets.MACRO_COUNT * GamePackets.MACRO_BYTES) {
            return;
        }
        String[] macros = new String[GamePackets.MACRO_COUNT];
        for (int i = 0; i < macros.length; i++) {
            macros[i] = reader.fixedStr(GamePackets.MACRO_BYTES);
        }
        repo.saveMacros(session.player().uid, macros);
    }

    private void requestServerList(Session session) {
        if (!session.authorized()) {
            return;
        }
        List<byte[]> servers = new ArrayList<>();
        for (LoginRepository.ServerListRow row : repo.serverList(1)) {
            ServerInfo info = new ServerInfo();
            info.name = row.name() == null ? "" : row.name();
            info.uid = row.uid();
            info.maxUser = row.maxUser();
            info.currUser = row.currUser();
            info.ip = row.ip() == null ? "" : row.ip();
            info.port = row.port();
            info.property = row.property();
            info.angelicWings = row.angelicWings();
            info.eventFlag = row.eventFlag();
            info.eventMap = row.eventMap();
            info.appRate = row.appRate();
            info.scratchRate = row.scratchRate();
            info.imgNo = row.imgNo();
            servers.add(info.toArray());
        }
        session.send(GamePackets.serverAndChannelList(servers, channels));
    }

    private void requestRank(Session session) {
        if (!session.authorized()) {
            return;
        }
        List<LoginRepository.ServerListRow> ranks = repo.serverList(4);
        if (ranks.isEmpty()) {
            return;
        }
        LoginRepository.ServerListRow rank = ranks.getFirst();
        session.send(GamePackets.rankAddress(rank.ip(), rank.port()));
    }

    private void changeTeam(Session session, PacketReader reader) {
        if (!session.authorized() || reader.remaining() < 1) {
            return;
        }
        int team = reader.u8() & 1;
        GameRoom room = rooms.get(session.player().roomNumber);
        if (room == null) {
            return;
        }
        GamePackets.PlayerRoomInfo pri = room.playerInfo(session);
        if (pri == null) {
            return;
        }
        pri.stateFlag = (pri.stateFlag & ~GamePackets.PLAYER_TEAM_BIT) | team;
        room.putPlayerInfo(session, pri);
        room.broadcast(GamePackets.teamState(session.oid(), team));
    }

    private void requestRoomDetail(Session session, PacketReader reader) {
        if (!session.authorized() || reader.remaining() < 2) {
            return;
        }
        int numero = reader.u16();
        GameRoom room = rooms.get(numero);
        if (room == null) {
            return;
        }
        List<GamePackets.RoomDetailPlayer> players = new ArrayList<>();
        for (Session member : room.snapshot()) {
            GamePackets.PlayerLobbyInfo info = makeLobbyInfo(member);
            players.add(new GamePackets.RoomDetailPlayer(
                    info.oid, info.level, 0, info.capability, info.title, info.teamPoint));
        }
        session.send(GamePackets.roomDetail(room.info, room.tipo, players));
    }

    private void invite(Session session, PacketReader reader) {
        if (!session.authorized() || reader.remaining() < 2) {
            return;
        }
        String nick = reader.pstr();
        if (reader.remaining() < 4) {
            session.send(GamePackets.inviteFail(GamePackets.INVITE_FAIL));
            return;
        }
        long uid = reader.u32() & 0xffff_ffffL;
        PlayerContext pi = session.player();
        GameRoom room = rooms.get(pi.roomNumber);
        Session target = sessions.findByNickname(nick);
        if (room == null
                || target == null
                || target.player().uid != uid
                || target.player().channelId != pi.channelId
                || target.player().roomNumber >= 0
                || target.player().place != 0) {
            session.send(GamePackets.inviteFail(GamePackets.INVITE_FAIL));
            return;
        }
        target.player().place = GamePackets.INVITE_PLACE;
        int fromUid = (int) pi.uid;
        String fromNick = pi.nickname == null ? "" : pi.nickname;
        int toUid = (int) uid;
        session.send(GamePackets.inviteOk(
                GamePackets.SERVER_INVITE_REPLY, config.uid(), pi.channelId, room.info.numero,
                fromUid, fromNick, toUid));
        target.send(GamePackets.inviteOk(
                GamePackets.SERVER_INVITE, config.uid(), pi.channelId, room.info.numero,
                fromUid, fromNick, toUid));
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

    private static byte[] i32le(int v) {
        return PacketIo.u32le(v);
    }

    private static byte[] u32le(int v) {
        return PacketIo.u32le(v);
    }

    private static byte[] concat(byte[] a, byte[] b) {
        return PacketIo.concat(a, b);
    }
}
