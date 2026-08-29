package org.pangya.db;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.pangya.protocol.game.GamePackets;
import org.pangya.protocol.iff.PangyaIffLoader;

import java.nio.file.Path;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemInitializerTest {

    private static final Path JP_IFF = Path.of(
            "reference/pangya-server-community/Server/JP/GameServer/data/pangya_jp.iff");

    @AfterEach
    void tearDown() {
        PangyaIffLoader.reload(null);
    }

    @Test
    void expandGreenlineSwimsetFromReferenceArchive() {
        if (!JP_IFF.toFile().isFile()) {
            return;
        }
        PangyaIffLoader.reload(JP_IFF);
        var rows = ItemInitializer.expandSetItem(true, 0x24200000);
        assertEquals(3, rows.size());
        assertEquals(0x08006010, rows.get(0).typeid());
        assertEquals(1, rows.get(0).c0());
        assertEquals(1, rows.get(0).qntdDep());
    }

    @Test
    void initPartUsesTypeItemFromIff() {
        if (!JP_IFF.toFile().isFile()) {
            return;
        }
        PangyaIffLoader.reload(JP_IFF);
        var ctx = new ItemInitializer.InitContext(10, true, false, true);
        var row = ItemInitializer.initFromBuyItem(ctx, 0x08006010, 1, 0).orElseThrow();
        assertEquals(0x08006010, row.typeid());
        assertEquals(0, row.itemType());
        assertEquals(1, row.c0());
    }

    @Test
    void resolveMailItemsExpandsGreenlineSet() {
        if (!JP_IFF.toFile().isFile()) {
            return;
        }
        PangyaIffLoader.reload(JP_IFF);
        var rows = ItemInitializer.resolveMailItems(
                List.of(new ItemInitializer.MailItemRef(0x24200000, 1)));
        assertEquals(3, rows.size());
        assertEquals(0x08006010, rows.get(0).typeid());
    }

    @Test
    void fallsBackWhenIffUnloaded() {
        PangyaIffLoader.reload(null);
        var row = ItemInitializer.initFromBuyItem(
                new ItemInitializer.InitContext(1, true, false, false),
                GamePackets.TYPEID_SHOP_PANG_ITEM,
                2,
                0).orElseThrow();
        assertEquals(2, row.c0());
        assertTrue(ItemInitializer.expandSetItem(true, 0x24200000).isEmpty());
    }
}
