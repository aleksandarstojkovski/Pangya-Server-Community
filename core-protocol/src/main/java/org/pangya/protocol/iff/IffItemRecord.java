package org.pangya.protocol.iff;

/** Parsed subset of C# {@code Item} used by delete/gift checks. */
public record IffItemRecord(int typeid, int itemType, IffShopFlags shopFlags, int statsPower) {

    /** C# {@code Item.ItemType}: artefact mana entries use {@code 4}. */
    public boolean isManaArtefact() {
        return itemType == 4;
    }

    /** C# {@code Channel.requestDeleteActiveItem} IFF gate. */
    public boolean canDeleteActiveItem() {
        if (IffGroups.isItemEquipable(typeid)) {
            return !shopFlags.isCash();
        }
        return shopFlags.isGift() && statsPower > 0;
    }
}
