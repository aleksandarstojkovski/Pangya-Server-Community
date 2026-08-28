package org.pangya.messenger;

import org.pangya.db.FriendRepository;
import org.pangya.db.LoginRepository;
import org.pangya.network.auth.AuthOutbound;
import org.pangya.network.session.PlayerContext;
import org.pangya.network.session.Session;
import org.pangya.network.session.SessionManager;
import org.pangya.protocol.auth.AuthS2s;
import org.pangya.protocol.messenger.MessengerPackets;
import org.pangya.protocol.packet.PacketReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JP {@code MessengerServer.requestLogin} + friend/presence/chat handlers.
 */
public final class MessengerHandler {

    private static final Logger log = LoggerFactory.getLogger(MessengerHandler.class);

    private final LoginRepository repo;
    private final FriendRepository friends;
    private final SessionManager sessions;
    private final AuthOutbound authOut;

    public MessengerHandler(LoginRepository repo, FriendRepository friends, SessionManager sessions) {
        this(repo, friends, sessions, (reqServerUid, info) -> {});
    }

    MessengerHandler(LoginRepository repo, FriendRepository friends, SessionManager sessions, AuthOutbound authOut) {
        this.repo = repo;
        this.friends = friends;
        this.sessions = sessions;
        this.authOut = authOut;
    }

    public void onPacket(Session session, byte[] plaintext) {
        if (plaintext.length < 2) {
            return;
        }
        PacketReader reader = new PacketReader(plaintext);
        int opcode = reader.opcode();
        switch (opcode) {
            case MessengerPackets.CLIENT_CONNECT -> requestLogin(session, reader);
            case MessengerPackets.CLIENT_REQ_USERINFO_OFFLINE -> offlineUserInfoNotify(session);
            case MessengerPackets.CLIENT_REQ_USERINFO -> friendList(session);
            case MessengerPackets.CLIENT_REQ_REGISTER_FRIEND -> addFriend(session, reader);
            case MessengerPackets.CLIENT_REQ_FRIEND_AGREE -> agreeFriend(session, reader);
            case MessengerPackets.CLIENT_REQ_FRIEND_BLOCK -> blockFriend(session, reader);
            case MessengerPackets.CLIENT_REQ_FRIEND_UNBLOCK -> unblockFriend(session, reader);
            case MessengerPackets.CLIENT_REQ_FRIEND_REMOVE -> removeFriend(session, reader);
            case MessengerPackets.CLIENT_NOTIFY_LOGOUT -> notifyLogout(session);
            case MessengerPackets.CLIENT_REQ_CHECK_NICK -> checkNickname(session, reader);
            case MessengerPackets.CLIENT_NOTIFY_UPDATE_MY_STATUS -> updatePlayerState(session, reader);
            case MessengerPackets.CLIENT_REQ_CHAT_FRIEND -> chatFriend(session, reader);
            case MessengerPackets.CLIENT_REQ_ASSIGN_APELIDO -> assignApelido(session, reader);
            case MessengerPackets.CLIENT_REQ_UPDATE_CHANNEL_INFO -> updateChannelInfo(session, reader);
            case MessengerPackets.CLIENT_REQ_CHAT_GUILD -> chatGuild(session, reader);
            case MessengerPackets.CLIENT_NOTIFY_ROOM_INVITE -> notifyRoomInvite(session, reader);
            case MessengerPackets.CLIENT_GUILD_BATTLE_ROOM_INVITE -> guildBattleRoomInvite(session, reader);
            case MessengerPackets.CLIENT_GIFT_ITEM_NOTIFY -> giftItemNotify(session, reader);
            case MessengerPackets.CLIENT_NOTIFY_GUILD_JOINED,
                 MessengerPackets.CLIENT_NOTIFY_GUILD_BANISH,
                 MessengerPackets.CLIENT_NOTIFY_GUILD_SHIELD_CHANGED,
                 MessengerPackets.CLIENT_NOTIFY_GUILD_NAME_CHANGED -> guildClientNotify(session, opcode);
            default -> log.debug("unhandled messenger opcode 0x{}", Integer.toHexString(opcode));
        }
    }

    public void onDisconnect(Session session) {
        sendLogoutToFriends(session);
    }

    /**
     * C# {@code authCmd*} from Auth via {@code unit_auth_server_connect}.
     */
    public void onAuthPacket(int opcode, PacketReader body) {
        switch (opcode) {
            case AuthS2s.AUTH_DISCONNECT_PLAYER -> authDisconnectPlayer(body);
            case AuthS2s.AUTH_INFO_PLAYER_ONLINE -> authInfoPlayerOnline(body);
            case AuthS2s.AUTH_CONFIRM_PLAYER_INFO -> authConfirmPlayerInfo(body);
            case AuthS2s.SEND_COMMAND_TO_OTHER -> {
                int reqServerUid = body.u32();
                short commandId = (short) body.i16();
                onAuthCommand(reqServerUid, commandId, body);
            }
            default -> log.debug("unhandled auth packet 0x{}", Integer.toHexString(opcode));
        }
    }

    /**
     * C# {@code funcs_as} guild callbacks ({@code packet_as001}–{@code packet_as003}).
     */
    public void onAuthCommand(int reqServerUid, short commandId, PacketReader body) {
        switch (commandId) {
            case AuthS2s.AS_ACCEPT_GUILD_MEMBER -> acceptGuildMember(body);
            case AuthS2s.AS_GUILD_MEMBER_EXIT -> memberExitedFromGuild(body);
            case AuthS2s.AS_KICK_GUILD_MEMBER -> kickGuildMember(body);
            default -> log.debug("unhandled auth command 0x{} reqServer={}", Integer.toHexString(commandId), reqServerUid);
        }
    }

    private void requestLogin(Session session, PacketReader reader) {
        try {
            MessengerPackets.Login data = MessengerPackets.readLogin(reader);
            if (data.uid() == 0 || data.nickname() == null || data.nickname().isEmpty()) {
                session.send(MessengerPackets.loginFail());
                session.disconnect();
                return;
            }
            var info = repo.playerInfo(data.uid() & 0xffff_ffffL).orElse(null);
            if (info == null || !data.nickname().equals(info.nickname())) {
                session.send(MessengerPackets.loginFail());
                session.disconnect();
                return;
            }
            if (info.idState() != 0) {
                session.send(MessengerPackets.loginFail());
                session.disconnect();
                return;
            }
            finishLogin(session, info);
        } catch (RuntimeException e) {
            log.warn("messenger login failed: {}", e.toString());
            session.send(MessengerPackets.loginFail());
            session.disconnect();
        }
    }

    /** C# {@code confirmLoginOnOtherServer} success path. */
    private void finishLogin(Session session, LoginRepository.PlayerLoginInfo info) {
        PlayerContext pi = session.player();
        pi.uid = info.uid();
        pi.id = info.id();
        pi.nickname = info.nickname();
        pi.level = info.level();
        pi.messengerState = MessengerPackets.STATE_ONLINE;
        pi.channelPlayerInfo = MessengerPackets.emptyChannelPlayerInfo();
        pi.messengerLogoutSent = false;
        reloadGuild(pi);
        sessions.disconnectOthersWithUid(pi.uid, session);
        session.setAuthorized(true);
        session.send(MessengerPackets.loginOk((int) pi.uid));
        log.info("messenger login nick={} uid={}", pi.nickname, pi.uid);
    }

    private void authDisconnectPlayer(PacketReader body) {
        AuthS2s.AuthDisconnectRequest req = AuthS2s.readAuthDisconnect(body);
        Session target = sessions.findByUid(req.playerUid());
        if (target == null) {
            log.debug("auth disconnect uid={} not on messenger", req.playerUid());
            return;
        }
        target.disconnect();
        log.info("auth disconnect uid={} server={} force={}", req.playerUid(), req.serverUid(), req.force());
    }

    /** C# {@code authCmdInfoPlayerOnline} → {@code sendInfoPlayerOnline}. */
    private void authInfoPlayerOnline(PacketReader body) {
        AuthS2s.AuthInfoPlayerOnlineRequest req = AuthS2s.readAuthInfoPlayerOnline(body);
        Session target = sessions.findByUid(req.playerUid());
        AuthS2s.AuthServerPlayerInfo info;
        if (target != null) {
            PlayerContext pi = target.player();
            info = AuthS2s.AuthServerPlayerInfo.online(pi.uid, pi.id, target.ip());
        } else {
            info = AuthS2s.AuthServerPlayerInfo.offline(req.playerUid());
        }
        authOut.sendInfoPlayerOnline(req.reqServerUid(), info);
        log.debug("auth info player online uid={} reqServer={} option={}", req.playerUid(), req.reqServerUid(), info.option());
    }

    private void authConfirmPlayerInfo(PacketReader body) {
        AuthS2s.AuthConfirmPlayerInfo info = AuthS2s.readAuthConfirmPlayerInfo(body);
        Session session = sessions.findByUid(info.uid());
        if (session == null) {
            log.debug("auth confirm login uid={} not connected", info.uid());
            return;
        }
        if (session.authorized()) {
            return;
        }
        var player = repo.playerInfo(info.uid()).orElse(null);
        if (player == null) {
            session.send(MessengerPackets.loginFail());
            session.disconnect();
            return;
        }
        finishLogin(session, player);
        log.info("auth confirmed messenger login uid={} reqServer={}", info.uid(), info.reqServerUid());
    }

    private void friendList(Session session) {
        if (!session.authorized()) {
            return;
        }
        PlayerContext pi = session.player();
        session.send(MessengerPackets.friendStatus(
                (int) pi.uid, pi.messengerState, channelInfo(pi)));
        pushFriendListPages(session);
    }

    private void pushFriendListPages(Session session) {
        if (!session.authorized()) {
            return;
        }
        List<FriendRepository.FriendRow> list = friends.friendsAndGuildMembers(session.player().uid);
        if (list.isEmpty()) {
            session.send(MessengerPackets.emptyFriendPage());
            return;
        }
        int remaining = list.size();
        int pages = (list.size() + MessengerPackets.FRIEND_PAG_LIMIT - 1) / MessengerPackets.FRIEND_PAG_LIMIT;
        for (int page = 0; page < pages; page++) {
            int start = page * MessengerPackets.FRIEND_PAG_LIMIT;
            int current = Math.min(MessengerPackets.FRIEND_PAG_LIMIT, remaining);
            List<byte[]> rows = new ArrayList<>();
            for (int i = start; i < start + current; i++) {
                rows.add(friendListRow(list.get(i), session.player().uid));
            }
            session.send(MessengerPackets.friendPage(page + 1, remaining, current, rows));
            remaining -= current;
        }
    }

    private void acceptGuildMember(PacketReader body) {
        long clubId = body.u32() & 0xffff_ffffL;
        long memberUid = body.u32() & 0xffff_ffffL;
        if (clubId == 0 || memberUid == 0) {
            log.warn("acceptGuildMember invalid club={} member={}", clubId, memberUid);
            return;
        }
        Session memberSession = sessions.findByUid(memberUid);
        if (memberSession != null) {
            reloadGuild(memberSession.player());
        }
        refreshOnlineGuildMembers(clubId);
        broadcastGuildJoined(clubId, memberUid, memberSession);
        log.info("guild accept member={} club={}", memberUid, clubId);
    }

    private void memberExitedFromGuild(PacketReader body) {
        handleGuildMemberRemoved(body, "exit");
    }

    private void kickGuildMember(PacketReader body) {
        handleGuildMemberRemoved(body, "kick");
    }

    private void handleGuildMemberRemoved(PacketReader body, String reason) {
        long clubId = body.u32() & 0xffff_ffffL;
        long memberUid = body.u32() & 0xffff_ffffL;
        if (clubId == 0 || memberUid == 0) {
            log.warn("guild {} invalid club={} member={}", reason, clubId, memberUid);
            return;
        }
        List<Session> guildOnline = new ArrayList<>(sessions.findByGuildUid(clubId));
        Session memberSession = sessions.findByUid(memberUid);
        for (Session member : guildOnline) {
            reloadGuild(member.player());
            pushFriendListPages(member);
        }
        if (memberSession != null) {
            clearGuild(memberSession.player());
        }
        broadcastGuildLeft(guildOnline, memberUid, memberSession);
        log.info("guild {} member={} club={}", reason, memberUid, clubId);
    }

    private void refreshOnlineGuildMembers(long clubId) {
        for (Session member : sessions.findByGuildUid(clubId)) {
            reloadGuild(member.player());
            pushFriendListPages(member);
        }
    }

    private void reloadGuild(PlayerContext pi) {
        pi.guildUid = 0;
        pi.guildName = "";
        repo.guildMembership(pi.uid).ifPresent(g -> {
            pi.guildUid = g.guildUid();
            pi.guildName = g.guildName() == null ? "" : g.guildName();
        });
        repo.syncAccountGuildUid(pi.uid, pi.guildUid);
    }

    private void clearGuild(PlayerContext pi) {
        pi.guildUid = 0;
        pi.guildName = "";
        repo.syncAccountGuildUid(pi.uid, 0);
    }

    private void broadcastGuildJoined(long clubId, long memberUid, Session memberSession) {
        var info = repo.playerInfo(memberUid & 0xffff_ffffL).orElse(null);
        if (info == null) {
            log.warn("acceptGuildMember missing player uid={}", memberUid);
            return;
        }
        byte[] packet = MessengerPackets.guildMemberJoined(
                info.uid(),
                clubId,
                repo.playerSex(info.uid()),
                info.id(),
                info.nickname());
        for (Session member : sessions.findByGuildUid(clubId)) {
            if (memberSession != null && member == memberSession) {
                continue;
            }
            member.send(packet);
        }
    }

    private void broadcastGuildLeft(List<Session> guildOnline, long memberUid, Session memberSession) {
        byte[] packet = MessengerPackets.guildMemberLeft(memberUid);
        for (Session member : guildOnline) {
            if (memberSession != null && member == memberSession) {
                continue;
            }
            member.send(packet);
        }
    }

    private byte[] friendListRow(FriendRepository.FriendRow row, long viewerUid) {
        byte[] info = MessengerPackets.friendInfo(
                row.nickname(),
                row.apelido(),
                (int) row.friendUid(),
                row.unknown1(),
                row.unknown2(),
                row.unknown3(),
                row.unknown4(),
                row.unknown5(),
                row.unknown6(),
                0);
        Session live = sessions.findByUid(row.friendUid());
        int state = row.stateFlag();
        byte[] channel;
        int icon;
        if (live != null && canSeeOnline(viewerUid, row.friendUid())) {
            channel = channelInfo(live.player());
            icon = live.player().messengerState;
            state |= MessengerPackets.FLAG_ONLINE;
        } else {
            channel = MessengerPackets.offlineChannelPlayerInfo();
            icon = MessengerPackets.OFFLINE_ICON;
        }
        int levelOrGuildRole = row.playerFlag() == MessengerPackets.GUILD_MEMBER_FLAG
                ? (row.friendUid() == viewerUid ? 1 : 0)
                : row.level();
        return MessengerPackets.friendListRow(
                info, channel, icon, row.flag1(), levelOrGuildRole, state, row.playerFlag());
    }

    private void guildClientNotify(Session session, int opcode) {
        if (!session.authorized()) {
            log.warn("guild notify 0x{} before login uid={}", Integer.toHexString(opcode), session.player().uid);
            return;
        }
        log.debug("guild client notify 0x{} uid={}", Integer.toHexString(opcode), session.player().uid);
    }

    /** C# {@code packet013}: auth-check only, no response. */
    private void offlineUserInfoNotify(Session session) {
        if (!session.authorized()) {
            log.warn("offline userinfo notify before login");
            return;
        }
        log.debug("offline userinfo notify uid={}", session.player().uid);
    }

    /**
     * C# {@code findFriendInAllFriend} + block check before showing live
     * {@code ChannelPlayerInfo} on friend-list rows.
     */
    private boolean canSeeOnline(long viewerUid, long targetUid) {
        if (viewerUid == targetUid) {
            return true;
        }
        var reverse = friends.find(targetUid, viewerUid);
        if (reverse.isPresent() && (reverse.get().stateFlag() & MessengerPackets.FLAG_BLOCK) == 0) {
            return true;
        }
        Session viewer = sessions.findByUid(viewerUid);
        Session target = sessions.findByUid(targetUid);
        return viewer != null
                && target != null
                && viewer.player().guildUid > 0
                && viewer.player().guildUid == target.player().guildUid;
    }

    private void addFriend(Session session, PacketReader reader) {
        if (!session.authorized()) {
            return;
        }
        int uid = reader.u32();
        String nickname = reader.remaining() >= 2 ? reader.pstr() : "";
        try {
            if (uid == 0) {
                session.send(MessengerPackets.addFriendError(0x5200601));
                return;
            }
            if (nickname == null || nickname.isEmpty()) {
                session.send(MessengerPackets.addFriendError(0x5200602));
                return;
            }
            var existing = friends.find(session.player().uid, uid);
            if (existing.isPresent() && (existing.get().stateFlag() & MessengerPackets.FLAG_FRIEND) != 0) {
                session.send(MessengerPackets.addFriendError(2));
                return;
            }
            if (friends.count(session.player().uid) >= FriendRepository.FRIEND_LIST_LIMIT) {
                session.send(MessengerPackets.addFriendError(0x5200603));
                return;
            }
            var info = repo.playerInfo(uid & 0xffff_ffffL).orElse(null);
            if (info == null || info.uid() == 0) {
                session.send(MessengerPackets.addFriendError(0x5200606));
                return;
            }
            if (!nickname.equals(info.nickname())) {
                session.send(MessengerPackets.addFriendError(0x5200607));
                return;
            }
            if (friends.count(info.uid()) >= FriendRepository.FRIEND_LIST_LIMIT) {
                session.send(MessengerPackets.addFriendError(3));
                return;
            }
            friends.add(session.player().uid, new FriendRepository.FriendRow(
                    info.uid(), info.nickname(), "Friend", -1, 0, -1, 0, 0, 0, 255,
                    MessengerPackets.FLAG_REQUEST, info.level(), MessengerPackets.FRIEND_FLAG));
            friends.add(info.uid(), new FriendRepository.FriendRow(
                    session.player().uid, session.player().nickname, "Friend", -1, 0, -1, 0, 0, 0, 255,
                    0, session.player().level, MessengerPackets.FRIEND_FLAG));
            byte[] fi = MessengerPackets.friendInfo(info.nickname(), "Friend", (int) info.uid());
            int flag = MessengerPackets.FRIEND_FLAG;
            int requestState = MessengerPackets.FLAG_REQUEST;
            Session live = sessions.findByUid(info.uid());
            if (live != null) {
                byte[] requesterFi = MessengerPackets.friendInfo(
                        session.player().nickname, "Friend", (int) session.player().uid);
                session.send(MessengerPackets.addFriendOkOnline(
                        fi,
                        channelInfo(live.player()),
                        live.player().messengerState,
                        info.level(),
                        requestState,
                        flag));
                live.send(MessengerPackets.newFriendMessage(
                        requesterFi,
                        channelInfo(session.player()),
                        session.player().messengerState,
                        session.player().level,
                        0,
                        flag));
            } else {
                session.send(MessengerPackets.addFriendOkOffline(fi, info.level(), requestState, flag));
            }
            log.info("add friend uid={} -> {}", session.player().uid, info.uid());
        } catch (RuntimeException e) {
            log.warn("add friend failed: {}", e.toString());
            session.send(MessengerPackets.addFriendError(0x5200600));
        }
    }

    private void agreeFriend(Session session, PacketReader reader) {
        if (!session.authorized()) {
            return;
        }
        int uid = reader.u32();
        try {
            if (uid == 0) {
                session.send(MessengerPackets.friendUidAck(MessengerPackets.SUB_FRIEND_AGREE, 0x5200801, 0));
                return;
            }
            var row = friends.find(session.player().uid, uid).orElse(null);
            if (row == null) {
                session.send(MessengerPackets.friendUidAck(MessengerPackets.SUB_FRIEND_AGREE, 0x5200802, 0));
                return;
            }
            if ((row.stateFlag() & MessengerPackets.FLAG_REQUEST) != 0) {
                session.send(MessengerPackets.friendUidAck(MessengerPackets.SUB_FRIEND_AGREE, 0x5200803, 0));
                return;
            }
            if ((row.stateFlag() & MessengerPackets.FLAG_FRIEND) != 0) {
                session.send(MessengerPackets.friendUidAck(MessengerPackets.SUB_FRIEND_AGREE, 0x5200804, 0));
                return;
            }
            friends.updateState(session.player().uid, uid, row.stateFlag() | MessengerPackets.FLAG_FRIEND);
            var other = friends.find(uid, session.player().uid);
            other.ifPresent(o -> friends.updateState(
                    uid, session.player().uid,
                    (o.stateFlag() & ~MessengerPackets.FLAG_REQUEST) | MessengerPackets.FLAG_FRIEND));
            session.send(MessengerPackets.friendUidAck(MessengerPackets.SUB_FRIEND_AGREE, 0, uid));
            Session live = sessions.findByUid(uid);
            if (live != null) {
                live.send(MessengerPackets.friendUidAck(
                        MessengerPackets.SUB_FRIEND_ACCEPTED, 0, (int) session.player().uid));
            }
        } catch (RuntimeException e) {
            log.warn("agree friend failed: {}", e.toString());
            session.send(MessengerPackets.friendUidAck(MessengerPackets.SUB_FRIEND_AGREE, 0x5200800, 0));
        }
    }

    private void blockFriend(Session session, PacketReader reader) {
        if (!session.authorized()) {
            return;
        }
        int uid = reader.u32();
        try {
            if (uid == 0) {
                session.send(MessengerPackets.friendUidAck(MessengerPackets.SUB_FRIEND_BLOCK, 0x5300101, 0));
                return;
            }
            var row = friends.find(session.player().uid, uid).orElse(null);
            if (row == null) {
                session.send(MessengerPackets.friendUidAck(MessengerPackets.SUB_FRIEND_BLOCK, 0x5300102, 0));
                return;
            }
            if ((row.stateFlag() & MessengerPackets.FLAG_BLOCK) != 0) {
                session.send(MessengerPackets.friendUidAck(MessengerPackets.SUB_FRIEND_BLOCK, 0x5300103, 0));
                return;
            }
            friends.updateState(session.player().uid, uid, row.stateFlag() | MessengerPackets.FLAG_BLOCK);
            session.send(MessengerPackets.friendUidAck(MessengerPackets.SUB_FRIEND_BLOCK, 0, uid));
            Session live = sessions.findByUid(uid);
            if (live != null) {
                live.send(MessengerPackets.friendLogout((int) session.player().uid));
            }
        } catch (RuntimeException e) {
            log.warn("block friend failed: {}", e.toString());
            session.send(MessengerPackets.friendUidAck(MessengerPackets.SUB_FRIEND_BLOCK, 0x5300100, 0));
        }
    }

    private void unblockFriend(Session session, PacketReader reader) {
        if (!session.authorized()) {
            return;
        }
        int uid = reader.u32();
        try {
            if (uid == 0) {
                session.send(MessengerPackets.friendUidAck(MessengerPackets.SUB_FRIEND_UNBLOCK, 0x5300201, 0));
                return;
            }
            var row = friends.find(session.player().uid, uid).orElse(null);
            if (row == null) {
                session.send(MessengerPackets.friendUidAck(MessengerPackets.SUB_FRIEND_UNBLOCK, 0x5300202, 0));
                return;
            }
            if ((row.stateFlag() & MessengerPackets.FLAG_BLOCK) == 0) {
                session.send(MessengerPackets.friendUidAck(MessengerPackets.SUB_FRIEND_UNBLOCK, 0x5300203, 0));
                return;
            }
            Session live = sessions.findByUid(uid);
            if (live != null && friends.find(uid, session.player().uid).isEmpty()) {
                session.send(MessengerPackets.friendUidAck(MessengerPackets.SUB_FRIEND_UNBLOCK, 0x5200204, 0));
                return;
            }
            int newState = row.stateFlag() & ~MessengerPackets.FLAG_BLOCK;
            friends.updateState(session.player().uid, uid, newState);
            session.send(MessengerPackets.friendUidAck(MessengerPackets.SUB_FRIEND_UNBLOCK, 0, uid));
            if (live != null) {
                live.send(MessengerPackets.friendStatus(
                        (int) session.player().uid,
                        session.player().messengerState,
                        channelInfo(session.player())));
            }
        } catch (RuntimeException e) {
            log.warn("unblock friend failed: {}", e.toString());
            session.send(MessengerPackets.friendUidAck(MessengerPackets.SUB_FRIEND_UNBLOCK, 0x5300200, 0));
        }
    }

    private void assignApelido(Session session, PacketReader reader) {
        if (!session.authorized()) {
            return;
        }
        int uid = reader.u32();
        String apelido = reader.remaining() >= 2 ? reader.pstr() : "";
        try {
            if (uid == 0) {
                session.send(MessengerPackets.assignApelidoError(0x5200901));
                return;
            }
            if (apelido == null || apelido.isEmpty()) {
                session.send(MessengerPackets.assignApelidoError(0x5200902));
                return;
            }
            if (apelido.length() >= 11) {
                session.send(MessengerPackets.assignApelidoError(0x5200903));
                return;
            }
            var row = friends.find(session.player().uid, uid).orElse(null);
            if (row == null) {
                session.send(MessengerPackets.assignApelidoError(0x5200903));
                return;
            }
            friends.updateApelido(session.player().uid, uid, apelido);
            session.send(MessengerPackets.assignApelidoOk(uid, apelido));
        } catch (RuntimeException e) {
            log.warn("assign apelido failed: {}", e.toString());
            session.send(MessengerPackets.assignApelidoError(0x5200900));
        }
    }

    private void guildBattleRoomInvite(Session session, PacketReader reader) {
        if (!session.authorized()) {
            return;
        }
        try {
            int serverUid = reader.u32();
            int channelId = reader.u8();
            int roomNum = reader.u16();
            int inviterUid = reader.u32();
            String inviterNick = reader.remaining() >= 2 ? reader.pstr() : "";
            int invitedUid = reader.u32();
            if (inviterUid != (int) session.player().uid) {
                log.warn("guild battle invite uid mismatch session={} packet={}", session.player().uid, inviterUid);
                return;
            }
            log.info(
                    "guild battle invite from uid={} nick={} invited={} server={} ch={} room={}",
                    inviterUid, inviterNick, invitedUid, serverUid, channelId, roomNum);
        } catch (RuntimeException e) {
            log.warn("guild battle invite failed uid={}: {}", session.player().uid, e.toString());
        }
    }

    private void giftItemNotify(Session session, PacketReader reader) {
        if (!session.authorized()) {
            return;
        }
        try {
            int senderUid = reader.u32();
            int receiverUid = reader.u32();
            log.info("messenger gift item sender={} receiver={} from session={}",
                    senderUid, receiverUid, session.player().uid);
        } catch (RuntimeException e) {
            log.warn("gift item notify failed uid={}: {}", session.player().uid, e.toString());
        }
    }

    private void notifyRoomInvite(Session session, PacketReader reader) {
        if (!session.authorized()) {
            return;
        }
        try {
            int uid = reader.u32();
            if (uid != (int) session.player().uid) {
                log.warn("room invite uid mismatch session={} packet={}", session.player().uid, uid);
                return;
            }
            log.info("messenger room invite uid={}", uid);
        } catch (RuntimeException e) {
            log.warn("room invite failed uid={}: {}", session.player().uid, e.toString());
        }
    }

    private void chatGuild(Session session, PacketReader reader) {
        if (!session.authorized()) {
            return;
        }
        try {
            String msg = reader.remaining() >= 2 ? reader.pstr() : "";
            if (session.player().guildUid == 0) {
                session.send(MessengerPackets.friendChatError());
                return;
            }
            if (msg == null || msg.isEmpty()) {
                session.send(MessengerPackets.friendChatError());
                return;
            }
            byte[] packet = MessengerPackets.guildChat(
                    (int) session.player().uid, session.player().nickname, msg);
            session.send(packet);
            for (Session member : sessions.findByGuildUid(session.player().guildUid)) {
                if (member != session) {
                    member.send(packet);
                }
            }
        } catch (RuntimeException e) {
            log.warn("chat guild failed uid={}: {}", session.player().uid, e.toString());
            session.send(MessengerPackets.friendChatError());
        }
    }

    private void removeFriend(Session session, PacketReader reader) {
        if (!session.authorized()) {
            return;
        }
        int uid = reader.u32();
        String nick = reader.remaining() >= 2 ? reader.pstr() : "";
        try {
            if (uid == 0) {
                session.send(MessengerPackets.friendUidAck(MessengerPackets.SUB_FRIEND_REMOVE, 0x5200701, 0));
                return;
            }
            var row = friends.find(session.player().uid, uid).orElse(null);
            if (row == null) {
                session.send(MessengerPackets.friendUidAck(MessengerPackets.SUB_FRIEND_REMOVE, 0x5200702, 0));
                return;
            }
            if (nick != null && !nick.isEmpty() && !nick.equals(row.nickname())) {
                session.send(MessengerPackets.friendUidAck(MessengerPackets.SUB_FRIEND_REMOVE, 0x5200703, 0));
                return;
            }
            friends.delete(session.player().uid, uid);
            friends.delete(uid, session.player().uid);
            session.send(MessengerPackets.friendUidAck(MessengerPackets.SUB_FRIEND_REMOVE, 0, uid));
            Session live = sessions.findByUid(uid);
            if (live != null) {
                live.send(MessengerPackets.friendUidAck(
                        MessengerPackets.SUB_FRIEND_REMOVE, 0, (int) session.player().uid));
            }
        } catch (RuntimeException e) {
            log.warn("remove friend failed: {}", e.toString());
            session.send(MessengerPackets.friendUidAck(MessengerPackets.SUB_FRIEND_REMOVE, 0x5200700, 0));
        }
    }

    private void notifyLogout(Session session) {
        if (!session.authorized()) {
            return;
        }
        sendLogoutToFriends(session);
    }

    private void checkNickname(Session session, PacketReader reader) {
        if (!session.authorized()) {
            return;
        }
        String nickname = reader.remaining() >= 2 ? reader.pstr() : "";
        try {
            if (nickname == null || nickname.isEmpty()) {
                session.send(MessengerPackets.checkNickError(MessengerPackets.CHECK_NICK_ERR_EMPTY, nickname));
                return;
            }
            var info = repo.playerInfoByNick(nickname);
            if (info.isEmpty()) {
                session.send(MessengerPackets.checkNickError(MessengerPackets.CHECK_NICK_ERR_MISSING, nickname));
                return;
            }
            session.send(MessengerPackets.checkNickOk(nickname, (int) info.get().uid()));
        } catch (RuntimeException e) {
            log.warn("check nick failed: {}", e.toString());
            session.send(MessengerPackets.checkNickError(MessengerPackets.CHECK_NICK_ERR_DEFAULT, nickname));
        }
    }

    private void updatePlayerState(Session session, PacketReader reader) {
        if (!session.authorized() || reader.remaining() < 1) {
            return;
        }
        int state = reader.u8();
        PlayerContext pi = session.player();
        if (pi.messengerState != state) {
            pi.messengerState = state;
        }
        broadcastStatus(session, false);
    }

    private void updateChannelInfo(Session session, PacketReader reader) {
        if (!session.authorized()) {
            return;
        }
        try {
            byte[] cpi = MessengerPackets.readChannelPlayerInfo(reader);
            session.player().channelPlayerInfo = cpi;
            session.send(MessengerPackets.friendStatus(
                    (int) session.player().uid, session.player().messengerState, cpi));
            broadcastStatus(session, false);
        } catch (RuntimeException e) {
            log.warn("update channel failed uid={}: {}", session.player().uid, e.toString());
            session.send(MessengerPackets.statusBroadcastError(
                    (int) session.player().uid, session.player().messengerState));
        }
    }

    private void chatFriend(Session session, PacketReader reader) {
        if (!session.authorized()) {
            return;
        }
        try {
            int uid = reader.u32();
            String msg = reader.remaining() >= 2 ? reader.pstr() : "";
            if (msg == null || msg.isEmpty()) {
                session.send(MessengerPackets.friendChatError());
                return;
            }
            if (uid == 0) {
                session.send(MessengerPackets.friendChatError());
                return;
            }
            var row = friends.find(session.player().uid, uid).orElse(null);
            if (row == null || (row.stateFlag() & MessengerPackets.FLAG_BLOCK) != 0) {
                session.send(MessengerPackets.friendChatError());
                return;
            }
            Session target = sessions.findByUid(uid);
            if (target == null) {
                session.send(MessengerPackets.friendChatError());
                return;
            }
            var reverse = friends.find(uid, session.player().uid).orElse(null);
            if (reverse == null || (reverse.stateFlag() & MessengerPackets.FLAG_BLOCK) != 0) {
                session.send(MessengerPackets.friendChatError());
                return;
            }
            target.send(MessengerPackets.friendChat(
                    (int) session.player().uid, session.player().nickname, msg));
        } catch (RuntimeException e) {
            log.warn("chat friend failed uid={}: {}", session.player().uid, e.toString());
            session.send(MessengerPackets.friendChatError());
        }
    }

    private void broadcastStatus(Session session, boolean includeSelf) {
        byte[] packet = MessengerPackets.friendStatus(
                (int) session.player().uid,
                session.player().messengerState,
                channelInfo(session.player()));
        for (Session friend : onlineFriends(session)) {
            if (!includeSelf && friend == session) {
                continue;
            }
            friend.send(packet);
        }
    }

    private void sendLogoutToFriends(Session session) {
        if (!session.authorized()) {
            return;
        }
        PlayerContext pi = session.player();
        if (pi.messengerLogoutSent) {
            return;
        }
        pi.messengerLogoutSent = true;
        byte[] packet = MessengerPackets.friendLogout((int) pi.uid);
        for (Session friend : onlineFriends(session)) {
            friend.send(packet);
        }
    }

    private List<Session> onlineFriends(Session session) {
        List<Session> out = new ArrayList<>();
        for (FriendRepository.FriendRow row : friends.friends(session.player().uid)) {
            if ((row.stateFlag() & MessengerPackets.FLAG_BLOCK) != 0) {
                continue;
            }
            Session live = sessions.findByUid(row.friendUid());
            if (live != null) {
                out.add(live);
            }
        }
        return out;
    }

    private static byte[] channelInfo(PlayerContext pi) {
        byte[] cpi = pi.channelPlayerInfo;
        return cpi != null ? cpi : MessengerPackets.emptyChannelPlayerInfo();
    }
}
