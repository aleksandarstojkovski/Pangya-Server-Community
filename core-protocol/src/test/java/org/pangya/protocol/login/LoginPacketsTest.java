package org.pangya.protocol.login;

import org.junit.jupiter.api.Test;
import org.pangya.protocol.auth.AuthS2s;
import org.pangya.protocol.packet.PacketIo;
import org.pangya.protocol.packet.PacketReader;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LoginPacketsTest {

    @Test
    void loginDataRoundtrip() {
        byte[] payload = LoginPackets.clientConnect("testuser", "testpass", "aa:bb:cc:dd:ee:ff");
        PacketReader r = new PacketReader(payload);
        assertEquals(LoginPackets.CLIENT_CONNECT, r.opcode());
        LoginPackets.LoginData data = LoginPackets.readLoginData(r);
        assertEquals("testuser", data.id());
        assertEquals("testpass", data.password());
        assertEquals(0, data.optCount());
        assertEquals("aa:bb:cc:dd:ee:ff", data.mac());
    }

    @Test
    void pacote001SuccessLayout() {
        byte[] pkt = LoginPackets.pacote001Success(
                "testuser", 10001, 4, LoginPackets.DEFAULT_ACCESS_CODE, "TestNick");
        PacketReader r = new PacketReader(pkt);
        assertEquals(0x01, r.opcode());
        assertEquals(0, r.u8());
        assertEquals("testuser", r.pstr());
        assertEquals(10001, r.u32());
        assertEquals(4, r.u32());
        assertEquals(1, r.u8());
        assertEquals(0, r.u32());
        assertEquals(1, r.u8());
        assertEquals(5, r.u32());
        r.readBytes(16);
        assertEquals(LoginPackets.DEFAULT_ACCESS_CODE, r.pstr());
        assertEquals(0, r.u64());
        assertEquals("TestNick", r.pstr());
        assertEquals(0, r.remaining());
    }

    @Test
    void pacote001BadPassHasFourZeroBytes() {
        byte[] pkt = LoginPackets.pacote001Option(LoginPackets.OPT_BAD_ID_OR_PASS);
        PacketReader r = new PacketReader(pkt);
        assertEquals(0x01, r.opcode());
        assertEquals(6, r.u8());
        assertEquals(0, r.u32());
        assertEquals(0, r.remaining());
    }

    @Test
    void pacote010IsPstrAuthKey() {
        byte[] pkt = LoginPackets.pacote010("ABCDEF12");
        PacketReader r = new PacketReader(pkt);
        assertEquals(0x10, r.opcode());
        assertEquals("ABCDEF12", r.pstr());
    }

    @Test
    void serverInfoIs92Bytes() {
        ServerInfo si = new ServerInfo();
        si.name = "PAPEL";
        si.uid = 20202;
        si.maxUser = 2001;
        si.currUser = 0;
        si.ip = "127.0.0.1";
        si.port = 20202;
        si.property = 2048;
        byte[] raw = si.toArray();
        assertEquals(92, raw.length);
        assertEquals('P', raw[0] & 0xff);
        assertEquals(0, raw[5] & 0xff);
        assertEquals(20202, PacketIo.readU32le(raw, 40));
    }

    @Test
    void pacote002CountPlusServerInfo() {
        ServerInfo si = new ServerInfo();
        si.name = "PAPEL";
        si.uid = 20202;
        si.ip = "127.0.0.1";
        si.port = 20202;
        byte[] pkt = LoginPackets.pacote002(List.of(si));
        PacketReader r = new PacketReader(pkt);
        assertEquals(0x02, r.opcode());
        assertEquals(1, r.u8());
        assertEquals(92, r.remaining());
    }

    @Test
    void pacote006IsNineTimes64() {
        byte[] pkt = LoginPackets.pacote006(new String[] {"hello"});
        PacketReader r = new PacketReader(pkt);
        assertEquals(0x06, r.opcode());
        assertEquals(9 * 64, r.remaining());
        byte[] first = r.readBytes(64);
        assertEquals('h', first[0] & 0xff);
        assertEquals(0, first[5] & 0xff);
    }

    @Test
    void authRegisterRoundtrip() {
        byte[] pkt = AuthS2s.register(0, 10203, "Login Server", "DEADBEEFDEADBEEF", "JP.R7.983.00", 2017110200);
        PacketReader r = new PacketReader(pkt);
        assertEquals(AuthS2s.REGISTER, r.opcode());
        AuthS2s.RegisterRequest req = AuthS2s.readRegister(r);
        assertEquals(0, req.tipo());
        assertEquals(10203, req.uid());
        assertEquals("Login Server", req.name());
        assertEquals("DEADBEEFDEADBEEF", req.key());
        assertEquals("JP.R7.983.00", req.clientVersion());
        assertEquals(2017110200, req.packetVersion());
    }

    @Test
    void authFirstKeyRawUsesMakeRaw() {
        byte[] frame = AuthS2s.firstKeyRaw(7, 8888);
        assertEquals(0, frame[0] & 0xff);
        assertEquals(0, frame[3] & 0xff);
        byte[] payload = PacketIo.slice(frame, 4, frame.length - 4);
        PacketReader r = new PacketReader(payload);
        assertEquals(0x00, r.opcode());
        assertEquals(7, r.u32());
        assertEquals(8888, r.u32());
        assertEquals(PacketIo.serverFrameLength(frame, 0), frame.length);
    }

    @Test
    void selectGsReadsUid() {
        byte[] pkt = LoginPackets.clientSelectGs(20202);
        PacketReader r = new PacketReader(pkt);
        assertEquals(LoginPackets.CLIENT_SELECT_GS, r.opcode());
        assertEquals(20202, r.u32());
    }

    @Test
    void emptyGsListIsCountZero() {
        byte[] pkt = LoginPackets.pacote002(List.of());
        assertArrayEquals(new byte[] {0x02, 0x00, 0x00}, pkt);
    }

    @Test
    void pacote00FHasJpUnknownAndAccessCode() {
        byte[] pkt = LoginPackets.pacote00F(1, "testuser");
        PacketReader r = new PacketReader(pkt);
        assertEquals(0x0F, r.opcode());
        assertEquals(1, r.u8());
        assertEquals("testuser", r.pstr());
        assertEquals(0, r.u32());
        assertEquals(5, r.u32());
        assertEquals(org.pangya.protocol.packet.PacketIo.FORMAT_DATE_EPOCH, r.pstr());
        assertEquals(LoginPackets.DEFAULT_ACCESS_CODE, r.pstr());
        assertEquals(0, r.remaining());
    }

    @Test
    void pacote00ESuccessWritesNick() {
        byte[] pkt = LoginPackets.pacote00E(LoginPackets.NICK_OK, "NewNick");
        PacketReader r = new PacketReader(pkt);
        assertEquals(0x0E, r.opcode());
        assertEquals(0, r.i32());
        assertEquals("NewNick", r.pstr());
        assertEquals(0, r.remaining());
    }

    @Test
    void pacote00ECodeErrorWritesUint32() {
        byte[] pkt = LoginPackets.pacote00E(
                LoginPackets.NICK_CODE_ERROR, "", LoginPackets.FIRST_SET_CHAR_ERROR);
        PacketReader r = new PacketReader(pkt);
        assertEquals(0x0E, r.opcode());
        assertEquals(12, r.i32());
        assertEquals(LoginPackets.FIRST_SET_CHAR_ERROR, r.u32());
        assertEquals(0, r.remaining());
    }

    @Test
    void pacote011IsUint16Option() {
        byte[] pkt = LoginPackets.pacote011(0);
        PacketReader r = new PacketReader(pkt);
        assertEquals(0x11, r.opcode());
        assertEquals(0, r.u16());
        assertEquals(0, r.remaining());
    }
}
