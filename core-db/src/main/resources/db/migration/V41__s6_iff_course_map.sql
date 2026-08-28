-- SQL stand-in for C# MapSystem / IFF Course (clear_bonus, name, star) until binary loaders exist.
CREATE TABLE IF NOT EXISTS pangya.iff_course (
    course_id SMALLINT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    star_tenths SMALLINT NOT NULL DEFAULT 10,
    clear_bonus INTEGER NOT NULL DEFAULT 0
);

INSERT INTO pangya.iff_course (course_id, name, star_tenths, clear_bonus) VALUES
    (0, 'Blue Lagoon', 10, 20),
    (1, 'Blue Water', 10, 50),
    (2, 'Sepia Wind', 10, 55),
    (3, 'Wind Hill', 10, 80),
    (4, 'Wiz Wiz', 10, 65),
    (5, 'West Wiz', 10, 24),
    (6, 'Blue Moon', 10, 50),
    (7, 'Silvia Cannon', 10, 70),
    (8, 'Ice Cannon', 10, 40),
    (9, 'White Wiz', 10, 55),
    (10, 'Shining Sand', 10, 40),
    (11, 'Pink Wind', 10, 20),
    (13, 'Deep Inferno', 10, 80),
    (14, 'Ice Spa', 10, 20),
    (15, 'Lost Seaway', 10, 20),
    (16, 'Eastern Valley', 10, 40),
    (17, 'Chronicle 1 Chaos', 10, 360),
    (18, 'Ice Inferno', 10, 70),
    (19, 'Wiz City', 10, 40),
    (20, 'Abbot Mine', 10, 40),
    (21, 'Mystic Ruins', 10, 40),
    (64, 'Grand Zodiac', 10, 0)
ON CONFLICT (course_id) DO UPDATE SET
    name = EXCLUDED.name,
    star_tenths = EXCLUDED.star_tenths,
    clear_bonus = EXCLUDED.clear_bonus;
