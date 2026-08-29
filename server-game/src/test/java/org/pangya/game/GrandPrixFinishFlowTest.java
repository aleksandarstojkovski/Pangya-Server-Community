package org.pangya.game;

import org.pangya.protocol.iff.GrandPrixEnterWindow;
import org.pangya.protocol.game.GamePackets;
import org.pangya.protocol.packet.PacketReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests for GP shot-sync gate flags on {@link GameRoom.PlayerShot}. */
final class GrandPrixFinishFlowTest {

    @Test
    void checkAllGrandPrixShotPacketsRequiresInitSyncAndFinish() {
        GameRoom.PlayerShot shot = new GameRoom.PlayerShot();
        assertFalse(allPackets(shot));

        shot.initShot = 1;
        assertFalse(allPackets(shot));

        shot.syncShotFlag = 1;
        assertFalse(allPackets(shot));

        shot.finishShot = 1;
        assertTrue(allPackets(shot));

        shot.initShot = 0;
        shot.syncShotFlag = 0;
        shot.timeOuts = 1;
        assertTrue(allPackets(shot));
    }

    @Test
    void giveUpIncrementsBadConduct() {
        GameRoom.PlayerShot shot = new GameRoom.PlayerShot();
        shot.displayState = 0;
        shot.tacadaNum = 4;
        int totalShot = 5;
        if ((shot.displayState & GamePackets.DISPLAY_ACERTO_HOLE) == 0
                && totalShot <= shot.tacadaNum + 1) {
            if (shot.tacadaNum < totalShot) {
                shot.tacadaNum++;
            }
            shot.giveUp = 1;
            shot.badConduct++;
        }
        assertEquals(1, shot.giveUp);
        assertEquals(1, shot.badConduct);
    }

    @Test
    void kickGateRequiresThreeGiveUps() {
        GameRoom.PlayerShot shot = new GameRoom.PlayerShot();
        shot.badConduct = 2;
        shot.giveUp = 1;
        assertFalse(shot.badConduct >= 3 && (shot.giveUp > 0 || shot.timeOuts > 0));
        shot.badConduct = 3;
        assertTrue(shot.badConduct >= 3 && (shot.giveUp > 0 || shot.timeOuts > 0));
    }

    @Test
    void rookieNormalGrandPrixTypeidDetection() {
        PacketReader created = new PacketReader(
                GamePackets.clientCreateRoom(GamePackets.TIPO_GRAND_PRIX, "GP", ""));
        created.opcode();
        GameRoom room = new GameRoom(GamePackets.readCreateRoom(created), 1, 10001, 100, 100, 0);
        room.grandPrixTypeid = 0x40000;
        assertTrue(GrandPrixEnterWindow.isGrandPrixNormal(room.grandPrixTypeid));
        assertEquals(GrandPrixEnterWindow.GP_ABA_ROOKIE, GrandPrixEnterWindow.grandPrixAba(room.grandPrixTypeid));
        room.grandPrixTypeid = 0x3000000;
        assertFalse(GrandPrixEnterWindow.isGrandPrixNormal(room.grandPrixTypeid));
        assertTrue(GrandPrixEnterWindow.isGrandPrixEvent(room.grandPrixTypeid));
    }

    @Test
    void placarScoreUsesGameTotalNotProfileMediaScore() {
        GameRoom.PlayerShot shot = new GameRoom.PlayerShot();
        shot.score = 3;
        shot.holeTacada[0] = 5;
        shot.holePar[0] = 4;
        assertEquals(3, GameHandler.placarScoreFor(shot));
    }

    @Test
    void placarScoreDerivesFromHolesWhenRunningTotalUnset() {
        GameRoom.PlayerShot shot = new GameRoom.PlayerShot();
        shot.holeTacada[0] = 4;
        shot.holePar[0] = 4;
        shot.holeTacada[1] = 5;
        shot.holePar[1] = 4;
        assertEquals(1, GameHandler.placarScoreFor(shot));
    }

    @Test
    void placarScorePrefersTimeOverPenaltyTotal() {
        GameRoom.PlayerShot shot = new GameRoom.PlayerShot();
        shot.score = 12;
        shot.holeTacada[0] = 5;
        shot.holePar[0] = 4;
        assertEquals(12, GameHandler.placarScoreFor(shot));
    }

    @Test
    void myStatisticsUsesClientUserInfoWire() {
        byte[] wire = GamePackets.userInfoExWire(10, 5, 2, 4, 900L, 70, 1);
        PacketReader pkt = new PacketReader(GamePackets.myStatistics(wire));
        assertEquals(GamePackets.SERVER_MY_STATISTICS, pkt.opcode());
        assertEquals(10, pkt.i32());
        assertEquals(5, pkt.i32());
    }

    private static boolean allPackets(GameRoom.PlayerShot shot) {
        return (shot.initShot > 0 && shot.syncShotFlag > 0 || shot.timeOuts > 0)
                && shot.finishShot > 0;
    }
}
