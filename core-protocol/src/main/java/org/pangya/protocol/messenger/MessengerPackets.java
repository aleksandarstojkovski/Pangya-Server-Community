package org.pangya.protocol.messenger;

import org.pangya.protocol.packet.PacketReader;
import org.pangya.protocol.packet.PacketWriter;

/**
 * GB {@code Definition.cs} + {@code MessengerServer.requestLogin}/{@code confirmLoginOnOtherServer}.
 */
public final class MessengerPackets {

    public static final int CLIENT_CONNECT = 0x12;
    public static final int CLIENT_REQ_USERINFO = 0x14;

    public static final int SERVER_CONNECT = 0x2E;
    public static final int SERVER_LOGIN_ACK = 0x2F;
    public static final int SERVER_FRIEND_AND_GUILD_LIST = 0x30;

    /** C# {@code USER_STATUS.IS_ONLINE}. */
    public static final int STATE_ONLINE = 4;

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

    public record Login(int uid, String nickname) {}
}
