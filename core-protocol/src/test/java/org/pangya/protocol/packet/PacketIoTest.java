package org.pangya.protocol.packet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PacketIoTest {

    @Test
    void loginHelloIsMakeRawKeyAndServerUid() {
        byte[] hello = PacketIo.loginHello(7, 10203);
        assertEquals(14, hello.length);
        assertEquals(0x00, hello[0] & 0xff);
        assertEquals(0x0B, hello[1] & 0xff);
        assertEquals(0x00, hello[2] & 0xff);
        assertEquals(7, hello[6] & 0xff);
        assertEquals(10203 & 0xff, hello[10] & 0xff);
        assertEquals((10203 >>> 8) & 0xff, hello[11] & 0xff);
    }

    @Test
    void gameHelloIsRaw0x3FWithKey() {
        byte[] hello = PacketIo.gameHello(3, "127.0.0.1");
        assertEquals(0, hello[0] & 0xff);
        assertEquals(0, hello[3] & 0xff);
        assertEquals(0x3F, hello[4] & 0xff);
        assertEquals(0x00, hello[5] & 0xff);
        assertEquals(1, hello[6] & 0xff);
        assertEquals(1, hello[7] & 0xff);
        assertEquals(3, hello[8] & 0xff);
    }

    @Test
    void rankingHelloIsRaw0x1388WithType5() {
        byte[] hello = PacketIo.rankingHello(4);
        assertEquals(0x88, hello[4] & 0xff);
        assertEquals(0x13, hello[5] & 0xff);
        assertEquals(4, hello[6] & 0xff);
        assertEquals(5, hello[10] & 0xff);
    }

    @Test
    void messengerHelloIsRaw0x2EWithU32Key() {
        byte[] hello = PacketIo.messengerHello(9);
        assertEquals(0x2E, hello[4] & 0xff);
        assertEquals(0x00, hello[5] & 0xff);
        assertEquals(1, hello[6] & 0xff);
        assertEquals(1, hello[7] & 0xff);
        assertEquals(9, hello[8] & 0xff);
    }
}
