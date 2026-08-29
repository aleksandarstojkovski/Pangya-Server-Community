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
        RankingPackets.RowSummary summary =
                new RankingPackets.RowSummary(5, 0, 0, "testuser", "TestNick");
        PacketReader page = new PacketReader(RankingPackets.firstPage(
                7, 3, 0, 0, java.util.List.of(new RankingPackets.RegistryRowWithSummary(row, summary)), 1, 1));
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
        assertEquals(1, page.u32());
        assertEquals(0, page.u32());
        assertEquals(42, page.i32());
        assertEquals(5, page.u8());
        assertEquals(0, page.u8());
        assertEquals(0, page.u8());
        assertEquals("testuser", page.pstr());
        assertEquals("TestNick", page.pstr());

        byte[] search = RankingPackets.clientSearchByNickname(
                "TestNick", new RankingPackets.SearchDados(7, 3, 0, 0, 0));
        PacketReader searchClient = new PacketReader(search);
        assertEquals(RankingPackets.CLIENT_REQ_SEARCH_PLAYER_IN_RANKING, searchClient.opcode());
        RankingPackets.SearchRequest req = RankingPackets.readSearch(searchClient);
        assertEquals(RankingPackets.SEARCH_BY_NICKNAME, req.option());
        assertEquals("TestNick", req.nickname());
        assertEquals(7, req.dados().menu());
    }
}
