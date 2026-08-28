package org.pangya.game.catalog;

import org.junit.jupiter.api.Test;
import org.pangya.db.DatabaseSupport;
import org.pangya.db.InventoryRepository;
import org.pangya.db.JdbiInventoryRepository;
import org.pangya.protocol.game.GamePackets;
import org.pangya.protocol.packet.PacketReader;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CubeCoinResolverTest {

    @Test
    void resolveCoinEdgeFromV37Seed() {
        String url = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        DatabaseSupport.migrate(url, user, password);

        try (var ds = DatabaseSupport.dataSource(url, user, password)) {
            InventoryRepository inv = new JdbiInventoryRepository(DatabaseSupport.jdbi(ds));
            assertTrue(inv.coinCubeCourseActive().getOrDefault((short) 0, false));
            assertTrue(inv.coinCubeLocations().stream()
                    .anyMatch(l -> l.index() == 99 && l.hole() == 1 && l.tipoLocation() == 0));

            GlobalCatalogs catalogs = new GlobalCatalogs(inv);
            var body = new GamePackets.ShotAckCubeCoin(1, List.of(new GamePackets.CubeCoinPick(0, 99)));
            List<GamePackets.DropItem> drops = CubeCoinResolver.resolve(catalogs, 0, 1, body);
            assertEquals(1, drops.size());
            assertEquals(GamePackets.TYPEID_COIN, drops.getFirst().typeid());
            assertEquals(GamePackets.DROP_TYPE_COIN_EDGE, drops.getFirst().type());

            PacketReader r = new PacketReader(GamePackets.endShot(1, drops));
            r.opcode();
            r.i32();
            r.u8();
            r.u32();
            r.u8();
            r.u8();
            r.i16();
            assertEquals(GamePackets.DROP_TYPE_COIN_EDGE, r.u64());
        }
    }

    private static String env(String name, String fallback) {
        String v = System.getenv(name);
        return (v == null || v.isBlank()) ? fallback : v;
    }
}
