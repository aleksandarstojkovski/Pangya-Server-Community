package org.pangya.protocol.iff;

import java.util.Map;
import java.util.Optional;

public final class IffGrandPrixConditionEquipIndex {

    private final Map<Integer, IffGrandPrixConditionEquipRecord> byTypeidLink;

    IffGrandPrixConditionEquipIndex(Map<Integer, IffGrandPrixConditionEquipRecord> byTypeidLink) {
        this.byTypeidLink = byTypeidLink;
    }

    static IffGrandPrixConditionEquipIndex empty() {
        return new IffGrandPrixConditionEquipIndex(Map.of());
    }

    public boolean isEmpty() {
        return byTypeidLink.isEmpty();
    }

    public int size() {
        return byTypeidLink.size();
    }

    /** C# {@code findGrandPrixConditionEquip}: keyed by {@code TypeID_Link}. */
    public Optional<IffGrandPrixConditionEquipRecord> find(int typeidLink) {
        return Optional.ofNullable(byTypeidLink.get(typeidLink));
    }
}
