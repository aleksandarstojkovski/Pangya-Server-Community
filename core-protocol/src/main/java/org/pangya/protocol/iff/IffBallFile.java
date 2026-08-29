package org.pangya.protocol.iff;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

/**
 * C# {@code IFFFile<Ball>} index ({@code Marshal.SizeOf(Ball)} = 816 bytes).
 */
public final class IffBallFile {

    public static final int VERSION = 13;
    public static final int RECORD_BYTES = 816;
    /** C# {@code Ball.Stats.getSlot[0]} within each record. */
    static final int STATS_OFFSET = 804;

    private IffBallFile() {}

    public static IffBallIndex loadIndex(byte[] data) {
        IffHeader header = readHeader(data);
        int expected = IffHeader.BYTES + header.count() * RECORD_BYTES;
        if (data.length < expected) {
            throw new IllegalArgumentException("Ball.iff truncated: need " + expected + " bytes");
        }
        Map<Integer, Integer> out = HashMap.newHashMap(header.count());
        for (int i = 0; i < header.count(); i++) {
            int base = IffHeader.BYTES + i * RECORD_BYTES;
            int typeid = ByteBuffer.wrap(data, base + 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            int stack = ByteBuffer.wrap(data, base + STATS_OFFSET, 2).order(ByteOrder.LITTLE_ENDIAN).getShort()
                    & 0xffff;
            out.put(typeid, stack);
        }
        return new IffBallIndex(Map.copyOf(out));
    }

    public static IffBallIndex loadIndex(PangyaIffArchive archive) throws java.io.IOException {
        return loadIndex(archive.readEntry("Ball.iff"));
    }

    private static IffHeader readHeader(byte[] data) {
        if (data.length < IffHeader.BYTES) {
            throw new IllegalArgumentException("Ball.iff too short");
        }
        IffHeader header = IffHeader.read(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN));
        if (header.version() != VERSION) {
            throw new IllegalArgumentException("Ball.iff version " + header.version() + " != " + VERSION);
        }
        return header;
    }
}
