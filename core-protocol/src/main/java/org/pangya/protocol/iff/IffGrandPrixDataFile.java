package org.pangya.protocol.iff;

import org.pangya.protocol.packet.PacketIo;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * C# {@code IFFFile<GrandPrixData>} index ({@code Marshal.SizeOf(GrandPrixData)} = 816 bytes).
 */
public final class IffGrandPrixDataFile {

    public static final int VERSION = 13;
    public static final int RECORD_BYTES = 816;

    static final int NAME_OFFSET = 18;
    static final int NAME_BYTES = 66;
    static final int NATURAL_MODE_OFFSET = 133;
    static final int RULE_OFFSET = 136;
    static final int COURSE_OFFSET = 140;
    static final int MODO_OFFSET = 144;
    static final int HOLES_OFFSET = 148;
    static final int MIN_LEVEL_OFFSET = 152;
    static final int MAX_LEVEL_OFFSET = 153;

    private static final Charset SHIFT_JIS = PacketIo.SHIFT_JIS;

    private IffGrandPrixDataFile() {}

    public static IffGrandPrixDataIndex loadIndex(byte[] data) {
        IffHeader header = readHeader(data);
        int expected = IffHeader.BYTES + header.count() * RECORD_BYTES;
        if (data.length < expected) {
            throw new IllegalArgumentException("GrandPrixData.iff truncated: need " + expected + " bytes");
        }
        Map<Integer, IffGrandPrixDataRecord> out = HashMap.newHashMap(header.count());
        for (int i = 0; i < header.count(); i++) {
            int base = IffHeader.BYTES + i * RECORD_BYTES;
            int active = ByteBuffer.wrap(data, base, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            if (active == 0) {
                continue;
            }
            int typeid = ByteBuffer.wrap(data, base + 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            int typeIdLink = ByteBuffer.wrap(data, base + 8, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            String name = readFixedString(data, base + NAME_OFFSET, NAME_BYTES);
            int rule = ByteBuffer.wrap(data, base + RULE_OFFSET, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            int course = ByteBuffer.wrap(data, base + COURSE_OFFSET, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            int modo = ByteBuffer.wrap(data, base + MODO_OFFSET, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            int holes = data[base + HOLES_OFFSET] & 0xff;
            boolean naturalMode = data[base + NATURAL_MODE_OFFSET] != 0;
            int minLevel = data[base + MIN_LEVEL_OFFSET] & 0xff;
            int maxLevel = data[base + MAX_LEVEL_OFFSET] & 0xff;
            out.put(typeid, new IffGrandPrixDataRecord(
                    typeid, typeIdLink, name, rule, course, modo, holes, naturalMode, minLevel, maxLevel));
        }
        return new IffGrandPrixDataIndex(Map.copyOf(out));
    }

    public static IffGrandPrixDataIndex loadIndex(PangyaIffArchive archive) throws java.io.IOException {
        return loadIndex(archive.readEntry("GrandPrixData.iff"));
    }

    private static IffHeader readHeader(byte[] data) {
        if (data.length < IffHeader.BYTES) {
            throw new IllegalArgumentException("GrandPrixData.iff too short");
        }
        IffHeader header = IffHeader.read(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN));
        if (header.version() != VERSION) {
            throw new IllegalArgumentException(
                    "GrandPrixData.iff version " + header.version() + " != " + VERSION);
        }
        return header;
    }

    private static String readFixedString(byte[] data, int offset, int length) {
        int end = offset;
        int limit = offset + length;
        while (end < limit && data[end] != 0) {
            end++;
        }
        return new String(data, offset, end - offset, SHIFT_JIS);
    }
}
