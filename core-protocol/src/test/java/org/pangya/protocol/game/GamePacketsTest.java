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
        byte[] pkt = GamePackets.clientLogin(
                "testuser", 10001, "ABCD1234", GamePackets.JP_CLIENT_VERSION, 2017110200, "EFGH5678");
        PacketReader r = new PacketReader(pkt);
        assertEquals(GamePackets.CLIENT_REQUEST_LOGIN, r.opcode());
        GamePackets.GameLogin login = GamePackets.readLogin(r);
        assertEquals("testuser", login.id());
        assertEquals(10001, login.uid());
        assertEquals("ABCD1234", login.authKeyLogin());
        assertEquals(GamePackets.JP_CLIENT_VERSION, login.clientVersion());
        assertEquals(2017110200, login.packetVersion());
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
        int plain = GamePackets.JP_PACKET_VERSION;
        int wire = GamePackets.xorPacketVersion(plain);
        assertEquals(plain, GamePackets.xorPacketVersion(wire));
    }

    @Test
    void principalPayloadMatchesJpLayout() {
        byte[] pkt = GamePackets.loginOkPrincipal(
                GamePackets.JP_CLIENT_VERSION, 1, "testuser", "TestNick", 0, 10001, 1, 2048);
        PacketReader r = new PacketReader(pkt);
        assertEquals(GamePackets.SERVER_LOGIN_ACK, r.opcode());
        assertEquals(GamePackets.ACK_LOGIN_OK, r.u8());
        assertEquals(GamePackets.PRINCIPAL_PAYLOAD_BYTES, r.remaining());
        assertEquals(GamePackets.JP_CLIENT_VERSION, r.pstr());
        assertEquals(GamePackets.PRINCIPAL_AFTER_VERSION_BYTES, r.remaining());
        assertEquals(GamePackets.MEMBER_INFO_EX_BYTES, GamePackets.memberInfoEx(1, "a", "b", 0).length);
        assertEquals(GamePackets.USER_INFO_BYTES, GamePackets.userInfo(1).length);
    }

    @Test
    void warehouseAndCharacterSizesMatchCsharp() {
        GamePackets.WarehouseItem w = new GamePackets.WarehouseItem();
        w.id = 2;
        w.typeid = GamePackets.TYPEID_AIR_KNIGHT;
        assertEquals(GamePackets.WAREHOUSE_ITEM_BYTES, w.toArray().length);
        GamePackets.CharacterInfo c = new GamePackets.CharacterInfo();
        c.id = 1;
        c.typeid = GamePackets.TYPEID_NURI;
        assertEquals(GamePackets.CHARACTER_INFO_BYTES, c.toArray().length);
        GamePackets.CaddieInfo cad = new GamePackets.CaddieInfo();
        assertEquals(GamePackets.CADDIE_INFO_BYTES, cad.toArray().length);
        assertEquals(GamePackets.USER_EQUIP_BYTES, new GamePackets.UserEquip().toArray().length);
        assertEquals(GamePackets.MASCOT_INFO_BYTES, new GamePackets.MascotInfo().toArray().length);
        assertEquals(GamePackets.CARD_INFO_BYTES, new GamePackets.CardInfo().toArray().length);
        GamePackets.RoomInfo room = new GamePackets.RoomInfo();
        room.name = "VS";
        room.numero = 1;
        room.tipoShow = GamePackets.tipoShow(GamePackets.TIPO_STROKE);
        room.tipoEx = GamePackets.tipoEx(GamePackets.TIPO_STROKE);
        assertEquals(GamePackets.ROOM_INFO_BYTES, room.toArray().length);
        assertEquals(GamePackets.TIPO_TOURNEY, GamePackets.tipoShow(GamePackets.TIPO_PRACTICE));
        assertEquals(19, GamePackets.tipoEx(GamePackets.TIPO_PRACTICE));
        assertEquals(255, GamePackets.tipoEx(GamePackets.TIPO_STROKE));
        assertEquals(true, GamePackets.isCharacterTypeid(GamePackets.TYPEID_NURI));
        assertEquals(false, GamePackets.isCharacterTypeid(GamePackets.TYPEID_AIR_KNIGHT));
        List<byte[]> tail = GamePackets.loginDumpTail(10001, 0, 0, 1);
        assertEquals(GamePackets.LOGIN_DUMP_TAIL_COUNT, tail.size());
        assertEquals(0x102, new PacketReader(tail.get(0)).opcode());
        assertEquals(0xF1, new PacketReader(tail.get(4)).opcode());
        assertEquals(0x135, new PacketReader(tail.get(5)).opcode());
        assertEquals(0x25D, new PacketReader(tail.getLast()).opcode());
    }

    @Test
    void coursePacketIs18HolesAndZeroCubes() {
        GamePackets.RoomInfo room = new GamePackets.RoomInfo();
        room.course = 0;
        room.tipoShow = GamePackets.TIPO_TOURNEY;
        room.holes = 18;
        java.util.ArrayList<GamePackets.HoleInfo> holes = new java.util.ArrayList<>();
        for (int n = 1; n <= 18; n++) {
            holes.add(new GamePackets.HoleInfo(n, (n - 1) % 3, 0, n, 0, 0, 0));
        }
        PacketReader r = new PacketReader(GamePackets.course(room, holes, 1));
        assertEquals(GamePackets.SERVER_COURSE, r.opcode());
        assertEquals(0, r.u8());
        assertEquals(GamePackets.TIPO_TOURNEY, r.u8());
        r.u8();
        assertEquals(18, r.u8());
        r.u32();
        r.u32();
        r.u32();
        assertEquals(18 * 7 + 4 + 18, r.remaining());
        byte[] shot = GamePackets.shotSyncPlain(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        assertEquals(GamePackets.SHOT_SYNC_BYTES, shot.length);
        byte[] key = new byte[16];
        key[0] = 7;
        byte[] enc = GamePackets.xorRoomKey(shot, key);
        byte[] dec = GamePackets.xorRoomKey(enc, key);
        assertEquals(shot[0], dec[0]);
        PacketReader eq = new PacketReader(GamePackets.equipAck(4, 5, new byte[] {1, 0, 0, 0}));
        assertEquals(GamePackets.SERVER_EQUIP_ACK, eq.opcode());
        assertEquals(GamePackets.EQUIP_OK, eq.u8());
        assertEquals(5, eq.u8());
        GamePackets.PlayerRoomInfo pri = new GamePackets.PlayerRoomInfo();
        pri.oid = 1;
        pri.nickname = "TestNick";
        pri.uid = 10001;
        pri.position = 1;
        pri.stateFlag = (1 << 3) | (1 << 9);
        pri.character = new GamePackets.CharacterInfo();
        assertEquals(GamePackets.PLAYER_ROOM_INFO_BYTES, pri.toArray().length);
        assertEquals(GamePackets.PLAYER_ROOM_INFO_EX_BYTES, pri.toArrayEx().length);
        PacketReader list = new PacketReader(GamePackets.roomPlayers(0x100, List.of(pri)));
        assertEquals(GamePackets.SERVER_ROOM_PLAYERS, list.opcode());
        assertEquals(0, list.u8());
        assertEquals(-1, list.i16());
        assertEquals(1, list.u8());
        assertEquals(GamePackets.PLAYER_ROOM_INFO_BYTES, list.remaining() - 1);
        assertEquals(GamePackets.CLUBSET_INFO_BYTES, new GamePackets.ClubSetInfo().toArray().length);
        GamePackets.VersusPlayer vp = new GamePackets.VersusPlayer(
                GamePackets.memberInfoExPublic(1, "a", "b", 0),
                10001,
                GamePackets.userInfoPublic(1),
                new GamePackets.UserEquip().toArray(),
                new GamePackets.CharacterInfo().toArray(),
                new GamePackets.CaddieInfo().toArray(),
                new GamePackets.ClubSetInfo().toArray(),
                new GamePackets.MascotInfo().toArray(),
                List.of());
        PacketReader vs = new PacketReader(GamePackets.gameInitVersus(GamePackets.TIPO_STROKE, List.of(vp)));
        assertEquals(GamePackets.SERVER_GAME_INIT, vs.opcode());
        assertEquals(GamePackets.TIPO_STROKE, vs.u8());
        assertEquals(1, vs.u8());
        PacketReader counters = new PacketReader(GamePackets.counters(List.of()));
        assertEquals(0x21D, counters.opcode());
        assertEquals(0, counters.u32());
        assertEquals(0, counters.u32());
        assertEquals(0, counters.u32());
    }
}
