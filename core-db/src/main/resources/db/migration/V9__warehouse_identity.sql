-- V6 inserted warehouse/character rows with OVERRIDING SYSTEM VALUE (ids 11–13)
-- without advancing the identity sequences. Next INSERT reused 12 and collided.
SELECT setval(
    pg_get_serial_sequence('pangya.pangya_item_warehouse', 'item_id'),
    (SELECT COALESCE(MAX(item_id), 1) FROM pangya.pangya_item_warehouse)
);
SELECT setval(
    pg_get_serial_sequence('pangya.pangya_character_information', 'item_id'),
    (SELECT COALESCE(MAX(item_id), 1) FROM pangya.pangya_character_information)
);
