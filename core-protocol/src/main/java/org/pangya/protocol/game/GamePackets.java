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
    /** C# {@code SERVER_ROOM_USER_INFO_CHANGED} / {@code pacote04B}. */
    public static final int SERVER_ROOM_USER_INFO_CHANGED = 0x4B;
    public static final int SERVER_EXIT_ROOM = 0x4C;
    /** C# {@code SERVER_MY_STATISTICS} / {@code sendUpdateInfoAndMapStatistics}. */
    public static final int SERVER_MY_STATISTICS = 0x45;
    /** C# {@code sendUpdateState}: oid + u8 (2 finished / 3 left). */
    public static final int SERVER_GAME_PLAYER_STATE = 0x6C;
    /** C# {@code updateFinishHole} / {@code SERVER_UPDATE_HOLE}. */
    public static final int SERVER_UPDATE_HOLE = 0x6D;
    /** C# {@code sendPlacar} / {@code SERVER_GAME_RESULT}. */
    public static final int SERVER_GAME_RESULT = 0x79;
    /** C# {@code SERVER_PAUSE} Versus {@code 0x8B}. */
    public static final int SERVER_PAUSE = 0x8B;
    /** C# {@code SERVER_SLEEP} / {@code pacote08E}: oid + u8 state. */
    public static final int SERVER_SLEEP = 0x8E;
    /** C# {@code SERVER_TEESHOT_READY_ACK} empty Versus {@code 0x90}. */
    public static final int SERVER_TEESHOT_READY_ACK = 0x90;
    /** C# {@code SERVER_REEMPLOY_CADDIE_ACK} / {@code 0x93}. */
    public static final int SERVER_REEMPLOY_CADDIE_ACK = 0x93;
    /** C# {@code SERVER_REPORT} / {@code 0x94}: u8 0 first report / 1 already. */
    public static final int SERVER_REPORT = 0x94;
    /** C# {@code sendDropItem} / {@code SERVER_PRIZE_LIST}. */
    public static final int SERVER_PRIZE_LIST = 0xCE;
    /** C# {@code requestSendTreasureHunterItem} / {@code SERVER_UPDATE_TREASURE_GIFT_LIST}. */
    public static final int SERVER_UPDATE_TREASURE_GIFT_LIST = 0x134;
    /** C# empty last-hole notify after the final Tourney hole. */
    public static final int SERVER_LAST_HOLE = 0x199;
    public static final int SERVER_PANG_RATE = 0x77;
    public static final int SERVER_COURSE = 0x52;
    public static final int SERVER_WIND = 0x5B;
    public static final int SERVER_CAMERA = 0x56;
    public static final int SERVER_POWER_SHOT = 0x58;
    public static final int SERVER_CLUB = 0x59;
    public static final int SERVER_ACTIVE_ITEM = 0x5A;
    public static final int SERVER_TIMEOUT = 0x5C;
    public static final int SERVER_TYPING = 0x5D;
    public static final int SERVER_MOVE_BALL = 0x60;
    public static final int SERVER_LOAD_PERCENT = 0xA3;
    public static final int SERVER_TEAM_CHAT = 0xB0;
    public static final int SERVER_GAME_INIT = 0x76;
    /** C# {@code SERVER_RESPONSE_GIFT_ITEM} / {@code 0x6A}: u32 code + u64 pang + u64 cookie. */
    public static final int SERVER_RESPONSE_GIFT_ITEM = 0x6A;
    public static final int SERVER_EQUIP_ACK = 0x6B;
    public static final int SERVER_SYNC_SHOT = 0x6E;
    public static final int SERVER_REMAIN_TIME = 0x8D;
    public static final int SERVER_WEATHER = 0x9E;
    public static final int SERVER_END_SHOT = 0xCC;
    public static final int SERVER_BUY_ACK = 0x68;
    public static final int SERVER_CHAT = 0x40;
    public static final int SERVER_USERLIST = 0x46;
    public static final int SERVER_ROOMLIST = 0x47;
    public static final int SERVER_READY = 0x78;
    public static final int SERVER_ENTER_LOBBY = 0xF5;
    public static final int SERVER_LEAVE_LOBBY = 0xF6;
    public static final int SERVER_WHISPER = 0x84;
    public static final int SERVER_INVITE = 0x83;
    /** C# {@code SERVER_RESPONSE_SERVER_TIME} / {@code WriteTime()} SYSTEMTIME. */
    public static final int SERVER_RESPONSE_SERVER_TIME = 0xBA;
    public static final int SERVER_ROOM_DETAIL = 0x86;
    public static final int SERVER_PLAYER_INFO = 0x89;
    public static final int SERVER_INVITE_REPLY = 0x12F;
    public static final int SERVER_TEAM = 0x7D;
    /** C# {@code SERVER_DECISION_ROOM_MASTER} / {@code pacote07C}. */
    public static final int SERVER_DECISION_ROOM_MASTER = 0x7C;
    public static final int SERVER_SERVER_LIST = 0x9F;
    /** C# {@code SERVER_RESPONSE_USERINFO_OFFLINE}. */
    public static final int SERVER_USERINFO_OFFLINE = 0xA1;
    public static final int SERVER_RANK_ADDRESS = 0xA2;
    /** C# {@code pacote10E} last-5 players. */
    public static final int SERVER_LAST5 = 0x10E;
    /** C# {@code pacote0AA} / {@code SERVER_NEW_ITEM}. */
    public static final int SERVER_NEW_ITEM = 0xAA;
    /** C# pang spent after shop buy ({@code 0xC8} + remaining + spent). */
    public static final int SERVER_PANG_SPENT = 0xC8;
    public static final int SERVER_COOKIE = 0x96;
    /** C# {@code SERVER_MSN_ACK} / {@code 0x95}: u16 sub + u32 + optional pang. */
    public static final int SERVER_MSN_ACK = 0x95;
    /** C# ticker error uses {@code SERVER_CHANGE_NICK_ACK} {@code 0x50} u32. */
    public static final int SERVER_CHANGE_NICK_ACK = 0x50;
    /** C# {@code SERVER_CHAT_PENALITY} / {@code 0xAC}: oid + u8. */
    public static final int SERVER_CHAT_PENALITY = 0xAC;
    /** C# {@code SERVER_SPEED_RATE} / {@code 0xC7}: f32 + oid. */
    public static final int SERVER_SPEED_RATE = 0xC7;
    /** C# {@code SERVER_ONELINE_MSG} ticker {@code 0xC9}: nick + msg PStr. */
    public static final int SERVER_ONELINE_MSG = 0xC9;
    /** C# {@code SERVER_ONELINE_QUERY} {@code 0xCA}: u16 count + u32 wait ms. */
    public static final int SERVER_ONELINE_QUERY = 0xCA;
    /** C# {@code SERVER_CHANGE_MASCOT} fail {@code 0xE2}. */
    public static final int SERVER_CHANGE_MASCOT = 0xE2;
    /** C# personal-shop {@code 0xE3} cancel-edit. */
    public static final int SERVER_SHOP_CANCEL = 0xE3;
    /** C# personal-shop {@code 0xE4} close. */
    public static final int SERVER_SHOP_CLOSE = 0xE4;
    /** C# personal-shop {@code 0xE5} open-edit (close-fail also uses this). */
    public static final int SERVER_SHOP_EDIT = 0xE5;
    /** C# personal-shop {@code 0xE6} view. */
    public static final int SERVER_SHOP_VIEW = 0xE6;
    /** C# personal-shop {@code 0xE7} close-view. */
    public static final int SERVER_SHOP_CLOSE_VIEW = 0xE7;
    /** C# personal-shop {@code 0xE8} name. */
    public static final int SERVER_SHOP_NAME = 0xE8;
    /** C# personal-shop {@code 0xE9} visit count. */
    public static final int SERVER_SHOP_VISIT = 0xE9;
    /** C# personal-shop {@code 0xEA} pang sale. */
    public static final int SERVER_SHOP_PANG = 0xEA;
    /** C# personal-shop {@code 0xEB} open-with-items. */
    public static final int SERVER_SHOP_ITEMS = 0xEB;
    /** C# personal-shop {@code 0xEC} buy. */
    public static final int SERVER_SHOP_BUY = 0xEC;
    /** C# {@code SERVER_OPEN_PAPEL_SHOP} {@code 0x10B}. */
    public static final int SERVER_PAPEL_SHOP = 0x10B;
    /** C# {@code SERVER_REQ_ENTER_SHOP_ACK} {@code 0x20E}. */
    public static final int SERVER_ENTER_SHOP = 0x20E;
    /** C# {@code SERVER_YOU_RECEIVED_NEW_MAIL} / {@code pacote210}. */
    public static final int SERVER_NEW_MAIL = 0x210;
    /** C# {@code SERVER_REQ_NEW_MAILBOX_OPEN_MAILBOX_ACK} {@code 0x211}. */
    public static final int SERVER_MAILBOX = 0x211;
    /** C# {@code SERVER_REQ_NEW_MAILBOX_OPEN_MAIL_ACK} {@code 0x212}. */
    public static final int SERVER_MAIL_INFO = 0x212;
    /** C# {@code SERVER_REQ_NEW_MAILBOX_SEND_MAIL_ACK} {@code 0x213}. */
    public static final int SERVER_MAIL_SEND = 0x213;
    /** C# {@code SERVER_REQ_NEW_MAILBOX_MOVE_ITEM_TO_MYROOM_ACK} {@code 0x214}. */
    public static final int SERVER_MAIL_TAKE = 0x214;
    /** C# {@code SERVER_REQ_NEW_MAILBOX_DELETE_MAIL_ACK} {@code 0x215}. */
    public static final int SERVER_MAIL_DELETE = 0x215;
    /** C# {@code SERVER_SYNC_ACTIVITY} / {@code pacote0C4}: oid + u8 type + payload. */
    public static final int SERVER_SYNC_ACTIVITY = 0xC4;
    public static final int SERVER_MASCOT_SEED = 0x16A;
    /** C# {@code pacote196}: oid + {@code StateCharacterLounge} (4 floats). */
    public static final int SERVER_LOUNGE_STATE = 0x196;
    public static final int SERVER_START_GAME_FLAG = 0x230;
    public static final int SERVER_START_GAME_FLAG2 = 0x231;
    public static final int SERVER_START_GAME_FAIL = 0x253;

    public static final int CLIENT_REQUEST_LOGIN = 0x02;
    public static final int CLIENT_CHAT = 0x03;
    public static final int CLIENT_ENTER_CHANNEL = 0x04;
    /** C# {@code CLIENT_MY_STATISTICS} / {@code packet006} {@code requestFinishGame}. */
    public static final int CLIENT_MY_STATISTICS = 0x06;
    /** C# {@code CLIENT_REQUEST_USERINFO_OFFLINE} / {@code packet007} {@code requestCheckNick}. */
    public static final int CLIENT_REQUEST_USERINFO_OFFLINE = 0x07;
    public static final int CLIENT_REQUEST_CREATE_ROOM = 0x08;
    public static final int CLIENT_REQUEST_JOIN_ROOM = 0x09;
    public static final int CLIENT_CHANGE_ROOM_INFO = 0x0A;
    /** C# {@code CLIENT_LOBBY_USERINFO_CHANGED} / {@code packet00B}. */
    public static final int CLIENT_LOBBY_USERINFO_CHANGED = 0x0B;
    /** C# {@code CLIENT_REQUEST_USERINFO_CHANGED} / {@code packet00C} in-room equip. */
    public static final int CLIENT_REQUEST_USERINFO_CHANGED = 0x0C;
    public static final int CLIENT_SET_READY = 0x0D;
    public static final int CLIENT_REQUEST_START_GAME = 0x0E;
    public static final int CLIENT_EXIT_ROOM = 0x0F;
    public static final int CLIENT_LOAD_OK = 0x11;
    public static final int CLIENT_SHOT = 0x12;
    public static final int CLIENT_CAMERA = 0x13;
    public static final int CLIENT_CLICK = 0x14;
    public static final int CLIENT_POWER_SHOT = 0x15;
    public static final int CLIENT_CLUB = 0x16;
    public static final int CLIENT_USE_ITEM = 0x17;
    public static final int CLIENT_EMOTICON = 0x18;
    public static final int CLIENT_DROP = 0x19;
    public static final int CLIENT_HOLE_INFO = 0x1A;
    public static final int CLIENT_SHOT_RESULT = 0x1B;
    public static final int CLIENT_SHOT_ACK = 0x1C;
    public static final int CLIENT_TIMECHECK = 0x22;
    /** C# {@code CLIENT_REQUEST_BANISH} / {@code packet026} {@code requestKickPlayerOfRoom}. */
    public static final int CLIENT_REQUEST_BANISH = 0x26;
    public static final int CLIENT_REQUEST_BUY_ITEM = 0x1D;
    /** C# {@code CLIENT_REQUEST_GIFT_ITEM} / {@code packet01F} {@code requestGiftItemShop}. */
    public static final int CLIENT_REQUEST_GIFT_ITEM = 0x1F;
    /** C# {@code CLIENT_REQ_CHARACTER_STAT_IN_CHATROOM} → {@code pacote196}. */
    public static final int CLIENT_LOUNGE_STATE = 0xEB;
    public static final int CLIENT_REQUEST_EQUIP_ITEM = 0x20;
    public static final int CLIENT_LEAVE_PRACTICE = 0x130;
    public static final int CLIENT_ENTER_LOBBY = 0x81;
    public static final int CLIENT_LEAVE_LOBBY = 0x82;
    public static final int CLIENT_KEEPALIVE = 0x01;
    public static final int CLIENT_WHISPER = 0x2A;
    public static final int CLIENT_CHECK_INVITE = 0x29;
    public static final int CLIENT_REQUEST_DETAIL_ROOM_INFO = 0x2D;
    public static final int CLIENT_REQUEST_CASH = 0x3D;
    public static final int CLIENT_REQUEST_USERINFO = 0x2F;
    /** C# {@code CLIENT_PAUSE} / {@code packet030} Versus {@code requestUnOrPause}. */
    public static final int CLIENT_PAUSE = 0x30;
    /** C# {@code CLIENT_HOLE_STAT} / {@code packet031} {@code requestFinishHoleData}. */
    public static final int CLIENT_HOLE_STAT = 0x31;
    /** C# {@code CLIENT_SLEEP} / {@code packet032} {@code requestChangePlayerStateAFKRoom}. */
    public static final int CLIENT_SLEEP = 0x32;
    /** C# {@code CLIENT_TEESHOT_READY} / {@code packet034} {@code requestFinishCharIntro}. */
    public static final int CLIENT_TEESHOT_READY = 0x34;
    /** C# {@code CLIENT_END_STROKE_GAME} / {@code packet037} {@code requestLastPlayerFinishVersus}. */
    public static final int CLIENT_END_STROKE_GAME = 0x37;
    /** C# {@code CLIENT_TEAM_HOLEIN_PANG} / {@code packet035} {@code requestTeamFinishHole}. */
    public static final int CLIENT_TEAM_HOLEIN_PANG = 0x35;
    /** C# {@code CLIENT_ANSWER_GOSTOP} / {@code packet036} {@code requestReplyContinueVersus}. */
    public static final int CLIENT_ANSWER_GOSTOP = 0x36;
    /** C# {@code CLIENT_REEMPLOY_CADDIE} / {@code packet039} {@code requestPayCaddieHolyDay}. */
    public static final int CLIENT_REEMPLOY_CADDIE = 0x39;
    /** C# {@code CLIENT_REPORT} / {@code packet03A} {@code requestPlayerReportChatGame}. */
    public static final int CLIENT_REPORT = 0x3A;
    /** C# {@code CLIENT_REPORT_ERROR} / {@code packet033} client exception. */
    public static final int CLIENT_REPORT_ERROR = 0x33;
    /** C# {@code CLIENT_MSN_REQUEST} / {@code packet03C} translate sub-packet. */
    public static final int CLIENT_MSN_REQUEST = 0x3C;
    /** C# {@code CLIENT_SHOT_COMMAND} / {@code packet042} arrow sequence. */
    public static final int CLIENT_SHOT_COMMAND = 0x42;
    /** C# {@code CLIENT_REPLAY_ONLINE} / {@code packet04A}; catch is silent. */
    public static final int CLIENT_REPLAY_ONLINE = 0x4A;
    /** C# {@code CLIENT_CHAT_PENALITY} / {@code packet04F} chat block. */
    public static final int CLIENT_CHAT_PENALITY = 0x4F;
    /** C# {@code CLIENT_NOTICE} / {@code packet057} GM notice. */
    public static final int CLIENT_NOTICE = 0x57;
    /** C# {@code CLIENT_DESTROY_ROOM} / {@code packet060} GM destroy. */
    public static final int CLIENT_DESTROY_ROOM = 0x60;
    /** C# {@code CLIENT_SPEED_RATE} / {@code packet065} time booster. */
    public static final int CLIENT_SPEED_RATE = 0x65;
    /** C# {@code CLIENT_ONELINE_REQUEST} / {@code packet066} send ticker. */
    public static final int CLIENT_ONELINE_REQUEST = 0x66;
    /** C# {@code CLIENT_ONELINE_QUERY} / {@code packet067} ticker queue. */
    public static final int CLIENT_ONELINE_QUERY = 0x67;
    /** C# {@code CLIENT_CHANGE_MASCOT} / {@code packet073} mascot message. */
    public static final int CLIENT_CHANGE_MASCOT = 0x73;
    /** C# {@code packet074} {@code requestCancelEditSaleShop}. */
    public static final int CLIENT_SHOP_CANCEL = 0x74;
    /** C# {@code packet075} {@code requestCloseSaleShop}. */
    public static final int CLIENT_SHOP_CLOSE = 0x75;
    /** C# {@code packet076} {@code requestOpenEditSaleShop}. */
    public static final int CLIENT_SHOP_OPEN_EDIT = 0x76;
    /** C# {@code packet077} {@code requestViewSaleShop}. */
    public static final int CLIENT_SHOP_VIEW = 0x77;
    /** C# {@code packet078} {@code requestCloseViewSaleShop}. */
    public static final int CLIENT_SHOP_CLOSE_VIEW = 0x78;
    /** C# {@code packet079} {@code requestChangeNameSaleShop}. */
    public static final int CLIENT_SHOP_NAME = 0x79;
    /** C# {@code packet07A} {@code requestVisitCountSaleShop}. */
    public static final int CLIENT_SHOP_VISIT = 0x7A;
    /** C# {@code packet07B} {@code requestPangSaleShop}. */
    public static final int CLIENT_SHOP_PANG = 0x7B;
    /** C# {@code packet07C} {@code requestOpenSaleShop} items. */
    public static final int CLIENT_SHOP_OPEN_ITEMS = 0x7C;
    /** C# {@code packet07D} {@code requestBuyItemSaleShop}. */
    public static final int CLIENT_SHOP_BUY = 0x7D;
    /** C# {@code packet098} {@code requestOpenPapelShop}. */
    public static final int CLIENT_PAPEL_SHOP = 0x98;
    /** C# {@code packet140} {@code requestEnterShop}. */
    public static final int CLIENT_ENTER_SHOP = 0x140;
    /** C# {@code packet143} {@code requestOpenMailBox}. */
    public static final int CLIENT_OPEN_MAILBOX = 0x143;
    /** C# {@code packet144} {@code requestInfoMail}. */
    public static final int CLIENT_OPEN_MAIL = 0x144;
    /** C# {@code packet145} {@code requestSendMail}. */
    public static final int CLIENT_SEND_MAIL = 0x145;
    /** C# {@code packet146} {@code requestTakeItemFomMail}. */
    public static final int CLIENT_TAKE_MAIL = 0x146;
    /** C# {@code packet147} {@code requestDeleteMail}. */
    public static final int CLIENT_DELETE_MAIL = 0x147;
    public static final int CLIENT_UPDATE_MACRO = 0x69;
    public static final int CLIENT_REQUEST_SERVER_LIST = 0x43;
    public static final int CLIENT_REQUEST_RANK = 0x47;
    /** C# {@code CLIENT_USER_MATCH_HISTORY} / {@code packet09C}. */
    public static final int CLIENT_USER_MATCH_HISTORY = 0x9C;
    public static final int CLIENT_CHANGE_TEAM = 0x10;
    public static final int CLIENT_LOADING_INFO = 0x48;
    public static final int CLIENT_TEAMCHAT = 0x54;
    public static final int CLIENT_ALLOW_WHISPER = 0x55;
    /** C# {@code CLIENT_REQUEST_SERVER_TIME} / {@code packet05C}. */
    public static final int CLIENT_REQUEST_SERVER_TIME = 0x5C;
    /** C# {@code CLIENT_SYNC_ACTIVITY} / {@code packet063} {@code requestPlayerLocationRoom}. */
    public static final int CLIENT_SYNC_ACTIVITY = 0x63;
    public static final int CLIENT_INVITE = 0xBA;

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

    /** C# {@code PlayerLobbyInfo.ToArray} (uid…sDisplayID 128). */
    public static final int PLAYER_LOBBY_INFO_BYTES = 200;
    /** C# {@code PlayerRoomInfo.uFlag.ready} bit. */
    public static final int PLAYER_READY_BIT = 1 << 9;
    public static final int CHAT_NORMAL = 0;
    public static final int CHAT_NOTICE = 7;
    public static final int CHAT_OFFLINE = 6;
    public static final int CHAT_GM = 0x80;
    public static final int WHISPER_FROM = 0;
    public static final int WHISPER_TO = 1;
    /** C# {@code pacote089} default err_code after the info dump. */
    public static final int PLAYER_INFO_OK = 1;
    public static final int PLAYER_INFO_NO_GM = 3;
    public static final int GUILD_INFO_BYTES = 77;
    public static final int MACRO_COUNT = 9;
    public static final int MACRO_BYTES = 64;
    public static final int PLAYER_INFO_DUMP_COUNT = 12;
    public static final int PLAYER_TEAM_BIT = 1;
    /** C# {@code PlayerRoomInfo.state_flag.away} bit 2. */
    public static final int PLAYER_AWAY_BIT = 1 << 2;
    /** C# {@code PlayerLobbyInfo.state_flag.away} bit 0 of {@code ucByte}. */
    public static final int PLAYER_LOBBY_AWAY_BIT = 1;
    /** C# {@code PlayerRoomInfo.state_flag.master}. */
    public static final int PLAYER_MASTER_BIT = 1 << 3;
    /** C# {@code RoomInfo.state_flag} when the master is GM. */
    public static final int ROOM_MASTER_GM_FLAG = 0x100;
    /** C# {@code pacote0A1} error 0 + uid + MemberInfoEx. */
    public static final int USERINFO_OFFLINE_FOUND = 0;
    /** C# {@code pacote0A1} default error when the nick is missing or invalid. */
    public static final int USERINFO_OFFLINE_MISSING = 2;
    public static final int CHANNEL_INFO_BYTES = 77;
    public static final int SERVER_INFO_BYTES = 92;
    public static final int INVITE_PLACE = 70;
    public static final int INVITE_FAIL = 23;
    public static final int LOBBY_USER_JOIN = 1;
    public static final int LOBBY_USER_LEAVE = 2;
    public static final int LOBBY_USER_UPDATE = 3;
    public static final int LOBBY_USER_CLEAR = 4;
    public static final int LOBBY_USER_LIST = 5;
    public static final int ROOM_LIST_FULL = 0;
    public static final int ROOM_LIST_ADD = 1;
    public static final int ROOM_LIST_REMOVE = 2;
    public static final int ROOM_LIST_UPDATE = 3;
    public static final int ROOM_CHANGE_NAME = 0;
    public static final int ROOM_CHANGE_PASSWORD = 1;
    public static final int ROOM_CHANGE_TIPO = 2;
    public static final int ROOM_CHANGE_COURSE = 3;
    public static final int ROOM_CHANGE_HOLES = 4;
    public static final int ROOM_CHANGE_MODO = 5;
    public static final int ROOM_CHANGE_TIME_VS = 6;
    public static final int ROOM_CHANGE_MAX_PLAYER = 7;
    public static final int ROOM_CHANGE_TIME_30S = 8;
    public static final int ROOM_CHANGE_STATE_FLAG = 9;
    public static final int ROOM_CHANGE_GALLERY = 10;
    public static final int ROOM_CHANGE_HOLE_REPEAT = 11;
    public static final int ROOM_CHANGE_FIXED_HOLE = 12;
    public static final int ROOM_CHANGE_ARTEFATO = 13;
    public static final int ROOM_CHANGE_NATURAL = 14;

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
    /** C# {@code ChangePlayerItemRoom.TYPE_CHANGE} / Channel {@code 0x0B} types. */
    public static final int ITEM_CADDIE = 1;
    public static final int ITEM_BALL = 2;
    public static final int ITEM_CLUBSET = 3;
    public static final int ITEM_CHARACTER = 4;
    public static final int ITEM_MASCOT = 5;
    public static final int ITEM_LOUNGE_EFFECT = 6;
    public static final int ITEM_ALL = 7;
    /** C# {@code TourneyBase.m_medal} length. */
    public static final int MEDAL_COUNT = 12;
    /** C# {@code Medal.ToArray}: i32 oid + u32 typeid. */
    public static final int MEDAL_BYTES = 8;
    /** C# {@code stMedal.ToArray}: 6×int32. */
    public static final int USER_MEDAL_BYTES = 24;
    /** C# {@code MapStatistics.ToArray} Debug.Assert size. */
    public static final int MAP_STATISTICS_BYTES = 43;
    /**
     * C# empty map-stat markers: 6 season/rest pairs as sbyte {@code -1}
     * (normal, natural, assist-normal, assist-natural, GP, GP-assist).
     */
    public static final int MAP_STATISTICS_EMPTY_BYTES = 12;
    /** C# Versus {@code requestUnOrPause} opt 0 resume / 1 pause. */
    public static final int PAUSE_RESUME = 0;
    public static final int PAUSE_PAUSE = 1;
    /** C# Versus max successful pauses before the request is rejected. */
    public static final int VERSUS_PAUSE_MAX = 3;
    /** C# {@code TPLAYER_ACTION} lounge / room motion types. */
    public static final int ACTION_ROTATION = 0;
    public static final int ACTION_MOTION_ROOM = 1;
    public static final int ACTION_LOUNGER_LOC = 4;
    public static final int ACTION_LOUNGER_STATE = 5;
    public static final int ACTION_MOVE = 6;
    public static final int ACTION_MOTION_LOUNGER = 7;
    public static final int ACTION_ACK_PLAYER = 8;
    /** C# unknown type throws; Java ignores it. */
    public static final int ACTION_UNK_NULL = 9;
    public static final int ACTION_ANIMATION_WITH_EFFECTS = 10;
    /** C# {@code PlayerRoomInfo.stLocation.ToArray}: 3 floats. */
    public static final int LOCATION_BYTES = 12;
    /** C# Versus continue {@code 0x36}: 0 stop / 1 go. */
    public static final int CONTINUE_STOP = 0;
    public static final int CONTINUE_GO = 1;
    /** C# report {@code 0x94}: first report. */
    public static final int REPORT_OK = 0;
    /** C# report {@code 0x94}: already reported. */
    public static final int REPORT_ALREADY = 1;
    /** C# caddie holiday {@code 0x93} catch. */
    public static final int CADDIE_HOLIDAY_FAIL = 1;
    /** C# caddie holiday {@code 0x93} success (needs IFF). */
    public static final int CADDIE_HOLIDAY_OK = 2;
    /** C# ticker cookie cost. */
    public static final int TICKER_COOKIE = 1;
    /** C# ticker wait per queued message. */
    public static final int TICKER_WAIT_MS = 30000;
    /** C# ticker / nick-ack fail generic. */
    public static final int TICKER_FAIL_GENERIC = 1;
    /** C# ticker fail: not enough cookies. */
    public static final int TICKER_FAIL_FUNDS = 4;
    /** C# {@code capability.game_master} bit. */
    public static final int CAPABILITY_GM = 4;
    /** C# {@code TranslationSubPacket.Msg_OFF}. */
    public static final int MSN_MSG_OFF = 0x111;
    /** C# {@code TranslationSubPacket.Friend_List} (not implemented in C#). */
    public static final int MSN_FRIEND_LIST = 0x11F;
    /** C# Msg_OFF pang cost. */
    public static final int MSN_OFF_PANG = 10;
    public static final int MSN_OK = 0;
    /**
     * C# catch {@code 0x95} writes {@code STDA_SYSTEM_ERROR_DECODE_TYPE}
     * ({@code err & 0xFFFF}). {@code MAKE_ERROR_TYPE(..., 0x5700101)} stores
     * {@code 0x0101}.
     */
    public static final int MSN_ERR_DEFAULT = 0x5700100;
    public static final int MSN_ERR_UID = 0x0101;
    public static final int MSN_ERR_EMPTY = 0x0102;
    public static final int MSN_ERR_SIZE = 0x0103;
    public static final int MSN_ERR_OPT = 0x0104;
    public static final int MSN_ERR_FUNDS = 0x0105;
    public static final int SHOP_OK = 1;
    /** C# {@code STDA_SYSTEM_ERROR_ENCODE}: {@code sys & 0xFFFF}. */
    public static int shopSys(int sys) {
        return sys & 0xFFFF;
    }
    public static final int SHOP_ERR_EDIT_DEFAULT = 5200100;
    public static final int SHOP_ERR_CANCEL_DEFAULT = 5200400;
    public static final int SHOP_ERR_CANCEL_NONE = 5200401;
    public static final int SHOP_ERR_CLOSE_DEFAULT = 5200150;
    public static final int SHOP_ERR_CLOSE_NONE = 5200151;
    public static final int SHOP_ERR_NAME_DEFAULT = 5200200;
    public static final int SHOP_ERR_NAME_EMPTY = 5200201;
    public static final int SHOP_ERR_NAME_DUP = 5200202;
    public static final int SHOP_ERR_NAME_NONE = 5200203;
    public static final int SHOP_ERR_VISIT_DEFAULT = 5200300;
    public static final int SHOP_ERR_VISIT_NONE = 5200301;
    public static final int SHOP_ERR_PANG_DEFAULT = 5200350;
    public static final int SHOP_ERR_PANG_NONE = 5200351;
    public static final int SHOP_ERR_VIEW_DEFAULT = 5200450;
    public static final int SHOP_ERR_VIEW_NONE = 5200452;
    public static final int SHOP_ERR_CLOSE_VIEW_DEFAULT = 5200500;
    public static final int SHOP_ERR_CLOSE_VIEW_NONE = 5200502;
    public static final int SHOP_ERR_OPEN_DEFAULT = 5200250;
    public static final int SHOP_ERR_OPEN_COUNT = 5200251;
    public static final int SHOP_ERR_OPEN_NONE = 5200252;
    public static final int SHOP_ERR_BUY_DEFAULT = 5200550;
    public static final int SHOP_ERR_BUY_NONE = 5200552;
    /** C# {@code NUM_OF_EMAIL_PER_PAGE}. */
    public static final int MAIL_PER_PAGE = 20;
    /** C# {@code LIMIT_OF_UNREAD_EMAIL}. */
    public static final int MAIL_UNREAD_LIMIT = 300;
    /** C# {@code MailBox.ToArray} {@code WriteStr(from_id, 30)}. */
    public static final int MAIL_FROM_BYTES = 30;
    /** C# {@code MailBox.ToArray} {@code WriteStr(msg, 80)}. */
    public static final int MAIL_MSG_PREVIEW_BYTES = 80;
    /** C# {@code MailBox.unknown2[18]}. */
    public static final int MAIL_UNKNOWN2_BYTES = 18;
    /** C# {@code EmailInfo.item.ToArray} / empty-item pad. */
    public static final int MAIL_ITEM_BYTES = 55;
    /**
     * C# {@code MailBox.ToArray}: id 4 + from 30 + msg 80 + unk 18 + visit 4 +
     * lida 1 + item_num 4 + item 55.
     */
    public static final int MAIL_BOX_ENTRY_BYTES = 4 + MAIL_FROM_BYTES + MAIL_MSG_PREVIEW_BYTES
            + MAIL_UNKNOWN2_BYTES + 4 + 1 + 4 + MAIL_ITEM_BYTES;
    /** C# text-only send {@code pang_price} must be 100. */
    public static final int MAIL_SEND_PANG = 100;
    /** C# with-items send {@code pang_price} is {@code count * 500}. */
    public static final int MAIL_SEND_ITEM_PANG = 500;
    /** C# max attached items. */
    public static final int MAIL_SEND_ITEM_MAX = 4;
    /**
     * C# {@code MAKE_ERROR_TYPE(CHANNEL, 6, 0x790002)} / delete {@code 0x791002}:
     * catch writes {@code sys & 0xFFFF} = 2.
     */
    public static final int MAIL_ERR_PAGE = 2;
    /** C# missing-mail / empty nick-msg: CHANNEL sys 1. */
    public static final int MAIL_ERR_CHANNEL = 1;
    /** C# take-item when the mail has no attachments: {@code pacote214(1)}. */
    public static final int MAIL_ERR_TAKE_EMPTY = 1;
    /** C# open-mailbox catch else {@code 0x5500200}. */
    public static final int MAIL_ERR_OPEN_DEFAULT = 0x5500200;
    /** C# open-mail catch else {@code 0x5500250}. */
    public static final int MAIL_ERR_INFO_DEFAULT = 0x5500250;
    /** C# send-mail catch else (PACKET_FUNC_SV / MAIL_BOX_MANAGER). */
    public static final int MAIL_ERR_SEND_DEFAULT = 0x5500300;
    /** C# take-item catch else. */
    public static final int MAIL_ERR_TAKE_DEFAULT = 0x5500100;
    /** C# delete-mail catch else. */
    public static final int MAIL_ERR_DELETE_DEFAULT = 0x5500150;
    /** C# {@code EmailInfo.ToArray} when {@code from_id} is empty. */
    public static final String MAIL_FROM_ADM = "@ADM";
    /** C# {@code Last5PlayersGame.LastPlayerGame.ToArray}: sex 4 + nick 22 + id 22 + uid 4. */
    public static final int LAST5_PLAYER_BYTES = 52;
    /** C# always writes 5 {@code LastPlayerGame} rows. */
    public static final int LAST5_COUNT = 5;
    /** C# view {@code WriteString(nick, 22)}. */
    public static final int SHOP_NICK_BYTES = 22;
    /** C# {@code requestBuyItemShop} {@code 0x68} option codes. */
    public static final int BUY_FAIL_INIT = 1;
    public static final int BUY_FAIL_PRICE = 2;
    public static final int BUY_FAIL_OWNED = 4;
    public static final int BUY_FAIL_NOT_BUYABLE = 6;
    public static final int BUY_FAIL_FUNDS = 7;
    public static final int BUY_FAIL_EMPTY = 9;
    /** C# catch: {@code 0x68} uint32 10. */
    public static final int BUY_FAIL_GENERIC = 10;
    /** C# {@code BuyItem} body after opcode: id+typeid+time+type+qntd+pang+cookie+13. */
    public static final int BUY_ITEM_BYTES = 37;
    /** C# {@code SYSTEMTIME} (8 × uint16). */
    public static final int SYSTEMTIME_BYTES = 16;
    /** C# {@code ucc.IDX} in {@code pacote0AA}. */
    public static final int UCC_IDX_BYTES = 9;
    /** C# {@code StateCharacterLounge.ToArray}: 4 floats. */
    public static final int STATE_CHARACTER_LOUNGE_BYTES = 16;

    /** C# {@code AIR_KNIGHT_SET} / IFF CLUBSET << 26. */
    public static final int TYPEID_AIR_KNIGHT = 0x10000000;
    /** C# {@code CHARACTER << 26} Nuri. */
    public static final int TYPEID_NURI = 0x4000000;
    /** IFF BALL << 26. */
    public static final int TYPEID_DEFAULT_BALL = 0x14000000;
    /**
     * IFF ITEM {@code 0x1A000006} (436207622). SQL shop catalog stand-in for C# {@code IsBuyItem}.
     */
    public static final int TYPEID_SHOP_PANG_ITEM = 0x1A000006;
    /** Seeded {@code shop_catalog.pang_price} for {@link #TYPEID_SHOP_PANG_ITEM}. */
    public static final int SHOP_PANG_PRICE = 100;
    /** C# {@code IFF_GROUP.CHARACTER}: {@code typeid >>> 26}. */
    public static final int IFF_GROUP_CHARACTER = 1;

    /**
     * C# {@code LoginManager} case 32 {@code pacote210} before
     * {@code sendCompleteData}.
     */
    public static final int LOGIN_NEW_MAIL_COUNT = 1;
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
    public static final int LOGIN_DUMP_PACKET_COUNT =
            LOGIN_NEW_MAIL_COUNT + LOGIN_DUMP_PREFIX_COUNT + LOGIN_DUMP_TAIL_COUNT;

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

    /**
     * C# {@code PlayerLobbyInfo} — 200 bytes. {@code Channel.makePlayerInfo} /
     * {@code makePlayerLobbyInfo}.
     */
    public static final class PlayerLobbyInfo {
        public int uid;
        public int oid;
        public int salaNumero = 0xFFFF;
        public String nick = "";
        public int level;
        public int capability;
        public int title;
        public int teamPoint;
        public int state;
        public int guildUid;
        public int guildMarkIndex;
        public String guildMarkName = "";
        public int flagVisibleGm;
        public int uidChanneling;
        public String nickDisplay = "";

        public byte[] toArray() {
            PacketWriter w = new PacketWriter();
            w.u32(uid);
            w.i32(oid);
            w.u16(salaNumero);
            w.fixedStr(nick, 22);
            w.u8(level);
            w.i32(flagVisibleGm == 0 ? 0 : capability);
            w.u32(title);
            w.u32(teamPoint);
            w.u8(state);
            w.i32(guildUid);
            w.u32(guildMarkIndex);
            w.fixedStr(guildMarkName, 12);
            w.i16(flagVisibleGm);
            w.u32(uidChanneling);
            w.fixedStr(nickDisplay, 128);
            byte[] body = w.toBytes();
            if (body.length != PLAYER_LOBBY_INFO_BYTES) {
                throw new IllegalStateException("PlayerLobbyInfo size " + body.length);
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
        return memberInfoExPublic(oid, id, nick, capability, 0xffff);
    }

    public static byte[] memberInfoExPublic(int oid, String id, String nick, int capability, int salaNumero) {
        PacketWriter w = new PacketWriter();
        w.u16(salaNumero);
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
        } else if (kind == 2) {
            int oid = players.isEmpty() ? 0 : players.getFirst().oid;
            return w.i32(oid).toBytes();
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

    /**
     * C# {@code pacote04B}: i32 error; on 0, type + oid + type-specific extra.
     */
    public static byte[] roomUserInfoChanged(int error, int type, int oid, byte[] extra) {
        PacketWriter w = new PacketWriter().opcode(SERVER_ROOM_USER_INFO_CHANGED).i32(error);
        if (error == 0) {
            w.u8(type);
            w.i32(oid);
            if (extra != null) {
                w.bytes(extra);
            }
        }
        return w.toBytes();
    }

    /**
     * C# {@code sendUpdateInfoAndMapStatistics} with empty map stats:
     * UserInfo 265 + TrofelInfo 78 + 12× sbyte {@code -1}.
     */
    public static byte[] myStatistics(byte[] userInfo) {
        PacketWriter w = new PacketWriter().opcode(SERVER_MY_STATISTICS);
        w.bytes(userInfo != null && userInfo.length == USER_INFO_BYTES
                ? userInfo
                : userInfoPublic(0));
        w.zero(TROPHY_BYTES);
        for (int i = 0; i < MAP_STATISTICS_EMPTY_BYTES; i++) {
            w.u8(0xff);
        }
        return w.toBytes();
    }

    /** C# {@code sendDropItem}: u8 0 + u16 count + typeids. */
    public static byte[] prizeList(int[] typeids) {
        int count = typeids == null ? 0 : typeids.length;
        PacketWriter w = new PacketWriter().opcode(SERVER_PRIZE_LIST).u8(0).u16(count);
        if (typeids != null) {
            for (int typeid : typeids) {
                w.u32(typeid);
            }
        }
        return w.toBytes();
    }

    /**
     * C# {@code sendPlacar}: exp + room trophy + player trophy + team + 12 medals +
     * {@code UserInfo.medal}.
     */
    public static byte[] gameResult(int exp, int roomTrophy, int playerTrophy, int team) {
        PacketWriter w = new PacketWriter().opcode(SERVER_GAME_RESULT);
        w.i32(exp);
        w.u32(roomTrophy);
        w.u8(playerTrophy);
        w.u8(team);
        for (int i = 0; i < MEDAL_COUNT; i++) {
            w.i32(-1);
            w.u32(0);
        }
        w.zero(USER_MEDAL_BYTES);
        return w.toBytes();
    }

    /** C# {@code requestSendTreasureHunterItem} with an empty drop list. */
    public static byte[] treasureHunterItem() {
        return new PacketWriter().opcode(SERVER_UPDATE_TREASURE_GIFT_LIST).u8(0).toBytes();
    }

    /** C# Versus {@code 0x8B}: i32 oid + u8 opt (0 resume / 1 pause). */
    public static byte[] pause(int oid, int opt) {
        return new PacketWriter().opcode(SERVER_PAUSE).i32(oid).u8(opt).toBytes();
    }

    /** C# {@code pacote08E}: i32 oid + u8 state. */
    public static byte[] sleep(int oid, int state) {
        return new PacketWriter().opcode(SERVER_SLEEP).i32(oid).u8(state).toBytes();
    }

    /** C# empty Versus {@code 0x90} after every player sends {@code 0x34}. */
    public static byte[] teeshotReady() {
        return new PacketWriter().opcode(SERVER_TEESHOT_READY_ACK).toBytes();
    }

    /** C# {@code 0x94}: u8 0 first / 1 already. */
    public static byte[] reportAck(int option) {
        return new PacketWriter().opcode(SERVER_REPORT).u8(option).toBytes();
    }

    /** C# holiday fail {@code 0x93} u8 1. Success is u8 2 + id + pang. */
    public static byte[] caddieHolidayFail() {
        return new PacketWriter().opcode(SERVER_REEMPLOY_CADDIE_ACK).u8(CADDIE_HOLIDAY_FAIL).toBytes();
    }

    /** C# {@code 0xAC}: oid + u8 chat-block. */
    public static byte[] chatPenalty(int oid, int block) {
        return new PacketWriter().opcode(SERVER_CHAT_PENALITY).i32(oid).u8(block).toBytes();
    }

    /** C# {@code 0xC7}: f32 speed + oid. */
    public static byte[] speedRate(float speed, int oid) {
        return new PacketWriter().opcode(SERVER_SPEED_RATE).f32(speed).i32(oid).toBytes();
    }

    /** C# {@code 0xCA}: queued ticker count + wait ms. */
    public static byte[] tickerQueue(int count, int waitMs) {
        return new PacketWriter().opcode(SERVER_ONELINE_QUERY).u16(count).u32(waitMs).toBytes();
    }

    /** C# {@code 0xC9}: nick + message. */
    public static byte[] tickerMsg(String nick, String msg) {
        return new PacketWriter()
                .opcode(SERVER_ONELINE_MSG)
                .pstr(nick == null ? "" : nick)
                .pstr(msg == null ? "" : msg)
                .toBytes();
    }

    /** C# ticker fail via nick-ack {@code 0x50} u32. */
    public static byte[] tickerFail(int code) {
        return new PacketWriter().opcode(SERVER_CHANGE_NICK_ACK).u32(code).toBytes();
    }

    /**
     * C# mascot-message catch {@code 0xE2}: sbyte -1, i32 -1, u16 0, u64 pang.
     */
    public static byte[] mascotMessageFail(long pang) {
        return new PacketWriter()
                .opcode(SERVER_CHANGE_MASCOT)
                .u8(0xff)
                .i32(-1)
                .u16(0)
                .u64(pang)
                .toBytes();
    }

    /** C# Msg_OFF success {@code 0x95}: sub + u32 0 + remaining pang. */
    public static byte[] msnAckOk(int sub, long pang) {
        return new PacketWriter().opcode(SERVER_MSN_ACK).u16(sub).u32(MSN_OK).u64(pang).toBytes();
    }

    /** C# {@code 0x95} catch: sub + u32 error, no pang. */
    public static byte[] msnAckFail(int sub, int code) {
        return new PacketWriter().opcode(SERVER_MSN_ACK).u16(sub).u32(code).toBytes();
    }

    public static byte[] shopEditOk(String nick, int uid) {
        return new PacketWriter()
                .opcode(SERVER_SHOP_EDIT)
                .u32(SHOP_OK)
                .pstr(nick == null ? "" : nick)
                .u32(uid)
                .toBytes();
    }

    public static byte[] shopEditFail(int code) {
        return new PacketWriter().opcode(SERVER_SHOP_EDIT).u32(code).toBytes();
    }

    public static byte[] shopCancelOk(String nick) {
        return new PacketWriter()
                .opcode(SERVER_SHOP_CANCEL)
                .u32(SHOP_OK)
                .pstr(nick == null ? "" : nick)
                .toBytes();
    }

    public static byte[] shopCancelFail(int code) {
        return new PacketWriter().opcode(SERVER_SHOP_CANCEL).u32(code).toBytes();
    }

    public static byte[] shopCloseOk(String nick, int uid) {
        return new PacketWriter()
                .opcode(SERVER_SHOP_CLOSE)
                .u32(SHOP_OK)
                .pstr(nick == null ? "" : nick)
                .u32(uid)
                .toBytes();
    }

    public static byte[] shopNameOk(String name, int uid, String nick) {
        return new PacketWriter()
                .opcode(SERVER_SHOP_NAME)
                .u32(SHOP_OK)
                .pstr(name == null ? "" : name)
                .u32(uid)
                .pstr(nick == null ? "" : nick)
                .toBytes();
    }

    public static byte[] shopNameFail(int code) {
        return new PacketWriter().opcode(SERVER_SHOP_NAME).u32(code).toBytes();
    }

    public static byte[] shopVisitOk(int count) {
        return new PacketWriter().opcode(SERVER_SHOP_VISIT).u32(SHOP_OK).u32(count).toBytes();
    }

    public static byte[] shopVisitFail(int code) {
        return new PacketWriter().opcode(SERVER_SHOP_VISIT).u32(code).toBytes();
    }

    public static byte[] shopPangOk(long pang) {
        return new PacketWriter().opcode(SERVER_SHOP_PANG).u32(SHOP_OK).u64(pang).toBytes();
    }

    public static byte[] shopPangFail(int code) {
        return new PacketWriter().opcode(SERVER_SHOP_PANG).u32(code).toBytes();
    }

    public static byte[] shopViewFail(int code) {
        return new PacketWriter().opcode(SERVER_SHOP_VIEW).u32(code).toBytes();
    }

    public static byte[] shopCloseViewOk() {
        return new PacketWriter().opcode(SERVER_SHOP_CLOSE_VIEW).u32(SHOP_OK).toBytes();
    }

    public static byte[] shopCloseViewFail(int code) {
        return new PacketWriter().opcode(SERVER_SHOP_CLOSE_VIEW).u32(code).toBytes();
    }

    public static byte[] shopItemsFail(int code) {
        return new PacketWriter().opcode(SERVER_SHOP_ITEMS).u32(code).toBytes();
    }

    public static byte[] shopBuyFail(int code) {
        return new PacketWriter().opcode(SERVER_SHOP_BUY).u32(code).toBytes();
    }

    /** C# {@code 0x10B}: u32 0 + i64 daily limit. */
    public static byte[] papelShopOk(long limit) {
        return new PacketWriter().opcode(SERVER_PAPEL_SHOP).u32(0).i64(limit).toBytes();
    }

    /** C# {@code 0x20E}: two int32 zeros. */
    public static byte[] enterShopOk() {
        return new PacketWriter().opcode(SERVER_ENTER_SHOP).u32(0).u32(0).toBytes();
    }

    /** C# {@code MailBox.ToArray} without items: 196 bytes. */
    public static byte[] mailBoxEntry(int id, String fromId, String msg, int visitCount, int lidaYn, int itemNum) {
        return new PacketWriter()
                .i32(id)
                .fixedStr(fromId, MAIL_FROM_BYTES)
                .fixedStr(msg, MAIL_MSG_PREVIEW_BYTES)
                .zero(MAIL_UNKNOWN2_BYTES)
                .u32(visitCount)
                .u8(lidaYn)
                .u32(itemNum)
                .zero(MAIL_ITEM_BYTES)
                .toBytes();
    }

    /** C# {@code pacote210}: i32 option + i32 count + {@code MailBox} rows. */
    public static byte[] newMail(List<byte[]> entries) {
        PacketWriter w = new PacketWriter().opcode(SERVER_NEW_MAIL).i32(0).i32(entries.size());
        for (byte[] entry : entries) {
            w.bytes(entry);
        }
        return w.toBytes();
    }

    /**
     * C# {@code pacote211}/{@code pacote215}: error, then page + pages + count + rows
     * when error is 0.
     */
    public static byte[] mailBoxPage(int opcode, int error, int page, int pages, List<byte[]> entries) {
        PacketWriter w = new PacketWriter().opcode(opcode).i32(error);
        if (error == 0) {
            w.i32(page).i32(pages).i32(entries.size());
            for (byte[] entry : entries) {
                w.bytes(entry);
            }
        }
        return w.toBytes();
    }

    public static byte[] mailFail(int opcode, int error) {
        return new PacketWriter().opcode(opcode).u32(error).toBytes();
    }

    /**
     * C# {@code pacote212} + {@code EmailInfo.ToArray} with no attachments
     * (count 0 still writes a 55-byte zero item).
     */
    public static byte[] mailInfoOk(int id, String fromId, String date, String msg, int lidaYn) {
        PacketWriter w = new PacketWriter().opcode(SERVER_MAIL_INFO).u32(0);
        w.i32(id);
        w.pstr(fromId == null || fromId.isEmpty() ? MAIL_FROM_ADM : fromId);
        w.pstr(date == null ? "" : date);
        w.pstr(msg == null ? "" : msg);
        w.u8(lidaYn);
        w.i32(0);
        w.zero(MAIL_ITEM_BYTES);
        return w.toBytes();
    }

    /** C# {@code PlayerRoomInfo.stLocation.ToArray}: x z r. */
    public static byte[] location(float x, float z, float r) {
        return new PacketWriter().f32(x).f32(z).f32(r).toBytes();
    }

    /**
     * C# {@code pacote0C4}: oid + {@code TPLAYER_ACTION} + type payload.
     */
    public static byte[] syncActivity(int oid, int type, byte[] payload) {
        return new PacketWriter()
                .opcode(SERVER_SYNC_ACTIVITY)
                .i32(oid)
                .u8(type)
                .bytes(payload)
                .toBytes();
    }

    /**
     * C# {@code updateFinishHole} {@code 0x6D}: oid + hole + shots + score + pang +
     * bonus + option (1 finished / 0 not).
     */
    public static byte[] updateHole(int oid, int hole, int shots, int score, long pang, long bonus, int option) {
        return new PacketWriter()
                .opcode(SERVER_UPDATE_HOLE)
                .i32(oid)
                .u8(hole)
                .u8(shots)
                .i32(score)
                .u64(pang)
                .u64(bonus)
                .u8(option)
                .toBytes();
    }

    /** C# {@code sendUpdateState} {@code 0x6C}: oid + u8 (2 finished / 3 left). */
    public static byte[] gamePlayerState(int oid, int option) {
        return new PacketWriter().opcode(SERVER_GAME_PLAYER_STATE).i32(oid).u8(option).toBytes();
    }

    /** C# empty {@code 0x199} after the last Tourney hole. */
    public static byte[] lastHole() {
        return new PacketWriter().opcode(SERVER_LAST_HOLE).toBytes();
    }

    /** C# {@code requestBuyItemShop} {@code 0x68} uint32 option. Extra pang/cookie only on option 0. */
    public static byte[] buyFailed(int code) {
        return new PacketWriter().opcode(SERVER_BUY_ACK).u32(code).toBytes();
    }

    /** C# {@code 0x68} option 0 + remaining pang + cookie. */
    public static byte[] buyOk(long pang, long cookie) {
        return new PacketWriter().opcode(SERVER_BUY_ACK).u32(0).u64(pang).u64(cookie).toBytes();
    }

    /**
     * C# gift {@code 0x6A}: u32 code + remaining pang + cookie (always present).
     */
    public static byte[] giftFailed(int code, long pang, long cookie) {
        return new PacketWriter()
                .opcode(SERVER_RESPONSE_GIFT_ITEM)
                .u32(code)
                .u64(pang)
                .u64(cookie)
                .toBytes();
    }

    /**
     * C# {@code pacote0AA}: count + per item typeid/id/time/flag_time/qntd_dep/SYSTEMTIME/ucc.IDX
     * then u64 pang + u64 cookie. Non-rental SYSTEMTIME is zeros.
     */
    public static byte[] buyNewItems(List<BoughtItem> items, long pang, long cookie) {
        PacketWriter w = new PacketWriter().opcode(SERVER_NEW_ITEM).u16(items.size());
        for (BoughtItem item : items) {
            w.u32(item.typeid());
            w.i32(item.id());
            w.u16(item.time());
            w.u8(item.flagTime());
            w.u16(item.qntdDep());
            w.zero(SYSTEMTIME_BYTES);
            w.zero(UCC_IDX_BYTES);
        }
        return w.u64(pang).u64(cookie).toBytes();
    }

    /** C# {@code 0xC8} after a pang shop purchase. */
    public static byte[] pangSpent(long remaining, long spent) {
        return new PacketWriter().opcode(SERVER_PANG_SPENT).u64(remaining).u64(spent).toBytes();
    }

    /** C# {@code 0x96} cookie balance after a cash shop purchase. */
    public static byte[] cookieBalance(long cookie) {
        return new PacketWriter().opcode(SERVER_COOKIE).u64(cookie).toBytes();
    }

    /**
     * C# {@code packet_func.pacote046} — lobby player list / join / leave / clear.
     */
    public static byte[] lobbyUsers(int option, List<PlayerLobbyInfo> players) {
        PacketWriter w = new PacketWriter().opcode(SERVER_USERLIST);
        w.u8(option);
        w.u8(players.size());
        for (PlayerLobbyInfo p : players) {
            w.bytes(p.toArray());
        }
        return w.toBytes();
    }

    /**
     * C# {@code packet_func.pacote047} — lobby room list.
     * Count is {@code option == 0 ? rooms.size() : 1} (C# {@code PacketMaker::makeRoomList}).
     */
    public static byte[] roomList(int option, List<byte[]> rooms) {
        PacketWriter w = new PacketWriter().opcode(SERVER_ROOMLIST);
        int count = option == ROOM_LIST_FULL ? rooms.size() : 1;
        w.u8(count);
        w.u8(option);
        w.i16(-1);
        for (byte[] room : rooms) {
            w.bytes(room);
        }
        return w.toBytes();
    }

    /**
     * C# {@code packet_func.pacote040} — chat.
     */
    public static byte[] chat(int option, String nick, String msg) {
        return new PacketWriter()
                .opcode(SERVER_CHAT)
                .u8(option)
                .pstr(nick == null ? "" : nick)
                .pstr(msg == null ? "" : msg)
                .toBytes();
    }

    /**
     * C# {@code packet_func.pacote078} — ready state for one player.
     */
    public static byte[] readyState(int oid, int ready) {
        return new PacketWriter().opcode(SERVER_READY).i32(oid).u8(ready).toBytes();
    }

    /**
     * C# {@code 0x84}: byte 0 = FROM (ack to sender), byte 1 = TO (deliver to target).
     */
    public static byte[] whisper(int direction, String nick, String msg) {
        return new PacketWriter()
                .opcode(SERVER_WHISPER)
                .u8(direction)
                .pstr(nick == null ? "" : nick)
                .pstr(msg == null ? "" : msg)
                .toBytes();
    }

    /** C# {@code pacote040} option {@code CHAT_OFFLINE} — nick only. */
    public static byte[] chatOffline(String nick) {
        return new PacketWriter().opcode(SERVER_CHAT).u8(CHAT_OFFLINE).pstr(nick == null ? "" : nick).toBytes();
    }

    /** C# {@code packet_func.pacote0F5} — empty. */
    public static byte[] enterLobbyAck() {
        return new PacketWriter().opcode(SERVER_ENTER_LOBBY).toBytes();
    }

    /** C# {@code packet_func.pacote0F6} — empty. */
    public static byte[] leaveLobbyAck() {
        return new PacketWriter().opcode(SERVER_LEAVE_LOBBY).toBytes();
    }

    /** C# {@code RoomManager.getRoomsInfo}: Practice / GZ Practice stay off the lobby list. */
    public static boolean hiddenFromLobby(int tipo) {
        return tipo == TIPO_PRACTICE || tipo == TIPO_GRAND_ZODIAC_PRACTICE;
    }

    /** C# {@code pacote089}: uint32 err, then season+uid when err > 0. */
    public static byte[] playerInfoAck(int err, int season, int uid) {
        PacketWriter w = new PacketWriter().opcode(SERVER_PLAYER_INFO).u32(err);
        if (err > 0) {
            w.u8(season).u32(uid);
        }
        return w.toBytes();
    }

    /**
     * C# {@code requestPlayerInfo} online dump before {@code pacote089}.
     * Map statistics stay empty when best_score is the C# unused sentinel 127.
     */
    public static List<byte[]> playerInfoDump(
            int uid,
            int season,
            int oid,
            int salaNumero,
            String id,
            String nick,
            int capability,
            int level,
            CharacterInfo character,
            UserEquip equip) {
        byte[] mi = memberInfoExPublic(oid, id, nick, capability, salaNumero);
        byte[] ci = character == null ? new byte[CHARACTER_INFO_BYTES] : character.toArray();
        byte[] ue = equip == null ? new byte[USER_EQUIP_BYTES] : equip.toArray();
        int natural = season != 0 ? 0x33 : 0x0A;
        int gp = season != 0 ? 0x34 : 0x0B;
        List<byte[]> out = new ArrayList<>();
        out.add(new PacketWriter()
                .opcode(0x157)
                .u8(season)
                .u32(uid)
                .bytes(mi)
                .u32(uid)
                .u32(0)
                .toBytes());
        out.add(new PacketWriter().opcode(0x15E).u32(uid).bytes(ci).toBytes());
        out.add(new PacketWriter().opcode(0x156).u8(season).u32(uid).bytes(ue).toBytes());
        out.add(new PacketWriter().opcode(0x158).u8(season).u32(uid).bytes(userInfo(level)).toBytes());
        out.add(new PacketWriter().opcode(0x15D).u32(uid).zero(GUILD_INFO_BYTES).toBytes());
        out.add(mapStats(uid, natural));
        out.add(mapStats(uid, gp));
        PacketWriter unknown = new PacketWriter().opcode(0x15B).u8(season).u32(uid).i16(1);
        for (int i = 0; i < 60; i++) {
            unknown.i32(i);
        }
        out.add(unknown.toBytes());
        out.add(new PacketWriter().opcode(0x15A).u8(season).u32(uid).u16(0).toBytes());
        out.add(new PacketWriter().opcode(0x159).u8(season).u32(uid).zero(TROPHY_BYTES).toBytes());
        out.add(mapStats(uid, season));
        out.add(new PacketWriter().opcode(0x257).u8(season).u32(uid).i16(0).toBytes());
        if (out.size() != PLAYER_INFO_DUMP_COUNT) {
            throw new IllegalStateException("player info dump count " + out.size());
        }
        return out;
    }

    private static byte[] mapStats(int uid, int season) {
        return new PacketWriter().opcode(0x15C).u8(season).u32(uid).i32(0).i32(0).toBytes();
    }

    /**
     * C# {@code pacote09F}: server count + {@code ServerInfo} rows + channel count +
     * {@code ChannelInfo} (no nested {@code 0x4D} opcode).
     */
    public static byte[] serverAndChannelList(List<byte[]> servers, List<ChannelInfo> channels) {
        PacketWriter w = new PacketWriter().opcode(SERVER_SERVER_LIST).u8(servers.size());
        for (byte[] server : servers) {
            w.bytes(server);
        }
        w.u8(channels.size());
        for (ChannelInfo channel : channels) {
            w.bytes(channel.toArray());
        }
        return w.toBytes();
    }

    public static byte[] rankAddress(String ip, int port) {
        return new PacketWriter().opcode(SERVER_RANK_ADDRESS).pstr(ip == null ? "" : ip).i32(port).toBytes();
    }

    /**
     * C# {@code pacote10E}: 5× {@code LastPlayerGame.ToArray} (seed zeros).
     */
    public static byte[] last5Players() {
        return new PacketWriter().opcode(SERVER_LAST5).zero(LAST5_COUNT * LAST5_PLAYER_BYTES).toBytes();
    }

    public static byte[] teamState(int oid, int team) {
        return new PacketWriter().opcode(SERVER_TEAM).i32(oid).u8(team).toBytes();
    }

    /** C# {@code 0x7C}: i32 oid + i16 0 after {@code updateMaster}. */
    public static byte[] decisionRoomMaster(int oid, int option) {
        return new PacketWriter().opcode(SERVER_DECISION_ROOM_MASTER).i32(oid).i16(option).toBytes();
    }

    public static byte[] userInfoOfflineMissing() {
        return new PacketWriter().opcode(SERVER_USERINFO_OFFLINE).u8(USERINFO_OFFLINE_MISSING).toBytes();
    }

    public static byte[] userInfoOffline(int uid, byte[] memberInfoEx) {
        return new PacketWriter()
                .opcode(SERVER_USERINFO_OFFLINE)
                .u8(USERINFO_OFFLINE_FOUND)
                .u32(uid)
                .bytes(memberInfoEx)
                .toBytes();
    }

    public static byte[] clientUserInfoOffline(int opt, String nick) {
        return new PacketWriter()
                .opcode(CLIENT_REQUEST_USERINFO_OFFLINE)
                .u8(opt)
                .pstr(nick == null ? "" : nick)
                .toBytes();
    }

    public static byte[] clientBanish(int uid) {
        return new PacketWriter().opcode(CLIENT_REQUEST_BANISH).u32(uid).toBytes();
    }

    public static byte[] clientRequestServerTime() {
        return new PacketWriter().opcode(CLIENT_REQUEST_SERVER_TIME).toBytes();
    }

    public static byte[] serverTime() {
        return new PacketWriter().opcode(SERVER_RESPONSE_SERVER_TIME).systemTimeNow().toBytes();
    }

    /**
     * C# {@code requestShowInfoRoom} / {@code pacote086}: room summary then per-player
     * oid/level/place/capability/title/ladder.
     */
    public static byte[] roomDetail(RoomInfo info, int tipo, List<RoomDetailPlayer> players) {
        int time;
        if (tipo == TIPO_STROKE || tipo == TIPO_MATCH || tipo == TIPO_PANG_BATTLE) {
            time = info.timeVs;
        } else if (tipo == TIPO_GUILD_BATTLE) {
            time = 0;
        } else {
            time = info.time30s;
        }
        PacketWriter w = new PacketWriter()
                .opcode(SERVER_ROOM_DETAIL)
                .u32(info.numPlayer)
                .u8(info.holes)
                .u32(time)
                .u8(info.course)
                .u8(tipo)
                .u8(info.modo)
                .u32(info.trophy);
        for (RoomDetailPlayer player : players) {
            w.i32(player.oid())
                    .u8(player.level())
                    .u8(player.place())
                    .i32(player.capability())
                    .u32(player.title())
                    .u32(player.ladderPoint());
        }
        return w.toBytes();
    }

    public record RoomDetailPlayer(int oid, int level, int place, int capability, int title, int ladderPoint) {}

    /** C# {@code pacote04C}: int16 option ({@code -1} after a successful leave). */
    public static byte[] exitRoomAck(int option) {
        return new PacketWriter().opcode(SERVER_EXIT_ROOM).i16(option).toBytes();
    }

    /**
     * C# {@code pacote196}: oid + {@code StateCharacterLounge} defaults (all 1.0f).
     */
    public static byte[] loungeState(int oid) {
        return new PacketWriter()
                .opcode(SERVER_LOUNGE_STATE)
                .i32(oid)
                .f32(1)
                .f32(1)
                .f32(1)
                .f32(1)
                .toBytes();
    }

    public static byte[] clientLoungeState() {
        return new PacketWriter().opcode(CLIENT_LOUNGE_STATE).toBytes();
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

    public static byte[] clientEnterLobby() {
        return new PacketWriter().opcode(CLIENT_ENTER_LOBBY).toBytes();
    }

    public static byte[] clientLeaveLobby() {
        return new PacketWriter().opcode(CLIENT_LEAVE_LOBBY).toBytes();
    }

    public static byte[] clientChat(String nick, String msg) {
        return new PacketWriter()
                .opcode(CLIENT_CHAT)
                .pstr(nick == null ? "" : nick)
                .pstr(msg == null ? "" : msg)
                .toBytes();
    }

    public static byte[] clientSetReady(int ready) {
        return new PacketWriter().opcode(CLIENT_SET_READY).u8(ready).toBytes();
    }

    public static byte[] clientKeepalive() {
        return new PacketWriter().opcode(CLIENT_KEEPALIVE).toBytes();
    }

    /** C# CLIENT {@code 0x0A}: i16 roomId, u8 count, then (type u8 + value)×count. */
    public static byte[] clientChangeRoomCourse(int roomId, int course) {
        return new PacketWriter()
                .opcode(CLIENT_CHANGE_ROOM_INFO)
                .i16(roomId)
                .u8(1)
                .u8(ROOM_CHANGE_COURSE)
                .u8(course)
                .toBytes();
    }

    public static byte[] clientWhisper(String nick, String msg) {
        return new PacketWriter()
                .opcode(CLIENT_WHISPER)
                .pstr(nick == null ? "" : nick)
                .pstr(msg == null ? "" : msg)
                .toBytes();
    }

    public static byte[] clientRequestCash() {
        return new PacketWriter().opcode(CLIENT_REQUEST_CASH).toBytes();
    }

    public static byte[] clientRequestUserInfo(int uid, int season) {
        return new PacketWriter().opcode(CLIENT_REQUEST_USERINFO).u32(uid).u8(season).toBytes();
    }

    public static byte[] clientUpdateMacros(String[] macros) {
        PacketWriter w = new PacketWriter().opcode(CLIENT_UPDATE_MACRO);
        for (int i = 0; i < MACRO_COUNT; i++) {
            String text = (macros != null && i < macros.length && macros[i] != null) ? macros[i] : "";
            w.fixedStr(text, MACRO_BYTES);
        }
        return w.toBytes();
    }

    public static byte[] clientRequestServerList() {
        return new PacketWriter().opcode(CLIENT_REQUEST_SERVER_LIST).toBytes();
    }

    public static byte[] clientRequestRank() {
        return new PacketWriter().opcode(CLIENT_REQUEST_RANK).toBytes();
    }

    /** C# CLIENT {@code 0x9C} empty. */
    public static byte[] clientLast5() {
        return new PacketWriter().opcode(CLIENT_USER_MATCH_HISTORY).toBytes();
    }

    public static byte[] clientChangeTeam(int team) {
        return new PacketWriter().opcode(CLIENT_CHANGE_TEAM).u8(team).toBytes();
    }

    public static byte[] clientRequestRoomDetail(int numero) {
        return new PacketWriter().opcode(CLIENT_REQUEST_DETAIL_ROOM_INFO).u16(numero).toBytes();
    }

    public static byte[] clientInvite(String nick, int uid) {
        return new PacketWriter()
                .opcode(CLIENT_INVITE)
                .pstr(nick == null ? "" : nick)
                .u32(uid)
                .toBytes();
    }

    /**
     * C# invite success {@code 0x12F}/{@code 0x83}: u16 0 + GS uid + channel + room +
     * inviter uid + nick + invited uid. Fail {@code 0x12F} is u16 err only.
     */
    public static byte[] inviteOk(int opcode, int serverUid, int channelId, int room,
            int fromUid, String fromNick, int toUid) {
        return new PacketWriter()
                .opcode(opcode)
                .u16(0)
                .u32(serverUid)
                .u8(channelId)
                .u16(room)
                .u32(fromUid)
                .pstr(fromNick == null ? "" : fromNick)
                .u32(toUid)
                .toBytes();
    }

    public static byte[] inviteFail(int err) {
        return new PacketWriter().opcode(SERVER_INVITE_REPLY).u16(err).toBytes();
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

    public static byte[] clientCamera(float mira) {
        return new PacketWriter().opcode(CLIENT_CAMERA).f32(mira).toBytes();
    }

    public static byte[] clientClick(int state, float point) {
        return new PacketWriter().opcode(CLIENT_CLICK).u8(state).f32(point).toBytes();
    }

    public static byte[] clientPowerShot(int power) {
        return new PacketWriter().opcode(CLIENT_POWER_SHOT).u8(power).toBytes();
    }

    public static byte[] clientClub(int club) {
        return new PacketWriter().opcode(CLIENT_CLUB).u8(club).toBytes();
    }

    public static byte[] clientEmoticon(int typing) {
        return new PacketWriter().opcode(CLIENT_EMOTICON).i16(typing).toBytes();
    }

    public static byte[] clientDrop(float x, float y, float z) {
        return new PacketWriter().opcode(CLIENT_DROP).f32(x).f32(y).f32(z).toBytes();
    }

    public static byte[] clientTimeCheck() {
        return new PacketWriter().opcode(CLIENT_TIMECHECK).toBytes();
    }

    public static byte[] clientLoadPercent(int percent) {
        return new PacketWriter().opcode(CLIENT_LOADING_INFO).u8(percent).toBytes();
    }

    public static byte[] clientTeamChat(String msg) {
        return new PacketWriter().opcode(CLIENT_TEAMCHAT).pstr(msg == null ? "" : msg).toBytes();
    }

    public static byte[] clientAllowWhisper(int on) {
        return new PacketWriter().opcode(CLIENT_ALLOW_WHISPER).u8(on).toBytes();
    }

    public static byte[] camera(int oid, float mira) {
        return new PacketWriter().opcode(SERVER_CAMERA).i32(oid).f32(mira).toBytes();
    }

    public static byte[] powerShot(int oid, int power) {
        return new PacketWriter().opcode(SERVER_POWER_SHOT).i32(oid).u8(power).toBytes();
    }

    public static byte[] club(int oid, int club) {
        return new PacketWriter().opcode(SERVER_CLUB).i32(oid).u8(club).toBytes();
    }

    public static byte[] typing(int oid, int typing) {
        return new PacketWriter().opcode(SERVER_TYPING).i32(oid).i16(typing).toBytes();
    }

    public static byte[] moveBall(float x, float y, float z) {
        return new PacketWriter().opcode(SERVER_MOVE_BALL).f32(x).f32(y).f32(z).toBytes();
    }

    public static byte[] loadPercent(int oid, int percent) {
        return new PacketWriter().opcode(SERVER_LOAD_PERCENT).i32(oid).u8(percent).toBytes();
    }

    public static byte[] teamChat(String nick, String msg) {
        return new PacketWriter()
                .opcode(SERVER_TEAM_CHAT)
                .pstr(nick == null ? "" : nick)
                .pstr(msg == null ? "" : msg)
                .toBytes();
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

    /** C# CLIENT {@code 0x06}: {@code UserInfoEx.ToRead} 265 bytes. */
    public static byte[] clientFinishGame() {
        return new PacketWriter().opcode(CLIENT_MY_STATISTICS).zero(USER_INFO_BYTES).toBytes();
    }

    /** C# CLIENT {@code 0x31}: {@code UserInfoEx.ToRead} 265 bytes, no reply. */
    public static byte[] clientHoleStat() {
        return new PacketWriter().opcode(CLIENT_HOLE_STAT).zero(USER_INFO_BYTES).toBytes();
    }

    /** C# CLIENT {@code 0x30}: u8 opt (0 resume / 1 pause). */
    public static byte[] clientPause(int opt) {
        return new PacketWriter().opcode(CLIENT_PAUSE).u8(opt).toBytes();
    }

    /** C# CLIENT {@code 0x32}: u8 AFK state. */
    public static byte[] clientSleep(int state) {
        return new PacketWriter().opcode(CLIENT_SLEEP).u8(state).toBytes();
    }

    /** C# CLIENT {@code 0x34}: empty body. */
    public static byte[] clientTeeshotReady() {
        return new PacketWriter().opcode(CLIENT_TEESHOT_READY).toBytes();
    }

    /** C# CLIENT {@code 0x37}: empty body. */
    public static byte[] clientEndStroke() {
        return new PacketWriter().opcode(CLIENT_END_STROKE_GAME).toBytes();
    }

    /** C# CLIENT {@code 0x35}: u16 team finish state. No reply. */
    public static byte[] clientTeamFinishHole(int state) {
        return new PacketWriter().opcode(CLIENT_TEAM_HOLEIN_PANG).u16(state).toBytes();
    }

    /** C# CLIENT {@code 0x36}: u8 0 stop / 1 continue. */
    public static byte[] clientContinueVersus(int opt) {
        return new PacketWriter().opcode(CLIENT_ANSWER_GOSTOP).u8(opt).toBytes();
    }

    /** C# CLIENT {@code 0x39}: i32 caddie id. */
    public static byte[] clientPayCaddieHoliday(int caddieId) {
        return new PacketWriter().opcode(CLIENT_REEMPLOY_CADDIE).i32(caddieId).toBytes();
    }

    /** C# CLIENT {@code 0x3A}: empty body. */
    public static byte[] clientReport() {
        return new PacketWriter().opcode(CLIENT_REPORT).toBytes();
    }

    /** C# CLIENT {@code 0x4F}: u8 chat-block. */
    public static byte[] clientChatPenalty(int block) {
        return new PacketWriter().opcode(CLIENT_CHAT_PENALITY).u8(block).toBytes();
    }

    /** C# CLIENT {@code 0x57}: PStr notice. */
    public static byte[] clientNotice(String notice) {
        return new PacketWriter().opcode(CLIENT_NOTICE).pstr(notice == null ? "" : notice).toBytes();
    }

    /** C# CLIENT {@code 0x60}: i16 room number. */
    public static byte[] clientDestroyRoom(int numero) {
        return new PacketWriter().opcode(CLIENT_DESTROY_ROOM).i16(numero).toBytes();
    }

    /** C# CLIENT {@code 0x65}: f32 speed. */
    public static byte[] clientSpeedRate(float speed) {
        return new PacketWriter().opcode(CLIENT_SPEED_RATE).f32(speed).toBytes();
    }

    /** C# CLIENT {@code 0x66}: PStr ticker. */
    public static byte[] clientTicker(String msg) {
        return new PacketWriter().opcode(CLIENT_ONELINE_REQUEST).pstr(msg == null ? "" : msg).toBytes();
    }

    /** C# CLIENT {@code 0x67}: empty body. */
    public static byte[] clientTickerQuery() {
        return new PacketWriter().opcode(CLIENT_ONELINE_QUERY).toBytes();
    }

    /** C# CLIENT {@code 0x73}: i32 mascot id + PStr message. */
    public static byte[] clientMascotMessage(int mascotId, String msg) {
        return new PacketWriter()
                .opcode(CLIENT_CHANGE_MASCOT)
                .i32(mascotId)
                .pstr(msg == null ? "" : msg)
                .toBytes();
    }

    /** C# CLIENT {@code 0x33}: u8 tipo + PStr. */
    public static byte[] clientReportError(int tipo, String msg) {
        return new PacketWriter()
                .opcode(CLIENT_REPORT_ERROR)
                .u8(tipo)
                .pstr(msg == null ? "" : msg)
                .toBytes();
    }

    /** C# CLIENT {@code 0x3C} Msg_OFF: u16 0x111 + uid + PStr + u8 opt. */
    public static byte[] clientMsnMsgOff(int uid, String msg, int opt) {
        return new PacketWriter()
                .opcode(CLIENT_MSN_REQUEST)
                .u16(MSN_MSG_OFF)
                .u32(uid)
                .pstr(msg == null ? "" : msg)
                .u8(opt)
                .toBytes();
    }

    /** C# CLIENT {@code 0x3C} Friend_List: u16 0x11F. */
    public static byte[] clientMsnFriendList() {
        return new PacketWriter().opcode(CLIENT_MSN_REQUEST).u16(MSN_FRIEND_LIST).toBytes();
    }

    /** C# CLIENT {@code 0x42}: u8 count + count×u32 arrows. */
    public static byte[] clientShotArrows(int... arrows) {
        PacketWriter w = new PacketWriter().opcode(CLIENT_SHOT_COMMAND).u8(arrows == null ? 0 : arrows.length);
        if (arrows != null) {
            for (int arrow : arrows) {
                w.u32(arrow);
            }
        }
        return w.toBytes();
    }

    /** C# CLIENT {@code 0x4A}: u32 warehouse typeid. */
    public static byte[] clientReplay(int typeid) {
        return new PacketWriter().opcode(CLIENT_REPLAY_ONLINE).u32(typeid).toBytes();
    }

    public static byte[] clientShopCancel() {
        return new PacketWriter().opcode(CLIENT_SHOP_CANCEL).toBytes();
    }

    public static byte[] clientShopClose() {
        return new PacketWriter().opcode(CLIENT_SHOP_CLOSE).toBytes();
    }

    public static byte[] clientShopOpenEdit() {
        return new PacketWriter().opcode(CLIENT_SHOP_OPEN_EDIT).toBytes();
    }

    public static byte[] clientShopView(int ownerUid) {
        return new PacketWriter().opcode(CLIENT_SHOP_VIEW).u32(ownerUid).toBytes();
    }

    public static byte[] clientShopCloseView(int ownerUid) {
        return new PacketWriter().opcode(CLIENT_SHOP_CLOSE_VIEW).u32(ownerUid).toBytes();
    }

    public static byte[] clientShopName(String name) {
        return new PacketWriter().opcode(CLIENT_SHOP_NAME).pstr(name == null ? "" : name).toBytes();
    }

    public static byte[] clientShopVisit() {
        return new PacketWriter().opcode(CLIENT_SHOP_VISIT).toBytes();
    }

    public static byte[] clientShopPang() {
        return new PacketWriter().opcode(CLIENT_SHOP_PANG).toBytes();
    }

    public static byte[] clientShopOpenItems(int count) {
        return new PacketWriter().opcode(CLIENT_SHOP_OPEN_ITEMS).u32(count).toBytes();
    }

    public static byte[] clientShopBuy(int ownerUid) {
        return new PacketWriter().opcode(CLIENT_SHOP_BUY).u32(ownerUid).toBytes();
    }

    public static byte[] clientPapelShop() {
        return new PacketWriter().opcode(CLIENT_PAPEL_SHOP).toBytes();
    }

    public static byte[] clientEnterShop() {
        return new PacketWriter().opcode(CLIENT_ENTER_SHOP).toBytes();
    }

    /** C# CLIENT {@code 0x143}: i32 page. */
    public static byte[] clientOpenMailBox(int page) {
        return new PacketWriter().opcode(CLIENT_OPEN_MAILBOX).i32(page).toBytes();
    }

    /** C# CLIENT {@code 0x144}: i32 email id. */
    public static byte[] clientOpenMail(int emailId) {
        return new PacketWriter().opcode(CLIENT_OPEN_MAIL).i32(emailId).toBytes();
    }

    /**
     * C# CLIENT {@code 0x145}: from/to uid, PStr nick, u16 opt, PStr msg, u64 pang,
     * u8 count + optional {@code EmailInfo.item} rows.
     */
    public static byte[] clientSendMail(
            int fromUid, int toUid, String nick, int opt, String msg, long pang, int count, byte[] items) {
        PacketWriter w = new PacketWriter()
                .opcode(CLIENT_SEND_MAIL)
                .u32(fromUid)
                .u32(toUid)
                .pstr(nick == null ? "" : nick)
                .u16(opt)
                .pstr(msg == null ? "" : msg)
                .u64(pang)
                .u8(count);
        if (items != null) {
            w.bytes(items);
        }
        return w.toBytes();
    }

    /** C# CLIENT {@code 0x146}: i32 email id. */
    public static byte[] clientTakeMail(int emailId) {
        return new PacketWriter().opcode(CLIENT_TAKE_MAIL).i32(emailId).toBytes();
    }

    /** C# CLIENT {@code 0x147}: u32 count + count×u32 ids + u32 page. */
    public static byte[] clientDeleteMail(int page, int... ids) {
        PacketWriter w = new PacketWriter().opcode(CLIENT_DELETE_MAIL).u32(ids == null ? 0 : ids.length);
        if (ids != null) {
            for (int id : ids) {
                w.u32(id);
            }
        }
        return w.u32(page).toBytes();
    }

    /** C# CLIENT {@code 0x63}: type + remaining payload. */
    public static byte[] clientSyncActivity(int type, byte[] payload) {
        return new PacketWriter().opcode(CLIENT_SYNC_ACTIVITY).u8(type).bytes(payload).toBytes();
    }

    /** C# {@code PLAYER_ACTION_ROTATION}: f32 r. */
    public static byte[] clientSyncActivityRotation(float r) {
        return new PacketWriter().opcode(CLIENT_SYNC_ACTIVITY).u8(ACTION_ROTATION).f32(r).toBytes();
    }

    /** C# lounge loc / move: 3 floats xz r. */
    public static byte[] clientSyncActivityLocation(int type, float x, float z, float r) {
        return new PacketWriter()
                .opcode(CLIENT_SYNC_ACTIVITY)
                .u8(type)
                .f32(x)
                .f32(z)
                .f32(r)
                .toBytes();
    }

    /** C# lounge state / ack-player: u32. */
    public static byte[] clientSyncActivityState(int type, int state) {
        return new PacketWriter().opcode(CLIENT_SYNC_ACTIVITY).u8(type).u32(state).toBytes();
    }

    /**
     * C# CLIENT {@code 0x1F} with {@code qntd == 0} → gift {@code 0x6A} code 9.
     */
    public static byte[] clientGiftEmpty(int uid) {
        return new PacketWriter()
                .opcode(CLIENT_REQUEST_GIFT_ITEM)
                .u16(0)
                .u32(uid)
                .pstr("")
                .u8(0)
                .u16(0)
                .toBytes();
    }

    /**
     * C# CLIENT {@code 0x1F} with one {@code BuyItem}. Without IFF this fails
     * {@code initItemFromBuyItem} → {@code 0x6A} code 1.
     */
    public static byte[] clientGiftItem(int uid, int typeid, int qntd, int pang, int cookie) {
        PacketWriter w = new PacketWriter()
                .opcode(CLIENT_REQUEST_GIFT_ITEM)
                .u16(0)
                .u32(uid)
                .pstr("")
                .u8(0)
                .u16(1);
        w.i32(0);
        w.u32(typeid);
        w.i16(0);
        w.i16(0);
        w.u32(qntd);
        w.u32(pang);
        w.u32(cookie);
        w.zero(13);
        return w.toBytes();
    }

    /** C# CLIENT {@code 0x0B}: u8 type + i32 id/typeid. */
    public static byte[] clientLobbyItem(int type, int id) {
        return new PacketWriter().opcode(CLIENT_LOBBY_USERINFO_CHANGED).u8(type).i32(id).toBytes();
    }

    /** C# CLIENT {@code 0x0C}: {@code TYPE_CHANGE} + i32 id (or u32 ball typeid). */
    public static byte[] clientRoomItem(int type, int id) {
        return new PacketWriter().opcode(CLIENT_REQUEST_USERINFO_CHANGED).u8(type).i32(id).toBytes();
    }

    /**
     * C# CLIENT {@code 0x0C} {@code TC_ALL}: character, caddie, clubset, ball.
     */
    public static byte[] clientRoomItemAll(int character, int caddie, int clubset, int ball) {
        return new PacketWriter()
                .opcode(CLIENT_REQUEST_USERINFO_CHANGED)
                .u8(ITEM_ALL)
                .i32(character)
                .i32(caddie)
                .i32(clubset)
                .u32(ball)
                .toBytes();
    }

    /**
     * Malformed buy (option+qntd underrun) so C# catch writes {@link #BUY_FAIL_GENERIC}.
     */
    public static byte[] clientBuyItem() {
        return new PacketWriter().opcode(CLIENT_REQUEST_BUY_ITEM).u16(0).toBytes();
    }

    /** C# {@code requestBuyItemShop} with {@code qntd == 0} → {@link #BUY_FAIL_EMPTY}. */
    public static byte[] clientBuyEmpty() {
        return new PacketWriter().opcode(CLIENT_REQUEST_BUY_ITEM).u8(0).u16(0).toBytes();
    }

    /**
     * C# CLIENT {@code 0x1D}: option, u16 count, {@code BuyItem} × count, int32 coupon id.
     */
    public static byte[] clientBuyItem(int typeid, int qntd, int pang, int cookie) {
        PacketWriter w = new PacketWriter().opcode(CLIENT_REQUEST_BUY_ITEM).u8(0).u16(1);
        w.i32(0);
        w.u32(typeid);
        w.i16(0);
        w.i16(0);
        w.u32(qntd);
        w.u32(pang);
        w.u32(cookie);
        w.zero(13);
        w.i32(0);
        return w.toBytes();
    }

    public static BuyRequest readBuyRequest(PacketReader reader) {
        int option = reader.u8();
        int count = reader.u16();
        List<BuyItem> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            items.add(readBuyItem(reader));
        }
        int coupon = 0;
        if (count > 0) {
            coupon = reader.i32();
        }
        return new BuyRequest(option, items, coupon);
    }

    public static BuyItem readBuyItem(PacketReader reader) {
        int id = reader.i32();
        int typeid = reader.u32();
        int time = reader.i16();
        int itemType = reader.i16();
        int qntd = reader.u32();
        int pang = reader.u32();
        int cookie = reader.u32();
        if (reader.remaining() >= 13) {
            reader.readBytes(13);
        }
        return new BuyItem(id, typeid, time, itemType, qntd, pang, cookie);
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

    public record BuyItem(int id, int typeid, int time, int itemType, int qntd, int pang, int cookie) {}

    public record BuyRequest(int option, List<BuyItem> items, int couponId) {}

    public record BoughtItem(int typeid, int id, int time, int flagTime, int qntdDep) {}

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
