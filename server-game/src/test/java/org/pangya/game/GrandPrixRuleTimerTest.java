package org.pangya.game;

import org.junit.jupiter.api.Test;
import org.pangya.protocol.game.GamePackets;
import org.pangya.protocol.packet.PacketReader;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** GP per-shot rule timer ({@code end_time_rule} / {@code timeRuleIsOver}) infrastructure. */
final class GrandPrixRuleTimerTest {

    @Test
    void stopGpRuleTimerCancelsPendingTimeout() throws Exception {
        GameRoom room = gpRoom();
        AtomicBoolean fired = new AtomicBoolean();
        CountDownLatch done = new CountDownLatch(1);
        room.startGpRuleTimer(7, 200, () -> {
            fired.set(true);
            done.countDown();
        });
        room.stopGpRuleTimer(7);
        assertFalse(done.await(500, TimeUnit.MILLISECONDS));
        assertFalse(fired.get());
    }

    @Test
    void gpRuleTimerFiresWhenNotStopped() throws Exception {
        GameRoom room = gpRoom();
        AtomicBoolean fired = new AtomicBoolean();
        CountDownLatch done = new CountDownLatch(1);
        room.startGpRuleTimer(8, 100, () -> {
            fired.set(true);
            done.countDown();
        });
        assertTrue(done.await(1, TimeUnit.SECONDS));
        assertTrue(fired.get());
    }

    @Test
    void rulePenaltyRequiresZeroInitShot() {
        GameRoom.PlayerShot shot = new GameRoom.PlayerShot();
        shot.initShot = 0;
        applyRuleTimerPenalty(shot);
        assertTrue(shot.penalidade > 0);

        shot.penalidade = 0;
        shot.initShot = 1;
        applyRuleTimerPenalty(shot);
        assertTrue(shot.penalidade == 0);
    }

    /** Mirrors C# {@code GrandPrix.timeRuleIsOver} when {@code m_game_init_state == 1}. */
    private static void applyRuleTimerPenalty(GameRoom.PlayerShot shot) {
        if (shot.initShot == 0) {
            shot.penalidade++;
        }
    }

    private static GameRoom gpRoom() {
        PacketReader created = new PacketReader(
                GamePackets.clientCreateRoom(GamePackets.TIPO_GRAND_PRIX, "GP", ""));
        created.opcode();
        return new GameRoom(GamePackets.readCreateRoom(created), 1, 10001, 100, 100, 0);
    }
}
