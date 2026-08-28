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
        RankingPackets.RegistryRow row = new RankingPackets.RegistryRow(10001, 1, 0, 42);
        PacketReader page = new PacketReader(RankingPackets.firstPage(7, 3, 0, 0, java.util.List.of(row), 1, 1));
        assertEquals(RankingPackets.SERVER_SEND_FIRST_PAGE, page.opcode());
        page.u8();
        page.u8();
        page.u8();
        page.u8();
        page.u8();
        assertEquals(1, page.u32());
        assertEquals(1, page.u32());
        assertEquals(1, page.u16());
        assertEquals(10001, page.u32());
    }
}
