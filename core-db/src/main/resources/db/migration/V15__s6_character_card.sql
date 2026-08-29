-- IFF Card.Effect / EffectValue for C# requestCharacterCardEquip.
-- TYPEID 0x7C000001 already has subgroup 22-bit 0 (T_CHARACTER).

ALTER TABLE pangya.iff_card
    ADD COLUMN efeito INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN efeito_qntd INTEGER NOT NULL DEFAULT 0;
