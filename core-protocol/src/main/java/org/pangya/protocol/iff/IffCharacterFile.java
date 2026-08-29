package org.pangya.protocol.iff;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

/**
 * C# {@code IFFFile<Character>} index ({@code Marshal.SizeOf(Character)} = 420 bytes).
 */
public final class IffCharacterFile {

    public static final int VERSION = 13;
    public static final int RECORD_BYTES = 420;

    private IffCharacterFile() {}

    public static Map<Integer, IffCharacterRecord> loadIndex(byte[] data) {
        IffHeader header = readHeader(data);
        int expected = IffHeader.BYTES + header.count() * RECORD_BYTES;
        if (data.length < expected) {
            throw new IllegalArgumentException("Character.iff truncated: need " + expected + " bytes");
        }
        Map<Integer, IffCharacterRecord> out = HashMap.newHashMap(header.count());
        for (int i = 0; i < header.count(); i++) {
            int base = IffHeader.BYTES + i * RECORD_BYTES;
            int typeid = ByteBuffer.wrap(data, base + 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            int[] pcl = new int[IffCharacterRecord.PCL_BYTES];
            for (int s = 0; s < pcl.length; s++) {
                pcl[s] = data[base + IffCharacterRecord.PCL_OFFSET + s] & 0xff;
            }
            out.put(typeid, new IffCharacterRecord(typeid, pcl));
        }
        return Map.copyOf(out);
    }

    public static Map<Integer, IffCharacterRecord> loadIndex(PangyaIffArchive archive)
            throws java.io.IOException {
        return loadIndex(archive.readEntry("Character.iff"));
    }

    private static IffHeader readHeader(byte[] data) {
        if (data.length < IffHeader.BYTES) {
            throw new IllegalArgumentException("Character.iff too short");
        }
        IffHeader header = IffHeader.read(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN));
        if (header.version() != VERSION) {
            throw new IllegalArgumentException("Character.iff version " + header.version() + " != " + VERSION);
        }
        return header;
    }
}
