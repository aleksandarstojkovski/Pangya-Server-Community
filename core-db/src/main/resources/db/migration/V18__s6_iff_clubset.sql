-- SQL stand-in for C# IFF ClubSet.work_shop.tipo (requestClubSetWorkShopRecoveryPts).
-- tipo -1 cannot recover; 0 can. Catalog rows are test-inserted.
CREATE TABLE pangya.iff_clubset (
    typeid INTEGER PRIMARY KEY,
    work_shop_tipo INTEGER NOT NULL
);
