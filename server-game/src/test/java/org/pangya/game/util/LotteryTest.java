package org.pangya.game.util;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LotteryTest {

    @Test
    void spinRoletaRespectsRateWeightsWithSeed() {
        Lottery lottery = new Lottery(42L);
        lottery.push(100, "common");
        lottery.push(900, "rare");

        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            Lottery.Entry<String> draw = lottery.spinRoleta(false);
            assertNotNull(draw);
            seen.add(draw.value());
        }
        assertTrue(seen.contains("common"));
        assertTrue(seen.contains("rare"));
    }

    @Test
    void spinRoletaRemoveDrawnDepletesPool() {
        Lottery lottery = new Lottery(7L);
        lottery.push(100, "a");
        lottery.push(100, "b");

        assertNotNull(lottery.spinRoleta(true));
        assertNotNull(lottery.spinRoleta(true));
        assertEquals(2, lottery.countItems());
    }

    @Test
    void emptyCtxThrowsOnSpin() {
        Lottery lottery = new Lottery(1L);
        assertThrows(IllegalStateException.class, () -> lottery.spinRoleta(false));
    }
}
