-- SQL stand-ins for C# IFF Character.PCL and Enchant.Pang.
-- ENCHANT group 13 << 26 = 0x34000000 (POWER at pcl 0).
-- CharacterMastery.stats is 1-based; 0 means no extra PCL slot.

ALTER TABLE pangya.iff_character_mastery
    ADD COLUMN stats INTEGER NOT NULL DEFAULT 0;

CREATE TABLE pangya.iff_character (
    typeid INTEGER PRIMARY KEY,
    pcl0 SMALLINT NOT NULL DEFAULT 0,
    pcl1 SMALLINT NOT NULL DEFAULT 0,
    pcl2 SMALLINT NOT NULL DEFAULT 0,
    pcl3 SMALLINT NOT NULL DEFAULT 0,
    pcl4 SMALLINT NOT NULL DEFAULT 0
);

INSERT INTO pangya.iff_character (typeid, pcl0, pcl1, pcl2, pcl3, pcl4)
VALUES (67108864, 0, 0, 0, 0, 0);

CREATE TABLE pangya.iff_enchant (
    typeid INTEGER PRIMARY KEY,
    pang BIGINT NOT NULL
);

INSERT INTO pangya.iff_enchant (typeid, pang)
VALUES (872415232, 100);
