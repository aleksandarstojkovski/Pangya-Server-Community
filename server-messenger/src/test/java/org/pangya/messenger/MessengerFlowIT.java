package org.pangya.messenger;

import org.junit.jupiter.api.Test;
import org.pangya.db.DatabaseSupport;
import org.pangya.network.AppConfig;
import org.pangya.network.client.PangyaFakeClient;
import org.pangya.protocol.messenger.MessengerPackets;
import org.pangya.protocol.packet.PacketIo;
import org.pangya.protocol.packet.PacketReader;

import java.net.ServerSocket;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessengerFlowIT {

    @Test
    void fakeClientLoginThenEmptyFriendList() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        DatabaseSupport.migrate(jdbc, user, password);

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
}
