package org.pangya.protocol.iff;

import java.util.Map;
import java.util.Optional;

/** C# {@code sIff.findCutinInfomation}. */
public record IffCutinInformationIndex(Map<Integer, IffCutinInformationRecord> byTypeid) {

    public Optional<IffCutinInformationRecord> find(int typeid) {
        return Optional.ofNullable(byTypeid.get(typeid));
    }

    public boolean isEmpty() {
        return byTypeid.isEmpty();
    }

    public int size() {
        return byTypeid.size();
    }

    public static IffCutinInformationIndex empty() {
        return new IffCutinInformationIndex(Map.of());
    }
}
