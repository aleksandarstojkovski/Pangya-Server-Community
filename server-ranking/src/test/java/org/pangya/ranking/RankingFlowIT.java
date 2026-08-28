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
