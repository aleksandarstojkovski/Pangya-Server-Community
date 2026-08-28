package org.pangya.protocol.messenger;

import org.pangya.protocol.packet.PacketReader;
import org.pangya.protocol.packet.PacketWriter;

/**
 * JP {@code Definition.cs} + {@code MessengerServer.requestLogin}/{@code confirmLoginOnOtherServer}.
 */
public final class MessengerPackets {

    public static final int CLIENT_CONNECT = 0x12;
    public static final int CLIENT_REQ_USERINFO = 0x14;
    public static final int CLIENT_REQ_REGISTER_FRIEND = 0x18;
    public static final int CLIENT_REQ_FRIEND_AGREE = 0x19;
    public static final int CLIENT_REQ_FRIEND_BLOCK = 0x1A;
    public static final int CLIENT_REQ_FRIEND_REMOVE = 0x1C;

    public static final int SERVER_CONNECT = 0x2E;
    public static final int SERVER_LOGIN_ACK = 0x2F;
    public static final int SERVER_FRIEND_AND_GUILD_LIST = 0x30;

    public static final int SUB_REGISTER_FRIEND = 0x104;
    public static final int SUB_NEW_FRIEND_MESSAGE = 0x106;
    public static final int SUB_FRIEND_AGREE = 0x109;
    public static final int SUB_FRIEND_ACCEPTED = 0x10A;
    public static final int SUB_FRIEND_REMOVE = 0x10B;
    public static final int SUB_FRIEND_BLOCK = 0x10C;
    public static final int SUB_FRIEND_LOGOUT = 0x10F;

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
     * then {@code ChannelPlayerInfo.ToArray()} (75 bytes). Empty friend pages follow only
     * when {@code ManyPacket.paginas > 0}.
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
        PacketWriter w = new PacketWriter();
        w.fixedStr(nickname == null ? "" : nickname, 22);
        w.fixedStr(apelido == null ? "Friend" : apelido, 11);
        w.u32(uid);
        w.i32(-1);
        w.i32(0);
        w.i32(-1);
        w.i32(0);
        w.i32(0);
        w.i32(0);
        w.i32(0);
        byte[] body = w.toBytes();
        if (body.length != FRIEND_INFO_BYTES) {
            throw new IllegalStateException("FriendInfo size " + body.length);
        }
        return body;
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

    public record Login(int uid, String nickname) {}
}
