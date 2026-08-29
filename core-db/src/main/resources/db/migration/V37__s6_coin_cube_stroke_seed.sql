-- SQL stand-in for stroke course 0 hole 1 coin (C# CubeCoinSystem / requestInitCubeCoin).
INSERT INTO pangya.pangya_coin_cube_info (course_id, active, update_date)
SELECT 0, 1, NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM pangya.pangya_coin_cube_info WHERE course_id = 0
);

INSERT INTO pangya.pangya_coin_cube_location (
    "index", course, hole, tipo, tipo_location, rate, x, y, z, reg_date
) OVERRIDING SYSTEM VALUE VALUES (
    99, 0, 1, 0, 0, 100, 0, 0, 0, NOW()
)
ON CONFLICT ("index") DO NOTHING;
