-- SQL stand-in for C# IFF CharacterMastery. Nuri 1 << 26 = 0x4000000.
-- Seq 1 is the first expand (pCi.mastery + 1). Condition is shop pang item qty 1.

CREATE TABLE pangya.iff_character_mastery (
    typeid INTEGER NOT NULL,
    seq INTEGER NOT NULL,
    level INTEGER NOT NULL,
    cond0_typeid INTEGER NOT NULL DEFAULT 0,
    cond0_qntd INTEGER NOT NULL DEFAULT 0,
    cond1_typeid INTEGER NOT NULL DEFAULT 0,
    cond1_qntd INTEGER NOT NULL DEFAULT 0,
    cond2_typeid INTEGER NOT NULL DEFAULT 0,
    cond2_qntd INTEGER NOT NULL DEFAULT 0,
    cond3_typeid INTEGER NOT NULL DEFAULT 0,
    cond3_qntd INTEGER NOT NULL DEFAULT 0,
    cond4_typeid INTEGER NOT NULL DEFAULT 0,
    cond4_qntd INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (typeid, seq)
);

INSERT INTO pangya.iff_character_mastery (
    typeid, seq, level, cond0_typeid, cond0_qntd
) VALUES (67108864, 1, 0, 436207622, 1);
