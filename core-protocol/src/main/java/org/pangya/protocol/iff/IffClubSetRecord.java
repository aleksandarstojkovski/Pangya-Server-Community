package org.pangya.protocol.iff;

/** Parsed subset of C# {@code ClubSet} used by workshop/stats paths. */
public record IffClubSetRecord(
        int typeid,
        short[] stats,
        short[] slots,
        int workShopTipo,
        float workShopRate,
        int tipoRankS,
        int totalRecovery,
        int flagTransformar,
        int textPangya) {

    public static final int STAT_BYTES = 5;

    public IffClubSetRecord {
        if (stats.length != STAT_BYTES || slots.length != STAT_BYTES) {
            throw new IllegalArgumentException("stats/slots must have " + STAT_BYTES + " elements");
        }
    }
}
