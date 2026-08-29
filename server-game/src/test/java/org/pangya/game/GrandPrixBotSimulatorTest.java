package org.pangya.game;

import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.pangya.network.session.Session;
import org.pangya.protocol.game.GamePackets;
import org.pangya.protocol.iff.GrandPrixEnterWindow;
import org.pangya.protocol.iff.PangyaIffLoader;
import org.pangya.protocol.packet.PacketReader;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GrandPrixBotSimulatorTest {

    private static final Path JP_IFF = Path.of(
            "reference/pangya-server-community/Server/JP/GameServer/data/pangya_jp.iff");

    @AfterEach
    void tearDown() {
        PangyaIffLoader.reload(null);
    }

    @Test
    void simulateFillsRoomBelowCapWhenIffPresent() throws Exception {
        if (!JP_IFF.toFile().isFile()) {
            return;
        }
        PangyaIffLoader.reload(JP_IFF);
        PacketReader created = new PacketReader(
                GamePackets.clientCreateRoom(GamePackets.TIPO_GRAND_PRIX, "GP", ""));
        GameRoom room = new GameRoom(GamePackets.readCreateRoom(created), 42, 10001, 100, 100, 0);
        room.grandPrixTypeid = 0x80101;
        room.info.holes = 9;
        room.info.course = 10;
        room.info.modo = 0;
        room.inGame = true;
        room.course = new GameCourse(room.info, new org.pangya.game.catalog.GlobalCatalogs(null, JP_IFF));
        Session member = new Session(new EmbeddedChannel(), 1, 1);
        member.player().uid = 10001;
        room.players.add(member);

        List<GamePackets.GrandPrixBot> bots = GrandPrixBotSimulator.simulate(
                room,
                room.course,
                s -> 72f);

        assertFalse(bots.isEmpty());
        assertTrue(bots.size() <= 29);
        assertTrue(GrandPrixEnterWindow.isGrandPrixNormal(room.grandPrixTypeid));
        for (GamePackets.GrandPrixBot bot : bots) {
            assertEquals(9, bot.holes().size());
        }
    }
}
