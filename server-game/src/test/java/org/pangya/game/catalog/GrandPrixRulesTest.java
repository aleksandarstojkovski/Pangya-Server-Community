package org.pangya.game.catalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrandPrixRulesTest {

    @Test
    void timedRulesMatchCsharpTypeids() {
        assertTrue(GrandPrixRules.isTimedRule(GrandPrixRules.TIME_10_SEC));
        assertTrue(GrandPrixRules.isTimedRule(GrandPrixRules.TIME_15_SEC));
        assertFalse(GrandPrixRules.isTimedRule(GrandPrixRules.SPECIAL_SHOT));
    }

    @Test
    void ruleMillisMatchesCsharpSeconds() {
        assertEquals(10_000, GrandPrixRules.ruleMillis(GrandPrixRules.TIME_10_SEC));
        assertEquals(15_000, GrandPrixRules.ruleMillis(GrandPrixRules.TIME_15_SEC));
        assertEquals(0, GrandPrixRules.ruleMillis(0));
    }
}
