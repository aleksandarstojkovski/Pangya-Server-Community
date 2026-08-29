package org.pangya.protocol.iff;

import java.util.Map;
import java.util.Optional;

/** C# {@code sIff.findCaddie}. */
public record IffCaddieIndex(Map<Integer, IffCaddieRecord> byTypeid) {

    public Optional<IffCaddieRecord> find(int typeid) {
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

    public static IffCaddieIndex empty() {
        return new IffCaddieIndex(Map.of());
    }
}
