package org.pangya.game;

import org.junit.jupiter.api.Test;
import org.pangya.db.DatabaseSupport;
import org.pangya.db.JdbiInventoryRepository;
import org.pangya.game.catalog.GlobalCatalogs;
import org.pangya.protocol.game.GamePackets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameCourseTest {

    @Test
    void blueLagoonHole1HasSqlCubesWhenCatalogActive() {
        String url = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        DatabaseSupport.migrate(url, user, password);

        try (var ds = DatabaseSupport.dataSource(url, user, password)) {
            GlobalCatalogs catalogs =
                    new GlobalCatalogs(new JdbiInventoryRepository(DatabaseSupport.jdbi(ds)));
            GamePackets.RoomInfo info = new GamePackets.RoomInfo();
            info.course = 0;
            GameCourse course = new GameCourse(info, catalogs);
            assertFalse(course.cubesByHole.getFirst().isEmpty());
            assertTrue(course.cubesByHole.getFirst().stream().anyMatch(c -> c.id() == 99));
        }
    }

    private static String env(String name, String fallback) {
        String v = System.getenv(name);
        return (v == null || v.isBlank()) ? fallback : v;
    }
}
