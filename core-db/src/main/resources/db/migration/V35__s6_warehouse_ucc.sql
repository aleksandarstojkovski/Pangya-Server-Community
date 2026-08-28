-- C# WarehouseItem.UCC fields used by UCC option 1 and 196-byte serialization.
ALTER TABLE pangya.pangya_item_warehouse
    ADD COLUMN ucc_name VARCHAR(40) NOT NULL DEFAULT '',
    ADD COLUMN ucc_trade SMALLINT NOT NULL DEFAULT 0,
    ADD COLUMN ucc_idx VARCHAR(9) NOT NULL DEFAULT '',
    ADD COLUMN ucc_status SMALLINT NOT NULL DEFAULT 0,
    ADD COLUMN ucc_seq SMALLINT NOT NULL DEFAULT 0,
    ADD COLUMN ucc_copier_nick VARCHAR(22) NOT NULL DEFAULT '',
    ADD COLUMN ucc_copier INTEGER NOT NULL DEFAULT 0;
