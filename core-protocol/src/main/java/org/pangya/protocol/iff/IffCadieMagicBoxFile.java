package org.pangya.protocol.iff;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

/**
 * C# {@code IFFFile<CadieMagicBox>} index ({@code Marshal.SizeOf(CadieMagicBox)} = 140 bytes).
 */
public final class IffCadieMagicBoxFile {

    public static final int VERSION = 13;
    public static final int RECORD_BYTES = 140;

    static final int LEVEL_OFFSET = 16;
    static final int RECEIVE_OFFSET = 24;
    static final int TRADE_OFFSET = 32;
    static final int BOX_RANDOM_OFFSET = 64;

    private IffCadieMagicBoxFile() {}

    public static IffCadieMagicBoxIndex loadIndex(byte[] data) {
        IffHeader header = readHeader(data);
        int expected = IffHeader.BYTES + header.count() * RECORD_BYTES;
        if (data.length < expected) {
            throw new IllegalArgumentException("CadieMagicBox.iff truncated: need " + expected + " bytes");
        }
        Map<Integer, IffCadieMagicBoxRecord> out = HashMap.newHashMap(header.count());
        for (int i = 0; i < header.count(); i++) {
            int base = IffHeader.BYTES + i * RECORD_BYTES;
            int seq = le32(data, base);
            int active = le32(data, base + 4);
            if (active == 0) {
                continue;
            }
            int level = le32(data, base + LEVEL_OFFSET);
            int receiveTypeid = le32(data, base + RECEIVE_OFFSET);
            int receiveQntd = le32(data, base + RECEIVE_OFFSET + 4);
            int[] tradeTypeids = new int[IffCadieMagicBoxRecord.TRADE_SLOTS];
            int[] tradeQntds = new int[IffCadieMagicBoxRecord.TRADE_SLOTS];
            for (int slot = 0; slot < IffCadieMagicBoxRecord.TRADE_SLOTS; slot++) {
                tradeTypeids[slot] = le32(data, base + TRADE_OFFSET + slot * 4);
                tradeQntds[slot] = le32(data, base + TRADE_OFFSET + 16 + slot * 4);
            }
            int boxRandomId = le32(data, base + BOX_RANDOM_OFFSET);
            out.put(seq, new IffCadieMagicBoxRecord(
                    seq, true, level, receiveTypeid, receiveQntd, tradeTypeids, tradeQntds, boxRandomId));
        }
        return new IffCadieMagicBoxIndex(Map.copyOf(out));
    }

    private static int le32(byte[] data, int offset) {
        return ByteBuffer.wrap(data, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    public static IffCadieMagicBoxIndex loadIndex(PangyaIffArchive archive) throws java.io.IOException {
        return loadIndex(archive.readEntry("CadieMagicBox.iff"));
    }

    private static IffHeader readHeader(byte[] data) {
        if (data.length < IffHeader.BYTES) {
            throw new IllegalArgumentException("CadieMagicBox.iff too short");
        }
        IffHeader header = IffHeader.read(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN));
        if (header.version() != VERSION) {
            throw new IllegalArgumentException("CadieMagicBox.iff version " + header.version() + " != " + VERSION);
        }
        return header;
    }
}
