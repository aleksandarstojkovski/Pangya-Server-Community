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

    /** C# {@code getAllCoinCubeInHoleWizCity} hole-index limits (course 19). */
    public static Limits limitsForWizCity(int holeNum) {
        return switch (holeNum) {
            case 3, 12 -> new Limits(5, 60);
            case 14 -> new Limits(2, 48);
            case 18 -> new Limits(3, 33);
            default -> new Limits(0, 20);
        };
    }
}
