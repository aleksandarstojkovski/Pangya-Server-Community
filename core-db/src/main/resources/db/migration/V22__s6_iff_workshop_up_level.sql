-- SQL stand-in for C# IFF findItem + findClubSetWorkShopLevelUpLimit/Prob
-- and ClubSet.work_shop.total_recovery (up-level cancel).
ALTER TABLE pangya.iff_clubset
    ADD COLUMN total_recovery INTEGER NOT NULL DEFAULT 0;

CREATE TABLE pangya.iff_item (
    typeid INTEGER PRIMARY KEY
);

CREATE TABLE pangya.iff_clubset_level_up_limit (
    tipo INTEGER NOT NULL,
    rank INTEGER NOT NULL,
    c0 SMALLINT NOT NULL DEFAULT 0,
    c1 SMALLINT NOT NULL DEFAULT 0,
    c2 SMALLINT NOT NULL DEFAULT 0,
    c3 SMALLINT NOT NULL DEFAULT 0,
    c4 SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (tipo, rank)
);

CREATE TABLE pangya.iff_clubset_level_up_prob (
    tipo INTEGER PRIMARY KEY,
    c0 INTEGER NOT NULL DEFAULT 0,
    c1 INTEGER NOT NULL DEFAULT 0,
    c2 INTEGER NOT NULL DEFAULT 0,
    c3 INTEGER NOT NULL DEFAULT 0,
    c4 INTEGER NOT NULL DEFAULT 0
);
