package org.pangya.game.catalog;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.pangya.protocol.game.GamePackets;
import org.pangya.protocol.iff.PangyaIffLoader;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PangBonusCalculatorTest {

    private static final Path JP_IFF = Path.of(
            "reference/pangya-server-community/Server/JP/GameServer/data/pangya_jp.iff");

    @AfterEach
    void tearDown() {
        PangyaIffLoader.reload(null);
    }

    @Test
    void calculeBonusPangAppliesItemAndServerRate() {
        long bonus = PangBonusCalculator.calculeBonusPang(
                1000, 100, 200, 200, 0, 0);
        assertTrue(bonus > 100);
    }

    @Test
    void applyPracticePangTaxScalesCoursePracticeByThird() {
        long[] taxed = PangBonusCalculator.applyPracticePangTax(
                900, 300, GamePackets.TIPO_PRACTICE, 0);
        assertEquals(300, taxed[0]);
        assertEquals(100, taxed[1]);
    }

    @Test
    void shuffleModoAddsTenToServerPangRate() {
        long normal = PangBonusCalculator.calculeBonusPang(
                1000, 0, 100, 100, 0, 0);
        long shuffle = PangBonusCalculator.calculeBonusPang(
                1000, 0, 100, 100, 0, PangBonusCalculator.MODO_SHUFFLE);
        assertTrue(shuffle > normal);
    }

    @Test
    void courseRatePangReadsFromIffWhenPresent() throws Exception {
        if (!JP_IFF.toFile().isFile()) {
            assertEquals(1.0f, PangBonusCalculator.courseRatePang(0));
            return;
        }
        PangyaIffLoader.reload(JP_IFF);
        float rate = PangBonusCalculator.courseRatePang(0);
        assertTrue(rate >= 1.0f);
    }
}
