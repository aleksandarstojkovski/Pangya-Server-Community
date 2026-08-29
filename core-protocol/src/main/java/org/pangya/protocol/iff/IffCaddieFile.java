package org.pangya.protocol.iff;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

/**
 * C# {@code IFFFile<Caddie>} index ({@code Marshal.SizeOf(Caddie)} = 248 bytes).
 */
public final class IffCaddieFile {

    public static final int VERSION = 13;
    public static final int RECORD_BYTES = 248;

    static final int SHOP_FLAG_OFFSET = 128;
    static final int VALOR_MENSAL_OFFSET = 192;

    private IffCaddieFile() {}

    public static IffCaddieIndex loadIndex(byte[] data) {
        IffHeader header = readHeader(data);
        int expected = IffHeader.BYTES + header.count() * RECORD_BYTES;
        if (data.length < expected) {
            throw new IllegalArgumentException("Caddie.iff truncated: need " + expected + " bytes");
        }
        Map<Integer, IffCaddieRecord> out = HashMap.newHashMap(header.count());
        for (int i = 0; i < header.count(); i++) {
            int base = IffHeader.BYTES + i * RECORD_BYTES;
            int active = ByteBuffer.wrap(data, base, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            if (active == 0) {
                continue;
            }
            int typeid = ByteBuffer.wrap(data, base + 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            int shopFlag = data[base + SHOP_FLAG_OFFSET] & 0xff;
            int moneyFlag = data[base + SHOP_FLAG_OFFSET + 1] & 0xff;
            int valorMensal = ByteBuffer.wrap(data, base + VALOR_MENSAL_OFFSET, 4)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .getInt();
            out.put(typeid, new IffCaddieRecord(typeid, valorMensal, new IffShopFlags(shopFlag, moneyFlag)));
        }
        return new IffCaddieIndex(Map.copyOf(out));
    }

    public static IffCaddieIndex loadIndex(PangyaIffArchive archive) throws java.io.IOException {
        return loadIndex(archive.readEntry("Caddie.iff"));
    }

    private static IffHeader readHeader(byte[] data) {
        if (data.length < IffHeader.BYTES) {
            throw new IllegalArgumentException("Caddie.iff too short");
        }
        IffHeader header = IffHeader.read(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN));
        if (header.version() != VERSION) {
            throw new IllegalArgumentException("Caddie.iff version " + header.version() + " != " + VERSION);
        }
        return header;
    }
}
