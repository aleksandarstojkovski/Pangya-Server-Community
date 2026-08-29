package org.pangya.protocol.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterComboDefTest {

    @Test
    void nuriSlotZeroMatchesCSharpFormula() {
        assertEquals(134218752, CharacterComboDef.partTypeid(GamePackets.TYPEID_NURI, 0));
    }

    @Test
    void applyFillsSlotsWhenPartExists() {
        int[] parts = new int[24];
        CharacterComboDef.apply(GamePackets.TYPEID_NURI, parts, typeid -> typeid == 134218752);
        assertEquals(134218752, parts[0]);
        for (int i = 1; i < 24; i++) {
            assertEquals(0, parts[i]);
        }
    }

    @Test
    void applySkipsMissingParts() {
        int[] parts = new int[24];
        CharacterComboDef.apply(GamePackets.TYPEID_NURI, parts, typeid -> false);
        for (int slot : parts) {
            assertEquals(0, slot);
        }
    }

    @Test
    void slotsIncrementBy8192() {
        assertEquals(134226944, CharacterComboDef.partTypeid(GamePackets.TYPEID_NURI, 1));
        assertTrue(CharacterComboDef.partTypeid(GamePackets.TYPEID_NURI, 23) > 0);
    }
}
