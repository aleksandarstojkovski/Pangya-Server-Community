package org.pangya.protocol.login;

import org.pangya.protocol.packet.PacketIo;
import org.pangya.protocol.packet.PacketReader;
import org.pangya.protocol.packet.PacketWriter;

import java.util.List;

/**
 * JP {@code PacketLogin.cs} + {@code packet_func_ls} builders.
 * Layouts copied from {@code Server/JP/LoginServer/PacketFunc/packet_func_ls.cs}.
 */
public final class LoginPackets {

    public static final int CLIENT_CONNECT = 0x01;
    public static final int CLIENT_SELECT_GS = 0x03;
    public static final int CLIENT_USER_MSG = 0x04;
    public static final int CLIENT_SET_NICK = 0x06;
    public static final int CLIENT_CONFIRM_SET_NICK = 0x07;
    public static final int CLIENT_SET_CHARACTER = 0x08;
    public static final int CLIENT_RECONNECT = 0x0B;

    public static final int SERVER_CONNECT = 0x00;
    public static final int SERVER_LOGIN = 0x01;
    public static final int SERVER_GS_LIST = 0x02;
    public static final int SERVER_AUTH_KEY_GAME = 0x03;
    public static final int SERVER_EVENT_PRIZE = 0x05;
    public static final int SERVER_MACRO_GAME_OPTION = 0x06;
    public static final int SERVER_MS_LIST = 0x09;
    public static final int SERVER_AGREEMENT = 0x0C;
    public static final int SERVER_CHECK_NICK = 0x0E;
    public static final int SERVER_TUTORIAL = 0x0F;
    public static final int SERVER_AUTH_KEY_LOGIN = 0x10;
    public static final int SERVER_CHARACTER_SAVE = 0x11;

    public static final int OPT_OK = 0;
    public static final int OPT_ALREADY_ON_GS = 4;
    public static final int OPT_BAD_ID_OR_PASS = 6;
    public static final int OPT_BLOCK_TIME = 7;
    public static final int OPT_BLOCK_FOREVER = 0x0C;
    public static final int OPT_MAINTENANCE = 15;
    public static final int OPT_REGION_BAN = 16;
    public static final int OPT_FIRST_LOGIN = 0xD8;
    public static final int OPT_FIRST_SET = 0xD9;
    public static final int OPT_ERROR = 0xE2;

    /** C# {@code PlayerInfo.acess_code} default until {@code ProcGeraWeblinkCookiesKey}. */
    public static final String DEFAULT_ACCESS_CODE = "302540";

    /** JP option 7: less than one hour of block is sent as 360 hours (15 days). */
    public static final int BLOCK_TIME_UNDER_ONE_HOUR = 360;

    private LoginPackets() {}

    /**
     * JP {@code pacote001} option=0 success:
     * byte 0, PStr id, uint32 uid, uint32 cap, byte 1, int32 0, byte 1, int32 5,
     * SYSTEMTIME, PStr acess_code, uint64 0, PStr nick.
     */
    public static byte[] pacote001Success(String id, long uid, int cap, String accessCode, String nickname) {
        return new PacketWriter()
                .opcode(SERVER_LOGIN)
                .u8(OPT_OK)
                .pstr(id)
                .u32((int) uid)
                .u32(cap)
                .u8(1)
                .i32(0)
                .u8(1)
                .i32(5)
                .systemTimeNow()
                .pstr(accessCode == null || accessCode.isBlank() ? DEFAULT_ACCESS_CODE : accessCode)
                .u64(0)
                .pstr(nickname)
                .toBytes();
    }

    public static byte[] pacote001Option(int option) {
        PacketWriter w = new PacketWriter().opcode(SERVER_LOGIN).u8(option);
        switch (option) {
            case 1, OPT_BAD_ID_OR_PASS -> w.i32(0);
            case OPT_FIRST_LOGIN -> w.i32(-1).i16(0);
            case OPT_FIRST_SET -> w.i16(0);
            default -> {
                // option-only body (maintenance 15, already-on-gs 4, …)
            }
        }
        return w.toBytes();
    }

    public static byte[] pacote001Option(int option, int subOpt) {
        return new PacketWriter()
                .opcode(SERVER_LOGIN)
                .u8(option)
                .i32(subOpt)
                .toBytes();
    }

    public static byte[] pacote001BlockTime(int hours) {
        return new PacketWriter()
                .opcode(SERVER_LOGIN)
                .u8(OPT_BLOCK_TIME)
                .i32(hours)
                .toBytes();
    }

    /** {@code pacote002}: byte count + {@code ServerInfo.ToArray()} × N. */
    public static byte[] pacote002(List<ServerInfo> servers) {
        PacketWriter w = new PacketWriter()
                .opcode(SERVER_GS_LIST)
                .u8(servers.size() & 0xff);
        for (ServerInfo s : servers) {
            w.bytes(s.toArray());
        }
        return w.toBytes();
    }

    /** {@code pacote009}: messenger list, same layout as pacote002. */
    public static byte[] pacote009(List<ServerInfo> servers) {
        PacketWriter w = new PacketWriter()
                .opcode(SERVER_MS_LIST)
                .u8(servers.size() & 0xff);
        for (ServerInfo s : servers) {
            w.bytes(s.toArray());
        }
        return w.toBytes();
    }

    /** {@code pacote003}: int32 option + PStr auth key game. */
    public static byte[] pacote003(String authKeyGame, int option) {
        return new PacketWriter()
                .opcode(SERVER_AUTH_KEY_GAME)
                .i32(option)
                .pstr(authKeyGame)
                .toBytes();
    }

    /** {@code pacote006}: 9 × 64-byte fixed Shift_JIS macros. */
    public static byte[] pacote006(String[] macros) {
        PacketWriter w = new PacketWriter().opcode(SERVER_MACRO_GAME_OPTION);
        for (int i = 0; i < 9; i++) {
            String text = (macros != null && i < macros.length && macros[i] != null) ? macros[i] : "";
            w.fixedStr(text, 64);
        }
        return w.toBytes();
    }

    /** {@code pacote010}: PStr auth key login. */
    public static byte[] pacote010(String authKeyLogin) {
        return new PacketWriter()
                .opcode(SERVER_AUTH_KEY_LOGIN)
                .pstr(authKeyLogin == null ? "" : authKeyLogin)
                .toBytes();
    }

    public static byte[] pacote00F(int option, String id) {
        return pacote00F(option, id, DEFAULT_ACCESS_CODE);
    }

    /**
     * JP {@code pacote00F}: byte option, PStr id, uint32 0, uint32 5,
     * PStr {@code formatDateLocal(0)}, PStr acess_code.
     */
    public static byte[] pacote00F(int option, String id, String accessCode) {
        return new PacketWriter()
                .opcode(SERVER_TUTORIAL)
                .u8(option)
                .pstr(id)
                .u32(0)
                .u32(5)
                .pstr(PacketIo.FORMAT_DATE_EPOCH)
                .pstr(accessCode == null || accessCode.isBlank() ? DEFAULT_ACCESS_CODE : accessCode)
                .toBytes();
    }

    public static LoginData readLoginData(PacketReader reader) {
        String id = reader.pstr();
        String password = reader.pstr();
        int optCount = reader.u8();
        int n = (optCount * 8) / 4;
        int[] opts = new int[Math.max(n, 0)];
        for (int i = 0; i < n; i++) {
            opts[i] = reader.u32();
        }
        String mac = reader.remaining() >= 2 ? reader.pstr() : "";
        return new LoginData(id, password, optCount, opts, mac);
    }

    public static byte[] clientConnect(String id, String password, String mac) {
        return new PacketWriter()
                .opcode(CLIENT_CONNECT)
                .pstr(id)
                .pstr(password)
                .u8(0)
                .pstr(mac)
                .toBytes();
    }

    public static byte[] clientSelectGs(int serverUid) {
        return new PacketWriter()
                .opcode(CLIENT_SELECT_GS)
                .u32(serverUid)
                .toBytes();
    }

    public record LoginData(String id, String password, int optCount, int[] options, String mac) {}
}
