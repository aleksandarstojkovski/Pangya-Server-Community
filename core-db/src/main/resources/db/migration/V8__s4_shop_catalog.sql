-- SQL stand-in for C# IFF IsBuyItem/price (no IFF binaries in this env).
-- Typeid 0x1A000006 (IFF ITEM group) is a pang shop consumable from C# item tables.
-- Seed Pang so Game CLIENT 0x1D can complete pacote0AA + 0x68 option 0.

CREATE TABLE pangya.shop_catalog (
    typeid INTEGER NOT NULL,
    pang_price INTEGER NOT NULL,
    cookie_price INTEGER NOT NULL,
    can_overlap SMALLINT NOT NULL,
    PRIMARY KEY (typeid)
);

INSERT INTO pangya.shop_catalog (typeid, pang_price, cookie_price, can_overlap)
VALUES (436207622, 100, 0, 1);

UPDATE pangya.user_info SET "Pang" = 100000 WHERE "UID" IN (10001, 10002);
