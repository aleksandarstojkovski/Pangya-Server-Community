package org.pangya.protocol.iff;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

/**
 * C# {@code IFFFile<Item>} index ({@code Marshal.SizeOf(Item)} = 248 bytes).
 */
public final class IffItemFile {

    public static final int VERSION = 13;
    public static final int RECORD_BYTES = 248;

    /** C# {@code IFFCommon.Shop} → {@code FlagShop.ShopFlag}. */
    static final int SHOP_FLAG_OFFSET = 128;
    /** C# {@code Item.Stats.Power} = {@code getSlot[0]}. */
    static final int STATS_POWER_OFFSET = 192 + 4 + 40;

    private IffItemFile() {}

    public static IffItemIndex loadIndex(byte[] data) {
        IffHeader header = readHeader(data);
        int expected = IffHeader.BYTES + header.count() * RECORD_BYTES;
        if (data.length < expected) {
            throw new IllegalArgumentException("Item.iff truncated: need " + expected + " bytes");
        }
        Map<Integer, IffItemRecord> out = HashMap.newHashMap(header.count());
        for (int i = 0; i < header.count(); i++) {
            int base = IffHeader.BYTES + i * RECORD_BYTES;
            int typeid = ByteBuffer.wrap(data, base + 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            int shopFlag = data[base + SHOP_FLAG_OFFSET] & 0xff;
            int moneyFlag = data[base + SHOP_FLAG_OFFSET + 1] & 0xff;
            int statsPower = ByteBuffer.wrap(data, base + STATS_POWER_OFFSET, 2)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .getShort() & 0xffff;
            out.put(typeid, new IffItemRecord(typeid, new IffShopFlags(shopFlag, moneyFlag), statsPower));
        }
        return new IffItemIndex(Map.copyOf(out));
    }

    public static IffItemIndex loadIndex(PangyaIffArchive archive) throws java.io.IOException {
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
