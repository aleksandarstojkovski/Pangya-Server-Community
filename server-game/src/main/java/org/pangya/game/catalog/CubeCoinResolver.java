package org.pangya.game.catalog;

import org.pangya.db.InventoryRepository;
import org.pangya.protocol.game.GamePackets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/** C# {@code GameBase.requestInitCubeCoin} using SQL {@code pangya_coin_cube_location}. */
public final class CubeCoinResolver {

    private CubeCoinResolver() {}

    /**
     * Validates client picks against hole cube/coin ids. Any unknown id yields an
     * empty list (C# catch → empty {@code DropItemRet}).
     */
    public static List<GamePackets.DropItem> resolve(
            GlobalCatalogs catalogs,
            int courseId,
            int holeNum,
            GamePackets.ShotAckCubeCoin body) {
        if (body.opt() != 1 || body.picks().isEmpty()) {
            return List.of();
        }
        if (!catalogs.coinCubeCourseActive().getOrDefault((short) courseId, false)) {
            return List.of();
        }
        Map<Integer, InventoryRepository.CoinCubeLocation> byId =
                indexHole(catalogs, courseId, holeNum);
        List<GamePackets.DropItem> drops = new ArrayList<>(body.picks().size());
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (GamePackets.CubeCoinPick pick : body.picks()) {
            InventoryRepository.CoinCubeLocation loc = byId.get(pick.id());
            if (loc == null) {
                return List.of();
            }
            if (pick.tipo() == 0) {
                int max = loc.tipoLocation() == 0 ? 50 : 200;
                int qntd = 1 + rng.nextInt(max);
                long dropType = loc.tipoLocation() == 0
                        ? GamePackets.DROP_TYPE_COIN_EDGE
                        : GamePackets.DROP_TYPE_COIN_GROUND;
                drops.add(new GamePackets.DropItem(
                        GamePackets.TYPEID_COIN, courseId, holeNum, qntd, dropType));
            } else if (pick.tipo() == 1) {
                drops.add(new GamePackets.DropItem(
                        GamePackets.TYPEID_SPINNING_CUBE,
                        courseId,
                        holeNum,
                        1,
                        GamePackets.DROP_TYPE_CUBE));
            } else {
                return List.of();
            }
        }
        return drops;
    }

    private static Map<Integer, InventoryRepository.CoinCubeLocation> indexHole(
            GlobalCatalogs catalogs, int courseId, int holeNum) {
        Map<Integer, InventoryRepository.CoinCubeLocation> out = new HashMap<>();
        for (InventoryRepository.CoinCubeLocation loc : catalogs.coinCubeLocations((short) courseId)) {
            if (loc.hole() == holeNum) {
                out.put((int) loc.index(), loc);
            }
        }
        return out;
    }
}
