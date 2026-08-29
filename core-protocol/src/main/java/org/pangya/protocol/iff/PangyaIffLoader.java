package org.pangya.protocol.iff;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

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
            IffItemIndex items,
            IffTypeIndex cards,
            Map<Integer, IffCharacterRecord> characters,
            IffCharacterMasteryIndex characterMastery,
            IffEnchantIndex enchants,
            IffClubSetIndex clubSets,
            Path source) {
        static Snapshot empty() {
            return new Snapshot(
                    List.of(),
                    IffPartIndex.empty(),
                    IffItemIndex.empty(),
                    IffTypeIndex.empty(),
                    Map.of(),
                    IffCharacterMasteryIndex.empty(),
                    IffEnchantIndex.empty(),
                    IffClubSetIndex.empty(),
                    null);
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
            IffItemIndex items = IffItemFile.loadIndex(archive);
            IffTypeIndex cards = IffCardFile.loadIndex(archive);
            Map<Integer, IffCharacterRecord> characters = IffCharacterFile.loadIndex(archive);
            IffCharacterMasteryIndex characterMastery = IffCharacterMasteryFile.loadIndex(archive);
            IffEnchantIndex enchants = IffEnchantFile.loadIndex(archive);
            IffClubSetIndex clubSets = IffClubSetFile.loadIndex(archive);
            snapshot = new Snapshot(
                    courses, parts, items, cards, characters, characterMastery, enchants, clubSets, path);
            log.info(
                    "loaded pangya iff {} ({} courses, {} parts, {} items, {} cards, {} chars, {} mastery, {} enchants, {} clubsets)",
                    path,
                    courses.size(),
                    parts.size(),
                    items.size(),
                    cards.size(),
                    characters.size(),
                    characterMastery.rowCount(),
                    enchants.size(),
                    clubSets.size());
        } catch (Exception e) {
            log.warn("failed to load pangya iff {}: {}", path, e.toString());
            snapshot = Snapshot.empty();
        }
    }

    public static IffPartIndex partIndex() {
        return snapshot.parts();
    }

    public static IffItemIndex itemIndex() {
        return snapshot.items();
    }

    /**
     * C# {@code requestDeleteActiveItem} IFF gate. Empty when {@code pangya_jp.iff}
     * is not loaded (SQL-only stand-in skips shop-flag enforcement).
     */
    public static Optional<Boolean> canDeleteActiveItem(int typeid) {
        IffItemIndex items = snapshot.items();
        if (items.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(items.canDeleteActiveItem(typeid));
    }

    public static IffTypeIndex cardIndex() {
        return snapshot.cards();
    }

    public static Optional<List<IffCourseRecord>> courses() {
        return snapshot.courses().isEmpty() ? Optional.empty() : Optional.of(snapshot.courses());
    }

    public static Optional<IffCharacterRecord> character(int typeid) {
        return Optional.ofNullable(snapshot.characters().get(typeid));
    }

    /** C# {@code sIff.findCharacterMastery}. */
    public static Optional<List<IffCharacterMasteryRecord>> characterMastery(int typeid) {
        return snapshot.characterMastery().find(typeid);
    }

    /** C# {@code sIff.findEnchant}. */
    public static OptionalLong enchantPang(int typeid) {
        IffEnchantIndex enchants = snapshot.enchants();
        if (enchants.isEmpty()) {
            return OptionalLong.empty();
        }
        return enchants.pang(typeid);
    }

    /** C# {@code sIff.findClubSet}. */
    public static Optional<IffClubSetRecord> clubSet(int typeid) {
        IffClubSetIndex clubSets = snapshot.clubSets();
        if (clubSets.isEmpty()) {
            return Optional.empty();
        }
        return clubSets.find(typeid);
    }

    public static Optional<Path> source() {
        return Optional.ofNullable(snapshot.source());
    }
}
