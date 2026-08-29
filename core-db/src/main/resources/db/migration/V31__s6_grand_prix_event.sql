-- SQL stand-in for active C# IFF GrandPrixData without optional restrictions.
CREATE TABLE pangya.grand_prix_event (
    typeid INTEGER PRIMARY KEY,
    active SMALLINT NOT NULL DEFAULT 1,
    name VARCHAR(64) NOT NULL,
    holes SMALLINT NOT NULL,
    course SMALLINT NOT NULL,
    modo SMALLINT NOT NULL,
    natural_mode SMALLINT NOT NULL DEFAULT 0,
    rule INTEGER NOT NULL DEFAULT 0,
    min_level INTEGER NOT NULL DEFAULT 0,
    max_level INTEGER NOT NULL DEFAULT 0
);
