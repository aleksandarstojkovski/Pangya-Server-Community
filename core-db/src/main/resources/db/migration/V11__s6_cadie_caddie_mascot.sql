-- SQL stand-ins for C# IFF CadieMagicBox / Caddie.valor_mensal / Mascot.msg.
-- Caddie group 7 << 26 = 0x1C000000 (Papel). Mascot group 16 << 26 = 0x40000000.
-- Cadie seq 1 trades shop pang item 0x1A000006 for the same typeid.

CREATE TABLE pangya.cadie_magic_box (
    seq INTEGER PRIMARY KEY,
    active SMALLINT NOT NULL,
    level INTEGER NOT NULL,
    receive_typeid INTEGER NOT NULL,
    receive_qntd INTEGER NOT NULL,
    trade0_typeid INTEGER NOT NULL,
    trade0_qntd INTEGER NOT NULL,
    trade1_typeid INTEGER NOT NULL DEFAULT 0,
    trade1_qntd INTEGER NOT NULL DEFAULT 0,
    trade2_typeid INTEGER NOT NULL DEFAULT 0,
    trade2_qntd INTEGER NOT NULL DEFAULT 0,
    trade3_typeid INTEGER NOT NULL DEFAULT 0,
    trade3_qntd INTEGER NOT NULL DEFAULT 0,
    box_random_id INTEGER NOT NULL DEFAULT 0
);

INSERT INTO pangya.cadie_magic_box (
    seq, active, level, receive_typeid, receive_qntd,
    trade0_typeid, trade0_qntd
) VALUES (1, 1, 0, 436207622, 1, 436207622, 1);

CREATE TABLE pangya.iff_caddie (
    typeid INTEGER PRIMARY KEY,
    valor_mensal INTEGER NOT NULL,
    is_cash SMALLINT NOT NULL
);

INSERT INTO pangya.iff_caddie (typeid, valor_mensal, is_cash)
VALUES (469762048, 1000, 0);

CREATE TABLE pangya.iff_mascot (
    typeid INTEGER PRIMARY KEY,
    msg_active SMALLINT NOT NULL,
    change_price INTEGER NOT NULL
);

INSERT INTO pangya.iff_mascot (typeid, msg_active, change_price)
VALUES (1073741824, 1, 100);

INSERT INTO pangya.pangya_caddie_information (
    item_id, "UID", typeid, parts_typeid, gift_flag, "cLevel", "Exp",
    "RegDate", "Period", "EndDate", "RentFlag", "Purchase",
    "parts_EndDate", "CheckEnd", "Valid"
) OVERRIDING SYSTEM VALUE VALUES (
    20, 10001, 469762048, 0, 0, 0, 0,
    NOW(), 30, NOW(), 2, 0,
    NULL, 0, 1
);

INSERT INTO pangya.pangya_mascot_info (
    item_id, "UID", typeid, "mLevel", "mExp", "Flag", "Tipo",
    "RegDate", "Period", "EndDate", "Message", "IsCash", "Price", "Valid"
) OVERRIDING SYSTEM VALUE VALUES (
    21, 10001, 1073741824, 0, 0, 0, 0,
    NOW(), 0, NULL, '', 0, 100, 1
);

SELECT setval(pg_get_serial_sequence('pangya.pangya_caddie_information', 'item_id'), 30, false);
SELECT setval(pg_get_serial_sequence('pangya.pangya_mascot_info', 'item_id'), 30, false);
