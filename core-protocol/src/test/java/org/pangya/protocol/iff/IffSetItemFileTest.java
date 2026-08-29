package org.pangya.protocol.iff;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IffSetItemFileTest {

    private static final Path JP_IFF = Path.of(
            "reference/pangya-server-community/Server/JP/GameServer/data/pangya_jp.iff");

    /** C# {@code Greenline Swimset (N)} first row in JP {@code SetItem.iff}. */
    private static final int TYPEID_GREENLINE_SWIMSET = 0x24200000;

    @AfterEach
    void tearDown() {
        PangyaIffLoader.reload(null);
    }

    @Test
    void loadsGreenlineSwimsetPackageFromSetItemIff() throws Exception {
        assumeReferenceIffPresent();
        PangyaIffLoader.reload(JP_IFF);
        IffSetItemRecord row = PangyaIffLoader.setItem(TYPEID_GREENLINE_SWIMSET).orElseThrow();
        assertEquals(3, row.packege().total());
        assertEquals(1, row.typeSet());
        assertEquals(0x08006010, row.packege().itemTypeids()[0]);
        assertEquals(0x0801000A, row.packege().itemTypeids()[1]);
        assertEquals(0x0800080A, row.packege().itemTypeids()[2]);
    }

    @Test
    void fileLoaderMatchesArchiveSize() throws Exception {
        assumeReferenceIffPresent();
        IffSetItemIndex index = IffSetItemFile.loadIndex(new PangyaIffArchive(JP_IFF));
        assertEquals(1088, index.size());
    }

    private static void assumeReferenceIffPresent() {
        assertTrue(JP_IFF.toFile().isFile(), () -> "missing " + JP_IFF);
    }
}
