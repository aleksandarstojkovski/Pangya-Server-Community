package org.pangya.protocol.iff;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IffGrandPrixRankRewardFileTest {

    private static final Path JP_IFF = Path.of(
            "reference/pangya-server-community/Server/JP/GameServer/data/pangya_jp.iff");

    @AfterEach
    void tearDown() {
        PangyaIffLoader.reload(null);
    }

    @Test
    void loadsRankRewardsForShiningSandCupLink() throws Exception {
        assumeReferenceIffPresent();
        PangyaIffLoader.reload(JP_IFF);
        var rows = PangyaIffLoader.grandPrixRankRewards(0x80100);
        assertEquals(3, rows.size());
        assertEquals(1, rows.getFirst().rank());
        assertEquals(0x1a000010, rows.getFirst().reward().typeids()[0]);
        assertEquals(400, rows.getFirst().reward().qntd()[0]);
        assertEquals(3, rows.get(2).rank());
        assertEquals(200, rows.get(2).reward().qntd()[0]);
    }

    @Test
    void fileLoaderMatchesArchiveSize() throws Exception {
        assumeReferenceIffPresent();
        IffGrandPrixRankRewardIndex index = IffGrandPrixRankRewardFile.loadIndex(new PangyaIffArchive(JP_IFF));
        assertEquals(781, index.rowCount());
    }

    private static void assumeReferenceIffPresent() {
        assertTrue(JP_IFF.toFile().isFile(), () -> "missing " + JP_IFF);
    }
}
