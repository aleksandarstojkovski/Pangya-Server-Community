package org.pangya.db;

import org.junit.jupiter.api.Test;
import org.pangya.protocol.game.GamePackets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryRepositoryTest {

    @Test
    void loadsStarterNuriAndAirKnight() {
        String url = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        DatabaseSupport.migrate(url, user, password);

        try (var ds = DatabaseSupport.dataSource(url, user, password)) {
            InventoryRepository repo = new JdbiInventoryRepository(DatabaseSupport.jdbi(ds));
            var chars = repo.characters(10001);
            assertEquals(1, chars.size());
            assertEquals(GamePackets.TYPEID_NURI, chars.getFirst().typeid);
            var warehouse = repo.warehouse(10001);
            assertFalse(warehouse.isEmpty());
            assertEquals(GamePackets.TYPEID_AIR_KNIGHT, warehouse.getFirst().typeid);
            GamePackets.UserEquip equip = repo.userEquip(10001);
            assertEquals(chars.getFirst().id, equip.characterId);
            assertEquals(warehouse.getFirst().id, equip.clubsetId);
            assertTrue(repo.mascots(10001).isEmpty());
            assertTrue(repo.cards(10001).isEmpty());
            repo.equipCharacter(10001, 1);
            repo.equipCaddie(10001, 0);
            repo.equipBallAndClub(10001, GamePackets.TYPEID_DEFAULT_BALL, 2);
            repo.equipMascot(10001, 0);
            GamePackets.UserEquip after = repo.userEquip(10001);
            assertEquals(1, after.characterId);
            assertEquals(0, after.caddieId);
            assertEquals(2, after.clubsetId);
            assertEquals(GamePackets.TYPEID_DEFAULT_BALL, after.ballTypeid);
            GamePackets.CharacterInfo parts = chars.getFirst();
            parts.defaultHair = 3;
            repo.updateCharacterParts(10001, parts);
            assertEquals(3, repo.characters(10001).getFirst().defaultHair);
            parts.defaultHair = 0;
            repo.updateCharacterParts(10001, parts);
            assertTrue(repo.counters(10001).isEmpty());
            assertTrue(repo.achievements(10001).isEmpty());
            assertEquals(1, repo.characters(10002).size());
        }
    }

    private static String env(String name, String fallback) {
        String v = System.getenv(name);
        return (v == null || v.isBlank()) ? fallback : v;
    }
}
