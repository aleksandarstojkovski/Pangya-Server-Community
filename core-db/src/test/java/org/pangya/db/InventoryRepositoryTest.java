package org.pangya.db;

import org.junit.jupiter.api.Test;
import org.pangya.protocol.game.GamePackets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryRepositoryTest {

    @Test
    void loadsStarterNuriAndAirKnight() {
        String url = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        DatabaseSupport.migrate(url, user, password);

        try (var ds = DatabaseSupport.dataSource(url, user, password)) {
            InventoryRepository repo = new JdbiInventoryRepository(DatabaseSupport.jdbi(ds));
            var chars = repo.characters(10001);
            assertEquals(1, chars.size());
            assertEquals(GamePackets.TYPEID_NURI, chars.getFirst().typeid);
            var warehouse = repo.warehouse(10001);
            assertFalse(warehouse.isEmpty());
            assertEquals(GamePackets.TYPEID_AIR_KNIGHT, warehouse.getFirst().typeid);
            GamePackets.UserEquip equip = repo.userEquip(10001);
            assertEquals(chars.getFirst().id, equip.characterId);
            assertEquals(warehouse.getFirst().id, equip.clubsetId);
            assertEquals(1, repo.caddies(10001).size());
            assertEquals(GamePackets.TYPEID_CADDIE_PAPEL, repo.caddies(10001).getFirst().typeid);
            assertEquals(GamePackets.CADDIE_RENT_HOLIDAY, repo.caddies(10001).getFirst().rentFlag);
            assertEquals(1, repo.mascots(10001).size());
            assertEquals(GamePackets.TYPEID_MASCOT, repo.mascots(10001).getFirst().typeid);
            assertTrue(repo.cards(10001).isEmpty());
            repo.equipCharacter(10001, 1);
            repo.equipCaddie(10001, 0);
            repo.equipBallAndClub(10001, GamePackets.TYPEID_DEFAULT_BALL, 2);
            repo.equipMascot(10001, 0);
            GamePackets.UserEquip after = repo.userEquip(10001);
            assertEquals(1, after.characterId);
            assertEquals(0, after.caddieId);
            assertEquals(2, after.clubsetId);
            assertEquals(GamePackets.TYPEID_DEFAULT_BALL, after.ballTypeid);
            GamePackets.CharacterInfo parts = chars.getFirst();
            parts.defaultHair = 3;
            repo.updateCharacterParts(10001, parts);
            assertEquals(3, repo.characters(10001).getFirst().defaultHair);
            parts.defaultHair = 0;
            repo.updateCharacterParts(10001, parts);
            assertTrue(repo.counters(10001).isEmpty());
            assertTrue(repo.achievements(10001).isEmpty());
            assertEquals(1, repo.characters(10002).size());
            assertTrue(repo.shopItem(GamePackets.TYPEID_SHOP_PANG_ITEM).isPresent());
            assertEquals(GamePackets.SHOP_PANG_PRICE, repo.shopItem(GamePackets.TYPEID_SHOP_PANG_ITEM).orElseThrow().pangPrice());
            repo.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
            repo.setPangCookie(10001, 100000, 0);
            var bought = repo.buyShopItem(
                    10001, GamePackets.TYPEID_SHOP_PANG_ITEM, 1, GamePackets.SHOP_PANG_PRICE, 0);
            assertEquals(0, bought.code());
            assertEquals(99900, bought.pang());
            assertTrue(repo.warehouse(10001).stream().anyMatch(w -> w.typeid == GamePackets.TYPEID_SHOP_PANG_ITEM));
            var missing = repo.buyShopItem(10001, 0x7FFF0001, 1, 1, 0);
            assertEquals(GamePackets.BUY_FAIL_NOT_BUYABLE, missing.code());
            repo.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
            repo.setPangCookie(10001, 100000, 0);
            var gifted = repo.giftShopItem(
                    10001, GamePackets.TYPEID_SHOP_PANG_ITEM, 1, GamePackets.SHOP_PANG_PRICE, 0);
            assertEquals(0, gifted.code());
            assertEquals(99900, gifted.pang());
            assertFalse(repo.warehouse(10001).stream().anyMatch(w -> w.typeid == GamePackets.TYPEID_SHOP_PANG_ITEM));
            var giftMissing = repo.giftShopItem(10001, 0x7FFF0001, 1, 1, 0);
            assertEquals(GamePackets.BUY_FAIL_NOT_BUYABLE, giftMissing.code());
            repo.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
            repo.deleteWarehouseByTypeid(10002, GamePackets.TYPEID_SHOP_PANG_ITEM);
            repo.setPangCookie(10001, 100000, 0);
            repo.setPangCookie(10002, 100000, 0);
            var stock = repo.buyShopItem(
                    10001, GamePackets.TYPEID_SHOP_PANG_ITEM, 1, GamePackets.SHOP_PANG_PRICE, 0);
            assertEquals(0, stock.code());
            repo.setPangCookie(10001, 100000, 0);
            var moved = repo.transferPersonalShop(
                    10001, 10002, stock.itemId(), GamePackets.TYPEID_SHOP_PANG_ITEM, 1, 1000);
            assertEquals(950, moved.sellerGain());
            assertEquals(100950, moved.sellerPangAfter());
            assertEquals(99000, moved.buyerPangAfter());
            assertEquals(1, moved.sellerPacket().c[0]);
            assertEquals(1, moved.buyerPacket().c[0]);
            assertEquals(100950, repo.pang(10001));
            assertEquals(99000, repo.pang(10002));
            assertFalse(repo.warehouse(10001).stream()
                    .anyMatch(w -> w.typeid == GamePackets.TYPEID_SHOP_PANG_ITEM));
            assertTrue(repo.warehouse(10002).stream()
                    .anyMatch(w -> w.typeid == GamePackets.TYPEID_SHOP_PANG_ITEM));
            repo.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
            repo.deleteWarehouseByTypeid(10002, GamePackets.TYPEID_SHOP_PANG_ITEM);
            repo.setPangCookie(10001, 0, 0);
            var papelFunds = repo.playPapel(10001, false);
            assertEquals(GamePackets.PAPEL_PLAY_ERR_FUNDS, papelFunds.code());
            repo.setPangCookie(10001, 100000, 0);
            var papel = repo.playPapel(10001, false);
            assertEquals(0, papel.code());
            assertTrue(papel.balls().size() >= GamePackets.PAPEL_MIN_BALL);
            assertTrue(papel.balls().size() <= GamePackets.PAPEL_MAX_BALL);
            assertEquals(100000 - GamePackets.PAPEL_PRICE_NORMAL, papel.pang());
            assertEquals(1, papel.awards().size());
            assertEquals(GamePackets.TYPEID_SHOP_PANG_ITEM, papel.awards().getFirst().typeid());
            var big = repo.playPapel(10001, true);
            assertEquals(0, big.code());
            assertEquals(GamePackets.PAPEL_BIG_BALLS, big.balls().size());
            assertEquals(100000 - GamePackets.PAPEL_PRICE_NORMAL - GamePackets.PAPEL_PRICE_BIG, big.pang());
            repo.setLevel(10001, GamePackets.GIFT_MIN_LEVEL);
            repo.setLevel(10001, 1);
            repo.setPangCookie(10001, 100000, 0);
            var holiday = repo.payCaddieHoliday(10001, repo.caddies(10001).getFirst().id);
            assertEquals(0, holiday.code());
            assertEquals(100000 - GamePackets.CADDIE_HOLIDAY_PANG, holiday.pang());
            assertEquals(1, repo.payCaddieHoliday(10001, 0).code());
            repo.setPangCookie(10001, 100000, 0);
            var mascot = repo.changeMascotMessage(10001, repo.mascots(10001).getFirst().id, "hello");
            assertEquals(0, mascot.code());
            assertEquals("hello", mascot.message());
            assertEquals("hello", repo.mascots(10001).getFirst().message);
            assertEquals(1, repo.changeMascotMessage(10001, 0, "hello").code());
            repo.changeMascotMessage(10001, repo.mascots(10001).getFirst().id, "ok");
            repo.setPangCookie(10001, 100000, 0);
            repo.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
            var stockCadie = repo.buyShopItem(
                    10001, GamePackets.TYPEID_SHOP_PANG_ITEM, 1, GamePackets.SHOP_PANG_PRICE, 0);
            assertEquals(0, stockCadie.code());
            var cadie = repo.cadieExchange(
                    10001,
                    0,
                    1,
                    1,
                    new int[] {GamePackets.TYPEID_SHOP_PANG_ITEM},
                    new int[] {stockCadie.itemId()});
            assertEquals(0, cadie.code());
            assertEquals(GamePackets.TYPEID_SHOP_PANG_ITEM, cadie.receiveTypeid());
            assertEquals(1, cadie.receiveQntd());
            repo.setPangCookie(10001, 100000, 0);
            repo.setPangCookie(10002, 100000, 0);
            repo.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
            repo.deleteWarehouseByTypeid(10002, GamePackets.TYPEID_SHOP_PANG_ITEM);
            var loloMiss = repo.loloCompose(10001, 0, 0, 0, 0);
            assertEquals(GamePackets.LOLO_ERR_IFF, loloMiss.code());
            int card = GamePackets.TYPEID_CARD_NORMAL;
            repo.addCard(10001, card, GamePackets.LOLO_CARD_COUNT);
            repo.setPangCookie(10001, 100000, 0);
            var lolo = repo.loloCompose(10001, 3L * GamePackets.LOLO_PANG_NORMAL, card, card, card);
            assertEquals(0, lolo.code());
            assertEquals(100000 - 3L * GamePackets.LOLO_PANG_NORMAL, lolo.pangAfter());
            assertEquals(3L * GamePackets.LOLO_PANG_NORMAL, lolo.pangSpent());
            assertEquals(GamePackets.CARD_TYPE_NORMAL, lolo.cardTipo());
            assertEquals(card, lolo.cardTypeid());
            assertEquals(2, lolo.awards().size());
            assertEquals(1, repo.cards(10001).getFirst().qntd);
            repo.deleteCardByTypeid(10001, card);
            assertTrue(repo.cards(10001).isEmpty());
            repo.setPangCookie(10001, 100000, 0);
            repo.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
            var stockMastery = repo.buyShopItem(
                    10001, GamePackets.TYPEID_SHOP_PANG_ITEM, 1, GamePackets.SHOP_PANG_PRICE, 0);
            assertEquals(0, stockMastery.code());
            var nuri = repo.characters(10001).getFirst();
            var masteryMiss = repo.expandCharacterMastery(10001, 0, 0, 1);
            assertEquals(GamePackets.CHAR_MASTERY_ERR_CHAR, masteryMiss.code());
            var mastery = repo.expandCharacterMastery(10001, nuri.typeid, nuri.id, 1);
            assertEquals(0, mastery.code());
            assertEquals(1, mastery.mastery());
            assertEquals(2, mastery.awards().size());
            assertEquals(GamePackets.PAPEL_AWARD_TYPE, mastery.awards().getFirst().type());
            assertEquals(-1, mastery.awards().getFirst().qntd());
            assertEquals(GamePackets.CHAR_MASTERY_AWARD_TYPE, mastery.awards().get(1).type());
            assertEquals(1, mastery.awards().get(1).extra());
            assertEquals(1, repo.characters(10001).getFirst().mastery);
            nuri.mastery = 0;
            repo.updateCharacterParts(10001, nuri);
            assertEquals(0, repo.characters(10001).getFirst().mastery);
            repo.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
            repo.setPangCookie(10001, 100000, 0);
            var statsMiss = repo.characterStatsUp(10001, 0, new GamePackets.CharacterInfo(), 1);
            assertEquals(GamePackets.CHAR_STATS_UP_ERR_CHAR, statsMiss.code());
            var statsUp = repo.characterStatsUp(10001, 0, nuri, 1);
            assertEquals(0, statsUp.code());
            assertEquals(1, statsUp.pcl()[0] & 0xff);
            assertEquals(100000 - GamePackets.CHAR_STATS_ENCHANT_PANG, statsUp.pangAfter());
            assertEquals(1, repo.characters(10001).getFirst().pcl[0] & 0xff);
            var statsDown = repo.characterStatsDown(10001, 0, nuri);
            assertEquals(0, statsDown.code());
            assertEquals(0, statsDown.pcl()[0] & 0xff);
            assertEquals(0, repo.characters(10001).getFirst().pcl[0] & 0xff);
            repo.setPangCookie(10001, 100000, 0);
            int cardId = repo.addCard(10001, GamePackets.TYPEID_CARD_NORMAL, 1);
            var cardMiss = repo.characterCardEquip(10001, 0, 0, 0, 0, 1);
            assertEquals(GamePackets.CHAR_CARD_ERR_IFF, cardMiss.code());
            var slotPart = repo.characterCardEquip(
                    10001, nuri.typeid, nuri.id, GamePackets.TYPEID_CARD_NORMAL, cardId, 7);
            assertEquals(GamePackets.CHAR_CARD_ERR_PART_SLOT, slotPart.code());
            var slotSub = repo.characterCardEquip(
                    10001, nuri.typeid, nuri.id, GamePackets.TYPEID_CARD_NORMAL, cardId, 5);
            assertEquals(GamePackets.CHAR_CARD_ERR_SUB, slotSub.code());
            var cardEq = repo.characterCardEquip(
                    10001, nuri.typeid, nuri.id, GamePackets.TYPEID_CARD_NORMAL, cardId, GamePackets.CHAR_CARD_SLOT);
            assertEquals(0, cardEq.code());
            assertEquals(2, cardEq.awards().size());
            assertEquals(GamePackets.CHAR_CARD_AWARD_TYPE, cardEq.awards().get(1).type());
            int extraCard = repo.addCard(10001, GamePackets.TYPEID_CARD_NORMAL, 1);
            var occupied = repo.characterCardEquip(
                    10001, nuri.typeid, nuri.id, GamePackets.TYPEID_CARD_NORMAL, extraCard, GamePackets.CHAR_CARD_SLOT);
            assertEquals(GamePackets.CHAR_CARD_ERR_OCCUPIED, occupied.code());
            repo.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_CLUB_PATCHER);
            var patcherMiss = repo.characterCardEquipWithPatcher(
                    10001, nuri.typeid, nuri.id, GamePackets.TYPEID_CARD_NORMAL, extraCard,
                    GamePackets.CHAR_CARD_PATCHER_SLOT);
            assertEquals(GamePackets.CHAR_CARD_PATCHER_ERR, patcherMiss.code());
            repo.addWarehouseItem(10001, GamePackets.TYPEID_CLUB_PATCHER, 1);
            var patcherSlot = repo.characterCardEquipWithPatcher(
                    10001, nuri.typeid, nuri.id, GamePackets.TYPEID_CARD_NORMAL, extraCard, 1);
            assertEquals(GamePackets.CHAR_CARD_PATCHER_ERR_SLOT, patcherSlot.code());
            var patcherSub = repo.characterCardEquipWithPatcher(
                    10001, nuri.typeid, nuri.id, GamePackets.TYPEID_CARD_NORMAL, extraCard, 8);
            assertEquals(GamePackets.CHAR_CARD_PATCHER_ERR_SUB, patcherSub.code());
            var patcherOk = repo.characterCardEquipWithPatcher(
                    10001, nuri.typeid, nuri.id, GamePackets.TYPEID_CARD_NORMAL, extraCard,
                    GamePackets.CHAR_CARD_PATCHER_SLOT);
            assertEquals(0, patcherOk.code());
            assertEquals(3, patcherOk.awards().size());
            assertEquals(GamePackets.TYPEID_CLUB_PATCHER, patcherOk.awards().getFirst().typeid());
            var remover = repo.buyShopItem(
                    10001, GamePackets.TYPEID_SHOP_PANG_ITEM, 2, GamePackets.SHOP_PANG_PRICE * 2, 0);
            assertEquals(0, remover.code());
            var cardRm = repo.characterRemoveCard(
                    10001, nuri.typeid, nuri.id, GamePackets.TYPEID_SHOP_PANG_ITEM, remover.itemId(),
                    GamePackets.CHAR_CARD_SLOT);
            assertEquals(0, cardRm.code());
            assertEquals(GamePackets.TYPEID_CARD_NORMAL, cardRm.cardTypeid());
            var patcherRm = repo.characterRemoveCard(
                    10001, nuri.typeid, nuri.id, GamePackets.TYPEID_SHOP_PANG_ITEM, remover.itemId(),
                    GamePackets.CHAR_CARD_PATCHER_SLOT);
            assertEquals(0, patcherRm.code());
            repo.deleteCardByTypeid(10001, GamePackets.TYPEID_CARD_NORMAL);
            repo.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
            repo.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_CLUB_PATCHER);
            repo.setPangCookie(10001, 100000, 0);
            repo.addWarehouseItem(10001, GamePackets.TYPEID_SHOP_PANG_ITEM, 2);
            assertEquals(1, repo.consumeWarehouseByTypeid(10001, GamePackets.TYPEID_SHOP_PANG_ITEM, 1).orElseThrow());
            assertEquals(0, repo.consumeWarehouseByTypeid(10001, GamePackets.TYPEID_SHOP_PANG_ITEM, 1).orElseThrow());
            assertTrue(repo.consumeWarehouseByTypeid(10001, GamePackets.TYPEID_SHOP_PANG_ITEM, 1).isEmpty());
            repo.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
        }
    }

    private static String env(String name, String fallback) {
        String v = System.getenv(name);
        return (v == null || v.isBlank()) ? fallback : v;
    }
}
