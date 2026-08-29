package org.pangya.game.catalog;

/** C# {@code grand_prix_type.eRULE} typeids from {@code GrandPrixData.rule}. */
public final class GrandPrixRules {

    public static final int TIME_10_SEC = 0x1A000268;
    public static final int TIME_15_SEC = 0x1A00029E;
    public static final int SPECIAL_SHOT = 0x1A000267;

    private GrandPrixRules() {}

    public static boolean isTimedRule(int rule) {
        return rule == TIME_10_SEC || rule == TIME_15_SEC;
    }

    public static int ruleMillis(int rule) {
        if (rule == TIME_10_SEC) {
            return 10_000;
        }
        if (rule == TIME_15_SEC) {
            return 15_000;
        }
        return 0;
    }
}
