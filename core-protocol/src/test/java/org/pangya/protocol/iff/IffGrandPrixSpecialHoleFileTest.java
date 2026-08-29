package org.pangya.protocol.iff;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IffGrandPrixSpecialHoleFileTest {

    private static final Path JP_IFF = Path.of(
            "reference/pangya-server-community/Server/JP/GameServer/data/pangya_jp.iff");

    /** Rank typeid linked from control-practice GP rows in JP IFF. */
    private static final int RANK_TYPEID_CONTROL_PRACTICE = 0x100;

    @AfterEach
    void tearDown() {
        PangyaIffLoader.reload(null);
    }

    @Test
    void loadsControlPracticeSpecialHolesSortedByHole() throws Exception {
        assumeReferenceIffPresent();
        PangyaIffLoader.reload(JP_IFF);
        List<IffGrandPrixSpecialHoleRecord> rows =
                PangyaIffLoader.grandPrixSpecialHoles(RANK_TYPEID_CONTROL_PRACTICE);
        assertEquals(3, rows.size());
        assertEquals(2, rows.get(0).hole());
        assertEquals(11, rows.get(0).map());
        assertEquals(6, rows.get(1).hole());
        assertEquals(16, rows.get(1).map());
        assertEquals(11, rows.get(2).hole());
        assertEquals(20, rows.get(2).map());
    }

    @Test
    void fileLoaderMatchesArchiveRowCount() throws Exception {
        assumeReferenceIffPresent();
        IffGrandPrixSpecialHoleIndex index = IffGrandPrixSpecialHoleFile.loadIndex(new PangyaIffArchive(JP_IFF));
        assertEquals(84, index.rowCount());
    }

    private static void assumeReferenceIffPresent() {
        assertTrue(JP_IFF.toFile().isFile(), () -> "missing " + JP_IFF);
    }
}
