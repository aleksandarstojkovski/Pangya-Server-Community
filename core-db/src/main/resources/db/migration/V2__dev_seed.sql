-- Minimal dev seed. Production historical data is out of scope (empty PostgreSQL).
-- Rank refresh interval is required for Ranking Server startup parity with C#.
-- Achievement / shop / event catalogs will be loaded in later slices from the C# dump.

INSERT INTO pangya.pangya_rank_config ("index", "refresh_time_H", reg_date)
VALUES (1, 1, NOW());
