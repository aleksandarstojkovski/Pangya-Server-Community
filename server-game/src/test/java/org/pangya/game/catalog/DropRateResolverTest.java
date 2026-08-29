package org.pangya.game.catalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DropRateResolverTest {

    @Test
    void rateContributionMatchesCSharpAuxPartRules() {
        assertEquals(0, DropRateResolver.rateContribution(0));
        assertEquals(20, DropRateResolver.rateContribution(20));
        assertEquals(20, DropRateResolver.rateContribution(120));
    }

    @Test
    void computeDropRateDefaultsWithoutIff() {
        assertEquals(100, DropRateResolver.computeDropRate(new int[] {0x70000001}, 0));
    }
}
