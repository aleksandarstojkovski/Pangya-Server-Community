package org.pangya.game.catalog;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.pangya.db.InventoryRepository.CardEquipRow;
import org.pangya.db.InventoryRepository.ItemBuffRow;
import org.pangya.protocol.game.GamePackets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerRateResolverTest {

    @Test
    void cSharpIndexOfNotLastExcludesLastArrayEntry() {
        assertTrue(PlayerRateResolver.cSharpIndexOfNotLast(
                new int[] {0x1A000001, 0x1A000002, 0x1A0000AE}, 0x1A000001));
        assertTrue(PlayerRateResolver.cSharpIndexOfNotLast(
                new int[] {0x1A000001, 0x1A000002, 0x1A0000AE}, 0x1A000002));
        assertFalse(PlayerRateResolver.cSharpIndexOfNotLast(
                new int[] {0x1A000001, 0x1A000002, 0x1A0000AE}, 0x1A0000AE));
        assertTrue(PlayerRateResolver.cSharpIndexOfNotLast(
                new int[] {0x1A000001, 0x1A000002, 0x1A0000AE}, 0x1A000040));
    }

    @Test
    void passivePangX2LastEntryExcludedFromX2Boost() {
        var onlyLast = PlayerRateResolver.compute(
                List.of(), List.of(), Set.of(0x1A0000AE), null, 0);
        var withFirst = PlayerRateResolver.compute(
                List.of(), List.of(), Set.of(0x1A000001), null, 0);
        assertTrue(withFirst.pang() > onlyLast.pang());
        assertEquals(100 + 510, onlyLast.pang());
    }

    @Test
    void kurafaitoRingAddsClubRate() {
        GamePackets.CharacterInfo character = new GamePackets.CharacterInfo();
        character.auxparts = new int[] {PlayerRateResolver.KURAFAITO_RING};
        var rates = PlayerRateResolver.compute(List.of(), List.of(), Set.of(), character, 0);
        assertEquals(110, rates.club());
    }

    @Test
    void npcCardAddsExpWhenEquippedOnCharacter() {
        GamePackets.CharacterInfo character = new GamePackets.CharacterInfo();
        character.id = 7;
        character.typeid = 0x04000001;
        CardEquipRow card = new CardEquipRow(7, 0x04000001, 0x7D400001, 2, 15, 5);
        var rates = PlayerRateResolver.compute(List.of(), List.of(card), Set.of(), character, 0);
        assertEquals(115, rates.exp());
    }

    @Test
    void itemBuffYamAddsExpFromPercent() {
        ItemBuffRow buff = new ItemBuffRow(
                1, GamePackets.TYPEID_SHOP_PANG_ITEM, Instant.EPOCH, Instant.EPOCH,
                GamePackets.ITEM_BUFF_TIPO_YAM, 25, 1);
        var rates = PlayerRateResolver.compute(List.of(buff), List.of(), Set.of(), null, 0);
        assertEquals(125, rates.exp());
    }
}
