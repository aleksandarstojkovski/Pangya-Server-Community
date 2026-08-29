package org.pangya.protocol.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CryptoOracleTest {

    @Test
    void tablesMatchCSharpLengthsAndKnownPrefix() {
        assertEquals(4096, CryptoOracle.PUBLIC_KEY_TABLE.length);
        assertEquals(4096, CryptoOracle.PRIVATE_KEY_TABLE.length);
        // First 8 bytes from Server/JP/.../CryptoOracle.cs (same tables as GB)
        assertEquals(0, Byte.toUnsignedInt(CryptoOracle.PUBLIC_KEY_TABLE[0]));
        assertEquals(1, Byte.toUnsignedInt(CryptoOracle.PUBLIC_KEY_TABLE[1]));
        assertEquals(41, Byte.toUnsignedInt(CryptoOracle.PUBLIC_KEY_TABLE[2]));
        assertEquals(35, Byte.toUnsignedInt(CryptoOracle.PUBLIC_KEY_TABLE[3]));
        assertEquals(190, Byte.toUnsignedInt(CryptoOracle.PUBLIC_KEY_TABLE[4]));
        assertEquals(0, Byte.toUnsignedInt(CryptoOracle.PRIVATE_KEY_TABLE[0]));
        assertEquals(1, Byte.toUnsignedInt(CryptoOracle.PRIVATE_KEY_TABLE[1]));
        assertEquals(85, Byte.toUnsignedInt(CryptoOracle.PRIVATE_KEY_TABLE[2]));
        assertEquals(39, Byte.toUnsignedInt(CryptoOracle.PRIVATE_KEY_TABLE[3]));
    }

    @Test
    void oracleIndexIsKeyShiftedPlusSalt() {
        assertEquals(0x0500 + 7, CryptoOracle.oracleIndex(5, 7));
    }
}
