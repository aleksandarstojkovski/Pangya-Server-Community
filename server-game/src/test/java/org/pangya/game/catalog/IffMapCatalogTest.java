package org.pangya.game.catalog;

import org.junit.jupiter.api.Test;
import org.pangya.db.DatabaseSupport;
import org.pangya.db.JdbiInventoryRepository;
import org.pangya.protocol.iff.IffCourseFile;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IffMapCatalogTest {

    private static final Path JP_IFF = Path.of(
            "reference/pangya-server-community/Server/JP/GameServer/data/pangya_jp.iff");

    @Test
    void iffOverlayLoadsBlueLagoonParAndStar() throws Exception {
        assumeReferenceIffPresent();
        String jdbc = System.getenv().getOrDefault(
                "PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = System.getenv().getOrDefault("PANGYA_TEST_JDBC_USER", "pangya");
        String password = System.getenv().getOrDefault("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        DatabaseSupport.migrate(jdbc, user, password);
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password)) {
            var inv = new JdbiInventoryRepository(DatabaseSupport.jdbi(ds));
            GlobalCatalogs catalogs = new GlobalCatalogs(inv, JP_IFF);
            MapCatalog.CourseCtx map = catalogs.courseMap(0);
            assertEquals("BLUE LAGOON", map.name());
            assertEquals(20, map.clearBonus());
            assertEquals(2.7f, map.star(), 0.001f);
            assertEquals(4, catalogs.parFor(0, 1));
            assertEquals(3, catalogs.parFor(0, 2));
            assertEquals(22, IffCourseFile.load(new org.pangya.protocol.iff.PangyaIffArchive(JP_IFF)).size());
        }
    }

    private static void assumeReferenceIffPresent() {
        assertTrue(JP_IFF.toFile().isFile(), () -> "missing " + JP_IFF);
    }
}
