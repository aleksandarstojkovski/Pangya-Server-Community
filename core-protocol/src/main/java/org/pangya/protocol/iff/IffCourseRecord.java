package org.pangya.protocol.iff;

/** One row from {@code Course.iff} (C# {@code PangyaAPI.IFF.JP.Models.Data.Course}). */
public record IffCourseRecord(
        int typeid,
        String name,
        int star,
        float ratePang,
        int[] parByHole,
        int[] maxScoreByHole) {

    public IffCourseRecord(int typeid, String name, int star, float ratePang, int[] parByHole) {
        this(typeid, name, star, ratePang, parByHole, defaultMaxScores(parByHole));
    }

    private static int[] defaultMaxScores(int[] parByHole) {
        int[] max = new int[18];
        for (int i = 0; i < 18; i++) {
            max[i] = 5;
        }
        return max;
    }

    public int courseId() {
        return MapClearBonusTable.courseIndex(typeid);
    }

    public int clearBonus() {
        return MapClearBonusTable.clearBonusForCourse(courseId());
    }

    /** C# {@code 1f + (el.Star / 10f)}. */
    public float starFactor() {
        return 1f + star / 10f;
    }
}
