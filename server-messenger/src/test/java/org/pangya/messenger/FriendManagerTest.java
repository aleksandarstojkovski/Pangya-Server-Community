package org.pangya.messenger;

import org.junit.jupiter.api.Test;
import org.pangya.db.DatabaseSupport;
import org.pangya.db.FriendRepository;
import org.pangya.db.JdbiFriendRepository;
import org.pangya.protocol.messenger.MessengerPackets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FriendManagerTest {

    @Test
    void initLoadsFriendsAndGuildMembers() {
        String url = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        DatabaseSupport.migrate(url, user, password);

        try (var ds = DatabaseSupport.dataSource(url, user, password)) {
            FriendRepository repo = new JdbiFriendRepository(DatabaseSupport.jdbi(ds));
            repo.delete(10001, 10002);
            repo.delete(10002, 10001);
            repo.add(10001, new FriendRepository.FriendRow(
                    10002, "TestNick2", "Friend", -1, 0, -1, 0, 0, 0, 255,
                    MessengerPackets.FLAG_FRIEND, 1, MessengerPackets.FRIEND_FLAG));
            repo.add(10002, new FriendRepository.FriendRow(
                    10001, "TestNick", "Friend", -1, 0, -1, 0, 0, 0, 255,
                    MessengerPackets.FLAG_FRIEND, 1, MessengerPackets.FRIEND_FLAG));

            FriendManager fm = new FriendManager();
            fm.init(repo, 10001);
            assertTrue(fm.isInitialized());
            assertEquals(1, fm.countFriend());
            assertEquals(1, fm.getAllFriendAndGuildMember(false).size());
            assertTrue(fm.findInAllFriend(10002).isPresent());
            assertTrue(fm.findFriend(10002).isPresent());
        }
    }

    @Test
    void putFriendMergesGuildAndFriendFlags() {
        FriendManager fm = new FriendManager();
        fm.putFriend(new FriendRepository.FriendRow(
                10002, "A", "Friend", 0, 0, 0, 0, 0, 0, 255, 0, 1, MessengerPackets.FRIEND_FLAG));
        fm.putFriend(new FriendRepository.FriendRow(
                10002, "A", "Friend", 0, 0, 0, 0, 0, 0, 255, 0, 1, MessengerPackets.GUILD_MEMBER_FLAG));
        assertEquals(3, fm.findInAllFriend(10002).orElseThrow().playerFlag());
    }

    @Test
    void getAllFriendAndGuildMemberCanExcludeBlocked() {
        FriendManager fm = new FriendManager();
        fm.putFriend(new FriendRepository.FriendRow(
                10002, "A", "Friend", 0, 0, 0, 0, 0, 0, 255, MessengerPackets.FLAG_BLOCK, 1, 1));
        fm.putFriend(new FriendRepository.FriendRow(
                10003, "B", "Friend", 0, 0, 0, 0, 0, 0, 255, 0, 1, 1));
        assertEquals(2, fm.getAllFriendAndGuildMember(false).size());
        assertEquals(1, fm.getAllFriendAndGuildMember(true).size());
        assertFalse(fm.getAllFriendAndGuildMember(true).stream().anyMatch(r -> r.friendUid() == 10002));
    }

    private static String env(String key, String fallback) {
        String v = System.getenv(key);
        return v == null || v.isBlank() ? fallback : v;
    }
}
