package org.pangya.protocol.iff;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashSet;
import java.util.Set;

/**
 * C# {@code IFFFile<Item>} index ({@code Marshal.SizeOf(Item)} = 248 bytes).
 */
public final class IffItemFile {

    public static final int VERSION = 13;
    public static final int RECORD_BYTES = 248;

    private IffItemFile() {}

    public static IffTypeIndex loadIndex(byte[] data) {
        IffHeader header = readHeader(data);
        int expected = IffHeader.BYTES + header.count() * RECORD_BYTES;
        if (data.length < expected) {
            throw new IllegalArgumentException("Item.iff truncated: need " + expected + " bytes");
        }
        Set<Integer> ids = HashSet.newHashSet(header.count());
        for (int i = 0; i < header.count(); i++) {
            int base = IffHeader.BYTES + i * RECORD_BYTES;
            int typeid = ByteBuffer.wrap(data, base + 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            ids.add(typeid);
        }
        return new IffTypeIndex(Set.copyOf(ids));
    }

    public static IffTypeIndex loadIndex(PangyaIffArchive archive) throws java.io.IOException {
        return loadIndex(archive.readEntry("Item.iff"));
    }

    private static IffHeader readHeader(byte[] data) {
        if (data.length < IffHeader.BYTES) {
            throw new IllegalArgumentException("Item.iff too short");
        }
        IffHeader header = IffHeader.read(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN));
        if (header.version() != VERSION) {
            throw new IllegalArgumentException("Item.iff version " + header.version() + " != " + VERSION);
        }
        return header;
    }
}
