package org.pangya.db;

import org.jdbi.v3.core.statement.SqlStatement;

/** Binds {@code parts_1..parts_24} placeholders for character INSERT/UPDATE. */
final class CharacterPartsBinder {

    private CharacterPartsBinder() {}

    static void bind(SqlStatement<?> stmt, int[] parts) {
        for (int i = 0; i < 24; i++) {
            stmt.bind("p" + (i + 1), parts[i]);
        }
    }
}
