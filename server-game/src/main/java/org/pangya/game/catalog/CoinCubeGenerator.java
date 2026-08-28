package org.pangya.game.catalog;

import org.pangya.db.InventoryRepository;
import org.pangya.protocol.game.GamePackets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** C# {@code CubeCoinSystem.CourseCtx.Hole.getAllCoinCube} using SQL locations. */
public final class CoinCubeGenerator {

    /** C# {@code Cube.eTYPE.COIN}. */
    public static final int TIPO_COIN = 0;
    /** C# {@code Cube.eTYPE.CUBE}. */
    public static final int TIPO_CUBE = 1;

    /** C# {@code Cube.eFLAG_LOCATION.EDGE_GREEN}. */
    public static final int LOC_EDGE = 0;
    /** C# {@code Cube.eFLAG_LOCATION.CARPET} (Wiz City lottery pool). */
    public static final int LOC_CARPET = 1;
    /** C# {@code Cube.eFLAG_LOCATION.AIR}. */
    public static final int LOC_AIR = 2;
    /** C# {@code Cube.eFLAG_LOCATION.GROUND}. */
    public static final int LOC_GROUND = 3;

    /** C# {@code CourseManager.m_flag_cube_coin}. */
    public static final int FLAG_CUBE_COIN = 1;

    /** C# {@code RoomInfo.ROOM_INFO_COURSE.WIZ_CITY}. */
    public static final int COURSE_WIZ_CITY = 19;

    private CoinCubeGenerator() {}

    /**
     * Builds hole cube/coin list for {@code 0x52} {@code makePacketHoleSpinningCubeInfo}.
     *
     * @param seqIndex 0-based hole sequence index written as {@code el.Key - 1} on wire
     */
    public static List<GamePackets.CourseCubeEntry> generate(
            GlobalCatalogs catalogs,
            int courseId,
            int holeNum,
            int seqIndex,
            boolean enableCube,
            boolean enableCoin) {
        if (!catalogs.coinCubeCourseActive().getOrDefault((short) courseId, false)) {
            return List.of();
        }
        List<InventoryRepository.CoinCubeLocation> locations = new ArrayList<>();
        for (InventoryRepository.CoinCubeLocation loc : catalogs.coinCubeLocations((short) courseId)) {
            if (loc.hole() == holeNum) {
                locations.add(loc);
            }
        }
        if (locations.isEmpty()) {
            return List.of();
        }

        if ((courseId & 0x7f) == COURSE_WIZ_CITY) {
            return generateWizCity(locations, courseId, holeNum, seqIndex, enableCube);
        }

        CoinCubeInHole.Limits limits =
                CoinCubeInHole.limitsForPar(catalogs.parFor(courseId, holeNum));
        List<GamePackets.CourseCubeEntry> out = new ArrayList<>(limits.maxCoinAndCube());
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        // Edge coins are always present (C# standard path adds ground coins via lottery).
        for (InventoryRepository.CoinCubeLocation loc : locations) {
            if (loc.tipo() == TIPO_COIN && loc.tipoLocation() == LOC_EDGE) {
                out.add(toEntry(loc, courseId, holeNum, seqIndex));
            }
        }

        if (enableCube && limits.maxCube() > 0) {
            List<InventoryRepository.CoinCubeLocation> airCubes = locations.stream()
                    .filter(l -> l.tipo() == TIPO_CUBE && l.tipoLocation() == LOC_AIR)
                    .toList();
            int cubeCount = Math.min(limits.maxCube(), airCubes.size());
            out.addAll(lotteryPick(airCubes, cubeCount, rng, courseId, holeNum, seqIndex));
        }

        if (enableCoin && limits.maxCoinAndCube() > out.size()) {
            List<InventoryRepository.CoinCubeLocation> groundCoins = locations.stream()
                    .filter(l -> l.tipo() == TIPO_COIN && l.tipoLocation() == LOC_GROUND)
                    .toList();
            if (groundCoins.isEmpty()) {
                // V38 seed uses tipo_location 1 for ground stand-in until full JP coords exist.
                groundCoins = locations.stream()
                        .filter(l -> l.tipo() == TIPO_COIN && l.tipoLocation() == LOC_CARPET)
                        .toList();
            }
            int rest = limits.maxCoinAndCube() - out.size();
            out.addAll(lotteryPick(groundCoins, Math.min(rest, groundCoins.size()), rng, courseId, holeNum, seqIndex));
        }

        return List.copyOf(out);
    }

    private static List<GamePackets.CourseCubeEntry> generateWizCity(
            List<InventoryRepository.CoinCubeLocation> locations,
            int courseId,
            int holeNum,
            int seqIndex,
            boolean enableCube) {
        CoinCubeInHole.Limits limits = CoinCubeInHole.limitsForWizCity(holeNum);
        List<GamePackets.CourseCubeEntry> out = new ArrayList<>(limits.maxCoinAndCube());
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        List<InventoryRepository.CoinCubeLocation> carpetCoins = locations.stream()
                .filter(l -> l.tipo() == TIPO_COIN && l.tipoLocation() == LOC_CARPET)
                .toList();

        for (InventoryRepository.CoinCubeLocation loc : locations) {
            if (loc.tipoLocation() == LOC_EDGE) {
                out.add(toEntry(loc, courseId, holeNum, seqIndex));
            }
        }

        if (enableCube && limits.maxCube() > 0 && !carpetCoins.isEmpty()) {
            List<InventoryRepository.CoinCubeLocation> shuffled = new ArrayList<>(carpetCoins);
            Collections.shuffle(shuffled, rng);
            int cubeCount = Math.min(limits.maxCube(), shuffled.size());
            for (int i = 0; i < cubeCount; i++) {
                InventoryRepository.CoinCubeLocation src = shuffled.get(i);
                out.add(new GamePackets.CourseCubeEntry(
                        TIPO_CUBE,
                        (int) src.index(),
                        0,
                        courseId & 0x7f,
                        holeNum,
                        seqIndex,
                        FLAG_CUBE_COIN,
                        (float) src.x(),
                        (float) src.y(),
                        (float) src.z(),
                        src.tipoLocation()));
            }
        }

        if (limits.maxCoinAndCube() > out.size() && !carpetCoins.isEmpty()) {
            int rest = limits.maxCoinAndCube() - out.size();
            out.addAll(lotteryPick(carpetCoins, Math.min(rest, carpetCoins.size()), rng, courseId, holeNum, seqIndex));
        }

        return List.copyOf(out);
    }

    private static List<GamePackets.CourseCubeEntry> lotteryPick(
            List<InventoryRepository.CoinCubeLocation> pool,
            int count,
            ThreadLocalRandom rng,
            int courseId,
            int holeNum,
            int seqIndex) {
        if (pool.isEmpty() || count <= 0) {
            return List.of();
        }
        List<InventoryRepository.CoinCubeLocation> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, rng);
        List<GamePackets.CourseCubeEntry> picked = new ArrayList<>(count);
        for (int i = 0; i < count && i < shuffled.size(); i++) {
            picked.add(toEntry(shuffled.get(i), courseId, holeNum, seqIndex));
        }
        return picked;
    }

    private static GamePackets.CourseCubeEntry toEntry(
            InventoryRepository.CoinCubeLocation loc,
            int courseId,
            int holeNum,
            int seqIndex) {
        return new GamePackets.CourseCubeEntry(
                loc.tipo(),
                (int) loc.index(),
                0,
                courseId & 0x7f,
                holeNum,
                seqIndex,
                FLAG_CUBE_COIN,
                (float) loc.x(),
                (float) loc.y(),
                (float) loc.z(),
                loc.tipoLocation());
    }
}
