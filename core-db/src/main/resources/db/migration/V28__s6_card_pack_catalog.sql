-- Deterministic SQL stand-in for C# CardSystem card-pack draws.
CREATE TABLE pangya.card_pack_catalog (
    pack_typeid INTEGER NOT NULL,
    seq INTEGER NOT NULL,
    card_typeid INTEGER NOT NULL,
    PRIMARY KEY (pack_typeid, seq)
);
