package org.pangya.game;

import org.junit.jupiter.api.Test;
import org.pangya.network.AppConfig;
import org.pangya.protocol.game.GamePackets;
import org.pangya.protocol.packet.PacketReader;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameHandlerTest {

    @Test
    void loadChannelsAssignsZeroBasedIds() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("channels", List.of(
                Map.of("name", "Channel (Rookies)", "maxUser", 500),
                Map.of("name", "Channel (Geral)", "maxUser", 200)
        ));
        List<GamePackets.ChannelInfo> channels = GameHandler.loadChannels(new AppConfig(root));
        assertEquals(2, channels.size());
        assertEquals(0, channels.get(0).id);
        assertEquals("Channel (Rookies)", channels.get(0).name);
        assertEquals(500, channels.get(0).maxUser);
        assertEquals(1, channels.get(1).id);
        assertEquals(77, channels.get(0).toArray().length);
    }

    @Test
    void keyMatchesRedisOrSql() {
        assertTrue(GameHandler.keyMatches("ABCD1234", "ABCD1234", null));
        assertTrue(GameHandler.keyMatches("ABCD1234", null, "ABCD1234"));
        assertFalse(GameHandler.keyMatches("ABCD1234", "other", "nope"));
        assertFalse(GameHandler.keyMatches("", "ABCD1234", "ABCD1234"));
    }

    @Test
    void applyInfoChangeUpdatesCourse() {
        PacketReader created = new PacketReader(
                GamePackets.clientCreateRoom(GamePackets.TIPO_STROKE, "VS", ""));
        created.opcode();
        GameRoom room = new GameRoom(GamePackets.readCreateRoom(created), 7, 10001, 100, 100, 0);
        PacketReader change = new PacketReader(GamePackets.clientChangeRoomCourse(7, 5));
        change.opcode();
        assertTrue(room.applyInfoChange(change));
        assertEquals(5, room.info.course);
        assertEquals(false, room.hiddenFromLobby());
    }
}
