-- Deterministic SQL stand-in for C# MemorialSystem coin draws.
CREATE TABLE pangya.memorial_reward_catalog (
    coin_typeid INTEGER NOT NULL,
    seq INTEGER NOT NULL,
    rarity INTEGER NOT NULL,
    reward_typeid INTEGER NOT NULL,
    qntd INTEGER NOT NULL,
    PRIMARY KEY (coin_typeid, seq)
);
