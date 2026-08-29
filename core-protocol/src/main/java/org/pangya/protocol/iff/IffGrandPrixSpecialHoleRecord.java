package org.pangya.protocol.iff;

/** One row from C# {@code GrandPrixSpecialHole.iff}. */
public record IffGrandPrixSpecialHoleRecord(
        int enable, int typeid, int holePos, int map, int hole) {}
