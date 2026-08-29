package org.pangya.protocol.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CryptoOracleTest {

    @Test
    void tablesMatchCSharpLengthsAndKnownPrefix() {
        assertEquals(4096, CryptoOracle.PUBLIC_KEY_TABLE.length);
        assertEquals(4096, CryptoOracle.PRIVATE_KEY_TABLE.length);
        // First 32 bytes copied from Server/JP/PangyaAPI/.../CryptoOracle.cs
        // (verified 4096-byte identity vs that file on 2026-08-29).
        int[] publicPrefix = {
                0, 1, 41, 35, 190, 132, 225, 108, 214, 174, 82, 144, 73, 241, 187, 233,
                235, 179, 166, 219, 60, 135, 12, 62, 153, 36, 94, 13, 28, 6, 183, 71
        };
        int[] privatePrefix = {
                0, 1, 85, 39, 159, 144, 29, 146, 178, 42, 55, 171, 22, 27, 140, 207,
                216, 165, 33, 53, 70, 145, 113, 227, 148, 241, 249, 208, 28, 115, 111, 38
        };
        for (int i = 0; i < publicPrefix.length; i++) {
            assertEquals(publicPrefix[i], Byte.toUnsignedInt(CryptoOracle.PUBLIC_KEY_TABLE[i]), "public[" + i + "]");
        }
        for (int i = 0; i < privatePrefix.length; i++) {
            assertEquals(privatePrefix[i], Byte.toUnsignedInt(CryptoOracle.PRIVATE_KEY_TABLE[i]), "private[" + i + "]");
        }
    }

    @Test
    void oracleIndexIsKeyShiftedPlusSalt() {
        assertEquals(0x0500 + 7, CryptoOracle.oracleIndex(5, 7));
    }
}
