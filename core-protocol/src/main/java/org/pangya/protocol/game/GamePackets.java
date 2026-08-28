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
    /**
     * C# Versus {@code sendReplyFinishLoadHole} {@code 0x53} i32 oid.
     * Hole-start turn (not {@link #SERVER_PLAYER_TURN}).
     */
    public static final int SERVER_HOLE_TURN = 0x53;
    /**
     * C# Versus {@code sendPlayerTurn} {@code 0x63} i32 oid. Same numeric as
     * {@link #CLIENT_SYNC_ACTIVITY}, opposite direction.
     */
    public static final int SERVER_PLAYER_TURN = 0x63;
    public static final int SERVER_CAMERA = 0x56;
    public static final int SERVER_POWER_SHOT = 0x58;
    public static final int SERVER_CLUB = 0x59;
    /**
     * C# Versus/Tourney {@code requestUseActiveItem} {@code 0x5A}: u32 typeid +
     * i32 rand seed + i32 oid. Fail is silent.
     */
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
    /**
     * C# {@code pacote09A} / {@code SERVER_ADMIT_IDENTITY}. Same numeric as
     * CLIENT PCBANG mascot-msg in C#, opposite direction.
     */
    public static final int SERVER_ADMIT_IDENTITY = 0x9A;
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
    /**
     * C# {@code pacote0A5} club-set/character stat up/down. Opposite direction
     * from CLIENT enchant {@code 0x4B}.
     */
    public static final int SERVER_CLUB_STATS = 0xA5;
    /** C# {@code pacote0FC} messenger {@code ServerInfo} list. */
    public static final int SERVER_MESSENGER_LIST = 0xFC;
    /**
     * C# {@code pacote102} gacha tickets + pang + cookie. Same layout as the
     * login-dump first tail packet.
     */
    public static final int SERVER_GACHA_COUPON = 0x102;
    /** C# {@code pacote113} intrusion (enter tourney after start). */
    public static final int SERVER_INTRUSION = 0x113;
    /** C# {@code pacote10E} last-5 players. */
    public static final int SERVER_LAST5 = 0x10E;
    /** C# {@code SERVER_OPEN_TIKI_REPORT} fail {@code 0x11A}. */
    public static final int SERVER_TICKET_REPORT = 0x11A;
    /** C# lucky-pouch fail {@code 0x129}. Opposite direction from CLIENT tiki {@code 0x129}. */
    public static final int SERVER_LUCKY_POUCH = 0x129;
    /** C# {@code SERVER_ITEMSTORAGE_RES_ACCESS} {@code 0x16C}. */
    public static final int SERVER_LOCKER_ACCESS = 0x16C;
    /** C# {@code SERVER_ITEMSTORAGE_RES_STATE} {@code 0x170}. */
    public static final int SERVER_LOCKER_STATE = 0x170;
    /** C# {@code SERVER_WEB_AUTH_KEY_ACK} {@code 0x1AD}. */
    public static final int SERVER_WEB_AUTH_KEY = 0x1AD;
    /** C# {@code SERVER_REQ_CHANGE_GAME_SERVER_ACK} {@code 0x1D4}. */
    public static final int SERVER_CHANGE_GAME_SERVER = 0x1D4;
    /** C# {@code SERVER_REQ_POINT_SHOP_OPEN_ACK} {@code 0x1E7}. */
    public static final int SERVER_TIKI_SHOP = 0x1E7;
    /** C# {@code SERVER_CLUBSETWORKSHOP_REQ_UP_LEVEL_ACK} {@code 0x23D}. */
    public static final int SERVER_CLUB_WORKSHOP_LEVEL = 0x23D;
    /** C# workshop confirm catch {@code 0x23E}. */
    public static final int SERVER_CLUB_WORKSHOP_CONFIRM = 0x23E;
    /** C# workshop cancel catch {@code 0x23F}. */
    public static final int SERVER_CLUB_WORKSHOP_CANCEL = 0x23F;
    /** C# workshop rank catch {@code 0x240}. */
    public static final int SERVER_CLUB_WORKSHOP_RANK = 0x240;
    /** C# Tiki points {@code 0x1E8}. */
    public static final int SERVER_TIKI_POINTS = 0x1E8;
    /** C# Tiki item→TP {@code 0x1E9}. */
    public static final int SERVER_TIKI_EXCHANGE_TP = 0x1E9;
    /** C# Tiki TP→item {@code 0x1EA}. */
    public static final int SERVER_TIKI_EXCHANGE_ITEM = 0x1EA;
    /** C# item-buff catch {@code 0x181}. */
    public static final int SERVER_ITEM_BUFF = 0x181;
    /** C# comet-refill catch {@code 0x197}. */
    public static final int SERVER_COMET_REFILL = 0x197;
    /** C# mail-box catch {@code 0x19D}. */
    public static final int SERVER_BOX_MAIL = 0x19D;
    /** C# locker item list {@code 0x16D}. */
    public static final int SERVER_LOCKER_ITEMS = 0x16D;
    /** C# locker pang {@code 0x172}. */
    public static final int SERVER_LOCKER_PANG = 0x172;
    /**
     * C# My Room check {@code 0x12B}. Option 0 is deny (seed {@code allow_enter==0}).
     */
    public static final int SERVER_MY_ROOM = 0x12B;
    /** C# Dolfini make-pass {@code 0x176}. */
    public static final int SERVER_LOCKER_MAKE_PASS = 0x176;
    /** C# Dolfini change-pass {@code 0x174}. */
    public static final int SERVER_LOCKER_CHANGE_PASS = 0x174;
    /** C# Dolfini mode-enter {@code 0x173}. */
    public static final int SERVER_LOCKER_MODE = 0x173;
    /** C# Dolfini add-item catch {@code 0x16E}. */
    public static final int SERVER_LOCKER_ADD = 0x16E;
    /** C# Dolfini remove-item catch {@code 0x16F}. */
    public static final int SERVER_LOCKER_REMOVE = 0x16F;
    /** C# Dolfini update-pang catch {@code 0x171}. Opposite CLIENT earcuff {@code 0x171}. */
    public static final int SERVER_LOCKER_UPDATE_PANG = 0x171;
    /**
     * C# open-card-pack catch {@code 0x154} u32 1. Same numeric as
     * {@link #CLIENT_LEAVE_DAILY_QUEST}, opposite direction.
     */
    public static final int SERVER_OPEN_CARD_PACK = 0x154;
    /** C# use-card-special catch {@code 0x160}. */
    public static final int SERVER_USE_CARD = 0x160;
    /** C# extend-rental catch {@code 0x18F} u8 1. */
    public static final int SERVER_EXTEND_RENTAL = 0x18F;
    /**
     * C# cutin {@code 0x18D}: u8 0 + u16 1 fail, u8 0 + u16 3 GZ disabled,
     * or u8 1 + CutinInformation on IFF hit. Same numeric as
     * {@link #CLIENT_TIKI_SHOP_EXCHANGE}, opposite direction.
     */
    public static final int SERVER_CUTIN = 0x18D;
    /** C# cutin catch error u16. */
    public static final int CUTIN_ERR = 1;
    /** C# Grand Zodiac cutin disabled u16. */
    public static final int CUTIN_GZ_DISABLED = 3;
    /** C# {@code stActiveCutin} body: u32 uid + u32 tipo + u16 opt + u32 char + u8. */
    public static final int CUTIN_BODY_BYTES = 15;
    /** C# delete-rental catch {@code 0x190} u8 1. */
    public static final int SERVER_DELETE_RENTAL = 0x190;
    /** C# workshop transform-confirm catch {@code 0x242}. */
    public static final int SERVER_WORKSHOP_TRANSFORM_CONFIRM = 0x242;
    /** C# workshop transform-cancel catch {@code 0x243}. */
    public static final int SERVER_WORKSHOP_TRANSFORM_CANCEL = 0x243;
    /** C# workshop transfer catch {@code 0x245}. */
    public static final int SERVER_WORKSHOP_TRANSFER = 0x245;
    /** C# workshop recovery catch {@code 0x246}. */
    public static final int SERVER_WORKSHOP_RECOVERY = 0x246;
    /** C# club-set reset catch {@code 0x247}. */
    public static final int SERVER_CLUBSET_RESET = 0x247;
    /** C# memorial catch {@code 0x264}. */
    public static final int SERVER_MEMORIAL = 0x264;
    /**
     * C# UCC catch {@code 0x12E} sbyte -1. Same numeric as
     * {@link #CLIENT_MARKER}, opposite direction.
     */
    public static final int SERVER_UCC = 0x12E;
    /**
     * C# UCC web-key {@code 0x153}. Same numeric as
     * {@link #CLIENT_REWARD_DAILY_QUEST}, opposite direction.
     */
    public static final int SERVER_UCC_WEB_KEY = 0x153;
    /** C# club workshop event {@code pacote24E} {@code 0x24E}. */
    public static final int SERVER_WORKSHOP_EVENT = 0x24E;
    /** C# {@code requestClubWorkShopEventCount} {@code 0x24B}: i32 0 + 16 subcodes. */
    public static final int SERVER_WORKSHOP_EVENT_COUNT = 0x24B;
    /**
     * C# Chip-in Practice / Grand Zodiac {@code 0x1F2} empty end-game.
     */
    public static final int SERVER_GZ_END_GAME = 0x1F2;
    /**
     * C# Versus/Tourney {@code requestShotEndData} {@code 0x1F7}: i32 oid +
     * u8 hole + {@code ShotEndLocationData.ToArray()} (87 bytes). Fail is
     * silent. Same numeric family as {@link #SERVER_MARKER}.
     */
    public static final int SERVER_SHOT_END = 0x1F7;
    /** C# {@code ShotEndLocationData} {@code ToArray} size. */
    public static final int SHOT_END_LOCATION_BYTES = 87;
    /** C# Versus {@code requestMarkerOnCourse} {@code 0x1F8}. */
    public static final int SERVER_MARKER = 0x1F8;
    /** C# {@code requestActivePaws} {@code 0x236} u32 uid. */
    public static final int SERVER_ACTIVE_PAWS = 0x236;
    /**
     * C# Versus/Tourney {@code requestActiveWing} {@code 0x203}: u32 uid +
     * u32 typeid. Fail is silent (log only).
     */
    public static final int SERVER_ACTIVE_WING = 0x203;
    /**
     * C# room-wait toggle-assist {@code 0x26A}: u32 0 + typeid + uid, or
     * u32 error.
     */
    public static final int SERVER_TOGGLE_ASSIST = 0x26A;
    /**
     * C# {@code requestActiveAssistGreen} {@code 0x26B}: u32 0 + typeid + uid,
     * or u32 error. GAME errors write the full code.
     */
    public static final int SERVER_ASSIST_GREEN = 0x26B;
    /**
     * C# {@code requestActiveRing} {@code 0x237}: u32 0 + uid + typeid + u8
     * efeito, or u32 error.
     */
    public static final int SERVER_ACTIVE_RING = 0x237;
    /**
     * C# {@code requestActiveGlove} {@code 0x265}: u32 0 + typeid + uid, or
     * u32 error.
     */
    public static final int SERVER_ACTIVE_GLOVE = 0x265;
    /**
     * C# {@code requestActiveEarcuff} {@code 0x24C}: u32 0 + typeid + uid +
     * u8 angle + f32, or u32 error.
     */
    public static final int SERVER_ACTIVE_EARCUFF = 0x24C;
    /**
     * C# {@code requestActiveRingGround} {@code 0x266}: u32 0 +
     * {@code stRingGround} + uid, or u32 error. Versus session-sends.
     */
    public static final int SERVER_ACTIVE_RING_GROUND = 0x266;
    /** C# rainbow paws {@code 0x27E} u32 uid. */
    public static final int SERVER_RING_PAWS_RAINBOW = 0x27E;
    /** C# power-gauge ring {@code 0x27F} u32 uid. Fail is silent. */
    public static final int SERVER_RING_POWER = 0x27F;
    /**
     * C# miracle-sign {@code 0x280}: u32 0 + typeid + uid, or u32 error.
     */
    public static final int SERVER_RING_MIRACLE = 0x280;
    /** C# paws ring-set {@code 0x281} u32 uid. */
    public static final int SERVER_RING_PAWS_SET = 0x281;
    /** C# {@code leaveRoomGrandPrix} {@code 0x254}: u32 0 + i16 -1. */
    public static final int SERVER_GP_EXIT_ROOM = 0x254;
    /** C# attendance check catch {@code 0x248} u32 {@code ~0}. */
    public static final int SERVER_ATTENDANCE = 0x248;
    /** C# attendance login-count catch {@code 0x249} u32 {@code ~0}. */
    public static final int SERVER_ATTENDANCE_LOGIN = 0x249;
    /** C# Grand Prix lobby {@code 0x250}. */
    public static final int SERVER_GP_LOBBY = 0x250;
    /** C# Grand Prix leave lobby {@code 0x251} u32 0. */
    public static final int SERVER_GP_LEAVE = 0x251;
    /**
     * C# My Room enter character {@code 0x168} {@code PlayerRoomInfoEx}.
     * Same numeric as {@link #CLIENT_WORKSHOP_TRANSFORM_CONFIRM}, opposite
     * direction.
     */
    public static final int SERVER_MY_ROOM_CHAR = 0x168;
    /**
     * C# My Room posters {@code 0x12D}. Same numeric as
     * {@link #CLIENT_GZ_INITIAL}, opposite direction.
     */
    public static final int SERVER_MY_ROOM_POSTERS = 0x12D;
    /** C# big Papel catch {@code 0x26C}. */
    public static final int SERVER_BIG_PAPEL = 0x26C;
    /** C# character mastery expand catch {@code 0x26E}. */
    public static final int SERVER_CHAR_MASTERY = 0x26E;
    /** C# character stats-up catch {@code 0x26F}. */
    public static final int SERVER_CHAR_STATS_UP = 0x26F;
    /** C# character stats-down catch {@code 0x270}. */
    public static final int SERVER_CHAR_STATS_DOWN = 0x270;
    /** C# character card-equip catch {@code 0x271}. */
    public static final int SERVER_CHAR_CARD_EQUIP = 0x271;
    /** C# character card-equip patcher catch {@code 0x272}. */
    public static final int SERVER_CHAR_CARD_PATCHER = 0x272;
    /** C# character remove-card catch {@code 0x273}. */
    public static final int SERVER_CHAR_CARD_REMOVE = 0x273;
    /** C# Tiki shop exchange catch {@code 0x274}. */
    public static final int SERVER_TIKI_SHOP_EXCHANGE = 0x274;
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
    /** C# personal-shop sold notify {@code 0xED}. */
    public static final int SERVER_SHOP_SOLD = 0xED;
    /** C# {@code SERVER_OPEN_PAPEL_SHOP} {@code 0x10B}. */
    public static final int SERVER_PAPEL_SHOP = 0x10B;
    /** C# Papel-play result {@code 0x21B}. */
    public static final int SERVER_PAPEL_PLAY = 0x21B;
    /**
     * C# {@code SERVER_BS_USABLE_TIMES} {@code 0xFB} after Papel play.
     * Opposite CLIENT web-key {@code 0xFB}.
     */
    public static final int SERVER_PAPEL_REMAIN = 0xFB;
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
    /**
     * C# {@code pacote216} daily-quest stamp: unix + count (+ optional items).
     */
    public static final int SERVER_DAILY_QUEST_STAMP = 0x216;
    /** C# {@code pacote225} {@code DailyQuestInfoUser}. */
    public static final int SERVER_DAILY_QUEST_INFO = 0x225;
    /** C# {@code pacote226} accept daily quest. */
    public static final int SERVER_DAILY_QUEST_ACCEPT = 0x226;
    /** C# {@code pacote227} take daily-quest reward. */
    public static final int SERVER_DAILY_QUEST_REWARD = 0x227;
    /** C# {@code pacote228} leave daily quest. */
    public static final int SERVER_DAILY_QUEST_LEAVE = 0x228;
    /** C# {@code pacote22C} achievement GUI result. */
    public static final int SERVER_ACHIEVEMENT_GUI = 0x22C;
    /** C# Cadie Magic Box fail/success {@code 0x22F}. */
    public static final int SERVER_CADIE = 0x22F;
    /**
     * C# {@code requestLoloCardCompose} {@code 0x229} card tipo
     * ({@code SERVER_REQ_LOLO_CARD_COMPOSE_ACK}).
     */
    public static final int SERVER_LOLO_TIPO = 0x229;
    /** C# Lolo Card Compose {@code 0x22A}: fail u32 error / success u32 0 + typeid. */
    public static final int SERVER_LOLO = 0x22A;
    /**
     * C# {@code requestDeleteActiveItem} fail {@code 0xC5} sbyte -1.
     * Opposite direction from CLIENT mailbox-get {@code 0xC5} (JP uses {@code 0x146}).
     */
    public static final int SERVER_DELETE_ITEM = 0xC5;
    /** C# {@code SERVER_SYNC_ACTIVITY} / {@code pacote0C4}: oid + u8 type + payload. */
    public static final int SERVER_SYNC_ACTIVITY = 0xC4;
    public static final int SERVER_MASCOT_SEED = 0x16A;
    /**
     * C# in-game toggle-assist reject {@code pacote16A} u32 0. Same numeric as
     * {@link #SERVER_MASCOT_SEED}.
     */
    public static final int SERVER_ASSIST_INGAME = 0x16A;
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
    /**
     * C# {@code packet017} {@code requestUseActiveItem}. Not-in-room /
     * not-in-game / fail CHANNEL-ROOM catch is silent. Success broadcasts
     * {@link #SERVER_ACTIVE_ITEM} {@code 0x5A}: typeid + i32 seed + oid.
     * {@code findCommomItem}/{@code IsItemEquipable} stand-in is SQL
     * {@code shop_catalog} plus ITEM group bits.
     */
    public static final int CLIENT_USE_ITEM = 0x17;
    public static final int CLIENT_EMOTICON = 0x18;
    public static final int CLIENT_DROP = 0x19;
    public static final int CLIENT_HOLE_INFO = 0x1A;
    public static final int CLIENT_SHOT_RESULT = 0x1B;
    /**
     * C# {@code packet01C} {@code requestFinishShot}. Versus {@code game_broadcast}
     * {@link #SERVER_END_SHOT} {@code 0xCC}; Tourney {@code session_send}.
     * Body is cube/coin opt+count; without IFF cube IDs the drop list is empty
     * (oid + u8 0). Versus duplicate {@code finish_shot2} is silent.
     */
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
    /** C# {@code CLIENT_JOIN_GALLERY} / {@code packet03E} spy enter. Catch silent. */
    public static final int CLIENT_JOIN_GALLERY = 0x3E;
    /** C# {@code CLIENT_REPLAY_ONLINE} / {@code packet04A}; catch is silent. */
    public static final int CLIENT_REPLAY_ONLINE = 0x4A;
    /**
     * C# {@code CLIENT_ENCHANT} / {@code packet04B} club-set stats. Same numeric
     * value as {@link #SERVER_ROOM_USER_INFO_CHANGED}, opposite direction.
     */
    public static final int CLIENT_ENCHANT = 0x4B;
    /**
     * C# {@code CLIENT_REQUEST_KICK} / {@code packet061}: log only (GM kick is
     * {@code 0x8F}). Same numeric as C# {@code SERVER_DISCONNECT}.
     */
    public static final int CLIENT_REQUEST_KICK = 0x61;
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
    /** C# {@code CLIENT_INTRUSION} / {@code packet09D} enter tourney after start. */
    public static final int CLIENT_INTRUSION = 0x9D;
    /**
     * C# {@code CLIENT_REQUEST_REFRESH_GACHA_TICKETS} / {@code packet09E}.
     * Same numeric as {@link #SERVER_WEATHER}, opposite direction.
     */
    public static final int CLIENT_REFRESH_GACHA = 0x9E;
    /**
     * C# {@code CLIENT_UPDATE_INGAME_WEBPAGE} / {@code packet0A1}. Same numeric
     * as {@link #SERVER_USERINFO_OFFLINE}, opposite direction.
     */
    public static final int CLIENT_WEB_LINK = 0xA1;
    /**
     * C# {@code CLIENT_REQUEST_PANG_INFO} / {@code packet0A2} web-guild exit.
     * Same numeric as {@link #SERVER_RANK_ADDRESS}, opposite direction.
     */
    public static final int CLIENT_REQUEST_PANG_INFO = 0xA2;
    /**
     * C# {@code CLIENT_USE_TIKI_REPORT} / {@code packet0AA}. Same numeric as
     * {@link #SERVER_NEW_ITEM}, opposite direction. Not-in-room catch is silent.
     */
    public static final int CLIENT_USE_TICKET_REPORT = 0xAA;
    /** C# {@code CLIENT_OPEN_TIKI_REPORT} / {@code packet0AB}. */
    public static final int CLIENT_OPEN_TICKET_REPORT = 0xAB;
    /** C# {@code CLIENT_COMPLETE_QUEST} / {@code packet0AE} tutorial. */
    public static final int CLIENT_COMPLETE_QUEST = 0xAE;
    /** C# {@code CLIENT_OPEN_LUCKY_POUCH} / {@code packet0B2}. */
    public static final int CLIENT_OPEN_LUCKY_POUCH = 0xB2;
    /** C# {@code CLIENT_REQUEST_UPDATE_USER_PLACE} / {@code packet0C1}. */
    public static final int CLIENT_UPDATE_PLACE = 0xC1;
    /** C# {@code CLIENT_ITEMSTORAGE_REQ_ACCESS} / {@code packet0CC}. */
    public static final int CLIENT_LOCKER_ACCESS = 0xCC;
    /** C# {@code CLIENT_ITEMSTORAGE_REQ_STATE} / {@code packet0D3}. */
    public static final int CLIENT_LOCKER_STATE = 0xD3;
    /** C# {@code CLIENT_HEARTBEAT} / {@code packet0F4}. No reply. */
    public static final int CLIENT_HEARTBEAT = 0xF4;
    /** C# {@code CLIENT_WEB_AUTH_KEY} / {@code packet0FB}. */
    public static final int CLIENT_WEB_AUTH_KEY = 0xFB;
    /** C# {@code packet140} {@code requestEnterShop}. */
    public static final int CLIENT_ENTER_SHOP = 0x140;
    /** C# {@code CLIENT_REQ_NEW_BONGDARISHOP_PLAY_NORMAL} / {@code packet14B}. */
    public static final int CLIENT_PAPEL_PLAY = 0x14B;
    /** C# {@code CLIENT_REQ_CHANGE_GAME_SERVER} / {@code packet119}. */
    public static final int CLIENT_CHANGE_GAME_SERVER = 0x119;
    /** C# {@code CLIENT_REQ_POINT_SHOP_OPEN} / {@code packet126} legacy Tiki. */
    public static final int CLIENT_TIKI_SHOP = 0x126;
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
    /** C# {@code packet064} {@code requestDeleteActiveItem}. */
    public static final int CLIENT_DELETE_ITEM = 0x64;
    /** C# {@code packet06B} {@code requestSetNoticeBeginCaddieHolyDay}. */
    public static final int CLIENT_CADDIE_HOLIDAY_NOTICE = 0x6B;
    /**
     * C# {@code packet083} {@code requestEnterOtherChannelAndLobby}.
     * Same numeric value as {@link #SERVER_INVITE}, opposite direction.
     */
    public static final int CLIENT_ENTER_OTHER_CHANNEL = 0x83;
    /** C# {@code packet088} {@code requestCheckGameGuardAuthAnswer} (empty). */
    public static final int CLIENT_GAMEGUARD = 0x88;
    /**
     * C# {@code CLIENT_REQUEST_MESSENGER_SERVER_LIST} / {@code packet08B}.
     * Same numeric as {@link #SERVER_PAUSE}, opposite direction.
     */
    public static final int CLIENT_REQUEST_MESSENGER_LIST = 0x8B;
    /**
     * C# {@code CLIENT_GM_COMMAND} / {@code packet08F}. Non-GM / fail sends
     * {@code 0x40} notice; {@code CCG_VISIBLE} success updates lobby {@code 0x46}.
     */
    public static final int CLIENT_GM_COMMAND = 0x8F;
    /** C# {@code packet0B4}: log-only invite relog. */
    public static final int CLIENT_INVITE_RELOGIN = 0xB4;
    /** C# {@code packet141} {@code requestChangeWindNextHoleRepeat}. */
    public static final int CLIENT_WIND_NEXT_HOLE = 0x141;
    /** C# {@code packet151} {@code requestDailyQuest}. */
    public static final int CLIENT_DAILY_QUEST = 0x151;
    /** C# {@code packet152} {@code requestAcceptDailyQuest}. */
    public static final int CLIENT_ACCEPT_DAILY_QUEST = 0x152;
    /** C# {@code packet153} {@code requestTakeRewardDailyQuest}. */
    public static final int CLIENT_REWARD_DAILY_QUEST = 0x153;
    /** C# {@code packet154} {@code requestLeaveDailyQuest}. */
    public static final int CLIENT_LEAVE_DAILY_QUEST = 0x154;
    /** C# {@code packet155} {@code requestLoloCardCompose}. */
    public static final int CLIENT_LOLO = 0x155;
    /** C# {@code CLIENT_ACTIVE_AUTO_COMMAND} / {@code packet156}. Not-in-room silent. */
    public static final int CLIENT_ACTIVE_AUTO_COMMAND = 0x156;
    /** C# {@code packet157} achievement GUI. */
    public static final int CLIENT_ACHIEVEMENT = 0x157;
    /** C# {@code CLIENT_ACTIVE_PAWS_EFFECT} / {@code packet15C}. Not-in-room silent. */
    public static final int CLIENT_ACTIVE_PAWS = 0x15C;
    /**
     * C# {@code CLIENT_ACTIVE_RING_EFFECT} / {@code packet15D}: u32 typeid +
     * u32 effect_value + u8 efeito. Not-in-room / not-in-game silent.
     * Versus broadcasts {@link #SERVER_ACTIVE_RING}.
     */
    public static final int CLIENT_ACTIVE_RING = 0x15D;
    /** C# {@code CLIENT_CLUBSETWORKSHOP_REQ_UP_LEVEL} / {@code packet164}. */
    public static final int CLIENT_CLUB_WORKSHOP_LEVEL = 0x164;
    /** C# {@code packet165} workshop confirm. Empty pending → {@code 0x23E}. */
    public static final int CLIENT_CLUB_WORKSHOP_CONFIRM = 0x165;
    /** C# {@code packet166} workshop cancel. Empty pending → {@code 0x23F}. */
    public static final int CLIENT_CLUB_WORKSHOP_CANCEL = 0x166;
    /** C# {@code packet167} workshop rank. Missing card → {@code 0x240}. */
    public static final int CLIENT_CLUB_WORKSHOP_RANK = 0x167;
    /** C# {@code packet168} transform confirm. No pending ClubSet → {@code 0x242}. */
    public static final int CLIENT_WORKSHOP_TRANSFORM_CONFIRM = 0x168;
    /** C# {@code packet169} transform cancel. No pending ClubSet → {@code 0x243}. */
    public static final int CLIENT_WORKSHOP_TRANSFORM_CANCEL = 0x169;
    /** C# {@code packet16B} recovery. Missing warehouse → {@code 0x246}. */
    public static final int CLIENT_WORKSHOP_RECOVERY = 0x16B;
    /**
     * C# {@code packet16C} workshop transfer. Same numeric as
     * {@link #SERVER_LOCKER_ACCESS}, opposite direction.
     */
    public static final int CLIENT_WORKSHOP_TRANSFER = 0x16C;
    /**
     * C# {@code packet16D} club-set reset. Same numeric as
     * {@link #SERVER_LOCKER_ITEMS}, opposite direction.
     */
    public static final int CLIENT_CLUBSET_RESET = 0x16D;
    /** C# {@code packet17F} memorial. Coin 0 → {@code 0x264}. */
    public static final int CLIENT_MEMORIAL = 0x17F;
    /**
     * C# {@code packet0B5} My Room check. Seed {@code allow_enter==0} →
     * {@code 0x12B} option 0 + to_uid. No channel.
     */
    public static final int CLIENT_MY_ROOM = 0xB5;
    /** C# {@code packet0BD} use card special. Typeid 0 → {@code 0x160}. */
    public static final int CLIENT_USE_CARD = 0xBD;
    /**
     * C# {@code packet0CA} open card pack. Same numeric as
     * {@link #SERVER_ONELINE_QUERY}, opposite direction. Catch always u32 1.
     */
    public static final int CLIENT_OPEN_CARD_PACK = 0xCA;
    /** C# {@code packet0CE} Dolfini add. Count 0 → {@code 0x16E}. */
    public static final int CLIENT_LOCKER_ADD = 0xCE;
    /** C# {@code packet0CF} Dolfini remove. Truncated → {@code 0x16F}. */
    public static final int CLIENT_LOCKER_REMOVE = 0xCF;
    /** C# {@code packet0D0} make Dolfini pass. Empty → {@code 0x176} u32 1. */
    public static final int CLIENT_LOCKER_MAKE_PASS = 0xD0;
    /** C# {@code packet0D1} change Dolfini pass. Empty old → {@code 0x174} u32 1. */
    public static final int CLIENT_LOCKER_CHANGE_PASS = 0xD1;
    /** C# {@code packet0D2} Dolfini mode-enter. Empty pass → {@code 0x173}. */
    public static final int CLIENT_LOCKER_MODE = 0xD2;
    /** C# {@code packet0D4} Dolfini update pang. Opt 0 over-withdraw → {@code 0x171}. */
    public static final int CLIENT_LOCKER_UPDATE_PANG = 0xD4;
    /**
     * C# {@code packet0E5} {@code requestActiveCutin}. Not-in-room /
     * not-in-game CHANNEL/ROOM catch is silent. In-game Tourney/Versus catch
     * sends {@link #SERVER_CUTIN} u8 0 + u16 {@link #CUTIN_ERR}. Grand Zodiac
     * sends u8 0 + u16 {@link #CUTIN_GZ_DISABLED}. Success needs IFF
     * {@code findCutinInfomation}.
     */
    public static final int CLIENT_CUTIN = 0xE5;
    /** C# {@code packet0E6} extend rental. Catch always {@code 0x18F} u8 1. */
    public static final int CLIENT_EXTEND_RENTAL = 0xE6;
    /** C# {@code packet0E7} delete rental. Catch always {@code 0x190} u8 1. */
    public static final int CLIENT_DELETE_RENTAL = 0xE7;
    /** C# {@code packet0FE} UCC load. No reply. */
    public static final int CLIENT_UCC_LOAD = 0xFE;
    /**
     * C# {@code packet0B9} {@code HandleUCC}. Unknown opt → {@code 0x12E}
     * sbyte -1. No channel.
     */
    public static final int CLIENT_UCC = 0xB9;
    /**
     * C# {@code packet0C9} UCC web-key. Same numeric as
     * {@link #SERVER_ONELINE_MSG}, opposite direction. uid 0 → {@code 0x153}.
     */
    public static final int CLIENT_UCC_WEB_KEY = 0xC9;
    /**
     * C# {@code packet16E} attendance check. Same numeric as
     * {@link #SERVER_LOCKER_ADD}, opposite direction. Empty catalog →
     * {@code 0x248} u32 {@code ~0}.
     */
    public static final int CLIENT_ATTENDANCE = 0x16E;
    /**
     * C# {@code packet16F} attendance login-count. Same numeric as
     * {@link #SERVER_LOCKER_REMOVE}, opposite direction.
     */
    public static final int CLIENT_ATTENDANCE_LOGIN = 0x16F;
    /**
     * C# {@code packet172} club workshop event. Same numeric as
     * {@link #SERVER_LOCKER_PANG}, opposite direction. Always {@code 0x24E}.
     */
    public static final int CLIENT_WORKSHOP_EVENT = 0x172;
    /**
     * C# {@code packet173} club workshop event count → {@code 0x24B}.
     * Same numeric as {@link #SERVER_LOCKER_MODE}, opposite direction.
     */
    public static final int CLIENT_WORKSHOP_EVENT_COUNT = 0x173;
    /**
     * C# {@code packet176} GP lobby. Same numeric as
     * {@link #SERVER_LOCKER_MAKE_PASS}, opposite direction.
     */
    public static final int CLIENT_GP_LOBBY = 0x176;
    /** C# {@code packet177} leave GP lobby → {@code 0x251} u32 0. */
    public static final int CLIENT_GP_LEAVE = 0x177;
    /**
     * C# {@code packet179} enter GP room. IFF miss typeid 0 → {@code 0x253}
     * {@code shopSys(0x6700001)}. Same SERVER opcode as
     * {@link #SERVER_START_GAME_FAIL}.
     */
    public static final int CLIENT_GP_ENTER = 0x179;
    /** C# {@code packet17A} exit GP room. Not-in-room CHANNEL catch silent. */
    public static final int CLIENT_GP_EXIT_ROOM = 0x17A;
    /** C# {@code packet12D} GZ initial value. Not-in-room silent. */
    public static final int CLIENT_GZ_INITIAL = 0x12D;
    /**
     * C# {@code packet12E} marker-on-course. Same numeric as
     * {@link #SERVER_UCC}, opposite direction. Not-in-room silent.
     */
    public static final int CLIENT_MARKER = 0x12E;
    /**
     * C# {@code packet12F} {@code requestShotEndData}. Same numeric as
     * {@link #SERVER_INVITE_REPLY}, opposite direction. Not-in-room /
     * not-in-game / truncated CHANNEL catch is silent. Versus and Tourney
     * broadcast {@link #SERVER_SHOT_END}.
     */
    public static final int CLIENT_SHOT_END = 0x12F;
    /**
     * C# {@code packet131} {@code requestLeaveChipInPractice}. Not-in-room /
     * not-in-game / wrong tipo ROOM catch is silent. GZ Practice in-game
     * sends {@link #SERVER_GZ_END_GAME} then the finish dump.
     */
    public static final int CLIENT_LEAVE_CHIP_IN = 0x131;
    /** C# {@code packet137} GZ first hole. Not-in-room silent. */
    public static final int CLIENT_GZ_FIRST_HOLE = 0x137;
    /**
     * C# {@code packet138} {@code requestActiveWing}. Not-in-room / not-in-game
     * CHANNEL catch is silent. Versus broadcasts {@link #SERVER_ACTIVE_WING};
     * Tourney/Practice send only to self. Fail is silent.
     */
    public static final int CLIENT_WING = 0x138;
    /**
     * C# {@code packet171} {@code requestActiveEarcuff}. Not-in-room /
     * not-in-game CHANNEL catch is silent. PART uses warehouse + parts;
     * MASCOT uses mascot list. Versus broadcasts {@link #SERVER_ACTIVE_EARCUFF}.
     */
    public static final int CLIENT_EARCUFF = 0x171;
    /**
     * C# {@code packet180} {@code requestActiveGlove}. Not-in-room /
     * not-in-game CHANNEL catch is silent. PART checks parts; AUX_PART
     * checks auxparts. Versus broadcasts {@link #SERVER_ACTIVE_GLOVE}.
     */
    public static final int CLIENT_GLOVE = 0x180;
    /**
     * C# {@code packet181} {@code requestActiveRingGround}. Not-in-room /
     * not-in-game CHANNEL catch is silent. Versus session-sends
     * {@link #SERVER_ACTIVE_RING_GROUND}.
     */
    public static final int CLIENT_RING_GROUND = 0x181;
    /**
     * C# {@code packet184} {@code requestToggleAssist}. Not-in-room CHANNEL
     * catch is silent. In-game rejects with {@link #SERVER_ASSIST_INGAME}.
     */
    public static final int CLIENT_TOGGLE_ASSIST = 0x184;
    /**
     * C# {@code packet185} {@code requestActiveAssistGreen}. Not-in-room /
     * not-in-game CHANNEL catch is silent.
     */
    public static final int CLIENT_ASSIST_GREEN = 0x185;
    /** C# {@code packet192} Event Arin 2014 log only. */
    public static final int CLIENT_EVENT_ARIN = 0x192;
    /**
     * C# {@code packet0B7} {@code requestEnterMyRoom}. Channel required.
     * {@code 0x168} {@code PlayerRoomInfoEx} then {@code 0x12D} option 1 +
     * poster count.
     */
    public static final int CLIENT_ENTER_MY_ROOM = 0xB7;
    /**
     * C# {@code packet0CB} {@code requestFinishGame}. Same as
     * {@link #CLIENT_MY_STATISTICS}; not-in-game silent.
     */
    public static final int CLIENT_FINISH_GAME_CB = 0xCB;
    /**
     * C# {@code packet12C} {@code requestFinishGame}. Same as
     * {@link #CLIENT_MY_STATISTICS}; not-in-game silent.
     */
    public static final int CLIENT_FINISH_GAME_12C = 0x12C;
    /** C# {@code packet186} big Papel. Empty balls → {@code 0x26C}. */
    public static final int CLIENT_BIG_PAPEL = 0x186;
    /** C# {@code packet187} character mastery expand. Missing char → {@code 0x26E}. */
    public static final int CLIENT_CHAR_MASTERY = 0x187;
    /** C# {@code packet188} character stats up. Missing char → {@code 0x26F}. */
    public static final int CLIENT_CHAR_STATS_UP = 0x188;
    /** C# {@code packet189} character stats down. Missing char → {@code 0x270}. */
    public static final int CLIENT_CHAR_STATS_DOWN = 0x189;
    /** C# {@code packet18A} character card equip. IFF miss → {@code 0x271}. */
    public static final int CLIENT_CHAR_CARD_EQUIP = 0x18A;
    /** C# {@code packet18B} card equip with patcher. Missing patcher → {@code 0x272}. */
    public static final int CLIENT_CHAR_CARD_PATCHER = 0x18B;
    /** C# {@code packet18C} character remove card. Missing char → {@code 0x273}. */
    public static final int CLIENT_CHAR_CARD_REMOVE = 0x18C;
    /**
     * C# {@code packet18D} Tiki shop exchange. Count 0 → {@code 0x274}.
     * Distinct from {@link #CLIENT_TIKI_EXCHANGE_ITEM} {@code 0x129}.
     */
    public static final int CLIENT_TIKI_SHOP_EXCHANGE = 0x18D;
    /**
     * C# {@code packet196} rainbow paws. Versus broadcasts
     * {@link #SERVER_RING_PAWS_RAINBOW}; Tourney session-send. Fail silent.
     */
    public static final int CLIENT_RING_PAWS_RAINBOW = 0x196;
    /**
     * C# {@code packet197} power-gauge ring. Versus broadcasts
     * {@link #SERVER_RING_POWER}; fail is silent.
     */
    public static final int CLIENT_RING_POWER = 0x197;
    /**
     * C# {@code packet198} miracle-sign. Versus broadcasts
     * {@link #SERVER_RING_MIRACLE}.
     */
    public static final int CLIENT_RING_MIRACLE = 0x198;
    /**
     * C# {@code packet199} paws ring-set. Versus broadcasts
     * {@link #SERVER_RING_PAWS_SET}; fail silent.
     */
    public static final int CLIENT_RING_PAWS_SET = 0x199;
    /** C# {@code packet041} GM identity. Non-GM CHANNEL catch is silent. */
    public static final int CLIENT_IDENTITY = 0x41;
    /** C# {@code packet0CD} Dolfini locker item page. */
    public static final int CLIENT_LOCKER_ITEMS = 0xCD;
    /** C# {@code packet0D5} Dolfini locker pang. */
    public static final int CLIENT_LOCKER_PANG = 0xD5;
    /** C# {@code packet0D8} use item buff. */
    public static final int CLIENT_ITEM_BUFF = 0xD8;
    /** C# {@code packet0DE} refuse whisper. */
    public static final int CLIENT_REFUSE_WHISPER = 0xDE;
    /** C# {@code packet0EC} comet refill. */
    public static final int CLIENT_COMET_REFILL = 0xEC;
    /** C# {@code packet0EF} open box from mail. */
    public static final int CLIENT_BOX_MAIL = 0xEF;
    /** C# {@code packet127} Tiki points. */
    public static final int CLIENT_TIKI_POINTS = 0x127;
    /** C# {@code packet128} Tiki item→TP. Opposite {@link #SERVER_LUCKY_POUCH}. */
    public static final int CLIENT_TIKI_EXCHANGE_TP = 0x128;
    /**
     * C# {@code packet129} Tiki TP→item. Same numeric value as
     * {@link #SERVER_LUCKY_POUCH}, opposite direction.
     */
    public static final int CLIENT_TIKI_EXCHANGE_ITEM = 0x129;
    /** C# {@code packet158} {@code requestCadieCauldronExchange}. */
    public static final int CLIENT_CADIE = 0x158;
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
    /** C# {@code eChatMsg.CHAT_REFUSE_WHISPER}. {@code pacote040} writes nick only. */
    public static final int CHAT_REFUSE_WHISPER = 4;
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
    /** C# caddie holiday {@code 0x93} success {@code WriteByte(2)}. */
    public static final int CADDIE_HOLIDAY_OK = 2;
    /** C# {@code pCi.rent_flag != 2} rejects holiday pay. */
    public static final int CADDIE_RENT_HOLIDAY = 2;
    /** C# {@code GetSystemTimeAsUnix() + (30 * 24 * 3600)}. */
    public static final int CADDIE_HOLIDAY_SECONDS = 30 * 24 * 3600;
    /** Seeded {@code iff_caddie.valor_mensal} for {@link #TYPEID_CADDIE_PAPEL}. */
    public static final int CADDIE_HOLIDAY_PANG = 1000;
    /** C# mascot-message success {@code 0xE2} {@code WriteByte(4)}. */
    public static final int MASCOT_MSG_OK = 4;
    /** C# {@code msg.Length > 30} rejects. */
    public static final int MASCOT_MSG_MAX = 30;
    /** Seeded {@code iff_mascot.change_price} for {@link #TYPEID_MASCOT}. */
    public static final int MASCOT_MSG_PRICE = 100;
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
    /** C# {@code uCapability.block_give_item_gm} bit 16. */
    public static final int CAPABILITY_BLOCK_GIVEITEM = 16;
    /** C# {@code uCapability.gm_normal} bit 128. */
    public static final int CAPABILITY_GM_NORMAL = 128;
    /** C# {@code uCapability.title_gm} setter {@code 32768}. */
    public static final int CAPABILITY_TITLE_GM = 32768;
    /** C# {@code COMMON_CMD_GM.CCG_VISIBLE}. */
    public static final int GM_CMD_VISIBLE = 3;
    /** C# {@code COMMON_CMD_GM.CCG_WHISPER}. */
    public static final int GM_CMD_WHISPER = 4;
    /** C# {@code COMMON_CMD_GM.CCG_CHANNEL}. */
    public static final int GM_CMD_CHANNEL = 5;
    /** C# {@code COMMON_CMD_GM.CCG_OPEN_WHISPER_PLAYER_LIST}. */
    public static final int GM_CMD_OPEN_WHISPER = 8;
    /** C# {@code COMMON_CMD_GM.CCG_CLOSE_WHISPER_PLAYER_LIST}. */
    public static final int GM_CMD_CLOSE_WHISPER = 9;
    /** C# {@code COMMON_CMD_GM.CCG_KICK}. */
    public static final int GM_CMD_KICK = 10;
    /** C# {@code COMMON_CMD_GM.CCG_DISCONNECT}. */
    public static final int GM_CMD_DISCONNECT = 11;
    /** C# {@code COMMON_CMD_GM.CCG_DESTROY} (empty then green OK). */
    public static final int GM_CMD_DESTROY = 13;
    /** C# {@code COMMON_CMD_GM.CCG_CHANGE_WIND_VERSUS}. */
    public static final int GM_CMD_WIND = 14;
    /** C# {@code COMMON_CMD_GM.CCG_CHANGE_WEATHER}. */
    public static final int GM_CMD_WEATHER = 15;
    /** C# {@code COMMON_CMD_GM.CCG_IDENTITY} (same numeric as {@code CCG_NOTICE}). */
    public static final int GM_CMD_IDENTITY = 16;
    /** C# {@code COMMON_CMD_GM.CCG_GIVEITEM}. */
    public static final int GM_CMD_GIVEITEM = 18;
    /** C# {@code COMMON_CMD_GM.CCG_GOLDENBELL}. */
    public static final int GM_CMD_GOLDENBELL = 19;
    /** C# {@code LIMIT_DEGREE} ({@code byte} 255). */
    public static final int LIMIT_DEGREE = 255;
    /** C# giveitem/goldenbell {@code item_qntd > 20000}. */
    public static final int GM_GIVEITEM_MAX = 20000;
    /** C# lounge/game weather {@code 0x9E} type 0 (course). */
    public static final int WEATHER_NORMAL = 0;
    /** C# GM weather {@code 0x9E} type 1. */
    public static final int WEATHER_GM = 1;
    /** C# {@code UtilChat.ChatColor.Green} {@code ToHexString}. */
    public static final String CHAT_GREEN_HEX = "0xff00ff00";
    /** C# {@code UtilChat.ChatColor.Red} {@code ToHexString}. */
    public static final String CHAT_RED_HEX = "0xffff0000";
    /** C# {@code SendChatNotice("Executed Command.")} after a GM command. */
    public static final String GM_CMD_OK = "Executed Command.";
    /** C# {@code SendChatNotice} catch else. */
    public static final String GM_CMD_FAIL = "Nao conseguiu executar o comando.";
    /** C# catch decode 9 (blocked giveitem/goldenbell). */
    public static final String GM_CMD_BLOCKED = "Nao pode executar esse comando, voce foi bloqueado pelo ADM.";
    /**
     * SQL {@code shop_catalog} stand-in for IFF {@code findCommomItem.Name}
     * in GM giveitem/goldenbell mail text.
     */
    public static final String GM_SHOP_ITEM_NAME = "Pang Item";
    /** C# {@code "GM Send Gift: item[ " + name + " ]"}. */
    public static final String GM_GIVEITEM_MSG = "GM Send Gift: item[ " + GM_SHOP_ITEM_NAME + " ]";
    /** C# {@code "GM enviou um item para voce: item[ " + name + " ]"}. */
    public static final String GM_GOLDENBELL_MSG = "GM enviou um item para voce: item[ " + GM_SHOP_ITEM_NAME + " ]";
    /** C# chat spy {@code pacote040} nick after the first-space {@code \\1} insert. */
    public static final String GM_CHAT_SPY_CHANNEL = "\\1[Channel=";
    /** C# PM spy {@code pacote040} nick. */
    public static final String GM_PM_SPY_NICK = "\\1[PM]";
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
    /** C# visit-limit remap {@code PERSONAL_SHOP_MANAGER} sys {@code 5200453}. */
    public static final int SHOP_ERR_VIEW_LIMIT = 5200453;
    public static final int SHOP_ERR_VIEW_NONE = 5200452;
    public static final int SHOP_ERR_CLOSE_VIEW_DEFAULT = 5200500;
    public static final int SHOP_ERR_CLOSE_VIEW_NONE = 5200502;
    public static final int SHOP_ERR_OPEN_DEFAULT = 5200250;
    public static final int SHOP_ERR_OPEN_COUNT = 5200251;
    public static final int SHOP_ERR_OPEN_NONE = 5200252;
    public static final int SHOP_ERR_BUY_DEFAULT = 5200550;
    public static final int SHOP_ERR_BUY_NONE = 5200552;
    /** C# {@code TradeItem.ToArray} 168 bytes. */
    public static final int TRADE_ITEM_BYTES = 168;
    /** C# {@code PersonalShopItem}: u32 index + {@link #TRADE_ITEM_BYTES}. */
    public static final int PERSONAL_SHOP_ITEM_BYTES = 172;
    /** JP {@code personal_config.ini} {@code ITEM_MIN_PRICE}. */
    public static final int SHOP_ITEM_MIN_PRICE = 1;
    /** JP {@code personal_config.ini} {@code ITEM_MAX_PRICE}. */
    public static final int SHOP_ITEM_MAX_PRICE = 20_000_000;
    /** C# {@code Math.Round(cost * 0.95f)} seller share. */
    public static final float SHOP_SALE_RATE = 0.95f;
    /** C# {@code IFF_GROUP.ITEM} → buy packet group byte 1. */
    public static final int SHOP_GROUP_ITEM_BYTE = 1;
    /** C# sold notice nick {@code @INI3}. */
    public static final String SHOP_SALE_NICK = "@INI3";
    /** C# sold notice body. */
    public static final String SHOP_SALE_MSG = "\\c0xff00ff00\\cParabéns, sua venda foi um sucesso!.";
    public static final int SHOP_VISIT_LIMIT = 15;
    /** C# buy {@code qntd} upper bound. */
    public static final int SHOP_BUY_QNTD_MAX = 30000;
    /** C# {@code 0xED} i32 when the shop vector is empty after the sale. */
    public static final int SHOP_SOLD_EMPTY = 3;
    /** C# {@code 0xED} i32 when listed items remain. */
    public static final int SHOP_SOLD_REMAIN = 1;
    /** C# open-shop {@code pang > 2000000000} abuse check. */
    public static final long SHOP_PANG_ABUSE = 2_000_000_000L;
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
    /** C# {@code DailyQuestInfoUser._typeid} SizeConst 3. */
    public static final int DAILY_QUEST_TYPEID_COUNT = 3;
    /** C# {@code pacote226(empty, 1)} option. */
    public static final int DAILY_QUEST_ACCEPT_FAIL = 1;
    /** C# {@code pacote228(empty, 1)} option. */
    public static final int DAILY_QUEST_LEAVE_FAIL = 1;
    /**
     * C# {@code pacote227} catch else {@code 500050} when source is not CHANNEL
     * ({@code MGR_DAILY_QUEST} {@code num_quest<=0}).
     */
    public static final int DAILY_QUEST_REWARD_FAIL = 500050;
    /** C# {@code pacote22C(1)} achievement GUI fail. */
    public static final int ACHIEVEMENT_GUI_FAIL = 1;
    /**
     * C# Cadie {@code count==0||count>4}: CHANNEL sys {@code 5200451}, catch writes
     * {@code sys & 0xFFFF}.
     */
    public static final int CADIE_ERR_COUNT = 5200451;
    /** C# Cadie catch else (non-CHANNEL). */
    public static final int CADIE_ERR_DEFAULT = 5200450;
    /**
     * C# Cadie IFF/{@code findCadieMagicBox} miss / truncated items: CHANNEL sys
     * {@code 5200452}.
     */
    public static final int CADIE_ERR_IFF = 5200452;
    /**
     * C# Cadie {@code mi.level < cmb.level}: CHANNEL sys {@code 5200455}.
     */
    public static final int CADIE_ERR_LEVEL = 5200455;
    /**
     * C# Cadie trade typeid mismatch: CHANNEL sys {@code 5200454}.
     */
    public static final int CADIE_ERR_MISMATCH = 5200454;
    /**
     * C# Cadie {@code exchangeCadieMagicBox <= 0}: CHANNEL sys {@code 5200458}.
     */
    public static final int CADIE_ERR_EXCHANGE = 5200458;
    /** C# {@code findCadieMagicBox((uint)(seq + 1))} for the seeded recipe. */
    public static final int CADIE_SEQ = 1;
    /** C# {@code CadieMagicBox} max {@code item_trade} slots. */
    public static final int CADIE_MAX_TRADE = 4;
    /**
     * C# Lolo {@code findCard} null: CHANNEL sys {@code 0x5400151}.
     */
    public static final int LOLO_ERR_IFF = 0x5400151;
    /** C# Lolo catch else. */
    public static final int LOLO_ERR_DEFAULT = 0x5400150;
    /** C# Lolo {@code CARD_TYPE.T_SECRET}: CHANNEL sys {@code 0x5400152}. */
    public static final int LOLO_ERR_SECRET = 0x5400152;
    /** C# Lolo {@code findCardByTypeid} null: CHANNEL sys {@code 0x5400153}. */
    public static final int LOLO_ERR_OWN = 0x5400153;
    /** C# Lolo {@code pCi.qntd < 1}: CHANNEL sys {@code 0x5400154}. */
    public static final int LOLO_ERR_QNTD = 0x5400154;
    /** C# Lolo client pang ≠ computed: CHANNEL sys {@code 0x5400155}. */
    public static final int LOLO_ERR_PANG = 0x5400155;
    /** C# Lolo {@code drawsLoloCardCompose} null: CHANNEL sys {@code 0x5400156}. */
    public static final int LOLO_ERR_DRAW = 0x5400156;
    /** C# Lolo {@code removeItem <= 0}: CHANNEL sys {@code 0x5400157}. */
    public static final int LOLO_ERR_REMOVE = 0x5400157;
    /** C# {@code LoloCardCompose._typeid} length. */
    public static final int LOLO_CARD_COUNT = 3;
    /** C# {@code CARD_TYPE.T_NORMAL} pang per fused card. */
    public static final int LOLO_PANG_NORMAL = 1000;
    /** C# {@code CARD_TYPE.T_RARE} pang per fused card. */
    public static final int LOLO_PANG_RARE = 2000;
    /** C# {@code CARD_TYPE.T_SUPER_RARE} pang per fused card. */
    public static final int LOLO_PANG_SUPER_RARE = 5000;
    /** C# {@code CARD_TYPE.T_NORMAL}. */
    public static final int CARD_TYPE_NORMAL = 0;
    /** C# {@code CARD_TYPE.T_RARE}. */
    public static final int CARD_TYPE_RARE = 1;
    /** C# {@code CARD_TYPE.T_SUPER_RARE}. */
    public static final int CARD_TYPE_SUPER_RARE = 2;
    /** C# {@code CARD_TYPE.T_SECRET}. */
    public static final int CARD_TYPE_SECRET = 3;
    /**
     * C# Papel play empty {@code dropBalls}: CHANNEL sys {@code 0x5900103}.
     */
    public static final int PAPEL_PLAY_ERR_BALLS = 0x5900103;
    /** C# Papel play catch else. */
    public static final int PAPEL_PLAY_ERR_DEFAULT = 0x5900100;
    /** C# funds CHANNEL sys {@code 0x5900102}. */
    public static final int PAPEL_PLAY_ERR_FUNDS = 0x5900102;
    /** C# {@code Random.Next(PAPEL_SHOP_MIN_BALL, MAX-MIN+1)} → 1–4. */
    public static final int PAPEL_MIN_BALL = 1;
    public static final int PAPEL_MAX_BALL = 4;
    /** C# {@code PAPEL_SHOP_BIG_BALL}. */
    public static final int PAPEL_BIG_BALLS = 10;
    /** C# {@code Random.Next(0, 3)} ball color. */
    public static final int PAPEL_COLOR_COUNT = 3;
    /** C# {@code Random.Next(1, 4)} non-rare qntd. */
    public static final int PAPEL_ITEM_MIN_QNTD = 1;
    public static final int PAPEL_ITEM_MAX_QNTD = 3;
    /** C# {@code PAPEL_SHOP_TYPE.PST_COMMUN}. */
    public static final int PAPEL_TYPE_COMMUN = 0;
    /** C# {@code stItem.type} default from {@code initItemFromBuyItem}. */
    public static final int PAPEL_AWARD_TYPE = 2;
    /** C# {@code WriteZero(25)} after the {@code 0x216} qntd. */
    public static final int PAPEL_AWARD_PAD = 25;
    /** Seeded {@code pangya_papel_shop_config.Price_Normal}. */
    public static final int PAPEL_PRICE_NORMAL = 1000;
    /** Seeded {@code pangya_papel_shop_config.Price_Big}. */
    public static final int PAPEL_PRICE_BIG = 3000;
    /** C# unlimited {@code 0xFB} remain / flag. */
    public static final int PAPEL_UNLIMITED_REMAIN = -1;
    public static final int PAPEL_UNLIMITED_FLAG = -3;
    /**
     * C# gacha catch else (non-CHANNEL) on {@code 0x44} u8 {@code 0xE2}.
     */
    public static final int GACHA_ERR_DEFAULT = 0x5300600;
    /** C# gacha fail {@code 0x44} option byte. */
    public static final int GACHA_ERR_MARKER = 0xE2;
    /** C# intrusion catch {@code 0x113} first byte. */
    public static final int INTRUSION_ERR = 6;
    /**
     * C# missing room CHANNEL sys {@code 1}; else branch also writes {@code 1}.
     */
    public static final int INTRUSION_SYS = 1;
    /** C# club-stats catch {@code 0xA5} u8 0. */
    public static final int CLUB_STATS_ERR = 0;
    /** C# {@code TYPE_SERVER.GAME}. */
    public static final int SERVER_TYPE_GAME = 1;
    /** C# {@code TYPE_SERVER.MSN}. */
    public static final int SERVER_TYPE_MSN = 3;
    /** C# {@code TYPE_SERVER.RANK}. */
    public static final int SERVER_TYPE_RANK = 4;
    /**
     * C# tutorial catch else {@code 0x5300550} on {@code 0x44} u8 {@code 0xE2}.
     */
    public static final int TUTORIAL_ERR_DEFAULT = 0x5300550;
    /** C# tutorial unknown tipo CHANNEL sys {@code 0x5300552}. */
    public static final int TUTORIAL_ERR_TIPO = 0x5300552;
    /** C# workshop unknown item group CHANNEL sys {@code 0x5300201}. */
    public static final int WORKSHOP_ERR_GROUP = 0x5300201;
    /** C# workshop catch else. */
    public static final int WORKSHOP_ERR_DEFAULT = 0x5300200;
    /** C# locker empty-pass CHANNEL sys 1. */
    public static final int LOCKER_ERR_EMPTY = 1;
    /** C# locker wrong pass {@code 0x75}. */
    public static final int LOCKER_ERR_WRONG = 0x75;
    /** C# locker catch else {@code 5100150}. */
    public static final int LOCKER_ERR_DEFAULT = 5100150;
    /** C# {@code DolfiniLocker.isLocker} empty pass. */
    public static final int LOCKER_STATE_NO_PASS = 2;
    /** C# ticket-report catch {@code 0x11A} i32 -1. */
    public static final int TICKET_REPORT_ERR = -1;
    /** C# lucky-pouch catch {@code 0x129} u8 1. */
    public static final int LUCKY_POUCH_ERR = 1;
    /** C# {@code pacote1AD} success option. */
    public static final int WEB_KEY_OK = 1;
    /** C# {@code pacote1AD} fail option. */
    public static final int WEB_KEY_FAIL = 0;
    /** C# {@code pacote1D4} success option. */
    public static final int CHANGE_GS_OK = 0;
    /** C# item-buff typeid 0 CHANNEL sys. */
    public static final int BUFF_ERR_TYPEID = 0x5500401;
    /** C# item-buff catch else. */
    public static final int BUFF_ERR_DEFAULT = 0x5500400;
    /** C# mail-box typeid 0 CHANNEL sys. */
    public static final int BOX_MAIL_ERR_TYPEID = 0x6300101;
    /** C# mail-box catch else. */
    public static final int BOX_MAIL_ERR_DEFAULT = 0x6300100;
    /** C# Tiki exchange count 0 CHANNEL sys {@code 0x5200905}. */
    public static final int TIKI_EXCHANGE_ERR_PTS = 0x5200905;
    /** C# Tiki exchange catch else. */
    public static final int TIKI_EXCHANGE_ERR_DEFAULT = 1;
    /** C# workshop confirm missing ClubSet CHANNEL sys. */
    public static final int WORKSHOP_CONFIRM_ERR = 0x5300301;
    /** C# workshop confirm catch else. */
    public static final int WORKSHOP_CONFIRM_DEFAULT = 0x5300300;
    /** C# workshop cancel missing ClubSet CHANNEL sys. */
    public static final int WORKSHOP_CANCEL_ERR = 0x5300251;
    /** C# workshop cancel catch else. */
    public static final int WORKSHOP_CANCEL_DEFAULT = 0x5300250;
    /** C# workshop rank missing card CHANNEL sys. */
    public static final int WORKSHOP_RANK_ERR = 0x5300351;
    /** C# workshop rank catch else. */
    public static final int WORKSHOP_RANK_DEFAULT = 0x5300350;
    /** C# Dolfini make-pass empty CHANNEL sys 1. */
    public static final int LOCKER_MAKE_PASS_EMPTY = 1;
    /** C# Dolfini make-pass length&gt;4 CHANNEL sys. */
    public static final int LOCKER_MAKE_PASS_LEN = 5100102;
    /** C# Dolfini make-pass catch else. */
    public static final int LOCKER_MAKE_PASS_DEFAULT = 5100100;
    /** C# Dolfini change-pass wrong/empty CHANNEL sys 1. */
    public static final int LOCKER_CHANGE_PASS_WRONG = 1;
    /** C# Dolfini change-pass length&gt;4 CHANNEL sys. */
    public static final int LOCKER_CHANGE_PASS_LEN = 5100202;
    /** C# Dolfini change-pass catch else. */
    public static final int LOCKER_CHANGE_PASS_DEFAULT = 5100200;
    /** C# Dolfini mode-enter empty pass CHANNEL sys. */
    public static final int LOCKER_MODE_EMPTY = 5100251;
    /** C# Dolfini mode-enter catch else. */
    public static final int LOCKER_MODE_DEFAULT = 5100250;
    /** C# Dolfini add count 0 CHANNEL sys. */
    public static final int LOCKER_ADD_ERR_NONE = 5100404;
    /** C# Dolfini add catch else. */
    public static final int LOCKER_ADD_ERR_DEFAULT = 5100400;
    /** C# Dolfini remove catch else. */
    public static final int LOCKER_REMOVE_ERR_DEFAULT = 5100450;
    /** C# Dolfini withdraw pang&gt;locker CHANNEL sys. */
    public static final int LOCKER_PANG_WITHDRAW_ERR = 5100353;
    /** C# Dolfini update-pang catch else. */
    public static final int LOCKER_PANG_ERR_DEFAULT = 5100350;
    /** C# use-card typeid 0 CHANNEL sys. */
    public static final int CARD_ERR_TYPEID = 0x5500351;
    /** C# use-card catch else. */
    public static final int CARD_ERR_DEFAULT = 0x5500350;
    /** C# open-card-pack catch always u32 1. */
    public static final int CARD_PACK_ERR = 1;
    /** C# extend/delete rental catch u8 1. */
    public static final int RENTAL_FAIL = 1;
    /** C# transform-confirm missing ClubSet CHANNEL sys. */
    public static final int WORKSHOP_TRANSFORM_CONFIRM_ERR = 0x5300451;
    /** C# transform-confirm catch else. */
    public static final int WORKSHOP_TRANSFORM_CONFIRM_DEFAULT = 0x5300450;
    /** C# transform-cancel missing ClubSet CHANNEL sys. */
    public static final int WORKSHOP_TRANSFORM_CANCEL_ERR = 0x5300401;
    /** C# transform-cancel catch else. */
    public static final int WORKSHOP_TRANSFORM_CANCEL_DEFAULT = 0x5300400;
    /** C# recovery missing warehouse CHANNEL sys. */
    public static final int WORKSHOP_RECOVERY_ERR = 0x5300151;
    /** C# recovery catch else. */
    public static final int WORKSHOP_RECOVERY_DEFAULT = 0x5300150;
    /** C# transfer missing UCIM CHANNEL sys. */
    public static final int WORKSHOP_TRANSFER_ERR = 0x5300104;
    /** C# transfer catch else. */
    public static final int WORKSHOP_TRANSFER_DEFAULT = 0x5300100;
    /** C# club-set reset unknown typeid CHANNEL sys. */
    public static final int CLUBSET_RESET_ERR = 0x5300506;
    /** C# club-set reset catch else. */
    public static final int CLUBSET_RESET_DEFAULT = 0x5300500;
    /** C# memorial coin 0 CHANNEL sys. */
    public static final int MEMORIAL_ERR_COIN = 0x6300301;
    /** C# memorial catch else. */
    public static final int MEMORIAL_ERR_DEFAULT = 0x6300300;
    /** C# UCC catch {@code WriteSByte(-1)}. */
    public static final int UCC_FAIL = 0xff;
    /** C# UCC web-key uid==0 GAME_SERVER sys {@code 0x5100101}. */
    public static final int UCC_WEB_KEY_ERR_UID = 0x5100101;
    /** C# UCC web-key catch else. */
    public static final int UCC_WEB_KEY_ERR_DEFAULT = 0x5100100;
    /**
     * C# attendance catch writes {@code ~0u} because {@code DECODE_TYPE} is
     * sys&amp;0xFFFF (0) and never equals {@code ATTENDANCE_REWARD_SYSTEM}.
     */
    public static final int ATTENDANCE_FAIL = 0xffff_ffff;
    /** C# {@code uProperty.grand_prix} bit 11. */
    public static final int PROPERTY_GRAND_PRIX = 1 << 11;
    /** C# GP lobby disabled CHANNEL sys {@code 0x750001}. */
    public static final int GP_LOBBY_ERR_DISABLED = 0x750001;
    /** C# GP lobby catch else. */
    public static final int GP_LOBBY_ERR_DEFAULT = 0x750000;
    /** C# GP room IFF miss CHANNEL sys {@code 0x6700001}. */
    public static final int GP_ENTER_ERR_IFF = 0x6700001;
    /** C# GP room catch else (full, not shopSys). */
    public static final int GP_ENTER_ERR_DEFAULT = 0x6700000;
    /** C# {@code pacote24E} holes-per-phase literal. */
    public static final int WORKSHOP_EVENT_HOLES = 3000;
    /** C# {@code requestClubWorkShopEventCount} writes 16 subcodes {@code 1..16}. */
    public static final int WORKSHOP_EVENT_COUNT_SLOTS = 16;
    /** C# {@code 3000/30} barra max. */
    public static final int WORKSHOP_EVENT_BARRA_MAX = 100;
    /** C# {@code barraMax} / last-byte literals. */
    public static final int WORKSHOP_EVENT_BARRA = 10;
    /** C# My Room posters option 1. */
    public static final int MY_ROOM_POSTERS_OPTION = 1;
    /** C# character mastery missing character CHANNEL sys. */
    public static final int CHAR_MASTERY_ERR_CHAR = 0x5200651;
    /** C# character mastery catch else. */
    public static final int CHAR_MASTERY_ERR_DEFAULT = 0x5200650;
    /** C# character mastery empty IFF list CHANNEL sys {@code 0x5200652}. */
    public static final int CHAR_MASTERY_ERR_IFF = 0x5200652;
    /** C# character mastery already max CHANNEL sys {@code 0x5200653}. */
    public static final int CHAR_MASTERY_ERR_MAX = 0x5200653;
    /** C# character mastery seq mismatch CHANNEL sys {@code 0x5200654}. */
    public static final int CHAR_MASTERY_ERR_SEQ = 0x5200654;
    /** C# character mastery level CHANNEL sys {@code 0x5200655}. */
    public static final int CHAR_MASTERY_ERR_LEVEL = 0x5200655;
    /** C# character mastery unknown condition CHANNEL sys {@code 0x5200656}. */
    public static final int CHAR_MASTERY_ERR_COND = 0x5200656;
    /** C# character mastery missing warehouse item CHANNEL sys {@code 0x5200657}. */
    public static final int CHAR_MASTERY_ERR_ITEM = 0x5200657;
    /** C# character mastery item qntd CHANNEL sys {@code 0x5200658}. */
    public static final int CHAR_MASTERY_ERR_QNTD = 0x5200658;
    /** C# {@code CharacterMastery.seq} for the first expand. */
    public static final int CHAR_MASTERY_SEQ = 1;
    /** C# {@code stItem.type} {@code 0xCD} character mastery row on {@code 0x216}. */
    public static final int CHAR_MASTERY_AWARD_TYPE = 0xCD;
    /** C# character stats-up missing character CHANNEL sys. */
    public static final int CHAR_STATS_UP_ERR_CHAR = 0x5200501;
    /** C# character stats-up catch else. */
    public static final int CHAR_STATS_UP_ERR_DEFAULT = 0x5200500;
    /** C# character stats-up invalid slot CHANNEL sys {@code 0x5200502}. */
    public static final int CHAR_STATS_UP_ERR_STAT = 0x5200502;
    /** C# character stats-up PCL limit CHANNEL sys {@code 0x5200503}. */
    public static final int CHAR_STATS_UP_ERR_LIMIT = 0x5200503;
    /** C# character stats-up missing enchant CHANNEL sys {@code 0x5200504}. */
    public static final int CHAR_STATS_UP_ERR_ENCHANT = 0x5200504;
    /** C# character stats-up missing IFF Character CHANNEL sys {@code 0x5200505}. */
    public static final int CHAR_STATS_UP_ERR_CHAR_IFF = 0x5200505;
    /** C# character stats-up empty mastery list CHANNEL sys {@code 0x5200506}. */
    public static final int CHAR_STATS_UP_ERR_MASTERY = 0x5200506;
    /** C# character stats-up mastery count CHANNEL sys {@code 0x5200507}. */
    public static final int CHAR_STATS_UP_ERR_MASTERY_VAL = 0x5200507;
    /** C# character stats-down missing character CHANNEL sys. */
    public static final int CHAR_STATS_DOWN_ERR_CHAR = 0x5200551;
    /** C# character stats-down catch else. */
    public static final int CHAR_STATS_DOWN_ERR_DEFAULT = 0x5200550;
    /** C# character stats-down invalid stat CHANNEL sys {@code 0x5200552}. */
    public static final int CHAR_STATS_DOWN_ERR_STAT = 0x5200552;
    /** C# character stats-down pcl already 0 CHANNEL sys {@code 0x5200553}. */
    public static final int CHAR_STATS_DOWN_ERR_EMPTY = 0x5200553;
    /** C# character stats-down missing IFF Character CHANNEL sys {@code 0x5200554}. */
    public static final int CHAR_STATS_DOWN_ERR_CHAR_IFF = 0x5200554;
    /** C# {@code CharacterInfo.Stats.S_POWER}. */
    public static final int CHAR_STATS_POWER = 0;
    /** C# {@code CharacterInfo.Stats.S_CURVE}. */
    public static final int CHAR_STATS_CURVE = 4;
    /** C# {@code stItem.type} {@code 0xC9} PCL row on {@code 0x216}. */
    public static final int CHAR_STATS_AWARD_TYPE = 0xC9;
    /** C# {@code WriteZeroByte(15)} after 5× u16 PCL. */
    public static final int CHAR_STATS_PCL_PAD = 15;
    /** Seeded {@code iff_enchant.pang} for POWER at pcl 0. */
    public static final int CHAR_STATS_ENCHANT_PANG = 100;
    /** C# card-equip IFF miss CHANNEL sys. */
    public static final int CHAR_CARD_ERR_IFF = 0x5200757;
    /** C# card-equip catch else. */
    public static final int CHAR_CARD_ERR_DEFAULT = 0x5200750;
    /** C# card-equip missing character CHANNEL sys {@code 0x5200751}. */
    public static final int CHAR_CARD_ERR_CHAR = 0x5200751;
    /** C# card-equip missing card CHANNEL sys {@code 0x5200752}. */
    public static final int CHAR_CARD_ERR_OWN = 0x5200752;
    /** C# card-equip slot 4/8 without patcher CHANNEL sys {@code 0x5200753}. */
    public static final int CHAR_CARD_ERR_PATCHER_SLOT = 0x5200753;
    /** C# card-equip slot 7 without part CHANNEL sys {@code 0x5200754}. */
    public static final int CHAR_CARD_ERR_PART_SLOT = 0x5200754;
    /** C# card-equip unknown slot CHANNEL sys {@code 0x5200755}. */
    public static final int CHAR_CARD_ERR_SLOT = 0x5200755;
    /** C# card-equip subgroup mismatch CHANNEL sys {@code 0x5200756}. */
    public static final int CHAR_CARD_ERR_SUB = 0x5200756;
    /** C# card-equip occupied slot CHANNEL sys {@code 0x5200759}. */
    public static final int CHAR_CARD_ERR_OCCUPIED = 0x5200759;
    /** C# {@code stItem.type} {@code 0xCB} card-equip row on {@code 0x216}. */
    public static final int CHAR_CARD_AWARD_TYPE = 0xCB;
    /** C# character card slot 1 (Card_Character[0]). */
    public static final int CHAR_CARD_SLOT = 1;
    /** C# {@code CARD_SUB_TYPE.T_CHARACTER}. */
    public static final int CARD_SUB_CHARACTER = 0;
    /** C# {@code CARD_SUB_TYPE.T_CADDIE}. */
    public static final int CARD_SUB_CADDIE = 1;
    /** C# {@code CARD_SUB_TYPE.T_NPC}. */
    public static final int CARD_SUB_NPC = 5;
    /** C# {@code WriteZeroByte(10)} after 5× i16 {@code stItem.c}. */
    public static final int CHAR_CARD_AWARD_MID_PAD = 10;
    /** C# {@code stItem.c[1..4]} after {@code c[0]} (4× i16). */
    public static final int CHAR_CARD_AWARD_C_REST = 8;
    /**
     * C# {@code STDA_C_ITEM_QNTD = -1} stores {@code c[0] = 32767}.
     */
    public static final int CHAR_CARD_CONSUME_C0 = 32767;
    /** C# card-equip patcher missing item CHANNEL sys. */
    public static final int CHAR_CARD_PATCHER_ERR = 0x5200810;
    /** C# card-equip patcher catch else. */
    public static final int CHAR_CARD_PATCHER_DEFAULT = 0x5200800;
    /** C# card-equip patcher qntd CHANNEL sys {@code 0x5200811}. */
    public static final int CHAR_CARD_PATCHER_ERR_QNTD = 0x5200811;
    /** C# card-equip patcher IFF miss CHANNEL sys {@code 0x5200807}. */
    public static final int CHAR_CARD_PATCHER_ERR_IFF = 0x5200807;
    /** C# card-equip patcher missing character CHANNEL sys {@code 0x5200801}. */
    public static final int CHAR_CARD_PATCHER_ERR_CHAR = 0x5200801;
    /** C# card-equip patcher missing card CHANNEL sys {@code 0x5200802}. */
    public static final int CHAR_CARD_PATCHER_ERR_OWN = 0x5200802;
    /** C# card-equip patcher slot not 4/8 CHANNEL sys {@code 0x5200803}. */
    public static final int CHAR_CARD_PATCHER_ERR_SLOT = 0x5200803;
    /** C# card-equip patcher NPC slot CHANNEL sys {@code 0x5200804}. */
    public static final int CHAR_CARD_PATCHER_ERR_NPC = 0x5200804;
    /** C# card-equip patcher unknown slot CHANNEL sys {@code 0x5200805}. */
    public static final int CHAR_CARD_PATCHER_ERR_UNKNOWN = 0x5200805;
    /** C# card-equip patcher subgroup CHANNEL sys {@code 0x5200806}. */
    public static final int CHAR_CARD_PATCHER_ERR_SUB = 0x5200806;
    /** C# card-equip patcher occupied CHANNEL sys {@code 0x5200812}. */
    public static final int CHAR_CARD_PATCHER_ERR_OCCUPIED = 0x5200812;
    /** C# Club Patcher character slot 4. */
    public static final int CHAR_CARD_PATCHER_SLOT = 4;
    /** C# remove-card missing character CHANNEL sys. */
    public static final int CHAR_CARD_REMOVE_ERR_CHAR = 0x5200851;
    /** C# remove-card catch else. */
    public static final int CHAR_CARD_REMOVE_DEFAULT = 0x5200850;
    /** C# remove-card missing removedor CHANNEL sys {@code 0x5200852}. */
    public static final int CHAR_CARD_REMOVE_ERR_ITEM = 0x5200852;
    /** C# remove-card unknown slot CHANNEL sys {@code 0x5200853}. */
    public static final int CHAR_CARD_REMOVE_ERR_UNKNOWN = 0x5200853;
    /** C# remove-card empty slot CHANNEL sys {@code 0x5200854}. */
    public static final int CHAR_CARD_REMOVE_ERR_SLOT = 0x5200854;
    /** C# remove-card removedor qntd CHANNEL sys {@code 0x5200855}. */
    public static final int CHAR_CARD_REMOVE_ERR_QNTD = 0x5200855;
    /**
     * C# Tiki shop exchange count 0/{@code >5} CHANNEL sys {@code 5200451}
     * (same numeric as {@link #CADIE_ERR_COUNT}).
     */
    public static final int TIKI_SHOP_EXCHANGE_ERR_COUNT = 5200451;
    /**
     * C# Tiki shop exchange truncated body CHANNEL sys {@code 5200452}
     * ({@code BytesRemaining < count * 8}).
     */
    public static final int TIKI_SHOP_EXCHANGE_ERR_TRUNCATED = 5200452;
    /** C# Tiki shop exchange missing item CHANNEL sys. */
    public static final int TIKI_SHOP_EXCHANGE_ERR_ITEM = 0x52000901;
    /** C# Tiki shop exchange catch else. */
    public static final int TIKI_SHOP_EXCHANGE_ERR_DEFAULT = 0x5200900;
    /** C# truncated check uses 8 bytes per item even though {@code ToRead} is 12. */
    public static final int TIKI_SHOP_EXCHANGE_ITEM_CHECK_BYTES = 8;
    /** C# {@code CardEquip}/{@code CardRemove} {@code ToRead} is 5×u32. */
    public static final int CARD_EQUIP_BYTES = 20;
    /** C# My Room deny option. */
    public static final int MY_ROOM_DENY = 0;
    /** C# {@code IFF_GROUP.ITEM}. {@code (typeid & 0xFC000000) >> 26}. */
    public static final int IFF_GROUP_ITEM = 6;
    /** C# {@code IFF_GROUP.ENCHANT} {@code 13}. */
    public static final int IFF_GROUP_ENCHANT = 13;
    /**
     * C# {@code (ENCHANT << 26) | (stat << 20) + pcl}. POWER at pcl 0 is
     * {@code 0x34000000}.
     */
    public static int enchantTypeid(int stat, int pcl) {
        return (IFF_GROUP_ENCHANT << 26) | (stat << 20) | (pcl & 0xff);
    }
    /** C# {@code WriteSByte(-1)} on delete-item fail. */
    public static final int DELETE_ITEM_FAIL = 0xff;
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
    /** C# gift mailbox insert fail {@code 0x6A} u32 8. */
    public static final int GIFT_FAIL_MAIL = 8;
    /**
     * C# {@code enLEVEL.BEGINNER_E} {@code 0x06}. Gift below this → {@code 0x6A}
     * u32 1.
     */
    public static final int GIFT_MIN_LEVEL = 6;
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
    /** C# {@code MULLIGAN_ROSE_TYPEID} {@code 0x1800000E}. Banned in Versus. */
    public static final int TYPEID_MULLIGAN_ROSE = 0x1800000E;
    /** C# {@code COIN_TYPEID} {@code 0x1A000010}. */
    public static final int TYPEID_COIN = 0x1A000010;
    /** C# {@code SPINNING_CUBE_TYPEID} {@code 0x1A00015B}. */
    public static final int TYPEID_SPINNING_CUBE = 0x1A00015B;
    /** C# {@code DropItem.ToArray} 16 bytes; {@code sendEndShot} pads to 128 slots. */
    public static final int DROP_ITEM_BYTES = 16;
    public static final int END_SHOT_DROP_SLOTS = 128;
    /** C# {@code DropItem.eTYPE.CUBE}. */
    public static final long DROP_TYPE_CUBE = 5;
    /** C# {@code DropItem.eTYPE.COIN_EDGE_GREEN}. */
    public static final long DROP_TYPE_COIN_EDGE = 3;
    /** C# {@code DropItem.eTYPE.COIN_GROUND}. */
    public static final long DROP_TYPE_COIN_GROUND = 4;
    /** C# {@code CLUB_PATCHER_TYPEID} {@code 0x1A00018F}. */
    public static final int TYPEID_CLUB_PATCHER = 0x1A00018F;
    /** C# gacha ticket typeid {@code 436207744}. */
    public static final int TYPEID_GACHA_TICKET = 0x1A000080;
    /** C# gacha sub-ticket typeid {@code 436207747}. */
    public static final int TYPEID_GACHA_SUB = 0x1A000083;
    /** Seeded {@code shop_catalog.pang_price} for {@link #TYPEID_SHOP_PANG_ITEM}. */
    public static final int SHOP_PANG_PRICE = 100;
    /**
     * C# {@code IFF_GROUP.CADDIE} {@code 7 << 26}: Papel caddie {@code 0x1C000000}.
     */
    public static final int TYPEID_CADDIE_PAPEL = 0x1C000000;
    /**
     * C# {@code IFF_GROUP.MASCOT} {@code 16 << 26}: {@code 0x40000000}.
     */
    public static final int TYPEID_MASCOT = 0x40000000;
    /** C# {@code IFF_GROUP.CARD} {@code 31 << 26 | 1}: {@code 0x7C000001}. */
    public static final int TYPEID_CARD_NORMAL = 0x7C000001;
    /** C# {@code ASSIST_ITEM_TYPEID} {@code 0x1BE00016}. */
    public static final int TYPEID_ASSIST = 0x1BE00016;
    /** C# toggle-assist add fail {@code 0x5200801}. */
    public static final int TOGGLE_ASSIST_ERR_ADD = 0x5200801;
    /** C# toggle-assist remove fail {@code 0x5200802}. */
    public static final int TOGGLE_ASSIST_ERR_REMOVE = 0x5200802;
    /** C# toggle-assist catch else {@code 0x5200800}. */
    public static final int TOGGLE_ASSIST_ERR_DEFAULT = 0x5200800;
    /** C# assist-green bad/zero typeid {@code 0x5200101}. */
    public static final int ASSIST_GREEN_ERR_TYPEID = 0x5200101;
    /** C# assist-green missing item or assist off {@code 0x5200102}. */
    public static final int ASSIST_GREEN_ERR_OFF = 0x5200102;
    /** C# assist-green catch else {@code 0x5200100}. */
    public static final int ASSIST_GREEN_ERR_DEFAULT = 0x5200100;
    /** C# {@code IFF_GROUP.CARD}. */
    public static final int IFF_GROUP_CARD = 31;
    /** C# {@code IFF_GROUP.CHARACTER}: {@code typeid >>> 26}. */
    public static final int IFF_GROUP_CHARACTER = 1;
    /** C# {@code IFF_GROUP.PART} {@code 2}. */
    public static final int IFF_GROUP_PART = 2;
    /** C# {@code IFF_GROUP.MASCOT} {@code 16}. */
    public static final int IFF_GROUP_MASCOT = 16;
    /** C# {@code IFF_GROUP.AUX_PART} {@code 28}. */
    public static final int IFF_GROUP_AUX_PART = 28;
    /** C# ring catch else {@code 0x330000}. */
    public static final int RING_ERR_DEFAULT = 0x330000;
    /** C# ring typeid 0 {@code 0x330001}. */
    public static final int RING_ERR_TYPEID = 0x330001;
    /** C# ring missing warehouse {@code 0x330002}. */
    public static final int RING_ERR_ITEM = 0x330002;
    /** C# ring no character {@code 0x330003}. */
    public static final int RING_ERR_CHAR = 0x330003;
    /** C# ring not in auxparts {@code 0x330004}. */
    public static final int RING_ERR_EQUIP = 0x330004;
    /** C# glove catch else {@code 0x370000}. */
    public static final int GLOVE_ERR_DEFAULT = 0x370000;
    public static final int GLOVE_ERR_TYPEID = 0x370001;
    public static final int GLOVE_ERR_ITEM = 0x370002;
    public static final int GLOVE_ERR_CHAR = 0x370003;
    public static final int GLOVE_ERR_PART = 0x370004;
    public static final int GLOVE_ERR_AUX = 0x370005;
    /** C# earcuff catch else {@code 0x380000}. */
    public static final int EARCUFF_ERR_DEFAULT = 0x380000;
    public static final int EARCUFF_ERR_TYPEID = 0x380001;
    public static final int EARCUFF_ERR_CHAR = 0x380002;
    public static final int EARCUFF_ERR_ITEM = 0x380003;
    public static final int EARCUFF_ERR_PART = 0x380004;
    public static final int EARCUFF_ERR_MASCOT = 0x380005;
    public static final int EARCUFF_ERR_EQUIP = 0x380006;
    /** C# ring-ground catch else {@code 0x340000}. */
    public static final int RING_GROUND_ERR_DEFAULT = 0x340000;
    public static final int RING_GROUND_ERR_TYPEID = 0x340001;
    public static final int RING_GROUND_ERR_ITEM = 0x340002;
    public static final int RING_GROUND_ERR_EQUIP = 0x340003;
    /** C# miracle catch else {@code 0x350000}. */
    public static final int MIRACLE_ERR_DEFAULT = 0x350000;
    public static final int MIRACLE_ERR_TYPEID = 0x350001;
    public static final int MIRACLE_ERR_ITEM = 0x350002;
    public static final int MIRACLE_ERR_CHAR = 0x350003;
    public static final int MIRACLE_ERR_AUX = 0x350004;
    public static final int MIRACLE_ERR_PART = 0x350005;

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
        out.add(gachaCoupon(0, 0, pang, cookie));
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
        return clientCreateRoom(tipo, name, password, 0, 0);
    }

    public static byte[] clientCreateRoom(int tipo, String name, String password, int timeVs, int time30s) {
        return new PacketWriter()
                .opcode(CLIENT_REQUEST_CREATE_ROOM)
                .u8(0)
                .u32(timeVs)
                .u32(time30s)
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
        return weather(weather, WEATHER_NORMAL);
    }

    /** C# {@code 0x9E}: u16 weather + u8 type (0 course, 1 GM). */
    public static byte[] weather(int weather, int type) {
        return new PacketWriter().opcode(SERVER_WEATHER).u16(weather).u8(type).toBytes();
    }

    /** C# {@code pacote09A}: i32 capability. */
    public static byte[] admitIdentity(int capability) {
        return new PacketWriter().opcode(SERVER_ADMIT_IDENTITY).i32(capability).toBytes();
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

    /** C# Versus hole-start {@code 0x53} i32 oid. */
    public static byte[] holeTurn(int oid) {
        return new PacketWriter().opcode(SERVER_HOLE_TURN).i32(oid).toBytes();
    }

    /** C# Versus {@code sendPlayerTurn} {@code 0x63} i32 oid. */
    public static byte[] playerTurn(int oid) {
        return new PacketWriter().opcode(SERVER_PLAYER_TURN).i32(oid).toBytes();
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
        return endShot(oid, List.of());
    }

    /**
     * C# Versus/Tourney {@code sendEndShot} {@code 0xCC}: i32 oid + u8 count;
     * if count &gt; 0, count×16-byte {@link DropItem} then pad to
     * {@link #END_SHOT_DROP_SLOTS} slots.
     */
    public static byte[] endShot(int oid, List<DropItem> drops) {
        PacketWriter w = new PacketWriter().opcode(SERVER_END_SHOT).i32(oid);
        int n = drops == null ? 0 : drops.size();
        w.u8(n);
        if (n > 0) {
            for (DropItem drop : drops) {
                w.u32(drop.typeid()).u8(drop.course()).u8(drop.hole())
                        .i16(drop.qntd()).u64(drop.type());
            }
            if (n < END_SHOT_DROP_SLOTS) {
                w.zero((END_SHOT_DROP_SLOTS - n) * DROP_ITEM_BYTES);
            }
        }
        return w.toBytes();
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

    /** C# holiday success {@code 0x93} u8 2 + id + pang. */
    public static byte[] caddieHolidayOk(int caddieId, long pang) {
        return new PacketWriter()
                .opcode(SERVER_REEMPLOY_CADDIE_ACK)
                .u8(CADDIE_HOLIDAY_OK)
                .i32(caddieId)
                .u64(pang)
                .toBytes();
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

    /**
     * C# mascot-message success {@code 0xE2}: u8 4 + id + PStr msg + pang.
     */
    public static byte[] mascotMessageOk(int mascotId, String msg, long pang) {
        return new PacketWriter()
                .opcode(SERVER_CHANGE_MASCOT)
                .u8(MASCOT_MSG_OK)
                .i32(mascotId)
                .pstr(msg == null ? "" : msg)
                .u64(pang)
                .toBytes();
    }

    /** C# Versus {@code timeIsOver} {@code 0x5C} i32 oid. */
    public static byte[] timeout(int oid) {
        return new PacketWriter().opcode(SERVER_TIMEOUT).i32(oid).toBytes();
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

    /**
     * C# open-shop success {@code 0xEB}: u32 1 + nick 22 + uid + count + items.
     */
    public static byte[] shopItemsOk(String nick, int uid, List<PersonalShopItem> items) {
        PacketWriter w = new PacketWriter()
                .opcode(SERVER_SHOP_ITEMS)
                .u32(SHOP_OK)
                .fixedStr(nick == null ? "" : nick, SHOP_NICK_BYTES)
                .u32(uid)
                .u32(items.size());
        for (PersonalShopItem item : items) {
            w.bytes(item.toArray());
        }
        return w.toBytes();
    }

    /**
     * C# view-shop success {@code 0xE6}: u32 1 + nick 22 + PStr name + uid + items.
     */
    public static byte[] shopViewOk(String nick, String name, int uid, List<PersonalShopItem> items) {
        PacketWriter w = new PacketWriter()
                .opcode(SERVER_SHOP_VIEW)
                .u32(SHOP_OK)
                .fixedStr(nick == null ? "" : nick, SHOP_NICK_BYTES)
                .pstr(name == null ? "" : name)
                .u32(uid)
                .u32(items.size());
        for (PersonalShopItem item : items) {
            w.bytes(item.toArray());
        }
        return w.toBytes();
    }

    /**
     * C# buy {@code 0xEC}: u32 1 + u8 remove + u64 pang + {@code PersonalShopItem}
     * + group byte + {@code WarehouseItemEx} 196.
     */
    public static byte[] shopBuyOk(int remove, long pang, PersonalShopItem item, int group, byte[] warehouse) {
        return new PacketWriter()
                .opcode(SERVER_SHOP_BUY)
                .u32(SHOP_OK)
                .u8(remove)
                .u64(pang)
                .bytes(item.toArray())
                .u8(group)
                .bytes(warehouse)
                .toBytes();
    }

    /**
     * C# sold {@code 0xED}: PStr nick + uid + item + i32 (3 empty / 1 remain).
     */
    public static byte[] shopSold(String nick, int uid, PersonalShopItem item, int remain) {
        return new PacketWriter()
                .opcode(SERVER_SHOP_SOLD)
                .pstr(nick == null ? "" : nick)
                .u32(uid)
                .bytes(item.toArray())
                .i32(remain)
                .toBytes();
    }

    public static long shopSellerGain(long cost) {
        return Math.round(cost * SHOP_SALE_RATE);
    }

    /** C# {@code 0x10B}: u32 0 + i64 daily limit. */
    public static byte[] papelShopOk(long limit) {
        return new PacketWriter().opcode(SERVER_PAPEL_SHOP).u32(0).i64(limit).toBytes();
    }

    /**
     * C# Papel-play catch {@code 0x21B} u32 sys. Empty balls write
     * {@code shopSys(0x5900103)}.
     */
    public static byte[] papelPlayFail(int code) {
        return new PacketWriter().opcode(SERVER_PAPEL_PLAY).u32(code).toBytes();
    }

    /**
     * C# {@code pacote216} item update used by Papel: unix + count + rows
     * (type/typeid/id/flag_time/stat 8/qntd + 25 zeros).
     */
    public static byte[] papelAwards(int unix, List<PapelAward> awards) {
        PacketWriter w = new PacketWriter().opcode(SERVER_DAILY_QUEST_STAMP).u32(unix).u32(awards.size());
        for (PapelAward award : awards) {
            w.u8(award.type())
                    .u32(award.typeid())
                    .i32(award.id())
                    .u32(award.flagTime())
                    .i32(award.qntdAnt())
                    .i32(award.qntdDep())
                    .i32(award.qntd());
            if (award.type() == CHAR_CARD_AWARD_TYPE) {
                writeCharCardTail(w, 0, award.extra(), award.slot());
            } else {
                w.zero(PAPEL_AWARD_PAD);
                if (award.type() == CHAR_MASTERY_AWARD_TYPE) {
                    w.u32(award.extra());
                }
            }
        }
        return w.toBytes();
    }

    /**
     * C# card-equip/remove {@code 0x216}: every row writes {@code stItem.c}
     * (5× i16) + 10 zeros + u32 price + u8 slot. Consume {@code qntd == -1}
     * stores {@code c[0] = 32767}.
     */
    public static byte[] charCardAwards(int unix, List<PapelAward> awards) {
        PacketWriter w = new PacketWriter().opcode(SERVER_DAILY_QUEST_STAMP).u32(unix).u32(awards.size());
        for (PapelAward award : awards) {
            w.u8(award.type())
                    .u32(award.typeid())
                    .i32(award.id())
                    .u32(award.flagTime())
                    .i32(award.qntdAnt())
                    .i32(award.qntdDep())
                    .i32(award.qntd());
            int c0 = award.qntd() == -1 ? CHAR_CARD_CONSUME_C0 : award.qntd();
            writeCharCardTail(w, c0, award.extra(), award.slot());
        }
        return w.toBytes();
    }

    private static void writeCharCardTail(PacketWriter w, int c0, int extra, int slot) {
        w.i16(c0).zero(CHAR_CARD_AWARD_C_REST + CHAR_CARD_AWARD_MID_PAD).u32(extra).u8(slot);
    }

    /**
     * C# stats {@code 0x216}: unix + count 1 + type {@code 0xC9} + ids +
     * 5× u16 PCL + 15 zeros.
     */
    public static byte[] charPclAwards(int unix, int typeid, int id, byte[] pcl) {
        PacketWriter w = new PacketWriter()
                .opcode(SERVER_DAILY_QUEST_STAMP)
                .u32(unix)
                .u32(1)
                .u8(CHAR_STATS_AWARD_TYPE)
                .u32(typeid)
                .i32(id)
                .u32(0)
                .u32(0)
                .u32(0)
                .u32(0);
        for (int i = 0; i < 5; i++) {
            w.u16(pcl[i] & 0xff);
        }
        return w.zero(CHAR_STATS_PCL_PAD).toBytes();
    }

    /** C# {@code 0x26F}/{@code 0x270} success: u32 0 + u32 stat. */
    public static byte[] charStatsOk(int opcode, int stat) {
        return new PacketWriter().opcode(opcode).u32(0).u32(stat).toBytes();
    }

    /** C# {@code 0xFB} after Papel: remain + flag (unlimited is -1 / -3). */
    public static byte[] papelRemain(int remain, int flag) {
        return new PacketWriter().opcode(SERVER_PAPEL_REMAIN).i32(remain).i32(flag).toBytes();
    }

    /**
     * C# Papel play {@code 0x21B}/{@code 0x26C}: u32 0 + i32 coupon + count +
     * balls (color/typeid/id/qntd/tipo) + pang + cookie. Ball warehouse id is
     * 0 in this C# ({@code ctx_papel_shop_ball.item} is never assigned).
     */
    public static byte[] papelPlayOk(int opcode, int couponId, List<PapelBall> balls, long pang, long cookie) {
        PacketWriter w = new PacketWriter().opcode(opcode).u32(0).i32(couponId).u32(balls.size());
        for (PapelBall ball : balls) {
            w.u32(ball.color()).u32(ball.typeid()).u32(ball.id()).u32(ball.qntd()).u32(ball.tipo());
        }
        return w.u64(pang).u64(cookie).toBytes();
    }

    public record PapelBall(int color, int typeid, int id, int qntd, int tipo) {}

    public record PapelAward(
            int type, int typeid, int id, int flagTime, int qntdAnt, int qntdDep, int qntd, int extra, int slot) {
        public PapelAward(int type, int typeid, int id, int flagTime, int qntdAnt, int qntdDep, int qntd) {
            this(type, typeid, id, flagTime, qntdAnt, qntdDep, qntd, 0, 0);
        }

        public PapelAward(int type, int typeid, int id, int flagTime, int qntdAnt, int qntdDep, int qntd, int extra) {
            this(type, typeid, id, flagTime, qntdAnt, qntdDep, qntd, extra, 0);
        }
    }

    /**
     * C# {@code pacote102}: i32 normal + i32 partial + u64 pang + u64 cookie.
     */
    public static byte[] gachaCoupon(int normal, int partial, long pang, long cookie) {
        return new PacketWriter()
                .opcode(SERVER_GACHA_COUPON)
                .i32(normal)
                .i32(partial)
                .u64(pang)
                .u64(cookie)
                .toBytes();
    }

    /**
     * C# gacha catch {@code 0x44} u8 {@code 0xE2} + u32 sys (else {@code 0x5300600}).
     */
    public static byte[] gachaCouponFail(int code) {
        return new PacketWriter().opcode(SERVER_LOGIN_ACK).u8(GACHA_ERR_MARKER).u32(code).toBytes();
    }

    /** C# club-stats catch {@code 0xA5} u8 0. */
    public static byte[] clubStatsFail() {
        return new PacketWriter().opcode(SERVER_CLUB_STATS).u8(CLUB_STATS_ERR).toBytes();
    }

    /**
     * C# intrusion catch {@code 0x113}: u8 6 + u8 sys (CHANNEL low 8 bits, else 1).
     */
    public static byte[] intrusionFail(int sys) {
        return new PacketWriter().opcode(SERVER_INTRUSION).u8(INTRUSION_ERR).u8(sys).toBytes();
    }

    /**
     * C# {@code pacote0FC}: u8 count + {@code ServerInfo.ToArray()} 92-byte rows.
     */
    public static byte[] messengerList(List<byte[]> servers) {
        PacketWriter w = new PacketWriter().opcode(SERVER_MESSENGER_LIST).u8(servers.size());
        for (byte[] server : servers) {
            w.bytes(server);
        }
        return w.toBytes();
    }

    /**
     * C# {@code pacote1AD}: i32 option + PStr key, or i16 0 when the key is empty.
     */
    public static byte[] webAuthKey(int option, String key) {
        PacketWriter w = new PacketWriter().opcode(SERVER_WEB_AUTH_KEY).i32(option);
        if (key == null || key.isEmpty()) {
            w.i16(0);
        } else {
            w.pstr(key);
        }
        return w.toBytes();
    }

    /**
     * C# {@code pacote1D4}: i32 option; PStr key only when option 0 and key non-empty.
     */
    public static byte[] changeGameServer(int option, String key) {
        PacketWriter w = new PacketWriter().opcode(SERVER_CHANGE_GAME_SERVER).i32(option);
        if (option == CHANGE_GS_OK && key != null && !key.isEmpty()) {
            w.pstr(key);
        }
        return w.toBytes();
    }

    /**
     * C# ticket-report catch {@code 0x11A}: i32 -1 + 16 zero date bytes.
     */
    public static byte[] ticketReportFail() {
        return new PacketWriter().opcode(SERVER_TICKET_REPORT).i32(TICKET_REPORT_ERR).zero(16).toBytes();
    }

    /** C# Tiki open {@code 0x1E7} u32 option (0 OK). */
    public static byte[] tikiShop(int option) {
        return new PacketWriter().opcode(SERVER_TIKI_SHOP).u32(option).toBytes();
    }

    /** C# locker access {@code 0x16C} u32. */
    public static byte[] lockerAccess(int code) {
        return new PacketWriter().opcode(SERVER_LOCKER_ACCESS).u32(code).toBytes();
    }

    /** C# locker state {@code 0x170}: u32 0 + u32 isLocker. */
    public static byte[] lockerState(int check) {
        return new PacketWriter().opcode(SERVER_LOCKER_STATE).u32(0).u32(check).toBytes();
    }

    /** C# workshop catch {@code 0x23D} u32 sys. */
    public static byte[] clubWorkshopFail(int code) {
        return new PacketWriter().opcode(SERVER_CLUB_WORKSHOP_LEVEL).u32(code).toBytes();
    }

    /**
     * C# lucky-pouch catch {@code 0x129}: u8 1 + 12 zero bytes.
     */
    public static byte[] luckyPouchFail() {
        return new PacketWriter().opcode(SERVER_LUCKY_POUCH).u8(LUCKY_POUCH_ERR).zero(12).toBytes();
    }

    /**
     * C# tutorial catch {@code 0x44} u8 {@code 0xE2} + u32 sys (same marker as gacha).
     */
    public static byte[] tutorialFail(int code) {
        return new PacketWriter().opcode(SERVER_LOGIN_ACK).u8(GACHA_ERR_MARKER).u32(code).toBytes();
    }

    /** C# Tiki points {@code 0x1E8}: u32 option + u32 pts. */
    public static byte[] tikiPoints(int option, int pts) {
        return new PacketWriter().opcode(SERVER_TIKI_POINTS).u32(option).u32(pts).toBytes();
    }

    /** C# Tiki exchange fail {@code 0x1E9}/{@code 0x1EA} u32. */
    public static byte[] tikiExchangeFail(int opcode, int code) {
        return new PacketWriter().opcode(opcode).u32(code).toBytes();
    }

    /** C# workshop confirm/cancel/rank catch u32 sys. */
    public static byte[] clubWorkshopOpcodeFail(int opcode, int code) {
        return new PacketWriter().opcode(opcode).u32(code).toBytes();
    }

    /** C# item-buff catch {@code 0x181} u32 sys. */
    public static byte[] itemBuffFail(int code) {
        return new PacketWriter().opcode(SERVER_ITEM_BUFF).u32(code).toBytes();
    }

    /** C# comet-refill catch {@code 0x197}: u8 0 + 10 zeros. */
    public static byte[] cometRefillFail() {
        return new PacketWriter().opcode(SERVER_COMET_REFILL).u8(0).zero(10).toBytes();
    }

    /** C# mail-box catch {@code 0x19D} u32 sys. */
    public static byte[] boxMailFail(int code) {
        return new PacketWriter().opcode(SERVER_BOX_MAIL).u32(code).toBytes();
    }

    /**
     * C# locker item page {@code 0x16D}: u16 pages + u16 page + u8 count + rows.
     */
    public static byte[] lockerItems(int pages, int page, int count) {
        return new PacketWriter()
                .opcode(SERVER_LOCKER_ITEMS)
                .u16(pages)
                .u16(page)
                .u8(count)
                .toBytes();
    }

    /** C# locker item catch {@code 0x16D} + 5 zeros. */
    public static byte[] lockerItemsFail() {
        return new PacketWriter().opcode(SERVER_LOCKER_ITEMS).zero(5).toBytes();
    }

    /** C# locker pang {@code 0x172} u64. */
    public static byte[] lockerPang(long pang) {
        return new PacketWriter().opcode(SERVER_LOCKER_PANG).u64(pang).toBytes();
    }

    /**
     * C# My Room check {@code 0x12B}: u32 option + u32 to_uid. Option 1 also
     * appends {@code MyRoomConfig.ToArray()} (108 bytes); seed never takes that
     * path.
     */
    public static byte[] myRoomCheck(int option, int toUid) {
        return new PacketWriter().opcode(SERVER_MY_ROOM).u32(option).u32(toUid).toBytes();
    }

    /** C# opcode + u32 sys (Dolfini / card / workshop / memorial catch). */
    public static byte[] sysAck(int opcode, int code) {
        return new PacketWriter().opcode(opcode).u32(code).toBytes();
    }

    /** C# rental catch {@code 0x18F}/{@code 0x190} u8 1. */
    public static byte[] rentalFail(int opcode) {
        return new PacketWriter().opcode(opcode).u8(RENTAL_FAIL).toBytes();
    }

    /** C# UCC catch {@code 0x12E} sbyte -1. */
    public static byte[] uccFail() {
        return new PacketWriter().opcode(SERVER_UCC).u8(UCC_FAIL).toBytes();
    }

    /**
     * C# UCC web-key catch {@code 0x153}: u8 1 + u8 1 + u32
     * ({@code DECODE_TYPE} if GAME_SERVER else {@code 0x5100100}).
     */
    public static byte[] uccWebKeyFail(int code) {
        return new PacketWriter()
                .opcode(SERVER_UCC_WEB_KEY)
                .u8(1)
                .u8(1)
                .u32(code)
                .toBytes();
    }

    /**
     * C# {@code pacote24E}: i32 0, i32 3000, i32 0, u8 100, u8 0, u8 10, u8 10.
     */
    public static byte[] workshopEvent() {
        return new PacketWriter()
                .opcode(SERVER_WORKSHOP_EVENT)
                .i32(0)
                .i32(WORKSHOP_EVENT_HOLES)
                .i32(0)
                .u8(WORKSHOP_EVENT_BARRA_MAX)
                .u8(0)
                .u8(WORKSHOP_EVENT_BARRA)
                .u8(WORKSHOP_EVENT_BARRA)
                .toBytes();
    }

    /**
     * C# {@code requestClubWorkShopEventCount}: {@code 0x24B} i32 0 then
     * bytes {@code 1..16}.
     */
    public static byte[] workshopEventCount() {
        PacketWriter w = new PacketWriter().opcode(SERVER_WORKSHOP_EVENT_COUNT).i32(0);
        for (int i = 0; i < WORKSHOP_EVENT_COUNT_SLOTS; i++) {
            w.u8(i + 1);
        }
        return w.toBytes();
    }

    /**
     * C# {@code ShotEndLocationData.ToArray}: 12×f32 + 3×u8 + u32 special +
     * 5×f32 + u32 time (87 bytes).
     */
    public static byte[] shotEndLocation(
            float porcentagem,
            float velX,
            float velY,
            float velZ,
            int option,
            float locX,
            float locY,
            float locZ,
            float windX,
            float windY,
            float windZ,
            float ballX,
            float ballY,
            int specialShot,
            float spin,
            float curve,
            int unknown,
            int taco,
            float powerFactor,
            float powerClub,
            float spinFactor,
            float curveFactor,
            float powerShot,
            int timeHoleSync) {
        return new PacketWriter()
                .f32(porcentagem)
                .f32(velX)
                .f32(velY)
                .f32(velZ)
                .u8(option)
                .f32(locX)
                .f32(locY)
                .f32(locZ)
                .f32(windX)
                .f32(windY)
                .f32(windZ)
                .f32(ballX)
                .f32(ballY)
                .u32(specialShot)
                .f32(spin)
                .f32(curve)
                .u8(unknown)
                .u8(taco)
                .f32(powerFactor)
                .f32(powerClub)
                .f32(spinFactor)
                .f32(curveFactor)
                .f32(powerShot)
                .u32(timeHoleSync)
                .toBytes();
    }

    /** Distinctive 87-byte {@code ShotEndLocationData} for tests. */
    public static byte[] shotEndLocationSample() {
        return shotEndLocation(
                1.0f,
                2.0f, 3.0f, 4.0f,
                5,
                6.0f, 7.0f, 8.0f,
                9.0f, 10.0f, 11.0f,
                12.0f, 13.0f,
                14,
                15.0f, 16.0f,
                17, 18,
                19.0f, 20.0f, 21.0f, 22.0f, 23.0f,
                24);
    }

    /** C# shot-end ack {@code 0x1F7}: i32 oid + u8 hole + location bytes. */
    public static byte[] shotEnd(int oid, int hole, byte[] location) {
        return new PacketWriter()
                .opcode(SERVER_SHOT_END)
                .i32(oid)
                .u8(hole)
                .bytes(location)
                .toBytes();
    }

    /** C# CLIENT {@code 0x12F}: {@code ShotEndLocationData} body. */
    public static byte[] clientShotEnd(byte[] location) {
        return new PacketWriter().opcode(CLIENT_SHOT_END).bytes(location).toBytes();
    }

    /**
     * C# cutin catch {@code 0x18D}: u8 0 + u16 error (1) or GZ disabled (3).
     */
    public static byte[] cutinFail(int code) {
        return new PacketWriter().opcode(SERVER_CUTIN).u8(0).u16(code).toBytes();
    }

    /** C# Chip-in / GZ {@code 0x1F2} empty. */
    public static byte[] gzEndGame() {
        return new PacketWriter().opcode(SERVER_GZ_END_GAME).toBytes();
    }

    /**
     * C# CLIENT {@code 0xE5} {@code stActiveCutin}: uid + tipo + opt + char +
     * active.
     */
    public static byte[] clientCutin(int uid, int tipo, int opt, int charTypeid, int active) {
        return new PacketWriter()
                .opcode(CLIENT_CUTIN)
                .u32(uid)
                .u32(tipo)
                .u16(opt)
                .u32(charTypeid)
                .u8(active)
                .toBytes();
    }

    /** C# Versus marker {@code 0x1F8}: i32 oid + 3× f32. */
    public static byte[] markerOnCourse(int oid, float x, float y, float z) {
        return new PacketWriter()
                .opcode(SERVER_MARKER)
                .i32(oid)
                .f32(x)
                .f32(y)
                .f32(z)
                .toBytes();
    }

    /** C# paws {@code 0x236}: u32 uid. */
    public static byte[] activePaws(int uid) {
        return new PacketWriter().opcode(SERVER_ACTIVE_PAWS).u32(uid).toBytes();
    }

    /**
     * C# wing {@code 0x203}: u32 uid + u32 typeid. Versus broadcasts;
     * Tourney/Practice session-send.
     */
    public static byte[] activeWing(int uid, int typeid) {
        return new PacketWriter().opcode(SERVER_ACTIVE_WING).u32(uid).u32(typeid).toBytes();
    }

    /** C# in-game toggle-assist reject {@code 0x16A} u32 0. */
    public static byte[] assistInGameReject() {
        return new PacketWriter().opcode(SERVER_ASSIST_INGAME).u32(0).toBytes();
    }

    /**
     * C# room-wait toggle-assist OK {@code 0x26A}: u32 0 + typeid + uid.
     */
    public static byte[] toggleAssistOk(int typeid, int uid) {
        return new PacketWriter()
                .opcode(SERVER_TOGGLE_ASSIST)
                .u32(0)
                .u32(typeid)
                .u32(uid)
                .toBytes();
    }

    /** C# toggle-assist catch {@code 0x26A} u32 error. */
    public static byte[] toggleAssistFail(int code) {
        return new PacketWriter().opcode(SERVER_TOGGLE_ASSIST).u32(code).toBytes();
    }

    /**
     * C# assist-green OK {@code 0x26B}: u32 0 + typeid + uid (session-send).
     */
    public static byte[] assistGreenOk(int typeid, int uid) {
        return new PacketWriter()
                .opcode(SERVER_ASSIST_GREEN)
                .u32(0)
                .u32(typeid)
                .u32(uid)
                .toBytes();
    }

    /** C# assist-green catch {@code 0x26B} u32 error (full GAME code). */
    public static byte[] assistGreenFail(int code) {
        return new PacketWriter().opcode(SERVER_ASSIST_GREEN).u32(code).toBytes();
    }

    /**
     * C# ring {@code 0x237}: u32 0 + uid + typeid + u8 efeito. Versus
     * broadcasts; Tourney session-send.
     */
    public static byte[] activeRingOk(int uid, int typeid, int efeito) {
        return new PacketWriter()
                .opcode(SERVER_ACTIVE_RING)
                .u32(0)
                .u32(uid)
                .u32(typeid)
                .u8(efeito)
                .toBytes();
    }

    /** C# ring catch {@code 0x237} u32 error. */
    public static byte[] activeRingFail(int code) {
        return new PacketWriter().opcode(SERVER_ACTIVE_RING).u32(code).toBytes();
    }

    /**
     * C# glove {@code 0x265}: u32 0 + typeid + uid. Versus broadcasts;
     * Tourney session-send.
     */
    public static byte[] activeGloveOk(int typeid, int uid) {
        return new PacketWriter()
                .opcode(SERVER_ACTIVE_GLOVE)
                .u32(0)
                .u32(typeid)
                .u32(uid)
                .toBytes();
    }

    /** C# glove catch {@code 0x265} u32 error. */
    public static byte[] activeGloveFail(int code) {
        return new PacketWriter().opcode(SERVER_ACTIVE_GLOVE).u32(code).toBytes();
    }

    /**
     * C# earcuff {@code 0x24C}: u32 0 + typeid + uid + u8 angle + f32.
     */
    public static byte[] activeEarcuffOk(int typeid, int uid, int angle, float xPoint) {
        return new PacketWriter()
                .opcode(SERVER_ACTIVE_EARCUFF)
                .u32(0)
                .u32(typeid)
                .u32(uid)
                .u8(angle)
                .f32(xPoint)
                .toBytes();
    }

    /** C# earcuff catch {@code 0x24C} u32 error. */
    public static byte[] activeEarcuffFail(int code) {
        return new PacketWriter().opcode(SERVER_ACTIVE_EARCUFF).u32(code).toBytes();
    }

    /**
     * C# ring-ground {@code 0x266}: u32 0 + efeito + 2× ring + option + uid.
     * Versus and Tourney session-send.
     */
    public static byte[] activeRingGroundOk(int efeito, int ring0, int ring1, int option, int uid) {
        return new PacketWriter()
                .opcode(SERVER_ACTIVE_RING_GROUND)
                .u32(0)
                .u32(efeito)
                .u32(ring0)
                .u32(ring1)
                .u32(option)
                .u32(uid)
                .toBytes();
    }

    /** C# ring-ground catch {@code 0x266} u32 error. */
    public static byte[] activeRingGroundFail(int code) {
        return new PacketWriter().opcode(SERVER_ACTIVE_RING_GROUND).u32(code).toBytes();
    }

    /** C# rainbow paws {@code 0x27E} / power {@code 0x27F} / set {@code 0x281}: u32 uid. */
    public static byte[] ringUidAck(int opcode, int uid) {
        return new PacketWriter().opcode(opcode).u32(uid).toBytes();
    }

    /**
     * C# miracle {@code 0x280}: u32 0 + typeid + uid. Versus broadcasts;
     * Tourney session-send.
     */
    public static byte[] ringMiracleOk(int typeid, int uid) {
        return new PacketWriter()
                .opcode(SERVER_RING_MIRACLE)
                .u32(0)
                .u32(typeid)
                .u32(uid)
                .toBytes();
    }

    /** C# miracle catch {@code 0x280} u32 error. */
    public static byte[] ringMiracleFail(int code) {
        return new PacketWriter().opcode(SERVER_RING_MIRACLE).u32(code).toBytes();
    }

    /** True when {@code ids} contains {@code typeid}. */
    public static boolean hasTypeid(int[] ids, int typeid) {
        if (ids == null) {
            return false;
        }
        for (int id : ids) {
            if (id == typeid) {
                return true;
            }
        }
        return false;
    }

    /** C# {@code leaveRoomGrandPrix} {@code 0x254}: u32 0 + i16 -1. */
    public static byte[] gpExitRoomAck() {
        return new PacketWriter().opcode(SERVER_GP_EXIT_ROOM).u32(0).i16(-1).toBytes();
    }

    /**
     * C# GP lobby OK {@code 0x250}: u32 0 + countBit + typeids ({@code i+1})
     * + v_gpc count 0 + f32 avg score.
     */
    public static byte[] gpLobbyOk(int grandPrixEvent, float avgScore) {
        PacketWriter w = new PacketWriter().opcode(SERVER_GP_LOBBY).u32(0);
        int count = 0;
        for (int i = 0; i < 16; i++) {
            if (((grandPrixEvent >> i) & 1) == 1) {
                count++;
            }
        }
        w.u32(count);
        for (int i = 0; i < 16; i++) {
            if (((grandPrixEvent >> i) & 1) == 1) {
                w.u32(i + 1);
            }
        }
        w.u32(0);
        w.f32(avgScore);
        return w.toBytes();
    }

    /** C# My Room enter {@code 0x168} + {@code PlayerRoomInfoEx.ToArrayEx}. */
    public static byte[] myRoomCharacter(PlayerRoomInfo pri) {
        return new PacketWriter()
                .opcode(SERVER_MY_ROOM_CHAR)
                .bytes(pri.toArrayEx())
                .toBytes();
    }

    /** C# My Room posters {@code 0x12D}: u32 option + u16 count. */
    public static byte[] myRoomPosters(int option, int count) {
        return new PacketWriter()
                .opcode(SERVER_MY_ROOM_POSTERS)
                .u32(option)
                .u16(count)
                .toBytes();
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
     * C# {@code UtilChat.FixColor}: {@code \c0xff00ff00\c} + text. Empty text is
     * returned unchanged.
     */
    public static String chatColor(String hex, String text) {
        if (text == null || text.isEmpty()) {
            return text == null ? "" : text;
        }
        return "\\c" + hex + "\\c" + text;
    }

    /**
     * C# {@code requestChat} GM spy nick: {@code \1[Channel=name, \1ROOM=n]} then
     * insert {@code \1} after the first space (channel names like
     * {@code Channel (Rookies)}).
     */
    public static String gmChatSpyFrom(String channelName, int roomNumero) {
        String from = "\\1[Channel=" + (channelName == null ? "" : channelName)
                + ", \\1ROOM=" + (roomNumero & 0xffff) + "]";
        int index = from.indexOf(' ');
        if (index >= 0) {
            from = from.substring(0, index) + " \\1" + from.substring(index + 1);
        }
        return from;
    }

    /** C# {@code \\5} + nick + {@code : '} + msg + {@code '}. */
    public static String gmChatSpyMsg(String nick, String msg) {
        return "\\5" + (nick == null ? "" : nick) + ": '" + (msg == null ? "" : msg) + "'";
    }

    /** C# {@code \\5} + from + {@code >} + to + {@code : '} + msg + {@code '}. */
    public static String gmPmSpyMsg(String from, String to, String msg) {
        return "\\5" + (from == null ? "" : from) + ">" + (to == null ? "" : to)
                + ": '" + (msg == null ? "" : msg) + "'";
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

    /**
     * C# {@code pacote040} {@code CHAT_REFUSE_WHISPER}: option + nick, no msg.
     */
    public static byte[] chatRefuseWhisper(String nick) {
        return new PacketWriter()
                .opcode(SERVER_CHAT)
                .u8(CHAT_REFUSE_WHISPER)
                .pstr(nick == null ? "" : nick)
                .toBytes();
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

    /**
     * C# {@code getItemGroupIdentify}: {@code (typeid & 0xFC000000) >> 26}.
     * Java uses unsigned {@code >>>} so the high bits stay in range 0–63.
     */
    public static int itemGroupIdentify(int typeid) {
        return (typeid & 0xFC000000) >>> 26;
    }

    /**
     * C# {@code getItemSubGroupIdentify22}: {@code (typeid & ~0xFC000000) >> 22}.
     */
    public static int itemSubGroupIdentify22(int typeid) {
        return (typeid & ~0xFC000000) >>> 22;
    }

    /**
     * C# {@code TzLocalUnixToUnixUTC}: {@code FromUnixTimeSeconds} is already UTC,
     * so {@code ToUniversalTime} is a no-op.
     */
    public static int tzLocalUnixToUnixUtc(int localUnix) {
        return localUnix;
    }

    public static int unixNow() {
        return (int) java.time.Instant.now().getEpochSecond();
    }

    /**
     * C# daily-quest {@code 0x216}: unix + count (empty path writes 0).
     */
    public static byte[] dailyQuestStamp(int unix, int count) {
        return new PacketWriter().opcode(SERVER_DAILY_QUEST_STAMP).i32(unix).i32(count).toBytes();
    }

    /**
     * C# {@code pacote225}: option; if 0: current/accept UTC unix, count, 3×typeid,
     * delete count + ids.
     */
    public static byte[] dailyQuestInfo(
            int option, int currentDate, int acceptDate, int count, int[] typeids, int[] deleted) {
        PacketWriter w = new PacketWriter().opcode(SERVER_DAILY_QUEST_INFO).i32(option);
        if (option == 0) {
            w.u32(tzLocalUnixToUnixUtc(currentDate)).u32(tzLocalUnixToUnixUtc(acceptDate)).u32(count);
            for (int i = 0; i < DAILY_QUEST_TYPEID_COUNT; i++) {
                w.u32(typeids != null && i < typeids.length ? typeids[i] : 0);
            }
            int n = deleted == null ? 0 : deleted.length;
            w.i32(n);
            if (deleted != null) {
                for (int id : deleted) {
                    w.i32(id);
                }
            }
        }
        return w.toBytes();
    }

    /**
     * C# {@code pacote226(empty, 1)}: option 1 + count 0.
     */
    public static byte[] dailyQuestAcceptFail() {
        return new PacketWriter()
                .opcode(SERVER_DAILY_QUEST_ACCEPT)
                .i32(DAILY_QUEST_ACCEPT_FAIL)
                .i32(0)
                .toBytes();
    }

    /**
     * C# {@code pacote227}: always option then count (0 when empty).
     */
    public static byte[] dailyQuestRewardFail() {
        return new PacketWriter()
                .opcode(SERVER_DAILY_QUEST_REWARD)
                .i32(DAILY_QUEST_REWARD_FAIL)
                .i32(0)
                .toBytes();
    }

    /**
     * C# {@code pacote228(empty, 1)}: option 1 only.
     */
    public static byte[] dailyQuestLeaveFail() {
        return new PacketWriter().opcode(SERVER_DAILY_QUEST_LEAVE).i32(DAILY_QUEST_LEAVE_FAIL).toBytes();
    }

    /** C# {@code pacote22C(option)}. */
    public static byte[] achievementGui(int option) {
        return new PacketWriter().opcode(SERVER_ACHIEVEMENT_GUI).i32(option).toBytes();
    }

    /** C# delete-item catch: {@code 0xC5} sbyte -1. */
    public static byte[] deleteItemFail() {
        return new PacketWriter().opcode(SERVER_DELETE_ITEM).u8(DELETE_ITEM_FAIL).toBytes();
    }

    /** C# Cadie {@code 0x22F} u32 error. */
    public static byte[] cadieFail(int code) {
        return new PacketWriter().opcode(SERVER_CADIE).u32(code).toBytes();
    }

    /**
     * C# Cadie success {@code 0x22F}: u32 0 + seq + count 1 + typeid/id/qntd/qntd_dep/flag_time.
     */
    public static byte[] cadieOk(int seq, int typeid, int id, int qntd, int qntdDep, int flagTime) {
        return new PacketWriter()
                .opcode(SERVER_CADIE)
                .u32(0)
                .u32(seq)
                .u32(1)
                .u32(typeid)
                .i32(id)
                .i32(qntd)
                .i32(qntdDep)
                .u32(flagTime)
                .toBytes();
    }

    /** C# Lolo {@code 0x22A} u32 error. */
    public static byte[] loloFail(int code) {
        return new PacketWriter().opcode(SERVER_LOLO).u32(code).toBytes();
    }

    /** C# Lolo {@code 0x229} u32 card tipo. */
    public static byte[] loloTipo(int tipo) {
        return new PacketWriter().opcode(SERVER_LOLO_TIPO).u32(tipo).toBytes();
    }

    /** C# Lolo success {@code 0x22A} u32 0 + u32 typeid. */
    public static byte[] loloOk(int typeid) {
        return new PacketWriter().opcode(SERVER_LOLO).u32(0).u32(typeid).toBytes();
    }

    /**
     * C# Lolo pang per fused card: NORMAL 1000 / RARE 2000 / SUPER_RARE 5000 /
     * else 1000. Secret is rejected before this runs.
     */
    public static int loloPang(int rarity) {
        if (rarity == CARD_TYPE_RARE) {
            return LOLO_PANG_RARE;
        }
        if (rarity == CARD_TYPE_SUPER_RARE) {
            return LOLO_PANG_SUPER_RARE;
        }
        return LOLO_PANG_NORMAL;
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

    /** C# modes that extend {@code GrandZodiacBase}. */
    public static boolean usesGrandZodiac(int tipo) {
        return tipo == TIPO_GRAND_ZODIAC_INT
                || tipo == TIPO_GRAND_ZODIAC_ADV
                || tipo == TIPO_GRAND_ZODIAC_PRACTICE;
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

    /**
     * C# use-active-item {@code 0x5A}: u32 typeid + i32 seed + i32 oid.
     * Versus and Tourney {@code game_broadcast}.
     */
    public static byte[] activeItem(int typeid, int seed, int oid) {
        return new PacketWriter()
                .opcode(SERVER_ACTIVE_ITEM)
                .u32(typeid)
                .i32(seed)
                .i32(oid)
                .toBytes();
    }

    /** C# CLIENT {@code 0x17}: u32 typeid. */
    public static byte[] clientUseItem(int typeid) {
        return new PacketWriter().opcode(CLIENT_USE_ITEM).u32(typeid).toBytes();
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

    /** C# CLIENT {@code 0x1C} cube/coin body: u8 opt + u8 count + count×(u8 tipo + u32 id). */
    public static byte[] clientShotAckCubes(int opt, int... ids) {
        PacketWriter w = new PacketWriter().opcode(CLIENT_SHOT_ACK).u8(opt).u8(ids.length);
        for (int id : ids) {
            w.u8(0).u32(id);
        }
        return w.toBytes();
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

    public static byte[] clientShopOpenItems(List<PersonalShopItem> items) {
        PacketWriter w = new PacketWriter().opcode(CLIENT_SHOP_OPEN_ITEMS).u32(items.size());
        for (PersonalShopItem item : items) {
            w.bytes(item.toArray());
        }
        return w.toBytes();
    }

    public static byte[] clientShopBuy(int ownerUid) {
        return new PacketWriter().opcode(CLIENT_SHOP_BUY).u32(ownerUid).toBytes();
    }

    public static byte[] clientShopBuy(int ownerUid, PersonalShopItem item) {
        return new PacketWriter()
                .opcode(CLIENT_SHOP_BUY)
                .u32(ownerUid)
                .bytes(item.toArray())
                .toBytes();
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

    /** C# CLIENT {@code 0x64}: u32 typeid + u32 qntd. */
    public static byte[] clientDeleteItem(int typeid, int qntd) {
        return new PacketWriter().opcode(CLIENT_DELETE_ITEM).u32(typeid).u32(qntd).toBytes();
    }

    /** C# CLIENT {@code 0x6B}: i32 caddie id + u8 check. */
    public static byte[] clientCaddieHolidayNotice(int caddieId, int check) {
        return new PacketWriter().opcode(CLIENT_CADDIE_HOLIDAY_NOTICE).i32(caddieId).u8(check).toBytes();
    }

    /** C# CLIENT {@code 0x83}: u8 channel. */
    public static byte[] clientEnterOtherChannel(int channelId) {
        return new PacketWriter().opcode(CLIENT_ENTER_OTHER_CHANNEL).u8(channelId).toBytes();
    }

    /** C# CLIENT {@code 0x88}: GameGuard answer (ignored). */
    public static byte[] clientGameGuard() {
        return new PacketWriter().opcode(CLIENT_GAMEGUARD).toBytes();
    }

    /** C# CLIENT {@code 0xB4}: u8 option + u16 room number (log only). */
    public static byte[] clientInviteRelog(int option, int roomNumber) {
        return new PacketWriter().opcode(CLIENT_INVITE_RELOGIN).u8(option).u16(roomNumber).toBytes();
    }

    /** C# CLIENT {@code 0x141}: wind next-hole (no success reply). */
    public static byte[] clientWindNextHole() {
        return new PacketWriter().opcode(CLIENT_WIND_NEXT_HOLE).toBytes();
    }

    /** C# CLIENT {@code 0x151}: open daily quest (no body). */
    public static byte[] clientDailyQuest() {
        return new PacketWriter().opcode(CLIENT_DAILY_QUEST).toBytes();
    }

    /** C# CLIENT {@code 0x152}/{@code 0x153}/{@code 0x154}: i32 count + ids. */
    public static byte[] clientDailyQuestAction(int opcode, int... questIds) {
        PacketWriter w = new PacketWriter().opcode(opcode).i32(questIds == null ? 0 : questIds.length);
        if (questIds != null) {
            for (int id : questIds) {
                w.i32(id);
            }
        }
        return w.toBytes();
    }

    public static byte[] clientAcceptDailyQuest(int... questIds) {
        return clientDailyQuestAction(CLIENT_ACCEPT_DAILY_QUEST, questIds);
    }

    public static byte[] clientRewardDailyQuest(int... questIds) {
        return clientDailyQuestAction(CLIENT_REWARD_DAILY_QUEST, questIds);
    }

    public static byte[] clientLeaveDailyQuest(int... questIds) {
        return clientDailyQuestAction(CLIENT_LEAVE_DAILY_QUEST, questIds);
    }

    /** C# CLIENT {@code 0x157}: u32 uid. */
    public static byte[] clientAchievement(int uid) {
        return new PacketWriter().opcode(CLIENT_ACHIEVEMENT).u32(uid).toBytes();
    }

    /** C# CLIENT {@code 0x157} truncated (ReadUInt32 throws → {@code pacote22C(1)}). */
    public static byte[] clientAchievementEmpty() {
        return new PacketWriter().opcode(CLIENT_ACHIEVEMENT).toBytes();
    }

    /**
     * C# CLIENT {@code 0x158}: u16 seq + u32 requested + u8 count + count×(u32 typeid + i32 id).
     * Three-arg form omits items (truncated → {@link #CADIE_ERR_IFF}).
     */
    public static byte[] clientCadie(int seq, int requested, int count) {
        return new PacketWriter()
                .opcode(CLIENT_CADIE)
                .u16(seq)
                .u32(requested)
                .u8(count)
                .toBytes();
    }

    public static byte[] clientCadieItems(int seq, int requested, int typeid, int id) {
        return new PacketWriter()
                .opcode(CLIENT_CADIE)
                .u16(seq)
                .u32(requested)
                .u8(1)
                .u32(typeid)
                .i32(id)
                .toBytes();
    }

    /** C# CLIENT {@code 0x155}: u64 pang + 3×u32 typeid. */
    public static byte[] clientLolo(long pang, int t0, int t1, int t2) {
        return new PacketWriter().opcode(CLIENT_LOLO).u64(pang).u32(t0).u32(t1).u32(t2).toBytes();
    }

    /** C# CLIENT {@code 0x8B} empty. */
    public static byte[] clientMessengerList() {
        return new PacketWriter().opcode(CLIENT_REQUEST_MESSENGER_LIST).toBytes();
    }

    /** C# CLIENT {@code 0x9E} empty. */
    public static byte[] clientRefreshGacha() {
        return new PacketWriter().opcode(CLIENT_REFRESH_GACHA).toBytes();
    }

    /** C# CLIENT {@code 0x4B}: u8 opt + u8 stat + i32 item id. */
    public static byte[] clientEnchant(int opt, int stat, int itemId) {
        return new PacketWriter().opcode(CLIENT_ENCHANT).u8(opt).u8(stat).i32(itemId).toBytes();
    }

    /** C# CLIENT {@code 0x9D}: u8 option + u16 room (options 0/1). */
    public static byte[] clientIntrusion(int option, int room) {
        return new PacketWriter().opcode(CLIENT_INTRUSION).u8(option).u16(room).toBytes();
    }

    /** C# CLIENT {@code 0x14B} empty. */
    public static byte[] clientPapelPlay() {
        return new PacketWriter().opcode(CLIENT_PAPEL_PLAY).toBytes();
    }

    /** C# CLIENT {@code 0xA1}: sbyte place. */
    public static byte[] clientWebLink(int place) {
        return new PacketWriter().opcode(CLIENT_WEB_LINK).u8(place).toBytes();
    }

    /** C# CLIENT {@code 0xA2} empty. */
    public static byte[] clientPangInfo() {
        return new PacketWriter().opcode(CLIENT_REQUEST_PANG_INFO).toBytes();
    }

    /** C# CLIENT {@code 0x3E}: u16 room + PStr password. */
    public static byte[] clientJoinGallery(int room, String password) {
        return new PacketWriter()
                .opcode(CLIENT_JOIN_GALLERY)
                .u16(room)
                .pstr(password == null ? "" : password)
                .toBytes();
    }

    /** C# CLIENT {@code 0x8F}: i16 cmd. */
    public static byte[] clientGmCommand(int cmd) {
        return new PacketWriter().opcode(CLIENT_GM_COMMAND).i16(cmd).toBytes();
    }

    /** C# CLIENT {@code 0x8F} {@code CCG_VISIBLE}: i16 3 + u16 visible. */
    public static byte[] clientGmVisible(int visible) {
        return clientGmU16(GM_CMD_VISIBLE, visible);
    }

    /** C# CLIENT {@code 0x8F} {@code CCG_WHISPER}/{@code CCG_CHANNEL}: i16 cmd + u16. */
    public static byte[] clientGmU16(int cmd, int value) {
        return new PacketWriter().opcode(CLIENT_GM_COMMAND).i16(cmd).u16(value).toBytes();
    }

    /** C# CLIENT {@code 0x8F} {@code CCG_CHANGE_WEATHER}: i16 15 + u8 weather. */
    public static byte[] clientGmWeather(int weather) {
        return new PacketWriter().opcode(CLIENT_GM_COMMAND).i16(GM_CMD_WEATHER).u8(weather).toBytes();
    }

    /** C# CLIENT {@code 0x8F} {@code CCG_KICK}: i16 10 + u32 oid + u8 force. */
    public static byte[] clientGmKick(int oid, int force) {
        return new PacketWriter().opcode(CLIENT_GM_COMMAND).i16(GM_CMD_KICK).u32(oid).u8(force).toBytes();
    }

    /** C# CLIENT {@code 0x8F} open/close whisper list: i16 cmd + PStr nick. */
    public static byte[] clientGmWhisperList(int cmd, String nick) {
        return new PacketWriter()
                .opcode(CLIENT_GM_COMMAND)
                .i16(cmd)
                .pstr(nick == null ? "" : nick)
                .toBytes();
    }

    /** C# CLIENT {@code 0x8F} {@code CCG_DISCONNECT}: i16 11 + u32 oid. */
    public static byte[] clientGmDisconnect(int oid) {
        return new PacketWriter().opcode(CLIENT_GM_COMMAND).i16(GM_CMD_DISCONNECT).u32(oid).toBytes();
    }

    /** C# CLIENT {@code 0x8F} {@code CCG_IDENTITY}: i16 16 + i32 cap + PStr nick. */
    public static byte[] clientGmIdentity(int cap, String nick) {
        return new PacketWriter()
                .opcode(CLIENT_GM_COMMAND)
                .i16(GM_CMD_IDENTITY)
                .i32(cap)
                .pstr(nick == null ? "" : nick)
                .toBytes();
    }

    /** C# CLIENT {@code 0x8F} {@code CCG_GIVEITEM}: i16 18 + u32 oid + u32 typeid + u32 qntd. */
    public static byte[] clientGmGiveitem(int oid, int typeid, int qntd) {
        return new PacketWriter()
                .opcode(CLIENT_GM_COMMAND)
                .i16(GM_CMD_GIVEITEM)
                .u32(oid)
                .u32(typeid)
                .u32(qntd)
                .toBytes();
    }

    /** C# CLIENT {@code 0x8F} {@code CCG_GOLDENBELL}: i16 19 + u32 typeid + u32 qntd. */
    public static byte[] clientGmGoldenbell(int typeid, int qntd) {
        return new PacketWriter()
                .opcode(CLIENT_GM_COMMAND)
                .i16(GM_CMD_GOLDENBELL)
                .u32(typeid)
                .u32(qntd)
                .toBytes();
    }

    /** C# CLIENT {@code 0x8F} {@code CCG_CHANGE_WIND_VERSUS}: i16 14 + u8 wind + u8 degree. */
    public static byte[] clientGmWind(int wind, int degree) {
        return new PacketWriter()
                .opcode(CLIENT_GM_COMMAND)
                .i16(GM_CMD_WIND)
                .u8(wind)
                .u8(degree)
                .toBytes();
    }

    /** C# CLIENT {@code 0x156} empty. */
    public static byte[] clientAutoCommand() {
        return new PacketWriter().opcode(CLIENT_ACTIVE_AUTO_COMMAND).toBytes();
    }

    /** C# CLIENT {@code 0x61} empty (log only). */
    public static byte[] clientRequestKick() {
        return new PacketWriter().opcode(CLIENT_REQUEST_KICK).toBytes();
    }

    /** C# CLIENT {@code 0xFB} empty. */
    public static byte[] clientWebAuthKey() {
        return new PacketWriter().opcode(CLIENT_WEB_AUTH_KEY).toBytes();
    }

    /** C# CLIENT {@code 0x119}: u32 server uid. */
    public static byte[] clientChangeGameServer(int serverUid) {
        return new PacketWriter().opcode(CLIENT_CHANGE_GAME_SERVER).u32(serverUid).toBytes();
    }

    /** C# CLIENT {@code 0xAB}: i32 item id + i32 scroll id. */
    public static byte[] clientOpenTicketReport(int itemId, int scrollId) {
        return new PacketWriter().opcode(CLIENT_OPEN_TICKET_REPORT).i32(itemId).i32(scrollId).toBytes();
    }

    /** C# CLIENT {@code 0x126} empty. */
    public static byte[] clientTikiShop() {
        return new PacketWriter().opcode(CLIENT_TIKI_SHOP).toBytes();
    }

    /** C# CLIENT {@code 0xCC}: PStr pass. */
    public static byte[] clientLockerAccess(String pass) {
        return new PacketWriter().opcode(CLIENT_LOCKER_ACCESS).pstr(pass == null ? "" : pass).toBytes();
    }

    /** C# CLIENT {@code 0xD3} empty. */
    public static byte[] clientLockerState() {
        return new PacketWriter().opcode(CLIENT_LOCKER_STATE).toBytes();
    }

    /** C# CLIENT {@code 0x164}: u32 typeid + u16 qntd + i32 clubset id. */
    public static byte[] clientClubWorkshopLevel(int typeid, int qntd, int clubsetId) {
        return new PacketWriter()
                .opcode(CLIENT_CLUB_WORKSHOP_LEVEL)
                .u32(typeid)
                .u16(qntd)
                .i32(clubsetId)
                .toBytes();
    }

    /** C# CLIENT {@code 0xB2}: u32 box typeid. */
    public static byte[] clientLuckyPouch(int typeid) {
        return new PacketWriter().opcode(CLIENT_OPEN_LUCKY_POUCH).u32(typeid).toBytes();
    }

    /**
     * C# CLIENT {@code 0xAE}: u16 tipo union + u32 value. Byte1 is {@code tipo}.
     */
    public static byte[] clientCompleteQuest(int tipo, int value) {
        return new PacketWriter().opcode(CLIENT_COMPLETE_QUEST).u8(0).u8(tipo).u32(value).toBytes();
    }

    /** C# CLIENT {@code 0xF4} empty. */
    public static byte[] clientHeartbeat() {
        return new PacketWriter().opcode(CLIENT_HEARTBEAT).toBytes();
    }

    /** C# CLIENT {@code 0xC1}: sbyte place. */
    public static byte[] clientUpdatePlace(int place) {
        return new PacketWriter().opcode(CLIENT_UPDATE_PLACE).u8(place).toBytes();
    }

    /** C# CLIENT {@code 0xAA} empty. */
    public static byte[] clientUseTicketReport() {
        return new PacketWriter().opcode(CLIENT_USE_TICKET_REPORT).toBytes();
    }

    /** C# CLIENT {@code 0x15C} empty. */
    public static byte[] clientActivePaws() {
        return new PacketWriter().opcode(CLIENT_ACTIVE_PAWS).toBytes();
    }

    /** C# CLIENT {@code 0x15D} empty. Lobby fail-path. */
    public static byte[] clientActiveRing() {
        return new PacketWriter().opcode(CLIENT_ACTIVE_RING).toBytes();
    }

    /** C# CLIENT {@code 0x15D}: u32 typeid + u32 effect_value + u8 efeito. */
    public static byte[] clientActiveRing(int typeid, int effectValue, int efeito) {
        return new PacketWriter()
                .opcode(CLIENT_ACTIVE_RING)
                .u32(typeid)
                .u32(effectValue)
                .u8(efeito)
                .toBytes();
    }

    /** C# CLIENT {@code 0x171}: u32 typeid + u8 angle + f32 x. */
    public static byte[] clientEarcuff(int typeid, int angle, float xPoint) {
        return new PacketWriter()
                .opcode(CLIENT_EARCUFF)
                .u32(typeid)
                .u8(angle)
                .f32(xPoint)
                .toBytes();
    }

    /** C# CLIENT {@code 0x181}/{@code 0x197}: 4× u32. */
    public static byte[] clientRingPair(int opcode, int efeito, int ring0, int ring1, int option) {
        return new PacketWriter()
                .opcode(opcode)
                .u32(efeito)
                .u32(ring0)
                .u32(ring1)
                .u32(option)
                .toBytes();
    }

    /** C# CLIENT {@code 0x127} empty. */
    public static byte[] clientTikiPoints() {
        return new PacketWriter().opcode(CLIENT_TIKI_POINTS).toBytes();
    }

    /** C# CLIENT {@code 0x128}/{@code 0x129}: u8 count. */
    public static byte[] clientTikiExchange(int opcode, int count) {
        return new PacketWriter().opcode(opcode).u8(count).toBytes();
    }

    /** C# CLIENT {@code 0x165}/{@code 0x166} empty. */
    public static byte[] clientClubWorkshopEmpty(int opcode) {
        return new PacketWriter().opcode(opcode).toBytes();
    }

    /** C# CLIENT {@code 0x167}: u32 typeid + u16 qntd + i32 clubset id. */
    public static byte[] clientClubWorkshopRank(int typeid, int qntd, int clubsetId) {
        return new PacketWriter()
                .opcode(CLIENT_CLUB_WORKSHOP_RANK)
                .u32(typeid)
                .u16(qntd)
                .i32(clubsetId)
                .toBytes();
    }

    /** C# CLIENT {@code 0xD8}: u32 typeid. */
    public static byte[] clientItemBuff(int typeid) {
        return new PacketWriter().opcode(CLIENT_ITEM_BUFF).u32(typeid).toBytes();
    }

    /** C# CLIENT {@code 0xEC}: u32 item + u32 ball. */
    public static byte[] clientCometRefill(int itemTypeid, int ballTypeid) {
        return new PacketWriter().opcode(CLIENT_COMET_REFILL).u32(itemTypeid).u32(ballTypeid).toBytes();
    }

    /** C# CLIENT {@code 0xEF}: u32 box typeid. */
    public static byte[] clientBoxMail(int typeid) {
        return new PacketWriter().opcode(CLIENT_BOX_MAIL).u32(typeid).toBytes();
    }

    /** C# CLIENT {@code 0xCD}: u32 opt + u16 page. */
    public static byte[] clientLockerItems(int opt, int page) {
        return new PacketWriter().opcode(CLIENT_LOCKER_ITEMS).u32(opt).u16(page).toBytes();
    }

    /** C# CLIENT {@code 0xD5} empty. */
    public static byte[] clientLockerPang() {
        return new PacketWriter().opcode(CLIENT_LOCKER_PANG).toBytes();
    }

    /** C# CLIENT {@code 0xDE}: PStr nick. */
    public static byte[] clientRefuseWhisper(String nick) {
        return new PacketWriter().opcode(CLIENT_REFUSE_WHISPER).pstr(nick == null ? "" : nick).toBytes();
    }

    /** C# CLIENT {@code 0x41}: i32 cap + PStr nick. */
    public static byte[] clientIdentity(int cap, String nick) {
        return new PacketWriter().opcode(CLIENT_IDENTITY).i32(cap).pstr(nick == null ? "" : nick).toBytes();
    }

    /** C# CLIENT {@code 0xB5}: u32 from_uid + u32 to_uid. */
    public static byte[] clientMyRoom(int fromUid, int toUid) {
        return new PacketWriter().opcode(CLIENT_MY_ROOM).u32(fromUid).u32(toUid).toBytes();
    }

    /** C# CLIENT {@code 0xBD}: u32 card typeid. */
    public static byte[] clientUseCard(int typeid) {
        return new PacketWriter().opcode(CLIENT_USE_CARD).u32(typeid).toBytes();
    }

    /** C# CLIENT {@code 0xCA}: u32 typeid + i32 id. */
    public static byte[] clientOpenCardPack(int typeid, int id) {
        return new PacketWriter().opcode(CLIENT_OPEN_CARD_PACK).u32(typeid).i32(id).toBytes();
    }

    /** C# CLIENT {@code 0xCE}/{@code 0xCF}: u8 count (0 skips {@code ToRead}). */
    public static byte[] clientLockerCount(int opcode, int count) {
        return new PacketWriter().opcode(opcode).u8(count).toBytes();
    }

    /** C# CLIENT {@code 0xD0}: PStr pass. */
    public static byte[] clientLockerMakePass(String pass) {
        return new PacketWriter().opcode(CLIENT_LOCKER_MAKE_PASS).pstr(pass == null ? "" : pass).toBytes();
    }

    /** C# CLIENT {@code 0xD1}: PStr old + PStr neu. */
    public static byte[] clientLockerChangePass(String oldPass, String newPass) {
        return new PacketWriter()
                .opcode(CLIENT_LOCKER_CHANGE_PASS)
                .pstr(oldPass == null ? "" : oldPass)
                .pstr(newPass == null ? "" : newPass)
                .toBytes();
    }

    /** C# CLIENT {@code 0xD2}: u8 locker + PStr pass. */
    public static byte[] clientLockerMode(int locker, String pass) {
        return new PacketWriter()
                .opcode(CLIENT_LOCKER_MODE)
                .u8(locker)
                .pstr(pass == null ? "" : pass)
                .toBytes();
    }

    /** C# CLIENT {@code 0xD4}: u8 opt + u64 pang. */
    public static byte[] clientLockerUpdatePang(int opt, long pang) {
        return new PacketWriter().opcode(CLIENT_LOCKER_UPDATE_PANG).u8(opt).u64(pang).toBytes();
    }

    /** C# CLIENT {@code 0xE6}/{@code 0xE7}: i32 item id. */
    public static byte[] clientRental(int opcode, int itemId) {
        return new PacketWriter().opcode(opcode).i32(itemId).toBytes();
    }

    /** C# CLIENT {@code 0x16B}/{@code 0x16D}: u32 typeid + i32 clubset id. */
    public static byte[] clientWorkshopTypeidClub(int opcode, int typeid, int clubsetId) {
        return new PacketWriter().opcode(opcode).u32(typeid).i32(clubsetId).toBytes();
    }

    /**
     * C# {@code ClubSetWorkShopTransferMasteryPts}: u32 UCIM + i32 src + i32 dst
     * + u32 qntd.
     */
    public static byte[] clientWorkshopTransfer(int typeid, int srcId, int dstId, int qntd) {
        return new PacketWriter()
                .opcode(CLIENT_WORKSHOP_TRANSFER)
                .u32(typeid)
                .i32(srcId)
                .i32(dstId)
                .u32(qntd)
                .toBytes();
    }

    /** C# CLIENT {@code 0x17F}: u32 coin typeid. */
    public static byte[] clientMemorial(int coinTypeid) {
        return new PacketWriter().opcode(CLIENT_MEMORIAL).u32(coinTypeid).toBytes();
    }

    /** C# CLIENT {@code 0xB9}: u8 opt. */
    public static byte[] clientUccOpt(int opt) {
        return new PacketWriter().opcode(CLIENT_UCC).u8(opt).toBytes();
    }

    /**
     * C# CLIENT {@code 0xC9}: u8 opt + u32 uid + u8 seq + i32 item_id.
     */
    public static byte[] clientUccWebKey(int opt, int uid, int seq, int itemId) {
        return new PacketWriter()
                .opcode(CLIENT_UCC_WEB_KEY)
                .u8(opt)
                .u32(uid)
                .u8(seq)
                .i32(itemId)
                .toBytes();
    }

    /** C# CLIENT {@code 0x179}: u32 GP typeid. */
    public static byte[] clientGpEnter(int typeid) {
        return new PacketWriter().opcode(CLIENT_GP_ENTER).u32(typeid).toBytes();
    }

    /** C# CLIENT {@code 0x187}: u32 typeid + i32 id. */
    public static byte[] clientCharMastery(int typeid, int id) {
        return new PacketWriter().opcode(CLIENT_CHAR_MASTERY).u32(typeid).i32(id).toBytes();
    }

    /**
     * C# CLIENT {@code 0x188}/{@code 0x189}: u32 stat + {@code CharacterInfo.ToRead}.
     */
    public static byte[] clientCharStats(int opcode, int stat) {
        return clientCharStats(opcode, stat, null);
    }

    public static byte[] clientCharStats(int opcode, int stat, CharacterInfo character) {
        return new PacketWriter()
                .opcode(opcode)
                .u32(stat)
                .bytes(character == null ? new byte[CHARACTER_INFO_BYTES] : character.toArray())
                .toBytes();
    }

    /** C# CLIENT {@code 0x18A}/{@code 0x18B}/{@code 0x18C}: 5×u32 {@code ToRead}. */
    public static byte[] clientCardEquip(int opcode) {
        return clientCardEquip(opcode, 0, 0, 0, 0, 0);
    }

    public static byte[] clientCardEquip(
            int opcode, int charTypeid, int charId, int cardTypeid, int cardId, int slot) {
        return new PacketWriter()
                .opcode(opcode)
                .u32(charTypeid)
                .i32(charId)
                .u32(cardTypeid)
                .i32(cardId)
                .u32(slot)
                .toBytes();
    }

    /** C# {@code 0x271}/{@code 0x273} success: u32 0 + u32 card typeid. */
    public static byte[] charCardOk(int opcode, int cardTypeid) {
        return new PacketWriter().opcode(opcode).u32(0).u32(cardTypeid).toBytes();
    }

    /** C# CLIENT {@code 0x18D}: u32 count. */
    public static byte[] clientTikiShopCount(int count) {
        return new PacketWriter().opcode(CLIENT_TIKI_SHOP_EXCHANGE).u32(count).toBytes();
    }

    /** C# CLIENT {@code 0xE5}/{@code 0xFE} empty. */
    public static byte[] clientEmpty(int opcode) {
        return new PacketWriter().opcode(opcode).toBytes();
    }

    /** C# CLIENT {@code 0x185}/{@code 0x138}: u32 typeid. */
    public static byte[] clientU32(int opcode, int value) {
        return new PacketWriter().opcode(opcode).u32(value).toBytes();
    }

    /** C# CLIENT {@code 0x12E} marker: 3× f32. */
    public static byte[] clientMarker(float x, float y, float z) {
        return new PacketWriter().opcode(CLIENT_MARKER).f32(x).f32(y).f32(z).toBytes();
    }

    /**
     * C# CLIENT {@code 0x17A}: u8 opt + i16 flag + 16-byte key then
     * {@code leaveRoomGrandPrix}.
     */
    public static byte[] clientGpExitRoom() {
        return new PacketWriter()
                .opcode(CLIENT_GP_EXIT_ROOM)
                .u8(0)
                .i16(-1)
                .zero(16)
                .toBytes();
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
     * C# CLIENT {@code 0x1F} with one {@code BuyItem}. Level &lt; Beginner E →
     * {@code 0x6A} code 1. Catalog miss → 6. Success charges pang and mails.
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

    public static PersonalShopItem readPersonalShopItem(PacketReader reader) {
        PersonalShopItem item = new PersonalShopItem();
        item.index = reader.u32();
        item.typeid = reader.u32();
        item.id = reader.i32();
        item.qntd = reader.i32();
        reader.readBytes(3);
        item.pang = reader.u64();
        reader.u32();
        for (int i = 0; i < 5; i++) {
            item.c[i] = (short) reader.u16();
        }
        reader.u16();
        reader.fixedStr(9);
        reader.i16();
        reader.u8();
        reader.readBytes(16 + 16 + 16);
        reader.u16();
        reader.u16();
        reader.u16();
        reader.fixedStr(41);
        reader.fixedStr(22);
        return item;
    }

    /**
     * C# {@code PersonalShopItem}: u32 index + {@code TradeItem} 168.
     */
    public static final class PersonalShopItem {
        public int index;
        public int typeid;
        public int id;
        public int qntd;
        public long pang;
        public short[] c = new short[5];

        public PersonalShopItem copy() {
            PersonalShopItem out = new PersonalShopItem();
            out.index = index;
            out.typeid = typeid;
            out.id = id;
            out.qntd = qntd;
            out.pang = pang;
            out.c = c.clone();
            return out;
        }

        public byte[] toArray() {
            PacketWriter w = new PacketWriter();
            w.u32(index);
            w.u32(typeid);
            w.i32(id);
            w.i32(qntd);
            w.zero(3);
            w.u64(pang);
            w.u32(0);
            for (short v : c) {
                w.u16(v);
            }
            w.u16(0);
            w.zero(9);
            w.i16(0);
            w.u8(0);
            w.zero(16 + 16 + 16);
            w.u16(0);
            w.u16(0);
            w.u16(0);
            w.zero(41);
            w.zero(22);
            byte[] body = w.toBytes();
            if (body.length != PERSONAL_SHOP_ITEM_BYTES) {
                throw new IllegalStateException("PersonalShopItem size " + body.length);
            }
            return body;
        }
    }

    public record BuyItem(int id, int typeid, int time, int itemType, int qntd, int pang, int cookie) {}

    public record BuyRequest(int option, List<BuyItem> items, int couponId) {}

    public record BoughtItem(int typeid, int id, int time, int flagTime, int qntdDep) {}

    public record HoleInfo(int id, int pin, int course, int numero, int weather, int wind, int degree) {}

    /** C# {@code DropItem} 16-byte {@code ToArray} used in {@code 0xCC}. */
    public record DropItem(int typeid, int course, int hole, int qntd, long type) {}

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
