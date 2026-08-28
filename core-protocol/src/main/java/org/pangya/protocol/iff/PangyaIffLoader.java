package org.pangya.protocol.iff;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * C# {@code sIff.reload()} snapshot: Course/Part/Item/Card indexes from {@code pangya_jp.iff}.
 */
public final class PangyaIffLoader {

    private static final Logger log = LoggerFactory.getLogger(PangyaIffLoader.class);

    private static volatile Snapshot snapshot = Snapshot.empty();

    private PangyaIffLoader() {}

    public record Snapshot(
            List<IffCourseRecord> courses,
            IffPartIndex parts,
            IffTypeIndex items,
            IffTypeIndex cards,
            Path source) {
        static Snapshot empty() {
            return new Snapshot(List.of(), IffPartIndex.empty(), IffTypeIndex.empty(), IffTypeIndex.empty(), null);
        }
    }

    public static synchronized void reload(Path path) {
        if (path == null || path.toString().isBlank()) {
            snapshot = Snapshot.empty();
            return;
        }
        if (!Files.isRegularFile(path)) {
            log.warn("PANGYA_IFF_PATH missing file: {}", path);
            snapshot = Snapshot.empty();
            return;
        }
        try {
            PangyaIffArchive archive = new PangyaIffArchive(path);
            List<IffCourseRecord> courses = IffCourseFile.load(archive);
            IffPartIndex parts = IffPartFile.loadIndex(archive);
            IffTypeIndex items = IffItemFile.loadIndex(archive);
            IffTypeIndex cards = IffCardFile.loadIndex(archive);
            snapshot = new Snapshot(courses, parts, items, cards, path);
            log.info(
                    "loaded pangya iff {} ({} courses, {} parts, {} items, {} cards)",
                    path,
                    courses.size(),
                    parts.size(),
                    items.size(),
                    cards.size());
        } catch (Exception e) {
            log.warn("failed to load pangya iff {}: {}", path, e.toString());
            snapshot = Snapshot.empty();
        }
    }

    public static IffPartIndex partIndex() {
        return snapshot.parts();
    }

    public static IffTypeIndex itemIndex() {
        return snapshot.items();
    }

    public static IffTypeIndex cardIndex() {
        return snapshot.cards();
    }

    public static Optional<List<IffCourseRecord>> courses() {
        return snapshot.courses().isEmpty() ? Optional.empty() : Optional.of(snapshot.courses());
    }

    public static Optional<Path> source() {
        return Optional.ofNullable(snapshot.source());
    }
}
