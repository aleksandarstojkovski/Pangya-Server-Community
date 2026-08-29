-- Ground/air cube locations for Blue Lagoon hole 1 (par 4 → up to 5 coin/cube slots).
INSERT INTO pangya.pangya_coin_cube_location (
    "index", course, hole, tipo, tipo_location, rate, x, y, z, reg_date
) OVERRIDING SYSTEM VALUE VALUES
    (100, 0, 1, 0, 1, 100, 1.0, 0.0, 1.0, NOW()),
    (101, 0, 1, 0, 1, 100, 2.0, 0.0, 2.0, NOW()),
    (102, 0, 1, 0, 1, 100, 3.0, 0.0, 3.0, NOW()),
    (103, 0, 3, 1, 2, 100, 0.0, 5.0, 0.0, NOW())
ON CONFLICT ("index") DO NOTHING;
