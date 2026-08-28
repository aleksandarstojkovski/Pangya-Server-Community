package org.pangya.protocol.game;

import org.junit.jupiter.api.Test;
import org.pangya.protocol.packet.PacketReader;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GamePacketsTest {

    @Test
    void channelInfoIs77Bytes() {
        GamePackets.ChannelInfo c = new GamePackets.ChannelInfo();
        c.name = "Channel (Rookies)";
        c.maxUser = 500;
        c.id = 1;
        byte[] raw = c.toArray();
        assertEquals(77, raw.length);
        PacketReader r = new PacketReader(GamePackets.channelList(List.of(c)));
        assertEquals(0x4D, r.opcode());
        assertEquals(1, r.u8());
        assertEquals(77, r.remaining());
    }

    @Test
    void loginRoundtrip() {
        byte[] pkt = GamePackets.clientLogin("testuser", 10001, "ABCD1234", "852.00", 2015031200, "EFGH5678");
        PacketReader r = new PacketReader(pkt);
        assertEquals(GamePackets.CLIENT_REQUEST_LOGIN, r.opcode());
        GamePackets.GameLogin login = GamePackets.readLogin(r);
        assertEquals("testuser", login.id());
        assertEquals(10001, login.uid());
        assertEquals("ABCD1234", login.authKeyLogin());
        assertEquals("852.00", login.clientVersion());
        assertEquals(2015031200, login.packetVersion());
        assertEquals("EFGH5678", login.authKeyGame());
    }

    @Test
    void createPracticeParsesTipo19() {
        byte[] pkt = GamePackets.clientCreatePractice("Single Player Practice Mode", "secret");
        PacketReader r = new PacketReader(pkt);
        assertEquals(GamePackets.CLIENT_REQUEST_CREATE_ROOM, r.opcode());
        GamePackets.CreateRoom room = GamePackets.readCreateRoom(r);
        assertEquals(GamePackets.TIPO_PRACTICE, room.tipo());
        assertEquals(1, room.maxPlayer());
        assertEquals("secret", room.password());
    }

    @Test
    void packetVersionXorIsInvolutive() {
        int plain = 2016110200;
        int wire = GamePackets.xorPacketVersion(plain);
        assertEquals(plain, GamePackets.xorPacketVersion(wire));
    }

    @Test
    void principalPayloadIs12512Bytes() {
        byte[] pkt = GamePackets.loginOkPrincipal(
                "852.00", "GS.Release.852.00", 1, "testuser", "TestNick", 0, 10001, 1, 2048);
        PacketReader r = new PacketReader(pkt);
        assertEquals(GamePackets.SERVER_LOGIN_ACK, r.opcode());
        assertEquals(GamePackets.ACK_LOGIN_OK, r.u8());
        assertEquals(GamePackets.PRINCIPAL_PAYLOAD_BYTES, r.remaining());
        assertEquals("852.00", r.pstr());
        assertEquals("GS.Release.852.00", r.pstr());
        assertEquals(GamePackets.MEMBER_INFO_EX_BYTES, GamePackets.memberInfoEx(1, "a", "b", 0).length);
        assertEquals(GamePackets.USER_INFO_BYTES, GamePackets.userInfo(1).length);
    }
}
