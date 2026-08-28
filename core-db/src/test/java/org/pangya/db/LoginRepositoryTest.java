package org.pangya.db;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginRepositoryTest {

    @Test
    void verifyTestUserAndGenerateAuthKeys() {
        String url = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        DatabaseSupport.migrate(url, user, password);

        try (var ds = DatabaseSupport.dataSource(url, user, password)) {
            LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(ds));
            assertEquals(10001L, repo.verifyId("testuser").orElseThrow());
            assertTrue(repo.verifyPass(10001, "testpass"));
            assertFalse(repo.verifyPass(10001, "wrong"));
            var info = repo.playerInfo(10001).orElseThrow();
            assertEquals("TestNick", info.nickname());
            assertTrue(repo.isFirstLoginDone(10001));
            assertTrue(repo.isFirstSetDone(10001));
            assertFalse(repo.isLogon(10001));
            assertFalse(repo.isBannedIp("127.0.0.1"));

            String loginKey = repo.generateAuthKeyLogin(10001);
            assertEquals(8, loginKey.length());
            String gameKey = repo.generateAuthKeyGame(10001, 20202);
            assertEquals(8, gameKey.length());
            assertEquals(loginKey, repo.loadAuthKeyLogin(10001).orElseThrow());
            assertEquals(gameKey, repo.loadAuthKeyGame(10001, 20202).orElseThrow());
            String s2s = repo.generateAuthServerKey(10203);
            assertEquals(16, s2s.length());
            var stored = repo.authServerKey(10203).orElseThrow();
            assertTrue(stored.valid());
            assertEquals(s2s, stored.key());

            LoginRepository.ServerListRow game = new LoginRepository.ServerListRow(
                    "PAPEL", 20202, "127.0.0.1", 20202, 2001, 0, 1,
                    2048, 0, 0, (short) 0, (short) 0, (short) 0, (short) 0,
                    "GS.Release.852.00", "852.00");
            repo.upsertServer(game);
            assertEquals(1, repo.serverList(1).size());
            assertEquals("PAPEL", repo.serverList(1).getFirst().name());
        }
    }

    private static String env(String name, String fallback) {
        String v = System.getenv(name);
        return (v == null || v.isBlank()) ? fallback : v;
    }
}
