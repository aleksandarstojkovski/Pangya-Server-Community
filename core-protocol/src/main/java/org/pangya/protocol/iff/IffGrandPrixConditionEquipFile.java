package org.pangya.protocol.iff;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

/**
 * C# {@code IFFFile<GrandPrixConditionEquip>} index
 * ({@code Marshal.SizeOf(GrandPrixConditionEquip)} = 528 bytes).
 */
public final class IffGrandPrixConditionEquipFile {

    public static final int VERSION = 13;
    public static final int RECORD_BYTES = 528;

    private IffGrandPrixConditionEquipFile() {}

    public static IffGrandPrixConditionEquipIndex loadIndex(byte[] data) {
        IffHeader header = readHeader(data);
        int expected = IffHeader.BYTES + header.count() * RECORD_BYTES;
        if (data.length < expected) {
            throw new IllegalArgumentException(
                    "GrandPrixConditionEquip.iff truncated: need " + expected + " bytes");
        }
        Map<Integer, IffGrandPrixConditionEquipRecord> out = HashMap.newHashMap(header.count());
        for (int i = 0; i < header.count(); i++) {
            int base = IffHeader.BYTES + i * RECORD_BYTES;
            int active = ByteBuffer.wrap(data, base, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            if (active == 0) {
                continue;
            }
            int typeidLink = ByteBuffer.wrap(data, base + 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            int itemTypeid = ByteBuffer.wrap(data, base + 8, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            out.put(typeidLink, new IffGrandPrixConditionEquipRecord(typeidLink, itemTypeid));
        }
        return new IffGrandPrixConditionEquipIndex(Map.copyOf(out));
    }

    public static IffGrandPrixConditionEquipIndex loadIndex(PangyaIffArchive archive)
            throws java.io.IOException {
        return loadIndex(archive.readEntry("GrandPrixConditionEquip.iff"));
    }

    private static IffHeader readHeader(byte[] data) {
        if (data.length < IffHeader.BYTES) {
            throw new IllegalArgumentException("GrandPrixConditionEquip.iff too short");
        }
        IffHeader header = IffHeader.read(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN));
        if (header.version() != VERSION) {
            throw new IllegalArgumentException(
                    "GrandPrixConditionEquip.iff version " + header.version() + " != " + VERSION);
        }
        return header;
    }
}
