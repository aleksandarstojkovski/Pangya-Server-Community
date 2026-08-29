package org.pangya.protocol.iff;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

/**
 * C# {@code IFFFile<Mascot>} index ({@code Marshal.SizeOf(Mascot)} = 304 bytes).
 */
public final class IffMascotFile {

    public static final int VERSION = 13;
    public static final int RECORD_BYTES = 304;

    /** C# {@code Mascot.msg} ({@code Pack = 1}, 7 bytes). */
    static final int MSG_OFFSET = 293;

    private IffMascotFile() {}

    public static IffMascotIndex loadIndex(byte[] data) {
        IffHeader header = readHeader(data);
        int expected = IffHeader.BYTES + header.count() * RECORD_BYTES;
        if (data.length < expected) {
            throw new IllegalArgumentException("Mascot.iff truncated: need " + expected + " bytes");
        }
        Map<Integer, IffMascotRecord> out = HashMap.newHashMap(header.count());
        for (int i = 0; i < header.count(); i++) {
            int base = IffHeader.BYTES + i * RECORD_BYTES;
            int active = ByteBuffer.wrap(data, base, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            if (active == 0) {
                continue;
            }
            int typeid = ByteBuffer.wrap(data, base + 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            boolean messageActive = data[base + MSG_OFFSET] != 0;
            int changePrice = ByteBuffer.wrap(data, base + MSG_OFFSET + 3, 4)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .getInt();
            out.put(typeid, new IffMascotRecord(typeid, messageActive, changePrice));
        }
        return new IffMascotIndex(Map.copyOf(out));
    }

    public static IffMascotIndex loadIndex(PangyaIffArchive archive) throws java.io.IOException {
        return loadIndex(archive.readEntry("Mascot.iff"));
    }

    private static IffHeader readHeader(byte[] data) {
        if (data.length < IffHeader.BYTES) {
            throw new IllegalArgumentException("Mascot.iff too short");
        }
        IffHeader header = IffHeader.read(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN));
        if (header.version() != VERSION) {
            throw new IllegalArgumentException("Mascot.iff version " + header.version() + " != " + VERSION);
        }
        return header;
    }
}
