-- Remaining C# IFF Card.EffectTime field for requestUseCardSpecial.
ALTER TABLE pangya.iff_card
    ADD COLUMN efeito_tempo INTEGER NOT NULL DEFAULT 0;
