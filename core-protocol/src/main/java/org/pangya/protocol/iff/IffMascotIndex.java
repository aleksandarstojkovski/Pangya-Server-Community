package org.pangya.protocol.iff;

import java.util.Map;
import java.util.Optional;

/** C# {@code sIff.findMascot}. */
public record IffMascotIndex(Map<Integer, IffMascotRecord> byTypeid) {

    public Optional<IffMascotRecord> find(int typeid) {
        return Optional.ofNullable(byTypeid.get(typeid));
    }

    public boolean contains(int typeid) {
        return byTypeid.containsKey(typeid);
    }

    public boolean isEmpty() {
        return byTypeid.isEmpty();
    }

    public int size() {
        return byTypeid.size();
    }

    public static IffMascotIndex empty() {
        return new IffMascotIndex(Map.of());
    }
}
