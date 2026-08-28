package org.pangya.game.catalog;

import org.pangya.db.InventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private volatile Map<Integer, InventoryRepository.CometRefill> cometRefills = Map.of();
    private volatile Map<Integer, InventoryRepository.ShopItem> shopItems = Map.of();
    private volatile Map<Integer, InventoryRepository.BoxMailReward> boxMail = Map.of();
    private volatile Map<Integer, List<InventoryRepository.CardPackReward>> cardPacks = Map.of();
    private volatile Map<Integer, List<InventoryRepository.MemorialReward>> memorial = Map.of();
    private volatile List<InventoryRepository.AttendanceCatalogItem> attendanceAll = List.of();
    private volatile Map<Integer, List<InventoryRepository.AttendanceCatalogItem>> attendanceByTipo = Map.of();
    private volatile Map<Short, Boolean> coinCubeActive = Map.of();
    private volatile Map<Short, List<InventoryRepository.CoinCubeLocation>> coinCubeByCourse = Map.of();

    public GlobalCatalogs(InventoryRepository inventory) {
        this.inventory = inventory;
        reload(0);
    }

    /** C# {@code GameService.reloadGlobalSystem}. */
    public void reload(int tipo) {
        try {
            switch (tipo) {
                case 0 -> reloadAll();
                case 1 -> log.info("auth reload IFF (binary files absent; SQL catalogs unchanged)");
                case 2 -> reloadCardPack();
                case 3 -> reloadCometRefill();
                case 4 -> log.info("auth reload papel shop (queried per request)");
                case 5 -> reloadBoxMail();
                case 6 -> reloadMemorial();
                case 7, 14 -> reloadCoinCube();
                case 8, 9 -> log.info("auth reload tipo={} (treasure/drop not cataloged in SQL yet)", tipo);
                case 10 -> reloadAttendance();
                case 11 -> log.info("auth reload map course data (IFF map files absent)");
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
        log.info("auth reload all global SQL catalogs ok");
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
}
