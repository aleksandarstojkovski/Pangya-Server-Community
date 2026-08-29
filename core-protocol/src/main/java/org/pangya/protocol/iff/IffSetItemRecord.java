package org.pangya.protocol.iff;

/** Parsed subset of C# {@code SetItem} used by shop/gift expansion and {@code ownerSetItem}. */
public record IffSetItemRecord(int typeid, IffSetItemPackage packege, int point) {

    /** C# {@code TypeSet => (ID & ~0xFC000000) >> 21}. */
    public int typeSet() {
        return (typeid & ~0xFC000000) >>> 21;
    }
}
