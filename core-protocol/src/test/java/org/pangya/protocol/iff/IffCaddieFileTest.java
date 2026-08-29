package org.pangya.protocol.iff;

import org.junit.jupiter.api.Test;
import org.pangya.protocol.game.GamePackets;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IffCaddieFileTest {

    private static final Path JP_IFF = Path.of(
            "reference/pangya-server-community/Server/JP/GameServer/data/pangya_jp.iff");

    @Test
    void loadsCaddieHolidayPricesFromCaddieIff() throws Exception {
        assumeReferenceIffPresent();
        PangyaIffLoader.reload(JP_IFF);
        var papel = PangyaIffLoader.caddie(GamePackets.TYPEID_CADDIE_PAPEL).orElseThrow();
        assertEquals(0, papel.valorMensal());
        assertTrue(papel.canPayHoliday());
        var caddie = PangyaIffLoader.caddie(0x1C000002).orElseThrow();
        assertTrue(caddie.canPayHoliday());
        assertEquals(10000, caddie.valorMensal());
    }

    @Test
    void fileLoaderMatchesArchiveSize() throws Exception {
        assumeReferenceIffPresent();
        IffCaddieIndex index = IffCaddieFile.loadIndex(new PangyaIffArchive(JP_IFF));
        assertEquals(43, index.size());
    }

    private static void assumeReferenceIffPresent() {
        assertTrue(JP_IFF.toFile().isFile(), () -> "missing " + JP_IFF);
    }
}
