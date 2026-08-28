package org.pangya.game;

import org.pangya.game.catalog.CoinCubeGenerator;
import org.pangya.game.catalog.GlobalCatalogs;
import org.pangya.protocol.game.GamePackets;

import java.util.ArrayList;
import java.util.List;

/**
 * Synthetic C# {@code CourseManager} without IFF files: 18 holes, FRONT sequence 1..18,
 * pin {@code (n-1)%3}. Cube/coin rows come from SQL via {@link CoinCubeGenerator}.
 */
final class GameCourse {

    /** C# {@code rnd.Next(1, short.MaxValue)} range; 1 is a valid production seed. */
    static final int SEED = 1;

    final int seed;
    final List<GamePackets.HoleInfo> holes = new ArrayList<>(GamePackets.COURSE_HOLE_COUNT);
    final List<List<GamePackets.CourseCubeEntry>> cubesByHole = new ArrayList<>(GamePackets.COURSE_HOLE_COUNT);

    GameCourse(GamePackets.RoomInfo info, GlobalCatalogs catalogs) {
        this.seed = SEED;
        int course = info.course & 0x7f;
        boolean coinCubeActive =
                catalogs.coinCubeCourseActive().getOrDefault((short) course, false);
        for (int n = 1; n <= GamePackets.COURSE_HOLE_COUNT; n++) {
            holes.add(new GamePackets.HoleInfo(n, (n - 1) % 3, course, n, 0, 0, 0));
            boolean enableCube = coinCubeActive && n % 3 == 0;
            boolean enableCoin = coinCubeActive;
            cubesByHole.add(CoinCubeGenerator.generate(
                    catalogs, course, n, n - 1, enableCube, enableCoin));
        }
    }

    GamePackets.HoleInfo find(int numero) {
        for (GamePackets.HoleInfo hole : holes) {
            if (hole.numero() == numero) {
                return hole;
            }
        }
        return null;
    }

    /**
     * C# {@code hole.setWind} for GM versus wind: keep pin/weather/degree, replace wind.
     */
    boolean setWind(int numero, int wind) {
        for (int i = 0; i < holes.size(); i++) {
            GamePackets.HoleInfo hole = holes.get(i);
            if (hole.numero() == numero) {
                holes.set(i, new GamePackets.HoleInfo(
                        hole.id(), hole.pin(), hole.course(), hole.numero(),
                        hole.weather(), wind, hole.degree()));
                return true;
            }
        }
        return false;
    }

    /**
     * C# {@code hole.setWeather} for GM in-game weather: keep pin/wind/degree.
     */
    boolean setWeather(int numero, int weather) {
        for (int i = 0; i < holes.size(); i++) {
            GamePackets.HoleInfo hole = holes.get(i);
            if (hole.numero() == numero) {
                holes.set(i, new GamePackets.HoleInfo(
                        hole.id(), hole.pin(), hole.course(), hole.numero(),
                        weather, hole.wind(), hole.degree()));
                return true;
            }
        }
        return false;
    }
}
