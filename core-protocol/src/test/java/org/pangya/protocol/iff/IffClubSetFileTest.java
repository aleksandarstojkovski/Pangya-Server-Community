package org.pangya.protocol.iff;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.pangya.protocol.game.GamePackets;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IffClubSetFileTest {

    private static final Path JP_IFF = Path.of(
            "reference/pangya-server-community/Server/JP/GameServer/data/pangya_jp.iff");

    @AfterEach
    void tearDown() {
        PangyaIffLoader.reload(null);
    }

    @Test
    void loadsAirKnightClubSetFromClubSetIff() throws Exception {
        assumeReferenceIffPresent();
        PangyaIffLoader.reload(JP_IFF);
        IffClubSetRecord air = PangyaIffLoader.clubSet(GamePackets.TYPEID_AIR_KNIGHT).orElseThrow();
        assertEquals(-1, air.workShopTipo());
        assertArrayEquals(new short[] {6, 6, 4, 3, 3}, air.stats());
        assertArrayEquals(new short[] {8, 9, 8, 3, 3}, air.slots());
        assertEquals(0, air.textPangya());
    }

    @Test
    void findClubSetOriginalMatchesTextPangya() throws Exception {
        assumeReferenceIffPresent();
        PangyaIffLoader.reload(JP_IFF);
        var originals = PangyaIffLoader.clubSetOriginals(GamePackets.TYPEID_WINGTROSS_EVO);
        assertEquals(5, originals.size());
        assertEquals(GamePackets.TYPEID_WINGTROSS_EVO, originals.getFirst().typeid());
        assertArrayEquals(new short[] {12, 10, 8, 3, 2}, originals.getFirst().slots());
        assertEquals(93, originals.getFirst().textPangya());
    }

    @Test
    void fileLoaderMatchesArchiveSize() throws Exception {
        assumeReferenceIffPresent();
        IffClubSetIndex index = IffClubSetFile.loadIndex(new PangyaIffArchive(JP_IFF));
        assertEquals(126, index.size());
    }

    private static void assumeReferenceIffPresent() {
        assertTrue(JP_IFF.toFile().isFile(), () -> "missing " + JP_IFF);
    }
}
