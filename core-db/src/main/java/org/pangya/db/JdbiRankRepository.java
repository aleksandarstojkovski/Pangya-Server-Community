package org.pangya.db;

import org.jdbi.v3.core.Jdbi;

import org.pangya.protocol.game.GamePackets;

import java.util.List;
import java.util.Optional;

public final class JdbiRankRepository implements RankRepository {

    private static final int PAGE_SIZE = 12;

    private final Jdbi jdbi;

    public JdbiRankRepository(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    @Override
    public List<RegistryRow> registry() {
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT a."UID" AS uid,
                               a.position AS current_position,
                               COALESCE((
                                   SELECT b.position
                                     FROM pangya.pangya_rank_antes b
                                    WHERE b.tipo_rank = a.tipo_rank
                                      AND b.tipo_rank_seq = a.tipo_rank_seq
                                      AND b."UID" = a."UID"
                                    ORDER BY b."index"
                                    LIMIT 1
                               ), 0) AS last_position,
                               a.valor,
                               a.tipo_rank,
                               a.tipo_rank_seq
                          FROM pangya.pangya_rank_atual a
                         ORDER BY a.tipo_rank, a.tipo_rank_seq, a.position
                        """)
                .map((rs, ctx) -> new RegistryRow(
                        rs.getLong("uid"),
                        rs.getInt("current_position"),
                        rs.getInt("last_position"),
                        rs.getInt("valor"),
                        rs.getInt("tipo_rank"),
                        rs.getInt("tipo_rank_seq")))
                .list());
    }

    @Override
    public List<RegistryRow> page(int menu, int item, int page) {
        List<RegistryRow> all = registry().stream()
                .filter(r -> r.menu() == menu && r.item() == item)
                .toList();
        int p = Math.max(page, 1);
        int from = (p - 1) * PAGE_SIZE;
        if (from >= all.size()) {
            return List.of();
        }
        return all.subList(from, Math.min(from + PAGE_SIZE, all.size()));
    }

    @Override
    public Optional<PlayerSnapshot> playerSnapshot(long uid) {
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT a."UID", a."ID", a."NICK", COALESCE(b."level", 0) AS level
                          FROM pangya.account a
                          LEFT JOIN pangya.user_info b ON a."UID" = b."UID"
                         WHERE a."UID" = :uid
                        """)
                .bind("uid", uid)
                .map((rs, ctx) -> new PlayerSnapshot(
                        rs.getLong("UID"),
                        rs.getString("ID"),
                        rs.getString("NICK"),
                        rs.getInt("level")))
                .findOne());
    }

    @Override
    public List<RegistryRow> overallForPlayer(long uid) {
        return registry().stream().filter(r -> r.uid() == uid).toList();
    }

    @Override
    public Optional<RowSummary> rowSummary(long uid) {
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT a."ID", a."NICK", COALESCE(b."level", 0) AS level
                          FROM pangya.account a
                          LEFT JOIN pangya.user_info b ON a."UID" = b."UID"
                         WHERE a."UID" = :uid
                        """)
                .bind("uid", uid)
                .map((rs, ctx) -> new RowSummary(
                        rs.getInt("level"),
                        0,
                        0,
                        rs.getString("ID"),
                        rs.getString("NICK")))
                .findOne());
    }

    @Override
    public Optional<RegistryRow> findInMenu(int menu, int item, long uid) {
        return registry().stream()
                .filter(r -> r.menu() == menu && r.item() == item && r.uid() == uid)
                .findFirst();
    }

    @Override
    public Optional<RegistryRow> findByPosition(int menu, int item, int position) {
        return registry().stream()
                .filter(r -> r.menu() == menu && r.item() == item && r.currentPosition() == position)
                .findFirst();
    }

    @Override
    public Optional<RegistryRow> findByNickname(int menu, int item, String nickname) {
        if (nickname == null || nickname.isEmpty()) {
            return Optional.empty();
        }
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT a."UID"
                          FROM pangya.account a
                         WHERE a."NICK" = :nick
                        """)
                .bind("nick", nickname)
                .mapTo(Long.class)
                .findOne()
                .flatMap(uid -> findInMenu(menu, item, uid)));
    }

    @Override
    public Optional<org.pangya.protocol.game.GamePackets.CharacterInfo> character(long uid) {
        Optional<org.pangya.protocol.game.GamePackets.CharacterInfo> snap = jdbi.withHandle(h -> h.createQuery("""
                        SELECT item_id, typeid,
                               parts_1, parts_2, parts_3, parts_4, parts_5, parts_6, parts_7, parts_8,
                               parts_9, parts_10, parts_11, parts_12, parts_13, parts_14, parts_15, parts_16,
                               parts_17, parts_18, parts_19, parts_20, parts_21, parts_22, parts_23, parts_24,
                               itemid_parts_1, itemid_parts_2, itemid_parts_3, itemid_parts_4,
                               itemid_parts_5, itemid_parts_6, itemid_parts_7, itemid_parts_8,
                               itemid_parts_9, itemid_parts_10, itemid_parts_11, itemid_parts_12,
                               itemid_parts_13, itemid_parts_14, itemid_parts_15, itemid_parts_16,
                               itemid_parts_17, itemid_parts_18, itemid_parts_19, itemid_parts_20,
                               itemid_parts_21, itemid_parts_22, itemid_parts_23, itemid_parts_24,
                               default_hair, default_shirts, gift_flag, purchase,
                               "PCL0", "PCL1", "PCL2", "PCL3", "PCL4",
                               "AUXPARTS_1", "AUXPARTS_2", "AUXPARTS_3", "AUXPARTS_4", "AUXPARTS_5",
                               "CutIn_1", "CutIn_2", "CutIn_3", "CutIn_4", mastery,
                               "CARD_CHARACTER_1", "CARD_CHARACTER_2", "CARD_CHARACTER_3", "CARD_CHARACTER_4",
                               "CARD_CADDIE_1", "CARD_CADDIE_2", "CARD_CADDIE_3", "CARD_CADDIE_4",
                               "CARD_NPC_1", "CARD_NPC_2", "CARD_NPC_3", "CARD_NPC_4"
                          FROM pangya.pangya_rank_atual_character
                         WHERE uid = :uid
                         LIMIT 1
                        """)
                .bind("uid", uid)
                .map((rs, ctx) -> {
                    org.pangya.protocol.game.GamePackets.CharacterInfo c =
                            new org.pangya.protocol.game.GamePackets.CharacterInfo();
                    c.id = rs.getInt("item_id");
                    c.typeid = rs.getInt("typeid");
                    for (int i = 0; i < 24; i++) {
                        c.partsTypeid[i] = rs.getInt("parts_" + (i + 1));
                        c.partsId[i] = rs.getInt("itemid_parts_" + (i + 1));
                    }
                    c.defaultHair = rs.getInt("default_hair");
                    c.defaultShirts = rs.getInt("default_shirts");
                    c.giftFlag = rs.getInt("gift_flag");
                    c.purchase = rs.getInt("purchase");
                    for (int i = 0; i < 5; i++) {
                        c.pcl[i] = (byte) rs.getInt("PCL" + i);
                        c.auxparts[i] = rs.getInt("AUXPARTS_" + (i + 1));
                    }
                    for (int i = 0; i < 4; i++) {
                        c.cutIn[i] = rs.getInt("CutIn_" + (i + 1));
                        c.cardCharacter[i] = rs.getInt("CARD_CHARACTER_" + (i + 1));
                        c.cardCaddie[i] = rs.getInt("CARD_CADDIE_" + (i + 1));
                        c.cardNpc[i] = rs.getInt("CARD_NPC_" + (i + 1));
                    }
                    c.mastery = rs.getInt("mastery");
                    return c;
                })
                .findOne());
        if (snap.isPresent()) {
            return snap;
        }
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
                         LIMIT 1
                        """)
                .bind("uid", uid)
                .map((rs, ctx) -> {
                    org.pangya.protocol.game.GamePackets.CharacterInfo c =
                            new org.pangya.protocol.game.GamePackets.CharacterInfo();
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
                        c.auxparts[i] = rs.getInt("auxparts_" + (i + 1));
                    }
                    for (int i = 0; i < 4; i++) {
                        c.cutIn[i] = rs.getInt("CutIn_" + (i + 1));
                    }
                    c.mastery = rs.getInt("Mastery");
                    return c;
                })
                .findOne());
    }
}
