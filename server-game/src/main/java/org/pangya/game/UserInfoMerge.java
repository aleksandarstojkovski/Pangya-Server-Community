package org.pangya.game;

import org.pangya.db.InventoryRepository.UserInfoRow;
import org.pangya.protocol.game.GamePackets;

/** C# {@code UserInfo.add} + {@code GameBase.requestSaveInfo} overlay for DB persistence. */
final class UserInfoMerge {

    /** C# {@code DefineConstants.DECREASE_COMBO_VALUE} (JP). */
    static final int DECREASE_COMBO_VALUE = 3;

    private UserInfoMerge() {}

    /**
     * C# {@code requestSaveInfo}: swap hole_in/mad_conduta, apply option overlay, merge into DB row.
     */
    static UserInfoRow saveInfo(
            UserInfoRow db,
            GamePackets.UserInfoEx client,
            int option,
            int gameScore,
            int gameSeconds,
            boolean connectionTimeout) {
        GamePackets.UserInfoEx swapped = swapHoleInMadConduta(client);
        MutableDelta delta = MutableDelta.fromClient(swapped);
        applySaveInfoOption(delta, option, gameScore, gameSeconds, connectionTimeout);
        return merge(db, delta);
    }

    /** C# wire swap before DB save. */
    static GamePackets.UserInfoEx swapHoleInMadConduta(GamePackets.UserInfoEx ui) {
        return new GamePackets.UserInfoEx(
                ui.tacada(),
                ui.putt(),
                ui.tempo(),
                ui.tempoTacada(),
                ui.bestDrive(),
                ui.acertoPangya(),
                ui.timeout(),
                ui.ob(),
                ui.totalDistancia(),
                ui.hole(),
                ui.madConduta(),
                ui.hio(),
                ui.bunker(),
                ui.fairway(),
                ui.albatross(),
                ui.holeIn(),
                ui.puttIn(),
                ui.bestLongPutt(),
                ui.bestChipIn(),
                ui.exp(),
                ui.level(),
                ui.pang(),
                ui.mediaScore(),
                ui.combo());
    }

    static void applySaveInfoOption(
            MutableDelta delta,
            int option,
            int gameScore,
            int gameSeconds,
            boolean connectionTimeout) {
        if (option == 0) {
            delta.exp = 0;
            delta.combo = 1;
            delta.jogado = 1;
            delta.jogadosDisconnect = 1;
            delta.mediaScore = gameScore;
            delta.tempo = gameSeconds;
            return;
        }
        if (option == 1) {
            delta.exp = 0;
            delta.combo = -DECREASE_COMBO_VALUE;
            delta.jogado = 1;
            delta.jogadosDisconnect = 1;
            delta.mediaScore = gameScore;
            delta.tempo = gameSeconds;
            if (connectionTimeout) {
                delta.disconnect = 1;
            } else {
                delta.quitado = 1;
            }
            return;
        }
        if (option == 2) {
            delta.exp = 0;
            delta.jogado = 1;
            delta.jogadosDisconnect = 1;
            delta.tempo = gameSeconds;
            return;
        }
        if (option == 5) {
            delta.exp = 0;
            delta.jogado = 1;
            delta.jogadosDisconnect = 1;
            delta.mediaScore = gameScore;
            delta.tempo = gameSeconds;
        }
    }

    static UserInfoRow merge(UserInfoRow db, MutableDelta delta) {
        float maxDistancia = Math.max(db.maxDistancia(), delta.bestDrive);
        float longPutt = Math.max(db.longPutt(), delta.bestLongPutt);
        float chipIn = Math.max(db.chipIn(), delta.bestChipIn);

        long combos = mergeCombo(db.combos(), delta.combo);
        long todosCombos = db.todosCombos();
        if (delta.combo > 0 && combos > todosCombos) {
            todosCombos += delta.combo;
        }

        long quitado = db.quitado();
        if (delta.quitado < 0) {
            quitado = Math.max(0, quitado + delta.quitado);
        } else {
            quitado += delta.quitado;
        }

        int skinAllin = db.skinAllinCount();
        long skinPang = db.skinPang();
        if (skinAllin + delta.skinAllinCount >= 5) {
            skinAllin = 0;
            skinPang += 1000;
        } else {
            skinAllin += delta.skinAllinCount;
        }

        int holeInDelta = delta.hole - delta.holeInDisplay;

        return new UserInfoRow(
                db.tacadas() + delta.tacada,
                db.putt() + delta.putt,
                db.tempo() + delta.tempo,
                db.tempoTacadas() + delta.tempoTacada,
                maxDistancia,
                db.acertoPangya() + delta.acertoPangya,
                db.bunker() + delta.bunker,
                db.ob() + delta.ob,
                db.totalDistancia() + delta.totalDistancia,
                db.holes() + delta.hole,
                db.holeIn() + holeInDelta,
                db.hio() + delta.hio,
                db.timeout() + delta.timeout,
                db.fairway() + delta.fairway,
                db.albatross() + delta.albatross,
                db.maConduta() + delta.madConduta,
                db.acertoPutt() + delta.puttIn,
                longPutt,
                chipIn,
                db.xp(),
                db.level(),
                db.pang(),
                db.mediaScore() + delta.mediaScore,
                db.bestScore0(),
                db.bestScore1(),
                db.bestScore2(),
                db.bestScore3(),
                db.bestScore4(),
                db.maxPang0(),
                db.maxPang1(),
                db.maxPang2(),
                db.maxPang3(),
                db.maxPang4(),
                db.sumPang(),
                db.eventFlag(),
                db.jogado() + delta.jogado,
                quitado,
                skinPang,
                db.skinWin(),
                db.skinLose(),
                db.skinRunHole(),
                db.skinStrikePoint(),
                skinAllin,
                todosCombos,
                combos,
                db.teamWin(),
                db.teamGames(),
                db.teamHole(),
                db.ladderPoint(),
                db.ladderWin(),
                db.ladderLose(),
                db.ladderDraw(),
                db.ladderHole(),
                db.eventValue(),
                db.naoSei() + delta.disconnect,
                db.maxJogoNaoSei(),
                db.jogosNaoSei() + delta.jogadosDisconnect,
                db.gameCountSeason(),
                db.cookie(),
                db.totalPangWinGame(),
                db.luckyMedal(),
                db.fastMedal(),
                db.bestDriveMedal(),
                db.bestChipinMedal(),
                db.bestPuttinMedal(),
                db.bestRecoveryMedal(),
                db.bit16NaoSei());
    }

    static long mergeCombo(long current, int delta) {
        if (delta < 0) {
            if (current <= DECREASE_COMBO_VALUE) {
                return 0;
            }
            return current + delta;
        }
        return current + delta;
    }

    /** Mutable game-stat delta mirroring C# {@code UserInfoEx} merge fields. */
    static final class MutableDelta {
        int tacada;
        int putt;
        int tempo;
        int tempoTacada;
        float bestDrive;
        int acertoPangya;
        int timeout;
        int ob;
        int totalDistancia;
        int hole;
        /** Wire {@code hole_in} after swap (unfinished holes). */
        int holeInDisplay;
        int hio;
        int bunker;
        int fairway;
        int albatross;
        int madConduta;
        int puttIn;
        float bestLongPutt;
        float bestChipIn;
        int exp;
        int mediaScore;
        int combo;
        int jogado;
        int quitado;
        int disconnect;
        int jogadosDisconnect;
        int skinAllinCount;

        static MutableDelta fromClient(GamePackets.UserInfoEx ui) {
            MutableDelta delta = new MutableDelta();
            delta.tacada = ui.tacada();
            delta.putt = ui.putt();
            delta.tempo = ui.tempo();
            delta.tempoTacada = ui.tempoTacada();
            delta.bestDrive = ui.bestDrive();
            delta.acertoPangya = ui.acertoPangya();
            delta.timeout = ui.timeout();
            delta.ob = ui.ob();
            delta.totalDistancia = ui.totalDistancia();
            delta.hole = ui.hole();
            delta.holeInDisplay = ui.holeIn();
            delta.hio = ui.hio();
            delta.bunker = ui.bunker();
            delta.fairway = ui.fairway();
            delta.albatross = ui.albatross();
            delta.madConduta = ui.madConduta();
            delta.puttIn = ui.puttIn();
            delta.bestLongPutt = ui.bestLongPutt();
            delta.bestChipIn = ui.bestChipIn();
            delta.exp = ui.exp();
            delta.mediaScore = ui.mediaScore();
            delta.combo = ui.combo();
            return delta;
        }
    }
}
