package org.pangya.game.catalog;

import org.pangya.db.InventoryRepository;

import java.util.Arrays;

import org.pangya.protocol.iff.IffCourseRecord;

/** C# {@code MapSystem} / {@code Map} using SQL {@code iff_course} + {@code iff_course_hole}. */
public final class MapCatalog {

    /** C# {@code Map.stCtx}. */
    public record CourseCtx(String name, int clearBonus, float star, int[] parByHole) {}

    private MapCatalog() {}

    public static CourseCtx build(
            InventoryRepository.CourseMap row, java.util.Map<Integer, Integer> parIndex) {
        int course = row.courseId() & 0x7f;
        int[] par = new int[18];
        Arrays.fill(par, 4);
        for (int hole = 1; hole <= 18; hole++) {
            par[hole - 1] = parIndex.getOrDefault((course << 8) | hole, 4);
        }
        float star = 1f + row.starTenths() / 10f;
        return new CourseCtx(row.name(), row.clearBonus(), star, par);
    }

    /** C# {@code Map.initialize} from {@code Course.iff} + clear_bonus switch. */
    public static CourseCtx fromIff(IffCourseRecord row) {
        int[] par = Arrays.copyOf(row.parByHole(), 18);
        return new CourseCtx(row.name(), row.clearBonus(), row.starFactor(), par);
    }

    /** C# {@code calculeClearVS}. */
    public static int calculeClearVs(CourseCtx ctx, int numPlayers, int holeCount) {
        if (ctx == null || numPlayers <= 1) {
            return 0;
        }
        return ctx.clearBonus() * holeCount * (numPlayers - 1);
    }

    /** C# {@code calculeClearMatch}. */
    public static int calculeClearMatch(CourseCtx ctx, int holeCount) {
        if (ctx == null) {
            return 0;
        }
        return ctx.clearBonus() * holeCount;
    }

    /** C# {@code calculeClear30s}. */
    public static int calculeClear30s(CourseCtx ctx, int holeCount) {
        if (ctx == null || ctx.clearBonus() == 0 || holeCount == 0) {
            return 0;
        }
        return (ctx.clearBonus() * holeCount) / 2;
    }

    /** C# {@code calculeClearSSC}. */
    public static int calculeClearSsc(CourseCtx ctx) {
        return ctx == null ? 0 : ctx.clearBonus();
    }
}
