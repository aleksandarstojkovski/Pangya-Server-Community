package org.pangya.game.catalog;

/** C# {@code CubeCoinSystem.getAllCoinCubeInHole} par-based limits. */
public final class CoinCubeInHole {

    private CoinCubeInHole() {}

    public record Limits(int maxCube, int maxCoinAndCube) {}

    /**
     * C# {@code CoinCubeInHole} from {@code MapSystem} course {@code range_score.par[hole-1]}.
     */
    public static Limits limitsForPar(int par) {
        return switch (par) {
            case 3 -> new Limits(1, 1);
            case 4 -> new Limits(1, 5);
            case 5 -> new Limits(2, 8);
            default -> new Limits(0, 0);
        };
    }
}
