package org.pangya.protocol.iff;

import org.junit.jupiter.api.Test;
import org.pangya.protocol.game.CharacterComboDef;
import org.pangya.protocol.game.GamePackets;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IffPartFileTest {

    private static final Path JP_IFF = Path.of(
            "reference/pangya-server-community/Server/JP/GameServer/data/pangya_jp.iff");

    @Test
    void loadsNuriDefaultComboParts() throws Exception {
        assumeReferenceIffPresent();
        IffPartIndex index = IffPartFile.loadIndex(new PangyaIffArchive(JP_IFF));
        assertTrue(index.size() > 9000);
        assertTrue(index.contains(CharacterComboDef.partTypeid(GamePackets.TYPEID_NURI, 0)));
        assertTrue(index.contains(CharacterComboDef.partTypeid(GamePackets.TYPEID_NURI, 2)));
        assertTrue(!index.contains(CharacterComboDef.partTypeid(GamePackets.TYPEID_NURI, 23)));
    }

    @Test
    void initComboDefFromIffMatchesCsharpFindPart() throws Exception {
        assumeReferenceIffPresent();
        PangyaIffLoader.reload(JP_IFF);
        int[] parts = new int[24];
        CharacterComboDef.apply(GamePackets.TYPEID_NURI, parts, PangyaIffLoader.partIndex()::contains);
        assertEquals(134218752, parts[0]);
        assertEquals(134235136, parts[2]);
        assertEquals(0, parts[1]);
        assertEquals(0, parts[23]);
    }

    private static void assumeReferenceIffPresent() {
        assertTrue(JP_IFF.toFile().isFile(), () -> "missing " + JP_IFF);
    }
}
