IF COL_LENGTH('pangya.pangya_item_warehouse', 'item_id') IS NULL
BEGIN
    THROW 50001, 'A tabela pangya.pangya_item_warehouse precisa expor a coluna única item_id antes da implantação.', 1;
END;
GO

IF OBJECT_ID('pangya.web_audit_log', 'U') IS NULL
BEGIN
    CREATE TABLE pangya.web_audit_log (
        audit_id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        actor_uid INT NOT NULL,
        action NVARCHAR(80) NOT NULL,
        item_id INT NULL,
        ip_address NVARCHAR(45) NOT NULL,
        details NVARCHAR(MAX) NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT DF_web_audit_log_created_at DEFAULT SYSUTCDATETIME()
    );
    CREATE INDEX IX_web_audit_log_actor_created ON pangya.web_audit_log(actor_uid, created_at DESC);
END;
GO

IF OBJECT_ID('pangya.web_marketplace_listing', 'U') IS NULL
BEGIN
    CREATE TABLE pangya.web_marketplace_listing (
        listing_id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        seller_uid INT NOT NULL,
        item_id INT NOT NULL,
        typeid INT NOT NULL,
        price BIGINT NOT NULL CHECK (price > 0),
        currency VARCHAR(10) NOT NULL CHECK (currency IN ('Pang', 'Cookie')),
        status VARCHAR(10) NOT NULL CONSTRAINT DF_web_marketplace_listing_status DEFAULT 'active',
        buyer_uid INT NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT DF_web_marketplace_listing_created_at DEFAULT SYSUTCDATETIME(),
        sold_at DATETIME2 NULL
    );
    CREATE UNIQUE INDEX UX_web_marketplace_listing_active_item
        ON pangya.web_marketplace_listing(item_id)
        WHERE status = 'active';
    CREATE INDEX IX_web_marketplace_listing_active ON pangya.web_marketplace_listing(status, created_at DESC);
END;
GO

IF OBJECT_ID('pangya.web_shop_transaction', 'U') IS NULL
BEGIN
    CREATE TABLE pangya.web_shop_transaction (
        transaction_id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        buyer_uid INT NOT NULL,
        seller_uid INT NULL,
        item_id INT NULL,
        typeid INT NULL,
        amount BIGINT NOT NULL,
        currency VARCHAR(10) NOT NULL,
        kind VARCHAR(30) NOT NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT DF_web_shop_transaction_created_at DEFAULT SYSUTCDATETIME()
    );
    CREATE INDEX IX_web_shop_transaction_buyer_created ON pangya.web_shop_transaction(buyer_uid, created_at DESC);
END;
GO
