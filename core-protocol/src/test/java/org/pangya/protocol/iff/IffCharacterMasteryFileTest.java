package org.pangya.protocol.iff;

import org.junit.jupiter.api.Test;
import org.pangya.protocol.game.GamePackets;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IffCharacterMasteryFileTest {

    private static final Path JP_IFF = Path.of(
            "reference/pangya-server-community/Server/JP/GameServer/data/pangya_jp.iff");

    /** JP CharacterMastery.iff Nuri seq 1 condition item (not shop-pang). */
    private static final int NURI_MASTERY_COND = 0x1A0002A6;

    @Test
    void loadsNuriMasteryFromCharacterMasteryIff() throws Exception {
        assumeReferenceIffPresent();
        PangyaIffLoader.reload(JP_IFF);
        List<IffCharacterMasteryRecord> nuri =
                PangyaIffLoader.characterMastery(GamePackets.TYPEID_NURI).orElseThrow();
        assertEquals(10, nuri.size());
        IffCharacterMasteryRecord first = nuri.getFirst();
        assertEquals(1, first.seq());
        assertEquals(1, first.stats());
        assertEquals(0, first.level());
        assertEquals(NURI_MASTERY_COND, first.conditionTypeid()[0]);
        assertEquals(2, first.conditionQntd()[0]);
    }

    @Test
    void fileLoaderMatchesPangyaIffLoader() throws Exception {
        assumeReferenceIffPresent();
        IffCharacterMasteryIndex index =
                IffCharacterMasteryFile.loadIndex(new PangyaIffArchive(JP_IFF));
        assertTrue(index.rowCount() > 100);
        assertTrue(index.find(GamePackets.TYPEID_NURI).isPresent());
    }

    private static void assumeReferenceIffPresent() {
        assertTrue(JP_IFF.toFile().isFile(), () -> "missing " + JP_IFF);
    }
}
