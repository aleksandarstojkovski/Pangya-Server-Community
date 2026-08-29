package org.pangya.game;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.pangya.protocol.game.GamePackets;
import org.pangya.protocol.iff.PangyaIffLoader;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameCourseSpecialHoleTest {

    private static final Path JP_IFF = Path.of(
            "reference/pangya-server-community/Server/JP/GameServer/data/pangya_jp.iff");

    @AfterEach
    void tearDown() {
        PangyaIffLoader.reload(null);
    }

    @Test
    void gpSpecialHoleSequenceUsesIffMapsForFirstThreeHoles() throws Exception {
        if (!JP_IFF.toFile().isFile()) {
            return;
        }
        PangyaIffLoader.reload(JP_IFF);
        GamePackets.RoomInfo info = new GamePackets.RoomInfo();
        info.gpActive = 1;
        info.gpRankTypeid = 0x100;
        info.course = 0;
        List<int[]> seq = GameCourse.holeSequence(info, info.course);
        assertEquals(18, seq.size());
        assertEquals(11, seq.get(0)[0]);
        assertEquals(2, seq.get(0)[1]);
        assertEquals(16, seq.get(1)[0]);
        assertEquals(6, seq.get(1)[1]);
        assertEquals(20, seq.get(2)[0]);
        assertEquals(11, seq.get(2)[1]);
        assertEquals(0, seq.get(3)[0]);
        assertEquals(4, seq.get(3)[1]);
    }
}
