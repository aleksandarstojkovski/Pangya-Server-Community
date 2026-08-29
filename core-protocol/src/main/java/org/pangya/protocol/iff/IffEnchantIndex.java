package org.pangya.protocol.iff;

import java.util.Map;
import java.util.OptionalLong;

/** C# {@code sIff.findEnchant}: typeid → pang cost. */
public record IffEnchantIndex(Map<Integer, IffEnchantRecord> byTypeid) {

    public OptionalLong pang(int typeid) {
        IffEnchantRecord row = byTypeid.get(typeid);
        if (row == null) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(row.pang());
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

    public static IffEnchantIndex empty() {
        return new IffEnchantIndex(Map.of());
    }
}
