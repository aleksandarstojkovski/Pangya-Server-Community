package org.pangya.protocol.iff;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

/**
 * C# {@code IFFFile<ClubSetWorkShopLevelUpProb>} index
 * ({@code Marshal.SizeOf(ClubSetWorkShopLevelUpProb)} = 24 bytes).
 */
public final class IffClubSetWorkShopLevelUpProbFile {

    public static final int VERSION = 13;
    public static final int RECORD_BYTES = 24;

    private IffClubSetWorkShopLevelUpProbFile() {}

    public static IffClubSetWorkShopLevelUpProbIndex loadIndex(byte[] data) {
        IffHeader header = readHeader(data);
        int expected = IffHeader.BYTES + header.count() * RECORD_BYTES;
        if (data.length < expected) {
            throw new IllegalArgumentException(
                    "ClubSetWorkShopLevelUpProb.iff truncated: need " + expected + " bytes");
        }
        Map<Integer, IffClubSetWorkShopLevelUpProbRecord> out = HashMap.newHashMap(header.count());
        for (int i = 0; i < header.count(); i++) {
            int base = IffHeader.BYTES + i * RECORD_BYTES;
            ByteBuffer buf = ByteBuffer.wrap(data, base, RECORD_BYTES).order(ByteOrder.LITTLE_ENDIAN);
            int tipo = buf.getInt();
            int[] c = new int[IffClubSetWorkShopLevelUpProbRecord.STAT_BYTES];
            for (int j = 0; j < c.length; j++) {
                c[j] = buf.getInt();
            }
            out.put(tipo, new IffClubSetWorkShopLevelUpProbRecord(tipo, c));
        }
        return new IffClubSetWorkShopLevelUpProbIndex(Map.copyOf(out));
    }

    public static IffClubSetWorkShopLevelUpProbIndex loadIndex(PangyaIffArchive archive)
            throws java.io.IOException {
        return loadIndex(archive.readEntry("ClubSetWorkShopLevelUpProb.iff"));
    }

    private static IffHeader readHeader(byte[] data) {
        if (data.length < IffHeader.BYTES) {
            throw new IllegalArgumentException("ClubSetWorkShopLevelUpProb.iff too short");
        }
        IffHeader header = IffHeader.read(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN));
        if (header.version() != VERSION) {
            throw new IllegalArgumentException(
                    "ClubSetWorkShopLevelUpProb.iff version " + header.version() + " != " + VERSION);
        }
        return header;
    }
}
