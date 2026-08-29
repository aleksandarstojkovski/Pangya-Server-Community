package org.pangya.protocol.iff;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * C# {@code IFFFile<CadieMagicBoxRandom>} index
 * ({@code Marshal.SizeOf(CadieMagicBoxRandom)} = 16 bytes).
 */
public final class IffCadieMagicBoxRandomFile {

    public static final int VERSION = 13;
    public static final int RECORD_BYTES = 16;

    private IffCadieMagicBoxRandomFile() {}

    public static IffCadieMagicBoxRandomIndex loadIndex(byte[] data) {
        IffHeader header = readHeader(data);
        int expected = IffHeader.BYTES + header.count() * RECORD_BYTES;
        if (data.length < expected) {
            throw new IllegalArgumentException("CadieMagicBoxRandom.iff truncated: need " + expected + " bytes");
        }
        Map<Integer, List<IffCadieMagicBoxRandomRecord>> grouped = HashMap.newHashMap(header.count());
        for (int i = 0; i < header.count(); i++) {
            int base = IffHeader.BYTES + i * RECORD_BYTES;
            int groupId = le32(data, base);
            int itemTypeid = le32(data, base + 4);
            int qty = le32(data, base + 8);
            int rate = le32(data, base + 12);
            grouped.computeIfAbsent(groupId, k -> new ArrayList<>())
                    .add(new IffCadieMagicBoxRandomRecord(groupId, itemTypeid, qty, rate));
        }
        Map<Integer, List<IffCadieMagicBoxRandomRecord>> frozen = HashMap.newHashMap(grouped.size());
        grouped.forEach((id, rows) -> frozen.put(id, List.copyOf(rows)));
        return new IffCadieMagicBoxRandomIndex(Map.copyOf(frozen));
    }

    public static IffCadieMagicBoxRandomIndex loadIndex(PangyaIffArchive archive) throws java.io.IOException {
        return loadIndex(archive.readEntry("CadieMagicBoxRandom.iff"));
    }

    private static int le32(byte[] data, int offset) {
        return ByteBuffer.wrap(data, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    private static IffHeader readHeader(byte[] data) {
        if (data.length < IffHeader.BYTES) {
            throw new IllegalArgumentException("CadieMagicBoxRandom.iff too short");
        }
        IffHeader header = IffHeader.read(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN));
        if (header.version() != VERSION) {
            throw new IllegalArgumentException(
                    "CadieMagicBoxRandom.iff version " + header.version() + " != " + VERSION);
        }
        return header;
    }
}
