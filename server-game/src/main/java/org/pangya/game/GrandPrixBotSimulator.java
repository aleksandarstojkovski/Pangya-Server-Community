package org.pangya.game;

import org.pangya.game.util.Lottery;
import org.pangya.network.session.Session;
import org.pangya.protocol.game.GamePackets;
import org.pangya.protocol.iff.IffCourseRecord;
import org.pangya.protocol.iff.IffGrandPrixDataRecord;
import org.pangya.protocol.iff.PangyaIffLoader;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * C# {@code GrandPrix.init_bots}: simulate AI opponents for GP init {@code 0x256}.
 */
public final class GrandPrixBotSimulator {

    private static final int DEFAULT_MIN_SCORE = -2;

    private GrandPrixBotSimulator() {}

    public static List<GamePackets.GrandPrixBot> simulate(GameRoom room, GameCourse course) {
        if (room == null || course == null || room.grandPrixTypeid == 0) {
            return List.of();
        }
        Optional<IffGrandPrixDataRecord> gpOpt = PangyaIffLoader.grandPrixData(room.grandPrixTypeid);
        if (gpOpt.isEmpty()) {
            return List.of();
        }
        IffGrandPrixDataRecord gp = gpOpt.get();
        int qntdBots = 30 - room.players.size();
        if (qntdBots <= 0) {
            return List.of();
        }
        int qntdHoles = room.info.holes & 0xff;
        if (qntdHoles <= 0) {
            qntdHoles = gp.holes() > 0 ? gp.holes() : 18;
        }
        float roomAvgScore = averageRoomScore(room.snapshot());
        BotScores botScores = botScoresByRoomAvg(gp, roomAvgScore, qntdHoles);
        Random rnd = new Random(room.info.numero * 31L + room.grandPrixTypeid);
        Lottery lottery = new Lottery();
        for (IffGrandPrixDataRecord row : PangyaIffLoader.grandPrixDataIndex().all()) {
            lottery.push(1000, row.typeid());
        }
        List<GamePackets.GrandPrixBot> bots = new ArrayList<>();
        long rest = lottery.countItems();
        for (int i = 0; i < qntdBots; i++) {
            Lottery.Entry<Integer> draw = lottery.spinRoleta(true);
            if (draw == null || draw.value() == null) {
                break;
            }
            GamePackets.GrandPrixBot bot = buildBot(
                    draw.value(),
                    botScores,
                    course,
                    qntdHoles,
                    room.info.modo,
                    rnd);
            bots.add(bot);
            if (--rest == 0) {
                break;
            }
        }
        bots.sort(Comparator
                .comparingInt((GamePackets.GrandPrixBot b) -> b.record())
                .thenComparingLong(GamePackets.GrandPrixBot::pangTotal));
        return bots;
    }

    private static GamePackets.GrandPrixBot buildBot(
            int botTypeid,
            BotScores botScores,
            GameCourse course,
            int qntdHoles,
            int modo,
            Random rnd) {
        ScoreType typeScore = pickScoreType(rnd);
        int maxRecord = maxRecord(typeScore, botScores, rnd);
        int record = 0;
        long pangTotal = 0;
        long bonusPangTotal = 0;
        List<GamePackets.GrandPrixBotHole> holes = new ArrayList<>();
        float mediaAllParHole = course.mediaAllParHolesBySeq(qntdHoles);
        for (int j = 0; j < qntdHoles; j++) {
            GamePackets.HoleInfo holeInfo = course.holeBySeq(j + 1);
            if (holeInfo == null) {
                continue;
            }
            HolePar holePar = holePar(holeInfo);
            int holesLeft = qntdHoles - j;
            int medShot = Math.round(
                    (holesLeft * mediaAllParHole + (maxRecord - record)) / (float) holesLeft);
            int score = scoreForHole(holeInfo, holePar, medShot, typeScore, rnd);
            long pang = rnd.nextInt(351) * (holeInfo.weather() == 2 ? 2L : 1L);
            long bonusPang = rnd.nextInt(200);
            holes.add(new GamePackets.GrandPrixBotHole(
                    holeInfo.course() & 0xff,
                    holeInfo.numero() & 0xff,
                    score,
                    pang,
                    bonusPang));
            record += score;
            pangTotal += pang;
            bonusPangTotal += bonusPang;
        }
        return new GamePackets.GrandPrixBot(
                botTypeid & 0xffffffffL,
                (byte) qntdHoles,
                record,
                maxRecord,
                pangTotal,
                bonusPangTotal,
                List.copyOf(holes));
    }

    private static int scoreForHole(
            GamePackets.HoleInfo holeInfo,
            HolePar holePar,
            int medShot,
            ScoreType typeScore,
            Random rnd) {
        int minShot = holePar.par + holePar.rangeMin;
        int maxShot = holePar.par + holePar.rangeMax;
        if (minShot >= medShot) {
            return minShot - holePar.par;
        }
        if (medShot >= maxShot) {
            return maxShot - holePar.par;
        }
        int diffMin = medShot - minShot;
        int diffMax = maxShot - medShot;
        Lottery lottery = new Lottery();
        if (medShot < holePar.par) {
            lottery.push(1000 * diffMax * windFactor(holeInfo, ScoreType.MAX, typeScore), 1);
        }
        lottery.push(1000 * medShot * windFactor(holeInfo, ScoreType.MED, typeScore), 2);
        lottery.push(1000 * diffMin * windFactor(holeInfo, ScoreType.MIN, typeScore), 3);
        Lottery.Entry<Integer> draw = lottery.spinRoleta(true);
        if (draw == null || draw.value() == null) {
            return medShot - holePar.par;
        }
        return switch (draw.value()) {
            case 1 -> (medShot - rnd.nextInt(Math.max(1, diffMin))) - holePar.par;
            case 2 -> medShot - holePar.par;
            default -> (medShot + rnd.nextInt(Math.max(1, diffMax))) - holePar.par;
        };
    }

    private static int windFactor(
            GamePackets.HoleInfo holeInfo,
            ScoreType bucket,
            ScoreType botType) {
        int wind = holeInfo.wind() & 0xff;
        int factor = 1;
        if (wind >= 0 && wind < 3 && bucket == ScoreType.MAX) {
            factor = 2;
        } else if (wind >= 3 && wind < 6 && bucket == ScoreType.MED) {
            factor = 4;
        } else if (wind >= 6 && wind < 8 && bucket == ScoreType.MIN) {
            factor = 6;
        } else if (wind >= 8 && bucket == ScoreType.MIN) {
            factor = 7;
        }
        if (holeInfo.weather() == 2 && (bucket == ScoreType.MED || bucket == ScoreType.MIN)) {
            factor += 2;
        }
        if (bucket == botType) {
            factor += 2;
        }
        return factor;
    }

    private static HolePar holePar(GamePackets.HoleInfo holeInfo) {
        int courseId = holeInfo.course() & 0x7f;
        int holeNum = holeInfo.numero();
        Optional<IffCourseRecord> course = PangyaIffLoader.courses()
                .orElse(List.of())
                .stream()
                .filter(c -> c.courseId() == courseId)
                .findFirst();
        if (course.isEmpty() || holeNum < 1 || holeNum > 18) {
            return new HolePar(4, DEFAULT_MIN_SCORE, 5);
        }
        int par = course.get().parByHole()[holeNum - 1];
        int rangeMax = course.get().maxScoreByHole()[holeNum - 1];
        return new HolePar(par, DEFAULT_MIN_SCORE, rangeMax);
    }

    private static float averageRoomScore(List<Session> players) {
        if (players.isEmpty()) {
            return 0f;
        }
        // Room avg is applied when live UserInfo is wired; default keeps C# bot scaling stable.
        return 0f;
    }

    private static BotScores botScoresByRoomAvg(
            IffGrandPrixDataRecord gp,
            float roomAvgScore,
            int qntdHoles) {
        float mediaBot = ((gp.scoreBotMin() + gp.scoreBotMed() + gp.scoreBotMax()) / 3.0f) * 1.7f;
        float mediaScorePorHole = ((18.0f / qntdHoles) * mediaBot + roomAvgScore) / 180.0f;
        return new BotScores(
                bySign(gp.scoreBotMin(), mediaScorePorHole),
                bySign(gp.scoreBotMed(), mediaScorePorHole),
                bySign(gp.scoreBotMax(), mediaScorePorHole));
    }

    private static int bySign(int score, float mediaScorePorHole) {
        if (mediaScorePorHole <= 0.001f) {
            return score >= 0 ? 1 : -1;
        }
        if (score < 0) {
            return (int) Math.round(score * mediaScorePorHole);
        }
        return Math.max(1, (int) Math.round(score / mediaScorePorHole));
    }

    private static ScoreType pickScoreType(Random rnd) {
        if (rnd.nextInt(5) == 0) {
            return ScoreType.MAX;
        }
        if (rnd.nextInt(3) == 0) {
            return ScoreType.MED;
        }
        return ScoreType.MIN;
    }

    private static int maxRecord(ScoreType typeScore, BotScores botScores, Random rnd) {
        return switch (typeScore) {
            case MAX -> botScores.max + rnd.nextInt(3);
            case MED -> botScores.med + rnd.nextInt(6) - 3;
            default -> botScores.min + rnd.nextInt(5) - 3;
        };
    }

    private record BotScores(int min, int med, int max) {}

    private record HolePar(int par, int rangeMin, int rangeMax) {}

    private enum ScoreType { MIN, MED, MAX }
}
