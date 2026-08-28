package org.pangya.db;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Migrates Compose Postgres ({@code docker compose up -d postgres}).
 * Does not DROP the schema so other modules can test against the same database in parallel.
 * JDBC URL can be overridden with {@code PANGYA_TEST_JDBC_URL}.
 */
class FlywayMigrationTest {

    @Test
    void migratesAndIsIdempotent() throws Exception {
        String url = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");

        DatabaseSupport.migrate(url, user, password);
        int second = DatabaseSupport.migrate(url, user, password);
        assertEquals(0, second, "second migrate must be a no-op");

        try (var ds = DatabaseSupport.dataSource(url, user, password)) {
            var jdbi = DatabaseSupport.jdbi(ds);
            int tables = jdbi.withHandle(h ->
                    h.createQuery("select count(*) from information_schema.tables where table_schema = 'pangya'")
                            .mapTo(Integer.class)
                            .one());
            assertEquals(176, tables);

            int rankRows = jdbi.withHandle(h ->
                    h.createQuery("select count(*) from pangya.pangya_rank_config")
                            .mapTo(Integer.class)
                            .one());
            assertEquals(1, rankRows);

            int accounts = jdbi.withHandle(h ->
                    h.createQuery("select count(*) from pangya.account where \"ID\" = 'testuser'")
                            .mapTo(Integer.class)
                            .one());
            assertEquals(1, accounts);
            int accounts2 = jdbi.withHandle(h ->
                    h.createQuery("select count(*) from pangya.account where \"ID\" = 'testuser2'")
                            .mapTo(Integer.class)
                            .one());
            assertEquals(1, accounts2);
            int firstSet = jdbi.withHandle(h ->
                    h.createQuery("select count(*) from pangya.account where \"ID\" = 'newuser'")
                            .mapTo(Integer.class)
                            .one());
            assertEquals(1, firstSet);
            int shop = jdbi.withHandle(h ->
                    h.createQuery("select count(*) from pangya.shop_catalog where typeid = 436207622")
                            .mapTo(Integer.class)
                            .one());
            assertEquals(1, shop);
            int papel = jdbi.withHandle(h ->
                    h.createQuery("select count(*) from pangya.pangya_papel_shop_item where typeid = 436207622")
                            .mapTo(Integer.class)
                            .one());
            assertEquals(1, papel);
            assertTrue(accounts >= 1);
        }
    }

    private static String env(String name, String fallback) {
        String v = System.getenv(name);
        return (v == null || v.isBlank()) ? fallback : v;
    }
}
