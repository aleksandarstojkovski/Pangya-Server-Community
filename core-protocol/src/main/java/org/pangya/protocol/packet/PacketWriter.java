package org.pangya.protocol.packet;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

/**
 * Little-endian writer matching C# {@code PangyaBinaryWriter}
 * ({@code init_plain}, {@code WritePStr}/{@code WriteString}, {@code WriteStr}).
 */
public final class PacketWriter {

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    public PacketWriter opcode(int opcode) {
        u16(opcode);
        return this;
    }

    public PacketWriter u8(int value) {
        out.write(value & 0xff);
        return this;
    }

    public PacketWriter u16(int value) {
        out.write(value & 0xff);
        out.write((value >>> 8) & 0xff);
        return this;
    }

    public PacketWriter i16(int value) {
        return u16(value);
    }

    public PacketWriter u32(int value) {
        out.write(value & 0xff);
        out.write((value >>> 8) & 0xff);
        out.write((value >>> 16) & 0xff);
        out.write((value >>> 24) & 0xff);
        return this;
    }

    public PacketWriter i32(int value) {
        return u32(value);
    }

    /** C# {@code Write(float)} / {@code WriteFloat}: IEEE-754 little-endian. */
    public PacketWriter f32(float value) {
        return u32(Float.floatToIntBits(value));
    }

    public PacketWriter u64(long value) {
        for (int i = 0; i < 8; i++) {
            out.write((int) ((value >>> (8 * i)) & 0xff));
        }
        return this;
    }

    public PacketWriter i64(long value) {
        return u64(value);
    }

    /** C# {@code WritePStr}/{@code WriteString(string)}: ushort length + Shift_JIS bytes. */
    public PacketWriter pstr(String value) {
        byte[] encoded = PacketIo.pstr(value);
        out.writeBytes(encoded);
        return this;
    }

    /** C# {@code WriteStr(s, length)}: fixed Shift_JIS field, zero-padded. */
    public PacketWriter fixedStr(String value, int length) {
        byte[] encoded = (value == null ? "" : value).getBytes(PacketIo.SHIFT_JIS);
        byte[] field = new byte[length];
        System.arraycopy(encoded, 0, field, 0, Math.min(encoded.length, length));
        out.writeBytes(field);
        return this;
    }

    public PacketWriter bytes(byte[] value) {
        if (value != null) {
            out.writeBytes(value);
        }
        return this;
    }

    /** C# {@code WriteBytes(buf, length)} / {@code WriteStr(s, length)}: zero-padded field. */
    public PacketWriter bytes(byte[] value, int length) {
        byte[] field = new byte[length];
        if (value != null) {
            System.arraycopy(value, 0, field, 0, Math.min(value.length, length));
        }
        out.writeBytes(field);
        return this;
    }

    public PacketWriter zero(int n) {
        if (n > 0) {
            out.writeBytes(new byte[n]);
        }
        return this;
    }

    public PacketWriter systemTimeNow() {
        java.time.ZonedDateTime now = java.time.ZonedDateTime.now(java.time.ZoneId.systemDefault());
        u16(now.getYear());
        u16(now.getMonthValue());
        u16(now.getDayOfWeek().getValue() % 7); // C# DayOfWeek: Sunday=0
        u16(now.getDayOfMonth());
        u16(now.getHour());
        u16(now.getMinute());
        u16(now.getSecond());
        u16(now.getNano() / 1_000_000);
        return this;
    }

    public byte[] toBytes() {
        return out.toByteArray();
    }

    public static byte[] of(byte[]... parts) {
        return PacketIo.concat(parts);
    }

    public static byte[] zeros(int n) {
        return new byte[n];
    }

    public static byte[] copy(byte[] src) {
        return Arrays.copyOf(src, src.length);
    }
}
