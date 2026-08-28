package org.pangya.protocol.iff;

import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;

/** C# {@code sIff.findPart}: typeid membership + optional {@code valor_rental}. */
public record IffPartIndex(Set<Integer> typeids, Map<Integer, Long> valorRentalByTypeid) {

    public boolean contains(int typeid) {
        return typeids.contains(typeid);
    }

    public boolean isEmpty() {
        return typeids.isEmpty();
    }

    public int size() {
        return typeids.size();
    }

    public OptionalLong valorRental(int typeid) {
        Long value = valorRentalByTypeid.get(typeid);
        if (value == null || value <= 0) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(value);
    }

    public static IffPartIndex empty() {
        return new IffPartIndex(Set.of(), Map.of());
    }
}
