package org.pangya.game.catalog;

import org.junit.jupiter.api.Test;
import org.pangya.protocol.game.GamePackets;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HoleDropResolverTest {

    @Test
    void ticketCountDoublesWithCharMotion() {
        assertEquals(2, HoleDropResolver.ticketCount(1, 0));
        assertEquals(1, HoleDropResolver.ticketCount(0, 0));
    }

    @Test
    void ticketCountRainbowHatAddsOne() {
        assertEquals(2, HoleDropResolver.ticketCount(0, HoleDropResolver.ART_RAINBOW_MAGIC_HAT));
        assertEquals(3, HoleDropResolver.ticketCount(1, HoleDropResolver.ART_RAINBOW_MAGIC_HAT));
    }

    @Test
    void drawSscTicketsShapeWhenWin() {
        boolean sawWin = false;
        for (long seed = 0; seed < 20_000; seed++) {
            List<GamePackets.DropItem> drops = HoleDropResolver.drawSscTickets(
                    100, 0, 1, 0, 1, 100, 0, seed);
            if (drops.isEmpty()) {
                continue;
            }
            sawWin = true;
            assertEquals(HoleDropResolver.ticketCount(1, 0), drops.size());
            for (GamePackets.DropItem drop : drops) {
                assertEquals(GamePackets.TYPEID_SSC_TICKET, drop.typeid());
                assertEquals(GamePackets.DROP_TYPE_NORMAL_QNTD, drop.type());
                assertEquals(1, drop.qntd());
                assertEquals(0, drop.course());
                assertEquals(1, drop.hole());
            }
            break;
        }
        assertTrue(sawWin, "expected at least one SSC win in probe range");
    }

    @Test
    void grandPrixTicketQntdOnLongGames() {
        assertEquals(2, HoleDropResolver.grandPrixTicketQntd(0, 18));
        assertEquals(1, HoleDropResolver.grandPrixTicketQntd(49, 18));
        assertEquals(1, HoleDropResolver.grandPrixTicketQntd(0, 9));
        assertEquals(0, HoleDropResolver.grandPrixTicketQntd(50, 18));
    }

    @Test
    void drawGrandPrixTicketBlockedAtWarehouseCap() {
        assertTrue(HoleDropResolver.drawGrandPrixTicket(0, 18, 18, 50).isEmpty());
    }

    @Test
    void drawManaArtefactEmptyWithoutPool() {
        assertTrue(HoleDropResolver.drawManaArtefact(100, 0, 1, 100, 0, List.of()).isEmpty());
    }
}
