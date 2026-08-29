package org.pangya.protocol.iff;

import java.util.Map;
import java.util.Optional;

/** C# {@code sIff.findClubSetWorkShopRankExp}. */
public record IffClubSetWorkShopRankUpExpIndex(Map<Integer, IffClubSetWorkShopRankUpExpRecord> byTipo) {

    public boolean contains(int tipo) {
        return byTipo.containsKey(tipo);
    }

    public Optional<int[]> ranks(int tipo) {
        IffClubSetWorkShopRankUpExpRecord row = byTipo.get(tipo);
        return row == null ? Optional.empty() : Optional.of(row.rank().clone());
    }

    public boolean isEmpty() {
        return byTipo.isEmpty();
    }

    public int size() {
        return byTipo.size();
    }

    public static IffClubSetWorkShopRankUpExpIndex empty() {
        return new IffClubSetWorkShopRankUpExpIndex(Map.of());
    }
}
