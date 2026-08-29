-- SQL stand-in for C# IFF Card.Rarity + CardSystem.drawsLoloCardCompose.
-- CARD group 31 << 26 | 1 = 0x7C000001. Rarity 0 = T_NORMAL (1000 pang each).

CREATE TABLE pangya.iff_card (
    typeid INTEGER PRIMARY KEY,
    rarity SMALLINT NOT NULL,
    probabilidade INTEGER NOT NULL
);

INSERT INTO pangya.iff_card (typeid, rarity, probabilidade)
VALUES (2080374785, 0, 100);
