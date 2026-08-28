package org.pangya.db;

import org.jdbi.v3.core.Jdbi;
import org.pangya.protocol.game.GamePackets;

import java.util.List;
import java.util.Optional;

public final class JdbiInventoryRepository implements InventoryRepository {

    private final Jdbi jdbi;

    public JdbiInventoryRepository(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    @Override
    public List<GamePackets.WarehouseItem> warehouse(long uid) {
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT item_id, typeid, "C0", "C1", "C2", "C3", "C4", "Purchase", flag, "ItemType",
                               "ClubSet_WorkShop_C0", "ClubSet_WorkShop_C1", "ClubSet_WorkShop_C2",
                               "ClubSet_WorkShop_C3", "ClubSet_WorkShop_C4"
                          FROM pangya.pangya_item_warehouse
                         WHERE "UID" = :uid AND valid = 1
                         ORDER BY item_id
                        """)
                .bind("uid", uid)
                .map((rs, ctx) -> {
                    GamePackets.WarehouseItem w = new GamePackets.WarehouseItem();
                    w.id = rs.getInt("item_id");
                    w.typeid = rs.getInt("typeid");
                    w.c[0] = rs.getShort("C0");
                    w.c[1] = rs.getShort("C1");
                    w.c[2] = rs.getShort("C2");
                    w.c[3] = rs.getShort("C3");
                    w.c[4] = rs.getShort("C4");
                    w.purchase = rs.getInt("Purchase");
                    w.flag = rs.getInt("flag");
                    w.type = rs.getInt("ItemType");
                    w.workshopC[0] = rs.getShort("ClubSet_WorkShop_C0");
                    w.workshopC[1] = rs.getShort("ClubSet_WorkShop_C1");
                    w.workshopC[2] = rs.getShort("ClubSet_WorkShop_C2");
                    w.workshopC[3] = rs.getShort("ClubSet_WorkShop_C3");
                    w.workshopC[4] = rs.getShort("ClubSet_WorkShop_C4");
                    return w;
                })
                .list());
    }

    @Override
    public List<GamePackets.CharacterInfo> characters(long uid) {
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT item_id, typeid, default_hair, default_shirts, gift_flag, "Purchase",
                               parts_1, parts_2, parts_3, parts_4, parts_5, parts_6, parts_7, parts_8,
                               parts_9, parts_10, parts_11, parts_12, parts_13, parts_14, parts_15, parts_16,
                               parts_17, parts_18, parts_19, parts_20, parts_21, parts_22, parts_23, parts_24,
                               "PCL0", "PCL1", "PCL2", "PCL3", "PCL4",
                               auxparts_1, auxparts_2, auxparts_3, auxparts_4, auxparts_5,
                               "CutIn_1", "CutIn_2", "CutIn_3", "CutIn_4", "Mastery"
                          FROM pangya.pangya_character_information
                         WHERE "UID" = :uid
                         ORDER BY item_id
                        """)
                .bind("uid", uid)
                .map((rs, ctx) -> {
                    GamePackets.CharacterInfo c = new GamePackets.CharacterInfo();
                    c.id = rs.getInt("item_id");
                    c.typeid = rs.getInt("typeid");
                    c.defaultHair = rs.getInt("default_hair");
                    c.defaultShirts = rs.getInt("default_shirts");
                    c.giftFlag = rs.getInt("gift_flag");
                    c.purchase = rs.getInt("Purchase");
                    for (int i = 0; i < 24; i++) {
                        c.partsTypeid[i] = rs.getInt("parts_" + (i + 1));
                    }
                    for (int i = 0; i < 5; i++) {
                        c.pcl[i] = (byte) rs.getInt("PCL" + i);
                    }
                    for (int i = 0; i < 5; i++) {
                        c.auxparts[i] = rs.getInt("auxparts_" + (i + 1));
                    }
                    for (int i = 0; i < 4; i++) {
                        c.cutIn[i] = rs.getInt("CutIn_" + (i + 1));
                    }
                    c.mastery = rs.getInt("Mastery");
                    return c;
                })
                .list());
    }

    @Override
    public List<GamePackets.CaddieInfo> caddies(long uid) {
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT item_id, typeid, parts_typeid, "cLevel", "Exp", "RentFlag", "Purchase", "CheckEnd"
                          FROM pangya.pangya_caddie_information
                         WHERE "UID" = :uid AND "Valid" = 1
                         ORDER BY item_id
                        """)
                .bind("uid", uid)
                .map((rs, ctx) -> {
                    GamePackets.CaddieInfo c = new GamePackets.CaddieInfo();
                    c.id = rs.getInt("item_id");
                    c.typeid = rs.getInt("typeid");
                    c.partsTypeid = rs.getInt("parts_typeid");
                    c.level = rs.getInt("cLevel");
                    c.exp = rs.getInt("Exp");
                    c.rentFlag = rs.getInt("RentFlag");
                    c.purchase = rs.getInt("Purchase");
                    c.checkEnd = rs.getInt("CheckEnd");
                    return c;
                })
                .list());
    }

    @Override
    public GamePackets.UserEquip userEquip(long uid) {
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT caddie_id, character_id, club_id, ball_type,
                               item_slot_1, item_slot_2, item_slot_3, item_slot_4, item_slot_5,
                               item_slot_6, item_slot_7, item_slot_8, item_slot_9, item_slot_10,
                               "Skin_1", "Skin_2", "Skin_3", "Skin_4", "Skin_5", "Skin_6",
                               mascot_id, poster_1, poster_2
                          FROM pangya.pangya_user_equip
                         WHERE "UID" = :uid
                         LIMIT 1
                        """)
                .bind("uid", uid)
                .map((rs, ctx) -> {
                    GamePackets.UserEquip e = new GamePackets.UserEquip();
                    e.caddieId = rs.getInt("caddie_id");
                    e.characterId = rs.getInt("character_id");
                    e.clubsetId = rs.getInt("club_id");
                    e.ballTypeid = rs.getInt("ball_type");
                    for (int i = 0; i < 10; i++) {
                        e.itemSlot[i] = rs.getInt("item_slot_" + (i + 1));
                    }
                    for (int i = 0; i < 6; i++) {
                        e.skinTypeid[i] = rs.getInt("Skin_" + (i + 1));
                    }
                    e.mascotId = rs.getInt("mascot_id");
                    e.poster[0] = rs.getInt("poster_1");
                    e.poster[1] = rs.getInt("poster_2");
                    return e;
                })
                .findOne()
                .orElseGet(GamePackets.UserEquip::new));
    }

    @Override
    public List<GamePackets.MascotInfo> mascots(long uid) {
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT item_id, typeid, "mLevel", "mExp", "Tipo", "Message"
                          FROM pangya.pangya_mascot_info
                         WHERE "UID" = :uid AND "Valid" = 1
                         ORDER BY item_id
                        """)
                .bind("uid", uid)
                .map((rs, ctx) -> {
                    GamePackets.MascotInfo m = new GamePackets.MascotInfo();
                    m.id = rs.getInt("item_id");
                    m.typeid = rs.getInt("typeid");
                    m.level = rs.getInt("mLevel");
                    m.exp = rs.getInt("mExp");
                    m.tipo = rs.getInt("Tipo");
                    m.message = rs.getString("Message");
                    return m;
                })
                .list());
    }

    @Override
    public List<GamePackets.CardInfo> cards(long uid) {
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT card_itemid, card_typeid, "Slot", "Efeito", "Efeito_Qntd", "QNTD",
                               card_type, "USE_YN"
                          FROM pangya.pangya_card
                         WHERE "UID" = :uid
                         ORDER BY card_itemid
                        """)
                .bind("uid", uid)
                .map((rs, ctx) -> {
                    GamePackets.CardInfo c = new GamePackets.CardInfo();
                    c.id = rs.getInt("card_itemid");
                    c.typeid = rs.getInt("card_typeid");
                    c.slot = rs.getInt("Slot");
                    c.efeito = rs.getInt("Efeito");
                    c.efeitoQntd = rs.getInt("Efeito_Qntd");
                    c.qntd = rs.getInt("QNTD");
                    c.type = rs.getInt("card_type");
                    String yn = rs.getString("USE_YN");
                    c.useYn = "Y".equalsIgnoreCase(yn) || "1".equals(yn) ? 1 : 0;
                    return c;
                })
                .list());
    }

    @Override
    public long pang(long uid) {
        return jdbi.withHandle(h -> h.createQuery("SELECT COALESCE(\"Pang\", 0) FROM pangya.user_info WHERE \"UID\" = :uid")
                .bind("uid", uid)
                .mapTo(Long.class)
                .findOne()
                .orElse(0L));
    }

    @Override
    public long cookie(long uid) {
        return jdbi.withHandle(h -> h.createQuery("SELECT COALESCE(\"Cookie\", 0) FROM pangya.user_info WHERE \"UID\" = :uid")
                .bind("uid", uid)
                .mapTo(Long.class)
                .findOne()
                .orElse(0L));
    }

    @Override
    public void equipCharacter(long uid, int characterId) {
        jdbi.useHandle(h -> h.createUpdate(
                        "UPDATE pangya.pangya_user_equip SET character_id = :id WHERE \"UID\" = :uid")
                .bind("id", characterId)
                .bind("uid", uid)
                .execute());
    }

    @Override
    public void equipCaddie(long uid, int caddieId) {
        jdbi.useHandle(h -> h.createUpdate(
                        "UPDATE pangya.pangya_user_equip SET caddie_id = :id WHERE \"UID\" = :uid")
                .bind("id", caddieId)
                .bind("uid", uid)
                .execute());
    }

    @Override
    public void equipBallAndClub(long uid, int ballTypeid, int clubsetId) {
        jdbi.useHandle(h -> h.createUpdate(
                        "UPDATE pangya.pangya_user_equip SET ball_type = :ball, club_id = :club WHERE \"UID\" = :uid")
                .bind("ball", ballTypeid)
                .bind("club", clubsetId)
                .bind("uid", uid)
                .execute());
    }

    @Override
    public void equipMascot(long uid, int mascotId) {
        jdbi.useHandle(h -> h.createUpdate(
                        "UPDATE pangya.pangya_user_equip SET mascot_id = :id WHERE \"UID\" = :uid")
                .bind("id", mascotId)
                .bind("uid", uid)
                .execute());
    }

    @Override
    public void updateCharacterParts(long uid, GamePackets.CharacterInfo c) {
        jdbi.useHandle(h -> h.createUpdate("""
                        UPDATE pangya.pangya_character_information SET
                               default_hair = :hair, default_shirts = :shirts,
                               gift_flag = :gift, "Purchase" = :purchase,
                               parts_1 = :p1, parts_2 = :p2, parts_3 = :p3, parts_4 = :p4,
                               parts_5 = :p5, parts_6 = :p6, parts_7 = :p7, parts_8 = :p8,
                               parts_9 = :p9, parts_10 = :p10, parts_11 = :p11, parts_12 = :p12,
                               parts_13 = :p13, parts_14 = :p14, parts_15 = :p15, parts_16 = :p16,
                               parts_17 = :p17, parts_18 = :p18, parts_19 = :p19, parts_20 = :p20,
                               parts_21 = :p21, parts_22 = :p22, parts_23 = :p23, parts_24 = :p24,
                               auxparts_1 = :a1, auxparts_2 = :a2, auxparts_3 = :a3,
                               auxparts_4 = :a4, auxparts_5 = :a5,
                               "CutIn_1" = :c1, "CutIn_2" = :c2, "CutIn_3" = :c3, "CutIn_4" = :c4,
                               "PCL0" = :pcl0, "PCL1" = :pcl1, "PCL2" = :pcl2, "PCL3" = :pcl3, "PCL4" = :pcl4,
                               "Mastery" = :mastery
                         WHERE "UID" = :uid AND item_id = :id
                        """)
                .bind("hair", c.defaultHair)
                .bind("shirts", c.defaultShirts)
                .bind("gift", c.giftFlag)
                .bind("purchase", c.purchase)
                .bind("p1", c.partsTypeid[0]).bind("p2", c.partsTypeid[1])
                .bind("p3", c.partsTypeid[2]).bind("p4", c.partsTypeid[3])
                .bind("p5", c.partsTypeid[4]).bind("p6", c.partsTypeid[5])
                .bind("p7", c.partsTypeid[6]).bind("p8", c.partsTypeid[7])
                .bind("p9", c.partsTypeid[8]).bind("p10", c.partsTypeid[9])
                .bind("p11", c.partsTypeid[10]).bind("p12", c.partsTypeid[11])
                .bind("p13", c.partsTypeid[12]).bind("p14", c.partsTypeid[13])
                .bind("p15", c.partsTypeid[14]).bind("p16", c.partsTypeid[15])
                .bind("p17", c.partsTypeid[16]).bind("p18", c.partsTypeid[17])
                .bind("p19", c.partsTypeid[18]).bind("p20", c.partsTypeid[19])
                .bind("p21", c.partsTypeid[20]).bind("p22", c.partsTypeid[21])
                .bind("p23", c.partsTypeid[22]).bind("p24", c.partsTypeid[23])
                .bind("a1", c.auxparts[0]).bind("a2", c.auxparts[1]).bind("a3", c.auxparts[2])
                .bind("a4", c.auxparts[3]).bind("a5", c.auxparts[4])
                .bind("c1", c.cutIn[0]).bind("c2", c.cutIn[1]).bind("c3", c.cutIn[2]).bind("c4", c.cutIn[3])
                .bind("pcl0", c.pcl[0] & 0xff).bind("pcl1", c.pcl[1] & 0xff)
                .bind("pcl2", c.pcl[2] & 0xff).bind("pcl3", c.pcl[3] & 0xff).bind("pcl4", c.pcl[4] & 0xff)
                .bind("mastery", c.mastery)
                .bind("uid", uid)
                .bind("id", c.id)
                .execute());
    }

    @Override
    public List<GamePackets.CounterItem> counters(long uid) {
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT "Count_ID", "TypeID", active, "Count_Num_Item"
                          FROM pangya.pangya_counter_item
                         WHERE "UID" = :uid
                         ORDER BY "Count_ID"
                        """)
                .bind("uid", uid)
                .map((rs, ctx) -> new GamePackets.CounterItem(
                        rs.getInt("Count_ID"),
                        rs.getInt("TypeID"),
                        rs.getInt("active"),
                        rs.getInt("Count_Num_Item")))
                .list());
    }

    @Override
    public List<GamePackets.AchievementInfo> achievements(long uid) {
        return jdbi.withHandle(h -> {
            List<GamePackets.CounterItem> counters = h.createQuery("""
                            SELECT "Count_ID", "TypeID", active, "Count_Num_Item"
                              FROM pangya.pangya_counter_item
                             WHERE "UID" = :uid
                            """)
                    .bind("uid", uid)
                    .map((rs, ctx) -> new GamePackets.CounterItem(
                            rs.getInt("Count_ID"),
                            rs.getInt("TypeID"),
                            rs.getInt("active"),
                            rs.getInt("Count_Num_Item")))
                    .list();
            record QuestRow(int achievementId, int typeid, int counterId, int clearUnix) {}
            List<QuestRow> quests = h.createQuery("""
                            SELECT achievement_id, typeid, counter_item_id,
                                   COALESCE(EXTRACT(EPOCH FROM "Date")::int, 0) AS clear_unix
                              FROM pangya.pangya_quest
                             WHERE uid = :uid
                             ORDER BY id
                            """)
                    .bind("uid", uid)
                    .map((rs, ctx) -> new QuestRow(
                            rs.getInt("achievement_id"),
                            rs.getInt("typeid"),
                            rs.getInt("counter_item_id"),
                            rs.getInt("clear_unix")))
                    .list();
            return h.createQuery("""
                            SELECT "ID_ACHIEVEMENT", "TypeID", active, status
                              FROM pangya.pangya_achievement
                             WHERE "UID" = :uid
                             ORDER BY "ID_ACHIEVEMENT"
                            """)
                    .bind("uid", uid)
                    .map((rs, ctx) -> {
                        int id = rs.getInt("ID_ACHIEVEMENT");
                        List<GamePackets.QuestStuff> qsi = new java.util.ArrayList<>();
                        for (QuestRow q : quests) {
                            if (q.achievementId() != id) {
                                continue;
                            }
                            int counterType = 0;
                            for (GamePackets.CounterItem c : counters) {
                                if (c.id() == q.counterId()) {
                                    counterType = c.typeid();
                                    break;
                                }
                            }
                            qsi.add(new GamePackets.QuestStuff(q.typeid(), counterType, q.counterId(), q.clearUnix()));
                        }
                        return new GamePackets.AchievementInfo(
                                id,
                                rs.getInt("TypeID"),
                                rs.getInt("active"),
                                rs.getInt("status"),
                                qsi);
                    })
                    .list();
        });
    }

    @Override
    public Optional<ShopItem> shopItem(int typeid) {
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT typeid, pang_price, cookie_price, can_overlap
                          FROM pangya.shop_catalog
                         WHERE typeid = :typeid
                        """)
                .bind("typeid", typeid)
                .map((rs, ctx) -> new ShopItem(
                        rs.getInt("typeid"),
                        rs.getInt("pang_price"),
                        rs.getInt("cookie_price"),
                        rs.getInt("can_overlap") != 0))
                .findOne());
    }

    @Override
    public ShopBuyResult buyShopItem(long uid, int typeid, int qntd, int clientPang, int clientCookie) {
        return jdbi.inTransaction(h -> {
            ShopItem item = h.createQuery("""
                            SELECT typeid, pang_price, cookie_price, can_overlap
                              FROM pangya.shop_catalog
                             WHERE typeid = :typeid
                            """)
                    .bind("typeid", typeid)
                    .map((rs, ctx) -> new ShopItem(
                            rs.getInt("typeid"),
                            rs.getInt("pang_price"),
                            rs.getInt("cookie_price"),
                            rs.getInt("can_overlap") != 0))
                    .findOne()
                    .orElse(null);
            if (item == null) {
                return ShopBuyResult.fail(GamePackets.BUY_FAIL_NOT_BUYABLE);
            }
            boolean cash = item.cookiePrice() > 0;
            int expected = cash ? item.cookiePrice() * qntd : item.pangPrice() * qntd;
            int offered = cash ? clientCookie : clientPang;
            if (offered != expected) {
                return ShopBuyResult.fail(GamePackets.BUY_FAIL_PRICE);
            }
            if (!item.canOverlap()) {
                int owned = h.createQuery("""
                                SELECT COUNT(*) FROM pangya.pangya_item_warehouse
                                 WHERE "UID" = :uid AND typeid = :typeid AND valid = 1
                                """)
                        .bind("uid", uid)
                        .bind("typeid", typeid)
                        .mapTo(Integer.class)
                        .one();
                if (owned > 0) {
                    return ShopBuyResult.fail(GamePackets.BUY_FAIL_OWNED);
                }
            }
            long pang = h.createQuery("SELECT COALESCE(\"Pang\", 0) FROM pangya.user_info WHERE \"UID\" = :uid")
                    .bind("uid", uid)
                    .mapTo(Long.class)
                    .findOne()
                    .orElse(0L);
            long cookie = h.createQuery("SELECT COALESCE(\"Cookie\", 0) FROM pangya.user_info WHERE \"UID\" = :uid")
                    .bind("uid", uid)
                    .mapTo(Long.class)
                    .findOne()
                    .orElse(0L);
            long pangSpent = cash ? 0L : (long) item.pangPrice() * qntd;
            long cookieSpent = cash ? (long) item.cookiePrice() * qntd : 0L;
            if (pang < pangSpent || cookie < cookieSpent) {
                return ShopBuyResult.fail(GamePackets.BUY_FAIL_FUNDS);
            }
            int itemId = h.createQuery("""
                            INSERT INTO pangya.pangya_item_warehouse (
                                "UID", typeid, valid, "Gift_flag", flag,
                                "C0", "C1", "C2", "C3", "C4", "Purchase", "ItemType",
                                "ClubSet_WorkShop_Flag", "ClubSet_WorkShop_C0", "ClubSet_WorkShop_C1",
                                "ClubSet_WorkShop_C2", "ClubSet_WorkShop_C3", "ClubSet_WorkShop_C4",
                                "Mastery_Pts", "Recovery_Pts", "Level", "Up",
                                "Total_Mastery_Pts", "Mastery_Gasto"
                            ) VALUES (
                                :uid, :typeid, 1, 0, 0,
                                :qntd, 0, 0, 0, 0, 1, 2,
                                0, 0, 0, 0, 0, 0,
                                0, 0, 0, 0, 0, 0
                            )
                            RETURNING item_id
                            """)
                    .bind("uid", uid)
                    .bind("typeid", typeid)
                    .bind("qntd", qntd)
                    .mapTo(Integer.class)
                    .one();
            long pangAfter = pang - pangSpent;
            long cookieAfter = cookie - cookieSpent;
            h.createUpdate("""
                            UPDATE pangya.user_info
                               SET "Pang" = :pang, "Cookie" = :cookie
                             WHERE "UID" = :uid
                            """)
                    .bind("pang", pangAfter)
                    .bind("cookie", cookieAfter)
                    .bind("uid", uid)
                    .execute();
            return new ShopBuyResult(
                    0, itemId, typeid, qntd, pangAfter, cookieAfter, pangSpent, cookieSpent);
        });
    }

    @Override
    public void setPangCookie(long uid, long pang, long cookie) {
        jdbi.useHandle(h -> h.createUpdate("""
                        UPDATE pangya.user_info
                           SET "Pang" = :pang, "Cookie" = :cookie
                         WHERE "UID" = :uid
                        """)
                .bind("pang", pang)
                .bind("cookie", cookie)
                .bind("uid", uid)
                .execute());
    }

    @Override
    public void deleteWarehouseByTypeid(long uid, int typeid) {
        jdbi.useHandle(h -> h.createUpdate("""
                        DELETE FROM pangya.pangya_item_warehouse
                         WHERE "UID" = :uid AND typeid = :typeid
                        """)
                .bind("uid", uid)
                .bind("typeid", typeid)
                .execute());
    }
}
