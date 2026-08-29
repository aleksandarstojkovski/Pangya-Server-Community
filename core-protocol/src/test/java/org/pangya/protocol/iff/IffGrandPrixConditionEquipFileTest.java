package org.pangya.protocol.iff;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.pangya.protocol.game.GamePackets;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IffGrandPrixConditionEquipFileTest {

    private static final Path JP_IFF = Path.of(
            "reference/pangya-server-community/Server/JP/GameServer/data/pangya_jp.iff");

    @AfterEach
    void tearDown() {
        PangyaIffLoader.reload(null);
    }

    @Test
    void loadsBeginnerCharacterRequirement() throws Exception {
        assumeReferenceIffPresent();
        PangyaIffLoader.reload(JP_IFF);
        IffGrandPrixConditionEquipRecord row =
                PangyaIffLoader.grandPrixConditionEquip(0x180500).orElseThrow();
        assertEquals(0x180500, row.typeidLink());
        assertEquals(0x10000000, row.itemTypeid());
    }

    @Test
    void fileLoaderMatchesArchiveSize() throws Exception {
        assumeReferenceIffPresent();
        IffGrandPrixConditionEquipIndex index =
                IffGrandPrixConditionEquipFile.loadIndex(new PangyaIffArchive(JP_IFF));
        assertEquals(2, index.size());
    }

    private static void assumeReferenceIffPresent() {
        assertTrue(JP_IFF.toFile().isFile(), () -> "missing " + JP_IFF);
    }
}
