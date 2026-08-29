package org.pangya.game;

import org.junit.jupiter.api.Test;
import org.pangya.db.InventoryRepository.UserInfoRow;
import org.pangya.protocol.game.GamePackets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests for C# {@code UserInfo.add} / {@code requestSaveInfo} merge. */
final class UserInfoMergeTest {

    private static UserInfoRow emptyRow() {
        return new UserInfoRow(
                0L, 0L, 0L, 0L, 0f,
                0L, 0, 0L, 0L, 0L, 0, 0L, 0, 0L, 0L, 0, 0L,
                0f, 0f,
                0L, 1, 0L, 0,
                0, 0, 0, 0, 0,
                0L, 0L, 0L, 0L, 0L, 0L, 0,
                0L, 0L,
                0L, 0, 0, 0, 0, 0,
                3L, 5L,
                0, 0, 0L,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0L, 0L,
                0, 0, 0, 0, 0, 0, 0);
    }

    @Test
    void finishOptionIncrementsJogadoAndCombo() {
        UserInfoRow db = emptyRow();
        GamePackets.UserInfoEx client = new GamePackets.UserInfoEx(
                8, 4, 0, 0, 100f, 1, 0, 0, 80, 18, 0, 0, 0, 0, 0, 0, 0, 0f, 0f,
                0, 1, 0, 0, 0);
        UserInfoRow merged = UserInfoMerge.saveInfo(db, client, 0, -1, 900, false);
        assertEquals(6, merged.combos());
        assertEquals(1, merged.jogado());
        assertEquals(-1, merged.mediaScore());
        assertEquals(900, merged.tempo());
        assertEquals(8, merged.tacadas());
    }

    @Test
    void quitOptionDecreasesComboAndIncrementsQuit() {
        UserInfoRow db = emptyRow();
        GamePackets.UserInfoEx client = new GamePackets.UserInfoEx(
                10, 5, 0, 0, 120f, 2, 0, 1, 100, 9, 0, 0, 0, 0, 0, 0, 0, 0f, 0f,
                0, 1, 0, 0, 0);
        UserInfoRow merged = UserInfoMerge.saveInfo(db, client, 1, 4, 600, false);
        assertEquals(2, merged.combos());
        assertEquals(1, merged.quitado());
        assertEquals(0, merged.naoSei());
        assertEquals(1, merged.jogado());
        assertEquals(4, merged.mediaScore());
        assertEquals(600, merged.tempo());
        assertEquals(10, merged.tacadas());
        assertTrue(merged.maxDistancia() >= 120f);
    }

    @Test
    void dcOptionSkipsQuitPenalty() {
        UserInfoRow db = emptyRow();
        GamePackets.UserInfoEx client = new GamePackets.UserInfoEx(
                0, 0, 0, 0, 0f, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0f, 0f,
                0, 1, 0, 0, 0);
        UserInfoRow merged = UserInfoMerge.saveInfo(db, client, 5, 2, 120, true);
        assertEquals(5, merged.combos());
        assertEquals(0, merged.quitado());
        assertEquals(1, merged.jogado());
        assertEquals(2, merged.mediaScore());
    }

    @Test
    void mergeComboMatchesCSharpDecrease() {
        assertEquals(0, UserInfoMerge.mergeCombo(2, -3));
        assertEquals(2, UserInfoMerge.mergeCombo(5, -3));
    }
}
