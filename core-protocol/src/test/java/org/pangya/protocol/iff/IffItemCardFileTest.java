package org.pangya.protocol.iff;

import org.junit.jupiter.api.Test;
import org.pangya.protocol.game.GamePackets;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IffItemCardFileTest {

    private static final Path JP_IFF = Path.of(
            "reference/pangya-server-community/Server/JP/GameServer/data/pangya_jp.iff");

    @Test
    void loadsShopPangItemFromItemIff() throws Exception {
        assumeReferenceIffPresent();
        PangyaIffLoader.reload(JP_IFF);
        assertTrue(PangyaIffLoader.itemIndex().contains(GamePackets.TYPEID_SHOP_PANG_ITEM));
        assertFalse(IffGroups.isItemEquipable(GamePackets.TYPEID_SHOP_PANG_ITEM));
    }

    @Test
    void deleteActiveItemFlagsMatchCSharp() throws Exception {
        assumeReferenceIffPresent();
        PangyaIffLoader.reload(JP_IFF);
        assertFalse(PangyaIffLoader.canDeleteActiveItem(GamePackets.TYPEID_SHOP_PANG_ITEM).orElseThrow());
        assertTrue(PangyaIffLoader.canDeleteActiveItem(GamePackets.TYPEID_PASSIVE_GIFT_ITEM).orElseThrow());
    }

    @Test
    void loadsCardsFromCardIff() throws Exception {
        assumeReferenceIffPresent();
        IffTypeIndex cards = IffCardFile.loadIndex(new PangyaIffArchive(JP_IFF));
        assertTrue(cards.size() > 200);
    }

    @Test
    void partIndexIncludesValorRental() throws Exception {
        assumeReferenceIffPresent();
        IffPartIndex parts = IffPartFile.loadIndex(new PangyaIffArchive(JP_IFF));
        assertTrue(parts.valorRental(0x8000802).orElse(0) > 0);
    }

    private static void assumeReferenceIffPresent() {
        assertTrue(JP_IFF.toFile().isFile(), () -> "missing " + JP_IFF);
    }
}
