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

    /** Auth→child opcodes ({@code unit_auth_server_connect.init_Packets}). */
    public static final int AUTH_DISCONNECT_PLAYER = 0x06;
    /** Auth→child: {@code requestInfoPlayerOnline}. */
    public static final int AUTH_INFO_PLAYER_ONLINE = 0x0B;
    /** Auth→child: {@code requestConfirmSendInfoPlayerOnline}. */
    public static final int AUTH_CONFIRM_PLAYER_INFO = 0x0C;
    /** Auth→child: {@code requestSendCommandToOtherServer}. */
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

    /** Auth→child {@code 0x06}: player uid, server uid, force u8. */
    public static AuthDisconnectRequest readAuthDisconnect(PacketReader reader) {
        long playerUid = reader.u32() & 0xffff_ffffL;
        long serverUid = reader.u32() & 0xffff_ffffL;
        int force = reader.u8();
        return new AuthDisconnectRequest(playerUid, serverUid, force);
    }

    /** Auth→child {@code 0x0B}: req server uid, player uid. */
    public static AuthInfoPlayerOnlineRequest readAuthInfoPlayerOnline(PacketReader reader) {
        int reqServerUid = reader.u32();
        long playerUid = reader.u32() & 0xffff_ffffL;
        return new AuthInfoPlayerOnlineRequest(reqServerUid, playerUid);
    }

    /**
     * Child→Auth {@code 0x05} ({@code sendInfoPlayerOnline}): server uid, option i32,
     * uid u32, optional id+ip PStr when option==1.
     */
    public static byte[] infoPlayerOnlineResponse(int reqServerUid, AuthServerPlayerInfo info) {
        PacketWriter w = new PacketWriter()
                .opcode(CONFIRM_INFO)
                .u32(reqServerUid)
                .i32(info.option())
                .u32((int) info.uid());
        if (info.option() == 1) {
            w.pstr(info.id()).pstr(info.ip());
        }
        return w.toBytes();
    }

    /** Auth→child {@code 0x0C}: req server uid, option i32, uid u32, optional id+ip PStr. */
    public static AuthConfirmPlayerInfo readAuthConfirmPlayerInfo(PacketReader reader) {
        int reqServerUid = reader.u32();
        int option = reader.i32();
        long uid = reader.u32() & 0xffff_ffffL;
        if (option == 1 && reader.remaining() >= 2) {
            String id = reader.pstr();
            String ip = reader.remaining() >= 2 ? reader.pstr() : "";
            return new AuthConfirmPlayerInfo(reqServerUid, option, uid, id, ip);
        }
        return new AuthConfirmPlayerInfo(reqServerUid, option, uid, "", "");
    }

    public record AuthDisconnectRequest(long playerUid, long serverUid, int force) {}

    public record AuthInfoPlayerOnlineRequest(int reqServerUid, long playerUid) {}

    /** C# {@code AuthServerPlayerInfo}: option 1 = online with id/ip, -1 = offline. */
    public record AuthServerPlayerInfo(long uid, String id, String ip, int option) {
        public static AuthServerPlayerInfo online(long uid, String id, String ip) {
            return new AuthServerPlayerInfo(uid, id, ip, 1);
        }

        public static AuthServerPlayerInfo offline(long uid) {
            return new AuthServerPlayerInfo(uid, "", "", -1);
        }
    }

    public record AuthConfirmPlayerInfo(
            int reqServerUid, int option, long uid, String id, String ip) {}

    public record RegisterRequest(
            int tipo, int uid, String name, String key, String clientVersion, int packetVersion) {}
}
