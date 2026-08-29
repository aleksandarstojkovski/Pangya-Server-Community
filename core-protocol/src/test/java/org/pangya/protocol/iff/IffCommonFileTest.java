package org.pangya.protocol.iff;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.pangya.protocol.game.GamePackets;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IffCommonFileTest {

    private static final Path JP_IFF = Path.of(
            "reference/pangya-server-community/Server/JP/GameServer/data/pangya_jp.iff");

    @AfterEach
    void tearDown() {
        PangyaIffLoader.reload(null);
    }

    @Test
    void commonItemNameLoadsNecklaceFromItemIff() {
        if (!JP_IFF.toFile().isFile()) {
            return;
        }
        PangyaIffLoader.reload(JP_IFF);
        assertEquals(
                "Necklace of Good Fortune",
                PangyaIffLoader.commonItemName(GamePackets.TYPEID_SHOP_PANG_ITEM).orElseThrow());
    }

    @Test
    void gmGiveitemMsgUsesIffNameWhenLoaded() {
        if (!JP_IFF.toFile().isFile()) {
            return;
        }
        PangyaIffLoader.reload(JP_IFF);
        assertTrue(GamePackets.gmGiveitemMsg(GamePackets.TYPEID_SHOP_PANG_ITEM)
                .contains("Necklace of Good Fortune"));
    }

    @Test
    void skinAndCaddieItemIndexesLoadFromReferenceArchive() {
        if (!JP_IFF.toFile().isFile()) {
            return;
        }
        PangyaIffLoader.reload(JP_IFF);
        assertTrue(PangyaIffLoader.skinIndex().contains(GamePackets.TYPEID_SKIN_RABBITS));
        assertTrue(PangyaIffLoader.caddieItemIndex().contains(GamePackets.TYPEID_CAD_ITEM_PAPEL));
    }
}
