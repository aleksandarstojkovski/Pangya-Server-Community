package org.pangya.protocol.iff;

import org.junit.jupiter.api.Test;
import org.pangya.protocol.game.GamePackets;

import java.nio.file.Path;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IffEnchantFileTest {

    private static final Path JP_IFF = Path.of(
            "reference/pangya-server-community/Server/JP/GameServer/data/pangya_jp.iff");

    @Test
    void loadsPowerPcl0EnchantFromEnchantIff() throws Exception {
        assumeReferenceIffPresent();
        PangyaIffLoader.reload(JP_IFF);
        int typeid = GamePackets.enchantTypeid(GamePackets.CHAR_STATS_POWER, 0);
        OptionalLong pang = PangyaIffLoader.enchantPang(typeid);
        assertTrue(pang.isPresent());
        assertEquals(2100L, pang.getAsLong());
    }

    @Test
    void fileLoaderMatchesArchiveSize() throws Exception {
        assumeReferenceIffPresent();
        IffEnchantIndex index = IffEnchantFile.loadIndex(new PangyaIffArchive(JP_IFF));
        assertEquals(175, index.size());
    }

    private static void assumeReferenceIffPresent() {
        assertTrue(JP_IFF.toFile().isFile(), () -> "missing " + JP_IFF);
    }
}
