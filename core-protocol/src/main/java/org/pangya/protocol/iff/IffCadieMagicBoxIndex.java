package org.pangya.protocol.iff;

import java.util.Map;
import java.util.Optional;

/** C# {@code sIff.findCadieMagicBox}. */
public record IffCadieMagicBoxIndex(Map<Integer, IffCadieMagicBoxRecord> bySeq) {

    public Optional<IffCadieMagicBoxRecord> find(int seq) {
        return Optional.ofNullable(bySeq.get(seq));
    }

    public boolean isEmpty() {
        return bySeq.isEmpty();
    }

    public int size() {
        return bySeq.size();
    }

    public static IffCadieMagicBoxIndex empty() {
        return new IffCadieMagicBoxIndex(Map.of());
    }
}
