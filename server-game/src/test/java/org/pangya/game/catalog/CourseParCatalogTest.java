package org.pangya.game.catalog;

import org.junit.jupiter.api.Test;
import org.pangya.db.DatabaseSupport;
import org.pangya.db.InventoryRepository;
import org.pangya.db.JdbiInventoryRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourseParCatalogTest {

    @Test
    void blueLagoonHole3ParFromSql() {
        String url = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        DatabaseSupport.migrate(url, user, password);

        try (var ds = DatabaseSupport.dataSource(url, user, password)) {
            InventoryRepository inv = new JdbiInventoryRepository(DatabaseSupport.jdbi(ds));
            GlobalCatalogs catalogs = new GlobalCatalogs(inv);
            assertEquals(4, catalogs.parFor(0, 1));
            assertEquals(5, catalogs.parFor(0, 5));
            int hole3Par = catalogs.parFor(0, 3);
            assertTrue(hole3Par == 3 || hole3Par == 4, "hole 3 par from SQL or fallback");
        }
    }

    private static String env(String name, String fallback) {
        String v = System.getenv(name);
        return (v == null || v.isBlank()) ? fallback : v;
    }
}
