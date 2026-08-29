package org.pangya.protocol.iff;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IffCadieMagicBoxFileTest {

    private static final Path JP_IFF = Path.of(
            "reference/pangya-server-community/Server/JP/GameServer/data/pangya_jp.iff");

    @AfterEach
    void tearDown() {
        PangyaIffLoader.reload(null);
    }

    @Test
    void loadsSeq1CadieMagicBoxFromReferenceArchive() throws Exception {
        assumeReferenceIffPresent();
        PangyaIffLoader.reload(JP_IFF);
        IffCadieMagicBoxRecord box = PangyaIffLoader.cadieMagicBox(1).orElseThrow();
        assertEquals(0, box.level());
        assertEquals(0x1A000133, box.receiveTypeid());
        assertEquals(1, box.receiveQntd());
        assertEquals(0x1A00005B, box.tradeTypeids()[0]);
        assertEquals(1, box.tradeQntds()[0]);
        assertEquals(0, box.boxRandomId());
    }

    @Test
    void fileLoaderMatchesArchiveCounts() throws Exception {
        assumeReferenceIffPresent();
        assertEquals(2056, IffCadieMagicBoxFile.loadIndex(new PangyaIffArchive(JP_IFF)).size());
        assertEquals(179, IffCadieMagicBoxRandomFile.loadIndex(new PangyaIffArchive(JP_IFF)).rowCount());
        assertEquals(9, IffCadieMagicBoxRandomFile.loadIndex(new PangyaIffArchive(JP_IFF)).find(2).size());
    }

    @Test
    void randomPoolSpinReturnsMember() throws Exception {
        assumeReferenceIffPresent();
        PangyaIffLoader.reload(JP_IFF);
        IffCadieMagicBoxRandomRecord row = PangyaIffLoader.spinCadieMagicBoxRandom(2).orElseThrow();
        assertEquals(2, row.groupId());
        assertTrue(row.itemTypeid() != 0);
        assertTrue(row.rate() > 0);
    }

    private static void assumeReferenceIffPresent() {
        assertTrue(JP_IFF.toFile().isFile(), () -> "missing " + JP_IFF);
    }
}
