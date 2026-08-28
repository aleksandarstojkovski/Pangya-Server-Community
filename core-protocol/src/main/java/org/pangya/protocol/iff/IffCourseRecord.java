package org.pangya.protocol.iff;

/** One row from {@code Course.iff} (C# {@code PangyaAPI.IFF.JP.Models.Data.Course}). */
public record IffCourseRecord(
        int typeid,
        String name,
        int star,
        float ratePang,
        int[] parByHole) {

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
