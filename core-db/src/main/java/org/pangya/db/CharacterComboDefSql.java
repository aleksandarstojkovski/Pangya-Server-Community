package org.pangya.db;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.pangya.protocol.game.CharacterComboDef;

/** SQL stand-in for C# {@code sIff.findPart} during {@code initComboDef}. */
public final class CharacterComboDefSql {

    private CharacterComboDefSql() {}

    public static boolean partExists(Jdbi jdbi, int typeid) {
        return jdbi.withHandle(h -> partExists(h, typeid));
    }

    public static boolean partExists(Handle h, int typeid) {
        return h.createQuery("""
                        SELECT 1 FROM pangya.iff_part WHERE typeid = :typeid LIMIT 1
                        """)
                .bind("typeid", typeid)
                .mapTo(Integer.class)
                .findOne()
                .isPresent();
    }

    public static int[] defaultParts(Jdbi jdbi, int characterTypeid) {
        return jdbi.withHandle(h -> defaultParts(h, characterTypeid));
    }

    public static int[] defaultParts(Handle h, int characterTypeid) {
        int[] parts = new int[24];
        CharacterComboDef.apply(characterTypeid, parts, pt -> partExists(h, pt));
        return parts;
    }
}
