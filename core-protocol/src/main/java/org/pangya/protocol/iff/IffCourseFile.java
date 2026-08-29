package org.pangya.protocol.iff;

import org.pangya.protocol.packet.PacketIo;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * C# {@code IFFFile<Course>} parser for {@code Course.iff} v13 records
 * ({@code Marshal.SizeOf(Course)} = 464 bytes).
 */
public final class IffCourseFile {

    public static final int VERSION = 13;
    public static final int RECORD_BYTES = 464;
    private static final Charset SHIFT_JIS = PacketIo.SHIFT_JIS;

    private IffCourseFile() {}

    public static List<IffCourseRecord> parse(byte[] data) {
        if (data.length < IffHeader.BYTES) {
            throw new IllegalArgumentException("Course.iff too short");
        }
        ByteBuffer headerBuf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        IffHeader header = IffHeader.read(headerBuf);
        if (header.version() != VERSION) {
            throw new IllegalArgumentException("Course.iff version " + header.version() + " != " + VERSION);
        }
        int expected = IffHeader.BYTES + header.count() * RECORD_BYTES;
        if (data.length < expected) {
            throw new IllegalArgumentException("Course.iff truncated: need " + expected + " bytes");
        }

        List<IffCourseRecord> rows = new ArrayList<>(header.count());
        for (int i = 0; i < header.count(); i++) {
            int base = IffHeader.BYTES + i * RECORD_BYTES;
            int typeid = ByteBuffer.wrap(data, base + 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            String name = readFixedString(data, base + 8, 64);
            int star = data[base + 272] & 0xff;
            float ratePang = ByteBuffer.wrap(data, base + 316, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
            int[] par = new int[18];
            int[] maxScore = new int[18];
            for (int h = 0; h < 18; h++) {
                par[h] = data[base + 408 + h];
                maxScore[h] = data[base + 444 + h] & 0xff;
            }
            rows.add(new IffCourseRecord(typeid, name, star, ratePang, par, maxScore));
        }
        rows.sort(Comparator.comparingInt(IffCourseRecord::typeid));
        return List.copyOf(rows);
    }

    public static List<IffCourseRecord> load(PangyaIffArchive archive) throws java.io.IOException {
        return parse(archive.readEntry("Course.iff"));
    }

    private static String readFixedString(byte[] data, int offset, int length) {
        int end = offset;
        int limit = offset + length;
        while (end < limit && data[end] != 0) {
            end++;
        }
        return new String(data, offset, end - offset, SHIFT_JIS);
    }
}
