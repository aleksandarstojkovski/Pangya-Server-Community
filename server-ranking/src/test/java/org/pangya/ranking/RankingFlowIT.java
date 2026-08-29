package org.pangya.ranking;

import org.junit.jupiter.api.Test;
import org.pangya.db.DatabaseSupport;
import org.pangya.db.JdbiRankRepository;
import org.pangya.db.RankRepository;
import org.pangya.network.AppConfig;
import org.pangya.network.auth.AuthOutbound;
import org.pangya.network.client.PangyaFakeClient;
import org.pangya.protocol.auth.AuthS2s;
import org.pangya.protocol.packet.PacketIo;
import org.pangya.protocol.packet.PacketReader;
import org.pangya.protocol.packet.PacketWriter;
import org.pangya.protocol.ranking.RankingPackets;

import java.net.ServerSocket;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RankingFlowIT {

    @Test
    void geraRankAllFirstPageIsServedToFakeClient() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        DatabaseSupport.migrate(jdbc, user, password);

        try (var ds = DatabaseSupport.dataSource(jdbc, user, password)) {
            RankRepository ranks = new JdbiRankRepository(DatabaseSupport.jdbi(ds));
            assertTrue(ranks.geraRankAll() > 0);
        }

        try (RankingRuntime runtime = new RankingRuntime(new AppConfig(testYaml(jdbc, user, password)));
             PangyaFakeClient client = new PangyaFakeClient()) {
            client.connect("127.0.0.1", runtime.port(), PangyaFakeClient.HelloKind.RANKING);
            client.awaitHello(5, TimeUnit.SECONDS);
            client.sendPlain(RankingPackets.clientLogin(10001, "testuser", 2, 3, 0, 0, 0));
            PacketReader page = new PacketReader(client.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(RankingPackets.SERVER_SEND_FIRST_PAGE, page.opcode());
            assertEquals(0, page.u8());
            assertEquals(2, page.u8());
            assertEquals(3, page.u8());
            page.u8();
            page.u8();
            assertTrue(page.u32() >= 1);
            page.u32();
            int n = page.u16();
            assertTrue(n >= 1);
            boolean found = false;
            for (int i = 0; i < n; i++) {
                long uid = page.u32();
                page.u32();
                page.u32();
                page.i32();
                page.u8();
                page.u8();
                page.u8();
                page.pstr();
                page.pstr();
                if (uid == 10001) {
                    found = true;
                }
            }
            assertTrue(found, "level board must include testuser after GeraRankAll");
        }
    }

    @Test
    void fakeClientLoginReceivesEmptyFirstPage() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        DatabaseSupport.migrate(jdbc, user, password);

        try (RankingRuntime runtime = new RankingRuntime(new AppConfig(testYaml(jdbc, user, password)));
             PangyaFakeClient client = new PangyaFakeClient()) {
            client.connect("127.0.0.1", runtime.port(), PangyaFakeClient.HelloKind.RANKING);
            byte[] hello = client.awaitHello(5, TimeUnit.SECONDS);
            PacketReader helloBody = new PacketReader(PacketIo.slice(hello, 4, hello.length - 4));
            assertEquals(RankingPackets.SERVER_CONNECT_LOGIN, helloBody.opcode());
            assertEquals(client.key(), helloBody.u32());
            assertEquals(RankingPackets.RANK_SERVER_TYPE, helloBody.u8());

            client.sendPlain(RankingPackets.clientLogin(10001, "testuser", 0, 0, 0, 0, 0));
            PacketReader page = new PacketReader(client.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(RankingPackets.SERVER_SEND_FIRST_PAGE, page.opcode());
            assertEquals(0, page.u8());
        }
    }

    @Test
    void registryPageAndPlayerFullInfoComeFromSql() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        DatabaseSupport.migrate(jdbc, user, password);

        try (RankingRuntime runtime = new RankingRuntime(new AppConfig(testYaml(jdbc, user, password)));
             PangyaFakeClient client = new PangyaFakeClient()) {
            client.connect("127.0.0.1", runtime.port(), PangyaFakeClient.HelloKind.RANKING);
            client.awaitHello(5, TimeUnit.SECONDS);
            // C# RankingServer.init_systems → GeraRankAll; tipo 2 seq 3 is level.
            client.sendPlain(RankingPackets.clientLogin(10001, "testuser", 2, 3, 0, 0, 0));
            PacketReader page = new PacketReader(client.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(RankingPackets.SERVER_SEND_FIRST_PAGE, page.opcode());
            assertEquals(0, page.u8());
            assertEquals(2, page.u8());
            assertEquals(3, page.u8());
            page.u8();
            page.u8();
            assertTrue(page.u32() >= 1);
            page.u32();
            int n = page.u16();
            assertTrue(n >= 1);
            boolean found = false;
            int levelValor = 0;
            for (int i = 0; i < n; i++) {
                long uid = page.u32();
                page.u32();
                page.u32();
                int valor = page.i32();
                assertTrue(page.u8() > 0);
                page.u8();
                page.u8();
                String id = page.pstr();
                page.pstr();
                if (uid == 10001) {
                    found = true;
                    levelValor = valor;
                    assertEquals("testuser", id);
                }
            }
            assertTrue(found, "level board must include testuser after RankingRuntime GeraRankAll");
            assertTrue(levelValor >= 1, "seeded testuser level is at least 1");

            client.sendPlain(RankingPackets.clientSearchByNickname(
                    "TestNick", new RankingPackets.SearchDados(2, 3, 0, 0, 0)));
            PacketReader search = new PacketReader(client.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(RankingPackets.SERVER_PAGE_NOT_FOUND, search.opcode());
            assertEquals(0, search.u8());
            assertEquals(2, search.u8());
            assertEquals(3, search.u8());
            search.u8();
            search.u8();
            search.u32();
            search.u32();
            int searchN = search.u16();
            assertTrue(searchN >= 1);
            boolean searchFound = false;
            for (int i = 0; i < searchN; i++) {
                long uid = search.u32();
                search.u32();
                search.u32();
                search.i32();
                search.u8();
                search.u8();
                search.u8();
                search.pstr();
                search.pstr();
                if (uid == 10001) {
                    searchFound = true;
                }
            }
            search.u16();
            assertTrue(searchFound, "nickname search must find TestNick on the level board");

            client.sendPlain(RankingPackets.clientSearchByNickname(
                    "Nobody", new RankingPackets.SearchDados(2, 3, 0, 0, 0)));
            PacketReader miss = new PacketReader(client.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(RankingPackets.SERVER_PAGE_NOT_FOUND, miss.opcode());
            assertEquals(1, miss.u8());

            client.sendPlain(new org.pangya.protocol.packet.PacketWriter()
                    .opcode(RankingPackets.CLIENT_REQUEST_PLAYER_INFO)
                    .u32(10001)
                    .pstr("testuser")
                    .u8(1)
                    .toBytes());
            PacketReader full = new PacketReader(client.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(RankingPackets.SERVER_SEND_PLAYER_FULL_INFO, full.opcode());
            assertEquals(0, full.u8());
            assertEquals(10001, full.u32());
        }
    }

    @Test
    void authInfoPlayerOnlineReportsOnlineAndOffline() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        DatabaseSupport.migrate(jdbc, user, password);

        List<AuthS2s.AuthServerPlayerInfo> sent = new CopyOnWriteArrayList<>();
        AppConfig config = new AppConfig(testYaml(jdbc, user, password));
        try (RankingRuntime runtime = new RankingRuntime(config, new AuthOutbound() {
                    @Override
                    public void sendInfoPlayerOnline(int reqServerUid, AuthS2s.AuthServerPlayerInfo info) {
                        sent.add(info);
                    }
                });
             PangyaFakeClient client = new PangyaFakeClient()) {
            runtime.authHandler().onAuthPacket(
                    AuthS2s.AUTH_INFO_PLAYER_ONLINE,
                    new PacketReader(new PacketWriter().u32(30201).u32(99999).toBytes()));
            assertEquals(1, sent.size());
            assertEquals(-1, sent.get(0).option());
            sent.clear();

            client.connect("127.0.0.1", runtime.port(), PangyaFakeClient.HelloKind.RANKING);
            client.awaitHello(5, TimeUnit.SECONDS);
            client.sendPlain(RankingPackets.clientLogin(10001, "testuser", 0, 0, 0, 0, 0));
            PacketReader page = new PacketReader(client.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(RankingPackets.SERVER_SEND_FIRST_PAGE, page.opcode());
            assertEquals(0, page.u8());

            runtime.authHandler().onAuthPacket(
                    AuthS2s.AUTH_INFO_PLAYER_ONLINE,
                    new PacketReader(new PacketWriter().u32(30201).u32(10001).toBytes()));
            assertEquals(1, sent.size());
            assertEquals(1, sent.get(0).option());
            assertEquals("testuser", sent.get(0).id());
        }
    }

    @Test
    void authDisconnectDisconnectsLoggedInPlayer() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        DatabaseSupport.migrate(jdbc, user, password);

        List<long[]> confirms = new CopyOnWriteArrayList<>();
        AppConfig config = new AppConfig(testYaml(jdbc, user, password));
        try (RankingRuntime runtime = new RankingRuntime(config, new AuthOutbound() {
                    @Override
                    public void sendInfoPlayerOnline(int reqServerUid, AuthS2s.AuthServerPlayerInfo info) {}

                    @Override
                    public void sendConfirmDisconnectPlayer(long serverUid, long playerUid) {
                        confirms.add(new long[] {serverUid, playerUid});
                    }
                });
             PangyaFakeClient client = new PangyaFakeClient()) {
            client.connect("127.0.0.1", runtime.port(), PangyaFakeClient.HelloKind.RANKING);
            client.awaitHello(5, TimeUnit.SECONDS);
            client.sendPlain(RankingPackets.clientLogin(10001, "testuser", 0, 0, 0, 0, 0));
            PacketReader page = new PacketReader(client.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(RankingPackets.SERVER_SEND_FIRST_PAGE, page.opcode());
            assertTrue(client.connected());

            runtime.authHandler().onAuthPacket(
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
        try (RankingRuntime runtime = new RankingRuntime(config, null, requested::set)) {
            runtime.authHandler().onAuthPacket(
                    AuthS2s.AUTH_SHUTDOWN,
                    new PacketReader(new PacketWriter().i32(60).toBytes()));
            assertEquals(60, requested.get());
        }
    }

    @Test
    void unknownIdSendsErrorPage() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        DatabaseSupport.migrate(jdbc, user, password);

        try (RankingRuntime runtime = new RankingRuntime(new AppConfig(testYaml(jdbc, user, password)));
             PangyaFakeClient client = new PangyaFakeClient()) {
            client.connect("127.0.0.1", runtime.port(), PangyaFakeClient.HelloKind.RANKING);
            client.awaitHello(5, TimeUnit.SECONDS);
            client.sendPlain(RankingPackets.clientLogin(1, "nobody", 0, 0, 0, 0, 0));
            PacketReader page = new PacketReader(client.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(RankingPackets.SERVER_SEND_FIRST_PAGE, page.opcode());
            assertEquals(1, page.u8());
        }
    }

    private static Map<String, Object> testYaml(String jdbc, String user, String password) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("server", Map.of(
                "name", "ranking-test",
                "uid", 4774,
                "tipo", 4,
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
