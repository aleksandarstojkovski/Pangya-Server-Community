package org.pangya.db;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FriendRepositoryTest {

    @Test
    void addAgreeBlockRemoveRoundtrip() {
        String url = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        DatabaseSupport.migrate(url, user, password);

        try (var ds = DatabaseSupport.dataSource(url, user, password)) {
            FriendRepository repo = new JdbiFriendRepository(DatabaseSupport.jdbi(ds));
            repo.delete(10001, 10002);
            repo.delete(10002, 10001);
            repo.add(10001, new FriendRepository.FriendRow(
                    10002, "TestNick2", "Friend", -1, 0, -1, 0, 0, 0, 255, 8));
            repo.add(10002, new FriendRepository.FriendRow(
                    10001, "TestNick", "Friend", -1, 0, -1, 0, 0, 0, 255, 0));
            assertEquals(1, repo.count(10001));
            assertTrue(repo.find(10001, 10002).isPresent());
            repo.updateState(10002, 10001, 4);
            assertEquals(4, repo.find(10002, 10001).orElseThrow().stateFlag());
            repo.delete(10001, 10002);
            repo.delete(10002, 10001);
            assertEquals(0, repo.count(10001));
        }
    }

    private static String env(String name, String fallback) {
        String v = System.getenv(name);
        return (v == null || v.isBlank()) ? fallback : v;
    }
}
