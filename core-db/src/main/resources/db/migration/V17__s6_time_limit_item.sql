-- SQL stand-in for C# IFF TimeLimitItem (requestUseItemBuff / findTimeLimitItem).
-- Catalog rows are inserted by tests; production C# loads the IFF file.
CREATE TABLE pangya.iff_time_limit_item (
    typeid INTEGER PRIMARY KEY,
    tipo INTEGER NOT NULL,
    percent INTEGER NOT NULL,
    time INTEGER NOT NULL
);
