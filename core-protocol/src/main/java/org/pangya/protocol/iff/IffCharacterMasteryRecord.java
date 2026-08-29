package org.pangya.protocol.iff;

/** C# {@code CharacterMastery} row ({@code CharacterMastery.iff}, 60 bytes). */
public record IffCharacterMasteryRecord(
        int typeid,
        int seq,
        int stats,
        int level,
        int[] conditionTypeid,
        int[] conditionQntd) {

    public static final int CONDITION_SLOTS = 5;

    public IffCharacterMasteryRecord {
        if (conditionTypeid.length != CONDITION_SLOTS || conditionQntd.length != CONDITION_SLOTS) {
            throw new IllegalArgumentException("condition arrays must have " + CONDITION_SLOTS + " slots");
        }
    }
}
