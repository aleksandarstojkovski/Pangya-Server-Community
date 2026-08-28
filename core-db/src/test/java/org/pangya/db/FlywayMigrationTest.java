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
            assertEquals(186, tables);

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
            int cadie = jdbi.withHandle(h ->
                    h.createQuery("select count(*) from pangya.cadie_magic_box where seq = 1")
                            .mapTo(Integer.class)
                            .one());
            assertEquals(1, cadie);
            int caddieIff = jdbi.withHandle(h ->
                    h.createQuery("select count(*) from pangya.iff_caddie where typeid = 469762048")
                            .mapTo(Integer.class)
                            .one());
            assertEquals(1, caddieIff);
            int masteryIff = jdbi.withHandle(h ->
                    h.createQuery("select count(*) from pangya.iff_character_mastery where typeid = 67108864 and seq = 1")
                            .mapTo(Integer.class)
                            .one());
            assertEquals(1, masteryIff);
            int charIff = jdbi.withHandle(h ->
                    h.createQuery("select count(*) from pangya.iff_character where typeid = 67108864")
                            .mapTo(Integer.class)
                            .one());
            assertEquals(1, charIff);
            int enchantIff = jdbi.withHandle(h ->
                    h.createQuery("select count(*) from pangya.iff_enchant where typeid = 872415232")
                            .mapTo(Integer.class)
                            .one());
            assertEquals(1, enchantIff);
            int cardIff = jdbi.withHandle(h ->
                    h.createQuery("select efeito from pangya.iff_card where typeid = 2080374785")
                            .mapTo(Integer.class)
                            .one());
            assertEquals(0, cardIff);
            int comet = jdbi.withHandle(h ->
                    h.createQuery("select count(*) from pangya.pangya_comet_refill where typeid = 436207877")
                            .mapTo(Integer.class)
                            .one());
            assertEquals(1, comet);
            int tli = jdbi.withHandle(h ->
                    h.createQuery("select count(*) from pangya.iff_time_limit_item")
                            .mapTo(Integer.class)
                            .one());
            assertEquals(0, tli);
            int clubset = jdbi.withHandle(h ->
                    h.createQuery("select count(*) from pangya.iff_clubset")
                            .mapTo(Integer.class)
                            .one());
            assertEquals(0, clubset);
            int clubsetSlots = jdbi.withHandle(h ->
                    h.createQuery("select slot0 from pangya.iff_clubset where typeid = 0")
                            .mapTo(Integer.class)
                            .findOne()
                            .orElse(0));
            assertEquals(0, clubsetSlots);
            int partIff = jdbi.withHandle(h ->
                    h.createQuery("select count(*) from pangya.iff_part")
                            .mapTo(Integer.class)
                            .one());
            assertEquals(0, partIff);
            assertTrue(accounts >= 1);
        }
    }

    private static String env(String name, String fallback) {
        String v = System.getenv(name);
        return (v == null || v.isBlank()) ? fallback : v;
    }
}
