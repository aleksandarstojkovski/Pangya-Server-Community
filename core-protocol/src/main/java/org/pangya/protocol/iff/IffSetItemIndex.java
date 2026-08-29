package org.pangya.protocol.iff;

import java.util.Map;
import java.util.Optional;

public final class IffSetItemIndex {

    private final Map<Integer, IffSetItemRecord> byTypeid;

    IffSetItemIndex(Map<Integer, IffSetItemRecord> byTypeid) {
        this.byTypeid = byTypeid;
    }

    static IffSetItemIndex empty() {
        return new IffSetItemIndex(Map.of());
    }

    public boolean isEmpty() {
        return byTypeid.isEmpty();
    }

    public int size() {
        return byTypeid.size();
    }

    public Optional<IffSetItemRecord> find(int typeid) {
        return Optional.ofNullable(byTypeid.get(typeid));
    }
}
