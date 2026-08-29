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
            IffCaddieIndex caddies,
            IffMascotIndex mascots,
            IffClubSetWorkShopLevelUpLimitIndex clubSetWorkShopLevelUpLimits,
            IffClubSetWorkShopLevelUpProbIndex clubSetWorkShopLevelUpProbs,
            IffClubSetWorkShopRankUpExpIndex clubSetWorkShopRankExps,
            IffCutinInformationIndex cutins,
            IffTimeLimitItemIndex timeLimitItems,
            IffCadieMagicBoxIndex cadieMagicBoxes,
            IffCadieMagicBoxRandomIndex cadieMagicBoxRandoms,
            IffSetItemIndex setItems,
            IffGrandPrixDataIndex grandPrixData,
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
                    IffCaddieIndex.empty(),
                    IffMascotIndex.empty(),
                    IffClubSetWorkShopLevelUpLimitIndex.empty(),
                    IffClubSetWorkShopLevelUpProbIndex.empty(),
                    IffClubSetWorkShopRankUpExpIndex.empty(),
                    IffCutinInformationIndex.empty(),
                    IffTimeLimitItemIndex.empty(),
                    IffCadieMagicBoxIndex.empty(),
                    IffCadieMagicBoxRandomIndex.empty(),
                    IffSetItemIndex.empty(),
                    IffGrandPrixDataIndex.empty(),
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
            IffCaddieIndex caddies = IffCaddieFile.loadIndex(archive);
            IffMascotIndex mascots = IffMascotFile.loadIndex(archive);
            IffClubSetWorkShopLevelUpLimitIndex clubSetWorkShopLevelUpLimits =
                    IffClubSetWorkShopLevelUpLimitFile.loadIndex(archive);
            IffClubSetWorkShopLevelUpProbIndex clubSetWorkShopLevelUpProbs =
                    IffClubSetWorkShopLevelUpProbFile.loadIndex(archive);
            IffClubSetWorkShopRankUpExpIndex clubSetWorkShopRankExps =
                    IffClubSetWorkShopRankUpExpFile.loadIndex(archive);
            IffCutinInformationIndex cutins = IffCutinInformationFile.loadIndex(archive);
            IffTimeLimitItemIndex timeLimitItems = IffTimeLimitItemFile.loadIndex(archive);
            IffCadieMagicBoxIndex cadieMagicBoxes = IffCadieMagicBoxFile.loadIndex(archive);
            IffCadieMagicBoxRandomIndex cadieMagicBoxRandoms = IffCadieMagicBoxRandomFile.loadIndex(archive);
            IffSetItemIndex setItems = IffSetItemFile.loadIndex(archive);
            IffGrandPrixDataIndex grandPrixData = IffGrandPrixDataFile.loadIndex(archive);
            snapshot = new Snapshot(
                    courses, parts, items, cards, characters, characterMastery, enchants, clubSets, caddies,
                    mascots, clubSetWorkShopLevelUpLimits, clubSetWorkShopLevelUpProbs, clubSetWorkShopRankExps,
                    cutins, timeLimitItems, cadieMagicBoxes, cadieMagicBoxRandoms, setItems, grandPrixData, path);
            log.info(
                    "loaded pangya iff {} ({} courses, {} parts, {} items, {} cards, {} chars, {} mastery, {} enchants, {} clubsets, {} caddies, {} mascots, {} ws limits, {} ws probs, {} ws rank exp, {} cutins, {} time limit items, {} cadie boxes, {} cadie random rows, {} set items, {} grand prix)",
                    path,
                    courses.size(),
                    parts.size(),
                    items.size(),
                    cards.size(),
                    characters.size(),
                    characterMastery.rowCount(),
                    enchants.size(),
                    clubSets.size(),
                    caddies.size(),
                    mascots.size(),
                    clubSetWorkShopLevelUpLimits.rowCount(),
                    clubSetWorkShopLevelUpProbs.size(),
                    clubSetWorkShopRankExps.size(),
                    cutins.size(),
                    timeLimitItems.size(),
                    cadieMagicBoxes.size(),
                    cadieMagicBoxRandoms.rowCount(),
                    setItems.size(),
                    grandPrixData.size());
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

    /** C# {@code sIff.findClubSetOriginal}. */
    public static List<IffClubSetRecord> clubSetOriginals(int specialTypeid) {
        IffClubSetIndex clubSets = snapshot.clubSets();
        if (clubSets.isEmpty()) {
            return List.of();
        }
        return clubSets.findOriginals(specialTypeid);
    }

    /** C# {@code sIff.findCaddie}. */
    public static Optional<IffCaddieRecord> caddie(int typeid) {
        IffCaddieIndex caddies = snapshot.caddies();
        if (caddies.isEmpty()) {
            return Optional.empty();
        }
        return caddies.find(typeid);
    }

    /** C# {@code sIff.findMascot}. */
    public static Optional<IffMascotRecord> mascot(int typeid) {
        IffMascotIndex mascots = snapshot.mascots();
        if (mascots.isEmpty()) {
            return Optional.empty();
        }
        return mascots.find(typeid);
    }

    /** C# {@code sIff.findClubSetWorkShopLevelUpLimit}. */
    public static Optional<short[]> clubSetWorkShopLevelUpLimit(int tipo, int rank) {
        IffClubSetWorkShopLevelUpLimitIndex limits = snapshot.clubSetWorkShopLevelUpLimits();
        if (limits.isEmpty()) {
            return Optional.empty();
        }
        return limits.limit(tipo, rank);
    }

    public static boolean clubSetWorkShopLevelUpAny(int tipo) {
        IffClubSetWorkShopLevelUpLimitIndex limits = snapshot.clubSetWorkShopLevelUpLimits();
        if (limits.isEmpty()) {
            return false;
        }
        return limits.hasTipo(tipo);
    }

    /** C# {@code sIff.findClubSetWorkShopLevelUpProb}. */
    public static Optional<int[]> clubSetWorkShopLevelUpProb(int tipo) {
        IffClubSetWorkShopLevelUpProbIndex probs = snapshot.clubSetWorkShopLevelUpProbs();
        if (probs.isEmpty()) {
            return Optional.empty();
        }
        return probs.prob(tipo);
    }

    /** C# {@code sIff.findClubSetWorkShopRankExp}. */
    public static boolean clubSetWorkShopRankExp(int tipo) {
        IffClubSetWorkShopRankUpExpIndex rankExps = snapshot.clubSetWorkShopRankExps();
        if (rankExps.isEmpty()) {
            return false;
        }
        return rankExps.contains(tipo);
    }

    public static Optional<int[]> clubSetWorkShopRankExpRanks(int tipo) {
        IffClubSetWorkShopRankUpExpIndex rankExps = snapshot.clubSetWorkShopRankExps();
        if (rankExps.isEmpty()) {
            return Optional.empty();
        }
        return rankExps.ranks(tipo);
    }

    /** C# {@code sIff.findCutinInfomation}. */
    public static Optional<IffCutinInformationRecord> cutin(int typeid) {
        IffCutinInformationIndex cutins = snapshot.cutins();
        if (cutins.isEmpty()) {
            return Optional.empty();
        }
        return cutins.find(typeid);
    }

    /** C# {@code sIff.findTimeLimitItem}. */
    public static Optional<IffTimeLimitItemRecord> timeLimitItem(int typeid) {
        IffTimeLimitItemIndex items = snapshot.timeLimitItems();
        if (items.isEmpty()) {
            return Optional.empty();
        }
        return items.find(typeid);
    }

    /** C# {@code sIff.findCadieMagicBox}. */
    public static Optional<IffCadieMagicBoxRecord> cadieMagicBox(int seq) {
        IffCadieMagicBoxIndex boxes = snapshot.cadieMagicBoxes();
        if (boxes.isEmpty()) {
            return Optional.empty();
        }
        return boxes.find(seq);
    }

    /** C# CadieCauldron random pool spin for {@code Box_Random_ID}. */
    public static Optional<IffCadieMagicBoxRandomRecord> spinCadieMagicBoxRandom(int groupId) {
        IffCadieMagicBoxRandomIndex randoms = snapshot.cadieMagicBoxRandoms();
        if (randoms.isEmpty()) {
            return Optional.empty();
        }
        return randoms.spin(groupId);
    }

    /** C# {@code sIff.findSetItem}. */
    public static Optional<IffSetItemRecord> setItem(int typeid) {
        IffSetItemIndex setItems = snapshot.setItems();
        if (setItems.isEmpty()) {
            return Optional.empty();
        }
        return setItems.find(typeid);
    }

    /** C# {@code sIff.findGrandPrixData}. */
    public static Optional<IffGrandPrixDataRecord> grandPrixData(int typeid) {
        IffGrandPrixDataIndex grandPrix = snapshot.grandPrixData();
        if (grandPrix.isEmpty()) {
            return Optional.empty();
        }
        return grandPrix.find(typeid);
    }

    public static Optional<Path> source() {
        return Optional.ofNullable(snapshot.source());
    }
}
