package org.pangya.game;

import org.pangya.protocol.game.GamePackets;

import java.util.List;

/** C# {@code CourseManager.init_dados_rain}: rain-hole and consecutive-rain counters. */
final class CourseRainStats {

    private final byte[] holesRain = new byte[18];
    private final byte[] rain2Consec = new byte[18];
    private final byte[] rain3Consec = new byte[18];
    private final byte[] rain4PlusConsec = new byte[18];

    static CourseRainStats build(List<GamePackets.HoleInfo> holes, int qntdHole) {
        CourseRainStats stats = new CourseRainStats();
        int count = 0;
        for (int numero = 1; numero <= qntdHole; numero++) {
            GamePackets.HoleInfo hole = find(holes, numero);
            int weather = hole == null ? 0 : hole.weather();
            if (weather == GamePackets.WEATHER_RAIN) {
                stats.holesRain[numero - 1] = 1;
                count++;
            }
            if (count > 1 && (weather != GamePackets.WEATHER_RAIN || numero == qntdHole)) {
                if (count >= 4) {
                    stats.rain4PlusConsec[numero - 1] = 1;
                } else if (count == 3) {
                    stats.rain3Consec[numero - 1] = 1;
                } else {
                    stats.rain2Consec[numero - 1] = 1;
                }
                count = 0;
            }
        }
        return stats;
    }

    int countHolesRainBySeq(int seq) {
        return prefixSum(holesRain, seq);
    }

    int countRain2ConsecBySeq(int seq) {
        return prefixSum(rain2Consec, seq);
    }

    int countRain3ConsecBySeq(int seq) {
        return prefixSum(rain3Consec, seq);
    }

    int countRain4PlusConsecBySeq(int seq) {
        return prefixSum(rain4PlusConsec, seq);
    }

    boolean hasRainHoles() {
        return prefixSum(holesRain, 18) > 0;
    }

    private static int prefixSum(byte[] values, int seq) {
        if (seq < 1 || seq > 18) {
            return 0;
        }
        int sum = 0;
        for (int i = 0; i < seq; i++) {
            sum += values[i];
        }
        return sum;
    }

    private static GamePackets.HoleInfo find(List<GamePackets.HoleInfo> holes, int numero) {
        for (GamePackets.HoleInfo hole : holes) {
            if (hole.numero() == numero) {
                return hole;
            }
        }
        return null;
    }
}
