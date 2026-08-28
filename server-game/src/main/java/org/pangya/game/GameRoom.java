package org.pangya.game;

import org.pangya.protocol.game.GamePackets;

import java.security.SecureRandom;

/** In-memory C# {@code Room} subset: {@code RoomInfoEx} + start-game state. */
final class GameRoom {

    private static final SecureRandom RNG = new SecureRandom();

    final GamePackets.RoomInfo info = new GamePackets.RoomInfo();
    final int tipo;
    volatile boolean inGame;

    GameRoom(GamePackets.CreateRoom req, int numero, int masterUid, int ratePang, int rateExp) {
        this.tipo = req.tipo();
        info.numero = numero;
        info.maxPlayer = req.maxPlayer();
        info.numPlayer = 1;
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
}
