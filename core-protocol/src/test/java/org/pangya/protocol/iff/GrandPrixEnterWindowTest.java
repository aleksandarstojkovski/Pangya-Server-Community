package org.pangya.protocol.iff;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrandPrixEnterWindowTest {

    @Test
    void rookieControlPracticeForcesNewRoom() {
        assertTrue(GrandPrixEnterWindow.forceNewRoomInstance(0x100));
        assertFalse(GrandPrixEnterWindow.forceNewRoomInstance(0x180101));
    }

    @Test
    void rejectsBeforeOpenAndAfterStart() {
        IffSystemTime open = new IffSystemTime(0, 0, 0, 0, 1, 0, 0, 0);
        IffSystemTime start = new IffSystemTime(0, 0, 0, 0, 1, 10, 0, 0);
        assertTrue(GrandPrixEnterWindow.outsideEnterWindow(open, start, LocalTime.of(0, 59)));
        assertFalse(GrandPrixEnterWindow.outsideEnterWindow(open, start, LocalTime.of(1, 5)));
        assertTrue(GrandPrixEnterWindow.outsideEnterWindow(open, start, LocalTime.of(1, 11)));
    }

    @Test
    void skipsWhenOpenAndStartAreEmpty() {
        assertFalse(GrandPrixEnterWindow.outsideEnterWindow(
                IffSystemTime.empty(), IffSystemTime.empty(), LocalTime.of(12, 0)));
    }

    @Test
    void hourDiffMatchesCSharpSecondRounding() {
        IffSystemTime earlier = new IffSystemTime(0, 0, 0, 0, 1, 0, 0, 0);
        IffSystemTime later = new IffSystemTime(0, 0, 0, 0, 1, 1, 0, 500);
        assertEquals(61L, GrandPrixEnterWindow.hourDiffSeconds(later, earlier));
    }
}
