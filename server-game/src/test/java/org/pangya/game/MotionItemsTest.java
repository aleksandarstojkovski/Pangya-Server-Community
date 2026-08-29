package org.pangya.game;

import org.junit.jupiter.api.Test;
import org.pangya.protocol.game.GamePackets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MotionItemsTest {

    @Test
    void detectsMotionIntroPart() {
        GamePackets.CharacterInfo character = new GamePackets.CharacterInfo();
        character.typeid = 0x08000099;
        character.partsTypeid[0] = 0x08026800;
        assertTrue(MotionItems.hasMotionPart(character));
    }

    @Test
    void noMotionWithoutMatchingPart() {
        GamePackets.CharacterInfo character = new GamePackets.CharacterInfo();
        character.typeid = 0x08000099;
        assertFalse(MotionItems.hasMotionPart(character));
    }
}
