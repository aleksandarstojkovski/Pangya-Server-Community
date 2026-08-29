package org.pangya.protocol.iff;

/** Parsed subset of C# {@code Item} used by delete/gift checks. */
public record IffItemRecord(int typeid, IffShopFlags shopFlags, int statsPower) {

    /** C# {@code Channel.requestDeleteActiveItem} IFF gate. */
    public boolean canDeleteActiveItem() {
        if (IffGroups.isItemEquipable(typeid)) {
            return !shopFlags.isCash();
        }
        return shopFlags.isGift() && statsPower > 0;
    }
}
