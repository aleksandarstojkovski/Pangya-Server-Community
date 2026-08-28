-- SQL stand-in for C# MapSystem / IFF course Par_Hole until binary map loaders exist.
CREATE TABLE IF NOT EXISTS pangya.iff_course_hole (
    course_id SMALLINT NOT NULL,
    hole SMALLINT NOT NULL,
    par SMALLINT NOT NULL,
    PRIMARY KEY (course_id, hole)
);

-- Blue Lagoon (course 0) JP Season 9 par sequence.
INSERT INTO pangya.iff_course_hole (course_id, hole, par) VALUES
    (0, 1, 4), (0, 2, 4), (0, 3, 3), (0, 4, 4), (0, 5, 5), (0, 6, 4),
    (0, 7, 3), (0, 8, 4), (0, 9, 4), (0, 10, 4), (0, 11, 3), (0, 12, 4),
    (0, 13, 5), (0, 14, 4), (0, 15, 3), (0, 16, 4), (0, 17, 4), (0, 18, 4)
ON CONFLICT (course_id, hole) DO UPDATE SET par = EXCLUDED.par;
