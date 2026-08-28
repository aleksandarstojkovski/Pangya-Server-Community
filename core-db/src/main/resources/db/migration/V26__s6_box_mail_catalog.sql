-- Deterministic SQL stand-in for C# BoxSystem generic-box draw path.
CREATE TABLE pangya.box_mail_catalog (
    box_typeid INTEGER PRIMARY KEY,
    reward_typeid INTEGER NOT NULL,
    reward_qntd INTEGER NOT NULL,
    opened_typeid INTEGER NOT NULL DEFAULT 0,
    message VARCHAR(200) NOT NULL DEFAULT ''
);
