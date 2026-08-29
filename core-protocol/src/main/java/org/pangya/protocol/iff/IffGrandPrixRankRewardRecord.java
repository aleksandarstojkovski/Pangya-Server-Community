package org.pangya.protocol.iff;

/** Parsed row from C# {@code GrandPrixRankReward.iff}. */
public record IffGrandPrixRankRewardRecord(
        int typeIdLink,
        int rank,
        IffRewardSlots reward,
        int trophy) {}
