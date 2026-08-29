package org.pangya.protocol.iff;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

/**
 * C# {@code IFFFile<ClubSet>} index ({@code Marshal.SizeOf(ClubSet)} = 260 bytes).
 */
public final class IffClubSetFile {

    public static final int VERSION = 13;
    public static final int RECORD_BYTES = 260;

    static final int STATS_OFFSET = 208;
    static final int SLOT_STATS_OFFSET = 218;
    static final int WORK_SHOP_OFFSET = 228;

    private IffClubSetFile() {}

    public static IffClubSetIndex loadIndex(byte[] data) {
        IffHeader header = readHeader(data);
        int expected = IffHeader.BYTES + header.count() * RECORD_BYTES;
        if (data.length < expected) {
            throw new IllegalArgumentException("ClubSet.iff truncated: need " + expected + " bytes");
        }
        Map<Integer, IffClubSetRecord> out = HashMap.newHashMap(header.count());
        for (int i = 0; i < header.count(); i++) {
            int base = IffHeader.BYTES + i * RECORD_BYTES;
            int active = ByteBuffer.wrap(data, base, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            if (active == 0) {
                continue;
            }
            int typeid = ByteBuffer.wrap(data, base + 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            short[] stats = readShort5(data, base + STATS_OFFSET);
            short[] slots = readShort5(data, base + SLOT_STATS_OFFSET);
            int ws = base + WORK_SHOP_OFFSET;
            int workShopTipo = ByteBuffer.wrap(data, ws, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            int totalRecovery = ByteBuffer.wrap(data, ws + 8, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            int tipoRankS = ByteBuffer.wrap(data, ws + 16, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            int flagTransformar = ByteBuffer.wrap(data, ws + 20, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            out.put(typeid, new IffClubSetRecord(
                    typeid, stats, slots, workShopTipo, tipoRankS, totalRecovery, flagTransformar));
        }
        return new IffClubSetIndex(Map.copyOf(out));
    }

    public static IffClubSetIndex loadIndex(PangyaIffArchive archive) throws java.io.IOException {
        return loadIndex(archive.readEntry("ClubSet.iff"));
    }

    private static short[] readShort5(byte[] data, int offset) {
        short[] out = new short[IffClubSetRecord.STAT_BYTES];
        for (int i = 0; i < out.length; i++) {
            out[i] = ByteBuffer.wrap(data, offset + i * 2, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
        }
        return out;
    }

    private static IffHeader readHeader(byte[] data) {
        if (data.length < IffHeader.BYTES) {
            throw new IllegalArgumentException("ClubSet.iff too short");
        }
        IffHeader header = IffHeader.read(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN));
        if (header.version() != VERSION) {
            throw new IllegalArgumentException("ClubSet.iff version " + header.version() + " != " + VERSION);
        }
        return header;
    }
}
