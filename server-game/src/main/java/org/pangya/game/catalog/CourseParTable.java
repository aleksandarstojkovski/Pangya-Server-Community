package org.pangya.game.catalog;

/**
 * SQL/IFF stand-in for C# {@code MapSystem} / {@code Course.Par_Hole} until course
 * binary loaders exist. Blue Lagoon (course 0) uses JP Season 9 pars.
 */
public final class CourseParTable {

    /** Blue Lagoon 18-hole par sequence. */
    private static final int[] BLUE_LAGOON =
            {4, 4, 3, 4, 5, 4, 3, 4, 4, 4, 3, 4, 5, 4, 3, 4, 4, 4};

    private CourseParTable() {}

    public static int par(int courseId, int holeNum) {
        if (holeNum < 1 || holeNum > 18) {
            return 4;
        }
        if ((courseId & 0x7f) == 0) {
            return BLUE_LAGOON[holeNum - 1];
        }
        return 4;
    }
}
