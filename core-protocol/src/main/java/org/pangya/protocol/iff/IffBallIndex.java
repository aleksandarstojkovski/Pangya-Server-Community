package org.pangya.protocol.iff;

import java.util.Map;
import java.util.OptionalInt;

/** C# {@code sIff.findBall}: stack size from {@code Stats.getSlot[0]}. */
public record IffBallIndex(Map<Integer, Integer> stackByTypeid) {

    public boolean isEmpty() {
        return stackByTypeid.isEmpty();
    }

    public int size() {
        return stackByTypeid.size();
    }

    public OptionalInt stackSize(int typeid) {
        Integer value = stackByTypeid.get(typeid);
        if (value == null) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(value);
    }

    public static IffBallIndex empty() {
        return new IffBallIndex(Map.of());
    }
}
