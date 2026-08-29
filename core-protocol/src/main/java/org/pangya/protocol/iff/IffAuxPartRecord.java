package org.pangya.protocol.iff;

/** Parsed subset of C# {@code AuxPart.iff} rate fields. */
public record IffAuxPartRecord(int typeid, int dropRate, int pangRate, int expRate) {}
