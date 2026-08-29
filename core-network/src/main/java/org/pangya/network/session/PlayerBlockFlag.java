package org.pangya.network.session;

/**
 * C# {@code BlockFlag.m_flag} ({@code uFlag}) bits merged at game login and checked
 * on room enter (e.g. Grand Prix {@code requestEnterRoomGrandPrix}).
 */
public final class PlayerBlockFlag {

    /** C# {@code uFlag.all_game} bit 1 — blocks all game modes. */
    public static final int BIT_ALL_GAME = 1;
    /** C# {@code uFlag.grand_prix} bit 21 — blocks Grand Prix enter. */
    public static final int BIT_GRAND_PRIX = 21;

    private PlayerBlockFlag() {}

    public static boolean allGame(long flags) {
        return (flags & (1L << BIT_ALL_GAME)) != 0;
    }

    public static boolean grandPrix(long flags) {
        return (flags & (1L << BIT_GRAND_PRIX)) != 0;
    }

    /** C# {@code setIDState}: map account IDState bits into {@code uFlag}. */
    public static long fromIdState(long idState) {
        long flags = 0;
        if ((idState & 4L) != 0) {
            flags |= 1L << 13; // lounge
        }
        if ((idState & 8L) != 0) {
            flags |= 1L << 5; // personal_shop
        }
        if ((idState & 16L) != 0) {
            flags |= 1L << 3; // gift_shop
        }
        if ((idState & 32L) != 0) {
            flags |= 1L << 4; // papel_shop
        }
        if ((idState & 64L) != 0) {
            flags |= 1L << 18; // scratchy
        }
        if ((idState & 128L) != 0) {
            flags |= 1L << 17; // ticker
        }
        if ((idState & 256L) != 0) {
            flags |= 1L << 16; // memorial_shop
        }
        return flags;
    }
}
