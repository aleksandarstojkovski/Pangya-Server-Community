package org.pangya.game.catalog;

import org.junit.jupiter.api.Test;
import org.pangya.db.DatabaseSupport;
import org.pangya.db.InventoryRepository;
import org.pangya.db.JdbiInventoryRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MapCatalogTest {

    @Test
    void blueLagoonClearBonusFromSql() {
        String url = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        DatabaseSupport.migrate(url, user, password);

        try (var ds = DatabaseSupport.dataSource(url, user, password)) {
            InventoryRepository inv = new JdbiInventoryRepository(DatabaseSupport.jdbi(ds));
            GlobalCatalogs catalogs = new GlobalCatalogs(inv);
            MapCatalog.CourseCtx map = catalogs.courseMap(0);
            assertEquals(20, map.clearBonus());
            assertEquals(180, MapCatalog.calculeClear30s(map, 18));
            assertEquals(360, MapCatalog.calculeClearVs(map, 2, 18));
            assertEquals(360, MapCatalog.calculeClearMatch(map, 18));
            assertEquals(720, MapCatalog.calculeClearMatch(map, 18) * 2);
        }
    }

    private static String env(String name, String fallback) {
        String v = System.getenv(name);
        return (v == null || v.isBlank()) ? fallback : v;
    }
}
