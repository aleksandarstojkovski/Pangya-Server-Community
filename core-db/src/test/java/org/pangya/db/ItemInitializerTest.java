package org.pangya.db;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.pangya.protocol.game.GamePackets;
import org.pangya.protocol.iff.PangyaIffLoader;
import org.pangya.protocol.packet.PacketReader;

import java.nio.file.Path;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemInitializerTest {

    private static final Path JP_IFF = Path.of(
            "reference/pangya-server-community/Server/JP/GameServer/data/pangya_jp.iff");

    @AfterEach
    void tearDown() {
        PangyaIffLoader.reload(null);
    }

    @Test
    void initGrandPrixAwardUsesTimedRentalDays() {
        if (!JP_IFF.toFile().isFile()) {
            return;
        }
        PangyaIffLoader.reload(JP_IFF);
        var ctx = new ItemInitializer.InitContext(10, false, false, true);
        var row = ItemInitializer.initGrandPrixAward(ctx, 0x08006010, 1, 7).orElseThrow();
        assertEquals(4, row.rentFlag());
        assertEquals(7, row.caddiePeriodDays());
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
    void mailInfoItemsExpandsGreenlineSet() {
        if (!JP_IFF.toFile().isFile()) {
            return;
        }
        PangyaIffLoader.reload(JP_IFF);
        var bytes = ItemInitializer.mailInfoItems(
                List.of(new ItemInitializer.MailItemRef(0x24200000, 1)));
        assertEquals(3, bytes.size());
        PacketReader first = new PacketReader(bytes.getFirst());
        assertEquals(-1, first.i32());
        assertEquals(0x08006010, first.u32());
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
        assertNotNull(rows.get(0).warehouse());
    }

    @Test
    void resolveMailItemsInitializesCardWhenIffLoaded() {
        if (!JP_IFF.toFile().isFile()) {
            return;
        }
        PangyaIffLoader.reload(JP_IFF);
        var rows = ItemInitializer.resolveMailItems(
                List.of(new ItemInitializer.MailItemRef(GamePackets.TYPEID_CARD_NORMAL, 2)));
        assertEquals(1, rows.size());
        assertEquals(GamePackets.TYPEID_CARD_NORMAL, rows.get(0).typeid());
        assertEquals(2, rows.get(0).qntd());
    }

    @Test
    void resolveMailItemsInitializesSkinWhenIffLoaded() {
        if (!JP_IFF.toFile().isFile()) {
            return;
        }
        PangyaIffLoader.reload(JP_IFF);
        var rows = ItemInitializer.resolveMailItems(
                List.of(new ItemInitializer.MailItemRef(GamePackets.TYPEID_SKIN_RABBITS, 1)));
        assertEquals(1, rows.size());
        assertEquals(GamePackets.TYPEID_SKIN_RABBITS, rows.get(0).typeid());
        assertNotNull(rows.get(0).warehouse());
    }

    @Test
    void resolveMailItemsInitializesCadItemWhenIffLoaded() {
        if (!JP_IFF.toFile().isFile()) {
            return;
        }
        PangyaIffLoader.reload(JP_IFF);
        var rows = ItemInitializer.resolveMailItems(
                List.of(new ItemInitializer.MailItemRef(GamePackets.TYPEID_CAD_ITEM_PAPEL, 7)));
        assertEquals(1, rows.size());
        assertEquals(GamePackets.TYPEID_CAD_ITEM_PAPEL, rows.get(0).typeid());
        assertEquals(4, rows.get(0).rentFlag());
        assertEquals(7, rows.get(0).caddiePeriodDays());
    }

    @Test
    void resolveMailTimeMatchesEmailItemConversion() {
        assertEquals(7, ItemInitializer.resolveMailTime(4, 7, 1));
        assertEquals(7, ItemInitializer.resolveMailTime(2, 168, 1));
        assertEquals(3, ItemInitializer.resolveMailTime(0, 0, 3));
    }

    @Test
    void resolveMailItemsInitializesTimedSkinWhenIffLoaded() {
        if (!JP_IFF.toFile().isFile()) {
            return;
        }
        PangyaIffLoader.reload(JP_IFF);
        var rows = ItemInitializer.resolveMailItems(List.of(
                new ItemInitializer.MailItemRef(GamePackets.TYPEID_SKIN_RABBITS, 1, 4, 7)));
        assertEquals(1, rows.size());
        assertEquals(4, rows.get(0).rentFlag());
        assertEquals(7, rows.get(0).caddiePeriodDays());
        assertEquals(0x40, rows.get(0).warehouse().flag());
    }

    @Test
    void resolveMailItemsInitializesTimedMascotWhenIffLoaded() {
        if (!JP_IFF.toFile().isFile()) {
            return;
        }
        PangyaIffLoader.reload(JP_IFF);
        var rows = ItemInitializer.resolveMailItems(List.of(
                new ItemInitializer.MailItemRef(GamePackets.TYPEID_MASCOT, 1, 4, 5)));
        assertEquals(1, rows.size());
        assertEquals(1, rows.get(0).mascotTipo());
        assertEquals(5, rows.get(0).mascotTimeDays());
    }

    @Test
    void initShopAwardUsesBuyTimeForCaddieWhenIffLoaded() {
        if (!JP_IFF.toFile().isFile()) {
            return;
        }
        PangyaIffLoader.reload(JP_IFF);
        int caddieTypeid = 0x1C000002;
        var ctx = new ItemInitializer.InitContext(10, true, false, false);
        var row = ItemInitializer.initShopAward(ctx, caddieTypeid, 1, 7).orElseThrow();
        assertEquals(GamePackets.IFF_GROUP_CADDIE, row.group());
        assertEquals(GamePackets.CADDIE_RENT_HOLIDAY, row.rentFlag());
        assertEquals(7, row.caddiePeriodDays());
    }

    @Test
    void initShopAwardInitializesWarehouseItemWhenIffUnloaded() {
        PangyaIffLoader.reload(null);
        var ctx = new ItemInitializer.InitContext(1, true, false, false);
        var row = ItemInitializer.initShopAward(ctx, GamePackets.TYPEID_SHOP_PANG_ITEM, 2, 0).orElseThrow();
        assertNotNull(row.warehouse());
        assertEquals(2, row.warehouse().c0());
    }

    @Test
    void isShopAwardGroupExcludesSetItems() {
        assertTrue(ItemInitializer.isShopAwardGroup(GamePackets.TYPEID_SHOP_PANG_ITEM));
        assertFalse(ItemInitializer.isShopAwardGroup(0x24200000));
    }

    @Test
    void initBoxAwardInitializesWarehouseRewardWhenIffUnloaded() {
        PangyaIffLoader.reload(null);
        var ctx = new ItemInitializer.InitContext(1, false, false, true);
        var row = ItemInitializer.initBoxAward(ctx, GamePackets.TYPEID_SHOP_PANG_ITEM, 3).orElseThrow();
        assertNotNull(row.warehouse());
        assertEquals(3, row.warehouse().c0());
    }

    @Test
    void boxMailRefMapsWarehouseReward() {
        PangyaIffLoader.reload(null);
        var ctx = new ItemInitializer.InitContext(1, false, false, true);
        var ref = ItemInitializer.boxMailRef(ctx, GamePackets.TYPEID_SHOP_PANG_ITEM, 2).orElseThrow();
        assertEquals(GamePackets.TYPEID_SHOP_PANG_ITEM, ref.typeid());
        assertEquals(2, ref.qntd());
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
