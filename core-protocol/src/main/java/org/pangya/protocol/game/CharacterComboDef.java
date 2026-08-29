package org.pangya.protocol.game;

import java.util.Arrays;
import java.util.function.IntPredicate;

/**
 * C# {@code CharacterInfo.initComboDef}: default part typeids from character
 * identify + slot ({@code Pangya_St.cs} / {@code IFFHandle}).
 */
public final class CharacterComboDef {

    /** C# default-part marker {@code 0x8000400}. */
    public static final int PART_BASE = 0x8000400;

    private CharacterComboDef() {}

    /** C# {@code (((_typeid << 5) | slot) << 13) | 0x8000400} on uint32. */
    public static int partTypeid(int characterTypeid, int slot) {
        long id = characterTypeid & 0xffff_ffffL;
        long v = ((id << 5) | (slot & 0xffL)) & 0xffff_ffffL;
        return (int) (((v << 13) | PART_BASE) & 0xffff_ffffL);
    }

    /**
     * Clears and fills {@code partsTypeid} for slots whose part exists in IFF
     * (Java stand-in: {@code pangya.iff_part} row present).
     */
    public static void apply(int characterTypeid, int[] partsTypeid, IntPredicate partExists) {
        if (characterTypeid == 0 || partsTypeid == null || partsTypeid.length < 24) {
            return;
        }
        Arrays.fill(partsTypeid, 0);
        for (int slot = 0; slot < 24; slot++) {
            int typeid = partTypeid(characterTypeid, slot);
            if (partExists.test(typeid)) {
                partsTypeid[slot] = typeid;
            }
        }
    }
}
