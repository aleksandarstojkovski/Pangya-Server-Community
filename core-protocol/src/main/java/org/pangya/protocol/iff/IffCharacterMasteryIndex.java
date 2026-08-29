package org.pangya.protocol.iff;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** C# {@code sIff.findCharacterMastery}: rows per character typeid ordered by {@code seq}. */
public record IffCharacterMasteryIndex(Map<Integer, List<IffCharacterMasteryRecord>> byTypeid) {

    public Optional<List<IffCharacterMasteryRecord>> find(int typeid) {
        List<IffCharacterMasteryRecord> rows = byTypeid.get(typeid);
        return rows == null || rows.isEmpty() ? Optional.empty() : Optional.of(rows);
    }

    public boolean isEmpty() {
        return byTypeid.isEmpty();
    }

    public int characterCount() {
        return byTypeid.size();
    }

    public int rowCount() {
        return byTypeid.values().stream().mapToInt(List::size).sum();
    }

    public static IffCharacterMasteryIndex empty() {
        return new IffCharacterMasteryIndex(Map.of());
    }
}
