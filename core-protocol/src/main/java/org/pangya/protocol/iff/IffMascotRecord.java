package org.pangya.protocol.iff;

/** Parsed subset of C# {@code Mascot} rate fields and {@code Mascot.msg}. */
public record IffMascotRecord(
        int typeid, boolean messageActive, int changePrice, int dropRate, int pangRate, int expRate) {}
