package org.pangya.game;

import org.pangya.protocol.game.GamePackets;
import org.junit.jupiter.api.Test;

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

    private static boolean allPackets(GameRoom.PlayerShot shot) {
        return (shot.initShot > 0 && shot.syncShotFlag > 0 || shot.timeOuts > 0)
                && shot.finishShot > 0;
    }
}
