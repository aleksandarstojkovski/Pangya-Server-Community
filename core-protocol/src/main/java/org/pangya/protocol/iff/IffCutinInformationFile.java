package org.pangya.protocol.iff;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * C# {@code IFFFile<CutinInformation>} index ({@code CutinInfomation.iff},
 * {@code Marshal.SizeOf(CutinInformation)} = 208 bytes).
 */
public final class IffCutinInformationFile {

    public static final int VERSION = 13;
    public static final int RECORD_BYTES = 208;

    static final int CONDITION_OFFSET = 16;
    static final int SECTOR_OFFSET = 20;
    static final int IMG_OFFSET = 28;
    static final int IMG_BYTES = 44;
    static final int TEMPO_OFFSET = 204;

    private IffCutinInformationFile() {}

    public static IffCutinInformationIndex loadIndex(byte[] data) {
        IffHeader header = readHeader(data);
        int expected = IffHeader.BYTES + header.count() * RECORD_BYTES;
        if (data.length < expected) {
            throw new IllegalArgumentException("CutinInfomation.iff truncated: need " + expected + " bytes");
        }
        Map<Integer, IffCutinInformationRecord> out = HashMap.newHashMap(header.count());
        for (int i = 0; i < header.count(); i++) {
            int base = IffHeader.BYTES + i * RECORD_BYTES;
            int active = ByteBuffer.wrap(data, base, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            if (active == 0) {
                continue;
            }
            int typeid = ByteBuffer.wrap(data, base + 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            int condition = ByteBuffer.wrap(data, base + CONDITION_OFFSET, 4)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .getInt();
            int sector = ByteBuffer.wrap(data, base + SECTOR_OFFSET, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            int[] imageTypes = new int[IffCutinInformationRecord.IMG_COUNT];
            String[] sprites = new String[IffCutinInformationRecord.IMG_COUNT];
            for (int j = 0; j < IffCutinInformationRecord.IMG_COUNT; j++) {
                int imgBase = base + IMG_OFFSET + j * IMG_BYTES;
                sprites[j] = readFixedString(data, imgBase, IffCutinInformationRecord.SPRITE_BYTES);
                imageTypes[j] = ByteBuffer.wrap(data, imgBase + IffCutinInformationRecord.SPRITE_BYTES, 4)
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .getInt();
            }
            int tempo = ByteBuffer.wrap(data, base + TEMPO_OFFSET, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            out.put(typeid, new IffCutinInformationRecord(typeid, sector, condition, imageTypes, tempo, sprites));
        }
        return new IffCutinInformationIndex(Map.copyOf(out));
    }

    public static IffCutinInformationIndex loadIndex(PangyaIffArchive archive) throws java.io.IOException {
        return loadIndex(archive.readEntry("CutinInfomation.iff"));
    }

    private static String readFixedString(byte[] data, int offset, int length) {
        int end = offset;
        int limit = offset + length;
        while (end < limit && data[end] != 0) {
            end++;
        }
        return new String(data, offset, end - offset, StandardCharsets.US_ASCII);
    }

    private static IffHeader readHeader(byte[] data) {
        if (data.length < IffHeader.BYTES) {
            throw new IllegalArgumentException("CutinInfomation.iff too short");
        }
        IffHeader header = IffHeader.read(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN));
        if (header.version() != VERSION) {
            throw new IllegalArgumentException(
                    "CutinInfomation.iff version " + header.version() + " != " + VERSION);
        }
        return header;
    }
}
