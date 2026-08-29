package org.pangya.protocol.iff;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Optional;

/** C# {@code IFFCommon.Name}: 64-byte Shift_JIS name at record offset 8. */
public final class IffCommonFile {

    private static final Charset SHIFT_JIS = Charset.forName("Shift_JIS");
    static final int NAME_OFFSET = 8;
    static final int NAME_BYTES = 64;
    static final int TYPEID_OFFSET = 4;

    private IffCommonFile() {}

    public static Optional<String> nameForTypeid(byte[] data, int recordBytes, int version, int typeid) {
        if (data == null || data.length < IffHeader.BYTES || recordBytes <= TYPEID_OFFSET) {
            return Optional.empty();
        }
        IffHeader header = readHeader(data);
        if (header.version() != version) {
            return Optional.empty();
        }
        int expected = IffHeader.BYTES + header.count() * recordBytes;
        if (data.length < expected) {
            return Optional.empty();
        }
        for (int i = 0; i < header.count(); i++) {
            int base = IffHeader.BYTES + i * recordBytes;
            int active = ByteBuffer.wrap(data, base, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            if (active == 0) {
                continue;
            }
            int rowTypeid = ByteBuffer.wrap(data, base + TYPEID_OFFSET, 4)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .getInt();
            if (rowTypeid != typeid) {
                continue;
            }
            return Optional.of(decodeName(data, base + NAME_OFFSET));
        }
        return Optional.empty();
    }

    public static IffTypeIndex loadTypeIndex(byte[] data, int recordBytes, int version) {
        if (data == null || data.length < IffHeader.BYTES) {
            return IffTypeIndex.empty();
        }
        IffHeader header = readHeader(data);
        if (header.version() != version) {
            return IffTypeIndex.empty();
        }
        java.util.Set<Integer> typeids = new java.util.HashSet<>();
        for (int i = 0; i < header.count(); i++) {
            int base = IffHeader.BYTES + i * recordBytes;
            int active = ByteBuffer.wrap(data, base, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            if (active == 0) {
                continue;
            }
            int typeid = ByteBuffer.wrap(data, base + TYPEID_OFFSET, 4)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .getInt();
            typeids.add(typeid);
        }
        return new IffTypeIndex(java.util.Set.copyOf(typeids));
    }

    private static String decodeName(byte[] data, int offset) {
        int end = offset;
        int limit = Math.min(data.length, offset + NAME_BYTES);
        while (end < limit && data[end] != 0) {
            end++;
        }
        return SHIFT_JIS.decode(java.nio.ByteBuffer.wrap(data, offset, end - offset)).toString().trim();
    }

    private static IffHeader readHeader(byte[] data) {
        return IffHeader.read(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN));
    }
}
