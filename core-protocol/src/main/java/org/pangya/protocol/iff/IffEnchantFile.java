package org.pangya.protocol.iff;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

/**
 * C# {@code IFFFile<Enchant>} index ({@code Marshal.SizeOf(Enchant)} = 16 bytes).
 */
public final class IffEnchantFile {

    public static final int VERSION = 13;
    public static final int RECORD_BYTES = 16;

    private IffEnchantFile() {}

    public static IffEnchantIndex loadIndex(byte[] data) {
        IffHeader header = readHeader(data);
        int expected = IffHeader.BYTES + header.count() * RECORD_BYTES;
        if (data.length < expected) {
            throw new IllegalArgumentException("Enchant.iff truncated: need " + expected + " bytes");
        }
        Map<Integer, IffEnchantRecord> out = HashMap.newHashMap(header.count());
        for (int i = 0; i < header.count(); i++) {
            int base = IffHeader.BYTES + i * RECORD_BYTES;
            ByteBuffer buf = ByteBuffer.wrap(data, base, RECORD_BYTES).order(ByteOrder.LITTLE_ENDIAN);
            int enable = buf.getInt();
            int typeid = buf.getInt();
            long pang = buf.getLong();
            out.put(typeid, new IffEnchantRecord(typeid, pang, enable != 0));
        }
        return new IffEnchantIndex(Map.copyOf(out));
    }

    public static IffEnchantIndex loadIndex(PangyaIffArchive archive) throws java.io.IOException {
        return loadIndex(archive.readEntry("Enchant.iff"));
    }

    private static IffHeader readHeader(byte[] data) {
        if (data.length < IffHeader.BYTES) {
            throw new IllegalArgumentException("Enchant.iff too short");
        }
        IffHeader header = IffHeader.read(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN));
        if (header.version() != VERSION) {
            throw new IllegalArgumentException("Enchant.iff version " + header.version() + " != " + VERSION);
        }
        return header;
    }
}
