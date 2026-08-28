package org.pangya.protocol.iff;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * C# {@code IFFHandle} / {@code data/pangya_jp.iff}: ZIP archive of embedded
 * {@code *.iff} datasets (Course.iff, Part.iff, …).
 */
public final class PangyaIffArchive {

    private final Path path;

    public PangyaIffArchive(Path path) {
        this.path = path;
    }

    public Path path() {
        return path;
    }

    public boolean exists() {
        return Files.isRegularFile(path);
    }

    /** Reads one member by exact name (case-sensitive, e.g. {@code Course.iff}). */
    public byte[] readEntry(String entryName) throws IOException {
        try (InputStream in = Files.newInputStream(path);
             ZipInputStream zip = new ZipInputStream(in)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entryName.equals(entry.getName())) {
                    return zip.readAllBytes();
                }
            }
        }
        throw new IOException("entry not found in " + path + ": " + entryName);
    }
}
