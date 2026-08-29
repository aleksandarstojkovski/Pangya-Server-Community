package org.pangya.game;

import org.junit.jupiter.api.Test;
import org.pangya.protocol.game.GamePackets;
import org.pangya.protocol.packet.PacketReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests for GP quit / {@code deletePlayer} flag gates. */
final class GrandPrixExitFlowTest {

    @Test
    void quitFlagMarksPlayerAsLeft() {
        GameRoom room = gpRoom();
        room.setGameFlag(7, GamePackets.FLAG_GAME_QUIT);
        assertEquals(GamePackets.FLAG_GAME_QUIT, room.gameFlag(7));
    }

    @Test
    void badConductAchievementGateMatchesCSharp() {
        GameRoom.PlayerShot shot = new GameRoom.PlayerShot();
        shot.badConduct = 2;
        shot.giveUp = 1;
        assertFalse(badConductAchievement(shot));
        shot.badConduct = 3;
        assertTrue(badConductAchievement(shot));
        shot.giveUp = 0;
        shot.timeOuts = 1;
        assertTrue(badConductAchievement(shot));
    }

    private static GameRoom gpRoom() {
        PacketReader created = new PacketReader(
                GamePackets.clientCreateRoom(GamePackets.TIPO_GRAND_PRIX, "GP", ""));
        created.opcode();
        return new GameRoom(GamePackets.readCreateRoom(created), 1, 10001, 100, 100, 0);
    }

    /** C# {@code GrandPrix.deletePlayer}: {@code bad_condute >= 3 && (giveup || time_out)}. */
    private static boolean badConductAchievement(GameRoom.PlayerShot shot) {
        return shot.badConduct >= 3 && (shot.giveUp > 0 || shot.timeOuts > 0);
    }
}
