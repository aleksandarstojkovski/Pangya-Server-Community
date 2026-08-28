package org.pangya.db;

import org.jdbi.v3.core.Jdbi;
import org.pangya.protocol.game.GamePackets;

import java.util.List;

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
}
