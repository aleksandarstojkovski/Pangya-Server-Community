package org.pangya.game.catalog;

import org.pangya.protocol.game.ExpLevelTable;
import org.pangya.protocol.game.GamePackets;

/** C# {@code requestFinishExpGame} per game mode. */
public final class ExpFinishCalculator {

    private ExpFinishCalculator() {}

    /**
     * C# hole_seq gate: sequence 1 requires {@code acerto_hole} unless on last hole.
     */
    public static int resolveHoleSeq(
            int holeSeq, int qntdHole, int displayState, boolean teamAcertoHole) {
        if (holeSeq == 1 && holeSeq != qntdHole) {
            boolean acerto = teamAcertoHole
                    || (displayState & GamePackets.DISPLAY_ACERTO_HOLE) != 0;
            if (!acerto) {
                return 0;
            }
        }
        return holeSeq;
    }

    /** C# {@code Practice.requestFinishExpGame}: always 0 in JP reference. */
    public static int practiceFinishExp() {
        return 0;
    }

    /** C# {@code Versus.requestFinishExpGame}. */
    public static int versusFinishExp(
            int playerCount,
            int holeSeq,
            float stars,
            int playerRateExp,
            int serverRateExp,
            int rankIndex) {
        if (playerCount <= 0 || holeSeq <= 0 || stars <= 0f) {
            return 0;
        }
        int exp = playerCount * holeSeq;
        exp = (int) (exp * stars);
        exp = (int) (exp
                * PangBonusCalculator.transfServerRate(playerRateExp)
                * PangBonusCalculator.transfServerRate(serverRateExp));
        exp = (int) (exp * (1.0f - rankIndex * 0.1f));
        return Math.max(0, exp);
    }

    /** C# {@code Match.requestFinishExpGame}. {@code teamFactor} is 1.0 or 0.6 when losing. */
    public static int matchFinishExp(
            int playerCount,
            int holeSeq,
            float stars,
            int playerRateExp,
            int serverRateExp,
            float teamFactor) {
        if (playerCount <= 0 || holeSeq <= 0 || stars <= 0f) {
            return 0;
        }
        int exp = playerCount * holeSeq;
        exp = (int) (exp * stars);
        exp = (int) (exp
                * PangBonusCalculator.transfServerRate(playerRateExp)
                * PangBonusCalculator.transfServerRate(serverRateExp));
        exp = (int) (exp * teamFactor);
        return Math.max(0, exp);
    }

    /** C# {@code Tourney.requestFinishExpGame} rank divisor uses integer {@code i / count}. */
    public static int tourneyFinishExp(
            int playerCount,
            int holeSeq,
            float stars,
            int playerRateExp,
            int serverRateExp,
            int rankIndex) {
        if (playerCount <= 0 || holeSeq <= 0 || stars <= 0f) {
            return 0;
        }
        int exp = playerCount * holeSeq;
        exp = (int) (exp * stars);
        exp = (int) (exp
                * PangBonusCalculator.transfServerRate(playerRateExp)
                * PangBonusCalculator.transfServerRate(serverRateExp));
        if (playerCount > 0) {
            exp = (int) (exp * (1 - (rankIndex / (float) playerCount)));
        }
        return Math.max(0, exp);
    }

    public static int finishExpForRoom(
            int roomTipo,
            int playerCount,
            int holeSeq,
            float stars,
            int playerRateExp,
            int serverRateExp,
            int rankIndex,
            float matchTeamFactor) {
        if (roomTipo == GamePackets.TIPO_PRACTICE) {
            return practiceFinishExp();
        }
        if (roomTipo == GamePackets.TIPO_MATCH) {
            return matchFinishExp(
                    playerCount, holeSeq, stars, playerRateExp, serverRateExp, matchTeamFactor);
        }
        if (GamePackets.usesTourneyInitialData(roomTipo)) {
            return tourneyFinishExp(
                    playerCount, holeSeq, stars, playerRateExp, serverRateExp, rankIndex);
        }
        if (GamePackets.usesVersusInitialData(roomTipo)) {
            return versusFinishExp(
                    playerCount, holeSeq, stars, playerRateExp, serverRateExp, rankIndex);
        }
        return 0;
    }

    public static boolean canAwardFinishExp(int level) {
        return level < ExpLevelTable.MAX_AWARD_LEVEL;
    }
}
