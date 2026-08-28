package org.pangya.protocol.ranking;

import org.junit.jupiter.api.Test;
import org.pangya.protocol.packet.PacketReader;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RankingPacketsTest {

    @Test
    void loginRoundtrip() {
        byte[] pkt = RankingPackets.clientLogin(10001, "testuser", 0, 1, 0, 0, 0);
        PacketReader r = new PacketReader(pkt);
        assertEquals(RankingPackets.CLIENT_CONNECT, r.opcode());
        RankingPackets.Login login = RankingPackets.readLogin(r);
        assertEquals(10001, login.uid());
        assertEquals("testuser", login.id());
        assertEquals(0, login.menu());
        assertEquals(1, login.item());
    }

    @Test
    void firstPageEmptyRegistry() {
        byte[] pkt = RankingPackets.firstPageOk(0, 0, 0, 0);
        PacketReader r = new PacketReader(pkt);
        assertEquals(RankingPackets.SERVER_SEND_FIRST_PAGE, r.opcode());
        assertEquals(0, r.u8());
        assertEquals(0, r.u8());
        assertEquals(0, r.u8());
        assertEquals(0, r.u8());
        assertEquals(0, r.u8());
        r.readBytes(10);
        assertEquals(RankingPackets.PPRT_NOT_TOP_RANK, r.u8());
        assertEquals(0, r.remaining());
    }
}
