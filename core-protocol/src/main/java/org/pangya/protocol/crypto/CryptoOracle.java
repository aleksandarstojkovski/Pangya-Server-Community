package org.pangya.protocol.crypto;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Objects;

/**
 * Bit-identical copy of C# {@code PangyaAPI.Network.Cryptor.CryptoOracle}
 * ({@code PUBLIC_KEY_TABLE} / {@code PRIVATE_KEY_TABLE}, 4096 bytes each).
 */
public final class CryptoOracle {

    public static final int TABLE_SIZE = 4096;

    public static final byte[] PUBLIC_KEY_TABLE = load("public-key-table.bin");
    public static final byte[] PRIVATE_KEY_TABLE = load("private-key-table.bin");

    private CryptoOracle() {}

    public static int oracleIndex(int key, int salt) {
        if (key < 0 || key >= 0x10) {
            throw new IllegalArgumentException("cryptography key must be 0..15, got " + key);
        }
        return (key << 8) + (salt & 0xff);
    }

    private static byte[] load(String name) {
        String path = "/org/pangya/protocol/crypto/" + name;
        try (InputStream in = Objects.requireNonNull(
                CryptoOracle.class.getResourceAsStream(path), "missing resource " + path)) {
            byte[] data = in.readAllBytes();
            if (data.length != TABLE_SIZE) {
                throw new IllegalStateException(name + " length " + data.length + " != " + TABLE_SIZE);
            }
            return data;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
