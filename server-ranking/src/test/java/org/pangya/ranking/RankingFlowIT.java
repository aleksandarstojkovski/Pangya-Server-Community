package org.pangya.ranking;

import org.junit.jupiter.api.Test;
import org.pangya.db.DatabaseSupport;
import org.pangya.network.AppConfig;
import org.pangya.network.client.PangyaFakeClient;
import org.pangya.protocol.packet.PacketIo;
import org.pangya.protocol.packet.PacketReader;
import org.pangya.protocol.ranking.RankingPackets;

import java.net.ServerSocket;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RankingFlowIT {

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

        try (var ds = DatabaseSupport.dataSource(jdbc, user, password)) {
            var jdbi = DatabaseSupport.jdbi(ds);
            jdbi.useHandle(h -> h.execute(
                    "DELETE FROM pangya.pangya_rank_atual WHERE tipo_rank = 7 AND tipo_rank_seq = 3"));
            jdbi.useHandle(h -> h.execute("""
                    INSERT INTO pangya.pangya_rank_atual (position, "UID", tipo_rank, tipo_rank_seq, valor)
                    VALUES (1, 10001, 7, 3, 42)
                    """));
        }

        try (RankingRuntime runtime = new RankingRuntime(new AppConfig(testYaml(jdbc, user, password)));
             PangyaFakeClient client = new PangyaFakeClient()) {
            client.connect("127.0.0.1", runtime.port(), PangyaFakeClient.HelloKind.RANKING);
            client.awaitHello(5, TimeUnit.SECONDS);
            client.sendPlain(RankingPackets.clientLogin(10001, "testuser", 7, 3, 0, 0, 0));
            PacketReader page = new PacketReader(client.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(RankingPackets.SERVER_SEND_FIRST_PAGE, page.opcode());
            assertEquals(0, page.u8());
            assertEquals(7, page.u8());
            assertEquals(3, page.u8());
            page.u8();
            page.u8();
            assertEquals(1, page.u32());
            assertEquals(1, page.u32());
            assertEquals(1, page.u16());
            assertEquals(10001, page.u32());
            assertEquals(1, page.u32());
            assertEquals(0, page.u32());
            assertEquals(42, page.i32());
            assertTrue(page.u8() > 0); // level from SQL
            page.u8();
            page.u8();
            assertEquals("testuser", page.pstr());
            page.pstr(); // nickname

            client.sendPlain(RankingPackets.clientSearchByNickname(
                    "TestNick", new RankingPackets.SearchDados(7, 3, 0, 0, 0)));
            PacketReader found = new PacketReader(client.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(RankingPackets.SERVER_PAGE_NOT_FOUND, found.opcode());
            assertEquals(0, found.u8());
            assertEquals(7, found.u8());
            assertEquals(3, found.u8());
            found.u8();
            found.u8();
            assertEquals(1, found.u32());
            assertEquals(1, found.u32());
            assertEquals(1, found.u16());
            assertEquals(10001, found.u32());
            assertEquals(1, found.u32());
            found.u32();
            found.i32();
            found.u8();
            found.u8();
            found.u8();
            found.pstr();
            found.pstr();
            assertEquals(0, found.u16());

            client.sendPlain(RankingPackets.clientSearchByNickname(
                    "Nobody", new RankingPackets.SearchDados(7, 3, 0, 0, 0)));
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

        try (var ds = DatabaseSupport.dataSource(jdbc, user, password)) {
            DatabaseSupport.jdbi(ds).useHandle(h -> h.execute(
                    "DELETE FROM pangya.pangya_rank_atual WHERE tipo_rank = 7 AND tipo_rank_seq = 3"));
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
