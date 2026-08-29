package org.pangya.protocol.iff;

import java.util.Map;
import java.util.Optional;

/** C# {@code sIff.findItem} with shop flags for delete/gift parity. */
public record IffItemIndex(Map<Integer, IffItemRecord> byTypeid) {

    public boolean contains(int typeid) {
        return byTypeid.containsKey(typeid);
    }

    public boolean isEmpty() {
        return byTypeid.isEmpty();
    }

    public int size() {
        return byTypeid.size();
    }

    public Optional<IffItemRecord> find(int typeid) {
        return Optional.ofNullable(byTypeid.get(typeid));
    }

    public boolean canDeleteActiveItem(int typeid) {
        IffItemRecord item = byTypeid.get(typeid);
        return item != null && item.canDeleteActiveItem();
    }

    public static IffItemIndex empty() {
        return new IffItemIndex(Map.of());
    }
}
