package org.pangya.protocol.iff;

/** C# {@code Packege} embedded in {@code SetItem.iff} rows. */
public record IffSetItemPackage(int total, int[] itemTypeids, int[] itemQntds) {

    public static final int MAX_ITEMS = 10;

    public IffSetItemPackage {
        if (itemTypeids == null || itemTypeids.length != MAX_ITEMS) {
            throw new IllegalArgumentException("itemTypeids must be length " + MAX_ITEMS);
        }
        if (itemQntds == null || itemQntds.length != MAX_ITEMS) {
            throw new IllegalArgumentException("itemQntds must be length " + MAX_ITEMS);
        }
        itemTypeids = itemTypeids.clone();
        itemQntds = itemQntds.clone();
    }
}
