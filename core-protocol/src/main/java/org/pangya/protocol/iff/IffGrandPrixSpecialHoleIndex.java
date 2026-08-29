package org.pangya.protocol.iff;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class IffGrandPrixSpecialHoleIndex {

    private final Map<Integer, List<IffGrandPrixSpecialHoleRecord>> byRankTypeid;
    private final int rowCount;

    IffGrandPrixSpecialHoleIndex(Map<Integer, List<IffGrandPrixSpecialHoleRecord>> byRankTypeid, int rowCount) {
        this.byRankTypeid = byRankTypeid;
        this.rowCount = rowCount;
    }

    static IffGrandPrixSpecialHoleIndex empty() {
        return new IffGrandPrixSpecialHoleIndex(Map.of(), 0);
    }

    public boolean isEmpty() {
        return byRankTypeid.isEmpty();
    }

    public int rowCount() {
        return rowCount;
    }

    /** C# {@code findGrandPrixSpecialHole}: sorted by {@code Hole} ascending. */
    public List<IffGrandPrixSpecialHoleRecord> find(int rankTypeid) {
        List<IffGrandPrixSpecialHoleRecord> rows = byRankTypeid.get(rankTypeid);
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<IffGrandPrixSpecialHoleRecord> sorted = new ArrayList<>(rows);
        sorted.sort(Comparator.comparingInt(IffGrandPrixSpecialHoleRecord::hole));
        return List.copyOf(sorted);
    }
}
