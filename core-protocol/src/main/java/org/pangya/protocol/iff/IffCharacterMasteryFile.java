package org.pangya.protocol.iff;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * C# {@code IFFFile<CharacterMastery>} index ({@code Marshal.SizeOf(CharacterMastery)} = 60 bytes).
 */
public final class IffCharacterMasteryFile {

    public static final int VERSION = 13;
    public static final int RECORD_BYTES = 60;

    private IffCharacterMasteryFile() {}

    public static IffCharacterMasteryIndex loadIndex(byte[] data) {
        IffHeader header = readHeader(data);
        int expected = IffHeader.BYTES + header.count() * RECORD_BYTES;
        if (data.length < expected) {
            throw new IllegalArgumentException("CharacterMastery.iff truncated: need " + expected + " bytes");
        }
        Map<Integer, List<IffCharacterMasteryRecord>> grouped = HashMap.newHashMap(header.count());
        for (int i = 0; i < header.count(); i++) {
            int base = IffHeader.BYTES + i * RECORD_BYTES;
            ByteBuffer buf = ByteBuffer.wrap(data, base, RECORD_BYTES).order(ByteOrder.LITTLE_ENDIAN);
            int active = buf.getInt();
            if (active == 0) {
                continue;
            }
            int typeid = buf.getInt();
            int seq = buf.getInt();
            int stats = buf.getInt();
            int level = buf.getInt();
            int[] condTypeid = new int[IffCharacterMasteryRecord.CONDITION_SLOTS];
            int[] condQntd = new int[IffCharacterMasteryRecord.CONDITION_SLOTS];
            for (int slot = 0; slot < IffCharacterMasteryRecord.CONDITION_SLOTS; slot++) {
                condTypeid[slot] = buf.getInt();
            }
            for (int slot = 0; slot < IffCharacterMasteryRecord.CONDITION_SLOTS; slot++) {
                condQntd[slot] = buf.getInt();
            }
            grouped.computeIfAbsent(typeid, k -> new ArrayList<>())
                    .add(new IffCharacterMasteryRecord(typeid, seq, stats, level, condTypeid, condQntd));
        }
        Map<Integer, List<IffCharacterMasteryRecord>> frozen = HashMap.newHashMap(grouped.size());
        grouped.forEach((typeid, rows) -> {
            rows.sort(Comparator.comparingInt(IffCharacterMasteryRecord::seq));
            frozen.put(typeid, List.copyOf(rows));
        });
        return new IffCharacterMasteryIndex(Map.copyOf(frozen));
    }

    public static IffCharacterMasteryIndex loadIndex(PangyaIffArchive archive) throws java.io.IOException {
        return loadIndex(archive.readEntry("CharacterMastery.iff"));
    }

    private static IffHeader readHeader(byte[] data) {
        if (data.length < IffHeader.BYTES) {
            throw new IllegalArgumentException("CharacterMastery.iff too short");
        }
        IffHeader header = IffHeader.read(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN));
        if (header.version() != VERSION) {
            throw new IllegalArgumentException(
                    "CharacterMastery.iff version " + header.version() + " != " + VERSION);
        }
        return header;
    }
}
