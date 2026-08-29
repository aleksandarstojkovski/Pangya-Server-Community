-- S4: starter Nuri + Air Knight club + default ball so Game login can dump live inventory.
-- Typeids from C# IFF_GROUP << 26: CHARACTER Nuri 0x4000000, CLUBSET Air Knight 0x10000000, BALL 0x14000000.

INSERT INTO pangya.pangya_character_information (
    item_id, typeid, "UID",
    parts_1, parts_2, parts_3, parts_4, parts_5, parts_6, parts_7, parts_8,
    parts_9, parts_10, parts_11, parts_12, parts_13, parts_14, parts_15, parts_16,
    parts_17, parts_18, parts_19, parts_20, parts_21, parts_22, parts_23, parts_24,
    default_hair, default_shirts, gift_flag,
    "PCL0", "PCL1", "PCL2", "PCL3", "PCL4", "Purchase",
    auxparts_1, auxparts_2, auxparts_3, auxparts_4, auxparts_5,
    "CutIn_1", "CutIn_2", "CutIn_3", "CutIn_4", "Mastery"
) OVERRIDING SYSTEM VALUE VALUES (
    1, 67108864, 10001,
    0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0,
    0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0,
    0, 0, 0, 0, 0
);

INSERT INTO pangya.pangya_item_warehouse (
    item_id, "UID", typeid, valid, "Gift_flag", flag,
    "C0", "C1", "C2", "C3", "C4", "Purchase", "ItemType",
    "ClubSet_WorkShop_Flag", "ClubSet_WorkShop_C0", "ClubSet_WorkShop_C1",
    "ClubSet_WorkShop_C2", "ClubSet_WorkShop_C3", "ClubSet_WorkShop_C4",
    "Mastery_Pts", "Recovery_Pts", "Level", "Up", "Total_Mastery_Pts", "Mastery_Gasto"
) OVERRIDING SYSTEM VALUE VALUES
    (2, 10001, 268435456, 1, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
    (3, 10001, 335544320, 1, 0, 0, 1, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

INSERT INTO pangya.pangya_user_equip (
    "UID", caddie_id, character_id, club_id, ball_type,
    item_slot_1, item_slot_2, item_slot_3, item_slot_4, item_slot_5,
    item_slot_6, item_slot_7, item_slot_8, item_slot_9, item_slot_10,
    "Skin_1", "Skin_2", "Skin_3", "Skin_4", "Skin_5", "Skin_6",
    mascot_id, poster_1, poster_2
) VALUES (
    10001, 0, 1, 2, 335544320,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0,
    0, 0, 0
);

SELECT setval(pg_get_serial_sequence('pangya.pangya_character_information', 'item_id'), 10, false);
SELECT setval(pg_get_serial_sequence('pangya.pangya_item_warehouse', 'item_id'), 10, false);
