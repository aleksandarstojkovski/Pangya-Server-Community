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
import org.pangya.protocol.packet.PacketWriter;
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
    /** C# {@code BroadcastManager} ticker queue; count × {@link GamePackets#TICKER_WAIT_MS}. */
    private final List<String> tickers = new ArrayList<>();
    /** C# {@code PlayerMailBox} / {@code MailBoxManager.sendMessage} in-memory store. */
    private final MailBoxStore mailboxes = new MailBoxStore();
    /** C# {@code Tools.Sanitize} SQL-keyword blacklist (OrdinalIgnoreCase). */
    private static final String[] MAIL_SANITIZE = {
            "--", ";--", "/*", "*/", "@@", "char", "nchar", "varchar", "nvarchar",
            "alter", "begin", "cast", "create", "cursor", "declare", "delete", "drop",
            "exec", "execute", "fetch", "insert", "kill", "open", "select", "sys",
            "sysobjects", "syscolumns", "table", "update"
    };

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
            case GamePackets.CLIENT_REQUEST_SERVER_TIME -> requestServerTime(session);
            case GamePackets.CLIENT_SHOT_RESULT -> syncShot(session, reader);
            case GamePackets.CLIENT_SHOT_ACK -> finishShot(session);
            case GamePackets.CLIENT_REQUEST_EQUIP_ITEM -> equipItem(session, reader);
            case GamePackets.CLIENT_REQUEST_BUY_ITEM -> buyItem(session, reader);
            case GamePackets.CLIENT_REQUEST_GIFT_ITEM -> giftItem(session, reader);
            case GamePackets.CLIENT_LOUNGE_STATE -> loungeState(session);
            case GamePackets.CLIENT_SYNC_ACTIVITY -> playerLocationRoom(session, reader);
            case GamePackets.CLIENT_SLEEP -> changeSleep(session, reader);
            case GamePackets.CLIENT_TEESHOT_READY -> finishCharIntro(session);
            case GamePackets.CLIENT_END_STROKE_GAME -> lastPlayerFinishVersus(session);
            case GamePackets.CLIENT_TEAM_HOLEIN_PANG -> teamFinishHole(session, reader);
            case GamePackets.CLIENT_ANSWER_GOSTOP -> replyContinueVersus(session, reader);
            case GamePackets.CLIENT_REEMPLOY_CADDIE -> payCaddieHoliday(session, reader);
            case GamePackets.CLIENT_REPORT -> reportChat(session);
            case GamePackets.CLIENT_REPORT_ERROR -> reportClientException(session, reader);
            case GamePackets.CLIENT_MSN_REQUEST -> translateSubPacket(session, reader);
            case GamePackets.CLIENT_SHOT_COMMAND -> initShotArrows(session, reader);
            case GamePackets.CLIENT_REPLAY_ONLINE -> activeReplay(session, reader);
            case GamePackets.CLIENT_CHAT_PENALITY -> changeChatBlock(session, reader);
            case GamePackets.CLIENT_NOTICE -> noticeGm(session, reader);
            case GamePackets.CLIENT_DESTROY_ROOM -> destroyRoom(session, reader);
            case GamePackets.CLIENT_SPEED_RATE -> activeBooster(session, reader);
            case GamePackets.CLIENT_ONELINE_REQUEST -> sendTicker(session, reader);
            case GamePackets.CLIENT_ONELINE_QUERY -> queueTicker(session);
            case GamePackets.CLIENT_CHANGE_MASCOT -> changeMascotMessage(session, reader);
            case GamePackets.CLIENT_SHOP_CANCEL -> cancelEditShop(session);
            case GamePackets.CLIENT_SHOP_CLOSE -> closeSaleShop(session);
            case GamePackets.CLIENT_SHOP_OPEN_EDIT -> openEditShop(session);
            case GamePackets.CLIENT_SHOP_VIEW -> viewSaleShop(session, reader);
            case GamePackets.CLIENT_SHOP_CLOSE_VIEW -> closeViewSaleShop(session, reader);
            case GamePackets.CLIENT_SHOP_NAME -> changeSaleShopName(session, reader);
            case GamePackets.CLIENT_SHOP_VISIT -> visitSaleShop(session);
            case GamePackets.CLIENT_SHOP_PANG -> pangSaleShop(session);
            case GamePackets.CLIENT_SHOP_OPEN_ITEMS -> openSaleShopItems(session, reader);
            case GamePackets.CLIENT_SHOP_BUY -> buySaleShop(session, reader);
            case GamePackets.CLIENT_PAPEL_SHOP -> openPapelShop(session);
            case GamePackets.CLIENT_ENTER_SHOP -> enterShop(session);
            case GamePackets.CLIENT_OPEN_MAILBOX -> openMailBox(session, reader);
            case GamePackets.CLIENT_OPEN_MAIL -> openMail(session, reader);
            case GamePackets.CLIENT_SEND_MAIL -> sendMail(session, reader);
            case GamePackets.CLIENT_TAKE_MAIL -> takeMail(session, reader);
            case GamePackets.CLIENT_DELETE_MAIL -> deleteMail(session, reader);
            case GamePackets.CLIENT_ENTER_LOBBY -> enterLobby(session);
            case GamePackets.CLIENT_LEAVE_LOBBY -> leaveLobby(session);
            case GamePackets.CLIENT_CHAT -> chat(session, reader);
            case GamePackets.CLIENT_SET_READY -> setReady(session, reader);
            case GamePackets.CLIENT_CHANGE_ROOM_INFO -> changeRoomInfo(session, reader);
            case GamePackets.CLIENT_MY_STATISTICS -> finishGame(session, reader);
            case GamePackets.CLIENT_HOLE_STAT -> finishHoleData(session, reader);
            case GamePackets.CLIENT_PAUSE -> pauseGame(session, reader);
            case GamePackets.CLIENT_LOBBY_USERINFO_CHANGED -> changeLobbyItem(session, reader);
            case GamePackets.CLIENT_REQUEST_USERINFO_CHANGED -> changeRoomItem(session, reader);
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

            // C# LoginManager case 32 pacote210, then sendCompleteData 0x44…
            session.send(GamePackets.newMail(unreadMailBytes(pi.uid)));
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
        room.clearCharIntro();
        room.reported.clear();
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

    private void requestServerTime(Session session) {
        if (!session.authorized()) {
            return;
        }
        session.send(GamePackets.serverTime());
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

    /**
     * C# {@code packet006} / {@code requestFinishGame}: {@code UserInfoEx} 265 then
     * Practice {@code finish_game(6)} → {@code 0xCE}/{@code 0x79}/{@code 0x45}/{@code 0x134}/{@code 0xC8}.
     */
    private void finishGame(Session session, PacketReader reader) {
        GameRoom room = inGameRoom(session);
        if (room == null || reader.remaining() < GamePackets.USER_INFO_BYTES) {
            return;
        }
        reader.readBytes(GamePackets.USER_INFO_BYTES);
        sendFinishGameDump(session, room);
        finishGameRoom(room);
    }

    /**
     * C# {@code packet037} / {@code requestLastPlayerFinishVersus}: Versus
     * {@code finish_game(first, 2)} then {@code room.finish_game()}.
     */
    private void lastPlayerFinishVersus(Session session) {
        GameRoom room = inGameRoom(session);
        if (room == null || !GamePackets.usesVersusInitialData(room.tipo)) {
            return;
        }
        sendFinishGameDump(session, room);
        finishGameRoom(room);
    }

    /**
     * C# {@code packet035} / {@code requestTeamFinishHole}: Match stores u16
     * finish state (9 putt / 10 chip-in). {@code GameBase} no-op, no reply.
     */
    private void teamFinishHole(Session session, PacketReader reader) {
        GameRoom room = inGameRoom(session);
        if (room == null) {
            return;
        }
        if (reader.remaining() >= 2) {
            reader.u16();
        }
    }

    /**
     * C# {@code packet036}: u8 0 stops Versus like {@code 0x37}; u8 1 calls
     * {@code changeTurn} which needs a turn timer (not invented here).
     */
    private void replyContinueVersus(Session session, PacketReader reader) {
        GameRoom room = inGameRoom(session);
        if (room == null || reader.remaining() < 1
                || !GamePackets.usesVersusInitialData(room.tipo)) {
            return;
        }
        int opt = reader.u8();
        if (opt == GamePackets.CONTINUE_STOP) {
            sendFinishGameDump(session, room);
            finishGameRoom(room);
        }
    }

    /**
     * C# {@code packet039}: holiday pay needs IFF {@code valor_mensal}. Catch
     * always writes {@code 0x93} u8 1.
     */
    private void payCaddieHoliday(Session session, PacketReader reader) {
        if (!session.authorized()) {
            return;
        }
        if (reader.remaining() >= 4) {
            reader.i32();
        }
        session.send(GamePackets.caddieHolidayFail());
    }

    /**
     * C# {@code packet03A}: in-game {@code 0x94} u8 0 first time, 1 if already
     * reported this game.
     */
    private void reportChat(Session session) {
        GameRoom room = inGameRoom(session);
        if (room == null) {
            return;
        }
        long uid = session.player().uid;
        if (room.reported.putIfAbsent(uid, Boolean.TRUE) == null) {
            session.send(GamePackets.reportAck(GamePackets.REPORT_OK));
        } else {
            session.send(GamePackets.reportAck(GamePackets.REPORT_ALREADY));
        }
    }

    /**
     * C# {@code packet04F}: u8 → {@code 0xAC} oid+state. Versus broadcasts;
     * Tourney/Practice {@code session_send}.
     */
    private void changeChatBlock(Session session, PacketReader reader) {
        GameRoom room = inGameRoom(session);
        if (room == null || reader.remaining() < 1) {
            return;
        }
        int block = reader.u8();
        byte[] pkt = GamePackets.chatPenalty(session.oid(), block);
        if (GamePackets.usesVersusInitialData(room.tipo)) {
            room.broadcast(pkt);
        } else {
            session.send(pkt);
        }
    }

    /**
     * C# {@code packet065}: f32 speed → {@code 0xC7}. C# non-premium consumes
     * warehouse TIME_BOOSTER (IFF); without that item the catch is silent.
     * Java skips consume and always replies.
     */
    private void activeBooster(Session session, PacketReader reader) {
        GameRoom room = inGameRoom(session);
        if (room == null || reader.remaining() < 4) {
            return;
        }
        float speed = reader.f32();
        byte[] pkt = GamePackets.speedRate(speed, session.oid());
        if (GamePackets.usesVersusInitialData(room.tipo)) {
            room.broadcast(pkt);
        } else {
            session.send(pkt);
        }
    }

    /**
     * C# {@code packet067}: {@code 0xCA} count + count×30000 ms.
     */
    private void queueTicker(Session session) {
        if (!session.authorized()) {
            return;
        }
        int count;
        synchronized (tickers) {
            count = tickers.size();
        }
        session.send(GamePackets.tickerQueue(count, count * GamePackets.TICKER_WAIT_MS));
    }

    /**
     * C# {@code packet066}: PStr → consume 1 cookie, queue, {@code 0x96}, then
     * {@code 0xC9} to every channel. Empty/funds fail {@code 0x50}.
     */
    private void sendTicker(Session session, PacketReader reader) {
        if (!session.authorized()) {
            return;
        }
        String msg = reader.remaining() >= 2 ? reader.pstr() : "";
        if (msg.isEmpty()) {
            session.send(GamePackets.tickerFail(GamePackets.TICKER_FAIL_GENERIC));
            return;
        }
        long uid = session.player().uid;
        long cookie = inventory.cookie(uid);
        if (cookie < GamePackets.TICKER_COOKIE) {
            session.send(GamePackets.tickerFail(GamePackets.TICKER_FAIL_FUNDS));
            return;
        }
        inventory.setPangCookie(uid, inventory.pang(uid), cookie - GamePackets.TICKER_COOKIE);
        long left = cookie - GamePackets.TICKER_COOKIE;
        synchronized (tickers) {
            tickers.add(msg);
        }
        session.send(GamePackets.cookieBalance(left));
        String nick = session.player().nickname == null ? "" : session.player().nickname;
        broadcastAll(GamePackets.tickerMsg(nick, msg));
    }

    /**
     * C# {@code packet073}: IFF mascot message. Without IFF the catch is
     * {@code 0xE2} sbyte -1 + id -1 + empty msg + pang.
     */
    private void changeMascotMessage(Session session, PacketReader reader) {
        if (!session.authorized()) {
            return;
        }
        if (reader.remaining() >= 4) {
            reader.i32();
        }
        if (reader.remaining() >= 2) {
            reader.pstr();
        }
        session.send(GamePackets.mascotMessageFail(inventory.pang(session.player().uid)));
    }

    /**
     * C# {@code packet057}: GM broadcasts {@code 0x40} option 7. Others get
     * {@code SendChatNotice("Command no Executed")}.
     */
    private void noticeGm(Session session, PacketReader reader) {
        if (!session.authorized()) {
            return;
        }
        String nick = session.player().nickname == null ? "" : session.player().nickname;
        if ((session.player().capability & GamePackets.CAPABILITY_GM) == 0) {
            session.send(GamePackets.chat(GamePackets.CHAT_NOTICE, nick, "Command no Executed"));
            return;
        }
        String notice = reader.remaining() >= 2 ? reader.pstr() : "";
        if (notice.isEmpty()) {
            session.send(GamePackets.chat(GamePackets.CHAT_NOTICE, nick, "Command no Executed"));
            return;
        }
        broadcastAll(GamePackets.chat(GamePackets.CHAT_NOTICE, nick, notice));
    }

    /**
     * C# {@code packet060}: GM i16 room number, kick everyone. Non-GM notice.
     */
    private void destroyRoom(Session session, PacketReader reader) {
        if (!session.authorized()) {
            return;
        }
        String nick = session.player().nickname == null ? "" : session.player().nickname;
        if ((session.player().capability & GamePackets.CAPABILITY_GM) == 0) {
            session.send(GamePackets.chat(GamePackets.CHAT_NOTICE, nick, "Command no executed!"));
            return;
        }
        if (reader.remaining() < 2) {
            return;
        }
        int numero = reader.i16();
        GameRoom room = rooms.get(numero);
        if (room == null) {
            session.send(GamePackets.chat(GamePackets.CHAT_NOTICE, nick, "Command no executed!"));
            return;
        }
        for (Session member : room.snapshot()) {
            leaveRoom(member);
        }
    }

    private void broadcastAll(byte[] packet) {
        for (Session other : sessions.snapshot()) {
            if (other.authorized()) {
                other.send(packet);
            }
        }
    }

    private GameRoom playerRoom(Session session) {
        if (!session.authorized()) {
            return null;
        }
        int numero = session.player().roomNumber;
        return numero < 0 ? null : rooms.get(numero);
    }

    private void sendShop(Session session, GameRoom.ShopReply reply) {
        if (reply.broadcast) {
            GameRoom room = playerRoom(session);
            if (room != null) {
                room.broadcast(reply.packet);
                return;
            }
        }
        session.send(reply.packet);
    }

    /** C# {@code packet076} {@code openShopToEdit} → {@code 0xE5} broadcast. */
    private void openEditShop(Session session) {
        GameRoom room = playerRoom(session);
        if (room == null) {
            return;
        }
        sendShop(session, room.openEditShop(session));
    }

    /** C# {@code packet074} cancel edit → {@code 0xE3}. */
    private void cancelEditShop(Session session) {
        GameRoom room = playerRoom(session);
        if (room == null) {
            return;
        }
        sendShop(session, room.cancelEditShop(session));
    }

    /** C# {@code packet075} close → {@code 0xE4} ok / {@code 0xE5} fail. */
    private void closeSaleShop(Session session) {
        GameRoom room = playerRoom(session);
        if (room == null) {
            return;
        }
        sendShop(session, room.closeShop(session));
    }

    /** C# {@code packet079} PStr name → {@code 0xE8}. */
    private void changeSaleShopName(Session session, PacketReader reader) {
        GameRoom room = playerRoom(session);
        if (room == null) {
            return;
        }
        String name = reader.remaining() >= 2 ? reader.pstr() : "";
        sendShop(session, room.changeShopName(session, name));
    }

    /** C# {@code packet07A} → {@code 0xE9}. */
    private void visitSaleShop(Session session) {
        GameRoom room = playerRoom(session);
        if (room == null) {
            return;
        }
        session.send(room.visitCountShop(session));
    }

    /** C# {@code packet07B} → {@code 0xEA}. */
    private void pangSaleShop(Session session) {
        GameRoom room = playerRoom(session);
        if (room == null) {
            return;
        }
        session.send(room.pangShop(session));
    }

    /** C# {@code packet077}: u32 owner → {@code 0xE6}. Empty items fail 5200450. */
    private void viewSaleShop(Session session, PacketReader reader) {
        GameRoom room = playerRoom(session);
        if (room == null) {
            return;
        }
        long owner = reader.remaining() >= 4 ? reader.u32Unsigned() : 0;
        session.send(room.viewShop(session, owner));
    }

    /** C# {@code packet078}: u32 owner → {@code 0xE7}. */
    private void closeViewSaleShop(Session session, PacketReader reader) {
        GameRoom room = playerRoom(session);
        if (room == null) {
            return;
        }
        long owner = reader.remaining() >= 4 ? reader.u32Unsigned() : 0;
        session.send(room.closeViewShop(session, owner));
    }

    /**
     * C# {@code packet07C}: u32 count + items. Item sale needs IFF; Java fails
     * {@code 0xEB} after the count check.
     */
    private void openSaleShopItems(Session session, PacketReader reader) {
        GameRoom room = playerRoom(session);
        if (room == null) {
            return;
        }
        int count = reader.remaining() >= 4 ? reader.u32() : 0;
        if (count == 0 || count > 10) {
            session.send(GamePackets.shopItemsFail(GamePackets.shopSys(GamePackets.SHOP_ERR_OPEN_COUNT)));
            return;
        }
        if (room.shops.get(session.player().uid) == null) {
            session.send(GamePackets.shopItemsFail(GamePackets.shopSys(GamePackets.SHOP_ERR_OPEN_NONE)));
            return;
        }
        session.send(GamePackets.shopItemsFail(GamePackets.SHOP_ERR_OPEN_DEFAULT));
    }

    /**
     * C# {@code packet07D}: u32 owner + item. Without IFF, missing shop is
     * {@code 0xEC} sys; otherwise default 5200550.
     */
    private void buySaleShop(Session session, PacketReader reader) {
        GameRoom room = playerRoom(session);
        if (room == null) {
            return;
        }
        long owner = reader.remaining() >= 4 ? reader.u32Unsigned() : 0;
        if (room.shops.get(owner) == null) {
            session.send(GamePackets.shopBuyFail(GamePackets.shopSys(GamePackets.SHOP_ERR_BUY_NONE)));
            return;
        }
        session.send(GamePackets.shopBuyFail(GamePackets.SHOP_ERR_BUY_DEFAULT));
    }

    /** C# {@code packet098}: {@code 0x10B} u32 0 + i64 daily limit. */
    private void openPapelShop(Session session) {
        if (!session.authorized()) {
            return;
        }
        session.send(GamePackets.papelShopOk(0));
    }

    /** C# {@code packet140}: {@code 0x20E} two zeros. */
    private void enterShop(Session session) {
        if (!session.authorized()) {
            return;
        }
        session.send(GamePackets.enterShopOk());
    }

    /**
     * C# {@code packet143} / {@code requestOpenMailBox}: i32 page → {@code 0x211}.
     * Page ≤ 0 writes sys 2. Empty box: error 0 + page + total 1 + count 0.
     */
    private void openMailBox(Session session, PacketReader reader) {
        if (!inChannel(session) || reader.remaining() < 4) {
            return;
        }
        int page = reader.i32();
        if (page <= 0) {
            session.send(GamePackets.mailFail(GamePackets.SERVER_MAILBOX, GamePackets.MAIL_ERR_PAGE));
            return;
        }
        try {
            List<MailBoxStore.MailEntry> mails = mailboxes.page(session.player().uid, page);
            if (mails.isEmpty()) {
                session.send(GamePackets.mailBoxPage(
                        GamePackets.SERVER_MAILBOX, 0, page, 1, List.of()));
                return;
            }
            session.send(GamePackets.mailBoxPage(
                    GamePackets.SERVER_MAILBOX,
                    0,
                    page,
                    mailboxes.totalPages(session.player().uid),
                    mailListBytes(mails)));
        } catch (RuntimeException e) {
            session.send(GamePackets.mailFail(GamePackets.SERVER_MAILBOX, GamePackets.MAIL_ERR_OPEN_DEFAULT));
        }
    }

    /**
     * C# {@code packet144} / {@code requestInfoMail}: i32 id → {@code 0x212}.
     * Missing id is CHANNEL sys 1.
     */
    private void openMail(Session session, PacketReader reader) {
        if (!inChannel(session) || reader.remaining() < 4) {
            return;
        }
        int emailId = reader.i32();
        var found = mailboxes.get(session.player().uid, emailId, true);
        if (found.isEmpty()) {
            session.send(GamePackets.mailFail(GamePackets.SERVER_MAIL_INFO, GamePackets.MAIL_ERR_CHANNEL));
            return;
        }
        MailBoxStore.MailEntry mail = found.get();
        session.send(GamePackets.mailInfoOk(mail.id, mail.fromId, mail.giftDate, mail.msg, mail.lidaYn));
    }

    /**
     * C# {@code packet145} / {@code requestSendMail}. Text-only costs 100 pang and
     * stores the message. Attachments need IFF ({@code 0x5500300}).
     */
    private void sendMail(Session session, PacketReader reader) {
        if (!inChannel(session)) {
            return;
        }
        try {
            if (reader.remaining() < 8) {
                session.send(GamePackets.mailFail(GamePackets.SERVER_MAIL_SEND, GamePackets.MAIL_ERR_SEND_DEFAULT));
                return;
            }
            long fromUid = reader.u32Unsigned();
            long toUid = reader.u32Unsigned();
            String toNick = reader.remaining() >= 2 ? reader.pstr() : "";
            if (reader.remaining() < 2) {
                session.send(GamePackets.mailFail(GamePackets.SERVER_MAIL_SEND, GamePackets.MAIL_ERR_SEND_DEFAULT));
                return;
            }
            reader.u16();
            String msg = reader.remaining() >= 2 ? reader.pstr() : "";
            if (reader.remaining() < 9) {
                session.send(GamePackets.mailFail(GamePackets.SERVER_MAIL_SEND, GamePackets.MAIL_ERR_SEND_DEFAULT));
                return;
            }
            long pangPrice = reader.u64();
            int countItem = reader.u8();
            if (toNick.isEmpty() || !mailSanitize(toNick) || msg.isEmpty() || !mailSanitize(msg)) {
                session.send(GamePackets.mailFail(GamePackets.SERVER_MAIL_SEND, GamePackets.MAIL_ERR_CHANNEL));
                return;
            }
            if (countItem > 0) {
                if (countItem > GamePackets.MAIL_SEND_ITEM_MAX
                        || pangPrice != (long) countItem * GamePackets.MAIL_SEND_ITEM_PANG) {
                    session.send(GamePackets.mailFail(GamePackets.SERVER_MAIL_SEND, GamePackets.MAIL_ERR_SEND_DEFAULT));
                    return;
                }
                int need = countItem * GamePackets.MAIL_ITEM_BYTES;
                if (reader.remaining() >= need) {
                    reader.readBytes(need);
                }
                session.send(GamePackets.mailFail(GamePackets.SERVER_MAIL_SEND, GamePackets.MAIL_ERR_SEND_DEFAULT));
                return;
            }
            if (pangPrice != GamePackets.MAIL_SEND_PANG || toUid == 0) {
                session.send(GamePackets.mailFail(GamePackets.SERVER_MAIL_SEND, GamePackets.MAIL_ERR_SEND_DEFAULT));
                return;
            }
            long uid = session.player().uid;
            long pang = inventory.pang(uid);
            if (pang < pangPrice) {
                session.send(GamePackets.mailFail(GamePackets.SERVER_MAIL_SEND, GamePackets.MAIL_ERR_SEND_DEFAULT));
                return;
            }
            String fromId = fromUid == 0 ? GamePackets.MAIL_FROM_ADM : session.player().nickname;
            mailboxes.add(toUid, fromId, msg);
            inventory.setPangCookie(uid, pang - pangPrice, inventory.cookie(uid));
            session.send(GamePackets.pangSpent(pang - pangPrice, pangPrice));
            session.send(GamePackets.mailFail(GamePackets.SERVER_MAIL_SEND, 0));
            Session target = sessions.findByUid(toUid);
            if (target != null && target.authorized()) {
                target.send(GamePackets.newMail(unreadMailBytes(toUid)));
            }
        } catch (RuntimeException e) {
            log.debug("send-mail failed uid={}", session.player().uid, e);
            session.send(GamePackets.mailFail(GamePackets.SERVER_MAIL_SEND, GamePackets.MAIL_ERR_SEND_DEFAULT));
        }
    }

    /**
     * C# {@code packet146}: i32 id. No IFF warehouse move; empty attachments
     * write {@code pacote214(1)}. Empty mailbox catch writes {@code 0x5500100}.
     */
    private void takeMail(Session session, PacketReader reader) {
        if (!inChannel(session) || reader.remaining() < 4) {
            return;
        }
        int emailId = reader.i32();
        if (mailboxes.isEmpty(session.player().uid)) {
            session.send(GamePackets.mailFail(GamePackets.SERVER_MAIL_TAKE, GamePackets.MAIL_ERR_TAKE_DEFAULT));
            return;
        }
        mailboxes.get(session.player().uid, emailId, false);
        session.send(GamePackets.mailFail(GamePackets.SERVER_MAIL_TAKE, GamePackets.MAIL_ERR_TAKE_EMPTY));
    }

    /**
     * C# {@code packet147}: u32 count + ids + u32 page → {@code 0x215}.
     */
    private void deleteMail(Session session, PacketReader reader) {
        if (!inChannel(session) || reader.remaining() < 4) {
            return;
        }
        int count = reader.u32();
        if (count < 0 || reader.remaining() < count * 4L + 4) {
            session.send(GamePackets.mailFail(GamePackets.SERVER_MAIL_DELETE, GamePackets.MAIL_ERR_DELETE_DEFAULT));
            return;
        }
        int[] ids = new int[count];
        for (int i = 0; i < count; i++) {
            ids[i] = reader.u32();
        }
        int page = reader.u32();
        if (page <= 0) {
            session.send(GamePackets.mailFail(GamePackets.SERVER_MAIL_DELETE, GamePackets.MAIL_ERR_PAGE));
            return;
        }
        try {
            mailboxes.delete(session.player().uid, ids);
            List<MailBoxStore.MailEntry> mails = mailboxes.page(session.player().uid, page);
            if (mails.isEmpty()) {
                session.send(GamePackets.mailBoxPage(
                        GamePackets.SERVER_MAIL_DELETE, 0, page, 1, List.of()));
                return;
            }
            session.send(GamePackets.mailBoxPage(
                    GamePackets.SERVER_MAIL_DELETE,
                    0,
                    page,
                    mailboxes.totalPages(session.player().uid),
                    mailListBytes(mails)));
        } catch (RuntimeException e) {
            session.send(GamePackets.mailFail(GamePackets.SERVER_MAIL_DELETE, GamePackets.MAIL_ERR_DELETE_DEFAULT));
        }
    }

    private boolean inChannel(Session session) {
        return session.authorized() && session.player().channelId >= 0;
    }

    private List<byte[]> unreadMailBytes(long uid) {
        return mailListBytes(mailboxes.unread(uid));
    }

    private static List<byte[]> mailListBytes(List<MailBoxStore.MailEntry> mails) {
        List<byte[]> out = new ArrayList<>(mails.size());
        for (MailBoxStore.MailEntry mail : mails) {
            out.add(MailBoxStore.toListBytes(mail));
        }
        return out;
    }

    /** C# {@code Tools.Sanitize}: reject SQL-keyword substrings. */
    private static boolean mailSanitize(String input) {
        if (input == null || input.isBlank()) {
            return true;
        }
        if (input.length() > 256) {
            return false;
        }
        String lower = input.toLowerCase(java.util.Locale.ROOT);
        for (String word : MAIL_SANITIZE) {
            if (lower.contains(word)) {
                return false;
            }
        }
        return true;
    }

    /**
     * C# {@code packet033}: u8 + PStr then {@code DisconnectSession}.
     */
    private void reportClientException(Session session, PacketReader reader) {
        if (reader.remaining() >= 1) {
            int tipo = reader.u8();
            String msg = reader.remaining() >= 2 ? reader.pstr() : "";
            log.debug("client exception uid={} tipo={} msg={}",
                    session.authorized() ? session.player().uid : 0, tipo, msg);
        }
        if (session.authorized()) {
            leaveRoom(session);
        }
        session.disconnect();
    }

    /**
     * C# {@code packet03C}: u16 sub. Msg_OFF spends 10 pang and {@code 0x95};
     * Friend_List is unimplemented in C# (fail {@code 0x5700105}).
     */
    private void translateSubPacket(Session session, PacketReader reader) {
        if (!session.authorized() || reader.remaining() < 2) {
            return;
        }
        int sub = reader.u16();
        if (sub == GamePackets.MSN_FRIEND_LIST) {
            session.send(GamePackets.msnAckFail(sub, GamePackets.MSN_ERR_FUNDS));
            return;
        }
        if (sub != GamePackets.MSN_MSG_OFF) {
            return;
        }
        if (reader.remaining() < 4) {
            session.send(GamePackets.msnAckFail(sub, GamePackets.MSN_ERR_DEFAULT));
            return;
        }
        long toUid = reader.u32Unsigned();
        String msg = reader.remaining() >= 2 ? reader.pstr() : "";
        int opt = reader.remaining() >= 1 ? reader.u8() : 1;
        if (toUid == 0) {
            session.send(GamePackets.msnAckFail(sub, GamePackets.MSN_ERR_UID));
            return;
        }
        if (msg.isEmpty()) {
            session.send(GamePackets.msnAckFail(sub, GamePackets.MSN_ERR_EMPTY));
            return;
        }
        if (msg.length() > 256) {
            session.send(GamePackets.msnAckFail(sub, GamePackets.MSN_ERR_SIZE));
            return;
        }
        if (opt != 0) {
            session.send(GamePackets.msnAckFail(sub, GamePackets.MSN_ERR_OPT));
            return;
        }
        long uid = session.player().uid;
        long pang = inventory.pang(uid);
        if (pang < GamePackets.MSN_OFF_PANG) {
            session.send(GamePackets.msnAckFail(sub, GamePackets.MSN_ERR_FUNDS));
            return;
        }
        try {
            inventory.setPangCookie(uid, pang - GamePackets.MSN_OFF_PANG, inventory.cookie(uid));
            repo.insertMsgOff(uid, toUid, msg);
            session.send(GamePackets.msnAckOk(sub, pang - GamePackets.MSN_OFF_PANG));
        } catch (RuntimeException e) {
            log.debug("msg-off insert failed uid={}", uid, e);
            session.send(GamePackets.msnAckFail(sub, GamePackets.MSN_ERR_DEFAULT));
        }
    }

    /**
     * C# {@code packet042}: u8 count + count×u32. No success reply.
     */
    private void initShotArrows(Session session, PacketReader reader) {
        GameRoom room = inGameRoom(session);
        if (room == null || reader.remaining() < 1) {
            return;
        }
        int count = reader.u8();
        if (count == 0 || reader.remaining() < count * 4) {
            return;
        }
        for (int i = 0; i < count; i++) {
            reader.u32();
        }
    }

    /**
     * C# {@code packet04A}: u32 typeid. Success {@code 0xA4} needs warehouse;
     * catch is silent.
     */
    private void activeReplay(Session session, PacketReader reader) {
        if (inGameRoom(session) == null) {
            return;
        }
        if (reader.remaining() >= 4) {
            reader.u32();
        }
    }

    private void sendFinishGameDump(Session session, GameRoom room) {
        session.send(GamePackets.prizeList(new int[0]));
        session.send(GamePackets.gameResult(0, room.info.trophy, 0, 2));
        session.send(GamePackets.myStatistics(GamePackets.userInfoPublic(session.player().level)));
        session.send(GamePackets.treasureHunterItem());
        session.send(GamePackets.pangSpent(inventory.pang(session.player().uid), 0));
    }

    /**
     * C# {@code packet031} / {@code requestFinishHoleData}: stores {@code UserInfoEx}, no reply.
     */
    private void finishHoleData(Session session, PacketReader reader) {
        GameRoom room = inGameRoom(session);
        if (room == null || reader.remaining() < GamePackets.USER_INFO_BYTES) {
            return;
        }
        reader.readBytes(GamePackets.USER_INFO_BYTES);
    }

    /**
     * C# Versus {@code requestUnOrPause}: u8 opt → broadcast {@code 0x8B} oid + opt.
     * Tourney/Practice inherit {@code GameBase} no-op.
     */
    private void pauseGame(Session session, PacketReader reader) {
        GameRoom room = inGameRoom(session);
        if (room == null || reader.remaining() < 1 || !GamePackets.usesVersusInitialData(room.tipo)) {
            return;
        }
        int opt = reader.u8();
        if (opt == GamePackets.PAUSE_PAUSE) {
            if (room.pauseCount >= GamePackets.VERSUS_PAUSE_MAX) {
                return;
            }
            room.pauseCount++;
        } else if (opt != GamePackets.PAUSE_RESUME) {
            return;
        }
        room.broadcast(GamePackets.pause(session.oid(), opt));
    }

    /**
     * C# {@code packet00B} / {@code requestChangePlayerItemChannel}: lobby appearance
     * via {@code pacote04B} plus lobby {@code 0x46} option 3.
     */
    private void changeLobbyItem(Session session, PacketReader reader) {
        if (!session.authorized() || reader.remaining() < 1) {
            return;
        }
        int type = reader.u8();
        ItemChange change = applyItemChange(session, type, reader);
        session.send(GamePackets.roomUserInfoChanged(change.err, type, session.oid(), change.extra));
        if (change.err == 0) {
            sendLobbyPlayerInfo(session, GamePackets.LOBBY_USER_UPDATE);
        }
    }

    /**
     * C# {@code packet00C} / {@code requestChangePlayerItemRoom}: in-room {@code TYPE_CHANGE}
     * then broadcast {@code pacote04B}. {@code TC_ALL} only applies equips (Java {@code 0x0E}
     * already sent initial data). Lounge effect needs IFF parts and stays a no-op.
     */
    private void changeRoomItem(Session session, PacketReader reader) {
        if (!session.authorized() || reader.remaining() < 1) {
            return;
        }
        PlayerContext pi = session.player();
        GameRoom room = rooms.get(pi.roomNumber);
        if (room == null) {
            if (pi.inLobby) {
                return;
            }
            session.send(GamePackets.roomUserInfoChanged(1, 0xff, session.oid(), new byte[0]));
            return;
        }
        int type = reader.u8();
        if (type == GamePackets.ITEM_ALL) {
            if (reader.remaining() < 16) {
                return;
            }
            applyItemChange(session, GamePackets.ITEM_CHARACTER, reader.i32());
            applyItemChange(session, GamePackets.ITEM_CADDIE, reader.i32());
            applyItemChange(session, GamePackets.ITEM_CLUBSET, reader.i32());
            applyItemChange(session, GamePackets.ITEM_BALL, reader.i32());
            room.putPlayerInfo(session, makePlayerInfo(session, room));
            return;
        }
        if (type == GamePackets.ITEM_LOUNGE_EFFECT) {
            if (reader.remaining() >= 8) {
                reader.readBytes(8);
            }
            session.send(GamePackets.roomUserInfoChanged(1, type, session.oid(), new byte[0]));
            return;
        }
        ItemChange change = applyItemChange(session, type, reader);
        if (change.err == 0) {
            room.putPlayerInfo(session, makePlayerInfo(session, room));
            room.broadcast(GamePackets.roomUserInfoChanged(0, type, session.oid(), change.extra));
        } else {
            session.send(GamePackets.roomUserInfoChanged(change.err, type, session.oid(), new byte[0]));
        }
    }

    private ItemChange applyItemChange(Session session, int type, PacketReader reader) {
        int id = reader.remaining() >= 4 ? reader.i32() : 0;
        return applyItemChange(session, type, id);
    }

    private ItemChange applyItemChange(Session session, int type, int id) {
        long uid = session.player().uid;
        try {
            return switch (type) {
                case GamePackets.ITEM_CADDIE -> {
                    if (id != 0 && inventory.caddies(uid).stream().noneMatch(c -> c.id == id)) {
                        yield ItemChange.fail(2);
                    }
                    inventory.equipCaddie(uid, id);
                    byte[] extra = inventory.caddies(uid).stream()
                            .filter(c -> c.id == id)
                            .findFirst()
                            .map(GamePackets.CaddieInfo::toArray)
                            .orElse(new byte[GamePackets.CADDIE_INFO_BYTES]);
                    yield ItemChange.ok(extra);
                }
                case GamePackets.ITEM_BALL -> {
                    if (id != 0 && inventory.warehouse(uid).stream().noneMatch(w -> w.typeid == id)) {
                        yield ItemChange.fail(2);
                    }
                    GamePackets.UserEquip equip = inventory.userEquip(uid);
                    inventory.equipBallAndClub(uid, id, equip.clubsetId);
                    yield ItemChange.ok(u32le(id));
                }
                case GamePackets.ITEM_CLUBSET -> {
                    var found = inventory.warehouse(uid).stream().filter(w -> w.id == id).findFirst();
                    if (id == 0 || found.isEmpty()) {
                        yield ItemChange.fail(2);
                    }
                    GamePackets.UserEquip equip = inventory.userEquip(uid);
                    inventory.equipBallAndClub(uid, equip.ballTypeid, id);
                    yield ItemChange.ok(GamePackets.ClubSetInfo.fromWarehouse(found.get()).toArray());
                }
                case GamePackets.ITEM_CHARACTER -> {
                    var found = inventory.characters(uid).stream().filter(c -> c.id == id).findFirst();
                    if (found.isEmpty()) {
                        yield ItemChange.fail(2);
                    }
                    inventory.equipCharacter(uid, id);
                    yield ItemChange.ok(found.get().toArray());
                }
                case GamePackets.ITEM_MASCOT -> {
                    var found = inventory.mascots(uid).stream().filter(m -> m.id == id).findFirst();
                    if (id != 0 && found.isEmpty()) {
                        yield ItemChange.fail(2);
                    }
                    inventory.equipMascot(uid, id);
                    byte[] extra = found.map(GamePackets.MascotInfo::toArray)
                            .orElse(new byte[GamePackets.MASCOT_INFO_BYTES]);
                    yield ItemChange.ok(extra);
                }
                default -> ItemChange.fail(13);
            };
        } catch (RuntimeException e) {
            log.warn("item change type={} uid={} failed: {}", type, uid, e.toString());
            return ItemChange.fail(1);
        }
    }

    /**
     * C# {@code room.finish_game}: waiting state + {@code pacote04A} + per-player {@code 0x48} option 3.
     */
    private void finishGameRoom(GameRoom room) {
        room.inGame = false;
        room.info.state = 1;
        room.course = null;
        room.pauseCount = 0;
        room.shots.clear();
        room.clearCharIntro();
        room.reported.clear();
        for (Session member : room.snapshot()) {
            GamePackets.PlayerRoomInfo pri = room.playerInfo(member);
            if (pri == null) {
                continue;
            }
            if (room.tipo == GamePackets.TIPO_PRACTICE
                    || room.tipo == GamePackets.TIPO_GRAND_ZODIAC_PRACTICE) {
                pri.place = 2;
            } else {
                pri.place = 0;
            }
            room.putPlayerInfo(member, pri);
            room.broadcast(GamePackets.roomPlayers(3, List.of(pri)));
        }
        room.broadcast(GamePackets.roomUpdate(room.info));
        sendLobbyRoomInfo(room, GamePackets.ROOM_LIST_UPDATE);
    }

    private record ItemChange(int err, byte[] extra) {
        static ItemChange ok(byte[] extra) {
            return new ItemChange(0, extra == null ? new byte[0] : extra);
        }

        static ItemChange fail(int err) {
            return new ItemChange(err, new byte[0]);
        }
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

    /**
     * C# {@code packet01F} / {@code requestGiftItemShop}. Success needs IFF + mailbox;
     * Java only emits fail {@code 0x6A} (empty → 9, otherwise init-item 1 / catch 10).
     */
    private void giftItem(Session session, PacketReader reader) {
        if (!session.authorized()) {
            return;
        }
        long pang = inventory.pang(session.player().uid);
        long cookie = inventory.cookie(session.player().uid);
        try {
            if (reader.remaining() < 9) {
                session.send(GamePackets.giftFailed(GamePackets.BUY_FAIL_GENERIC, pang, cookie));
                return;
            }
            reader.u16();
            reader.u32();
            reader.pstr();
            if (reader.remaining() < 3) {
                session.send(GamePackets.giftFailed(GamePackets.BUY_FAIL_GENERIC, pang, cookie));
                return;
            }
            reader.u8();
            int qntd = reader.u16();
            if (qntd <= 0) {
                session.send(GamePackets.giftFailed(GamePackets.BUY_FAIL_EMPTY, pang, cookie));
                return;
            }
            for (int i = 0; i < qntd; i++) {
                GamePackets.readBuyItem(reader);
            }
            session.send(GamePackets.giftFailed(GamePackets.BUY_FAIL_INIT, pang, cookie));
        } catch (RuntimeException e) {
            log.warn("gift item uid={} failed: {}", session.player().uid, e.toString());
            session.send(GamePackets.giftFailed(GamePackets.BUY_FAIL_GENERIC, pang, cookie));
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

    /**
     * C# {@code packet063} / {@code requestPlayerLocationRoom}: type + payload →
     * room {@code 0xC4}. Location types 4/6 add xz and set r; type 9 is unknown.
     */
    private void playerLocationRoom(Session session, PacketReader reader) {
        if (!session.authorized() || reader.remaining() < 1) {
            return;
        }
        GameRoom room = rooms.get(session.player().roomNumber);
        if (room == null) {
            return;
        }
        int type = reader.u8();
        PlayerContext pi = session.player();
        GamePackets.PlayerRoomInfo pri = room.playerInfo(session);
        byte[] payload;
        switch (type) {
            case GamePackets.ACTION_ROTATION -> {
                if (reader.remaining() < 4) {
                    return;
                }
                pi.locR = reader.f32();
                if (pri != null) {
                    pri.r = pi.locR;
                }
                payload = new PacketWriter().f32(pi.locR).toBytes();
            }
            case GamePackets.ACTION_LOUNGER_LOC, GamePackets.ACTION_MOVE -> {
                if (reader.remaining() < GamePackets.LOCATION_BYTES) {
                    return;
                }
                float dx = reader.f32();
                float dz = reader.f32();
                float r = reader.f32();
                pi.locX += dx;
                pi.locZ += dz;
                pi.locR = r;
                if (pri != null) {
                    pri.x = pi.locX;
                    pri.z = pi.locZ;
                    pri.r = pi.locR;
                }
                payload = GamePackets.location(dx, dz, r);
            }
            case GamePackets.ACTION_LOUNGER_STATE -> {
                if (reader.remaining() < 4) {
                    return;
                }
                pi.loungeState = reader.u32();
                if (pri != null) {
                    pri.state = pi.loungeState;
                }
                payload = new PacketWriter().u32(pi.loungeState).toBytes();
            }
            case GamePackets.ACTION_ACK_PLAYER -> {
                if (reader.remaining() < 4) {
                    return;
                }
                pi.stateLounge = reader.u32();
                if (pri != null) {
                    pri.stateLounge = pi.stateLounge;
                }
                payload = new PacketWriter().u32(pi.stateLounge).toBytes();
            }
            case GamePackets.ACTION_MOTION_ROOM,
                    GamePackets.ACTION_MOTION_LOUNGER,
                    GamePackets.ACTION_ANIMATION_WITH_EFFECTS -> payload = reader.remainingBytes();
            default -> {
                return;
            }
        }
        room.broadcast(GamePackets.syncActivity(session.oid(), type, payload));
    }

    /**
     * C# {@code packet032}: u8 state → room {@code 0x8E} plus lobby {@code 0x46} option 3.
     */
    private void changeSleep(Session session, PacketReader reader) {
        if (!session.authorized() || reader.remaining() < 1) {
            return;
        }
        GameRoom room = rooms.get(session.player().roomNumber);
        if (room == null) {
            return;
        }
        int state = reader.u8();
        PlayerContext pi = session.player();
        pi.away = state & 1;
        GamePackets.PlayerRoomInfo pri = room.playerInfo(session);
        if (pri == null) {
            return;
        }
        if (pi.away != 0) {
            pri.stateFlag |= GamePackets.PLAYER_AWAY_BIT;
        } else {
            pri.stateFlag &= ~GamePackets.PLAYER_AWAY_BIT;
        }
        room.broadcast(GamePackets.sleep(session.oid(), state));
        sendLobbyPlayerInfo(session, GamePackets.LOBBY_USER_UPDATE);
    }

    /**
     * C# {@code packet034}: Tourney stores the flag with no reply. Versus broadcasts
     * empty {@code 0x90} when every in-room player has finished the intro.
     */
    private void finishCharIntro(Session session) {
        GameRoom room = inGameRoom(session);
        if (room == null || !GamePackets.usesVersusInitialData(room.tipo)) {
            return;
        }
        if (room.markCharIntro(session)) {
            room.clearCharIntro();
            room.broadcast(GamePackets.teeshotReady());
        }
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
        pri.x = pi.locX;
        pri.z = pi.locZ;
        pri.r = pi.locR;
        pri.state = pi.loungeState;
        pri.stateLounge = pi.stateLounge;
        if (pi.away != 0) {
            pri.stateFlag |= GamePackets.PLAYER_AWAY_BIT;
        }
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
        if (pi.away != 0) {
            info.state |= GamePackets.PLAYER_LOBBY_AWAY_BIT;
        }
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
