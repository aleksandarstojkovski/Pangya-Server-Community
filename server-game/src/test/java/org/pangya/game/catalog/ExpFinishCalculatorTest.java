package org.pangya.game.catalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExpFinishCalculatorTest {

    @Test
    void versusFinishExpAppliesRatesAndRankPenalty() {
        int exp = ExpFinishCalculator.versusFinishExp(
                2, 18, 1.0f, 200, 100, 1);
        assertEquals(64, exp);
    }

    @Test
    void matchFinishExpAppliesTeamLossFactor() {
        int winner = ExpFinishCalculator.matchFinishExp(2, 18, 1.0f, 100, 100, 1.0f);
        int loser = ExpFinishCalculator.matchFinishExp(2, 18, 1.0f, 100, 100, 0.6f);
        assertEquals(36, winner);
        assertEquals(21, loser);
    }

    @Test
    void practiceFinishExpIsZeroInJpReference() {
        assertEquals(0, ExpFinishCalculator.practiceFinishExp());
    }

    @Test
    void resolveHoleSeqZeroWhenFirstHoleMissed() {
        assertEquals(0, ExpFinishCalculator.resolveHoleSeq(1, 18, 0, false));
    }
}
