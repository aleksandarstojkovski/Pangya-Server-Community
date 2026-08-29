package org.pangya.protocol.iff;

/** Parsed subset of C# {@code ClubSetWorkShopLevelUpLimit}. */
public record IffClubSetWorkShopLevelUpLimitRecord(int tipo, int rank, short[] c, int option) {

    public static final int STAT_BYTES = 5;

    public IffClubSetWorkShopLevelUpLimitRecord {
        if (c == null || c.length != STAT_BYTES) {
            throw new IllegalArgumentException("c must be length " + STAT_BYTES);
        }
        c = c.clone();
    }
}
