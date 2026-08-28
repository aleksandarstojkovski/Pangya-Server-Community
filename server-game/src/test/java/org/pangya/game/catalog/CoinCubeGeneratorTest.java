package org.pangya.game.catalog;

import org.junit.jupiter.api.Test;
import org.pangya.db.DatabaseSupport;
import org.pangya.db.InventoryRepository;
import org.pangya.db.JdbiInventoryRepository;
import org.pangya.protocol.game.GamePackets;
import org.pangya.protocol.packet.PacketReader;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoinCubeGeneratorTest {

    @Test
    void hole1IncludesEdgeCoinAndGroundCoinsFromV38Seed() {
        String url = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        DatabaseSupport.migrate(url, user, password);

        try (var ds = DatabaseSupport.dataSource(url, user, password)) {
            InventoryRepository inv = new JdbiInventoryRepository(DatabaseSupport.jdbi(ds));
            GlobalCatalogs catalogs = new GlobalCatalogs(inv);
            List<GamePackets.CourseCubeEntry> cubes =
                    CoinCubeGenerator.generate(catalogs, 0, 1, 0, false, true);
            assertFalse(cubes.isEmpty());
            assertTrue(cubes.stream().anyMatch(c -> c.id() == 99 && c.flagLocation() == CoinCubeGenerator.LOC_EDGE));
            assertTrue(cubes.size() <= CoinCubeInHole.limitsForPar(4).maxCoinAndCube());
        }
    }

    @Test
    void coursePacketEncodesGeneratedCubes() {
        String url = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        DatabaseSupport.migrate(url, user, password);

        try (var ds = DatabaseSupport.dataSource(url, user, password)) {
            InventoryRepository inv = new JdbiInventoryRepository(DatabaseSupport.jdbi(ds));
            GlobalCatalogs catalogs = new GlobalCatalogs(inv);
            List<GamePackets.CourseCubeEntry> hole1 =
                    CoinCubeGenerator.generate(catalogs, 0, 1, 0, false, true);
            GamePackets.RoomInfo room = new GamePackets.RoomInfo();
            room.course = 0;
            room.tipoShow = GamePackets.TIPO_STROKE;
            room.modo = 0;
            room.holes = 18;
            List<GamePackets.HoleInfo> holes = List.of(
                    new GamePackets.HoleInfo(1, 0, 0, 1, 0, 0, 0));
            List<List<GamePackets.CourseCubeEntry>> cubeByHole = List.of(hole1);
            PacketReader r = new PacketReader(GamePackets.course(room, holes, 1, cubeByHole));
            r.opcode();
            r.u8();
            r.u8();
            r.u8();
            r.u8();
            r.u32();
            r.u32();
            r.u32();
            r.u32();
            r.u8();
            r.u8();
            r.u8();
            r.u32();
            assertEquals(hole1.size(), r.u8());
            GamePackets.CourseCubeEntry first = hole1.getFirst();
            assertEquals(first.tipo(), r.u32());
            assertEquals(first.id(), r.u32());
            assertEquals(first.flagUnknown(), r.u32());
            assertEquals(first.course(), r.u32());
            assertEquals(first.hole(), r.u8());
            assertEquals(first.seqIndex(), r.u8());
            assertEquals(first.flagCubeCoin(), r.u16());
            assertEquals(first.x(), r.f32());
            assertEquals(first.y(), r.f32());
            assertEquals(first.z(), r.f32());
            assertEquals(first.flagLocation(), r.u32());
        }
    }

    private static String env(String name, String fallback) {
        String v = System.getenv(name);
        return (v == null || v.isBlank()) ? fallback : v;
    }
}
