package org.pangya.auth;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.pangya.db.DatabaseSupport;
import org.pangya.db.JdbiLoginRepository;
import org.pangya.network.AppConfig;
import org.pangya.network.auth.AuthServerConnector;

import java.net.ServerSocket;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthS2sIT {

    @Test
    void loginChildRegistersAndReceivesOid() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        DatabaseSupport.migrate(jdbc, user, password);

        Map<String, Object> authYaml = new LinkedHashMap<>();
        authYaml.put("server", Map.of(
                "name", "auth-test",
                "port", 0,
                "healthPort", freePort(),
                "uid", 8888
        ));
        authYaml.put("database", Map.of(
                "url", jdbc, "user", user, "password", password, "migrateOnStart", false));
        authYaml.put("auth", Map.of("guid", 8888));

        try (AuthRuntime auth = new AuthRuntime(new AppConfig(authYaml));
             HikariDataSource ds = DatabaseSupport.dataSource(jdbc, user, password)) {
            Map<String, Object> childYaml = new LinkedHashMap<>();
            childYaml.put("server", Map.of(
                    "name", "login-test",
                    "uid", 10203,
                    "tipo", 0,
                    "clientVersion", "JP.R7.983.00",
                    "packetVersion", 2017110200
            ));
            childYaml.put("auth", Map.of("host", "127.0.0.1", "port", auth.port()));
            var repo = new JdbiLoginRepository(DatabaseSupport.jdbi(ds));
            try (AuthServerConnector connector = new AuthServerConnector(
                    new AppConfig(childYaml), repo::generateAuthServerKey)) {
                connector.start();
                assertTrue(connector.awaitRegistered(10, TimeUnit.SECONDS), "child should get oid from auth");
                assertTrue(connector.oid() > 0);
            }
        }
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
