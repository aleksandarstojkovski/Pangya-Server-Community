package org.pangya.game.catalog;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.pangya.db.ItemInitializer;
import org.pangya.protocol.iff.IffRewardSlots;
import org.pangya.protocol.iff.PangyaIffLoader;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrandPrixRewardResolverTest {

    private static final Path JP_IFF = Path.of(
            "reference/pangya-server-community/Server/JP/GameServer/data/pangya_jp.iff");

    private static final int TYPEID_GREENLINE_SWIMSET = 0x24200000;

    @AfterEach
    void tearDown() {
        PangyaIffLoader.reload(null);
    }

    @Test
    void shouldGrantOverlappableExceptCadItem() {
        Set<Integer> overlap = Set.of(0x1a000010);
        Set<Integer> owned = new HashSet<>();
        var checks = checks(owned, overlap);
        assertTrue(GrandPrixRewardResolver.shouldGrant(checks, 1, 0x1a000010));
        owned.add(0x140000a1);
        assertFalse(GrandPrixRewardResolver.shouldGrant(checks, 1, 0x140000a1));
    }

    @Test
    void resolveParticipationRewardFromReferenceIff() {
        if (!JP_IFF.toFile().isFile()) {
            return;
        }
        PangyaIffLoader.reload(JP_IFF);
        var gp = PangyaIffLoader.grandPrixData(0x80101).orElseThrow();
        var checks = checks(Set.of(), Set.of(0x1a000010, 0x140000a1));
        List<ItemInitializer.MailAwardRow> rows =
                GrandPrixRewardResolver.resolveRewardAwards(checks, 10001, 10, gp.reward());
        assertEquals(3, rows.size());
        assertEquals(0x1a000010, rows.get(0).typeid());
        assertEquals(300, rows.get(0).warehouse().qntdDep());
        assertEquals(0x140000a1, rows.get(1).typeid());
        assertEquals(10, rows.get(1).warehouse().qntdDep());
        assertEquals(0x1a0000c2, rows.get(2).typeid());
        assertEquals(5, rows.get(2).warehouse().qntdDep());
    }

    @Test
    void resolveSetItemExpandsPackageMembers() {
        if (!JP_IFF.toFile().isFile()) {
            return;
        }
        PangyaIffLoader.reload(JP_IFF);
        int[] typeids = new int[IffRewardSlots.SLOTS];
        int[] qntd = new int[IffRewardSlots.SLOTS];
        typeids[0] = TYPEID_GREENLINE_SWIMSET;
        qntd[0] = 1;
        IffRewardSlots reward = new IffRewardSlots(typeids, qntd, new int[IffRewardSlots.SLOTS]);
        List<ItemInitializer.MailAwardRow> rows = GrandPrixRewardResolver.resolveRewardAwards(
                checks(Set.of(), Set.of()), 10001, 10, reward);
        assertEquals(3, rows.size());
        assertEquals(0x08006010, rows.get(0).typeid());
        assertEquals(0x0801000a, rows.get(1).typeid());
        assertEquals(0x0800080a, rows.get(2).typeid());
    }

    @Test
    void skipsOwnedSetItemPackage() {
        if (!JP_IFF.toFile().isFile()) {
            return;
        }
        PangyaIffLoader.reload(JP_IFF);
        int[] typeids = new int[IffRewardSlots.SLOTS];
        int[] qntd = new int[IffRewardSlots.SLOTS];
        typeids[0] = TYPEID_GREENLINE_SWIMSET;
        qntd[0] = 1;
        IffRewardSlots reward = new IffRewardSlots(typeids, qntd, new int[IffRewardSlots.SLOTS]);
        List<ItemInitializer.MailAwardRow> rows = GrandPrixRewardResolver.resolveRewardAwards(
                checks(Set.of(TYPEID_GREENLINE_SWIMSET), Set.of()), 10001, 10, reward);
        assertTrue(rows.isEmpty());
    }

    @Test
    void resolveTimedRewardUsesRentalDays() {
        if (!JP_IFF.toFile().isFile()) {
            return;
        }
        PangyaIffLoader.reload(JP_IFF);
        int[] typeids = new int[IffRewardSlots.SLOTS];
        int[] qntd = new int[IffRewardSlots.SLOTS];
        int[] time = new int[IffRewardSlots.SLOTS];
        typeids[0] = 0x08006010;
        time[0] = 7;
        IffRewardSlots reward = new IffRewardSlots(typeids, qntd, time);
        List<ItemInitializer.MailAwardRow> rows = GrandPrixRewardResolver.resolveRewardAwards(
                checks(Set.of(), Set.of()), 10001, 10, reward);
        assertEquals(1, rows.size());
        assertEquals(4, rows.getFirst().rentFlag());
        assertEquals(7, rows.getFirst().caddiePeriodDays());
    }

    private static GrandPrixRewardResolver.GrantChecks checks(Set<Integer> owned, Set<Integer> overlap) {
        return new GrandPrixRewardResolver.GrantChecks(
                overlap::contains, (uid, typeid) -> owned.contains(typeid));
    }
}
