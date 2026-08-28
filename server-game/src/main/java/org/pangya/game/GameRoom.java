package org.pangya.game;

import org.pangya.network.session.Session;
import org.pangya.protocol.game.GamePackets;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory C# {@code Room} subset: {@code RoomInfoEx} + start-game + Practice hole state. */
final class GameRoom {

    private static final SecureRandom RNG = new SecureRandom();

    final GamePackets.RoomInfo info = new GamePackets.RoomInfo();
    final int tipo;
    final List<Session> players = new ArrayList<>();
    final ConcurrentHashMap<Integer, GamePackets.PlayerRoomInfo> playerInfos = new ConcurrentHashMap<>();
    final ConcurrentHashMap<Integer, PlayerShot> shots = new ConcurrentHashMap<>();
    volatile boolean inGame;
    volatile long startMillis;
    volatile GameCourse course;

    GameRoom(GamePackets.CreateRoom req, int numero, int masterUid, int ratePang, int rateExp) {
        this.tipo = req.tipo();
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
    }

    synchronized List<Session> snapshot() {
        return List.copyOf(players);
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
