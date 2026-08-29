package org.pangya.game;

import org.junit.jupiter.api.Test;
import org.pangya.protocol.game.GamePackets;
import org.pangya.protocol.packet.PacketReader;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AchievementCounterTypeidsTest {

    @Test
    void holeScoreCounterMatchesCsharp() {
        assertEquals(GamePackets.TYPEID_HIO_HOLE_COUNTER, AchievementCounterTypeids.holeScoreCounter(1, 4));
        assertEquals(GamePackets.TYPEID_ALBA_HOLE_COUNTER, AchievementCounterTypeids.holeScoreCounter(2, 5));
        assertEquals(GamePackets.TYPEID_EAGLE_HOLE_COUNTER, AchievementCounterTypeids.holeScoreCounter(3, 5));
        assertEquals(GamePackets.TYPEID_BIRDIE_HOLE_COUNTER, AchievementCounterTypeids.holeScoreCounter(3, 4));
        assertEquals(GamePackets.TYPEID_PAR_HOLE_COUNTER, AchievementCounterTypeids.holeScoreCounter(4, 4));
        assertEquals(0, AchievementCounterTypeids.holeScoreCounter(5, 4));
    }

    @Test
    void scoreConsecutivosIncrementsWhenStreakBreaks() {
        PacketReader created = new PacketReader(GamePackets.clientCreatePractice("t", "s"));
        created.opcode();
        GameRoom room = new GameRoom(GamePackets.readCreateRoom(created), 1, 10001, 100, 100, 0);
        room.info.holes = 3;
        int[] tacada = {3, 3, 4};
        int[] par = {4, 4, 4};
        AchievementCounterTypeids.queueScoreConsecutivosCounters(room, 10001, tacada, par, 3);
        var pending = room.takePendingAchievementCounters(10001);
        assertEquals(1, pending.getOrDefault(GamePackets.TYPEID_CONSEC_BIRDIE_COUNTER, 0));
    }

    @Test
    void scoreNumMatchesCsharpLabels() {
        assertEquals(0, AchievementCounterTypeids.scoreNum(1, 4));
        assertEquals(3, AchievementCounterTypeids.scoreNum(3, 4));
        assertEquals(4, AchievementCounterTypeids.scoreNum(4, 4));
        assertEquals(-1, AchievementCounterTypeids.scoreNum(8, 4));
    }
}
