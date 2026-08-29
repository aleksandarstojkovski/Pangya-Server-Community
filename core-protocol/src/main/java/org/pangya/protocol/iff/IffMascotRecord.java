package org.pangya.protocol.iff;

/** Parsed subset of C# {@code Mascot.msg} used by message-change paths. */
public record IffMascotRecord(int typeid, boolean messageActive, int changePrice) {}
