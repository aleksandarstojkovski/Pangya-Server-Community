package org.pangya.game;

import org.junit.jupiter.api.Test;
import org.pangya.db.DatabaseSupport;
import org.pangya.db.JdbiLoginRepository;
import org.pangya.db.LoginRepository;
import org.pangya.network.AppConfig;
import org.pangya.network.client.PangyaFakeClient;
import org.pangya.network.redis.SessionKeyStore;
import org.pangya.protocol.game.GamePackets;
import org.pangya.protocol.packet.PacketIo;
import org.pangya.protocol.packet.PacketReader;

import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameFlowIT {

    @Test
    void fakeClientLogsInEntersChannelCreatesAndLeavesPractice() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config)) {
            LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(ds));
            String loginKey = repo.generateAuthKeyLogin(10001);
            String gameKey = repo.generateAuthKeyGame(10001, 20202);
            keys.putLoginKey(10001, loginKey);
            keys.putGameKey(10001, 20202, gameKey);

            try (PangyaFakeClient client = new PangyaFakeClient()) {
                client.connect("127.0.0.1", runtime.port(), PangyaFakeClient.HelloKind.GAME);
                byte[] hello = client.awaitHello(5, TimeUnit.SECONDS);
                PacketReader helloBody = new PacketReader(PacketIo.slice(hello, 4, hello.length - 4));
                assertEquals(GamePackets.SERVER_HELLO, helloBody.opcode());
                assertEquals(1, helloBody.u8());
                assertEquals(1, helloBody.u8());
                assertEquals(client.key(), helloBody.u8());

                int wireVersion = GamePackets.xorPacketVersion(2016110200);
                client.sendPlain(GamePackets.clientLogin(
                        "testuser", 10001, loginKey, "852.00", wireVersion, gameKey));
                List<byte[]> loginPkts = collect(client, 2, 5, TimeUnit.SECONDS);
                assertEquals(2, loginPkts.size(), "expected 0x44 then 0x4D");

                PacketReader ack = new PacketReader(loginPkts.get(0));
                assertEquals(GamePackets.SERVER_LOGIN_ACK, ack.opcode());
                assertEquals(GamePackets.ACK_LOGIN_OK, ack.u8());

                PacketReader channels = new PacketReader(loginPkts.get(1));
                assertEquals(GamePackets.SERVER_CHANNEL_LIST, channels.opcode());
                assertEquals(2, channels.u8());
                assertEquals(2 * 77, channels.remaining());

                client.sendPlain(GamePackets.clientEnterChannel(0));
                PacketReader entered = new PacketReader(client.awaitPlain(5, TimeUnit.SECONDS));
                assertEquals(GamePackets.SERVER_CHANNEL_ENTER_ACK, entered.opcode());
                assertEquals(GamePackets.CHANNEL_ENTER_OK, entered.u8());

                client.sendPlain(GamePackets.clientCreatePractice("practice", "secret"));
                PacketReader room = new PacketReader(client.awaitPlain(5, TimeUnit.SECONDS));
                assertEquals(GamePackets.SERVER_ROOM_ENTER_RESULT, room.opcode());
                assertEquals(0, room.i16());
                assertTrue(room.u16() >= 1);
                assertEquals(GamePackets.TIPO_PRACTICE, room.u8());

                client.sendPlain(GamePackets.clientLeavePractice());
                List<byte[]> afterLeave = client.drainPlain(200);
                assertTrue(afterLeave.isEmpty(), "leave practice is a no-payload ack in S3");
            }

            assertTrue(awaitSessionCount(runtime, 0, 5, TimeUnit.SECONDS), "kill session must drop the player");

            try (PangyaFakeClient client = new PangyaFakeClient()) {
                String loginKey2 = repo.generateAuthKeyLogin(10001);
                String gameKey2 = repo.generateAuthKeyGame(10001, 20202);
                keys.putLoginKey(10001, loginKey2);
                keys.putGameKey(10001, 20202, gameKey2);
                client.connect("127.0.0.1", runtime.port(), PangyaFakeClient.HelloKind.GAME);
                client.awaitHello(5, TimeUnit.SECONDS);
                client.sendPlain(GamePackets.clientLogin(
                        "testuser",
                        10001,
                        loginKey2,
                        "852.00",
                        GamePackets.xorPacketVersion(2016110200),
                        gameKey2));
                List<byte[]> again = collect(client, 2, 5, TimeUnit.SECONDS);
                assertEquals(GamePackets.SERVER_LOGIN_ACK, new PacketReader(again.get(0)).opcode());
            }
        }
    }

    @Test
    void badGameKeySendsSecurityAck() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient client = new PangyaFakeClient()) {
            LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(ds));
            String loginKey = repo.generateAuthKeyLogin(10001);
            String gameKey = repo.generateAuthKeyGame(10001, 20202);
            keys.putLoginKey(10001, loginKey);
            keys.putGameKey(10001, 20202, gameKey);

            client.connect("127.0.0.1", runtime.port(), PangyaFakeClient.HelloKind.GAME);
            client.awaitHello(5, TimeUnit.SECONDS);
            client.sendPlain(GamePackets.clientLogin(
                    "testuser",
                    10001,
                    loginKey,
                    "852.00",
                    GamePackets.xorPacketVersion(2016110200),
                    "DEADBEEF"));
            PacketReader r = new PacketReader(client.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(GamePackets.SERVER_LOGIN_ACK, r.opcode());
            assertEquals(GamePackets.ACK_SECURITY_KEY, r.u32());
            assertFalse(runtime.sessions().snapshot().stream().anyMatch(s -> s.authorized()));
        }
    }

    private static boolean awaitSessionCount(GameRuntime runtime, int expected, long timeout, TimeUnit unit)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
        while (System.currentTimeMillis() < deadline) {
            if (runtime.sessions().size() == expected) {
                return true;
            }
            Thread.sleep(50);
        }
        return runtime.sessions().size() == expected;
    }

    private static List<byte[]> collect(PangyaFakeClient client, int n, long timeout, TimeUnit unit)
            throws InterruptedException {
        List<byte[]> out = new ArrayList<>();
        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
        while (out.size() < n && System.currentTimeMillis() < deadline) {
            long left = Math.max(1, deadline - System.currentTimeMillis());
            try {
                out.add(client.awaitPlain(left, TimeUnit.MILLISECONDS));
            } catch (IllegalStateException e) {
                break;
            }
        }
        return out;
    }

    private static Map<String, Object> testYaml(String jdbc, String user, String password, String redis) {
        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> server = new LinkedHashMap<>();
        server.put("name", "game-test");
        server.put("uid", 20202);
        server.put("tipo", 1);
        server.put("port", 0);
        server.put("healthPort", freePort());
        server.put("ip", "127.0.0.1");
        server.put("maxUser", 2001);
        server.put("property", 2048);
        server.put("version", "GS.Release.852.00");
        server.put("clientVersion", "852.00");
        server.put("packetVersion", 2016110200);
        root.put("server", server);
        root.put("database", Map.of("url", jdbc, "user", user, "password", password));
        root.put("redis", Map.of("uri", redis));
        root.put("auth", Map.of("enabled", false, "host", "127.0.0.1", "port", 1));
        root.put("channels", List.of(
                Map.of("name", "Channel (Rookies)", "maxUser", 500, "flag", 0),
                Map.of("name", "Channel (Geral)", "maxUser", 500, "flag", 0)
        ));
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
