package org.pangya.game;

import org.pangya.protocol.game.GamePackets;

/** C# {@code AchievementSystem} counter typeid lookups for game-mode achievements. */
final class AchievementCounterTypeids {

    private AchievementCounterTypeids() {}

    static void queueInitGameCounters(
            GameRoom room, long uid, int characterTypeid, int caddieTypeid, int mascotTypeid) {
        room.addPendingAchievementCounter(uid, GamePackets.TYPEID_NORMAL_GAME_COUNTER, 1);
        if (GamePackets.usesVersusInitialData(room.tipo)) {
            room.addPendingAchievementCounter(uid, GamePackets.TYPEID_VERSUS_GAME_COUNTER, 1);
        }
        if (room.info.master == uid) {
            room.addPendingAchievementCounter(uid, GamePackets.TYPEID_ROOM_MASTER_COUNTER, 1);
        }
        int charCounter = characterCounter(characterTypeid);
        if (charCounter != 0) {
            room.addPendingAchievementCounter(uid, charCounter, 1);
        }
        int caddieCounter = caddieCounter(caddieTypeid);
        if (caddieCounter != 0) {
            room.addPendingAchievementCounter(uid, caddieCounter, 1);
        }
        int mascotCounter = mascotCounter(mascotTypeid);
        if (mascotCounter != 0) {
            room.addPendingAchievementCounter(uid, mascotCounter, 1);
        }
        int courseCounter = courseCounter(room.info.course & 0x7f);
        if (courseCounter != 0) {
            room.addPendingAchievementCounter(uid, courseCounter, 1);
        }
        int holesCounter = qntdHoleCounter(room.info.holes);
        if (holesCounter != 0) {
            room.addPendingAchievementCounter(uid, holesCounter, 1);
        }
    }

    /** C# {@code GameBase.records_player_achievement} numeric counters from {@code UserInfoEx}. */
    static void queueRecordCounters(
            GameRoom room, long uid, GamePackets.UserInfoEx ui, long gamePang, int score) {
        if (ui.ob() > 0) {
            room.addPendingAchievementCounter(uid, GamePackets.TYPEID_OB_COUNTER, ui.ob());
        }
        if (ui.bunker() > 0) {
            room.addPendingAchievementCounter(uid, GamePackets.TYPEID_BUNKER_COUNTER, ui.bunker());
        }
        int shots = ui.tacada() + ui.putt();
        if (shots > 0) {
            room.addPendingAchievementCounter(uid, GamePackets.TYPEID_SHOTS_COUNTER, shots);
        }
        if (ui.hole() > 0) {
            room.addPendingAchievementCounter(uid, GamePackets.TYPEID_HOLE_COUNT_COUNTER, ui.hole());
        }
        if (ui.totalDistancia() > 0) {
            room.addPendingAchievementCounter(uid, GamePackets.TYPEID_YARDS_COUNTER, ui.totalDistancia());
        }
        if (ui.bestDrive() >= 1.0f) {
            room.addPendingAchievementCounter(uid, GamePackets.TYPEID_BEST_DRIVE_COUNTER, (int) ui.bestDrive());
        }
        if (ui.bestChipIn() >= 1.0f) {
            room.addPendingAchievementCounter(uid, GamePackets.TYPEID_BEST_CHIP_IN_COUNTER, (int) ui.bestChipIn());
        }
        if (ui.bestLongPutt() >= 1.0f) {
            room.addPendingAchievementCounter(uid, GamePackets.TYPEID_BEST_LONG_PUTT_COUNTER, (int) ui.bestLongPutt());
        }
        if (ui.acertoPangya() > 0) {
            room.addPendingAchievementCounter(uid, GamePackets.TYPEID_ACERTO_PANGYA_COUNTER, ui.acertoPangya());
        }
        if (gamePang > 0) {
            long capped = Math.min(gamePang, Integer.MAX_VALUE);
            room.addPendingAchievementCounter(uid, GamePackets.TYPEID_PANG_GAME_COUNTER, (int) capped);
        }
        if (score != 0) {
            room.addPendingAchievementCounter(uid, GamePackets.TYPEID_SCORE_COUNTER, score);
        }
        if (ui.combo() > 0) {
            room.addPendingAchievementCounter(uid, GamePackets.TYPEID_GAME_COMBO_COUNTER, ui.combo());
        }
    }

    /** C# {@code AchievementSystem.getScoreCounterTypeId} at {@code requestFinishHole}. */
    static int holeScoreCounter(int tacada, int par) {
        if (tacada == 1) {
            return GamePackets.TYPEID_HIO_HOLE_COUNTER;
        }
        return switch (tacada - par) {
            case -3 -> GamePackets.TYPEID_ALBA_HOLE_COUNTER;
            case -2 -> GamePackets.TYPEID_EAGLE_HOLE_COUNTER;
            case -1 -> GamePackets.TYPEID_BIRDIE_HOLE_COUNTER;
            case 0 -> GamePackets.TYPEID_PAR_HOLE_COUNTER;
            default -> 0;
        };
    }

    /**
     * C# {@code GameBase.score_consecutivos_count}: two or more consecutive holes with the
     * same score class increment the matching counter when the streak ends.
     */
    static void queueScoreConsecutivosCounters(
            GameRoom room, long uid, int[] holeTacada, int[] holePar, int holes) {
        int lastScore = -2;
        int count = 0;
        for (int i = 0; i < holes; i++) {
            int score = scoreNum(holeTacada[i], holePar[i]);
            if ((score != lastScore || i == holes - 1) && lastScore != -2) {
                if (count >= 1 && lastScore >= 0) {
                    int typeid = consecutiveScoreCounter(lastScore);
                    if (typeid != 0) {
                        room.addPendingAchievementCounter(uid, typeid, 1);
                    }
                }
                count = 0;
            } else if (score == lastScore) {
                count++;
            }
            lastScore = score;
        }
    }

    /** C# {@code AchievementSystem.getScoreNum}: 0 HIO … 6 double bogey, -1 ignored. */
    static int scoreNum(int tacada, int par) {
        if (tacada == 1) {
            return 0;
        }
        return switch (tacada - par) {
            case -3 -> 1;
            case -2 -> 2;
            case -1 -> 3;
            case 0 -> 4;
            case 1 -> 5;
            case 2 -> 6;
            default -> -1;
        };
    }

    static int consecutiveScoreCounter(int scoreNum) {
        return switch (scoreNum) {
            case 0 -> GamePackets.TYPEID_CONSEC_HIO_COUNTER;
            case 1 -> GamePackets.TYPEID_CONSEC_ALBA_COUNTER;
            case 2 -> GamePackets.TYPEID_CONSEC_EAGLE_COUNTER;
            case 3 -> GamePackets.TYPEID_CONSEC_BIRDIE_COUNTER;
            case 4 -> GamePackets.TYPEID_CONSEC_PAR_COUNTER;
            case 5 -> GamePackets.TYPEID_CONSEC_BOGEY_COUNTER;
            case 6 -> GamePackets.TYPEID_CONSEC_DOUBLE_BOGEY_COUNTER;
            default -> 0;
        };
    }

    static int characterCounter(int typeid) {
        return switch (typeid) {
            case 0x4000000 -> 0x6C40000F; // Nuri
            case 0x4000001 -> 0x6C400010; // Hana
            case 0x4000002 -> 0x6C400011; // Azer
            case 0x4000003 -> 0x6C400012; // Cecilia
            case 0x4000004 -> 0x6C400013; // Max
            case 0x4000005 -> 0x6C400014; // Kooh
            case 0x4000006 -> 0x6C400015; // Arin
            case 0x4000007 -> 0x6C400016; // Kaz
            case 0x4000008 -> 0x6C400017; // Lucia
            case 0x4000009 -> 0x6C400018; // Nell
            case 0x400000A -> 0x6C400040; // Spika
            default -> 0;
        };
    }

    static int caddieCounter(int typeid) {
        return switch (typeid) {
            case 0x1C000000 -> 0x6C400019; // Ancient Papel
            case 0x1C000001 -> 0x6C40001C; // Ancient Pippin
            case 0x1C000002 -> 0x6C40001B; // Ancient Titan Boo
            case 0x1C000003 -> 0x6C400042; // Ancient Dolfini
            default -> 0;
        };
    }

    static int mascotCounter(int typeid) {
        return switch (typeid) {
            case 0x40000000 -> 0x6C400049; // Lemmy
            case 0x40000001 -> 0x6C400048; // Puff
            case 0x40000002 -> 0x6C400047; // Cocoa
            case 0x40000003 -> 0x6C400046; // Billy
            default -> 0;
        };
    }

    static int courseCounter(int courseId) {
        return switch (courseId) {
            case 0 -> 0x6C400020; // Blue Lagoon
            case 1 -> 0x6C400021;
            case 2 -> 0x6C400022;
            case 3 -> 0x6C400023;
            case 4 -> 0x6C400024;
            case 5 -> 0x6C400025;
            case 6 -> 0x6C400026;
            case 7 -> 0x6C400027;
            case 8 -> 0x6C400028;
            case 9 -> 0x6C400029;
            case 10 -> 0x6C40002A;
            case 11 -> 0x6C40002B;
            case 13 -> 0x6C40002C;
            case 14 -> 0x6C40002D;
            case 15 -> 0x6C40002E;
            case 16 -> 0x6C40002F;
            case 17 -> 0x6C40006D;
            case 18 -> 0x6C400031;
            case 19 -> 0x6C400030;
            case 20 -> 0x6C4000A1;
            case 21 -> 0x6C4000C5;
            default -> 0;
        };
    }

    static int qntdHoleCounter(int holes) {
        return switch (holes) {
            case 3 -> 0x6C400069;
            case 6 -> 0x6C40006A;
            case 9 -> 0x6C40006B;
            case 18 -> 0x6C40006C;
            default -> 0;
        };
    }
}
