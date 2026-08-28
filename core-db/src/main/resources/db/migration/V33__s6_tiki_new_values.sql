-- New Tiki exchange fields from IFF common-item tiki metadata.
ALTER TABLE pangya.legacy_tiki_item_value
    ADD COLUMN tiki_pang BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN mileage INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN bonus_min INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN bonus_max INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN bonus_prob INTEGER NOT NULL DEFAULT 0;
