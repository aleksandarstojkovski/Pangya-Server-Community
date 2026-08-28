package org.pangya.protocol.game;

import org.pangya.protocol.packet.PacketIo;
import org.pangya.protocol.packet.PacketReader;
import org.pangya.protocol.packet.PacketWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * JP {@code PacketGame.cs} subset for S3/S4 (login, channel, rooms, start-game).
 * C# {@code RoomInfo.TIPO.PRACTICE} = 19 (SSC is 18).
 */
public final class GamePackets {

    public static final int SERVER_HELLO = 0x3F;
    public static final int SERVER_LOGIN_ACK = 0x44;
    public static final int SERVER_CHANNEL_LIST = 0x4D;
    public static final int SERVER_CHANNEL_ENTER_ACK = 0x4E;
    public static final int SERVER_ROOM_PLAYERS = 0x48;
    public static final int SERVER_ROOM_ENTER_RESULT = 0x49;
    public static final int SERVER_ROOM_UPDATE = 0x4A;
    public static final int SERVER_PANG_RATE = 0x77;
    public static final int SERVER_COURSE = 0x52;
    public static final int SERVER_WIND = 0x5B;
    public static final int SERVER_GAME_INIT = 0x76;
    public static final int SERVER_EQUIP_ACK = 0x6B;
    public static final int SERVER_SYNC_SHOT = 0x6E;
    public static final int SERVER_REMAIN_TIME = 0x8D;
    public static final int SERVER_WEATHER = 0x9E;
    public static final int SERVER_END_SHOT = 0xCC;
    public static final int SERVER_BUY_ACK = 0x68;
    public static final int SERVER_MASCOT_SEED = 0x16A;
    public static final int SERVER_START_GAME_FLAG = 0x230;
    public static final int SERVER_START_GAME_FLAG2 = 0x231;
    public static final int SERVER_START_GAME_FAIL = 0x253;

    public static final int CLIENT_REQUEST_LOGIN = 0x02;
    public static final int CLIENT_ENTER_CHANNEL = 0x04;
    public static final int CLIENT_REQUEST_CREATE_ROOM = 0x08;
    public static final int CLIENT_REQUEST_JOIN_ROOM = 0x09;
    public static final int CLIENT_REQUEST_START_GAME = 0x0E;
    public static final int CLIENT_EXIT_ROOM = 0x0F;
    public static final int CLIENT_LOAD_OK = 0x11;
    public static final int CLIENT_SHOT = 0x12;
    public static final int CLIENT_HOLE_INFO = 0x1A;
    public static final int CLIENT_SHOT_RESULT = 0x1B;
    public static final int CLIENT_SHOT_ACK = 0x1C;
    public static final int CLIENT_REQUEST_BUY_ITEM = 0x1D;
    public static final int CLIENT_REQUEST_EQUIP_ITEM = 0x20;
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

    /** C# {@code WarehouseItem.ToArray} Debug.Assert. */
    public static final int WAREHOUSE_ITEM_BYTES = 196;
    /** C# {@code CharacterInfo} struct size. */
    public static final int CHARACTER_INFO_BYTES = 513;
    /** C# {@code CaddieInfo.ToArray}. */
    public static final int CADDIE_INFO_BYTES = 25;
    public static final int MS_NUM_MAPS = 21;

    public static final int TIPO_STROKE = 0;
    public static final int TIPO_MATCH = 1;
    public static final int TIPO_LOUNGE = 2;
    public static final int TIPO_TOURNEY = 4;
    public static final int TIPO_TOURNEY_TEAM = 5;
    public static final int TIPO_GUILD_BATTLE = 6;
    public static final int TIPO_PANG_BATTLE = 7;
    public static final int TIPO_APPROACH = 10;
    public static final int TIPO_GRAND_ZODIAC_INT = 11;
    public static final int TIPO_GRAND_ZODIAC_ADV = 13;
    public static final int TIPO_GRAND_ZODIAC_PRACTICE = 14;
    public static final int TIPO_SPECIAL_SHUFFLE_COURSE = 18;
    public static final int TIPO_PRACTICE = 19;
    public static final int TIPO_GRAND_PRIX = 20;
    public static final int TIPO_MAX = 20;

    /** C# {@code RoomInfoEx.ToArray} (nome 40 + senha 24 + … + grand_prix 16). */
    public static final int ROOM_INFO_BYTES = 210;
    /** C# {@code MascotInfo.ToArray}. */
    public static final int MASCOT_INFO_BYTES = 62;
    /** C# {@code CardInfo.ToArray}. */
    public static final int CARD_INFO_BYTES = 58;
    /** JP {@code PlayerRoomInfo.ToArray} Debug.Assert size (guild 20, mark 12, unknown 3). */
    public static final int PLAYER_ROOM_INFO_BYTES = 348;
    /** JP {@code PlayerRoomInfoEx.ToArrayEx} = ToArray 348 + CharacterInfo 513. */
    public static final int PLAYER_ROOM_INFO_EX_BYTES = 861;
    /** C# {@code ClubSetInfo.ToArray}. */
    public static final int CLUBSET_INFO_BYTES = 28;
    /** C# {@code RoomInfo.eMODO.M_REPEAT}. */
    public static final int MODO_REPEAT = 4;

    /** C# start-game fail when the room is not ready ({@code 0x5900202}). */
    public static final int START_GAME_NOT_READY = 0x5900202;

    /** C# {@code CourseManager} always materializes 18 {@code HoleManager} entries. */
    public static final int COURSE_HOLE_COUNT = 18;
    /** C# {@code ShotSyncData.ToArray} / {@code DecryptShot} buffer. */
    public static final int SHOT_SYNC_BYTES = 54;
    /** C# {@code pacote06B} success err_code. */
    public static final int EQUIP_OK = 4;
    /** C# {@code requestBuyItemShop} catch: {@code 0x68} uint32 10. */
    public static final int BUY_FAIL_GENERIC = 10;

    /** C# {@code AIR_KNIGHT_SET} / IFF CLUBSET << 26. */
    public static final int TYPEID_AIR_KNIGHT = 0x10000000;
    /** C# {@code CHARACTER << 26} Nuri. */
    public static final int TYPEID_NURI = 0x4000000;
    /** IFF BALL << 26. */
    public static final int TYPEID_DEFAULT_BALL = 0x14000000;
    /** C# {@code IFF_GROUP.CHARACTER}: {@code typeid >>> 26}. */
    public static final int IFF_GROUP_CHARACTER = 1;

    /**
     * JP {@code LoginTask.sendCompleteData} prefix after decrypt:
     * {@code 0x44, 0x70, 0x71, 0x73, 0xE1, 0x72, 0x4D}.
     */
    public static final int LOGIN_DUMP_PREFIX_COUNT = 7;
    /**
     * JP tail after the channel list: {@code 0x102}…two {@code 0x25D}
     * (includes {@code 0xF1}/{@code 0x135}, no GB {@code 0x1B1}).
     */
    public static final int LOGIN_DUMP_TAIL_COUNT = 20;
    public static final int LOGIN_DUMP_PACKET_COUNT = LOGIN_DUMP_PREFIX_COUNT + LOGIN_DUMP_TAIL_COUNT;

    public static boolean isCharacterTypeid(int typeid) {
        return (typeid >>> 26) == IFF_GROUP_CHARACTER;
    }

    private GamePackets() {}

    /** C# {@code TrofelInfo.ToArray} Debug.Assert size. */
    public static final int TROPHY_BYTES = 78;
    /** C# {@code UserEquip.ToArray}. */
    public static final int USER_EQUIP_BYTES = 116;
    /** C# {@code PlayerInfo.GetMapStatistic} (21 maps × 43 × (3+9 seasons)). */
    public static final int MAP_STAT_BYTES = 10836;
    /** C# {@code UserEquipedItem.ToArray}. */
    public static final int EQUIPED_ITEM_BYTES = 628;
    /** JP {@code MemberInfoEx.ToArrayEx} (sala_numero + ToArray 297). */
    public static final int MEMBER_INFO_EX_BYTES = 299;
    /** JP {@code UserInfo.ToArray} (stMedal is 6×int32). */
    public static final int USER_INFO_BYTES = 265;
    /**
     * Bytes after opcode+option+PStr(clientVersion) in JP {@code principal()}
     * (no server-version PStr, no GB 277-byte guild pad).
     */
    public static final int PRINCIPAL_AFTER_VERSION_BYTES = 12270;
    /**
     * Bytes after opcode+option for canonical JP client version {@code JP.R7.983.00}
     * (PStr 14 + {@link #PRINCIPAL_AFTER_VERSION_BYTES}).
     */
    public static final int PRINCIPAL_PAYLOAD_BYTES = 14 + PRINCIPAL_AFTER_VERSION_BYTES;
    public static final String JP_CLIENT_VERSION = "JP.R7.983.00";
    public static final int JP_PACKET_VERSION = 2017110200;

    public static byte[] loginAck(int option) {
        return new PacketWriter().opcode(SERVER_LOGIN_ACK).u8(option).toBytes();
    }

    /**
     * JP {@code pacote044} option 0 + {@code principal()}: PStr clientVersion only
     * (no server version), then MemberInfoEx + uid + UserInfo + trophy + equip + map
     * + equipped items + SYSTEMTIME + server flags. No GB 277-byte pad.
     */
    public static byte[] loginOkPrincipal(
            String clientVersion,
            int oid,
            String id,
            String nick,
            int capability,
            int uid,
            int level,
            int serverProperty) {
        PacketWriter w = new PacketWriter().opcode(SERVER_LOGIN_ACK).u8(ACK_LOGIN_OK);
        w.pstr(clientVersion == null ? "" : clientVersion);
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
        w.i32(0); // ToTalClubsetCNT + ToTalPartsCNT
        w.u32(serverProperty);
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
        return mascots(List.of());
    }

    public static byte[] mascots(List<MascotInfo> items) {
        PacketWriter w = new PacketWriter().opcode(0xE1).u16(items.size() & 0xff);
        for (MascotInfo m : items) {
            w.bytes(m.toArray());
        }
        return w.toBytes();
    }

    /** C# {@code pacote138}: int32 option + uint16 count + {@code CardInfo} rows. */
    public static byte[] cards(List<CardInfo> items) {
        PacketWriter w = new PacketWriter().opcode(0x138).i32(0).u16(items.size());
        for (CardInfo c : items) {
            w.bytes(c.toArray());
        }
        return w.toBytes();
    }

    public static byte[] warehouse(List<WarehouseItem> items) {
        PacketWriter w = new PacketWriter().opcode(0x73).u16(items.size()).u16(items.size());
        for (WarehouseItem item : items) {
            w.bytes(item.toArray());
        }
        return w.toBytes();
    }

    public static byte[] characters(List<CharacterInfo> chars) {
        PacketWriter w = new PacketWriter().opcode(0x70).i16(chars.size()).i16(chars.size());
        for (CharacterInfo c : chars) {
            w.bytes(c.toArray());
        }
        return w.toBytes();
    }

    public static byte[] caddies(List<CaddieInfo> caddies) {
        PacketWriter w = new PacketWriter().opcode(0x71).i16(caddies.size()).i16(caddies.size());
        for (CaddieInfo c : caddies) {
            w.bytes(c.toArray());
        }
        return w.toBytes();
    }

    public static byte[] userEquip(UserEquip equip) {
        return new PacketWriter().opcode(0x72).bytes(equip.toArray()).toBytes();
    }

    /** Remaining C# {@code LoginTask.sendCompleteData} packets after the channel list. */
    public static List<byte[]> loginDumpTail(int uid, long pang, long cookie, int level) {
        return loginDumpTail(uid, pang, cookie, level, List.of());
    }

    public static List<byte[]> loginDumpTail(int uid, long pang, long cookie, int level, List<CardInfo> cardList) {
        return loginDumpTail(uid, pang, cookie, level, cardList, List.of(), List.of());
    }

    public static List<byte[]> loginDumpTail(
            int uid,
            long pang,
            long cookie,
            int level,
            List<CardInfo> cardList,
            List<CounterItem> counterList,
            List<AchievementInfo> achievementList) {
        List<byte[]> out = new ArrayList<>();
        out.add(new PacketWriter().opcode(0x102).i32(0).i32(0).u64(pang).u64(cookie).toBytes());
        PacketWriter th = new PacketWriter().opcode(0x131).u8(1).u8(MS_NUM_MAPS);
        for (int i = 0; i < MS_NUM_MAPS; i++) {
            th.u8(i).i32(1000);
        }
        out.add(th.toBytes());
        out.add(counters(counterList == null ? List.of() : counterList));
        out.add(achievements(achievementList == null ? List.of() : achievementList));
        // JP always sends messenger-ready 0xF1 and empty 0x135 before 0x144.
        out.add(new PacketWriter().opcode(0xF1).u8(0).toBytes());
        out.add(new PacketWriter().opcode(0x135).toBytes());
        out.add(new PacketWriter().opcode(0x144).u8(0).toBytes());
        out.add(cards(cardList));
        out.add(new PacketWriter().opcode(0x136).toBytes());
        out.add(new PacketWriter().opcode(0x137).u16(0).toBytes());
        out.add(new PacketWriter().opcode(0x13F).u8(0).toBytes());
        out.add(new PacketWriter().opcode(0x181).i32(0).u8(0).toBytes());
        out.add(new PacketWriter().opcode(0x96).u64(cookie).toBytes());
        out.add(new PacketWriter().opcode(0x169).u8(5).zero(TROPHY_BYTES).toBytes());
        out.add(new PacketWriter().opcode(0x169).u8(0).zero(TROPHY_BYTES).toBytes());
        out.add(new PacketWriter().opcode(0xB4).i16(5).u8(0).toBytes());
        out.add(new PacketWriter().opcode(0xB4).i16(0).u8(0).toBytes());
        out.add(new PacketWriter().opcode(0x158).u8(0).u32(uid).bytes(userInfo(level)).toBytes());
        out.add(new PacketWriter().opcode(0x25D).u8(5).u32(0).u32(0).toBytes());
        out.add(new PacketWriter().opcode(0x25D).u8(0).u32(0).u32(0).toBytes());
        return out;
    }

    public static byte[] clientCreateRoom(int tipo, String name, String password) {
        return new PacketWriter()
                .opcode(CLIENT_REQUEST_CREATE_ROOM)
                .u8(0)
                .u32(0)
                .u32(0)
                .u8(tipo == TIPO_PRACTICE ? 1 : 4)
                .u8(tipo)
                .u8(18)
                .u8(0)
                .u8(0)
                .u32(0)
                .pstr(name)
                .pstr(password)
                .u32(0)
                .toBytes();
    }

    public static final class WarehouseItem {
        public int id;
        public int typeid;
        public short[] c = new short[5];
        public int purchase;
        public int flag;
        public long applyDate;
        public long endDate;
        public int type;
        public short[] workshopC = new short[5];

        public byte[] toArray() {
            PacketWriter w = new PacketWriter();
            w.i32(id);
            w.u32(typeid);
            w.i32(0);
            for (short v : c) {
                w.i16(v);
            }
            w.u8(purchase);
            w.u8(flag);
            w.i64(applyDate);
            w.i64(endDate);
            w.u8(type);
            w.zero(40);
            w.u8(0);
            w.zero(9);
            w.u8(0);
            w.i16(0);
            w.zero(22);
            w.u32(0);
            w.zero(16 + 16 + 16);
            w.i16(0);
            for (short v : workshopC) {
                w.i16(v);
            }
            w.u32(0).u32(0).i32(0).i32(0);
            byte[] body = w.toBytes();
            if (body.length != WAREHOUSE_ITEM_BYTES) {
                throw new IllegalStateException("WarehouseItem size " + body.length);
            }
            return body;
        }
    }

    public static final class CharacterInfo {
        public int id;
        public int typeid;
        public int defaultHair;
        public int defaultShirts;
        public int giftFlag;
        public int purchase;
        public int[] partsTypeid = new int[24];
        public int[] partsId = new int[24];
        public int[] auxparts = new int[5];
        public int[] cutIn = new int[4];
        public byte[] pcl = new byte[5];
        public int mastery;
        public int[] cardCharacter = new int[4];
        public int[] cardCaddie = new int[4];
        public int[] cardNpc = new int[4];

        public byte[] toArray() {
            PacketWriter w = new PacketWriter();
            w.u32(typeid);
            w.i32(id);
            w.u8(defaultHair);
            w.u8(defaultShirts);
            w.u8(giftFlag);
            w.u8(purchase);
            for (int v : partsTypeid) {
                w.u32(v);
            }
            for (int v : partsId) {
                w.u32(v);
            }
            w.zero(216);
            for (int v : auxparts) {
                w.u32(v);
            }
            for (int v : cutIn) {
                w.u32(v);
            }
            w.bytes(pcl);
            w.u32(mastery);
            for (int v : cardCharacter) {
                w.u32(v);
            }
            for (int v : cardCaddie) {
                w.u32(v);
            }
            for (int v : cardNpc) {
                w.u32(v);
            }
            byte[] body = w.toBytes();
            if (body.length != CHARACTER_INFO_BYTES) {
                throw new IllegalStateException("CharacterInfo size " + body.length);
            }
            return body;
        }

        /** C# {@code CharacterInfo.ToRead}. */
        public static CharacterInfo read(PacketReader r) {
            CharacterInfo c = new CharacterInfo();
            c.typeid = r.u32();
            c.id = r.i32();
            c.defaultHair = r.u8();
            c.defaultShirts = r.u8();
            c.giftFlag = r.u8();
            c.purchase = r.u8();
            for (int i = 0; i < 24; i++) {
                c.partsTypeid[i] = r.u32();
            }
            for (int i = 0; i < 24; i++) {
                c.partsId[i] = r.u32();
            }
            if (r.remaining() >= 216) {
                r.readBytes(216);
            }
            for (int i = 0; i < 5; i++) {
                c.auxparts[i] = r.remaining() >= 4 ? r.u32() : 0;
            }
            for (int i = 0; i < 4; i++) {
                c.cutIn[i] = r.remaining() >= 4 ? r.u32() : 0;
            }
            if (r.remaining() >= 5) {
                byte[] pcl = r.readBytes(5);
                System.arraycopy(pcl, 0, c.pcl, 0, 5);
            }
            c.mastery = r.remaining() >= 4 ? r.u32() : 0;
            for (int i = 0; i < 4; i++) {
                c.cardCharacter[i] = r.remaining() >= 4 ? r.u32() : 0;
            }
            for (int i = 0; i < 4; i++) {
                c.cardCaddie[i] = r.remaining() >= 4 ? r.u32() : 0;
            }
            for (int i = 0; i < 4; i++) {
                c.cardNpc[i] = r.remaining() >= 4 ? r.u32() : 0;
            }
            return c;
        }
    }

    public static final class CaddieInfo {
        public int id;
        public int typeid;
        public int partsTypeid;
        public int level;
        public int exp;
        public int rentFlag;
        public int endDateUnix;
        public int partsEndDateUnix;
        public int purchase;
        public int checkEnd;

        public byte[] toArray() {
            PacketWriter w = new PacketWriter();
            w.i32(id);
            w.u32(typeid);
            w.u32(partsTypeid);
            w.u8(level);
            w.u32(exp);
            w.u8(rentFlag);
            w.u16(endDateUnix);
            w.i16(partsEndDateUnix);
            w.u8(purchase);
            w.i16(checkEnd);
            byte[] body = w.toBytes();
            if (body.length != CADDIE_INFO_BYTES) {
                throw new IllegalStateException("CaddieInfo size " + body.length);
            }
            return body;
        }
    }

    public static final class UserEquip {
        public int caddieId;
        public int characterId;
        public int clubsetId;
        public int ballTypeid;
        public int[] itemSlot = new int[10];
        public int[] skinId = new int[6];
        public int[] skinTypeid = new int[6];
        public int mascotId;
        public int[] poster = new int[2];

        public byte[] toArray() {
            PacketWriter w = new PacketWriter();
            w.i32(caddieId);
            w.i32(characterId);
            w.i32(clubsetId);
            w.u32(ballTypeid);
            for (int v : itemSlot) {
                w.u32(v);
            }
            for (int v : skinId) {
                w.u32(v);
            }
            for (int v : skinTypeid) {
                w.u32(v);
            }
            w.i32(mascotId);
            for (int v : poster) {
                w.u32(v);
            }
            byte[] body = w.toBytes();
            if (body.length != USER_EQUIP_BYTES) {
                throw new IllegalStateException("UserEquip size " + body.length);
            }
            return body;
        }
    }

    static byte[] memberInfoEx(int oid, String id, String nick, int capability) {
        return memberInfoExPublic(oid, id, nick, capability);
    }

    public static byte[] memberInfoExPublic(int oid, String id, String nick, int capability) {
        PacketWriter w = new PacketWriter();
        w.u16(0xffff); // sala_numero DEFAULT_ROOM_ID
        w.fixedStr(id, 22);
        w.fixedStr(nick, 22);
        w.zero(17); // guild_name
        w.zero(12); // guild_mark_img
        w.zero(35); // sComment (JP only)
        w.u32(0); // school
        w.i32(capability);
        w.u32(0); // galleryUid
        w.i32(oid);
        w.u32(0).u32(0).u32(0); // rank[3]
        w.u32(0); // guild_uid
        w.u32(0); // guild_mark_img_no
        w.u8(0); // state_flag
        w.u16(1); // flag_login_time (JP writes ushort)
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
        return userInfoPublic(level);
    }

    public static byte[] userInfoPublic(int level) {
        PacketWriter w = new PacketWriter();
        w.zero(16); // tacada, putt, tempo, tempo_tacada
        w.zero(4); // best_drive float
        w.zero(28); // acerto..hio (7×int32)
        w.zero(2); // bunker
        w.zero(16); // fairway, albatross, mad, putt_in
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
        w.zero(20); // ladder_point, hole, win, lose, draw
        w.zero(12); // combo, all_combo, quitado
        w.zero(8); // skin_pang
        w.zero(8); // skin_win, skin_lose
        w.zero(16); // skin_all_in, run_hole, strike, jogados_disconnect
        w.zero(2); // event_value int16
        w.zero(4); // disconnect
        w.zero(24); // stMedal 6×int32
        w.zero(4); // sys_school_serie
        w.zero(4); // game_count_season
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
     * C# {@code pacote049} success: int16 0 + {@code Room.getInfo().ToArray()} (210 bytes).
     */
    public static byte[] roomEntered(RoomInfo room) {
        return new PacketWriter()
                .opcode(SERVER_ROOM_ENTER_RESULT)
                .i16(0)
                .bytes(room.toArray())
                .toBytes();
    }

    /**
     * C# {@code pacote04A}: int16 option (always -1 from {@code Room.SendUpdate}) +
     * {@code RoomInfoEx.ToArrayEx()} lobby summary.
     */
    public static byte[] roomUpdate(RoomInfo room) {
        PacketWriter w = new PacketWriter().opcode(SERVER_ROOM_UPDATE).i16(-1);
        w.u8(room.tipoShow);
        w.u8(room.course & 0x7f);
        w.u8(room.holes);
        w.u8(room.modo);
        if (room.holeRepeat > 0 || room.modo == MODO_REPEAT) {
            w.u8(room.holeRepeat);
            w.u32(room.fixedHole);
        }
        w.u32(room.natural);
        w.u8(room.maxPlayer);
        w.u8(room.thirtyS);
        w.u8(room.stateFlag);
        w.u32(room.timeVs);
        w.u32(room.time30s);
        w.u32(room.trophy);
        w.u8(room.senhaFlag);
        w.pstr(room.name == null ? "" : room.name);
        return w.toBytes();
    }

    /**
     * C# {@code pacote048}. {@code option & 0x100} selects compact {@code PlayerRoomInfo};
     * the wire option byte is {@code option & 0xFF} (so Practice 0x100 writes 0).
     */
    public static byte[] roomPlayers(int option, List<PlayerRoomInfo> players) {
        boolean compact = (option & 0x100) != 0;
        int kind = option & 0xff;
        PacketWriter w = new PacketWriter().opcode(SERVER_ROOM_PLAYERS).u8(kind).i16(-1);
        if (kind == 0 || kind == 5) {
            w.u8(players.size());
        } else if (kind == 3) {
            int oid = players.isEmpty() ? 0 : players.getFirst().oid;
            w.i32(oid);
        }
        for (PlayerRoomInfo player : players) {
            w.bytes(compact ? player.toArray() : player.toArrayEx());
        }
        w.u8(0);
        return w.toBytes();
    }

    public static byte[] counters(List<CounterItem> items) {
        PacketWriter w = new PacketWriter().opcode(0x21D).u32(0).u32(items.size()).u32(items.size());
        for (CounterItem c : items) {
            w.u8(c.active());
            w.u32(c.typeid());
            w.i32(c.id());
            w.u32(c.value());
        }
        return w.toBytes();
    }

    public static byte[] achievements(List<AchievementInfo> items) {
        PacketWriter w = new PacketWriter().opcode(0x21E).u32(0).u32(items.size()).u32(items.size());
        for (AchievementInfo a : items) {
            w.u8(a.active());
            w.u32(a.typeid());
            w.i32(a.id());
            w.u32(a.status());
            w.u32(a.quests().size());
            for (QuestStuff q : a.quests()) {
                w.u32(q.typeid());
                w.u32(q.counterTypeid());
                w.i32(q.counterId());
                w.u32(q.clearDateUnix());
            }
        }
        return w.toBytes();
    }

    public record CounterItem(int id, int typeid, int active, int value) {}

    public record QuestStuff(int typeid, int counterTypeid, int counterId, int clearDateUnix) {}

    public record AchievementInfo(int id, int typeid, int active, int status, List<QuestStuff> quests) {}

    /** C# {@code Room::setTipo}: tipo_show for the lobby list. */
    public static int tipoShow(int tipo) {
        if (tipo > TIPO_GRAND_ZODIAC_PRACTICE) {
            return TIPO_TOURNEY;
        }
        if (tipo == TIPO_GRAND_ZODIAC_ADV || tipo == TIPO_GRAND_ZODIAC_PRACTICE) {
            return TIPO_GRAND_ZODIAC_INT;
        }
        return tipo;
    }

    /** C# {@code Room::setTipo}: tipo_ex is 255 unless tipo ≥ Grand Zodiac INT. */
    public static int tipoEx(int tipo) {
        return tipo >= TIPO_GRAND_ZODIAC_INT ? tipo : 255;
    }

    /** C# {@code requestStartGame} allows a single player for these tipos. */
    public static boolean allowsSoloStart(int tipo) {
        return tipo == TIPO_PRACTICE
                || tipo == TIPO_GRAND_PRIX
                || tipo == TIPO_GRAND_ZODIAC_INT
                || tipo == TIPO_GRAND_ZODIAC_ADV
                || tipo == TIPO_GRAND_ZODIAC_PRACTICE;
    }

    /** C# start-game success: empty {@code 0x230}, empty {@code 0x231}, {@code 0x77} pang rate. */
    public static byte[] startGameFlag() {
        return new PacketWriter().opcode(SERVER_START_GAME_FLAG).toBytes();
    }

    public static byte[] startGameFlag2() {
        return new PacketWriter().opcode(SERVER_START_GAME_FLAG2).toBytes();
    }

    public static byte[] pangRate(int rate) {
        return new PacketWriter().opcode(SERVER_PANG_RATE).u32(rate).toBytes();
    }

    public static byte[] startGameFailed(int code) {
        return new PacketWriter().opcode(SERVER_START_GAME_FAIL).u32(code).toBytes();
    }

    /**
     * C# {@code TourneyBase.sendInitialData} {@code 0x76}: tipo_show, uint32 1, SYSTEMTIME start.
     * Versus writes a full player dump instead — {@link #gameInitVersus}.
     */
    public static byte[] gameInitTourney(int tipoShow) {
        return new PacketWriter()
                .opcode(SERVER_GAME_INIT)
                .u8(tipoShow)
                .u32(1)
                .systemTimeNow()
                .toBytes();
    }

    /**
     * C# {@code VersusBase.sendInitialData} {@code 0x76}: tipo_show, player count,
     * then per player MemberInfoEx + uid + UserInfo + trophy + UserEquip + map stats
     * + CharacterInfo + Caddie + ClubSet + Mascot + SYSTEMTIME + card count.
     * Map stats are zeros when {@code pangya_mapstat} rows are absent (same as login principal).
     */
    public static byte[] gameInitVersus(int tipoShow, List<VersusPlayer> players) {
        PacketWriter w = new PacketWriter().opcode(SERVER_GAME_INIT).u8(tipoShow).u8(players.size());
        for (VersusPlayer player : players) {
            w.bytes(player.memberInfoEx());
            w.u32(player.uid());
            w.bytes(player.userInfo());
            w.zero(TROPHY_BYTES);
            w.bytes(player.userEquip());
            w.zero(MAP_STAT_BYTES);
            w.bytes(player.character());
            w.bytes(player.caddie());
            w.bytes(player.clubset());
            w.bytes(player.mascot());
            w.systemTimeNow();
            w.u8(player.cards() == null ? 0 : player.cards().size());
            if (player.cards() != null) {
                for (CardInfo card : player.cards()) {
                    w.bytes(card.toArray());
                }
            }
        }
        return w.toBytes();
    }

    public record VersusPlayer(
            byte[] memberInfoEx,
            int uid,
            byte[] userInfo,
            byte[] userEquip,
            byte[] character,
            byte[] caddie,
            byte[] clubset,
            byte[] mascot,
            List<CardInfo> cards) {}

    /** C# {@code 0x16A} mascot-effect seed after Versus {@code 0x52}. */
    public static byte[] mascotSeed(int seed) {
        return new PacketWriter().opcode(SERVER_MASCOT_SEED).u32(seed).toBytes();
    }

    /**
     * C# {@code GameBase.sendInitialData} {@code 0x52} + {@code CourseManager.makePacketHoleInfo}
     * option 0. Cube count 0 is valid when IFF/cube files are absent.
     */
    public static byte[] course(RoomInfo room, List<HoleInfo> holes, int seed) {
        PacketWriter w = new PacketWriter().opcode(SERVER_COURSE);
        w.u8(room.course & 0x7f);
        w.u8(room.tipoShow);
        w.u8(room.modo);
        w.u8(room.holes);
        w.u32(room.trophy);
        w.u32(room.timeVs);
        w.u32(room.time30s);
        for (HoleInfo hole : holes) {
            w.u32(hole.id());
            w.u8(hole.pin());
            w.u8(hole.course());
            w.u8(hole.numero());
        }
        w.u32(seed);
        for (int i = 0; i < holes.size(); i++) {
            w.u8(0);
        }
        return w.toBytes();
    }

    public static byte[] weather(int weather) {
        return new PacketWriter().opcode(SERVER_WEATHER).u16(weather).u8(0).toBytes();
    }

    public static byte[] wind(int wind, int cardFlag, int degree, int reset) {
        return new PacketWriter()
                .opcode(SERVER_WIND)
                .u8(wind)
                .u8(cardFlag)
                .u16(degree)
                .u8(reset)
                .toBytes();
    }

    public static byte[] remainTime(int millis) {
        return new PacketWriter().opcode(SERVER_REMAIN_TIME).u32(millis).toBytes();
    }

    /** C# {@code TourneyBase.sendSyncShot} {@code 0x6E}. */
    public static byte[] syncShot(int oid, int hole, float x, float z, int shotState, int tempo) {
        return new PacketWriter()
                .opcode(SERVER_SYNC_SHOT)
                .i32(oid)
                .u8(hole)
                .f32(x)
                .f32(z)
                .u32(shotState)
                .u16(tempo)
                .toBytes();
    }

    /** C# {@code sendEndShot} with empty drop list: oid + count 0. */
    public static byte[] endShot(int oid) {
        return new PacketWriter().opcode(SERVER_END_SHOT).i32(oid).u8(0).toBytes();
    }

    /**
     * C# {@code pacote06B}: err 4 = success. Extra body is type-specific and omitted on error.
     */
    public static byte[] equipAck(int err, int type, byte[] extra) {
        PacketWriter w = new PacketWriter().opcode(SERVER_EQUIP_ACK).u8(err).u8(type);
        if (err == EQUIP_OK && extra != null) {
            w.bytes(extra);
        }
        return w.toBytes();
    }

    /** C# {@code requestBuyItemShop} catch path. Shop catalog (IFF) is not in this env. */
    public static byte[] buyFailed(int code) {
        return new PacketWriter().opcode(SERVER_BUY_ACK).u32(code).toBytes();
    }

    public static boolean usesTourneyInitialData(int tipo) {
        return tipo == TIPO_PRACTICE
                || tipo == TIPO_TOURNEY
                || tipo == TIPO_TOURNEY_TEAM
                || tipo == TIPO_GRAND_PRIX
                || tipo == TIPO_GRAND_ZODIAC_INT
                || tipo == TIPO_GRAND_ZODIAC_ADV
                || tipo == TIPO_GRAND_ZODIAC_PRACTICE
                || tipo == TIPO_SPECIAL_SHUFFLE_COURSE
                || tipo == TIPO_APPROACH
                || tipo == TIPO_GUILD_BATTLE;
    }

    /** C# modes that extend {@code VersusBase} (Stroke / Match / Pang Battle). */
    public static boolean usesVersusInitialData(int tipo) {
        return tipo == TIPO_STROKE || tipo == TIPO_MATCH || tipo == TIPO_PANG_BATTLE;
    }

    /**
     * C# {@code room.sendCharacter}: compact {@code PlayerRoomInfo} unless the room is
     * Stroke/Match/Lounge/Pang Battle (those send {@code PlayerRoomInfoEx}).
     */
    public static boolean usesCompactPlayerRoomInfo(int tipo) {
        return tipo != TIPO_STROKE
                && tipo != TIPO_MATCH
                && tipo != TIPO_LOUNGE
                && tipo != TIPO_PANG_BATTLE;
    }

    public static byte[] clientStartGame() {
        return new PacketWriter().opcode(CLIENT_REQUEST_START_GAME).toBytes();
    }

    public static byte[] clientExitRoom() {
        return new PacketWriter().opcode(CLIENT_EXIT_ROOM).toBytes();
    }

    public static byte[] clientJoinRoom(int numero, String password) {
        return new PacketWriter()
                .opcode(CLIENT_REQUEST_JOIN_ROOM)
                .i16(numero)
                .pstr(password == null ? "" : password)
                .toBytes();
    }

    public static byte[] clientInitHole(int numero, int option, int unknown, int par,
            float teeX, float teeZ, float pinX, float pinZ) {
        return new PacketWriter()
                .opcode(CLIENT_HOLE_INFO)
                .u8(numero)
                .u32(option)
                .u32(unknown)
                .u8(par)
                .f32(teeX)
                .f32(teeZ)
                .f32(pinX)
                .f32(pinZ)
                .toBytes();
    }

    public static byte[] clientLoadOk() {
        return new PacketWriter().opcode(CLIENT_LOAD_OK).toBytes();
    }

    public static byte[] clientShot() {
        return new PacketWriter().opcode(CLIENT_SHOT).u16(0).toBytes();
    }

    public static byte[] clientShotAck() {
        return new PacketWriter().opcode(CLIENT_SHOT_ACK).toBytes();
    }

    /**
     * C# {@code DecryptShot}: XOR the 54-byte {@code ShotSyncData} with {@code RoomInfo.key[i%16]}.
     */
    public static byte[] xorRoomKey(byte[] src, byte[] key) {
        byte[] out = src.clone();
        for (int i = 0; i < out.length; i++) {
            out[i] ^= key[i % 16];
        }
        return out;
    }

    public static byte[] shotSyncPlain(
            int oid, float x, float y, float z, int state, int bunker, int unknown,
            int pang, int bonusPang, int displayState, int shotState, int tempo, int gpPenalty) {
        PacketWriter w = new PacketWriter();
        w.i32(oid);
        w.f32(x).f32(y).f32(z);
        w.u8(state).u8(bunker).u8(unknown);
        w.u32(pang).u32(bonusPang);
        w.u32(displayState).u32(shotState);
        w.i16(tempo);
        w.u8(gpPenalty);
        w.zero(16);
        byte[] body = w.toBytes();
        if (body.length != SHOT_SYNC_BYTES) {
            throw new IllegalStateException("ShotSyncData size " + body.length);
        }
        return body;
    }

    public static byte[] clientShotResult(byte[] encrypted54) {
        return new PacketWriter().opcode(CLIENT_SHOT_RESULT).bytes(encrypted54).toBytes();
    }

    public static byte[] clientEquipCharacter(int characterId) {
        return new PacketWriter().opcode(CLIENT_REQUEST_EQUIP_ITEM).u8(5).i32(characterId).toBytes();
    }

    public static byte[] clientEquipParts(CharacterInfo character) {
        return new PacketWriter()
                .opcode(CLIENT_REQUEST_EQUIP_ITEM)
                .u8(0)
                .bytes(character.toArray())
                .toBytes();
    }

    public static byte[] clientEquipCaddie(int caddieId) {
        return new PacketWriter().opcode(CLIENT_REQUEST_EQUIP_ITEM).u8(1).i32(caddieId).toBytes();
    }

    public static byte[] clientEquipBallAndClub(int ballTypeid, int clubId) {
        return new PacketWriter()
                .opcode(CLIENT_REQUEST_EQUIP_ITEM)
                .u8(3)
                .i32(ballTypeid)
                .i32(clubId)
                .toBytes();
    }

    public static byte[] clientEquipMascot(int mascotId) {
        return new PacketWriter().opcode(CLIENT_REQUEST_EQUIP_ITEM).u8(8).i32(mascotId).toBytes();
    }

    public static byte[] clientBuyItem() {
        return new PacketWriter().opcode(CLIENT_REQUEST_BUY_ITEM).u16(0).toBytes();
    }

    public static InitHole readInitHole(PacketReader reader) {
        int numero = reader.u8();
        int option = reader.remaining() >= 4 ? reader.u32() : 0;
        int unknown = reader.remaining() >= 4 ? reader.u32() : 0;
        int par = reader.remaining() >= 1 ? reader.u8() : 0;
        float teeX = reader.remaining() >= 4 ? reader.f32() : 0;
        float teeZ = reader.remaining() >= 4 ? reader.f32() : 0;
        float pinX = reader.remaining() >= 4 ? reader.f32() : 0;
        float pinZ = reader.remaining() >= 4 ? reader.f32() : 0;
        return new InitHole(numero, option, unknown, par, teeX, teeZ, pinX, pinZ);
    }

    public static JoinRoom readJoinRoom(PacketReader reader) {
        int numero = reader.i16();
        String password = reader.remaining() >= 2 ? reader.pstr() : "";
        return new JoinRoom(numero, password);
    }

    public static ShotSync readShotSync(byte[] plain54) {
        PacketReader r = new PacketReader(plain54);
        int oid = r.i32();
        float x = r.f32();
        float y = r.f32();
        float z = r.f32();
        int state = r.u8();
        int bunker = r.u8();
        int unknown = r.u8();
        int pang = r.u32();
        int bonus = r.u32();
        int display = r.u32();
        int shot = r.u32();
        int tempo = r.i16();
        int gp = r.remaining() >= 1 ? r.u8() : 0;
        return new ShotSync(oid, x, y, z, state, bunker, unknown, pang, bonus, display, shot, tempo, gp);
    }

    public record HoleInfo(int id, int pin, int course, int numero, int weather, int wind, int degree) {}

    public record InitHole(
            int numero, int option, int unknown, int par,
            float teeX, float teeZ, float pinX, float pinZ) {}

    public record JoinRoom(int numero, String password) {}

    public record ShotSync(
            int oid, float x, float y, float z, int state, int bunker, int unknown,
            int pang, int bonusPang, int displayState, int shotState, int tempo, int gpPenalty) {}

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
        int artefato = reader.remaining() >= 4 ? reader.u32() : 0;
        return new CreateRoom(
                option, timeVs, time30s, maxPlayer, tipo, holes, course, modo, natural, name, password, artefato);
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
            String password,
            int artefato) {}

    public static final class MascotInfo {
        public int id;
        public int typeid;
        public int level;
        public int exp;
        public String message = "";
        public int tipo;
        public int pcBangMascot;

        public byte[] toArray() {
            PacketWriter w = new PacketWriter();
            w.i32(id);
            w.u32(typeid);
            w.u8(level);
            w.u32(exp);
            w.fixedStr(message, 30);
            w.u16(tipo);
            w.zero(16);
            w.u8(pcBangMascot);
            byte[] body = w.toBytes();
            if (body.length != MASCOT_INFO_BYTES) {
                throw new IllegalStateException("MascotInfo size " + body.length);
            }
            return body;
        }
    }

    public static final class CardInfo {
        public int id;
        public int typeid;
        public int slot;
        public int efeito;
        public int efeitoQntd;
        public int qntd;
        public int type;
        public int useYn;

        public byte[] toArray() {
            PacketWriter w = new PacketWriter();
            w.i32(id);
            w.u32(typeid);
            w.u32(slot);
            w.u32(efeito);
            w.u32(efeitoQntd);
            w.i32(qntd);
            w.zero(16);
            w.zero(16);
            w.u8(type);
            w.u8(useYn);
            byte[] body = w.toBytes();
            if (body.length != CARD_INFO_BYTES) {
                throw new IllegalStateException("CardInfo size " + body.length);
            }
            return body;
        }
    }

    /**
     * C# {@code RoomInfoEx.ToArray} used by {@code pacote049}.
     * Guild {@code ToArray} writes uid pair + two 17-byte names + two 12-byte marks (no index).
     */
    public static final class RoomInfo {
        public String name = "";
        public String password = "";
        public int senhaFlag = 1;
        public int state = 1;
        public int flag;
        public int maxPlayer;
        public int numPlayer;
        public byte[] key = new byte[16];
        public int galleryNum;
        public int thirtyS = 30;
        public int holes;
        public int tipoShow;
        public int numero;
        public int modo;
        public int course;
        public int timeVs;
        public int time30s;
        public int trophy;
        public int stateFlag;
        public int ratePang;
        public int rateExp;
        public int master;
        public int tipoEx = 255;
        public int artefato;
        public int natural;
        public int holeRepeat;
        public int fixedHole;
        public int gpDadosTypeid;
        public int gpRankTypeid;
        public int gpTempo;
        public int gpActive;

        public byte[] toArray() {
            PacketWriter w = new PacketWriter();
            w.fixedStr(name, 40);
            w.fixedStr(password, 24);
            w.u8(senhaFlag);
            w.u8(state);
            w.u8(flag);
            w.u8(maxPlayer);
            w.u8(numPlayer);
            w.bytes(key, 16);
            w.u8(galleryNum);
            w.u8(thirtyS);
            w.u8(holes);
            w.u8(tipoShow);
            w.u16(numero);
            w.u8(modo);
            w.u8(course & 0x7f);
            w.u32(timeVs);
            w.u32(time30s);
            w.u32(trophy);
            w.i16(stateFlag);
            w.i32(0).i32(0);
            w.fixedStr("", 17);
            w.fixedStr("", 17);
            w.fixedStr("", 12);
            w.fixedStr("", 12);
            w.u32(ratePang);
            w.u32(rateExp);
            w.i32(master);
            w.u8(tipoEx);
            w.u32(artefato);
            w.u32(natural);
            w.u32(gpDadosTypeid);
            w.u32(gpRankTypeid);
            w.u32(gpTempo);
            w.u32(gpActive);
            byte[] body = w.toBytes();
            if (body.length != ROOM_INFO_BYTES) {
                throw new IllegalStateException("RoomInfo size " + body.length);
            }
            return body;
        }
    }

    /**
     * C# {@code PlayerRoomInfo.ToArray} / {@code PlayerRoomInfoEx.ToArrayEx}.
     * Master sets bits 3 and 9; team is {@code (position-1)%2} on bit 0; place is 0x0A.
     */
    public static final class PlayerRoomInfo {
        public int oid;
        public String nickname = "";
        public String guildName = "";
        public int position;
        public int capability;
        public int title;
        public int charTypeid;
        public int[] skin = new int[6];
        public int stateFlag;
        public int level;
        public int iconAngel;
        public int place = 10;
        public int guildUid;
        public String guildMark = "";
        public int guildMarkIndex;
        public int uid;
        public int stateLounge;
        public int unknownFlg;
        public int state;
        public float x;
        public float z;
        public float r;
        public int shopActive;
        public String shopName = "";
        public int mascotTypeid;
        public int itemBoost;
        public int unknownFlg2;
        public String displayId = "";
        public int convidado;
        public float avgScore;
        public CharacterInfo character;

        public byte[] toArray() {
            PacketWriter w = new PacketWriter();
            w.i32(oid);
            w.fixedStr(nickname, 22);
            w.fixedStr(guildName, 20);
            w.u8(position);
            w.i32(capability);
            w.u32(title);
            w.u32(charTypeid);
            for (int v : skin) {
                w.u32(v);
            }
            w.u16(stateFlag);
            w.u8(level);
            w.u8(iconAngel);
            w.u8(place);
            w.i32(guildUid);
            w.fixedStr(guildMark, 12);
            w.u32(guildMarkIndex);
            w.u32(uid);
            w.u32(stateLounge);
            w.i16(unknownFlg);
            w.u32(state);
            w.f32(x);
            w.f32(z);
            w.f32(r);
            w.u32(shopActive);
            w.fixedStr(shopName, 64);
            w.u32(mascotTypeid);
            w.u16(itemBoost);
            w.u32(unknownFlg2);
            w.fixedStr(displayId, 128);
            w.u8(convidado);
            w.f32(avgScore);
            w.zero(3);
            byte[] body = w.toBytes();
            if (body.length != PLAYER_ROOM_INFO_BYTES) {
                throw new IllegalStateException("PlayerRoomInfo size " + body.length);
            }
            return body;
        }

        public byte[] toArrayEx() {
            PacketWriter w = new PacketWriter();
            w.bytes(toArray());
            w.bytes(character == null ? new byte[CHARACTER_INFO_BYTES] : character.toArray());
            byte[] body = w.toBytes();
            if (body.length != PLAYER_ROOM_INFO_EX_BYTES) {
                throw new IllegalStateException("PlayerRoomInfoEx size " + body.length);
            }
            return body;
        }
    }

    public static final class ClubSetInfo {
        public int id;
        public int typeid;
        public short[] slotC = new short[5];
        public short[] enchantC = new short[5];

        public byte[] toArray() {
            PacketWriter w = new PacketWriter();
            w.i32(id);
            w.u32(typeid);
            for (short v : slotC) {
                w.i16(v);
            }
            for (short v : enchantC) {
                w.i16(v);
            }
            byte[] body = w.toBytes();
            if (body.length != CLUBSET_INFO_BYTES) {
                throw new IllegalStateException("ClubSetInfo size " + body.length);
            }
            return body;
        }

        public static ClubSetInfo fromWarehouse(WarehouseItem item) {
            ClubSetInfo c = new ClubSetInfo();
            if (item == null) {
                return c;
            }
            c.id = item.id;
            c.typeid = item.typeid;
            System.arraycopy(item.c, 0, c.slotC, 0, Math.min(5, item.c.length));
            System.arraycopy(item.workshopC, 0, c.enchantC, 0, Math.min(5, item.workshopC.length));
            return c;
        }
    }

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
