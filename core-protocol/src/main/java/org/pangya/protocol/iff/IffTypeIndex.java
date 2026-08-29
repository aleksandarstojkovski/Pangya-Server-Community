package org.pangya.protocol.iff;

import java.util.Set;

/** C# {@code findItem}/{@code findCard}: typeid membership from an IFF dataset. */
public record IffTypeIndex(Set<Integer> typeids) {

    public boolean contains(int typeid) {
        return typeids.contains(typeid);
    }

    public boolean isEmpty() {
        return typeids.isEmpty();
    }

    public int size() {
        return typeids.size();
    }

    public static IffTypeIndex empty() {
        return new IffTypeIndex(Set.of());
    }
}
