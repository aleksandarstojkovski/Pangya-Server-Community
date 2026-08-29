package org.pangya.protocol.iff;

import java.util.Map;
import java.util.Optional;

public final class IffGrandPrixDataIndex {

    private final Map<Integer, IffGrandPrixDataRecord> byTypeid;

    IffGrandPrixDataIndex(Map<Integer, IffGrandPrixDataRecord> byTypeid) {
        this.byTypeid = byTypeid;
    }

    static IffGrandPrixDataIndex empty() {
        return new IffGrandPrixDataIndex(Map.of());
    }

    public boolean isEmpty() {
        return byTypeid.isEmpty();
    }

    public int size() {
        return byTypeid.size();
    }

    public Optional<IffGrandPrixDataRecord> find(int typeid) {
        return Optional.ofNullable(byTypeid.get(typeid));
    }
}
