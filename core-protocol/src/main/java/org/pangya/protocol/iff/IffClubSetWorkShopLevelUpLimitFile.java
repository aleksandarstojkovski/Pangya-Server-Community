package org.pangya.protocol.iff;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * C# {@code IFFFile<ClubSetWorkShopLevelUpLimit>} index
 * ({@code Marshal.SizeOf(ClubSetWorkShopLevelUpLimit)} = 20 bytes).
 */
public final class IffClubSetWorkShopLevelUpLimitFile {

    public static final int VERSION = 13;
    public static final int RECORD_BYTES = 20;

    private IffClubSetWorkShopLevelUpLimitFile() {}

    public static IffClubSetWorkShopLevelUpLimitIndex loadIndex(byte[] data) {
        IffHeader header = readHeader(data);
        int expected = IffHeader.BYTES + header.count() * RECORD_BYTES;
        if (data.length < expected) {
            throw new IllegalArgumentException(
                    "ClubSetWorkShopLevelUpLimit.iff truncated: need " + expected + " bytes");
        }
        Map<Integer, List<IffClubSetWorkShopLevelUpLimitRecord>> grouped = HashMap.newHashMap(header.count());
        for (int i = 0; i < header.count(); i++) {
            int base = IffHeader.BYTES + i * RECORD_BYTES;
            ByteBuffer buf = ByteBuffer.wrap(data, base, RECORD_BYTES).order(ByteOrder.LITTLE_ENDIAN);
            int tipo = buf.getInt();
            int rank = buf.getInt();
            short[] c = new short[IffClubSetWorkShopLevelUpLimitRecord.STAT_BYTES];
            for (int j = 0; j < c.length; j++) {
                c[j] = buf.getShort();
            }
            int option = buf.getShort() & 0xffff;
            grouped.computeIfAbsent(tipo, k -> new ArrayList<>())
                    .add(new IffClubSetWorkShopLevelUpLimitRecord(tipo, rank, c, option));
        }
        Map<Integer, List<IffClubSetWorkShopLevelUpLimitRecord>> frozen = HashMap.newHashMap(grouped.size());
        grouped.forEach((tipo, rows) -> {
            rows.sort(Comparator.comparingInt(IffClubSetWorkShopLevelUpLimitRecord::rank));
            frozen.put(tipo, List.copyOf(rows));
        });
        return new IffClubSetWorkShopLevelUpLimitIndex(Map.copyOf(frozen));
    }

    public static IffClubSetWorkShopLevelUpLimitIndex loadIndex(PangyaIffArchive archive)
            throws java.io.IOException {
        return loadIndex(archive.readEntry("ClubSetWorkShopLevelUpLimit.iff"));
    }

    private static IffHeader readHeader(byte[] data) {
        if (data.length < IffHeader.BYTES) {
            throw new IllegalArgumentException("ClubSetWorkShopLevelUpLimit.iff too short");
        }
        IffHeader header = IffHeader.read(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN));
        if (header.version() != VERSION) {
            throw new IllegalArgumentException(
                    "ClubSetWorkShopLevelUpLimit.iff version " + header.version() + " != " + VERSION);
        }
        return header;
    }
}
