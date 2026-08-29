package org.pangya.db;

import org.junit.jupiter.api.Test;
import org.pangya.protocol.game.GamePackets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RankRepositoryTest {

    @Test
    void readsProcGetRankRegistryInfoEquivalent() {
        String url = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        DatabaseSupport.migrate(url, user, password);

        try (var ds = DatabaseSupport.dataSource(url, user, password)) {
            var jdbi = DatabaseSupport.jdbi(ds);
            jdbi.useHandle(h -> h.execute(
                    "DELETE FROM pangya.pangya_rank_atual WHERE tipo_rank = 7 AND tipo_rank_seq = 3"));
            jdbi.useHandle(h -> h.execute("""
                    INSERT INTO pangya.pangya_rank_atual (position, "UID", tipo_rank, tipo_rank_seq, valor)
                    VALUES (1, 10001, 7, 3, 42)
                    """));
            RankRepository repo = new JdbiRankRepository(jdbi);
            var page = repo.page(7, 3, 1);
            assertEquals(1, page.size());
            assertEquals(10001, page.getFirst().uid());
            assertEquals(42, page.getFirst().value());
            assertTrue(repo.playerSnapshot(10001).isPresent());
            assertTrue(repo.character(10001).isPresent());
            assertEquals(GamePackets.TYPEID_NURI, repo.character(10001).orElseThrow().typeid);
            jdbi.useHandle(h -> h.execute(
                    "DELETE FROM pangya.pangya_rank_atual WHERE tipo_rank = 7 AND tipo_rank_seq = 3"));
        }
    }

    @Test
    void geraRankAllWritesLevelBoardForEligibleAccounts() {
        String url = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        DatabaseSupport.migrate(url, user, password);

        try (var ds = DatabaseSupport.dataSource(url, user, password)) {
            RankRepository repo = new JdbiRankRepository(DatabaseSupport.jdbi(ds));
            int written = repo.geraRankAll();
            assertTrue(written > 0, "GeraRankAll must write rows for FIRST_LOGIN+FIRST_SET=2 accounts");
            var level = repo.findInMenu(2, 3, 10001);
            assertTrue(level.isPresent(), "tipo_rank=2 seq=3 is C# level board");
            assertTrue(level.get().value() >= 1, "seeded testuser level is at least 1");
        }
    }

    private static String env(String name, String fallback) {
        String v = System.getenv(name);
        return (v == null || v.isBlank()) ? fallback : v;
    }
}
