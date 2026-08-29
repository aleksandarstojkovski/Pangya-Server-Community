package org.pangya.network.session;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerBlockFlagTest {

    @Test
    void allGameUsesBitOne() {
        assertFalse(PlayerBlockFlag.allGame(0));
        assertTrue(PlayerBlockFlag.allGame(1L << PlayerBlockFlag.BIT_ALL_GAME));
    }

    @Test
    void grandPrixUsesBitTwentyOne() {
        assertFalse(PlayerBlockFlag.grandPrix(0));
        assertTrue(PlayerBlockFlag.grandPrix(1L << PlayerBlockFlag.BIT_GRAND_PRIX));
    }

    @Test
    void fromIdStateMapsLoungeAndShopBits() {
        long flags = PlayerBlockFlag.fromIdState(4L | 32L);
        assertTrue((flags & (1L << 13)) != 0);
        assertTrue((flags & (1L << 4)) != 0);
    }
}
