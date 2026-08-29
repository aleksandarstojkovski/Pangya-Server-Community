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

    @Test
    void syncShotCountersQueueLongPuttAndPowerShot() {
        PacketReader created = new PacketReader(GamePackets.clientCreatePractice("t", "s"));
        created.opcode();
        GameRoom room = new GameRoom(GamePackets.readCreateRoom(created), 1, 10001, 100, 100, 0);
        int display = GamePackets.DISPLAY_ACERTO_HOLE | GamePackets.DISPLAY_LONG_PUTT;
        int shot = GamePackets.SHOT_CLUB_PUTT | GamePackets.SHOT_POWER_SHOT;
        AchievementCounterTypeids.queueSyncShotCounters(room, 10001, display, shot, 30.0f, (byte) 0);
        var pending = room.takePendingAchievementCounters(10001);
        assertEquals(1, pending.getOrDefault(GamePackets.TYPEID_LONG_PUTT_17_COUNTER, 0));
        assertEquals(1, pending.getOrDefault(GamePackets.TYPEID_LONG_PUTT_20_COUNTER, 0));
        assertEquals(1, pending.getOrDefault(GamePackets.TYPEID_LONG_PUTT_25_COUNTER, 0));
        assertEquals(1, pending.getOrDefault(GamePackets.TYPEID_LONG_PUTT_30_COUNTER, 0));
        assertEquals(1, pending.getOrDefault(GamePackets.TYPEID_POWER_SHOT_COUNTER, 0));
    }

    @Test
    void syncShotCountersErrandoPangyaOnHoleOut() {
        PacketReader created = new PacketReader(GamePackets.clientCreatePractice("t", "s"));
        created.opcode();
        GameRoom room = new GameRoom(GamePackets.readCreateRoom(created), 1, 10001, 100, 100, 0);
        AchievementCounterTypeids.queueSyncShotCounters(
                room,
                10001,
                GamePackets.DISPLAY_ACERTO_HOLE,
                0,
                0f,
                (byte) GamePackets.ACERTO_PANGYA_MISS);
        var pending = room.takePendingAchievementCounters(10001);
        assertEquals(1, pending.getOrDefault(GamePackets.TYPEID_ERRANDO_PANGYA_COUNTER, 0));
    }

    @Test
    void grandPrixInitCountersQueuePlayAndClassCounters() {
        PacketReader created = new PacketReader(GamePackets.clientCreateRoom(GamePackets.TIPO_GRAND_PRIX, "GP", ""));
        created.opcode();
        GameRoom room = new GameRoom(GamePackets.readCreateRoom(created), 1, 10001, 100, 100, 0);
        AchievementCounterTypeids.queueGrandPrixInitCounters(room, 10001, 0x100);
        var pending = room.takePendingAchievementCounters(10001);
        assertEquals(1, pending.getOrDefault(GamePackets.TYPEID_GP_PLAY_COUNTER, 0));
        assertEquals(1, pending.getOrDefault(GamePackets.TYPEID_GP_CLASS_ROOKIE_COUNTER, 0));
    }

    @Test
    void grandPrixClassCounterUsesEventSpecialFlag() {
        assertEquals(
                GamePackets.TYPEID_GP_CLASS_EVENT_SPECIAL_COUNTER,
                AchievementCounterTypeids.grandPrixClassCounter(0x3000000));
        assertEquals(
                GamePackets.TYPEID_GP_CLASS_BEGINNER_COUNTER,
                AchievementCounterTypeids.grandPrixClassCounter(0x80000));
    }

    @Test
    void initCountersIncludeShortGameAndMasterArtefact() {
        PacketReader created = new PacketReader(GamePackets.clientCreateRoom(GamePackets.TIPO_STROKE, "VS", ""));
        created.opcode();
        GameRoom room = new GameRoom(GamePackets.readCreateRoom(created), 1, 10001, 100, 100, 0);
        room.info.natural = 0x2;
        room.info.artefato = 1;
        AchievementCounterTypeids.queueInitGameCounters(room, 10001, 0, 0, 0);
        var pending = room.takePendingAchievementCounters(10001);
        assertEquals(1, pending.getOrDefault(GamePackets.TYPEID_SHORT_GAME_COUNTER, 0));
        assertEquals(1, pending.getOrDefault(GamePackets.TYPEID_MASTER_ARTEFACT_COUNTER, 0));
    }

    @Test
    void itemUsedCountersQueueActiveAndPassiveUses() {
        PacketReader created = new PacketReader(GamePackets.clientCreatePractice("t", "s"));
        created.opcode();
        GameRoom room = new GameRoom(GamePackets.readCreateRoom(created), 1, 10001, 100, 100, 0);
        room.initActiveItems(1, new int[] {0x18000025, 0x18000025});
        room.tryUseActive(1, 0x18000025);
        room.initPassiveItem(1, GamePackets.TYPEID_AUTO_COMMAND);
        room.tryUsePassive(1, GamePackets.TYPEID_AUTO_COMMAND, 5);
        AchievementCounterTypeids.queueItemUsedCounters(room, 1, 10001);
        var pending = room.takePendingAchievementCounters(10001);
        assertEquals(1, pending.getOrDefault(GamePackets.TYPEID_ACTIVE_ITEM_COUNTER, 0));
        assertEquals(1, pending.getOrDefault(GamePackets.TYPEID_POWER_MILK_COUNTER, 0));
        assertEquals(1, pending.getOrDefault(GamePackets.TYPEID_PASSIVE_ITEM_COUNTER, 0));
    }
}
