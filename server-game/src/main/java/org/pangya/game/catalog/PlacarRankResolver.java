package org.pangya.game.catalog;

import java.util.Comparator;
import java.util.List;

/** C# {@code requestCalculeRankPlace} + {@code sort_player_rank}. */
public final class PlacarRankResolver {

    private PlacarRankResolver() {}

    public record RankEntry(int oid, int score, long pang) {}

    /** Build rank order: lower score first, higher pang breaks ties. */
    public static List<RankEntry> sortRankEntries(List<RankEntry> entries) {
        return entries.stream()
                .sorted(Comparator.comparingInt(RankEntry::score)
                        .thenComparing(Comparator.comparingLong(RankEntry::pang).reversed()))
                .toList();
    }

    public static int rankIndex(List<RankEntry> sorted, int oid) {
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).oid() == oid) {
                return i;
            }
        }
        return 0;
    }

    /** C# {@code matchFinishExp} team factor: 1.0 for winners/draw, 0.6 for losing team. */
    public static float matchExpFactor(int teamWin, int teamId) {
        if (teamWin == 2 || teamWin == teamId) {
            return 1.0f;
        }
        return 0.6f;
    }

    /** C# {@code Match.requestCalculeTeamWin}: 0 red, 1 blue, 2 draw. */
    public static int calcMatchTeamWin(int redPoint, long redPang, int bluePoint, long bluePang) {
        if (redPoint == bluePoint) {
            if (redPang == bluePang) {
                return 2;
            }
            return bluePang > redPang ? 1 : 0;
        }
        return bluePoint > redPoint ? 1 : 0;
    }

    /** C# {@code Match.requestFinishTeamHole} hole point award at hole end. Returns new red/blue points. */
    public static int[] awardMatchHolePoint(
            int redPoint,
            int bluePoint,
            boolean redAcerto,
            boolean blueAcerto,
            int redStateFinish,
            int blueStateFinish,
            int redTacada,
            int blueTacada) {
        if (redAcerto && redStateFinish > 0 && blueStateFinish == 0 && redTacada < blueTacada + 1) {
            return new int[] {redPoint + 1, bluePoint};
        }
        if (blueAcerto && blueStateFinish > 0 && redStateFinish == 0 && blueTacada < redTacada + 1) {
            return new int[] {redPoint, bluePoint + 1};
        }
        return new int[] {redPoint, bluePoint};
    }

    /**
     * C# {@code Match.changeTurn} hole-end gate: all clear, odd players, chip-in win,
     * or point lead exceeds remaining holes.
     */
    public static boolean matchHoleEnds(
            boolean allTeamsCleared,
            int playerCount,
            boolean redAcerto,
            boolean blueAcerto,
            boolean redTimeout,
            boolean blueTimeout,
            int redTacada,
            int blueTacada,
            int redPoint,
            int bluePoint,
            int holeSeq,
            int qntdHole) {
        if (allTeamsCleared) {
            return true;
        }
        if ((playerCount % 2) == 1) {
            return true;
        }
        int holeDiff = qntdHole - holeSeq;
        if (redAcerto && !blueTimeout && blueTacada > 0 && redTacada < blueTacada + 1) {
            return true;
        }
        if (blueAcerto && !redTimeout && redTacada > 0 && blueTacada < redTacada + 1) {
            return true;
        }
        if (redAcerto && !blueTimeout && redTacada == blueTacada + 1 && redPoint - bluePoint > holeDiff) {
            return true;
        }
        return blueAcerto && !redTimeout && blueTacada == redTacada + 1 && bluePoint - redPoint > holeDiff;
    }

    /**
     * C# {@code Match.changeHole}: point lead exceeds remaining holes (early match end).
     */
    public static boolean matchGameEndsEarly(int redPoint, int bluePoint, int holeSeq, int qntdHole) {
        int holeDiff = qntdHole - holeSeq;
        return redPoint - bluePoint > holeDiff || bluePoint - redPoint > holeDiff;
    }

    /**
     * C# {@code Match.changeHole} + {@code checkEndGame}: point lead, last hole seq,
     * or odd player count.
     */
    public static boolean matchGameEnds(
            int redPoint, int bluePoint, int holeSeq, int qntdHole, int playerCount) {
        if (matchGameEndsEarly(redPoint, bluePoint, holeSeq, qntdHole)) {
            return true;
        }
        if (holeSeq >= qntdHole) {
            return true;
        }
        return (playerCount % 2) == 1;
    }
}
