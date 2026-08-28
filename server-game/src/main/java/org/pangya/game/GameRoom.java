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
    /** C# {@code m_player_report_game} UIDs that already sent {@code 0x3A}. */
    final ConcurrentHashMap<Long, Boolean> reported = new ConcurrentHashMap<>();

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
}
