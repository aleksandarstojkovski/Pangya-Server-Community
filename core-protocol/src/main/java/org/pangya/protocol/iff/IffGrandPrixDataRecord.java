package org.pangya.protocol.iff;

/** Parsed subset of C# {@code GrandPrixData} for room enter and course init. */
public record IffGrandPrixDataRecord(
        int typeid,
        int typeIdLink,
        int typeGp,
        int timeHole,
        String name,
        int rule,
        int course,
        int modo,
        int holes,
        boolean naturalMode,
        boolean shotMode,
        int minLevel,
        int maxLevel,
        int ticketTypeid,
        int ticketQntd,
        int conditionMin,
        int conditionMax,
        int clearGpTypeid,
        int lockYn,
        IffRewardSlots reward,
        IffSystemTime open,
        IffSystemTime start,
        int scoreBotMin,
        int scoreBotMed,
        int scoreBotMax) {}
