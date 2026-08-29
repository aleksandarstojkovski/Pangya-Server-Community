package org.pangya.protocol.iff;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IffClubSetWorkShopFileTest {

    private static final Path JP_IFF = Path.of(
            "reference/pangya-server-community/Server/JP/GameServer/data/pangya_jp.iff");

    @AfterEach
    void tearDown() {
        PangyaIffLoader.reload(null);
    }

    @Test
    void loadsWorkshopTablesFromReferenceArchive() throws Exception {
        assumeReferenceIffPresent();
        PangyaIffLoader.reload(JP_IFF);
        assertArrayEquals(
                new short[] {14, 12, 12, 5, 5},
                PangyaIffLoader.clubSetWorkShopLevelUpLimit(0, 0).orElseThrow());
        assertArrayEquals(
                new int[] {500, 200, 800, 500, 600},
                PangyaIffLoader.clubSetWorkShopLevelUpProb(0).orElseThrow());
        assertTrue(PangyaIffLoader.clubSetWorkShopLevelUpAny(0));
        assertTrue(PangyaIffLoader.clubSetWorkShopRankExp(0));
        assertArrayEquals(
                new int[] {0, 900, 2800, 11000, 20200, 68000},
                PangyaIffLoader.clubSetWorkShopRankExpRanks(0).orElseThrow());
    }

    @Test
    void fileLoaderMatchesArchiveCounts() throws Exception {
        assumeReferenceIffPresent();
        assertEquals(
                30,
                IffClubSetWorkShopLevelUpLimitFile.loadIndex(new PangyaIffArchive(JP_IFF)).rowCount());
        assertEquals(
                5,
                IffClubSetWorkShopLevelUpProbFile.loadIndex(new PangyaIffArchive(JP_IFF)).size());
        assertEquals(
                4,
                IffClubSetWorkShopRankUpExpFile.loadIndex(new PangyaIffArchive(JP_IFF)).size());
    }

    private static void assumeReferenceIffPresent() {
        assertTrue(JP_IFF.toFile().isFile(), () -> "missing " + JP_IFF);
    }
}
