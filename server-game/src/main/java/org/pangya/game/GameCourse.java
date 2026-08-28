package org.pangya.game;

import org.pangya.protocol.game.GamePackets;

import java.util.ArrayList;
import java.util.List;

/**
 * Synthetic C# {@code CourseManager} without IFF files: 18 holes, FRONT sequence 1..18,
 * pin {@code (n-1)%3}, cube count 0. Real pin/tee coords arrive later in CLIENT {@code 0x1A}.
 */
final class GameCourse {

    /** C# {@code rnd.Next(1, short.MaxValue)} range; 1 is a valid production seed. */
    static final int SEED = 1;

    final int seed;
    final List<GamePackets.HoleInfo> holes = new ArrayList<>(GamePackets.COURSE_HOLE_COUNT);

    GameCourse(GamePackets.RoomInfo info) {
        this.seed = SEED;
        int course = info.course & 0x7f;
        for (int n = 1; n <= GamePackets.COURSE_HOLE_COUNT; n++) {
            holes.add(new GamePackets.HoleInfo(n, (n - 1) % 3, course, n, 0, 0, 0));
        }
    }

    GamePackets.HoleInfo find(int numero) {
        for (GamePackets.HoleInfo hole : holes) {
            if (hole.numero() == numero) {
                return hole;
            }
        }
        return null;
    }
}
