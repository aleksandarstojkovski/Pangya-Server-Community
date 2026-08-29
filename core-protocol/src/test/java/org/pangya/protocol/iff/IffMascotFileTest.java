package org.pangya.protocol.iff;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.pangya.protocol.game.GamePackets;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IffMascotFileTest {

    private static final Path JP_IFF = Path.of(
            "reference/pangya-server-community/Server/JP/GameServer/data/pangya_jp.iff");

    @AfterEach
    void tearDown() {
        PangyaIffLoader.reload(null);
    }

    @Test
    void loadsMascotMessageFromMascotIff() throws Exception {
        assumeReferenceIffPresent();
        PangyaIffLoader.reload(JP_IFF);
        var mascot = PangyaIffLoader.mascot(GamePackets.TYPEID_MASCOT).orElseThrow();
        assertTrue(mascot.messageActive());
        assertEquals(0, mascot.changePrice());
        var paid = PangyaIffLoader.mascot(0x40000005).orElseThrow();
        assertTrue(paid.messageActive());
        assertEquals(1000, paid.changePrice());
    }

    @Test
    void fileLoaderMatchesArchiveSize() throws Exception {
        assumeReferenceIffPresent();
        IffMascotIndex index = IffMascotFile.loadIndex(new PangyaIffArchive(JP_IFF));
        assertEquals(69, index.size());
    }

    private static void assumeReferenceIffPresent() {
        assertTrue(JP_IFF.toFile().isFile(), () -> "missing " + JP_IFF);
    }
}
