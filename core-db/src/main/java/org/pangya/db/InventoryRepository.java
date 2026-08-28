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

    void setPangCookie(long uid, long pang, long cookie);

    void deleteWarehouseByTypeid(long uid, int typeid);

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
}
