package org.pangya.db;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Migrates a freshly-created, dedicated Postgres database on the Compose server
 * ({@code docker compose up -d postgres}).
 *
 * <p>The idempotency assertion (second {@code migrate()} must apply 0 migrations) requires
 * that no other actor migrates the same database between the two calls. The shared
 * {@code pangya} database does not satisfy that: other test modules run in parallel
 * ({@code org.gradle.parallel=true}) and the {@code auth}/{@code game} containers migrate it
 * on start ({@code PANGYA_MIGRATE_ON_START}). This test therefore creates its own database
 * ({@link #DEDICATED_DB}) and runs entirely against it, so the check is deterministic.
 *
 * <p>The server/base connection is taken from {@code PANGYA_TEST_JDBC_URL} (default
 * {@code jdbc:postgresql://localhost:5432/pangya}); the dedicated database is created on the
 * same server by swapping the database name in that URL.
 */
class FlywayMigrationTest {

    private static final String DEDICATED_DB = "pangya_flyway_it";

    @Test
    void migratesAndIsIdempotent() throws Exception {
        String baseUrl = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");

        // Recreate a dedicated, isolated database so the idempotency check cannot be
        // perturbed by concurrent migrate() from other modules/containers on the shared DB.
        recreateDedicatedDatabase(baseUrl, user, password);
        String url = swapDatabase(baseUrl, DEDICATED_DB);

        DatabaseSupport.migrate(url, user, password);
        int second = DatabaseSupport.migrate(url, user, password);
        assertEquals(0, second, "second migrate must be a no-op");

        try (var ds = DatabaseSupport.dataSource(url, user, password)) {
            var jdbi = DatabaseSupport.jdbi(ds);
            int tables = jdbi.withHandle(h ->
                    h.createQuery("select count(*) from information_schema.tables where table_schema = 'pangya'")
                            .mapTo(Integer.class)
                            .one());
            assertEquals(202, tables);

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
            assertEquals(24, partIff);
            int rankExp = jdbi.withHandle(h ->
                    h.createQuery("select count(*) from pangya.iff_clubset_rank_exp")
                            .mapTo(Integer.class)
                            .one());
            assertEquals(0, rankExp);
            int itemIff = jdbi.withHandle(h ->
                    h.createQuery("select count(*) from pangya.iff_item")
                            .mapTo(Integer.class)
                            .one());
            assertEquals(0, itemIff);
            int levelLimit = jdbi.withHandle(h ->
                    h.createQuery("select count(*) from pangya.iff_clubset_level_up_limit")
                            .mapTo(Integer.class)
                            .one());
            assertEquals(0, levelLimit);
            int levelProb = jdbi.withHandle(h ->
                    h.createQuery("select count(*) from pangya.iff_clubset_level_up_prob")
                            .mapTo(Integer.class)
                            .one());
            assertEquals(0, levelProb);
            int rankExpCols = jdbi.withHandle(h ->
                    h.createQuery("""
                            select count(*) from information_schema.columns
                             where table_schema = 'pangya'
                               and table_name = 'iff_clubset_rank_exp'
                               and column_name in ('rank0','rank1','rank2','rank3','rank4','rank5')
                            """)
                            .mapTo(Integer.class)
                            .one());
            assertEquals(6, rankExpCols);
            int originalIff = jdbi.withHandle(h ->
                    h.createQuery("select count(*) from pangya.iff_clubset_original")
                            .mapTo(Integer.class)
                            .one());
            assertEquals(0, originalIff);
            int flagCol = jdbi.withHandle(h ->
                    h.createQuery("""
                            select count(*) from information_schema.columns
                             where table_schema = 'pangya'
                               and table_name = 'iff_clubset'
                               and column_name = 'flag_transformar'
                            """)
                            .mapTo(Integer.class)
                            .one());
            assertEquals(1, flagCol);
            int cutinIff = jdbi.withHandle(h ->
                    h.createQuery("select count(*) from pangya.iff_cutin_information")
                            .mapTo(Integer.class)
                            .one());
            assertEquals(0, cutinIff);
            int boxMail = jdbi.withHandle(h ->
                    h.createQuery("select count(*) from pangya.box_mail_catalog")
                            .mapTo(Integer.class)
                            .one());
            assertEquals(0, boxMail);
            int effectTimeCol = jdbi.withHandle(h ->
                    h.createQuery("""
                            select count(*) from information_schema.columns
                             where table_schema = 'pangya'
                               and table_name = 'iff_card'
                               and column_name = 'efeito_tempo'
                            """)
                            .mapTo(Integer.class)
                            .one());
            assertEquals(1, effectTimeCol);
            int cardPack = jdbi.withHandle(h ->
                    h.createQuery("select count(*) from pangya.card_pack_catalog")
                            .mapTo(Integer.class)
                            .one());
            assertEquals(0, cardPack);
            int memorial = jdbi.withHandle(h ->
                    h.createQuery("select count(*) from pangya.memorial_reward_catalog")
                            .mapTo(Integer.class)
                            .one());
            assertEquals(0, memorial);
            int ticketReports = jdbi.withHandle(h ->
                    h.createQuery("select count(*) from pangya.ticket_report_catalog")
                            .mapTo(Integer.class)
                            .one());
            assertEquals(0, ticketReports);
            int gpEvents = jdbi.withHandle(h ->
                    h.createQuery("select count(*) from pangya.grand_prix_event")
                            .mapTo(Integer.class)
                            .one());
            assertEquals(0, gpEvents);
            int tikiValues = jdbi.withHandle(h ->
                    h.createQuery("select count(*) from pangya.legacy_tiki_item_value")
                            .mapTo(Integer.class)
                            .one());
            assertEquals(0, tikiValues);
            int tikiNewCols = jdbi.withHandle(h ->
                    h.createQuery("""
                            select count(*) from information_schema.columns
                             where table_schema = 'pangya'
                               and table_name = 'legacy_tiki_item_value'
                               and column_name in (
                                   'tiki_pang','mileage','bonus_min','bonus_max','bonus_prob')
                            """)
                            .mapTo(Integer.class)
                            .one());
            assertEquals(5, tikiNewCols);
            int dailyIff = jdbi.withHandle(h ->
                    h.createQuery("""
                            select count(*) from information_schema.tables
                             where table_schema = 'pangya'
                               and table_name in (
                                   'iff_daily_quest_stuff','iff_daily_quest_reward')
                            """)
                            .mapTo(Integer.class)
                            .one());
            assertEquals(2, dailyIff);
            int uccCols = jdbi.withHandle(h ->
                    h.createQuery("""
                            select count(*) from information_schema.columns
                             where table_schema = 'pangya'
                               and table_name = 'pangya_item_warehouse'
                               and column_name in (
                                   'ucc_name','ucc_trade','ucc_idx','ucc_status',
                                   'ucc_seq','ucc_copier_nick','ucc_copier')
                            """)
                            .mapTo(Integer.class)
                            .one());
            assertEquals(7, uccCols);
            int courseParRows = jdbi.withHandle(h ->
                    h.createQuery("select count(*) from pangya.iff_course_hole where course_id = 0")
                            .mapTo(Integer.class)
                            .one());
            assertTrue(courseParRows >= 17, "Blue Lagoon par rows seeded");
            int courseMapRows = jdbi.withHandle(h ->
                    h.createQuery("select count(*) from pangya.iff_course")
                            .mapTo(Integer.class)
                            .one());
            assertTrue(courseMapRows >= 20, "iff_course map rows seeded");
            assertTrue(accounts >= 1);
        }
    }

    private static String env(String name, String fallback) {
        String v = System.getenv(name);
        return (v == null || v.isBlank()) ? fallback : v;
    }

    /**
     * Returns {@code baseUrl} with its database name replaced by {@code database},
     * preserving any query string. Example:
     * {@code jdbc:postgresql://h:5432/pangya?ssl=false} → {@code .../pangya_flyway_it?ssl=false}.
     */
    static String swapDatabase(String baseUrl, String database) {
        int q = baseUrl.indexOf('?');
        String noQuery = q >= 0 ? baseUrl.substring(0, q) : baseUrl;
        String query = q >= 0 ? baseUrl.substring(q) : "";
        int slash = noQuery.lastIndexOf('/');
        return noQuery.substring(0, slash + 1) + database + query;
    }

    /**
     * Drops (with FORCE, Postgres 13+) and recreates {@link #DEDICATED_DB} on the server that
     * {@code baseUrl} points at, connecting through {@code baseUrl}'s own database. Retries the
     * same transient connect failures {@link DatabaseSupport#migrate} does (nested-Docker
     * published-port path).
     */
    private static void recreateDedicatedDatabase(String baseUrl, String user, String password) throws Exception {
        Exception last = null;
        for (int attempt = 1; attempt <= 15; attempt++) {
            try (Connection c = DriverManager.getConnection(baseUrl, user, password);
                 Statement s = c.createStatement()) {
                s.execute("DROP DATABASE IF EXISTS " + DEDICATED_DB + " WITH (FORCE)");
                s.execute("CREATE DATABASE " + DEDICATED_DB);
                return;
            } catch (Exception e) {
                last = e;
                if (!DatabaseSupport.isTransientConnectFailure(e) || attempt == 15) {
                    break;
                }
                Thread.sleep(2_000);
            }
        }
        throw last;
    }
}
