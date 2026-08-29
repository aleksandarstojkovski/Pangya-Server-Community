package org.pangya.protocol.iff;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

/**
 * C# {@code IFFFile<AuxPart>} index ({@code Marshal.SizeOf(AuxPart)} = 228 bytes,
 * {@code Pack = 1}).
 */
public final class IffAuxPartFile {

    public static final int VERSION = 13;
    public static final int RECORD_BYTES = 228;
    /** C# {@code AuxPart.Drop_Rate} after {@code IFFCommon} + cc/c/slot + Power_Drive. */
    static final int DROP_RATE_OFFSET = 214;
    static final int PANG_RATE_OFFSET = 218;
    static final int EXP_RATE_OFFSET = 220;

    private IffAuxPartFile() {}

    public static IffAuxPartIndex loadIndex(byte[] data) {
        IffHeader header = readHeader(data);
        int expected = IffHeader.BYTES + header.count() * RECORD_BYTES;
        if (data.length < expected) {
            throw new IllegalArgumentException("AuxPart.iff truncated: need " + expected + " bytes");
        }
        Map<Integer, IffAuxPartRecord> out = HashMap.newHashMap(header.count());
        for (int i = 0; i < header.count(); i++) {
            int base = IffHeader.BYTES + i * RECORD_BYTES;
            int active = ByteBuffer.wrap(data, base, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            if (active == 0) {
                continue;
            }
            int typeid = ByteBuffer.wrap(data, base + 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            int dropRate = u16(data, base + DROP_RATE_OFFSET);
            int pangRate = u16(data, base + PANG_RATE_OFFSET);
            int expRate = u16(data, base + EXP_RATE_OFFSET);
            out.put(typeid, new IffAuxPartRecord(typeid, dropRate, pangRate, expRate));
        }
        return new IffAuxPartIndex(Map.copyOf(out));
    }

    public static IffAuxPartIndex loadIndex(PangyaIffArchive archive) throws java.io.IOException {
        return loadIndex(archive.readEntry("AuxPart.iff"));
    }

    private static IffHeader readHeader(byte[] data) {
        if (data.length < IffHeader.BYTES) {
            throw new IllegalArgumentException("AuxPart.iff too short");
        }
        IffHeader header = IffHeader.read(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN));
        if (header.version() != VERSION) {
            throw new IllegalArgumentException("AuxPart.iff version " + header.version() + " != " + VERSION);
        }
        return header;
    }

    private static int u16(byte[] data, int offset) {
        return Short.toUnsignedInt(
                ByteBuffer.wrap(data, offset, 2).order(ByteOrder.LITTLE_ENDIAN).getShort());
    }
}
