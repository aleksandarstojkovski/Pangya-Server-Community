package org.pangya.db;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.pangya.protocol.game.GamePackets;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.concurrent.ThreadLocalRandom;

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
                               "ClubSet_WorkShop_C3", "ClubSet_WorkShop_C4",
                               "Mastery_Pts", "Recovery_Pts", "Level", "Up",
                               ucc_name, ucc_trade, ucc_idx, ucc_status, ucc_seq,
                               ucc_copier_nick, ucc_copier
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
                    w.uccName = rs.getString("ucc_name");
                    w.uccTrade = rs.getInt("ucc_trade");
                    w.uccIdx = rs.getString("ucc_idx");
                    w.uccStatus = rs.getInt("ucc_status");
                    w.uccSeq = rs.getInt("ucc_seq");
                    w.uccCopierNick = rs.getString("ucc_copier_nick");
                    w.uccCopier = rs.getInt("ucc_copier");
                    w.workshopC[0] = rs.getShort("ClubSet_WorkShop_C0");
                    w.workshopC[1] = rs.getShort("ClubSet_WorkShop_C1");
                    w.workshopC[2] = rs.getShort("ClubSet_WorkShop_C2");
                    w.workshopC[3] = rs.getShort("ClubSet_WorkShop_C3");
                    w.workshopC[4] = rs.getShort("ClubSet_WorkShop_C4");
                    w.workshopMastery = rs.getInt("Mastery_Pts");
                    w.workshopRecovery = rs.getInt("Recovery_Pts");
                    w.workshopLevel = rs.getInt("Level");
                    w.workshopRank = rs.getInt("Up");
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
    public void updateUserEquip(long uid, GamePackets.UserEquip equip) {
        persistUserEquip(uid, equip);
    }

    @Override
    public GamePackets.UserEquip reconcileEquipAtLogin(long uid) {
        ensureDefaultInventory(uid);
        GamePackets.UserEquip equip = userEquip(uid);
        List<GamePackets.CharacterInfo> chars = characters(uid);
        List<GamePackets.CaddieInfo> caddies = caddies(uid);
        List<GamePackets.MascotInfo> mascots = mascots(uid);
        List<GamePackets.WarehouseItem> wh = warehouse(uid);
        boolean changed = false;

        if (equip.characterId == 0 || chars.stream().noneMatch(c -> c.id == equip.characterId)) {
            int fallback = chars.isEmpty() ? 0 : chars.get(0).id;
            if (equip.characterId != fallback) {
                equip.characterId = fallback;
                changed = true;
            }
        }
        if (equip.caddieId != 0 && caddies.stream().noneMatch(c -> c.id == equip.caddieId)) {
            equip.caddieId = 0;
            changed = true;
        }
        if (equip.mascotId != 0 && mascots.stream().noneMatch(m -> m.id == equip.mascotId)) {
            equip.mascotId = 0;
            changed = true;
        }
        if (equip.clubsetId == 0 || wh.stream().noneMatch(w -> w.id == equip.clubsetId)) {
            int fallback = findWarehouseIdByTypeid(wh, GamePackets.TYPEID_AIR_KNIGHT);
            if (fallback == 0) {
                fallback = wh.stream()
                        .filter(w -> (w.typeid >>> 26) == GamePackets.IFF_GROUP_CLUBSET)
                        .map(w -> w.id)
                        .findFirst()
                        .orElse(0);
            }
            if (equip.clubsetId != fallback) {
                equip.clubsetId = fallback;
                changed = true;
            }
        }
        if (equip.ballTypeid == 0 || wh.stream().noneMatch(w -> w.typeid == equip.ballTypeid)) {
            int fallback = GamePackets.TYPEID_DEFAULT_BALL;
            boolean hasDefaultBall = wh.stream().anyMatch(w -> w.typeid == GamePackets.TYPEID_DEFAULT_BALL);
            if (!hasDefaultBall) {
                fallback = wh.stream()
                        .filter(w -> (w.typeid >>> 26) == GamePackets.IFF_GROUP_BALL)
                        .map(w -> w.typeid)
                        .findFirst()
                        .orElse(GamePackets.TYPEID_DEFAULT_BALL);
            }
            if (equip.ballTypeid != fallback) {
                equip.ballTypeid = fallback;
                changed = true;
            }
        }
        for (int i = 0; i < equip.itemSlot.length; i++) {
            int slotTypeid = equip.itemSlot[i];
            if (slotTypeid != 0 && wh.stream().noneMatch(w -> w.typeid == slotTypeid)) {
                equip.itemSlot[i] = 0;
                changed = true;
            }
        }
        if (changed) {
            persistUserEquip(uid, equip);
        }
        return equip;
    }

    /** C# {@code equipDefaultCharacter|ClubSet|Ball} when inventory rows are missing. */
    private void ensureDefaultInventory(long uid) {
        if (characters(uid).isEmpty()) {
            insertDefaultCharacter(uid, GamePackets.TYPEID_NURI);
        }
        List<GamePackets.WarehouseItem> wh = warehouse(uid);
        if (wh.stream().noneMatch(w -> (w.typeid >>> 26) == GamePackets.IFF_GROUP_CLUBSET)) {
            addWarehouseItem(uid, GamePackets.TYPEID_AIR_KNIGHT, 1);
        }
        wh = warehouse(uid);
        if (wh.stream().noneMatch(w -> (w.typeid >>> 26) == GamePackets.IFF_GROUP_BALL)) {
            addWarehouseItem(uid, GamePackets.TYPEID_DEFAULT_BALL, 1);
        }
    }

    private void insertDefaultCharacter(long uid, int typeid) {
        jdbi.useHandle(h -> {
            int[] parts = CharacterComboDefSql.defaultParts(h, typeid);
            var u = h.createUpdate("""
                        INSERT INTO pangya.pangya_character_information (
                            typeid, "UID",
                            parts_1, parts_2, parts_3, parts_4, parts_5, parts_6, parts_7, parts_8,
                            parts_9, parts_10, parts_11, parts_12, parts_13, parts_14, parts_15, parts_16,
                            parts_17, parts_18, parts_19, parts_20, parts_21, parts_22, parts_23, parts_24,
                            default_hair, default_shirts, gift_flag,
                            "PCL0", "PCL1", "PCL2", "PCL3", "PCL4", "Purchase",
                            auxparts_1, auxparts_2, auxparts_3, auxparts_4, auxparts_5,
                            "CutIn_1", "CutIn_2", "CutIn_3", "CutIn_4", "Mastery"
                        ) VALUES (
                            :typeid, :uid,
                            :p1, :p2, :p3, :p4, :p5, :p6, :p7, :p8,
                            :p9, :p10, :p11, :p12, :p13, :p14, :p15, :p16,
                            :p17, :p18, :p19, :p20, :p21, :p22, :p23, :p24,
                            0, 0, 0,
                            0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0
                        )
                        """)
                    .bind("typeid", typeid)
                    .bind("uid", uid);
            CharacterPartsBinder.bind(u, parts);
            u.execute();
        });
    }

    private static int findWarehouseIdByTypeid(List<GamePackets.WarehouseItem> wh, int typeid) {
        for (GamePackets.WarehouseItem item : wh) {
            if (item.typeid == typeid) {
                return item.id;
            }
        }
        return 0;
    }

    private void persistUserEquip(long uid, GamePackets.UserEquip equip) {
        jdbi.useHandle(h -> h.createUpdate("""
                        UPDATE pangya.pangya_user_equip
                           SET caddie_id = :caddie,
                               character_id = :character,
                               club_id = :club,
                               ball_type = :ball,
                               item_slot_1 = :s1, item_slot_2 = :s2, item_slot_3 = :s3,
                               item_slot_4 = :s4, item_slot_5 = :s5, item_slot_6 = :s6,
                               item_slot_7 = :s7, item_slot_8 = :s8, item_slot_9 = :s9,
                               item_slot_10 = :s10,
                               mascot_id = :mascot
                         WHERE "UID" = :uid
                        """)
                .bind("uid", uid)
                .bind("caddie", equip.caddieId)
                .bind("character", equip.characterId)
                .bind("club", equip.clubsetId)
                .bind("ball", equip.ballTypeid)
                .bind("mascot", equip.mascotId)
                .bind("s1", equip.itemSlot[0])
                .bind("s2", equip.itemSlot[1])
                .bind("s3", equip.itemSlot[2])
                .bind("s4", equip.itemSlot[3])
                .bind("s5", equip.itemSlot[4])
                .bind("s6", equip.itemSlot[5])
                .bind("s7", equip.itemSlot[6])
                .bind("s8", equip.itemSlot[7])
                .bind("s9", equip.itemSlot[8])
                .bind("s10", equip.itemSlot[9])
                .execute());
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
    public boolean mascotMessageEnabled(int typeid) {
        return org.pangya.protocol.iff.PangyaIffLoader.mascot(typeid)
                .map(org.pangya.protocol.iff.IffMascotRecord::messageActive)
                .orElseGet(() -> mascotMessageEnabledSql(typeid));
    }

    private boolean mascotMessageEnabledSql(int typeid) {
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT 1 FROM pangya.iff_mascot
                         WHERE typeid = :typeid AND msg_active = 1
                        """)
                .bind("typeid", typeid)
                .mapTo(Integer.class)
                .findOne()
                .isPresent());
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
    public float quitRate(long uid) {
        return jdbi.withHandle(h -> h.createQuery(
                        """
                        SELECT COALESCE("Jogado", 0), COALESCE("Quitado", 0)
                        FROM pangya.user_info WHERE "UID" = :uid
                        """)
                .bind("uid", uid)
                .map((rs, ctx) -> {
                    long jogado = rs.getLong(1);
                    long quitado = rs.getLong(2);
                    if (jogado <= 0) {
                        return 0f;
                    }
                    return quitado * 100f / jogado;
                })
                .findOne()
                .orElse(0f));
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
    public List<InventoryRepository.CounterIncrement> incrementActiveCounters(
            long uid, int counterTypeid, int delta) {
        if (counterTypeid == 0 || delta == 0) {
            return List.of();
        }
        return jdbi.inTransaction(h -> h.createQuery("""
                        UPDATE pangya.pangya_counter_item c
                           SET "Count_Num_Item" = c."Count_Num_Item" + :delta
                          FROM pangya.pangya_quest q
                          JOIN pangya.pangya_achievement a
                            ON a."ID_ACHIEVEMENT" = q.achievement_id
                           AND a."UID" = :uid
                         WHERE c."Count_ID" = q.counter_item_id
                           AND q.uid = :uid
                           AND c."UID" = :uid
                           AND c.active = 1
                           AND c."TypeID" = :typeid
                           AND a.status = 3
                           AND q."Date" IS NULL
                        RETURNING c."Count_ID", c."TypeID",
                                  c."Count_Num_Item" - :delta AS before_val,
                                  c."Count_Num_Item" AS after_val
                        """)
                .bind("uid", uid)
                .bind("typeid", counterTypeid)
                .bind("delta", delta)
                .map((rs, ctx) -> new InventoryRepository.CounterIncrement(
                        rs.getInt("Count_ID"),
                        rs.getInt("TypeID"),
                        rs.getInt("before_val"),
                        rs.getInt("after_val"),
                        delta))
                .list());
    }

    @Override
    public List<InventoryRepository.CounterIncrement> incrementCounterItemsByTypeid(
            long uid, int counterTypeid, int delta) {
        if (counterTypeid == 0 || delta == 0) {
            return List.of();
        }
        return jdbi.inTransaction(h -> h.createQuery("""
                        UPDATE pangya.pangya_counter_item c
                           SET "Count_Num_Item" = c."Count_Num_Item" + :delta
                         WHERE c."UID" = :uid
                           AND c."TypeID" = :typeid
                           AND c.active = 1
                        RETURNING c."Count_ID", c."TypeID",
                                  c."Count_Num_Item" - :delta AS before_val,
                                  c."Count_Num_Item" AS after_val
                        """)
                .bind("uid", uid)
                .bind("typeid", counterTypeid)
                .bind("delta", delta)
                .map((rs, ctx) -> new InventoryRepository.CounterIncrement(
                        rs.getInt("Count_ID"),
                        rs.getInt("TypeID"),
                        rs.getInt("before_val"),
                        rs.getInt("after_val"),
                        delta))
                .list());
    }

    @Override
    public InventoryRepository.CounterApplyResult applyCounterIncrements(
            long uid, int counterTypeid, int delta) {
        return applyCounterIncrements(uid, Map.of(counterTypeid, delta));
    }

    @Override
    public InventoryRepository.CounterApplyResult applyCounterIncrements(
            long uid, Map<Integer, Integer> deltas) {
        if (deltas == null || deltas.isEmpty()) {
            return new InventoryRepository.CounterApplyResult(List.of(), List.of(), List.of(), List.of());
        }
        List<InventoryRepository.CounterIncrement> increments = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : deltas.entrySet()) {
            int counterTypeid = entry.getKey();
            int delta = entry.getValue();
            if (counterTypeid == 0 || delta == 0) {
                continue;
            }
            increments.addAll(incrementCounterItemsByTypeid(uid, counterTypeid, delta));
        }
        if (increments.isEmpty()) {
            return new InventoryRepository.CounterApplyResult(List.of(), List.of(), List.of(), List.of());
        }
        Instant now = Instant.now();
        java.util.Set<Integer> affectedAchievements = new java.util.HashSet<>();
        List<InventoryRepository.QuestClearRow> clears = new ArrayList<>();
        jdbi.useTransaction(h -> {
            for (InventoryRepository.CounterIncrement inc : increments) {
                record QuestLink(int questId, int achievementId, int questTypeid, int achievementTypeid) {}
                Optional<QuestLink> quest = h.createQuery("""
                                SELECT q.id AS quest_id, q.achievement_id, q.typeid AS quest_typeid,
                                       a."TypeID" AS achievement_typeid
                                  FROM pangya.pangya_quest q
                                  JOIN pangya.pangya_achievement a
                                    ON a."ID_ACHIEVEMENT" = q.achievement_id
                                   AND a."UID" = :uid
                                 WHERE q.uid = :uid
                                   AND q.counter_item_id = :counter
                                   AND q."Date" IS NULL
                                """)
                        .bind("uid", uid)
                        .bind("counter", inc.id())
                        .map((rs, ctx) -> new QuestLink(
                                rs.getInt("quest_id"),
                                rs.getInt("achievement_id"),
                                rs.getInt("quest_typeid"),
                                rs.getInt("achievement_typeid")))
                        .findOne();
                if (quest.isEmpty()) {
                    continue;
                }
                QuestLink q = quest.get();
                affectedAchievements.add(q.achievementId());
                int target = h.createQuery("""
                                SELECT counter_qntd
                                  FROM pangya.iff_daily_quest_stuff
                                 WHERE quest_typeid = :typeid
                                """)
                        .bind("typeid", q.questTypeid())
                        .mapTo(Integer.class)
                        .findOne()
                        .orElse(1);
                if (target <= 0) {
                    target = 1;
                }
                if (inc.after() < target) {
                    continue;
                }
                h.createUpdate("""
                                UPDATE pangya.pangya_quest
                                   SET "Date" = :now
                                 WHERE id = :id AND uid = :uid
                                """)
                        .bind("now", now)
                        .bind("id", q.questId())
                        .bind("uid", uid)
                        .execute();
                clears.add(new InventoryRepository.QuestClearRow(
                        q.achievementTypeid(), q.questTypeid()));
                int uncleared = h.createQuery("""
                                SELECT COUNT(*)
                                  FROM pangya.pangya_quest
                                 WHERE uid = :uid
                                   AND achievement_id = :achievement
                                   AND "Date" IS NULL
                                """)
                        .bind("uid", uid)
                        .bind("achievement", q.achievementId())
                        .mapTo(Integer.class)
                        .one();
                if (uncleared == 0) {
                    h.createUpdate("""
                                    UPDATE pangya.pangya_achievement
                                       SET status = 4
                                     WHERE "UID" = :uid
                                       AND "ID_ACHIEVEMENT" = :id
                                    """)
                            .bind("uid", uid)
                            .bind("id", q.achievementId())
                            .execute();
                }
            }
        });
        List<GamePackets.AchievementInfo> updated = achievements(uid).stream()
                .filter(a -> affectedAchievements.contains(a.id()))
                .toList();
        List<GamePackets.PapelAward> rewardAwards = grantDailyQuestClearRewards(uid, clears);
        return new InventoryRepository.CounterApplyResult(increments, clears, updated, rewardAwards);
    }

    /** C# {@code finish_and_update} {@code v_reward} warehouse rows after quest clear. */
    private List<GamePackets.PapelAward> grantDailyQuestClearRewards(
            long uid, List<InventoryRepository.QuestClearRow> clears) {
        if (clears.isEmpty()) {
            return List.of();
        }
        java.util.Set<Integer> achievementTypeids = new java.util.HashSet<>();
        for (InventoryRepository.QuestClearRow clear : clears) {
            achievementTypeids.add(clear.achievementTypeid());
        }
        List<GamePackets.PapelAward> out = new ArrayList<>();
        for (int achievementTypeid : achievementTypeids) {
            for (InventoryRepository.DailyQuestReward reward : dailyQuestRewards(achievementTypeid)) {
                if (reward.typeid() == 0 || reward.qntd() <= 0) {
                    continue;
                }
                int ant = warehouseItemQntd(uid, reward.typeid());
                int itemId = addWarehouseItem(uid, reward.typeid(), reward.qntd());
                out.add(new GamePackets.PapelAward(
                        GamePackets.PAPEL_AWARD_TYPE,
                        reward.typeid(),
                        itemId,
                        0,
                        ant,
                        ant + reward.qntd(),
                        reward.time() > 0 ? reward.time() : reward.qntd()));
            }
        }
        return out;
    }

    private int warehouseItemQntd(long uid, int typeid) {
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT "C0" FROM pangya.pangya_item_warehouse
                         WHERE "UID" = :uid AND typeid = :typeid AND valid = 1
                         ORDER BY item_id
                         LIMIT 1
                        """)
                .bind("uid", uid)
                .bind("typeid", typeid)
                .mapTo(Integer.class)
                .findOne()
                .orElse(0)) & 0xffff;
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
    public java.util.Map<Integer, ShopItem> shopCatalogIndex() {
        return jdbi.withHandle(h -> {
            java.util.Map<Integer, ShopItem> out = new java.util.HashMap<>();
            h.createQuery("""
                            SELECT typeid, pang_price, cookie_price, can_overlap
                              FROM pangya.shop_catalog
                            """)
                    .map((rs, ctx) -> new ShopItem(
                            rs.getInt("typeid"),
                            rs.getInt("pang_price"),
                            rs.getInt("cookie_price"),
                            rs.getInt("can_overlap") != 0))
                    .list()
                    .forEach(item -> out.put(item.typeid(), item));
            return out;
        });
    }

    @Override
    public ShopBuyResult buyShopItem(long uid, int typeid, int qntd, int clientPang, int clientCookie) {
        return buyShopItem(uid, typeid, qntd, clientPang, clientCookie, 0);
    }

    @Override
    public ShopBuyResult buyShopItem(
            long uid, int typeid, int qntd, int clientPang, int clientCookie, int buyTime) {
        return jdbi.inTransaction(h -> chargeShopItem(h, uid, typeid, qntd, clientPang, clientCookie, true, buyTime));
    }

    @Override
    public ShopBuyResult giftShopItem(long uid, int typeid, int qntd, int clientPang, int clientCookie) {
        return jdbi.inTransaction(h -> {
            if (GamePackets.itemGroupIdentify(typeid) == GamePackets.IFF_GROUP_SET_ITEM) {
                return chargeSetShopItem(h, uid, typeid, qntd, clientPang, clientCookie, false);
            }
            return chargeShopItem(h, uid, typeid, qntd, clientPang, clientCookie, false, 0);
        });
    }

    @Override
    public void setLevel(long uid, int level) {
        jdbi.useHandle(h -> h.createUpdate("""
                        UPDATE pangya.user_info
                           SET "level" = :level
                         WHERE "UID" = :uid
                        """)
                .bind("level", level)
                .bind("uid", uid)
                .execute());
    }

    @Override
    public void setLevelExp(long uid, int level, int exp) {
        jdbi.useHandle(h -> h.createUpdate("""
                        UPDATE pangya.user_info
                           SET "level" = :level, "Xp" = :exp
                         WHERE "UID" = :uid
                        """)
                .bind("level", level)
                .bind("exp", exp)
                .bind("uid", uid)
                .execute());
    }

    @Override
    public PlayerLevelExp levelExp(long uid) {
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT COALESCE("level", 0), COALESCE("Xp", 0)
                          FROM pangya.user_info
                         WHERE "UID" = :uid
                        """)
                .bind("uid", uid)
                .map((rs, ctx) -> new PlayerLevelExp(rs.getInt(1), rs.getInt(2)))
                .findOne()
                .orElse(new PlayerLevelExp(0, 0)));
    }

    @Override
    public AddExpResult addExp(long uid, int expGain) {
        if (expGain <= 0) {
            PlayerLevelExp current = levelExp(uid);
            return new AddExpResult(current.level(), current.exp(), 0);
        }
        return jdbi.inTransaction(h -> {
            PlayerLevelExp row = h.createQuery("""
                            SELECT COALESCE("level", 0), COALESCE("Xp", 0)
                              FROM pangya.user_info
                             WHERE "UID" = :uid
                             FOR UPDATE
                            """)
                    .bind("uid", uid)
                    .map((rs, ctx) -> new PlayerLevelExp(rs.getInt(1), rs.getInt(2)))
                    .findOne()
                    .orElse(new PlayerLevelExp(0, 0));
            int level = row.level();
            int exp = row.exp() + expGain;
            int levelsGained = 0;
            if (level >= org.pangya.protocol.game.ExpLevelTable.MAX_LEVEL_INDEX) {
                h.createUpdate("""
                                UPDATE pangya.user_info
                                   SET "Xp" = :exp, "level" = :level
                                 WHERE "UID" = :uid
                                """)
                        .bind("exp", exp)
                        .bind("level", level)
                        .bind("uid", uid)
                        .execute();
                return new AddExpResult(level, exp, 0);
            }
            while (level < org.pangya.protocol.game.ExpLevelTable.MAX_LEVEL_INDEX) {
                int cost = org.pangya.protocol.game.ExpLevelTable.costForLevel(level);
                if (cost <= 0 || exp < cost) {
                    break;
                }
                exp -= cost;
                level++;
                levelsGained++;
            }
            h.createUpdate("""
                            UPDATE pangya.user_info
                               SET "Xp" = :exp, "level" = :level
                             WHERE "UID" = :uid
                            """)
                    .bind("exp", exp)
                    .bind("level", level)
                    .bind("uid", uid)
                    .execute();
            return new AddExpResult(level, exp, levelsGained);
        });
    }

    private static final int LIMIT_LEVEL_CADDIE = 3;
    private static final int LIMIT_LEVEL_MASCOT = 9;

    private static int caddieExpThreshold(int level) {
        return 520 + (160 * level);
    }

    private static int mascotExpThreshold(int level) {
        return 50 + (((20 + (20 + ((level - 1) * 10))) * level) / 2);
    }

    @Override
    public Optional<GamePackets.CaddieInfo> addCaddieExp(long uid, int exp) {
        if (exp <= 0) {
            return Optional.empty();
        }
        GamePackets.UserEquip equip = userEquip(uid);
        if (equip.caddieId == 0) {
            return Optional.empty();
        }
        return jdbi.inTransaction(h -> {
            GamePackets.CaddieInfo caddie = h.createQuery("""
                            SELECT item_id, typeid, parts_typeid, "cLevel", "Exp", "RentFlag",
                                   "Purchase", "CheckEnd"
                              FROM pangya.pangya_caddie_information
                             WHERE "UID" = :uid AND item_id = :caddieId AND "Valid" = 1
                             LIMIT 1
                            """)
                    .bind("uid", (int) uid)
                    .bind("caddieId", equip.caddieId)
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
                    .findOne()
                    .orElse(null);
            if (caddie == null) {
                return Optional.<GamePackets.CaddieInfo>empty();
            }
            if (caddie.level >= LIMIT_LEVEL_CADDIE) {
                return Optional.of(caddie);
            }
            caddie.exp += exp;
            while (caddie.level < LIMIT_LEVEL_CADDIE
                    && caddie.exp >= caddieExpThreshold(caddie.level)) {
                caddie.exp -= caddieExpThreshold(caddie.level);
                caddie.level++;
            }
            h.createUpdate("""
                            UPDATE pangya.pangya_caddie_information
                               SET "cLevel" = :level, "Exp" = :exp
                             WHERE "UID" = :uid AND item_id = :caddieId AND "Valid" = 1
                            """)
                    .bind("level", caddie.level)
                    .bind("exp", caddie.exp)
                    .bind("uid", (int) uid)
                    .bind("caddieId", caddie.id)
                    .execute();
            return Optional.of(caddie);
        });
    }

    @Override
    public Optional<GamePackets.MascotInfo> addMascotExp(long uid, int exp) {
        if (exp <= 0) {
            return Optional.empty();
        }
        GamePackets.UserEquip equip = userEquip(uid);
        if (equip.mascotId == 0) {
            return Optional.empty();
        }
        return jdbi.inTransaction(h -> {
            GamePackets.MascotInfo mascot = h.createQuery("""
                            SELECT item_id, typeid, "mLevel", "mExp", "Tipo", "Message"
                              FROM pangya.pangya_mascot_info
                             WHERE "UID" = :uid AND item_id = :mascotId AND "Valid" = 1
                             LIMIT 1
                            """)
                    .bind("uid", (int) uid)
                    .bind("mascotId", equip.mascotId)
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
                    .findOne()
                    .orElse(null);
            if (mascot == null || mascot.typeid == 0) {
                return Optional.<GamePackets.MascotInfo>empty();
            }
            if (mascot.level >= LIMIT_LEVEL_MASCOT) {
                return Optional.of(mascot);
            }
            mascot.exp += exp;
            while (mascot.level < LIMIT_LEVEL_MASCOT
                    && mascot.exp >= mascotExpThreshold(mascot.level)) {
                mascot.exp -= mascotExpThreshold(mascot.level);
                mascot.level++;
            }
            h.createUpdate("""
                            UPDATE pangya.pangya_mascot_info
                               SET "mLevel" = :level, "mExp" = :exp
                             WHERE "UID" = :uid AND item_id = :mascotId AND "Valid" = 1
                            """)
                    .bind("level", mascot.level)
                    .bind("exp", mascot.exp)
                    .bind("uid", (int) uid)
                    .bind("mascotId", mascot.id)
                    .execute();
            return Optional.of(mascot);
        });
    }

    private ShopBuyResult chargeShopItem(
            Handle h,
            long uid,
            int typeid,
            int qntd,
            int clientPang,
            int clientCookie,
            boolean addWarehouse,
            int buyTime) {
        if (addWarehouse && GamePackets.itemGroupIdentify(typeid) == GamePackets.IFF_GROUP_SET_ITEM) {
            return chargeSetShopItem(h, uid, typeid, qntd, clientPang, clientCookie, true);
        }
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
        if (addWarehouse && !item.canOverlap()) {
            int group = GamePackets.itemGroupIdentify(typeid);
            if (group != GamePackets.IFF_GROUP_CAD_ITEM
                    && !(group == GamePackets.IFF_GROUP_MASCOT && buyTime > 0)
                    && ownsAwardTypeid(uid, typeid)) {
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
        int itemId = 0;
        int qntdDep = qntd;
        java.util.List<org.pangya.protocol.game.GamePackets.BoughtItem> bought = java.util.List.of();
        if (addWarehouse) {
            if (!ItemInitializer.isShopAwardGroup(typeid)) {
                return ShopBuyResult.fail(GamePackets.BUY_FAIL_NOT_BUYABLE);
            }
            ItemInitializer.InitContext ctx = new ItemInitializer.InitContext(
                    playerLevel(h, uid), true, false, false);
            Optional<ItemInitializer.MailAwardRow> awardOpt =
                    ItemInitializer.initShopAward(ctx, typeid, qntd, buyTime);
            if (awardOpt.isEmpty()) {
                return ShopBuyResult.fail(GamePackets.BUY_FAIL_NOT_BUYABLE);
            }
            ItemInitializer.MailAwardRow award = awardOpt.get();
            if (award.group() != GamePackets.IFF_GROUP_CAD_ITEM
                    && !(award.group() == GamePackets.IFF_GROUP_MASCOT && award.mascotTimeDays() > 0)
                    && !item.canOverlap()
                    && ownsAwardTypeid(uid, typeid)) {
                return ShopBuyResult.fail(GamePackets.BUY_FAIL_OWNED);
            }
            if (award.group() == GamePackets.IFF_GROUP_CAD_ITEM
                    && !ownsCaddieTypeid(h, uid, GamePackets.caddieBaseTypeid(typeid))) {
                return ShopBuyResult.fail(GamePackets.BUY_FAIL_NOT_BUYABLE);
            }
            Optional<AwardInsert> insert = insertAwardHandle(h, uid, award);
            if (insert.isEmpty()) {
                int fail = award.group() == GamePackets.IFF_GROUP_CADDIE
                        || award.group() == GamePackets.IFF_GROUP_SKIN_WAREHOUSE
                        ? GamePackets.BUY_FAIL_OWNED
                        : GamePackets.BUY_FAIL_NOT_BUYABLE;
                return ShopBuyResult.fail(fail);
            }
            itemId = insert.get().id();
            qntdDep = insert.get().addQntd();
            bought = java.util.List.of(new org.pangya.protocol.game.GamePackets.BoughtItem(
                    typeid, itemId, buyTime, award.rentFlag(), qntdDep));
        }
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
                0, itemId, typeid, qntdDep, pangAfter, cookieAfter, pangSpent, cookieSpent, bought);
    }

    private ShopBuyResult chargeSetShopItem(
            Handle h, long uid, int setTypeid, int qntd, int clientPang, int clientCookie, boolean addWarehouse) {
        Optional<SetItemIff> setOpt = setItemIff(setTypeid);
        if (setOpt.isEmpty()) {
            return ShopBuyResult.fail(GamePackets.BUY_FAIL_NOT_BUYABLE);
        }
        if (ownerSetItem(uid, setTypeid)) {
            return ShopBuyResult.fail(GamePackets.BUY_FAIL_OWNED);
        }
        ShopItem item = h.createQuery("""
                        SELECT typeid, pang_price, cookie_price, can_overlap
                          FROM pangya.shop_catalog
                         WHERE typeid = :typeid
                        """)
                .bind("typeid", setTypeid)
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
        List<GamePackets.BoughtItem> awards = new ArrayList<>();
        if (addWarehouse) {
            List<ItemInitializer.WarehouseInitRow> components =
                    ItemInitializer.expandSetItem(true, setTypeid);
            if (components.isEmpty()) {
                return ShopBuyResult.fail(GamePackets.BUY_FAIL_NOT_BUYABLE);
            }
            for (ItemInitializer.WarehouseInitRow comp : components) {
                if (!item.canOverlap() && ownsWarehouseTypeid(h, uid, comp.typeid())) {
                    continue;
                }
                int id = insertWarehouse(h, uid, comp);
                awards.add(new GamePackets.BoughtItem(
                        comp.typeid(), id, 0, 0, comp.qntdDep()));
            }
        } else if (ItemInitializer.expandSetItem(true, setTypeid).isEmpty()) {
            return ShopBuyResult.fail(GamePackets.BUY_FAIL_NOT_BUYABLE);
        }
        if (addWarehouse && awards.isEmpty()) {
            return ShopBuyResult.fail(GamePackets.BUY_FAIL_NOT_BUYABLE);
        }
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
        GamePackets.BoughtItem first = awards.isEmpty()
                ? new GamePackets.BoughtItem(setTypeid, 0, 0, 0, qntd)
                : awards.getFirst();
        return new ShopBuyResult(
                0, first.id(), setTypeid, first.qntdDep(), pangAfter, cookieAfter, pangSpent, cookieSpent, awards);
    }

    @Override
    public boolean setItemExpandable(int setTypeid) {
        return !ItemInitializer.expandSetItem(true, setTypeid).isEmpty();
    }

    private static int playerLevel(Handle h, long uid) {
        return h.createQuery("SELECT COALESCE(\"level\", 0) FROM pangya.user_info WHERE \"UID\" = :uid")
                .bind("uid", uid)
                .mapTo(Integer.class)
                .findOne()
                .orElse(0);
    }

    private static boolean ownsWarehouseTypeid(Handle h, long uid, int typeid) {
        return h.createQuery("""
                        SELECT 1 FROM pangya.pangya_item_warehouse
                         WHERE "UID" = :uid AND typeid = :typeid AND valid = 1
                         LIMIT 1
                        """)
                .bind("uid", uid)
                .bind("typeid", typeid)
                .mapTo(Integer.class)
                .findOne()
                .isPresent();
    }

    @Override
    public boolean ownerSetItem(long uid, int setTypeid) {
        if (GamePackets.itemGroupIdentify(setTypeid) != GamePackets.IFF_GROUP_SET_ITEM) {
            return false;
        }
        Optional<SetItemIff> setOpt = setItemIff(setTypeid);
        if (setOpt.isEmpty()) {
            return false;
        }
        SetItemIff set = setOpt.get();
        return jdbi.withHandle(h -> {
            for (int i = 0; i < set.total(); i++) {
                int compTypeid = set.itemTypeids()[i];
                if (compTypeid == 0) {
                    continue;
                }
                if (GamePackets.itemGroupIdentify(compTypeid) == GamePackets.IFF_GROUP_CHARACTER) {
                    continue;
                }
                if (ownsWarehouseTypeid(h, uid, compTypeid)) {
                    return true;
                }
            }
            return false;
        });
    }

    @Override
    public PersonalShopMove transferPersonalShop(
            long sellerUid, long buyerUid, int itemId, int typeid, int qntd, long unitPang) {
        return jdbi.inTransaction(h -> {
            GamePackets.WarehouseItem seller = h.createQuery("""
                            SELECT item_id, typeid, "C0", "C1", "C2", "C3", "C4", "Purchase", flag, "ItemType",
                                   "ClubSet_WorkShop_C0", "ClubSet_WorkShop_C1", "ClubSet_WorkShop_C2",
                                   "ClubSet_WorkShop_C3", "ClubSet_WorkShop_C4"
                              FROM pangya.pangya_item_warehouse
                             WHERE "UID" = :uid AND item_id = :id AND valid = 1
                            """)
                    .bind("uid", sellerUid)
                    .bind("id", itemId)
                    .map((rs, ctx) -> mapWarehouse(rs))
                    .findOne()
                    .orElse(null);
            if (seller == null || seller.typeid != typeid || (seller.c[0] & 0xffff) < qntd) {
                throw new IllegalStateException("personal-shop transfer missing seller item");
            }
            long sellerPang = h.createQuery("SELECT COALESCE(\"Pang\", 0) FROM pangya.user_info WHERE \"UID\" = :uid")
                    .bind("uid", sellerUid)
                    .mapTo(Long.class)
                    .findOne()
                    .orElse(0L);
            long buyerPang = h.createQuery("SELECT COALESCE(\"Pang\", 0) FROM pangya.user_info WHERE \"UID\" = :uid")
                    .bind("uid", buyerUid)
                    .mapTo(Long.class)
                    .findOne()
                    .orElse(0L);
            long cost = unitPang * qntd;
            if (buyerPang < cost) {
                throw new IllegalStateException("personal-shop funds");
            }
            long gain = GamePackets.shopSellerGain(cost);
            int remain = (seller.c[0] & 0xffff) - qntd;
            if (remain == 0) {
                h.createUpdate("""
                                DELETE FROM pangya.pangya_item_warehouse
                                 WHERE "UID" = :uid AND item_id = :id
                                """)
                        .bind("uid", sellerUid)
                        .bind("id", itemId)
                        .execute();
            } else {
                h.createUpdate("""
                                UPDATE pangya.pangya_item_warehouse
                                   SET "C0" = :c0
                                 WHERE "UID" = :uid AND item_id = :id
                                """)
                        .bind("c0", remain)
                        .bind("uid", sellerUid)
                        .bind("id", itemId)
                        .execute();
            }
            GamePackets.WarehouseItem buyer = h.createQuery("""
                            SELECT item_id, typeid, "C0", "C1", "C2", "C3", "C4", "Purchase", flag, "ItemType",
                                   "ClubSet_WorkShop_C0", "ClubSet_WorkShop_C1", "ClubSet_WorkShop_C2",
                                   "ClubSet_WorkShop_C3", "ClubSet_WorkShop_C4"
                              FROM pangya.pangya_item_warehouse
                             WHERE "UID" = :uid AND typeid = :typeid AND valid = 1
                             ORDER BY item_id
                             LIMIT 1
                            """)
                    .bind("uid", buyerUid)
                    .bind("typeid", typeid)
                    .map((rs, ctx) -> mapWarehouse(rs))
                    .findOne()
                    .orElse(null);
            if (buyer != null) {
                int next = (buyer.c[0] & 0xffff) + qntd;
                h.createUpdate("""
                                UPDATE pangya.pangya_item_warehouse
                                   SET "C0" = :c0
                                 WHERE "UID" = :uid AND item_id = :id
                                """)
                        .bind("c0", next)
                        .bind("uid", buyerUid)
                        .bind("id", buyer.id)
                        .execute();
                buyer.c[0] = (short) qntd;
            } else {
                int newId = h.createQuery("""
                                INSERT INTO pangya.pangya_item_warehouse (
                                    "UID", typeid, valid, "Gift_flag", flag,
                                    "C0", "C1", "C2", "C3", "C4", "Purchase", "ItemType",
                                    "ClubSet_WorkShop_Flag", "ClubSet_WorkShop_C0", "ClubSet_WorkShop_C1",
                                    "ClubSet_WorkShop_C2", "ClubSet_WorkShop_C3", "ClubSet_WorkShop_C4",
                                    "Mastery_Pts", "Recovery_Pts", "Level", "Up",
                                    "Total_Mastery_Pts", "Mastery_Gasto"
                                ) VALUES (
                                    :uid, :typeid, 1, 0, 5,
                                    :qntd, 0, 0, 0, 0, 1, 2,
                                    0, 0, 0, 0, 0, 0,
                                    0, 0, 0, 0, 0, 0
                                )
                                RETURNING item_id
                                """)
                        .bind("uid", buyerUid)
                        .bind("typeid", typeid)
                        .bind("qntd", qntd)
                        .mapTo(Integer.class)
                        .one();
                buyer = new GamePackets.WarehouseItem();
                buyer.id = newId;
                buyer.typeid = typeid;
                buyer.c[0] = (short) qntd;
                buyer.flag = 5;
                buyer.purchase = 1;
                buyer.type = 2;
            }
            seller.c[0] = (short) qntd;
            long sellerAfter = sellerPang + gain;
            long buyerAfter = buyerPang - cost;
            h.createUpdate("""
                            UPDATE pangya.user_info
                               SET "Pang" = :pang
                             WHERE "UID" = :uid
                            """)
                    .bind("pang", sellerAfter)
                    .bind("uid", sellerUid)
                    .execute();
            h.createUpdate("""
                            UPDATE pangya.user_info
                               SET "Pang" = :pang
                             WHERE "UID" = :uid
                            """)
                    .bind("pang", buyerAfter)
                    .bind("uid", buyerUid)
                    .execute();
            return new PersonalShopMove(seller, buyer, sellerAfter, buyerAfter, gain);
        });
    }

    private static GamePackets.WarehouseItem mapWarehouse(java.sql.ResultSet rs) throws java.sql.SQLException {
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
    public long dolfiniLockerPang(long uid) {
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT COALESCE(pang, 0) FROM pangya.pangya_dolfini_locker
                         WHERE uid = :uid
                        """)
                .bind("uid", uid)
                .mapTo(Long.class)
                .findOne()
                .orElse(0L));
    }

    @Override
    public LockerPangMoveResult updateDolfiniLockerPang(long uid, int opt, long pang) {
        if (pang <= 0 || (opt != GamePackets.LOCKER_PANG_WITHDRAW && opt != GamePackets.LOCKER_PANG_DEPOSIT)) {
            return LockerPangMoveResult.fail(GamePackets.LOCKER_PANG_ERR_DEFAULT);
        }
        return jdbi.inTransaction(h -> {
            h.createUpdate("""
                            INSERT INTO pangya.pangya_dolfini_locker (uid, pang, locker)
                            VALUES (:uid, 0, 0)
                            ON CONFLICT (uid) DO NOTHING
                            """)
                    .bind("uid", uid)
                    .execute();
            long player = h.createQuery("""
                            SELECT COALESCE("Pang", 0) FROM pangya.user_info
                             WHERE "UID" = :uid
                             FOR UPDATE
                            """)
                    .bind("uid", uid)
                    .mapTo(Long.class)
                    .findOne()
                    .orElse(0L);
            long locker = h.createQuery("""
                            SELECT COALESCE(pang, 0) FROM pangya.pangya_dolfini_locker
                             WHERE uid = :uid
                             FOR UPDATE
                            """)
                    .bind("uid", uid)
                    .mapTo(Long.class)
                    .one();
            long nextPlayer;
            long nextLocker;
            if (opt == GamePackets.LOCKER_PANG_DEPOSIT) {
                if (pang > player) {
                    return LockerPangMoveResult.fail(GamePackets.LOCKER_PANG_DEPOSIT_ERR);
                }
                nextPlayer = player - pang;
                nextLocker = locker + pang;
            } else {
                if (pang > locker) {
                    return LockerPangMoveResult.fail(GamePackets.LOCKER_PANG_WITHDRAW_ERR);
                }
                nextPlayer = player + pang;
                nextLocker = locker - pang;
            }
            h.createUpdate("UPDATE pangya.user_info SET \"Pang\" = :pang WHERE \"UID\" = :uid")
                    .bind("pang", nextPlayer)
                    .bind("uid", uid)
                    .execute();
            h.createUpdate("UPDATE pangya.pangya_dolfini_locker SET pang = :pang WHERE uid = :uid")
                    .bind("pang", nextLocker)
                    .bind("uid", uid)
                    .execute();
            return new LockerPangMoveResult(0, nextPlayer, nextLocker, pang);
        });
    }

    @Override
    public OptionalLong addDolfiniLockerItem(long uid, int itemId) {
        if (itemId <= 0) {
            return OptionalLong.empty();
        }
        return jdbi.inTransaction(h -> {
            int updated = h.createUpdate("""
                            UPDATE pangya.pangya_item_warehouse
                               SET valid = 0
                             WHERE "UID" = :uid AND item_id = :id AND valid = 1
                            """)
                    .bind("uid", uid)
                    .bind("id", itemId)
                    .execute();
            if (updated <= 0) {
                return OptionalLong.empty();
            }
            long idx = h.createQuery("""
                            INSERT INTO pangya.pangya_dolfini_locker_item (uid, item_id, flag)
                            VALUES (:uid, :id, 1)
                            RETURNING idx
                            """)
                    .bind("uid", uid)
                    .bind("id", itemId)
                    .mapTo(Long.class)
                    .one();
            return OptionalLong.of(idx);
        });
    }

    @Override
    public Optional<GamePackets.WarehouseItem> removeDolfiniLockerItem(long uid, long index, int itemId) {
        if (index <= 0 || itemId <= 0) {
            return Optional.empty();
        }
        return jdbi.inTransaction(h -> {
            Integer stored = h.createQuery("""
                            SELECT item_id FROM pangya.pangya_dolfini_locker_item
                             WHERE uid = :uid AND idx = :idx AND flag = 1
                            """)
                    .bind("uid", uid)
                    .bind("idx", index)
                    .mapTo(Integer.class)
                    .findOne()
                    .orElse(null);
            if (stored == null || stored != itemId) {
                return Optional.empty();
            }
            h.createUpdate("""
                            UPDATE pangya.pangya_item_warehouse
                               SET valid = 1
                             WHERE "UID" = :uid AND item_id = :id
                            """)
                    .bind("uid", uid)
                    .bind("id", itemId)
                    .execute();
            h.createUpdate("""
                            UPDATE pangya.pangya_dolfini_locker_item
                               SET flag = 0
                             WHERE uid = :uid AND idx = :idx
                            """)
                    .bind("uid", uid)
                    .bind("idx", index)
                    .execute();
            GamePackets.WarehouseItem item = new GamePackets.WarehouseItem();
            item.id = itemId;
            return Optional.of(item);
        });
    }

    @Override
    public OptionalLong dolfiniLockerIndex(long uid, int itemId) {
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT idx FROM pangya.pangya_dolfini_locker_item
                         WHERE uid = :uid AND item_id = :id AND flag = 1
                         ORDER BY idx
                         LIMIT 1
                        """)
                .bind("uid", uid)
                .bind("id", itemId)
                .mapTo(Long.class)
                .findOne()
                .map(OptionalLong::of)
                .orElseGet(OptionalLong::empty));
    }

    @Override
    public void deleteDolfiniLockerByItemId(long uid, int itemId) {
        jdbi.useHandle(h -> h.createUpdate("""
                        DELETE FROM pangya.pangya_dolfini_locker_item
                         WHERE uid = :uid AND item_id = :id
                        """)
                .bind("uid", uid)
                .bind("id", itemId)
                .execute());
    }

    @Override
    public TutorialFlags tutorial(long uid) {
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT "Rookie", "Beginner", "Advancer"
                          FROM pangya.tutorial
                         WHERE "UID" = :uid
                        """)
                .bind("uid", uid)
                .map((rs, ctx) -> new TutorialFlags(
                        rs.getInt("Rookie"),
                        rs.getInt("Beginner"),
                        rs.getInt("Advancer")))
                .findOne()
                .orElse(new TutorialFlags(0, 0, 0)));
    }

    @Override
    public void updateTutorial(long uid, int rookie, int beginner, int advancer) {
        jdbi.useHandle(h -> h.createUpdate("""
                        INSERT INTO pangya.tutorial ("UID", "Rookie", "Beginner", "Advancer")
                        VALUES (:uid, :rookie, :beginner, :advancer)
                        ON CONFLICT ("UID") DO UPDATE SET
                            "Rookie" = :rookie,
                            "Beginner" = :beginner,
                            "Advancer" = :advancer
                        """)
                .bind("uid", uid)
                .bind("rookie", rookie)
                .bind("beginner", beginner)
                .bind("advancer", advancer)
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

    @Override
    public boolean deleteWarehouseById(long uid, int itemId) {
        return jdbi.withHandle(h -> h.createUpdate("""
                        UPDATE pangya.pangya_item_warehouse
                           SET valid = 0
                         WHERE "UID" = :uid AND item_id = :id AND valid = 1
                        """)
                .bind("uid", uid)
                .bind("id", itemId)
                .execute()) > 0;
    }

    @Override
    public OptionalLong partValorRental(int typeid) {
        var iff = org.pangya.protocol.iff.PangyaIffLoader.partIndex();
        if (!iff.isEmpty()) {
            return iff.valorRental(typeid);
        }
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT valor_rental FROM pangya.iff_part WHERE typeid = :typeid
                        """)
                .bind("typeid", typeid)
                .mapTo(Long.class)
                .findOne())
                .map(OptionalLong::of)
                .orElseGet(OptionalLong::empty);
    }

    @Override
    public void upsertPartValorRental(int typeid, long valorRental) {
        jdbi.useHandle(h -> h.createUpdate("""
                        INSERT INTO pangya.iff_part (typeid, valor_rental)
                        VALUES (:typeid, :valor)
                        ON CONFLICT (typeid) DO UPDATE SET valor_rental = EXCLUDED.valor_rental
                        """)
                .bind("typeid", typeid)
                .bind("valor", valorRental)
                .execute());
    }

    @Override
    public void deletePartIff(int typeid) {
        jdbi.useHandle(h -> h.createUpdate(
                        "DELETE FROM pangya.iff_part WHERE typeid = :typeid")
                .bind("typeid", typeid)
                .execute());
    }

    @Override
    public void setWarehouseEndDate(long uid, int itemId, Instant endDate) {
        jdbi.useHandle(h -> h.createUpdate("""
                        UPDATE pangya.pangya_item_warehouse
                           SET "EndDate" = :end
                         WHERE "UID" = :uid AND item_id = :id
                        """)
                .bind("uid", uid)
                .bind("id", itemId)
                .bind("end", Timestamp.from(endDate))
                .execute());
    }

    @Override
    public int addWarehouseItem(long uid, int typeid, int qntd) {
        return jdbi.inTransaction(h -> {
            Integer existing = h.createQuery("""
                            SELECT item_id FROM pangya.pangya_item_warehouse
                             WHERE "UID" = :uid AND typeid = :typeid AND valid = 1
                             ORDER BY item_id
                             LIMIT 1
                            """)
                    .bind("uid", uid)
                    .bind("typeid", typeid)
                    .mapTo(Integer.class)
                    .findOne()
                    .orElse(null);
            if (existing != null) {
                h.createUpdate("""
                                UPDATE pangya.pangya_item_warehouse
                                   SET "C0" = "C0" + :qntd
                                 WHERE item_id = :id
                                """)
                        .bind("qntd", qntd)
                        .bind("id", existing)
                        .execute();
                return existing;
            }
            return insertWarehouse(h, uid, ItemInitializer.WarehouseInitRow.simple(typeid, qntd));
        });
    }

    @Override
    public int addInitializedWarehouseItem(long uid, ItemInitializer.WarehouseInitRow row) {
        return jdbi.inTransaction(h -> {
            Integer existing = h.createQuery("""
                            SELECT item_id FROM pangya.pangya_item_warehouse
                             WHERE "UID" = :uid AND typeid = :typeid AND valid = 1
                             ORDER BY item_id
                             LIMIT 1
                            """)
                    .bind("uid", uid)
                    .bind("typeid", row.typeid())
                    .mapTo(Integer.class)
                    .findOne()
                    .orElse(null);
            int addC0 = row.c0() & 0xffff;
            if (existing != null) {
                h.createUpdate("""
                                UPDATE pangya.pangya_item_warehouse
                                   SET "C0" = "C0" + :c0
                                 WHERE item_id = :id
                                """)
                        .bind("c0", addC0)
                        .bind("id", existing)
                        .execute();
                return existing;
            }
            return insertWarehouse(h, uid, row);
        });
    }

    @Override
    public Optional<AwardInsert> addAwardItem(long uid, ItemInitializer.MailAwardRow row) {
        if (row == null || row.typeid() == 0) {
            return Optional.empty();
        }
        return jdbi.inTransaction(h -> insertAwardHandle(h, uid, row));
    }

    @Override
    public Optional<AwardInsert> grantBoxAward(long uid, int typeid, int drawQntd) {
        if (typeid == 0 || drawQntd <= 0) {
            return Optional.empty();
        }
        return jdbi.inTransaction(h -> {
            int level = h.createQuery("SELECT COALESCE(\"level\", 0) FROM pangya.user_info WHERE \"UID\" = :uid")
                    .bind("uid", uid)
                    .mapTo(Integer.class)
                    .findOne()
                    .orElse(0);
            ItemInitializer.InitContext ctx = new ItemInitializer.InitContext(level, false, false, true);
            Optional<ItemInitializer.MailAwardRow> row = ItemInitializer.initBoxAward(ctx, typeid, drawQntd);
            if (row.isEmpty()) {
                return Optional.empty();
            }
            return insertAwardHandle(h, uid, row.get());
        });
    }

    private Optional<AwardInsert> insertAwardHandle(
            org.jdbi.v3.core.Handle h, long uid, ItemInitializer.MailAwardRow row) {
        return switch (row.group()) {
            case GamePackets.IFF_GROUP_ITEM, GamePackets.IFF_GROUP_PART,
                    GamePackets.IFF_GROUP_BALL, GamePackets.IFF_GROUP_CLUBSET ->
                    insertWarehouseAwardHandle(h, uid, row);
            case GamePackets.IFF_GROUP_CADDIE -> insertCaddieAwardHandle(h, uid, row);
            case GamePackets.IFF_GROUP_MASCOT -> insertMascotAwardHandle(h, uid, row);
            case GamePackets.IFF_GROUP_CARD -> insertCardAwardHandle(h, uid, row.typeid(), row.qntd());
            case GamePackets.IFF_GROUP_CHARACTER -> insertCharacterAwardHandle(h, uid, row.typeid());
            case GamePackets.IFF_GROUP_SKIN_WAREHOUSE -> insertSkinAwardHandle(h, uid, row);
            case GamePackets.IFF_GROUP_CAD_ITEM -> insertCadItemAwardHandle(h, uid, row);
            default -> Optional.empty();
        };
    }

    private Optional<AwardInsert> insertSkinAwardHandle(
            org.jdbi.v3.core.Handle h, long uid, ItemInitializer.MailAwardRow row) {
        if (row.warehouse() == null || ownsWarehouseTypeid(h, uid, row.typeid())) {
            return Optional.empty();
        }
        int id = insertWarehouse(h, uid, row.warehouse());
        long seconds = ItemInitializer.stdaTimeSeconds(row.rentFlag(), row.caddiePeriodDays());
        if (seconds > 0) {
            h.createUpdate("""
                            UPDATE pangya.pangya_item_warehouse
                               SET "EndDate" = NOW() + (:secs * INTERVAL '1 second')
                             WHERE item_id = :id
                            """)
                    .bind("secs", seconds)
                    .bind("id", id)
                    .execute();
        }
        return Optional.of(new AwardInsert(id, 0, 1, 1));
    }

    private Optional<AwardInsert> insertCadItemAwardHandle(
            org.jdbi.v3.core.Handle h, long uid, ItemInitializer.MailAwardRow row) {
        int caddieTypeid = GamePackets.caddieBaseTypeid(row.typeid());
        int[] caddie = h.createQuery("""
                        SELECT item_id, parts_typeid,
                               EXTRACT(EPOCH FROM "parts_EndDate")::bigint AS parts_end
                          FROM pangya.pangya_caddie_information
                         WHERE "UID" = :uid AND typeid = :typeid AND "Valid" = 1
                         ORDER BY item_id
                         LIMIT 1
                        """)
                .bind("uid", uid)
                .bind("typeid", caddieTypeid)
                .map((rs, ctx) -> new int[] {
                        rs.getInt("item_id"),
                        rs.getInt("parts_typeid"),
                        rs.getLong("parts_end") > 0 ? (int) rs.getLong("parts_end") : 0
                })
                .findOne()
                .orElse(null);
        if (caddie == null) {
            return Optional.empty();
        }
        int itemId = caddie[0];
        int currentParts = caddie[1];
        java.time.Instant endInstant = cadItemPartsEnd(row, currentParts == row.typeid(), caddie[2]);
        h.createUpdate("""
                        UPDATE pangya.pangya_caddie_information
                           SET parts_typeid = :partsTypeid,
                               "parts_EndDate" = :endDate
                         WHERE item_id = :itemId
                        """)
                .bind("partsTypeid", row.typeid())
                .bind("endDate", endInstant == null ? null : java.sql.Timestamp.from(endInstant))
                .bind("itemId", itemId)
                .execute();
        return Optional.of(new AwardInsert(itemId, 0, 1, 1));
    }

    private Optional<AwardInsert> insertWarehouseAwardHandle(
            org.jdbi.v3.core.Handle h, long uid, ItemInitializer.MailAwardRow row) {
        if (row.warehouse() == null) {
            return Optional.empty();
        }
        ItemInitializer.WarehouseInitRow wh = row.warehouse();
        int[] existing = h.createQuery("""
                        SELECT item_id, "C0" FROM pangya.pangya_item_warehouse
                         WHERE "UID" = :uid AND typeid = :typeid AND valid = 1
                         ORDER BY item_id
                         LIMIT 1
                        """)
                .bind("uid", uid)
                .bind("typeid", wh.typeid())
                .map((rs, ctx) -> new int[] {rs.getInt("item_id"), rs.getInt("C0") & 0xffff})
                .findOne()
                .orElse(null);
        int addC0 = wh.c0() & 0xffff;
        int ant = existing == null ? 0 : existing[1];
        int id;
        if (existing != null) {
            id = existing[0];
            h.createUpdate("""
                            UPDATE pangya.pangya_item_warehouse
                               SET "C0" = "C0" + :c0
                             WHERE item_id = :itemId
                            """)
                    .bind("c0", addC0)
                    .bind("itemId", id)
                    .execute();
        } else {
            id = insertWarehouse(h, uid, wh);
        }
        long seconds = ItemInitializer.stdaTimeSeconds(row.rentFlag(), row.caddiePeriodDays());
        if (seconds > 0) {
            h.createUpdate("""
                            UPDATE pangya.pangya_item_warehouse
                               SET "EndDate" = NOW() + (:secs * INTERVAL '1 second')
                             WHERE item_id = :id
                            """)
                    .bind("secs", seconds)
                    .bind("id", id)
                    .execute();
        }
        return Optional.of(new AwardInsert(id, ant, ant + addC0, addC0));
    }

    private Optional<AwardInsert> insertCaddieAwardHandle(
            org.jdbi.v3.core.Handle h, long uid, ItemInitializer.MailAwardRow row) {
        if (ownsCaddieTypeid(h, uid, row.typeid())) {
            return Optional.empty();
        }
        int rentFlag = row.rentFlag() <= 0 ? 1 : row.rentFlag();
        int period = row.caddiePeriodDays();
        Integer id = h.createQuery("""
                        INSERT INTO pangya.pangya_caddie_information (
                            "UID", typeid, parts_typeid, gift_flag, "cLevel", "Exp",
                            "RegDate", "Period", "EndDate", "RentFlag", "Purchase",
                            "parts_EndDate", "CheckEnd", "Valid"
                        ) VALUES (
                            :uid, :typeid, 0, 1, 0, 0,
                            NOW(), :period,
                            CASE WHEN :rent = :holiday THEN NOW() + make_interval(days => :period) ELSE NULL END,
                            :rent, 0, NULL, 1, 1
                        )
                        RETURNING item_id
                        """)
                .bind("uid", uid)
                .bind("typeid", row.typeid())
                .bind("period", period)
                .bind("rent", rentFlag)
                .bind("holiday", GamePackets.CADDIE_RENT_HOLIDAY)
                .mapTo(Integer.class)
                .one();
        return Optional.of(new AwardInsert(id, 0, 1, 1));
    }

    private Optional<AwardInsert> insertMascotAwardHandle(
            org.jdbi.v3.core.Handle h, long uid, ItemInitializer.MailAwardRow row) {
        int[] existing = h.createQuery("""
                        SELECT item_id, "Price",
                               EXTRACT(EPOCH FROM "EndDate")::bigint AS end_unix
                          FROM pangya.pangya_mascot_info
                         WHERE "UID" = :uid AND typeid = :typeid AND "Valid" = 1
                         ORDER BY item_id
                         LIMIT 1
                        """)
                .bind("uid", uid)
                .bind("typeid", row.typeid())
                .map((rs, ctx) -> new int[] {
                        rs.getInt("item_id"),
                        rs.getInt("Price"),
                        rs.getLong("end_unix") > 0 ? (int) rs.getLong("end_unix") : 0
                })
                .findOne()
                .orElse(null);
        int timeDays = row.mascotTimeDays();
        if (existing != null) {
            if (timeDays <= 0) {
                return Optional.empty();
            }
            long base = existing[2] > 0
                    ? existing[2]
                    : java.time.Instant.now().getEpochSecond();
            java.time.Instant end = java.time.Instant.ofEpochSecond(base + timeDays * 86400L);
            h.createUpdate("""
                            UPDATE pangya.pangya_mascot_info
                               SET "Tipo" = 1,
                                   "Period" = :period,
                                   "EndDate" = :endDate
                             WHERE item_id = :id
                            """)
                    .bind("period", timeDays)
                    .bind("endDate", java.sql.Timestamp.from(end))
                    .bind("id", existing[0])
                    .execute();
            return Optional.of(new AwardInsert(existing[0], 0, 1, 1));
        }
        int price = org.pangya.protocol.iff.PangyaIffLoader.mascot(row.typeid())
                .map(org.pangya.protocol.iff.IffMascotRecord::changePrice)
                .orElseGet(() -> {
                    Integer sqlPrice = mascotChangePriceSql(row.typeid());
                    return sqlPrice != null ? sqlPrice : 0;
                });
        String message = row.mascotMessage() == null ? "" : row.mascotMessage();
        int tipo = row.mascotTipo();
        Integer id = h.createQuery("""
                        INSERT INTO pangya.pangya_mascot_info (
                            "UID", typeid, "mLevel", "mExp", "Flag", "Tipo",
                            "RegDate", "Period", "EndDate", "Message", "IsCash", "Price", "Valid"
                        ) VALUES (
                            :uid, :typeid, 0, 0, 0, :tipo,
                            NOW(), :period,
                            CASE WHEN :tipo = 1 AND :timeDays > 0
                                 THEN NOW() + make_interval(days => :timeDays)
                                 ELSE NULL END,
                            :message, 0, :price, 1
                        )
                        RETURNING item_id
                        """)
                .bind("uid", uid)
                .bind("typeid", row.typeid())
                .bind("tipo", tipo)
                .bind("period", timeDays)
                .bind("timeDays", timeDays)
                .bind("message", message)
                .bind("price", price)
                .mapTo(Integer.class)
                .one();
        return Optional.of(new AwardInsert(id, 0, 1, 1));
    }

    private Optional<AwardInsert> insertCardAwardHandle(
            org.jdbi.v3.core.Handle h, long uid, int typeid, int qntd) {
        if (qntd <= 0) {
            return Optional.empty();
        }
        int[] existing = h.createQuery("""
                        SELECT card_itemid, COALESCE("QNTD", 0) FROM pangya.pangya_card
                         WHERE "UID" = :uid AND card_typeid = :typeid
                         ORDER BY card_itemid
                         LIMIT 1
                        """)
                .bind("uid", uid)
                .bind("typeid", typeid)
                .map((rs, ctx) -> new int[] {rs.getInt("card_itemid"), rs.getInt(2)})
                .findOne()
                .orElse(null);
        int ant = existing == null ? 0 : existing[1];
        int id;
        if (existing != null) {
            id = existing[0];
            h.createUpdate("""
                            UPDATE pangya.pangya_card
                               SET "QNTD" = COALESCE("QNTD", 0) + :qntd
                             WHERE card_itemid = :id
                            """)
                    .bind("qntd", qntd)
                    .bind("id", id)
                    .execute();
        } else {
            id = h.createQuery("""
                            INSERT INTO pangya.pangya_card (
                                "UID", card_typeid, "QNTD", "GET_DT",
                                "Slot", "Efeito", "Efeito_Qntd", card_type, "USE_YN"
                            ) VALUES (
                                :uid, :typeid, :qntd, NOW(),
                                0, 0, 0, 1, 'N'
                            )
                            RETURNING card_itemid
                            """)
                    .bind("uid", uid)
                    .bind("typeid", typeid)
                    .bind("qntd", qntd)
                    .mapTo(Integer.class)
                    .one();
        }
        return Optional.of(new AwardInsert(id, ant, ant + qntd, qntd));
    }

    private Optional<AwardInsert> insertCharacterAwardHandle(
            org.jdbi.v3.core.Handle h, long uid, int typeid) {
        if (h.createQuery("""
                        SELECT 1 FROM pangya.pangya_character_information
                         WHERE "UID" = :uid AND typeid = :typeid
                         LIMIT 1
                        """)
                .bind("uid", uid)
                .bind("typeid", typeid)
                .mapTo(Integer.class)
                .findOne()
                .isPresent()) {
            return Optional.empty();
        }
        int[] parts = CharacterComboDefSql.defaultParts(h, typeid);
        var q = h.createQuery("""
                        INSERT INTO pangya.pangya_character_information (
                            typeid, "UID",
                            parts_1, parts_2, parts_3, parts_4, parts_5, parts_6, parts_7, parts_8,
                            parts_9, parts_10, parts_11, parts_12, parts_13, parts_14, parts_15, parts_16,
                            parts_17, parts_18, parts_19, parts_20, parts_21, parts_22, parts_23, parts_24,
                            default_hair, default_shirts, gift_flag,
                            "PCL0", "PCL1", "PCL2", "PCL3", "PCL4", "Purchase",
                            auxparts_1, auxparts_2, auxparts_3, auxparts_4, auxparts_5,
                            "CutIn_1", "CutIn_2", "CutIn_3", "CutIn_4", "Mastery"
                        ) VALUES (
                            :typeid, :uid,
                            :p1, :p2, :p3, :p4, :p5, :p6, :p7, :p8,
                            :p9, :p10, :p11, :p12, :p13, :p14, :p15, :p16,
                            :p17, :p18, :p19, :p20, :p21, :p22, :p23, :p24,
                            0, 0, 1,
                            0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0
                        )
                        RETURNING item_id
                        """)
                .bind("typeid", typeid)
                .bind("uid", uid);
        CharacterPartsBinder.bind(q, parts);
        int id = q.mapTo(Integer.class).one();
        return Optional.of(new AwardInsert(id, 0, 1, 1));
    }

    private static java.time.Instant cadItemPartsEnd(
            ItemInitializer.MailAwardRow row, boolean sameParts, int existingEndUnix) {
        int flagTime = row.rentFlag();
        int itemTime = row.caddiePeriodDays();
        if (itemTime <= 0) {
            return null;
        }
        long addSeconds = ItemInitializer.stdaTimeSeconds(flagTime, itemTime);
        if (addSeconds <= 0) {
            return null;
        }
        long base = sameParts && existingEndUnix > 0
                ? existingEndUnix
                : java.time.Instant.now().getEpochSecond();
        return java.time.Instant.ofEpochSecond(base + addSeconds);
    }

    @Override
    public boolean ownsAwardTypeid(long uid, int typeid) {
        return switch (GamePackets.itemGroupIdentify(typeid)) {
            case GamePackets.IFF_GROUP_CHARACTER ->
                    characters(uid).stream().anyMatch(c -> c.typeid == typeid);
            case GamePackets.IFF_GROUP_CADDIE -> ownsCaddieTypeid(uid, typeid);
            case GamePackets.IFF_GROUP_MASCOT -> mascots(uid).stream().anyMatch(m -> m.typeid == typeid);
            case GamePackets.IFF_GROUP_CARD -> cards(uid).stream().anyMatch(c -> c.typeid == typeid);
            case GamePackets.IFF_GROUP_SET_ITEM -> ownerSetItem(uid, typeid);
            case GamePackets.IFF_GROUP_SKIN_WAREHOUSE -> ownsWarehouseTypeid(uid, typeid);
            case GamePackets.IFF_GROUP_CAD_ITEM -> ownsCadItemParts(uid, typeid);
            default -> ownsWarehouseTypeid(uid, typeid);
        };
    }

    private boolean ownsCadItemParts(long uid, int cadItemTypeid) {
        int caddieTypeid = GamePackets.caddieBaseTypeid(cadItemTypeid);
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT 1 FROM pangya.pangya_caddie_information
                         WHERE "UID" = :uid AND typeid = :caddieTypeid
                           AND parts_typeid = :partsTypeid AND "Valid" = 1
                         LIMIT 1
                        """)
                .bind("uid", uid)
                .bind("caddieTypeid", caddieTypeid)
                .bind("partsTypeid", cadItemTypeid)
                .mapTo(Integer.class)
                .findOne()
                .isPresent());
    }

    private boolean ownsCaddieTypeid(org.jdbi.v3.core.Handle h, long uid, int typeid) {
        return h.createQuery("""
                        SELECT 1 FROM pangya.pangya_caddie_information
                         WHERE "UID" = :uid AND typeid = :typeid AND "Valid" = 1
                         LIMIT 1
                        """)
                .bind("uid", uid)
                .bind("typeid", typeid)
                .mapTo(Integer.class)
                .findOne()
                .isPresent();
    }

    private boolean ownsCaddieTypeid(long uid, int typeid) {
        return jdbi.withHandle(h -> ownsCaddieTypeid(h, uid, typeid));
    }

    @Override
    public boolean ownsWarehouseTypeid(long uid, int typeid) {
        return jdbi.withHandle(h -> ownsWarehouseTypeid(h, uid, typeid));
    }

    @Override
    public boolean itemCanOverlap(int typeid) {
        ShopItem item = shopItem(typeid).orElse(null);
        if (item != null) {
            return item.canOverlap();
        }
        int group = GamePackets.itemGroupIdentify(typeid);
        if (group == GamePackets.IFF_GROUP_CARD) {
            return true;
        }
        if (group == GamePackets.IFF_GROUP_CAD_ITEM) {
            return true;
        }
        return group == GamePackets.IFF_GROUP_ITEM || group == GamePackets.IFF_GROUP_BALL;
    }

    @Override
    public OptionalInt consumeWarehouseByTypeid(long uid, int typeid, int qntd) {
        if (qntd <= 0) {
            return OptionalInt.empty();
        }
        return jdbi.inTransaction(h -> {
            int[] row = h.createQuery("""
                            SELECT item_id, "C0" FROM pangya.pangya_item_warehouse
                             WHERE "UID" = :uid AND typeid = :typeid AND valid = 1
                             ORDER BY item_id
                             LIMIT 1
                            """)
                    .bind("uid", uid)
                    .bind("typeid", typeid)
                    .map((rs, ctx) -> new int[] {rs.getInt("item_id"), rs.getInt("C0") & 0xffff})
                    .findOne()
                    .orElse(null);
            if (row == null || row[1] < qntd) {
                return OptionalInt.empty();
            }
            int remaining = row[1] - qntd;
            if (remaining <= 0) {
                h.createUpdate("""
                                DELETE FROM pangya.pangya_item_warehouse
                                 WHERE item_id = :id
                                """)
                        .bind("id", row[0])
                        .execute();
                return OptionalInt.of(0);
            }
            h.createUpdate("""
                            UPDATE pangya.pangya_item_warehouse
                               SET "C0" = :c0
                             WHERE item_id = :id
                            """)
                    .bind("c0", remaining)
                    .bind("id", row[0])
                    .execute();
            return OptionalInt.of(remaining);
        });
    }

    private static int insertWarehouse(Handle h, long uid, ItemInitializer.WarehouseInitRow row) {
        return h.createQuery("""
                        INSERT INTO pangya.pangya_item_warehouse (
                            "UID", typeid, valid, "Gift_flag", flag,
                            "C0", "C1", "C2", "C3", "C4", "Purchase", "ItemType",
                            "ClubSet_WorkShop_Flag", "ClubSet_WorkShop_C0", "ClubSet_WorkShop_C1",
                            "ClubSet_WorkShop_C2", "ClubSet_WorkShop_C3", "ClubSet_WorkShop_C4",
                            "Mastery_Pts", "Recovery_Pts", "Level", "Up",
                            "Total_Mastery_Pts", "Mastery_Gasto"
                        ) VALUES (
                            :uid, :typeid, 1, 0, :flag,
                            :c0, :c1, :c2, :c3, :c4, :purchase, :itemType,
                            0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0
                        )
                        RETURNING item_id
                        """)
                .bind("uid", uid)
                .bind("typeid", row.typeid())
                .bind("flag", row.flag())
                .bind("c0", row.c0())
                .bind("c1", row.c1())
                .bind("c2", row.c2())
                .bind("c3", row.c3())
                .bind("c4", row.c4())
                .bind("purchase", row.purchase())
                .bind("itemType", row.itemType())
                .mapTo(Integer.class)
                .one();
    }

    private static int insertWarehouse(Handle h, long uid, int typeid, int qntd) {
        return insertWarehouse(h, uid, ItemInitializer.WarehouseInitRow.simple(typeid, qntd));
    }

    @Override
    public PapelPlayResult playPapel(long uid, boolean big) {
        return jdbi.inTransaction(h -> {
            List<int[]> catalog = h.createQuery("""
                            SELECT typeid, probabilidade, tipo
                              FROM pangya.pangya_papel_shop_item
                             WHERE active = 1 AND (numero = -1 OR numero = (
                                   SELECT "Numero" FROM pangya.pangya_papel_shop_config LIMIT 1))
                            """)
                    .map((rs, ctx) -> new int[] {
                            rs.getInt("typeid"), rs.getInt("probabilidade"), rs.getInt("tipo") })
                    .list();
            if (catalog.isEmpty()) {
                return PapelPlayResult.fail(GamePackets.PAPEL_PLAY_ERR_BALLS);
            }
            long price = h.createQuery(big
                            ? "SELECT COALESCE(\"Price_Big\", 0) FROM pangya.pangya_papel_shop_config LIMIT 1"
                            : "SELECT COALESCE(\"Price_Normal\", 0) FROM pangya.pangya_papel_shop_config LIMIT 1")
                    .mapTo(Long.class)
                    .findOne()
                    .orElse(0L);
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
            if (pang < price) {
                return PapelPlayResult.fail(GamePackets.PAPEL_PLAY_ERR_FUNDS);
            }
            ThreadLocalRandom rng = ThreadLocalRandom.current();
            int numBall = big ? GamePackets.PAPEL_BIG_BALLS
                    : GamePackets.PAPEL_MIN_BALL
                            + rng.nextInt(GamePackets.PAPEL_MAX_BALL - GamePackets.PAPEL_MIN_BALL + 1);
            int totalProb = 0;
            for (int[] row : catalog) {
                totalProb += Math.max(row[1], 1);
            }
            List<GamePackets.PapelBall> balls = new ArrayList<>();
            Map<Integer, Integer> merged = new LinkedHashMap<>();
            for (int i = 0; i < numBall; i++) {
                int pick = rng.nextInt(totalProb);
                int[] chosen = catalog.getFirst();
                int acc = 0;
                for (int[] row : catalog) {
                    acc += Math.max(row[1], 1);
                    if (pick < acc) {
                        chosen = row;
                        break;
                    }
                }
                int qntd = GamePackets.PAPEL_ITEM_MIN_QNTD
                        + rng.nextInt(GamePackets.PAPEL_ITEM_MAX_QNTD - GamePackets.PAPEL_ITEM_MIN_QNTD + 1);
                int color = rng.nextInt(GamePackets.PAPEL_COLOR_COUNT);
                balls.add(new GamePackets.PapelBall(color, chosen[0], 0, qntd, chosen[2]));
                merged.merge(chosen[0], qntd, Integer::sum);
            }
            List<GamePackets.PapelAward> awards = new ArrayList<>();
            for (Map.Entry<Integer, Integer> entry : merged.entrySet()) {
                int typeid = entry.getKey();
                int add = entry.getValue();
                Integer existingId = h.createQuery("""
                                SELECT item_id FROM pangya.pangya_item_warehouse
                                 WHERE "UID" = :uid AND typeid = :typeid AND valid = 1
                                 ORDER BY item_id
                                 LIMIT 1
                                """)
                        .bind("uid", uid)
                        .bind("typeid", typeid)
                        .mapTo(Integer.class)
                        .findOne()
                        .orElse(null);
                int ant;
                int id;
                if (existingId == null) {
                    ant = 0;
                    id = h.createQuery("""
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
                            .bind("qntd", add)
                            .mapTo(Integer.class)
                            .one();
                } else {
                    id = existingId;
                    ant = h.createQuery("""
                                    SELECT "C0" FROM pangya.pangya_item_warehouse
                                     WHERE item_id = :id
                                    """)
                            .bind("id", id)
                            .mapTo(Integer.class)
                            .one() & 0xffff;
                    h.createUpdate("""
                                    UPDATE pangya.pangya_item_warehouse
                                       SET "C0" = :c0
                                     WHERE item_id = :id
                                    """)
                            .bind("c0", ant + add)
                            .bind("id", id)
                            .execute();
                }
                awards.add(new GamePackets.PapelAward(
                        GamePackets.PAPEL_AWARD_TYPE, typeid, id, 0, ant, ant + add, add));
            }
            long pangAfter = pang - price;
            h.createUpdate("""
                            UPDATE pangya.user_info
                               SET "Pang" = :pang
                             WHERE "UID" = :uid
                            """)
                    .bind("pang", pangAfter)
                    .bind("uid", uid)
                    .execute();
            return new PapelPlayResult(0, balls, awards, pangAfter, cookie);
        });
    }

    @Override
    public CaddieHolidayResult payCaddieHoliday(long uid, int caddieId) {
        return jdbi.inTransaction(h -> {
            long pang = h.createQuery("SELECT COALESCE(\"Pang\", 0) FROM pangya.user_info WHERE \"UID\" = :uid")
                    .bind("uid", uid)
                    .mapTo(Long.class)
                    .findOne()
                    .orElse(0L);
            if (caddieId <= 0) {
                return CaddieHolidayResult.fail(pang);
            }
            Integer typeid = h.createQuery("""
                            SELECT typeid FROM pangya.pangya_caddie_information
                             WHERE "UID" = :uid AND item_id = :id AND "Valid" = 1 AND "RentFlag" = :rent
                            """)
                    .bind("uid", uid)
                    .bind("id", caddieId)
                    .bind("rent", GamePackets.CADDIE_RENT_HOLIDAY)
                    .mapTo(Integer.class)
                    .findOne()
                    .orElse(null);
            if (typeid == null) {
                return CaddieHolidayResult.fail(pang);
            }
            Integer price = caddieHolidayPrice(typeid);
            if (price == null || pang < price) {
                return CaddieHolidayResult.fail(pang);
            }
            long pangAfter = pang - price;
            h.createUpdate("""
                            UPDATE pangya.user_info
                               SET "Pang" = :pang
                             WHERE "UID" = :uid
                            """)
                    .bind("pang", pangAfter)
                    .bind("uid", uid)
                    .execute();
            h.createUpdate("""
                            UPDATE pangya.pangya_caddie_information
                               SET "EndDate" = NOW() + (:secs * INTERVAL '1 second')
                             WHERE item_id = :id AND "UID" = :uid
                            """)
                    .bind("secs", GamePackets.CADDIE_HOLIDAY_SECONDS)
                    .bind("id", caddieId)
                    .bind("uid", uid)
                    .execute();
            return new CaddieHolidayResult(0, caddieId, pangAfter);
        });
    }

    private Integer caddieHolidayPrice(int typeid) {
        return org.pangya.protocol.iff.PangyaIffLoader.caddie(typeid)
                .filter(org.pangya.protocol.iff.IffCaddieRecord::canPayHoliday)
                .map(org.pangya.protocol.iff.IffCaddieRecord::valorMensal)
                .orElseGet(() -> caddieHolidayPriceSql(typeid));
    }

    private Integer caddieHolidayPriceSql(int typeid) {
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT valor_mensal FROM pangya.iff_caddie
                         WHERE typeid = :typeid AND (is_cash = 1 OR valor_mensal > 0)
                        """)
                .bind("typeid", typeid)
                .mapTo(Integer.class)
                .findOne()
                .orElse(null));
    }

    private Integer mascotChangePrice(int typeid) {
        return org.pangya.protocol.iff.PangyaIffLoader.mascot(typeid)
                .filter(org.pangya.protocol.iff.IffMascotRecord::messageActive)
                .map(org.pangya.protocol.iff.IffMascotRecord::changePrice)
                .orElseGet(() -> mascotChangePriceSql(typeid));
    }

    private Integer mascotChangePriceSql(int typeid) {
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT change_price FROM pangya.iff_mascot
                         WHERE typeid = :typeid AND msg_active = 1
                        """)
                .bind("typeid", typeid)
                .mapTo(Integer.class)
                .findOne()
                .orElse(null));
    }

    @Override
    public MascotMessageResult changeMascotMessage(long uid, int mascotId, String message) {
        return jdbi.inTransaction(h -> {
            long pang = h.createQuery("SELECT COALESCE(\"Pang\", 0) FROM pangya.user_info WHERE \"UID\" = :uid")
                    .bind("uid", uid)
                    .mapTo(Long.class)
                    .findOne()
                    .orElse(0L);
            if (message == null || message.isEmpty() || message.length() > GamePackets.MASCOT_MSG_MAX) {
                return MascotMessageResult.fail(pang);
            }
            Integer typeid = h.createQuery("""
                            SELECT typeid FROM pangya.pangya_mascot_info
                             WHERE "UID" = :uid AND item_id = :id AND "Valid" = 1
                            """)
                    .bind("uid", uid)
                    .bind("id", mascotId)
                    .mapTo(Integer.class)
                    .findOne()
                    .orElse(null);
            if (typeid == null) {
                return MascotMessageResult.fail(pang);
            }
            Integer price = mascotChangePrice(typeid);
            if (price == null || pang < price) {
                return MascotMessageResult.fail(pang);
            }
            long pangAfter = pang - price;
            h.createUpdate("""
                            UPDATE pangya.user_info
                               SET "Pang" = :pang
                             WHERE "UID" = :uid
                            """)
                    .bind("pang", pangAfter)
                    .bind("uid", uid)
                    .execute();
            h.createUpdate("""
                            UPDATE pangya.pangya_mascot_info
                               SET "Message" = :msg
                             WHERE item_id = :id AND "UID" = :uid
                            """)
                    .bind("msg", message)
                    .bind("id", mascotId)
                    .bind("uid", uid)
                    .execute();
            return new MascotMessageResult(0, mascotId, message, pangAfter);
        });
    }

    @Override
    public CadieExchangeResult cadieExchange(
            long uid, int seq, int requested, int level, int[] typeids, int[] ids) {
        return jdbi.inTransaction(h -> {
            CadieMagicBoxRow box = loadCadieMagicBox(h, seq + 1);
            if (box == null) {
                return CadieExchangeResult.fail(GamePackets.CADIE_ERR_IFF);
            }
            if (level < box.level()) {
                return CadieExchangeResult.fail(GamePackets.CADIE_ERR_LEVEL);
            }
            if (requested <= 0) {
                return CadieExchangeResult.fail(GamePackets.CADIE_ERR_EXCHANGE);
            }
            int count = typeids == null ? 0 : typeids.length;
            int[] have = new int[count];
            int[] need = new int[count];
            for (int i = 0; i < count; i++) {
                int tradeTypeid = box.tradeTypeids()[i];
                int tradeQntd = box.tradeQntds()[i];
                if (tradeTypeid != 0 && tradeTypeid != typeids[i]) {
                    return CadieExchangeResult.fail(GamePackets.CADIE_ERR_MISMATCH);
                }
                if (tradeTypeid == 0 || tradeQntd <= 0) {
                    continue;
                }
                need[i] = tradeQntd * requested;
                Integer c0 = h.createQuery("""
                                SELECT "C0" FROM pangya.pangya_item_warehouse
                                 WHERE "UID" = :uid AND item_id = :id AND typeid = :typeid AND valid = 1
                                """)
                        .bind("uid", uid)
                        .bind("id", ids[i])
                        .bind("typeid", typeids[i])
                        .mapTo(Integer.class)
                        .findOne()
                        .orElse(null);
                if (c0 == null || (c0 & 0xffff) < need[i]) {
                    return CadieExchangeResult.fail(GamePackets.CADIE_ERR_EXCHANGE);
                }
                have[i] = c0 & 0xffff;
            }
            List<GamePackets.PapelAward> awards = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                if (need[i] <= 0) {
                    continue;
                }
                int ant = have[i];
                int dep = ant - need[i];
                h.createUpdate("""
                                UPDATE pangya.pangya_item_warehouse
                                   SET "C0" = :c0
                                 WHERE item_id = :id
                                """)
                        .bind("c0", dep)
                        .bind("id", ids[i])
                        .execute();
                awards.add(new GamePackets.PapelAward(
                        GamePackets.PAPEL_AWARD_TYPE, typeids[i], ids[i], 0, ant, dep, -need[i]));
            }
            int receiveTypeid = box.receiveTypeid();
            int receiveUnitQntd = box.receiveQntd();
            if (box.boxRandomId() > 0 && org.pangya.protocol.iff.PangyaIffLoader.source().isPresent()) {
                var random = org.pangya.protocol.iff.PangyaIffLoader.spinCadieMagicBoxRandom(box.boxRandomId());
                if (random.isEmpty()) {
                    return CadieExchangeResult.fail(GamePackets.CADIE_ERR_EXCHANGE);
                }
                receiveTypeid = random.get().itemTypeid();
                receiveUnitQntd = random.get().qty();
            }
            int add = receiveUnitQntd * requested;
            Integer existingId = h.createQuery("""
                            SELECT item_id FROM pangya.pangya_item_warehouse
                             WHERE "UID" = :uid AND typeid = :typeid AND valid = 1
                             ORDER BY item_id
                             LIMIT 1
                            """)
                    .bind("uid", uid)
                    .bind("typeid", receiveTypeid)
                    .mapTo(Integer.class)
                    .findOne()
                    .orElse(null);
            int ant;
            int id;
            if (existingId == null) {
                ant = 0;
                id = h.createQuery("""
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
                        .bind("typeid", receiveTypeid)
                        .bind("qntd", add)
                        .mapTo(Integer.class)
                        .one();
            } else {
                id = existingId;
                ant = h.createQuery("""
                                SELECT "C0" FROM pangya.pangya_item_warehouse
                                 WHERE item_id = :id
                                """)
                        .bind("id", id)
                        .mapTo(Integer.class)
                        .one() & 0xffff;
                h.createUpdate("""
                                UPDATE pangya.pangya_item_warehouse
                                   SET "C0" = :c0
                                 WHERE item_id = :id
                                """)
                        .bind("c0", ant + add)
                        .bind("id", id)
                        .execute();
            }
            awards.add(new GamePackets.PapelAward(
                    GamePackets.PAPEL_AWARD_TYPE, receiveTypeid, id, 0, ant, ant + add, add));
            return new CadieExchangeResult(0, seq, awards, receiveTypeid, id, add, ant + add, 0);
        });
    }

    @Override
    public int addCard(long uid, int typeid, int qntd) {
        return jdbi.inTransaction(h -> {
            Integer existing = h.createQuery("""
                            SELECT card_itemid FROM pangya.pangya_card
                             WHERE "UID" = :uid AND card_typeid = :typeid
                             ORDER BY card_itemid
                             LIMIT 1
                            """)
                    .bind("uid", uid)
                    .bind("typeid", typeid)
                    .mapTo(Integer.class)
                    .findOne()
                    .orElse(null);
            if (existing != null) {
                h.createUpdate("""
                                UPDATE pangya.pangya_card
                                   SET "QNTD" = COALESCE("QNTD", 0) + :qntd
                                 WHERE card_itemid = :id
                                """)
                        .bind("qntd", qntd)
                        .bind("id", existing)
                        .execute();
                return existing;
            }
            return h.createQuery("""
                            INSERT INTO pangya.pangya_card (
                                "UID", card_typeid, "QNTD", "GET_DT",
                                "Slot", "Efeito", "Efeito_Qntd", card_type, "USE_YN"
                            ) VALUES (
                                :uid, :typeid, :qntd, NOW(),
                                0, 0, 0, 0, 'N'
                            )
                            RETURNING card_itemid
                            """)
                    .bind("uid", uid)
                    .bind("typeid", typeid)
                    .bind("qntd", qntd)
                    .mapTo(Integer.class)
                    .one();
        });
    }

    @Override
    public void deleteCardByTypeid(long uid, int typeid) {
        jdbi.useHandle(h -> h.createUpdate("""
                        DELETE FROM pangya.pangya_card
                         WHERE "UID" = :uid AND card_typeid = :typeid
                        """)
                .bind("uid", uid)
                .bind("typeid", typeid)
                .execute());
    }

    @Override
    public LoloComposeResult loloCompose(long uid, long clientPang, int t0, int t1, int t2) {
        return jdbi.inTransaction(h -> {
            int[] typeids = {t0, t1, t2};
            Map<Integer, Integer> removeById = new LinkedHashMap<>();
            Map<Integer, Integer> typeidById = new LinkedHashMap<>();
            Map<Integer, Integer> haveById = new LinkedHashMap<>();
            long cost = 0;
            for (int typeid : typeids) {
                Integer rarity = h.createQuery("""
                                SELECT rarity FROM pangya.iff_card WHERE typeid = :typeid
                                """)
                        .bind("typeid", typeid)
                        .mapTo(Integer.class)
                        .findOne()
                        .orElse(null);
                if (rarity == null) {
                    return LoloComposeResult.fail(GamePackets.LOLO_ERR_IFF);
                }
                if (rarity == GamePackets.CARD_TYPE_SECRET) {
                    return LoloComposeResult.fail(GamePackets.LOLO_ERR_SECRET);
                }
                int[] owned = h.createQuery("""
                                SELECT card_itemid, COALESCE("QNTD", 0) AS qntd
                                  FROM pangya.pangya_card
                                 WHERE "UID" = :uid AND card_typeid = :typeid
                                 ORDER BY card_itemid
                                 LIMIT 1
                                """)
                        .bind("uid", uid)
                        .bind("typeid", typeid)
                        .map((rs, ctx) -> new int[] {rs.getInt("card_itemid"), rs.getInt("qntd")})
                        .findOne()
                        .orElse(null);
                if (owned == null) {
                    return LoloComposeResult.fail(GamePackets.LOLO_ERR_OWN);
                }
                if (owned[1] < 1) {
                    return LoloComposeResult.fail(GamePackets.LOLO_ERR_QNTD);
                }
                removeById.merge(owned[0], 1, Integer::sum);
                typeidById.put(owned[0], typeid);
                haveById.put(owned[0], owned[1]);
                cost += GamePackets.loloPang(rarity);
            }
            if (cost != clientPang) {
                return LoloComposeResult.fail(GamePackets.LOLO_ERR_PANG);
            }
            List<int[]> pool = h.createQuery("""
                            SELECT typeid, rarity, probabilidade FROM pangya.iff_card
                            """)
                    .map((rs, ctx) -> new int[] {
                            rs.getInt("typeid"), rs.getInt("rarity"), rs.getInt("probabilidade")
                    })
                    .list();
            int total = 0;
            for (int[] row : pool) {
                total += Math.max(row[2], 0);
            }
            if (total <= 0) {
                return LoloComposeResult.fail(GamePackets.LOLO_ERR_DRAW);
            }
            int pick = ThreadLocalRandom.current().nextInt(total);
            int drawnTypeid = 0;
            int drawnTipo = 0;
            int walk = 0;
            for (int[] row : pool) {
                walk += Math.max(row[2], 0);
                if (pick < walk) {
                    drawnTypeid = row[0];
                    drawnTipo = row[1];
                    break;
                }
            }
            if (drawnTypeid == 0) {
                return LoloComposeResult.fail(GamePackets.LOLO_ERR_DRAW);
            }
            for (Map.Entry<Integer, Integer> e : removeById.entrySet()) {
                if (haveById.getOrDefault(e.getKey(), 0) < e.getValue()) {
                    return LoloComposeResult.fail(GamePackets.LOLO_ERR_REMOVE);
                }
            }
            List<GamePackets.PapelAward> awards = new ArrayList<>();
            for (Map.Entry<Integer, Integer> e : removeById.entrySet()) {
                int id = e.getKey();
                int need = e.getValue();
                int ant = haveById.get(id);
                int dep = ant - need;
                h.createUpdate("""
                                UPDATE pangya.pangya_card
                                   SET "QNTD" = :qntd
                                 WHERE card_itemid = :id
                                """)
                        .bind("qntd", dep)
                        .bind("id", id)
                        .execute();
                awards.add(new GamePackets.PapelAward(
                        GamePackets.PAPEL_AWARD_TYPE, typeidById.get(id), id, 0, ant, dep, -need));
            }
            Integer existingId = h.createQuery("""
                            SELECT card_itemid FROM pangya.pangya_card
                             WHERE "UID" = :uid AND card_typeid = :typeid
                             ORDER BY card_itemid
                             LIMIT 1
                            """)
                    .bind("uid", uid)
                    .bind("typeid", drawnTypeid)
                    .mapTo(Integer.class)
                    .findOne()
                    .orElse(null);
            int ant;
            int id;
            if (existingId == null) {
                ant = 0;
                id = h.createQuery("""
                                INSERT INTO pangya.pangya_card (
                                    "UID", card_typeid, "QNTD", "GET_DT",
                                    "Slot", "Efeito", "Efeito_Qntd", card_type, "USE_YN"
                                ) VALUES (
                                    :uid, :typeid, 1, NOW(),
                                    0, 0, 0, :tipo, 'N'
                                )
                                RETURNING card_itemid
                                """)
                        .bind("uid", uid)
                        .bind("typeid", drawnTypeid)
                        .bind("tipo", drawnTipo)
                        .mapTo(Integer.class)
                        .one();
            } else {
                id = existingId;
                ant = h.createQuery("""
                                SELECT COALESCE("QNTD", 0) FROM pangya.pangya_card
                                 WHERE card_itemid = :id
                                """)
                        .bind("id", id)
                        .mapTo(Integer.class)
                        .one();
                h.createUpdate("""
                                UPDATE pangya.pangya_card
                                   SET "QNTD" = :qntd, card_type = :tipo
                                 WHERE card_itemid = :id
                                """)
                        .bind("qntd", ant + 1)
                        .bind("tipo", drawnTipo)
                        .bind("id", id)
                        .execute();
            }
            awards.add(new GamePackets.PapelAward(
                    GamePackets.PAPEL_AWARD_TYPE, drawnTypeid, id, 0, ant, ant + 1, 1));
            long pang = h.createQuery("SELECT COALESCE(\"Pang\", 0) FROM pangya.user_info WHERE \"UID\" = :uid")
                    .bind("uid", uid)
                    .mapTo(Long.class)
                    .findOne()
                    .orElse(0L);
            long pangAfter = pang - cost;
            h.createUpdate("""
                            UPDATE pangya.user_info
                               SET "Pang" = :pang
                             WHERE "UID" = :uid
                            """)
                    .bind("pang", pangAfter)
                    .bind("uid", uid)
                    .execute();
            return new LoloComposeResult(0, pangAfter, cost, awards, drawnTipo, drawnTypeid);
        });
    }

    @Override
    public CharMasteryResult expandCharacterMastery(long uid, int typeid, int id, int level) {
        return jdbi.inTransaction(h -> {
            Integer current = h.createQuery("""
                            SELECT "Mastery" FROM pangya.pangya_character_information
                             WHERE "UID" = :uid AND item_id = :id AND typeid = :typeid
                            """)
                    .bind("uid", uid)
                    .bind("id", id)
                    .bind("typeid", typeid)
                    .mapTo(Integer.class)
                    .findOne()
                    .orElse(null);
            if (current == null) {
                return CharMasteryResult.fail(GamePackets.CHAR_MASTERY_ERR_CHAR);
            }
            int seq = current + 1;
            var iffRows = org.pangya.protocol.iff.PangyaIffLoader.characterMastery(typeid);
            if (iffRows.isPresent()) {
                return expandCharacterMasteryFromIff(h, uid, typeid, id, level, current, seq, iffRows.get());
            }
            int count = h.createQuery("""
                            SELECT COUNT(*) FROM pangya.iff_character_mastery WHERE typeid = :typeid
                            """)
                    .bind("typeid", typeid)
                    .mapTo(Integer.class)
                    .one();
            if (count == 0) {
                return CharMasteryResult.fail(GamePackets.CHAR_MASTERY_ERR_IFF);
            }
            if (current + 1 > count) {
                return CharMasteryResult.fail(GamePackets.CHAR_MASTERY_ERR_MAX);
            }
            int[] row = h.createQuery("""
                            SELECT seq, level, cond0_typeid, cond0_qntd, cond1_typeid, cond1_qntd,
                                   cond2_typeid, cond2_qntd, cond3_typeid, cond3_qntd,
                                   cond4_typeid, cond4_qntd
                              FROM pangya.iff_character_mastery
                             WHERE typeid = :typeid AND seq = :seq
                            """)
                    .bind("typeid", typeid)
                    .bind("seq", seq)
                    .map((rs, ctx) -> new int[] {
                            rs.getInt("seq"),
                            rs.getInt("level"),
                            rs.getInt("cond0_typeid"), rs.getInt("cond0_qntd"),
                            rs.getInt("cond1_typeid"), rs.getInt("cond1_qntd"),
                            rs.getInt("cond2_typeid"), rs.getInt("cond2_qntd"),
                            rs.getInt("cond3_typeid"), rs.getInt("cond3_qntd"),
                            rs.getInt("cond4_typeid"), rs.getInt("cond4_qntd")
                    })
                    .findOne()
                    .orElse(null);
            if (row == null || row[0] != seq) {
                return CharMasteryResult.fail(GamePackets.CHAR_MASTERY_ERR_SEQ);
            }
            if (row[1] > level) {
                return CharMasteryResult.fail(GamePackets.CHAR_MASTERY_ERR_LEVEL);
            }
            return finishCharacterMasteryExpand(h, uid, typeid, id, current, row);
        });
    }

    private CharMasteryResult expandCharacterMasteryFromIff(
            Handle h,
            long uid,
            int typeid,
            int id,
            int level,
            int current,
            int seq,
            List<org.pangya.protocol.iff.IffCharacterMasteryRecord> rows) {
        if (rows.isEmpty()) {
            return CharMasteryResult.fail(GamePackets.CHAR_MASTERY_ERR_IFF);
        }
        if (current + 1 > rows.size()) {
            return CharMasteryResult.fail(GamePackets.CHAR_MASTERY_ERR_MAX);
        }
        org.pangya.protocol.iff.IffCharacterMasteryRecord row = rows.stream()
                .filter(r -> r.seq() == seq)
                .findFirst()
                .orElse(null);
        if (row == null) {
            return CharMasteryResult.fail(GamePackets.CHAR_MASTERY_ERR_SEQ);
        }
        if (row.level() > level) {
            return CharMasteryResult.fail(GamePackets.CHAR_MASTERY_ERR_LEVEL);
        }
        int[] flat = new int[12];
        flat[0] = row.seq();
        flat[1] = row.level();
        for (int i = 0; i < org.pangya.protocol.iff.IffCharacterMasteryRecord.CONDITION_SLOTS; i++) {
            flat[2 + i * 2] = row.conditionTypeid()[i];
            flat[3 + i * 2] = row.conditionQntd()[i];
        }
        return finishCharacterMasteryExpand(h, uid, typeid, id, current, flat);
    }

    private CharMasteryResult finishCharacterMasteryExpand(
            Handle h, long uid, int typeid, int id, int current, int[] row) {
        List<int[]> consume = new ArrayList<>();
        for (int i = 0; i < org.pangya.protocol.iff.IffCharacterMasteryRecord.CONDITION_SLOTS; i++) {
            int condTypeid = row[2 + i * 2];
            int condQntd = row[3 + i * 2];
            if (condTypeid <= 0 || condQntd <= 0) {
                continue;
            }
            if (GamePackets.itemGroupIdentify(condTypeid) != GamePackets.IFF_GROUP_ITEM) {
                return CharMasteryResult.fail(GamePackets.CHAR_MASTERY_ERR_COND);
            }
            int[] owned = h.createQuery("""
                            SELECT item_id, "C0" FROM pangya.pangya_item_warehouse
                             WHERE "UID" = :uid AND typeid = :typeid AND valid = 1
                             ORDER BY item_id
                             LIMIT 1
                            """)
                    .bind("uid", uid)
                    .bind("typeid", condTypeid)
                    .map((rs, ctx) -> new int[] {rs.getInt("item_id"), rs.getInt("C0") & 0xffff})
                    .findOne()
                    .orElse(null);
            if (owned == null) {
                return CharMasteryResult.fail(GamePackets.CHAR_MASTERY_ERR_ITEM);
            }
            if (owned[1] < condQntd) {
                return CharMasteryResult.fail(GamePackets.CHAR_MASTERY_ERR_QNTD);
            }
            consume.add(new int[] {owned[0], condTypeid, owned[1], condQntd});
        }
        List<GamePackets.PapelAward> awards = new ArrayList<>();
        for (int[] item : consume) {
            int ant = item[2];
            int need = item[3];
            int dep = ant - need;
            h.createUpdate("""
                            UPDATE pangya.pangya_item_warehouse
                               SET "C0" = :c0
                             WHERE item_id = :id
                            """)
                    .bind("c0", dep)
                    .bind("id", item[0])
                    .execute();
            awards.add(new GamePackets.PapelAward(
                    GamePackets.PAPEL_AWARD_TYPE, item[1], item[0], 0, ant, dep, -need));
        }
        int mastery = current + 1;
        h.createUpdate("""
                        UPDATE pangya.pangya_character_information
                           SET "Mastery" = :mastery
                         WHERE item_id = :id
                        """)
                .bind("mastery", mastery)
                .bind("id", id)
                .execute();
        awards.add(new GamePackets.PapelAward(
                GamePackets.CHAR_MASTERY_AWARD_TYPE, typeid, id, 0, 0, 0, 0, mastery));
        return new CharMasteryResult(0, awards, mastery);
    }

    @Override
    public CharStatsResult characterStatsUp(long uid, int stat, GamePackets.CharacterInfo client, int level) {
        return jdbi.inTransaction(h -> {
            GamePackets.CharacterInfo pCi = loadCharacter(h, uid, client.id, client.typeid);
            if (pCi == null) {
                return CharStatsResult.fail(GamePackets.CHAR_STATS_UP_ERR_CHAR);
            }
            int[] iffPcl = org.pangya.protocol.iff.PangyaIffLoader.character(pCi.typeid)
                    .map(org.pangya.protocol.iff.IffCharacterRecord::pclMax)
                    .orElseGet(() -> h.createQuery("""
                            SELECT pcl0, pcl1, pcl2, pcl3, pcl4
                              FROM pangya.iff_character
                             WHERE typeid = :typeid
                            """)
                            .bind("typeid", pCi.typeid)
                            .map((rs, ctx) -> new int[] {
                                    rs.getInt("pcl0"), rs.getInt("pcl1"), rs.getInt("pcl2"),
                                    rs.getInt("pcl3"), rs.getInt("pcl4")
                            })
                            .findOne()
                            .orElse(null));
            if (iffPcl == null) {
                return CharStatsResult.fail(GamePackets.CHAR_STATS_UP_ERR_CHAR_IFF);
            }
            if (stat > GamePackets.CHAR_STATS_CURVE) {
                return CharStatsResult.fail(GamePackets.CHAR_STATS_UP_ERR_STAT);
            }
            int bonus = 0;
            if (stat == GamePackets.CHAR_STATS_POWER) {
                bonus += (level - 1) / 5;
            }
            List<Integer> masteryStats;
            var iffMastery = org.pangya.protocol.iff.PangyaIffLoader.characterMastery(pCi.typeid);
            if (iffMastery.isPresent()) {
                masteryStats = iffMastery.get().stream()
                        .map(org.pangya.protocol.iff.IffCharacterMasteryRecord::stats)
                        .toList();
            } else {
                masteryStats = h.createQuery("""
                                SELECT stats FROM pangya.iff_character_mastery
                                 WHERE typeid = :typeid
                                 ORDER BY seq
                                """)
                        .bind("typeid", pCi.typeid)
                        .mapTo(Integer.class)
                        .list();
            }
            if (masteryStats.isEmpty()) {
                return CharStatsResult.fail(GamePackets.CHAR_STATS_UP_ERR_MASTERY);
            }
            if (masteryStats.size() < pCi.mastery) {
                return CharStatsResult.fail(GamePackets.CHAR_STATS_UP_ERR_MASTERY_VAL);
            }
            int extras = 0;
            for (int i = 0; i < pCi.mastery; i++) {
                if (masteryStats.get(i) - 1 == stat) {
                    extras++;
                }
            }
            int limit = iffPcl[stat] + extras + bonus;
            int current = pCi.pcl[stat] & 0xff;
            if (current > limit) {
                return CharStatsResult.fail(GamePackets.CHAR_STATS_UP_ERR_LIMIT);
            }
            int enchantTypeid = GamePackets.enchantTypeid(stat, current);
            OptionalLong costOpt = enchantPang(enchantTypeid);
            if (costOpt.isEmpty()) {
                return CharStatsResult.fail(GamePackets.CHAR_STATS_UP_ERR_ENCHANT);
            }
            long cost = costOpt.getAsLong();
            if (cost <= 0) {
                throw new IllegalStateException("enchant pang " + cost);
            }
            long pang = h.createQuery("SELECT COALESCE(\"Pang\", 0) FROM pangya.user_info WHERE \"UID\" = :uid")
                    .bind("uid", uid)
                    .mapTo(Long.class)
                    .findOne()
                    .orElse(0L);
            if (pang < cost) {
                throw new IllegalStateException("pang " + pang + " < " + cost);
            }
            byte[] pcl = pCi.pcl.clone();
            pcl[stat] = (byte) (current + 1);
            long pangAfter = pang - cost;
            h.createUpdate("""
                            UPDATE pangya.user_info
                               SET "Pang" = :pang
                             WHERE "UID" = :uid
                            """)
                    .bind("pang", pangAfter)
                    .bind("uid", uid)
                    .execute();
            h.createUpdate("""
                            UPDATE pangya.pangya_character_information
                               SET "PCL0" = :p0, "PCL1" = :p1, "PCL2" = :p2, "PCL3" = :p3, "PCL4" = :p4
                             WHERE item_id = :id
                            """)
                    .bind("p0", pcl[0] & 0xff).bind("p1", pcl[1] & 0xff)
                    .bind("p2", pcl[2] & 0xff).bind("p3", pcl[3] & 0xff)
                    .bind("p4", pcl[4] & 0xff)
                    .bind("id", pCi.id)
                    .execute();
            return new CharStatsResult(0, pangAfter, cost, pcl, stat, pCi.typeid, pCi.id);
        });
    }

    @Override
    public CharStatsResult characterStatsDown(long uid, int stat, GamePackets.CharacterInfo client) {
        return jdbi.inTransaction(h -> {
            GamePackets.CharacterInfo pCi = loadCharacter(h, uid, client.id, client.typeid);
            if (pCi == null) {
                return CharStatsResult.fail(GamePackets.CHAR_STATS_DOWN_ERR_CHAR);
            }
            boolean hasIff = org.pangya.protocol.iff.PangyaIffLoader.character(pCi.typeid)
                    .isPresent()
                    || h.createQuery("SELECT 1 FROM pangya.iff_character WHERE typeid = :typeid")
                    .bind("typeid", pCi.typeid)
                    .mapTo(Integer.class)
                    .findOne()
                    .isPresent();
            if (!hasIff) {
                return CharStatsResult.fail(GamePackets.CHAR_STATS_DOWN_ERR_CHAR_IFF);
            }
            if (stat > GamePackets.CHAR_STATS_CURVE) {
                return CharStatsResult.fail(GamePackets.CHAR_STATS_DOWN_ERR_STAT);
            }
            int current = pCi.pcl[stat] & 0xff;
            if (current == 0) {
                return CharStatsResult.fail(GamePackets.CHAR_STATS_DOWN_ERR_EMPTY);
            }
            byte[] pcl = pCi.pcl.clone();
            pcl[stat] = (byte) (current - 1);
            h.createUpdate("""
                            UPDATE pangya.pangya_character_information
                               SET "PCL0" = :p0, "PCL1" = :p1, "PCL2" = :p2, "PCL3" = :p3, "PCL4" = :p4
                             WHERE item_id = :id
                            """)
                    .bind("p0", pcl[0] & 0xff).bind("p1", pcl[1] & 0xff)
                    .bind("p2", pcl[2] & 0xff).bind("p3", pcl[3] & 0xff)
                    .bind("p4", pcl[4] & 0xff)
                    .bind("id", pCi.id)
                    .execute();
            return new CharStatsResult(0, 0, 0, pcl, stat, pCi.typeid, pCi.id);
        });
    }

    private static GamePackets.CharacterInfo loadCharacter(Handle h, long uid, int id, int typeid) {
        return h.createQuery("""
                        SELECT item_id, typeid, "PCL0", "PCL1", "PCL2", "PCL3", "PCL4", "Mastery"
                          FROM pangya.pangya_character_information
                         WHERE "UID" = :uid AND item_id = :id AND typeid = :typeid
                        """)
                .bind("uid", uid)
                .bind("id", id)
                .bind("typeid", typeid)
                .map((rs, ctx) -> {
                    GamePackets.CharacterInfo c = new GamePackets.CharacterInfo();
                    c.id = rs.getInt("item_id");
                    c.typeid = rs.getInt("typeid");
                    for (int i = 0; i < 5; i++) {
                        c.pcl[i] = (byte) rs.getInt("PCL" + i);
                    }
                    c.mastery = rs.getInt("Mastery");
                    return c;
                })
                .findOne()
                .orElse(null);
    }

    @Override
    public CharCardResult characterCardEquip(
            long uid, int charTypeid, int charId, int cardTypeid, int cardId, int slot) {
        return jdbi.inTransaction(h -> {
            int[] iff = h.createQuery("""
                            SELECT efeito, efeito_qntd FROM pangya.iff_card WHERE typeid = :typeid
                            """)
                    .bind("typeid", cardTypeid)
                    .map((rs, ctx) -> new int[] {rs.getInt("efeito"), rs.getInt("efeito_qntd")})
                    .findOne()
                    .orElse(null);
            if (iff == null) {
                return CharCardResult.fail(GamePackets.CHAR_CARD_ERR_IFF);
            }
            GamePackets.CharacterInfo pCi = loadCharacter(h, uid, charId, charTypeid);
            if (pCi == null) {
                return CharCardResult.fail(GamePackets.CHAR_CARD_ERR_CHAR);
            }
            int[] owned = h.createQuery("""
                            SELECT card_itemid, COALESCE("QNTD", 0) FROM pangya.pangya_card
                             WHERE "UID" = :uid AND card_itemid = :id AND card_typeid = :typeid
                            """)
                    .bind("uid", uid)
                    .bind("id", cardId)
                    .bind("typeid", cardTypeid)
                    .map((rs, ctx) -> new int[] {rs.getInt(1), rs.getInt(2)})
                    .findOne()
                    .orElse(null);
            if (owned == null || owned[1] < 1) {
                return CharCardResult.fail(GamePackets.CHAR_CARD_ERR_OWN);
            }
            if (slot == 4 || slot == 8) {
                return CharCardResult.fail(GamePackets.CHAR_CARD_ERR_PATCHER_SLOT);
            }
            int needSub;
            if (slot >= 1 && slot <= 3) {
                needSub = GamePackets.CARD_SUB_CHARACTER;
            } else if (slot >= 5 && slot <= 7) {
                if (slot == 7) {
                    return CharCardResult.fail(GamePackets.CHAR_CARD_ERR_PART_SLOT);
                }
                needSub = GamePackets.CARD_SUB_CADDIE;
            } else if (slot >= 9 && slot <= 12) {
                needSub = GamePackets.CARD_SUB_NPC;
            } else {
                return CharCardResult.fail(GamePackets.CHAR_CARD_ERR_SLOT);
            }
            if (GamePackets.itemSubGroupIdentify22(cardTypeid) != needSub) {
                return CharCardResult.fail(GamePackets.CHAR_CARD_ERR_SUB);
            }
            boolean occupied = h.createQuery("""
                            SELECT 1 FROM pangya.pangya_card_equip
                             WHERE "UID" = :uid AND parts_id = :id AND "Slot" = :slot AND "USE_YN" = 1
                            """)
                    .bind("uid", uid)
                    .bind("id", charId)
                    .bind("slot", slot)
                    .mapTo(Integer.class)
                    .findOne()
                    .isPresent();
            if (occupied) {
                return CharCardResult.fail(GamePackets.CHAR_CARD_ERR_OCCUPIED);
            }
            int ant = owned[1];
            int dep = ant - 1;
            h.createUpdate("""
                            UPDATE pangya.pangya_card
                               SET "QNTD" = :qntd
                             WHERE card_itemid = :id
                            """)
                    .bind("qntd", dep)
                    .bind("id", cardId)
                    .execute();
            int tipo = GamePackets.itemSubGroupIdentify22(cardTypeid);
            h.createUpdate("""
                            INSERT INTO pangya.pangya_card_equip (
                                "UID", parts_id, parts_typeid, card_typeid,
                                "Efeito", "Efeito_Qntd", "Slot", "Tipo", "USE_YN", date
                            ) VALUES (
                                :uid, :partsId, :partsTypeid, :cardTypeid,
                                :efeito, :efeitoQntd, :slot, :tipo, 1, NOW()
                            )
                            """)
                    .bind("uid", uid)
                    .bind("partsId", charId)
                    .bind("partsTypeid", charTypeid)
                    .bind("cardTypeid", cardTypeid)
                    .bind("efeito", iff[0])
                    .bind("efeitoQntd", iff[1])
                    .bind("slot", slot)
                    .bind("tipo", tipo)
                    .execute();
            List<GamePackets.PapelAward> awards = new ArrayList<>();
            awards.add(new GamePackets.PapelAward(
                    GamePackets.PAPEL_AWARD_TYPE, cardTypeid, cardId, 0, ant, dep, -1));
            awards.add(new GamePackets.PapelAward(
                    GamePackets.CHAR_CARD_AWARD_TYPE, charTypeid, charId, 0, 0, 0, 0, cardTypeid, slot));
            return new CharCardResult(0, awards, cardTypeid);
        });
    }

    @Override
    public CharCardResult characterCardEquipWithPatcher(
            long uid, int charTypeid, int charId, int cardTypeid, int cardId, int slot) {
        return jdbi.inTransaction(h -> {
            int[] patcher = h.createQuery("""
                            SELECT item_id, "C0" FROM pangya.pangya_item_warehouse
                             WHERE "UID" = :uid AND typeid = :typeid AND valid = 1
                             ORDER BY item_id
                             LIMIT 1
                            """)
                    .bind("uid", uid)
                    .bind("typeid", GamePackets.TYPEID_CLUB_PATCHER)
                    .map((rs, ctx) -> new int[] {rs.getInt("item_id"), rs.getInt("C0") & 0xffff})
                    .findOne()
                    .orElse(null);
            if (patcher == null) {
                return CharCardResult.fail(GamePackets.CHAR_CARD_PATCHER_ERR);
            }
            if (patcher[1] < 1) {
                return CharCardResult.fail(GamePackets.CHAR_CARD_PATCHER_ERR_QNTD);
            }
            int[] iff = h.createQuery("""
                            SELECT efeito, efeito_qntd FROM pangya.iff_card WHERE typeid = :typeid
                            """)
                    .bind("typeid", cardTypeid)
                    .map((rs, ctx) -> new int[] {rs.getInt("efeito"), rs.getInt("efeito_qntd")})
                    .findOne()
                    .orElse(null);
            if (iff == null) {
                return CharCardResult.fail(GamePackets.CHAR_CARD_PATCHER_ERR_IFF);
            }
            GamePackets.CharacterInfo pCi = loadCharacter(h, uid, charId, charTypeid);
            if (pCi == null) {
                return CharCardResult.fail(GamePackets.CHAR_CARD_PATCHER_ERR_CHAR);
            }
            int[] owned = h.createQuery("""
                            SELECT card_itemid, COALESCE("QNTD", 0) FROM pangya.pangya_card
                             WHERE "UID" = :uid AND card_itemid = :id AND card_typeid = :typeid
                            """)
                    .bind("uid", uid)
                    .bind("id", cardId)
                    .bind("typeid", cardTypeid)
                    .map((rs, ctx) -> new int[] {rs.getInt(1), rs.getInt(2)})
                    .findOne()
                    .orElse(null);
            if (owned == null || owned[1] < 1) {
                return CharCardResult.fail(GamePackets.CHAR_CARD_PATCHER_ERR_OWN);
            }
            if (slot != 4 && slot != 8) {
                return CharCardResult.fail(GamePackets.CHAR_CARD_PATCHER_ERR_SLOT);
            }
            int needSub = slot == 4 ? GamePackets.CARD_SUB_CHARACTER : GamePackets.CARD_SUB_CADDIE;
            if (GamePackets.itemSubGroupIdentify22(cardTypeid) != needSub) {
                return CharCardResult.fail(GamePackets.CHAR_CARD_PATCHER_ERR_SUB);
            }
            boolean occupied = h.createQuery("""
                            SELECT 1 FROM pangya.pangya_card_equip
                             WHERE "UID" = :uid AND parts_id = :id AND "Slot" = :slot AND "USE_YN" = 1
                            """)
                    .bind("uid", uid)
                    .bind("id", charId)
                    .bind("slot", slot)
                    .mapTo(Integer.class)
                    .findOne()
                    .isPresent();
            if (occupied) {
                return CharCardResult.fail(GamePackets.CHAR_CARD_PATCHER_ERR_OCCUPIED);
            }
            int patchAnt = patcher[1];
            int patchDep = patchAnt - 1;
            h.createUpdate("""
                            UPDATE pangya.pangya_item_warehouse
                               SET "C0" = :c0
                             WHERE item_id = :id
                            """)
                    .bind("c0", patchDep)
                    .bind("id", patcher[0])
                    .execute();
            int ant = owned[1];
            int dep = ant - 1;
            h.createUpdate("""
                            UPDATE pangya.pangya_card
                               SET "QNTD" = :qntd
                             WHERE card_itemid = :id
                            """)
                    .bind("qntd", dep)
                    .bind("id", cardId)
                    .execute();
            int tipo = GamePackets.itemSubGroupIdentify22(cardTypeid);
            h.createUpdate("""
                            INSERT INTO pangya.pangya_card_equip (
                                "UID", parts_id, parts_typeid, card_typeid,
                                "Efeito", "Efeito_Qntd", "Slot", "Tipo", "USE_YN", date
                            ) VALUES (
                                :uid, :partsId, :partsTypeid, :cardTypeid,
                                :efeito, :efeitoQntd, :slot, :tipo, 1, NOW()
                            )
                            """)
                    .bind("uid", uid)
                    .bind("partsId", charId)
                    .bind("partsTypeid", charTypeid)
                    .bind("cardTypeid", cardTypeid)
                    .bind("efeito", iff[0])
                    .bind("efeitoQntd", iff[1])
                    .bind("slot", slot)
                    .bind("tipo", tipo)
                    .execute();
            List<GamePackets.PapelAward> awards = new ArrayList<>();
            awards.add(new GamePackets.PapelAward(
                    GamePackets.PAPEL_AWARD_TYPE, GamePackets.TYPEID_CLUB_PATCHER, patcher[0], 0,
                    patchAnt, patchDep, -1));
            awards.add(new GamePackets.PapelAward(
                    GamePackets.PAPEL_AWARD_TYPE, cardTypeid, cardId, 0, ant, dep, -1));
            awards.add(new GamePackets.PapelAward(
                    GamePackets.CHAR_CARD_AWARD_TYPE, charTypeid, charId, 0, 0, 0, 0, cardTypeid, slot));
            return new CharCardResult(0, awards, cardTypeid);
        });
    }

    @Override
    public CharCardResult characterRemoveCard(
            long uid, int charTypeid, int charId, int removerTypeid, int removerId, int slot) {
        return jdbi.inTransaction(h -> {
            GamePackets.CharacterInfo pCi = loadCharacter(h, uid, charId, charTypeid);
            if (pCi == null) {
                return CharCardResult.fail(GamePackets.CHAR_CARD_REMOVE_ERR_CHAR);
            }
            int[] remover = h.createQuery("""
                            SELECT item_id, typeid, "C0" FROM pangya.pangya_item_warehouse
                             WHERE "UID" = :uid AND item_id = :id AND typeid = :typeid AND valid = 1
                            """)
                    .bind("uid", uid)
                    .bind("id", removerId)
                    .bind("typeid", removerTypeid)
                    .map((rs, ctx) -> new int[] {rs.getInt("item_id"), rs.getInt("typeid"), rs.getInt("C0") & 0xffff})
                    .findOne()
                    .orElse(null);
            if (remover == null) {
                return CharCardResult.fail(GamePackets.CHAR_CARD_REMOVE_ERR_ITEM);
            }
            if (remover[2] < 1) {
                return CharCardResult.fail(GamePackets.CHAR_CARD_REMOVE_ERR_QNTD);
            }
            if (slot < 1 || slot > 12) {
                return CharCardResult.fail(GamePackets.CHAR_CARD_REMOVE_ERR_UNKNOWN);
            }
            Integer equipped = h.createQuery("""
                            SELECT card_typeid FROM pangya.pangya_card_equip
                             WHERE "UID" = :uid AND parts_id = :id AND "Slot" = :slot AND "USE_YN" = 1
                            """)
                    .bind("uid", uid)
                    .bind("id", charId)
                    .bind("slot", slot)
                    .mapTo(Integer.class)
                    .findOne()
                    .orElse(null);
            if (equipped == null || equipped == 0) {
                return CharCardResult.fail(GamePackets.CHAR_CARD_REMOVE_ERR_SLOT);
            }
            int remAnt = remover[2];
            int remDep = remAnt - 1;
            h.createUpdate("""
                            UPDATE pangya.pangya_item_warehouse
                               SET "C0" = :c0
                             WHERE item_id = :id
                            """)
                    .bind("c0", remDep)
                    .bind("id", removerId)
                    .execute();
            Integer cardRow = h.createQuery("""
                            SELECT card_itemid FROM pangya.pangya_card
                             WHERE "UID" = :uid AND card_typeid = :typeid
                             ORDER BY card_itemid
                             LIMIT 1
                            """)
                    .bind("uid", uid)
                    .bind("typeid", equipped)
                    .mapTo(Integer.class)
                    .findOne()
                    .orElse(null);
            int cardAnt;
            int cardId;
            if (cardRow != null) {
                cardAnt = h.createQuery("SELECT COALESCE(\"QNTD\", 0) FROM pangya.pangya_card WHERE card_itemid = :id")
                        .bind("id", cardRow)
                        .mapTo(Integer.class)
                        .one();
                cardId = cardRow;
                h.createUpdate("UPDATE pangya.pangya_card SET \"QNTD\" = :qntd WHERE card_itemid = :id")
                        .bind("qntd", cardAnt + 1)
                        .bind("id", cardId)
                        .execute();
            } else {
                cardAnt = 0;
                cardId = h.createQuery("""
                                INSERT INTO pangya.pangya_card (
                                    "UID", card_typeid, "QNTD", "GET_DT",
                                    "Slot", "Efeito", "Efeito_Qntd", card_type, "USE_YN"
                                ) VALUES (
                                    :uid, :typeid, 1, NOW(),
                                    0, 0, 0, 0, 'N'
                                )
                                RETURNING card_itemid
                                """)
                        .bind("uid", uid)
                        .bind("typeid", equipped)
                        .mapTo(Integer.class)
                        .one();
            }
            h.createUpdate("""
                            DELETE FROM pangya.pangya_card_equip
                             WHERE "UID" = :uid AND parts_id = :id AND "Slot" = :slot
                            """)
                    .bind("uid", uid)
                    .bind("id", charId)
                    .bind("slot", slot)
                    .execute();
            List<GamePackets.PapelAward> awards = new ArrayList<>();
            awards.add(new GamePackets.PapelAward(
                    GamePackets.PAPEL_AWARD_TYPE, removerTypeid, removerId, 0, remAnt, remDep, -1));
            awards.add(new GamePackets.PapelAward(
                    GamePackets.PAPEL_AWARD_TYPE, equipped, cardId, 0, cardAnt, cardAnt + 1, 1));
            awards.add(new GamePackets.PapelAward(
                    GamePackets.CHAR_CARD_AWARD_TYPE, charTypeid, charId, 0, 0, 0, 0, 0, slot));
            return new CharCardResult(0, awards, equipped);
        });
    }

    @Override
    public Optional<CometRefill> cometRefill(int typeid) {
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT typeid, "min", "max" FROM pangya.pangya_comet_refill
                         WHERE typeid = :typeid
                         LIMIT 1
                        """)
                .bind("typeid", typeid)
                .map((rs, ctx) -> new CometRefill(
                        rs.getInt("typeid"),
                        rs.getInt("min") & 0xffff,
                        rs.getInt("max") & 0xffff))
                .findOne());
    }

    @Override
    public void upsertCometRefill(int typeid, int min, int max) {
        jdbi.useHandle(h -> {
            h.createUpdate("DELETE FROM pangya.pangya_comet_refill WHERE typeid = :typeid")
                    .bind("typeid", typeid)
                    .execute();
            h.createUpdate("""
                            INSERT INTO pangya.pangya_comet_refill (typeid, min, max)
                            VALUES (:typeid, :min, :max)
                            """)
                    .bind("typeid", typeid)
                    .bind("min", min)
                    .bind("max", max)
                    .execute();
        });
    }

    @Override
    public void deleteCometRefill(int typeid) {
        jdbi.useHandle(h -> h.createUpdate(
                        "DELETE FROM pangya.pangya_comet_refill WHERE typeid = :typeid")
                .bind("typeid", typeid)
                .execute());
    }

    @Override
    public java.util.Map<Integer, CometRefill> cometRefillIndex() {
        return jdbi.withHandle(h -> {
            java.util.Map<Integer, CometRefill> out = new java.util.HashMap<>();
            h.createQuery("""
                            SELECT typeid, "min", "max" FROM pangya.pangya_comet_refill
                            """)
                    .map((rs, ctx) -> new CometRefill(
                            rs.getInt("typeid"),
                            rs.getInt("min") & 0xffff,
                            rs.getInt("max") & 0xffff))
                    .list()
                    .forEach(row -> out.put(row.typeid(), row));
            return out;
        });
    }

    @Override
    public Optional<AttendanceReward> attendanceReward(long uid) {
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT counter, item_typeid_now, item_qntd_now,
                               item_typeid_after, item_qntd_after, last_login
                          FROM pangya.pangya_attendance_reward
                         WHERE "UID" = :uid
                        """)
                .bind("uid", uid)
                .map((rs, ctx) -> {
                    Timestamp ts = rs.getTimestamp("last_login");
                    Instant last = ts == null ? null : ts.toInstant();
                    return new AttendanceReward(
                            rs.getInt("counter"),
                            rs.getInt("item_typeid_now"),
                            rs.getInt("item_qntd_now"),
                            rs.getInt("item_typeid_after"),
                            rs.getInt("item_qntd_after"),
                            last);
                })
                .findOne());
    }

    @Override
    public void upsertAttendanceReward(long uid, AttendanceReward ari) {
        jdbi.useHandle(h -> h.createUpdate("""
                        INSERT INTO pangya.pangya_attendance_reward
                            ("UID", counter, item_typeid_now, item_qntd_now,
                             item_typeid_after, item_qntd_after, last_login)
                        VALUES (:uid, :counter, :nowTypeid, :nowQntd,
                                :afterTypeid, :afterQntd, :lastLogin)
                        ON CONFLICT ("UID") DO UPDATE SET
                            counter = EXCLUDED.counter,
                            item_typeid_now = EXCLUDED.item_typeid_now,
                            item_qntd_now = EXCLUDED.item_qntd_now,
                            item_typeid_after = EXCLUDED.item_typeid_after,
                            item_qntd_after = EXCLUDED.item_qntd_after,
                            last_login = EXCLUDED.last_login
                        """)
                .bind("uid", uid)
                .bind("counter", ari.counter())
                .bind("nowTypeid", ari.nowTypeid())
                .bind("nowQntd", ari.nowQntd())
                .bind("afterTypeid", ari.afterTypeid())
                .bind("afterQntd", ari.afterQntd())
                .bind("lastLogin", ari.lastLogin() == null ? null : Timestamp.from(ari.lastLogin()))
                .execute());
    }

    @Override
    public void deleteAttendanceReward(long uid) {
        jdbi.useHandle(h -> h.createUpdate(
                        "DELETE FROM pangya.pangya_attendance_reward WHERE \"UID\" = :uid")
                .bind("uid", uid)
                .execute());
    }

    @Override
    public List<AttendanceCatalogItem> attendanceCatalog(int tipo) {
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT typeid, quantidade, tipo
                          FROM pangya.pangya_attendance_table_item_reward
                         WHERE tipo = :tipo
                         ORDER BY idx
                        """)
                .bind("tipo", tipo)
                .map((rs, ctx) -> new AttendanceCatalogItem(
                        rs.getInt("typeid"),
                        rs.getInt("quantidade"),
                        rs.getInt("tipo")))
                .list());
    }

    @Override
    public List<AttendanceCatalogItem> attendanceCatalogAll() {
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT typeid, quantidade, tipo
                          FROM pangya.pangya_attendance_table_item_reward
                         ORDER BY idx
                        """)
                .map((rs, ctx) -> new AttendanceCatalogItem(
                        rs.getInt("typeid"),
                        rs.getInt("quantidade"),
                        rs.getInt("tipo")))
                .list());
    }

    @Override
    public void upsertAttendanceCatalog(int typeid, int qntd, int tipo) {
        jdbi.useHandle(h -> {
            h.createUpdate(
                            "DELETE FROM pangya.pangya_attendance_table_item_reward WHERE typeid = :typeid")
                    .bind("typeid", typeid)
                    .execute();
            h.createUpdate("""
                            INSERT INTO pangya.pangya_attendance_table_item_reward
                                (nome, typeid, quantidade, tipo)
                            VALUES ('test', :typeid, :qntd, :tipo)
                            """)
                    .bind("typeid", typeid)
                    .bind("qntd", qntd)
                    .bind("tipo", tipo)
                    .execute();
        });
    }

    @Override
    public void deleteAttendanceCatalog(int typeid) {
        jdbi.useHandle(h -> h.createUpdate(
                        "DELETE FROM pangya.pangya_attendance_table_item_reward WHERE typeid = :typeid")
                .bind("typeid", typeid)
                .execute());
    }

    @Override
    public List<AttendanceCatalogItem> attendanceCatalogIndex() {
        return attendanceCatalogAll();
    }

    @Override
    public Optional<TimeLimitItem> timeLimitItem(int typeid) {
        if (org.pangya.protocol.iff.PangyaIffLoader.source().isPresent()) {
            return org.pangya.protocol.iff.PangyaIffLoader.timeLimitItem(typeid)
                    .map(row -> new TimeLimitItem(row.typeid(), row.tipo(), row.percent(), row.timeMinutes()));
        }
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT typeid, tipo, percent, time
                          FROM pangya.iff_time_limit_item
                         WHERE typeid = :typeid
                        """)
                .bind("typeid", typeid)
                .map((rs, ctx) -> new TimeLimitItem(
                        rs.getInt("typeid"),
                        rs.getInt("tipo"),
                        rs.getInt("percent"),
                        rs.getInt("time")))
                .findOne());
    }

    @Override
    public void upsertTimeLimitItem(int typeid, int tipo, int percent, int timeMinutes) {
        jdbi.useHandle(h -> h.createUpdate("""
                        INSERT INTO pangya.iff_time_limit_item (typeid, tipo, percent, time)
                        VALUES (:typeid, :tipo, :percent, :time)
                        ON CONFLICT (typeid) DO UPDATE SET
                            tipo = EXCLUDED.tipo,
                            percent = EXCLUDED.percent,
                            time = EXCLUDED.time
                        """)
                .bind("typeid", typeid)
                .bind("tipo", tipo)
                .bind("percent", percent)
                .bind("time", timeMinutes)
                .execute());
    }

    @Override
    public void deleteTimeLimitItem(int typeid) {
        jdbi.useHandle(h -> h.createUpdate(
                        "DELETE FROM pangya.iff_time_limit_item WHERE typeid = :typeid")
                .bind("typeid", typeid)
                .execute());
    }

    @Override
    public OptionalInt clubSetWorkShopTipo(int typeid) {
        return clubSetIff(typeid).map(ClubSetIff::tipo).map(OptionalInt::of).orElseGet(OptionalInt::empty);
    }

    @Override
    public void upsertClubSetWorkShopTipo(int typeid, int tipo) {
        jdbi.useHandle(h -> h.createUpdate("""
                        INSERT INTO pangya.iff_clubset (typeid, work_shop_tipo)
                        VALUES (:typeid, :tipo)
                        ON CONFLICT (typeid) DO UPDATE SET work_shop_tipo = EXCLUDED.work_shop_tipo
                        """)
                .bind("typeid", typeid)
                .bind("tipo", tipo)
                .execute());
    }

    @Override
    public void deleteClubSetIff(int typeid) {
        jdbi.useHandle(h -> h.createUpdate(
                        "DELETE FROM pangya.iff_clubset WHERE typeid = :typeid")
                .bind("typeid", typeid)
                .execute());
    }

    @Override
    public Optional<ClubSetIff> clubSetIff(int typeid) {
        Optional<ClubSetIff> iff = clubSetIffFromLoader(typeid);
        if (iff.isPresent()) {
            return iff;
        }
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT work_shop_tipo, stats0, stats1, stats2, stats3, stats4,
                               slot0, slot1, slot2, slot3, slot4, tipo_rank_s, total_recovery,
                               flag_transformar
                          FROM pangya.iff_clubset
                         WHERE typeid = :typeid
                        """)
                .bind("typeid", typeid)
                .map((rs, ctx) -> new ClubSetIff(
                        rs.getInt("work_shop_tipo"),
                        new short[] {
                            rs.getShort("stats0"),
                            rs.getShort("stats1"),
                            rs.getShort("stats2"),
                            rs.getShort("stats3"),
                            rs.getShort("stats4")
                        },
                        new short[] {
                            rs.getShort("slot0"),
                            rs.getShort("slot1"),
                            rs.getShort("slot2"),
                            rs.getShort("slot3"),
                            rs.getShort("slot4")
                        },
                        rs.getInt("tipo_rank_s"),
                        rs.getInt("total_recovery"),
                        rs.getInt("flag_transformar")))
                .findOne());
    }

    private static Optional<ClubSetIff> clubSetIffFromLoader(int typeid) {
        return org.pangya.protocol.iff.PangyaIffLoader.clubSet(typeid)
                .map(row -> new ClubSetIff(
                        row.workShopTipo(),
                        row.stats(),
                        row.slots(),
                        row.tipoRankS(),
                        row.totalRecovery(),
                        row.flagTransformar()));
    }

    @Override
    public void upsertClubSetIff(int typeid, int tipo, short[] stats, short[] slots) {
        upsertClubSetIff(typeid, tipo, stats, slots, 0, 0);
    }

    @Override
    public void upsertClubSetIff(int typeid, int tipo, short[] stats, short[] slots, int tipoRankS) {
        upsertClubSetIff(typeid, tipo, stats, slots, tipoRankS, 0);
    }

    @Override
    public void upsertClubSetIff(
            int typeid, int tipo, short[] stats, short[] slots, int tipoRankS, int totalRecovery) {
        upsertClubSetIff(typeid, tipo, stats, slots, tipoRankS, totalRecovery, 0);
    }

    @Override
    public void upsertClubSetIff(
            int typeid,
            int tipo,
            short[] stats,
            short[] slots,
            int tipoRankS,
            int totalRecovery,
            int flagTransformar) {
        short[] st = pad5(stats);
        short[] sl = pad5(slots);
        jdbi.useHandle(h -> h.createUpdate("""
                        INSERT INTO pangya.iff_clubset (
                            typeid, work_shop_tipo,
                            stats0, stats1, stats2, stats3, stats4,
                            slot0, slot1, slot2, slot3, slot4, tipo_rank_s, total_recovery,
                            flag_transformar)
                        VALUES (
                            :typeid, :tipo,
                            :s0, :s1, :s2, :s3, :s4,
                            :l0, :l1, :l2, :l3, :l4, :rank, :recovery, :flag)
                        ON CONFLICT (typeid) DO UPDATE SET
                            work_shop_tipo = EXCLUDED.work_shop_tipo,
                            stats0 = EXCLUDED.stats0, stats1 = EXCLUDED.stats1,
                            stats2 = EXCLUDED.stats2, stats3 = EXCLUDED.stats3,
                            stats4 = EXCLUDED.stats4,
                            slot0 = EXCLUDED.slot0, slot1 = EXCLUDED.slot1,
                            slot2 = EXCLUDED.slot2, slot3 = EXCLUDED.slot3,
                            slot4 = EXCLUDED.slot4,
                            tipo_rank_s = EXCLUDED.tipo_rank_s,
                            total_recovery = EXCLUDED.total_recovery,
                            flag_transformar = EXCLUDED.flag_transformar
                        """)
                .bind("typeid", typeid)
                .bind("tipo", tipo)
                .bind("s0", st[0]).bind("s1", st[1]).bind("s2", st[2])
                .bind("s3", st[3]).bind("s4", st[4])
                .bind("l0", sl[0]).bind("l1", sl[1]).bind("l2", sl[2])
                .bind("l3", sl[3]).bind("l4", sl[4])
                .bind("rank", tipoRankS)
                .bind("recovery", totalRecovery)
                .bind("flag", flagTransformar)
                .execute());
    }

    @Override
    public boolean clubSetOriginalAny(int specialTypeid) {
        if (org.pangya.protocol.iff.PangyaIffLoader.source().isPresent()) {
            return !org.pangya.protocol.iff.PangyaIffLoader.clubSetOriginals(specialTypeid).isEmpty();
        }
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT 1 FROM pangya.iff_clubset_original
                         WHERE special_typeid = :special
                         LIMIT 1
                        """)
                .bind("special", specialTypeid)
                .mapTo(Integer.class)
                .findOne())
                .isPresent();
    }

    @Override
    public List<ClubSetOriginal> clubSetOriginals(int specialTypeid) {
        if (org.pangya.protocol.iff.PangyaIffLoader.source().isPresent()) {
            return org.pangya.protocol.iff.PangyaIffLoader.clubSetOriginals(specialTypeid).stream()
                    .map(row -> new ClubSetOriginal(row.typeid(), row.slots()))
                    .toList();
        }
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT original_typeid, slot0, slot1, slot2, slot3, slot4
                          FROM pangya.iff_clubset_original
                         WHERE special_typeid = :special
                         ORDER BY original_typeid
                        """)
                .bind("special", specialTypeid)
                .map((rs, ctx) -> new ClubSetOriginal(
                        rs.getInt("original_typeid"),
                        new short[] {
                            rs.getShort("slot0"),
                            rs.getShort("slot1"),
                            rs.getShort("slot2"),
                            rs.getShort("slot3"),
                            rs.getShort("slot4")
                        }))
                .list());
    }

    @Override
    public void upsertClubSetOriginal(int specialTypeid, int originalTypeid, short[] slots) {
        short[] sl = pad5(slots);
        jdbi.useHandle(h -> h.createUpdate("""
                        INSERT INTO pangya.iff_clubset_original (
                            special_typeid, original_typeid, slot0, slot1, slot2, slot3, slot4)
                        VALUES (:special, :original, :l0, :l1, :l2, :l3, :l4)
                        ON CONFLICT (special_typeid, original_typeid) DO UPDATE SET
                            slot0 = EXCLUDED.slot0, slot1 = EXCLUDED.slot1,
                            slot2 = EXCLUDED.slot2, slot3 = EXCLUDED.slot3,
                            slot4 = EXCLUDED.slot4
                        """)
                .bind("special", specialTypeid)
                .bind("original", originalTypeid)
                .bind("l0", sl[0]).bind("l1", sl[1]).bind("l2", sl[2])
                .bind("l3", sl[3]).bind("l4", sl[4])
                .execute());
    }

    @Override
    public void deleteClubSetOriginal(int specialTypeid) {
        jdbi.useHandle(h -> h.createUpdate(
                        "DELETE FROM pangya.iff_clubset_original WHERE special_typeid = :special")
                .bind("special", specialTypeid)
                .execute());
    }

    @Override
    public Optional<CutinIff> cutinIff(int typeid) {
        if (org.pangya.protocol.iff.PangyaIffLoader.source().isPresent()) {
            return org.pangya.protocol.iff.PangyaIffLoader.cutin(typeid)
                    .map(row -> new CutinIff(
                            row.typeid(),
                            row.sector(),
                            row.condition(),
                            row.imageTypes(),
                            row.tempo(),
                            row.sprites()));
        }
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT typeid, sector, condition,
                               img0_tipo, img1_tipo, img2_tipo, img3_tipo, tempo,
                               sprite0, sprite1, sprite2, sprite3
                          FROM pangya.iff_cutin_information
                         WHERE typeid = :typeid
                        """)
                .bind("typeid", typeid)
                .map((rs, ctx) -> new CutinIff(
                        rs.getInt("typeid"),
                        rs.getInt("sector"),
                        rs.getInt("condition"),
                        new int[] {
                            rs.getInt("img0_tipo"),
                            rs.getInt("img1_tipo"),
                            rs.getInt("img2_tipo"),
                            rs.getInt("img3_tipo")
                        },
                        rs.getInt("tempo"),
                        new String[] {
                            rs.getString("sprite0"),
                            rs.getString("sprite1"),
                            rs.getString("sprite2"),
                            rs.getString("sprite3")
                        }))
                .findOne());
    }

    @Override
    public Optional<SetItemIff> setItemIff(int typeid) {
        if (!org.pangya.protocol.iff.PangyaIffLoader.source().isPresent()) {
            return Optional.empty();
        }
        return org.pangya.protocol.iff.PangyaIffLoader.setItem(typeid)
                .map(row -> new SetItemIff(
                        row.typeid(),
                        row.packege().total(),
                        row.packege().itemTypeids(),
                        row.packege().itemQntds(),
                        row.point(),
                        row.typeSet()));
    }

    @Override
    public void upsertCutinIff(
            int typeid, int sector, int condition, int[] imageTypes, int tempo, String[] sprites) {
        int[] img = imageTypes == null ? new int[4] : imageTypes;
        String[] spr = sprites == null ? new String[4] : sprites;
        jdbi.useHandle(h -> h.createUpdate("""
                        INSERT INTO pangya.iff_cutin_information (
                            typeid, sector, condition,
                            img0_tipo, img1_tipo, img2_tipo, img3_tipo, tempo,
                            sprite0, sprite1, sprite2, sprite3)
                        VALUES (
                            :typeid, :sector, :condition,
                            :i0, :i1, :i2, :i3, :tempo,
                            :s0, :s1, :s2, :s3)
                        ON CONFLICT (typeid) DO UPDATE SET
                            sector = EXCLUDED.sector,
                            condition = EXCLUDED.condition,
                            img0_tipo = EXCLUDED.img0_tipo,
                            img1_tipo = EXCLUDED.img1_tipo,
                            img2_tipo = EXCLUDED.img2_tipo,
                            img3_tipo = EXCLUDED.img3_tipo,
                            tempo = EXCLUDED.tempo,
                            sprite0 = EXCLUDED.sprite0,
                            sprite1 = EXCLUDED.sprite1,
                            sprite2 = EXCLUDED.sprite2,
                            sprite3 = EXCLUDED.sprite3
                        """)
                .bind("typeid", typeid)
                .bind("sector", sector)
                .bind("condition", condition)
                .bind("i0", img.length > 0 ? img[0] : 0)
                .bind("i1", img.length > 1 ? img[1] : 0)
                .bind("i2", img.length > 2 ? img[2] : 0)
                .bind("i3", img.length > 3 ? img[3] : 0)
                .bind("tempo", tempo)
                .bind("s0", spr.length > 0 && spr[0] != null ? spr[0] : "")
                .bind("s1", spr.length > 1 && spr[1] != null ? spr[1] : "")
                .bind("s2", spr.length > 2 && spr[2] != null ? spr[2] : "")
                .bind("s3", spr.length > 3 && spr[3] != null ? spr[3] : "")
                .execute());
    }

    @Override
    public void deleteCutinIff(int typeid) {
        jdbi.useHandle(h -> h.createUpdate(
                        "DELETE FROM pangya.iff_cutin_information WHERE typeid = :typeid")
                .bind("typeid", typeid)
                .execute());
    }

    @Override
    public Optional<BoxMailReward> boxMailReward(int boxTypeid) {
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT box_typeid, reward_typeid, reward_qntd, opened_typeid, message
                          FROM pangya.box_mail_catalog
                         WHERE box_typeid = :typeid
                        """)
                .bind("typeid", boxTypeid)
                .map((rs, ctx) -> new BoxMailReward(
                        rs.getInt("box_typeid"),
                        rs.getInt("reward_typeid"),
                        rs.getInt("reward_qntd"),
                        rs.getInt("opened_typeid"),
                        rs.getString("message")))
                .findOne());
    }

    @Override
    public void upsertBoxMailReward(
            int boxTypeid, int rewardTypeid, int rewardQntd, int openedTypeid, String message) {
        jdbi.useHandle(h -> h.createUpdate("""
                        INSERT INTO pangya.box_mail_catalog (
                            box_typeid, reward_typeid, reward_qntd, opened_typeid, message)
                        VALUES (:box, :reward, :qntd, :opened, :message)
                        ON CONFLICT (box_typeid) DO UPDATE SET
                            reward_typeid = EXCLUDED.reward_typeid,
                            reward_qntd = EXCLUDED.reward_qntd,
                            opened_typeid = EXCLUDED.opened_typeid,
                            message = EXCLUDED.message
                        """)
                .bind("box", boxTypeid)
                .bind("reward", rewardTypeid)
                .bind("qntd", rewardQntd)
                .bind("opened", openedTypeid)
                .bind("message", message == null ? "" : message)
                .execute());
    }

    @Override
    public void deleteBoxMailReward(int boxTypeid) {
        jdbi.useHandle(h -> h.createUpdate(
                        "DELETE FROM pangya.box_mail_catalog WHERE box_typeid = :typeid")
                .bind("typeid", boxTypeid)
                .execute());
    }

    @Override
    public java.util.Map<Integer, BoxMailReward> boxMailIndex() {
        return jdbi.withHandle(h -> {
            java.util.Map<Integer, BoxMailReward> out = new java.util.HashMap<>();
            h.createQuery("""
                            SELECT box_typeid, reward_typeid, reward_qntd, opened_typeid, message
                              FROM pangya.box_mail_catalog
                            """)
                    .map((rs, ctx) -> new BoxMailReward(
                            rs.getInt("box_typeid"),
                            rs.getInt("reward_typeid"),
                            rs.getInt("reward_qntd"),
                            rs.getInt("opened_typeid"),
                            rs.getString("message")))
                    .list()
                    .forEach(row -> out.put(row.boxTypeid(), row));
            return out;
        });
    }

    @Override
    public boolean itemIff(int typeid) {
        var iff = org.pangya.protocol.iff.PangyaIffLoader.itemIndex();
        if (!iff.isEmpty()) {
            return iff.contains(typeid);
        }
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT 1 FROM pangya.iff_item WHERE typeid = :typeid
                        """)
                .bind("typeid", typeid)
                .mapTo(Integer.class)
                .findOne())
                .isPresent();
    }

    @Override
    public void upsertItemIff(int typeid) {
        jdbi.useHandle(h -> h.createUpdate("""
                        INSERT INTO pangya.iff_item (typeid) VALUES (:typeid)
                        ON CONFLICT (typeid) DO NOTHING
                        """)
                .bind("typeid", typeid)
                .execute());
    }

    @Override
    public void deleteItemIff(int typeid) {
        jdbi.useHandle(h -> h.createUpdate("DELETE FROM pangya.iff_item WHERE typeid = :typeid")
                .bind("typeid", typeid)
                .execute());
    }

    @Override
    public boolean cardIff(int typeid) {
        var iff = org.pangya.protocol.iff.PangyaIffLoader.cardIndex();
        if (!iff.isEmpty()) {
            return iff.contains(typeid);
        }
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT 1 FROM pangya.iff_card WHERE typeid = :typeid
                        """)
                .bind("typeid", typeid)
                .mapTo(Integer.class)
                .findOne())
                .isPresent();
    }

    @Override
    public Optional<CardSpecialIff> cardSpecialIff(int typeid) {
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT typeid, efeito, efeito_qntd, efeito_tempo
                          FROM pangya.iff_card
                         WHERE typeid = :typeid
                        """)
                .bind("typeid", typeid)
                .map((rs, ctx) -> new CardSpecialIff(
                        rs.getInt("typeid"),
                        rs.getInt("efeito"),
                        rs.getInt("efeito_qntd"),
                        rs.getInt("efeito_tempo")))
                .findOne());
    }

    @Override
    public void upsertCardSpecialIff(
            int typeid, int rarity, int probability, int effect, int effectValue, int effectTime) {
        jdbi.useHandle(h -> h.createUpdate("""
                        INSERT INTO pangya.iff_card (
                            typeid, rarity, probabilidade, efeito, efeito_qntd, efeito_tempo)
                        VALUES (:typeid, :rarity, :prob, :effect, :value, :time)
                        ON CONFLICT (typeid) DO UPDATE SET
                            rarity = EXCLUDED.rarity,
                            probabilidade = EXCLUDED.probabilidade,
                            efeito = EXCLUDED.efeito,
                            efeito_qntd = EXCLUDED.efeito_qntd,
                            efeito_tempo = EXCLUDED.efeito_tempo
                        """)
                .bind("typeid", typeid)
                .bind("rarity", rarity)
                .bind("prob", probability)
                .bind("effect", effect)
                .bind("value", effectValue)
                .bind("time", effectTime)
                .execute());
    }

    @Override
    public void deleteCardIff(int typeid) {
        jdbi.useHandle(h -> h.createUpdate("DELETE FROM pangya.iff_card WHERE typeid = :typeid")
                .bind("typeid", typeid)
                .execute());
    }

    @Override
    public List<CardPackReward> cardPackRewards(int packTypeid) {
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT seq, card_typeid
                          FROM pangya.card_pack_catalog
                         WHERE pack_typeid = :typeid
                         ORDER BY seq
                        """)
                .bind("typeid", packTypeid)
                .map((rs, ctx) -> new CardPackReward(
                        rs.getInt("seq"), rs.getInt("card_typeid")))
                .list());
    }

    @Override
    public void upsertCardPackReward(int packTypeid, int seq, int cardTypeid) {
        jdbi.useHandle(h -> h.createUpdate("""
                        INSERT INTO pangya.card_pack_catalog (pack_typeid, seq, card_typeid)
                        VALUES (:pack, :seq, :card)
                        ON CONFLICT (pack_typeid, seq) DO UPDATE SET
                            card_typeid = EXCLUDED.card_typeid
                        """)
                .bind("pack", packTypeid)
                .bind("seq", seq)
                .bind("card", cardTypeid)
                .execute());
    }

    @Override
    public void deleteCardPackRewards(int packTypeid) {
        jdbi.useHandle(h -> h.createUpdate(
                        "DELETE FROM pangya.card_pack_catalog WHERE pack_typeid = :typeid")
                .bind("typeid", packTypeid)
                .execute());
    }

    @Override
    public java.util.Map<Integer, List<CardPackReward>> cardPackIndex() {
        return jdbi.withHandle(h -> {
            java.util.Map<Integer, List<CardPackReward>> out = new java.util.HashMap<>();
            h.createQuery("""
                            SELECT pack_typeid, seq, card_typeid
                              FROM pangya.card_pack_catalog
                             ORDER BY pack_typeid, seq
                            """)
                    .map((rs, ctx) -> new Object[] {
                            rs.getInt("pack_typeid"),
                            new CardPackReward(rs.getInt("seq"), rs.getInt("card_typeid"))
                    })
                    .list()
                    .forEach(row -> {
                        int pack = (Integer) row[0];
                        CardPackReward reward = (CardPackReward) row[1];
                        out.computeIfAbsent(pack, k -> new java.util.ArrayList<>()).add(reward);
                    });
            return out;
        });
    }

    @Override
    public List<MemorialReward> memorialRewards(int coinTypeid) {
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT seq, rarity, reward_typeid, qntd
                          FROM pangya.memorial_reward_catalog
                         WHERE coin_typeid = :coin
                         ORDER BY seq
                        """)
                .bind("coin", coinTypeid)
                .map((rs, ctx) -> new MemorialReward(
                        rs.getInt("seq"),
                        rs.getInt("rarity"),
                        rs.getInt("reward_typeid"),
                        rs.getInt("qntd")))
                .list());
    }

    @Override
    public void upsertMemorialReward(
            int coinTypeid, int seq, int rarity, int rewardTypeid, int qntd) {
        jdbi.useHandle(h -> h.createUpdate("""
                        INSERT INTO pangya.memorial_reward_catalog (
                            coin_typeid, seq, rarity, reward_typeid, qntd)
                        VALUES (:coin, :seq, :rarity, :reward, :qntd)
                        ON CONFLICT (coin_typeid, seq) DO UPDATE SET
                            rarity = EXCLUDED.rarity,
                            reward_typeid = EXCLUDED.reward_typeid,
                            qntd = EXCLUDED.qntd
                        """)
                .bind("coin", coinTypeid)
                .bind("seq", seq)
                .bind("rarity", rarity)
                .bind("reward", rewardTypeid)
                .bind("qntd", qntd)
                .execute());
    }

    @Override
    public void deleteMemorialRewards(int coinTypeid) {
        jdbi.useHandle(h -> h.createUpdate(
                        "DELETE FROM pangya.memorial_reward_catalog WHERE coin_typeid = :coin")
                .bind("coin", coinTypeid)
                .execute());
    }

    @Override
    public java.util.Map<Integer, List<MemorialReward>> memorialIndex() {
        return jdbi.withHandle(h -> {
            java.util.Map<Integer, List<MemorialReward>> out = new java.util.HashMap<>();
            h.createQuery("""
                            SELECT coin_typeid, seq, rarity, reward_typeid, qntd
                              FROM pangya.memorial_reward_catalog
                             ORDER BY coin_typeid, seq
                            """)
                    .map((rs, ctx) -> new Object[] {
                            rs.getInt("coin_typeid"),
                            new MemorialReward(
                                    rs.getInt("seq"),
                                    rs.getInt("rarity"),
                                    rs.getInt("reward_typeid"),
                                    rs.getInt("qntd"))
                    })
                    .list()
                    .forEach(row -> {
                        int coin = (Integer) row[0];
                        MemorialReward reward = (MemorialReward) row[1];
                        out.computeIfAbsent(coin, k -> new java.util.ArrayList<>()).add(reward);
                    });
            return out;
        });
    }

    @Override
    public java.util.Map<Short, Boolean> coinCubeCourseActive() {
        return jdbi.withHandle(h -> {
            java.util.Map<Short, Boolean> out = new java.util.HashMap<>();
            h.createQuery("""
                            SELECT course_id, active FROM pangya.pangya_coin_cube_info
                            """)
                    .map((rs, ctx) -> new Object[] {
                            rs.getShort("course_id"),
                            rs.getShort("active") == 1
                    })
                    .list()
                    .forEach(row -> out.put((Short) row[0], (Boolean) row[1]));
            return out;
        });
    }

    @Override
    public List<CoinCubeLocation> coinCubeLocations() {
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT "index", course, hole, tipo, tipo_location, rate, x, y, z
                          FROM pangya.pangya_coin_cube_location
                         ORDER BY course, hole, "index"
                        """)
                .map((rs, ctx) -> new CoinCubeLocation(
                        rs.getLong("index"),
                        rs.getShort("course"),
                        rs.getShort("hole"),
                        rs.getShort("tipo"),
                        rs.getShort("tipo_location"),
                        rs.getLong("rate"),
                        rs.getDouble("x"),
                        rs.getDouble("y"),
                        rs.getDouble("z")))
                .list());
    }

    @Override
    public java.util.Map<Integer, List<CourseDropItem>> courseDropIndex() {
        return jdbi.withHandle(h -> {
            java.util.Map<Integer, List<CourseDropItem>> out = new java.util.HashMap<>();
            h.createQuery("""
                            SELECT course, tipo, typeid, quantidade,
                                   "probabilidade_3H", "probabilidade_6H",
                                   "probabilidade_9H", "probabilidade_18H"
                              FROM pangya.pangya_new_course_drop_item
                             WHERE active = 1
                             ORDER BY course, "index"
                            """)
                    .map((rs, ctx) -> new CourseDropItem(
                            rs.getInt("course"),
                            rs.getInt("tipo"),
                            rs.getInt("typeid"),
                            rs.getInt("quantidade"),
                            rs.getInt("probabilidade_3H"),
                            rs.getInt("probabilidade_6H"),
                            rs.getInt("probabilidade_9H"),
                            rs.getInt("probabilidade_18H")))
                    .list()
                    .forEach(row -> out.computeIfAbsent(row.course(), k -> new java.util.ArrayList<>()).add(row));
            java.util.Map<Integer, List<CourseDropItem>> frozen = new java.util.HashMap<>();
            out.forEach((k, v) -> frozen.put(k, List.copyOf(v)));
            return frozen;
        });
    }

    @Override
    public Optional<CourseDropConfig> courseDropConfig() {
        return jdbi.withHandle(h -> h.createQuery("""
                            SELECT rate_mana_artefact, rate_grand_prix_ticket, "rate_SSC_ticket"
                              FROM pangya.pangya_new_course_drop
                             ORDER BY "index"
                             LIMIT 1
                            """)
                .map((rs, ctx) -> new CourseDropConfig(
                        rs.getInt("rate_mana_artefact"),
                        rs.getInt("rate_grand_prix_ticket"),
                        rs.getInt("rate_SSC_ticket")))
                .findOne());
    }

    @Override
    public java.util.Map<Integer, Integer> courseParIndex() {
        return jdbi.withHandle(h -> {
            java.util.Map<Integer, Integer> out = new java.util.HashMap<>();
            h.createQuery("""
                            SELECT course_id, hole, par FROM pangya.iff_course_hole
                            """)
                    .map((rs, ctx) -> new Object[] {
                            ((rs.getShort("course_id") & 0x7f) << 8) | (rs.getShort("hole") & 0xff),
                            (int) rs.getShort("par")
                    })
                    .list()
                    .forEach(row -> out.put((Integer) row[0], (Integer) row[1]));
            return out;
        });
    }

    @Override
    public java.util.Map<Short, InventoryRepository.CourseMap> courseMapIndex() {
        return jdbi.withHandle(h -> {
            java.util.Map<Short, InventoryRepository.CourseMap> out = new java.util.HashMap<>();
            h.createQuery("""
                            SELECT course_id, name, star_tenths, clear_bonus
                              FROM pangya.iff_course
                            """)
                    .map((rs, ctx) -> new InventoryRepository.CourseMap(
                            rs.getShort("course_id"),
                            rs.getString("name"),
                            rs.getShort("star_tenths"),
                            rs.getInt("clear_bonus")))
                    .list()
                    .forEach(row -> out.put(row.courseId(), row));
            return out;
        });
    }

    @Override
    public void upsertCourseMap(int courseId, String name, int starTenths, int clearBonus) {
        jdbi.useHandle(h -> h.createUpdate("""
                        INSERT INTO pangya.iff_course (course_id, name, star_tenths, clear_bonus)
                        VALUES (:course, :name, :star, :bonus)
                        ON CONFLICT (course_id) DO UPDATE SET
                            name = EXCLUDED.name,
                            star_tenths = EXCLUDED.star_tenths,
                            clear_bonus = EXCLUDED.clear_bonus
                        """)
                .bind("course", (short) (courseId & 0x7f))
                .bind("name", name)
                .bind("star", (short) starTenths)
                .bind("bonus", clearBonus)
                .execute());
    }

    @Override
    public void deleteCourseMap(int courseId) {
        jdbi.useHandle(h -> h.createUpdate(
                        "DELETE FROM pangya.iff_course WHERE course_id = :course")
                .bind("course", (short) (courseId & 0x7f))
                .execute());
    }

    @Override
    public Optional<Instant> ticketReportDate(int ticketId) {
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT report_date FROM pangya.ticket_report_catalog WHERE ticket_id = :id
                        """)
                .bind("id", ticketId)
                .map((rs, ctx) -> rs.getTimestamp("report_date").toInstant())
                .findOne());
    }

    @Override
    public void upsertTicketReport(int ticketId, Instant date) {
        jdbi.useHandle(h -> h.createUpdate("""
                        INSERT INTO pangya.ticket_report_catalog (ticket_id, report_date)
                        VALUES (:id, :date)
                        ON CONFLICT (ticket_id) DO UPDATE SET report_date = EXCLUDED.report_date
                        """)
                .bind("id", ticketId)
                .bind("date", Timestamp.from(date))
                .execute());
    }

    @Override
    public void deleteTicketReport(int ticketId) {
        jdbi.useHandle(h -> h.createUpdate(
                        "DELETE FROM pangya.ticket_report_catalog WHERE ticket_id = :id")
                .bind("id", ticketId)
                .execute());
    }

    @Override
    public void upsertCoursePar(int courseId, int hole, int par) {
        jdbi.useHandle(h -> h.createUpdate("""
                        INSERT INTO pangya.iff_course_hole (course_id, hole, par)
                        VALUES (:course, :hole, :par)
                        ON CONFLICT (course_id, hole) DO UPDATE SET par = EXCLUDED.par
                        """)
                .bind("course", (short) (courseId & 0x7f))
                .bind("hole", (short) hole)
                .bind("par", (short) par)
                .execute());
    }

    @Override
    public void deleteCoursePar(int courseId, int hole) {
        jdbi.useHandle(h -> h.createUpdate(
                        "DELETE FROM pangya.iff_course_hole WHERE course_id = :course AND hole = :hole")
                .bind("course", (short) (courseId & 0x7f))
                .bind("hole", (short) hole)
                .execute());
    }

    @Override
    public Optional<GrandPrixEvent> grandPrixEvent(int typeid) {
        if (org.pangya.protocol.iff.PangyaIffLoader.source().isPresent()) {
            return org.pangya.protocol.iff.PangyaIffLoader.grandPrixData(typeid)
                    .map(row -> {
                        int natural = 0;
                        if (row.naturalMode()) {
                            natural |= 0x1;
                        }
                        if (row.shotMode()) {
                            natural |= 0x2;
                        }
                        return new GrandPrixEvent(
                                row.typeid(),
                                row.name(),
                                row.holes(),
                                row.course(),
                                row.modo(),
                                natural,
                                row.rule(),
                                row.minLevel(),
                                row.maxLevel());
                    });
        }
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT typeid, name, holes, course, modo, natural_mode, rule, min_level, max_level
                          FROM pangya.grand_prix_event
                         WHERE typeid = :typeid AND active = 1
                        """)
                .bind("typeid", typeid)
                .map((rs, ctx) -> new GrandPrixEvent(
                        rs.getInt("typeid"),
                        rs.getString("name"),
                        rs.getInt("holes"),
                        rs.getInt("course"),
                        rs.getInt("modo"),
                        rs.getInt("natural_mode"),
                        rs.getInt("rule"),
                        rs.getInt("min_level"),
                        rs.getInt("max_level")))
                .findOne());
    }

    @Override
    public void upsertGrandPrixEvent(
            int typeid,
            String name,
            int holes,
            int course,
            int modo,
            int natural,
            int rule,
            int minLevel,
            int maxLevel) {
        jdbi.useHandle(h -> h.createUpdate("""
                        INSERT INTO pangya.grand_prix_event (
                            typeid, active, name, holes, course, modo, natural_mode, rule,
                            min_level, max_level)
                        VALUES (:typeid, 1, :name, :holes, :course, :modo, :natural, :rule,
                                :min, :max)
                        ON CONFLICT (typeid) DO UPDATE SET
                            active = 1, name = EXCLUDED.name, holes = EXCLUDED.holes,
                            course = EXCLUDED.course, modo = EXCLUDED.modo,
                            natural_mode = EXCLUDED.natural_mode, rule = EXCLUDED.rule,
                            min_level = EXCLUDED.min_level, max_level = EXCLUDED.max_level
                        """)
                .bind("typeid", typeid)
                .bind("name", name == null ? "" : name)
                .bind("holes", holes)
                .bind("course", course)
                .bind("modo", modo)
                .bind("natural", natural)
                .bind("rule", rule)
                .bind("min", minLevel)
                .bind("max", maxLevel)
                .execute());
    }

    @Override
    public void deleteGrandPrixEvent(int typeid) {
        jdbi.useHandle(h -> h.createUpdate(
                        "DELETE FROM pangya.grand_prix_event WHERE typeid = :typeid")
                .bind("typeid", typeid)
                .execute());
    }

    @Override
    public float mediaScore(long uid) {
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT COALESCE("Holes", 0), COALESCE("Holein", 0), COALESCE("Media_score", 0)
                          FROM pangya.user_info
                         WHERE "UID" = :uid
                        """)
                .bind("uid", uid)
                .map((rs, ctx) -> mediaScoreFromStats(
                        rs.getLong(1), rs.getInt(2), rs.getInt(3)))
                .findOne()
                .orElse(0f));
    }

    static float mediaScoreFromStats(long holes, int holeIn, int rawMediaScore) {
        if (holes - holeIn == 0) {
            return 0f;
        }
        return (18.0f / (holes - holeIn)) * rawMediaScore + 72.0f;
    }

    @Override
    public boolean hasGrandPrixClear(long uid, int typeid) {
        if (typeid == 0) {
            return false;
        }
        return jdbi.withHandle(h -> Boolean.TRUE.equals(h.createQuery("""
                        SELECT 1
                          FROM pangya.pangya_grandprix_clear
                         WHERE uid = :uid AND typeid = :typeid
                         LIMIT 1
                        """)
                .bind("uid", (int) uid)
                .bind("typeid", typeid)
                .mapTo(Integer.class)
                .findOne()
                .isPresent()));
    }

    @Override
    public java.util.OptionalInt grandPrixClearPosition(long uid, int typeid) {
        if (typeid == 0) {
            return java.util.OptionalInt.empty();
        }
        Integer flag = jdbi.withHandle(h -> h.createQuery("""
                        SELECT flag
                          FROM pangya.pangya_grandprix_clear
                         WHERE uid = :uid AND typeid = :typeid
                         LIMIT 1
                        """)
                .bind("uid", (int) uid)
                .bind("typeid", typeid)
                .mapTo(Integer.class)
                .findOne()
                .orElse(null));
        return flag == null ? java.util.OptionalInt.empty() : java.util.OptionalInt.of(flag);
    }

    @Override
    public boolean updateGrandPrixClearIfBetter(long uid, int typeid, int position) {
        if (typeid == 0 || position <= 0) {
            return false;
        }
        java.util.OptionalInt existing = grandPrixClearPosition(uid, typeid);
        if (existing.isEmpty()) {
            upsertGrandPrixClear(uid, typeid, position);
            return true;
        }
        if (existing.getAsInt() > position) {
            upsertGrandPrixClear(uid, typeid, position);
            return true;
        }
        return false;
    }

    @Override
    public void upsertGrandPrixClear(long uid, int typeid, int flag) {
        jdbi.useHandle(h -> {
            Integer existing = h.createQuery("""
                            SELECT "index"
                              FROM pangya.pangya_grandprix_clear
                             WHERE uid = :uid AND typeid = :typeid
                             LIMIT 1
                            """)
                    .bind("uid", (int) uid)
                    .bind("typeid", typeid)
                    .mapTo(Integer.class)
                    .findOne()
                    .orElse(null);
            if (existing == null) {
                h.createUpdate("""
                                INSERT INTO pangya.pangya_grandprix_clear (uid, typeid, flag)
                                VALUES (:uid, :typeid, :flag)
                                """)
                        .bind("uid", (int) uid)
                        .bind("typeid", typeid)
                        .bind("flag", flag)
                        .execute();
            } else {
                h.createUpdate("""
                                UPDATE pangya.pangya_grandprix_clear
                                   SET flag = :flag
                                 WHERE uid = :uid AND typeid = :typeid
                                """)
                        .bind("uid", (int) uid)
                        .bind("typeid", typeid)
                        .bind("flag", flag)
                        .execute();
            }
        });
    }

    @Override
    public void deleteGrandPrixClear(long uid, int typeid) {
        jdbi.useHandle(h -> h.createUpdate("""
                        DELETE FROM pangya.pangya_grandprix_clear
                         WHERE uid = :uid AND typeid = :typeid
                        """)
                .bind("uid", (int) uid)
                .bind("typeid", typeid)
                .execute());
    }

    @Override
    public Optional<MapStatisticsRow> mapStatistics(long uid, int tipo, int course, int assist) {
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT tipo, course, assist, tacada, putt, hole, fairway, holein, puttin,
                               total_score, best_score, best_pang, character_typeid, event_score
                          FROM pangya.pangya_record
                         WHERE "UID" = :uid AND tipo = :tipo AND course = :course AND assist = :assist
                         LIMIT 1
                        """)
                .bind("uid", (int) uid)
                .bind("tipo", tipo)
                .bind("course", course)
                .bind("assist", assist)
                .map((rs, ctx) -> new MapStatisticsRow(
                        rs.getInt("tipo"),
                        rs.getInt("course"),
                        rs.getInt("assist"),
                        rs.getInt("tacada"),
                        rs.getInt("putt"),
                        rs.getInt("hole"),
                        rs.getInt("fairway"),
                        rs.getInt("holein"),
                        rs.getInt("puttin"),
                        rs.getInt("total_score"),
                        rs.getInt("best_score"),
                        rs.getLong("best_pang"),
                        rs.getInt("character_typeid"),
                        rs.getInt("event_score")))
                .findOne());
    }

    @Override
    public void upsertMapStatistics(long uid, MapStatisticsRow row) {
        if (uid <= 0) {
            return;
        }
        jdbi.useHandle(h -> {
            Integer existing = h.createQuery("""
                            SELECT tacada
                              FROM pangya.pangya_record
                             WHERE "UID" = :uid AND tipo = :tipo AND course = :course AND assist = :assist
                             LIMIT 1
                            """)
                    .bind("uid", (int) uid)
                    .bind("tipo", row.tipo())
                    .bind("course", row.course())
                    .bind("assist", row.assist())
                    .mapTo(Integer.class)
                    .findOne()
                    .orElse(null);
            if (existing == null) {
                h.createUpdate("""
                                INSERT INTO pangya.pangya_record (
                                    "UID", tipo, course, tacada, putt, hole, fairway, holein, puttin,
                                    total_score, best_score, best_pang, character_typeid, event_score, assist
                                ) VALUES (
                                    :uid, :tipo, :course, :tacada, :putt, :hole, :fairway, :holein, :puttin,
                                    :totalScore, :bestScore, :bestPang, :characterTypeid, :eventScore, :assist
                                )
                                """)
                        .bind("uid", (int) uid)
                        .bind("tipo", row.tipo())
                        .bind("course", row.course())
                        .bind("assist", row.assist())
                        .bind("tacada", row.tacada())
                        .bind("putt", row.putt())
                        .bind("hole", row.hole())
                        .bind("fairway", row.fairway())
                        .bind("holein", row.holeIn())
                        .bind("puttin", row.puttIn())
                        .bind("totalScore", row.totalScore())
                        .bind("bestScore", row.bestScore())
                        .bind("bestPang", row.bestPang())
                        .bind("characterTypeid", row.characterTypeid())
                        .bind("eventScore", row.eventScore())
                        .execute();
            } else {
                h.createUpdate("""
                                UPDATE pangya.pangya_record
                                   SET tacada = :tacada,
                                       putt = :putt,
                                       hole = :hole,
                                       fairway = :fairway,
                                       holein = :holein,
                                       puttin = :puttin,
                                       total_score = :totalScore,
                                       best_score = :bestScore,
                                       best_pang = :bestPang,
                                       character_typeid = :characterTypeid,
                                       event_score = :eventScore
                                 WHERE "UID" = :uid AND tipo = :tipo AND course = :course AND assist = :assist
                                """)
                        .bind("uid", (int) uid)
                        .bind("tipo", row.tipo())
                        .bind("course", row.course())
                        .bind("assist", row.assist())
                        .bind("tacada", row.tacada())
                        .bind("putt", row.putt())
                        .bind("hole", row.hole())
                        .bind("fairway", row.fairway())
                        .bind("holein", row.holeIn())
                        .bind("puttin", row.puttIn())
                        .bind("totalScore", row.totalScore())
                        .bind("bestScore", row.bestScore())
                        .bind("bestPang", row.bestPang())
                        .bind("characterTypeid", row.characterTypeid())
                        .bind("eventScore", row.eventScore())
                        .execute();
            }
        });
    }

    @Override
    public Optional<GrandPrixTrofelInsert> addGrandPrixTrofel(long uid, int typeid) {
        if (typeid == 0) {
            return Optional.empty();
        }
        return jdbi.inTransaction(h -> {
            Integer existingId = h.createQuery("""
                            SELECT item_id
                              FROM pangya.pangya_trofel_grandprix
                             WHERE "UID" = :uid AND typeid = :typeid
                             LIMIT 1
                            """)
                    .bind("uid", (int) uid)
                    .bind("typeid", typeid)
                    .mapTo(Integer.class)
                    .findOne()
                    .orElse(null);
            if (existingId != null) {
                int qntdAnt = h.createQuery("""
                                SELECT qntd
                                  FROM pangya.pangya_trofel_grandprix
                                 WHERE item_id = :id
                                """)
                        .bind("id", existingId)
                        .mapTo(Integer.class)
                        .one();
                int qntdDep = qntdAnt + 1;
                h.createUpdate("""
                                UPDATE pangya.pangya_trofel_grandprix
                                   SET qntd = :qntd
                                 WHERE item_id = :id
                                """)
                        .bind("qntd", qntdDep)
                        .bind("id", existingId)
                        .execute();
                return Optional.of(new GrandPrixTrofelInsert(existingId, qntdAnt, qntdDep));
            }
            long itemId = h.createUpdate("""
                            INSERT INTO pangya.pangya_trofel_grandprix ("UID", typeid, qntd)
                            VALUES (:uid, :typeid, 1)
                            """)
                    .bind("uid", (int) uid)
                    .bind("typeid", typeid)
                    .executeAndReturnGeneratedKeys("item_id")
                    .mapTo(Long.class)
                    .one();
            return Optional.of(new GrandPrixTrofelInsert(itemId, 0, 1));
        });
    }

    @Override
    public long legacyTikiPoints(long uid) {
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT COALESCE(MAX("Tiki_Points"), 0)
                          FROM pangya.pangya_tiki_points
                         WHERE "UID" = :uid
                        """)
                .bind("uid", uid)
                .mapTo(Long.class)
                .one());
    }

    @Override
    public void setLegacyTikiPoints(long uid, long points) {
        jdbi.useTransaction(h -> {
            h.createUpdate("DELETE FROM pangya.pangya_tiki_points WHERE \"UID\" = :uid")
                    .bind("uid", uid)
                    .execute();
            h.createUpdate("""
                            INSERT INTO pangya.pangya_tiki_points (
                                "UID", "Tiki_Points", "REG_DATE", "MOD_DATE")
                            VALUES (:uid, :points, NOW(), NOW())
                            """)
                    .bind("uid", uid)
                    .bind("points", points)
                    .execute();
        });
    }

    @Override
    public Optional<TikiItemValue> tikiItemValue(int typeid) {
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT typeid, item_count, points
                          FROM pangya.legacy_tiki_item_value
                         WHERE typeid = :typeid
                        """)
                .bind("typeid", typeid)
                .map((rs, ctx) -> new TikiItemValue(
                        rs.getInt("typeid"), rs.getInt("item_count"), rs.getInt("points")))
                .findOne());
    }

    @Override
    public void upsertTikiItemValue(int typeid, int itemCount, int points) {
        jdbi.useHandle(h -> h.createUpdate("""
                        INSERT INTO pangya.legacy_tiki_item_value (typeid, item_count, points)
                        VALUES (:typeid, :count, :points)
                        ON CONFLICT (typeid) DO UPDATE SET
                            item_count = EXCLUDED.item_count, points = EXCLUDED.points
                        """)
                .bind("typeid", typeid)
                .bind("count", itemCount)
                .bind("points", points)
                .execute());
    }

    @Override
    public void deleteTikiItemValue(int typeid) {
        jdbi.useHandle(h -> h.createUpdate(
                        "DELETE FROM pangya.legacy_tiki_item_value WHERE typeid = :typeid")
                .bind("typeid", typeid)
                .execute());
    }

    @Override
    public Optional<TikiPointShopItem> tikiPointShopItem(int typeid) {
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT "ITEM_TYPEID", "ITEM_QNTD", "REQ_POINTS"
                          FROM pangya.pangya_tiki_points_items
                         WHERE "ITEM_TYPEID" = :typeid AND COALESCE("ITEM_ACTIVE", 0) = 1
                         ORDER BY "INDEX"
                         LIMIT 1
                        """)
                .bind("typeid", typeid)
                .map((rs, ctx) -> new TikiPointShopItem(
                        rs.getInt("ITEM_TYPEID"),
                        rs.getInt("ITEM_QNTD"),
                        rs.getInt("REQ_POINTS")))
                .findOne());
    }

    @Override
    public void upsertTikiPointShopItem(int typeid, int quantity, int points) {
        deleteTikiPointShopItem(typeid);
        jdbi.useHandle(h -> h.createUpdate("""
                        INSERT INTO pangya.pangya_tiki_points_items (
                            "ITEM_NAME", "ITEM_TYPEID", "ITEM_QNTD", "REQ_POINTS",
                            "ITEM_FLAG", "ITEM_ACTIVE", "REG_DATE")
                        VALUES ('Java test', :typeid, :qntd, :points, 0, 1, NOW())
                        """)
                .bind("typeid", typeid)
                .bind("qntd", quantity)
                .bind("points", points)
                .execute());
    }

    @Override
    public void deleteTikiPointShopItem(int typeid) {
        jdbi.useHandle(h -> h.createUpdate(
                        "DELETE FROM pangya.pangya_tiki_points_items WHERE \"ITEM_TYPEID\" = :typeid")
                .bind("typeid", typeid)
                .execute());
    }

    @Override
    public Optional<TikiNewValue> tikiNewValue(int typeid) {
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT typeid, tiki_pang, mileage, bonus_min, bonus_max, bonus_prob
                          FROM pangya.legacy_tiki_item_value
                         WHERE typeid = :typeid
                        """)
                .bind("typeid", typeid)
                .map((rs, ctx) -> new TikiNewValue(
                        rs.getInt("typeid"),
                        rs.getLong("tiki_pang"),
                        rs.getInt("mileage"),
                        rs.getInt("bonus_min"),
                        rs.getInt("bonus_max"),
                        rs.getInt("bonus_prob")))
                .findOne());
    }

    @Override
    public void upsertTikiNewValue(
            int typeid, long pang, int mileage, int bonusMin, int bonusMax, int bonusProb) {
        jdbi.useHandle(h -> h.createUpdate("""
                        INSERT INTO pangya.legacy_tiki_item_value (
                            typeid, item_count, points, tiki_pang, mileage,
                            bonus_min, bonus_max, bonus_prob)
                        VALUES (:typeid, 1, 1, :pang, :mileage, :min, :max, :prob)
                        ON CONFLICT (typeid) DO UPDATE SET
                            tiki_pang = EXCLUDED.tiki_pang,
                            mileage = EXCLUDED.mileage,
                            bonus_min = EXCLUDED.bonus_min,
                            bonus_max = EXCLUDED.bonus_max,
                            bonus_prob = EXCLUDED.bonus_prob
                        """)
                .bind("typeid", typeid)
                .bind("pang", pang)
                .bind("mileage", mileage)
                .bind("min", bonusMin)
                .bind("max", bonusMax)
                .bind("prob", bonusProb)
                .execute());
    }

    @Override
    public DailyQuestMutation acceptDailyQuests(long uid, int[] achievementIds) {
        if (achievementIds == null || achievementIds.length == 0) {
            return new DailyQuestMutation(List.of(), List.of());
        }
        List<GamePackets.CounterItem> created = jdbi.inTransaction(h -> {
            List<GamePackets.CounterItem> out = new ArrayList<>();
            for (int achievementId : achievementIds) {
                boolean owned = h.createQuery("""
                                SELECT 1 FROM pangya.pangya_achievement
                                 WHERE "UID" = :uid AND "ID_ACHIEVEMENT" = :id
                                """)
                        .bind("uid", uid)
                        .bind("id", achievementId)
                        .mapTo(Integer.class)
                        .findOne()
                        .isPresent();
                if (!owned) {
                    continue;
                }
                List<int[]> quests = h.createQuery("""
                                SELECT id, typeid, counter_item_id
                                  FROM pangya.pangya_quest
                                 WHERE uid = :uid AND achievement_id = :id
                                 ORDER BY id
                                """)
                        .bind("uid", uid)
                        .bind("id", achievementId)
                        .map((rs, ctx) -> new int[] {
                            rs.getInt("id"), rs.getInt("typeid"), rs.getInt("counter_item_id")
                        })
                        .list();
                for (int[] quest : quests) {
                    if (quest[2] > 0) {
                        continue;
                    }
                    int counterTypeid = h.createQuery("""
                                    SELECT counter_typeid FROM pangya.iff_daily_quest_stuff
                                     WHERE quest_typeid = :typeid
                                    """)
                            .bind("typeid", quest[1])
                            .mapTo(Integer.class)
                            .findOne()
                            .orElseThrow(() -> new IllegalStateException(
                                    "missing daily quest stuff " + quest[1]));
                    int counterId = h.createQuery("""
                                    INSERT INTO pangya.pangya_counter_item (
                                        "UID", "TypeID", active, "Count_Num_Item")
                                    VALUES (:uid, :typeid, 1, 0)
                                    RETURNING "Count_ID"
                                    """)
                            .bind("uid", uid)
                            .bind("typeid", counterTypeid)
                            .mapTo(Integer.class)
                            .one();
                    h.createUpdate("""
                                    UPDATE pangya.pangya_quest SET counter_item_id = :counter
                                     WHERE uid = :uid AND id = :id
                                    """)
                            .bind("counter", counterId)
                            .bind("uid", uid)
                            .bind("id", quest[0])
                            .execute();
                    out.add(new GamePackets.CounterItem(counterId, counterTypeid, 1, 0));
                }
                h.createUpdate("""
                                UPDATE pangya.pangya_achievement SET status = 3
                                 WHERE "UID" = :uid AND "ID_ACHIEVEMENT" = :id
                                """)
                        .bind("uid", uid)
                        .bind("id", achievementId)
                        .execute();
            }
            return out;
        });
        List<GamePackets.AchievementInfo> selected = achievements(uid).stream()
                .filter(a -> contains(achievementIds, a.id()))
                .toList();
        return new DailyQuestMutation(selected, created);
    }

    @Override
    public DailyQuestMutation removeDailyQuests(long uid, int[] achievementIds) {
        if (achievementIds == null || achievementIds.length == 0) {
            return new DailyQuestMutation(List.of(), List.of());
        }
        List<GamePackets.AchievementInfo> selected = achievements(uid).stream()
                .filter(a -> contains(achievementIds, a.id()))
                .toList();
        List<GamePackets.CounterItem> allCounters = counters(uid);
        List<Integer> counterIds = selected.stream()
                .flatMap(a -> a.quests().stream())
                .map(GamePackets.QuestStuff::counterId)
                .filter(id -> id > 0)
                .distinct()
                .toList();
        List<GamePackets.CounterItem> removedCounters = allCounters.stream()
                .filter(c -> counterIds.contains(c.id()))
                .toList();
        List<Integer> ids = selected.stream().map(GamePackets.AchievementInfo::id).toList();
        if (!ids.isEmpty()) {
            jdbi.useTransaction(h -> {
                h.createUpdate("""
                                DELETE FROM pangya.pangya_quest
                                 WHERE uid = :uid AND achievement_id IN (<ids>)
                                """)
                        .bind("uid", uid)
                        .bindList("ids", ids)
                        .execute();
                h.createUpdate("""
                                DELETE FROM pangya.pangya_achievement
                                 WHERE "UID" = :uid AND "ID_ACHIEVEMENT" IN (<ids>)
                                """)
                        .bind("uid", uid)
                        .bindList("ids", ids)
                        .execute();
                if (!counterIds.isEmpty()) {
                    h.createUpdate("""
                                    DELETE FROM pangya.pangya_counter_item
                                     WHERE "UID" = :uid AND "Count_ID" IN (<ids>)
                                    """)
                            .bind("uid", uid)
                            .bindList("ids", counterIds)
                            .execute();
                }
            });
        }
        return new DailyQuestMutation(selected, removedCounters);
    }

    private static boolean contains(int[] ids, int value) {
        for (int id : ids) {
            if (id == value) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<DailyQuestReward> dailyQuestRewards(int achievementTypeid) {
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT seq, reward_typeid, qntd, time
                          FROM pangya.iff_daily_quest_reward
                         WHERE achievement_typeid = :typeid
                         ORDER BY seq
                        """)
                .bind("typeid", achievementTypeid)
                .map((rs, ctx) -> new DailyQuestReward(
                        rs.getInt("seq"),
                        rs.getInt("reward_typeid"),
                        rs.getInt("qntd"),
                        rs.getInt("time")))
                .list());
    }

    @Override
    public void upsertDailyQuestStuff(int questTypeid, int counterTypeid) {
        upsertDailyQuestStuff(questTypeid, counterTypeid, 1);
    }

    @Override
    public void upsertDailyQuestStuff(int questTypeid, int counterTypeid, int counterQntd) {
        jdbi.useHandle(h -> h.createUpdate("""
                        INSERT INTO pangya.iff_daily_quest_stuff (
                            quest_typeid, counter_typeid, counter_qntd)
                        VALUES (:quest, :counter, :qntd)
                        ON CONFLICT (quest_typeid) DO UPDATE SET
                            counter_typeid = EXCLUDED.counter_typeid,
                            counter_qntd = EXCLUDED.counter_qntd
                        """)
                .bind("quest", questTypeid)
                .bind("counter", counterTypeid)
                .bind("qntd", counterQntd)
                .execute());
    }

    @Override
    public void deleteDailyQuestStuff(int questTypeid) {
        jdbi.useHandle(h -> h.createUpdate(
                        "DELETE FROM pangya.iff_daily_quest_stuff WHERE quest_typeid = :typeid")
                .bind("typeid", questTypeid)
                .execute());
    }

    @Override
    public void upsertDailyQuestReward(
            int achievementTypeid, int seq, int rewardTypeid, int qntd, int time) {
        jdbi.useHandle(h -> h.createUpdate("""
                        INSERT INTO pangya.iff_daily_quest_reward (
                            achievement_typeid, seq, reward_typeid, qntd, time)
                        VALUES (:achievement, :seq, :reward, :qntd, :time)
                        ON CONFLICT (achievement_typeid, seq) DO UPDATE SET
                            reward_typeid = EXCLUDED.reward_typeid,
                            qntd = EXCLUDED.qntd,
                            time = EXCLUDED.time
                        """)
                .bind("achievement", achievementTypeid)
                .bind("seq", seq)
                .bind("reward", rewardTypeid)
                .bind("qntd", qntd)
                .bind("time", time)
                .execute());
    }

    @Override
    public void deleteDailyQuestRewards(int achievementTypeid) {
        jdbi.useHandle(h -> h.createUpdate(
                        "DELETE FROM pangya.iff_daily_quest_reward WHERE achievement_typeid = :typeid")
                .bind("typeid", achievementTypeid)
                .execute());
    }

    @Override
    public void setDailyQuestAcceptDate(long uid, Instant date) {
        jdbi.useHandle(h -> h.createUpdate("""
                        INSERT INTO pangya.pangya_daily_quest_player (
                            uid, last_quest_accept, today_quest)
                        VALUES (:uid, :date, :date)
                        ON CONFLICT (uid) DO UPDATE SET last_quest_accept = EXCLUDED.last_quest_accept
                        """)
                .bind("uid", uid)
                .bind("date", Timestamp.from(date))
                .execute());
    }

    @Override
    public OptionalInt consumeCardByTypeid(long uid, int typeid, int qntd) {
        if (qntd <= 0) {
            return OptionalInt.empty();
        }
        return jdbi.inTransaction(h -> {
            int[] row = h.createQuery("""
                            SELECT card_itemid, COALESCE("QNTD", 0) AS qntd
                              FROM pangya.pangya_card
                             WHERE "UID" = :uid AND card_typeid = :typeid
                             ORDER BY card_itemid
                             LIMIT 1
                            """)
                    .bind("uid", uid)
                    .bind("typeid", typeid)
                    .map((rs, ctx) -> new int[] {rs.getInt("card_itemid"), rs.getInt("qntd")})
                    .findOne()
                    .orElse(null);
            if (row == null || row[1] < qntd) {
                return OptionalInt.empty();
            }
            int remaining = row[1] - qntd;
            if (remaining <= 0) {
                h.createUpdate("DELETE FROM pangya.pangya_card WHERE card_itemid = :id")
                        .bind("id", row[0])
                        .execute();
                return OptionalInt.of(0);
            }
            h.createUpdate("""
                            UPDATE pangya.pangya_card
                               SET "QNTD" = :qntd
                             WHERE card_itemid = :id
                            """)
                    .bind("qntd", remaining)
                    .bind("id", row[0])
                    .execute();
            return OptionalInt.of(remaining);
        });
    }

    @Override
    public Optional<short[]> clubSetLevelUpLimit(int tipo, int rank) {
        if (org.pangya.protocol.iff.PangyaIffLoader.source().isPresent()) {
            return org.pangya.protocol.iff.PangyaIffLoader.clubSetWorkShopLevelUpLimit(tipo, rank);
        }
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT c0, c1, c2, c3, c4
                          FROM pangya.iff_clubset_level_up_limit
                         WHERE tipo = :tipo AND rank = :rank
                        """)
                .bind("tipo", tipo)
                .bind("rank", rank)
                .map((rs, ctx) -> new short[] {
                    rs.getShort("c0"),
                    rs.getShort("c1"),
                    rs.getShort("c2"),
                    rs.getShort("c3"),
                    rs.getShort("c4")
                })
                .findOne());
    }

    @Override
    public boolean clubSetLevelUpAny(int tipo) {
        if (org.pangya.protocol.iff.PangyaIffLoader.source().isPresent()) {
            return org.pangya.protocol.iff.PangyaIffLoader.clubSetWorkShopLevelUpAny(tipo);
        }
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT 1 FROM pangya.iff_clubset_level_up_limit
                         WHERE tipo = :tipo
                         LIMIT 1
                        """)
                .bind("tipo", tipo)
                .mapTo(Integer.class)
                .findOne())
                .isPresent();
    }

    @Override
    public void upsertClubSetLevelUpLimit(int tipo, int rank, short[] c) {
        short[] v = pad5(c);
        jdbi.useHandle(h -> h.createUpdate("""
                        INSERT INTO pangya.iff_clubset_level_up_limit (
                            tipo, rank, c0, c1, c2, c3, c4)
                        VALUES (:tipo, :rank, :c0, :c1, :c2, :c3, :c4)
                        ON CONFLICT (tipo, rank) DO UPDATE SET
                            c0 = EXCLUDED.c0, c1 = EXCLUDED.c1, c2 = EXCLUDED.c2,
                            c3 = EXCLUDED.c3, c4 = EXCLUDED.c4
                        """)
                .bind("tipo", tipo)
                .bind("rank", rank)
                .bind("c0", v[0]).bind("c1", v[1]).bind("c2", v[2])
                .bind("c3", v[3]).bind("c4", v[4])
                .execute());
    }

    @Override
    public void deleteClubSetLevelUpLimit(int tipo, int rank) {
        jdbi.useHandle(h -> h.createUpdate("""
                        DELETE FROM pangya.iff_clubset_level_up_limit
                         WHERE tipo = :tipo AND rank = :rank
                        """)
                .bind("tipo", tipo)
                .bind("rank", rank)
                .execute());
    }

    @Override
    public Optional<int[]> clubSetLevelUpProb(int tipo) {
        if (org.pangya.protocol.iff.PangyaIffLoader.source().isPresent()) {
            return org.pangya.protocol.iff.PangyaIffLoader.clubSetWorkShopLevelUpProb(tipo);
        }
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT c0, c1, c2, c3, c4
                          FROM pangya.iff_clubset_level_up_prob
                         WHERE tipo = :tipo
                        """)
                .bind("tipo", tipo)
                .map((rs, ctx) -> new int[] {
                    rs.getInt("c0"),
                    rs.getInt("c1"),
                    rs.getInt("c2"),
                    rs.getInt("c3"),
                    rs.getInt("c4")
                })
                .findOne());
    }

    @Override
    public void upsertClubSetLevelUpProb(int tipo, int[] c) {
        int[] v = c == null ? new int[5] : c;
        jdbi.useHandle(h -> h.createUpdate("""
                        INSERT INTO pangya.iff_clubset_level_up_prob (
                            tipo, c0, c1, c2, c3, c4)
                        VALUES (:tipo, :c0, :c1, :c2, :c3, :c4)
                        ON CONFLICT (tipo) DO UPDATE SET
                            c0 = EXCLUDED.c0, c1 = EXCLUDED.c1, c2 = EXCLUDED.c2,
                            c3 = EXCLUDED.c3, c4 = EXCLUDED.c4
                        """)
                .bind("tipo", tipo)
                .bind("c0", v.length > 0 ? v[0] : 0)
                .bind("c1", v.length > 1 ? v[1] : 0)
                .bind("c2", v.length > 2 ? v[2] : 0)
                .bind("c3", v.length > 3 ? v[3] : 0)
                .bind("c4", v.length > 4 ? v[4] : 0)
                .execute());
    }

    @Override
    public void deleteClubSetLevelUpProb(int tipo) {
        jdbi.useHandle(h -> h.createUpdate(
                        "DELETE FROM pangya.iff_clubset_level_up_prob WHERE tipo = :tipo")
                .bind("tipo", tipo)
                .execute());
    }

    @Override
    public boolean clubSetRankExp(int tipo) {
        if (org.pangya.protocol.iff.PangyaIffLoader.source().isPresent()) {
            return org.pangya.protocol.iff.PangyaIffLoader.clubSetWorkShopRankExp(tipo);
        }
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT 1 FROM pangya.iff_clubset_rank_exp WHERE tipo = :tipo
                        """)
                .bind("tipo", tipo)
                .mapTo(Integer.class)
                .findOne())
                .isPresent();
    }

    @Override
    public Optional<int[]> clubSetRankExpRanks(int tipo) {
        if (org.pangya.protocol.iff.PangyaIffLoader.source().isPresent()) {
            return org.pangya.protocol.iff.PangyaIffLoader.clubSetWorkShopRankExpRanks(tipo);
        }
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT rank0, rank1, rank2, rank3, rank4, rank5
                          FROM pangya.iff_clubset_rank_exp
                         WHERE tipo = :tipo
                        """)
                .bind("tipo", tipo)
                .map((rs, ctx) -> new int[] {
                    rs.getInt("rank0"),
                    rs.getInt("rank1"),
                    rs.getInt("rank2"),
                    rs.getInt("rank3"),
                    rs.getInt("rank4"),
                    rs.getInt("rank5")
                })
                .findOne());
    }

    @Override
    public void upsertClubSetRankExp(int tipo) {
        upsertClubSetRankExp(tipo, new int[6]);
    }

    @Override
    public void upsertClubSetRankExp(int tipo, int[] ranks) {
        int[] r = ranks == null ? new int[6] : ranks;
        jdbi.useHandle(h -> h.createUpdate("""
                        INSERT INTO pangya.iff_clubset_rank_exp (
                            tipo, rank0, rank1, rank2, rank3, rank4, rank5)
                        VALUES (:tipo, :r0, :r1, :r2, :r3, :r4, :r5)
                        ON CONFLICT (tipo) DO UPDATE SET
                            rank0 = EXCLUDED.rank0, rank1 = EXCLUDED.rank1,
                            rank2 = EXCLUDED.rank2, rank3 = EXCLUDED.rank3,
                            rank4 = EXCLUDED.rank4, rank5 = EXCLUDED.rank5
                        """)
                .bind("tipo", tipo)
                .bind("r0", r.length > 0 ? r[0] : 0)
                .bind("r1", r.length > 1 ? r[1] : 0)
                .bind("r2", r.length > 2 ? r[2] : 0)
                .bind("r3", r.length > 3 ? r[3] : 0)
                .bind("r4", r.length > 4 ? r[4] : 0)
                .bind("r5", r.length > 5 ? r[5] : 0)
                .execute());
    }

    @Override
    public void deleteClubSetRankExp(int tipo) {
        jdbi.useHandle(h -> h.createUpdate(
                        "DELETE FROM pangya.iff_clubset_rank_exp WHERE tipo = :tipo")
                .bind("tipo", tipo)
                .execute());
    }

    @Override
    public void resetClubSetWorkshopAndC(long uid, int itemId) {
        jdbi.useHandle(h -> h.createUpdate("""
                        UPDATE pangya.pangya_item_warehouse
                           SET "C0" = 0, "C1" = 0, "C2" = 0, "C3" = 0, "C4" = 0,
                               "ClubSet_WorkShop_C0" = 0, "ClubSet_WorkShop_C1" = 0,
                               "ClubSet_WorkShop_C2" = 0, "ClubSet_WorkShop_C3" = 0,
                               "ClubSet_WorkShop_C4" = 0,
                               "Level" = 0, "Up" = 0, "Recovery_Pts" = 0
                         WHERE "UID" = :uid AND item_id = :id
                        """)
                .bind("uid", uid)
                .bind("id", itemId)
                .execute());
    }

    @Override
    public void setClubSetWorkshop(
            long uid, int itemId, short[] workshopC, int level, int rank, int recovery) {
        short[] c = pad5(workshopC);
        jdbi.useHandle(h -> h.createUpdate("""
                        UPDATE pangya.pangya_item_warehouse
                           SET "ClubSet_WorkShop_C0" = :c0, "ClubSet_WorkShop_C1" = :c1,
                               "ClubSet_WorkShop_C2" = :c2, "ClubSet_WorkShop_C3" = :c3,
                               "ClubSet_WorkShop_C4" = :c4,
                               "Level" = :level, "Up" = :rank, "Recovery_Pts" = :recovery
                         WHERE "UID" = :uid AND item_id = :id
                        """)
                .bind("uid", uid)
                .bind("id", itemId)
                .bind("c0", c[0]).bind("c1", c[1]).bind("c2", c[2])
                .bind("c3", c[3]).bind("c4", c[4])
                .bind("level", level)
                .bind("rank", rank)
                .bind("recovery", recovery)
                .execute());
    }

    @Override
    public void setWarehouseClubC(long uid, int itemId, short[] c) {
        short[] v = pad5(c);
        jdbi.useHandle(h -> h.createUpdate("""
                        UPDATE pangya.pangya_item_warehouse
                           SET "C0" = :c0, "C1" = :c1, "C2" = :c2, "C3" = :c3, "C4" = :c4
                         WHERE "UID" = :uid AND item_id = :id
                        """)
                .bind("uid", uid)
                .bind("id", itemId)
                .bind("c0", v[0]).bind("c1", v[1]).bind("c2", v[2])
                .bind("c3", v[3]).bind("c4", v[4])
                .execute());
    }

    @Override
    public OptionalLong enchantPang(int typeid) {
        var iff = org.pangya.protocol.iff.PangyaIffLoader.enchantPang(typeid);
        if (iff.isPresent()) {
            return iff;
        }
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT pang FROM pangya.iff_enchant WHERE typeid = :typeid
                        """)
                .bind("typeid", typeid)
                .mapTo(Long.class)
                .findOne())
                .map(OptionalLong::of)
                .orElseGet(OptionalLong::empty);
    }

    private record CadieMagicBoxRow(
            int level,
            int receiveTypeid,
            int receiveQntd,
            int[] tradeTypeids,
            int[] tradeQntds,
            int boxRandomId) {}

    private CadieMagicBoxRow loadCadieMagicBox(org.jdbi.v3.core.Handle h, int lookup) {
        if (org.pangya.protocol.iff.PangyaIffLoader.source().isPresent()) {
            return org.pangya.protocol.iff.PangyaIffLoader.cadieMagicBox(lookup)
                    .filter(org.pangya.protocol.iff.IffCadieMagicBoxRecord::active)
                    .map(row -> new CadieMagicBoxRow(
                            row.level(),
                            row.receiveTypeid(),
                            row.receiveQntd(),
                            row.tradeTypeids(),
                            row.tradeQntds(),
                            row.boxRandomId()))
                    .orElse(null);
        }
        return h.createQuery("""
                        SELECT level, receive_typeid, receive_qntd, box_random_id,
                               trade0_typeid, trade0_qntd, trade1_typeid, trade1_qntd,
                               trade2_typeid, trade2_qntd, trade3_typeid, trade3_qntd
                          FROM pangya.cadie_magic_box
                         WHERE seq = :seq AND active = 1
                        """)
                .bind("seq", lookup)
                .map((rs, ctx) -> new CadieMagicBoxRow(
                        rs.getInt("level"),
                        rs.getInt("receive_typeid"),
                        rs.getInt("receive_qntd"),
                        new int[] {
                            rs.getInt("trade0_typeid"),
                            rs.getInt("trade1_typeid"),
                            rs.getInt("trade2_typeid"),
                            rs.getInt("trade3_typeid")
                        },
                        new int[] {
                            rs.getInt("trade0_qntd"),
                            rs.getInt("trade1_qntd"),
                            rs.getInt("trade2_qntd"),
                            rs.getInt("trade3_qntd")
                        },
                        rs.getInt("box_random_id")))
                .findOne()
                .orElse(null);
    }

    private static short[] pad5(short[] src) {
        short[] out = new short[5];
        if (src != null) {
            System.arraycopy(src, 0, out, 0, Math.min(5, src.length));
        }
        return out;
    }

    @Override
    public void setClubSetRecoveryPts(long uid, int itemId, int recoveryPts) {
        jdbi.useHandle(h -> h.createUpdate("""
                        UPDATE pangya.pangya_item_warehouse
                           SET "Recovery_Pts" = :pts
                         WHERE "UID" = :uid AND item_id = :id
                        """)
                .bind("uid", uid)
                .bind("id", itemId)
                .bind("pts", recoveryPts)
                .execute());
    }

    @Override
    public void setClubSetMasteryPts(long uid, int itemId, int masteryPts) {
        jdbi.useHandle(h -> h.createUpdate("""
                        UPDATE pangya.pangya_item_warehouse
                           SET "Mastery_Pts" = :pts
                         WHERE "UID" = :uid AND item_id = :id
                        """)
                .bind("uid", uid)
                .bind("id", itemId)
                .bind("pts", masteryPts)
                .execute());
    }

    @Override
    public Optional<ItemBuffRow> itemBuff(long uid, int typeid) {
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT "index", typeid, reg_date, end_date, tipo, "percent", use_yn
                          FROM pangya.pangya_item_buff
                         WHERE uid = :uid AND typeid = :typeid AND use_yn = 1
                         ORDER BY "index"
                         LIMIT 1
                        """)
                .bind("uid", uid)
                .bind("typeid", typeid)
                .map((rs, ctx) -> {
                    Timestamp reg = rs.getTimestamp("reg_date");
                    Timestamp end = rs.getTimestamp("end_date");
                    return new ItemBuffRow(
                            rs.getLong("index"),
                            rs.getInt("typeid"),
                            reg == null ? Instant.EPOCH : reg.toInstant(),
                            end == null ? Instant.EPOCH : end.toInstant(),
                            rs.getInt("tipo"),
                            rs.getInt("percent"),
                            rs.getInt("use_yn"));
                })
                .findOne());
    }

    @Override
    public long insertItemBuff(
            long uid, int typeid, int tipo, int percent, Instant useDate, Instant endDate) {
        return jdbi.withHandle(h -> h.createQuery("""
                        INSERT INTO pangya.pangya_item_buff
                            (uid, typeid, tipo, "percent", reg_date, end_date, use_yn)
                        VALUES (:uid, :typeid, :tipo, :percent, :reg, :end, 1)
                        RETURNING "index"
                        """)
                .bind("uid", uid)
                .bind("typeid", typeid)
                .bind("tipo", tipo)
                .bind("percent", percent)
                .bind("reg", Timestamp.from(useDate))
                .bind("end", Timestamp.from(endDate))
                .mapTo(Long.class)
                .one());
    }

    @Override
    public void updateItemBuff(long uid, long index, int typeid, int tipo, Instant endDate) {
        jdbi.useHandle(h -> h.createUpdate("""
                        UPDATE pangya.pangya_item_buff
                           SET typeid = :typeid, tipo = :tipo, end_date = :end
                         WHERE uid = :uid AND "index" = :index
                        """)
                .bind("uid", uid)
                .bind("index", index)
                .bind("typeid", typeid)
                .bind("tipo", tipo)
                .bind("end", Timestamp.from(endDate))
                .execute());
    }

    @Override
    public void deleteItemBuff(long uid, int typeid) {
        jdbi.useHandle(h -> h.createUpdate("""
                        DELETE FROM pangya.pangya_item_buff
                         WHERE uid = :uid AND typeid = :typeid
                        """)
                .bind("uid", uid)
                .bind("typeid", typeid)
                .execute());
    }

    @Override
    public List<ItemBuffRow> activeItemBuffs(long uid) {
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT "index", typeid, reg_date, end_date, tipo, "percent", use_yn
                          FROM pangya.pangya_item_buff
                         WHERE uid = :uid AND use_yn = 1 AND end_date > NOW()
                         ORDER BY "index"
                        """)
                .bind("uid", uid)
                .map((rs, ctx) -> {
                    Timestamp reg = rs.getTimestamp("reg_date");
                    Timestamp end = rs.getTimestamp("end_date");
                    return new ItemBuffRow(
                            rs.getLong("index"),
                            rs.getInt("typeid"),
                            reg == null ? Instant.EPOCH : reg.toInstant(),
                            end == null ? Instant.EPOCH : end.toInstant(),
                            rs.getInt("tipo"),
                            rs.getInt("percent"),
                            rs.getInt("use_yn"));
                })
                .list());
    }

    @Override
    public List<CardEquipRow> cardEquips(long uid) {
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT parts_id, parts_typeid, card_typeid, "Efeito", "Efeito_Qntd", "Tipo"
                          FROM pangya.pangya_card_equip
                         WHERE "UID" = :uid AND "USE_YN" = 1
                         ORDER BY "index"
                        """)
                .bind("uid", uid)
                .map((rs, ctx) -> new CardEquipRow(
                        rs.getInt("parts_id"),
                        rs.getInt("parts_typeid"),
                        rs.getInt("card_typeid"),
                        rs.getInt("Efeito"),
                        rs.getInt("Efeito_Qntd"),
                        rs.getInt("Tipo")))
                .list());
    }

    private static final String USER_INFO_SELECT = """
            SELECT "Tacadas", "Putt", "Tempo", "Tempo tacadas", "Max_distancia", "Acerto_pangya",
                   "Bunker", "O.B", "Total_distancia", "Holes", "Holein", "HIO", "Timeout", "Fairway",
                   "Albatross", "MaConduta", "Acerto_Putt", "Long-putt", "Chip-in", "Xp", "level", "Pang",
                   "Media_score", "BestScore0", "BestScore1", "BestScore2", "BestScore3", "BestScore4",
                   "MaxPang0", "maxPang1", "maxPang2", "maxPang3", "maxPang4", "SumPang", "EventFlag",
                   "Jogado", "Quitado", "SkinPang", "SkinWin", "SkinLose", "SkinRunHole", "SkinStrikePoint",
                   "SkinAllinCount", "Todos_combos", "Combos", "TeamWin", "TeamGames", "Teamhole",
                   "LadderPoint", "LadderWin", "LadderLose", "LadderDraw", "LadderHole", "EventValue",
                   "NaoSei", "MaxJogoNaoSei", "JogosNaoSei", "GameCountSeason", "Cookie",
                   total_pang_win_game, lucky_medal, fast_medal, best_drive_medal, best_chipin_medal,
                   best_puttin_medal, best_recovery_medal, "16bit_naosei"
              FROM pangya.user_info
             WHERE "UID" = :uid
            """;

    @Override
    public Optional<UserInfoRow> userInfo(long uid) {
        if (uid <= 0) {
            return Optional.empty();
        }
        return jdbi.withHandle(h -> h.createQuery(USER_INFO_SELECT)
                .bind("uid", uid)
                .map((rs, ctx) -> mapUserInfoRow(rs))
                .findOne());
    }

    private static UserInfoRow mapUserInfoRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new UserInfoRow(
                rs.getLong("Tacadas"),
                rs.getLong("Putt"),
                rs.getLong("Tempo"),
                rs.getLong("Tempo tacadas"),
                rs.getFloat("Max_distancia"),
                rs.getLong("Acerto_pangya"),
                rs.getInt("Bunker"),
                rs.getLong("O.B"),
                rs.getLong("Total_distancia"),
                rs.getLong("Holes"),
                rs.getInt("Holein"),
                rs.getLong("HIO"),
                rs.getInt("Timeout"),
                rs.getLong("Fairway"),
                rs.getLong("Albatross"),
                rs.getInt("MaConduta"),
                rs.getLong("Acerto_Putt"),
                rs.getFloat("Long-putt"),
                rs.getFloat("Chip-in"),
                rs.getLong("Xp"),
                rs.getInt("level"),
                rs.getLong("Pang"),
                rs.getInt("Media_score"),
                rs.getInt("BestScore0"),
                rs.getInt("BestScore1"),
                rs.getInt("BestScore2"),
                rs.getInt("BestScore3"),
                rs.getInt("BestScore4"),
                rs.getLong("MaxPang0"),
                rs.getLong("maxPang1"),
                rs.getLong("maxPang2"),
                rs.getLong("maxPang3"),
                rs.getLong("maxPang4"),
                rs.getLong("SumPang"),
                rs.getInt("EventFlag"),
                rs.getLong("Jogado"),
                rs.getLong("Quitado"),
                rs.getLong("SkinPang"),
                rs.getInt("SkinWin"),
                rs.getInt("SkinLose"),
                rs.getInt("SkinRunHole"),
                rs.getInt("SkinStrikePoint"),
                rs.getInt("SkinAllinCount"),
                rs.getLong("Todos_combos"),
                rs.getLong("Combos"),
                rs.getInt("TeamWin"),
                rs.getInt("TeamGames"),
                rs.getLong("Teamhole"),
                rs.getInt("LadderPoint"),
                rs.getInt("LadderWin"),
                rs.getInt("LadderLose"),
                rs.getInt("LadderDraw"),
                rs.getInt("LadderHole"),
                rs.getInt("EventValue"),
                rs.getInt("NaoSei"),
                rs.getInt("MaxJogoNaoSei"),
                rs.getInt("JogosNaoSei"),
                rs.getInt("GameCountSeason"),
                rs.getLong("Cookie"),
                rs.getLong("total_pang_win_game"),
                rs.getInt("lucky_medal"),
                rs.getInt("fast_medal"),
                rs.getInt("best_drive_medal"),
                rs.getInt("best_chipin_medal"),
                rs.getInt("best_puttin_medal"),
                rs.getInt("best_recovery_medal"),
                rs.getInt("16bit_naosei"));
    }

    @Override
    public void updateUserInfo(long uid, UserInfoRow row) {
        if (uid <= 0 || row == null) {
            return;
        }
        jdbi.useHandle(h -> h.createUpdate("""
                        UPDATE pangya.user_info SET
                            "Tacadas" = :tacadas,
                            "Putt" = :putt,
                            "Tempo" = :tempo,
                            "Tempo tacadas" = :tempoTacadas,
                            "Max_distancia" = :maxDistancia,
                            "Acerto_pangya" = :acertoPangya,
                            "Bunker" = :bunker,
                            "O.B" = :ob,
                            "Total_distancia" = :totalDistancia,
                            "Holes" = :holes,
                            "Holein" = :holeIn,
                            "HIO" = :hio,
                            "Timeout" = :timeout,
                            "Fairway" = :fairway,
                            "Albatross" = :albatross,
                            "MaConduta" = :maConduta,
                            "Acerto_Putt" = :acertoPutt,
                            "Long-putt" = :longPutt,
                            "Chip-in" = :chipIn,
                            "Xp" = :xp,
                            "level" = :level,
                            "Pang" = :pang,
                            "Media_score" = :mediaScore,
                            "BestScore0" = :bestScore0,
                            "BestScore1" = :bestScore1,
                            "BestScore2" = :bestScore2,
                            "BestScore3" = :bestScore3,
                            "BestScore4" = :bestScore4,
                            "MaxPang0" = :maxPang0,
                            "maxPang1" = :maxPang1,
                            "maxPang2" = :maxPang2,
                            "maxPang3" = :maxPang3,
                            "maxPang4" = :maxPang4,
                            "SumPang" = :sumPang,
                            "EventFlag" = :eventFlag,
                            "Jogado" = :jogado,
                            "Quitado" = :quitado,
                            "SkinPang" = :skinPang,
                            "SkinWin" = :skinWin,
                            "SkinLose" = :skinLose,
                            "SkinRunHole" = :skinRunHole,
                            "SkinStrikePoint" = :skinStrikePoint,
                            "SkinAllinCount" = :skinAllinCount,
                            "Todos_combos" = :todosCombos,
                            "Combos" = :combos,
                            "TeamWin" = :teamWin,
                            "TeamGames" = :teamGames,
                            "Teamhole" = :teamHole,
                            "LadderPoint" = :ladderPoint,
                            "LadderWin" = :ladderWin,
                            "LadderLose" = :ladderLose,
                            "LadderDraw" = :ladderDraw,
                            "LadderHole" = :ladderHole,
                            "EventValue" = :eventValue,
                            "NaoSei" = :naoSei,
                            "MaxJogoNaoSei" = :maxJogoNaoSei,
                            "JogosNaoSei" = :jogosNaoSei,
                            "GameCountSeason" = :gameCountSeason,
                            "Cookie" = :cookie,
                            total_pang_win_game = :totalPangWinGame,
                            lucky_medal = :luckyMedal,
                            fast_medal = :fastMedal,
                            best_drive_medal = :bestDriveMedal,
                            best_chipin_medal = :bestChipinMedal,
                            best_puttin_medal = :bestPuttinMedal,
                            best_recovery_medal = :bestRecoveryMedal,
                            "16bit_naosei" = :bit16NaoSei
                         WHERE "UID" = :uid
                        """)
                .bind("uid", uid)
                .bind("tacadas", row.tacadas())
                .bind("putt", row.putt())
                .bind("tempo", row.tempo())
                .bind("tempoTacadas", row.tempoTacadas())
                .bind("maxDistancia", row.maxDistancia())
                .bind("acertoPangya", row.acertoPangya())
                .bind("bunker", row.bunker())
                .bind("ob", row.ob())
                .bind("totalDistancia", row.totalDistancia())
                .bind("holes", row.holes())
                .bind("holeIn", row.holeIn())
                .bind("hio", row.hio())
                .bind("timeout", row.timeout())
                .bind("fairway", row.fairway())
                .bind("albatross", row.albatross())
                .bind("maConduta", row.maConduta())
                .bind("acertoPutt", row.acertoPutt())
                .bind("longPutt", row.longPutt())
                .bind("chipIn", row.chipIn())
                .bind("xp", row.xp())
                .bind("level", row.level())
                .bind("pang", row.pang())
                .bind("mediaScore", row.mediaScore())
                .bind("bestScore0", row.bestScore0())
                .bind("bestScore1", row.bestScore1())
                .bind("bestScore2", row.bestScore2())
                .bind("bestScore3", row.bestScore3())
                .bind("bestScore4", row.bestScore4())
                .bind("maxPang0", row.maxPang0())
                .bind("maxPang1", row.maxPang1())
                .bind("maxPang2", row.maxPang2())
                .bind("maxPang3", row.maxPang3())
                .bind("maxPang4", row.maxPang4())
                .bind("sumPang", row.sumPang())
                .bind("eventFlag", row.eventFlag())
                .bind("jogado", row.jogado())
                .bind("quitado", row.quitado())
                .bind("skinPang", row.skinPang())
                .bind("skinWin", row.skinWin())
                .bind("skinLose", row.skinLose())
                .bind("skinRunHole", row.skinRunHole())
                .bind("skinStrikePoint", row.skinStrikePoint())
                .bind("skinAllinCount", row.skinAllinCount())
                .bind("todosCombos", row.todosCombos())
                .bind("combos", row.combos())
                .bind("teamWin", row.teamWin())
                .bind("teamGames", row.teamGames())
                .bind("teamHole", row.teamHole())
                .bind("ladderPoint", row.ladderPoint())
                .bind("ladderWin", row.ladderWin())
                .bind("ladderLose", row.ladderLose())
                .bind("ladderDraw", row.ladderDraw())
                .bind("ladderHole", row.ladderHole())
                .bind("eventValue", row.eventValue())
                .bind("naoSei", row.naoSei())
                .bind("maxJogoNaoSei", row.maxJogoNaoSei())
                .bind("jogosNaoSei", row.jogosNaoSei())
                .bind("gameCountSeason", row.gameCountSeason())
                .bind("cookie", row.cookie())
                .bind("totalPangWinGame", row.totalPangWinGame())
                .bind("luckyMedal", row.luckyMedal())
                .bind("fastMedal", row.fastMedal())
                .bind("bestDriveMedal", row.bestDriveMedal())
                .bind("bestChipinMedal", row.bestChipinMedal())
                .bind("bestPuttinMedal", row.bestPuttinMedal())
                .bind("bestRecoveryMedal", row.bestRecoveryMedal())
                .bind("bit16NaoSei", row.bit16NaoSei())
                .execute());
    }

    @Override
    public void addTotalPangWinGame(long uid, long credit) {
        if (uid <= 0 || credit <= 0) {
            return;
        }
        jdbi.useHandle(h -> h.createUpdate("""
                        UPDATE pangya.user_info
                           SET total_pang_win_game = total_pang_win_game + :credit
                         WHERE "UID" = :uid
                        """)
                .bind("uid", uid)
                .bind("credit", credit)
                .execute());
    }
}
