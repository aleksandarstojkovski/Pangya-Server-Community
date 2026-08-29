package org.pangya.game.catalog;

import org.junit.jupiter.api.Test;
import org.pangya.protocol.game.GamePackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AngelWingsResolverTest {

    @Test
    void quitRateMatchesCSharpFormula() {
        assertEquals(0f, AngelWingsResolver.quitRate(0, 5));
        assertEquals(2f, AngelWingsResolver.quitRate(100, 2));
    }

    @Test
    void angelEquippedBlockedByQuitRate() {
        GamePackets.CharacterInfo character = new GamePackets.CharacterInfo();
        character.typeid = 0x08000000;
        int wing = 134309903;
        character.partsTypeid[GamePackets.itemCharPartNumber(wing)] = wing;
        assertEquals(0, AngelWingsResolver.angelEquipped(character, 3.0f));
        assertEquals(1, AngelWingsResolver.angelEquipped(character, 2.9f));
    }

    @Test
    void angelEquippedRequiresMatchingPartSlot() {
        GamePackets.CharacterInfo character = new GamePackets.CharacterInfo();
        character.typeid = 0x08000000;
        assertEquals(0, AngelWingsResolver.angelEquipped(character, 0f));
    }
}
