package org.pangya.protocol.iff;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

/**
 * C# {@code IFFFile<SetItem>} index ({@code Marshal.SizeOf(SetItem)} = 268 bytes).
 */
public final class IffSetItemFile {

    public static final int VERSION = 13;
    public static final int RECORD_BYTES = 268;

    /** C# {@code Packege} at offset 192 inside {@code IFFCommon}. */
    static final int PACKAGE_OFFSET = 192;
    static final int POINT_OFFSET = 266;

    private IffSetItemFile() {}

    public static IffSetItemIndex loadIndex(byte[] data) {
        IffHeader header = readHeader(data);
        int expected = IffHeader.BYTES + header.count() * RECORD_BYTES;
        if (data.length < expected) {
            throw new IllegalArgumentException("SetItem.iff truncated: need " + expected + " bytes");
        }
        Map<Integer, IffSetItemRecord> out = HashMap.newHashMap(header.count());
        for (int i = 0; i < header.count(); i++) {
            int base = IffHeader.BYTES + i * RECORD_BYTES;
            int active = ByteBuffer.wrap(data, base, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            if (active == 0) {
                continue;
            }
            int typeid = ByteBuffer.wrap(data, base + 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            int total = ByteBuffer.wrap(data, base + PACKAGE_OFFSET, 4)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .getInt();
            int[] itemTypeids = new int[IffSetItemPackage.MAX_ITEMS];
            int[] itemQntds = new int[IffSetItemPackage.MAX_ITEMS];
            for (int slot = 0; slot < IffSetItemPackage.MAX_ITEMS; slot++) {
                itemTypeids[slot] = ByteBuffer.wrap(data, base + PACKAGE_OFFSET + 4 + slot * 4, 4)
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .getInt();
                itemQntds[slot] = ByteBuffer.wrap(data, base + PACKAGE_OFFSET + 44 + slot * 2, 2)
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .getShort() & 0xffff;
            }
            int point = ByteBuffer.wrap(data, base + POINT_OFFSET, 2)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .getShort() & 0xffff;
            out.put(typeid, new IffSetItemRecord(typeid, new IffSetItemPackage(total, itemTypeids, itemQntds), point));
        }
        return new IffSetItemIndex(Map.copyOf(out));
    }

    public static IffSetItemIndex loadIndex(PangyaIffArchive archive) throws java.io.IOException {
        return loadIndex(archive.readEntry("SetItem.iff"));
    }

    private static IffHeader readHeader(byte[] data) {
        if (data.length < IffHeader.BYTES) {
            throw new IllegalArgumentException("SetItem.iff too short");
        }
        IffHeader header = IffHeader.read(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN));
        if (header.version() != VERSION) {
            throw new IllegalArgumentException("SetItem.iff version " + header.version() + " != " + VERSION);
        }
        return header;
    }
}
