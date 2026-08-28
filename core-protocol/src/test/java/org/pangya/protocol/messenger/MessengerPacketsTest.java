package org.pangya.protocol.messenger;

import org.junit.jupiter.api.Test;
import org.pangya.protocol.packet.PacketReader;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessengerPacketsTest {

    @Test
    void loginRoundtrip() {
        byte[] pkt = MessengerPackets.clientLogin(10001, "TestNick");
        PacketReader r = new PacketReader(pkt);
        assertEquals(MessengerPackets.CLIENT_CONNECT, r.opcode());
        MessengerPackets.Login login = MessengerPackets.readLogin(r);
        assertEquals(10001, login.uid());
        assertEquals("TestNick", login.nickname());
    }

    @Test
    void channelPlayerInfoIs75Bytes() {
        assertEquals(MessengerPackets.CHANNEL_PLAYER_INFO_BYTES, MessengerPackets.emptyChannelPlayerInfo().length);
        byte[] pkt = MessengerPackets.friendStatus(10001, MessengerPackets.STATE_ONLINE, MessengerPackets.emptyChannelPlayerInfo());
        PacketReader r = new PacketReader(pkt);
        assertEquals(MessengerPackets.SERVER_FRIEND_AND_GUILD_LIST, r.opcode());
        assertEquals(MessengerPackets.SUB_CHANGE_MY_STATUS, r.u16());
        assertEquals(10001, r.u32());
        assertEquals(MessengerPackets.STATE_ONLINE, r.u32());
        assertEquals(1, r.u8());
        assertEquals(75, r.remaining());
        assertEquals(MessengerPackets.FRIEND_INFO_BYTES, MessengerPackets.friendInfo("TestNick2", "Friend", 10002).length);
        byte[] page = MessengerPackets.emptyFriendPage();
        PacketReader p = new PacketReader(page);
        assertEquals(MessengerPackets.SERVER_FRIEND_AND_GUILD_LIST, p.opcode());
        assertEquals(MessengerPackets.SUB_FRIEND_LIST_PAGE, p.u16());
        assertEquals(1, p.u8());
        assertEquals(0, p.u16());
        assertEquals(0, p.u16());
        assertEquals(0, p.remaining());
        assertEquals(MessengerPackets.CHANNEL_PLAYER_INFO_BYTES, MessengerPackets.offlineChannelPlayerInfo().length);
        byte[] row = MessengerPackets.friendListRow(
                MessengerPackets.friendInfo("TestNick2", "Friend", 10002),
                MessengerPackets.offlineChannelPlayerInfo(),
                MessengerPackets.OFFLINE_ICON,
                MessengerPackets.CUNKNOWN_FLAG_DEFAULT,
                1,
                MessengerPackets.FLAG_FRIEND,
                MessengerPackets.FRIEND_FLAG);
        assertEquals(MessengerPackets.FRIEND_INFO_BYTES + MessengerPackets.CHANNEL_PLAYER_INFO_BYTES + 5, row.length);
    }

    @Test
    void checkNickChatAndLogoutPackets() {
        byte[] ok = MessengerPackets.checkNickOk("TestNick2", 10002);
        PacketReader okR = new PacketReader(ok);
        assertEquals(MessengerPackets.SERVER_FRIEND_AND_GUILD_LIST, okR.opcode());
        assertEquals(MessengerPackets.SUB_CHECK_NICK, okR.u16());
        assertEquals(0, okR.u32());
        assertEquals("TestNick2", okR.pstr());
        assertEquals(10002, okR.u32());

        byte[] err = MessengerPackets.checkNickError(MessengerPackets.CHECK_NICK_ERR_MISSING, "Missing");
        PacketReader errR = new PacketReader(err);
        assertEquals(MessengerPackets.SERVER_FRIEND_AND_GUILD_LIST, errR.opcode());
        assertEquals(MessengerPackets.SUB_CHECK_NICK, errR.u16());
        assertEquals(MessengerPackets.CHECK_NICK_ERR_MISSING, errR.u32());
        assertEquals("Missing", errR.pstr());

        byte[] chat = MessengerPackets.friendChat(10001, "TestNick", "hi");
        PacketReader chatR = new PacketReader(chat);
        assertEquals(MessengerPackets.SERVER_FRIEND_AND_GUILD_LIST, chatR.opcode());
        assertEquals(MessengerPackets.SUB_FRIEND_CHAT, chatR.u16());
        assertEquals(10001, chatR.u32());
        assertEquals("TestNick", chatR.pstr());
        assertEquals("hi", chatR.pstr());
        assertEquals(0, chatR.u8());

        byte[] logout = MessengerPackets.friendLogout(10001);
        PacketReader logoutR = new PacketReader(logout);
        assertEquals(MessengerPackets.SERVER_FRIEND_AND_GUILD_LIST, logoutR.opcode());
        assertEquals(MessengerPackets.SUB_FRIEND_LOGOUT, logoutR.u16());
        assertEquals(10001, logoutR.u32());

        byte[] cpi = MessengerPackets.channelPlayerInfo(10, 2, 30201, 1, "Ch-A");
        assertEquals(MessengerPackets.CHANNEL_PLAYER_INFO_BYTES, cpi.length);
        byte[] update = MessengerPackets.clientUpdateChannel(cpi);
        PacketReader updateR = new PacketReader(update);
        assertEquals(MessengerPackets.CLIENT_REQ_UPDATE_CHANNEL_INFO, updateR.opcode());
        assertEquals(MessengerPackets.CHANNEL_PLAYER_INFO_BYTES, updateR.remaining());

        byte[] guild = MessengerPackets.guildChat(10001, "TestNick", "guild hi");
        PacketReader guildR = new PacketReader(guild);
        assertEquals(MessengerPackets.SERVER_FRIEND_AND_GUILD_LIST, guildR.opcode());
        assertEquals(MessengerPackets.SUB_FRIEND_CHAT, guildR.u16());
        assertEquals(10001, guildR.u32());
        assertEquals("TestNick", guildR.pstr());
        assertEquals("guild hi", guildR.pstr());
        assertEquals(1, guildR.u8());

        byte[] alias = MessengerPackets.assignApelidoOk(10002, "Buddy");
        PacketReader aliasR = new PacketReader(alias);
        assertEquals(MessengerPackets.SERVER_FRIEND_AND_GUILD_LIST, aliasR.opcode());
        assertEquals(MessengerPackets.SUB_FRIEND_APELIDO, aliasR.u16());
        assertEquals(0, aliasR.u32());
        assertEquals(10002, aliasR.u32());
        assertEquals("Buddy", aliasR.pstr());

        byte[] joined = MessengerPackets.guildMemberJoined(10002, 9001, 0, "testid", "TestNick2");
        PacketReader joinedR = new PacketReader(joined);
        assertEquals(MessengerPackets.SERVER_GUILD_MEMBER_JOINED, joinedR.opcode());
        assertEquals(10002, joinedR.u32());
        assertEquals(9001, joinedR.u32());
        assertEquals(0, joinedR.u8());
        assertEquals("testid", joinedR.pstr());
        assertEquals("TestNick2", joinedR.pstr());
        assertEquals(0x1F, joinedR.u16());

        byte[] left = MessengerPackets.guildMemberLeft(10002);
        PacketReader leftR = new PacketReader(left);
        assertEquals(MessengerPackets.SERVER_GUILD_MEMBER_LEFT, leftR.opcode());
        assertEquals(10002, leftR.u32());
    }
}
