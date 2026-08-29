package org.pangya.game;

import org.junit.jupiter.api.Test;
import org.pangya.protocol.game.GamePackets;
import org.pangya.protocol.packet.PacketReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CourseRainStatsTest {

    @Test
    void threeConsecutiveRainHolesMarkThreeHoleStreak() {
        List<GamePackets.HoleInfo> holes = new ArrayList<>();
        for (int n = 1; n <= 6; n++) {
            int weather = n <= 3 ? GamePackets.WEATHER_RAIN : 0;
            holes.add(new GamePackets.HoleInfo(n, 0, 0, n, weather, 0, 0));
        }
        CourseRainStats stats = CourseRainStats.build(holes, 6);
        assertEquals(3, stats.countHolesRainBySeq(4));
        assertEquals(0, stats.countRain2ConsecBySeq(3));
        assertEquals(1, stats.countRain3ConsecBySeq(4));
        assertEquals(0, stats.countRain4PlusConsecBySeq(4));
    }

    @Test
    void queueRainCountersMatchesCsharpTypeids() {
        List<GamePackets.HoleInfo> holes = new ArrayList<>();
        for (int n = 1; n <= 4; n++) {
            holes.add(new GamePackets.HoleInfo(n, 0, 0, n, GamePackets.WEATHER_RAIN, 0, 0));
        }
        CourseRainStats stats = CourseRainStats.build(holes, 4);
        PacketReader created = new PacketReader(GamePackets.clientCreatePractice("rain", "s"));
        created.opcode();
        GameRoom room = new GameRoom(GamePackets.readCreateRoom(created), 1, 10001, 100, 100, 0);
        AchievementCounterTypeids.queueRainCounters(room, 10001, stats, 4);
        var pending = room.takePendingAchievementCounters(10001);
        assertEquals(4, pending.getOrDefault(GamePackets.TYPEID_RAIN_COUNTER, 0));
        assertEquals(1, pending.getOrDefault(GamePackets.TYPEID_RAIN_4_CONSEC_COUNTER, 0));
    }
}
