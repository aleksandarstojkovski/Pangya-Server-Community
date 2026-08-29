package org.pangya.protocol.crypto;

import java.util.Arrays;

/**
 * Bit-compatible port of {@code PangyaAPI.Network.Cryptor.Cipher}.
 */
public final class Cipher {

    private Cipher() {}

    public static byte[] decryptClient(byte[] source, int key) {
        if (key < 0 || key >= 0x10) {
            throw new IllegalArgumentException("cryptography key is too big: " + key);
        }
        if (source.length < 5) {
            throw new IllegalArgumentException("packet too small to decrypt (" + source.length + " < 5)");
        }
        byte[] buffer = Arrays.copyOf(source, source.length);
        int idx = CryptoOracle.oracleIndex(key, source[0] & 0xff);
        buffer[4] = CryptoOracle.PRIVATE_KEY_TABLE[idx];
        for (int i = 8; i < buffer.length; i++) {
            buffer[i] ^= buffer[i - 4];
        }
        return Arrays.copyOfRange(buffer, 5, buffer.length);
    }

    /**
     * Inverse of {@link #decryptClient} for fake clients and tests. Not present as a
     * named method in C# live send path (clients use their own encoder).
     */
    public static byte[] encryptClient(byte[] plain, int key, int salt) {
        if (key < 0 || key >= 0x10) {
            throw new IllegalArgumentException("key too large: " + key);
        }
        byte[] buffer = new byte[plain.length + 5];
        int pLen = buffer.length - 4;
        buffer[0] = (byte) salt;
        buffer[1] = (byte) pLen;
        buffer[2] = (byte) (pLen >>> 8);
        int idx = CryptoOracle.oracleIndex(key, salt);
        buffer[4] = CryptoOracle.PRIVATE_KEY_TABLE[idx];
        System.arraycopy(plain, 0, buffer, 5, plain.length);
        for (int i = buffer.length - 1; i >= 8; i--) {
            buffer[i] ^= buffer[i - 4];
        }
        return buffer;
    }

    public static byte[] decryptServer(byte[] source, int key) {
        if (key < 0 || key >= 0x10) {
            throw new IllegalArgumentException("key too large: " + key);
        }
        if (source.length < 8) {
            return source;
        }
        int idx = CryptoOracle.oracleIndex(key, source[0] & 0xff);
        byte oracleByte = CryptoOracle.PRIVATE_KEY_TABLE[idx];
        byte[] buffer = Arrays.copyOf(source, source.length);
        buffer[7] ^= oracleByte;
        for (int i = 10; i < source.length; i++) {
            buffer[i] ^= buffer[i - 4];
        }
        byte[] compressed = Arrays.copyOfRange(buffer, 8, source.length);
        try {
            return MiniLzo.decompress(compressed);
        } catch (RuntimeException e) {
            return source;
        }
    }

    public static byte[] serverEncrypt(byte[] source, int key, int salt) {
        if (key < 0 || key >= 0x10) {
            throw new IllegalArgumentException("key too large: " + key);
        }
        int oracleIndex = CryptoOracle.oracleIndex(key, salt);
        byte[] compressed = MiniLzo.compress(source);
        byte[] buffer = new byte[compressed.length + 8];
        int pLen = buffer.length - 3;
        int u = source.length;
        int x = (u + u / 255) & 0xff;
        int v = (u - x) / 255;
        int y = (v + v / 255) & 0xff;
        int w = (v - y) / 255;
        int z = (w + w / 255) & 0xff;
        buffer[0] = (byte) salt;
        buffer[1] = (byte) pLen;
        buffer[2] = (byte) (pLen >>> 8);
        buffer[3] = (byte) (CryptoOracle.PUBLIC_KEY_TABLE[oracleIndex] ^ CryptoOracle.PRIVATE_KEY_TABLE[oracleIndex]);
        buffer[5] = (byte) z;
        buffer[6] = (byte) y;
        buffer[7] = (byte) x;
        System.arraycopy(compressed, 0, buffer, 8, compressed.length);
        for (int i = buffer.length - 1; i >= 10; i--) {
            buffer[i] ^= buffer[i - 4];
        }
        buffer[7] ^= CryptoOracle.PRIVATE_KEY_TABLE[oracleIndex];
        return buffer;
    }
}
