package org.pangya.db;

import org.pangya.protocol.game.GamePackets;

import java.util.List;
import java.util.Optional;

/** Game inventory replacing C# {@code CmdWarehouseItem} / {@code CmdCharacterInfo} / {@code CmdCaddieInfo}. */
public interface InventoryRepository {

    List<GamePackets.WarehouseItem> warehouse(long uid);

    List<GamePackets.CharacterInfo> characters(long uid);

    List<GamePackets.CaddieInfo> caddies(long uid);

    GamePackets.UserEquip userEquip(long uid);

    List<GamePackets.MascotInfo> mascots(long uid);

    List<GamePackets.CardInfo> cards(long uid);

    long pang(long uid);

    long cookie(long uid);

    void equipCharacter(long uid, int characterId);

    void equipCaddie(long uid, int caddieId);

    void equipBallAndClub(long uid, int ballTypeid, int clubsetId);

    void equipMascot(long uid, int mascotId);

    void updateCharacterParts(long uid, GamePackets.CharacterInfo character);

    List<GamePackets.CounterItem> counters(long uid);

    List<GamePackets.AchievementInfo> achievements(long uid);

    Optional<ShopItem> shopItem(int typeid);

    ShopBuyResult buyShopItem(long uid, int typeid, int qntd, int clientPang, int clientCookie);

    /**
     * C# gift {@code consomeMoeda} without adding warehouse to the sender.
     * Catalog miss is {@link GamePackets#BUY_FAIL_NOT_BUYABLE}.
     */
    ShopBuyResult giftShopItem(long uid, int typeid, int qntd, int clientPang, int clientCookie);

    /**
     * C# {@code ItemManager.transferItem} for IFF ITEM: move warehouse C0 and pang
     * (seller gets {@link GamePackets#shopSellerGain}).
     */
    PersonalShopMove transferPersonalShop(
            long sellerUid, long buyerUid, int itemId, int typeid, int qntd, long unitPang);

    void setPangCookie(long uid, long pang, long cookie);

    void setLevel(long uid, int level);

    void deleteWarehouseByTypeid(long uid, int typeid);

    /**
     * C# {@code PapelShopSystem.dropBalls} / {@code dropBigBall} then warehouse
     * add + pang charge. Empty catalog is {@link GamePackets#PAPEL_PLAY_ERR_BALLS}.
     */
    PapelPlayResult playPapel(long uid, boolean big);

    /**
     * C# {@code requestPayCaddieHolyDay}: SQL {@code iff_caddie.valor_mensal}
     * stand-in. Fail codes are swallowed on the wire as {@code 0x93} u8 1.
     */
    CaddieHolidayResult payCaddieHoliday(long uid, int caddieId);

    /**
     * C# {@code requestChangeMascotMessage}: SQL {@code iff_mascot} stand-in.
     */
    MascotMessageResult changeMascotMessage(long uid, int mascotId, String message);

    /**
     * C# {@code requestCadieCauldronExchange}: SQL {@code cadie_magic_box}
     * stand-in for IFF {@code CadieMagicBox}. {@code seq} is the client ushort;
     * lookup uses {@code seq + 1}.
     */
    CadieExchangeResult cadieExchange(long uid, int seq, int requested, int level, int[] typeids, int[] ids);

    int addCard(long uid, int typeid, int qntd);

    void deleteCardByTypeid(long uid, int typeid);

    /**
     * C# {@code requestLoloCardCompose}: SQL {@code iff_card} stand-in for
     * IFF Card + {@code CardSystem.drawsLoloCardCompose}.
     */
    LoloComposeResult loloCompose(long uid, long clientPang, int t0, int t1, int t2);

    /**
     * C# {@code requestCharacterMasteryExpand}: SQL {@code iff_character_mastery}
     * stand-in. {@code level} is {@code PlayerInfo.level}.
     */
    CharMasteryResult expandCharacterMastery(long uid, int typeid, int id, int level);

    /**
     * C# {@code requestCharacterStatsUp}: SQL {@code iff_character} /
     * {@code iff_enchant} / {@code iff_character_mastery} stand-ins.
     * {@code level} is {@code PlayerInfo.level}.
     */
    CharStatsResult characterStatsUp(long uid, int stat, GamePackets.CharacterInfo client, int level);

    /**
     * C# {@code requestCharacterStatsDown}: SQL {@code iff_character} stand-in.
     */
    CharStatsResult characterStatsDown(long uid, int stat, GamePackets.CharacterInfo client);

    record ShopItem(int typeid, int pangPrice, int cookiePrice, boolean canOverlap) {}

    record ShopBuyResult(
            int code,
            int itemId,
            int typeid,
            int qntdDep,
            long pang,
            long cookie,
            long pangSpent,
            long cookieSpent) {

        public static ShopBuyResult fail(int code) {
            return new ShopBuyResult(code, 0, 0, 0, 0, 0, 0, 0);
        }
    }

    record PersonalShopMove(
            GamePackets.WarehouseItem sellerPacket,
            GamePackets.WarehouseItem buyerPacket,
            long sellerPangAfter,
            long buyerPangAfter,
            long sellerGain) {}

    record PapelPlayResult(
            int code,
            List<GamePackets.PapelBall> balls,
            List<GamePackets.PapelAward> awards,
            long pang,
            long cookie) {

        public static PapelPlayResult fail(int code) {
            return new PapelPlayResult(code, List.of(), List.of(), 0, 0);
        }
    }

    record CaddieHolidayResult(int code, int caddieId, long pang) {

        public static CaddieHolidayResult fail(long pang) {
            return new CaddieHolidayResult(1, 0, pang);
        }
    }

    record MascotMessageResult(int code, int mascotId, String message, long pang) {

        public static MascotMessageResult fail(long pang) {
            return new MascotMessageResult(1, -1, "", pang);
        }
    }

    record CadieExchangeResult(
            int code,
            int seq,
            List<GamePackets.PapelAward> awards,
            int receiveTypeid,
            int receiveId,
            int receiveQntd,
            int qntdDep,
            int flagTime) {

        public static CadieExchangeResult fail(int code) {
            return new CadieExchangeResult(code, 0, List.of(), 0, 0, 0, 0, 0);
        }
    }

    record LoloComposeResult(
            int code,
            long pangAfter,
            long pangSpent,
            List<GamePackets.PapelAward> awards,
            int cardTipo,
            int cardTypeid) {

        public static LoloComposeResult fail(int code) {
            return new LoloComposeResult(code, 0, 0, List.of(), 0, 0);
        }
    }

    record CharMasteryResult(int code, List<GamePackets.PapelAward> awards, int mastery) {

        public static CharMasteryResult fail(int code) {
            return new CharMasteryResult(code, List.of(), 0);
        }
    }

    record CharStatsResult(
            int code,
            long pangAfter,
            long pangSpent,
            byte[] pcl,
            int stat,
            int typeid,
            int id) {

        public static CharStatsResult fail(int code) {
            return new CharStatsResult(code, 0, 0, new byte[5], 0, 0, 0);
        }
    }
}
