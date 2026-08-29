package org.pangya.protocol.iff;

import java.util.Map;
import java.util.Optional;

public record IffAuxPartIndex(Map<Integer, IffAuxPartRecord> byTypeid) {

    public Optional<IffAuxPartRecord> find(int typeid) {
        return Optional.ofNullable(byTypeid.get(typeid));
    }

    public int size() {
        return byTypeid.size();
    }

    static IffAuxPartIndex empty() {
        return new IffAuxPartIndex(Map.of());
    }
}
