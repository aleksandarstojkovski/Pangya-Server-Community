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
                    10002, "TestNick2", "Friend", -1, 0, -1, 0, 0, 0, 255, 8, 1, 1));
            repo.add(10002, new FriendRepository.FriendRow(
                    10001, "TestNick", "Friend", -1, 0, -1, 0, 0, 0, 255, 0, 1, 1));
            assertEquals(1, repo.count(10001));
            assertTrue(repo.find(10001, 10002).isPresent());
            repo.updateState(10002, 10001, 4);
            assertEquals(4, repo.find(10002, 10001).orElseThrow().stateFlag());
            repo.delete(10001, 10002);
            repo.delete(10002, 10001);
            assertEquals(0, repo.count(10001));
        }
    }

    @Test
    void friendsAndGuildMembersIncludesGuildMate() {
        String url = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        DatabaseSupport.migrate(url, user, password);

        try (var ds = DatabaseSupport.dataSource(url, user, password)) {
            var jdbi = DatabaseSupport.jdbi(ds);
            jdbi.useHandle(h -> {
                h.createUpdate("DELETE FROM pangya.pangya_guild_member WHERE \"MEMBER_UID\" IN (10001, 10002)").execute();
                h.createUpdate("DELETE FROM pangya.pangya_guild WHERE \"GUILD_UID\" = 9002").execute();
                h.createUpdate("""
                        INSERT INTO pangya.pangya_guild (
                            "GUILD_UID", "GUILD_ID", "GUILD_NAME", "GUILD_LEADER", "GUILD_SUB_MASTER",
                            "GUILD_CONDITION_LEVEL", "GUILD_STATE", "GUILD_FLAG", "GUILD_PERMITION_JOIN",
                            "GUILD_PANG", "GUILD_POINT", "GUILD_WIN", "GUILD_LOSE", "GUILD_DRAW",
                            "GUILD_MARK_IMG", "GUILD_MARK_IMG_IDX", "GUILD_NEW_MARK_IDX",
                            "GUILD_NOTICE", "GUILD_INFO", "GUILD_REG_DATE"
                        ) OVERRIDING SYSTEM VALUE VALUES (
                            9002, 'TG9002', 'GuildTwo', 10001, 0,
                            0, 0, 0, 0,
                            0, 0, 0, 0, 0,
                            '', 0, 0,
                            '', '', NOW()
                        )
                        """).execute();
                h.createUpdate("""
                        INSERT INTO pangya.pangya_guild_member (
                            "GUILD_UID", "MEMBER_UID", "GUILD_PANG", "GUILD_POINT",
                            "MEMBER_FLAG", "MEMBER_STATE_FLAG", "REG_DATE"
                        ) VALUES (9002, 10001, 0, 0, 0, 0, NOW())
                        """).execute();
                h.createUpdate("""
                        INSERT INTO pangya.pangya_guild_member (
                            "GUILD_UID", "MEMBER_UID", "GUILD_PANG", "GUILD_POINT",
                            "MEMBER_FLAG", "MEMBER_STATE_FLAG", "REG_DATE"
                        ) VALUES (9002, 10002, 0, 0, 0, 0, NOW())
                        """).execute();
            });
            FriendRepository repo = new JdbiFriendRepository(jdbi);
            var list = repo.friendsAndGuildMembers(10001);
            assertEquals(2, list.size());
            var mate = list.stream().filter(r -> r.friendUid() == 10002).findFirst().orElseThrow();
            assertEquals(2, mate.playerFlag());
        }
    }

    private static String env(String name, String fallback) {
        String v = System.getenv(name);
        return (v == null || v.isBlank()) ? fallback : v;
    }
}
