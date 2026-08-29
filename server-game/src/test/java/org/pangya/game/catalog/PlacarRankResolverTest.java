package org.pangya.game.catalog;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlacarRankResolverTest {

    @Test
    void sortPlayerRankUsesScoreThenPang() {
        List<PlacarRankResolver.RankEntry> sorted = PlacarRankResolver.sortRankEntries(List.of(
                new PlacarRankResolver.RankEntry(1, 0, 50),
                new PlacarRankResolver.RankEntry(2, -2, 100),
                new PlacarRankResolver.RankEntry(3, 0, 80)));
        assertEquals(2, sorted.get(0).oid());
        assertEquals(3, sorted.get(1).oid());
        assertEquals(1, sorted.get(2).oid());
        assertEquals(1, PlacarRankResolver.rankIndex(sorted, 3));
    }

    @Test
    void calcMatchTeamWinUsesPointsThenPang() {
        assertEquals(0, PlacarRankResolver.calcMatchTeamWin(1, 0, 0, 0));
        assertEquals(1, PlacarRankResolver.calcMatchTeamWin(1, 50, 1, 100));
        assertEquals(2, PlacarRankResolver.calcMatchTeamWin(1, 100, 1, 100));
    }

    @Test
    void matchExpFactorAppliesLossPenalty() {
        assertEquals(1.0f, PlacarRankResolver.matchExpFactor(2, 0));
        assertEquals(1.0f, PlacarRankResolver.matchExpFactor(0, 0));
        assertEquals(0.6f, PlacarRankResolver.matchExpFactor(0, 1));
    }

    @Test
    void awardMatchHolePointIncrementsWinningTeam() {
        int[] points = PlacarRankResolver.awardMatchHolePoint(
                0, 0, true, false, 9, 0, 2, 4);
        assertEquals(1, points[0]);
        assertEquals(0, points[1]);
    }
}
