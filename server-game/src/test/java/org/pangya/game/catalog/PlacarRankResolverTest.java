package org.pangya.game.catalog;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void matchHoleEndsDetectsChipInWin() {
        assertTrue(PlacarRankResolver.matchHoleEnds(
                false, 4, true, false, false, false, 2, 4, 0, 0, 5, 18));
        assertFalse(PlacarRankResolver.matchHoleEnds(
                false, 4, true, false, false, false, 0, 0, 0, 0, 5, 18));
    }

    @Test
    void matchHoleEndsDetectsPointLeadOverRemainingHoles() {
        assertTrue(PlacarRankResolver.matchHoleEnds(
                false, 4, true, false, false, false, 4, 3, 10, 1, 10, 18));
        assertFalse(PlacarRankResolver.matchHoleEnds(
                false, 4, true, false, false, false, 4, 3, 3, 2, 10, 18));
    }

    @Test
    void matchHoleEndsRespectsOpponentTimeout() {
        assertFalse(PlacarRankResolver.matchHoleEnds(
                false, 4, true, false, false, true, 2, 4, 0, 0, 5, 18));
    }

    @Test
    void matchGameEndsEarlyOnPointLeadOnly() {
        assertTrue(PlacarRankResolver.matchGameEndsEarly(10, 0, 9, 18));
        assertFalse(PlacarRankResolver.matchGameEndsEarly(5, 0, 9, 18));
    }

    @Test
    void matchGameEndsIncludesLastHoleAndOddPlayers() {
        assertTrue(PlacarRankResolver.matchGameEnds(1, 1, 18, 18, 4));
        assertTrue(PlacarRankResolver.matchGameEnds(0, 0, 5, 18, 3));
        assertFalse(PlacarRankResolver.matchGameEnds(5, 0, 9, 18, 4));
    }

    @Test
    void awardMatchHolePointIncrementsWinningTeam() {
        int[] points = PlacarRankResolver.awardMatchHolePoint(
                0, 0, true, false, 9, 0, 2, 4);
        assertEquals(1, points[0]);
        assertEquals(0, points[1]);
    }
}
