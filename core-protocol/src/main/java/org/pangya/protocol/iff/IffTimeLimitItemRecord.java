package org.pangya.protocol.iff;

/** Parsed subset of C# {@code TimeLimitItem}. */
public record IffTimeLimitItemRecord(int typeid, int tipo, int percent, int timeMinutes) {}
