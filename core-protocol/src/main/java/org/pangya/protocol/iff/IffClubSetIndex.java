package org.pangya.protocol.iff;

import java.util.Map;
import java.util.Optional;

/** C# {@code sIff.findClubSet}: typeid → workshop stats/slots. */
public record IffClubSetIndex(Map<Integer, IffClubSetRecord> byTypeid) {

    public Optional<IffClubSetRecord> find(int typeid) {
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

    public static IffClubSetIndex empty() {
        return new IffClubSetIndex(Map.of());
    }
}
