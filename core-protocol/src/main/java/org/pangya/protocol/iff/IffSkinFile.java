package org.pangya.protocol.iff;

/** C# {@code IFFFile<Skin>} index ({@code Marshal.SizeOf(Skin)} = 244 bytes). */
public final class IffSkinFile {

    public static final int VERSION = 13;
    public static final int RECORD_BYTES = 244;

    private IffSkinFile() {}

    public static IffTypeIndex loadIndex(byte[] data) {
        return IffCommonFile.loadTypeIndex(data, RECORD_BYTES, VERSION);
    }

    public static IffTypeIndex loadIndex(PangyaIffArchive archive) throws java.io.IOException {
        return loadIndex(archive.readEntry("Skin.iff"));
    }
}
