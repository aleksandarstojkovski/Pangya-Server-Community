package org.pangya.protocol.iff;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

/**
 * C# {@code IFFFile<ClubSetWorkShopRankUpExp>} index
 * ({@code Marshal.SizeOf(ClubSetWorkShopRankUpExp)} = 28 bytes).
 */
public final class IffClubSetWorkShopRankUpExpFile {

    public static final int VERSION = 13;
    public static final int RECORD_BYTES = 28;

    private IffClubSetWorkShopRankUpExpFile() {}

    public static IffClubSetWorkShopRankUpExpIndex loadIndex(byte[] data) {
        IffHeader header = readHeader(data);
        int expected = IffHeader.BYTES + header.count() * RECORD_BYTES;
        if (data.length < expected) {
            throw new IllegalArgumentException(
                    "ClubSetWorkShopRankUpExp.iff truncated: need " + expected + " bytes");
        }
        Map<Integer, IffClubSetWorkShopRankUpExpRecord> out = HashMap.newHashMap(header.count());
        for (int i = 0; i < header.count(); i++) {
            int base = IffHeader.BYTES + i * RECORD_BYTES;
            ByteBuffer buf = ByteBuffer.wrap(data, base, RECORD_BYTES).order(ByteOrder.LITTLE_ENDIAN);
            int tipo = buf.getInt();
            int[] rank = new int[IffClubSetWorkShopRankUpExpRecord.RANK_BYTES];
            for (int j = 0; j < rank.length; j++) {
                rank[j] = buf.getInt();
            }
            out.put(tipo, new IffClubSetWorkShopRankUpExpRecord(tipo, rank));
        }
        return new IffClubSetWorkShopRankUpExpIndex(Map.copyOf(out));
    }

    public static IffClubSetWorkShopRankUpExpIndex loadIndex(PangyaIffArchive archive)
            throws java.io.IOException {
        return loadIndex(archive.readEntry("ClubSetWorkShopRankUpExp.iff"));
    }

    private static IffHeader readHeader(byte[] data) {
        if (data.length < IffHeader.BYTES) {
            throw new IllegalArgumentException("ClubSetWorkShopRankUpExp.iff too short");
        }
        IffHeader header = IffHeader.read(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN));
        if (header.version() != VERSION) {
            throw new IllegalArgumentException(
                    "ClubSetWorkShopRankUpExp.iff version " + header.version() + " != " + VERSION);
        }
        return header;
    }
}
