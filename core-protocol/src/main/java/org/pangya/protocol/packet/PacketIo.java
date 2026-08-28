package org.pangya.protocol.packet;

import java.nio.charset.Charset;
import java.util.Arrays;

/** Little-endian packet body helpers matching C# {@code PangyaBinaryWriter}. */
public final class PacketIo {

    public static final Charset SHIFT_JIS = Charset.forName("Shift_JIS");

    private PacketIo() {}

    public static byte[] u16le(int value) {
        return new byte[] {(byte) value, (byte) (value >>> 8)};
    }

    public static byte[] u32le(int value) {
        return new byte[] {
                (byte) value,
                (byte) (value >>> 8),
                (byte) (value >>> 16),
                (byte) (value >>> 24)
        };
    }

    public static int readU16le(byte[] buf, int offset) {
        return (buf[offset] & 0xff) | ((buf[offset + 1] & 0xff) << 8);
    }

    public static int readU32le(byte[] buf, int offset) {
        return (buf[offset] & 0xff)
                | ((buf[offset + 1] & 0xff) << 8)
                | ((buf[offset + 2] & 0xff) << 16)
                | ((buf[offset + 3] & 0xff) << 24);
    }

    public static byte[] pstr(String value) {
        byte[] bytes = (value == null ? "" : value).getBytes(SHIFT_JIS);
        if (bytes.length > 0xffff) {
            throw new IllegalArgumentException("PStr too long");
        }
        byte[] out = new byte[2 + bytes.length];
        out[0] = (byte) bytes.length;
        out[1] = (byte) (bytes.length >>> 8);
        System.arraycopy(bytes, 0, out, 2, bytes.length);
        return out;
    }

    public static byte[] concat(byte[]... parts) {
        int n = 0;
        for (byte[] p : parts) {
            n += p.length;
        }
        byte[] out = new byte[n];
        int i = 0;
        for (byte[] p : parts) {
            System.arraycopy(p, 0, out, i, p.length);
            i += p.length;
        }
        return out;
    }

    public static byte[] opcode(int opcode) {
        return u16le(opcode);
    }

    public static byte[] makeRaw(byte[] payload) {
        // packet_head: low_key=0, size=payload.length+1 (LE), then raw flag 0, then payload.
        int size = payload.length + 1;
        byte[] out = new byte[3 + 1 + payload.length];
        out[0] = 0;
        out[1] = (byte) size;
        out[2] = (byte) (size >>> 8);
        out[3] = 0;
        System.arraycopy(payload, 0, out, 4, payload.length);
        return out;
    }

    /** Default Login {@code GUID} used by Java listen config (JP INI is 10903). */
    public static final int DEFAULT_LOGIN_UID = 10203;

    /**
     * JP {@code LoginServer.onAcceptCompleted}: {@code makeRaw} opcode {@code 0x00}
     * + int32 session key + int32 server uid.
     */
    public static byte[] loginHello(int key) {
        return loginHello(key, DEFAULT_LOGIN_UID);
    }

    public static byte[] loginHello(int key, int serverUid) {
        return makeRaw(concat(opcode(0x00), u32le(key), u32le(serverUid)));
    }

    /** GameServer onAcceptCompleted: raw 0x3F + options + key + PStr(ip). */
    public static byte[] gameHello(int key, String ip) {
        byte[] payload = concat(
                opcode(0x3F),
                new byte[] {1, 1, (byte) key},
                pstr(ip)
        );
        return makeRaw(payload);
    }

    /**
     * RankingServer onAcceptCompleted: raw {@code 0x1388} + int32 key + byte 5 + PStr(epoch).
     * JP {@code UtilTime.formatDateLocal(0)} is {@code yyyy-MM-dd HH:mm:ss} (no millis).
     */
    public static final String FORMAT_DATE_EPOCH = "1970-01-01 00:00:00";
    public static final String RANKING_EPOCH = FORMAT_DATE_EPOCH;

    public static byte[] rankingHello(int key) {
        byte[] payload = concat(
                opcode(0x1388),
                u32le(key),
                new byte[] {5},
                pstr(RANKING_EPOCH)
        );
        return makeRaw(payload);
    }

    /** MessengerServer onAcceptCompleted: raw {@code 0x2E} + byte 1 + byte 1 + uint32 key. */
    public static byte[] messengerHello(int key) {
        byte[] payload = concat(
                opcode(0x2E),
                new byte[] {1, 1},
                u32le(key)
        );
        return makeRaw(payload);
    }

    public static int clientFrameLength(byte[] buf, int offset) {
        int lenField = readU16le(buf, offset + 1);
        return lenField + 4;
    }

    public static int serverFrameLength(byte[] buf, int offset) {
        int lenField = readU16le(buf, offset + 1);
        return lenField + 3;
    }

    public static byte[] slice(byte[] buf, int from, int len) {
        return Arrays.copyOfRange(buf, from, from + len);
    }
}
