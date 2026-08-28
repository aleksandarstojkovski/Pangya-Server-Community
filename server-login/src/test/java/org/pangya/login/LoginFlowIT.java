package org.pangya.login;

import org.junit.jupiter.api.Test;
import org.pangya.db.DatabaseSupport;
import org.pangya.network.AppConfig;
import org.pangya.network.auth.AuthOutbound;
import org.pangya.network.client.PangyaFakeClient;
import org.pangya.network.redis.SessionKeyStore;
import org.pangya.protocol.auth.AuthS2s;
import org.pangya.protocol.game.GamePackets;
import org.pangya.protocol.login.LoginPackets;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginFlowIT {

    @Test
    void fakeClientLoginReceivesServerListAndCanSelectGs() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redis = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redis));
        try (LoginRuntime runtime = new LoginRuntime(config);
             PangyaFakeClient client = new PangyaFakeClient();
             SessionKeyStore keys = new SessionKeyStore(redis)) {
            client.connect("127.0.0.1", runtime.port());
            byte[] hello = client.awaitHello(5, TimeUnit.SECONDS);
            assertEquals(14, hello.length);
            assertEquals(PacketIo.loginHello(client.key())[6], hello[6]);

            client.sendPlain(LoginPackets.clientConnect("testuser", "testpass", "00:11:22:33:44:55"));
            List<byte[]> packets = collect(client, 5, 5, TimeUnit.SECONDS);
            assertEquals(5, packets.size(), "expected 0x10, 0x01, 0x02, 0x09, 0x06");

            PacketReader authKeyPkt = new PacketReader(packets.get(0));
            assertEquals(LoginPackets.SERVER_AUTH_KEY_LOGIN, authKeyPkt.opcode());
            String loginKey = authKeyPkt.pstr();
            assertEquals(8, loginKey.length());
            assertEquals(loginKey, keys.getLoginKey(10001));

            PacketReader login = new PacketReader(packets.get(1));
            assertEquals(LoginPackets.SERVER_LOGIN, login.opcode());
            assertEquals(0, login.u8());
            assertEquals("testuser", login.pstr());
            assertEquals(10001, login.u32());
            login.u32();
            assertEquals(1, login.u8());
            assertEquals(0, login.u32());
            assertEquals(1, login.u8());
            assertEquals(5, login.u32());
            login.readBytes(16);
            assertEquals(6, login.pstr().length());
            assertEquals(0, login.u64());
            assertEquals("TestNick", login.pstr());

            PacketReader gs = new PacketReader(packets.get(2));
            assertEquals(LoginPackets.SERVER_GS_LIST, gs.opcode());
            assertTrue(gs.u8() >= 1, "game server list must not be empty");

            PacketReader ms = new PacketReader(packets.get(3));
            assertEquals(LoginPackets.SERVER_MS_LIST, ms.opcode());

            PacketReader macros = new PacketReader(packets.get(4));
            assertEquals(LoginPackets.SERVER_MACRO_GAME_OPTION, macros.opcode());
            assertEquals(9 * 64, macros.remaining());

            client.sendPlain(LoginPackets.clientSelectGs(20202));
            byte[] keyGame = client.awaitPlain(5, TimeUnit.SECONDS);
            PacketReader ak = new PacketReader(keyGame);
            assertEquals(LoginPackets.SERVER_AUTH_KEY_GAME, ak.opcode());
            assertEquals(0, ak.u32());
            String gameKey = ak.pstr();
            assertEquals(8, gameKey.length());
            assertEquals(gameKey, keys.getGameKey(10001, 20202));
        }
    }

    @Test
    void authDisconnectDisconnectsAndConfirmsWhenPlayerOnLogin() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redis = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        List<long[]> confirms = new CopyOnWriteArrayList<>();
        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redis));
        try (LoginRuntime runtime = new LoginRuntime(config, new AuthOutbound() {
                    @Override
                    public void sendInfoPlayerOnline(int reqServerUid, AuthS2s.AuthServerPlayerInfo info) {}

                    @Override
                    public void sendConfirmDisconnectPlayer(long serverUid, long playerUid) {
                        confirms.add(new long[] {serverUid, playerUid});
                    }
                });
             PangyaFakeClient client = new PangyaFakeClient()) {
            client.connect("127.0.0.1", runtime.port());
            client.awaitHello(5, TimeUnit.SECONDS);
            client.sendPlain(LoginPackets.clientConnect("testuser", "testpass", "00:11:22:33:44:55"));
            collect(client, 5, 5, TimeUnit.SECONDS);
            assertTrue(client.connected());

            runtime.handler().onAuthPacket(
                    AuthS2s.AUTH_DISCONNECT_PLAYER,
                    new PacketReader(new PacketWriter().u32(10001).u32(10203).u8(1).toBytes()));

            long deadline = System.currentTimeMillis() + 2000;
            while (client.connected() && System.currentTimeMillis() < deadline) {
                Thread.sleep(20);
            }
            assertFalse(client.connected());
            assertEquals(1, confirms.size());
            assertEquals(10203L, confirms.get(0)[0]);
            assertEquals(10001L, confirms.get(0)[1]);
        }
    }

    @Test
    void authConfirmDisconnectResumesSuccessLogin() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redis = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password)) {
            DatabaseSupport.jdbi(ds).useHandle(h -> h.execute(
                    "UPDATE pangya.account SET \"Logon\" = 1 WHERE \"UID\" = 10001"));
        }

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redis));
        try (LoginRuntime runtime = new LoginRuntime(config);
             PangyaFakeClient client = new PangyaFakeClient()) {
            client.connect("127.0.0.1", runtime.port());
            client.awaitHello(5, TimeUnit.SECONDS);
            client.sendPlain(LoginPackets.clientConnect("testuser", "testpass", "00:11:22:33:44:55"));
            PacketReader waiting = new PacketReader(client.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(LoginPackets.SERVER_LOGIN, waiting.opcode());
            assertEquals(LoginPackets.OPT_ALREADY_ON_GS, waiting.u8());

            runtime.handler().onAuthPacket(
                    AuthS2s.AUTH_CONFIRM_DISCONNECT,
                    new PacketReader(new PacketWriter().u32(10001).toBytes()));

            List<byte[]> packets = collect(client, 5, 5, TimeUnit.SECONDS);
            assertEquals(5, packets.size());
            assertEquals(LoginPackets.SERVER_AUTH_KEY_LOGIN, new PacketReader(packets.get(0)).opcode());
            PacketReader loginOk = new PacketReader(packets.get(1));
            assertEquals(LoginPackets.SERVER_LOGIN, loginOk.opcode());
            assertEquals(LoginPackets.OPT_OK, loginOk.u8());
        } finally {
            try (var ds = DatabaseSupport.dataSource(jdbc, user, password)) {
                DatabaseSupport.jdbi(ds).useHandle(h -> h.execute(
                        "UPDATE pangya.account SET \"Logon\" = 0 WHERE \"UID\" = 10001"));
            }
        }
    }

    @Test
    void authShutdownForwardsSecondsToScheduler() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redis = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        java.util.concurrent.atomic.AtomicInteger requested = new java.util.concurrent.atomic.AtomicInteger(-1);
        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redis));
        try (LoginRuntime runtime = new LoginRuntime(config, null, requested::set)) {
            runtime.handler().onAuthPacket(
                    AuthS2s.AUTH_SHUTDOWN,
                    new PacketReader(new PacketWriter().i32(120).toBytes()));
            assertEquals(120, requested.get());
        }
    }

    @Test
    void badPasswordSendsOption6() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redis = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redis));
        try (LoginRuntime runtime = new LoginRuntime(config);
             PangyaFakeClient client = new PangyaFakeClient()) {
            client.connect("127.0.0.1", runtime.port());
            assertNotNull(client.awaitHello(5, TimeUnit.SECONDS));
            client.sendPlain(LoginPackets.clientConnect("testuser", "wrongpass", "00:11:22:33:44:55"));
            byte[] pkt = client.awaitPlain(5, TimeUnit.SECONDS);
            PacketReader r = new PacketReader(pkt);
            assertEquals(LoginPackets.SERVER_LOGIN, r.opcode());
            assertEquals(LoginPackets.OPT_BAD_ID_OR_PASS, r.u8());
        }
    }

    @Test
    void firstLoginCompletesNickAndCharacter() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redis = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password)) {
            DatabaseSupport.jdbi(ds).useHandle(h -> {
                h.execute("DELETE FROM pangya.pangya_item_warehouse WHERE \"UID\" = 10003");
                h.execute("DELETE FROM pangya.pangya_character_information WHERE \"UID\" = 10003");
                h.execute("DELETE FROM pangya.pangya_user_equip WHERE \"UID\" = 10003");
                h.execute("UPDATE pangya.user_info SET \"Pang\" = 0, \"Cookie\" = 0 WHERE \"UID\" = 10003");
                h.execute("""
                        UPDATE pangya.account
                           SET "FIRST_LOGIN" = 0, "FIRST_SET" = 0, "NICK" = '', "Event" = 0, "IDState" = 0
                         WHERE "UID" = 10003
                        """);
            });
        }

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redis));
        try (LoginRuntime runtime = new LoginRuntime(config);
             PangyaFakeClient client = new PangyaFakeClient()) {
            client.connect("127.0.0.1", runtime.port());
            assertNotNull(client.awaitHello(5, TimeUnit.SECONDS));
            client.sendPlain(LoginPackets.clientConnect("newuser", "testpass", "00:11:22:33:44:55"));
            List<byte[]> first = collect(client, 2, 5, TimeUnit.SECONDS);
            assertEquals(2, first.size());
            assertEquals(LoginPackets.SERVER_TUTORIAL, new PacketReader(first.get(0)).opcode());
            PacketReader firstLogin = new PacketReader(first.get(1));
            assertEquals(LoginPackets.SERVER_LOGIN, firstLogin.opcode());
            assertEquals(LoginPackets.OPT_FIRST_LOGIN, firstLogin.u8());

            client.sendPlain(LoginPackets.clientConfirmNick("newuser"));
            PacketReader sameId = new PacketReader(client.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(LoginPackets.SERVER_CHECK_NICK, sameId.opcode());
            assertEquals(LoginPackets.NICK_SAME_AS_ID, sameId.i32());

            client.sendPlain(LoginPackets.clientConfirmNick("TestNick"));
            PacketReader inUse = new PacketReader(client.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(LoginPackets.SERVER_CHECK_NICK, inUse.opcode());
            assertEquals(LoginPackets.NICK_IN_USE, inUse.i32());

            client.sendPlain(LoginPackets.clientConfirmNick("FreshNick"));
            PacketReader ok = new PacketReader(client.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(LoginPackets.SERVER_CHECK_NICK, ok.opcode());
            assertEquals(LoginPackets.NICK_OK, ok.i32());
            assertEquals("FreshNick", ok.pstr());

            client.sendPlain(LoginPackets.clientSetNick("FreshNick"));
            PacketReader firstSet = new PacketReader(client.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(LoginPackets.SERVER_LOGIN, firstSet.opcode());
            assertEquals(LoginPackets.OPT_FIRST_SET, firstSet.u8());

            client.sendPlain(LoginPackets.clientSetCharacter(GamePackets.TYPEID_NURI, 3, 1));
            PacketReader badSave = new PacketReader(client.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(LoginPackets.SERVER_CHARACTER_SAVE, badSave.opcode());
            PacketReader badCode = new PacketReader(client.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(LoginPackets.SERVER_CHECK_NICK, badCode.opcode());
            assertEquals(LoginPackets.NICK_CODE_ERROR, badCode.i32());
            assertEquals(LoginPackets.FIRST_SET_CHAR_ERROR, badCode.u32());

            client.sendPlain(LoginPackets.clientSetCharacter(GamePackets.TYPEID_NURI, 3, 0));
            PacketReader saved = new PacketReader(client.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(LoginPackets.SERVER_CHARACTER_SAVE, saved.opcode());
            assertEquals(0, saved.u16());
            List<byte[]> success = collect(client, 5, 5, TimeUnit.SECONDS);
            assertEquals(5, success.size(), "expected 0x10, 0x01, 0x02, 0x09, 0x06");
            PacketReader login = new PacketReader(success.get(1));
            assertEquals(LoginPackets.SERVER_LOGIN, login.opcode());
            assertEquals(0, login.u8());
            assertEquals("newuser", login.pstr());
            assertEquals(10003, login.u32());
            login.u32();
            login.u8();
            login.u32();
            login.u8();
            login.u32();
            login.readBytes(16);
            login.pstr();
            login.u64();
            assertEquals("FreshNick", login.pstr());
        }
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
        server.put("name", "login-test");
        server.put("uid", 10203);
        server.put("tipo", 0);
        server.put("port", 0);
        server.put("healthPort", freePort());
        server.put("ip", "127.0.0.1");
        server.put("maxUser", 2001);
        server.put("property", 2048);
        server.put("version", "LS.Release.2.0");
        server.put("clientVersion", "JP.R7.983.00");
        server.put("packetVersion", 2017110200);
        root.put("server", server);
        root.put("database", Map.of("url", jdbc, "user", user, "password", password));
        root.put("redis", Map.of("uri", redis));
        root.put("auth", Map.of("enabled", false, "host", "127.0.0.1", "port", 1));
        root.put("game", Map.of(
                "name", "PAPEL",
                "uid", 20202,
                "ip", "127.0.0.1",
                "port", 20202,
                "maxUser", 2001,
                "property", 2048
        ));
        root.put("messenger", Map.of(
                "name", "Messenger Server",
                "uid", 30201,
                "ip", "127.0.0.1",
                "port", 30201,
                "maxUser", 2001,
                "property", 4096
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
