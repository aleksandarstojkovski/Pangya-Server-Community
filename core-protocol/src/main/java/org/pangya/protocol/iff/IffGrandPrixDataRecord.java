package org.pangya.protocol.iff;

/** Parsed subset of C# {@code GrandPrixData} for room enter and course init. */
public record IffGrandPrixDataRecord(
        int typeid,
        String name,
        int rule,
        int course,
        int modo,
        int holes,
        boolean naturalMode,
        int minLevel,
        int maxLevel) {}
