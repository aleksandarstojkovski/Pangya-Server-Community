package org.pangya.protocol.iff;

/** C# {@code IFFFile<CaddieItem>} index ({@code Marshal.SizeOf(CaddieItem)} = 284 bytes). */
public final class IffCaddieItemFile {

    public static final int VERSION = 13;
    public static final int RECORD_BYTES = 284;

    private IffCaddieItemFile() {}

    public static IffTypeIndex loadIndex(byte[] data) {
        return IffCommonFile.loadTypeIndex(data, RECORD_BYTES, VERSION);
    }

    public static IffTypeIndex loadIndex(PangyaIffArchive archive) throws java.io.IOException {
        return loadIndex(archive.readEntry("CaddieItem.iff"));
    }
}
