package org.pangya.protocol.iff;

/** Parsed subset of C# {@code ClubSetWorkShopRankUpExp}. */
public record IffClubSetWorkShopRankUpExpRecord(int tipo, int[] rank) {

    public static final int RANK_BYTES = 6;

    public IffClubSetWorkShopRankUpExpRecord {
        if (rank == null || rank.length != RANK_BYTES) {
            throw new IllegalArgumentException("rank must be length " + RANK_BYTES);
        }
        rank = rank.clone();
    }
}
