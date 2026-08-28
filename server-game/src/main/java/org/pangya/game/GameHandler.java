package org.pangya.game;

import org.pangya.db.InventoryRepository;
import org.pangya.db.LoginRepository;
import org.pangya.network.AppConfig;
import org.pangya.network.redis.SessionKeyStore;
import org.pangya.network.session.PlayerContext;
import org.pangya.network.session.Session;
import org.pangya.network.session.SessionManager;
import org.pangya.protocol.game.GamePackets;
import org.pangya.protocol.login.ServerInfo;
import org.pangya.protocol.packet.PacketIo;
import org.pangya.protocol.packet.PacketReader;
import org.pangya.protocol.packet.PacketWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * JP {@code GameServer.requestLogin} + channel enter + {@code Channel.requestMakeRoom}
 * ({@code Room.getInfo().ToArray()}) + start-game flags + Practice leave.
 */
public final class GameHandler {

    private static final Logger log = LoggerFactory.getLogger(GameHandler.class);
    /** C# {@code requestCheckNick} forbidden-character class. */
    private static final Pattern NICK_BAD = Pattern.compile(".*[\\^$&,\\\\?`´~|\"@#¨'%*!].*");

    private final AppConfig config;
    private final LoginRepository repo;
    private final InventoryRepository inventory;
    private final SessionKeyStore redis;
    private final SessionManager sessions;
    private final List<GamePackets.ChannelInfo> channels;
    private final AtomicInteger nextRoom = new AtomicInteger(1);
    private final ConcurrentHashMap<Integer, GameRoom> rooms = new ConcurrentHashMap<>();
    /** C# {@code BroadcastManager} ticker queue; count × {@link GamePackets#TICKER_WAIT_MS}. */
    private final List<String> tickers = new ArrayList<>();
    /** C# {@code PlayerMailBox} / {@code MailBoxManager.sendMessage} in-memory store. */
    private final MailBoxStore mailboxes = new MailBoxStore();
    /** C# {@code Tools.Sanitize} SQL-keyword blacklist (OrdinalIgnoreCase). */
    private static final String[] MAIL_SANITIZE = {
            "--", ";--", "/*", "*/", "@@", "char", "nchar", "varchar", "nvarchar",
            "alter", "begin", "cast", "create", "cursor", "declare", "delete", "drop",
            "exec", "execute", "fetch", "insert", "kill", "open", "select", "sys",
            "sysobjects", "syscolumns", "table", "update"
    };

    public GameHandler(
            AppConfig config,
            LoginRepository repo,
            InventoryRepository inventory,
            SessionKeyStore redis,
            SessionManager sessions,
            List<GamePackets.ChannelInfo> channels) {
        this.config = config;
        this.repo = repo;
        this.inventory = inventory;
        this.redis = redis;
        this.sessions = sessions;
        this.channels = List.copyOf(channels);
    }

    public static List<GamePackets.ChannelInfo> loadChannels(AppConfig config) {
        List<GamePackets.ChannelInfo> out = new ArrayList<>();
        byte id = 0;
        for (Map<String, Object> row : config.list("channels")) {
            GamePackets.ChannelInfo c = new GamePackets.ChannelInfo();
            c.name = config.nestedFrom(row, "name", "Channel");
            c.maxUser = (short) config.nestedIntFrom(row, "maxUser", 500);
            c.id = id++;
            c.flag = config.nestedIntFrom(row, "flag", 0);
            c.flag2 = config.nestedIntFrom(row, "flag2", 0);
            out.add(c);
        }
        if (out.isEmpty()) {
            GamePackets.ChannelInfo fallback = new GamePackets.ChannelInfo();
            fallback.name = "Channel (Rookies)";
            fallback.maxUser = 500;
            fallback.id = 0;
            out.add(fallback);
        }
        return out;
    }

    public void onPacket(Session session, byte[] plaintext) {
        if (plaintext.length < 2) {
            return;
        }
        PacketReader reader = new PacketReader(plaintext);
        int opcode = reader.opcode();
        switch (opcode) {
            case GamePackets.CLIENT_REQUEST_LOGIN -> requestLogin(session, reader);
            case GamePackets.CLIENT_ENTER_CHANNEL -> enterChannel(session, reader);
            case GamePackets.CLIENT_REQUEST_CREATE_ROOM -> createRoom(session, reader);
            case GamePackets.CLIENT_REQUEST_JOIN_ROOM -> joinRoom(session, reader);
            case GamePackets.CLIENT_REQUEST_START_GAME -> startGame(session);
            case GamePackets.CLIENT_EXIT_ROOM -> leaveRoom(session);
            case GamePackets.CLIENT_REQUEST_BANISH -> banish(session, reader);
            case GamePackets.CLIENT_REQUEST_USERINFO_OFFLINE -> requestUserInfoOffline(session, reader);
            case GamePackets.CLIENT_LEAVE_PRACTICE -> leavePractice(session);
            case GamePackets.CLIENT_LOAD_OK -> finishLoadHole(session);
            case GamePackets.CLIENT_HOLE_INFO -> initHole(session, reader);
            case GamePackets.CLIENT_SHOT -> initShot(session);
            case GamePackets.CLIENT_CAMERA -> changeMira(session, reader);
            case GamePackets.CLIENT_CLICK -> changeBarSpace(session, reader);
            case GamePackets.CLIENT_POWER_SHOT -> activePowerShot(session, reader);
            case GamePackets.CLIENT_CLUB -> changeClub(session, reader);
            case GamePackets.CLIENT_USE_ITEM -> { }
            case GamePackets.CLIENT_EMOTICON -> changeTyping(session, reader);
            case GamePackets.CLIENT_DROP -> moveBall(session, reader);
            case GamePackets.CLIENT_TIMECHECK -> startTurnTime(session);
            case GamePackets.CLIENT_LOADING_INFO -> loadPercent(session, reader);
            case GamePackets.CLIENT_TEAMCHAT -> teamChat(session, reader);
            case GamePackets.CLIENT_ALLOW_WHISPER -> allowWhisper(session, reader);
            case GamePackets.CLIENT_REQUEST_SERVER_TIME -> requestServerTime(session);
            case GamePackets.CLIENT_SHOT_RESULT -> syncShot(session, reader);
            case GamePackets.CLIENT_SHOT_ACK -> finishShot(session);
            case GamePackets.CLIENT_REQUEST_EQUIP_ITEM -> equipItem(session, reader);
            case GamePackets.CLIENT_REQUEST_BUY_ITEM -> buyItem(session, reader);
            case GamePackets.CLIENT_REQUEST_GIFT_ITEM -> giftItem(session, reader);
            case GamePackets.CLIENT_LOUNGE_STATE -> loungeState(session);
            case GamePackets.CLIENT_SYNC_ACTIVITY -> playerLocationRoom(session, reader);
            case GamePackets.CLIENT_SLEEP -> changeSleep(session, reader);
            case GamePackets.CLIENT_TEESHOT_READY -> finishCharIntro(session);
            case GamePackets.CLIENT_END_STROKE_GAME -> lastPlayerFinishVersus(session);
            case GamePackets.CLIENT_TEAM_HOLEIN_PANG -> teamFinishHole(session, reader);
            case GamePackets.CLIENT_ANSWER_GOSTOP -> replyContinueVersus(session, reader);
            case GamePackets.CLIENT_REEMPLOY_CADDIE -> payCaddieHoliday(session, reader);
            case GamePackets.CLIENT_REPORT -> reportChat(session);
            case GamePackets.CLIENT_REPORT_ERROR -> reportClientException(session, reader);
            case GamePackets.CLIENT_MSN_REQUEST -> translateSubPacket(session, reader);
            case GamePackets.CLIENT_SHOT_COMMAND -> initShotArrows(session, reader);
            case GamePackets.CLIENT_REPLAY_ONLINE -> activeReplay(session, reader);
            case GamePackets.CLIENT_CHAT_PENALITY -> changeChatBlock(session, reader);
            case GamePackets.CLIENT_NOTICE -> noticeGm(session, reader);
            case GamePackets.CLIENT_DESTROY_ROOM -> destroyRoom(session, reader);
            case GamePackets.CLIENT_SPEED_RATE -> activeBooster(session, reader);
            case GamePackets.CLIENT_ONELINE_REQUEST -> sendTicker(session, reader);
            case GamePackets.CLIENT_ONELINE_QUERY -> queueTicker(session);
            case GamePackets.CLIENT_CHANGE_MASCOT -> changeMascotMessage(session, reader);
            case GamePackets.CLIENT_SHOP_CANCEL -> cancelEditShop(session);
            case GamePackets.CLIENT_SHOP_CLOSE -> closeSaleShop(session);
            case GamePackets.CLIENT_SHOP_OPEN_EDIT -> openEditShop(session);
            case GamePackets.CLIENT_SHOP_VIEW -> viewSaleShop(session, reader);
            case GamePackets.CLIENT_SHOP_CLOSE_VIEW -> closeViewSaleShop(session, reader);
            case GamePackets.CLIENT_SHOP_NAME -> changeSaleShopName(session, reader);
            case GamePackets.CLIENT_SHOP_VISIT -> visitSaleShop(session);
            case GamePackets.CLIENT_SHOP_PANG -> pangSaleShop(session);
            case GamePackets.CLIENT_SHOP_OPEN_ITEMS -> openSaleShopItems(session, reader);
            case GamePackets.CLIENT_SHOP_BUY -> buySaleShop(session, reader);
            case GamePackets.CLIENT_PAPEL_SHOP -> openPapelShop(session);
            case GamePackets.CLIENT_PAPEL_PLAY -> playPapelShop(session);
            case GamePackets.CLIENT_TIKI_SHOP -> openTikiShop(session);
            case GamePackets.CLIENT_TIKI_POINTS -> tikiPoints(session);
            case GamePackets.CLIENT_TIKI_EXCHANGE_TP -> tikiExchangeTp(session, reader);
            case GamePackets.CLIENT_TIKI_EXCHANGE_ITEM -> tikiExchangeItem(session, reader);
            case GamePackets.CLIENT_CHANGE_GAME_SERVER -> changeGameServer(session, reader);
            case GamePackets.CLIENT_ENTER_SHOP -> enterShop(session);
            case GamePackets.CLIENT_OPEN_MAILBOX -> openMailBox(session, reader);
            case GamePackets.CLIENT_OPEN_MAIL -> openMail(session, reader);
            case GamePackets.CLIENT_SEND_MAIL -> sendMail(session, reader);
            case GamePackets.CLIENT_TAKE_MAIL -> takeMail(session, reader);
            case GamePackets.CLIENT_DELETE_MAIL -> deleteMail(session, reader);
            case GamePackets.CLIENT_DELETE_ITEM -> deleteActiveItem(session, reader);
            case GamePackets.CLIENT_CADDIE_HOLIDAY_NOTICE -> caddieHolidayNotice(session, reader);
            case GamePackets.CLIENT_ENTER_OTHER_CHANNEL -> enterOtherChannelAndLobby(session, reader);
            case GamePackets.CLIENT_GAMEGUARD -> { }
            case GamePackets.CLIENT_INVITE_RELOGIN -> { }
            case GamePackets.CLIENT_WIND_NEXT_HOLE -> changeWindNextHole(session);
            case GamePackets.CLIENT_DAILY_QUEST -> dailyQuest(session);
            case GamePackets.CLIENT_ACCEPT_DAILY_QUEST -> acceptDailyQuest(session, reader);
            case GamePackets.CLIENT_REWARD_DAILY_QUEST -> rewardDailyQuest(session, reader);
            case GamePackets.CLIENT_LEAVE_DAILY_QUEST -> leaveDailyQuest(session, reader);
            case GamePackets.CLIENT_LOLO -> loloCardCompose(session, reader);
            case GamePackets.CLIENT_ACTIVE_AUTO_COMMAND -> activeAutoCommand(session);
            case GamePackets.CLIENT_ACHIEVEMENT -> achievementGui(session, reader);
            case GamePackets.CLIENT_CADIE -> cadieExchange(session, reader);
            case GamePackets.CLIENT_ENCHANT -> clubSetStats(session, reader);
            case GamePackets.CLIENT_INTRUSION -> enterGameAfterStarted(session, reader);
            case GamePackets.CLIENT_REFRESH_GACHA -> updateGachaCoupon(session);
            case GamePackets.CLIENT_WEB_LINK -> enterWebLink(session, reader);
            case GamePackets.CLIENT_REQUEST_PANG_INFO -> exitedFromWebGuild(session);
            case GamePackets.CLIENT_JOIN_GALLERY -> enterSpyRoom(session, reader);
            case GamePackets.CLIENT_GM_COMMAND -> commonCmdGm(session, reader);
            case GamePackets.CLIENT_REQUEST_KICK -> { }
            case GamePackets.CLIENT_USE_TICKET_REPORT -> useTicketReport(session);
            case GamePackets.CLIENT_OPEN_TICKET_REPORT -> openTicketReport(session, reader);
            case GamePackets.CLIENT_COMPLETE_QUEST -> makeTutorial(session, reader);
            case GamePackets.CLIENT_OPEN_LUCKY_POUCH -> openLuckyPouch(session, reader);
            case GamePackets.CLIENT_UPDATE_PLACE -> updatePlace(session, reader);
            case GamePackets.CLIENT_LOCKER_ACCESS -> lockerAccess(session, reader);
            case GamePackets.CLIENT_LOCKER_STATE -> lockerState(session);
            case GamePackets.CLIENT_LOCKER_ITEMS -> lockerItems(session, reader);
            case GamePackets.CLIENT_LOCKER_PANG -> lockerPang(session);
            case GamePackets.CLIENT_LOCKER_ADD -> lockerAdd(session, reader);
            case GamePackets.CLIENT_LOCKER_REMOVE -> lockerRemove(session, reader);
            case GamePackets.CLIENT_LOCKER_MAKE_PASS -> lockerMakePass(session, reader);
            case GamePackets.CLIENT_LOCKER_CHANGE_PASS -> lockerChangePass(session, reader);
            case GamePackets.CLIENT_LOCKER_MODE -> lockerMode(session, reader);
            case GamePackets.CLIENT_LOCKER_UPDATE_PANG -> lockerUpdatePang(session, reader);
            case GamePackets.CLIENT_MY_ROOM -> myRoomCheck(session, reader);
            case GamePackets.CLIENT_USE_CARD -> useCardSpecial(session, reader);
            case GamePackets.CLIENT_OPEN_CARD_PACK -> openCardPack(session, reader);
            case GamePackets.CLIENT_EXTEND_RENTAL -> extendRental(session, reader);
            case GamePackets.CLIENT_DELETE_RENTAL -> deleteRental(session, reader);
            case GamePackets.CLIENT_CUTIN -> { }
            case GamePackets.CLIENT_UCC_LOAD -> { }
            case GamePackets.CLIENT_UCC -> handleUcc(session, reader);
            case GamePackets.CLIENT_UCC_WEB_KEY -> uccWebKey(session, reader);
            case GamePackets.CLIENT_ATTENDANCE -> checkAttendance(session);
            case GamePackets.CLIENT_ATTENDANCE_LOGIN -> attendanceLoginCount(session);
            case GamePackets.CLIENT_WORKSHOP_EVENT -> openClubWorkshopEvent(session);
            case GamePackets.CLIENT_GP_LOBBY -> enterLobbyGrandPrix(session);
            case GamePackets.CLIENT_GP_LEAVE -> leaveLobbyGrandPrix(session);
            case GamePackets.CLIENT_GP_ENTER -> enterRoomGrandPrix(session, reader);
            case GamePackets.CLIENT_GP_EXIT_ROOM -> { }
            case GamePackets.CLIENT_GZ_INITIAL -> { }
            case GamePackets.CLIENT_MARKER -> { }
            case GamePackets.CLIENT_SHOT_END -> { }
            case GamePackets.CLIENT_LEAVE_CHIP_IN -> { }
            case GamePackets.CLIENT_GZ_FIRST_HOLE -> { }
            case GamePackets.CLIENT_WING -> { }
            case GamePackets.CLIENT_EARCUFF -> { }
            case GamePackets.CLIENT_GLOVE -> { }
            case GamePackets.CLIENT_RING_GROUND -> { }
            case GamePackets.CLIENT_TOGGLE_ASSIST -> { }
            case GamePackets.CLIENT_ASSIST_GREEN -> { }
            case GamePackets.CLIENT_EVENT_ARIN -> { }
            case GamePackets.CLIENT_ENTER_MY_ROOM -> enterMyRoom(session);
            case GamePackets.CLIENT_FINISH_GAME_CB -> finishGame(session, reader);
            case GamePackets.CLIENT_FINISH_GAME_12C -> finishGame(session, reader);
            case GamePackets.CLIENT_BIG_PAPEL -> playBigPapelShop(session);
            case GamePackets.CLIENT_CHAR_MASTERY -> characterMasteryExpand(session, reader);
            case GamePackets.CLIENT_CHAR_STATS_UP -> characterStatsUp(session, reader);
            case GamePackets.CLIENT_CHAR_STATS_DOWN -> characterStatsDown(session, reader);
            case GamePackets.CLIENT_CHAR_CARD_EQUIP -> characterCardEquip(session, reader);
            case GamePackets.CLIENT_CHAR_CARD_PATCHER -> characterCardPatcher(session, reader);
            case GamePackets.CLIENT_CHAR_CARD_REMOVE -> characterRemoveCard(session, reader);
            case GamePackets.CLIENT_TIKI_SHOP_EXCHANGE -> tikiShopExchange(session, reader);
            case GamePackets.CLIENT_RING_PAWS_RAINBOW -> { }
            case GamePackets.CLIENT_RING_POWER -> { }
            case GamePackets.CLIENT_RING_MIRACLE -> { }
            case GamePackets.CLIENT_RING_PAWS_SET -> { }
            case GamePackets.CLIENT_WORKSHOP_TRANSFORM_CONFIRM -> workshopTransformConfirm(session);
            case GamePackets.CLIENT_WORKSHOP_TRANSFORM_CANCEL -> workshopTransformCancel(session);
            case GamePackets.CLIENT_WORKSHOP_RECOVERY -> workshopRecovery(session, reader);
            case GamePackets.CLIENT_WORKSHOP_TRANSFER -> workshopTransfer(session, reader);
            case GamePackets.CLIENT_CLUBSET_RESET -> clubSetReset(session, reader);
            case GamePackets.CLIENT_MEMORIAL -> playMemorial(session, reader);
            case GamePackets.CLIENT_HEARTBEAT -> { }
            case GamePackets.CLIENT_WEB_AUTH_KEY -> webAuthKey(session);
            case GamePackets.CLIENT_ACTIVE_PAWS -> activePaws(session);
            case GamePackets.CLIENT_ACTIVE_RING -> activeRing(session);
            case GamePackets.CLIENT_CLUB_WORKSHOP_LEVEL -> clubWorkshopLevel(session, reader);
            case GamePackets.CLIENT_CLUB_WORKSHOP_CONFIRM -> clubWorkshopConfirm(session);
            case GamePackets.CLIENT_CLUB_WORKSHOP_CANCEL -> clubWorkshopCancel(session);
            case GamePackets.CLIENT_CLUB_WORKSHOP_RANK -> clubWorkshopRank(session, reader);
            case GamePackets.CLIENT_ITEM_BUFF -> useItemBuff(session, reader);
            case GamePackets.CLIENT_COMET_REFILL -> cometRefill(session, reader);
            case GamePackets.CLIENT_BOX_MAIL -> openBoxMail(session, reader);
            case GamePackets.CLIENT_REFUSE_WHISPER -> refuseWhisper(session, reader);
            case GamePackets.CLIENT_IDENTITY -> execIdentity(session);
            case GamePackets.CLIENT_ENTER_LOBBY -> enterLobby(session);
            case GamePackets.CLIENT_LEAVE_LOBBY -> leaveLobby(session);
            case GamePackets.CLIENT_CHAT -> chat(session, reader);
            case GamePackets.CLIENT_SET_READY -> setReady(session, reader);
            case GamePackets.CLIENT_CHANGE_ROOM_INFO -> changeRoomInfo(session, reader);
            case GamePackets.CLIENT_MY_STATISTICS -> finishGame(session, reader);
            case GamePackets.CLIENT_HOLE_STAT -> finishHoleData(session, reader);
            case GamePackets.CLIENT_PAUSE -> pauseGame(session, reader);
            case GamePackets.CLIENT_LOBBY_USERINFO_CHANGED -> changeLobbyItem(session, reader);
            case GamePackets.CLIENT_REQUEST_USERINFO_CHANGED -> changeRoomItem(session, reader);
            case GamePackets.CLIENT_KEEPALIVE -> { }
            case GamePackets.CLIENT_WHISPER -> whisper(session, reader);
            case GamePackets.CLIENT_REQUEST_CASH -> requestCash(session);
            case GamePackets.CLIENT_REQUEST_USERINFO -> requestPlayerInfo(session, reader);
            case GamePackets.CLIENT_UPDATE_MACRO -> updateMacros(session, reader);
            case GamePackets.CLIENT_REQUEST_SERVER_LIST -> requestServerList(session);
            case GamePackets.CLIENT_REQUEST_MESSENGER_LIST -> requestMessengerList(session);
            case GamePackets.CLIENT_REQUEST_RANK -> requestRank(session);
            case GamePackets.CLIENT_USER_MATCH_HISTORY -> last5Players(session);
            case GamePackets.CLIENT_CHANGE_TEAM -> changeTeam(session, reader);
            case GamePackets.CLIENT_REQUEST_DETAIL_ROOM_INFO -> requestRoomDetail(session, reader);
            case GamePackets.CLIENT_INVITE -> invite(session, reader);
            case GamePackets.CLIENT_CHECK_INVITE -> { }
            default -> log.debug("unhandled game opcode 0x{}", Integer.toHexString(opcode));
        }
    }

    private void requestLogin(Session session, PacketReader reader) {
        try {
            GamePackets.GameLogin data = GamePackets.readLogin(reader);
            PlayerContext pi = session.player();
            pi.id = data.id() == null ? "" : data.id();
            pi.uid = data.uid() & 0xffff_ffffL;

            if (data.packetVersion() == 0) {
                session.send(GamePackets.loginAckU32(GamePackets.ACK_INVALID_VERSION));
                session.setAuthorized(false);
                return;
            }
            if (pi.uid == 0) {
                session.send(GamePackets.loginAckU32(GamePackets.ACK_LOGIN_FAIL));
                session.setAuthorized(false);
                return;
            }
            if (data.clientVersion() == null || data.clientVersion().isEmpty()) {
                session.send(GamePackets.loginAckU32(GamePackets.ACK_INVALID_VERSION));
                session.setAuthorized(false);
                return;
            }
            if (data.authKeyLogin() == null || data.authKeyLogin().isEmpty()) {
                session.send(GamePackets.loginAckU32(GamePackets.ACK_SECURITY_KEY));
                session.setAuthorized(false);
                return;
            }
            if (data.authKeyGame() == null || data.authKeyGame().isEmpty()) {
                session.send(GamePackets.loginAckU32(GamePackets.ACK_INVALID_VERSION));
                session.setAuthorized(false);
                return;
            }
            if (pi.id.isEmpty() || pi.id.length() >= 0x40) {
                session.send(GamePackets.loginAckU32(GamePackets.ACK_INVALID_ID));
                session.setAuthorized(false);
                return;
            }
            if (repo.isBannedIp(session.ip())) {
                session.send(GamePackets.loginAckU32(GamePackets.ACK_GENERIC_ERROR));
                session.setAuthorized(false);
                session.disconnect();
                return;
            }

            var info = repo.playerInfo(pi.uid).orElse(null);
            if (info == null || info.uid() <= 0) {
                session.send(GamePackets.loginAckU32(GamePackets.ACK_INVALID_ID));
                session.setAuthorized(false);
                return;
            }
            if (!Objects.equals(info.id(), pi.id)) {
                session.send(GamePackets.loginAckU32(GamePackets.ACK_INVALID_ID));
                session.setAuthorized(false);
                return;
            }
            pi.nickname = info.nickname();
            pi.capability = info.capability();
            pi.gmVisible = (pi.capability & GamePackets.CAPABILITY_GM) != 0 ? 0 : 1;
            pi.level = info.level();
            pi.idState = info.idState();
            pi.blockTime = info.blockTimeSeconds();

            if (pi.idState != 0) {
                session.send(GamePackets.loginAckU32(GamePackets.ACK_GENERIC_ERROR));
                session.setAuthorized(false);
                session.disconnect();
                return;
            }

            int decryptedVersion = GamePackets.xorPacketVersion(data.packetVersion());
            if (decryptedVersion != config.packetVersion()) {
                log.warn("packet version mismatch uid={} server={} client={}",
                        pi.uid, config.packetVersion(), decryptedVersion);
                session.send(GamePackets.loginAckU32(GamePackets.ACK_INVALID_VERSION));
                session.setAuthorized(false);
                return;
            }

            if (!keyMatches(data.authKeyLogin(), redis.getLoginKey(pi.uid), repo.loadAuthKeyLogin(pi.uid).orElse(null))) {
                session.send(GamePackets.loginAckU32(GamePackets.ACK_SECURITY_KEY));
                session.setAuthorized(false);
                return;
            }
            if (!keyMatches(
                    data.authKeyGame(),
                    redis.getGameKey(pi.uid, config.uid()),
                    repo.loadAuthKeyGame(pi.uid, config.uid()).orElse(null))) {
                session.send(GamePackets.loginAckU32(GamePackets.ACK_SECURITY_KEY));
                session.setAuthorized(false);
                return;
            }

            sessions.disconnectOthersWithUid(pi.uid, session);
            session.setAuthorized(true);
            pi.authKeyLogin = data.authKeyLogin();
            try {
                repo.registerLogonServer(pi.uid, config.uid());
            } catch (RuntimeException e) {
                log.warn("register game logon failed uid={}: {}", pi.uid, e.toString());
            }

            // C# LoginManager case 32 pacote210, then sendCompleteData 0x44…
            session.send(GamePackets.newMail(unreadMailBytes(pi.uid)));
            // JP LoginTask.sendCompleteData: 0x44, chars 0x70, caddies 0x71,
            // warehouse 0x73, mascots 0xE1, equip 0x72, channel 0x4D, then tail.
            session.send(GamePackets.loginOkPrincipal(
                    config.clientVersion(),
                    session.oid(),
                    pi.id,
                    pi.nickname,
                    pi.capability,
                    (int) pi.uid,
                    pi.level,
                    config.property()));
            var warehouse = inventory.warehouse(pi.uid);
            var characters = inventory.characters(pi.uid);
            var caddies = inventory.caddies(pi.uid);
            session.send(GamePackets.characters(characters));
            session.send(GamePackets.caddies(caddies));
            session.send(GamePackets.warehouse(warehouse));
            session.send(GamePackets.mascots(inventory.mascots(pi.uid)));
            session.send(GamePackets.userEquip(inventory.userEquip(pi.uid)));
            session.send(GamePackets.channelList(channels));
            for (byte[] extra : GamePackets.loginDumpTail(
                    (int) pi.uid,
                    inventory.pang(pi.uid),
                    inventory.cookie(pi.uid),
                    pi.level,
                    inventory.cards(pi.uid),
                    inventory.counters(pi.uid),
                    inventory.achievements(pi.uid))) {
                session.send(extra);
            }
            log.info("game login id={} uid={}", pi.id, pi.uid);
        } catch (RuntimeException e) {
            log.warn("game login failed: {}", e.toString());
            session.setAuthorized(false);
            session.send(GamePackets.loginAckU32(GamePackets.ACK_GENERIC_ERROR));
            session.disconnect();
        }
    }

    private void enterChannel(Session session, PacketReader reader) {
        if (!session.authorized() || reader.remaining() < 1) {
            return;
        }
        tryEnterChannel(session, reader.u8());
    }

    /**
     * C# {@code GameService.enterChannel}: {@code 0x4E} option 3 missing / 2 full / 1 OK.
     *
     * @return {@code true} when the player is in the requested channel
     */
    private boolean tryEnterChannel(Session session, int channelId) {
        GamePackets.ChannelInfo found = findChannel(channelId);
        if (found == null) {
            session.send(GamePackets.channelEnter(GamePackets.CHANNEL_NOT_FOUND));
            return false;
        }
        if (found.currUser >= found.maxUser && found.maxUser > 0) {
            session.send(GamePackets.channelEnter(GamePackets.CHANNEL_FULL));
            return false;
        }
        PlayerContext pi = session.player();
        if (pi.channelId == channelId) {
            session.send(GamePackets.channelEnter(GamePackets.CHANNEL_ENTER_OK));
            return true;
        }
        adjustChannelCount(pi.channelId, -1);
        pi.channelId = channelId;
        found.currUser++;
        session.send(GamePackets.channelEnter(GamePackets.CHANNEL_ENTER_OK));
        return true;
    }

    /**
     * C# {@code requestEnterOtherChannelAndLobby}: {@code enterChannel} then
     * {@code enterLobby} (no {@code 0xF5}). Missing/full channel → {@code DisconnectSession}.
     */
    private void enterOtherChannelAndLobby(Session session, PacketReader reader) {
        if (!session.authorized() || reader.remaining() < 1) {
            return;
        }
        if (!tryEnterChannel(session, reader.u8())) {
            session.disconnect();
            return;
        }
        if (session.player().inLobby) {
            return;
        }
        sendLobbyEnter(session, false);
    }

    private void createRoom(Session session, PacketReader reader) {
        if (!session.authorized() || session.player().channelId < 0) {
            session.send(GamePackets.roomCreateFailed(GamePackets.CREATE_ROOM_FAILED));
            return;
        }
        GamePackets.CreateRoom room = GamePackets.readCreateRoom(reader);
        if (room.tipo() < 0 || room.tipo() > GamePackets.TIPO_MAX) {
            session.send(GamePackets.roomCreateFailed(GamePackets.CREATE_ROOM_FAILED));
            return;
        }
        if (room.tipo() == GamePackets.TIPO_PRACTICE
                && (room.maxPlayer() > 1 || room.password() == null || room.password().isEmpty())) {
            session.send(GamePackets.roomCreateFailed(GamePackets.CREATE_ROOM_FAILED));
            return;
        }
        int number = nextRoom.getAndIncrement() & 0xffff;
        PlayerContext pi = session.player();
        GameRoom created = new GameRoom(room, number, (int) pi.uid, config.ratePang(), config.rateExp(), pi.channelId);
        created.addPlayer(session);
        created.putPlayerInfo(session, makePlayerInfo(session, created));
        rooms.put(number, created);
        pi.roomNumber = number;
        pi.inPractice = room.tipo() == GamePackets.TIPO_PRACTICE;
        pi.place = 0;
        sendRoomEnterPackets(session, created);
        sendLobbyRoomInfo(created, GamePackets.ROOM_LIST_ADD);
        sendLobbyPlayerInfo(session, GamePackets.LOBBY_USER_UPDATE);
        log.info("room {} tipo={} uid={}", number, room.tipo(), pi.uid);
    }

    private void joinRoom(Session session, PacketReader reader) {
        if (!session.authorized() || session.player().channelId < 0) {
            session.send(GamePackets.roomCreateFailed(GamePackets.CREATE_ROOM_FAILED));
            return;
        }
        GamePackets.JoinRoom req = GamePackets.readJoinRoom(reader);
        GameRoom room = rooms.get(req.numero() & 0xffff);
        if (room == null || room.inGame) {
            session.send(GamePackets.roomCreateFailed(GamePackets.CREATE_ROOM_FAILED));
            return;
        }
        if (room.info.senhaFlag == 0 && !room.info.password.equals(req.password() == null ? "" : req.password())) {
            session.send(GamePackets.roomCreateFailed(GamePackets.CREATE_ROOM_FAILED));
            return;
        }
        enterExistingRoom(session, room);
    }

    /**
     * C# {@code Room.enter} after password/invite checks: add, {@code pacote04A}/
     * {@code 049}/{@code 048}, lobby room + player update.
     */
    private void enterExistingRoom(Session session, GameRoom room) {
        if (!room.addPlayer(session)) {
            session.send(GamePackets.roomCreateFailed(GamePackets.CREATE_ROOM_FAILED));
            return;
        }
        PlayerContext pi = session.player();
        pi.roomNumber = room.info.numero;
        pi.inPractice = room.tipo == GamePackets.TIPO_PRACTICE;
        pi.place = 0;
        room.putPlayerInfo(session, makePlayerInfo(session, room));
        sendRoomEnterPackets(session, room);
        sendLobbyRoomInfo(room, GamePackets.ROOM_LIST_UPDATE);
        sendLobbyPlayerInfo(session, GamePackets.LOBBY_USER_UPDATE);
    }

    private void startGame(Session session) {
        if (!session.authorized()) {
            return;
        }
        PlayerContext pi = session.player();
        GameRoom room = rooms.get(pi.roomNumber);
        if (room == null) {
            session.send(GamePackets.startGameFailed(GamePackets.START_GAME_NOT_READY));
            return;
        }
        if (room.info.master != (int) pi.uid) {
            session.send(GamePackets.startGameFailed(GamePackets.START_GAME_NOT_READY));
            return;
        }
        if (room.info.numPlayer < 2 && !GamePackets.allowsSoloStart(room.tipo)) {
            session.send(GamePackets.startGameFailed(GamePackets.START_GAME_NOT_READY));
            return;
        }
        if (room.inGame) {
            session.send(GamePackets.startGameFailed(GamePackets.START_GAME_NOT_READY));
            return;
        }
        room.inGame = true;
        room.info.state = 0;
        room.startMillis = System.currentTimeMillis();
        room.course = new GameCourse(room.info);
        room.clearCharIntro();
        room.clearLoadHole();
        room.turnOid = 0;
        room.reported.clear();
        room.broadcast(GamePackets.startGameFlag());
        room.broadcast(GamePackets.startGameFlag2());
        room.broadcast(GamePackets.pangRate(room.info.ratePang));
        if (GamePackets.usesTourneyInitialData(room.tipo)) {
            // C# TourneyBase.sendInitialData: 0x76 then per-player 0x52.
            room.broadcast(GamePackets.gameInitTourney(room.info.tipoShow));
            for (var member : room.snapshot()) {
                member.send(GamePackets.course(room.info, room.course.holes, room.course.seed));
            }
        } else if (GamePackets.usesVersusInitialData(room.tipo)) {
            List<GamePackets.VersusPlayer> dump = new ArrayList<>();
            for (var member : room.snapshot()) {
                dump.add(versusPlayer(member));
            }
            room.broadcast(GamePackets.gameInitVersus(room.info.tipoShow, dump));
            for (var member : room.snapshot()) {
                member.send(GamePackets.course(room.info, room.course.holes, room.course.seed));
                member.send(GamePackets.mascotSeed(room.course.seed));
            }
        }
        log.info("start game room {} tipo={} uid={}", room.info.numero, room.tipo, pi.uid);
    }

    private void initHole(Session session, PacketReader reader) {
        GameRoom room = inGameRoom(session);
        if (room == null || room.course == null) {
            return;
        }
        GamePackets.InitHole hole = GamePackets.readInitHole(reader);
        GamePackets.HoleInfo info = room.course.find(hole.numero());
        if (info == null) {
            log.warn("init hole missing numero={} room={}", hole.numero(), room.info.numero);
            return;
        }
        GameRoom.PlayerShot shot = room.shots.computeIfAbsent(session.oid(), id -> new GameRoom.PlayerShot());
        shot.hole = hole.numero();
        shot.x = hole.teeX();
        shot.z = hole.teeZ();
        if (GamePackets.usesTourneyInitialData(room.tipo)) {
            session.send(GamePackets.weather(info.weather()));
            session.send(GamePackets.wind(info.wind(), 0, info.degree(), 1));
            int elapsed = (int) Math.max(0, System.currentTimeMillis() - room.startMillis);
            session.send(GamePackets.remainTime(elapsed));
        }
    }

    private void finishLoadHole(Session session) {
        GameRoom room = inGameRoom(session);
        if (room == null || room.course == null || !GamePackets.usesVersusInitialData(room.tipo)) {
            return;
        }
        if (!room.markLoadHole(session)) {
            return;
        }
        room.clearLoadHole();
        int oid = room.startHoleTurn();
        GamePackets.HoleInfo info = versusHole(room, oid);
        room.broadcast(GamePackets.weather(info.weather()));
        room.broadcast(GamePackets.wind(info.wind(), 0, info.degree(), 1));
        room.broadcast(GamePackets.holeTurn(oid));
    }

    private static GamePackets.HoleInfo versusHole(GameRoom room, int oid) {
        int numero = 1;
        GameRoom.PlayerShot shot = room.shots.get(oid);
        if (shot != null && shot.hole > 0) {
            numero = shot.hole;
        }
        GamePackets.HoleInfo info = room.course == null ? null : room.course.find(numero);
        if (info != null) {
            return info;
        }
        return new GamePackets.HoleInfo(numero, 0, 0, numero, 0, 0, 0);
    }

    private void initShot(Session session) {
        // C# TourneyBase.requestInitShot stores ShotDataEx and does not reply.
        inGameRoom(session);
    }

    /**
     * VersusBase {@code game_broadcast}; TourneyBase {@code session_send} to the actor.
     */
    private void replyInGame(GameRoom room, Session session, byte[] pkt) {
        if (GamePackets.usesVersusInitialData(room.tipo)) {
            room.broadcast(pkt);
        } else {
            session.send(pkt);
        }
    }

    private void changeMira(Session session, PacketReader reader) {
        GameRoom room = inGameRoom(session);
        if (room == null || reader.remaining() < 4) {
            return;
        }
        replyInGame(room, session, GamePackets.camera(session.oid(), reader.f32()));
    }

    private void changeBarSpace(Session session, PacketReader reader) {
        GameRoom room = inGameRoom(session);
        if (room == null || reader.remaining() < 5) {
            return;
        }
        int state = reader.u8();
        reader.f32();
        GameRoom.PlayerShot shot = room.shots.computeIfAbsent(session.oid(), id -> new GameRoom.PlayerShot());
        shot.barState = state;
        if (!GamePackets.usesVersusInitialData(room.tipo) || state != 0 || shot.turnTempo != 1) {
            return;
        }
        shot.turnTempo = 0;
        shot.timeOuts++;
        room.broadcast(GamePackets.timeout(session.oid()));
    }

    /**
     * C# {@code packet022} / {@code requestStartTurnTime}: Versus {@code startTime}
     * with {@code m_ri.time_vs}. {@code 0x5C} is broadcast when the timer fires.
     */
    private void startTurnTime(Session session) {
        GameRoom room = inGameRoom(session);
        if (room == null || !GamePackets.usesVersusInitialData(room.tipo) || room.info.timeVs <= 0) {
            return;
        }
        int oid = session.oid();
        room.startTurnTimer(room.info.timeVs, () -> onTurnTimeout(room, oid));
    }

    /**
     * C# {@code VersusBase.timeIsOver} always broadcasts {@code 0x5C}; Versus then
     * sets {@code pgi.tempo = 1} and resets it when the bar is still at state 0.
     */
    private void onTurnTimeout(GameRoom room, int oid) {
        if (!room.inGame) {
            return;
        }
        GameRoom.PlayerShot shot = room.shots.computeIfAbsent(oid, id -> new GameRoom.PlayerShot());
        shot.turnTempo = 1;
        if (shot.barState == 0 && room.turnOid == oid) {
            shot.turnTempo = 0;
            shot.timeOuts++;
        }
        room.broadcast(GamePackets.timeout(oid));
    }

    private void activePowerShot(Session session, PacketReader reader) {
        GameRoom room = inGameRoom(session);
        if (room == null || reader.remaining() < 1) {
            return;
        }
        replyInGame(room, session, GamePackets.powerShot(session.oid(), reader.u8()));
    }

    private void changeClub(Session session, PacketReader reader) {
        GameRoom room = inGameRoom(session);
        if (room == null || reader.remaining() < 1) {
            return;
        }
        replyInGame(room, session, GamePackets.club(session.oid(), reader.u8()));
    }

    private void changeTyping(Session session, PacketReader reader) {
        GameRoom room = inGameRoom(session);
        if (room == null || reader.remaining() < 2) {
            return;
        }
        replyInGame(room, session, GamePackets.typing(session.oid(), reader.i16()));
    }

    private void moveBall(Session session, PacketReader reader) {
        GameRoom room = inGameRoom(session);
        if (room == null || reader.remaining() < 12) {
            return;
        }
        replyInGame(room, session, GamePackets.moveBall(reader.f32(), reader.f32(), reader.f32()));
    }

    private void loadPercent(Session session, PacketReader reader) {
        GameRoom room = inGameRoom(session);
        if (room == null || reader.remaining() < 1 || !GamePackets.usesVersusInitialData(room.tipo)) {
            return;
        }
        room.broadcast(GamePackets.loadPercent(session.oid(), reader.u8()));
    }

    private void teamChat(Session session, PacketReader reader) {
        if (!session.authorized() || reader.remaining() < 2) {
            return;
        }
        String msg = reader.pstr();
        if (msg == null || msg.isEmpty()) {
            return;
        }
        GameRoom room = rooms.get(session.player().roomNumber);
        if (room == null || (room.tipo != GamePackets.TIPO_MATCH && room.tipo != GamePackets.TIPO_GUILD_BATTLE)) {
            return;
        }
        GamePackets.PlayerRoomInfo self = room.playerInfo(session);
        if (self == null) {
            return;
        }
        int team = self.stateFlag & GamePackets.PLAYER_TEAM_BIT;
        String nick = session.player().nickname == null ? "" : session.player().nickname;
        byte[] pkt = GamePackets.teamChat(nick, msg);
        for (Session member : room.snapshot()) {
            GamePackets.PlayerRoomInfo info = room.playerInfo(member);
            if (info != null && (info.stateFlag & GamePackets.PLAYER_TEAM_BIT) == team) {
                member.send(pkt);
            }
        }
    }

    private void allowWhisper(Session session, PacketReader reader) {
        if (!session.authorized() || reader.remaining() < 1) {
            return;
        }
        int whisper = reader.u8();
        if (whisper > 1) {
            return;
        }
        session.player().whisper = whisper;
    }

    private void requestServerTime(Session session) {
        if (!session.authorized()) {
            return;
        }
        session.send(GamePackets.serverTime());
    }

    private void syncShot(Session session, PacketReader reader) {
        GameRoom room = inGameRoom(session);
        if (room == null || reader.remaining() != GamePackets.SHOT_SYNC_BYTES) {
            return;
        }
        byte[] encrypted = reader.readBytes(GamePackets.SHOT_SYNC_BYTES);
        GamePackets.ShotSync sync = GamePackets.readShotSync(GamePackets.xorRoomKey(encrypted, room.info.key));
        GameRoom.PlayerShot shot = room.shots.computeIfAbsent(session.oid(), id -> new GameRoom.PlayerShot());
        shot.x = sync.x();
        shot.z = sync.z();
        shot.shotState = sync.shotState();
        shot.tempo = sync.tempo() & 0xffff;
        int hole = shot.hole == 0 ? 1 : shot.hole;
        room.broadcast(GamePackets.syncShot(sync.oid(), hole, shot.x, shot.z, shot.shotState, shot.tempo));
    }

    private void finishShot(Session session) {
        GameRoom room = inGameRoom(session);
        if (room == null) {
            return;
        }
        session.send(GamePackets.endShot(session.oid()));
    }

    /**
     * C# {@code packet006} / {@code requestFinishGame}: {@code UserInfoEx} 265 then
     * Practice {@code finish_game(6)} → {@code 0xCE}/{@code 0x79}/{@code 0x45}/{@code 0x134}/{@code 0xC8}.
     */
    private void finishGame(Session session, PacketReader reader) {
        GameRoom room = inGameRoom(session);
        if (room == null || reader.remaining() < GamePackets.USER_INFO_BYTES) {
            return;
        }
        reader.readBytes(GamePackets.USER_INFO_BYTES);
        sendFinishGameDump(session, room);
        finishGameRoom(room);
    }

    /**
     * C# {@code packet037} / {@code requestLastPlayerFinishVersus}: Versus
     * {@code finish_game(first, 2)} then {@code room.finish_game()}.
     */
    private void lastPlayerFinishVersus(Session session) {
        GameRoom room = inGameRoom(session);
        if (room == null || !GamePackets.usesVersusInitialData(room.tipo)) {
            return;
        }
        sendFinishGameDump(session, room);
        finishGameRoom(room);
    }

    /**
     * C# {@code packet035} / {@code requestTeamFinishHole}: Match stores u16
     * finish state (9 putt / 10 chip-in). {@code GameBase} no-op, no reply.
     */
    private void teamFinishHole(Session session, PacketReader reader) {
        GameRoom room = inGameRoom(session);
        if (room == null) {
            return;
        }
        if (reader.remaining() >= 2) {
            reader.u16();
        }
    }

    /**
     * C# {@code packet036}: u8 0 stops Versus like {@code 0x37}; u8 1 calls
     * {@code changeTurn} → {@code sendPlayerTurn} ({@code 0x5B}+{@code 0x63}).
     * {@code m_player_turn == null} throws and is silent. Last-hole {@code 0x199}
     * needs {@code acerto_hole} and is not invented here.
     */
    private void replyContinueVersus(Session session, PacketReader reader) {
        GameRoom room = inGameRoom(session);
        if (room == null || reader.remaining() < 1
                || !GamePackets.usesVersusInitialData(room.tipo)) {
            return;
        }
        int opt = reader.u8();
        if (opt == GamePackets.CONTINUE_STOP) {
            sendFinishGameDump(session, room);
            finishGameRoom(room);
            return;
        }
        if (opt != GamePackets.CONTINUE_GO || room.turnOid == 0 || room.course == null) {
            return;
        }
        int oid = room.rotateTurn();
        if (oid == 0) {
            return;
        }
        GamePackets.HoleInfo info = versusHole(room, oid);
        room.broadcast(GamePackets.wind(info.wind(), 0, info.degree(), 1));
        room.broadcast(GamePackets.playerTurn(oid));
    }

    /**
     * C# {@code packet039}: SQL {@code iff_caddie.valor_mensal}. Catch always
     * writes {@code 0x93} u8 1.
     */
    private void payCaddieHoliday(Session session, PacketReader reader) {
        if (!session.authorized()) {
            return;
        }
        int caddieId = reader.remaining() >= 4 ? reader.i32() : 0;
        InventoryRepository.CaddieHolidayResult result;
        try {
            result = inventory.payCaddieHoliday(session.player().uid, caddieId);
        } catch (RuntimeException e) {
            log.warn("caddie holiday uid={} failed: {}", session.player().uid, e.toString());
            session.send(GamePackets.caddieHolidayFail());
            return;
        }
        if (result.code() != 0) {
            session.send(GamePackets.caddieHolidayFail());
            return;
        }
        session.send(GamePackets.caddieHolidayOk(result.caddieId(), result.pang()));
    }

    /**
     * C# {@code packet03A}: in-game {@code 0x94} u8 0 first time, 1 if already
     * reported this game.
     */
    private void reportChat(Session session) {
        GameRoom room = inGameRoom(session);
        if (room == null) {
            return;
        }
        long uid = session.player().uid;
        if (room.reported.putIfAbsent(uid, Boolean.TRUE) == null) {
            session.send(GamePackets.reportAck(GamePackets.REPORT_OK));
        } else {
            session.send(GamePackets.reportAck(GamePackets.REPORT_ALREADY));
        }
    }

    /**
     * C# {@code packet04F}: u8 → {@code 0xAC} oid+state. Versus broadcasts;
     * Tourney/Practice {@code session_send}.
     */
    private void changeChatBlock(Session session, PacketReader reader) {
        GameRoom room = inGameRoom(session);
        if (room == null || reader.remaining() < 1) {
            return;
        }
        int block = reader.u8();
        byte[] pkt = GamePackets.chatPenalty(session.oid(), block);
        if (GamePackets.usesVersusInitialData(room.tipo)) {
            room.broadcast(pkt);
        } else {
            session.send(pkt);
        }
    }

    /**
     * C# {@code packet065}: f32 speed → {@code 0xC7}. C# non-premium consumes
     * warehouse TIME_BOOSTER (IFF); without that item the catch is silent.
     * Java skips consume and always replies.
     */
    private void activeBooster(Session session, PacketReader reader) {
        GameRoom room = inGameRoom(session);
        if (room == null || reader.remaining() < 4) {
            return;
        }
        float speed = reader.f32();
        byte[] pkt = GamePackets.speedRate(speed, session.oid());
        if (GamePackets.usesVersusInitialData(room.tipo)) {
            room.broadcast(pkt);
        } else {
            session.send(pkt);
        }
    }

    /**
     * C# {@code packet067}: {@code 0xCA} count + count×30000 ms.
     */
    private void queueTicker(Session session) {
        if (!session.authorized()) {
            return;
        }
        int count;
        synchronized (tickers) {
            count = tickers.size();
        }
        session.send(GamePackets.tickerQueue(count, count * GamePackets.TICKER_WAIT_MS));
    }

    /**
     * C# {@code packet066}: PStr → consume 1 cookie, queue, {@code 0x96}, then
     * {@code 0xC9} to every channel. Empty/funds fail {@code 0x50}.
     */
    private void sendTicker(Session session, PacketReader reader) {
        if (!session.authorized()) {
            return;
        }
        String msg = reader.remaining() >= 2 ? reader.pstr() : "";
        if (msg.isEmpty()) {
            session.send(GamePackets.tickerFail(GamePackets.TICKER_FAIL_GENERIC));
            return;
        }
        long uid = session.player().uid;
        long cookie = inventory.cookie(uid);
        if (cookie < GamePackets.TICKER_COOKIE) {
            session.send(GamePackets.tickerFail(GamePackets.TICKER_FAIL_FUNDS));
            return;
        }
        inventory.setPangCookie(uid, inventory.pang(uid), cookie - GamePackets.TICKER_COOKIE);
        long left = cookie - GamePackets.TICKER_COOKIE;
        synchronized (tickers) {
            tickers.add(msg);
        }
        session.send(GamePackets.cookieBalance(left));
        String nick = session.player().nickname == null ? "" : session.player().nickname;
        broadcastAll(GamePackets.tickerMsg(nick, msg));
    }

    /**
     * C# {@code packet073}: SQL {@code iff_mascot} message. Catch is
     * {@code 0xE2} sbyte -1 + id -1 + empty msg + pang.
     */
    private void changeMascotMessage(Session session, PacketReader reader) {
        if (!session.authorized()) {
            return;
        }
        int mascotId = reader.remaining() >= 4 ? reader.i32() : 0;
        String msg = reader.remaining() >= 2 ? reader.pstr() : "";
        InventoryRepository.MascotMessageResult result;
        try {
            result = inventory.changeMascotMessage(session.player().uid, mascotId, msg);
        } catch (RuntimeException e) {
            log.warn("mascot message uid={} failed: {}", session.player().uid, e.toString());
            session.send(GamePackets.mascotMessageFail(inventory.pang(session.player().uid)));
            return;
        }
        if (result.code() != 0) {
            session.send(GamePackets.mascotMessageFail(result.pang()));
            return;
        }
        session.send(GamePackets.mascotMessageOk(result.mascotId(), result.message(), result.pang()));
    }

    /**
     * C# {@code packet057}: GM broadcasts {@code 0x40} option 7. Others get
     * {@code SendChatNotice("Command no Executed")}.
     */
    private void noticeGm(Session session, PacketReader reader) {
        if (!session.authorized()) {
            return;
        }
        String nick = session.player().nickname == null ? "" : session.player().nickname;
        if ((session.player().capability & GamePackets.CAPABILITY_GM) == 0) {
            session.send(GamePackets.chat(GamePackets.CHAT_NOTICE, nick, "Command no Executed"));
            return;
        }
        String notice = reader.remaining() >= 2 ? reader.pstr() : "";
        if (notice.isEmpty()) {
            session.send(GamePackets.chat(GamePackets.CHAT_NOTICE, nick, "Command no Executed"));
            return;
        }
        broadcastAll(GamePackets.chat(GamePackets.CHAT_NOTICE, nick, notice));
    }

    /**
     * C# {@code packet060}: GM i16 room number, kick everyone. Non-GM notice.
     */
    private void destroyRoom(Session session, PacketReader reader) {
        if (!session.authorized()) {
            return;
        }
        String nick = session.player().nickname == null ? "" : session.player().nickname;
        if ((session.player().capability & GamePackets.CAPABILITY_GM) == 0) {
            session.send(GamePackets.chat(GamePackets.CHAT_NOTICE, nick, "Command no executed!"));
            return;
        }
        if (reader.remaining() < 2) {
            return;
        }
        int numero = reader.i16();
        GameRoom room = rooms.get(numero);
        if (room == null) {
            session.send(GamePackets.chat(GamePackets.CHAT_NOTICE, nick, "Command no executed!"));
            return;
        }
        for (Session member : room.snapshot()) {
            leaveRoom(member);
        }
    }

    private void broadcastAll(byte[] packet) {
        for (Session other : sessions.snapshot()) {
            if (other.authorized()) {
                other.send(packet);
            }
        }
    }

    private GameRoom playerRoom(Session session) {
        if (!session.authorized()) {
            return null;
        }
        int numero = session.player().roomNumber;
        return numero < 0 ? null : rooms.get(numero);
    }

    private void sendShop(Session session, GameRoom.ShopReply reply) {
        if (reply.broadcast) {
            GameRoom room = playerRoom(session);
            if (room != null) {
                room.broadcast(reply.packet);
                return;
            }
        }
        session.send(reply.packet);
    }

    /** C# {@code packet076} {@code openShopToEdit} → {@code 0xE5} broadcast. */
    private void openEditShop(Session session) {
        GameRoom room = playerRoom(session);
        if (room == null) {
            return;
        }
        sendShop(session, room.openEditShop(session));
    }

    /** C# {@code packet074} cancel edit → {@code 0xE3}. */
    private void cancelEditShop(Session session) {
        GameRoom room = playerRoom(session);
        if (room == null) {
            return;
        }
        sendShop(session, room.cancelEditShop(session));
    }

    /** C# {@code packet075} close → {@code 0xE4} ok / {@code 0xE5} fail. */
    private void closeSaleShop(Session session) {
        GameRoom room = playerRoom(session);
        if (room == null) {
            return;
        }
        sendShop(session, room.closeShop(session));
    }

    /** C# {@code packet079} PStr name → {@code 0xE8}. */
    private void changeSaleShopName(Session session, PacketReader reader) {
        GameRoom room = playerRoom(session);
        if (room == null) {
            return;
        }
        String name = reader.remaining() >= 2 ? reader.pstr() : "";
        sendShop(session, room.changeShopName(session, name));
    }

    /** C# {@code packet07A} → {@code 0xE9}. */
    private void visitSaleShop(Session session) {
        GameRoom room = playerRoom(session);
        if (room == null) {
            return;
        }
        session.send(room.visitCountShop(session));
    }

    /** C# {@code packet07B} → {@code 0xEA}. */
    private void pangSaleShop(Session session) {
        GameRoom room = playerRoom(session);
        if (room == null) {
            return;
        }
        session.send(room.pangShop(session));
    }

    /** C# {@code packet077}: u32 owner → {@code 0xE6}. Empty items fail 5200450. */
    private void viewSaleShop(Session session, PacketReader reader) {
        GameRoom room = playerRoom(session);
        if (room == null) {
            return;
        }
        long owner = reader.remaining() >= 4 ? reader.u32Unsigned() : 0;
        session.send(room.viewShop(session, owner));
    }

    /** C# {@code packet078}: u32 owner → {@code 0xE7}. */
    private void closeViewSaleShop(Session session, PacketReader reader) {
        GameRoom room = playerRoom(session);
        if (room == null) {
            return;
        }
        long owner = reader.remaining() >= 4 ? reader.u32Unsigned() : 0;
        session.send(room.closeViewShop(session, owner));
    }

    /**
     * C# {@code packet07C}: u32 count + {@code PersonalShopItem}×count.
     * IFF {@code findCommomItem} / {@code can_send_mail_and_personal_shop} is
     * {@code shop_catalog}. Success {@code 0xEB} u32 1 + nick 22 + uid + items.
     */
    private void openSaleShopItems(Session session, PacketReader reader) {
        GameRoom room = playerRoom(session);
        if (room == null) {
            return;
        }
        int count = reader.remaining() >= 4 ? reader.u32() : 0;
        if (count == 0 || count > 10) {
            session.send(GamePackets.shopItemsFail(GamePackets.shopSys(GamePackets.SHOP_ERR_OPEN_COUNT)));
            return;
        }
        if (room.shops.get(session.player().uid) == null) {
            session.send(GamePackets.shopItemsFail(GamePackets.shopSys(GamePackets.SHOP_ERR_OPEN_NONE)));
            return;
        }
        List<GamePackets.WarehouseItem> warehouse = inventory.warehouse(session.player().uid);
        List<GamePackets.PersonalShopItem> listed = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            if (reader.remaining() < GamePackets.PERSONAL_SHOP_ITEM_BYTES) {
                session.send(GamePackets.shopItemsFail(GamePackets.SHOP_ERR_OPEN_DEFAULT));
                return;
            }
            GamePackets.PersonalShopItem item = GamePackets.readPersonalShopItem(reader);
            if (!personalShopItemListable(warehouse, item)) {
                session.send(GamePackets.shopItemsFail(GamePackets.SHOP_ERR_OPEN_DEFAULT));
                return;
            }
            listed.add(item);
        }
        if (!room.listShopItems(session.player().uid, listed)) {
            session.send(GamePackets.shopItemsFail(GamePackets.shopSys(GamePackets.SHOP_ERR_OPEN_NONE)));
            return;
        }
        session.send(GamePackets.shopItemsOk(session.player().nickname, (int) session.player().uid, listed));
    }

    /**
     * C# {@code PersonalShop.pushItem} + warehouse qntd. IFF stand-in is
     * {@code shop_catalog}; Java lists {@code IFF_GROUP.ITEM} only.
     */
    private boolean personalShopItemListable(
            List<GamePackets.WarehouseItem> warehouse, GamePackets.PersonalShopItem item) {
        if (item.typeid == 0 || item.qntd <= 0) {
            return false;
        }
        if (GamePackets.itemGroupIdentify(item.typeid) != GamePackets.IFF_GROUP_ITEM) {
            return false;
        }
        if (inventory.shopItem(item.typeid).isEmpty()) {
            return false;
        }
        if (item.pang < GamePackets.SHOP_ITEM_MIN_PRICE
                || item.pang > GamePackets.SHOP_ITEM_MAX_PRICE
                || item.pang > GamePackets.SHOP_PANG_ABUSE) {
            return false;
        }
        int have = 0;
        boolean owned = false;
        for (GamePackets.WarehouseItem row : warehouse) {
            if (row.typeid == item.typeid) {
                owned = true;
                have = row.c[0] & 0xffff;
                break;
            }
        }
        return owned && item.qntd <= have;
    }

    /**
     * C# {@code packet07D}: u32 owner + item. Missing shop is {@code 0xEC}
     * sys {@code 5200552}; truncated {@code ToRead} and buy errors are
     * {@code 5200550}. Success {@code 0xEC} to both, {@code 0xED} to owner+
     * viewers, seller {@code 0x40} option 7.
     */
    private void buySaleShop(Session session, PacketReader reader) {
        GameRoom room = playerRoom(session);
        if (room == null) {
            return;
        }
        long ownerUid = reader.remaining() >= 4 ? reader.u32Unsigned() : 0;
        if (reader.remaining() < GamePackets.PERSONAL_SHOP_ITEM_BYTES) {
            session.send(GamePackets.shopBuyFail(GamePackets.SHOP_ERR_BUY_DEFAULT));
            return;
        }
        GamePackets.PersonalShopItem buy = GamePackets.readPersonalShopItem(reader);
        if (room.shops.get(ownerUid) == null) {
            session.send(GamePackets.shopBuyFail(GamePackets.shopSys(GamePackets.SHOP_ERR_BUY_NONE)));
            return;
        }
        Session owner = room.findByUid(ownerUid);
        GamePackets.PersonalShopItem listed = room.findListedItem(ownerUid, buy.id);
        if (owner == null
                || ownerUid == session.player().uid
                || !room.shopIsOpen(ownerUid)
                || !room.shopHasViewer(ownerUid, session.player().uid)
                || listed == null
                || buy.qntd <= 0
                || buy.qntd > GamePackets.SHOP_BUY_QNTD_MAX
                || buy.qntd > listed.qntd
                || inventory.shopItem(listed.typeid).isEmpty()) {
            session.send(GamePackets.shopBuyFail(GamePackets.SHOP_ERR_BUY_DEFAULT));
            return;
        }
        long cost = listed.pang * (long) buy.qntd;
        if (inventory.pang(session.player().uid) < cost) {
            session.send(GamePackets.shopBuyFail(GamePackets.SHOP_ERR_BUY_DEFAULT));
            return;
        }
        InventoryRepository.PersonalShopMove move;
        try {
            move = inventory.transferPersonalShop(
                    ownerUid, session.player().uid, listed.id, listed.typeid, buy.qntd, listed.pang);
        } catch (RuntimeException e) {
            log.debug("personal-shop transfer failed owner={} buyer={}", ownerUid, session.player().uid, e);
            session.send(GamePackets.shopBuyFail(GamePackets.SHOP_ERR_BUY_DEFAULT));
            return;
        }
        int remainCount = room.consumeListedItem(ownerUid, listed.id, buy.qntd);
        room.addPangSale(ownerUid, move.sellerGain());
        GamePackets.PersonalShopItem buyerItem = buy.copy();
        buyerItem.id = move.buyerPacket().id;
        int remainFlag = remainCount == 0 ? GamePackets.SHOP_SOLD_EMPTY : GamePackets.SHOP_SOLD_REMAIN;
        owner.send(GamePackets.shopBuyOk(
                1, move.sellerGain(), buy, GamePackets.SHOP_GROUP_ITEM_BYTE, move.sellerPacket().toArray()));
        session.send(GamePackets.shopBuyOk(
                0,
                move.buyerPangAfter(),
                buyerItem,
                GamePackets.SHOP_GROUP_ITEM_BYTE,
                move.buyerPacket().toArray()));
        byte[] sold = GamePackets.shopSold(owner.player().nickname, (int) ownerUid, buy, remainFlag);
        for (Session target : room.shopSoldTargets(ownerUid)) {
            target.send(sold);
        }
        owner.send(GamePackets.chat(GamePackets.CHAT_NOTICE, GamePackets.SHOP_SALE_NICK, GamePackets.SHOP_SALE_MSG));
    }

    /** C# {@code packet098}: {@code 0x10B} u32 0 + i64 daily limit. */
    private void openPapelShop(Session session) {
        if (!session.authorized()) {
            return;
        }
        session.send(GamePackets.papelShopOk(0));
    }

    /**
     * C# {@code requestPlayPapelShop}: SQL catalog stand-in for IFF drops.
     * Empty balls → {@code 0x21B} {@code shopSys(0x5900103)}; success is
     * {@code 0x216} awards + {@code 0xFB} remain + {@code 0x21B} balls.
     */
    private void playPapelShop(Session session) {
        playPapel(session, false);
    }

    /**
     * C# {@code requestPlayBigPapelShop}: same drops with 10 balls and
     * {@code 0xC8} remaining pang + 0, then {@code 0x216}/{@code 0xFB}/{@code 0x26C}.
     */
    private void playBigPapelShop(Session session) {
        playPapel(session, true);
    }

    private void playPapel(Session session, boolean big) {
        if (!inChannel(session)) {
            return;
        }
        int ack = big ? GamePackets.SERVER_BIG_PAPEL : GamePackets.SERVER_PAPEL_PLAY;
        InventoryRepository.PapelPlayResult result = inventory.playPapel(session.player().uid, big);
        if (result.code() != 0) {
            session.send(GamePackets.sysAck(ack, GamePackets.shopSys(result.code())));
            return;
        }
        if (big) {
            session.send(GamePackets.pangSpent(result.pang(), 0));
        }
        session.send(GamePackets.papelAwards(GamePackets.unixNow(), result.awards()));
        session.send(GamePackets.papelRemain(
                GamePackets.PAPEL_UNLIMITED_REMAIN, GamePackets.PAPEL_UNLIMITED_FLAG));
        session.send(GamePackets.papelPlayOk(ack, 0, result.balls(), result.pang(), result.cookie()));
    }

    /**
     * C# {@code requestOpenLegacyTikiShop}: seed not blocked → {@code 0x1E7} u32 0.
     */
    private void openTikiShop(Session session) {
        if (!inChannel(session)) {
            return;
        }
        session.send(GamePackets.tikiShop(0));
    }

    /**
     * C# {@code requestPointLegacyTikiShop}: not blocked → {@code 0x1E8} u32 0
     * + u32 tiki pts (seed 0).
     */
    private void tikiPoints(Session session) {
        if (!inChannel(session)) {
            return;
        }
        session.send(GamePackets.tikiPoints(0, 0));
    }

    /**
     * C# {@code requestExchangeTPByItemLegacyTikiShop}: count 0 →
     * {@code 0x1E9} {@code shopSys(0x5200905)}.
     */
    private void tikiExchangeTp(Session session, PacketReader reader) {
        tikiExchange(session, reader, GamePackets.SERVER_TIKI_EXCHANGE_TP);
    }

    /**
     * C# {@code requestExchangeItemByTPLegacyTikiShop}: count 0 →
     * {@code 0x1EA} {@code shopSys(0x5200905)}.
     */
    private void tikiExchangeItem(Session session, PacketReader reader) {
        tikiExchange(session, reader, GamePackets.SERVER_TIKI_EXCHANGE_ITEM);
    }

    private void tikiExchange(Session session, PacketReader reader, int opcode) {
        if (!inChannel(session)) {
            return;
        }
        if (reader.remaining() < 1) {
            session.send(GamePackets.tikiExchangeFail(opcode, GamePackets.TIKI_EXCHANGE_ERR_DEFAULT));
            return;
        }
        reader.u8();
        session.send(GamePackets.tikiExchangeFail(
                opcode, GamePackets.shopSys(GamePackets.TIKI_EXCHANGE_ERR_PTS)));
    }

    /** C# {@code packet140}: {@code 0x20E} two zeros. */
    private void enterShop(Session session) {
        if (!session.authorized()) {
            return;
        }
        session.send(GamePackets.enterShopOk());
    }

    /**
     * C# {@code packet143} / {@code requestOpenMailBox}: i32 page → {@code 0x211}.
     * Page ≤ 0 writes sys 2. Empty box: error 0 + page + total 1 + count 0.
     */
    private void openMailBox(Session session, PacketReader reader) {
        if (!inChannel(session) || reader.remaining() < 4) {
            return;
        }
        int page = reader.i32();
        if (page <= 0) {
            session.send(GamePackets.mailFail(GamePackets.SERVER_MAILBOX, GamePackets.MAIL_ERR_PAGE));
            return;
        }
        try {
            List<MailBoxStore.MailEntry> mails = mailboxes.page(session.player().uid, page);
            if (mails.isEmpty()) {
                session.send(GamePackets.mailBoxPage(
                        GamePackets.SERVER_MAILBOX, 0, page, 1, List.of()));
                return;
            }
            session.send(GamePackets.mailBoxPage(
                    GamePackets.SERVER_MAILBOX,
                    0,
                    page,
                    mailboxes.totalPages(session.player().uid),
                    mailListBytes(mails)));
        } catch (RuntimeException e) {
            session.send(GamePackets.mailFail(GamePackets.SERVER_MAILBOX, GamePackets.MAIL_ERR_OPEN_DEFAULT));
        }
    }

    /**
     * C# {@code packet144} / {@code requestInfoMail}: i32 id → {@code 0x212}.
     * Missing id is CHANNEL sys 1.
     */
    private void openMail(Session session, PacketReader reader) {
        if (!inChannel(session) || reader.remaining() < 4) {
            return;
        }
        int emailId = reader.i32();
        var found = mailboxes.get(session.player().uid, emailId, true);
        if (found.isEmpty()) {
            session.send(GamePackets.mailFail(GamePackets.SERVER_MAIL_INFO, GamePackets.MAIL_ERR_CHANNEL));
            return;
        }
        MailBoxStore.MailEntry mail = found.get();
        session.send(GamePackets.mailInfoOk(mail.id, mail.fromId, mail.giftDate, mail.msg, mail.lidaYn));
    }

    /**
     * C# {@code packet145} / {@code requestSendMail}. Text-only costs 100 pang and
     * stores the message. Attachments need IFF ({@code 0x5500300}).
     */
    private void sendMail(Session session, PacketReader reader) {
        if (!inChannel(session)) {
            return;
        }
        try {
            if (reader.remaining() < 8) {
                session.send(GamePackets.mailFail(GamePackets.SERVER_MAIL_SEND, GamePackets.MAIL_ERR_SEND_DEFAULT));
                return;
            }
            long fromUid = reader.u32Unsigned();
            long toUid = reader.u32Unsigned();
            String toNick = reader.remaining() >= 2 ? reader.pstr() : "";
            if (reader.remaining() < 2) {
                session.send(GamePackets.mailFail(GamePackets.SERVER_MAIL_SEND, GamePackets.MAIL_ERR_SEND_DEFAULT));
                return;
            }
            reader.u16();
            String msg = reader.remaining() >= 2 ? reader.pstr() : "";
            if (reader.remaining() < 9) {
                session.send(GamePackets.mailFail(GamePackets.SERVER_MAIL_SEND, GamePackets.MAIL_ERR_SEND_DEFAULT));
                return;
            }
            long pangPrice = reader.u64();
            int countItem = reader.u8();
            if (toNick.isEmpty() || !mailSanitize(toNick) || msg.isEmpty() || !mailSanitize(msg)) {
                session.send(GamePackets.mailFail(GamePackets.SERVER_MAIL_SEND, GamePackets.MAIL_ERR_CHANNEL));
                return;
            }
            if (countItem > 0) {
                if (countItem > GamePackets.MAIL_SEND_ITEM_MAX
                        || pangPrice != (long) countItem * GamePackets.MAIL_SEND_ITEM_PANG) {
                    session.send(GamePackets.mailFail(GamePackets.SERVER_MAIL_SEND, GamePackets.MAIL_ERR_SEND_DEFAULT));
                    return;
                }
                int need = countItem * GamePackets.MAIL_ITEM_BYTES;
                if (reader.remaining() >= need) {
                    reader.readBytes(need);
                }
                session.send(GamePackets.mailFail(GamePackets.SERVER_MAIL_SEND, GamePackets.MAIL_ERR_SEND_DEFAULT));
                return;
            }
            if (pangPrice != GamePackets.MAIL_SEND_PANG || toUid == 0) {
                session.send(GamePackets.mailFail(GamePackets.SERVER_MAIL_SEND, GamePackets.MAIL_ERR_SEND_DEFAULT));
                return;
            }
            long uid = session.player().uid;
            long pang = inventory.pang(uid);
            if (pang < pangPrice) {
                session.send(GamePackets.mailFail(GamePackets.SERVER_MAIL_SEND, GamePackets.MAIL_ERR_SEND_DEFAULT));
                return;
            }
            String fromId = fromUid == 0 ? GamePackets.MAIL_FROM_ADM : session.player().nickname;
            mailboxes.add(toUid, fromId, msg);
            inventory.setPangCookie(uid, pang - pangPrice, inventory.cookie(uid));
            session.send(GamePackets.pangSpent(pang - pangPrice, pangPrice));
            session.send(GamePackets.mailFail(GamePackets.SERVER_MAIL_SEND, 0));
            Session target = sessions.findByUid(toUid);
            if (target != null && target.authorized()) {
                target.send(GamePackets.newMail(unreadMailBytes(toUid)));
            }
        } catch (RuntimeException e) {
            log.debug("send-mail failed uid={}", session.player().uid, e);
            session.send(GamePackets.mailFail(GamePackets.SERVER_MAIL_SEND, GamePackets.MAIL_ERR_SEND_DEFAULT));
        }
    }

    /**
     * C# {@code packet146}: i32 id. No IFF warehouse move; empty attachments
     * write {@code pacote214(1)}. Empty mailbox catch writes {@code 0x5500100}.
     */
    private void takeMail(Session session, PacketReader reader) {
        if (!inChannel(session) || reader.remaining() < 4) {
            return;
        }
        int emailId = reader.i32();
        if (mailboxes.isEmpty(session.player().uid)) {
            session.send(GamePackets.mailFail(GamePackets.SERVER_MAIL_TAKE, GamePackets.MAIL_ERR_TAKE_DEFAULT));
            return;
        }
        mailboxes.get(session.player().uid, emailId, false);
        session.send(GamePackets.mailFail(GamePackets.SERVER_MAIL_TAKE, GamePackets.MAIL_ERR_TAKE_EMPTY));
    }

    /**
     * C# {@code packet147}: u32 count + ids + u32 page → {@code 0x215}.
     */
    private void deleteMail(Session session, PacketReader reader) {
        if (!inChannel(session) || reader.remaining() < 4) {
            return;
        }
        int count = reader.u32();
        if (count < 0 || reader.remaining() < count * 4L + 4) {
            session.send(GamePackets.mailFail(GamePackets.SERVER_MAIL_DELETE, GamePackets.MAIL_ERR_DELETE_DEFAULT));
            return;
        }
        int[] ids = new int[count];
        for (int i = 0; i < count; i++) {
            ids[i] = reader.u32();
        }
        int page = reader.u32();
        if (page <= 0) {
            session.send(GamePackets.mailFail(GamePackets.SERVER_MAIL_DELETE, GamePackets.MAIL_ERR_PAGE));
            return;
        }
        try {
            mailboxes.delete(session.player().uid, ids);
            List<MailBoxStore.MailEntry> mails = mailboxes.page(session.player().uid, page);
            if (mails.isEmpty()) {
                session.send(GamePackets.mailBoxPage(
                        GamePackets.SERVER_MAIL_DELETE, 0, page, 1, List.of()));
                return;
            }
            session.send(GamePackets.mailBoxPage(
                    GamePackets.SERVER_MAIL_DELETE,
                    0,
                    page,
                    mailboxes.totalPages(session.player().uid),
                    mailListBytes(mails)));
        } catch (RuntimeException e) {
            session.send(GamePackets.mailFail(GamePackets.SERVER_MAIL_DELETE, GamePackets.MAIL_ERR_DELETE_DEFAULT));
        }
    }

    private boolean inChannel(Session session) {
        return session.authorized() && session.player().channelId >= 0;
    }

    private List<byte[]> unreadMailBytes(long uid) {
        return mailListBytes(mailboxes.unread(uid));
    }

    private static List<byte[]> mailListBytes(List<MailBoxStore.MailEntry> mails) {
        List<byte[]> out = new ArrayList<>(mails.size());
        for (MailBoxStore.MailEntry mail : mails) {
            out.add(MailBoxStore.toListBytes(mail));
        }
        return out;
    }

    /** C# {@code Tools.Sanitize}: reject SQL-keyword substrings. */
    private static boolean mailSanitize(String input) {
        if (input == null || input.isBlank()) {
            return true;
        }
        if (input.length() > 256) {
            return false;
        }
        String lower = input.toLowerCase(java.util.Locale.ROOT);
        for (String word : MAIL_SANITIZE) {
            if (lower.contains(word)) {
                return false;
            }
        }
        return true;
    }

    /**
     * C# {@code packet033}: u8 + PStr then {@code DisconnectSession}.
     */
    private void reportClientException(Session session, PacketReader reader) {
        if (reader.remaining() >= 1) {
            int tipo = reader.u8();
            String msg = reader.remaining() >= 2 ? reader.pstr() : "";
            log.debug("client exception uid={} tipo={} msg={}",
                    session.authorized() ? session.player().uid : 0, tipo, msg);
        }
        if (session.authorized()) {
            leaveRoom(session);
        }
        session.disconnect();
    }

    /**
     * C# {@code packet03C}: u16 sub. Msg_OFF spends 10 pang and {@code 0x95};
     * Friend_List is unimplemented in C# (fail {@code 0x5700105}).
     */
    private void translateSubPacket(Session session, PacketReader reader) {
        if (!session.authorized() || reader.remaining() < 2) {
            return;
        }
        int sub = reader.u16();
        if (sub == GamePackets.MSN_FRIEND_LIST) {
            session.send(GamePackets.msnAckFail(sub, GamePackets.MSN_ERR_FUNDS));
            return;
        }
        if (sub != GamePackets.MSN_MSG_OFF) {
            return;
        }
        if (reader.remaining() < 4) {
            session.send(GamePackets.msnAckFail(sub, GamePackets.MSN_ERR_DEFAULT));
            return;
        }
        long toUid = reader.u32Unsigned();
        String msg = reader.remaining() >= 2 ? reader.pstr() : "";
        int opt = reader.remaining() >= 1 ? reader.u8() : 1;
        if (toUid == 0) {
            session.send(GamePackets.msnAckFail(sub, GamePackets.MSN_ERR_UID));
            return;
        }
        if (msg.isEmpty()) {
            session.send(GamePackets.msnAckFail(sub, GamePackets.MSN_ERR_EMPTY));
            return;
        }
        if (msg.length() > 256) {
            session.send(GamePackets.msnAckFail(sub, GamePackets.MSN_ERR_SIZE));
            return;
        }
        if (opt != 0) {
            session.send(GamePackets.msnAckFail(sub, GamePackets.MSN_ERR_OPT));
            return;
        }
        long uid = session.player().uid;
        long pang = inventory.pang(uid);
        if (pang < GamePackets.MSN_OFF_PANG) {
            session.send(GamePackets.msnAckFail(sub, GamePackets.MSN_ERR_FUNDS));
            return;
        }
        try {
            inventory.setPangCookie(uid, pang - GamePackets.MSN_OFF_PANG, inventory.cookie(uid));
            repo.insertMsgOff(uid, toUid, msg);
            session.send(GamePackets.msnAckOk(sub, pang - GamePackets.MSN_OFF_PANG));
        } catch (RuntimeException e) {
            log.debug("msg-off insert failed uid={}", uid, e);
            session.send(GamePackets.msnAckFail(sub, GamePackets.MSN_ERR_DEFAULT));
        }
    }

    /**
     * C# {@code packet042}: u8 count + count×u32. No success reply.
     */
    private void initShotArrows(Session session, PacketReader reader) {
        GameRoom room = inGameRoom(session);
        if (room == null || reader.remaining() < 1) {
            return;
        }
        int count = reader.u8();
        if (count == 0 || reader.remaining() < count * 4) {
            return;
        }
        for (int i = 0; i < count; i++) {
            reader.u32();
        }
    }

    /**
     * C# {@code packet04A}: u32 typeid. Success {@code 0xA4} needs warehouse;
     * catch is silent.
     */
    private void activeReplay(Session session, PacketReader reader) {
        if (inGameRoom(session) == null) {
            return;
        }
        if (reader.remaining() >= 4) {
            reader.u32();
        }
    }

    private void sendFinishGameDump(Session session, GameRoom room) {
        session.send(GamePackets.prizeList(new int[0]));
        session.send(GamePackets.gameResult(0, room.info.trophy, 0, 2));
        session.send(GamePackets.myStatistics(GamePackets.userInfoPublic(session.player().level)));
        session.send(GamePackets.treasureHunterItem());
        session.send(GamePackets.pangSpent(inventory.pang(session.player().uid), 0));
    }

    /**
     * C# {@code packet031} / {@code requestFinishHoleData}: stores {@code UserInfoEx}, no reply.
     */
    private void finishHoleData(Session session, PacketReader reader) {
        GameRoom room = inGameRoom(session);
        if (room == null || reader.remaining() < GamePackets.USER_INFO_BYTES) {
            return;
        }
        reader.readBytes(GamePackets.USER_INFO_BYTES);
    }

    /**
     * C# Versus {@code requestUnOrPause}: u8 opt → broadcast {@code 0x8B} oid + opt.
     * Tourney/Practice inherit {@code GameBase} no-op.
     */
    private void pauseGame(Session session, PacketReader reader) {
        GameRoom room = inGameRoom(session);
        if (room == null || reader.remaining() < 1 || !GamePackets.usesVersusInitialData(room.tipo)) {
            return;
        }
        int opt = reader.u8();
        if (opt == GamePackets.PAUSE_PAUSE) {
            if (room.pauseCount >= GamePackets.VERSUS_PAUSE_MAX) {
                return;
            }
            room.pauseCount++;
        } else if (opt != GamePackets.PAUSE_RESUME) {
            return;
        }
        room.broadcast(GamePackets.pause(session.oid(), opt));
    }

    /**
     * C# {@code packet00B} / {@code requestChangePlayerItemChannel}: lobby appearance
     * via {@code pacote04B} plus lobby {@code 0x46} option 3.
     */
    private void changeLobbyItem(Session session, PacketReader reader) {
        if (!session.authorized() || reader.remaining() < 1) {
            return;
        }
        int type = reader.u8();
        ItemChange change = applyItemChange(session, type, reader);
        session.send(GamePackets.roomUserInfoChanged(change.err, type, session.oid(), change.extra));
        if (change.err == 0) {
            sendLobbyPlayerInfo(session, GamePackets.LOBBY_USER_UPDATE);
        }
    }

    /**
     * C# {@code packet00C} / {@code requestChangePlayerItemRoom}: in-room {@code TYPE_CHANGE}
     * then broadcast {@code pacote04B}. {@code TC_ALL} only applies equips (Java {@code 0x0E}
     * already sent initial data). Lounge effect needs IFF parts and stays a no-op.
     */
    private void changeRoomItem(Session session, PacketReader reader) {
        if (!session.authorized() || reader.remaining() < 1) {
            return;
        }
        PlayerContext pi = session.player();
        GameRoom room = rooms.get(pi.roomNumber);
        if (room == null) {
            if (pi.inLobby) {
                return;
            }
            session.send(GamePackets.roomUserInfoChanged(1, 0xff, session.oid(), new byte[0]));
            return;
        }
        int type = reader.u8();
        if (type == GamePackets.ITEM_ALL) {
            if (reader.remaining() < 16) {
                return;
            }
            applyItemChange(session, GamePackets.ITEM_CHARACTER, reader.i32());
            applyItemChange(session, GamePackets.ITEM_CADDIE, reader.i32());
            applyItemChange(session, GamePackets.ITEM_CLUBSET, reader.i32());
            applyItemChange(session, GamePackets.ITEM_BALL, reader.i32());
            room.putPlayerInfo(session, makePlayerInfo(session, room));
            return;
        }
        if (type == GamePackets.ITEM_LOUNGE_EFFECT) {
            if (reader.remaining() >= 8) {
                reader.readBytes(8);
            }
            session.send(GamePackets.roomUserInfoChanged(1, type, session.oid(), new byte[0]));
            return;
        }
        ItemChange change = applyItemChange(session, type, reader);
        if (change.err == 0) {
            room.putPlayerInfo(session, makePlayerInfo(session, room));
            room.broadcast(GamePackets.roomUserInfoChanged(0, type, session.oid(), change.extra));
        } else {
            session.send(GamePackets.roomUserInfoChanged(change.err, type, session.oid(), new byte[0]));
        }
    }

    private ItemChange applyItemChange(Session session, int type, PacketReader reader) {
        int id = reader.remaining() >= 4 ? reader.i32() : 0;
        return applyItemChange(session, type, id);
    }

    private ItemChange applyItemChange(Session session, int type, int id) {
        long uid = session.player().uid;
        try {
            return switch (type) {
                case GamePackets.ITEM_CADDIE -> {
                    if (id != 0 && inventory.caddies(uid).stream().noneMatch(c -> c.id == id)) {
                        yield ItemChange.fail(2);
                    }
                    inventory.equipCaddie(uid, id);
                    byte[] extra = inventory.caddies(uid).stream()
                            .filter(c -> c.id == id)
                            .findFirst()
                            .map(GamePackets.CaddieInfo::toArray)
                            .orElse(new byte[GamePackets.CADDIE_INFO_BYTES]);
                    yield ItemChange.ok(extra);
                }
                case GamePackets.ITEM_BALL -> {
                    if (id != 0 && inventory.warehouse(uid).stream().noneMatch(w -> w.typeid == id)) {
                        yield ItemChange.fail(2);
                    }
                    GamePackets.UserEquip equip = inventory.userEquip(uid);
                    inventory.equipBallAndClub(uid, id, equip.clubsetId);
                    yield ItemChange.ok(u32le(id));
                }
                case GamePackets.ITEM_CLUBSET -> {
                    var found = inventory.warehouse(uid).stream().filter(w -> w.id == id).findFirst();
                    if (id == 0 || found.isEmpty()) {
                        yield ItemChange.fail(2);
                    }
                    GamePackets.UserEquip equip = inventory.userEquip(uid);
                    inventory.equipBallAndClub(uid, equip.ballTypeid, id);
                    yield ItemChange.ok(GamePackets.ClubSetInfo.fromWarehouse(found.get()).toArray());
                }
                case GamePackets.ITEM_CHARACTER -> {
                    var found = inventory.characters(uid).stream().filter(c -> c.id == id).findFirst();
                    if (found.isEmpty()) {
                        yield ItemChange.fail(2);
                    }
                    inventory.equipCharacter(uid, id);
                    yield ItemChange.ok(found.get().toArray());
                }
                case GamePackets.ITEM_MASCOT -> {
                    var found = inventory.mascots(uid).stream().filter(m -> m.id == id).findFirst();
                    if (id != 0 && found.isEmpty()) {
                        yield ItemChange.fail(2);
                    }
                    inventory.equipMascot(uid, id);
                    byte[] extra = found.map(GamePackets.MascotInfo::toArray)
                            .orElse(new byte[GamePackets.MASCOT_INFO_BYTES]);
                    yield ItemChange.ok(extra);
                }
                default -> ItemChange.fail(13);
            };
        } catch (RuntimeException e) {
            log.warn("item change type={} uid={} failed: {}", type, uid, e.toString());
            return ItemChange.fail(1);
        }
    }

    /**
     * C# {@code room.finish_game}: waiting state + {@code pacote04A} + per-player {@code 0x48} option 3.
     */
    private void finishGameRoom(GameRoom room) {
        room.inGame = false;
        room.info.state = 1;
        room.course = null;
        room.pauseCount = 0;
        room.stopTurnTimer();
        room.shots.clear();
        room.clearCharIntro();
        room.clearLoadHole();
        room.turnOid = 0;
        room.reported.clear();
        for (Session member : room.snapshot()) {
            GamePackets.PlayerRoomInfo pri = room.playerInfo(member);
            if (pri == null) {
                continue;
            }
            if (room.tipo == GamePackets.TIPO_PRACTICE
                    || room.tipo == GamePackets.TIPO_GRAND_ZODIAC_PRACTICE) {
                pri.place = 2;
            } else {
                pri.place = 0;
            }
            room.putPlayerInfo(member, pri);
            room.broadcast(GamePackets.roomPlayers(3, List.of(pri)));
        }
        room.broadcast(GamePackets.roomUpdate(room.info));
        sendLobbyRoomInfo(room, GamePackets.ROOM_LIST_UPDATE);
    }

    private record ItemChange(int err, byte[] extra) {
        static ItemChange ok(byte[] extra) {
            return new ItemChange(0, extra == null ? new byte[0] : extra);
        }

        static ItemChange fail(int err) {
            return new ItemChange(err, new byte[0]);
        }
    }

    private void equipItem(Session session, PacketReader reader) {
        if (!session.authorized() || reader.remaining() < 1) {
            return;
        }
        int type = reader.u8();
        long uid = session.player().uid;
        int err = GamePackets.EQUIP_OK;
        byte[] extra = new byte[0];
        try {
            switch (type) {
                case 0 -> { // C# CharacterInfo.ToRead — parts without IFF validation.
                    if (reader.remaining() < GamePackets.CHARACTER_INFO_BYTES) {
                        err = 1;
                    } else {
                        GamePackets.CharacterInfo ci = GamePackets.CharacterInfo.read(reader);
                        if (ci.id == 0) {
                            err = 1;
                        } else if (inventory.characters(uid).stream().noneMatch(c -> c.id == ci.id)) {
                            err = 2;
                        } else {
                            inventory.updateCharacterParts(uid, ci);
                            extra = ci.toArray();
                        }
                    }
                }
                case 1 -> { // Caddie id
                    int id = reader.remaining() >= 4 ? reader.i32() : 0;
                    if (id != 0 && inventory.caddies(uid).stream().noneMatch(c -> c.id == id)) {
                        err = 2;
                    } else {
                        inventory.equipCaddie(uid, id);
                        extra = i32le(id);
                    }
                }
                case 3 -> { // Ball typeid + clubset id
                    int ball = reader.remaining() >= 4 ? reader.i32() : 0;
                    int club = reader.remaining() >= 4 ? reader.i32() : 0;
                    boolean ballOk = ball == 0
                            || inventory.warehouse(uid).stream().anyMatch(w -> w.typeid == ball);
                    boolean clubOk = club == 0
                            || inventory.warehouse(uid).stream().anyMatch(w -> w.id == club);
                    if (!ballOk || !clubOk) {
                        err = 2;
                    } else {
                        inventory.equipBallAndClub(uid, ball, club);
                        extra = concat(u32le(ball), i32le(club));
                    }
                }
                case 5 -> { // Character id
                    int id = reader.remaining() >= 4 ? reader.i32() : 0;
                    if (inventory.characters(uid).stream().noneMatch(c -> c.id == id)) {
                        err = 2;
                    } else {
                        inventory.equipCharacter(uid, id);
                        extra = i32le(id);
                    }
                }
                case 8 -> { // Mascot id
                    int id = reader.remaining() >= 4 ? reader.i32() : 0;
                    var found = inventory.mascots(uid).stream().filter(m -> m.id == id).findFirst();
                    if (id != 0 && found.isEmpty()) {
                        err = 2;
                    } else {
                        inventory.equipMascot(uid, id);
                        extra = found.map(m -> m.toArray()).orElse(new byte[GamePackets.MASCOT_INFO_BYTES]);
                    }
                }
                default -> extra = new byte[0];
            }
        } catch (RuntimeException e) {
            log.warn("equip type={} uid={} failed: {}", type, uid, e.toString());
            err = 1;
            extra = new byte[0];
        }
        session.send(GamePackets.equipAck(err, type, extra));
    }

    private void buyItem(Session session, PacketReader reader) {
        if (!session.authorized()) {
            return;
        }
        try {
            GamePackets.BuyRequest req = GamePackets.readBuyRequest(reader);
            if (req.items().isEmpty()) {
                session.send(GamePackets.buyFailed(GamePackets.BUY_FAIL_EMPTY));
                return;
            }
            List<GamePackets.BoughtItem> bought = new ArrayList<>();
            long pang = 0;
            long cookie = 0;
            long pangSpent = 0;
            long cookieSpent = 0;
            for (GamePackets.BuyItem item : req.items()) {
                InventoryRepository.ShopBuyResult result = inventory.buyShopItem(
                        session.player().uid, item.typeid(), item.qntd(), item.pang(), item.cookie());
                if (result.code() != 0) {
                    session.send(GamePackets.buyFailed(result.code()));
                    return;
                }
                bought.add(new GamePackets.BoughtItem(
                        result.typeid(), result.itemId(), 0, 0, result.qntdDep()));
                pang = result.pang();
                cookie = result.cookie();
                pangSpent += result.pangSpent();
                cookieSpent += result.cookieSpent();
            }
            if (pangSpent > 0) {
                session.send(GamePackets.pangSpent(pang, pangSpent));
            }
            if (cookieSpent > 0) {
                session.send(GamePackets.cookieBalance(cookie));
            }
            session.send(GamePackets.buyNewItems(bought, pang, cookie));
            session.send(GamePackets.buyOk(pang, cookie));
        } catch (RuntimeException e) {
            log.warn("buy item uid={} failed: {}", session.player().uid, e.toString());
            session.send(GamePackets.buyFailed(GamePackets.BUY_FAIL_GENERIC));
        }
    }

    /**
     * C# {@code packet01F} / {@code requestGiftItemShop}. Level &lt; Beginner E
     * ({@code 6}) is CHANNEL sys 1 before {@code qntd}. Catalog is SQL
     * {@code shop_catalog} (IFF {@code IsGiftItem} stand-in). Success charges
     * the sender without warehouse and puts the item in the recipient mailbox.
     */
    private void giftItem(Session session, PacketReader reader) {
        if (!session.authorized()) {
            return;
        }
        long pang = inventory.pang(session.player().uid);
        long cookie = inventory.cookie(session.player().uid);
        try {
            if (reader.remaining() < 9) {
                session.send(GamePackets.giftFailed(GamePackets.BUY_FAIL_GENERIC, pang, cookie));
                return;
            }
            reader.u16();
            long toUid = reader.u32Unsigned();
            String msg = reader.pstr();
            if (reader.remaining() < 3) {
                session.send(GamePackets.giftFailed(GamePackets.BUY_FAIL_GENERIC, pang, cookie));
                return;
            }
            reader.u8();
            int qntd = reader.u16();
            if (session.player().level < GamePackets.GIFT_MIN_LEVEL) {
                session.send(GamePackets.giftFailed(GamePackets.BUY_FAIL_INIT, pang, cookie));
                return;
            }
            if (qntd <= 0) {
                session.send(GamePackets.giftFailed(GamePackets.BUY_FAIL_EMPTY, pang, cookie));
                return;
            }
            List<GamePackets.BuyItem> items = new ArrayList<>();
            for (int i = 0; i < qntd; i++) {
                items.add(GamePackets.readBuyItem(reader));
            }
            long pangAfter = pang;
            long cookieAfter = cookie;
            long pangSpent = 0;
            long cookieSpent = 0;
            for (GamePackets.BuyItem item : items) {
                InventoryRepository.ShopBuyResult result = inventory.giftShopItem(
                        session.player().uid, item.typeid(), item.qntd(), item.pang(), item.cookie());
                if (result.code() != 0) {
                    session.send(GamePackets.giftFailed(result.code(), pang, cookie));
                    return;
                }
                pangAfter = result.pang();
                cookieAfter = result.cookie();
                pangSpent += result.pangSpent();
                cookieSpent += result.cookieSpent();
            }
            String fromId = session.player().nickname == null ? "" : session.player().nickname;
            mailboxes.add(toUid, fromId, msg == null ? "" : msg, items.size());
            if (pangSpent > 0) {
                session.send(GamePackets.pangSpent(pangAfter, pangSpent));
            }
            if (cookieSpent > 0) {
                session.send(GamePackets.cookieBalance(cookieAfter));
            }
            session.send(GamePackets.giftFailed(0, pangAfter, cookieAfter));
            Session target = sessions.findByUid(toUid);
            if (target != null && target.authorized()) {
                target.send(GamePackets.newMail(unreadMailBytes(toUid)));
            }
        } catch (RuntimeException e) {
            log.warn("gift item uid={} failed: {}", session.player().uid, e.toString());
            session.send(GamePackets.giftFailed(GamePackets.BUY_FAIL_GENERIC, pang, cookie));
        }
    }

    private void loungeState(Session session) {
        if (!session.authorized()) {
            return;
        }
        GameRoom room = rooms.get(session.player().roomNumber);
        if (room == null || room.tipo != GamePackets.TIPO_LOUNGE) {
            return;
        }
        room.broadcast(GamePackets.loungeState(session.oid()));
    }

    /**
     * C# {@code packet063} / {@code requestPlayerLocationRoom}: type + payload →
     * room {@code 0xC4}. Location types 4/6 add xz and set r; type 9 is unknown.
     */
    private void playerLocationRoom(Session session, PacketReader reader) {
        if (!session.authorized() || reader.remaining() < 1) {
            return;
        }
        GameRoom room = rooms.get(session.player().roomNumber);
        if (room == null) {
            return;
        }
        int type = reader.u8();
        PlayerContext pi = session.player();
        GamePackets.PlayerRoomInfo pri = room.playerInfo(session);
        byte[] payload;
        switch (type) {
            case GamePackets.ACTION_ROTATION -> {
                if (reader.remaining() < 4) {
                    return;
                }
                pi.locR = reader.f32();
                if (pri != null) {
                    pri.r = pi.locR;
                }
                payload = new PacketWriter().f32(pi.locR).toBytes();
            }
            case GamePackets.ACTION_LOUNGER_LOC, GamePackets.ACTION_MOVE -> {
                if (reader.remaining() < GamePackets.LOCATION_BYTES) {
                    return;
                }
                float dx = reader.f32();
                float dz = reader.f32();
                float r = reader.f32();
                pi.locX += dx;
                pi.locZ += dz;
                pi.locR = r;
                if (pri != null) {
                    pri.x = pi.locX;
                    pri.z = pi.locZ;
                    pri.r = pi.locR;
                }
                payload = GamePackets.location(dx, dz, r);
            }
            case GamePackets.ACTION_LOUNGER_STATE -> {
                if (reader.remaining() < 4) {
                    return;
                }
                pi.loungeState = reader.u32();
                if (pri != null) {
                    pri.state = pi.loungeState;
                }
                payload = new PacketWriter().u32(pi.loungeState).toBytes();
            }
            case GamePackets.ACTION_ACK_PLAYER -> {
                if (reader.remaining() < 4) {
                    return;
                }
                pi.stateLounge = reader.u32();
                if (pri != null) {
                    pri.stateLounge = pi.stateLounge;
                }
                payload = new PacketWriter().u32(pi.stateLounge).toBytes();
            }
            case GamePackets.ACTION_MOTION_ROOM,
                    GamePackets.ACTION_MOTION_LOUNGER,
                    GamePackets.ACTION_ANIMATION_WITH_EFFECTS -> payload = reader.remainingBytes();
            default -> {
                return;
            }
        }
        room.broadcast(GamePackets.syncActivity(session.oid(), type, payload));
    }

    /**
     * C# {@code packet032}: u8 state → room {@code 0x8E} plus lobby {@code 0x46} option 3.
     */
    private void changeSleep(Session session, PacketReader reader) {
        if (!session.authorized() || reader.remaining() < 1) {
            return;
        }
        GameRoom room = rooms.get(session.player().roomNumber);
        if (room == null) {
            return;
        }
        int state = reader.u8();
        PlayerContext pi = session.player();
        pi.away = state & 1;
        GamePackets.PlayerRoomInfo pri = room.playerInfo(session);
        if (pri == null) {
            return;
        }
        if (pi.away != 0) {
            pri.stateFlag |= GamePackets.PLAYER_AWAY_BIT;
        } else {
            pri.stateFlag &= ~GamePackets.PLAYER_AWAY_BIT;
        }
        room.broadcast(GamePackets.sleep(session.oid(), state));
        sendLobbyPlayerInfo(session, GamePackets.LOBBY_USER_UPDATE);
    }

    /**
     * C# {@code packet034}: Tourney stores the flag with no reply. Versus broadcasts
     * empty {@code 0x90} when every in-room player has finished the intro.
     */
    private void finishCharIntro(Session session) {
        GameRoom room = inGameRoom(session);
        if (room == null || !GamePackets.usesVersusInitialData(room.tipo)) {
            return;
        }
        if (room.markCharIntro(session)) {
            room.clearCharIntro();
            room.broadcast(GamePackets.teeshotReady());
        }
    }

    private GameRoom inGameRoom(Session session) {
        if (!session.authorized()) {
            return null;
        }
        GameRoom room = rooms.get(session.player().roomNumber);
        if (room == null || !room.inGame) {
            return null;
        }
        return room;
    }

    private void leaveRoom(Session session) {
        PlayerContext pi = session.player();
        if (!session.authorized() || pi.roomNumber < 0) {
            return;
        }
        GameRoom room = rooms.get(pi.roomNumber);
        boolean destroyed = false;
        GameRoom leftover = null;
        GamePackets.PlayerRoomInfo leaver = room == null ? null : room.playerInfo(session);
        if (room != null) {
            boolean wasMaster = room.info.master == (int) pi.uid;
            room.removePlayer(session);
            if (room.info.numPlayer <= 0) {
                rooms.remove(pi.roomNumber);
                destroyed = true;
            } else if (wasMaster && !room.inGame) {
                Session next = room.electMaster();
                if (next != null) {
                    room.broadcast(GamePackets.decisionRoomMaster(next.oid(), 0));
                }
            }
            leftover = room;
        } else {
            rooms.remove(pi.roomNumber);
        }
        pi.inPractice = false;
        pi.roomNumber = -1;
        pi.place = 0;
        if (leftover != null && !destroyed) {
            leftover.broadcast(GamePackets.roomUpdate(leftover.info));
            int base = GamePackets.usesCompactPlayerRoomInfo(leftover.tipo) ? 0x100 : 0;
            GamePackets.PlayerRoomInfo left = leaver;
            if (left == null) {
                left = new GamePackets.PlayerRoomInfo();
                left.oid = session.oid();
            }
            leftover.broadcast(GamePackets.roomPlayers(base + 2, List.of(left)));
        }
        if (leftover != null) {
            sendLobbyRoomInfo(leftover, destroyed ? GamePackets.ROOM_LIST_REMOVE : GamePackets.ROOM_LIST_UPDATE);
        }
        sendLobbyPlayerInfo(session, GamePackets.LOBBY_USER_UPDATE);
        session.send(GamePackets.exitRoomAck(-1));
    }

    /**
     * C# {@code Channel.requestKickPlayerOfRoom}: master kicks uid via
     * {@code leaveRoomMultiPlayer(kick, 3)} → {@code 0x4C} -1.
     */
    private void banish(Session session, PacketReader reader) {
        if (!session.authorized() || reader.remaining() < 4) {
            return;
        }
        long uid = reader.u32() & 0xffff_ffffL;
        PlayerContext pi = session.player();
        GameRoom room = rooms.get(pi.roomNumber);
        if (room == null || room.info.master != (int) pi.uid) {
            return;
        }
        if (room.inGame && (pi.capability & 4) == 0) {
            return;
        }
        Session kick = room.findByUid(uid);
        if (kick == null) {
            return;
        }
        leaveRoom(kick);
    }

    /**
     * C# {@code Channel.requestCheckNick} → {@code pacote0A1}. Found nick is
     * error 0 + uid + MemberInfoEx; anything else is error 2.
     */
    private void requestUserInfoOffline(Session session, PacketReader reader) {
        if (!session.authorized() || reader.remaining() < 1) {
            return;
        }
        reader.u8();
        String nick = reader.remaining() >= 2 ? reader.pstr() : "";
        if (nick.isEmpty()
                || nick.length() < 4
                || nick.indexOf(' ') >= 0
                || NICK_BAD.matcher(nick).matches()) {
            session.send(GamePackets.userInfoOfflineMissing());
            return;
        }
        var info = repo.playerInfoByNick(nick).orElse(null);
        if (info == null || info.uid() == 0) {
            session.send(GamePackets.userInfoOfflineMissing());
            return;
        }
        Session online = sessions.findByUid(info.uid());
        int oid = online == null ? 0 : online.oid();
        int sala = 0xffff;
        if (online != null && online.player().roomNumber >= 0) {
            sala = online.player().roomNumber & 0xffff;
        }
        session.send(GamePackets.userInfoOffline(
                (int) info.uid(),
                GamePackets.memberInfoExPublic(oid, info.id(), info.nickname(), info.capability(), sala)));
    }

    private void leavePractice(Session session) {
        if (!session.authorized() || !session.player().inPractice) {
            return;
        }
        leaveRoom(session);
    }

    private void sendRoomEnterPackets(Session joiner, GameRoom room) {
        room.broadcast(GamePackets.roomUpdate(room.info));
        joiner.send(GamePackets.roomEntered(room.info));
        int base = GamePackets.usesCompactPlayerRoomInfo(room.tipo) ? 0x100 : 0;
        List<GamePackets.PlayerRoomInfo> all = room.playerInfoSnapshot();
        GamePackets.PlayerRoomInfo self = room.playerInfo(joiner);
        room.broadcast(GamePackets.roomPlayers(base, all));
        if (self != null) {
            room.broadcast(GamePackets.roomPlayers(base + 1, List.of(self)));
        }
    }

    private GamePackets.PlayerRoomInfo makePlayerInfo(Session session, GameRoom room) {
        PlayerContext pi = session.player();
        GamePackets.PlayerRoomInfo pri = new GamePackets.PlayerRoomInfo();
        pri.oid = session.oid();
        pri.nickname = pi.nickname == null ? "" : pi.nickname;
        pri.position = room.snapshot().indexOf(session) + 1;
        pri.capability = pi.capability;
        pri.uid = (int) pi.uid;
        pri.level = pi.level;
        pri.place = 10;
        pri.x = pi.locX;
        pri.z = pi.locZ;
        pri.r = pi.locR;
        pri.state = pi.loungeState;
        pri.stateLounge = pi.stateLounge;
        if (pi.away != 0) {
            pri.stateFlag |= GamePackets.PLAYER_AWAY_BIT;
        }
        if (room.info.master == (int) pi.uid) {
            pri.stateFlag |= GamePackets.PLAYER_MASTER_BIT | GamePackets.PLAYER_READY_BIT;
        }
        if (pri.position > 0) {
            pri.stateFlag |= (pri.position - 1) % 2;
        }
        GamePackets.UserEquip equip = inventory.userEquip(pi.uid);
        System.arraycopy(equip.skinTypeid, 0, pri.skin, 0, Math.min(6, equip.skinTypeid.length));
        pri.skin[4] = 0;
        pri.title = equip.skinTypeid.length > 5 ? equip.skinTypeid[5] : 0;
        for (GamePackets.CharacterInfo c : inventory.characters(pi.uid)) {
            if (c.id == equip.characterId || pri.character == null) {
                pri.character = c;
                pri.charTypeid = c.typeid;
                if (c.id == equip.characterId) {
                    break;
                }
            }
        }
        for (GamePackets.MascotInfo m : inventory.mascots(pi.uid)) {
            if (m.id == equip.mascotId) {
                pri.mascotTypeid = m.typeid;
                break;
            }
        }
        return pri;
    }

    private GamePackets.VersusPlayer versusPlayer(Session session) {
        PlayerContext pi = session.player();
        GamePackets.UserEquip equip = inventory.userEquip(pi.uid);
        byte[] character = new byte[GamePackets.CHARACTER_INFO_BYTES];
        byte[] caddie = new byte[GamePackets.CADDIE_INFO_BYTES];
        byte[] clubset = new byte[GamePackets.CLUBSET_INFO_BYTES];
        byte[] mascot = new byte[GamePackets.MASCOT_INFO_BYTES];
        for (GamePackets.CharacterInfo c : inventory.characters(pi.uid)) {
            if (c.id == equip.characterId) {
                character = c.toArray();
                break;
            }
        }
        for (GamePackets.CaddieInfo c : inventory.caddies(pi.uid)) {
            if (c.id == equip.caddieId) {
                caddie = c.toArray();
                break;
            }
        }
        for (GamePackets.WarehouseItem w : inventory.warehouse(pi.uid)) {
            if (w.id == equip.clubsetId) {
                clubset = GamePackets.ClubSetInfo.fromWarehouse(w).toArray();
                break;
            }
        }
        for (GamePackets.MascotInfo m : inventory.mascots(pi.uid)) {
            if (m.id == equip.mascotId) {
                mascot = m.toArray();
                break;
            }
        }
        List<GamePackets.CardInfo> cards = inventory.cards(pi.uid);
        return new GamePackets.VersusPlayer(
                GamePackets.memberInfoExPublic(session.oid(), pi.id, pi.nickname, pi.capability),
                (int) pi.uid,
                GamePackets.userInfoPublic(pi.level),
                equip.toArray(),
                character,
                caddie,
                clubset,
                mascot,
                cards);
    }

    private void enterLobby(Session session) {
        if (!session.authorized() || session.player().channelId < 0) {
            return;
        }
        if (session.player().inLobby) {
            return;
        }
        sendLobbyEnter(session, true);
    }

    /**
     * C# {@code Channel.enterLobby}: user list + room list + join broadcast.
     * {@code enterLobbyMultiPlayer} (CLIENT {@code 0x81}) also sends {@code pacote0F5}.
     */
    private void sendLobbyEnter(Session session, boolean sendAck) {
        PlayerContext pi = session.player();
        pi.inLobby = true;
        GamePackets.PlayerLobbyInfo self = makeLobbyInfo(session);
        List<GamePackets.PlayerLobbyInfo> lobby = lobbyPlayerInfos(pi.channelId);
        if (lobby.isEmpty()) {
            lobby = List.of(self);
        }
        session.send(GamePackets.lobbyUsers(GamePackets.LOBBY_USER_CLEAR, List.of(lobby.getFirst())));
        session.send(GamePackets.lobbyUsers(GamePackets.LOBBY_USER_LIST, lobby));
        session.send(GamePackets.roomList(GamePackets.ROOM_LIST_FULL, visibleRoomArrays(pi.channelId)));
        broadcastChannel(pi.channelId, GamePackets.lobbyUsers(GamePackets.LOBBY_USER_JOIN, List.of(self)));
        if (sendAck) {
            session.send(GamePackets.enterLobbyAck());
        }
    }

    private void leaveLobby(Session session) {
        if (!session.authorized()) {
            return;
        }
        PlayerContext pi = session.player();
        if (pi.roomNumber >= 0) {
            leaveRoom(session);
        }
        GamePackets.PlayerLobbyInfo info = makeLobbyInfo(session);
        pi.inLobby = false;
        if (pi.channelId >= 0) {
            broadcastChannel(pi.channelId, GamePackets.lobbyUsers(GamePackets.LOBBY_USER_LEAVE, List.of(info)));
        }
        session.send(GamePackets.leaveLobbyAck());
    }

    private void chat(Session session, PacketReader reader) {
        if (!session.authorized()) {
            return;
        }
        String nick = reader.remaining() >= 2 ? reader.pstr() : "";
        String msg = reader.remaining() >= 2 ? reader.pstr() : "";
        if (nick.isEmpty() || msg.isEmpty()) {
            return;
        }
        PlayerContext pi = session.player();
        String from = pi.nickname == null || pi.nickname.isEmpty() ? nick : pi.nickname;
        byte[] packet = GamePackets.chat(GamePackets.CHAT_NORMAL, from, msg);
        GameRoom room = rooms.get(pi.roomNumber);
        if (room != null) {
            room.broadcast(packet);
        } else if (pi.channelId >= 0) {
            broadcastChannel(pi.channelId, packet);
        }
    }

    private void setReady(Session session, PacketReader reader) {
        if (!session.authorized() || reader.remaining() < 1) {
            return;
        }
        int ready = reader.u8();
        GameRoom room = rooms.get(session.player().roomNumber);
        if (room == null) {
            return;
        }
        GamePackets.PlayerRoomInfo pri = room.playerInfo(session);
        if (pri == null) {
            return;
        }
        if (ready == 0) {
            pri.stateFlag |= GamePackets.PLAYER_READY_BIT;
        } else {
            pri.stateFlag &= ~GamePackets.PLAYER_READY_BIT;
        }
        room.putPlayerInfo(session, pri);
        room.broadcast(GamePackets.readyState(session.oid(), ready));
    }

    private void changeRoomInfo(Session session, PacketReader reader) {
        if (!session.authorized()) {
            return;
        }
        PlayerContext pi = session.player();
        GameRoom room = rooms.get(pi.roomNumber);
        if (room == null) {
            return;
        }
        if (room.info.master != (int) pi.uid) {
            return;
        }
        if (!room.applyInfoChange(reader)) {
            return;
        }
        room.broadcast(GamePackets.roomUpdate(room.info));
        sendLobbyRoomInfo(room, GamePackets.ROOM_LIST_UPDATE);
    }

    private GamePackets.PlayerLobbyInfo makeLobbyInfo(Session session) {
        PlayerContext pi = session.player();
        GamePackets.PlayerLobbyInfo info = new GamePackets.PlayerLobbyInfo();
        info.uid = (int) pi.uid;
        info.oid = session.oid();
        info.salaNumero = pi.roomNumber >= 0 ? pi.roomNumber : 0xFFFF;
        info.nick = pi.nickname == null ? "" : pi.nickname;
        info.level = pi.level;
        info.capability = pi.capability;
        info.flagVisibleGm = pi.gmVisible;
        info.teamPoint = 1000;
        info.nickDisplay = "@NT_" + info.nick;
        if (pi.away != 0) {
            info.state |= GamePackets.PLAYER_LOBBY_AWAY_BIT;
        }
        GamePackets.UserEquip equip = inventory.userEquip(pi.uid);
        info.title = equip.skinTypeid.length > 5 ? equip.skinTypeid[5] : 0;
        return info;
    }

    private List<GamePackets.PlayerLobbyInfo> lobbyPlayerInfos(int channelId) {
        List<GamePackets.PlayerLobbyInfo> out = new ArrayList<>();
        for (Session other : sessions.snapshot()) {
            PlayerContext pi = other.player();
            if (other.authorized() && pi.inLobby && pi.channelId == channelId) {
                out.add(makeLobbyInfo(other));
            }
        }
        return out;
    }

    private List<byte[]> visibleRoomArrays(int channelId) {
        List<byte[]> out = new ArrayList<>();
        for (GameRoom room : rooms.values()) {
            if (room.channelId == channelId && !room.hiddenFromLobby()) {
                out.add(room.info.toArray());
            }
        }
        return out;
    }

    private void sendLobbyRoomInfo(GameRoom room, int option) {
        if (room.hiddenFromLobby()) {
            return;
        }
        broadcastChannel(room.channelId, GamePackets.roomList(option, List.of(room.info.toArray())));
    }

    private void sendLobbyPlayerInfo(Session session, int option) {
        PlayerContext pi = session.player();
        if (!pi.inLobby || pi.channelId < 0) {
            return;
        }
        broadcastChannel(pi.channelId, GamePackets.lobbyUsers(option, List.of(makeLobbyInfo(session))));
    }

    private void broadcastChannel(int channelId, byte[] packet) {
        if (channelId < 0) {
            return;
        }
        for (Session other : sessions.snapshot()) {
            if (other.authorized() && other.player().channelId == channelId) {
                other.send(packet);
            }
        }
    }

    private void whisper(Session session, PacketReader reader) {
        if (!session.authorized()) {
            return;
        }
        String nick = reader.remaining() >= 2 ? reader.pstr() : "";
        String msg = reader.remaining() >= 2 ? reader.pstr() : "";
        if (nick.isEmpty() || msg.isEmpty()) {
            return;
        }
        Session target = sessions.findByNickname(nick);
        if (target == null || target.player().whisper != 1) {
            session.send(GamePackets.chatOffline(nick));
            return;
        }
        String from = session.player().nickname == null ? "" : session.player().nickname;
        String to = target.player().nickname == null ? nick : target.player().nickname;
        session.send(GamePackets.whisper(GamePackets.WHISPER_FROM, to, msg));
        target.send(GamePackets.whisper(GamePackets.WHISPER_TO, from, msg));
    }

    private void requestCash(Session session) {
        if (!session.authorized()) {
            return;
        }
        session.send(GamePackets.cookieBalance(inventory.cookie(session.player().uid)));
    }

    private void requestPlayerInfo(Session session, PacketReader reader) {
        if (!session.authorized() || reader.remaining() < 5) {
            return;
        }
        int uid = reader.u32();
        int season = reader.u8();
        var info = repo.playerInfo(uid & 0xffff_ffffL).orElse(null);
        if (info == null) {
            session.send(GamePackets.playerInfoAck(GamePackets.PLAYER_INFO_OK, 0, 0));
            return;
        }
        boolean viewerGm = (session.player().capability & 4) != 0;
        boolean targetGm = (info.capability() & 4) != 0;
        if (uid != (int) session.player().uid && !viewerGm && targetGm) {
            session.send(GamePackets.playerInfoAck(GamePackets.PLAYER_INFO_NO_GM, season, uid));
            return;
        }
        Session online = sessions.findByUid(info.uid());
        int oid = online == null ? 0 : online.oid();
        int sala = 0xffff;
        if (online != null && online.player().roomNumber >= 0) {
            sala = online.player().roomNumber & 0xffff;
        }
        GamePackets.UserEquip equip = inventory.userEquip(info.uid());
        GamePackets.CharacterInfo character = null;
        for (GamePackets.CharacterInfo c : inventory.characters(info.uid())) {
            if (c.id == equip.characterId || character == null) {
                character = c;
                if (c.id == equip.characterId) {
                    break;
                }
            }
        }
        for (byte[] pkt : GamePackets.playerInfoDump(
                (int) info.uid(),
                season,
                oid,
                sala,
                info.id(),
                info.nickname(),
                info.capability(),
                info.level(),
                character,
                equip)) {
            session.send(pkt);
        }
        session.send(GamePackets.playerInfoAck(GamePackets.PLAYER_INFO_OK, season, (int) info.uid()));
    }

    private void updateMacros(Session session, PacketReader reader) {
        if (!session.authorized() || reader.remaining() < GamePackets.MACRO_COUNT * GamePackets.MACRO_BYTES) {
            return;
        }
        String[] macros = new String[GamePackets.MACRO_COUNT];
        for (int i = 0; i < macros.length; i++) {
            macros[i] = reader.fixedStr(GamePackets.MACRO_BYTES);
        }
        repo.saveMacros(session.player().uid, macros);
    }

    private void requestServerList(Session session) {
        if (!session.authorized()) {
            return;
        }
        sendGameAndChannelList(session);
    }

    private void sendGameAndChannelList(Session session) {
        List<byte[]> servers = new ArrayList<>();
        for (LoginRepository.ServerListRow row : repo.serverList(GamePackets.SERVER_TYPE_GAME)) {
            servers.add(toServerInfo(row).toArray());
        }
        session.send(GamePackets.serverAndChannelList(servers, channels));
    }

    /**
     * C# {@code requestChangeServer}: unknown uid resends {@code pacote09F};
     * known GS → {@code pacote1D4} option 0 + PStr game key.
     */
    private void changeGameServer(Session session, PacketReader reader) {
        if (!session.authorized()) {
            return;
        }
        if (reader.remaining() < 4) {
            sendGameAndChannelList(session);
            return;
        }
        int serverUid = reader.u32();
        boolean known = repo.serverList(GamePackets.SERVER_TYPE_GAME).stream()
                .anyMatch(s -> s.uid() == serverUid);
        if (!known) {
            sendGameAndChannelList(session);
            return;
        }
        String key = repo.generateAuthKeyGame(session.player().uid, serverUid);
        try {
            redis.putGameKey(session.player().uid, serverUid, key);
        } catch (RuntimeException e) {
            log.warn("redis game key failed uid={}: {}", session.player().uid, e.toString());
        }
        session.send(GamePackets.changeGameServer(GamePackets.CHANGE_GS_OK, key));
    }

    /**
     * C# {@code packet08B}: {@code CmdServerList(TYPE_SERVER.MSN)} then
     * {@code pacote0FC} (u8 count + 92-byte rows). Empty list still sends count 0.
     */
    private void requestMessengerList(Session session) {
        if (!session.authorized()) {
            return;
        }
        List<byte[]> servers = new ArrayList<>();
        for (LoginRepository.ServerListRow row : repo.serverList(GamePackets.SERVER_TYPE_MSN)) {
            servers.add(toServerInfo(row).toArray());
        }
        session.send(GamePackets.messengerList(servers));
    }

    private static ServerInfo toServerInfo(LoginRepository.ServerListRow row) {
        ServerInfo info = new ServerInfo();
        info.name = row.name() == null ? "" : row.name();
        info.uid = row.uid();
        info.maxUser = row.maxUser();
        info.currUser = row.currUser();
        info.ip = row.ip() == null ? "" : row.ip();
        info.port = row.port();
        info.property = row.property();
        info.angelicWings = row.angelicWings();
        info.eventFlag = row.eventFlag();
        info.eventMap = row.eventMap();
        info.appRate = row.appRate();
        info.scratchRate = row.scratchRate();
        info.imgNo = row.imgNo();
        return info;
    }

    private void requestRank(Session session) {
        if (!session.authorized()) {
            return;
        }
        List<LoginRepository.ServerListRow> ranks = repo.serverList(GamePackets.SERVER_TYPE_RANK);
        if (ranks.isEmpty()) {
            return;
        }
        LoginRepository.ServerListRow rank = ranks.getFirst();
        session.send(GamePackets.rankAddress(rank.ip(), rank.port()));
    }

    /**
     * C# {@code packet09C}: {@code pacote10E} with 5 zeroed {@code LastPlayerGame} rows.
     */
    private void last5Players(Session session) {
        if (!session.authorized()) {
            return;
        }
        session.send(GamePackets.last5Players());
    }

    /**
     * C# {@code DailyQuestManager.requestCheckAndSendDailyQuest}: empty IFF table
     * still sets {@code current_date} and sends {@code 0x216} unix+0 then {@code pacote225}.
     */
    private void dailyQuest(Session session) {
        if (!inChannel(session)) {
            return;
        }
        PlayerContext pi = session.player();
        if (pi.dailyCurrentDate == 0) {
            pi.dailyCurrentDate = GamePackets.unixNow() & 0xffff_ffffL;
        }
        session.send(GamePackets.dailyQuestStamp(GamePackets.unixNow(), 0));
        session.send(GamePackets.dailyQuestInfo(
                0,
                (int) pi.dailyCurrentDate,
                (int) pi.dailyAcceptDate,
                pi.dailyCount,
                pi.dailyQuestTypeids,
                null));
    }

    /**
     * C# {@code requestAcceptQuest}: {@code num_quest<=0} (and missing quests) →
     * {@code pacote226(empty, 1)}.
     */
    private void acceptDailyQuest(Session session, PacketReader reader) {
        if (!inChannel(session)) {
            return;
        }
        skipDailyQuestIds(reader);
        session.send(GamePackets.dailyQuestAcceptFail());
    }

    /**
     * C# {@code requestTakeRewardQuest}: {@code num_quest<=0} → {@code pacote227(empty, 500050)}.
     */
    private void rewardDailyQuest(Session session, PacketReader reader) {
        if (!inChannel(session)) {
            return;
        }
        skipDailyQuestIds(reader);
        session.send(GamePackets.dailyQuestRewardFail());
    }

    /**
     * C# {@code requestLeaveQuest}: {@code num_quest<=0} → {@code pacote228(empty, 1)}.
     */
    private void leaveDailyQuest(Session session, PacketReader reader) {
        if (!inChannel(session)) {
            return;
        }
        skipDailyQuestIds(reader);
        session.send(GamePackets.dailyQuestLeaveFail());
    }

    private static void skipDailyQuestIds(PacketReader reader) {
        if (reader.remaining() < 4) {
            return;
        }
        int num = reader.i32();
        if (num <= 0) {
            return;
        }
        int bytes = Math.min(num, reader.remaining() / 4) * 4;
        if (bytes > 0) {
            reader.readBytes(bytes);
        }
    }

    /**
     * C# {@code packet157}: empty {@code map_ai} returns with no packet. Short body
     * throws → {@code pacote22C(1)}.
     */
    private void achievementGui(Session session, PacketReader reader) {
        if (!session.authorized()) {
            return;
        }
        if (reader.remaining() < 4) {
            session.send(GamePackets.achievementGui(GamePackets.ACHIEVEMENT_GUI_FAIL));
            return;
        }
        reader.u32();
    }

    /**
     * C# {@code requestDeleteActiveItem}: no IFF / wrong group → {@code 0xC5} sbyte -1.
     */
    private void deleteActiveItem(Session session, PacketReader reader) {
        if (!inChannel(session)) {
            return;
        }
        if (reader.remaining() >= 8) {
            reader.u32();
            reader.u32();
        }
        session.send(GamePackets.deleteItemFail());
    }

    /**
     * C# {@code requestSetNoticeBeginCaddieHolyDay}: invalid/missing/IFF fail is silent.
     */
    private void caddieHolidayNotice(Session session, PacketReader reader) {
        if (!inChannel(session)) {
            return;
        }
        if (reader.remaining() >= 4) {
            reader.i32();
        }
        if (reader.remaining() >= 1) {
            reader.u8();
        }
    }

    /**
     * C# {@code requestChangeWindNextHoleRepeat}: not-in-room catch is silent;
     * {@code GameBase} success is a no-op.
     */
    private void changeWindNextHole(Session session) {
        if (!inChannel(session)) {
            return;
        }
    }

    /**
     * C# {@code requestCadieCauldronExchange}: count 0/&gt;4 sys {@code 5200451};
     * truncated items / SQL miss {@code 5200452}; both written as {@code sys & 0xFFFF}.
     * Success is {@code 0x216} awards then {@code 0x22F} u32 0.
     */
    private void cadieExchange(Session session, PacketReader reader) {
        if (!inChannel(session)) {
            return;
        }
        if (reader.remaining() < 7) {
            session.send(GamePackets.cadieFail(GamePackets.CADIE_ERR_DEFAULT));
            return;
        }
        int seq = reader.u16();
        int requested = reader.u32();
        int count = reader.u8();
        if (count == 0 || count > GamePackets.CADIE_MAX_TRADE) {
            session.send(GamePackets.cadieFail(GamePackets.shopSys(GamePackets.CADIE_ERR_COUNT)));
            return;
        }
        if (reader.remaining() < count * 8) {
            session.send(GamePackets.cadieFail(GamePackets.shopSys(GamePackets.CADIE_ERR_IFF)));
            return;
        }
        int[] typeids = new int[count];
        int[] ids = new int[count];
        for (int i = 0; i < count; i++) {
            typeids[i] = reader.u32();
            ids[i] = reader.i32();
        }
        InventoryRepository.CadieExchangeResult result;
        try {
            result = inventory.cadieExchange(
                    session.player().uid, seq, requested, session.player().level, typeids, ids);
        } catch (RuntimeException e) {
            log.warn("cadie exchange uid={} failed: {}", session.player().uid, e.toString());
            session.send(GamePackets.cadieFail(GamePackets.CADIE_ERR_DEFAULT));
            return;
        }
        if (result.code() != 0) {
            session.send(GamePackets.cadieFail(GamePackets.shopSys(result.code())));
            return;
        }
        session.send(GamePackets.papelAwards(GamePackets.unixNow(), result.awards()));
        session.send(GamePackets.cadieOk(
                result.seq(),
                result.receiveTypeid(),
                result.receiveId(),
                result.receiveQntd(),
                result.qntdDep(),
                result.flagTime()));
    }

    /**
     * C# {@code requestLoloCardCompose}: truncated ToRead → full {@code 0x5400150};
     * CHANNEL codes as {@code shopSys}; success {@code 0xC8}/{@code 0x216}/
     * {@code 0x229}/{@code 0x22A}.
     */
    private void loloCardCompose(Session session, PacketReader reader) {
        if (!inChannel(session)) {
            return;
        }
        if (reader.remaining() < 20) {
            session.send(GamePackets.loloFail(GamePackets.LOLO_ERR_DEFAULT));
            return;
        }
        long pang = reader.u64();
        int t0 = reader.u32();
        int t1 = reader.u32();
        int t2 = reader.u32();
        InventoryRepository.LoloComposeResult result;
        try {
            result = inventory.loloCompose(session.player().uid, pang, t0, t1, t2);
        } catch (RuntimeException e) {
            log.warn("lolo compose uid={} failed: {}", session.player().uid, e.toString());
            session.send(GamePackets.loloFail(GamePackets.LOLO_ERR_DEFAULT));
            return;
        }
        if (result.code() != 0) {
            session.send(GamePackets.loloFail(GamePackets.shopSys(result.code())));
            return;
        }
        session.send(GamePackets.pangSpent(result.pangAfter(), result.pangSpent()));
        session.send(GamePackets.papelAwards(GamePackets.unixNow(), result.awards()));
        session.send(GamePackets.loloTipo(result.cardTipo()));
        session.send(GamePackets.loloOk(result.cardTypeid()));
    }

    /**
     * C# {@code requestClubSetStatsUpdate}: missing warehouse / IFF → {@code 0xA5}
     * u8 0. Opt 1/3 is clubset.
     */
    private void clubSetStats(Session session, PacketReader reader) {
        if (!inChannel(session)) {
            return;
        }
        if (reader.remaining() < 6) {
            session.send(GamePackets.clubStatsFail());
            return;
        }
        int opt = reader.u8();
        reader.u8();
        int itemId = reader.i32();
        if (opt != 1 && opt != 3) {
            return;
        }
        long uid = session.player().uid;
        boolean owned = inventory.warehouse(uid).stream().anyMatch(w -> w.id == itemId);
        if (!owned) {
            session.send(GamePackets.clubStatsFail());
        }
    }

    /**
     * C# {@code requestEnterGameAfterStarted}: missing room CHANNEL sys 1 →
     * {@code 0x113} u8 6 + u8 1.
     */
    private void enterGameAfterStarted(Session session, PacketReader reader) {
        if (!inChannel(session)) {
            return;
        }
        if (reader.remaining() < 1) {
            session.send(GamePackets.intrusionFail(GamePackets.INTRUSION_SYS));
            return;
        }
        int option = reader.u8();
        if (option == 0 || option == 1) {
            if (reader.remaining() < 2) {
                session.send(GamePackets.intrusionFail(GamePackets.INTRUSION_SYS));
                return;
            }
            int numero = reader.u16();
            if (rooms.get(numero) == null) {
                session.send(GamePackets.intrusionFail(GamePackets.INTRUSION_SYS));
            }
        }
    }

    /**
     * C# {@code requestUpdateGachaCoupon}: SQL {@code c0} for typeids
     * {@code 0x1A000080}/{@code 0x1A000083} then {@code pacote102}.
     */
    private void updateGachaCoupon(Session session) {
        if (!inChannel(session)) {
            return;
        }
        try {
            int normal = 0;
            int partial = 0;
            for (GamePackets.WarehouseItem item : inventory.warehouse(session.player().uid)) {
                if (item.typeid == GamePackets.TYPEID_GACHA_TICKET) {
                    normal = item.c[0];
                } else if (item.typeid == GamePackets.TYPEID_GACHA_SUB) {
                    partial = item.c[0];
                }
            }
            session.send(GamePackets.gachaCoupon(
                    normal,
                    partial,
                    inventory.pang(session.player().uid),
                    inventory.cookie(session.player().uid)));
        } catch (RuntimeException e) {
            session.send(GamePackets.gachaCouponFail(GamePackets.GACHA_ERR_DEFAULT));
        }
    }

    /**
     * C# {@code requestEnterWebLinkState}: ReadSByte {@code place}. No reply.
     */
    private void enterWebLink(Session session, PacketReader reader) {
        if (!inChannel(session) || reader.remaining() < 1) {
            return;
        }
        session.player().place = (byte) reader.u8();
    }

    /**
     * C# {@code requestExitedFromWebGuild}: {@code 0xC8} only when pang changed.
     * Java reads pang from SQL so seed matches in-memory and stays silent.
     */
    private void exitedFromWebGuild(Session session) {
        if (!inChannel(session)) {
            return;
        }
    }

    /**
     * C# {@code requestEnterSpyRoom}: locked room + matching password enters;
     * missing/unlocked/wrong password catch is silent.
     */
    private void enterSpyRoom(Session session, PacketReader reader) {
        if (!inChannel(session) || reader.remaining() < 2) {
            return;
        }
        int numero = reader.u16() & 0xffff;
        String senha = reader.remaining() >= 2 ? reader.pstr() : "";
        GameRoom room = rooms.get(numero);
        if (room == null || room.info.senhaFlag != 0) {
            return;
        }
        if (!room.info.password.equals(senha == null ? "" : senha)) {
            return;
        }
        if (room.inGame) {
            return;
        }
        enterExistingRoom(session, room);
    }

    /**
     * C# {@code requestCommonCmdGM}: non-GM / fail {@code 0x40} red notice;
     * {@code CCG_VISIBLE} sets {@code GMInfo.visible} and broadcasts lobby
     * {@code 0x46} option 3 then green {@code Executed Command.}.
     */
    private void commonCmdGm(Session session, PacketReader reader) {
        if (!session.authorized() || reader.remaining() < 2) {
            return;
        }
        int cmd = reader.i16();
        String nick = session.player().nickname == null ? "" : session.player().nickname;
        if ((session.player().capability & GamePackets.CAPABILITY_GM) == 0) {
            session.send(GamePackets.chat(
                    GamePackets.CHAT_NOTICE,
                    nick,
                    GamePackets.chatColor(GamePackets.CHAT_RED_HEX, GamePackets.GM_CMD_FAIL)));
            return;
        }
        if (cmd != GamePackets.GM_CMD_VISIBLE) {
            session.send(GamePackets.chat(
                    GamePackets.CHAT_NOTICE,
                    nick,
                    GamePackets.chatColor(GamePackets.CHAT_RED_HEX, GamePackets.GM_CMD_FAIL)));
            return;
        }
        if (!inChannel(session) || reader.remaining() < 2) {
            session.send(GamePackets.chat(
                    GamePackets.CHAT_NOTICE,
                    nick,
                    GamePackets.chatColor(GamePackets.CHAT_RED_HEX, GamePackets.GM_CMD_FAIL)));
            return;
        }
        int visible = reader.u16() & 1;
        PlayerContext pi = session.player();
        if (pi.roomNumber >= 0 && rooms.get(pi.roomNumber) == null) {
            session.send(GamePackets.chat(
                    GamePackets.CHAT_NOTICE,
                    nick,
                    GamePackets.chatColor(GamePackets.CHAT_RED_HEX, GamePackets.GM_CMD_FAIL)));
            return;
        }
        pi.gmVisible = visible;
        if (pi.channelId >= 0) {
            broadcastChannel(pi.channelId, GamePackets.lobbyUsers(
                    GamePackets.LOBBY_USER_UPDATE, List.of(makeLobbyInfo(session))));
        }
        GameRoom room = rooms.get(pi.roomNumber);
        if (room != null) {
            GamePackets.PlayerRoomInfo pri = room.playerInfo(session);
            if (pri != null) {
                int base = GamePackets.usesCompactPlayerRoomInfo(room.tipo) ? 0x100 : 0;
                room.broadcast(GamePackets.roomPlayers(base + 3, List.of(pri)));
            }
        }
        session.send(GamePackets.chat(
                GamePackets.CHAT_NOTICE,
                nick,
                GamePackets.chatColor(GamePackets.CHAT_GREEN_HEX, GamePackets.GM_CMD_OK)));
    }

    /**
     * C# {@code requestActiveAutoCommand}: not-in-room CHANNEL catch is silent.
     */
    private void activeAutoCommand(Session session) {
        if (!inChannel(session)) {
            return;
        }
    }

    /**
     * C# {@code packet0FB}: {@code pacote1AD} option 1 + PStr key; catch option 0
     * + i16 0.
     */
    private void webAuthKey(Session session) {
        if (!session.authorized()) {
            return;
        }
        try {
            session.send(GamePackets.webAuthKey(GamePackets.WEB_KEY_OK, repo.generateWebKey(session.player().uid)));
        } catch (RuntimeException e) {
            session.send(GamePackets.webAuthKey(GamePackets.WEB_KEY_FAIL, ""));
        }
    }

    /**
     * C# {@code requestOpenTicketReportScroll}: ItemManager miss → {@code 0x11A}
     * i32 -1 + 16 zero date bytes.
     */
    private void openTicketReport(Session session, PacketReader reader) {
        if (!inChannel(session)) {
            return;
        }
        session.send(GamePackets.ticketReportFail());
    }

    /**
     * C# {@code requestUseTicketReport}: not-in-room CHANNEL catch is silent.
     */
    private void useTicketReport(Session session) {
        if (!inChannel(session)) {
            return;
        }
    }

    /**
     * C# {@code requestMakeTutorial}: unknown tipo → {@code 0x44} u8 {@code 0xE2}
     * + {@code shopSys(0x5300552)}.
     */
    private void makeTutorial(Session session, PacketReader reader) {
        if (!inChannel(session)) {
            return;
        }
        if (reader.remaining() < 6) {
            session.send(GamePackets.tutorialFail(GamePackets.TUTORIAL_ERR_DEFAULT));
            return;
        }
        reader.u8();
        int tipo = reader.u8();
        reader.u32();
        if (tipo != 0 && tipo != 1 && tipo != 2) {
            session.send(GamePackets.tutorialFail(GamePackets.shopSys(GamePackets.TUTORIAL_ERR_TIPO)));
        }
    }

    /**
     * C# {@code requestOpenBoxMyRoom}: catch always {@code 0x129} u8 1 + 12 zeros.
     */
    private void openLuckyPouch(Session session, PacketReader reader) {
        if (!inChannel(session)) {
            return;
        }
        session.send(GamePackets.luckyPouchFail());
    }

    /**
     * C# {@code packet0C1}: ReadSByte {@code place}. No reply.
     */
    private void updatePlace(Session session, PacketReader reader) {
        if (!session.authorized() || reader.remaining() < 1) {
            return;
        }
        session.player().place = (byte) reader.u8();
    }

    /**
     * C# {@code requestCheckDolfiniLockerPass}: empty → sys 1; seed has no pass
     * so a non-empty 1–4 char pass is {@code 0x75}.
     */
    private void lockerAccess(Session session, PacketReader reader) {
        if (!inChannel(session)) {
            return;
        }
        if (reader.remaining() < 2) {
            session.send(GamePackets.lockerAccess(GamePackets.LOCKER_ERR_DEFAULT));
            return;
        }
        String pass = reader.pstr();
        if (pass == null || pass.isEmpty() || !mailSanitize(pass)) {
            session.send(GamePackets.lockerAccess(GamePackets.LOCKER_ERR_EMPTY));
            return;
        }
        if (pass.length() > 4) {
            session.send(GamePackets.lockerAccess(GamePackets.shopSys(5100152)));
            return;
        }
        String stored = session.player().dolfiniPass;
        if (stored == null) {
            stored = "";
        }
        if (!pass.equals(stored)) {
            session.send(GamePackets.lockerAccess(GamePackets.LOCKER_ERR_WRONG));
            return;
        }
        session.send(GamePackets.lockerAccess(0));
    }

    /**
     * C# {@code packet0D3}: {@code 0x170} option 0 + {@code isLocker()}. Empty
     * pass is 2.
     */
    private void lockerState(Session session) {
        if (!session.authorized()) {
            return;
        }
        session.send(GamePackets.lockerState(GamePackets.LOCKER_STATE_NO_PASS));
    }

    /**
     * C# {@code requestClubSetWorkShopUpLevel}: unknown IFF group →
     * {@code 0x23D} {@code shopSys(0x5300201)}.
     */
    private void clubWorkshopLevel(Session session, PacketReader reader) {
        if (!inChannel(session)) {
            return;
        }
        if (reader.remaining() < 10) {
            session.send(GamePackets.clubWorkshopFail(GamePackets.WORKSHOP_ERR_DEFAULT));
            return;
        }
        int typeid = reader.u32();
        reader.u16();
        reader.i32();
        int group = GamePackets.itemGroupIdentify(typeid);
        if (group != GamePackets.IFF_GROUP_ITEM) {
            session.send(GamePackets.clubWorkshopFail(GamePackets.shopSys(GamePackets.WORKSHOP_ERR_GROUP)));
            return;
        }
        session.send(GamePackets.clubWorkshopFail(GamePackets.shopSys(0x5300202)));
    }

    /**
     * C# {@code requestClubSetWorkShopUpLevelConfirm}: no pending ClubSet →
     * {@code 0x23E} {@code shopSys(0x5300301)}.
     */
    private void clubWorkshopConfirm(Session session) {
        if (!inChannel(session)) {
            return;
        }
        session.send(GamePackets.clubWorkshopOpcodeFail(
                GamePackets.SERVER_CLUB_WORKSHOP_CONFIRM,
                GamePackets.shopSys(GamePackets.WORKSHOP_CONFIRM_ERR)));
    }

    /**
     * C# {@code requestClubSetWorkShopUpLevelCancel}: no pending ClubSet →
     * {@code 0x23F} {@code shopSys(0x5300251)}.
     */
    private void clubWorkshopCancel(Session session) {
        if (!inChannel(session)) {
            return;
        }
        session.send(GamePackets.clubWorkshopOpcodeFail(
                GamePackets.SERVER_CLUB_WORKSHOP_CANCEL,
                GamePackets.shopSys(GamePackets.WORKSHOP_CANCEL_ERR)));
    }

    /**
     * C# {@code requestClubSetWorkShopUpRank}: qntd&gt;0 missing card →
     * {@code 0x240} {@code shopSys(0x5300351)}.
     */
    private void clubWorkshopRank(Session session, PacketReader reader) {
        if (!inChannel(session)) {
            return;
        }
        if (reader.remaining() < 10) {
            session.send(GamePackets.clubWorkshopOpcodeFail(
                    GamePackets.SERVER_CLUB_WORKSHOP_RANK, GamePackets.WORKSHOP_RANK_DEFAULT));
            return;
        }
        reader.u32();
        int qntd = reader.u16();
        reader.i32();
        if (qntd > 0) {
            session.send(GamePackets.clubWorkshopOpcodeFail(
                    GamePackets.SERVER_CLUB_WORKSHOP_RANK,
                    GamePackets.shopSys(GamePackets.WORKSHOP_RANK_ERR)));
            return;
        }
        session.send(GamePackets.clubWorkshopOpcodeFail(
                GamePackets.SERVER_CLUB_WORKSHOP_RANK, GamePackets.WORKSHOP_RANK_DEFAULT));
    }

    /**
     * C# {@code requestUseItemBuff}: typeid 0 → {@code 0x181}
     * {@code shopSys(0x5500401)}.
     */
    private void useItemBuff(Session session, PacketReader reader) {
        if (!inChannel(session)) {
            return;
        }
        if (reader.remaining() < 4) {
            session.send(GamePackets.itemBuffFail(GamePackets.BUFF_ERR_DEFAULT));
            return;
        }
        int typeid = reader.u32();
        if (typeid == 0) {
            session.send(GamePackets.itemBuffFail(GamePackets.shopSys(GamePackets.BUFF_ERR_TYPEID)));
            return;
        }
        session.send(GamePackets.itemBuffFail(GamePackets.shopSys(0x5500402)));
    }

    /**
     * C# {@code requestCometRefill}: catch always {@code 0x197} u8 0 + 10 zeros.
     */
    private void cometRefill(Session session, PacketReader reader) {
        if (!inChannel(session)) {
            return;
        }
        session.send(GamePackets.cometRefillFail());
    }

    /**
     * C# {@code requestOpenBoxMail}: typeid 0 → {@code 0x19D}
     * {@code shopSys(0x6300101)}.
     */
    private void openBoxMail(Session session, PacketReader reader) {
        if (!inChannel(session)) {
            return;
        }
        if (reader.remaining() < 4) {
            session.send(GamePackets.boxMailFail(GamePackets.BOX_MAIL_ERR_DEFAULT));
            return;
        }
        int typeid = reader.u32();
        if (typeid == 0) {
            session.send(GamePackets.boxMailFail(GamePackets.shopSys(GamePackets.BOX_MAIL_ERR_TYPEID)));
            return;
        }
        session.send(GamePackets.boxMailFail(GamePackets.shopSys(0x6300102)));
    }

    /**
     * C# {@code requestDolfiniLockerItem}: empty locker page 1 →
     * {@code 0x16D} pages 0 + page 0 + count 0.
     */
    private void lockerItems(Session session, PacketReader reader) {
        if (!inChannel(session)) {
            return;
        }
        if (reader.remaining() < 6) {
            session.send(GamePackets.lockerItemsFail());
            return;
        }
        reader.u32();
        reader.u16();
        session.send(GamePackets.lockerItems(0, 0, 0));
    }

    /**
     * C# {@code requestDolfiniLockerPang}: {@code 0x172} u64 pang (seed 0).
     */
    private void lockerPang(Session session) {
        if (!inChannel(session)) {
            return;
        }
        session.send(GamePackets.lockerPang(0));
    }

    /**
     * C# {@code packet0B5}: seed {@code allow_enter==0} → {@code 0x12B} option 0
     * + to_uid. No channel required.
     */
    private void myRoomCheck(Session session, PacketReader reader) {
        if (!session.authorized() || reader.remaining() < 8) {
            return;
        }
        reader.u32();
        int toUid = reader.u32();
        session.send(GamePackets.myRoomCheck(GamePackets.MY_ROOM_DENY, toUid));
    }

    /**
     * C# {@code requestMakePassDolfiniLocker}: empty/{@code Sanitize} →
     * {@code 0x176} u32 1; length&gt;4 → {@code shopSys(5100102)}.
     */
    private void lockerMakePass(Session session, PacketReader reader) {
        if (!inChannel(session)) {
            return;
        }
        if (reader.remaining() < 2) {
            session.send(GamePackets.sysAck(
                    GamePackets.SERVER_LOCKER_MAKE_PASS, GamePackets.LOCKER_MAKE_PASS_DEFAULT));
            return;
        }
        String pass = reader.pstr();
        if (pass == null || pass.isEmpty() || !mailSanitize(pass)) {
            session.send(GamePackets.sysAck(
                    GamePackets.SERVER_LOCKER_MAKE_PASS, GamePackets.LOCKER_MAKE_PASS_EMPTY));
            return;
        }
        if (pass.length() > 4) {
            session.send(GamePackets.sysAck(
                    GamePackets.SERVER_LOCKER_MAKE_PASS,
                    GamePackets.shopSys(GamePackets.LOCKER_MAKE_PASS_LEN)));
            return;
        }
        session.player().dolfiniPass = pass;
        session.send(GamePackets.sysAck(GamePackets.SERVER_LOCKER_MAKE_PASS, 0));
    }

    /**
     * C# {@code requestChangeDolfiniLockerPass}: empty old → {@code 0x174} u32 1.
     */
    private void lockerChangePass(Session session, PacketReader reader) {
        if (!inChannel(session)) {
            return;
        }
        if (reader.remaining() < 2) {
            session.send(GamePackets.sysAck(
                    GamePackets.SERVER_LOCKER_CHANGE_PASS, GamePackets.LOCKER_CHANGE_PASS_DEFAULT));
            return;
        }
        String oldPass = reader.pstr();
        if (oldPass == null || oldPass.isEmpty() || !mailSanitize(oldPass)) {
            session.send(GamePackets.sysAck(
                    GamePackets.SERVER_LOCKER_CHANGE_PASS, GamePackets.LOCKER_CHANGE_PASS_WRONG));
            return;
        }
        if (reader.remaining() < 2) {
            session.send(GamePackets.sysAck(
                    GamePackets.SERVER_LOCKER_CHANGE_PASS, GamePackets.LOCKER_CHANGE_PASS_DEFAULT));
            return;
        }
        String newPass = reader.pstr();
        if (newPass == null || newPass.isEmpty() || !mailSanitize(newPass)) {
            session.send(GamePackets.sysAck(
                    GamePackets.SERVER_LOCKER_CHANGE_PASS, GamePackets.LOCKER_CHANGE_PASS_WRONG));
            return;
        }
        if (oldPass.length() > 4 || newPass.length() > 4) {
            session.send(GamePackets.sysAck(
                    GamePackets.SERVER_LOCKER_CHANGE_PASS,
                    GamePackets.shopSys(GamePackets.LOCKER_CHANGE_PASS_LEN)));
            return;
        }
        String stored = session.player().dolfiniPass;
        if (stored == null) {
            stored = "";
        }
        if (!oldPass.equals(stored)) {
            session.send(GamePackets.sysAck(
                    GamePackets.SERVER_LOCKER_CHANGE_PASS, GamePackets.LOCKER_CHANGE_PASS_WRONG));
            return;
        }
        session.player().dolfiniPass = newPass;
        session.send(GamePackets.sysAck(GamePackets.SERVER_LOCKER_CHANGE_PASS, 0));
    }

    /**
     * C# {@code requestChangeDolfiniLockerModeEnter}: empty pass →
     * {@code 0x173} {@code shopSys(5100251)}.
     */
    private void lockerMode(Session session, PacketReader reader) {
        if (!inChannel(session)) {
            return;
        }
        if (reader.remaining() < 3) {
            session.send(GamePackets.sysAck(
                    GamePackets.SERVER_LOCKER_MODE, GamePackets.LOCKER_MODE_DEFAULT));
            return;
        }
        reader.u8();
        String pass = reader.pstr();
        if (pass == null || pass.isEmpty()) {
            session.send(GamePackets.sysAck(
                    GamePackets.SERVER_LOCKER_MODE,
                    GamePackets.shopSys(GamePackets.LOCKER_MODE_EMPTY)));
            return;
        }
        if (pass.length() > 4) {
            session.send(GamePackets.sysAck(
                    GamePackets.SERVER_LOCKER_MODE, GamePackets.shopSys(5100252)));
            return;
        }
        String stored = session.player().dolfiniPass;
        if (stored == null) {
            stored = "";
        }
        if (!pass.equals(stored)) {
            session.send(GamePackets.sysAck(GamePackets.SERVER_LOCKER_MODE, 1));
            return;
        }
        session.send(GamePackets.sysAck(GamePackets.SERVER_LOCKER_MODE, 0));
    }

    /**
     * C# {@code requestAddDolfiniLockerItem}: count 0 → {@code 0x16E}
     * {@code shopSys(5100404)}.
     */
    private void lockerAdd(Session session, PacketReader reader) {
        if (!inChannel(session)) {
            return;
        }
        if (reader.remaining() < 1) {
            session.send(GamePackets.sysAck(
                    GamePackets.SERVER_LOCKER_ADD, GamePackets.LOCKER_ADD_ERR_DEFAULT));
            return;
        }
        int count = reader.u8();
        if (count == 0) {
            session.send(GamePackets.sysAck(
                    GamePackets.SERVER_LOCKER_ADD,
                    GamePackets.shopSys(GamePackets.LOCKER_ADD_ERR_NONE)));
            return;
        }
        session.send(GamePackets.sysAck(
                GamePackets.SERVER_LOCKER_ADD, GamePackets.LOCKER_ADD_ERR_DEFAULT));
    }

    /**
     * C# {@code requestRemoveDolfiniLockerItem}: truncated → {@code 0x16F}
     * else {@code 5100450}.
     */
    private void lockerRemove(Session session, PacketReader reader) {
        if (!inChannel(session)) {
            return;
        }
        if (reader.remaining() < 1) {
            session.send(GamePackets.sysAck(
                    GamePackets.SERVER_LOCKER_REMOVE, GamePackets.LOCKER_REMOVE_ERR_DEFAULT));
            return;
        }
        int count = reader.u8();
        if (count == 0) {
            session.send(GamePackets.sysAck(
                    GamePackets.SERVER_LOCKER_REMOVE,
                    GamePackets.shopSys(GamePackets.LOCKER_ADD_ERR_NONE)));
            return;
        }
        session.send(GamePackets.sysAck(
                GamePackets.SERVER_LOCKER_REMOVE, GamePackets.LOCKER_REMOVE_ERR_DEFAULT));
    }

    /**
     * C# {@code requestUpdateDolfiniLockerPang}: opt 0 pang&gt;locker (seed 0)
     * → {@code 0x171} {@code shopSys(5100353)}.
     */
    private void lockerUpdatePang(Session session, PacketReader reader) {
        if (!inChannel(session)) {
            return;
        }
        if (reader.remaining() < 9) {
            session.send(GamePackets.sysAck(
                    GamePackets.SERVER_LOCKER_UPDATE_PANG, GamePackets.LOCKER_PANG_ERR_DEFAULT));
            return;
        }
        int opt = reader.u8();
        long pang = reader.u64();
        if (opt == 0 && pang > 0) {
            session.send(GamePackets.sysAck(
                    GamePackets.SERVER_LOCKER_UPDATE_PANG,
                    GamePackets.shopSys(GamePackets.LOCKER_PANG_WITHDRAW_ERR)));
            return;
        }
        if (opt != 0 && opt != 1) {
            session.send(GamePackets.sysAck(
                    GamePackets.SERVER_LOCKER_UPDATE_PANG, GamePackets.shopSys(5100351)));
            return;
        }
        session.send(GamePackets.sysAck(
                GamePackets.SERVER_LOCKER_UPDATE_PANG, GamePackets.LOCKER_PANG_ERR_DEFAULT));
    }

    /**
     * C# {@code requestUseCardSpecial}: typeid 0 → {@code 0x160}
     * {@code shopSys(0x5500351)}.
     */
    private void useCardSpecial(Session session, PacketReader reader) {
        if (!inChannel(session)) {
            return;
        }
        if (reader.remaining() < 4) {
            session.send(GamePackets.sysAck(
                    GamePackets.SERVER_USE_CARD, GamePackets.CARD_ERR_DEFAULT));
            return;
        }
        int typeid = reader.u32();
        if (typeid == 0) {
            session.send(GamePackets.sysAck(
                    GamePackets.SERVER_USE_CARD, GamePackets.shopSys(GamePackets.CARD_ERR_TYPEID)));
            return;
        }
        session.send(GamePackets.sysAck(
                GamePackets.SERVER_USE_CARD, GamePackets.shopSys(0x5500352)));
    }

    /**
     * C# {@code requestOpenCardPack}: catch always {@code 0x154} u32 1.
     */
    private void openCardPack(Session session, PacketReader reader) {
        if (!inChannel(session)) {
            return;
        }
        session.send(GamePackets.sysAck(
                GamePackets.SERVER_OPEN_CARD_PACK, GamePackets.CARD_PACK_ERR));
    }

    /**
     * C# {@code requestExtendRental}: catch always {@code 0x18F} u8 1.
     */
    private void extendRental(Session session, PacketReader reader) {
        if (!inChannel(session)) {
            return;
        }
        session.send(GamePackets.rentalFail(GamePackets.SERVER_EXTEND_RENTAL));
    }

    /**
     * C# {@code requestDeleteRental}: catch always {@code 0x190} u8 1.
     */
    private void deleteRental(Session session, PacketReader reader) {
        if (!inChannel(session)) {
            return;
        }
        session.send(GamePackets.rentalFail(GamePackets.SERVER_DELETE_RENTAL));
    }

    /**
     * C# {@code requestClubSetWorkShopUpRankTransformConfirm}: no pending
     * ClubSet → {@code 0x242} {@code shopSys(0x5300451)}.
     */
    private void workshopTransformConfirm(Session session) {
        if (!inChannel(session)) {
            return;
        }
        session.send(GamePackets.sysAck(
                GamePackets.SERVER_WORKSHOP_TRANSFORM_CONFIRM,
                GamePackets.shopSys(GamePackets.WORKSHOP_TRANSFORM_CONFIRM_ERR)));
    }

    /**
     * C# {@code requestClubSetWorkShopUpRankTransformCancel}: no pending
     * ClubSet → {@code 0x243} {@code shopSys(0x5300401)}.
     */
    private void workshopTransformCancel(Session session) {
        if (!inChannel(session)) {
            return;
        }
        session.send(GamePackets.sysAck(
                GamePackets.SERVER_WORKSHOP_TRANSFORM_CANCEL,
                GamePackets.shopSys(GamePackets.WORKSHOP_TRANSFORM_CANCEL_ERR)));
    }

    /**
     * C# {@code requestClubSetWorkShopRecoveryPts}: missing warehouse →
     * {@code 0x246} {@code shopSys(0x5300151)}.
     */
    private void workshopRecovery(Session session, PacketReader reader) {
        if (!inChannel(session)) {
            return;
        }
        if (reader.remaining() < 8) {
            session.send(GamePackets.sysAck(
                    GamePackets.SERVER_WORKSHOP_RECOVERY, GamePackets.WORKSHOP_RECOVERY_DEFAULT));
            return;
        }
        int typeid = reader.u32();
        reader.i32();
        if (typeid == 0) {
            session.send(GamePackets.sysAck(
                    GamePackets.SERVER_WORKSHOP_RECOVERY,
                    GamePackets.shopSys(GamePackets.WORKSHOP_RECOVERY_ERR)));
            return;
        }
        session.send(GamePackets.sysAck(
                GamePackets.SERVER_WORKSHOP_RECOVERY, GamePackets.shopSys(0x5300152)));
    }

    /**
     * C# {@code requestClubSetWorkShopTransferMasteryPts}: missing UCIM →
     * {@code 0x245} {@code shopSys(0x5300104)}.
     */
    private void workshopTransfer(Session session, PacketReader reader) {
        if (!inChannel(session)) {
            return;
        }
        if (reader.remaining() < 16) {
            session.send(GamePackets.sysAck(
                    GamePackets.SERVER_WORKSHOP_TRANSFER, GamePackets.WORKSHOP_TRANSFER_DEFAULT));
            return;
        }
        int typeid = reader.u32();
        reader.i32();
        reader.i32();
        reader.u32();
        if (typeid == 0) {
            session.send(GamePackets.sysAck(
                    GamePackets.SERVER_WORKSHOP_TRANSFER,
                    GamePackets.shopSys(GamePackets.WORKSHOP_TRANSFER_ERR)));
            return;
        }
        session.send(GamePackets.sysAck(
                GamePackets.SERVER_WORKSHOP_TRANSFER, GamePackets.shopSys(0x5300104)));
    }

    /**
     * C# {@code requestClubSetReset}: unknown typeid → {@code 0x247}
     * {@code shopSys(0x5300506)}.
     */
    private void clubSetReset(Session session, PacketReader reader) {
        if (!inChannel(session)) {
            return;
        }
        if (reader.remaining() < 8) {
            session.send(GamePackets.sysAck(
                    GamePackets.SERVER_CLUBSET_RESET, GamePackets.CLUBSET_RESET_DEFAULT));
            return;
        }
        int typeid = reader.u32();
        reader.i32();
        if (typeid != 0x1A00024B && typeid != 0x1A000247) {
            session.send(GamePackets.sysAck(
                    GamePackets.SERVER_CLUBSET_RESET,
                    GamePackets.shopSys(GamePackets.CLUBSET_RESET_ERR)));
            return;
        }
        session.send(GamePackets.sysAck(
                GamePackets.SERVER_CLUBSET_RESET, GamePackets.shopSys(0x5300501)));
    }

    /**
     * C# {@code HandleUCC}: unknown opt / IFF miss always catch
     * {@code 0x12E} sbyte -1. No channel.
     */
    private void handleUcc(Session session, PacketReader reader) {
        if (!session.authorized()) {
            return;
        }
        if (reader.remaining() >= 1) {
            reader.u8();
        }
        session.send(GamePackets.uccFail());
    }

    /**
     * C# {@code requestUCCWebKey}: uid 0 → {@code 0x153} u8 1 + u8 1 +
     * {@code shopSys(0x5100101)}. No channel.
     */
    private void uccWebKey(Session session, PacketReader reader) {
        if (!session.authorized()) {
            return;
        }
        if (reader.remaining() < 10) {
            session.send(GamePackets.uccWebKeyFail(GamePackets.UCC_WEB_KEY_ERR_DEFAULT));
            return;
        }
        reader.u8();
        int uid = reader.u32();
        reader.u8();
        reader.i32();
        if (uid == 0) {
            session.send(GamePackets.uccWebKeyFail(
                    GamePackets.shopSys(GamePackets.UCC_WEB_KEY_ERR_UID)));
            return;
        }
        session.send(GamePackets.uccWebKeyFail(
                GamePackets.shopSys(0x5100103)));
    }

    /**
     * C# {@code requestCheckAttendance}: empty catalog {@code drawReward}
     * throws then catch writes {@code 0x248} u32 {@code ~0}.
     */
    private void checkAttendance(Session session) {
        if (!inChannel(session)) {
            return;
        }
        session.send(GamePackets.sysAck(
                GamePackets.SERVER_ATTENDANCE, GamePackets.ATTENDANCE_FAIL));
    }

    /**
     * C# {@code requestUpdateCountLogin}: empty catalog → {@code 0x249}
     * u32 {@code ~0}.
     */
    private void attendanceLoginCount(Session session) {
        if (!inChannel(session)) {
            return;
        }
        session.send(GamePackets.sysAck(
                GamePackets.SERVER_ATTENDANCE_LOGIN, GamePackets.ATTENDANCE_FAIL));
    }

    /**
     * C# {@code requestOpenClubWorkShopEvent}: always {@code pacote24E}.
     */
    private void openClubWorkshopEvent(Session session) {
        if (!inChannel(session)) {
            return;
        }
        session.send(GamePackets.workshopEvent());
    }

    /**
     * C# {@code enterLobbyGrandPrix}: {@code uProperty.grand_prix} (bit 11)
     * required; already-in-lobby CHANNEL sys 0 → {@code 0x250} u32 0;
     * else {@code enterLobby} without {@code 0xF5} then OK body.
     */
    private void enterLobbyGrandPrix(Session session) {
        if (!inChannel(session)) {
            return;
        }
        if ((config.property() & GamePackets.PROPERTY_GRAND_PRIX) == 0) {
            session.send(GamePackets.sysAck(
                    GamePackets.SERVER_GP_LOBBY,
                    GamePackets.shopSys(GamePackets.GP_LOBBY_ERR_DISABLED)));
            return;
        }
        if (session.player().inLobby) {
            session.send(GamePackets.sysAck(GamePackets.SERVER_GP_LOBBY, 0));
            return;
        }
        sendLobbyEnter(session, false);
        session.send(GamePackets.gpLobbyOk(config.rateGrandPrixEvent(), 0f));
    }

    /**
     * C# {@code leaveLobbyGrandPrix}: {@code leaveLobby} (no {@code 0xF6})
     * then {@code 0x251} u32 0. Catch silent.
     */
    private void leaveLobbyGrandPrix(Session session) {
        if (!inChannel(session)) {
            return;
        }
        PlayerContext pi = session.player();
        if (pi.roomNumber >= 0) {
            leaveRoom(session);
        }
        GamePackets.PlayerLobbyInfo info = makeLobbyInfo(session);
        pi.inLobby = false;
        if (pi.channelId >= 0) {
            broadcastChannel(pi.channelId, GamePackets.lobbyUsers(GamePackets.LOBBY_USER_LEAVE, List.of(info)));
        }
        session.send(GamePackets.sysAck(GamePackets.SERVER_GP_LEAVE, 0));
    }

    /**
     * C# {@code requestEnterRoomGrandPrix}: IFF miss typeid 0 → {@code 0x253}
     * {@code shopSys(0x6700001)}.
     */
    private void enterRoomGrandPrix(Session session, PacketReader reader) {
        if (!inChannel(session)) {
            return;
        }
        if (reader.remaining() < 4) {
            session.send(GamePackets.sysAck(
                    GamePackets.SERVER_START_GAME_FAIL, GamePackets.GP_ENTER_ERR_DEFAULT));
            return;
        }
        reader.u32();
        session.send(GamePackets.sysAck(
                GamePackets.SERVER_START_GAME_FAIL,
                GamePackets.shopSys(GamePackets.GP_ENTER_ERR_IFF)));
    }

    /**
     * C# {@code requestEnterMyRoom}: {@code 0x168} {@code PlayerRoomInfoEx}
     * then {@code 0x12D} option 1 + empty posters. Catch silent.
     */
    private void enterMyRoom(Session session) {
        if (!inChannel(session)) {
            return;
        }
        session.send(GamePackets.myRoomCharacter(makeMyRoomPlayerInfo(session)));
        session.send(GamePackets.myRoomPosters(GamePackets.MY_ROOM_POSTERS_OPTION, 0));
    }

    /**
     * C# My Room {@code PlayerRoomInfoEx}: position 0, master+ready, place 0x0A.
     */
    private GamePackets.PlayerRoomInfo makeMyRoomPlayerInfo(Session session) {
        PlayerContext pi = session.player();
        GamePackets.PlayerRoomInfo pri = new GamePackets.PlayerRoomInfo();
        pri.oid = session.oid();
        pri.nickname = pi.nickname == null ? "" : pi.nickname;
        pri.position = 0;
        pri.capability = pi.capability;
        pri.uid = (int) pi.uid;
        pri.level = pi.level;
        pri.place = 10;
        pri.stateFlag = GamePackets.PLAYER_MASTER_BIT | GamePackets.PLAYER_READY_BIT;
        GamePackets.UserEquip equip = inventory.userEquip(pi.uid);
        System.arraycopy(equip.skinTypeid, 0, pri.skin, 0, Math.min(6, equip.skinTypeid.length));
        pri.skin[4] = 0;
        pri.title = equip.skinTypeid.length > 5 ? equip.skinTypeid[5] : 0;
        for (GamePackets.CharacterInfo c : inventory.characters(pi.uid)) {
            if (c.id == equip.characterId || pri.character == null) {
                pri.character = c;
                pri.charTypeid = c.typeid;
                if (c.id == equip.characterId) {
                    break;
                }
            }
        }
        for (GamePackets.MascotInfo m : inventory.mascots(pi.uid)) {
            if (m.id == equip.mascotId) {
                pri.mascotTypeid = m.typeid;
                break;
            }
        }
        return pri;
    }

    /**
     * C# {@code requestCharacterMasteryExpand}: truncated ToRead → full
     * {@code 0x5200650}; CHANNEL codes as {@code shopSys}; success {@code 0x216}
     * then {@code 0x26E} u32 0.
     */
    private void characterMasteryExpand(Session session, PacketReader reader) {
        if (!inChannel(session)) {
            return;
        }
        if (reader.remaining() < 8) {
            session.send(GamePackets.sysAck(
                    GamePackets.SERVER_CHAR_MASTERY, GamePackets.CHAR_MASTERY_ERR_DEFAULT));
            return;
        }
        int typeid = reader.u32();
        int id = reader.i32();
        InventoryRepository.CharMasteryResult result;
        try {
            result = inventory.expandCharacterMastery(
                    session.player().uid, typeid, id, session.player().level);
        } catch (RuntimeException e) {
            log.warn("char mastery uid={} failed: {}", session.player().uid, e.toString());
            session.send(GamePackets.sysAck(
                    GamePackets.SERVER_CHAR_MASTERY, GamePackets.CHAR_MASTERY_ERR_DEFAULT));
            return;
        }
        if (result.code() != 0) {
            session.send(GamePackets.sysAck(
                    GamePackets.SERVER_CHAR_MASTERY, GamePackets.shopSys(result.code())));
            return;
        }
        session.send(GamePackets.papelAwards(GamePackets.unixNow(), result.awards()));
        session.send(GamePackets.sysAck(GamePackets.SERVER_CHAR_MASTERY, 0));
    }

    /**
     * C# {@code requestCharacterStatsUp}: truncated ToRead → full
     * {@code 0x5200500}; CHANNEL codes as {@code shopSys}; success
     * {@code 0xC8}/{@code 0x216} type {@code 0xC9}/{@code 0x26F} u32 0 + stat.
     */
    private void characterStatsUp(Session session, PacketReader reader) {
        if (!inChannel(session)) {
            return;
        }
        if (reader.remaining() < 4 + GamePackets.CHARACTER_INFO_BYTES) {
            session.send(GamePackets.sysAck(
                    GamePackets.SERVER_CHAR_STATS_UP, GamePackets.CHAR_STATS_UP_ERR_DEFAULT));
            return;
        }
        int stat = reader.u32();
        GamePackets.CharacterInfo ci = GamePackets.CharacterInfo.read(reader);
        InventoryRepository.CharStatsResult result;
        try {
            result = inventory.characterStatsUp(
                    session.player().uid, stat, ci, session.player().level);
        } catch (RuntimeException e) {
            log.warn("char stats up uid={} failed: {}", session.player().uid, e.toString());
            session.send(GamePackets.sysAck(
                    GamePackets.SERVER_CHAR_STATS_UP, GamePackets.CHAR_STATS_UP_ERR_DEFAULT));
            return;
        }
        if (result.code() != 0) {
            session.send(GamePackets.sysAck(
                    GamePackets.SERVER_CHAR_STATS_UP, GamePackets.shopSys(result.code())));
            return;
        }
        session.send(GamePackets.pangSpent(result.pangAfter(), result.pangSpent()));
        session.send(GamePackets.charPclAwards(
                GamePackets.unixNow(), result.typeid(), result.id(), result.pcl()));
        session.send(GamePackets.charStatsOk(GamePackets.SERVER_CHAR_STATS_UP, result.stat()));
    }

    /**
     * C# {@code requestCharacterStatsDown}: truncated ToRead → full
     * {@code 0x5200550}; CHANNEL codes as {@code shopSys}; success
     * {@code 0x216} type {@code 0xC9} then {@code 0x270} u32 0 + stat.
     */
    private void characterStatsDown(Session session, PacketReader reader) {
        if (!inChannel(session)) {
            return;
        }
        if (reader.remaining() < 4 + GamePackets.CHARACTER_INFO_BYTES) {
            session.send(GamePackets.sysAck(
                    GamePackets.SERVER_CHAR_STATS_DOWN, GamePackets.CHAR_STATS_DOWN_ERR_DEFAULT));
            return;
        }
        int stat = reader.u32();
        GamePackets.CharacterInfo ci = GamePackets.CharacterInfo.read(reader);
        InventoryRepository.CharStatsResult result;
        try {
            result = inventory.characterStatsDown(session.player().uid, stat, ci);
        } catch (RuntimeException e) {
            log.warn("char stats down uid={} failed: {}", session.player().uid, e.toString());
            session.send(GamePackets.sysAck(
                    GamePackets.SERVER_CHAR_STATS_DOWN, GamePackets.CHAR_STATS_DOWN_ERR_DEFAULT));
            return;
        }
        if (result.code() != 0) {
            session.send(GamePackets.sysAck(
                    GamePackets.SERVER_CHAR_STATS_DOWN, GamePackets.shopSys(result.code())));
            return;
        }
        session.send(GamePackets.charPclAwards(
                GamePackets.unixNow(), result.typeid(), result.id(), result.pcl()));
        session.send(GamePackets.charStatsOk(GamePackets.SERVER_CHAR_STATS_DOWN, result.stat()));
    }

    /**
     * C# {@code requestCharacterCardEquip}: IFF miss → {@code 0x271}
     * {@code shopSys(0x5200757)}.
     */
    private void characterCardEquip(Session session, PacketReader reader) {
        if (!inChannel(session)) {
            return;
        }
        if (reader.remaining() < GamePackets.CARD_EQUIP_BYTES) {
            session.send(GamePackets.sysAck(
                    GamePackets.SERVER_CHAR_CARD_EQUIP, GamePackets.CHAR_CARD_ERR_DEFAULT));
            return;
        }
        reader.readBytes(GamePackets.CARD_EQUIP_BYTES);
        session.send(GamePackets.sysAck(
                GamePackets.SERVER_CHAR_CARD_EQUIP,
                GamePackets.shopSys(GamePackets.CHAR_CARD_ERR_IFF)));
    }

    /**
     * C# {@code requestCharacterCardEquipWithPatcher}: missing Club Patcher
     * → {@code 0x272} {@code shopSys(0x5200810)}.
     */
    private void characterCardPatcher(Session session, PacketReader reader) {
        if (!inChannel(session)) {
            return;
        }
        if (reader.remaining() < GamePackets.CARD_EQUIP_BYTES) {
            session.send(GamePackets.sysAck(
                    GamePackets.SERVER_CHAR_CARD_PATCHER, GamePackets.CHAR_CARD_PATCHER_DEFAULT));
            return;
        }
        reader.readBytes(GamePackets.CARD_EQUIP_BYTES);
        session.send(GamePackets.sysAck(
                GamePackets.SERVER_CHAR_CARD_PATCHER,
                GamePackets.shopSys(GamePackets.CHAR_CARD_PATCHER_ERR)));
    }

    /**
     * C# {@code requestCharacterRemoveCard}: missing character → {@code 0x273}
     * {@code shopSys(0x5200851)}.
     */
    private void characterRemoveCard(Session session, PacketReader reader) {
        if (!inChannel(session)) {
            return;
        }
        if (reader.remaining() < GamePackets.CARD_EQUIP_BYTES) {
            session.send(GamePackets.sysAck(
                    GamePackets.SERVER_CHAR_CARD_REMOVE, GamePackets.CHAR_CARD_REMOVE_DEFAULT));
            return;
        }
        reader.readBytes(GamePackets.CARD_EQUIP_BYTES);
        session.send(GamePackets.sysAck(
                GamePackets.SERVER_CHAR_CARD_REMOVE,
                GamePackets.shopSys(GamePackets.CHAR_CARD_REMOVE_ERR_CHAR)));
    }

    /**
     * C# {@code requestTikiShopExchangeItem}: count 0/{@code >5} → {@code 0x274}
     * {@code shopSys(5200451)}.
     */
    private void tikiShopExchange(Session session, PacketReader reader) {
        if (!inChannel(session)) {
            return;
        }
        if (reader.remaining() < 4) {
            session.send(GamePackets.sysAck(
                    GamePackets.SERVER_TIKI_SHOP_EXCHANGE,
                    GamePackets.TIKI_SHOP_EXCHANGE_ERR_DEFAULT));
            return;
        }
        int count = reader.u32();
        if (count == 0 || count > 5) {
            session.send(GamePackets.sysAck(
                    GamePackets.SERVER_TIKI_SHOP_EXCHANGE,
                    GamePackets.shopSys(GamePackets.TIKI_SHOP_EXCHANGE_ERR_COUNT)));
            return;
        }
        if (reader.remaining() < count * GamePackets.TIKI_SHOP_EXCHANGE_ITEM_CHECK_BYTES) {
            session.send(GamePackets.sysAck(
                    GamePackets.SERVER_TIKI_SHOP_EXCHANGE,
                    GamePackets.shopSys(GamePackets.TIKI_SHOP_EXCHANGE_ERR_TRUNCATED)));
            return;
        }
        session.send(GamePackets.sysAck(
                GamePackets.SERVER_TIKI_SHOP_EXCHANGE,
                GamePackets.shopSys(GamePackets.TIKI_SHOP_EXCHANGE_ERR_ITEM)));
    }

    /**
     * C# {@code requestPlayMemorial}: coin 0 → {@code 0x264}
     * {@code shopSys(0x6300301)}.
     */
    private void playMemorial(Session session, PacketReader reader) {
        if (!inChannel(session)) {
            return;
        }
        if (reader.remaining() < 4) {
            session.send(GamePackets.sysAck(
                    GamePackets.SERVER_MEMORIAL, GamePackets.MEMORIAL_ERR_DEFAULT));
            return;
        }
        int coin = reader.u32();
        if (coin == 0) {
            session.send(GamePackets.sysAck(
                    GamePackets.SERVER_MEMORIAL, GamePackets.shopSys(GamePackets.MEMORIAL_ERR_COIN)));
            return;
        }
        session.send(GamePackets.sysAck(
                GamePackets.SERVER_MEMORIAL, GamePackets.shopSys(0x6300303)));
    }

    /**
     * C# {@code requestNotifyNotDisplayPrivateMessageNow}: sends
     * {@code pacote040} option 4 + nick to the named online player.
     */
    private void refuseWhisper(Session session, PacketReader reader) {
        if (!session.authorized() || reader.remaining() < 2) {
            return;
        }
        String nick = reader.pstr();
        if (nick == null || nick.isEmpty()) {
            return;
        }
        Session target = sessions.findByNickname(nick);
        if (target != null && target.authorized()) {
            target.send(GamePackets.chatRefuseWhisper(nick));
        }
    }

    /**
     * C# {@code requestExecCCGIdentity}: non-GM CHANNEL catch is silent.
     */
    private void execIdentity(Session session) {
        if (!inChannel(session)) {
            return;
        }
    }

    /**
     * C# {@code requestActivePaws}: not-in-room CHANNEL catch is silent.
     */
    private void activePaws(Session session) {
        if (!inChannel(session)) {
            return;
        }
    }

    /**
     * C# {@code requestActiveRing}: not-in-room CHANNEL catch is silent.
     */
    private void activeRing(Session session) {
        if (!inChannel(session)) {
            return;
        }
    }

    private void changeTeam(Session session, PacketReader reader) {
        if (!session.authorized() || reader.remaining() < 1) {
            return;
        }
        int team = reader.u8() & 1;
        GameRoom room = rooms.get(session.player().roomNumber);
        if (room == null) {
            return;
        }
        GamePackets.PlayerRoomInfo pri = room.playerInfo(session);
        if (pri == null) {
            return;
        }
        pri.stateFlag = (pri.stateFlag & ~GamePackets.PLAYER_TEAM_BIT) | team;
        room.putPlayerInfo(session, pri);
        room.broadcast(GamePackets.teamState(session.oid(), team));
    }

    private void requestRoomDetail(Session session, PacketReader reader) {
        if (!session.authorized() || reader.remaining() < 2) {
            return;
        }
        int numero = reader.u16();
        GameRoom room = rooms.get(numero);
        if (room == null) {
            return;
        }
        List<GamePackets.RoomDetailPlayer> players = new ArrayList<>();
        for (Session member : room.snapshot()) {
            GamePackets.PlayerLobbyInfo info = makeLobbyInfo(member);
            players.add(new GamePackets.RoomDetailPlayer(
                    info.oid, info.level, 0, info.capability, info.title, info.teamPoint));
        }
        session.send(GamePackets.roomDetail(room.info, room.tipo, players));
    }

    private void invite(Session session, PacketReader reader) {
        if (!session.authorized() || reader.remaining() < 2) {
            return;
        }
        String nick = reader.pstr();
        if (reader.remaining() < 4) {
            session.send(GamePackets.inviteFail(GamePackets.INVITE_FAIL));
            return;
        }
        long uid = reader.u32() & 0xffff_ffffL;
        PlayerContext pi = session.player();
        GameRoom room = rooms.get(pi.roomNumber);
        Session target = sessions.findByNickname(nick);
        if (room == null
                || target == null
                || target.player().uid != uid
                || target.player().channelId != pi.channelId
                || target.player().roomNumber >= 0
                || target.player().place != 0) {
            session.send(GamePackets.inviteFail(GamePackets.INVITE_FAIL));
            return;
        }
        target.player().place = GamePackets.INVITE_PLACE;
        int fromUid = (int) pi.uid;
        String fromNick = pi.nickname == null ? "" : pi.nickname;
        int toUid = (int) uid;
        session.send(GamePackets.inviteOk(
                GamePackets.SERVER_INVITE_REPLY, config.uid(), pi.channelId, room.info.numero,
                fromUid, fromNick, toUid));
        target.send(GamePackets.inviteOk(
                GamePackets.SERVER_INVITE, config.uid(), pi.channelId, room.info.numero,
                fromUid, fromNick, toUid));
    }

    private GamePackets.ChannelInfo findChannel(int id) {
        for (GamePackets.ChannelInfo c : channels) {
            if ((c.id & 0xff) == id) {
                return c;
            }
        }
        return null;
    }

    private void adjustChannelCount(int channelId, int delta) {
        GamePackets.ChannelInfo c = findChannel(channelId);
        if (c != null) {
            int next = c.currUser + delta;
            c.currUser = (short) Math.max(0, next);
        }
    }

    static boolean keyMatches(String presented, String redisKey, String sqlKey) {
        if (presented == null || presented.isEmpty()) {
            return false;
        }
        return presented.equals(redisKey) || presented.equals(sqlKey);
    }

    private static byte[] i32le(int v) {
        return PacketIo.u32le(v);
    }

    private static byte[] u32le(int v) {
        return PacketIo.u32le(v);
    }

    private static byte[] concat(byte[] a, byte[] b) {
        return PacketIo.concat(a, b);
    }
}
