package org.pangya.protocol.iff;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** C# {@code SYSTEMTIME} (16 bytes, pack 1). */
public record IffSystemTime(
        int year,
        int month,
        int dayOfWeek,
        int day,
        int hour,
        int minute,
        int second,
        int milliSecond) {

    public static IffSystemTime empty() {
        return new IffSystemTime(0, 0, 0, 0, 0, 0, 0, 0);
    }

    public static IffSystemTime read(byte[] data, int offset) {
        ByteBuffer buf = ByteBuffer.wrap(data, offset, 16).order(ByteOrder.LITTLE_ENDIAN);
        return new IffSystemTime(
                buf.getShort() & 0xffff,
                buf.getShort() & 0xffff,
                buf.getShort() & 0xffff,
                buf.getShort() & 0xffff,
                buf.getShort() & 0xffff,
                buf.getShort() & 0xffff,
                buf.getShort() & 0xffff,
                buf.getShort() & 0xffff);
    }

    /** C# {@code SYSTEMTIME.IsEmpty}. */
    public boolean isEmpty() {
        return year == 0
                && month == 0
                && dayOfWeek == 0
                && day == 0
                && hour == 0
                && minute == 0
                && second == 0
                && milliSecond == 0;
    }

    /** GP enter gate uses hour/minute when date fields are zero. */
    public boolean hasGpClock() {
        return !isEmpty() && (hour != 0 || minute != 0);
    }

    public IffSystemTime withDay(int newDay) {
        return new IffSystemTime(year, month, dayOfWeek, newDay, hour, minute, second, milliSecond);
    }
}
