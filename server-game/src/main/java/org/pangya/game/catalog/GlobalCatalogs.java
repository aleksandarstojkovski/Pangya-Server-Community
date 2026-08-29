package org.pangya.game.catalog;

import org.pangya.db.InventoryRepository;
import org.pangya.protocol.iff.IffCourseRecord;
import org.pangya.protocol.iff.PangyaIffLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * C# {@code reload_systems} / {@code reloadGlobalSystem} in-memory catalog cache.
 * SQL is authoritative; reload refreshes snapshots used by {@link org.pangya.game.GameHandler}.
 */
public final class GlobalCatalogs {

    private static final Logger log = LoggerFactory.getLogger(GlobalCatalogs.class);

    private final InventoryRepository inventory;
    private final Path pangyaIffPath;

    private volatile Map<Integer, InventoryRepository.CometRefill> cometRefills = Map.of();
    private volatile Map<Integer, InventoryRepository.ShopItem> shopItems = Map.of();
    private volatile Map<Integer, InventoryRepository.BoxMailReward> boxMail = Map.of();
    private volatile Map<Integer, List<InventoryRepository.CardPackReward>> cardPacks = Map.of();
    private volatile Map<Integer, List<InventoryRepository.MemorialReward>> memorial = Map.of();
    private volatile List<InventoryRepository.AttendanceCatalogItem> attendanceAll = List.of();
    private volatile Map<Integer, List<InventoryRepository.AttendanceCatalogItem>> attendanceByTipo = Map.of();
    private volatile Map<Short, Boolean> coinCubeActive = Map.of();
    private volatile Map<Short, List<InventoryRepository.CoinCubeLocation>> coinCubeByCourse = Map.of();
    private volatile Map<Integer, List<InventoryRepository.CourseDropItem>> courseDropByCourse = Map.of();
    private volatile Map<Integer, Integer> coursePar = Map.of();
    private volatile Map<Short, MapCatalog.CourseCtx> courseMaps = Map.of();

    public GlobalCatalogs(InventoryRepository inventory) {
        this(inventory, null);
    }

    public GlobalCatalogs(InventoryRepository inventory, Path pangyaIffPath) {
        this.inventory = inventory;
        this.pangyaIffPath = pangyaIffPath;
        PangyaIffLoader.reload(pangyaIffPath);
        reload(0);
    }

    /** C# {@code GameService.reloadGlobalSystem}. */
    public void reload(int tipo) {
        try {
            switch (tipo) {
                case 0 -> reloadAll();
                case 1 -> reloadIffSqlStandIns();
                case 2 -> reloadCardPack();
                case 3 -> reloadCometRefill();
                case 4 -> log.info("auth reload papel shop (queried per request)");
                case 5 -> reloadBoxMail();
                case 6 -> reloadMemorial();
                case 7, 14 -> reloadCoinCube();
                case 8, 9 -> reloadCourseDrops();
                case 10 -> reloadAttendance();
                case 11 -> reloadCourseData();
                case 12, 13, 15, 16, 17 -> log.info("auth reload event tipo={} (event SQL stub)", tipo);
                case 18 -> log.info("auth reload smart calculator (not ported)");
                default -> {
                    log.warn("auth reload global system unknown tipo={}", tipo);
                    return;
                }
            }
            if (tipo != 0) {
                log.info("auth reload global system tipo={} ok", tipo);
            }
        } catch (RuntimeException e) {
            log.warn("auth reload global system tipo={} failed: {}", tipo, e.toString());
        }
    }

    private void reloadAll() {
        reloadCometRefill();
        reloadShop();
        reloadBoxMail();
        reloadCardPack();
        reloadMemorial();
        reloadAttendance();
        reloadCoinCube();
        reloadCourseDrops();
        reloadCourseData();
        log.info("auth reload all global SQL catalogs ok");
    }

    /** C# {@code sIff.reload()} stand-in: SQL catalogs + optional {@code pangya_jp.iff} overlay. */
    private void reloadIffSqlStandIns() {
        PangyaIffLoader.reload(pangyaIffPath);
        reloadCourseData();
        reloadCoinCube();
        if (pangyaIffPath != null) {
            log.info("auth reload IFF (Course/Part/Item/Card/Character/CharacterMastery/Enchant/ClubSet/Caddie/Mascot/ClubSetWorkShop/Cutin/TimeLimit/CadieMagicBox/SetItem/GrandPrixData/GrandPrixSpecialHole/GrandPrixConditionEquip from {})", pangyaIffPath);
        } else {
            log.info("auth reload IFF SQL stand-ins (set PANGYA_IFF_PATH for binary)");
        }
    }

    private void reloadCometRefill() {
        cometRefills = Map.copyOf(inventory.cometRefillIndex());
    }

    private void reloadShop() {
        shopItems = Map.copyOf(inventory.shopCatalogIndex());
    }

    private void reloadBoxMail() {
        boxMail = Map.copyOf(inventory.boxMailIndex());
    }

    private void reloadCardPack() {
        cardPacks = Map.copyOf(inventory.cardPackIndex());
    }

    private void reloadMemorial() {
        memorial = Map.copyOf(inventory.memorialIndex());
    }

    private void reloadAttendance() {
        List<InventoryRepository.AttendanceCatalogItem> rows = inventory.attendanceCatalogIndex();
        attendanceAll = List.copyOf(rows);
        Map<Integer, List<InventoryRepository.AttendanceCatalogItem>> byTipo = new HashMap<>();
        for (InventoryRepository.AttendanceCatalogItem row : rows) {
            byTipo.computeIfAbsent(row.tipo(), k -> new ArrayList<>()).add(row);
        }
        Map<Integer, List<InventoryRepository.AttendanceCatalogItem>> frozen = new HashMap<>();
        byTipo.forEach((k, v) -> frozen.put(k, List.copyOf(v)));
        attendanceByTipo = Map.copyOf(frozen);
    }

    private void reloadCoinCube() {
        coinCubeActive = Map.copyOf(inventory.coinCubeCourseActive());
        Map<Short, List<InventoryRepository.CoinCubeLocation>> byCourse = new HashMap<>();
        for (InventoryRepository.CoinCubeLocation loc : inventory.coinCubeLocations()) {
            byCourse.computeIfAbsent(loc.course(), k -> new ArrayList<>()).add(loc);
        }
        Map<Short, List<InventoryRepository.CoinCubeLocation>> frozen = new HashMap<>();
        byCourse.forEach((k, v) -> frozen.put(k, List.copyOf(v)));
        coinCubeByCourse = Map.copyOf(frozen);
    }

    private void reloadCourseDrops() {
        courseDropByCourse = Map.copyOf(inventory.courseDropIndex());
    }

    /** C# {@code DropSystem.findCourse}. */
    public List<InventoryRepository.CourseDropItem> courseDropItems(int courseId) {
        return courseDropByCourse.getOrDefault(courseId, List.of());
    }

    private void reloadCourseData() {
        Map<Integer, Integer> par = new HashMap<>(inventory.courseParIndex());
        Map<Short, MapCatalog.CourseCtx> built = new HashMap<>();

        Optional<List<IffCourseRecord>> iffCourses = loadIffCourses();
        if (iffCourses.isPresent()) {
            for (IffCourseRecord row : iffCourses.get()) {
                short courseId = (short) row.courseId();
                for (int hole = 1; hole <= 18; hole++) {
                    int holePar = row.parByHole()[hole - 1];
                    if (holePar > 0) {
                        par.put((courseId << 8) | hole, holePar);
                    }
                }
                built.put(courseId, MapCatalog.fromIff(row));
            }
            for (InventoryRepository.CourseMap row : inventory.courseMapIndex().values()) {
                built.putIfAbsent(row.courseId(), MapCatalog.build(row, par));
            }
        } else {
            for (InventoryRepository.CourseMap row : inventory.courseMapIndex().values()) {
                built.put(row.courseId(), MapCatalog.build(row, par));
            }
        }

        coursePar = Map.copyOf(par);
        courseMaps = Map.copyOf(built);
    }

    private Optional<List<IffCourseRecord>> loadIffCourses() {
        return PangyaIffLoader.courses();
    }

    public int parFor(int courseId, int holeNum) {
        return coursePar.getOrDefault(((courseId & 0x7f) << 8) | (holeNum & 0xff), 4);
    }

    /** C# {@code MapSystem.getMap}. */
    public MapCatalog.CourseCtx courseMap(int courseId) {
        return courseMaps.get((short) (courseId & 0x7f));
    }

    public Optional<InventoryRepository.CometRefill> cometRefill(int typeid) {
        return Optional.ofNullable(cometRefills.get(typeid));
    }

    public Optional<InventoryRepository.ShopItem> shopItem(int typeid) {
        return Optional.ofNullable(shopItems.get(typeid));
    }

    public Optional<InventoryRepository.BoxMailReward> boxMailReward(int boxTypeid) {
        return Optional.ofNullable(boxMail.get(boxTypeid));
    }

    public List<InventoryRepository.CardPackReward> cardPackRewards(int packTypeid) {
        return cardPacks.getOrDefault(packTypeid, List.of());
    }

    public List<InventoryRepository.MemorialReward> memorialRewards(int coinTypeid) {
        return memorial.getOrDefault(coinTypeid, List.of());
    }

    public List<InventoryRepository.AttendanceCatalogItem> attendanceCatalog(int tipo) {
        return attendanceByTipo.getOrDefault(tipo, List.of());
    }

    public List<InventoryRepository.AttendanceCatalogItem> attendanceCatalogAll() {
        return attendanceAll;
    }

    public Map<Short, Boolean> coinCubeCourseActive() {
        return coinCubeActive;
    }

    public List<InventoryRepository.CoinCubeLocation> coinCubeLocations(short course) {
        return coinCubeByCourse.getOrDefault(course, List.of());
    }

    /** Tests assert catalog snapshots after auth reload. */
    int cometRefillCount() {
        return cometRefills.size();
    }

    int courseParCount() {
        return coursePar.size();
    }

    int courseMapCount() {
        return courseMaps.size();
    }
}
