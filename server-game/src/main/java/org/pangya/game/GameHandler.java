package org.pangya.game;

import org.pangya.db.InventoryRepository;
import org.pangya.db.LoginRepository;
import org.pangya.network.AppConfig;
import org.pangya.network.redis.SessionKeyStore;
import org.pangya.network.session.PlayerContext;
import org.pangya.network.session.Session;
import org.pangya.network.session.SessionManager;
import org.pangya.protocol.game.GamePackets;
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

/**
 * JP {@code GameServer.requestLogin} + channel enter + {@code Channel.requestMakeRoom}
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
            case GamePackets.CLIENT_REQUEST_JOIN_ROOM -> joinRoom(session, reader);
            case GamePackets.CLIENT_REQUEST_START_GAME -> startGame(session);
            case GamePackets.CLIENT_EXIT_ROOM -> leaveRoom(session);
            case GamePackets.CLIENT_LEAVE_PRACTICE -> leavePractice(session);
            case GamePackets.CLIENT_LOAD_OK -> finishLoadHole(session);
            case GamePackets.CLIENT_HOLE_INFO -> initHole(session, reader);
            case GamePackets.CLIENT_SHOT -> initShot(session);
            case GamePackets.CLIENT_SHOT_RESULT -> syncShot(session, reader);
            case GamePackets.CLIENT_SHOT_ACK -> finishShot(session);
            case GamePackets.CLIENT_REQUEST_EQUIP_ITEM -> equipItem(session, reader);
            case GamePackets.CLIENT_REQUEST_BUY_ITEM -> buyItem(session, reader);
            case GamePackets.CLIENT_LOUNGE_STATE -> loungeState(session);
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
        GameRoom created = new GameRoom(room, number, (int) pi.uid, config.ratePang(), config.rateExp());
        created.addPlayer(session);
        created.putPlayerInfo(session, makePlayerInfo(session, created));
        rooms.put(number, created);
        pi.roomNumber = number;
        pi.inPractice = room.tipo() == GamePackets.TIPO_PRACTICE;
        sendRoomEnterPackets(session, created);
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
        room.putPlayerInfo(session, makePlayerInfo(session, room));
        sendRoomEnterPackets(session, room);
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
        if (room != null) {
            room.removePlayer(session);
            if (room.info.numPlayer <= 0 || room.info.master == (int) pi.uid) {
                rooms.remove(pi.roomNumber);
            }
        } else {
            rooms.remove(pi.roomNumber);
        }
        pi.inPractice = false;
        pi.roomNumber = -1;
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
            pri.stateFlag |= (1 << 3) | (1 << 9);
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
