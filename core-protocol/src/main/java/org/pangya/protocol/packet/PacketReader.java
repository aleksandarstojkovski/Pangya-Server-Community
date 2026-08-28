package org.pangya.protocol.packet;

/**
 * Little-endian reader matching C# {@code packet.ReadPStr}/{@code ReadUInt32}/{@code ReadByte}.
 */
public final class PacketReader {

    private final byte[] buf;
    private int pos;

    public PacketReader(byte[] buf) {
        this.buf = buf;
        this.pos = 0;
    }

    public int remaining() {
        return buf.length - pos;
    }

    public int position() {
        return pos;
    }

    public int opcode() {
        return u16();
    }

    public int u8() {
        require(1);
        return buf[pos++] & 0xff;
    }

    public int u16() {
        require(2);
        int v = PacketIo.readU16le(buf, pos);
        pos += 2;
        return v;
    }

    public int i16() {
        return (short) u16();
    }

    public int u32() {
        require(4);
        int v = PacketIo.readU32le(buf, pos);
        pos += 4;
        return v;
    }

    public long u32Unsigned() {
        return u32() & 0xffff_ffffL;
    }

    public String pstr() {
        int len = u16();
        require(len);
        String s = new String(buf, pos, len, PacketIo.SHIFT_JIS);
        pos += len;
        return s;
    }

    public byte[] remainingBytes() {
        byte[] rest = PacketIo.slice(buf, pos, remaining());
        pos = buf.length;
        return rest;
    }

    public byte[] readBytes(int n) {
        require(n);
        byte[] out = PacketIo.slice(buf, pos, n);
        pos += n;
        return out;
    }

    private void require(int n) {
        if (remaining() < n) {
            throw new IllegalArgumentException("packet underrun need=" + n + " remaining=" + remaining());
        }
    }
}
