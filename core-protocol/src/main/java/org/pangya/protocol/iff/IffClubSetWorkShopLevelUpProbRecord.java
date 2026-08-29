package org.pangya.protocol.iff;

/** Parsed subset of C# {@code ClubSetWorkShopLevelUpProb}. */
public record IffClubSetWorkShopLevelUpProbRecord(int tipo, int[] c) {

    public static final int STAT_BYTES = 5;

    public IffClubSetWorkShopLevelUpProbRecord {
        if (c == null || c.length != STAT_BYTES) {
            throw new IllegalArgumentException("c must be length " + STAT_BYTES);
        }
        c = c.clone();
    }
}
