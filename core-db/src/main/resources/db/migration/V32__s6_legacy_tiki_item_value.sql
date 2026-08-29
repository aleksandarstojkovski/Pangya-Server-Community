-- SQL stand-in for IFF common-item Tiki_Qnt_Pts/Tiki_Pts (item -> legacy TP).
CREATE TABLE pangya.legacy_tiki_item_value (
    typeid INTEGER PRIMARY KEY,
    item_count INTEGER NOT NULL,
    points INTEGER NOT NULL
);
