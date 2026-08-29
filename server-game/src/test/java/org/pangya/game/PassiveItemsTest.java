package org.pangya.game;

import org.junit.jupiter.api.Test;
import org.pangya.protocol.game.GamePackets;
import org.pangya.protocol.packet.PacketReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PassiveItemsTest {

    @Test
    void perHoleFinishIncrementsBallAndExpPerGamePassive() {
        PacketReader created = new PacketReader(GamePackets.clientCreatePractice("t", "s"));
        created.opcode();
        GameRoom room = new GameRoom(GamePackets.readCreateRoom(created), 1, 10001, 100, 100, 0);
        room.initPassiveItem(1, 0x1A00000F);
        room.initPassiveItem(1, GamePackets.TYPEID_DEFAULT_BALL + 1);
        room.updatePassiveOnHoleFinish(1, GamePackets.TYPEID_DEFAULT_BALL + 1, null);
        assertEquals(1, room.passiveUses.get(1).get(0x1A00000F));
        assertEquals(1, room.passiveUses.get(1).get(GamePackets.TYPEID_DEFAULT_BALL + 1));
    }

    @Test
    void finishExpPerGamePassiveAddsFinalIncrement() {
        PacketReader created = new PacketReader(GamePackets.clientCreatePractice("t", "s"));
        created.opcode();
        GameRoom room = new GameRoom(GamePackets.readCreateRoom(created), 1, 10001, 100, 100, 0);
        room.initPassiveItem(1, 0x1A00000F);
        room.passiveUses.get(1).put(0x1A00000F, 2);
        room.finishExpPerGamePassive(1);
        assertEquals(3, room.passiveUses.get(1).get(0x1A00000F));
    }

    @Test
    void activeUseTracksSlotIndices() {
        PacketReader created = new PacketReader(GamePackets.clientCreatePractice("t", "s"));
        created.opcode();
        GameRoom room = new GameRoom(GamePackets.readCreateRoom(created), 1, 10001, 100, 100, 0);
        room.initActiveItems(1, new int[] {0, 0x18000025, 0x18000025, 0});
        GameRoom.ActiveUse use = room.activeUses.get(1).get(0x18000025);
        assertEquals(2, use.slotIndices.size());
        assertEquals(1, use.slotIndices.get(0));
        assertEquals(2, use.slotIndices.get(1));
        assertTrue(room.tryUseActive(1, 0x18000025));
        assertEquals(1, use.count);
    }
}
