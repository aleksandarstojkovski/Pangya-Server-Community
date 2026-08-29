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
            IffBallIndex balls,
            IffTypeIndex cards,
            Map<Integer, IffCharacterRecord> characters,
            IffCharacterMasteryIndex characterMastery,
            IffEnchantIndex enchants,
            IffClubSetIndex clubSets,
            IffCaddieIndex caddies,
            IffMascotIndex mascots,
            IffAuxPartIndex auxParts,
            IffClubSetWorkShopLevelUpLimitIndex clubSetWorkShopLevelUpLimits,
            IffClubSetWorkShopLevelUpProbIndex clubSetWorkShopLevelUpProbs,
            IffClubSetWorkShopRankUpExpIndex clubSetWorkShopRankExps,
            IffCutinInformationIndex cutins,
            IffTimeLimitItemIndex timeLimitItems,
            IffCadieMagicBoxIndex cadieMagicBoxes,
            IffCadieMagicBoxRandomIndex cadieMagicBoxRandoms,
            IffSetItemIndex setItems,
            IffGrandPrixDataIndex grandPrixData,
            IffGrandPrixSpecialHoleIndex grandPrixSpecialHoles,
            IffGrandPrixConditionEquipIndex grandPrixConditionEquip,
            IffTypeIndex skins,
            IffTypeIndex caddieItems,
            Path source) {
        static Snapshot empty() {
            return new Snapshot(
                    List.of(),
                    IffPartIndex.empty(),
                    IffItemIndex.empty(),
                    IffBallIndex.empty(),
                    IffTypeIndex.empty(),
                    Map.of(),
                    IffCharacterMasteryIndex.empty(),
                    IffEnchantIndex.empty(),
                    IffClubSetIndex.empty(),
                    IffCaddieIndex.empty(),
                    IffMascotIndex.empty(),
                    IffAuxPartIndex.empty(),
                    IffClubSetWorkShopLevelUpLimitIndex.empty(),
                    IffClubSetWorkShopLevelUpProbIndex.empty(),
                    IffClubSetWorkShopRankUpExpIndex.empty(),
                    IffCutinInformationIndex.empty(),
                    IffTimeLimitItemIndex.empty(),
                    IffCadieMagicBoxIndex.empty(),
                    IffCadieMagicBoxRandomIndex.empty(),
                    IffSetItemIndex.empty(),
                    IffGrandPrixDataIndex.empty(),
                    IffGrandPrixSpecialHoleIndex.empty(),
                    IffGrandPrixConditionEquipIndex.empty(),
                    IffTypeIndex.empty(),
                    IffTypeIndex.empty(),
                    null);
        }
    }

    private record IffNameDataset(String entry, int recordBytes, int version) {}

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
            IffBallIndex balls = IffBallFile.loadIndex(archive);
            IffTypeIndex cards = IffCardFile.loadIndex(archive);
            Map<Integer, IffCharacterRecord> characters = IffCharacterFile.loadIndex(archive);
            IffCharacterMasteryIndex characterMastery = IffCharacterMasteryFile.loadIndex(archive);
            IffEnchantIndex enchants = IffEnchantFile.loadIndex(archive);
            IffClubSetIndex clubSets = IffClubSetFile.loadIndex(archive);
            IffCaddieIndex caddies = IffCaddieFile.loadIndex(archive);
            IffMascotIndex mascots = IffMascotFile.loadIndex(archive);
            IffAuxPartIndex auxParts = IffAuxPartFile.loadIndex(archive);
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
            IffGrandPrixSpecialHoleIndex grandPrixSpecialHoles = IffGrandPrixSpecialHoleFile.loadIndex(archive);
            IffGrandPrixConditionEquipIndex grandPrixConditionEquip =
                    IffGrandPrixConditionEquipFile.loadIndex(archive);
            IffTypeIndex skins = IffSkinFile.loadIndex(archive);
            IffTypeIndex caddieItems = IffCaddieItemFile.loadIndex(archive);
            snapshot = new Snapshot(
                    courses, parts, items, balls, cards, characters, characterMastery, enchants, clubSets, caddies,
                    mascots, auxParts, clubSetWorkShopLevelUpLimits, clubSetWorkShopLevelUpProbs, clubSetWorkShopRankExps,
                    cutins, timeLimitItems, cadieMagicBoxes, cadieMagicBoxRandoms, setItems, grandPrixData,
                    grandPrixSpecialHoles, grandPrixConditionEquip, skins, caddieItems, path);
            log.info(
                    "loaded pangya iff {} ({} courses, {} parts, {} items, {} balls, {} cards, {} chars, {} mastery, {} enchants, {} clubsets, {} caddies, {} mascots, {} auxparts, {} skins, {} caddie items, {} ws limits, {} ws probs, {} ws rank exp, {} cutins, {} time limit items, {} cadie boxes, {} cadie random rows, {} set items, {} grand prix, {} gp special holes, {} gp condition equip)",
                    path,
                    courses.size(),
                    parts.size(),
                    items.size(),
                    balls.size(),
                    cards.size(),
                    characters.size(),
                    characterMastery.rowCount(),
                    enchants.size(),
                    clubSets.size(),
                    caddies.size(),
                    mascots.size(),
                    auxParts.size(),
                    skins.size(),
                    caddieItems.size(),
                    clubSetWorkShopLevelUpLimits.rowCount(),
                    clubSetWorkShopLevelUpProbs.size(),
                    clubSetWorkShopRankExps.size(),
                    cutins.size(),
                    timeLimitItems.size(),
                    cadieMagicBoxes.size(),
                    cadieMagicBoxRandoms.rowCount(),
                    setItems.size(),
                    grandPrixData.size(),
                    grandPrixSpecialHoles.rowCount(),
                    grandPrixConditionEquip.size());
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

    /** C# {@code sIff.findItem}. */
    public static Optional<IffItemRecord> item(int typeid) {
        IffItemIndex items = snapshot.items();
        if (items.isEmpty()) {
            return Optional.empty();
        }
        return items.find(typeid);
    }

    /** C# {@code DropSystem.drawManaArtefact} IFF pool ({@code ItemType == 4}). */
    public static List<Integer> manaArtefactTypeids() {
        IffItemIndex items = snapshot.items();
        if (items.isEmpty()) {
            return List.of();
        }
        return items.manaArtefactTypeids();
    }

    /** C# {@code Ball.Stats.getSlot[0]}; {@code 0} when IFF unloaded or unknown. */
    public static int ballStackSize(int typeid) {
        IffBallIndex balls = snapshot.balls();
        if (balls.isEmpty()) {
            return 0;
        }
        return balls.stackSize(typeid).orElse(0);
    }

    public static IffBallIndex ballIndex() {
        return snapshot.balls();
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

    /** C# {@code sIff.findAuxPart}. */
    public static Optional<IffAuxPartRecord> auxPart(int typeid) {
        IffAuxPartIndex auxParts = snapshot.auxParts();
        if (auxParts.byTypeid().isEmpty()) {
            return Optional.empty();
        }
        return auxParts.find(typeid);
    }

    public static IffAuxPartIndex auxPartIndex() {
        return snapshot.auxParts();
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

    /** C# {@code sIff.findGrandPrixSpecialHole}. */
    public static List<IffGrandPrixSpecialHoleRecord> grandPrixSpecialHoles(int rankTypeid) {
        IffGrandPrixSpecialHoleIndex special = snapshot.grandPrixSpecialHoles();
        if (special.isEmpty()) {
            return List.of();
        }
        return special.find(rankTypeid);
    }

    /** C# {@code sIff.findGrandPrixConditionEquip}. */
    public static Optional<IffGrandPrixConditionEquipRecord> grandPrixConditionEquip(int typeidLink) {
        IffGrandPrixConditionEquipIndex conditionEquip = snapshot.grandPrixConditionEquip();
        if (conditionEquip.isEmpty()) {
            return Optional.empty();
        }
        return conditionEquip.find(typeidLink);
    }

    public static Optional<Path> source() {
        return Optional.ofNullable(snapshot.source());
    }

    public static IffTypeIndex skinIndex() {
        return snapshot.skins();
    }

    public static IffTypeIndex caddieItemIndex() {
        return snapshot.caddieItems();
    }

    /** C# {@code sIff.findCommomItem}.Name from the matching IFF dataset. */
    public static Optional<String> commonItemName(int typeid) {
        Path src = snapshot.source();
        if (src == null) {
            return Optional.empty();
        }
        IffNameDataset dataset = nameDataset(typeid);
        if (dataset == null) {
            return Optional.empty();
        }
        try {
            byte[] data = new PangyaIffArchive(src).readEntry(dataset.entry());
            return IffCommonFile.nameForTypeid(data, dataset.recordBytes(), dataset.version(), typeid);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static IffNameDataset nameDataset(int typeid) {
        int group = (typeid & 0xFC000000) >>> 26;
        return switch (group) {
            case 1 -> new IffNameDataset("Character.iff", 420, 13);
            case 2 -> new IffNameDataset("Part.iff", 568, 13);
            case 4 -> new IffNameDataset("ClubSet.iff", 260, 13);
            case 5 -> new IffNameDataset("Ball.iff", 816, 13);
            case 6 -> new IffNameDataset("Item.iff", 248, 13);
            case 7 -> new IffNameDataset("Caddie.iff", 248, 13);
            case 9 -> new IffNameDataset("SetItem.iff", 268, 13);
            case 16 -> new IffNameDataset("Mascot.iff", 304, 13);
            case 28 -> new IffNameDataset("AuxPart.iff", 228, 13);
            case 31 -> new IffNameDataset("Card.iff", 384, 13);
            case 8 -> new IffNameDataset("CaddieItem.iff", 284, 13);
            case 14 -> new IffNameDataset("Skin.iff", 244, 13);
            case 56 -> new IffNameDataset("CutinInfomation.iff", 244, 13);
            default -> null;
        };
    }
}
