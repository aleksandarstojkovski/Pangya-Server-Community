package org.pangya.protocol.iff;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * C# {@code IFFFile<GrandPrixSpecialHole>} index ({@code Marshal.SizeOf} = 20 bytes).
 */
public final class IffGrandPrixSpecialHoleFile {

    public static final int VERSION = 13;
    public static final int RECORD_BYTES = 20;

    private IffGrandPrixSpecialHoleFile() {}

    public static IffGrandPrixSpecialHoleIndex loadIndex(byte[] data) {
        IffHeader header = readHeader(data);
        int expected = IffHeader.BYTES + header.count() * RECORD_BYTES;
        if (data.length < expected) {
            throw new IllegalArgumentException("GrandPrixSpecialHole.iff truncated: need " + expected + " bytes");
        }
        Map<Integer, List<IffGrandPrixSpecialHoleRecord>> out = HashMap.newHashMap(header.count());
        for (int i = 0; i < header.count(); i++) {
            int base = IffHeader.BYTES + i * RECORD_BYTES;
            int enable = ByteBuffer.wrap(data, base, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            int typeid = ByteBuffer.wrap(data, base + 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            int holePos = ByteBuffer.wrap(data, base + 8, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            int map = ByteBuffer.wrap(data, base + 12, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            int hole = ByteBuffer.wrap(data, base + 16, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            out.computeIfAbsent(typeid, k -> new ArrayList<>())
                    .add(new IffGrandPrixSpecialHoleRecord(enable, typeid, holePos, map, hole));
        }
        return new IffGrandPrixSpecialHoleIndex(Map.copyOf(out), header.count());
    }

    public static IffGrandPrixSpecialHoleIndex loadIndex(PangyaIffArchive archive) throws java.io.IOException {
        return loadIndex(archive.readEntry("GrandPrixSpecialHole.iff"));
    }

    private static IffHeader readHeader(byte[] data) {
        if (data.length < IffHeader.BYTES) {
            throw new IllegalArgumentException("GrandPrixSpecialHole.iff too short");
        }
        IffHeader header = IffHeader.read(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN));
        if (header.version() != VERSION) {
            throw new IllegalArgumentException(
                    "GrandPrixSpecialHole.iff version " + header.version() + " != " + VERSION);
        }
        return header;
    }
}
