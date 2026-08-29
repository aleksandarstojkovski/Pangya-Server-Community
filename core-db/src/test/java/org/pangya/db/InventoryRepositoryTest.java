package org.pangya.db;

import org.junit.jupiter.api.Test;
import org.pangya.protocol.game.GamePackets;
import org.pangya.protocol.iff.PangyaIffLoader;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
            assertTrue(repo.mascotMessageEnabled(GamePackets.TYPEID_MASCOT));
            assertFalse(repo.mascotMessageEnabled(0));
            repo.changeMascotMessage(10001, repo.mascots(10001).getFirst().id, "ok");
            repo.setPangCookie(10001, 100000, 0);
            PangyaIffLoader.reload(null);
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
            repo.setPangCookie(10001, 100000, 0);
            assertEquals(0, repo.dolfiniLockerPang(10001));
            var deposit = repo.updateDolfiniLockerPang(10001, GamePackets.LOCKER_PANG_DEPOSIT, 1000);
            assertEquals(0, deposit.code());
            assertEquals(99000, deposit.playerPang());
            assertEquals(1000, deposit.lockerPang());
            assertEquals(1000, repo.dolfiniLockerPang(10001));
            assertEquals(99000, repo.pang(10001));
            var withdraw = repo.updateDolfiniLockerPang(10001, GamePackets.LOCKER_PANG_WITHDRAW, 1000);
            assertEquals(0, withdraw.code());
            assertEquals(100000, withdraw.playerPang());
            assertEquals(0, withdraw.lockerPang());
            assertEquals(GamePackets.LOCKER_PANG_DEPOSIT_ERR,
                    repo.updateDolfiniLockerPang(10001, GamePackets.LOCKER_PANG_DEPOSIT, 999999).code());
            assertEquals(GamePackets.LOCKER_PANG_WITHDRAW_ERR,
                    repo.updateDolfiniLockerPang(10001, GamePackets.LOCKER_PANG_WITHDRAW, 1).code());
            repo.setPangCookie(10001, 100000, 0);
            assertTrue(repo.cometRefill(GamePackets.TYPEID_COMET_REFILL).isPresent());
            assertEquals(1, repo.cometRefill(GamePackets.TYPEID_COMET_REFILL).orElseThrow().min());
            assertEquals(30, repo.cometRefill(GamePackets.TYPEID_COMET_REFILL).orElseThrow().max());
            repo.updateTutorial(10001, 0, 0, 0);
            assertEquals(0, repo.tutorial(10001).rookie());
            repo.updateTutorial(10001, 1, 0, 0);
            assertEquals(1, repo.tutorial(10001).rookie());
            repo.updateTutorial(10001, 0, 0, 0);
            assertTrue(repo.attendanceReward(10001).isEmpty());
            try {
                repo.upsertAttendanceReward(10001, new InventoryRepository.AttendanceReward(
                        1, GamePackets.TYPEID_SHOP_PANG_ITEM, 3, 0, 0, java.time.Instant.EPOCH));
                var ari = repo.attendanceReward(10001).orElseThrow();
                assertEquals(1, ari.counter());
                assertEquals(GamePackets.TYPEID_SHOP_PANG_ITEM, ari.nowTypeid());
                assertEquals(3, ari.nowQntd());
            } finally {
                repo.deleteAttendanceReward(10001);
            }
            assertTrue(repo.attendanceReward(10001).isEmpty());
            PangyaIffLoader.reload(null);
            repo.deleteTimeLimitItem(GamePackets.TYPEID_SHOP_PANG_ITEM);
            repo.upsertTimeLimitItem(
                    GamePackets.TYPEID_SHOP_PANG_ITEM,
                    GamePackets.ITEM_BUFF_TIPO_YAM,
                    10,
                    1);
            try {
                assertEquals(1, repo.timeLimitItem(GamePackets.TYPEID_SHOP_PANG_ITEM).orElseThrow().timeMinutes());
                repo.deleteItemBuff(10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
                long idx = repo.insertItemBuff(
                        10001,
                        GamePackets.TYPEID_SHOP_PANG_ITEM,
                        GamePackets.ITEM_BUFF_TIPO_YAM,
                        10,
                        java.time.Instant.EPOCH,
                        java.time.Instant.EPOCH.plusSeconds(60));
                assertTrue(idx > 0);
                assertEquals(GamePackets.ITEM_BUFF_TIPO_YAM,
                        repo.itemBuff(10001, GamePackets.TYPEID_SHOP_PANG_ITEM).orElseThrow().tipo());
            } finally {
                repo.deleteItemBuff(10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
                repo.deleteTimeLimitItem(GamePackets.TYPEID_SHOP_PANG_ITEM);
            }
            int clubId = repo.warehouse(10001).stream()
                    .filter(w -> w.typeid == GamePackets.TYPEID_AIR_KNIGHT)
                    .findFirst()
                    .orElseThrow()
                    .id;
            repo.deleteClubSetIff(GamePackets.TYPEID_AIR_KNIGHT);
            repo.upsertClubSetWorkShopTipo(GamePackets.TYPEID_AIR_KNIGHT, 0);
            try {
                assertEquals(0, repo.clubSetWorkShopTipo(GamePackets.TYPEID_AIR_KNIGHT).orElseThrow());
                repo.setClubSetRecoveryPts(10001, clubId, 5);
                assertEquals(5, repo.warehouse(10001).stream()
                        .filter(w -> w.id == clubId)
                        .findFirst()
                        .orElseThrow()
                        .workshopRecovery);
                repo.setClubSetMasteryPts(10001, clubId, 300);
                assertEquals(300, repo.warehouse(10001).stream()
                        .filter(w -> w.id == clubId)
                        .findFirst()
                        .orElseThrow()
                        .workshopMastery);
            } finally {
                repo.setClubSetRecoveryPts(10001, clubId, 0);
                repo.setClubSetMasteryPts(10001, clubId, 0);
                repo.deleteClubSetIff(GamePackets.TYPEID_AIR_KNIGHT);
            }
            repo.deleteClubSetIff(GamePackets.TYPEID_AIR_KNIGHT);
            repo.upsertClubSetIff(
                    GamePackets.TYPEID_AIR_KNIGHT,
                    0,
                    new short[5],
                    new short[] {1, 0, 0, 0, 0});
            try {
                var iff = repo.clubSetIff(GamePackets.TYPEID_AIR_KNIGHT).orElseThrow();
                assertEquals(1, iff.slots()[0]);
                assertEquals(0, iff.stats()[0]);
                assertEquals(
                        GamePackets.CHAR_STATS_ENCHANT_PANG,
                        repo.enchantPang(GamePackets.enchantTypeid(GamePackets.CHAR_STATS_POWER, 0))
                                .orElseThrow());
                repo.setWarehouseClubC(10001, clubId, new short[] {1, 0, 0, 0, 0});
                assertEquals(1, repo.warehouse(10001).stream()
                        .filter(w -> w.id == clubId)
                        .findFirst()
                        .orElseThrow()
                        .c[0]);
            } finally {
                repo.setWarehouseClubC(10001, clubId, new short[5]);
                repo.deleteClubSetIff(GamePackets.TYPEID_AIR_KNIGHT);
            }
            repo.deleteClubSetIff(GamePackets.TYPEID_AIR_KNIGHT);
            repo.deleteClubSetRankExp(0);
            PangyaIffLoader.reload(null);
            repo.upsertClubSetIff(
                    GamePackets.TYPEID_AIR_KNIGHT,
                    0,
                    new short[5],
                    new short[] {6, 6, 6, 6, 6},
                    0);
            repo.upsertClubSetRankExp(0);
            try {
                assertEquals(0, repo.clubSetIff(GamePackets.TYPEID_AIR_KNIGHT).orElseThrow().tipoRankS());
                assertEquals(0, repo.clubSetIff(GamePackets.TYPEID_AIR_KNIGHT).orElseThrow().totalRecovery());
                assertEquals(0, repo.clubSetIff(GamePackets.TYPEID_AIR_KNIGHT).orElseThrow().flagTransformar());
                assertTrue(repo.clubSetRankExp(0));
                assertArrayEquals(new int[6], repo.clubSetRankExpRanks(0).orElseThrow());
                repo.upsertClubSetRankExp(0, new int[] {0, 50, 0, 0, 0, 0});
                assertEquals(50, repo.clubSetRankExpRanks(0).orElseThrow()[1]);
                repo.setClubSetWorkshop(10001, clubId, new short[] {1, 0, 0, 0, 0}, 2, 1, 5);
                repo.setWarehouseClubC(10001, clubId, new short[] {1, 0, 0, 0, 0});
                repo.resetClubSetWorkshopAndC(10001, clubId);
                var reset = repo.warehouse(10001).stream()
                        .filter(w -> w.id == clubId)
                        .findFirst()
                        .orElseThrow();
                assertEquals(0, reset.c[0]);
                assertEquals(0, reset.workshopC[0]);
                assertEquals(0, reset.workshopLevel);
                assertEquals(0, reset.workshopRank);
                assertEquals(0, reset.workshopRecovery);
            } finally {
                repo.setWarehouseClubC(10001, clubId, new short[5]);
                repo.setClubSetWorkshop(10001, clubId, new short[5], 0, 0, 0);
                repo.deleteClubSetIff(GamePackets.TYPEID_AIR_KNIGHT);
                repo.deleteClubSetRankExp(0);
            }
            repo.deleteItemIff(GamePackets.TYPEID_SHOP_PANG_ITEM);
            repo.upsertItemIff(GamePackets.TYPEID_SHOP_PANG_ITEM);
            PangyaIffLoader.reload(null);
            repo.deleteClubSetLevelUpLimit(0, 0);
            repo.deleteClubSetLevelUpProb(0);
            repo.upsertClubSetLevelUpLimit(0, 0, new short[] {7, 0, 0, 0, 0});
            repo.upsertClubSetLevelUpProb(0, new int[] {100, 0, 0, 0, 0});
            try {
                assertTrue(repo.itemIff(GamePackets.TYPEID_SHOP_PANG_ITEM));
                assertTrue(repo.clubSetLevelUpAny(0));
                assertEquals(7, repo.clubSetLevelUpLimit(0, 0).orElseThrow()[0]);
                assertEquals(100, repo.clubSetLevelUpProb(0).orElseThrow()[0]);
                repo.deleteClubSetOriginal(GamePackets.TYPEID_WINGTROSS_EVO);
                repo.upsertClubSetOriginal(
                        GamePackets.TYPEID_WINGTROSS_EVO,
                        GamePackets.TYPEID_WORKSHOP_TRANSFORM_ORIGINAL,
                        new short[] {7, 7, 7, 7, 7});
                assertTrue(repo.clubSetOriginalAny(GamePackets.TYPEID_WINGTROSS_EVO));
                assertEquals(
                        GamePackets.TYPEID_WORKSHOP_TRANSFORM_ORIGINAL,
                        repo.clubSetOriginals(GamePackets.TYPEID_WINGTROSS_EVO).get(0).typeid());
                assertEquals(1, GamePackets.workshopSCalcRank(
                        repo.clubSetOriginals(GamePackets.TYPEID_WINGTROSS_EVO).get(0).slots()));
                PangyaIffLoader.reload(null);
                repo.deleteCutinIff(GamePackets.TYPEID_CUTIN_SKIN);
                repo.upsertCutinIff(
                        GamePackets.TYPEID_CUTIN_SKIN,
                        2,
                        1,
                        new int[] {10, 11, 12, 13},
                        7,
                        new String[] {"char", "bg", "pattern", "text"});
                var cutin = repo.cutinIff(GamePackets.TYPEID_CUTIN_SKIN).orElseThrow();
                assertEquals(2, cutin.sector());
                assertEquals(1, cutin.condition());
                assertArrayEquals(new int[] {10, 11, 12, 13}, cutin.imageTypes());
                assertArrayEquals(new String[] {"char", "bg", "pattern", "text"}, cutin.sprites());
                repo.deleteBoxMailReward(GamePackets.TYPEID_BOX_MAIL_TEST);
                repo.upsertBoxMailReward(
                        GamePackets.TYPEID_BOX_MAIL_TEST,
                        GamePackets.TYPEID_SHOP_PANG_ITEM,
                        2,
                        GamePackets.TYPEID_BOX_MAIL_OPENED_TEST,
                        "box");
                var box = repo.boxMailReward(GamePackets.TYPEID_BOX_MAIL_TEST).orElseThrow();
                assertEquals(GamePackets.TYPEID_SHOP_PANG_ITEM, box.rewardTypeid());
                assertEquals(2, box.rewardQntd());
                assertEquals(GamePackets.TYPEID_BOX_MAIL_OPENED_TEST, box.openedTypeid());
                repo.deleteCardIff(GamePackets.TYPEID_CARD_SPECIAL_PANG);
                repo.upsertCardSpecialIff(
                        GamePackets.TYPEID_CARD_SPECIAL_PANG,
                        0,
                        100,
                        GamePackets.CARD_EFFECT_PANG,
                        500,
                        0);
                var special = repo.cardSpecialIff(GamePackets.TYPEID_CARD_SPECIAL_PANG).orElseThrow();
                assertEquals(GamePackets.CARD_EFFECT_PANG, special.effect());
                assertEquals(500, special.effectValue());
                repo.deleteCardPackRewards(GamePackets.TYPEID_CARD_PACK_TEST);
                repo.upsertCardPackReward(
                        GamePackets.TYPEID_CARD_PACK_TEST, 0, GamePackets.TYPEID_CARD_PACK_REWARD_1);
                repo.upsertCardPackReward(
                        GamePackets.TYPEID_CARD_PACK_TEST, 1, GamePackets.TYPEID_CARD_PACK_REWARD_2);
                assertEquals(2, repo.cardPackRewards(GamePackets.TYPEID_CARD_PACK_TEST).size());
                assertEquals(
                        GamePackets.TYPEID_CARD_PACK_REWARD_1,
                        repo.cardPackRewards(GamePackets.TYPEID_CARD_PACK_TEST).get(0).cardTypeid());
                repo.deleteMemorialRewards(GamePackets.TYPEID_MEMORIAL_COIN_TEST);
                repo.upsertMemorialReward(
                        GamePackets.TYPEID_MEMORIAL_COIN_TEST,
                        0,
                        2,
                        GamePackets.TYPEID_MEMORIAL_REWARD_TEST,
                        3);
                var memorial = repo.memorialRewards(GamePackets.TYPEID_MEMORIAL_COIN_TEST).get(0);
                assertEquals(2, memorial.rarity());
                assertEquals(GamePackets.TYPEID_MEMORIAL_REWARD_TEST, memorial.rewardTypeid());
                assertEquals(3, memorial.qntd());
                repo.deleteTicketReport(0x1234);
                repo.upsertTicketReport(0x1234, java.time.Instant.EPOCH);
                assertEquals(java.time.Instant.EPOCH, repo.ticketReportDate(0x1234).orElseThrow());
                repo.deleteGrandPrixEvent(GamePackets.TYPEID_GP_EVENT_TEST);
                repo.upsertGrandPrixEvent(
                        GamePackets.TYPEID_GP_EVENT_TEST, "Test GP", 18, 0, 0, 0, 0, 1, 10);
                var gp = repo.grandPrixEvent(GamePackets.TYPEID_GP_EVENT_TEST).orElseThrow();
                assertEquals("Test GP", gp.name());
                assertEquals(18, gp.holes());
                assertEquals(1, gp.minLevel());
                repo.setLegacyTikiPoints(10001, 50);
                assertEquals(50, repo.legacyTikiPoints(10001));
                repo.deleteTikiItemValue(GamePackets.TYPEID_TIKI_VALUE_TEST);
                repo.upsertTikiItemValue(GamePackets.TYPEID_TIKI_VALUE_TEST, 2, 10);
                assertEquals(2, repo.tikiItemValue(GamePackets.TYPEID_TIKI_VALUE_TEST).orElseThrow().itemCount());
                repo.deleteTikiPointShopItem(GamePackets.TYPEID_TIKI_REWARD_TEST);
                repo.upsertTikiPointShopItem(GamePackets.TYPEID_TIKI_REWARD_TEST, 3, 20);
                assertEquals(
                        20,
                        repo.tikiPointShopItem(GamePackets.TYPEID_TIKI_REWARD_TEST).orElseThrow().points());
                repo.deleteTikiItemValue(GamePackets.TYPEID_TIKI_NEW_TEST);
                repo.upsertTikiNewValue(GamePackets.TYPEID_TIKI_NEW_TEST, 100, 600, 0, 0, 0);
                var tikiNew = repo.tikiNewValue(GamePackets.TYPEID_TIKI_NEW_TEST).orElseThrow();
                assertEquals(100, tikiNew.pang());
                assertEquals(600, tikiNew.mileage());
                repo.deleteDailyQuestStuff(GamePackets.TYPEID_DAILY_QUEST_STUFF_TEST);
                repo.deleteDailyQuestRewards(GamePackets.TYPEID_DAILY_ACHIEVEMENT_TEST);
                repo.upsertDailyQuestStuff(
                        GamePackets.TYPEID_DAILY_QUEST_STUFF_TEST,
                        GamePackets.TYPEID_DAILY_COUNTER_TEST);
                repo.upsertDailyQuestReward(
                        GamePackets.TYPEID_DAILY_ACHIEVEMENT_TEST,
                        0,
                        GamePackets.TYPEID_DAILY_REWARD_TEST,
                        2,
                        0);
                assertEquals(
                        GamePackets.TYPEID_DAILY_REWARD_TEST,
                        repo.dailyQuestRewards(GamePackets.TYPEID_DAILY_ACHIEVEMENT_TEST)
                                .get(0)
                                .typeid());
                repo.deleteCardByTypeid(10001, GamePackets.TYPEID_CARD_NORMAL);
                repo.addCard(10001, GamePackets.TYPEID_CARD_NORMAL, 2);
                assertEquals(1, repo.consumeCardByTypeid(10001, GamePackets.TYPEID_CARD_NORMAL, 1).orElseThrow());
                assertEquals(0, repo.consumeCardByTypeid(10001, GamePackets.TYPEID_CARD_NORMAL, 1).orElseThrow());
                assertTrue(repo.consumeCardByTypeid(10001, GamePackets.TYPEID_CARD_NORMAL, 1).isEmpty());
            } finally {
                repo.deleteItemIff(GamePackets.TYPEID_SHOP_PANG_ITEM);
                repo.deleteClubSetLevelUpLimit(0, 0);
                repo.deleteClubSetLevelUpProb(0);
                repo.deleteClubSetOriginal(GamePackets.TYPEID_WINGTROSS_EVO);
                repo.deleteCutinIff(GamePackets.TYPEID_CUTIN_SKIN);
                repo.deleteBoxMailReward(GamePackets.TYPEID_BOX_MAIL_TEST);
                repo.deleteCardIff(GamePackets.TYPEID_CARD_SPECIAL_PANG);
                repo.deleteCardPackRewards(GamePackets.TYPEID_CARD_PACK_TEST);
                repo.deleteMemorialRewards(GamePackets.TYPEID_MEMORIAL_COIN_TEST);
                repo.deleteTicketReport(0x1234);
                repo.deleteGrandPrixEvent(GamePackets.TYPEID_GP_EVENT_TEST);
                repo.setLegacyTikiPoints(10001, 0);
                repo.deleteTikiItemValue(GamePackets.TYPEID_TIKI_VALUE_TEST);
                repo.deleteTikiPointShopItem(GamePackets.TYPEID_TIKI_REWARD_TEST);
                repo.deleteTikiItemValue(GamePackets.TYPEID_TIKI_NEW_TEST);
                repo.deleteDailyQuestStuff(GamePackets.TYPEID_DAILY_QUEST_STUFF_TEST);
                repo.deleteDailyQuestRewards(GamePackets.TYPEID_DAILY_ACHIEVEMENT_TEST);
                repo.deleteCardByTypeid(10001, GamePackets.TYPEID_CARD_NORMAL);
            }
            repo.deletePartIff(GamePackets.TYPEID_RENTAL_PART);
            repo.upsertPartValorRental(GamePackets.TYPEID_RENTAL_PART, 100);
            try {
                assertEquals(100, repo.partValorRental(GamePackets.TYPEID_RENTAL_PART).orElseThrow());
                repo.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_RENTAL_PART);
                int rentalId = repo.addWarehouseItem(10001, GamePackets.TYPEID_RENTAL_PART, 1);
                repo.setWarehouseEndDate(10001, rentalId, java.time.Instant.EPOCH.plusSeconds(60));
                assertTrue(repo.deleteWarehouseById(10001, rentalId));
                assertTrue(repo.warehouse(10001).stream().noneMatch(w -> w.id == rentalId));
            } finally {
                repo.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_RENTAL_PART);
                repo.deletePartIff(GamePackets.TYPEID_RENTAL_PART);
            }
            int partTypeid = (GamePackets.IFF_GROUP_PART << 26) | 0x99;
            repo.deleteWarehouseByTypeid(10001, partTypeid);
            int partId = repo.addWarehouseItem(10001, partTypeid, 1);
            try {
                assertTrue(repo.addDolfiniLockerItem(10001, partId).isPresent());
                assertTrue(repo.warehouse(10001).stream().noneMatch(w -> w.id == partId));
                long idx = repo.dolfiniLockerIndex(10001, partId).orElseThrow();
                assertTrue(repo.removeDolfiniLockerItem(10001, idx, partId).isPresent());
                assertTrue(repo.warehouse(10001).stream().anyMatch(w -> w.id == partId));
            } finally {
                repo.deleteDolfiniLockerByItemId(10001, partId);
                repo.deleteWarehouseByTypeid(10001, partTypeid);
            }
        }
    }

    @Test
    void reconcileEquipFixesInvalidReferences() {
        String url = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        DatabaseSupport.migrate(url, user, password);

        try (var ds = DatabaseSupport.dataSource(url, user, password)) {
            var jdbi = DatabaseSupport.jdbi(ds);
            InventoryRepository repo = new JdbiInventoryRepository(jdbi);
            int charId = repo.characters(10001).getFirst().id;
            int clubId = repo.warehouse(10001).stream()
                    .filter(w -> w.typeid == GamePackets.TYPEID_AIR_KNIGHT)
                    .findFirst()
                    .orElseThrow()
                    .id;
            try {
                jdbi.useHandle(h -> h.createUpdate("""
                                UPDATE pangya.pangya_user_equip
                                   SET character_id = 99999,
                                       club_id = 99998,
                                       ball_type = 1,
                                       item_slot_1 = 0x7FFF0001,
                                       caddie_id = 99996,
                                       mascot_id = 99995
                                 WHERE "UID" = 10001
                                """)
                        .execute());
                GamePackets.UserEquip fixed = repo.reconcileEquipAtLogin(10001);
                assertEquals(charId, fixed.characterId);
                assertEquals(clubId, fixed.clubsetId);
                assertEquals(GamePackets.TYPEID_DEFAULT_BALL, fixed.ballTypeid);
                assertEquals(0, fixed.caddieId);
                assertEquals(0, fixed.mascotId);
                assertEquals(0, fixed.itemSlot[0]);
                assertEquals(charId, repo.userEquip(10001).characterId);
            } finally {
                repo.equipCharacter(10001, charId);
                repo.equipBallAndClub(10001, GamePackets.TYPEID_DEFAULT_BALL, clubId);
                repo.equipCaddie(10001, 0);
                repo.equipMascot(10001, 0);
            }
        }
    }

    @Test
    void reconcileEquipAddsDefaultsWhenMissing() {
        String url = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        DatabaseSupport.migrate(url, user, password);

        try (var ds = DatabaseSupport.dataSource(url, user, password)) {
            var jdbi = DatabaseSupport.jdbi(ds);
            InventoryRepository repo = new JdbiInventoryRepository(jdbi);
            long uid = 10002L;
            try {
                jdbi.useHandle(h -> {
                    h.createUpdate("DELETE FROM pangya.pangya_character_information WHERE \"UID\" = :uid")
                            .bind("uid", uid)
                            .execute();
                    h.createUpdate("""
                                    DELETE FROM pangya.pangya_item_warehouse
                                     WHERE "UID" = :uid
                                       AND (typeid = :club OR typeid = :ball)
                                    """)
                            .bind("uid", uid)
                            .bind("club", GamePackets.TYPEID_AIR_KNIGHT)
                            .bind("ball", GamePackets.TYPEID_DEFAULT_BALL)
                            .execute();
                    h.createUpdate("""
                                    UPDATE pangya.pangya_user_equip
                                       SET character_id = 0, club_id = 0, ball_type = 0
                                     WHERE "UID" = :uid
                                    """)
                            .bind("uid", uid)
                            .execute();
                });
                GamePackets.UserEquip fixed = repo.reconcileEquipAtLogin(uid);
                assertFalse(repo.characters(uid).isEmpty());
                assertEquals(GamePackets.TYPEID_NURI, repo.characters(uid).getFirst().typeid);
                assertEquals(134218752, repo.characters(uid).getFirst().partsTypeid[0]);
                assertTrue(repo.warehouse(uid).stream()
                        .anyMatch(w -> w.typeid == GamePackets.TYPEID_AIR_KNIGHT));
                assertTrue(repo.warehouse(uid).stream()
                        .anyMatch(w -> w.typeid == GamePackets.TYPEID_DEFAULT_BALL));
                assertEquals(repo.characters(uid).getFirst().id, fixed.characterId);
                assertTrue(fixed.clubsetId > 0);
                assertEquals(GamePackets.TYPEID_DEFAULT_BALL, fixed.ballTypeid);
            } finally {
                DatabaseSupport.migrate(url, user, password);
            }
        }
    }

    private static String env(String name, String fallback) {
        String v = System.getenv(name);
        return (v == null || v.isBlank()) ? fallback : v;
    }
}
