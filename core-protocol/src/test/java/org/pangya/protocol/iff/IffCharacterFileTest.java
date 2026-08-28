package org.pangya.protocol.iff;

import org.junit.jupiter.api.Test;
import org.pangya.protocol.game.GamePackets;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IffCharacterFileTest {

    private static final Path JP_IFF = Path.of(
            "reference/pangya-server-community/Server/JP/GameServer/data/pangya_jp.iff");

    @Test
    void loadsNuriPclFromReferenceArchive() throws Exception {
        assumeReferenceIffPresent();
        Map<Integer, IffCharacterRecord> chars = IffCharacterFile.loadIndex(new PangyaIffArchive(JP_IFF));
        IffCharacterRecord nuri = chars.get(GamePackets.TYPEID_NURI);
        assertEquals(9, nuri.pcl(0));
        assertEquals(11, nuri.pcl(1));
        assertEquals(6, nuri.pcl(2));
        assertEquals(2, nuri.pcl(3));
        assertEquals(2, nuri.pcl(4));
    }

    @Test
    void loaderExposesCharacterLookup() throws Exception {
        assumeReferenceIffPresent();
        PangyaIffLoader.reload(JP_IFF);
        assertEquals(9, PangyaIffLoader.character(GamePackets.TYPEID_NURI).orElseThrow().pcl(0));
        PangyaIffLoader.reload(null);
    }

    private static void assumeReferenceIffPresent() {
        assertTrue(JP_IFF.toFile().isFile(), () -> "missing " + JP_IFF);
    }
}
