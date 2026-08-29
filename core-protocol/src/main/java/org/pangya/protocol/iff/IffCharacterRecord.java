package org.pangya.protocol.iff;

/** One row from {@code Character.iff} (C# {@code Character.PCL} max stats). */
public record IffCharacterRecord(int typeid, int[] pclMax) {

    public static final int PCL_BYTES = 5;
    public static final int PCL_OFFSET = 372;

    public int pcl(int stat) {
        if (stat < 0 || stat >= pclMax.length) {
            return 0;
        }
        return pclMax[stat];
    }
}
