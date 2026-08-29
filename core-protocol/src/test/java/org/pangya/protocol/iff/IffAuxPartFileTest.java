package org.pangya.protocol.iff;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IffAuxPartFileTest {

    private static final Path JP_IFF = Path.of(
            "reference/pangya-server-community/Server/JP/GameServer/data/pangya_jp.iff");

    @AfterEach
    void tearDown() {
        PangyaIffLoader.reload(null);
    }

    @Test
    void loadsDropRateFromAuxPartIff() throws Exception {
        if (!JP_IFF.toFile().isFile()) {
            return;
        }
        PangyaIffLoader.reload(JP_IFF);
        assertTrue(PangyaIffLoader.auxPartIndex().size() > 0);
        IffAuxPartRecord sample =
                PangyaIffLoader.auxPartIndex().byTypeid().values().iterator().next();
        assertTrue(sample.dropRate() >= 0);
    }
}
