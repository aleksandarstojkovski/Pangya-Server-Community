package org.pangya.protocol.iff;

import java.time.LocalTime;

/**
 * C# {@code Channel.requestEnterRoomGrandPrix} open/start window.
 * Uses signed second delta (C# {@code UtilTime.GetHourDiff} compares before clamping).
 */
public final class GrandPrixEnterWindow {

    private GrandPrixEnterWindow() {}

    public static boolean outsideEnterWindow(IffSystemTime open, IffSystemTime start, LocalTime now) {
        IffSystemTime local = fromLocalTime(now);
        IffSystemTime startGate = start;
        if (open.hasGpClock() && open.hour() >= 23 && start.hasGpClock() && start.hour() <= 1) {
            startGate = start.withDay(1);
        }
        if (open.hasGpClock() && hourDiffSeconds(local, open) < 0) {
            return true;
        }
        return startGate.hasGpClock() && hourDiffSeconds(local, startGate) > 0;
    }

    static IffSystemTime fromLocalTime(LocalTime time) {
        return new IffSystemTime(0, 0, 0, 0, time.getHour(), time.getMinute(), time.getSecond(), 0);
    }

    /** C# {@code UtilTime.GetHourDiff}: second delta from {@code st1 - st2}. */
    static long hourDiffSeconds(IffSystemTime st1, IffSystemTime st2) {
        long ms1 = clockMillis(st1);
        long ms2 = clockMillis(st2);
        return Math.round((ms1 - ms2) / 1000.0);
    }

    private static long clockMillis(IffSystemTime st) {
        return ((st.hour() * 3600L + st.minute() * 60L + st.second()) * 1000L) + st.milliSecond();
    }

    /** C# {@code isGrandPrixNormal}: event flag in bits 24-25 is zero. */
    public static boolean isGrandPrixNormal(int typeid) {
        return ((typeid & 0x3000000) >>> 24) == 0;
    }

    /** C# {@code getGrandPrixAbaType}: {@code GP_ABA} nibble from typeid. */
    public static int grandPrixAba(int typeid) {
        return (typeid & 0x00FFFFFF) >>> 19;
    }

    /** C# {@code GP_ABA.ROOKIE}. */
    public static final int GP_ABA_ROOKIE = 0;
    /** C# {@code GP_ABA.BEGINNER}. */
    public static final int GP_ABA_BEGINNER = 1;
    /** C# {@code GP_ABA.JUNIOR}. */
    public static final int GP_ABA_JUNIOR = 2;
    /** C# {@code GP_ABA.EVENT}. */
    public static final int GP_ABA_EVENT = 3;

    /** C# {@code isGrandPrixEvent}: event flag nibble is 3. */
    public static boolean isGrandPrixEvent(int typeid) {
        return ((typeid & 0x3000000) >>> 24) == 3;
    }

    /** Rookie normal GP always allocates a fresh room instance. */
    public static boolean forceNewRoomInstance(int typeid) {
        return isGrandPrixNormal(typeid) && grandPrixAba(typeid) == GP_ABA_ROOKIE;
    }
}
