-- SQL stand-in for C# PapelShopSystem (pangya_papel_shop_*).
-- C# isLoad() needs at least one coupon row and one item row; dropBalls uses
-- shop_catalog typeid 0x1A000006 (same IFF ITEM as shop buy). Limitted_YN=0
-- matches unlimited 0xFB i32 -1 / -3. Coupon typeid is unused gacha 0x1A000080.

INSERT INTO pangya.pangya_papel_shop_config
    ("Numero", "Price_Normal", "Price_Big", "Limitted_YN", "Update_Date")
VALUES (1, 1000, 3000, 0, NOW());

INSERT INTO pangya.pangya_papel_shop_coupon (typeid, active)
VALUES (436207744, 1);

INSERT INTO pangya.pangya_papel_shop_item
    ("Nome", typeid, probabilidade, numero, tipo, active)
VALUES ('Pang Item', 436207622, 100, -1, 0, 1);
