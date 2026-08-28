package org.pangya.protocol.iff;

import java.util.Set;

/** C# {@code sIff.findPart}: typeid membership from {@code Part.iff}. */
public record IffPartIndex(Set<Integer> typeids) {

    public boolean contains(int typeid) {
        return typeids.contains(typeid);
    }

    public boolean isEmpty() {
        return typeids.isEmpty();
    }

    public int size() {
        return typeids.size();
    }

    public static IffPartIndex empty() {
        return new IffPartIndex(Set.of());
    }
}
