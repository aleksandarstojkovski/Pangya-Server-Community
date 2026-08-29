package org.pangya.protocol.iff;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.pangya.protocol.game.GamePackets;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IffGrandPrixDataFileTest {

    private static final Path JP_IFF = Path.of(
            "reference/pangya-server-community/Server/JP/GameServer/data/pangya_jp.iff");

    /** C# Shining Sand cup 9H row in JP {@code GrandPrixData.iff}. */
    private static final int TYPEID_SHINING_SAND_9H = 0x80101;

    @AfterEach
    void tearDown() {
        PangyaIffLoader.reload(null);
    }

    @Test
    void loadsShiningSandCupFromGrandPrixDataIff() throws Exception {
        assumeReferenceIffPresent();
        PangyaIffLoader.reload(JP_IFF);
        IffGrandPrixDataRecord row = PangyaIffLoader.grandPrixData(TYPEID_SHINING_SAND_9H).orElseThrow();
        assertEquals(10, row.course());
        assertEquals(9, row.holes());
        assertEquals(0, row.modo());
        assertEquals(0, row.rule());
    }

    @Test
    void loadsGpTicketFromControlPracticeRow() throws Exception {
        assumeReferenceIffPresent();
        PangyaIffLoader.reload(JP_IFF);
        IffGrandPrixDataRecord row = PangyaIffLoader.grandPrixData(0x100).orElseThrow();
        assertEquals(GamePackets.TYPEID_GP_TICKET, row.ticketTypeid());
        assertEquals(1, row.ticketQntd());
    }

    @Test
    void fileLoaderMatchesArchiveSize() throws Exception {
        assumeReferenceIffPresent();
        IffGrandPrixDataIndex index = IffGrandPrixDataFile.loadIndex(new PangyaIffArchive(JP_IFF));
        assertEquals(741, index.size());
    }

    private static void assumeReferenceIffPresent() {
        assertTrue(JP_IFF.toFile().isFile(), () -> "missing " + JP_IFF);
    }
}
