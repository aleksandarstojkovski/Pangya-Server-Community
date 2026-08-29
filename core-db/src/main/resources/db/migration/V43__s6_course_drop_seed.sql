-- C# pangya_new_course_drop config + course 0 event items for HoleDropResolver tests.
INSERT INTO pangya.pangya_new_course_drop (
    rate_mana_artefact, rate_grand_prix_ticket, "rate_SSC_ticket"
) VALUES (100, 100, 100);

INSERT INTO pangya.pangya_new_course_drop_item (
    course, tipo, typeid, quantidade,
    "probabilidade_3H", "probabilidade_6H", "probabilidade_9H", "probabilidade_18H", active
) VALUES
    (0, 0, 436207786, 1, 1000, 1000, 1000, 1000, 1),
    (0, 0, 436208428, 1, 500, 500, 500, 500, 1);
