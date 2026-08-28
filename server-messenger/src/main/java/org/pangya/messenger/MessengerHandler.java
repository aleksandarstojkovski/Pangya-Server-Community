package org.pangya.messenger;

import org.pangya.db.FriendRepository;
import org.pangya.db.LoginRepository;
import org.pangya.network.session.PlayerContext;
import org.pangya.network.session.Session;
import org.pangya.network.session.SessionManager;
import org.pangya.protocol.messenger.MessengerPackets;
import org.pangya.protocol.packet.PacketReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GB {@code MessengerServer.requestLogin} + friend add/agree/block/remove.
 */
public final class MessengerHandler {

    private static final Logger log = LoggerFactory.getLogger(MessengerHandler.class);

    private final LoginRepository repo;
    private final FriendRepository friends;
    private final SessionManager sessions;

    public MessengerHandler(LoginRepository repo, FriendRepository friends, SessionManager sessions) {
        this.repo = repo;
        this.friends = friends;
        this.sessions = sessions;
    }

    public void onPacket(Session session, byte[] plaintext) {
        if (plaintext.length < 2) {
            return;
        }
        PacketReader reader = new PacketReader(plaintext);
        int opcode = reader.opcode();
        switch (opcode) {
            case MessengerPackets.CLIENT_CONNECT -> requestLogin(session, reader);
            case MessengerPackets.CLIENT_REQ_USERINFO -> friendList(session);
            case MessengerPackets.CLIENT_REQ_REGISTER_FRIEND -> addFriend(session, reader);
            case MessengerPackets.CLIENT_REQ_FRIEND_AGREE -> agreeFriend(session, reader);
            case MessengerPackets.CLIENT_REQ_FRIEND_BLOCK -> blockFriend(session, reader);
            case MessengerPackets.CLIENT_REQ_FRIEND_REMOVE -> removeFriend(session, reader);
            default -> log.debug("unhandled messenger opcode 0x{}", Integer.toHexString(opcode));
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
            PlayerContext pi = session.player();
            pi.uid = info.uid();
            pi.id = info.id();
            pi.nickname = info.nickname();
            pi.level = info.level();
            sessions.disconnectOthersWithUid(pi.uid, session);
            session.setAuthorized(true);
            session.send(MessengerPackets.loginOk((int) pi.uid));
            log.info("messenger login nick={} uid={}", pi.nickname, pi.uid);
        } catch (RuntimeException e) {
            log.warn("messenger login failed: {}", e.toString());
            session.send(MessengerPackets.loginFail());
            session.disconnect();
        }
    }

    private void friendList(Session session) {
        if (!session.authorized()) {
            return;
        }
        PlayerContext pi = session.player();
        session.send(MessengerPackets.friendStatus(
                (int) pi.uid, MessengerPackets.STATE_ONLINE, MessengerPackets.emptyChannelPlayerInfo()));
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
            int flag = MessengerPackets.FRIEND_FLAG;
            // C# CmdAddFriend zeros online/sex before INSERT. Requester keeps request bit.
            friends.add(session.player().uid, new FriendRepository.FriendRow(
                    info.uid(), info.nickname(), "Friend", -1, 0, -1, 0, 0, 0, 255,
                    MessengerPackets.FLAG_REQUEST));
            friends.add(info.uid(), new FriendRepository.FriendRow(
                    session.player().uid, session.player().nickname, "Friend", -1, 0, -1, 0, 0, 0, 255,
                    0));
            byte[] fi = MessengerPackets.friendInfo(info.nickname(), "Friend", (int) info.uid());
            session.send(MessengerPackets.addFriendOkOffline(
                    fi, info.level(), MessengerPackets.FLAG_REQUEST, flag));
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
                live.send(new org.pangya.protocol.packet.PacketWriter()
                        .opcode(MessengerPackets.SERVER_FRIEND_AND_GUILD_LIST)
                        .u16(MessengerPackets.SUB_FRIEND_LOGOUT)
                        .u32((int) session.player().uid)
                        .toBytes());
            }
        } catch (RuntimeException e) {
            log.warn("block friend failed: {}", e.toString());
            session.send(MessengerPackets.friendUidAck(MessengerPackets.SUB_FRIEND_BLOCK, 0x5300100, 0));
        }
    }

    private void removeFriend(Session session, PacketReader reader) {
        if (!session.authorized()) {
            return;
        }
        int uid = reader.u32();
        try {
            if (uid == 0) {
                session.send(MessengerPackets.friendUidAck(MessengerPackets.SUB_FRIEND_REMOVE, 0x5200701, 0));
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
}
