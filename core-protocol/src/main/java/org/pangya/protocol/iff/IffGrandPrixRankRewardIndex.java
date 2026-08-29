package org.pangya.protocol.iff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** C# {@code findGrandPrixRankReward(TypeID_Link)} rows sorted by rank. */
public final class IffGrandPrixRankRewardIndex {

    private final Map<Integer, List<IffGrandPrixRankRewardRecord>> byTypeIdLink;
    private final int rowCount;

    IffGrandPrixRankRewardIndex(Map<Integer, List<IffGrandPrixRankRewardRecord>> byTypeIdLink, int rowCount) {
        this.byTypeIdLink = byTypeIdLink;
        this.rowCount = rowCount;
    }

    static IffGrandPrixRankRewardIndex empty() {
        return new IffGrandPrixRankRewardIndex(Map.of(), 0);
    }

    public int rowCount() {
        return rowCount;
    }

    public List<IffGrandPrixRankRewardRecord> find(int typeIdLink) {
        List<IffGrandPrixRankRewardRecord> rows = byTypeIdLink.get(typeIdLink);
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return Collections.unmodifiableList(rows);
    }

    public Optional<IffGrandPrixRankRewardRecord> findRank(int typeIdLink, int rank) {
        return find(typeIdLink).stream().filter(row -> row.rank() == rank).findFirst();
    }
}
