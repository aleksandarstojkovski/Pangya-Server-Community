package org.pangya.protocol.crypto;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CipherTest {

    @Test
    void miniLzoRoundtripVariousSizes() {
        byte[][] samples = {
                new byte[0],
                new byte[] {1},
                "hello pangya".getBytes(StandardCharsets.US_ASCII),
                bytes(16, 0x11),
                bytes(64, 0x22),
                bytes(255, 0x33),
                bytes(1024, 0x44),
                repeating((byte) 7, 200)
        };
        for (byte[] sample : samples) {
            byte[] compressed = MiniLzo.compress(sample);
            byte[] restored = MiniLzo.decompress(compressed);
            assertArrayEquals(sample, restored, "len=" + sample.length);
        }
        byte[] random = new byte[777];
        new Random(2026).nextBytes(random);
        assertArrayEquals(random, MiniLzo.decompress(MiniLzo.compress(random)));
    }

    @Test
    void serverEncryptDecryptRoundtrip() {
        byte[] plain = concat(new byte[] {0x02, 0x00}, "pangya-login".getBytes(StandardCharsets.US_ASCII));
        for (int key = 0; key < 16; key++) {
            byte[] enc = Cipher.serverEncrypt(plain, key, 0);
            assertTrue(enc.length >= 8);
            assertEquals(0, enc[0] & 0xff);
            int lenField = (enc[1] & 0xff) | ((enc[2] & 0xff) << 8);
            assertEquals(enc.length, lenField + 3);
            byte[] dec = Cipher.decryptServer(enc, key);
            assertArrayEquals(plain, dec, "key=" + key);
        }
    }

    @Test
    void clientEncryptDecryptRoundtrip() {
        byte[] plain = new byte[] {0x01, 0x00, 0x41, 0x42, 0x43};
        for (int key = 0; key < 16; key++) {
            byte[] enc = Cipher.encryptClient(plain, key, 0);
            assertEquals(plain.length + 5, enc.length);
            int lenField = (enc[1] & 0xff) | ((enc[2] & 0xff) << 8);
            assertEquals(enc.length, lenField + 4);
            byte[] dec = Cipher.decryptClient(enc, key);
            assertArrayEquals(plain, dec, "key=" + key);
        }
    }

    private static byte[] bytes(int n, int fill) {
        byte[] a = new byte[n];
        Arrays.fill(a, (byte) fill);
        return a;
    }

    private static byte[] repeating(byte v, int n) {
        return bytes(n, v);
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] c = Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }
}
