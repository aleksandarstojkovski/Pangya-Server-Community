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

    public PacketWriter u64(long value) {
        for (int i = 0; i < 8; i++) {
            out.write((int) ((value >>> (8 * i)) & 0xff));
        }
        return this;
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
