package org.pangya.db;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

/**
 * PostgreSQL port of C# {@code pangya.GeraRankAll} ({@code CmdUpdateRankRegistry}).
 * Course boards and user_info boards are rebuilt; character snapshot is the
 * first {@code pangya_character_information} row per ranked UID.
 */
final class GeraRankAll {

    /** C# comment block: BL..MR (course 12 / 17 omitted). */
    private static final int[] COURSES = {
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 14, 15, 16, 18, 19, 20, 21
    };

    private GeraRankAll() {}

    static int run(Jdbi jdbi) {
        return jdbi.inTransaction(GeraRankAll::runInHandle);
    }

    private static int runInHandle(Handle h) {
        h.execute("DROP TABLE IF EXISTS gera_rank_tmp");
        h.execute("""
                CREATE TEMP TABLE gera_rank_tmp (
                    position INTEGER NOT NULL,
                    uid INTEGER NOT NULL,
                    tipo_rank SMALLINT NOT NULL,
                    tipo_rank_seq SMALLINT NOT NULL,
                    valor INTEGER NOT NULL
                ) ON COMMIT DROP
                """);
        insertOverallScore(h);
        insertTrophies(h);
        insertUserInfoBoard(h, 0, 3, "FLOOR(a.total_pang_win_game / 1000)");
        insertUserInfoBoard(h, 0, 4, "FLOOR((a.\"Holes\" - a.\"Holein\") / 18)");
        insertAchievements(h);
        insertOverallSum(h);
        insertUserInfoBoard(h, 2, 0, "a.\"Albatross\"");
        insertUserInfoBoard(h, 2, 1, "a.\"HIO\"");
        insertUserInfoLevel(h);
        insertUserInfoBoard(h, 2, 4, "a.\"Total_distancia\"");
        insertCourseBoards(h, 0, 1);
        insertCourseBoards(h, 51, 3);
        insertCourseBoards(h, 52, 4);

        int tmp = h.createQuery("SELECT COUNT(*) FROM gera_rank_tmp").mapTo(Integer.class).one();
        int atual = h.createQuery("SELECT COUNT(*) FROM pangya.pangya_rank_atual").mapTo(Integer.class).one();
        if (tmp <= 0 || tmp < atual) {
            return 0;
        }
        h.execute("DELETE FROM pangya.pangya_rank_antes");
        h.execute("""
                INSERT INTO pangya.pangya_rank_antes (position, "UID", tipo_rank, tipo_rank_seq, valor)
                SELECT position, "UID", tipo_rank, tipo_rank_seq, valor FROM pangya.pangya_rank_atual
                """);
        h.execute("DELETE FROM pangya.pangya_rank_atual");
        h.execute("""
                INSERT INTO pangya.pangya_rank_atual (position, "UID", tipo_rank, tipo_rank_seq, valor)
                SELECT position, uid, tipo_rank, tipo_rank_seq, valor FROM gera_rank_tmp
                """);
        refreshCharacters(h);
        return tmp;
    }

    private static void insertOverallScore(Handle h) {
        h.execute("""
                INSERT INTO gera_rank_tmp (position, uid, tipo_rank, tipo_rank_seq, valor)
                SELECT ROW_NUMBER() OVER (ORDER BY x.score DESC, x.uid), x.uid, 0, 1, x.score
                  FROM (
                    SELECT b.uid,
                           (b.s1 + b.s2 + b.s3 + b.s4 + b.s5 + b.s7 + b.s8 + b.s9 + b.s10
                            + b.s11 + b.s12 + b.s13 + b.s14 + b.s15 + b.s16 + b.s17 + b.s18
                            + b.s19 + b.s20) AS score
                      FROM (
                        SELECT a.uid,
                               MAX(CASE WHEN a.course = 0 THEN (50 - a.score_1) * 10 ELSE 50 END) AS s1,
                               MAX(CASE WHEN a.course = 5 THEN (50 - a.score_1) * 10 ELSE 50 END) AS s2,
                               MAX(CASE WHEN a.course = 11 THEN (50 - a.score_1) * 10 ELSE 50 END) AS s3,
                               MAX(CASE WHEN a.course = 14 THEN (50 - a.score_1) * 10 ELSE 50 END) AS s4,
                               MAX(CASE WHEN a.course = 15 THEN (50 - a.score_1) * 10 ELSE 50 END) AS s5,
                               MAX(CASE WHEN a.course = 8 THEN (50 - a.score_1) * 20 ELSE 50 END) AS s6,
                               MAX(CASE WHEN a.course = 10 THEN (50 - a.score_1) * 20 ELSE 50 END) AS s7,
                               MAX(CASE WHEN a.course = 16 THEN (50 - a.score_1) * 20 ELSE 50 END) AS s8,
                               MAX(CASE WHEN a.course = 19 THEN (50 - a.score_1) * 20 ELSE 50 END) AS s9,
                               MAX(CASE WHEN a.course = 20 THEN (50 - a.score_1) * 20 ELSE 50 END) AS s10,
                               MAX(CASE WHEN a.course = 1 THEN (50 - a.score_1) * 30 ELSE 50 END) AS s11,
                               MAX(CASE WHEN a.course = 2 THEN (50 - a.score_1) * 30 ELSE 50 END) AS s12,
                               MAX(CASE WHEN a.course = 6 THEN (50 - a.score_1) * 30 ELSE 50 END) AS s13,
                               MAX(CASE WHEN a.course = 9 THEN (50 - a.score_1) * 30 ELSE 50 END) AS s14,
                               MAX(CASE WHEN a.course = 21 THEN (50 - a.score_1) * 30 ELSE 50 END) AS s15,
                               MAX(CASE WHEN a.course = 4 THEN (50 - a.score_1) * 40 ELSE 50 END) AS s16,
                               MAX(CASE WHEN a.course = 7 THEN (50 - a.score_1) * 40 ELSE 50 END) AS s17,
                               MAX(CASE WHEN a.course = 18 THEN (50 - a.score_1) * 40 ELSE 50 END) AS s18,
                               MAX(CASE WHEN a.course = 3 THEN (50 - a.score_1) * 50 ELSE 50 END) AS s19,
                               MAX(CASE WHEN a.course = 13 THEN (50 - a.score_1) * 50 ELSE 50 END) AS s20
                          FROM (
                                SELECT f.course, f."UID" AS uid, f.best_score AS score_1
                                  FROM pangya.pangya_record f
                                  JOIN pangya.account g ON f."UID" = g."UID"
                                 WHERE (g."FIRST_LOGIN" + g."FIRST_SET") = 2
                                   AND f.best_score <> 127
                                   AND f.tipo IN (0, 51, 52)
                               ) a
                         GROUP BY a.uid
                      ) b
                  ) x
                """);
    }

    private static void insertTrophies(Handle h) {
        h.execute("""
                INSERT INTO gera_rank_tmp (position, uid, tipo_rank, tipo_rank_seq, valor)
                SELECT ROW_NUMBER() OVER (ORDER BY c.soma DESC, c.uid), c.uid, 0, 2, c.soma
                  FROM (
                    SELECT a."UID" AS uid,
                           (a."AMA_6_G" * 3 + a."AMA_6_S" * 2 + a."AMA_6_B"
                            + a."AMA_5_G" * 3 + a."AMA_5_S" * 2 + a."AMA_5_B"
                            + a."AMA_4_G" * 3 + a."AMA_4_S" * 2 + a."AMA_4_B"
                            + (a."AMA_3_G" * 3 + a."AMA_3_S" * 2 + a."AMA_3_B") * 2
                            + (a."AMA_2_G" * 3 + a."AMA_2_S" * 2 + a."AMA_2_B") * 2
                            + (a."AMA_1_G" * 3 + a."AMA_1_S" * 2 + a."AMA_1_B") * 2
                            + (a."PRO_1_G" * 3 + a."PRO_1_S" * 2 + a."PRO_1_B") * 3
                            + (a."PRO_2_G" * 3 + a."PRO_2_S" * 2 + a."PRO_2_B") * 3
                            + (a."PRO_3_G" * 3 + a."PRO_3_S" * 2 + a."PRO_3_B") * 3
                            + (a."PRO_4_G" * 3 + a."PRO_4_S" * 2 + a."PRO_4_B") * 4
                            + (a."PRO_5_G" * 3 + a."PRO_5_S" * 2 + a."PRO_5_B") * 4
                            + (a."PRO_6_G" * 3 + a."PRO_6_S" * 2 + a."PRO_6_B") * 4
                            + (a."PRO_7_G" * 3 + a."PRO_7_S" * 2 + a."PRO_7_B") * 5) AS soma
                      FROM pangya.trofel_stat a
                      JOIN pangya.account b ON a."UID" = b."UID"
                     WHERE (b."FIRST_LOGIN" + b."FIRST_SET") = 2
                  ) c
                """);
    }

    private static void insertUserInfoBoard(Handle h, int menu, int item, String expr) {
        h.execute("""
                INSERT INTO gera_rank_tmp (position, uid, tipo_rank, tipo_rank_seq, valor)
                SELECT ROW_NUMBER() OVER (ORDER BY v DESC, uid), uid, %d, %d, v
                  FROM (
                    SELECT a."UID" AS uid, (%s)::int AS v
                      FROM pangya.user_info a
                      JOIN pangya.account b ON a."UID" = b."UID"
                     WHERE (b."FIRST_LOGIN" + b."FIRST_SET") = 2
                  ) q
                """.formatted(menu, item, expr));
    }

    private static void insertUserInfoLevel(Handle h) {
        h.execute("""
                INSERT INTO gera_rank_tmp (position, uid, tipo_rank, tipo_rank_seq, valor)
                SELECT ROW_NUMBER() OVER (ORDER BY a."level" DESC, a."Xp" DESC, a."UID"),
                       a."UID", 2, 3, a."level"
                  FROM pangya.user_info a
                  JOIN pangya.account b ON a."UID" = b."UID"
                 WHERE (b."FIRST_LOGIN" + b."FIRST_SET") = 2
                """);
    }

    private static void insertAchievements(Handle h) {
        h.execute("""
                INSERT INTO gera_rank_tmp (position, uid, tipo_rank, tipo_rank_seq, valor)
                SELECT ROW_NUMBER() OVER (ORDER BY q.pontos DESC, q.uid), q.uid, 0, 5, q.pontos
                  FROM (
                    SELECT a."UID" AS uid, COALESCE(x.pontos, 0)::int AS pontos
                      FROM pangya.account a
                      LEFT JOIN (
                            SELECT b."UID" AS uid, COUNT(*) * 10 AS pontos
                              FROM pangya.pangya_achievement b
                              LEFT JOIN pangya.pangya_quest c
                                ON b."ID_ACHIEVEMENT" = c.achievement_id
                             WHERE c."Date" IS NOT NULL
                             GROUP BY b."UID"
                      ) x ON a."UID" = x.uid
                     WHERE (a."FIRST_LOGIN" + a."FIRST_SET") = 2
                  ) q
                """);
    }

    private static void insertOverallSum(Handle h) {
        h.execute("""
                INSERT INTO gera_rank_tmp (position, uid, tipo_rank, tipo_rank_seq, valor)
                SELECT ROW_NUMBER() OVER (ORDER BY a.valor DESC), a.uid, 0, 0, a.valor
                  FROM (
                    SELECT uid, SUM(valor)::int AS valor
                      FROM gera_rank_tmp
                     WHERE tipo_rank = 0
                     GROUP BY uid
                  ) a
                """);
    }

    private static void insertCourseBoards(Handle h, int recordTipo, int menu) {
        for (int i = 0; i < COURSES.length; i++) {
            h.createUpdate("""
                    INSERT INTO gera_rank_tmp (position, uid, tipo_rank, tipo_rank_seq, valor)
                    SELECT ROW_NUMBER() OVER (ORDER BY z.best_score, z.best_pang DESC, z.uid),
                           z.uid, :menu, :seq, z.best_score
                      FROM (
                        SELECT f."UID" AS uid,
                               MIN(f.best_score) AS best_score,
                               MAX(f.best_pang) AS best_pang
                          FROM pangya.pangya_record f
                          JOIN pangya.account g ON f."UID" = g."UID"
                         WHERE (g."FIRST_LOGIN" + g."FIRST_SET") = 2
                           AND f.best_score <> 127
                           AND f.course = :course
                           AND f.tipo = :tipo
                         GROUP BY f."UID"
                      ) z
                    """)
                    .bind("menu", menu)
                    .bind("seq", i)
                    .bind("course", COURSES[i])
                    .bind("tipo", recordTipo)
                    .execute();
        }
    }

    private static void refreshCharacters(Handle h) {
        h.execute("DELETE FROM pangya.pangya_rank_atual_character");
        h.execute("""
                INSERT INTO pangya.pangya_rank_atual_character (
                    uid, item_id, typeid,
                    itemid_parts_1, itemid_parts_2, itemid_parts_3, itemid_parts_4,
                    itemid_parts_5, itemid_parts_6, itemid_parts_7, itemid_parts_8,
                    itemid_parts_9, itemid_parts_10, itemid_parts_11, itemid_parts_12,
                    itemid_parts_13, itemid_parts_14, itemid_parts_15, itemid_parts_16,
                    itemid_parts_17, itemid_parts_18, itemid_parts_19, itemid_parts_20,
                    itemid_parts_21, itemid_parts_22, itemid_parts_23, itemid_parts_24,
                    parts_1, parts_2, parts_3, parts_4, parts_5, parts_6, parts_7, parts_8,
                    parts_9, parts_10, parts_11, parts_12, parts_13, parts_14, parts_15, parts_16,
                    parts_17, parts_18, parts_19, parts_20, parts_21, parts_22, parts_23, parts_24,
                    default_hair, default_shirts, gift_flag,
                    "PCL0", "PCL1", "PCL2", "PCL3", "PCL4", purchase,
                    "AUXPARTS_1", "AUXPARTS_2", "AUXPARTS_3", "AUXPARTS_4", "AUXPARTS_5",
                    "CutIn_1", "CutIn_2", "CutIn_3", "CutIn_4", mastery,
                    "CARD_CHARACTER_1", "CARD_CHARACTER_2", "CARD_CHARACTER_3", "CARD_CHARACTER_4",
                    "CARD_CADDIE_1", "CARD_CADDIE_2", "CARD_CADDIE_3", "CARD_CADDIE_4",
                    "CARD_NPC_1", "CARD_NPC_2", "CARD_NPC_3", "CARD_NPC_4"
                )
                SELECT DISTINCT ON (c."UID")
                       c."UID", c.item_id, c.typeid,
                       0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
                       c.parts_1, c.parts_2, c.parts_3, c.parts_4, c.parts_5, c.parts_6, c.parts_7, c.parts_8,
                       c.parts_9, c.parts_10, c.parts_11, c.parts_12, c.parts_13, c.parts_14, c.parts_15, c.parts_16,
                       c.parts_17, c.parts_18, c.parts_19, c.parts_20, c.parts_21, c.parts_22, c.parts_23, c.parts_24,
                       c.default_hair, c.default_shirts, c.gift_flag,
                       c."PCL0", c."PCL1", c."PCL2", c."PCL3", c."PCL4", c."Purchase",
                       c.auxparts_1, c.auxparts_2, c.auxparts_3, c.auxparts_4, c.auxparts_5,
                       c."CutIn_1", c."CutIn_2", c."CutIn_3", c."CutIn_4", c."Mastery",
                       0,0,0,0,0,0,0,0,0,0,0,0
                  FROM pangya.pangya_character_information c
                 WHERE c."UID" IN (SELECT DISTINCT "UID" FROM pangya.pangya_rank_atual)
                 ORDER BY c."UID", c.item_id
                """);
    }
}
