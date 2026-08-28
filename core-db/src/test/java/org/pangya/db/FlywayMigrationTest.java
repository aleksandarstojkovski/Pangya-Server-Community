package org.pangya.db;

import org.junit.jupiter.api.Test;

import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Migrates the empty Compose Postgres ({@code docker compose up -d postgres}).
 * JDBC URL can be overridden with {@code PANGYA_TEST_JDBC_URL}.
 */
class FlywayMigrationTest {

    @Test
    void migratesEmptyDatabaseAndIsIdempotent() throws Exception {
        String url = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");

        try (var conn = DriverManager.getConnection(url, user, password);
             var st = conn.createStatement()) {
            st.execute("DROP SCHEMA IF EXISTS pangya CASCADE");
            st.execute("DROP TABLE IF EXISTS public.flyway_schema_history");
        }

        int first = DatabaseSupport.migrate(url, user, password);
        assertTrue(first >= 2, "expected V1 schema + V2 seed, executed=" + first);

        int second = DatabaseSupport.migrate(url, user, password);
        assertEquals(0, second, "second migrate must be a no-op");

        try (var ds = DatabaseSupport.dataSource(url, user, password)) {
            var jdbi = DatabaseSupport.jdbi(ds);
            int tables = jdbi.withHandle(h ->
                    h.createQuery("select count(*) from information_schema.tables where table_schema = 'pangya'")
                            .mapTo(Integer.class)
                            .one());
            assertEquals(175, tables);

            int rankRows = jdbi.withHandle(h ->
                    h.createQuery("select count(*) from pangya.pangya_rank_config")
                            .mapTo(Integer.class)
                            .one());
            assertEquals(1, rankRows);
        }
    }

    private static String env(String name, String fallback) {
        String v = System.getenv(name);
        return (v == null || v.isBlank()) ? fallback : v;
    }
}
