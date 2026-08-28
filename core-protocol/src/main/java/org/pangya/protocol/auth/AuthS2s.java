package org.pangya.protocol.auth;

import org.pangya.protocol.packet.PacketIo;
import org.pangya.protocol.packet.PacketReader;
import org.pangya.protocol.packet.PacketWriter;

/**
 * Auth ↔ child TCP. First Auth→child frame is raw {@code 0x00};
 * later packets use the same Cipher as player sessions.
 */
public final class AuthS2s {

    public static final int FIRST_KEY = 0x00;
    public static final int REGISTER = 0x01;
    public static final int REGISTER_ACK = 0x01;
    public static final int DISCONNECT_PLAYER = 0x02;
    public static final int CONFIRM_DISCONNECT = 0x03;
    public static final int INFO_PLAYER = 0x04;
    public static final int CONFIRM_INFO = 0x05;
    public static final int COMMAND_TO_OTHER = 0x06;
    public static final int REPLY_TO_OTHER = 0x07;
    /** Auth→child: {@code requestSendCommandToOtherServer} (C# {@code unit_auth_server_connect} 0x0D). */
    public static final int SEND_COMMAND_TO_OTHER = 0x0D;

    /** Messenger {@code funcs_as} guild callbacks ({@code packet_as001}–{@code packet_as003}). */
    public static final short AS_ACCEPT_GUILD_MEMBER = 0x01;
    public static final short AS_GUILD_MEMBER_EXIT = 0x02;
    public static final short AS_KICK_GUILD_MEMBER = 0x03;

    /** C# AuthServer tipo=5. */
    public static final int TIPO_LOGIN = 0;
    public static final int TIPO_GAME = 1;
    public static final int TIPO_MESSENGER = 3;
    public static final int TIPO_RANKING = 4;
    public static final int TIPO_AUTH = 5;

    private AuthS2s() {}

    public static byte[] firstKeyRaw(int sessionKey, int authUid) {
        byte[] payload = PacketIo.concat(
                PacketIo.opcode(FIRST_KEY),
                PacketIo.u32le(sessionKey),
                PacketIo.u32le(authUid)
        );
        return PacketIo.makeRaw(payload);
    }

    /**
     * Child→Auth {@code 0x01}: uint32 tipo, uint32 uid, PStr name, PStr dbKey,
     * PStr clientVersion, uint32 packetVersion.
     */
    public static byte[] register(
            int tipo, int uid, String name, String dbKey, String clientVersion, int packetVersion) {
        return new PacketWriter()
                .opcode(REGISTER)
                .u32(tipo)
                .u32(uid)
                .pstr(name)
                .pstr(dbKey)
                .pstr(clientVersion)
                .u32(packetVersion)
                .toBytes();
    }

    public static RegisterRequest readRegister(PacketReader reader) {
        int tipo = reader.u32();
        int uid = reader.u32();
        String name = reader.pstr();
        String key = reader.pstr();
        String version = reader.remaining() >= 2 ? reader.pstr() : "";
        int packetVersion = reader.remaining() >= 4 ? reader.u32() : 0;
        return new RegisterRequest(tipo, uid, name, key, version, packetVersion);
    }

    public static byte[] registerAck(int oid) {
        return new PacketWriter().opcode(REGISTER_ACK).i32(oid).toBytes();
    }

    public record RegisterRequest(
            int tipo, int uid, String name, String key, String clientVersion, int packetVersion) {}
}
