package org.pangya.protocol.iff;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IffCourseFileTest {

    private static final Path JP_IFF = Path.of(
            "reference/pangya-server-community/Server/JP/GameServer/data/pangya_jp.iff");

    @Test
    void loadsBlueLagoonFromReferenceArchive() throws Exception {
        assumeReferenceIffPresent();
        PangyaIffArchive archive = new PangyaIffArchive(JP_IFF);
        var courses = IffCourseFile.load(archive);
        assertEquals(22, courses.size());

        IffCourseRecord blue = courses.stream()
                .filter(c -> c.courseId() == 0)
                .findFirst()
                .orElseThrow();
        assertEquals("BLUE LAGOON", blue.name());
        assertEquals(17, blue.star());
        assertEquals(20, blue.clearBonus());
        assertEquals(2.7f, blue.starFactor(), 0.001f);
        assertEquals(4, blue.parByHole()[0]);
        assertEquals(3, blue.parByHole()[1]);
    }

    @Test
    void clearBonusMatchesCsharpMapSystem() {
        assertEquals(360, MapClearBonusTable.clearBonusForCourse(17));
        assertEquals(0, MapClearBonusTable.clearBonusForCourse(64));
    }

    private static void assumeReferenceIffPresent() {
        assertTrue(JP_IFF.toFile().isFile(), () -> "missing " + JP_IFF);
    }
}
