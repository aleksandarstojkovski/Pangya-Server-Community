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
}
