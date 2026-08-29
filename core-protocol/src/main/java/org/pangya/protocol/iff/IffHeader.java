package org.pangya.protocol.iff;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** C# {@code IFFHeader}: count, binding id, version (8 bytes LE). */
public record IffHeader(int count, int bindingId, int version) {

    public static final int BYTES = 8;

    public static IffHeader read(ByteBuffer buf) {
        buf.order(ByteOrder.LITTLE_ENDIAN);
        int count = buf.getShort() & 0xffff;
        int binding = buf.getShort() & 0xffff;
        int version = buf.getInt();
        return new IffHeader(count, binding, version);
    }
}
