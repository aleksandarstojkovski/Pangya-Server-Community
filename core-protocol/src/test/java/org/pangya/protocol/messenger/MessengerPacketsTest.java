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
}
