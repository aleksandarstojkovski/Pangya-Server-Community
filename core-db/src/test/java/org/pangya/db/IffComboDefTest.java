package org.pangya.db;

import org.junit.jupiter.api.Test;
import org.pangya.protocol.game.GamePackets;
import org.pangya.protocol.iff.PangyaIffLoader;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IffComboDefTest {

    private static final Path JP_IFF = Path.of(
            "reference/pangya-server-community/Server/JP/GameServer/data/pangya_jp.iff");

    @Test
    void insertCharacterUsesIffPartIndexWhenLoaded() {
        assumeReferenceIffPresent();
        String url = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        DatabaseSupport.migrate(url, user, password);

        PangyaIffLoader.reload(JP_IFF);
        try (var ds = DatabaseSupport.dataSource(url, user, password)) {
            var jdbi = DatabaseSupport.jdbi(ds);
            LoginRepository repo = new JdbiLoginRepository(jdbi);
            InventoryRepository inv = new JdbiInventoryRepository(jdbi);
            long uid = 10003L;
            try {
                jdbi.useHandle(h -> h.createUpdate(
                                "DELETE FROM pangya.pangya_character_information WHERE \"UID\" = :uid")
                        .bind("uid", uid)
                        .execute());
                int charId = repo.insertCharacter(uid, GamePackets.TYPEID_NURI, 3, 1);
                assertTrue(charId > 0);
                var chars = inv.characters(uid);
                assertEquals(134218752, chars.getFirst().partsTypeid[0]);
                assertEquals(134235136, chars.getFirst().partsTypeid[2]);
                assertEquals(0, chars.getFirst().partsTypeid[23]);
            } finally {
                PangyaIffLoader.reload(null);
                DatabaseSupport.migrate(url, user, password);
            }
        }
    }

    private static void assumeReferenceIffPresent() {
        assertTrue(JP_IFF.toFile().isFile(), () -> "missing " + JP_IFF);
    }

    private static String env(String name, String fallback) {
        String v = System.getenv(name);
        return (v == null || v.isBlank()) ? fallback : v;
    }
}
