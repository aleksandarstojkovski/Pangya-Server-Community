-- SQL stand-in for C# ClubSet.work_shop.flag_transformar + findClubSetOriginal.
ALTER TABLE pangya.iff_clubset
    ADD COLUMN flag_transformar INTEGER NOT NULL DEFAULT 0;

CREATE TABLE pangya.iff_clubset_original (
    special_typeid INTEGER NOT NULL,
    original_typeid INTEGER NOT NULL,
    slot0 SMALLINT NOT NULL DEFAULT 0,
    slot1 SMALLINT NOT NULL DEFAULT 0,
    slot2 SMALLINT NOT NULL DEFAULT 0,
    slot3 SMALLINT NOT NULL DEFAULT 0,
    slot4 SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (special_typeid, original_typeid)
);
