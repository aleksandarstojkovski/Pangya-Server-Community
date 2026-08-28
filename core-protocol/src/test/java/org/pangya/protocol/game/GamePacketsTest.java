package org.pangya.protocol.game;

import org.junit.jupiter.api.Test;
import org.pangya.protocol.packet.PacketReader;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertEquals(GamePackets.LOGIN_NEW_MAIL_COUNT + GamePackets.LOGIN_DUMP_PREFIX_COUNT
                + GamePackets.LOGIN_DUMP_TAIL_COUNT, GamePackets.LOGIN_DUMP_PACKET_COUNT);
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
        PacketReader holes = new PacketReader(GamePackets.clientChangeRoomHoles(3, 1));
        assertEquals(GamePackets.CLIENT_CHANGE_ROOM_INFO, holes.opcode());
        assertEquals(3, holes.i16());
        assertEquals(1, holes.u8());
        assertEquals(GamePackets.ROOM_CHANGE_HOLES, holes.u8());
        assertEquals(1, holes.u8());
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
        PacketReader clock = new PacketReader(GamePackets.serverTime());
        assertEquals(GamePackets.SERVER_RESPONSE_SERVER_TIME, clock.opcode());
        assertEquals(16, clock.remaining());
        assertTrue(clock.u16() >= 2026);
    }

    @Test
    void finishGameAndItemSwapPacketsMatchCsharp() {
        PacketReader finish = new PacketReader(GamePackets.clientFinishGame());
        assertEquals(GamePackets.CLIENT_MY_STATISTICS, finish.opcode());
        assertEquals(GamePackets.USER_INFO_BYTES, finish.remaining());

        PacketReader hole = new PacketReader(GamePackets.clientHoleStat());
        assertEquals(GamePackets.CLIENT_HOLE_STAT, hole.opcode());
        assertEquals(GamePackets.USER_INFO_BYTES, hole.remaining());

        PacketReader pause = new PacketReader(GamePackets.clientPause(GamePackets.PAUSE_PAUSE));
        assertEquals(GamePackets.CLIENT_PAUSE, pause.opcode());
        assertEquals(GamePackets.PAUSE_PAUSE, pause.u8());

        PacketReader lobbyItem = new PacketReader(GamePackets.clientLobbyItem(GamePackets.ITEM_CHARACTER, 1));
        assertEquals(GamePackets.CLIENT_LOBBY_USERINFO_CHANGED, lobbyItem.opcode());
        assertEquals(GamePackets.ITEM_CHARACTER, lobbyItem.u8());
        assertEquals(1, lobbyItem.i32());

        PacketReader roomItem = new PacketReader(GamePackets.clientRoomItem(GamePackets.ITEM_CADDIE, 0));
        assertEquals(GamePackets.CLIENT_REQUEST_USERINFO_CHANGED, roomItem.opcode());
        assertEquals(GamePackets.ITEM_CADDIE, roomItem.u8());
        assertEquals(0, roomItem.i32());

        PacketReader all = new PacketReader(GamePackets.clientRoomItemAll(1, 0, 2, GamePackets.TYPEID_DEFAULT_BALL));
        assertEquals(GamePackets.CLIENT_REQUEST_USERINFO_CHANGED, all.opcode());
        assertEquals(GamePackets.ITEM_ALL, all.u8());
        assertEquals(1, all.i32());
        assertEquals(0, all.i32());
        assertEquals(2, all.i32());
        assertEquals(GamePackets.TYPEID_DEFAULT_BALL, all.i32());

        PacketReader stats = new PacketReader(GamePackets.myStatistics(GamePackets.userInfoPublic(1)));
        assertEquals(GamePackets.SERVER_MY_STATISTICS, stats.opcode());
        assertEquals(GamePackets.USER_INFO_BYTES + GamePackets.TROPHY_BYTES
                + GamePackets.MAP_STATISTICS_EMPTY_BYTES, stats.remaining());
        stats.readBytes(GamePackets.USER_INFO_BYTES);
        stats.readBytes(GamePackets.TROPHY_BYTES);
        for (int i = 0; i < GamePackets.MAP_STATISTICS_EMPTY_BYTES; i++) {
            assertEquals(0xff, stats.u8());
        }

        PacketReader prizes = new PacketReader(GamePackets.prizeList(new int[0]));
        assertEquals(GamePackets.SERVER_PRIZE_LIST, prizes.opcode());
        assertEquals(0, prizes.u8());
        assertEquals(0, prizes.u16());
        assertEquals(0, prizes.remaining());

        PacketReader result = new PacketReader(GamePackets.gameResult(0, 0, 0, 2));
        assertEquals(GamePackets.SERVER_GAME_RESULT, result.opcode());
        assertEquals(0, result.i32());
        assertEquals(0, result.u32());
        assertEquals(0, result.u8());
        assertEquals(2, result.u8());
        assertEquals(GamePackets.MEDAL_COUNT * GamePackets.MEDAL_BYTES
                + GamePackets.USER_MEDAL_BYTES, result.remaining());
        assertEquals(-1, result.i32());
        assertEquals(0, result.u32());

        PacketReader treasure = new PacketReader(GamePackets.treasureHunterItem());
        assertEquals(GamePackets.SERVER_UPDATE_TREASURE_GIFT_LIST, treasure.opcode());
        assertEquals(0, treasure.u8());

        PacketReader paused = new PacketReader(GamePackets.pause(7, GamePackets.PAUSE_PAUSE));
        assertEquals(GamePackets.SERVER_PAUSE, paused.opcode());
        assertEquals(7, paused.i32());
        assertEquals(GamePackets.PAUSE_PAUSE, paused.u8());

        byte[] extra = new byte[GamePackets.CHARACTER_INFO_BYTES];
        PacketReader changed = new PacketReader(GamePackets.roomUserInfoChanged(0, GamePackets.ITEM_CHARACTER, 7, extra));
        assertEquals(GamePackets.SERVER_ROOM_USER_INFO_CHANGED, changed.opcode());
        assertEquals(0, changed.i32());
        assertEquals(GamePackets.ITEM_CHARACTER, changed.u8());
        assertEquals(7, changed.i32());
        assertEquals(GamePackets.CHARACTER_INFO_BYTES, changed.remaining());

        PacketReader failed = new PacketReader(GamePackets.roomUserInfoChanged(2, GamePackets.ITEM_CADDIE, 0, new byte[0]));
        assertEquals(GamePackets.SERVER_ROOM_USER_INFO_CHANGED, failed.opcode());
        assertEquals(2, failed.i32());
        assertEquals(0, failed.remaining());

        PacketReader holeFinish = new PacketReader(GamePackets.updateHole(7, 1, 3, -1, 10, 2, 1));
        assertEquals(GamePackets.SERVER_UPDATE_HOLE, holeFinish.opcode());
        assertEquals(7, holeFinish.i32());
        assertEquals(1, holeFinish.u8());
        assertEquals(3, holeFinish.u8());
        assertEquals(-1, holeFinish.i32());
        assertEquals(10, holeFinish.u64());
        assertEquals(2, holeFinish.u64());
        assertEquals(1, holeFinish.u8());

        PacketReader state = new PacketReader(GamePackets.gamePlayerState(7, 2));
        assertEquals(GamePackets.SERVER_GAME_PLAYER_STATE, state.opcode());
        assertEquals(7, state.i32());
        assertEquals(2, state.u8());
        assertEquals(2, GamePackets.lastHole().length);
        assertEquals(GamePackets.SERVER_BUY_ACK, 0x68);
        assertEquals(GamePackets.CREATE_ROOM_FAILED, 0x07);
    }

    @Test
    void loungeSleepTeeshotGiftPacketsMatchCsharp() {
        PacketReader loc = new PacketReader(GamePackets.clientSyncActivityLocation(
                GamePackets.ACTION_LOUNGER_LOC, 1.5f, 2.5f, 0.25f));
        assertEquals(GamePackets.CLIENT_SYNC_ACTIVITY, loc.opcode());
        assertEquals(GamePackets.ACTION_LOUNGER_LOC, loc.u8());
        assertEquals(1.5f, loc.f32());
        assertEquals(2.5f, loc.f32());
        assertEquals(0.25f, loc.f32());
        assertEquals(0, loc.remaining());

        PacketReader rot = new PacketReader(GamePackets.clientSyncActivityRotation(1.0f));
        assertEquals(GamePackets.CLIENT_SYNC_ACTIVITY, rot.opcode());
        assertEquals(GamePackets.ACTION_ROTATION, rot.u8());
        assertEquals(1.0f, rot.f32());

        byte[] motion = {1, 2, 3};
        PacketReader sync = new PacketReader(GamePackets.syncActivity(7, GamePackets.ACTION_MOTION_ROOM, motion));
        assertEquals(GamePackets.SERVER_SYNC_ACTIVITY, sync.opcode());
        assertEquals(7, sync.i32());
        assertEquals(GamePackets.ACTION_MOTION_ROOM, sync.u8());
        assertEquals(3, sync.remaining());

        PacketReader sleep = new PacketReader(GamePackets.clientSleep(1));
        assertEquals(GamePackets.CLIENT_SLEEP, sleep.opcode());
        assertEquals(1, sleep.u8());

        PacketReader slept = new PacketReader(GamePackets.sleep(7, 1));
        assertEquals(GamePackets.SERVER_SLEEP, slept.opcode());
        assertEquals(7, slept.i32());
        assertEquals(1, slept.u8());
        assertEquals(GamePackets.PLAYER_AWAY_BIT, 1 << 2);
        assertEquals(GamePackets.PLAYER_LOBBY_AWAY_BIT, 1);

        PacketReader teeshot = new PacketReader(GamePackets.clientTeeshotReady());
        assertEquals(GamePackets.CLIENT_TEESHOT_READY, teeshot.opcode());
        assertEquals(0, teeshot.remaining());

        PacketReader ready = new PacketReader(GamePackets.teeshotReady());
        assertEquals(GamePackets.SERVER_TEESHOT_READY_ACK, ready.opcode());
        assertEquals(0, ready.remaining());

        PacketReader end = new PacketReader(GamePackets.clientEndStroke());
        assertEquals(GamePackets.CLIENT_END_STROKE_GAME, end.opcode());
        assertEquals(0, end.remaining());

        PacketReader emptyGift = new PacketReader(GamePackets.clientGiftEmpty(10002));
        assertEquals(GamePackets.CLIENT_REQUEST_GIFT_ITEM, emptyGift.opcode());
        assertEquals(0, emptyGift.u16());
        assertEquals(10002, emptyGift.u32());
        assertEquals("", emptyGift.pstr());
        assertEquals(0, emptyGift.u8());
        assertEquals(0, emptyGift.u16());

        PacketReader giftFail = new PacketReader(GamePackets.giftFailed(GamePackets.BUY_FAIL_EMPTY, 100000, 0));
        assertEquals(GamePackets.SERVER_RESPONSE_GIFT_ITEM, giftFail.opcode());
        assertEquals(GamePackets.BUY_FAIL_EMPTY, giftFail.u32());
        assertEquals(100000, giftFail.u64());
        assertEquals(0, giftFail.u64());

        PacketReader giftItem = new PacketReader(GamePackets.clientGiftItem(
                10002, GamePackets.TYPEID_SHOP_PANG_ITEM, 1, GamePackets.SHOP_PANG_PRICE, 0));
        assertEquals(GamePackets.CLIENT_REQUEST_GIFT_ITEM, giftItem.opcode());
        giftItem.u16();
        giftItem.u32();
        giftItem.pstr();
        giftItem.u8();
        assertEquals(1, giftItem.u16());
        assertEquals(0, giftItem.i32());
        assertEquals(GamePackets.TYPEID_SHOP_PANG_ITEM, giftItem.u32());

        PacketReader holeTurn = new PacketReader(GamePackets.holeTurn(7));
        assertEquals(GamePackets.SERVER_HOLE_TURN, holeTurn.opcode());
        assertEquals(7, holeTurn.i32());
        PacketReader playerTurn = new PacketReader(GamePackets.playerTurn(9));
        assertEquals(GamePackets.SERVER_PLAYER_TURN, playerTurn.opcode());
        assertEquals(9, playerTurn.i32());
        PacketReader go = new PacketReader(GamePackets.clientContinueVersus(GamePackets.CONTINUE_GO));
        assertEquals(GamePackets.CLIENT_ANSWER_GOSTOP, go.opcode());
        assertEquals(GamePackets.CONTINUE_GO, go.u8());
        PacketReader giftOk = new PacketReader(GamePackets.giftFailed(0, 99900, 0));
        assertEquals(GamePackets.SERVER_RESPONSE_GIFT_ITEM, giftOk.opcode());
        assertEquals(0, giftOk.u32());
        assertEquals(99900, giftOk.u64());
        assertEquals(GamePackets.GIFT_MIN_LEVEL, 6);
        assertEquals(GamePackets.GIFT_FAIL_MAIL, 8);
        assertEquals(GamePackets.SERVER_HOLE_TURN, 0x53);
        assertEquals(GamePackets.SERVER_PLAYER_TURN, GamePackets.CLIENT_SYNC_ACTIVITY);

        assertEquals(GamePackets.SERVER_BUY_ACK, 0x68);
        assertEquals(GamePackets.CREATE_ROOM_FAILED, 0x07);
        assertEquals(GamePackets.SERVER_RESPONSE_GIFT_ITEM, 0x6A);
        assertEquals(GamePackets.SERVER_SYNC_ACTIVITY, 0xC4);
        assertEquals(GamePackets.CLIENT_SYNC_ACTIVITY, 0x63);

        PacketReader teamHole = new PacketReader(GamePackets.clientTeamFinishHole(9));
        assertEquals(GamePackets.CLIENT_TEAM_HOLEIN_PANG, teamHole.opcode());
        assertEquals(9, teamHole.u16());

        PacketReader cont = new PacketReader(GamePackets.clientContinueVersus(GamePackets.CONTINUE_STOP));
        assertEquals(GamePackets.CLIENT_ANSWER_GOSTOP, cont.opcode());
        assertEquals(GamePackets.CONTINUE_STOP, cont.u8());

        PacketReader holiday = new PacketReader(GamePackets.clientPayCaddieHoliday(7));
        assertEquals(GamePackets.CLIENT_REEMPLOY_CADDIE, holiday.opcode());
        assertEquals(7, holiday.i32());
        PacketReader holidayFail = new PacketReader(GamePackets.caddieHolidayFail());
        assertEquals(GamePackets.SERVER_REEMPLOY_CADDIE_ACK, holidayFail.opcode());
        assertEquals(GamePackets.CADDIE_HOLIDAY_FAIL, holidayFail.u8());
        PacketReader holidayOk = new PacketReader(GamePackets.caddieHolidayOk(20, 99000));
        assertEquals(GamePackets.SERVER_REEMPLOY_CADDIE_ACK, holidayOk.opcode());
        assertEquals(GamePackets.CADDIE_HOLIDAY_OK, holidayOk.u8());
        assertEquals(20, holidayOk.i32());
        assertEquals(99000, holidayOk.u64());

        PacketReader report = new PacketReader(GamePackets.clientReport());
        assertEquals(GamePackets.CLIENT_REPORT, report.opcode());
        assertEquals(0, report.remaining());
        PacketReader reported = new PacketReader(GamePackets.reportAck(GamePackets.REPORT_OK));
        assertEquals(GamePackets.SERVER_REPORT, reported.opcode());
        assertEquals(GamePackets.REPORT_OK, reported.u8());
        assertEquals(GamePackets.SERVER_REPORT, 0x94);
        assertEquals(GamePackets.SERVER_REEMPLOY_CADDIE_ACK, 0x93);

        PacketReader penalty = new PacketReader(GamePackets.clientChatPenalty(1));
        assertEquals(GamePackets.CLIENT_CHAT_PENALITY, penalty.opcode());
        assertEquals(1, penalty.u8());
        PacketReader blocked = new PacketReader(GamePackets.chatPenalty(7, 1));
        assertEquals(GamePackets.SERVER_CHAT_PENALITY, blocked.opcode());
        assertEquals(7, blocked.i32());
        assertEquals(1, blocked.u8());

        PacketReader boost = new PacketReader(GamePackets.clientSpeedRate(1.5f));
        assertEquals(GamePackets.CLIENT_SPEED_RATE, boost.opcode());
        assertEquals(1.5f, boost.f32());
        PacketReader rate = new PacketReader(GamePackets.speedRate(1.5f, 7));
        assertEquals(GamePackets.SERVER_SPEED_RATE, rate.opcode());
        assertEquals(1.5f, rate.f32());
        assertEquals(7, rate.i32());

        PacketReader tq = new PacketReader(GamePackets.clientTickerQuery());
        assertEquals(GamePackets.CLIENT_ONELINE_QUERY, tq.opcode());
        PacketReader queued = new PacketReader(GamePackets.tickerQueue(2, 60000));
        assertEquals(GamePackets.SERVER_ONELINE_QUERY, queued.opcode());
        assertEquals(2, queued.u16());
        assertEquals(60000, queued.u32());
        PacketReader tfail = new PacketReader(GamePackets.tickerFail(GamePackets.TICKER_FAIL_FUNDS));
        assertEquals(GamePackets.SERVER_CHANGE_NICK_ACK, tfail.opcode());
        assertEquals(GamePackets.TICKER_FAIL_FUNDS, tfail.u32());
        PacketReader tmsg = new PacketReader(GamePackets.tickerMsg("TestNick", "hello"));
        assertEquals(GamePackets.SERVER_ONELINE_MSG, tmsg.opcode());
        assertEquals("TestNick", tmsg.pstr());
        assertEquals("hello", tmsg.pstr());
        PacketReader notice = new PacketReader(GamePackets.clientNotice("gm"));
        assertEquals(GamePackets.CLIENT_NOTICE, notice.opcode());
        assertEquals("gm", notice.pstr());
        PacketReader destroy = new PacketReader(GamePackets.clientDestroyRoom(1));
        assertEquals(GamePackets.CLIENT_DESTROY_ROOM, destroy.opcode());
        assertEquals(1, destroy.i16());

        PacketReader mascot = new PacketReader(GamePackets.mascotMessageFail(100000));
        assertEquals(GamePackets.SERVER_CHANGE_MASCOT, mascot.opcode());
        assertEquals(0xff, mascot.u8());
        assertEquals(-1, mascot.i32());
        assertEquals(0, mascot.u16());
        assertEquals(100000, mascot.u64());
        PacketReader mascotOk = new PacketReader(GamePackets.mascotMessageOk(21, "hello", 99900));
        assertEquals(GamePackets.SERVER_CHANGE_MASCOT, mascotOk.opcode());
        assertEquals(GamePackets.MASCOT_MSG_OK, mascotOk.u8());
        assertEquals(21, mascotOk.i32());
        assertEquals("hello", mascotOk.pstr());
        assertEquals(99900, mascotOk.u64());
        PacketReader timeout = new PacketReader(GamePackets.timeout(9));
        assertEquals(GamePackets.SERVER_TIMEOUT, timeout.opcode());
        assertEquals(9, timeout.i32());

        assertEquals(GamePackets.SERVER_ONELINE_MSG, 0xC9);
        assertEquals(GamePackets.SERVER_CHAT_PENALITY, 0xAC);
        assertEquals(GamePackets.SERVER_BUY_ACK, 0x68);
        assertEquals(GamePackets.CREATE_ROOM_FAILED, 0x07);

        PacketReader msnOff = new PacketReader(GamePackets.clientMsnMsgOff(10002, "offline", 0));
        assertEquals(GamePackets.CLIENT_MSN_REQUEST, msnOff.opcode());
        assertEquals(GamePackets.MSN_MSG_OFF, msnOff.u16());
        assertEquals(10002, msnOff.u32());
        assertEquals("offline", msnOff.pstr());
        assertEquals(0, msnOff.u8());
        PacketReader msnOk = new PacketReader(GamePackets.msnAckOk(GamePackets.MSN_MSG_OFF, 99890));
        assertEquals(GamePackets.SERVER_MSN_ACK, msnOk.opcode());
        assertEquals(GamePackets.MSN_MSG_OFF, msnOk.u16());
        assertEquals(0, msnOk.u32());
        assertEquals(99890, msnOk.u64());
        PacketReader msnFail = new PacketReader(GamePackets.msnAckFail(
                GamePackets.MSN_FRIEND_LIST, GamePackets.MSN_ERR_FUNDS));
        assertEquals(GamePackets.SERVER_MSN_ACK, msnFail.opcode());
        assertEquals(GamePackets.MSN_FRIEND_LIST, msnFail.u16());
        assertEquals(GamePackets.MSN_ERR_FUNDS, msnFail.u32());
        PacketReader arrows = new PacketReader(GamePackets.clientShotArrows(0x11, 0x22));
        assertEquals(GamePackets.CLIENT_SHOT_COMMAND, arrows.opcode());
        assertEquals(2, arrows.u8());
        assertEquals(0x11, arrows.u32());
        assertEquals(0x22, arrows.u32());
        PacketReader replay = new PacketReader(GamePackets.clientReplay(0x1A000001));
        assertEquals(GamePackets.CLIENT_REPLAY_ONLINE, replay.opcode());
        assertEquals(0x1A000001, replay.u32());
        PacketReader replayAck = new PacketReader(GamePackets.replay(1));
        assertEquals(GamePackets.SERVER_REPLAY, replayAck.opcode());
        assertEquals(1, replayAck.u16());
        PacketReader autoCmd = new PacketReader(GamePackets.autoCommandFail(GamePackets.STDA_ERROR_TYPE_GAME));
        assertEquals(GamePackets.SERVER_AUTO_COMMAND_ACK, autoCmd.opcode());
        assertEquals(GamePackets.STDA_ERROR_TYPE_GAME, autoCmd.u32());
        assertEquals(GamePackets.SERVER_MSN_ACK, 0x95);
        assertEquals(GamePackets.CLIENT_REPORT_ERROR, 0x33);
        assertEquals(GamePackets.SERVER_BUY_ACK, 0x68);
        assertEquals(GamePackets.CREATE_ROOM_FAILED, 0x07);

        PacketReader edit = new PacketReader(GamePackets.shopEditOk("TestNick", 10001));
        assertEquals(GamePackets.SERVER_SHOP_EDIT, edit.opcode());
        assertEquals(GamePackets.SHOP_OK, edit.u32());
        assertEquals("TestNick", edit.pstr());
        assertEquals(10001, edit.u32());
        PacketReader cancelFail = new PacketReader(
                GamePackets.shopCancelFail(GamePackets.shopSys(GamePackets.SHOP_ERR_CANCEL_NONE)));
        assertEquals(GamePackets.SERVER_SHOP_CANCEL, cancelFail.opcode());
        assertEquals(GamePackets.shopSys(GamePackets.SHOP_ERR_CANCEL_NONE), cancelFail.u32());
        PacketReader papel = new PacketReader(GamePackets.papelShopOk(0));
        assertEquals(GamePackets.SERVER_PAPEL_SHOP, papel.opcode());
        assertEquals(0, papel.u32());
        assertEquals(0, papel.u64());
        PacketReader enter = new PacketReader(GamePackets.enterShopOk());
        assertEquals(GamePackets.SERVER_ENTER_SHOP, enter.opcode());
        assertEquals(0, enter.u32());
        assertEquals(0, enter.u32());
        assertEquals(GamePackets.CLIENT_SHOP_OPEN_EDIT, 0x76);
        assertEquals(GamePackets.CLIENT_ENTER_SHOP, 0x140);
        PacketReader mailbox = new PacketReader(GamePackets.clientOpenMailBox(1));
        assertEquals(GamePackets.CLIENT_OPEN_MAILBOX, mailbox.opcode());
        assertEquals(1, mailbox.i32());
        PacketReader emptyBox = new PacketReader(GamePackets.mailBoxPage(
                GamePackets.SERVER_MAILBOX, 0, 1, 1, List.of()));
        assertEquals(GamePackets.SERVER_MAILBOX, emptyBox.opcode());
        assertEquals(0, emptyBox.i32());
        assertEquals(1, emptyBox.i32());
        assertEquals(1, emptyBox.i32());
        assertEquals(0, emptyBox.i32());
        byte[] row = GamePackets.mailBoxEntry(7, "TestNick", "hello", 0, 0, 0);
        assertEquals(GamePackets.MAIL_BOX_ENTRY_BYTES, row.length);
        PacketReader info = new PacketReader(GamePackets.mailInfoOk(7, "TestNick", "28/08/2026", "hello", 1));
        assertEquals(GamePackets.SERVER_MAIL_INFO, info.opcode());
        assertEquals(0, info.u32());
        assertEquals(7, info.i32());
        assertEquals("TestNick", info.pstr());
        assertEquals("28/08/2026", info.pstr());
        assertEquals("hello", info.pstr());
        assertEquals(1, info.u8());
        assertEquals(0, info.i32());
        assertEquals(GamePackets.MAIL_ITEM_BYTES, info.remaining());
        PacketReader sendOk = new PacketReader(GamePackets.mailFail(GamePackets.SERVER_MAIL_SEND, 0));
        assertEquals(GamePackets.SERVER_MAIL_SEND, sendOk.opcode());
        assertEquals(0, sendOk.u32());
        PacketReader unread = new PacketReader(GamePackets.newMail(List.of()));
        assertEquals(GamePackets.SERVER_NEW_MAIL, unread.opcode());
        assertEquals(0, unread.i32());
        assertEquals(0, unread.i32());
        PacketReader sendPkt = new PacketReader(GamePackets.clientSendMail(
                10001, 10002, "TestNick2", 0, "hello", GamePackets.MAIL_SEND_PANG, 0, null));
        assertEquals(GamePackets.CLIENT_SEND_MAIL, sendPkt.opcode());
        assertEquals(10001, sendPkt.u32());
        assertEquals(10002, sendPkt.u32());
        assertEquals("TestNick2", sendPkt.pstr());
        assertEquals(0, sendPkt.u16());
        assertEquals("hello", sendPkt.pstr());
        assertEquals(GamePackets.MAIL_SEND_PANG, sendPkt.u64());
        assertEquals(0, sendPkt.u8());
        assertEquals(GamePackets.CLIENT_OPEN_MAILBOX, 0x143);
        assertEquals(GamePackets.CLIENT_DELETE_MAIL, 0x147);
        assertEquals(GamePackets.SERVER_MAILBOX, 0x211);
        assertEquals(GamePackets.SERVER_MAIL_DELETE, 0x215);
        PacketReader last5 = new PacketReader(GamePackets.last5Players());
        assertEquals(GamePackets.SERVER_LAST5, last5.opcode());
        assertEquals(GamePackets.LAST5_COUNT * GamePackets.LAST5_PLAYER_BYTES, last5.remaining());
        assertEquals(GamePackets.CLIENT_USER_MATCH_HISTORY, 0x9C);
        assertEquals(GamePackets.SERVER_LAST5, 0x10E);

        PacketReader stamp = new PacketReader(GamePackets.dailyQuestStamp(1_700_000_000, 0));
        assertEquals(GamePackets.SERVER_DAILY_QUEST_STAMP, stamp.opcode());
        assertEquals(1_700_000_000, stamp.i32());
        assertEquals(0, stamp.i32());
        assertEquals(0, stamp.remaining());
        PacketReader dqInfo = new PacketReader(GamePackets.dailyQuestInfo(
                0, 1_700_000_000, 0, 0, new int[GamePackets.DAILY_QUEST_TYPEID_COUNT], null));
        assertEquals(GamePackets.SERVER_DAILY_QUEST_INFO, dqInfo.opcode());
        assertEquals(0, dqInfo.i32());
        assertEquals(1_700_000_000, dqInfo.u32());
        assertEquals(0, dqInfo.u32());
        assertEquals(0, dqInfo.u32());
        assertEquals(0, dqInfo.u32());
        assertEquals(0, dqInfo.u32());
        assertEquals(0, dqInfo.u32());
        assertEquals(0, dqInfo.i32());
        assertEquals(0, dqInfo.remaining());
        PacketReader acceptFail = new PacketReader(GamePackets.dailyQuestAcceptFail());
        assertEquals(GamePackets.SERVER_DAILY_QUEST_ACCEPT, acceptFail.opcode());
        assertEquals(GamePackets.DAILY_QUEST_ACCEPT_FAIL, acceptFail.i32());
        assertEquals(0, acceptFail.i32());
        PacketReader rewardFail = new PacketReader(GamePackets.dailyQuestRewardFail());
        assertEquals(GamePackets.SERVER_DAILY_QUEST_REWARD, rewardFail.opcode());
        assertEquals(GamePackets.DAILY_QUEST_REWARD_FAIL, rewardFail.i32());
        assertEquals(0, rewardFail.i32());
        PacketReader leaveFail = new PacketReader(GamePackets.dailyQuestLeaveFail());
        assertEquals(GamePackets.SERVER_DAILY_QUEST_LEAVE, leaveFail.opcode());
        assertEquals(GamePackets.DAILY_QUEST_LEAVE_FAIL, leaveFail.i32());
        assertEquals(0, leaveFail.remaining());
        PacketReader delFail = new PacketReader(GamePackets.deleteItemFail());
        assertEquals(GamePackets.SERVER_DELETE_ITEM, delFail.opcode());
        assertEquals(GamePackets.DELETE_ITEM_FAIL, delFail.u8());
        PacketReader achFail = new PacketReader(GamePackets.achievementGui(GamePackets.ACHIEVEMENT_GUI_FAIL));
        assertEquals(GamePackets.SERVER_ACHIEVEMENT_GUI, achFail.opcode());
        assertEquals(1, achFail.i32());
        assertEquals(0, GamePackets.itemGroupIdentify(1));
        assertEquals(GamePackets.IFF_GROUP_ITEM, GamePackets.itemGroupIdentify(0x1A000006));
        assertEquals(1_700_000_000, GamePackets.tzLocalUnixToUnixUtc(1_700_000_000));
        assertEquals(GamePackets.CLIENT_DAILY_QUEST, 0x151);
        assertEquals(GamePackets.CLIENT_ACCEPT_DAILY_QUEST, 0x152);
        assertEquals(GamePackets.CLIENT_REWARD_DAILY_QUEST, 0x153);
        assertEquals(GamePackets.CLIENT_LEAVE_DAILY_QUEST, 0x154);
        assertEquals(GamePackets.CLIENT_ACHIEVEMENT, 0x157);
        assertEquals(GamePackets.CLIENT_DELETE_ITEM, 0x64);
        assertEquals(GamePackets.CLIENT_ENTER_OTHER_CHANNEL, 0x83);
        assertEquals(GamePackets.CLIENT_GAMEGUARD, 0x88);
        assertEquals(GamePackets.SERVER_DAILY_QUEST_STAMP, 0x216);
        assertEquals(GamePackets.SERVER_DAILY_QUEST_INFO, 0x225);
        assertEquals(GamePackets.SERVER_DELETE_ITEM, 0xC5);
        assertEquals(GamePackets.SERVER_ACHIEVEMENT_GUI, 0x22C);
        PacketReader cadieFail = new PacketReader(GamePackets.cadieFail(GamePackets.shopSys(GamePackets.CADIE_ERR_COUNT)));
        assertEquals(GamePackets.SERVER_CADIE, cadieFail.opcode());
        assertEquals(GamePackets.shopSys(GamePackets.CADIE_ERR_COUNT), cadieFail.u32());
        PacketReader cadieOk = new PacketReader(GamePackets.cadieOk(
                0, GamePackets.TYPEID_SHOP_PANG_ITEM, 9, 1, 1, 0));
        assertEquals(GamePackets.SERVER_CADIE, cadieOk.opcode());
        assertEquals(0, cadieOk.u32());
        assertEquals(0, cadieOk.u32());
        assertEquals(1, cadieOk.u32());
        assertEquals(GamePackets.TYPEID_SHOP_PANG_ITEM, cadieOk.u32());
        assertEquals(9, cadieOk.i32());
        assertEquals(1, cadieOk.i32());
        assertEquals(1, cadieOk.i32());
        assertEquals(0, cadieOk.u32());
        PacketReader cadieItems = new PacketReader(
                GamePackets.clientCadieItems(0, 1, GamePackets.TYPEID_SHOP_PANG_ITEM, 9));
        assertEquals(GamePackets.CLIENT_CADIE, cadieItems.opcode());
        assertEquals(0, cadieItems.u16());
        assertEquals(1, cadieItems.u32());
        assertEquals(1, cadieItems.u8());
        assertEquals(GamePackets.TYPEID_SHOP_PANG_ITEM, cadieItems.u32());
        assertEquals(9, cadieItems.i32());
        PacketReader loloFail = new PacketReader(GamePackets.loloFail(GamePackets.shopSys(GamePackets.LOLO_ERR_IFF)));
        assertEquals(GamePackets.SERVER_LOLO, loloFail.opcode());
        assertEquals(GamePackets.shopSys(GamePackets.LOLO_ERR_IFF), loloFail.u32());
        PacketReader loloTipo = new PacketReader(GamePackets.loloTipo(GamePackets.CARD_TYPE_NORMAL));
        assertEquals(GamePackets.SERVER_LOLO_TIPO, loloTipo.opcode());
        assertEquals(GamePackets.CARD_TYPE_NORMAL, loloTipo.u32());
        PacketReader loloOk = new PacketReader(GamePackets.loloOk(GamePackets.TYPEID_CARD_NORMAL));
        assertEquals(GamePackets.SERVER_LOLO, loloOk.opcode());
        assertEquals(0, loloOk.u32());
        assertEquals(GamePackets.TYPEID_CARD_NORMAL, loloOk.u32());
        assertEquals(GamePackets.CLIENT_CADIE, 0x158);
        assertEquals(GamePackets.CLIENT_LOLO, 0x155);
        assertEquals(GamePackets.SERVER_CADIE, 0x22F);
        assertEquals(GamePackets.SERVER_LOLO, 0x22A);
        assertEquals(GamePackets.SERVER_LOLO_TIPO, 0x229);
        PacketReader msnList = new PacketReader(GamePackets.messengerList(List.of()));
        assertEquals(GamePackets.SERVER_MESSENGER_LIST, msnList.opcode());
        assertEquals(0, msnList.u8());
        assertEquals(0, msnList.remaining());
        PacketReader gachaOk = new PacketReader(GamePackets.gachaCoupon(0, 0, 100000, 0));
        assertEquals(GamePackets.SERVER_GACHA_COUPON, gachaOk.opcode());
        assertEquals(0, gachaOk.i32());
        assertEquals(0, gachaOk.i32());
        assertEquals(100000, gachaOk.u64());
        assertEquals(0, gachaOk.u64());
        PacketReader gachaFail = new PacketReader(GamePackets.gachaCouponFail(GamePackets.GACHA_ERR_DEFAULT));
        assertEquals(GamePackets.SERVER_LOGIN_ACK, gachaFail.opcode());
        assertEquals(GamePackets.GACHA_ERR_MARKER, gachaFail.u8());
        assertEquals(GamePackets.GACHA_ERR_DEFAULT, gachaFail.u32());
        PacketReader clubFail = new PacketReader(GamePackets.clubStatsFail());
        assertEquals(GamePackets.SERVER_CLUB_STATS, clubFail.opcode());
        assertEquals(GamePackets.CLUB_STATS_ERR, clubFail.u8());
        PacketReader intrusion = new PacketReader(GamePackets.intrusionFail(GamePackets.INTRUSION_SYS));
        assertEquals(GamePackets.SERVER_INTRUSION, intrusion.opcode());
        assertEquals(GamePackets.INTRUSION_ERR, intrusion.u8());
        assertEquals(GamePackets.INTRUSION_SYS, intrusion.u8());
        PacketReader papelPlay = new PacketReader(GamePackets.papelPlayFail(
                GamePackets.shopSys(GamePackets.PAPEL_PLAY_ERR_BALLS)));
        assertEquals(GamePackets.SERVER_PAPEL_PLAY, papelPlay.opcode());
        assertEquals(GamePackets.shopSys(GamePackets.PAPEL_PLAY_ERR_BALLS), papelPlay.u32());
        assertEquals(0x0103, GamePackets.shopSys(GamePackets.PAPEL_PLAY_ERR_BALLS));

        PacketReader webOk = new PacketReader(GamePackets.webAuthKey(GamePackets.WEB_KEY_OK, "ABCDEF"));
        assertEquals(GamePackets.SERVER_WEB_AUTH_KEY, webOk.opcode());
        assertEquals(GamePackets.WEB_KEY_OK, webOk.i32());
        assertEquals("ABCDEF", webOk.pstr());
        PacketReader webFail = new PacketReader(GamePackets.webAuthKey(GamePackets.WEB_KEY_FAIL, ""));
        assertEquals(GamePackets.SERVER_WEB_AUTH_KEY, webFail.opcode());
        assertEquals(GamePackets.WEB_KEY_FAIL, webFail.i32());
        assertEquals(0, webFail.i16());
        PacketReader gsOk = new PacketReader(GamePackets.changeGameServer(GamePackets.CHANGE_GS_OK, "KEY12345"));
        assertEquals(GamePackets.SERVER_CHANGE_GAME_SERVER, gsOk.opcode());
        assertEquals(GamePackets.CHANGE_GS_OK, gsOk.i32());
        assertEquals("KEY12345", gsOk.pstr());
        PacketReader gsSkip = new PacketReader(GamePackets.changeGameServer(1, "KEY"));
        assertEquals(GamePackets.SERVER_CHANGE_GAME_SERVER, gsSkip.opcode());
        assertEquals(1, gsSkip.i32());
        assertEquals(0, gsSkip.remaining());
        PacketReader ticket = new PacketReader(GamePackets.ticketReportFail());
        assertEquals(GamePackets.SERVER_TICKET_REPORT, ticket.opcode());
        assertEquals(GamePackets.TICKET_REPORT_ERR, ticket.i32());
        assertEquals(16, ticket.remaining());
        PacketReader tiki = new PacketReader(GamePackets.tikiShop(0));
        assertEquals(GamePackets.SERVER_TIKI_SHOP, tiki.opcode());
        assertEquals(0, tiki.u32());
        PacketReader locker = new PacketReader(GamePackets.lockerAccess(GamePackets.LOCKER_ERR_WRONG));
        assertEquals(GamePackets.SERVER_LOCKER_ACCESS, locker.opcode());
        assertEquals(GamePackets.LOCKER_ERR_WRONG, locker.u32());
        PacketReader lockerSt = new PacketReader(GamePackets.lockerState(GamePackets.LOCKER_STATE_NO_PASS));
        assertEquals(GamePackets.SERVER_LOCKER_STATE, lockerSt.opcode());
        assertEquals(0, lockerSt.u32());
        assertEquals(GamePackets.LOCKER_STATE_NO_PASS, lockerSt.u32());
        PacketReader workshop = new PacketReader(GamePackets.clubWorkshopFail(
                GamePackets.shopSys(GamePackets.WORKSHOP_ERR_GROUP)));
        assertEquals(GamePackets.SERVER_CLUB_WORKSHOP_LEVEL, workshop.opcode());
        assertEquals(0x0201, workshop.u32());
        PacketReader pouch = new PacketReader(GamePackets.luckyPouchFail());
        assertEquals(GamePackets.SERVER_LUCKY_POUCH, pouch.opcode());
        assertEquals(GamePackets.LUCKY_POUCH_ERR, pouch.u8());
        assertEquals(12, pouch.remaining());
        PacketReader tuto = new PacketReader(GamePackets.tutorialFail(
                GamePackets.shopSys(GamePackets.TUTORIAL_ERR_TIPO)));
        assertEquals(GamePackets.SERVER_LOGIN_ACK, tuto.opcode());
        assertEquals(GamePackets.GACHA_ERR_MARKER, tuto.u8());
        assertEquals(0x0552, tuto.u32());
        PacketReader webClient = new PacketReader(GamePackets.clientWebAuthKey());
        assertEquals(GamePackets.CLIENT_WEB_AUTH_KEY, webClient.opcode());
        assertEquals(0, webClient.remaining());
        PacketReader gsClient = new PacketReader(GamePackets.clientChangeGameServer(20202));
        assertEquals(GamePackets.CLIENT_CHANGE_GAME_SERVER, gsClient.opcode());
        assertEquals(20202, gsClient.u32());
        PacketReader quest = new PacketReader(GamePackets.clientCompleteQuest(99, 1));
        assertEquals(GamePackets.CLIENT_COMPLETE_QUEST, quest.opcode());
        assertEquals(0, quest.u8());
        assertEquals(99, quest.u8());
        assertEquals(1, quest.u32());
        PacketReader cw = new PacketReader(GamePackets.clientClubWorkshopLevel(0, 1, 7));
        assertEquals(GamePackets.CLIENT_CLUB_WORKSHOP_LEVEL, cw.opcode());
        assertEquals(0, cw.u32());
        assertEquals(1, cw.u16());
        assertEquals(7, cw.i32());
        PacketReader tikiPts = new PacketReader(GamePackets.tikiPoints(0, 0));
        assertEquals(GamePackets.SERVER_TIKI_POINTS, tikiPts.opcode());
        assertEquals(0, tikiPts.u32());
        assertEquals(0, tikiPts.u32());
        PacketReader tikiTp = new PacketReader(GamePackets.tikiExchangeFail(
                GamePackets.SERVER_TIKI_EXCHANGE_TP, GamePackets.shopSys(GamePackets.TIKI_EXCHANGE_ERR_PTS)));
        assertEquals(GamePackets.SERVER_TIKI_EXCHANGE_TP, tikiTp.opcode());
        assertEquals(0x0905, tikiTp.u32());
        PacketReader tikiItem = new PacketReader(GamePackets.tikiExchangeFail(
                GamePackets.SERVER_TIKI_EXCHANGE_ITEM, GamePackets.shopSys(GamePackets.TIKI_EXCHANGE_ERR_PTS)));
        assertEquals(GamePackets.SERVER_TIKI_EXCHANGE_ITEM, tikiItem.opcode());
        assertEquals(0x0905, tikiItem.u32());
        PacketReader confirm = new PacketReader(GamePackets.clubWorkshopOpcodeFail(
                GamePackets.SERVER_CLUB_WORKSHOP_CONFIRM,
                GamePackets.shopSys(GamePackets.WORKSHOP_CONFIRM_ERR)));
        assertEquals(GamePackets.SERVER_CLUB_WORKSHOP_CONFIRM, confirm.opcode());
        assertEquals(0x0301, confirm.u32());
        PacketReader cancel = new PacketReader(GamePackets.clubWorkshopOpcodeFail(
                GamePackets.SERVER_CLUB_WORKSHOP_CANCEL,
                GamePackets.shopSys(GamePackets.WORKSHOP_CANCEL_ERR)));
        assertEquals(GamePackets.SERVER_CLUB_WORKSHOP_CANCEL, cancel.opcode());
        assertEquals(0x0251, cancel.u32());
        PacketReader rank = new PacketReader(GamePackets.clubWorkshopOpcodeFail(
                GamePackets.SERVER_CLUB_WORKSHOP_RANK,
                GamePackets.shopSys(GamePackets.WORKSHOP_RANK_ERR)));
        assertEquals(GamePackets.SERVER_CLUB_WORKSHOP_RANK, rank.opcode());
        assertEquals(0x0351, rank.u32());
        PacketReader buff = new PacketReader(GamePackets.itemBuffFail(
                GamePackets.shopSys(GamePackets.BUFF_ERR_TYPEID)));
        assertEquals(GamePackets.SERVER_ITEM_BUFF, buff.opcode());
        assertEquals(0x0401, buff.u32());
        PacketReader comet = new PacketReader(GamePackets.cometRefillFail());
        assertEquals(GamePackets.SERVER_COMET_REFILL, comet.opcode());
        assertEquals(0, comet.u8());
        assertEquals(10, comet.remaining());
        PacketReader boxMail = new PacketReader(GamePackets.boxMailFail(
                GamePackets.shopSys(GamePackets.BOX_MAIL_ERR_TYPEID)));
        assertEquals(GamePackets.SERVER_BOX_MAIL, boxMail.opcode());
        assertEquals(0x0101, boxMail.u32());
        PacketReader lockerPage = new PacketReader(GamePackets.lockerItems(0, 0, 0));
        assertEquals(GamePackets.SERVER_LOCKER_ITEMS, lockerPage.opcode());
        assertEquals(0, lockerPage.u16());
        assertEquals(0, lockerPage.u16());
        assertEquals(0, lockerPage.u8());
        PacketReader lockerPang = new PacketReader(GamePackets.lockerPang(0));
        assertEquals(GamePackets.SERVER_LOCKER_PANG, lockerPang.opcode());
        assertEquals(0, lockerPang.u64());
        PacketReader refuse = new PacketReader(GamePackets.chatRefuseWhisper("TestNick"));
        assertEquals(GamePackets.SERVER_CHAT, refuse.opcode());
        assertEquals(GamePackets.CHAT_REFUSE_WHISPER, refuse.u8());
        assertEquals("TestNick", refuse.pstr());
        assertEquals(0, refuse.remaining());
        PacketReader myRoom = new PacketReader(GamePackets.myRoomCheck(GamePackets.MY_ROOM_DENY, 10001));
        assertEquals(GamePackets.SERVER_MY_ROOM, myRoom.opcode());
        assertEquals(0, myRoom.u32());
        assertEquals(10001, myRoom.u32());
        PacketReader makePass = new PacketReader(GamePackets.sysAck(
                GamePackets.SERVER_LOCKER_MAKE_PASS, GamePackets.LOCKER_MAKE_PASS_EMPTY));
        assertEquals(GamePackets.SERVER_LOCKER_MAKE_PASS, makePass.opcode());
        assertEquals(1, makePass.u32());
        PacketReader changePass = new PacketReader(GamePackets.sysAck(
                GamePackets.SERVER_LOCKER_CHANGE_PASS, GamePackets.LOCKER_CHANGE_PASS_WRONG));
        assertEquals(GamePackets.SERVER_LOCKER_CHANGE_PASS, changePass.opcode());
        assertEquals(1, changePass.u32());
        PacketReader lockerMode = new PacketReader(GamePackets.sysAck(
                GamePackets.SERVER_LOCKER_MODE, GamePackets.shopSys(GamePackets.LOCKER_MODE_EMPTY)));
        assertEquals(GamePackets.SERVER_LOCKER_MODE, lockerMode.opcode());
        assertEquals(GamePackets.shopSys(5100251), lockerMode.u32());
        PacketReader lockerAdd = new PacketReader(GamePackets.sysAck(
                GamePackets.SERVER_LOCKER_ADD, GamePackets.shopSys(GamePackets.LOCKER_ADD_ERR_NONE)));
        assertEquals(GamePackets.SERVER_LOCKER_ADD, lockerAdd.opcode());
        assertEquals(GamePackets.shopSys(5100404), lockerAdd.u32());
        PacketReader lockerRm = new PacketReader(GamePackets.sysAck(
                GamePackets.SERVER_LOCKER_REMOVE, GamePackets.LOCKER_REMOVE_ERR_DEFAULT));
        assertEquals(GamePackets.SERVER_LOCKER_REMOVE, lockerRm.opcode());
        assertEquals(5100450, lockerRm.u32());
        PacketReader lockerUp = new PacketReader(GamePackets.sysAck(
                GamePackets.SERVER_LOCKER_UPDATE_PANG,
                GamePackets.shopSys(GamePackets.LOCKER_PANG_WITHDRAW_ERR)));
        assertEquals(GamePackets.SERVER_LOCKER_UPDATE_PANG, lockerUp.opcode());
        assertEquals(GamePackets.shopSys(5100353), lockerUp.u32());
        PacketReader cardPack = new PacketReader(GamePackets.sysAck(
                GamePackets.SERVER_OPEN_CARD_PACK, GamePackets.CARD_PACK_ERR));
        assertEquals(GamePackets.SERVER_OPEN_CARD_PACK, cardPack.opcode());
        assertEquals(1, cardPack.u32());
        PacketReader useCard = new PacketReader(GamePackets.sysAck(
                GamePackets.SERVER_USE_CARD, GamePackets.shopSys(GamePackets.CARD_ERR_TYPEID)));
        assertEquals(GamePackets.SERVER_USE_CARD, useCard.opcode());
        assertEquals(0x0351, useCard.u32());
        PacketReader extend = new PacketReader(GamePackets.rentalFail(GamePackets.SERVER_EXTEND_RENTAL));
        assertEquals(GamePackets.SERVER_EXTEND_RENTAL, extend.opcode());
        assertEquals(1, extend.u8());
        PacketReader deleteRental = new PacketReader(GamePackets.rentalFail(GamePackets.SERVER_DELETE_RENTAL));
        assertEquals(GamePackets.SERVER_DELETE_RENTAL, deleteRental.opcode());
        assertEquals(1, deleteRental.u8());
        PacketReader xfConfirm = new PacketReader(GamePackets.sysAck(
                GamePackets.SERVER_WORKSHOP_TRANSFORM_CONFIRM,
                GamePackets.shopSys(GamePackets.WORKSHOP_TRANSFORM_CONFIRM_ERR)));
        assertEquals(GamePackets.SERVER_WORKSHOP_TRANSFORM_CONFIRM, xfConfirm.opcode());
        assertEquals(0x0451, xfConfirm.u32());
        PacketReader xfCancel = new PacketReader(GamePackets.sysAck(
                GamePackets.SERVER_WORKSHOP_TRANSFORM_CANCEL,
                GamePackets.shopSys(GamePackets.WORKSHOP_TRANSFORM_CANCEL_ERR)));
        assertEquals(GamePackets.SERVER_WORKSHOP_TRANSFORM_CANCEL, xfCancel.opcode());
        assertEquals(0x0401, xfCancel.u32());
        PacketReader recovery = new PacketReader(GamePackets.sysAck(
                GamePackets.SERVER_WORKSHOP_RECOVERY,
                GamePackets.shopSys(GamePackets.WORKSHOP_RECOVERY_ERR)));
        assertEquals(GamePackets.SERVER_WORKSHOP_RECOVERY, recovery.opcode());
        assertEquals(0x0151, recovery.u32());
        PacketReader transfer = new PacketReader(GamePackets.sysAck(
                GamePackets.SERVER_WORKSHOP_TRANSFER,
                GamePackets.shopSys(GamePackets.WORKSHOP_TRANSFER_ERR)));
        assertEquals(GamePackets.SERVER_WORKSHOP_TRANSFER, transfer.opcode());
        assertEquals(0x0104, transfer.u32());
        PacketReader reset = new PacketReader(GamePackets.sysAck(
                GamePackets.SERVER_CLUBSET_RESET, GamePackets.shopSys(GamePackets.CLUBSET_RESET_ERR)));
        assertEquals(GamePackets.SERVER_CLUBSET_RESET, reset.opcode());
        assertEquals(0x0506, reset.u32());
        PacketReader memorial = new PacketReader(GamePackets.sysAck(
                GamePackets.SERVER_MEMORIAL, GamePackets.shopSys(GamePackets.MEMORIAL_ERR_COIN)));
        assertEquals(GamePackets.SERVER_MEMORIAL, memorial.opcode());
        assertEquals(0x0301, memorial.u32());
        PacketReader uccFail = new PacketReader(GamePackets.uccFail());
        assertEquals(GamePackets.SERVER_UCC, uccFail.opcode());
        assertEquals(GamePackets.UCC_FAIL, uccFail.u8());
        PacketReader uccKey = new PacketReader(GamePackets.uccWebKeyFail(
                GamePackets.shopSys(GamePackets.UCC_WEB_KEY_ERR_UID)));
        assertEquals(GamePackets.SERVER_UCC_WEB_KEY, uccKey.opcode());
        assertEquals(1, uccKey.u8());
        assertEquals(1, uccKey.u8());
        assertEquals(0x0101, uccKey.u32());
        PacketReader workshopEv = new PacketReader(GamePackets.workshopEvent());
        assertEquals(GamePackets.SERVER_WORKSHOP_EVENT, workshopEv.opcode());
        assertEquals(0, workshopEv.i32());
        assertEquals(3000, workshopEv.i32());
        assertEquals(0, workshopEv.i32());
        assertEquals(100, workshopEv.u8());
        assertEquals(0, workshopEv.u8());
        assertEquals(10, workshopEv.u8());
        assertEquals(10, workshopEv.u8());
        PacketReader workshopCount = new PacketReader(GamePackets.workshopEventCount());
        assertEquals(GamePackets.SERVER_WORKSHOP_EVENT_COUNT, workshopCount.opcode());
        assertEquals(0, workshopCount.i32());
        for (int i = 1; i <= GamePackets.WORKSHOP_EVENT_COUNT_SLOTS; i++) {
            assertEquals(i, workshopCount.u8());
        }
        PacketReader marker = new PacketReader(GamePackets.markerOnCourse(2, 1.5f, 2.5f, 3.5f));
        assertEquals(GamePackets.SERVER_MARKER, marker.opcode());
        assertEquals(2, marker.i32());
        assertEquals(1.5f, marker.f32());
        assertEquals(2.5f, marker.f32());
        assertEquals(3.5f, marker.f32());
        byte[] shotBody = GamePackets.shotEndLocationSample();
        assertEquals(GamePackets.SHOT_END_LOCATION_BYTES, shotBody.length);
        PacketReader shotEnd = new PacketReader(GamePackets.shotEnd(7, 1, shotBody));
        assertEquals(GamePackets.SERVER_SHOT_END, shotEnd.opcode());
        assertEquals(7, shotEnd.i32());
        assertEquals(1, shotEnd.u8());
        assertArrayEquals(shotBody, shotEnd.readBytes(GamePackets.SHOT_END_LOCATION_BYTES));
        assertEquals(0, shotEnd.remaining());
        PacketReader clientShot = new PacketReader(GamePackets.clientShotEnd(shotBody));
        assertEquals(GamePackets.CLIENT_SHOT_END, clientShot.opcode());
        assertArrayEquals(shotBody, clientShot.readBytes(GamePackets.SHOT_END_LOCATION_BYTES));
        PacketReader cutinFail = new PacketReader(GamePackets.cutinFail(GamePackets.CUTIN_ERR));
        assertEquals(GamePackets.SERVER_CUTIN, cutinFail.opcode());
        assertEquals(0, cutinFail.u8());
        assertEquals(GamePackets.CUTIN_ERR, cutinFail.u16());
        PacketReader cutinGz = new PacketReader(GamePackets.cutinFail(GamePackets.CUTIN_GZ_DISABLED));
        assertEquals(GamePackets.SERVER_CUTIN, cutinGz.opcode());
        assertEquals(0, cutinGz.u8());
        assertEquals(GamePackets.CUTIN_GZ_DISABLED, cutinGz.u16());
        PacketReader gzEnd = new PacketReader(GamePackets.gzEndGame());
        assertEquals(GamePackets.SERVER_GZ_END_GAME, gzEnd.opcode());
        assertEquals(0, gzEnd.remaining());
        PacketReader activeItem = new PacketReader(GamePackets.activeItem(
                GamePackets.TYPEID_SHOP_PANG_ITEM, 123, 7));
        assertEquals(GamePackets.SERVER_ACTIVE_ITEM, activeItem.opcode());
        assertEquals(GamePackets.TYPEID_SHOP_PANG_ITEM, activeItem.u32());
        assertEquals(123, activeItem.i32());
        assertEquals(7, activeItem.i32());
        PacketReader clientUse = new PacketReader(GamePackets.clientUseItem(
                GamePackets.TYPEID_SHOP_PANG_ITEM));
        assertEquals(GamePackets.CLIENT_USE_ITEM, clientUse.opcode());
        assertEquals(GamePackets.TYPEID_SHOP_PANG_ITEM, clientUse.u32());
        PacketReader endEmpty = new PacketReader(GamePackets.endShot(7));
        assertEquals(GamePackets.SERVER_END_SHOT, endEmpty.opcode());
        assertEquals(7, endEmpty.i32());
        assertEquals(0, endEmpty.u8());
        assertEquals(0, endEmpty.remaining());
        GamePackets.DropItem cubeDrop = new GamePackets.DropItem(
                GamePackets.TYPEID_SPINNING_CUBE, 0, 1, 1, GamePackets.DROP_TYPE_CUBE);
        PacketReader endCube = new PacketReader(GamePackets.endShot(7, List.of(cubeDrop)));
        assertEquals(GamePackets.SERVER_END_SHOT, endCube.opcode());
        assertEquals(7, endCube.i32());
        assertEquals(1, endCube.u8());
        assertEquals(GamePackets.TYPEID_SPINNING_CUBE, endCube.u32());
        assertEquals(0, endCube.u8());
        assertEquals(1, endCube.u8());
        assertEquals(1, endCube.i16());
        assertEquals(GamePackets.DROP_TYPE_CUBE, endCube.u64());
        assertEquals((GamePackets.END_SHOT_DROP_SLOTS - 1) * GamePackets.DROP_ITEM_BYTES,
                endCube.remaining());
        PacketReader clientCutin = new PacketReader(GamePackets.clientCutin(10001, 1, 0, 0x04000000, 1));
        assertEquals(GamePackets.CLIENT_CUTIN, clientCutin.opcode());
        assertEquals(10001, clientCutin.u32());
        assertEquals(1, clientCutin.u32());
        assertEquals(0, clientCutin.u16());
        assertEquals(0x04000000, clientCutin.u32());
        assertEquals(1, clientCutin.u8());
        PacketReader paws = new PacketReader(GamePackets.activePaws(10001));
        assertEquals(GamePackets.SERVER_ACTIVE_PAWS, paws.opcode());
        assertEquals(10001, paws.u32());
        PacketReader wing = new PacketReader(GamePackets.activeWing(10001, 0x08000099));
        assertEquals(GamePackets.SERVER_ACTIVE_WING, wing.opcode());
        assertEquals(10001, wing.u32());
        assertEquals(0x08000099, wing.u32());
        PacketReader assistInGame = new PacketReader(GamePackets.assistInGameReject());
        assertEquals(GamePackets.SERVER_ASSIST_INGAME, assistInGame.opcode());
        assertEquals(0, assistInGame.u32());
        PacketReader toggleOk = new PacketReader(GamePackets.toggleAssistOk(
                GamePackets.TYPEID_ASSIST, 10001));
        assertEquals(GamePackets.SERVER_TOGGLE_ASSIST, toggleOk.opcode());
        assertEquals(0, toggleOk.u32());
        assertEquals(GamePackets.TYPEID_ASSIST, toggleOk.u32());
        assertEquals(10001, toggleOk.u32());
        PacketReader toggleFail = new PacketReader(GamePackets.toggleAssistFail(
                GamePackets.TOGGLE_ASSIST_ERR_ADD));
        assertEquals(GamePackets.SERVER_TOGGLE_ASSIST, toggleFail.opcode());
        assertEquals(GamePackets.TOGGLE_ASSIST_ERR_ADD, toggleFail.u32());
        PacketReader greenOk = new PacketReader(GamePackets.assistGreenOk(
                GamePackets.TYPEID_ASSIST, 10001));
        assertEquals(GamePackets.SERVER_ASSIST_GREEN, greenOk.opcode());
        assertEquals(0, greenOk.u32());
        assertEquals(GamePackets.TYPEID_ASSIST, greenOk.u32());
        assertEquals(10001, greenOk.u32());
        PacketReader greenFail = new PacketReader(GamePackets.assistGreenFail(
                GamePackets.ASSIST_GREEN_ERR_TYPEID));
        assertEquals(GamePackets.SERVER_ASSIST_GREEN, greenFail.opcode());
        assertEquals(GamePackets.ASSIST_GREEN_ERR_TYPEID, greenFail.u32());
        PacketReader ringOk = new PacketReader(GamePackets.activeRingOk(10001, 0x70000001, 3));
        assertEquals(GamePackets.SERVER_ACTIVE_RING, ringOk.opcode());
        assertEquals(0, ringOk.u32());
        assertEquals(10001, ringOk.u32());
        assertEquals(0x70000001, ringOk.u32());
        assertEquals(3, ringOk.u8());
        PacketReader gloveOk = new PacketReader(GamePackets.activeGloveOk(0x08000099, 10001));
        assertEquals(GamePackets.SERVER_ACTIVE_GLOVE, gloveOk.opcode());
        assertEquals(0, gloveOk.u32());
        assertEquals(0x08000099, gloveOk.u32());
        assertEquals(10001, gloveOk.u32());
        PacketReader earcuffOk = new PacketReader(GamePackets.activeEarcuffOk(0x08000099, 10001, 1, 1.5f));
        assertEquals(GamePackets.SERVER_ACTIVE_EARCUFF, earcuffOk.opcode());
        assertEquals(0, earcuffOk.u32());
        assertEquals(0x08000099, earcuffOk.u32());
        assertEquals(10001, earcuffOk.u32());
        assertEquals(1, earcuffOk.u8());
        assertEquals(1.5f, earcuffOk.f32());
        PacketReader groundOk = new PacketReader(GamePackets.activeRingGroundOk(1, 2, 3, 4, 10001));
        assertEquals(GamePackets.SERVER_ACTIVE_RING_GROUND, groundOk.opcode());
        assertEquals(0, groundOk.u32());
        assertEquals(1, groundOk.u32());
        assertEquals(2, groundOk.u32());
        assertEquals(3, groundOk.u32());
        assertEquals(4, groundOk.u32());
        assertEquals(10001, groundOk.u32());
        PacketReader rainbow = new PacketReader(GamePackets.ringUidAck(
                GamePackets.SERVER_RING_PAWS_RAINBOW, 10001));
        assertEquals(GamePackets.SERVER_RING_PAWS_RAINBOW, rainbow.opcode());
        assertEquals(10001, rainbow.u32());
        PacketReader miracleOk = new PacketReader(GamePackets.ringMiracleOk(0x70000001, 10001));
        assertEquals(GamePackets.SERVER_RING_MIRACLE, miracleOk.opcode());
        assertEquals(0, miracleOk.u32());
        assertEquals(0x70000001, miracleOk.u32());
        assertEquals(10001, miracleOk.u32());
        PacketReader clientRing = new PacketReader(GamePackets.clientActiveRing(0x70000001, 5, 2));
        assertEquals(GamePackets.CLIENT_ACTIVE_RING, clientRing.opcode());
        assertEquals(0x70000001, clientRing.u32());
        assertEquals(5, clientRing.u32());
        assertEquals(2, clientRing.u8());
        PacketReader clientEarcuff = new PacketReader(GamePackets.clientEarcuff(0x08000099, 1, 2.5f));
        assertEquals(GamePackets.CLIENT_EARCUFF, clientEarcuff.opcode());
        assertEquals(0x08000099, clientEarcuff.u32());
        assertEquals(1, clientEarcuff.u8());
        assertEquals(2.5f, clientEarcuff.f32());
        assertEquals(GamePackets.IFF_GROUP_PART, GamePackets.itemGroupIdentify(0x08000099));
        assertEquals(GamePackets.IFF_GROUP_AUX_PART, GamePackets.itemGroupIdentify(0x70000001));
        assertEquals(GamePackets.IFF_GROUP_MASCOT, GamePackets.itemGroupIdentify(GamePackets.TYPEID_MASCOT));
        PacketReader clientWing = new PacketReader(GamePackets.clientU32(
                GamePackets.CLIENT_WING, 0x08000099));
        assertEquals(GamePackets.CLIENT_WING, clientWing.opcode());
        assertEquals(0x08000099, clientWing.u32());
        PacketReader clientGreen = new PacketReader(GamePackets.clientU32(
                GamePackets.CLIENT_ASSIST_GREEN, GamePackets.TYPEID_ASSIST));
        assertEquals(GamePackets.CLIENT_ASSIST_GREEN, clientGreen.opcode());
        assertEquals(GamePackets.TYPEID_ASSIST, clientGreen.u32());
        PacketReader gpExit = new PacketReader(GamePackets.gpExitRoomAck());
        assertEquals(GamePackets.SERVER_GP_EXIT_ROOM, gpExit.opcode());
        assertEquals(0, gpExit.u32());
        assertEquals(-1, gpExit.i16());
        PacketReader clientMarker = new PacketReader(GamePackets.clientMarker(1f, 2f, 3f));
        assertEquals(GamePackets.CLIENT_MARKER, clientMarker.opcode());
        assertEquals(1f, clientMarker.f32());
        assertEquals(2f, clientMarker.f32());
        assertEquals(3f, clientMarker.f32());
        PacketReader clientGpExit = new PacketReader(GamePackets.clientGpExitRoom());
        assertEquals(GamePackets.CLIENT_GP_EXIT_ROOM, clientGpExit.opcode());
        assertEquals(0, clientGpExit.u8());
        assertEquals(-1, clientGpExit.i16());
        PacketReader attend = new PacketReader(GamePackets.sysAck(
                GamePackets.SERVER_ATTENDANCE, GamePackets.ATTENDANCE_FAIL));
        assertEquals(GamePackets.SERVER_ATTENDANCE, attend.opcode());
        assertEquals(0xffff_ffff, attend.u32());
        PacketReader gpLobby = new PacketReader(GamePackets.gpLobbyOk(1, 0f));
        assertEquals(GamePackets.SERVER_GP_LOBBY, gpLobby.opcode());
        assertEquals(0, gpLobby.u32());
        assertEquals(1, gpLobby.u32());
        assertEquals(1, gpLobby.u32());
        assertEquals(0, gpLobby.u32());
        assertEquals(0f, gpLobby.f32());
        PacketReader gpEnter = new PacketReader(GamePackets.sysAck(
                GamePackets.SERVER_START_GAME_FAIL,
                GamePackets.shopSys(GamePackets.GP_ENTER_ERR_IFF)));
        assertEquals(GamePackets.SERVER_START_GAME_FAIL, gpEnter.opcode());
        assertEquals(1, gpEnter.u32());
        PacketReader uccClient = new PacketReader(GamePackets.clientUccOpt(99));
        assertEquals(GamePackets.CLIENT_UCC, uccClient.opcode());
        assertEquals(99, uccClient.u8());
        PacketReader uccWebClient = new PacketReader(GamePackets.clientUccWebKey(0, 0, 1, 0));
        assertEquals(GamePackets.CLIENT_UCC_WEB_KEY, uccWebClient.opcode());
        assertEquals(0, uccWebClient.u8());
        assertEquals(0, uccWebClient.u32());
        assertEquals(1, uccWebClient.u8());
        assertEquals(0, uccWebClient.i32());
        PacketReader myRoomClient = new PacketReader(GamePackets.clientMyRoom(10001, 10001));
        assertEquals(GamePackets.CLIENT_MY_ROOM, myRoomClient.opcode());
        assertEquals(10001, myRoomClient.u32());
        assertEquals(10001, myRoomClient.u32());
        assertEquals(GamePackets.CLIENT_WEB_AUTH_KEY, 0xFB);
        assertEquals(GamePackets.CLIENT_CHANGE_GAME_SERVER, 0x119);
        assertEquals(GamePackets.CLIENT_OPEN_TICKET_REPORT, 0xAB);
        assertEquals(GamePackets.CLIENT_TIKI_SHOP, 0x126);
        assertEquals(GamePackets.CLIENT_LOCKER_ACCESS, 0xCC);
        assertEquals(GamePackets.CLIENT_LOCKER_STATE, 0xD3);
        assertEquals(GamePackets.CLIENT_CLUB_WORKSHOP_LEVEL, 0x164);
        assertEquals(GamePackets.CLIENT_OPEN_LUCKY_POUCH, 0xB2);
        assertEquals(GamePackets.CLIENT_COMPLETE_QUEST, 0xAE);
        assertEquals(GamePackets.CLIENT_HEARTBEAT, 0xF4);
        assertEquals(GamePackets.CLIENT_UPDATE_PLACE, 0xC1);
        assertEquals(GamePackets.CLIENT_USE_TICKET_REPORT, 0xAA);
        assertEquals(GamePackets.CLIENT_ACTIVE_PAWS, 0x15C);
        assertEquals(GamePackets.CLIENT_ACTIVE_RING, 0x15D);
        assertEquals(GamePackets.SERVER_WEB_AUTH_KEY, 0x1AD);
        assertEquals(GamePackets.SERVER_CHANGE_GAME_SERVER, 0x1D4);
        assertEquals(GamePackets.SERVER_TICKET_REPORT, 0x11A);
        assertEquals(GamePackets.SERVER_TIKI_SHOP, 0x1E7);
        assertEquals(GamePackets.SERVER_LOCKER_ACCESS, 0x16C);
        assertEquals(GamePackets.SERVER_LOCKER_STATE, 0x170);
        assertEquals(GamePackets.SERVER_CLUB_WORKSHOP_LEVEL, 0x23D);
        assertEquals(GamePackets.SERVER_LUCKY_POUCH, 0x129);
        assertEquals(GamePackets.CLIENT_TIKI_POINTS, 0x127);
        assertEquals(GamePackets.CLIENT_TIKI_EXCHANGE_TP, 0x128);
        assertEquals(GamePackets.CLIENT_TIKI_EXCHANGE_ITEM, 0x129);
        assertEquals(GamePackets.CLIENT_CLUB_WORKSHOP_CONFIRM, 0x165);
        assertEquals(GamePackets.CLIENT_CLUB_WORKSHOP_CANCEL, 0x166);
        assertEquals(GamePackets.CLIENT_CLUB_WORKSHOP_RANK, 0x167);
        assertEquals(GamePackets.CLIENT_ITEM_BUFF, 0xD8);
        assertEquals(GamePackets.CLIENT_COMET_REFILL, 0xEC);
        assertEquals(GamePackets.CLIENT_BOX_MAIL, 0xEF);
        assertEquals(GamePackets.CLIENT_LOCKER_ITEMS, 0xCD);
        assertEquals(GamePackets.CLIENT_LOCKER_PANG, 0xD5);
        assertEquals(GamePackets.CLIENT_REFUSE_WHISPER, 0xDE);
        assertEquals(GamePackets.CLIENT_IDENTITY, 0x41);
        assertEquals(GamePackets.CLIENT_MY_ROOM, 0xB5);
        assertEquals(GamePackets.CLIENT_USE_CARD, 0xBD);
        assertEquals(GamePackets.CLIENT_OPEN_CARD_PACK, 0xCA);
        assertEquals(GamePackets.CLIENT_LOCKER_ADD, 0xCE);
        assertEquals(GamePackets.CLIENT_LOCKER_REMOVE, 0xCF);
        assertEquals(GamePackets.CLIENT_LOCKER_MAKE_PASS, 0xD0);
        assertEquals(GamePackets.CLIENT_LOCKER_CHANGE_PASS, 0xD1);
        assertEquals(GamePackets.CLIENT_LOCKER_MODE, 0xD2);
        assertEquals(GamePackets.CLIENT_LOCKER_UPDATE_PANG, 0xD4);
        assertEquals(GamePackets.CLIENT_CUTIN, 0xE5);
        assertEquals(GamePackets.CLIENT_EXTEND_RENTAL, 0xE6);
        assertEquals(GamePackets.CLIENT_DELETE_RENTAL, 0xE7);
        assertEquals(GamePackets.CLIENT_UCC_LOAD, 0xFE);
        assertEquals(GamePackets.CLIENT_UCC, 0xB9);
        assertEquals(GamePackets.CLIENT_UCC_WEB_KEY, 0xC9);
        assertEquals(GamePackets.CLIENT_ATTENDANCE, 0x16E);
        assertEquals(GamePackets.CLIENT_ATTENDANCE_LOGIN, 0x16F);
        assertEquals(GamePackets.CLIENT_WORKSHOP_EVENT, 0x172);
        assertEquals(GamePackets.CLIENT_GP_LOBBY, 0x176);
        assertEquals(GamePackets.CLIENT_GP_LEAVE, 0x177);
        assertEquals(GamePackets.CLIENT_GP_ENTER, 0x179);
        assertEquals(GamePackets.CLIENT_GP_EXIT_ROOM, 0x17A);
        assertEquals(GamePackets.CLIENT_MARKER, 0x12E);
        assertEquals(GamePackets.CLIENT_EARCUFF, 0x171);
        assertEquals(GamePackets.CLIENT_EVENT_ARIN, 0x192);
        assertEquals(GamePackets.CLIENT_WORKSHOP_TRANSFORM_CONFIRM, 0x168);
        assertEquals(GamePackets.CLIENT_WORKSHOP_TRANSFORM_CANCEL, 0x169);
        assertEquals(GamePackets.CLIENT_WORKSHOP_RECOVERY, 0x16B);
        assertEquals(GamePackets.CLIENT_WORKSHOP_TRANSFER, 0x16C);
        assertEquals(GamePackets.CLIENT_CLUBSET_RESET, 0x16D);
        assertEquals(GamePackets.CLIENT_MEMORIAL, 0x17F);
        assertEquals(GamePackets.SERVER_MY_ROOM, 0x12B);
        assertEquals(GamePackets.SERVER_LOCKER_MAKE_PASS, 0x176);
        assertEquals(GamePackets.SERVER_LOCKER_CHANGE_PASS, 0x174);
        assertEquals(GamePackets.SERVER_LOCKER_MODE, 0x173);
        assertEquals(GamePackets.SERVER_LOCKER_ADD, 0x16E);
        assertEquals(GamePackets.SERVER_LOCKER_REMOVE, 0x16F);
        assertEquals(GamePackets.SERVER_LOCKER_UPDATE_PANG, 0x171);
        assertEquals(GamePackets.SERVER_OPEN_CARD_PACK, 0x154);
        assertEquals(GamePackets.SERVER_USE_CARD, 0x160);
        assertEquals(GamePackets.SERVER_EXTEND_RENTAL, 0x18F);
        assertEquals(GamePackets.SERVER_DELETE_RENTAL, 0x190);
        assertEquals(GamePackets.SERVER_WORKSHOP_TRANSFORM_CONFIRM, 0x242);
        assertEquals(GamePackets.SERVER_WORKSHOP_TRANSFORM_CANCEL, 0x243);
        assertEquals(GamePackets.SERVER_WORKSHOP_TRANSFER, 0x245);
        assertEquals(GamePackets.SERVER_WORKSHOP_RECOVERY, 0x246);
        assertEquals(GamePackets.SERVER_CLUBSET_RESET, 0x247);
        assertEquals(GamePackets.SERVER_MEMORIAL, 0x264);
        assertEquals(GamePackets.SERVER_UCC, 0x12E);
        assertEquals(GamePackets.SERVER_UCC_WEB_KEY, 0x153);
        assertEquals(GamePackets.SERVER_WORKSHOP_EVENT, 0x24E);
        assertEquals(GamePackets.SERVER_ATTENDANCE, 0x248);
        assertEquals(GamePackets.SERVER_ATTENDANCE_LOGIN, 0x249);
        assertEquals(GamePackets.SERVER_GP_LOBBY, 0x250);
        assertEquals(GamePackets.SERVER_GP_LEAVE, 0x251);
        assertEquals(GamePackets.SERVER_TIKI_POINTS, 0x1E8);
        assertEquals(GamePackets.SERVER_TIKI_EXCHANGE_TP, 0x1E9);
        assertEquals(GamePackets.SERVER_TIKI_EXCHANGE_ITEM, 0x1EA);
        assertEquals(GamePackets.SERVER_CLUB_WORKSHOP_CONFIRM, 0x23E);
        assertEquals(GamePackets.SERVER_CLUB_WORKSHOP_CANCEL, 0x23F);
        assertEquals(GamePackets.SERVER_CLUB_WORKSHOP_RANK, 0x240);
        assertEquals(GamePackets.SERVER_ITEM_BUFF, 0x181);
        assertEquals(GamePackets.SERVER_COMET_REFILL, 0x197);
        assertEquals(GamePackets.SERVER_BOX_MAIL, 0x19D);
        assertEquals(GamePackets.SERVER_LOCKER_ITEMS, 0x16D);
        assertEquals(GamePackets.SERVER_LOCKER_PANG, 0x172);
        assertEquals(GamePackets.CLIENT_REQUEST_MESSENGER_LIST, 0x8B);
        assertEquals(GamePackets.CLIENT_REFRESH_GACHA, 0x9E);
        assertEquals(GamePackets.CLIENT_ENCHANT, 0x4B);
        assertEquals(GamePackets.CLIENT_INTRUSION, 0x9D);
        assertEquals(GamePackets.CLIENT_PAPEL_PLAY, 0x14B);
        assertEquals(GamePackets.CLIENT_WEB_LINK, 0xA1);
        assertEquals(GamePackets.CLIENT_JOIN_GALLERY, 0x3E);
        assertEquals(GamePackets.CLIENT_GM_COMMAND, 0x8F);
        assertEquals(GamePackets.GM_CMD_VISIBLE, 3);
        assertEquals(GamePackets.GM_CMD_WHISPER, 4);
        assertEquals(GamePackets.GM_CMD_CHANNEL, 5);
        assertEquals(GamePackets.GM_CMD_OPEN_WHISPER, 8);
        assertEquals(GamePackets.GM_CMD_CLOSE_WHISPER, 9);
        assertEquals(GamePackets.GM_CMD_KICK, 10);
        assertEquals(GamePackets.GM_CMD_DISCONNECT, 11);
        assertEquals(GamePackets.GM_CMD_DESTROY, 13);
        assertEquals(GamePackets.GM_CMD_WIND, 14);
        assertEquals(GamePackets.GM_CMD_WEATHER, 15);
        assertEquals(GamePackets.GM_CMD_IDENTITY, 16);
        assertEquals(GamePackets.GM_CMD_GIVEITEM, 18);
        assertEquals(GamePackets.GM_CMD_GOLDENBELL, 19);
        PacketReader gmVisible = new PacketReader(GamePackets.clientGmVisible(1));
        assertEquals(GamePackets.CLIENT_GM_COMMAND, gmVisible.opcode());
        assertEquals(GamePackets.GM_CMD_VISIBLE, gmVisible.i16());
        assertEquals(1, gmVisible.u16());
        PacketReader gmWhisper = new PacketReader(GamePackets.clientGmU16(GamePackets.GM_CMD_WHISPER, 1));
        assertEquals(GamePackets.CLIENT_GM_COMMAND, gmWhisper.opcode());
        assertEquals(GamePackets.GM_CMD_WHISPER, gmWhisper.i16());
        assertEquals(1, gmWhisper.u16());
        PacketReader gmWeather = new PacketReader(GamePackets.clientGmWeather(1));
        assertEquals(GamePackets.CLIENT_GM_COMMAND, gmWeather.opcode());
        assertEquals(GamePackets.GM_CMD_WEATHER, gmWeather.i16());
        assertEquals(1, gmWeather.u8());
        PacketReader weatherGm = new PacketReader(GamePackets.weather(1, GamePackets.WEATHER_GM));
        assertEquals(GamePackets.SERVER_WEATHER, weatherGm.opcode());
        assertEquals(1, weatherGm.u16());
        assertEquals(GamePackets.WEATHER_GM, weatherGm.u8());
        PacketReader gmKick = new PacketReader(GamePackets.clientGmKick(2, 0));
        assertEquals(GamePackets.CLIENT_GM_COMMAND, gmKick.opcode());
        assertEquals(GamePackets.GM_CMD_KICK, gmKick.i16());
        assertEquals(2, gmKick.u32());
        assertEquals(0, gmKick.u8());
        PacketReader gmIdent = new PacketReader(GamePackets.clientGmIdentity(
                GamePackets.CAPABILITY_GM_NORMAL, "TestNick"));
        assertEquals(GamePackets.CLIENT_GM_COMMAND, gmIdent.opcode());
        assertEquals(GamePackets.GM_CMD_IDENTITY, gmIdent.i16());
        assertEquals(GamePackets.CAPABILITY_GM_NORMAL, gmIdent.i32());
        assertEquals("TestNick", gmIdent.pstr());
        PacketReader admit = new PacketReader(GamePackets.admitIdentity(128));
        assertEquals(GamePackets.SERVER_ADMIT_IDENTITY, admit.opcode());
        assertEquals(128, admit.i32());
        PacketReader gmGive = new PacketReader(GamePackets.clientGmGiveitem(
                2, GamePackets.TYPEID_SHOP_PANG_ITEM, 1));
        assertEquals(GamePackets.CLIENT_GM_COMMAND, gmGive.opcode());
        assertEquals(GamePackets.GM_CMD_GIVEITEM, gmGive.i16());
        assertEquals(2, gmGive.u32());
        assertEquals(GamePackets.TYPEID_SHOP_PANG_ITEM, gmGive.u32());
        assertEquals(1, gmGive.u32());
        PacketReader gmBell = new PacketReader(GamePackets.clientGmGoldenbell(
                GamePackets.TYPEID_SHOP_PANG_ITEM, 1));
        assertEquals(GamePackets.CLIENT_GM_COMMAND, gmBell.opcode());
        assertEquals(GamePackets.GM_CMD_GOLDENBELL, gmBell.i16());
        assertEquals(GamePackets.TYPEID_SHOP_PANG_ITEM, gmBell.u32());
        assertEquals(1, gmBell.u32());
        PacketReader gmWind = new PacketReader(GamePackets.clientGmWind(5, 90));
        assertEquals(GamePackets.CLIENT_GM_COMMAND, gmWind.opcode());
        assertEquals(GamePackets.GM_CMD_WIND, gmWind.i16());
        assertEquals(5, gmWind.u8());
        assertEquals(90, gmWind.u8());
        PacketReader gmOpen = new PacketReader(GamePackets.clientGmWhisperList(
                GamePackets.GM_CMD_OPEN_WHISPER, "TestNick2"));
        assertEquals(GamePackets.CLIENT_GM_COMMAND, gmOpen.opcode());
        assertEquals(GamePackets.GM_CMD_OPEN_WHISPER, gmOpen.i16());
        assertEquals("TestNick2", gmOpen.pstr());
        PacketReader gmDisc = new PacketReader(GamePackets.clientGmDisconnect(2));
        assertEquals(GamePackets.CLIENT_GM_COMMAND, gmDisc.opcode());
        assertEquals(GamePackets.GM_CMD_DISCONNECT, gmDisc.i16());
        assertEquals(2, gmDisc.u32());
        assertEquals(
                "\\1[Channel=Channel \\1(Rookies), \\1ROOM=65535]",
                GamePackets.gmChatSpyFrom("Channel (Rookies)", 0xFFFF));
        assertEquals("\\5TestNick: 'hi'", GamePackets.gmChatSpyMsg("TestNick", "hi"));
        assertEquals("\\5A>B: 'pm'", GamePackets.gmPmSpyMsg("A", "B", "pm"));
        assertEquals("\\c0xff00ff00\\cExecuted Command.", GamePackets.chatColor(
                GamePackets.CHAT_GREEN_HEX, GamePackets.GM_CMD_OK));
        assertEquals(GamePackets.CLIENT_ACTIVE_AUTO_COMMAND, 0x156);
        assertEquals(GamePackets.CLIENT_REQUEST_KICK, 0x61);
        assertEquals(GamePackets.CLIENT_REQUEST_PANG_INFO, 0xA2);
        assertEquals(GamePackets.SERVER_MESSENGER_LIST, 0xFC);
        assertEquals(GamePackets.SERVER_GACHA_COUPON, 0x102);
        assertEquals(GamePackets.SERVER_CLUB_STATS, 0xA5);
        assertEquals(GamePackets.SERVER_INTRUSION, 0x113);
        assertEquals(GamePackets.SERVER_PAPEL_PLAY, 0x21B);
        assertEquals(GamePackets.SERVER_LAST5, 0x10E);
        assertEquals(GamePackets.TYPEID_GACHA_TICKET, 436207744);
        assertEquals(GamePackets.TYPEID_GACHA_SUB, 436207747);
        assertEquals(GamePackets.SERVER_MY_ROOM_CHAR, GamePackets.CLIENT_WORKSHOP_TRANSFORM_CONFIRM);
        assertEquals(GamePackets.SERVER_MY_ROOM_POSTERS, GamePackets.CLIENT_GZ_INITIAL);
        assertEquals(GamePackets.SERVER_LOUNGE_STATE, GamePackets.CLIENT_RING_PAWS_RAINBOW);
        assertEquals(GamePackets.SERVER_COMET_REFILL, GamePackets.CLIENT_RING_POWER);
        assertEquals(GamePackets.SERVER_MY_ROOM_CHAR, 0x168);
        assertEquals(GamePackets.SERVER_MY_ROOM_POSTERS, 0x12D);
        assertEquals(GamePackets.SERVER_BIG_PAPEL, 0x26C);
        assertEquals(GamePackets.SERVER_CHAR_MASTERY, 0x26E);
        assertEquals(GamePackets.SERVER_CHAR_STATS_UP, 0x26F);
        assertEquals(GamePackets.SERVER_CHAR_STATS_DOWN, 0x270);
        assertEquals(GamePackets.SERVER_CHAR_CARD_EQUIP, 0x271);
        assertEquals(GamePackets.SERVER_CHAR_CARD_PATCHER, 0x272);
        assertEquals(GamePackets.SERVER_CHAR_CARD_REMOVE, 0x273);
        assertEquals(GamePackets.SERVER_TIKI_SHOP_EXCHANGE, 0x274);
        assertEquals(GamePackets.CLIENT_ENTER_MY_ROOM, 0xB7);
        assertEquals(GamePackets.CLIENT_FINISH_GAME_CB, 0xCB);
        assertEquals(GamePackets.CLIENT_FINISH_GAME_12C, 0x12C);
        assertEquals(GamePackets.CLIENT_BIG_PAPEL, 0x186);
        assertEquals(GamePackets.CLIENT_CHAR_MASTERY, 0x187);
        assertEquals(GamePackets.CLIENT_CHAR_STATS_UP, 0x188);
        assertEquals(GamePackets.CLIENT_CHAR_STATS_DOWN, 0x189);
        assertEquals(GamePackets.CLIENT_CHAR_CARD_EQUIP, 0x18A);
        assertEquals(GamePackets.CLIENT_CHAR_CARD_PATCHER, 0x18B);
        assertEquals(GamePackets.CLIENT_CHAR_CARD_REMOVE, 0x18C);
        assertEquals(GamePackets.CLIENT_TIKI_SHOP_EXCHANGE, 0x18D);
        assertEquals(GamePackets.CLIENT_RING_PAWS_RAINBOW, 0x196);
        assertEquals(GamePackets.CLIENT_RING_POWER, 0x197);
        assertEquals(GamePackets.CLIENT_RING_MIRACLE, 0x198);
        assertEquals(GamePackets.CLIENT_RING_PAWS_SET, 0x199);
        assertEquals(GamePackets.MY_ROOM_POSTERS_OPTION, 1);
        assertEquals(GamePackets.CHAR_MASTERY_ERR_CHAR, 0x5200651);
        assertEquals(GamePackets.CHAR_MASTERY_ERR_DEFAULT, 0x5200650);
        assertEquals(GamePackets.CHAR_STATS_UP_ERR_CHAR, 0x5200501);
        assertEquals(GamePackets.CHAR_STATS_UP_ERR_DEFAULT, 0x5200500);
        assertEquals(GamePackets.CHAR_STATS_DOWN_ERR_CHAR, 0x5200551);
        assertEquals(GamePackets.CHAR_STATS_DOWN_ERR_DEFAULT, 0x5200550);
        assertEquals(GamePackets.CHAR_CARD_ERR_IFF, 0x5200757);
        assertEquals(GamePackets.CHAR_CARD_ERR_DEFAULT, 0x5200750);
        assertEquals(GamePackets.CHAR_CARD_PATCHER_ERR, 0x5200810);
        assertEquals(GamePackets.CHAR_CARD_PATCHER_DEFAULT, 0x5200800);
        assertEquals(GamePackets.CHAR_CARD_PATCHER_ERR_SLOT, 0x5200803);
        assertEquals(GamePackets.CHAR_CARD_PATCHER_ERR_SUB, 0x5200806);
        assertEquals(GamePackets.TYPEID_CLUB_PATCHER, 0x1A00018F);
        assertEquals(GamePackets.CHAR_CARD_PATCHER_SLOT, 4);
        assertEquals(GamePackets.CHAR_CARD_REMOVE_ERR_CHAR, 0x5200851);
        assertEquals(GamePackets.CHAR_CARD_REMOVE_DEFAULT, 0x5200850);
        assertEquals(GamePackets.TIKI_SHOP_EXCHANGE_ERR_COUNT, 5200451);
        assertEquals(GamePackets.TIKI_SHOP_EXCHANGE_ERR_TRUNCATED, 5200452);
        assertEquals(GamePackets.TIKI_SHOP_EXCHANGE_ERR_ITEM, 0x52000901);
        assertEquals(GamePackets.TIKI_SHOP_EXCHANGE_ERR_DEFAULT, 0x5200900);
        assertEquals(GamePackets.TIKI_SHOP_EXCHANGE_ITEM_CHECK_BYTES, 8);
        assertEquals(GamePackets.CARD_EQUIP_BYTES, 20);
        PacketReader myRoomChar = new PacketReader(GamePackets.myRoomCharacter(new GamePackets.PlayerRoomInfo()));
        assertEquals(GamePackets.SERVER_MY_ROOM_CHAR, myRoomChar.opcode());
        assertEquals(GamePackets.PLAYER_ROOM_INFO_EX_BYTES, myRoomChar.remaining());
        PacketReader myRoomPosters = new PacketReader(GamePackets.myRoomPosters(1, 0));
        assertEquals(GamePackets.SERVER_MY_ROOM_POSTERS, myRoomPosters.opcode());
        assertEquals(1, myRoomPosters.u32());
        assertEquals(0, myRoomPosters.u16());
        PacketReader bigPapel = new PacketReader(GamePackets.sysAck(
                GamePackets.SERVER_BIG_PAPEL, GamePackets.shopSys(GamePackets.PAPEL_PLAY_ERR_BALLS)));
        assertEquals(GamePackets.SERVER_BIG_PAPEL, bigPapel.opcode());
        assertEquals(GamePackets.PAPEL_PLAY_ERR_BALLS & 0xFFFF, bigPapel.u32());
        PacketReader mastery = new PacketReader(GamePackets.sysAck(
                GamePackets.SERVER_CHAR_MASTERY, GamePackets.shopSys(GamePackets.CHAR_MASTERY_ERR_CHAR)));
        assertEquals(GamePackets.SERVER_CHAR_MASTERY, mastery.opcode());
        assertEquals(GamePackets.CHAR_MASTERY_ERR_CHAR & 0xFFFF, mastery.u32());
        PacketReader statsUp = new PacketReader(GamePackets.sysAck(
                GamePackets.SERVER_CHAR_STATS_UP, GamePackets.shopSys(GamePackets.CHAR_STATS_UP_ERR_CHAR)));
        assertEquals(GamePackets.SERVER_CHAR_STATS_UP, statsUp.opcode());
        assertEquals(GamePackets.CHAR_STATS_UP_ERR_CHAR & 0xFFFF, statsUp.u32());
        PacketReader statsDown = new PacketReader(GamePackets.sysAck(
                GamePackets.SERVER_CHAR_STATS_DOWN, GamePackets.shopSys(GamePackets.CHAR_STATS_DOWN_ERR_CHAR)));
        assertEquals(GamePackets.SERVER_CHAR_STATS_DOWN, statsDown.opcode());
        assertEquals(GamePackets.CHAR_STATS_DOWN_ERR_CHAR & 0xFFFF, statsDown.u32());
        PacketReader cardEquip = new PacketReader(GamePackets.sysAck(
                GamePackets.SERVER_CHAR_CARD_EQUIP, GamePackets.shopSys(GamePackets.CHAR_CARD_ERR_IFF)));
        assertEquals(GamePackets.SERVER_CHAR_CARD_EQUIP, cardEquip.opcode());
        assertEquals(GamePackets.CHAR_CARD_ERR_IFF & 0xFFFF, cardEquip.u32());
        PacketReader patcher = new PacketReader(GamePackets.sysAck(
                GamePackets.SERVER_CHAR_CARD_PATCHER, GamePackets.shopSys(GamePackets.CHAR_CARD_PATCHER_ERR)));
        assertEquals(GamePackets.SERVER_CHAR_CARD_PATCHER, patcher.opcode());
        assertEquals(GamePackets.CHAR_CARD_PATCHER_ERR & 0xFFFF, patcher.u32());
        PacketReader cardRemove = new PacketReader(GamePackets.sysAck(
                GamePackets.SERVER_CHAR_CARD_REMOVE, GamePackets.shopSys(GamePackets.CHAR_CARD_REMOVE_ERR_CHAR)));
        assertEquals(GamePackets.SERVER_CHAR_CARD_REMOVE, cardRemove.opcode());
        assertEquals(GamePackets.CHAR_CARD_REMOVE_ERR_CHAR & 0xFFFF, cardRemove.u32());
        PacketReader tikiShop = new PacketReader(GamePackets.sysAck(
                GamePackets.SERVER_TIKI_SHOP_EXCHANGE,
                GamePackets.shopSys(GamePackets.TIKI_SHOP_EXCHANGE_ERR_COUNT)));
        assertEquals(GamePackets.SERVER_TIKI_SHOP_EXCHANGE, tikiShop.opcode());
        assertEquals(GamePackets.TIKI_SHOP_EXCHANGE_ERR_COUNT & 0xFFFF, tikiShop.u32());
        PacketReader clientMastery = new PacketReader(GamePackets.clientCharMastery(0, 0));
        assertEquals(GamePackets.CLIENT_CHAR_MASTERY, clientMastery.opcode());
        assertEquals(0, clientMastery.u32());
        assertEquals(0, clientMastery.u32());
        PacketReader clientStats = new PacketReader(GamePackets.clientCharStats(GamePackets.CLIENT_CHAR_STATS_UP, 0));
        assertEquals(GamePackets.CLIENT_CHAR_STATS_UP, clientStats.opcode());
        assertEquals(0, clientStats.u32());
        assertEquals(GamePackets.CHARACTER_INFO_BYTES, clientStats.remaining());
        PacketReader clientCard = new PacketReader(GamePackets.clientCardEquip(GamePackets.CLIENT_CHAR_CARD_EQUIP));
        assertEquals(GamePackets.CLIENT_CHAR_CARD_EQUIP, clientCard.opcode());
        assertEquals(GamePackets.CARD_EQUIP_BYTES, clientCard.remaining());
        PacketReader clientTiki = new PacketReader(GamePackets.clientTikiShopCount(0));
        assertEquals(GamePackets.CLIENT_TIKI_SHOP_EXCHANGE, clientTiki.opcode());
        assertEquals(0, clientTiki.u32());
        GamePackets.PersonalShopItem shopItem = new GamePackets.PersonalShopItem();
        shopItem.index = 1;
        shopItem.typeid = GamePackets.TYPEID_SHOP_PANG_ITEM;
        shopItem.id = 9;
        shopItem.qntd = 1;
        shopItem.pang = 1000;
        assertEquals(GamePackets.PERSONAL_SHOP_ITEM_BYTES, shopItem.toArray().length);
        assertEquals(GamePackets.SERVER_SHOP_SOLD, 0xED);
        PacketReader itemsOk = new PacketReader(GamePackets.shopItemsOk("TestNick", 10001, List.of(shopItem)));
        assertEquals(GamePackets.SERVER_SHOP_ITEMS, itemsOk.opcode());
        assertEquals(GamePackets.SHOP_OK, itemsOk.u32());
        assertEquals("TestNick", itemsOk.fixedStr(GamePackets.SHOP_NICK_BYTES));
        assertEquals(10001, itemsOk.u32());
        assertEquals(1, itemsOk.u32());
        assertEquals(GamePackets.PERSONAL_SHOP_ITEM_BYTES, itemsOk.remaining());
        PacketReader viewOk = new PacketReader(GamePackets.shopViewOk("TestNick", "MyShop", 10001, List.of(shopItem)));
        assertEquals(GamePackets.SERVER_SHOP_VIEW, viewOk.opcode());
        assertEquals(GamePackets.SHOP_OK, viewOk.u32());
        assertEquals("TestNick", viewOk.fixedStr(GamePackets.SHOP_NICK_BYTES));
        assertEquals("MyShop", viewOk.pstr());
        assertEquals(10001, viewOk.u32());
        assertEquals(1, viewOk.u32());
        PacketReader buyOk = new PacketReader(GamePackets.shopBuyOk(
                1, 950, shopItem, GamePackets.SHOP_GROUP_ITEM_BYTE, new GamePackets.WarehouseItem().toArray()));
        assertEquals(GamePackets.SERVER_SHOP_BUY, buyOk.opcode());
        assertEquals(GamePackets.SHOP_OK, buyOk.u32());
        assertEquals(1, buyOk.u8());
        assertEquals(950, buyOk.u64());
        buyOk.readBytes(GamePackets.PERSONAL_SHOP_ITEM_BYTES);
        assertEquals(GamePackets.SHOP_GROUP_ITEM_BYTE, buyOk.u8());
        assertEquals(GamePackets.WAREHOUSE_ITEM_BYTES, buyOk.remaining());
        PacketReader sold = new PacketReader(
                GamePackets.shopSold("TestNick", 10001, shopItem, GamePackets.SHOP_SOLD_EMPTY));
        assertEquals(GamePackets.SERVER_SHOP_SOLD, sold.opcode());
        assertEquals("TestNick", sold.pstr());
        assertEquals(10001, sold.u32());
        sold.readBytes(GamePackets.PERSONAL_SHOP_ITEM_BYTES);
        assertEquals(GamePackets.SHOP_SOLD_EMPTY, sold.i32());
        assertEquals(950, GamePackets.shopSellerGain(1000));
        PacketReader clientOpen = new PacketReader(GamePackets.clientShopOpenItems(List.of(shopItem)));
        assertEquals(GamePackets.CLIENT_SHOP_OPEN_ITEMS, clientOpen.opcode());
        assertEquals(1, clientOpen.u32());
        assertEquals(GamePackets.PERSONAL_SHOP_ITEM_BYTES, clientOpen.remaining());
        assertEquals(GamePackets.SERVER_PAPEL_REMAIN, 0xFB);
        assertEquals(GamePackets.CLIENT_WEB_AUTH_KEY, 0xFB);
        GamePackets.PapelAward award = new GamePackets.PapelAward(
                GamePackets.PAPEL_AWARD_TYPE, GamePackets.TYPEID_SHOP_PANG_ITEM, 9, 0, 0, 2, 2);
        PacketReader awards = new PacketReader(GamePackets.papelAwards(1, List.of(award)));
        assertEquals(GamePackets.SERVER_DAILY_QUEST_STAMP, awards.opcode());
        assertEquals(1, awards.u32());
        assertEquals(1, awards.u32());
        assertEquals(GamePackets.PAPEL_AWARD_TYPE, awards.u8());
        assertEquals(GamePackets.TYPEID_SHOP_PANG_ITEM, awards.u32());
        GamePackets.PapelAward masteryAward = new GamePackets.PapelAward(
                GamePackets.CHAR_MASTERY_AWARD_TYPE, GamePackets.TYPEID_NURI, 1, 0, 0, 0, 0, 1);
        PacketReader masteryAwards = new PacketReader(GamePackets.papelAwards(1, List.of(masteryAward)));
        assertEquals(GamePackets.SERVER_DAILY_QUEST_STAMP, masteryAwards.opcode());
        masteryAwards.u32();
        masteryAwards.u32();
        assertEquals(GamePackets.CHAR_MASTERY_AWARD_TYPE, masteryAwards.u8());
        assertEquals(GamePackets.TYPEID_NURI, masteryAwards.u32());
        masteryAwards.i32();
        masteryAwards.u32();
        masteryAwards.i32();
        masteryAwards.i32();
        masteryAwards.i32();
        masteryAwards.readBytes(GamePackets.PAPEL_AWARD_PAD);
        assertEquals(1, masteryAwards.u32());
        PacketReader remain = new PacketReader(GamePackets.papelRemain(
                GamePackets.PAPEL_UNLIMITED_REMAIN, GamePackets.PAPEL_UNLIMITED_FLAG));
        assertEquals(GamePackets.SERVER_PAPEL_REMAIN, remain.opcode());
        assertEquals(GamePackets.PAPEL_UNLIMITED_REMAIN, remain.i32());
        assertEquals(GamePackets.PAPEL_UNLIMITED_FLAG, remain.i32());
        GamePackets.PapelBall ball = new GamePackets.PapelBall(
                1, GamePackets.TYPEID_SHOP_PANG_ITEM, 0, 2, GamePackets.PAPEL_TYPE_COMMUN);
        PacketReader playOk = new PacketReader(GamePackets.papelPlayOk(
                GamePackets.SERVER_PAPEL_PLAY, 0, List.of(ball), 99000, 0));
        assertEquals(GamePackets.SERVER_PAPEL_PLAY, playOk.opcode());
        assertEquals(0, playOk.u32());
        assertEquals(0, playOk.i32());
        assertEquals(1, playOk.u32());
        assertEquals(1, playOk.u32());
        assertEquals(GamePackets.TYPEID_SHOP_PANG_ITEM, playOk.u32());
        assertEquals(0, playOk.u32());
        assertEquals(2, playOk.u32());
        assertEquals(GamePackets.PAPEL_TYPE_COMMUN, playOk.u32());
        assertEquals(99000, playOk.u64());
        assertEquals(0, playOk.u64());
        assertEquals(GamePackets.PAPEL_PRICE_NORMAL, 1000);
        assertEquals(GamePackets.CADDIE_HOLIDAY_PANG, 1000);
        assertEquals(GamePackets.MASCOT_MSG_PRICE, 100);
        assertEquals(GamePackets.SERVER_TIMEOUT, 0x5C);
        assertEquals(GamePackets.TYPEID_CADDIE_PAPEL, 0x1C000000);
        assertEquals(GamePackets.TYPEID_MASCOT, 0x40000000);
        assertEquals(GamePackets.TYPEID_CARD_NORMAL, 0x7C000001);
        assertEquals(GamePackets.LOLO_PANG_NORMAL, 1000);
        assertEquals(GamePackets.SERVER_LOLO_TIPO, 0x229);
        assertEquals(GamePackets.GM_CMD_VISIBLE, 3);
        assertEquals(GamePackets.CAPABILITY_GM, 4);
        assertEquals(GamePackets.CAPABILITY_GM_NORMAL, 128);
        assertEquals(GamePackets.CAPABILITY_TITLE_GM, 32768);
        assertEquals(GamePackets.SERVER_ADMIT_IDENTITY, 0x9A);
        assertEquals(GamePackets.GM_CMD_IDENTITY, 16);
        assertEquals(GamePackets.GM_CMD_OPEN_WHISPER, 8);
        assertEquals(GamePackets.GM_CMD_DISCONNECT, 11);
        assertEquals(GamePackets.GM_PM_SPY_NICK, "\\1[PM]");
        assertEquals(GamePackets.GM_GIVEITEM_MAX, 20000);
        assertEquals(GamePackets.LIMIT_DEGREE, 255);
        assertEquals(GamePackets.CHAR_MASTERY_AWARD_TYPE, 0xCD);
        assertEquals(GamePackets.TYPEID_NURI, 0x4000000);
        assertEquals(GamePackets.CHAR_STATS_AWARD_TYPE, 0xC9);
        assertEquals(GamePackets.IFF_GROUP_ENCHANT, 13);
        assertEquals(GamePackets.enchantTypeid(0, 0), 0x34000000);
        assertEquals(GamePackets.CHAR_STATS_ENCHANT_PANG, 100);
        PacketReader pclAwards = new PacketReader(GamePackets.charPclAwards(
                1, GamePackets.TYPEID_NURI, 1, new byte[] {1, 0, 0, 0, 0}));
        assertEquals(GamePackets.SERVER_DAILY_QUEST_STAMP, pclAwards.opcode());
        assertEquals(1, pclAwards.u32());
        assertEquals(1, pclAwards.u32());
        assertEquals(GamePackets.CHAR_STATS_AWARD_TYPE, pclAwards.u8());
        assertEquals(GamePackets.TYPEID_NURI, pclAwards.u32());
        assertEquals(1, pclAwards.i32());
        pclAwards.u32();
        pclAwards.u32();
        pclAwards.u32();
        pclAwards.u32();
        assertEquals(1, pclAwards.u16());
        assertEquals(0, pclAwards.u16());
        assertEquals(0, pclAwards.u16());
        assertEquals(0, pclAwards.u16());
        assertEquals(0, pclAwards.u16());
        assertEquals(GamePackets.CHAR_STATS_PCL_PAD, pclAwards.remaining());
        PacketReader statsOk = new PacketReader(GamePackets.charStatsOk(GamePackets.SERVER_CHAR_STATS_UP, 0));
        assertEquals(GamePackets.SERVER_CHAR_STATS_UP, statsOk.opcode());
        assertEquals(0, statsOk.u32());
        assertEquals(0, statsOk.u32());
        assertEquals(GamePackets.itemSubGroupIdentify22(GamePackets.TYPEID_CARD_NORMAL), 0);
        assertEquals(GamePackets.CHAR_CARD_AWARD_TYPE, 0xCB);
        GamePackets.PapelAward cardAward = new GamePackets.PapelAward(
                GamePackets.CHAR_CARD_AWARD_TYPE, GamePackets.TYPEID_NURI, 1, 0, 0, 0, 0,
                GamePackets.TYPEID_CARD_NORMAL, 1);
        PacketReader cardAwards = new PacketReader(GamePackets.papelAwards(1, List.of(cardAward)));
        assertEquals(GamePackets.SERVER_DAILY_QUEST_STAMP, cardAwards.opcode());
        cardAwards.u32();
        cardAwards.u32();
        assertEquals(GamePackets.CHAR_CARD_AWARD_TYPE, cardAwards.u8());
        assertEquals(GamePackets.TYPEID_NURI, cardAwards.u32());
        cardAwards.i32();
        cardAwards.u32();
        cardAwards.i32();
        cardAwards.i32();
        cardAwards.i32();
        cardAwards.readBytes(10 + GamePackets.CHAR_CARD_AWARD_MID_PAD);
        assertEquals(GamePackets.TYPEID_CARD_NORMAL, cardAwards.u32());
        assertEquals(1, cardAwards.u8());
        PacketReader cardOk = new PacketReader(GamePackets.charCardOk(
                GamePackets.SERVER_CHAR_CARD_EQUIP, GamePackets.TYPEID_CARD_NORMAL));
        assertEquals(GamePackets.SERVER_CHAR_CARD_EQUIP, cardOk.opcode());
        assertEquals(0, cardOk.u32());
        assertEquals(GamePackets.TYPEID_CARD_NORMAL, cardOk.u32());
        assertEquals(GamePackets.CHAR_CARD_ERR_PART_SLOT, 0x5200754);
        assertEquals(GamePackets.CHAR_CARD_REMOVE_ERR_UNKNOWN, 0x5200853);
        assertEquals(GamePackets.CARD_SUB_CADDIE, 1);
        assertEquals(GamePackets.CARD_SUB_NPC, 5);
        assertEquals(GamePackets.CHAR_CARD_CONSUME_C0, 32767);
        GamePackets.PapelAward consume = new GamePackets.PapelAward(
                GamePackets.PAPEL_AWARD_TYPE, GamePackets.TYPEID_CARD_NORMAL, 9, 0, 1, 0, -1);
        PacketReader cardPacket = new PacketReader(GamePackets.charCardAwards(1, List.of(consume, cardAward)));
        assertEquals(GamePackets.SERVER_DAILY_QUEST_STAMP, cardPacket.opcode());
        assertEquals(1, cardPacket.u32());
        assertEquals(2, cardPacket.u32());
        assertEquals(GamePackets.PAPEL_AWARD_TYPE, cardPacket.u8());
        assertEquals(GamePackets.TYPEID_CARD_NORMAL, cardPacket.u32());
        cardPacket.i32();
        cardPacket.u32();
        cardPacket.i32();
        cardPacket.i32();
        assertEquals(-1, cardPacket.i32());
        assertEquals(GamePackets.CHAR_CARD_CONSUME_C0, cardPacket.u16());
        cardPacket.readBytes(GamePackets.CHAR_CARD_AWARD_C_REST + GamePackets.CHAR_CARD_AWARD_MID_PAD);
        assertEquals(0, cardPacket.u32());
        assertEquals(0, cardPacket.u8());
        assertEquals(GamePackets.CHAR_CARD_AWARD_TYPE, cardPacket.u8());
        assertEquals(GamePackets.GM_CMD_BLOCKED,
                "Nao pode executar esse comando, voce foi bloqueado pelo ADM.");
        assertEquals(GamePackets.CLIENT_WORKSHOP_EVENT_COUNT, 0x173);
        assertEquals(GamePackets.SERVER_WORKSHOP_EVENT_COUNT, 0x24B);
        assertEquals(GamePackets.SERVER_MARKER, 0x1F8);
        assertEquals(GamePackets.SERVER_SHOT_END, 0x1F7);
        assertEquals(GamePackets.SHOT_END_LOCATION_BYTES, 87);
        assertEquals(GamePackets.CLIENT_SHOT_END, 0x12F);
        assertEquals(GamePackets.SERVER_CUTIN, 0x18D);
        assertEquals(GamePackets.SERVER_GZ_END_GAME, 0x1F2);
        assertEquals(GamePackets.CUTIN_BODY_BYTES, 15);
        assertEquals(GamePackets.CLIENT_TIKI_SHOP_EXCHANGE, 0x18D);
        assertEquals(GamePackets.SERVER_ACTIVE_PAWS, 0x236);
        assertEquals(GamePackets.SERVER_ACTIVE_WING, 0x203);
        assertEquals(GamePackets.SERVER_TOGGLE_ASSIST, 0x26A);
        assertEquals(GamePackets.SERVER_ASSIST_GREEN, 0x26B);
        assertEquals(GamePackets.SERVER_ASSIST_INGAME, 0x16A);
        assertEquals(GamePackets.SERVER_MASCOT_SEED, 0x16A);
        assertEquals(GamePackets.TYPEID_ASSIST, 0x1BE00016);
        assertEquals(GamePackets.TOGGLE_ASSIST_ERR_ADD, 0x5200801);
        assertEquals(GamePackets.TOGGLE_ASSIST_ERR_REMOVE, 0x5200802);
        assertEquals(GamePackets.TOGGLE_ASSIST_ERR_DEFAULT, 0x5200800);
        assertEquals(GamePackets.ASSIST_GREEN_ERR_TYPEID, 0x5200101);
        assertEquals(GamePackets.ASSIST_GREEN_ERR_OFF, 0x5200102);
        assertEquals(GamePackets.ASSIST_GREEN_ERR_DEFAULT, 0x5200100);
        assertEquals(GamePackets.SERVER_ACTIVE_RING, 0x237);
        assertEquals(GamePackets.SERVER_ACTIVE_GLOVE, 0x265);
        assertEquals(GamePackets.SERVER_ACTIVE_EARCUFF, 0x24C);
        assertEquals(GamePackets.SERVER_ACTIVE_RING_GROUND, 0x266);
        assertEquals(GamePackets.SERVER_RING_PAWS_RAINBOW, 0x27E);
        assertEquals(GamePackets.SERVER_RING_POWER, 0x27F);
        assertEquals(GamePackets.SERVER_RING_MIRACLE, 0x280);
        assertEquals(GamePackets.SERVER_RING_PAWS_SET, 0x281);
        assertEquals(GamePackets.IFF_GROUP_PART, 2);
        assertEquals(GamePackets.IFF_GROUP_MASCOT, 16);
        assertEquals(GamePackets.IFF_GROUP_AUX_PART, 28);
        assertEquals(GamePackets.RING_ERR_TYPEID, 0x330001);
        assertEquals(GamePackets.GLOVE_ERR_PART, 0x370004);
        assertEquals(GamePackets.EARCUFF_ERR_MASCOT, 0x380005);
        assertEquals(GamePackets.MIRACLE_ERR_AUX, 0x350004);
        assertEquals(GamePackets.SERVER_GP_EXIT_ROOM, 0x254);
        assertEquals(GamePackets.SERVER_ACTIVE_ITEM, 0x5A);
        assertEquals(GamePackets.CLIENT_USE_ITEM, 0x17);
        assertEquals(GamePackets.TYPEID_MULLIGAN_ROSE, 0x1800000E);
        assertEquals(GamePackets.SERVER_END_SHOT, 0xCC);
        assertEquals(GamePackets.CLIENT_SHOT_ACK, 0x1C);
        assertEquals(GamePackets.DROP_ITEM_BYTES, 16);
        assertEquals(GamePackets.END_SHOT_DROP_SLOTS, 128);
        assertEquals(GamePackets.TYPEID_SPINNING_CUBE, 0x1A00015B);
        assertEquals(GamePackets.TYPEID_COIN, 0x1A000010);
        assertEquals(GamePackets.SERVER_REPLAY, 0xA4);
        assertEquals(GamePackets.CLIENT_REPLAY_ONLINE, 0x4A);
        assertEquals(GamePackets.SERVER_AUTO_COMMAND_ACK, 0x22B);
        assertEquals(GamePackets.CLIENT_ACTIVE_AUTO_COMMAND, 0x156);
        assertEquals(GamePackets.TYPEID_AUTO_COMMAND, 0x1A00019F);
        assertEquals(GamePackets.STDA_ERROR_TYPE_GAME, 92);
        assertEquals(GamePackets.AUTO_COMMAND_ERR_USED, 0x550001);
        assertEquals(GamePackets.TYPEID_TICKET_REPORT, 0x1A000041);
        assertEquals(GamePackets.FLAG_GAME_PLAYING, 0);
        assertEquals(GamePackets.FLAG_GAME_TICKET_REPORT, 1);
        assertEquals(GamePackets.FLAG_GAME_FINISH, 2);
        assertEquals(GamePackets.SERVER_SCORE_LEAVE, 0x61);
        assertEquals(GamePackets.SERVER_TICKET_REPORT_LEAVE, 0x11B);
        assertEquals(GamePackets.SERVER_TICKET_REPORT_NOTICE, 0x12A);
        assertEquals(GamePackets.SERVER_TREASURE_DRAW, 0x133);
        assertEquals(GamePackets.SERVER_NEW_END_GAME_FLAG, 0x244);
        assertEquals(GamePackets.SERVER_NEW_END_GAME_FLAG2, 0x24F);
        PacketReader ticketClient = new PacketReader(GamePackets.clientUseTicketReport());
        assertEquals(GamePackets.CLIENT_USE_TICKET_REPORT, ticketClient.opcode());
        assertEquals(GamePackets.USER_INFO_BYTES, ticketClient.remaining());
        PacketReader ticketNotice = new PacketReader(GamePackets.ticketReportNotice());
        assertEquals(GamePackets.SERVER_TICKET_REPORT_NOTICE, ticketNotice.opcode());
        assertEquals(0, ticketNotice.u32());
        PacketReader draw = new PacketReader(GamePackets.treasureHunterDraw());
        assertEquals(GamePackets.SERVER_TREASURE_DRAW, draw.opcode());
        assertEquals(0, draw.u8());
        PacketReader scoreLeave = new PacketReader(GamePackets.scoreLeave(7));
        assertEquals(GamePackets.SERVER_SCORE_LEAVE, scoreLeave.opcode());
        assertEquals(7, scoreLeave.i32());
        PacketReader leaveUser = new PacketReader(GamePackets.ticketReportLeave(7));
        assertEquals(GamePackets.SERVER_TICKET_REPORT_LEAVE, leaveUser.opcode());
        assertEquals(7, leaveUser.i32());
        PacketReader end1 = new PacketReader(GamePackets.newEndGameFlag());
        assertEquals(GamePackets.SERVER_NEW_END_GAME_FLAG, end1.opcode());
        assertEquals(0, end1.u32());
        PacketReader end2 = new PacketReader(GamePackets.newEndGameFlag2());
        assertEquals(GamePackets.SERVER_NEW_END_GAME_FLAG2, end2.opcode());
        assertEquals(0, end2.u32());
        assertEquals(GamePackets.SERVER_BUY_ACK, 0x68);
        assertEquals(GamePackets.CREATE_ROOM_FAILED, 0x07);
        assertEquals(GamePackets.SERVER_LAST5, 0x10E);
    }
}
