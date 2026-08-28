package org.pangya.protocol.iff;

/** C# {@code IFFHandle} item group helpers. */
public final class IffGroups {

    private IffGroups() {}

    public static int groupIdentify(int typeid) {
        return (typeid >>> 26) & 0x3f;
    }

    /** C# {@code getItemSubGroupIdentify24}. */
    public static int subGroupIdentify24(int typeid) {
        return (typeid & 0x03FFFFFF) >>> 24;
    }

    /** C# {@code IsItemEquipable}: item exists and {@code (subGroup24 >> 1) == 0}. */
    public static boolean isItemEquipable(int typeid) {
        return (subGroupIdentify24(typeid) >>> 1) == 0;
    }
}
