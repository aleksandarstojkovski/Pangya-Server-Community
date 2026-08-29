package org.pangya.protocol.iff;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** C# {@code sIff.findClubSetWorkShopLevelUpLimit}. */
public record IffClubSetWorkShopLevelUpLimitIndex(Map<Integer, List<IffClubSetWorkShopLevelUpLimitRecord>> byTipo) {

    public Optional<short[]> limit(int tipo, int rank) {
        List<IffClubSetWorkShopLevelUpLimitRecord> rows = byTipo.get(tipo);
        if (rows == null) {
            return Optional.empty();
        }
        for (IffClubSetWorkShopLevelUpLimitRecord row : rows) {
            if (row.rank() == rank) {
                return Optional.of(row.c().clone());
            }
        }
        return Optional.empty();
    }

    public boolean hasTipo(int tipo) {
        List<IffClubSetWorkShopLevelUpLimitRecord> rows = byTipo.get(tipo);
        return rows != null && !rows.isEmpty();
    }

    public boolean isEmpty() {
        return byTipo.isEmpty();
    }

    public int rowCount() {
        return byTipo.values().stream().mapToInt(List::size).sum();
    }

    public static IffClubSetWorkShopLevelUpLimitIndex empty() {
        return new IffClubSetWorkShopLevelUpLimitIndex(Map.of());
    }
}
