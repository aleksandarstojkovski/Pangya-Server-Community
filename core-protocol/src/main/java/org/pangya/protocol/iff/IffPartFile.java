package org.pangya.protocol.iff;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * C# {@code IFFFile<Part>} index: reads {@code Part.iff} v13 record typeids
 * ({@code Marshal.SizeOf(Part)} = 568 bytes). Full struct parse not required for
 * {@code initComboDef} / {@code findPart}.
 */
public final class IffPartFile {

    public static final int VERSION = 13;
    public static final int RECORD_BYTES = 568;
    /** C# {@code Part.valor_rental} offset within each record. */
    public static final int VALOR_RENTAL_OFFSET = 560;
    /** C# {@code Part.type_item} after {@code IFFCommon} + {@code MPet}. */
    public static final int TYPE_ITEM_OFFSET = 192 + 40;

    private IffPartFile() {}

    public static IffPartIndex loadIndex(byte[] data) {
        if (data.length < IffHeader.BYTES) {
            throw new IllegalArgumentException("Part.iff too short");
        }
        ByteBuffer headerBuf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        IffHeader header = IffHeader.read(headerBuf);
        if (header.version() != VERSION) {
            throw new IllegalArgumentException("Part.iff version " + header.version() + " != " + VERSION);
        }
        int expected = IffHeader.BYTES + header.count() * RECORD_BYTES;
        if (data.length < expected) {
            throw new IllegalArgumentException("Part.iff truncated: need " + expected + " bytes");
        }

        Set<Integer> ids = HashSet.newHashSet(header.count());
        Map<Integer, Long> rentals = new HashMap<>();
        Map<Integer, Integer> typeItems = new HashMap<>();
        for (int i = 0; i < header.count(); i++) {
            int base = IffHeader.BYTES + i * RECORD_BYTES;
            int typeid = ByteBuffer.wrap(data, base + 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            ids.add(typeid);
            long rental = Integer.toUnsignedLong(
                    ByteBuffer.wrap(data, base + VALOR_RENTAL_OFFSET, 4).order(ByteOrder.LITTLE_ENDIAN).getInt());
            if (rental > 0) {
                rentals.put(typeid, rental);
            }
            int typeItem = ByteBuffer.wrap(data, base + TYPE_ITEM_OFFSET, 4)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .getInt();
            typeItems.put(typeid, typeItem);
        }
        return new IffPartIndex(Set.copyOf(ids), Map.copyOf(rentals), Map.copyOf(typeItems));
    }

    public static IffPartIndex loadIndex(PangyaIffArchive archive) throws java.io.IOException {
        return loadIndex(archive.readEntry("Part.iff"));
    }
}
