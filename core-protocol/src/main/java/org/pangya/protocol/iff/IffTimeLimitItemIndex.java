package org.pangya.protocol.iff;

import java.util.Map;
import java.util.Optional;

/** C# {@code sIff.findTimeLimitItem}. */
public record IffTimeLimitItemIndex(Map<Integer, IffTimeLimitItemRecord> byTypeid) {

    public Optional<IffTimeLimitItemRecord> find(int typeid) {
        return Optional.ofNullable(byTypeid.get(typeid));
    }

    public boolean isEmpty() {
        return byTypeid.isEmpty();
    }

    public int size() {
        return byTypeid.size();
    }

    public static IffTimeLimitItemIndex empty() {
        return new IffTimeLimitItemIndex(Map.of());
    }
}
