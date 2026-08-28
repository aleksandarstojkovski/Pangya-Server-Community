-- SQL stand-in for C# IFF ClubSet.work_shop.tipo_rank_s + findClubSetWorkShopRankExp.
-- Rank[] values are zeros until a later port needs hard-reset refunds.
ALTER TABLE pangya.iff_clubset
    ADD COLUMN tipo_rank_s INTEGER NOT NULL DEFAULT 0;

CREATE TABLE pangya.iff_clubset_rank_exp (
    tipo INTEGER PRIMARY KEY
);
