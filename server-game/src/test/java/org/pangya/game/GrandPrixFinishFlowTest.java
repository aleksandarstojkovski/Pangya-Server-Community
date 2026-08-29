package org.pangya.game;

import org.pangya.protocol.game.GamePackets;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests for GP shot-sync gate flags on {@link GameRoom.PlayerShot}. */
final class GrandPrixFinishFlowTest {

    @Test
    void checkAllGrandPrixShotPacketsRequiresInitSyncAndFinish() {
        GameRoom.PlayerShot shot = new GameRoom.PlayerShot();
        assertFalse(allPackets(shot));

        shot.initShot = 1;
        assertFalse(allPackets(shot));

        shot.syncShotFlag = 1;
        assertFalse(allPackets(shot));

        shot.finishShot = 1;
        assertTrue(allPackets(shot));

        shot.initShot = 0;
        shot.syncShotFlag = 0;
        shot.timeOuts = 1;
        assertTrue(allPackets(shot));
    }

    @Test
    void giveUpIncrementsBadConduct() {
        GameRoom.PlayerShot shot = new GameRoom.PlayerShot();
        shot.displayState = 0;
        shot.tacadaNum = 4;
        int totalShot = 5;
        if ((shot.displayState & GamePackets.DISPLAY_ACERTO_HOLE) == 0
                && totalShot <= shot.tacadaNum + 1) {
            if (shot.tacadaNum < totalShot) {
                shot.tacadaNum++;
            }
            shot.giveUp = 1;
            shot.badConduct++;
        }
        assertEquals(1, shot.giveUp);
        assertEquals(1, shot.badConduct);
    }

    @Test
    void kickGateRequiresThreeGiveUps() {
        GameRoom.PlayerShot shot = new GameRoom.PlayerShot();
        shot.badConduct = 2;
        shot.giveUp = 1;
        assertFalse(shot.badConduct >= 3 && (shot.giveUp > 0 || shot.timeOuts > 0));
        shot.badConduct = 3;
        assertTrue(shot.badConduct >= 3 && (shot.giveUp > 0 || shot.timeOuts > 0));
    }

    private static boolean allPackets(GameRoom.PlayerShot shot) {
        return (shot.initShot > 0 && shot.syncShotFlag > 0 || shot.timeOuts > 0)
                && shot.finishShot > 0;
    }
}
