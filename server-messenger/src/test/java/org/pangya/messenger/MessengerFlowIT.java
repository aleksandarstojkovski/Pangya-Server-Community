package org.pangya.messenger;

import org.junit.jupiter.api.Test;
import org.pangya.db.DatabaseSupport;
import org.pangya.network.AppConfig;
import org.pangya.network.client.PangyaFakeClient;
import org.pangya.network.auth.AuthOutbound;
import org.pangya.protocol.auth.AuthS2s;
import org.pangya.protocol.messenger.MessengerPackets;
import org.pangya.protocol.packet.PacketIo;
import org.pangya.protocol.packet.PacketReader;
import org.pangya.protocol.packet.PacketWriter;

import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessengerFlowIT {

    @Test
    void fakeClientLoginThenEmptyFriendList() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        DatabaseSupport.migrate(jdbc, user, password);
        clearGuildMembership(jdbc, user, password, 10001);

        try (MessengerRuntime runtime = new MessengerRuntime(new AppConfig(testYaml(jdbc, user, password)));
             PangyaFakeClient client = new PangyaFakeClient()) {
            client.connect("127.0.0.1", runtime.port(), PangyaFakeClient.HelloKind.MESSENGER);
            byte[] hello = client.awaitHello(5, TimeUnit.SECONDS);
            PacketReader helloBody = new PacketReader(PacketIo.slice(hello, 4, hello.length - 4));
            assertEquals(MessengerPackets.SERVER_CONNECT, helloBody.opcode());
            assertEquals(1, helloBody.u8());
            assertEquals(1, helloBody.u8());
            assertEquals(client.key(), helloBody.u32());

            client.sendPlain(MessengerPackets.clientLogin(10001, "TestNick"));
            PacketReader ack = new PacketReader(client.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(MessengerPackets.SERVER_LOGIN_ACK, ack.opcode());
            assertEquals(0, ack.u8());
            assertEquals(10001, ack.u32());

            client.sendPlain(new org.pangya.protocol.packet.PacketWriter()
                    .opcode(MessengerPackets.CLIENT_REQ_USERINFO)
                    .toBytes());
            PacketReader friends = new PacketReader(client.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(MessengerPackets.SERVER_FRIEND_AND_GUILD_LIST, friends.opcode());
            assertEquals(MessengerPackets.SUB_CHANGE_MY_STATUS, friends.u16());
            assertEquals(10001, friends.u32());
            assertEquals(MessengerPackets.STATE_ONLINE, friends.u32());
            assertEquals(1, friends.u8());
            assertEquals(MessengerPackets.CHANNEL_PLAYER_INFO_BYTES, friends.remaining());
            PacketReader page = new PacketReader(client.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(MessengerPackets.SERVER_FRIEND_AND_GUILD_LIST, page.opcode());
            assertEquals(MessengerPackets.SUB_FRIEND_LIST_PAGE, page.u16());
            assertEquals(1, page.u8());
            assertEquals(0, page.u16());
            assertEquals(0, page.u16());
            assertEquals(0, page.remaining());
        }
    }

    @Test
    void addAgreeBlockRemoveFriend() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        DatabaseSupport.migrate(jdbc, user, password);
        clearGuildMembership(jdbc, user, password, 10001, 10002);

        try (var ds = DatabaseSupport.dataSource(jdbc, user, password)) {
            var friends = new org.pangya.db.JdbiFriendRepository(DatabaseSupport.jdbi(ds));
            friends.delete(10001, 10002);
            friends.delete(10002, 10001);
        }

        try (MessengerRuntime runtime = new MessengerRuntime(new AppConfig(testYaml(jdbc, user, password)));
             PangyaFakeClient a = new PangyaFakeClient();
             PangyaFakeClient b = new PangyaFakeClient()) {
            a.connect("127.0.0.1", runtime.port(), PangyaFakeClient.HelloKind.MESSENGER);
            a.awaitHello(5, TimeUnit.SECONDS);
            a.sendPlain(MessengerPackets.clientLogin(10001, "TestNick"));
            PacketReader loginA = new PacketReader(a.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(MessengerPackets.SERVER_LOGIN_ACK, loginA.opcode());
            assertEquals(0, loginA.u8());

            a.sendPlain(MessengerPackets.clientAddFriend(10002, "TestNick2"));
            PacketReader added = new PacketReader(a.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(MessengerPackets.SERVER_FRIEND_AND_GUILD_LIST, added.opcode());
            assertEquals(MessengerPackets.SUB_REGISTER_FRIEND, added.u16());
            assertEquals(0, added.u32());

            b.connect("127.0.0.1", runtime.port(), PangyaFakeClient.HelloKind.MESSENGER);
            b.awaitHello(5, TimeUnit.SECONDS);
            b.sendPlain(MessengerPackets.clientLogin(10002, "TestNick2"));
            PacketReader loginB = new PacketReader(b.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(MessengerPackets.SERVER_LOGIN_ACK, loginB.opcode());
            assertEquals(0, loginB.u8());

            b.sendPlain(MessengerPackets.clientAgreeFriend(10001));
            PacketReader agreed = new PacketReader(b.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(MessengerPackets.SERVER_FRIEND_AND_GUILD_LIST, agreed.opcode());
            assertEquals(MessengerPackets.SUB_FRIEND_AGREE, agreed.u16());
            assertEquals(0, agreed.u32());
            assertEquals(10001, agreed.u32());

            PacketReader accepted = new PacketReader(a.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(MessengerPackets.SERVER_FRIEND_AND_GUILD_LIST, accepted.opcode());
            assertEquals(MessengerPackets.SUB_FRIEND_ACCEPTED, accepted.u16());

            a.sendPlain(new org.pangya.protocol.packet.PacketWriter()
                    .opcode(MessengerPackets.CLIENT_REQ_USERINFO)
                    .toBytes());
            PacketReader status = new PacketReader(a.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(MessengerPackets.SERVER_FRIEND_AND_GUILD_LIST, status.opcode());
            assertEquals(MessengerPackets.SUB_CHANGE_MY_STATUS, status.u16());
            PacketReader page = new PacketReader(a.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(MessengerPackets.SERVER_FRIEND_AND_GUILD_LIST, page.opcode());
            assertEquals(MessengerPackets.SUB_FRIEND_LIST_PAGE, page.u16());
            assertEquals(1, page.u8());
            assertEquals(1, page.u16());
            assertEquals(1, page.u16());
            assertEquals(
                    MessengerPackets.FRIEND_INFO_BYTES + MessengerPackets.CHANNEL_PLAYER_INFO_BYTES + 5,
                    page.remaining());

            a.sendPlain(MessengerPackets.clientBlockFriend(10002));
            PacketReader blocked = new PacketReader(a.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(MessengerPackets.SERVER_FRIEND_AND_GUILD_LIST, blocked.opcode());
            assertEquals(MessengerPackets.SUB_FRIEND_BLOCK, blocked.u16());
            assertEquals(0, blocked.u32());
            assertEquals(10002, blocked.u32());

            a.sendPlain(MessengerPackets.clientRemoveFriend(10002, "TestNick2"));
            PacketReader removed = new PacketReader(a.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(MessengerPackets.SERVER_FRIEND_AND_GUILD_LIST, removed.opcode());
            assertEquals(MessengerPackets.SUB_FRIEND_REMOVE, removed.u16());
            assertEquals(0, removed.u32());
            assertEquals(10002, removed.u32());
        }
    }

    @Test
    void checkNicknameOkAndMissing() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        DatabaseSupport.migrate(jdbc, user, password);

        try (MessengerRuntime runtime = new MessengerRuntime(new AppConfig(testYaml(jdbc, user, password)));
             PangyaFakeClient client = new PangyaFakeClient()) {
            client.connect("127.0.0.1", runtime.port(), PangyaFakeClient.HelloKind.MESSENGER);
            client.awaitHello(5, TimeUnit.SECONDS);
            client.sendPlain(MessengerPackets.clientLogin(10001, "TestNick"));
            PacketReader login = new PacketReader(client.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(MessengerPackets.SERVER_LOGIN_ACK, login.opcode());
            assertEquals(0, login.u8());

            client.sendPlain(MessengerPackets.clientCheckNick("TestNick2"));
            PacketReader ok = new PacketReader(client.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(MessengerPackets.SERVER_FRIEND_AND_GUILD_LIST, ok.opcode());
            assertEquals(MessengerPackets.SUB_CHECK_NICK, ok.u16());
            assertEquals(0, ok.u32());
            assertEquals("TestNick2", ok.pstr());
            assertEquals(10002, ok.u32());

            client.sendPlain(MessengerPackets.clientCheckNick("NoSuchNickZZ"));
            PacketReader missing = new PacketReader(client.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(MessengerPackets.SERVER_FRIEND_AND_GUILD_LIST, missing.opcode());
            assertEquals(MessengerPackets.SUB_CHECK_NICK, missing.u16());
            assertEquals(MessengerPackets.CHECK_NICK_ERR_MISSING, missing.u32());
            assertEquals("NoSuchNickZZ", missing.pstr());
        }
    }

    @Test
    void chatStateChannelUpdateAndLogout() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        DatabaseSupport.migrate(jdbc, user, password);

        try (var ds = DatabaseSupport.dataSource(jdbc, user, password)) {
            var friendRepo = new org.pangya.db.JdbiFriendRepository(DatabaseSupport.jdbi(ds));
            friendRepo.delete(10001, 10002);
            friendRepo.delete(10002, 10001);
            friendRepo.add(10001, new org.pangya.db.FriendRepository.FriendRow(
                    10002, "TestNick2", "Friend", -1, 0, -1, 0, 0, 0, 255,
                    MessengerPackets.FLAG_FRIEND, 1, MessengerPackets.FRIEND_FLAG));
            friendRepo.add(10002, new org.pangya.db.FriendRepository.FriendRow(
                    10001, "TestNick", "Friend", -1, 0, -1, 0, 0, 0, 255,
                    MessengerPackets.FLAG_FRIEND, 1, MessengerPackets.FRIEND_FLAG));
        }

        try (MessengerRuntime runtime = new MessengerRuntime(new AppConfig(testYaml(jdbc, user, password)));
             PangyaFakeClient a = new PangyaFakeClient();
             PangyaFakeClient b = new PangyaFakeClient()) {
            a.connect("127.0.0.1", runtime.port(), PangyaFakeClient.HelloKind.MESSENGER);
            a.awaitHello(5, TimeUnit.SECONDS);
            a.sendPlain(MessengerPackets.clientLogin(10001, "TestNick"));
            PacketReader loginA = new PacketReader(a.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(MessengerPackets.SERVER_LOGIN_ACK, loginA.opcode());
            assertEquals(0, loginA.u8());

            b.connect("127.0.0.1", runtime.port(), PangyaFakeClient.HelloKind.MESSENGER);
            b.awaitHello(5, TimeUnit.SECONDS);
            b.sendPlain(MessengerPackets.clientLogin(10002, "TestNick2"));
            PacketReader loginB = new PacketReader(b.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(MessengerPackets.SERVER_LOGIN_ACK, loginB.opcode());
            assertEquals(0, loginB.u8());

            byte[] cpi = MessengerPackets.channelPlayerInfo(42, 1, 30201, 3, "Lobby-1");
            a.sendPlain(MessengerPackets.clientUpdateChannel(cpi));
            PacketReader selfStatus = new PacketReader(a.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(MessengerPackets.SERVER_FRIEND_AND_GUILD_LIST, selfStatus.opcode());
            assertEquals(MessengerPackets.SUB_CHANGE_MY_STATUS, selfStatus.u16());
            assertEquals(10001, selfStatus.u32());
            assertEquals(MessengerPackets.STATE_ONLINE, selfStatus.u32());
            assertEquals(1, selfStatus.u8());
            assertEquals(MessengerPackets.CHANNEL_PLAYER_INFO_BYTES, selfStatus.remaining());

            PacketReader friendStatus = new PacketReader(b.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(MessengerPackets.SERVER_FRIEND_AND_GUILD_LIST, friendStatus.opcode());
            assertEquals(MessengerPackets.SUB_CHANGE_MY_STATUS, friendStatus.u16());
            assertEquals(10001, friendStatus.u32());

            a.sendPlain(MessengerPackets.clientUpdateState(6));
            PacketReader stateEcho = new PacketReader(b.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(MessengerPackets.SERVER_FRIEND_AND_GUILD_LIST, stateEcho.opcode());
            assertEquals(MessengerPackets.SUB_CHANGE_MY_STATUS, stateEcho.u16());
            assertEquals(10001, stateEcho.u32());
            assertEquals(6, stateEcho.u32());

            a.sendPlain(MessengerPackets.clientChatFriend(10002, "hello friend"));
            PacketReader chat = new PacketReader(b.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(MessengerPackets.SERVER_FRIEND_AND_GUILD_LIST, chat.opcode());
            assertEquals(MessengerPackets.SUB_FRIEND_CHAT, chat.u16());
            assertEquals(10001, chat.u32());
            assertEquals("TestNick", chat.pstr());
            assertEquals("hello friend", chat.pstr());
            assertEquals(0, chat.u8());

            a.sendPlain(MessengerPackets.clientNotifyLogout());
            PacketReader logout = new PacketReader(b.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(MessengerPackets.SERVER_FRIEND_AND_GUILD_LIST, logout.opcode());
            assertEquals(MessengerPackets.SUB_FRIEND_LOGOUT, logout.u16());
            assertEquals(10001, logout.u32());
        }
    }

    @Test
    void unblockFriendAndAssignApelido() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        DatabaseSupport.migrate(jdbc, user, password);

        try (var ds = DatabaseSupport.dataSource(jdbc, user, password)) {
            var friendRepo = new org.pangya.db.JdbiFriendRepository(DatabaseSupport.jdbi(ds));
            friendRepo.delete(10001, 10002);
            friendRepo.delete(10002, 10001);
            friendRepo.add(10001, new org.pangya.db.FriendRepository.FriendRow(
                    10002, "TestNick2", "Friend", -1, 0, -1, 0, 0, 0, 255,
                    MessengerPackets.FLAG_FRIEND, 1, MessengerPackets.FRIEND_FLAG));
            friendRepo.add(10002, new org.pangya.db.FriendRepository.FriendRow(
                    10001, "TestNick", "Friend", -1, 0, -1, 0, 0, 0, 255,
                    MessengerPackets.FLAG_FRIEND, 1, MessengerPackets.FRIEND_FLAG));
        }

        try (MessengerRuntime runtime = new MessengerRuntime(new AppConfig(testYaml(jdbc, user, password)));
             PangyaFakeClient a = new PangyaFakeClient();
             PangyaFakeClient b = new PangyaFakeClient()) {
            a.connect("127.0.0.1", runtime.port(), PangyaFakeClient.HelloKind.MESSENGER);
            a.awaitHello(5, TimeUnit.SECONDS);
            a.sendPlain(MessengerPackets.clientLogin(10001, "TestNick"));
            PacketReader loginA = new PacketReader(a.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(MessengerPackets.SERVER_LOGIN_ACK, loginA.opcode());
            assertEquals(0, loginA.u8());

            b.connect("127.0.0.1", runtime.port(), PangyaFakeClient.HelloKind.MESSENGER);
            b.awaitHello(5, TimeUnit.SECONDS);
            b.sendPlain(MessengerPackets.clientLogin(10002, "TestNick2"));
            PacketReader loginB = new PacketReader(b.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(MessengerPackets.SERVER_LOGIN_ACK, loginB.opcode());
            assertEquals(0, loginB.u8());

            a.sendPlain(MessengerPackets.clientBlockFriend(10002));
            PacketReader blocked = new PacketReader(a.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(MessengerPackets.SERVER_FRIEND_AND_GUILD_LIST, blocked.opcode());
            assertEquals(MessengerPackets.SUB_FRIEND_BLOCK, blocked.u16());

            PacketReader blockedNotify = new PacketReader(b.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(MessengerPackets.SERVER_FRIEND_AND_GUILD_LIST, blockedNotify.opcode());
            assertEquals(MessengerPackets.SUB_FRIEND_LOGOUT, blockedNotify.u16());

            a.sendPlain(MessengerPackets.clientUnblockFriend(10002));
            PacketReader unblocked = new PacketReader(a.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(MessengerPackets.SERVER_FRIEND_AND_GUILD_LIST, unblocked.opcode());
            assertEquals(MessengerPackets.SUB_FRIEND_UNBLOCK, unblocked.u16());
            assertEquals(0, unblocked.u32());
            assertEquals(10002, unblocked.u32());

            PacketReader onlineAgain = new PacketReader(b.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(MessengerPackets.SERVER_FRIEND_AND_GUILD_LIST, onlineAgain.opcode());
            assertEquals(MessengerPackets.SUB_CHANGE_MY_STATUS, onlineAgain.u16());
            assertEquals(10001, onlineAgain.u32());

            a.sendPlain(MessengerPackets.clientAssignApelido(10002, "Buddy"));
            PacketReader alias = new PacketReader(a.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(MessengerPackets.SERVER_FRIEND_AND_GUILD_LIST, alias.opcode());
            assertEquals(MessengerPackets.SUB_FRIEND_APELIDO, alias.u16());
            assertEquals(0, alias.u32());
            assertEquals(10002, alias.u32());
            assertEquals("Buddy", alias.pstr());
        }
    }

    @Test
    void guildChatBroadcastsToMembers() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        DatabaseSupport.migrate(jdbc, user, password);
        seedTestGuild(jdbc, user, password, 9001L, "TestGuild", 10001, 10002);

        try (MessengerRuntime runtime = new MessengerRuntime(new AppConfig(testYaml(jdbc, user, password)));
             PangyaFakeClient a = new PangyaFakeClient();
             PangyaFakeClient b = new PangyaFakeClient()) {
            a.connect("127.0.0.1", runtime.port(), PangyaFakeClient.HelloKind.MESSENGER);
            a.awaitHello(5, TimeUnit.SECONDS);
            a.sendPlain(MessengerPackets.clientLogin(10001, "TestNick"));
            PacketReader loginA = new PacketReader(a.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(MessengerPackets.SERVER_LOGIN_ACK, loginA.opcode());
            assertEquals(0, loginA.u8());

            b.connect("127.0.0.1", runtime.port(), PangyaFakeClient.HelloKind.MESSENGER);
            b.awaitHello(5, TimeUnit.SECONDS);
            b.sendPlain(MessengerPackets.clientLogin(10002, "TestNick2"));
            PacketReader loginB = new PacketReader(b.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(MessengerPackets.SERVER_LOGIN_ACK, loginB.opcode());
            assertEquals(0, loginB.u8());

            a.sendPlain(MessengerPackets.clientChatGuild("hello guild"));
            PacketReader self = new PacketReader(a.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(MessengerPackets.SERVER_FRIEND_AND_GUILD_LIST, self.opcode());
            assertEquals(MessengerPackets.SUB_FRIEND_CHAT, self.u16());
            assertEquals(10001, self.u32());
            assertEquals("TestNick", self.pstr());
            assertEquals("hello guild", self.pstr());
            assertEquals(1, self.u8());

            PacketReader peer = new PacketReader(b.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(MessengerPackets.SERVER_FRIEND_AND_GUILD_LIST, peer.opcode());
            assertEquals(MessengerPackets.SUB_FRIEND_CHAT, peer.u16());
            assertEquals(10001, peer.u32());
            assertEquals("TestNick", peer.pstr());
            assertEquals("hello guild", peer.pstr());
            assertEquals(1, peer.u8());
        }
    }

    @Test
    void roomInviteAcceptsOwnUid() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        DatabaseSupport.migrate(jdbc, user, password);

        try (MessengerRuntime runtime = new MessengerRuntime(new AppConfig(testYaml(jdbc, user, password)));
             PangyaFakeClient client = new PangyaFakeClient()) {
            client.connect("127.0.0.1", runtime.port(), PangyaFakeClient.HelloKind.MESSENGER);
            client.awaitHello(5, TimeUnit.SECONDS);
            client.sendPlain(MessengerPackets.clientLogin(10001, "TestNick"));
            PacketReader login = new PacketReader(client.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(MessengerPackets.SERVER_LOGIN_ACK, login.opcode());
            assertEquals(0, login.u8());

            client.sendPlain(MessengerPackets.clientNotifyRoomInvite(10001));
        }
    }

    @Test
    void guildBattleInviteAndGiftNotify() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        DatabaseSupport.migrate(jdbc, user, password);

        try (MessengerRuntime runtime = new MessengerRuntime(new AppConfig(testYaml(jdbc, user, password)));
             PangyaFakeClient client = new PangyaFakeClient()) {
            client.connect("127.0.0.1", runtime.port(), PangyaFakeClient.HelloKind.MESSENGER);
            client.awaitHello(5, TimeUnit.SECONDS);
            client.sendPlain(MessengerPackets.clientLogin(10001, "TestNick"));
            PacketReader login = new PacketReader(client.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(MessengerPackets.SERVER_LOGIN_ACK, login.opcode());
            assertEquals(0, login.u8());

            client.sendPlain(MessengerPackets.clientGuildBattleRoomInvite(
                    30201, 1, 42, 10001, "TestNick", 10002));
            client.sendPlain(MessengerPackets.clientGiftItemNotify(10001, 10002));
        }
    }

    @Test
    void friendListIncludesGuildMembers() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        DatabaseSupport.migrate(jdbc, user, password);
        seedTestGuild(jdbc, user, password, 9001L, "TestGuild", 10001, 10002);

        try (MessengerRuntime runtime = new MessengerRuntime(new AppConfig(testYaml(jdbc, user, password)));
             PangyaFakeClient leader = new PangyaFakeClient()) {
            leader.connect("127.0.0.1", runtime.port(), PangyaFakeClient.HelloKind.MESSENGER);
            leader.awaitHello(5, TimeUnit.SECONDS);
            leader.sendPlain(MessengerPackets.clientLogin(10001, "TestNick"));
            PacketReader login = new PacketReader(leader.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(MessengerPackets.SERVER_LOGIN_ACK, login.opcode());
            assertEquals(0, login.u8());

            leader.sendPlain(new PacketWriter().opcode(MessengerPackets.CLIENT_REQ_USERINFO).toBytes());
            PacketReader status = new PacketReader(leader.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(MessengerPackets.SERVER_FRIEND_AND_GUILD_LIST, status.opcode());
            assertEquals(MessengerPackets.SUB_CHANGE_MY_STATUS, status.u16());

            PacketReader page = new PacketReader(leader.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(MessengerPackets.SERVER_FRIEND_AND_GUILD_LIST, page.opcode());
            assertEquals(MessengerPackets.SUB_FRIEND_LIST_PAGE, page.u16());
            assertEquals(1, page.u8());
            assertEquals(2, page.u16());
            assertEquals(2, page.u16());
        }
    }

    @Test
    void authConfirmLoginCompletesPendingSession() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        DatabaseSupport.migrate(jdbc, user, password);
        clearGuildMembership(jdbc, user, password, 10001);

        List<Long> requested = new CopyOnWriteArrayList<>();
        try (MessengerRuntime runtime = new MessengerRuntime(
                new AppConfig(testYaml(jdbc, user, password)), new AuthOutbound() {
                    @Override
                    public boolean isLive() {
                        return true;
                    }

                    @Override
                    public void requestInfoPlayerOnline(int gameServerUid, long playerUid) {
                        requested.add(playerUid);
                    }

                    @Override
                    public void sendInfoPlayerOnline(int reqServerUid, AuthS2s.AuthServerPlayerInfo info) {}
                });
             PangyaFakeClient client = new PangyaFakeClient()) {
            client.connect("127.0.0.1", runtime.port(), PangyaFakeClient.HelloKind.MESSENGER);
            client.awaitHello(5, TimeUnit.SECONDS);
            client.sendPlain(MessengerPackets.clientLogin(10001, "TestNick"));

            long deadline = System.currentTimeMillis() + 500;
            while (System.currentTimeMillis() < deadline) {
                Thread.sleep(20);
            }
            assertEquals(1, requested.size());
            assertEquals(10001L, requested.get(0).longValue());

            runtime.handler().onAuthPacket(
                    AuthS2s.AUTH_CONFIRM_PLAYER_INFO,
                    new PacketReader(new PacketWriter()
                            .u32(20202)
                            .i32(1)
                            .u32(10001)
                            .pstr("testuser")
                            .pstr("127.0.0.1")
                            .toBytes()));

            PacketReader login = new PacketReader(client.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(MessengerPackets.SERVER_LOGIN_ACK, login.opcode());
            assertEquals(0, login.u8());
            assertEquals(10001, login.u32());
        }
    }

    @Test
    void authInfoPlayerOnlineReportsOnlineAndOffline() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        DatabaseSupport.migrate(jdbc, user, password);
        clearGuildMembership(jdbc, user, password, 10001);

        List<AuthS2s.AuthServerPlayerInfo> sent = new CopyOnWriteArrayList<>();
        try (MessengerRuntime runtime = new MessengerRuntime(
                new AppConfig(testYaml(jdbc, user, password)), new AuthOutbound() {
                    @Override
                    public void sendInfoPlayerOnline(int reqServerUid, AuthS2s.AuthServerPlayerInfo info) {
                        sent.add(info);
                    }
                });
             PangyaFakeClient client = new PangyaFakeClient()) {
            runtime.handler().onAuthPacket(
                    AuthS2s.AUTH_INFO_PLAYER_ONLINE,
                    new PacketReader(new PacketWriter().u32(30201).u32(99999).toBytes()));
            assertEquals(1, sent.size());
            assertEquals(-1, sent.get(0).option());
            assertEquals(99999L, sent.get(0).uid());
            sent.clear();

            client.connect("127.0.0.1", runtime.port(), PangyaFakeClient.HelloKind.MESSENGER);
            client.awaitHello(5, TimeUnit.SECONDS);
            client.sendPlain(MessengerPackets.clientLogin(10001, "TestNick"));
            PacketReader login = new PacketReader(client.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(MessengerPackets.SERVER_LOGIN_ACK, login.opcode());
            assertEquals(0, login.u8());

            runtime.handler().onAuthPacket(
                    AuthS2s.AUTH_INFO_PLAYER_ONLINE,
                    new PacketReader(new PacketWriter().u32(30201).u32(10001).toBytes()));
            assertEquals(1, sent.size());
            AuthS2s.AuthServerPlayerInfo online = sent.get(0);
            assertEquals(1, online.option());
            assertEquals(10001L, online.uid());
            assertEquals("testuser", online.id());
        }
    }

    @Test
    void authDisconnectDisconnectsLoggedInPlayer() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        DatabaseSupport.migrate(jdbc, user, password);
        clearGuildMembership(jdbc, user, password, 10001);

        List<long[]> confirms = new CopyOnWriteArrayList<>();
        try (MessengerRuntime runtime = new MessengerRuntime(new AppConfig(testYaml(jdbc, user, password)), new AuthOutbound() {
                    @Override
                    public void sendInfoPlayerOnline(int reqServerUid, AuthS2s.AuthServerPlayerInfo info) {}

                    @Override
                    public void sendConfirmDisconnectPlayer(long serverUid, long playerUid) {
                        confirms.add(new long[] {serverUid, playerUid});
                    }
                });
             PangyaFakeClient client = new PangyaFakeClient()) {
            client.connect("127.0.0.1", runtime.port(), PangyaFakeClient.HelloKind.MESSENGER);
            client.awaitHello(5, TimeUnit.SECONDS);
            client.sendPlain(MessengerPackets.clientLogin(10001, "TestNick"));
            PacketReader login = new PacketReader(client.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(MessengerPackets.SERVER_LOGIN_ACK, login.opcode());
            assertEquals(0, login.u8());

            assertTrue(client.connected());

            runtime.handler().onAuthPacket(
                    AuthS2s.AUTH_DISCONNECT_PLAYER,
                    new PacketReader(new PacketWriter().u32(10001).u32(30201).u8(1).toBytes()));

            long deadline = System.currentTimeMillis() + 2000;
            while (client.connected() && System.currentTimeMillis() < deadline) {
                Thread.sleep(20);
            }
            assertFalse(client.connected());
            assertEquals(1, confirms.size());
            assertEquals(30201L, confirms.get(0)[0]);
            assertEquals(10001L, confirms.get(0)[1]);
        }
    }

    @Test
    void authShutdownForwardsSecondsToScheduler() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        DatabaseSupport.migrate(jdbc, user, password);

        java.util.concurrent.atomic.AtomicInteger requested = new java.util.concurrent.atomic.AtomicInteger(-1);
        AppConfig config = new AppConfig(testYaml(jdbc, user, password));
        try (MessengerRuntime runtime = new MessengerRuntime(config, null, requested::set)) {
            runtime.handler().onAuthPacket(
                    AuthS2s.AUTH_SHUTDOWN,
                    new PacketReader(new PacketWriter().i32(45).toBytes()));
            assertEquals(45, requested.get());
        }
    }

    @Test
    void authReloadGlobalSystemAcceptsTipoZero() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password));
        try (MessengerRuntime runtime = new MessengerRuntime(config)) {
            runtime.handler().onAuthPacket(
                    AuthS2s.AUTH_RELOAD_SYSTEM,
                    new PacketReader(new PacketWriter().u32(0).toBytes()));
        }
    }

    @Test
    void authGuildAcceptBroadcastsJoinToMembers() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        DatabaseSupport.migrate(jdbc, user, password);
        seedTestGuild(jdbc, user, password, 9001L, "TestGuild", 10001);
        addGuildMember(jdbc, user, password, 9001L, 10002L);

        try (MessengerRuntime runtime = new MessengerRuntime(new AppConfig(testYaml(jdbc, user, password)));
             PangyaFakeClient leader = new PangyaFakeClient()) {
            leader.connect("127.0.0.1", runtime.port(), PangyaFakeClient.HelloKind.MESSENGER);
            leader.awaitHello(5, TimeUnit.SECONDS);
            leader.sendPlain(MessengerPackets.clientLogin(10001, "TestNick"));
            PacketReader login = new PacketReader(leader.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(MessengerPackets.SERVER_LOGIN_ACK, login.opcode());
            assertEquals(0, login.u8());

            runtime.handler().onAuthCommand(
                    0,
                    AuthS2s.AS_ACCEPT_GUILD_MEMBER,
                    new PacketReader(new PacketWriter().u32(9001).u32(10002).toBytes()));

            PacketReader joined = awaitGuildJoined(leader);
            assertEquals(MessengerPackets.SERVER_GUILD_MEMBER_JOINED, joined.opcode());
            assertEquals(10002, joined.u32());
            assertEquals(9001, joined.u32());
        }
    }

    @Test
    void authGuildKickBroadcastsLeaveToMembers() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        DatabaseSupport.migrate(jdbc, user, password);
        seedTestGuild(jdbc, user, password, 9001L, "TestGuild", 10001, 10002);

        try (MessengerRuntime runtime = new MessengerRuntime(new AppConfig(testYaml(jdbc, user, password)));
             PangyaFakeClient leader = new PangyaFakeClient();
             PangyaFakeClient member = new PangyaFakeClient()) {
            leader.connect("127.0.0.1", runtime.port(), PangyaFakeClient.HelloKind.MESSENGER);
            leader.awaitHello(5, TimeUnit.SECONDS);
            leader.sendPlain(MessengerPackets.clientLogin(10001, "TestNick"));
            PacketReader loginA = new PacketReader(leader.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(MessengerPackets.SERVER_LOGIN_ACK, loginA.opcode());
            assertEquals(0, loginA.u8());

            member.connect("127.0.0.1", runtime.port(), PangyaFakeClient.HelloKind.MESSENGER);
            member.awaitHello(5, TimeUnit.SECONDS);
            member.sendPlain(MessengerPackets.clientLogin(10002, "TestNick2"));
            PacketReader loginB = new PacketReader(member.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(MessengerPackets.SERVER_LOGIN_ACK, loginB.opcode());
            assertEquals(0, loginB.u8());

            removeGuildMember(jdbc, user, password, 10002L);
            runtime.handler().onAuthCommand(
                    0,
                    AuthS2s.AS_KICK_GUILD_MEMBER,
                    new PacketReader(new PacketWriter().u32(9001).u32(10002).toBytes()));

            PacketReader left = awaitGuildLeft(leader);
            assertEquals(MessengerPackets.SERVER_GUILD_MEMBER_LEFT, left.opcode());
            assertEquals(10002, left.u32());
        }
    }

    @Test
    void nickMismatchSendsLoginFail() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        DatabaseSupport.migrate(jdbc, user, password);

        try (MessengerRuntime runtime = new MessengerRuntime(new AppConfig(testYaml(jdbc, user, password)));
             PangyaFakeClient client = new PangyaFakeClient()) {
            client.connect("127.0.0.1", runtime.port(), PangyaFakeClient.HelloKind.MESSENGER);
            client.awaitHello(5, TimeUnit.SECONDS);
            client.sendPlain(MessengerPackets.clientLogin(10001, "WrongNick"));
            PacketReader ack = new PacketReader(client.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(MessengerPackets.SERVER_LOGIN_ACK, ack.opcode());
            assertEquals(1, ack.u8());
        }
    }

    private static PacketReader awaitGuildJoined(PangyaFakeClient client) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            byte[] raw = client.awaitPlain(500, TimeUnit.MILLISECONDS);
            PacketReader packet = new PacketReader(raw);
            if (packet.opcode() == MessengerPackets.SERVER_GUILD_MEMBER_JOINED) {
                return new PacketReader(raw);
            }
        }
        throw new AssertionError("timed out waiting for guild join packet");
    }

    private static PacketReader awaitGuildLeft(PangyaFakeClient client) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            byte[] raw = client.awaitPlain(500, TimeUnit.MILLISECONDS);
            PacketReader packet = new PacketReader(raw);
            if (packet.opcode() == MessengerPackets.SERVER_GUILD_MEMBER_LEFT) {
                return new PacketReader(raw);
            }
        }
        throw new AssertionError("timed out waiting for guild leave packet");
    }

    private static Map<String, Object> testYaml(String jdbc, String user, String password) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("server", Map.of(
                "name", "messenger-test",
                "uid", 30201,
                "tipo", 3,
                "port", 0,
                "healthPort", freePort(),
                "ip", "127.0.0.1"
        ));
        root.put("database", Map.of("url", jdbc, "user", user, "password", password));
        root.put("redis", Map.of("uri", "redis://localhost:6379"));
        root.put("auth", Map.of("enabled", false, "host", "127.0.0.1", "port", 1));
        return root;
    }

    private static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String env(String name, String fallback) {
        String v = System.getenv(name);
        return (v == null || v.isBlank()) ? fallback : v;
    }

    private static void seedTestGuild(
            String jdbc, String user, String password, long guildUid, String guildName, long... members) {
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password)) {
            var jdbi = DatabaseSupport.jdbi(ds);
            jdbi.useHandle(h -> {
                for (long memberUid : members) {
                    h.createUpdate("DELETE FROM pangya.pangya_guild_member WHERE \"MEMBER_UID\" = :uid")
                            .bind("uid", memberUid)
                            .execute();
                }
                h.createUpdate("DELETE FROM pangya.pangya_guild WHERE \"GUILD_UID\" = :gid")
                        .bind("gid", guildUid)
                        .execute();
                h.createUpdate("""
                                INSERT INTO pangya.pangya_guild (
                                    "GUILD_UID", "GUILD_ID", "GUILD_NAME", "GUILD_LEADER", "GUILD_SUB_MASTER",
                                    "GUILD_CONDITION_LEVEL", "GUILD_STATE", "GUILD_FLAG", "GUILD_PERMITION_JOIN",
                                    "GUILD_PANG", "GUILD_POINT", "GUILD_WIN", "GUILD_LOSE", "GUILD_DRAW",
                                    "GUILD_MARK_IMG", "GUILD_MARK_IMG_IDX", "GUILD_NEW_MARK_IDX",
                                    "GUILD_NOTICE", "GUILD_INFO", "GUILD_REG_DATE"
                                ) OVERRIDING SYSTEM VALUE VALUES (
                                    :gid, 'TG9001', :name, :leader, 0,
                                    0, 0, 0, 0,
                                    0, 0, 0, 0, 0,
                                    '', 0, 0,
                                    '', '', NOW()
                                )
                                """)
                        .bind("gid", guildUid)
                        .bind("name", guildName)
                        .bind("leader", members.length > 0 ? members[0] : 0L)
                        .execute();
                for (long memberUid : members) {
                    h.createUpdate("""
                                    INSERT INTO pangya.pangya_guild_member (
                                        "GUILD_UID", "MEMBER_UID", "GUILD_PANG", "GUILD_POINT",
                                        "MEMBER_FLAG", "MEMBER_STATE_FLAG", "REG_DATE"
                                    ) VALUES (
                                        :gid, :uid, 0, 0, 0, 0, NOW()
                                    )
                                    """)
                            .bind("gid", guildUid)
                            .bind("uid", memberUid)
                            .execute();
                }
            });
        }
    }

    private static void clearGuildMembership(
            String jdbc, String user, String password, long... members) {
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password)) {
            DatabaseSupport.jdbi(ds).useHandle(h -> {
                for (long memberUid : members) {
                    h.createUpdate("DELETE FROM pangya.pangya_guild_member WHERE \"MEMBER_UID\" = :uid")
                            .bind("uid", memberUid)
                            .execute();
                    h.createUpdate("UPDATE pangya.account SET \"Guild_UID\" = 0 WHERE \"UID\" = :uid")
                            .bind("uid", memberUid)
                            .execute();
                }
            });
        }
    }

    private static void addGuildMember(
            String jdbc, String user, String password, long guildUid, long memberUid) {
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password)) {
            DatabaseSupport.jdbi(ds).useHandle(h -> h.createUpdate("""
                            INSERT INTO pangya.pangya_guild_member (
                                "GUILD_UID", "MEMBER_UID", "GUILD_PANG", "GUILD_POINT",
                                "MEMBER_FLAG", "MEMBER_STATE_FLAG", "REG_DATE"
                            ) VALUES (
                                :gid, :uid, 0, 0, 0, 0, NOW()
                            )
                            """)
                    .bind("gid", guildUid)
                    .bind("uid", memberUid)
                    .execute());
        }
    }

    private static void removeGuildMember(String jdbc, String user, String password, long memberUid) {
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password)) {
            DatabaseSupport.jdbi(ds).useHandle(h -> h.createUpdate(
                            "DELETE FROM pangya.pangya_guild_member WHERE \"MEMBER_UID\" = :uid")
                    .bind("uid", memberUid)
                    .execute());
        }
    }
}
