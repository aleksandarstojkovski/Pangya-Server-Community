package org.pangya.protocol.iff;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IffCutinInformationFileTest {

    private static final Path JP_IFF = Path.of(
            "reference/pangya-server-community/Server/JP/GameServer/data/pangya_jp.iff");

    /** First Nuri cutin item in JP IFF. */
    private static final int NURI_CUTIN = 0x39400000;

    @AfterEach
    void tearDown() {
        PangyaIffLoader.reload(null);
    }

    @Test
    void loadsNuriCutinFromCutinInformationIff() throws Exception {
        assumeReferenceIffPresent();
        PangyaIffLoader.reload(JP_IFF);
        IffCutinInformationRecord cutin = PangyaIffLoader.cutin(NURI_CUTIN).orElseThrow();
        assertEquals(1, cutin.sector());
        assertEquals(1, cutin.condition());
        assertArrayEquals(new int[] {1, 5, 2, 0}, cutin.imageTypes());
        assertEquals(6, cutin.tempo());
        assertEquals("Layer_Nuri01.PNG", cutin.sprites()[0]);
    }

    @Test
    void fileLoaderMatchesArchiveSize() throws Exception {
        assumeReferenceIffPresent();
        IffCutinInformationIndex index = IffCutinInformationFile.loadIndex(new PangyaIffArchive(JP_IFF));
        assertEquals(641, index.size());
    }

    private static void assumeReferenceIffPresent() {
        assertTrue(JP_IFF.toFile().isFile(), () -> "missing " + JP_IFF);
    }
}
