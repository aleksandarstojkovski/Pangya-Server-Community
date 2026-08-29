package org.pangya.game;

import org.junit.jupiter.api.Test;
import org.pangya.protocol.game.GamePackets;
import org.pangya.protocol.packet.PacketReader;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** GP per-hole timer ({@code GrandPrix.timeIsOver} / {@code 0x259}) infrastructure. */
final class GrandPrixHoleTimerTest {

    @Test
    void stopGpHoleTimerCancelsPendingTimeout() throws Exception {
        GameRoom room = gpRoom();
        AtomicBoolean fired = new AtomicBoolean();
        CountDownLatch done = new CountDownLatch(1);
        room.startGpHoleTimer(3, 200, () -> {
            fired.set(true);
            done.countDown();
        });
        room.stopGpHoleTimer(3);
        assertFalse(done.await(500, TimeUnit.MILLISECONDS));
        assertFalse(fired.get());
    }

    @Test
    void holeTimeOverSetsMaxShotsAndTimeoutFlag() {
        GameRoom.PlayerShot shot = new GameRoom.PlayerShot();
        shot.hole = 2;
        int totalShot = 7;
        applyHoleTimeOver(shot, totalShot);
        assertEquals(totalShot, shot.tacadaNum);
        assertEquals(1, shot.timeOuts);
    }

    @Test
    void holeTimeOverSkippedWhenHoleAlreadyFinished() {
        GameRoom.PlayerShot shot = new GameRoom.PlayerShot();
        shot.finishHole2 = 1;
        shot.tacadaNum = 2;
        applyHoleTimeOver(shot, 7);
        assertEquals(2, shot.tacadaNum);
        assertEquals(0, shot.timeOuts);
    }

    /** Mirrors C# {@code GrandPrix.timeIsOver} when hole is still active. */
    private static void applyHoleTimeOver(GameRoom.PlayerShot shot, int totalShot) {
        if (shot.finishHole2 != 0 || shot.finishHole3 != 0) {
            return;
        }
        shot.tacadaNum = totalShot;
        shot.timeOuts = 1;
    }

    private static GameRoom gpRoom() {
        PacketReader created = new PacketReader(
                GamePackets.clientCreateRoom(GamePackets.TIPO_GRAND_PRIX, "GP", ""));
        created.opcode();
        return new GameRoom(GamePackets.readCreateRoom(created), 1, 10001, 100, 100, 0);
    }
}
