package org.pangya.protocol.iff;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IffTimeLimitItemFileTest {

    private static final Path JP_IFF = Path.of(
            "reference/pangya-server-community/Server/JP/GameServer/data/pangya_jp.iff");

    /** Yam buff item in JP IFF. */
    private static final int YAM_BUFF = 0x1A0000B3;

    @AfterEach
    void tearDown() {
        PangyaIffLoader.reload(null);
    }

    @Test
    void loadsYamBuffFromTimeLimitItemIff() throws Exception {
        assumeReferenceIffPresent();
        PangyaIffLoader.reload(JP_IFF);
        IffTimeLimitItemRecord item = PangyaIffLoader.timeLimitItem(YAM_BUFF).orElseThrow();
        assertEquals(1, item.tipo());
        assertEquals(5, item.percent());
        assertEquals(120, item.timeMinutes());
    }

    @Test
    void fileLoaderMatchesArchiveSize() throws Exception {
        assumeReferenceIffPresent();
        assertEquals(6, IffTimeLimitItemFile.loadIndex(new PangyaIffArchive(JP_IFF)).size());
    }

    private static void assumeReferenceIffPresent() {
        assertTrue(JP_IFF.toFile().isFile(), () -> "missing " + JP_IFF);
    }
}
