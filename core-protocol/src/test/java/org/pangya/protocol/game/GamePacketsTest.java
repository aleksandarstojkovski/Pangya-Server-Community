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
        byte[] lounge = GamePackets.loungeState(1);
        PacketReader lg = new PacketReader(lounge);
        assertEquals(GamePackets.SERVER_LOUNGE_STATE, lg.opcode());
        assertEquals(1, lg.i32());
        assertEquals(1.0f, lg.f32());
        assertEquals(GamePackets.STATE_CHARACTER_LOUNGE_BYTES - 4, lg.remaining());
        PacketReader emptyBuy = new PacketReader(GamePackets.clientBuyEmpty());
        assertEquals(GamePackets.CLIENT_REQUEST_BUY_ITEM, emptyBuy.opcode());
        GamePackets.BuyRequest empty = GamePackets.readBuyRequest(emptyBuy);
        assertEquals(0, empty.items().size());
        PacketReader buyPkt = new PacketReader(GamePackets.clientBuyItem(
                GamePackets.TYPEID_SHOP_PANG_ITEM, 1, GamePackets.SHOP_PANG_PRICE, 0));
        assertEquals(GamePackets.CLIENT_REQUEST_BUY_ITEM, buyPkt.opcode());
        GamePackets.BuyRequest buy = GamePackets.readBuyRequest(buyPkt);
        assertEquals(1, buy.items().size());
        assertEquals(GamePackets.TYPEID_SHOP_PANG_ITEM, buy.items().getFirst().typeid());
        assertEquals(GamePackets.SHOP_PANG_PRICE, buy.items().getFirst().pang());
        byte[] aa = GamePackets.buyNewItems(
                List.of(new GamePackets.BoughtItem(GamePackets.TYPEID_SHOP_PANG_ITEM, 99, 0, 0, 1)),
                99900,
                0);
        PacketReader newItem = new PacketReader(aa);
        assertEquals(GamePackets.SERVER_NEW_ITEM, newItem.opcode());
        assertEquals(1, newItem.u16());
        assertEquals(GamePackets.TYPEID_SHOP_PANG_ITEM, newItem.u32());
        assertEquals(99, newItem.i32());
        PacketReader ok = new PacketReader(GamePackets.buyOk(99900, 0));
        assertEquals(GamePackets.SERVER_BUY_ACK, ok.opcode());
        assertEquals(0, ok.u32());
        assertEquals(99900, ok.u64());
    }

    @Test
    void lobbyPacketsMatchCsharpLayouts() {
        GamePackets.PlayerLobbyInfo info = new GamePackets.PlayerLobbyInfo();
        info.uid = 10001;
        info.oid = 1;
        info.nick = "TestNick";
        info.level = 1;
        info.teamPoint = 1000;
        info.nickDisplay = "@NT_TestNick";
        assertEquals(GamePackets.PLAYER_LOBBY_INFO_BYTES, info.toArray().length);

        PacketReader users = new PacketReader(GamePackets.lobbyUsers(
                GamePackets.LOBBY_USER_LIST, List.of(info)));
        assertEquals(GamePackets.SERVER_USERLIST, users.opcode());
        assertEquals(GamePackets.LOBBY_USER_LIST, users.u8());
        assertEquals(1, users.u8());
        assertEquals(GamePackets.PLAYER_LOBBY_INFO_BYTES, users.remaining());
        assertEquals(10001, users.u32());
        assertEquals(1, users.i32());
        assertEquals(0xFFFF, users.u16());

        GamePackets.RoomInfo room = new GamePackets.RoomInfo();
        room.name = "VS";
        room.numero = 1;
        room.tipoShow = GamePackets.tipoShow(GamePackets.TIPO_STROKE);
        PacketReader rooms = new PacketReader(GamePackets.roomList(
                GamePackets.ROOM_LIST_FULL, List.of(room.toArray())));
        assertEquals(GamePackets.SERVER_ROOMLIST, rooms.opcode());
        assertEquals(1, rooms.u8());
        assertEquals(GamePackets.ROOM_LIST_FULL, rooms.u8());
        assertEquals(-1, rooms.i16());
        assertEquals(GamePackets.ROOM_INFO_BYTES, rooms.remaining());

        PacketReader emptyRooms = new PacketReader(GamePackets.roomList(
                GamePackets.ROOM_LIST_FULL, List.of()));
        assertEquals(GamePackets.SERVER_ROOMLIST, emptyRooms.opcode());
        assertEquals(0, emptyRooms.u8());
        assertEquals(0, emptyRooms.u8());
        assertEquals(-1, emptyRooms.i16());
        assertEquals(0, emptyRooms.remaining());

        PacketReader chat = new PacketReader(GamePackets.chat(GamePackets.CHAT_NORMAL, "TestNick", "hello"));
        assertEquals(GamePackets.SERVER_CHAT, chat.opcode());
        assertEquals(GamePackets.CHAT_NORMAL, chat.u8());
        assertEquals("TestNick", chat.pstr());
        assertEquals("hello", chat.pstr());

        PacketReader ready = new PacketReader(GamePackets.readyState(7, 1));
        assertEquals(GamePackets.SERVER_READY, ready.opcode());
        assertEquals(7, ready.i32());
        assertEquals(1, ready.u8());
        assertEquals(0, GamePackets.enterLobbyAck().length - 2);
        assertEquals(2, GamePackets.enterLobbyAck().length);
        assertEquals(2, GamePackets.leaveLobbyAck().length);
        assertEquals(true, GamePackets.hiddenFromLobby(GamePackets.TIPO_PRACTICE));
        assertEquals(false, GamePackets.hiddenFromLobby(GamePackets.TIPO_STROKE));

        PacketReader change = new PacketReader(GamePackets.clientChangeRoomCourse(3, 5));
        assertEquals(GamePackets.CLIENT_CHANGE_ROOM_INFO, change.opcode());
        assertEquals(3, change.i16());
        assertEquals(1, change.u8());
        assertEquals(GamePackets.ROOM_CHANGE_COURSE, change.u8());
        assertEquals(5, change.u8());
        PacketReader pm = new PacketReader(GamePackets.whisper(GamePackets.WHISPER_FROM, "TestNick2", "hi"));
        assertEquals(GamePackets.SERVER_WHISPER, pm.opcode());
        assertEquals(GamePackets.WHISPER_FROM, pm.u8());
        assertEquals("TestNick2", pm.pstr());
        assertEquals("hi", pm.pstr());
        PacketReader offline = new PacketReader(GamePackets.chatOffline("nobody"));
        assertEquals(GamePackets.SERVER_CHAT, offline.opcode());
        assertEquals(GamePackets.CHAT_OFFLINE, offline.u8());
        assertEquals("nobody", offline.pstr());
    }

    @Test
    void playerInfoDumpAndServerListMatchCsharpLayouts() {
        GamePackets.CharacterInfo character = new GamePackets.CharacterInfo();
        character.id = 1;
        character.typeid = GamePackets.TYPEID_NURI;
        List<byte[]> dump = GamePackets.playerInfoDump(
                10001, 0, 7, 0xffff, "testuser", "TestNick", 0, 1,
                character, new GamePackets.UserEquip());
        assertEquals(GamePackets.PLAYER_INFO_DUMP_COUNT, dump.size());
        int[] opcodes = {0x157, 0x15E, 0x156, 0x158, 0x15D, 0x15C, 0x15C, 0x15B, 0x15A, 0x159, 0x15C, 0x257};
        for (int i = 0; i < opcodes.length; i++) {
            assertEquals(opcodes[i], new PacketReader(dump.get(i)).opcode(), "dump[" + i + "]");
        }
        PacketReader member = new PacketReader(dump.get(0));
        member.opcode();
        assertEquals(0, member.u8());
        assertEquals(10001, member.u32());
        assertEquals(GamePackets.MEMBER_INFO_EX_BYTES, member.remaining() - 8);
        PacketReader characterPkt = new PacketReader(dump.get(1));
        characterPkt.opcode();
        assertEquals(10001, characterPkt.u32());
        assertEquals(GamePackets.CHARACTER_INFO_BYTES, characterPkt.remaining());
        PacketReader maps = new PacketReader(dump.get(5));
        maps.opcode();
        assertEquals(0x0A, maps.u8());
        assertEquals(10001, maps.u32());
        assertEquals(0, maps.i32());
        assertEquals(0, maps.i32());
        PacketReader unknown = new PacketReader(dump.get(7));
        unknown.opcode();
        assertEquals(0, unknown.u8());
        assertEquals(10001, unknown.u32());
        assertEquals(1, unknown.i16());
        for (int i = 0; i < 60; i++) {
            assertEquals(i, unknown.i32());
        }
        PacketReader ack = new PacketReader(GamePackets.playerInfoAck(GamePackets.PLAYER_INFO_OK, 0, 10001));
        assertEquals(GamePackets.SERVER_PLAYER_INFO, ack.opcode());
        assertEquals(GamePackets.PLAYER_INFO_OK, ack.u32());
        assertEquals(0, ack.u8());
        assertEquals(10001, ack.u32());
        PacketReader missing = new PacketReader(GamePackets.playerInfoAck(GamePackets.PLAYER_INFO_OK, 0, 0));
        assertEquals(GamePackets.SERVER_PLAYER_INFO, missing.opcode());
        assertEquals(1, missing.u32());
        assertEquals(0, missing.u8());
        assertEquals(0, missing.u32());

        org.pangya.protocol.login.ServerInfo server = new org.pangya.protocol.login.ServerInfo();
        server.name = "PAPEL";
        server.ip = "127.0.0.1";
        server.port = 20202;
        assertEquals(GamePackets.SERVER_INFO_BYTES, server.toArray().length);
        GamePackets.ChannelInfo channel = new GamePackets.ChannelInfo();
        channel.name = "Channel (Rookies)";
        channel.maxUser = 500;
        PacketReader list = new PacketReader(GamePackets.serverAndChannelList(
                List.of(server.toArray()), List.of(channel)));
        assertEquals(GamePackets.SERVER_SERVER_LIST, list.opcode());
        assertEquals(1, list.u8());
        assertEquals(GamePackets.SERVER_INFO_BYTES, list.remaining() - 1 - GamePackets.CHANNEL_INFO_BYTES);
        list.readBytes(GamePackets.SERVER_INFO_BYTES);
        assertEquals(1, list.u8());
        assertEquals(GamePackets.CHANNEL_INFO_BYTES, list.remaining());
        assertEquals(GamePackets.CHANNEL_INFO_BYTES, channel.toArray().length);

        PacketReader rank = new PacketReader(GamePackets.rankAddress("127.0.0.1", 4774));
        assertEquals(GamePackets.SERVER_RANK_ADDRESS, rank.opcode());
        assertEquals("127.0.0.1", rank.pstr());
        assertEquals(4774, rank.i32());
        PacketReader team = new PacketReader(GamePackets.teamState(3, 1));
        assertEquals(GamePackets.SERVER_TEAM, team.opcode());
        assertEquals(3, team.i32());
        assertEquals(1, team.u8());
        GamePackets.PlayerRoomInfo leaver = new GamePackets.PlayerRoomInfo();
        leaver.oid = 9;
        PacketReader left = new PacketReader(GamePackets.roomPlayers(2, List.of(leaver)));
        assertEquals(GamePackets.SERVER_ROOM_PLAYERS, left.opcode());
        assertEquals(2, left.u8());
        assertEquals(-1, left.i16());
        assertEquals(9, left.i32());
        assertEquals(0, left.remaining());
        PacketReader exit = new PacketReader(GamePackets.exitRoomAck(-1));
        assertEquals(GamePackets.SERVER_EXIT_ROOM, exit.opcode());
        assertEquals(-1, exit.i16());
        PacketReader invite = new PacketReader(GamePackets.inviteOk(
                GamePackets.SERVER_INVITE, 20202, 0, 3, 10001, "TestNick", 10002));
        assertEquals(GamePackets.SERVER_INVITE, invite.opcode());
        assertEquals(0, invite.u16());
        assertEquals(20202, invite.u32());
        assertEquals(0, invite.u8());
        assertEquals(3, invite.u16());
        assertEquals(10001, invite.u32());
        assertEquals("TestNick", invite.pstr());
        assertEquals(10002, invite.u32());
        PacketReader inviteFail = new PacketReader(GamePackets.inviteFail(GamePackets.INVITE_FAIL));
        assertEquals(GamePackets.SERVER_INVITE_REPLY, inviteFail.opcode());
        assertEquals(GamePackets.INVITE_FAIL, inviteFail.u16());
        PacketReader macros = new PacketReader(GamePackets.clientUpdateMacros(new String[] {"Nice shot!"}));
        assertEquals(GamePackets.CLIENT_UPDATE_MACRO, macros.opcode());
        assertEquals(GamePackets.MACRO_COUNT * GamePackets.MACRO_BYTES, macros.remaining());
        assertEquals("Nice shot!", macros.fixedStr(GamePackets.MACRO_BYTES));
        GamePackets.RoomInfo room = new GamePackets.RoomInfo();
        room.numPlayer = 2;
        room.holes = 18;
        room.tipoShow = GamePackets.tipoShow(GamePackets.TIPO_STROKE);
        PacketReader detail = new PacketReader(GamePackets.roomDetail(
                room, GamePackets.TIPO_STROKE, List.of(
                        new GamePackets.RoomDetailPlayer(1, 1, 0, 0, 0, 1000),
                        new GamePackets.RoomDetailPlayer(2, 1, 0, 0, 0, 1000))));
        assertEquals(GamePackets.SERVER_ROOM_DETAIL, detail.opcode());
        assertEquals(2, detail.u32());
        assertEquals(18, detail.u8());
        assertEquals(0, detail.u32());
        assertEquals(0, detail.u8());
        assertEquals(GamePackets.TIPO_STROKE, detail.u8());
        assertEquals(0, detail.u8());
        assertEquals(0, detail.u32());
        assertEquals(1, detail.i32());
        assertEquals(1, detail.u8());
        assertEquals(0, detail.u8());
        PacketReader cam = new PacketReader(GamePackets.camera(7, 1.5f));
        assertEquals(GamePackets.SERVER_CAMERA, cam.opcode());
        assertEquals(7, cam.i32());
        assertEquals(1.5f, cam.f32());
        PacketReader club = new PacketReader(GamePackets.club(7, 3));
        assertEquals(GamePackets.SERVER_CLUB, club.opcode());
        assertEquals(7, club.i32());
        assertEquals(3, club.u8());
        PacketReader load = new PacketReader(GamePackets.loadPercent(7, 50));
        assertEquals(GamePackets.SERVER_LOAD_PERCENT, load.opcode());
        assertEquals(7, load.i32());
        assertEquals(50, load.u8());
        PacketReader teamChat = new PacketReader(GamePackets.teamChat("TestNick", "go"));
        assertEquals(GamePackets.SERVER_TEAM_CHAT, teamChat.opcode());
        assertEquals("TestNick", teamChat.pstr());
        assertEquals("go", teamChat.pstr());
        PacketReader master = new PacketReader(GamePackets.decisionRoomMaster(7, 0));
        assertEquals(GamePackets.SERVER_DECISION_ROOM_MASTER, master.opcode());
        assertEquals(7, master.i32());
        assertEquals(0, master.i16());
        PacketReader offlineMiss = new PacketReader(GamePackets.userInfoOfflineMissing());
        assertEquals(GamePackets.SERVER_USERINFO_OFFLINE, offlineMiss.opcode());
        assertEquals(GamePackets.USERINFO_OFFLINE_MISSING, offlineMiss.u8());
        assertEquals(0, offlineMiss.remaining());
        PacketReader offlineFound = new PacketReader(GamePackets.userInfoOffline(
                10002, GamePackets.memberInfoExPublic(0, "testuser2", "TestNick2", 0)));
        assertEquals(GamePackets.SERVER_USERINFO_OFFLINE, offlineFound.opcode());
        assertEquals(GamePackets.USERINFO_OFFLINE_FOUND, offlineFound.u8());
        assertEquals(10002, offlineFound.u32());
        assertEquals(GamePackets.MEMBER_INFO_EX_BYTES, offlineFound.remaining());
    }
}
