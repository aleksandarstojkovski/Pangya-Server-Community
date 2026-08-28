package org.pangya.protocol.messenger;

import org.pangya.protocol.packet.PacketReader;
import org.pangya.protocol.packet.PacketWriter;

import java.util.List;

/**
 * JP {@code Definition.cs} + {@code MessengerServer.requestLogin}/{@code confirmLoginOnOtherServer}.
 */
public final class MessengerPackets {

    public static final int CLIENT_CONNECT = 0x12;
    public static final int CLIENT_REQ_USERINFO = 0x14;
    public static final int CLIENT_REQ_REGISTER_FRIEND = 0x18;
    public static final int CLIENT_REQ_FRIEND_AGREE = 0x19;
    public static final int CLIENT_REQ_FRIEND_BLOCK = 0x1A;
    public static final int CLIENT_REQ_FRIEND_UNBLOCK = 0x1B;
    public static final int CLIENT_REQ_FRIEND_REMOVE = 0x1C;
    public static final int CLIENT_NOTIFY_LOGOUT = 0x16;
    public static final int CLIENT_REQ_CHECK_NICK = 0x17;
    public static final int CLIENT_NOTIFY_UPDATE_MY_STATUS = 0x1D;
    public static final int CLIENT_REQ_CHAT_FRIEND = 0x1E;
    public static final int CLIENT_REQ_ASSIGN_APELIDO = 0x1F;
    public static final int CLIENT_REQ_UPDATE_CHANNEL_INFO = 0x23;
    public static final int CLIENT_REQ_CHAT_GUILD = 0x25;

    public static final int SERVER_CONNECT = 0x2E;
    public static final int SERVER_LOGIN_ACK = 0x2F;
    public static final int SERVER_FRIEND_AND_GUILD_LIST = 0x30;

    public static final int SUB_REGISTER_FRIEND = 0x104;
    public static final int SUB_NEW_FRIEND_MESSAGE = 0x106;
    public static final int SUB_FRIEND_AGREE = 0x109;
    public static final int SUB_FRIEND_ACCEPTED = 0x10A;
    public static final int SUB_FRIEND_REMOVE = 0x10B;
    public static final int SUB_FRIEND_BLOCK = 0x10C;
    public static final int SUB_FRIEND_UNBLOCK = 0x10D;
    public static final int SUB_FRIEND_LOGOUT = 0x10F;
    /** C# {@code requestFriendAndGuildMemberList} page sub-id. */
    public static final int SUB_FRIEND_LIST_PAGE = 0x102;
    public static final int SUB_FRIEND_CHAT = 0x113;
    public static final int SUB_FRIEND_APELIDO = 0x119;
    public static final int SUB_CHECK_NICK = 0x117;

    /** C# {@code requestCheckNickname} empty nick. */
    public static final int CHECK_NICK_ERR_EMPTY = 0x5200501;
    /** C# nick not found on verify. */
    public static final int CHECK_NICK_ERR_MISSING = 1;
    public static final int CHECK_NICK_ERR_DEFAULT = 0x5200500;

    /** C# {@code USER_STATUS.IS_ONLINE}. */
    public static final int STATE_ONLINE = 4;

    /** C# {@code PlayerState} bits stored in {@code FriendInfoEx.state}. */
    public static final int FLAG_SEX = 1 << 0;
    public static final int FLAG_ONLINE = 1 << 1;
    public static final int FLAG_FRIEND = 1 << 2;
    public static final int FLAG_REQUEST = 1 << 3;
    public static final int FLAG_BLOCK = 1 << 4;

    public static final int FRIEND_FLAG = 1;
    public static final int FRIEND_INFO_BYTES = 65;
    public static final int CUNKNOWN_FLAG_DEFAULT = 255;
    public static final int OFFLINE_ICON = 5;
    /** C# {@code MessengerServer.FRIEND_PAG_LIMIT}. */
    public static final int FRIEND_PAG_LIMIT = 30;
    /** C# {@code ManyPacket.Pagina}: byte pagina + u16 total + u16 current. */
    public static final int FRIEND_PAGE_HEADER_BYTES = 5;

    /** Sub-packet id written before the login player's {@code ChannelPlayerInfo}. */
    public static final int SUB_CHANGE_MY_STATUS = 0x115;

    /** C# {@code ChannelPlayerInfo} marshal size. */
    public static final int CHANNEL_PLAYER_INFO_BYTES = 75;

    private MessengerPackets() {}

    public static byte[] clientLogin(int uid, String nickname) {
        return new PacketWriter().opcode(CLIENT_CONNECT).u32(uid).pstr(nickname).toBytes();
    }

    public static Login readLogin(PacketReader reader) {
        int uid = reader.u32();
        String nickname = reader.remaining() >= 2 ? reader.pstr() : "";
        return new Login(uid, nickname);
    }

    public static byte[] loginOk(int uid) {
        return new PacketWriter().opcode(SERVER_LOGIN_ACK).u8(0).u32(uid).toBytes();
    }

    public static byte[] loginFail() {
        return new PacketWriter().opcode(SERVER_LOGIN_ACK).u8(1).toBytes();
    }

    /**
     * C# {@code requestFriendAndGuildMemberList} first packet: sub 0x115, uid, state, OK,
     * then {@code ChannelPlayerInfo.ToArray()} (75 bytes). An empty {@code 0x102} page
     * always follows when {@code ManyPacket.paginas == 0}.
     */
    public static byte[] friendStatus(int uid, int state, byte[] channelPlayerInfo) {
        return new PacketWriter()
                .opcode(SERVER_FRIEND_AND_GUILD_LIST)
                .u16(SUB_CHANGE_MY_STATUS)
                .u32(uid)
                .u32(state)
                .u8(1)
                .bytes(channelPlayerInfo)
                .toBytes();
    }

    /** Defaults from C# {@code ChannelPlayerInfo.clear()}: room 0xFFFF, server_uid max, id 0xFF. */
    public static byte[] emptyChannelPlayerInfo() {
        PacketWriter w = new PacketWriter();
        w.u16(0xffff); // room.number
        w.i32(0); // room.type
        w.u32(0xffff_ffff); // server_uid
        w.u8(0xff); // channel id
        w.fixedStr("", 64);
        byte[] body = w.toBytes();
        if (body.length != CHANNEL_PLAYER_INFO_BYTES) {
            throw new IllegalStateException("ChannelPlayerInfo size " + body.length);
        }
        return body;
    }

    public static byte[] friendInfo(String nickname, String apelido, int uid) {
        return friendInfo(nickname, apelido, uid, -1, 0, -1, 0, 0, 0, 0);
    }

    public static byte[] friendInfo(
            String nickname,
            String apelido,
            int uid,
            int unknown1,
            int unknown2,
            int unknown3,
            int unknown4,
            int unknown5,
            int unknown6,
            int unknown7) {
        PacketWriter w = new PacketWriter();
        w.fixedStr(nickname == null ? "" : nickname, 22);
        w.fixedStr(apelido == null ? "Friend" : apelido, 11);
        w.u32(uid);
        w.i32(unknown1);
        w.i32(unknown2);
        w.i32(unknown3);
        w.i32(unknown4);
        w.i32(unknown5);
        w.i32(unknown6);
        w.i32(unknown7);
        byte[] body = w.toBytes();
        if (body.length != FRIEND_INFO_BYTES) {
            throw new IllegalStateException("FriendInfo size " + body.length);
        }
        return body;
    }

    /**
     * C# offline ChannelPlayerInfo in friend pages: room/type/server -1, channel -1, 64 zeros.
     */
    public static byte[] offlineChannelPlayerInfo() {
        PacketWriter w = new PacketWriter();
        w.i16(-1);
        w.i32(-1);
        w.i32(-1);
        w.u8(0xff);
        w.zero(64);
        byte[] body = w.toBytes();
        if (body.length != CHANNEL_PLAYER_INFO_BYTES) {
            throw new IllegalStateException("offline ChannelPlayerInfo size " + body.length);
        }
        return body;
    }

    /**
     * C# {@code 0x30} sub {@code 0x102}: {@code ManyPacket.Pagina} then friend rows.
     * Empty list still sends pagina=1, total=0, current=0.
     */
    public static byte[] friendPage(int pagina, int total, int current, List<byte[]> rows) {
        PacketWriter w = new PacketWriter()
                .opcode(SERVER_FRIEND_AND_GUILD_LIST)
                .u16(SUB_FRIEND_LIST_PAGE)
                .u8(pagina)
                .u16(total)
                .u16(current);
        if (rows != null) {
            for (byte[] row : rows) {
                w.bytes(row);
            }
        }
        return w.toBytes();
    }

    public static byte[] emptyFriendPage() {
        return friendPage(1, 0, 0, List.of());
    }

    /**
     * One friend-page row: FriendInfo 65 + ChannelPlayerInfo 75 + icon + cUnknown + level + state + flag.
     */
    public static byte[] friendListRow(
            byte[] friendInfo,
            byte[] channelPlayerInfo,
            int icon,
            int cUnknown,
            int level,
            int state,
            int flag) {
        return new PacketWriter()
                .bytes(friendInfo)
                .bytes(channelPlayerInfo)
                .u8(icon)
                .u8(cUnknown)
                .u8(level)
                .u8(state)
                .u8(flag)
                .toBytes();
    }

    /**
     * C# offline {@code requestAddFriend} success: sub 0x104, OK, FriendInfo, ChannelPlayerInfo
     * with -1 room/type/server, icon 5, then cUnknown/level/state/flag.
     */
    public static byte[] addFriendOkOffline(byte[] friendInfo, int level, int state, int flag) {
        return new PacketWriter()
                .opcode(SERVER_FRIEND_AND_GUILD_LIST)
                .u16(SUB_REGISTER_FRIEND)
                .u32(0)
                .bytes(friendInfo)
                .i16(-1)
                .i32(-1)
                .i32(-1)
                .u8(0xff)
                .zero(64)
                .u8(OFFLINE_ICON)
                .u8(CUNKNOWN_FLAG_DEFAULT)
                .u8(level)
                .u8(state)
                .u8(flag)
                .toBytes();
    }

    public static byte[] addFriendError(int code) {
        return new PacketWriter()
                .opcode(SERVER_FRIEND_AND_GUILD_LIST)
                .u16(SUB_REGISTER_FRIEND)
                .u32(code)
                .toBytes();
    }

    public static byte[] friendUidAck(int sub, int code, int uid) {
        return new PacketWriter()
                .opcode(SERVER_FRIEND_AND_GUILD_LIST)
                .u16(sub)
                .u32(code)
                .u32(uid)
                .toBytes();
    }

    public static byte[] clientAddFriend(int uid, String nickname) {
        return new PacketWriter().opcode(CLIENT_REQ_REGISTER_FRIEND).u32(uid).pstr(nickname).toBytes();
    }

    public static byte[] clientAgreeFriend(int uid) {
        return new PacketWriter().opcode(CLIENT_REQ_FRIEND_AGREE).u32(uid).toBytes();
    }

    public static byte[] clientBlockFriend(int uid) {
        return new PacketWriter().opcode(CLIENT_REQ_FRIEND_BLOCK).u32(uid).toBytes();
    }

    public static byte[] clientRemoveFriend(int uid, String nickname) {
        return new PacketWriter().opcode(CLIENT_REQ_FRIEND_REMOVE).u32(uid).pstr(nickname).toBytes();
    }

    public static byte[] clientUnblockFriend(int uid) {
        return new PacketWriter().opcode(CLIENT_REQ_FRIEND_UNBLOCK).u32(uid).toBytes();
    }

    public static byte[] clientAssignApelido(int uid, String apelido) {
        return new PacketWriter().opcode(CLIENT_REQ_ASSIGN_APELIDO).u32(uid).pstr(apelido).toBytes();
    }

    public static byte[] clientChatGuild(String msg) {
        return new PacketWriter().opcode(CLIENT_REQ_CHAT_GUILD).pstr(msg).toBytes();
    }

    public static byte[] clientNotifyLogout() {
        return new PacketWriter().opcode(CLIENT_NOTIFY_LOGOUT).toBytes();
    }

    public static byte[] clientCheckNick(String nickname) {
        return new PacketWriter().opcode(CLIENT_REQ_CHECK_NICK).pstr(nickname).toBytes();
    }

    public static byte[] clientUpdateState(int state) {
        return new PacketWriter().opcode(CLIENT_NOTIFY_UPDATE_MY_STATUS).u8(state).toBytes();
    }

    public static byte[] clientChatFriend(int uid, String msg) {
        return new PacketWriter().opcode(CLIENT_REQ_CHAT_FRIEND).u32(uid).pstr(msg).toBytes();
    }

    public static byte[] clientUpdateChannel(byte[] channelPlayerInfo) {
        return new PacketWriter()
                .opcode(CLIENT_REQ_UPDATE_CHANNEL_INFO)
                .bytes(channelPlayerInfo)
                .toBytes();
    }

    /** Reads C# {@code ChannelPlayerInfo.ToRead} (75 bytes). */
    public static byte[] readChannelPlayerInfo(PacketReader reader) {
        byte[] body = reader.readBytes(CHANNEL_PLAYER_INFO_BYTES);
        if (body.length != CHANNEL_PLAYER_INFO_BYTES) {
            throw new IllegalArgumentException("ChannelPlayerInfo truncated");
        }
        return body;
    }

    public static byte[] channelPlayerInfo(int roomNum, int roomType, int serverUid, int channelId, String name) {
        PacketWriter w = new PacketWriter();
        w.u16(roomNum);
        w.i32(roomType);
        w.u32(serverUid);
        w.u8(channelId);
        w.fixedStr(name == null ? "" : name, 64);
        byte[] body = w.toBytes();
        if (body.length != CHANNEL_PLAYER_INFO_BYTES) {
            throw new IllegalStateException("ChannelPlayerInfo size " + body.length);
        }
        return body;
    }

    public static byte[] friendLogout(int uid) {
        return new PacketWriter()
                .opcode(SERVER_FRIEND_AND_GUILD_LIST)
                .u16(SUB_FRIEND_LOGOUT)
                .u32(uid)
                .toBytes();
    }

    public static byte[] checkNickOk(String nickname, int uid) {
        return new PacketWriter()
                .opcode(SERVER_FRIEND_AND_GUILD_LIST)
                .u16(SUB_CHECK_NICK)
                .u32(0)
                .pstr(nickname)
                .u32(uid)
                .toBytes();
    }

    public static byte[] checkNickError(int code, String nickname) {
        return new PacketWriter()
                .opcode(SERVER_FRIEND_AND_GUILD_LIST)
                .u16(SUB_CHECK_NICK)
                .u32(code)
                .pstr(nickname == null ? "" : nickname)
                .toBytes();
    }

    /** C# chat friend to recipient: sub 0x113 + from uid/nick + msg + u8 0. */
    public static byte[] friendChat(int fromUid, String fromNick, String msg) {
        return chatPacket(fromUid, fromNick, msg, 0);
    }

    /** C# guild chat: same sub 0x113 with trailing u8 1. */
    public static byte[] guildChat(int fromUid, String fromNick, String msg) {
        return chatPacket(fromUid, fromNick, msg, 1);
    }

    private static byte[] chatPacket(int fromUid, String fromNick, String msg, int kind) {
        return new PacketWriter()
                .opcode(SERVER_FRIEND_AND_GUILD_LIST)
                .u16(SUB_FRIEND_CHAT)
                .u32(fromUid)
                .pstr(fromNick == null ? "" : fromNick)
                .pstr(msg == null ? "" : msg)
                .u8(kind)
                .toBytes();
    }

    public static byte[] friendChatError() {
        return new PacketWriter()
                .opcode(SERVER_FRIEND_AND_GUILD_LIST)
                .u16(SUB_FRIEND_CHAT)
                .i32(-1)
                .toBytes();
    }

    /**
     * C# online {@code requestAddFriend}: sub 0x104 with live CPI + state icon tail.
     */
    public static byte[] addFriendOkOnline(
            byte[] friendInfo, byte[] channelPlayerInfo, int stateIcon, int level, int state, int flag) {
        return new PacketWriter()
                .opcode(SERVER_FRIEND_AND_GUILD_LIST)
                .u16(SUB_REGISTER_FRIEND)
                .u32(0)
                .bytes(friendInfo)
                .bytes(channelPlayerInfo)
                .u8(stateIcon)
                .u8(CUNKNOWN_FLAG_DEFAULT)
                .u8(level)
                .u8(state)
                .u8(flag)
                .toBytes();
    }

    /** C# sub 0x106 to the added player when target is online. */
    public static byte[] newFriendMessage(
            byte[] friendInfo, byte[] channelPlayerInfo, int stateIcon, int level, int state, int flag) {
        return new PacketWriter()
                .opcode(SERVER_FRIEND_AND_GUILD_LIST)
                .u16(SUB_NEW_FRIEND_MESSAGE)
                .bytes(friendInfo)
                .bytes(channelPlayerInfo)
                .u8(stateIcon)
                .u8(CUNKNOWN_FLAG_DEFAULT)
                .u8(level)
                .u8(state)
                .u8(flag)
                .toBytes();
    }

    public static byte[] statusBroadcastError(int uid, int state) {
        return new PacketWriter()
                .opcode(SERVER_FRIEND_AND_GUILD_LIST)
                .u16(SUB_CHANGE_MY_STATUS)
                .u32(uid)
                .u32(state)
                .u8(0)
                .toBytes();
    }

    public static byte[] assignApelidoOk(int uid, String apelido) {
        return new PacketWriter()
                .opcode(SERVER_FRIEND_AND_GUILD_LIST)
                .u16(SUB_FRIEND_APELIDO)
                .u32(0)
                .u32(uid)
                .pstr(apelido == null ? "" : apelido)
                .toBytes();
    }

    public static byte[] assignApelidoError(int code) {
        return new PacketWriter()
                .opcode(SERVER_FRIEND_AND_GUILD_LIST)
                .u16(SUB_FRIEND_APELIDO)
                .u32(code)
                .toBytes();
    }

    public record Login(int uid, String nickname) {}
}
