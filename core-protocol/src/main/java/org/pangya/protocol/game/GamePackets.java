package org.pangya.protocol.game;

import org.pangya.protocol.packet.PacketIo;
import org.pangya.protocol.packet.PacketReader;
import org.pangya.protocol.packet.PacketWriter;

import java.util.List;

/**
 * GB {@code PacketGame.cs} subset for S3 (login, channel, Practice room).
 * C# {@code RoomInfo.TIPO.PRACTICE} = 19 (SSC is 18).
 */
public final class GamePackets {

    public static final int SERVER_HELLO = 0x3F;
    public static final int SERVER_LOGIN_ACK = 0x44;
    public static final int SERVER_CHANNEL_LIST = 0x4D;
    public static final int SERVER_CHANNEL_ENTER_ACK = 0x4E;
    public static final int SERVER_ROOM_ENTER_RESULT = 0x49;

    public static final int CLIENT_REQUEST_LOGIN = 0x02;
    public static final int CLIENT_ENTER_CHANNEL = 0x04;
    public static final int CLIENT_REQUEST_CREATE_ROOM = 0x08;
    public static final int CLIENT_LEAVE_PRACTICE = 0x130;

    public static final int ACK_LOGIN_OK = 0;
    public static final int ACK_LOGIN_FAIL = 1;
    public static final int ACK_INVALID_ID = 2;
    public static final int ACK_INVALID_VERSION = 0x0B;
    public static final int ACK_SECURITY_KEY = 0x12;
    public static final int ACK_GENERIC_ERROR = 300;

    public static final int CHANNEL_ENTER_OK = 1;
    public static final int CHANNEL_FULL = 2;
    public static final int CHANNEL_NOT_FOUND = 3;

    /** C# {@code TGAME_CREATE_RESULT.CREATE_GAME_CREATE_FAILED}. */
    public static final int CREATE_ROOM_FAILED = 0x07;

    /** C# {@code GameServer.Version_Decrypt} GUID. XOR is involutive. */
    private static final String PACKET_VER_KEY = "{782AE110-2EEF-4c61-B030-A53F17634F7D}";

    /** C# {@code RoomInfo.TIPO.PRACTICE}. */
    public static final int TIPO_PRACTICE = 19;

    private GamePackets() {}

    /** C# {@code TrofelInfo.ToArray} Debug.Assert size. */
    public static final int TROPHY_BYTES = 78;
    /** C# {@code UserEquip.ToArray}. */
    public static final int USER_EQUIP_BYTES = 116;
    /** C# {@code PlayerInfo.GetMapStatistic} (21 maps × 43 × (3+9 seasons)). */
    public static final int MAP_STAT_BYTES = 10836;
    /** C# {@code UserEquipedItem.ToArray}. */
    public static final int EQUIPED_ITEM_BYTES = 628;
    /** C# {@code MemberInfoEx.ToArrayEx}. */
    public static final int MEMBER_INFO_EX_BYTES = 263;
    /** C# {@code UserInfo.ToArray}. */
    public static final int USER_INFO_BYTES = 239;
    /** Bytes after opcode+option in {@code pacote044} ACK_LOGIN_OK / {@code principal()}. */
    public static final int PRINCIPAL_PAYLOAD_BYTES = 12512;

    public static byte[] loginAck(int option) {
        return new PacketWriter().opcode(SERVER_LOGIN_ACK).u8(option).toBytes();
    }

    /**
     * C# {@code pacote044} option 0 + {@code principal()}. Payload after opcode+option is 12512 bytes.
     */
    public static byte[] loginOkPrincipal(
            String clientVersion,
            String serverVersion,
            int oid,
            String id,
            String nick,
            int capability,
            int uid,
            int level,
            int serverProperty) {
        PacketWriter w = new PacketWriter().opcode(SERVER_LOGIN_ACK).u8(ACK_LOGIN_OK);
        w.pstr(clientVersion == null ? "" : clientVersion);
        w.pstr(serverVersion == null ? "" : serverVersion);
        w.bytes(memberInfoEx(oid, id, nick, capability));
        w.u32(uid);
        w.bytes(userInfo(level));
        w.zero(TROPHY_BYTES);
        w.zero(USER_EQUIP_BYTES);
        w.zero(MAP_STAT_BYTES);
        w.zero(EQUIPED_ITEM_BYTES);
        w.systemTimeNow();
        w.u16(0);
        w.u16(0xffff).u16(0xffff).u16(0xffff); // PlayerPapelShopInfo defaults
        w.u32(0);
        w.u64(0);
        w.u32(0);
        w.u32(serverProperty);
        w.zero(277);
        return w.toBytes();
    }

    /** C# {@code pacote073} empty warehouse: two uint16 counts. */
    public static byte[] emptyWarehouse() {
        return new PacketWriter().opcode(0x73).u16(0).u16(0).toBytes();
    }

    /** C# {@code pacote070} empty character list. */
    public static byte[] emptyCharacters() {
        return new PacketWriter().opcode(0x70).i16(0).i16(0).toBytes();
    }

    /** C# {@code pacote071} empty caddie list. */
    public static byte[] emptyCaddies() {
        return new PacketWriter().opcode(0x71).i16(0).i16(0).toBytes();
    }

    /** C# {@code pacote072} + {@code UserEquip.ToArray} zeros. */
    public static byte[] emptyUserEquip() {
        return new PacketWriter().opcode(0x72).zero(USER_EQUIP_BYTES).toBytes();
    }

    /** C# {@code pacote0E1} + {@code MascotManager.Build} count 0. */
    public static byte[] emptyMascots() {
        return new PacketWriter().opcode(0xE1).u16(0).toBytes();
    }

    static byte[] memberInfoEx(int oid, String id, String nick, int capability) {
        PacketWriter w = new PacketWriter();
        w.u16(0xffff); // sala_numero DEFAULT_ROOM_ID
        w.fixedStr(id, 22);
        w.fixedStr(nick, 22);
        w.zero(17); // guild_name
        w.zero(12); // guild_mark_img
        w.u32(0); // school
        w.i32(capability);
        w.u32(0); // galleryUid
        w.i32(oid);
        w.u32(0).u32(0).u32(0); // rank[3]
        w.u32(0); // guild_uid
        w.u32(0); // guild_mark_img_no
        w.u8(0); // state_flag
        w.u8(1); // flag_login_time (first login on GS)
        w.u16(0xffff).u16(0xffff).u16(0xffff); // papel_shop
        w.u32(0); // point_point_event
        w.u64(0); // flag_block
        w.u32(0); // channeling_flag
        w.zero(128); // sDisplayID
        byte[] body = w.toBytes();
        if (body.length != MEMBER_INFO_EX_BYTES) {
            throw new IllegalStateException("MemberInfoEx size " + body.length);
        }
        return body;
    }

    static byte[] userInfo(int level) {
        PacketWriter w = new PacketWriter();
        w.zero(16); // tacada, putt, tempo, tempo_tacada
        w.zero(4); // best_drive float
        w.zero(24); // acerto..hole_in
        w.zero(4); // hio
        w.zero(2); // bunker
        w.zero(12); // fairway, albatross, mad
        w.zero(4); // putt_in
        w.zero(8); // best_long_putt, best_chip_in
        w.u32(0); // exp
        w.u8(level);
        w.u64(0); // pang
        w.zero(4); // media_score
        w.zero(5); // best_score
        w.u8(0); // event_flag
        w.zero(40); // best_pang[5]
        w.zero(8); // sum_pang
        w.zero(16); // jogado, team_hole, team_win, team_game
        w.zero(20); // ladder_*
        w.zero(12); // combo, all_combo, quitado
        w.zero(8); // skin_pang
        w.zero(16); // skin_win..strike
        w.zero(2); // skin_all_in
        w.zero(8); // event_value, jogados_disconnect
        w.zero(4); // game_count_season
        w.zero(6); // medal bytes
        w.zero(2); // _16bit
        byte[] body = w.toBytes();
        if (body.length != USER_INFO_BYTES) {
            throw new IllegalStateException("UserInfo size " + body.length);
        }
        return body;
    }

    /** Fail path used by {@code SendLoginAck}: uint32 ack. */
    public static byte[] loginAckU32(int option) {
        return new PacketWriter().opcode(SERVER_LOGIN_ACK).u32(option).toBytes();
    }

    public static byte[] channelList(List<ChannelInfo> channels) {
        PacketWriter w = new PacketWriter().opcode(SERVER_CHANNEL_LIST).u8(channels.size() & 0xff);
        for (ChannelInfo c : channels) {
            w.bytes(c.toArray());
        }
        return w.toBytes();
    }

    public static byte[] channelEnter(int option) {
        return new PacketWriter().opcode(SERVER_CHANNEL_ENTER_ACK).u8(option).toBytes();
    }

    /**
     * S3 stub of {@code pacote049} success: int16 0, uint16 room number, byte tipo.
     * Full {@code Room.getInfo().ToArray()} lands with S4 room serialization.
     */
    public static byte[] practiceRoomEntered(int roomNumber, int tipo) {
        return new PacketWriter()
                .opcode(SERVER_ROOM_ENTER_RESULT)
                .i16(0)
                .u16(roomNumber)
                .u8(tipo)
                .toBytes();
    }

    /** C# {@code pacote049} error path: single option byte (not int16). */
    public static byte[] roomCreateFailed(int option) {
        return new PacketWriter().opcode(SERVER_ROOM_ENTER_RESULT).u8(option).toBytes();
    }

    /**
     * C# {@code GameServer.Version_Decrypt}: XOR the four LE bytes with
     * {@code {782AE110-2EEF-4c61-B030-A53F17634F7D}}, cycling index 0..3.
     * The same function encrypts (XOR is involutive).
     */
    public static int xorPacketVersion(int packetVersion) {
        byte[] tmp = PacketIo.u32le(packetVersion);
        int index = 0;
        for (int i = 0; i < PACKET_VER_KEY.length(); i++) {
            tmp[index] ^= (byte) PACKET_VER_KEY.charAt(i);
            index = index == 3 ? 0 : index + 1;
        }
        return PacketIo.readU32le(tmp, 0);
    }

    public static byte[] clientLogin(
            String id, int uid, String authKeyLogin, String clientVersion, int packetVersion, String authKeyGame) {
        return new PacketWriter()
                .opcode(CLIENT_REQUEST_LOGIN)
                .pstr(id)
                .u32(uid)
                .u32(0)
                .u16(0)
                .pstr(authKeyLogin)
                .pstr(clientVersion)
                .u32(packetVersion)
                .u32(0)
                .pstr(authKeyGame)
                .toBytes();
    }

    public static byte[] clientEnterChannel(int channelId) {
        return new PacketWriter().opcode(CLIENT_ENTER_CHANNEL).u8(channelId).toBytes();
    }

    public static byte[] clientCreatePractice(String name, String password) {
        return new PacketWriter()
                .opcode(CLIENT_REQUEST_CREATE_ROOM)
                .u8(0)
                .u32(0)
                .u32(0)
                .u8(1)
                .u8(TIPO_PRACTICE)
                .u8(18)
                .u8(0)
                .u8(0)
                .u32(0)
                .pstr(name)
                .pstr(password)
                .u32(0)
                .toBytes();
    }

    public static byte[] clientLeavePractice() {
        return new PacketWriter().opcode(CLIENT_LEAVE_PRACTICE).toBytes();
    }

    public static GameLogin readLogin(PacketReader reader) {
        String id = reader.pstr();
        int uid = reader.u32();
        int ntreev = reader.remaining() >= 4 ? reader.u32() : 0;
        int command = reader.remaining() >= 2 ? reader.u16() : 0;
        String loginKey = reader.remaining() >= 2 ? reader.pstr() : "";
        String clientVersion = reader.remaining() >= 2 ? reader.pstr() : "";
        int packetVersion = reader.remaining() >= 4 ? reader.u32() : 0;
        int pcBang = reader.remaining() >= 4 ? reader.u32() : 0;
        String gameKey = reader.remaining() >= 2 ? reader.pstr() : "";
        return new GameLogin(id, uid, ntreev, command, loginKey, clientVersion, packetVersion, pcBang, gameKey);
    }

    public static CreateRoom readCreateRoom(PacketReader reader) {
        int option = reader.u8();
        int timeVs = reader.u32();
        int time30s = reader.u32();
        int maxPlayer = reader.u8();
        int tipo = reader.u8();
        int holes = reader.u8();
        int course = reader.u8();
        int modo = reader.u8();
        int natural = reader.u32();
        String name = reader.remaining() >= 2 ? reader.pstr() : "";
        String password = reader.remaining() >= 2 ? reader.pstr() : "";
        return new CreateRoom(option, timeVs, time30s, maxPlayer, tipo, holes, course, modo, natural, name, password);
    }

    public record GameLogin(
            String id,
            int uid,
            int ntreevUid,
            int command,
            String authKeyLogin,
            String clientVersion,
            int packetVersion,
            int pcBang,
            String authKeyGame) {}

    public record CreateRoom(
            int option,
            int timeVs,
            int time30s,
            int maxPlayer,
            int tipo,
            int holes,
            int course,
            int modo,
            int natural,
            String name,
            String password) {}

    public static final class ChannelInfo {
        public String name = "";
        public short maxUser;
        public short currUser;
        public byte id;
        public int flag;
        public int flag2;

        public byte[] toArray() {
            return new PacketWriter()
                    .fixedStr(name, 64)
                    .i16(maxUser)
                    .i16(currUser)
                    .u8(id)
                    .u32(flag)
                    .u32(flag2)
                    .toBytes();
        }
    }
}
