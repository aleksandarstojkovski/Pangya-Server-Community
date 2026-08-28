package org.pangya.game;

import org.pangya.network.session.Session;
import org.pangya.protocol.game.GamePackets;
import org.pangya.protocol.packet.PacketReader;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory C# {@code Room} subset: {@code RoomInfoEx} + start-game + Practice hole state. */
final class GameRoom {

    private static final SecureRandom RNG = new SecureRandom();

    final GamePackets.RoomInfo info = new GamePackets.RoomInfo();
    int tipo;
    final int channelId;
    final List<Session> players = new ArrayList<>();
    final ConcurrentHashMap<Integer, GamePackets.PlayerRoomInfo> playerInfos = new ConcurrentHashMap<>();
    final ConcurrentHashMap<Integer, PlayerShot> shots = new ConcurrentHashMap<>();
    volatile boolean inGame;
    volatile long startMillis;
    volatile GameCourse course;
    /** C# Versus {@code m_count_pause}; max {@link GamePackets#VERSUS_PAUSE_MAX}. */
    volatile int pauseCount;
    /** C# Versus {@code finish_char_intro}; cleared when all players have sent {@code 0x34}. */
    final ConcurrentHashMap<Integer, Boolean> charIntro = new ConcurrentHashMap<>();
    /** C# Versus {@code setLoadHole}; cleared when all players have sent {@code 0x11}. */
    final ConcurrentHashMap<Integer, Boolean> loadHole = new ConcurrentHashMap<>();
    /** C# Versus {@code m_player_turn.oid}; 0 until {@code sendReplyFinishLoadHole}. */
    volatile int turnOid;
    /** C# {@code m_player_report_game} UIDs that already sent {@code 0x3A}. */
    final ConcurrentHashMap<Long, Boolean> reported = new ConcurrentHashMap<>();
    /** C# {@code PersonalShopManager} per-owner shops. */
    final ConcurrentHashMap<Long, PersonalShop> shops = new ConcurrentHashMap<>();

    GameRoom(GamePackets.CreateRoom req, int numero, int masterUid, int ratePang, int rateExp, int channelId) {
        this.tipo = req.tipo();
        this.channelId = channelId;
        info.numero = numero;
        info.maxPlayer = req.maxPlayer();
        info.numPlayer = 0;
        info.holes = req.holes();
        info.course = req.course();
        info.modo = req.modo();
        info.timeVs = req.timeVs();
        info.time30s = req.time30s();
        info.natural = req.natural();
        info.artefato = req.artefato();
        info.master = masterUid;
        info.ratePang = ratePang;
        info.rateExp = rateExp;
        info.thirtyS = 30;
        info.state = 1;
        info.tipoShow = GamePackets.tipoShow(tipo);
        info.tipoEx = GamePackets.tipoEx(tipo);
        RNG.nextBytes(info.key);
        if (tipo == GamePackets.TIPO_PRACTICE) {
            info.name = "Single Player Practice Mode";
        } else {
            info.name = req.name() == null ? "" : req.name();
        }
        String password = req.password() == null ? "" : req.password();
        if (!password.isEmpty()) {
            info.password = password;
            info.senhaFlag = 0;
        } else {
            info.senhaFlag = 1;
        }
    }

    boolean hiddenFromLobby() {
        return GamePackets.hiddenFromLobby(tipo);
    }

    /**
     * C# {@code room.requestChangeInfoRoom}. Returns true when every change was applied.
     */
    synchronized boolean applyInfoChange(PacketReader reader) {
        if (reader.remaining() < 3) {
            return false;
        }
        reader.i16();
        int numInfo = reader.u8();
        if (numInfo <= 0) {
            return false;
        }
        for (int i = 0; i < numInfo; i++) {
            if (reader.remaining() < 1) {
                return false;
            }
            int type = reader.u8();
            switch (type) {
                case GamePackets.ROOM_CHANGE_NAME -> {
                    String title = reader.pstr();
                    if (hiddenFromLobby()) {
                        info.name = "Single Player Practice Mode";
                    } else {
                        info.name = title == null ? "" : title;
                    }
                }
                case GamePackets.ROOM_CHANGE_PASSWORD -> {
                    String pwd = reader.pstr();
                    if (pwd == null) {
                        pwd = "";
                    }
                    info.password = pwd;
                    info.senhaFlag = pwd.isEmpty() ? 1 : 0;
                }
                case GamePackets.ROOM_CHANGE_TIPO -> {
                    int next = reader.u8();
                    if (next >= 0 && next <= GamePackets.TIPO_MAX) {
                        tipo = next;
                        info.tipoShow = GamePackets.tipoShow(tipo);
                        info.tipoEx = GamePackets.tipoEx(tipo);
                    }
                }
                case GamePackets.ROOM_CHANGE_COURSE -> info.course = reader.u8();
                case GamePackets.ROOM_CHANGE_HOLES -> info.holes = reader.u8();
                case GamePackets.ROOM_CHANGE_MODO -> info.modo = reader.u8();
                case GamePackets.ROOM_CHANGE_TIME_VS -> {
                    int seconds = reader.u16();
                    if (seconds > 0) {
                        info.timeVs = seconds * 1000;
                    }
                }
                case GamePackets.ROOM_CHANGE_MAX_PLAYER -> {
                    int max = reader.u8();
                    if (max > players.size()) {
                        info.maxPlayer = max;
                    }
                }
                case GamePackets.ROOM_CHANGE_TIME_30S -> {
                    int minutes = reader.u8();
                    if (minutes > 0) {
                        info.time30s = minutes * 60_000;
                    }
                }
                case GamePackets.ROOM_CHANGE_STATE_FLAG -> info.stateFlag = reader.u8();
                case GamePackets.ROOM_CHANGE_GALLERY -> {
                    int gallery = reader.u8();
                    info.galleryNum = gallery;
                    info.thirtyS = gallery;
                }
                case GamePackets.ROOM_CHANGE_HOLE_REPEAT -> info.holeRepeat = reader.u8();
                case GamePackets.ROOM_CHANGE_FIXED_HOLE -> info.fixedHole = reader.u32();
                case GamePackets.ROOM_CHANGE_ARTEFATO -> info.artefato = reader.u32();
                case GamePackets.ROOM_CHANGE_NATURAL -> info.natural = reader.u32();
                default -> {
                    return false;
                }
            }
        }
        return true;
    }

    synchronized boolean addPlayer(Session session) {
        if (inGame) {
            return false;
        }
        if (info.numPlayer >= info.maxPlayer && info.maxPlayer > 0) {
            return false;
        }
        if (!players.contains(session)) {
            players.add(session);
            info.numPlayer = players.size();
        }
        return true;
    }

    synchronized void putPlayerInfo(Session session, GamePackets.PlayerRoomInfo info) {
        playerInfos.put(session.oid(), info);
    }

    GamePackets.PlayerRoomInfo playerInfo(Session session) {
        return playerInfos.get(session.oid());
    }

    synchronized List<GamePackets.PlayerRoomInfo> playerInfoSnapshot() {
        List<GamePackets.PlayerRoomInfo> out = new ArrayList<>();
        for (Session session : players) {
            GamePackets.PlayerRoomInfo info = playerInfos.get(session.oid());
            if (info != null) {
                out.add(info);
            }
        }
        return out;
    }

    synchronized void removePlayer(Session session) {
        players.remove(session);
        info.numPlayer = players.size();
        shots.remove(session.oid());
        playerInfos.remove(session.oid());
        charIntro.remove(session.oid());
        loadHole.remove(session.oid());
        shops.remove(session.player().uid);
        for (PersonalShop shop : shops.values()) {
            shop.viewers.remove(session.player().uid);
        }
    }

    /**
     * C# {@code room.updateMaster(null)} after the previous master left and
     * {@code m_pGame == null}. Prefers a GM ({@code capability & 4}).
     */
    synchronized Session electMaster() {
        if (players.isEmpty()) {
            return null;
        }
        Session master = null;
        for (Session session : players) {
            if ((session.player().capability & 4) != 0) {
                master = session;
                break;
            }
        }
        if (master == null) {
            master = players.getFirst();
        }
        info.master = (int) master.player().uid;
        info.stateFlag = (master.player().capability & 4) != 0
                ? GamePackets.ROOM_MASTER_GM_FLAG
                : 0;
        GamePackets.PlayerRoomInfo pri = playerInfos.get(master.oid());
        if (pri != null) {
            pri.stateFlag |= GamePackets.PLAYER_MASTER_BIT | GamePackets.PLAYER_READY_BIT;
            playerInfos.put(master.oid(), pri);
        }
        return master;
    }

    synchronized Session findByUid(long uid) {
        for (Session session : players) {
            if (session.player().uid == uid) {
                return session;
            }
        }
        return null;
    }

    synchronized List<Session> snapshot() {
        return List.copyOf(players);
    }

    /**
     * C# Versus {@code setFinishCharIntroAndCheckAllFinishCharIntroAndClear}.
     */
    synchronized boolean markCharIntro(Session session) {
        charIntro.put(session.oid(), Boolean.TRUE);
        return charIntro.size() >= players.size();
    }

    synchronized void clearCharIntro() {
        charIntro.clear();
    }

    /**
     * C# Versus {@code setLoadHole} then {@code checkAllLoadHole}.
     */
    synchronized boolean markLoadHole(Session session) {
        loadHole.put(session.oid(), Boolean.TRUE);
        return loadHole.size() >= players.size();
    }

    synchronized void clearLoadHole() {
        loadHole.clear();
    }

    /**
     * C# {@code init_turn_hole_start} then {@code getNextPlayerTurnHole}: join order
     * (equal hole-start scores keep {@code m_players} order).
     */
    synchronized int startHoleTurn() {
        if (players.isEmpty()) {
            turnOid = 0;
            return 0;
        }
        turnOid = players.getFirst().oid();
        return turnOid;
    }

    /**
     * C# {@code requestCalculePlayerTurn} after hole-start popped the first player.
     */
    synchronized int rotateTurn() {
        if (turnOid == 0 || players.isEmpty()) {
            return 0;
        }
        int idx = 0;
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).oid() == turnOid) {
                idx = i;
                break;
            }
        }
        turnOid = players.get((idx + 1) % players.size()).oid();
        return turnOid;
    }

    void broadcast(byte[] packet) {
        for (Session session : snapshot()) {
            session.send(packet);
        }
    }

    static final class PlayerShot {
        int hole;
        float x;
        float z;
        int shotState;
        int tempo;
    }

    static final class PersonalShop {
        final long ownerUid;
        final String ownerNick;
        volatile String name = "";
        volatile int visitCount;
        volatile long pangSale;
        final ConcurrentHashMap<Long, Boolean> viewers = new ConcurrentHashMap<>();

        PersonalShop(long ownerUid, String ownerNick) {
            this.ownerUid = ownerUid;
            this.ownerNick = ownerNick == null ? "" : ownerNick;
        }
    }

    static final class ShopReply {
        final boolean broadcast;
        final byte[] packet;

        ShopReply(boolean broadcast, byte[] packet) {
            this.broadcast = broadcast;
            this.packet = packet;
        }
    }

    ShopReply openEditShop(Session session) {
        long uid = session.player().uid;
        String nick = session.player().nickname;
        shops.computeIfAbsent(uid, k -> new PersonalShop(uid, nick));
        return new ShopReply(true, GamePackets.shopEditOk(nick, (int) uid));
    }

    ShopReply cancelEditShop(Session session) {
        PersonalShop shop = shops.get(session.player().uid);
        if (shop == null) {
            return new ShopReply(false, GamePackets.shopCancelFail(GamePackets.shopSys(GamePackets.SHOP_ERR_CANCEL_NONE)));
        }
        return new ShopReply(true, GamePackets.shopCancelOk(session.player().nickname));
    }

    ShopReply closeShop(Session session) {
        PersonalShop shop = shops.remove(session.player().uid);
        if (shop == null) {
            return new ShopReply(false, GamePackets.shopEditFail(GamePackets.shopSys(GamePackets.SHOP_ERR_CLOSE_NONE)));
        }
        return new ShopReply(true, GamePackets.shopCloseOk(session.player().nickname, (int) session.player().uid));
    }

    ShopReply changeShopName(Session session, String name) {
        if (name == null || name.isEmpty()) {
            return new ShopReply(false, GamePackets.shopNameFail(GamePackets.shopSys(GamePackets.SHOP_ERR_NAME_EMPTY)));
        }
        long uid = session.player().uid;
        for (PersonalShop other : shops.values()) {
            if (other.ownerUid != uid && name.equals(other.name)) {
                return new ShopReply(false, GamePackets.shopNameFail(GamePackets.shopSys(GamePackets.SHOP_ERR_NAME_DUP)));
            }
        }
        PersonalShop shop = shops.get(uid);
        if (shop == null) {
            return new ShopReply(false, GamePackets.shopNameFail(GamePackets.shopSys(GamePackets.SHOP_ERR_NAME_NONE)));
        }
        shop.name = name;
        return new ShopReply(true, GamePackets.shopNameOk(name, (int) uid, session.player().nickname));
    }

    byte[] visitCountShop(Session session) {
        PersonalShop shop = shops.get(session.player().uid);
        if (shop == null) {
            return GamePackets.shopVisitFail(GamePackets.shopSys(GamePackets.SHOP_ERR_VISIT_NONE));
        }
        return GamePackets.shopVisitOk(shop.visitCount);
    }

    byte[] pangShop(Session session) {
        PersonalShop shop = shops.get(session.player().uid);
        if (shop == null) {
            return GamePackets.shopPangFail(GamePackets.shopSys(GamePackets.SHOP_ERR_PANG_NONE));
        }
        return GamePackets.shopPangOk(shop.pangSale);
    }

    byte[] viewShop(Session session, long ownerUid) {
        PersonalShop shop = shops.get(ownerUid);
        if (shop == null) {
            return GamePackets.shopViewFail(GamePackets.shopSys(GamePackets.SHOP_ERR_VIEW_NONE));
        }
        return GamePackets.shopViewFail(GamePackets.SHOP_ERR_VIEW_DEFAULT);
    }

    byte[] closeViewShop(Session session, long ownerUid) {
        PersonalShop shop = shops.get(ownerUid);
        if (shop == null) {
            return GamePackets.shopCloseViewFail(GamePackets.shopSys(GamePackets.SHOP_ERR_CLOSE_VIEW_NONE));
        }
        if (shop.viewers.remove(session.player().uid) == null) {
            return GamePackets.shopCloseViewFail(GamePackets.SHOP_ERR_CLOSE_VIEW_DEFAULT);
        }
        return GamePackets.shopCloseViewOk();
    }
}
