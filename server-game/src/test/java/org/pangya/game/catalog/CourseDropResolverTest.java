package org.pangya.game.catalog;

import org.junit.jupiter.api.Test;
import org.pangya.db.DatabaseSupport;
import org.pangya.db.InventoryRepository;
import org.pangya.db.JdbiInventoryRepository;
import org.pangya.protocol.game.GamePackets;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourseDropResolverTest {

    @Test
    void drawCourseFromSqlSeed() {
        String url = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        DatabaseSupport.migrate(url, user, password);

        try (var ds = DatabaseSupport.dataSource(url, user, password)) {
            InventoryRepository inv = new JdbiInventoryRepository(DatabaseSupport.jdbi(ds));
            InventoryRepository.CourseDropConfig cfg =
                    inv.courseDropConfig().orElseThrow();
            assertEquals(100, cfg.rateSscTicket());
            assertEquals(100, cfg.rateManaArtefact());
            List<InventoryRepository.CourseDropItem> items = inv.courseDropIndex().get(0);
            assertFalse(items.isEmpty());

            CourseDropResolver.CourseDropCtx ctx = new CourseDropResolver.CourseDropCtx(
                    0, 1, 1, 18, 0, 1, 100, 0);
            List<GamePackets.DropItem> drops = CourseDropResolver.drawCourse(items, ctx, 42L);
            assertFalse(drops.isEmpty());
            assertEquals(GamePackets.DROP_TYPE_NORMAL_QNTD, drops.getFirst().type());
        }
    }

    @Test
    void lastHoleTipoSkipsUntilFinalHole() {
        InventoryRepository.CourseDropItem lastHole = new InventoryRepository.CourseDropItem(
                0, CourseDropResolver.TIPO_LAST_HOLE_PROBABILITY, 0x1A000006, 1, 1000, 1000, 1000, 1000);
        CourseDropResolver.CourseDropCtx mid = new CourseDropResolver.CourseDropCtx(
                0, 9, 9, 18, 0, 0, 100, 0);
        assertFalse(CourseDropResolver.eligible(lastHole, mid));
        CourseDropResolver.CourseDropCtx last = new CourseDropResolver.CourseDropCtx(
                0, 18, 18, 18, 0, 0, 100, 0);
        assertTrue(CourseDropResolver.eligible(lastHole, last));
    }

    @Test
    void artefactPangMultiplierMatchesCSharp() {
        assertEquals(1, HoleDropResolver.artefactPangMultiplier(GamePackets.ART_WICKED_BROOMSTICK, 1));
        assertEquals(6, HoleDropResolver.artefactPangMultiplier(GamePackets.ART_MAGANI_FLOWER, 30));
        assertEquals(0, HoleDropResolver.artefactPangMultiplier(0, 1));
    }

    private static String env(String name, String fallback) {
        String v = System.getenv(name);
        return (v == null || v.isBlank()) ? fallback : v;
    }
}
