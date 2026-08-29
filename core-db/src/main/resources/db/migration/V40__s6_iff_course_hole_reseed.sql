-- Ensure all 18 Blue Lagoon holes are present (idempotent re-seed).
DELETE FROM pangya.iff_course_hole WHERE course_id = 0;
INSERT INTO pangya.iff_course_hole (course_id, hole, par) VALUES
    (0, 1, 4), (0, 2, 4), (0, 3, 3), (0, 4, 4), (0, 5, 5), (0, 6, 4),
    (0, 7, 3), (0, 8, 4), (0, 9, 4), (0, 10, 4), (0, 11, 3), (0, 12, 4),
    (0, 13, 5), (0, 14, 4), (0, 15, 3), (0, 16, 4), (0, 17, 4), (0, 18, 4);
