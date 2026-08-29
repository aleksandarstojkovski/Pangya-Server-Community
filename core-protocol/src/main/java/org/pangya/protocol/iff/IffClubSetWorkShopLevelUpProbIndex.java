package org.pangya.protocol.iff;

import java.util.Map;
import java.util.Optional;

/** C# {@code sIff.findClubSetWorkShopLevelUpProb}. */
public record IffClubSetWorkShopLevelUpProbIndex(Map<Integer, IffClubSetWorkShopLevelUpProbRecord> byTipo) {

    public Optional<int[]> prob(int tipo) {
        IffClubSetWorkShopLevelUpProbRecord row = byTipo.get(tipo);
        return row == null ? Optional.empty() : Optional.of(row.c().clone());
    }

    public boolean isEmpty() {
        return byTipo.isEmpty();
    }

    public int size() {
        return byTipo.size();
    }

    public static IffClubSetWorkShopLevelUpProbIndex empty() {
        return new IffClubSetWorkShopLevelUpProbIndex(Map.of());
    }
}
