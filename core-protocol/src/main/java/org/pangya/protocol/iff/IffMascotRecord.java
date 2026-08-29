package org.pangya.protocol.iff;

/** Parsed subset of C# {@code Mascot.msg} and {@code Mascot.efeito.drop_rate}. */
public record IffMascotRecord(int typeid, boolean messageActive, int changePrice, int dropRate) {}
