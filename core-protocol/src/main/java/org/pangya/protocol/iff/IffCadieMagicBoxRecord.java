package org.pangya.protocol.iff;

/** Parsed subset of C# {@code CadieMagicBox} used by {@code requestCadieCauldronExchange}. */
public record IffCadieMagicBoxRecord(
        int seq,
        boolean active,
        int level,
        int receiveTypeid,
        int receiveQntd,
        int[] tradeTypeids,
        int[] tradeQntds,
        int boxRandomId) {

    public static final int TRADE_SLOTS = 4;

    public IffCadieMagicBoxRecord {
        if (tradeTypeids == null || tradeTypeids.length != TRADE_SLOTS) {
            throw new IllegalArgumentException("tradeTypeids must be length " + TRADE_SLOTS);
        }
        if (tradeQntds == null || tradeQntds.length != TRADE_SLOTS) {
            throw new IllegalArgumentException("tradeQntds must be length " + TRADE_SLOTS);
        }
        tradeTypeids = tradeTypeids.clone();
        tradeQntds = tradeQntds.clone();
    }
}
