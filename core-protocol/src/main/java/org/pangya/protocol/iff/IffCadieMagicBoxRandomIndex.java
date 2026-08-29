package org.pangya.protocol.iff;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/** C# {@code sIff.findCadieMagicBoxRandom} pool (multiple rows share {@code ID}). */
public record IffCadieMagicBoxRandomIndex(Map<Integer, List<IffCadieMagicBoxRandomRecord>> byGroupId) {

    public List<IffCadieMagicBoxRandomRecord> find(int groupId) {
        return byGroupId.getOrDefault(groupId, List.of());
    }

    public Optional<IffCadieMagicBoxRandomRecord> spin(int groupId) {
        List<IffCadieMagicBoxRandomRecord> pool = find(groupId);
        if (pool.isEmpty()) {
            return Optional.empty();
        }
        long total = 0;
        for (IffCadieMagicBoxRandomRecord row : pool) {
            total += Math.max(0, row.rate());
        }
        if (total <= 0) {
            return Optional.of(pool.getFirst());
        }
        long roll = ThreadLocalRandom.current().nextLong(total);
        long acc = 0;
        for (IffCadieMagicBoxRandomRecord row : pool) {
            acc += Math.max(0, row.rate());
            if (roll < acc) {
                return Optional.of(row);
            }
        }
        return Optional.of(pool.getLast());
    }

    public boolean isEmpty() {
        return byGroupId.isEmpty();
    }

    public int rowCount() {
        return byGroupId.values().stream().mapToInt(List::size).sum();
    }

    public static IffCadieMagicBoxRandomIndex empty() {
        return new IffCadieMagicBoxRandomIndex(Map.of());
    }
}
