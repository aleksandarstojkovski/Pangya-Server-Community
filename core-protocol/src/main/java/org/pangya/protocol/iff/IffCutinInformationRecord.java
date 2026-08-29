package org.pangya.protocol.iff;

/** Parsed subset of C# {@code CutinInformation} used by {@code requestActiveCutin}. */
public record IffCutinInformationRecord(
        int typeid,
        int sector,
        int condition,
        int[] imageTypes,
        int tempo,
        String[] sprites) {

    public static final int IMG_COUNT = 4;
    public static final int SPRITE_BYTES = 40;

    public IffCutinInformationRecord {
        if (imageTypes == null || imageTypes.length != IMG_COUNT) {
            throw new IllegalArgumentException("imageTypes must be length " + IMG_COUNT);
        }
        if (sprites == null || sprites.length != IMG_COUNT) {
            throw new IllegalArgumentException("sprites must be length " + IMG_COUNT);
        }
        imageTypes = imageTypes.clone();
        sprites = sprites.clone();
    }
}
