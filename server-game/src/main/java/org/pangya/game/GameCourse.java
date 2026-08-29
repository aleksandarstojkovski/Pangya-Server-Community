package org.pangya.game;

import org.pangya.game.catalog.CoinCubeGenerator;
import org.pangya.game.catalog.GlobalCatalogs;
import org.pangya.protocol.game.GamePackets;
import org.pangya.protocol.iff.IffGrandPrixSpecialHoleRecord;
import org.pangya.protocol.iff.PangyaIffLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * C# {@code CourseManager} subset: hole sequence (incl. GP special holes), synthetic pin
 * {@code (n-1)%3}. Cube/coin rows come from SQL via {@link CoinCubeGenerator}.
 */
final class GameCourse {

    /** C# {@code rnd.Next(1, short.MaxValue)} range; 1 is a valid production seed. */
    static final int SEED = 1;

    final int seed;
    final List<GamePackets.HoleInfo> holes = new ArrayList<>(GamePackets.COURSE_HOLE_COUNT);
    final List<List<GamePackets.CourseCubeEntry>> cubesByHole = new ArrayList<>(GamePackets.COURSE_HOLE_COUNT);

    GameCourse(GamePackets.RoomInfo info, GlobalCatalogs catalogs) {
        this.seed = SEED;
        int roomCourse = info.course & 0x7f;
        boolean coinCubeActive =
                info.gpActive != 1
                        && catalogs.coinCubeCourseActive().getOrDefault((short) roomCourse, false);
        boolean isWizCity = roomCourse == CoinCubeGenerator.COURSE_WIZ_CITY;
        int modo = info.modo;
        List<int[]> sequence = holeSequence(info, roomCourse);
        for (int n = 1; n <= GamePackets.COURSE_HOLE_COUNT; n++) {
            int[] slot = sequence.get(n - 1);
            int holeCourse = slot[0];
            int holeNum = slot[1];
            holes.add(new GamePackets.HoleInfo(n, (n - 1) % 3, holeCourse, holeNum, 0, 0, 0));
            boolean enableCube = false;
            if (coinCubeActive) {
                if (isWizCity) {
                    enableCube = (n == 3 || n == 12 || n == 14 || n == 18)
                            && (modo != GamePackets.MODO_REPEAT || n % 3 == 0);
                } else {
                    enableCube = n % 3 == 0;
                }
            }
            boolean enableCoin = coinCubeActive;
            cubesByHole.add(CoinCubeGenerator.generate(
                    catalogs, holeCourse, holeNum, n - 1, enableCube, enableCoin));
        }
    }

    /** C# {@code CourseManager.init_seq} for GP special holes + FRONT fallback. */
    static List<int[]> holeSequence(GamePackets.RoomInfo info, int roomCourse) {
        List<int[]> seq = new ArrayList<>(GamePackets.COURSE_HOLE_COUNT);
        if (info.gpActive == 1 && info.gpRankTypeid > 0) {
            List<IffGrandPrixSpecialHoleRecord> special =
                    PangyaIffLoader.grandPrixSpecialHoles(info.gpRankTypeid);
            if (!special.isEmpty()) {
                for (IffGrandPrixSpecialHoleRecord row : special) {
                    seq.add(new int[] {row.map(), row.hole()});
                }
                for (int i = seq.size() + 1; i <= GamePackets.COURSE_HOLE_COUNT; i++) {
                    seq.add(new int[] {roomCourse, i});
                }
                return List.copyOf(seq);
            }
        }
        for (int i = 1; i <= GamePackets.COURSE_HOLE_COUNT; i++) {
            seq.add(new int[] {roomCourse, i});
        }
        return List.copyOf(seq);
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
