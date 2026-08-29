package org.pangya.protocol.iff;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * C# {@code IFFFile<GrandPrixRankReward>} index ({@code Marshal.SizeOf(GrandPrixRankReward)} = 76 bytes).
 */
public final class IffGrandPrixRankRewardFile {

    public static final int VERSION = 13;
    public static final int RECORD_BYTES = 76;

    static final int ID_OFFSET = 4;
    static final int RANK_OFFSET = 8;
    static final int REWARD_OFFSET = 12;
    static final int TROPHY_OFFSET = 72;

    private IffGrandPrixRankRewardFile() {}

    public static IffGrandPrixRankRewardIndex loadIndex(byte[] data) {
        IffHeader header = readHeader(data);
        int expected = IffHeader.BYTES + header.count() * RECORD_BYTES;
        if (data.length < expected) {
            throw new IllegalArgumentException("GrandPrixRankReward.iff truncated: need " + expected + " bytes");
        }
        Map<Integer, List<IffGrandPrixRankRewardRecord>> out = HashMap.newHashMap(header.count());
        int rows = 0;
        for (int i = 0; i < header.count(); i++) {
            int base = IffHeader.BYTES + i * RECORD_BYTES;
            int active = ByteBuffer.wrap(data, base, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            if (active == 0) {
                continue;
            }
            int typeIdLink = ByteBuffer.wrap(data, base + ID_OFFSET, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            int rank = ByteBuffer.wrap(data, base + RANK_OFFSET, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            IffRewardSlots reward = IffRewardSlots.read(data, base + REWARD_OFFSET);
            int trophy = ByteBuffer.wrap(data, base + TROPHY_OFFSET, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            out.computeIfAbsent(typeIdLink, key -> new ArrayList<>())
                    .add(new IffGrandPrixRankRewardRecord(typeIdLink, rank, reward, trophy));
            rows++;
        }
        for (List<IffGrandPrixRankRewardRecord> list : out.values()) {
            list.sort(Comparator.comparingInt(IffGrandPrixRankRewardRecord::rank));
        }
        return new IffGrandPrixRankRewardIndex(Map.copyOf(out), rows);
    }

    public static IffGrandPrixRankRewardIndex loadIndex(PangyaIffArchive archive) throws java.io.IOException {
        return loadIndex(archive.readEntry("GrandPrixRankReward.iff"));
    }

    private static IffHeader readHeader(byte[] data) {
        if (data.length < IffHeader.BYTES) {
            throw new IllegalArgumentException("GrandPrixRankReward.iff too short");
        }
        IffHeader header = IffHeader.read(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN));
        if (header.version() != VERSION) {
            throw new IllegalArgumentException(
                    "GrandPrixRankReward.iff version " + header.version() + " != " + VERSION);
        }
        return header;
    }
}
