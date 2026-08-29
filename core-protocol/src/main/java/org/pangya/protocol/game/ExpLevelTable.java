package org.pangya.protocol.game;

/** C# {@code DefineConstants.ExpByLevel}. */
public final class ExpLevelTable {

    /** Cost to advance from level index {@code L} to {@code L + 1}. */
    public static final int[] COST_BY_LEVEL = {
            30, 40, 50, 60, 70, 140,
            105, 125, 145, 165, 330,
            248, 278, 308, 338, 675,
            506, 546, 586, 626, 1253,
            1002, 1052, 1102, 1152, 2304,
            1843, 1903, 1963, 2023, 4046,
            3237, 3307, 3377, 3447, 6894,
            5515, 5595, 5675, 5755, 11511,
            8058, 8148, 8238, 8328, 16655,
            8328, 8428, 8528, 8628, 17255,
            9490, 9690, 9890, 10090, 20181,
            20181, 20481, 20781, 21081, 42161,
            37945, 68301, 122942, 221296, 442592,
            663887, 995831, 1493747, 2240620, 0,
    };

    /** C# {@code level >= 69} is max (display level 70). */
    public static final int MAX_LEVEL_INDEX = 69;

    /** C# {@code level < 70} gate before awarding finish-game exp. */
    public static final int MAX_AWARD_LEVEL = 70;

    private ExpLevelTable() {}

    public static int costForLevel(int levelIndex) {
        if (levelIndex < 0 || levelIndex >= COST_BY_LEVEL.length) {
            return 0;
        }
        return COST_BY_LEVEL[levelIndex];
    }
}
