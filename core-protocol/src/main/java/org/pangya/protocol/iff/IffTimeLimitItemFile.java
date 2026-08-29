package org.pangya.protocol.iff;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

/**
 * C# {@code IFFFile<TimeLimitItem>} index ({@code Marshal.SizeOf(TimeLimitItem)} = 100 bytes,
 * {@code Pack = 1}).
 */
public final class IffTimeLimitItemFile {

    public static final int VERSION = 13;
    public static final int RECORD_BYTES = 100;

    static final int NAME_OFFSET = 8;
    static final int NAME_BYTES = 40;
    static final int ICON_BYTES = 40;
    static final int TYPE_OFFSET = 88;
    static final int PERCENT_OFFSET = 92;
    static final int TIME_OFFSET = 96;

    private IffTimeLimitItemFile() {}

    public static IffTimeLimitItemIndex loadIndex(byte[] data) {
        IffHeader header = readHeader(data);
        int expected = IffHeader.BYTES + header.count() * RECORD_BYTES;
        if (data.length < expected) {
            throw new IllegalArgumentException("TimeLimitItem.iff truncated: need " + expected + " bytes");
        }
        Map<Integer, IffTimeLimitItemRecord> out = HashMap.newHashMap(header.count());
        for (int i = 0; i < header.count(); i++) {
            int base = IffHeader.BYTES + i * RECORD_BYTES;
            int active = ByteBuffer.wrap(data, base, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            if (active == 0) {
                continue;
            }
            int typeid = ByteBuffer.wrap(data, base + 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            int tipo = ByteBuffer.wrap(data, base + TYPE_OFFSET, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            int percent = ByteBuffer.wrap(data, base + PERCENT_OFFSET, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            int timeMinutes = ByteBuffer.wrap(data, base + TIME_OFFSET, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            out.put(typeid, new IffTimeLimitItemRecord(typeid, tipo, percent, timeMinutes));
        }
        return new IffTimeLimitItemIndex(Map.copyOf(out));
    }

    public static IffTimeLimitItemIndex loadIndex(PangyaIffArchive archive) throws java.io.IOException {
        return loadIndex(archive.readEntry("TimeLimitItem.iff"));
    }

    private static IffHeader readHeader(byte[] data) {
        if (data.length < IffHeader.BYTES) {
            throw new IllegalArgumentException("TimeLimitItem.iff too short");
        }
        IffHeader header = IffHeader.read(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN));
        if (header.version() != VERSION) {
            throw new IllegalArgumentException("TimeLimitItem.iff version " + header.version() + " != " + VERSION);
        }
        return header;
    }
}
