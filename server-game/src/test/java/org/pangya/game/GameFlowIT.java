package org.pangya.game;

import org.junit.jupiter.api.Test;
import org.pangya.db.DatabaseSupport;
import org.pangya.db.InventoryRepository;
import org.pangya.db.JdbiInventoryRepository;
import org.pangya.db.JdbiLoginRepository;
import org.pangya.db.LoginRepository;
import org.pangya.network.AppConfig;
import org.pangya.network.client.PangyaFakeClient;
import org.pangya.network.redis.SessionKeyStore;
import org.pangya.protocol.game.GamePackets;
import org.pangya.protocol.packet.PacketIo;
import org.pangya.protocol.packet.PacketReader;

import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameFlowIT {

    @Test
    void fakeClientLogsInEntersChannelCreatesAndLeavesPractice() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config)) {
            LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(ds));
            String loginKey = repo.generateAuthKeyLogin(10001);
            String gameKey = repo.generateAuthKeyGame(10001, 20202);
            keys.putLoginKey(10001, loginKey);
            keys.putGameKey(10001, 20202, gameKey);

            try (PangyaFakeClient client = new PangyaFakeClient()) {
                client.connect("127.0.0.1", runtime.port(), PangyaFakeClient.HelloKind.GAME);
                byte[] hello = client.awaitHello(5, TimeUnit.SECONDS);
                PacketReader helloBody = new PacketReader(PacketIo.slice(hello, 4, hello.length - 4));
                assertEquals(GamePackets.SERVER_HELLO, helloBody.opcode());
                assertEquals(1, helloBody.u8());
                assertEquals(1, helloBody.u8());
                assertEquals(client.key(), helloBody.u8());

                int wireVersion = GamePackets.xorPacketVersion(GamePackets.JP_PACKET_VERSION);
                client.sendPlain(GamePackets.clientLogin(
                        "testuser", 10001, loginKey, GamePackets.JP_CLIENT_VERSION, wireVersion, gameKey));
                List<byte[]> loginPkts = collect(client, GamePackets.LOGIN_DUMP_PACKET_COUNT, 8, TimeUnit.SECONDS);
                assertEquals(GamePackets.LOGIN_DUMP_PACKET_COUNT, loginPkts.size(),
                        "expected JP sendCompleteData prefix + tail");

                PacketReader newMail = new PacketReader(loginPkts.get(0));
                assertEquals(GamePackets.SERVER_NEW_MAIL, newMail.opcode());
                assertEquals(0, newMail.i32());
                assertEquals(0, newMail.i32());

                PacketReader ack = new PacketReader(loginPkts.get(1));
                assertEquals(GamePackets.SERVER_LOGIN_ACK, ack.opcode());
                assertEquals(GamePackets.ACK_LOGIN_OK, ack.u8());
                assertEquals(GamePackets.PRINCIPAL_PAYLOAD_BYTES, ack.remaining());

                PacketReader chars = new PacketReader(loginPkts.get(2));
                assertEquals(0x70, chars.opcode());
                assertEquals(1, chars.i16());
                assertEquals(1, chars.i16());
                assertEquals(GamePackets.CHARACTER_INFO_BYTES, chars.remaining());

                assertEquals(0x71, new PacketReader(loginPkts.get(3)).opcode());
                PacketReader warehouse = new PacketReader(loginPkts.get(4));
                assertEquals(0x73, warehouse.opcode());
                assertEquals(2, warehouse.u16());
                assertEquals(2, warehouse.u16());
                assertEquals(2 * GamePackets.WAREHOUSE_ITEM_BYTES, warehouse.remaining());

                assertEquals(0xE1, new PacketReader(loginPkts.get(5)).opcode());
                PacketReader equip = new PacketReader(loginPkts.get(6));
                assertEquals(0x72, equip.opcode());
                assertEquals(GamePackets.USER_EQUIP_BYTES, equip.remaining());

                PacketReader channels = new PacketReader(loginPkts.get(7));
                assertEquals(GamePackets.SERVER_CHANNEL_LIST, channels.opcode());
                assertEquals(2, channels.u8());
                assertEquals(2 * 77, channels.remaining());

                client.sendPlain(GamePackets.clientEnterChannel(0));
                PacketReader entered = new PacketReader(client.awaitPlain(5, TimeUnit.SECONDS));
                assertEquals(GamePackets.SERVER_CHANNEL_ENTER_ACK, entered.opcode());
                assertEquals(GamePackets.CHANNEL_ENTER_OK, entered.u8());

                client.sendPlain(GamePackets.clientLobbyItem(GamePackets.ITEM_CHARACTER, 1));
                PacketReader lobbyItem = awaitOpcode(client, GamePackets.SERVER_ROOM_USER_INFO_CHANGED);
                assertEquals(0, lobbyItem.i32());
                assertEquals(GamePackets.ITEM_CHARACTER, lobbyItem.u8());
                assertTrue(lobbyItem.i32() > 0);
                assertEquals(GamePackets.CHARACTER_INFO_BYTES, lobbyItem.remaining());

                client.sendPlain(GamePackets.clientCreatePractice("practice", "secret"));
                PacketReader room = awaitOpcode(client, GamePackets.SERVER_ROOM_ENTER_RESULT);
                assertEquals(0, room.i16());
                assertEquals(GamePackets.ROOM_INFO_BYTES, room.remaining());
                byte[] practiceInfo = room.readBytes(GamePackets.ROOM_INFO_BYTES);
                assertEquals("Single Player Practice Mode", nameFromRoomInfo(practiceInfo));
                int practiceNum = roomNumberFromInfo(practiceInfo);
                assertTrue(practiceNum >= 1);

                client.sendPlain(GamePackets.clientRoomItem(GamePackets.ITEM_CHARACTER, 1));
                PacketReader roomItem = awaitOpcode(client, GamePackets.SERVER_ROOM_USER_INFO_CHANGED);
                assertEquals(0, roomItem.i32());
                assertEquals(GamePackets.ITEM_CHARACTER, roomItem.u8());
                assertTrue(roomItem.i32() > 0);
                assertEquals(GamePackets.CHARACTER_INFO_BYTES, roomItem.remaining());

                client.sendPlain(GamePackets.clientStartGame());
                PacketReader start1 = awaitOpcode(client, GamePackets.SERVER_START_GAME_FLAG);
                PacketReader start2 = awaitOpcode(client, GamePackets.SERVER_START_GAME_FLAG2);
                PacketReader rate = new PacketReader(client.awaitPlain(5, TimeUnit.SECONDS));
                assertEquals(GamePackets.SERVER_PANG_RATE, rate.opcode());
                assertEquals(100, rate.u32());

                PacketReader init = new PacketReader(client.awaitPlain(5, TimeUnit.SECONDS));
                assertEquals(GamePackets.SERVER_GAME_INIT, init.opcode());
                assertEquals(GamePackets.TIPO_TOURNEY, init.u8());
                assertEquals(1, init.u32());
                assertEquals(16, init.remaining()); // SYSTEMTIME

                PacketReader course = new PacketReader(client.awaitPlain(5, TimeUnit.SECONDS));
                assertEquals(GamePackets.SERVER_COURSE, course.opcode());
                assertEquals(0, course.u8()); // course from clientCreatePractice
                assertEquals(GamePackets.TIPO_TOURNEY, course.u8());
                assertEquals(0, course.u8()); // modo
                assertEquals(18, course.u8());
                course.u32();
                course.u32();
                course.u32();
                for (int n = 1; n <= GamePackets.COURSE_HOLE_COUNT; n++) {
                    assertEquals(n, course.u32());
                    assertEquals((n - 1) % 3, course.u8());
                    assertEquals(0, course.u8());
                    assertEquals(n, course.u8());
                }
                assertEquals(GameCourse.SEED, course.u32());
                for (int n = 0; n < GamePackets.COURSE_HOLE_COUNT; n++) {
                    assertEquals(0, course.u8());
                }
                assertEquals(0, course.remaining());

                byte[] roomKey = new byte[16];
                System.arraycopy(practiceInfo, 69, roomKey, 0, 16);
                client.sendPlain(GamePackets.clientInitHole(1, 0, 0, 4, 1.5f, 2.5f, 10f, 20f));
                PacketReader weather = new PacketReader(client.awaitPlain(5, TimeUnit.SECONDS));
                assertEquals(GamePackets.SERVER_WEATHER, weather.opcode());
                assertEquals(0, weather.u16());
                assertEquals(0, weather.u8());
                PacketReader wind = new PacketReader(client.awaitPlain(5, TimeUnit.SECONDS));
                assertEquals(GamePackets.SERVER_WIND, wind.opcode());
                assertEquals(0, wind.u8());
                assertEquals(0, wind.u8());
                assertEquals(0, wind.u16());
                assertEquals(1, wind.u8());
                PacketReader remain = new PacketReader(client.awaitPlain(5, TimeUnit.SECONDS));
                assertEquals(GamePackets.SERVER_REMAIN_TIME, remain.opcode());

                byte[] shotBody = GamePackets.shotEndLocationSample();
                client.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_SHOT_END));
                client.sendPlain(GamePackets.clientShotEnd(shotBody));
                PacketReader shotEnd = awaitOpcode(client, GamePackets.SERVER_SHOT_END);
                assertShotEnd(shotEnd, oidOf(runtime, 10001), 1, shotBody);

                client.sendPlain(GamePackets.clientCamera(1.25f));
                PacketReader mira = awaitOpcode(client, GamePackets.SERVER_CAMERA);
                assertTrue(mira.i32() > 0);
                assertEquals(1.25f, mira.f32());
                client.sendPlain(GamePackets.clientClub(3));
                PacketReader club = awaitOpcode(client, GamePackets.SERVER_CLUB);
                assertTrue(club.i32() > 0);
                assertEquals(3, club.u8());
                client.sendPlain(GamePackets.clientPowerShot(1));
                PacketReader ps = awaitOpcode(client, GamePackets.SERVER_POWER_SHOT);
                assertTrue(ps.i32() > 0);
                assertEquals(1, ps.u8());
                client.sendPlain(GamePackets.clientEmoticon(1));
                PacketReader typing = awaitOpcode(client, GamePackets.SERVER_TYPING);
                typing.i32();
                assertEquals(1, typing.i16());
                client.sendPlain(GamePackets.clientDrop(1f, 2f, 3f));
                PacketReader drop = awaitOpcode(client, GamePackets.SERVER_MOVE_BALL);
                assertEquals(1f, drop.f32());
                assertEquals(2f, drop.f32());
                assertEquals(3f, drop.f32());
                client.sendPlain(GamePackets.clientClick(1, 0.5f));
                client.sendPlain(GamePackets.clientTimeCheck());

                client.sendPlain(GamePackets.clientLoadOk());
                client.sendPlain(GamePackets.clientShot());
                byte[] shotPlain = GamePackets.shotSyncPlain(
                        1, 1.5f, 0f, 2.5f, 2, 0, 0, 0, 0, 0, 0x11, 100, 0);
                client.sendPlain(GamePackets.clientShotResult(GamePackets.xorRoomKey(shotPlain, roomKey)));
                PacketReader sync = new PacketReader(client.awaitPlain(5, TimeUnit.SECONDS));
                assertEquals(GamePackets.SERVER_SYNC_SHOT, sync.opcode());
                assertEquals(1, sync.i32());
                assertEquals(1, sync.u8());
                assertEquals(1.5f, sync.f32());
                assertEquals(2.5f, sync.f32());

                client.sendPlain(GamePackets.clientShotAck());
                PacketReader end = new PacketReader(client.awaitPlain(5, TimeUnit.SECONDS));
                assertEquals(GamePackets.SERVER_END_SHOT, end.opcode());
                assertEquals(1, end.i32());
                assertEquals(0, end.u8());

                client.sendPlain(GamePackets.clientHoleStat());
                client.sendPlain(GamePackets.clientFinishGame());
                PacketReader prizes = awaitOpcode(client, GamePackets.SERVER_PRIZE_LIST);
                assertEquals(0, prizes.u8());
                assertEquals(0, prizes.u16());
                PacketReader result = awaitOpcode(client, GamePackets.SERVER_GAME_RESULT);
                assertEquals(0, result.i32());
                result.u32();
                result.u8();
                assertEquals(2, result.u8());
                PacketReader stats = awaitOpcode(client, GamePackets.SERVER_MY_STATISTICS);
                assertEquals(GamePackets.USER_INFO_BYTES + GamePackets.TROPHY_BYTES
                        + GamePackets.MAP_STATISTICS_EMPTY_BYTES, stats.remaining());
                PacketReader treasure = awaitOpcode(client, GamePackets.SERVER_UPDATE_TREASURE_GIFT_LIST);
                assertEquals(0, treasure.u8());
                PacketReader pang = awaitOpcode(client, GamePackets.SERVER_PANG_SPENT);
                pang.u64();
                assertEquals(0, pang.u64());

                client.sendPlain(GamePackets.clientEquipCharacter(1));
                PacketReader equipped = awaitOpcode(client, GamePackets.SERVER_EQUIP_ACK);
                assertEquals(GamePackets.EQUIP_OK, equipped.u8());
                assertEquals(5, equipped.u8());
                assertEquals(1, equipped.i32());

                GamePackets.CharacterInfo parts = new GamePackets.CharacterInfo();
                parts.id = 1;
                parts.typeid = GamePackets.TYPEID_NURI;
                client.sendPlain(GamePackets.clientEquipParts(parts));
                PacketReader partsAck = awaitOpcode(client, GamePackets.SERVER_EQUIP_ACK);
                assertEquals(GamePackets.EQUIP_OK, partsAck.u8());
                assertEquals(0, partsAck.u8());
                assertEquals(GamePackets.CHARACTER_INFO_BYTES, partsAck.remaining());

                client.sendPlain(GamePackets.clientBuyItem());
                PacketReader buy = new PacketReader(client.awaitPlain(5, TimeUnit.SECONDS));
                assertEquals(GamePackets.SERVER_BUY_ACK, buy.opcode());
                assertEquals(GamePackets.BUY_FAIL_GENERIC, buy.u32());

                client.sendPlain(GamePackets.clientLeavePractice());
                client.drainPlain(200);

                client.sendPlain(GamePackets.clientCreateRoom(GamePackets.TIPO_STROKE, "VS", ""));
                PacketReader stroke = awaitOpcode(client, GamePackets.SERVER_ROOM_ENTER_RESULT);
                assertEquals(0, stroke.i16());
                assertEquals(GamePackets.ROOM_INFO_BYTES, stroke.remaining());
                byte[] strokeInfo = stroke.readBytes(GamePackets.ROOM_INFO_BYTES);
                assertEquals("VS", nameFromRoomInfo(strokeInfo));
                assertTrue(roomNumberFromInfo(strokeInfo) >= 1);

                client.sendPlain(GamePackets.clientStartGame());
                PacketReader denied = awaitOpcode(client, GamePackets.SERVER_START_GAME_FAIL);
                assertEquals(GamePackets.START_GAME_NOT_READY, denied.u32());
            }

            assertTrue(awaitSessionCount(runtime, 0, 5, TimeUnit.SECONDS), "kill session must drop the player");

            try (PangyaFakeClient client = new PangyaFakeClient()) {
                String loginKey2 = repo.generateAuthKeyLogin(10001);
                String gameKey2 = repo.generateAuthKeyGame(10001, 20202);
                keys.putLoginKey(10001, loginKey2);
                keys.putGameKey(10001, 20202, gameKey2);
                client.connect("127.0.0.1", runtime.port(), PangyaFakeClient.HelloKind.GAME);
                client.awaitHello(5, TimeUnit.SECONDS);
                client.sendPlain(GamePackets.clientLogin(
                        "testuser",
                        10001,
                        loginKey2,
                        GamePackets.JP_CLIENT_VERSION,
                        GamePackets.xorPacketVersion(GamePackets.JP_PACKET_VERSION),
                        gameKey2));
                List<byte[]> again = collect(client, GamePackets.LOGIN_DUMP_PACKET_COUNT, 8, TimeUnit.SECONDS);
                assertEquals(GamePackets.SERVER_NEW_MAIL, new PacketReader(again.get(0)).opcode());
                assertEquals(GamePackets.SERVER_LOGIN_ACK, new PacketReader(again.get(1)).opcode());
                assertEquals(0x70, new PacketReader(again.get(2)).opcode());
                assertEquals(GamePackets.SERVER_CHANNEL_LIST, new PacketReader(again.get(7)).opcode());
                assertEquals(0xF1, new PacketReader(again.get(12)).opcode());
                assertEquals(0x25D, new PacketReader(again.get(27)).opcode());
            }
        }
    }

    @Test
    void twoPlayersStartStrokeAndReceiveVersusDump() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient host = new PangyaFakeClient();
             PangyaFakeClient guest = new PangyaFakeClient()) {
            LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(ds));
            String hostLogin = repo.generateAuthKeyLogin(10001);
            String hostGame = repo.generateAuthKeyGame(10001, 20202);
            String guestLogin = repo.generateAuthKeyLogin(10002);
            String guestGame = repo.generateAuthKeyGame(10002, 20202);
            keys.putLoginKey(10001, hostLogin);
            keys.putGameKey(10001, 20202, hostGame);
            keys.putLoginKey(10002, guestLogin);
            keys.putGameKey(10002, 20202, guestGame);

            loginToChannel(host, runtime.port(), "testuser", 10001, hostLogin, hostGame);
            loginToChannel(guest, runtime.port(), "testuser2", 10002, guestLogin, guestGame);

            host.sendPlain(GamePackets.clientCreateRoom(GamePackets.TIPO_STROKE, "VS2", ""));
            PacketReader created = awaitOpcode(host, GamePackets.SERVER_ROOM_ENTER_RESULT);
            assertEquals(0, created.i16());
            byte[] info = created.readBytes(GamePackets.ROOM_INFO_BYTES);
            int numero = roomNumberFromInfo(info);

            guest.sendPlain(GamePackets.clientJoinRoom(numero, ""));
            PacketReader joined = awaitOpcode(guest, GamePackets.SERVER_ROOM_ENTER_RESULT);
            assertEquals(0, joined.i16());

            host.sendPlain(GamePackets.clientStartGame());
            awaitOpcode(host, GamePackets.SERVER_START_GAME_FLAG);
            awaitOpcode(host, GamePackets.SERVER_START_GAME_FLAG2);
            awaitOpcode(host, GamePackets.SERVER_PANG_RATE);
            PacketReader init = awaitOpcode(host, GamePackets.SERVER_GAME_INIT);
            assertEquals(GamePackets.TIPO_STROKE, init.u8());
            assertEquals(2, init.u8());
            assertTrue(init.remaining() > GamePackets.MEMBER_INFO_EX_BYTES);
            PacketReader course = awaitOpcode(host, GamePackets.SERVER_COURSE);
            assertEquals(0, course.u8());
            PacketReader seed = awaitOpcode(host, GamePackets.SERVER_MASCOT_SEED);
            assertEquals(GameCourse.SEED, seed.u32());

            PacketReader guestInit = awaitOpcode(guest, GamePackets.SERVER_GAME_INIT);
            assertEquals(GamePackets.TIPO_STROKE, guestInit.u8());
            assertEquals(2, guestInit.u8());

            host.sendPlain(GamePackets.clientLoadPercent(50));
            PacketReader load = awaitOpcode(host, GamePackets.SERVER_LOAD_PERCENT);
            assertTrue(load.i32() > 0);
            assertEquals(50, load.u8());
            PacketReader guestLoad = awaitOpcode(guest, GamePackets.SERVER_LOAD_PERCENT);
            assertTrue(guestLoad.i32() > 0);
            assertEquals(50, guestLoad.u8());

            host.sendPlain(GamePackets.clientInitHole(1, 0, 0, 4, 1.5f, 2.5f, 10f, 20f));
            guest.sendPlain(GamePackets.clientInitHole(1, 0, 0, 4, 1.5f, 2.5f, 10f, 20f));
            host.sendPlain(GamePackets.clientLoadOk());
            guest.sendPlain(GamePackets.clientLoadOk());
            PacketReader weather = awaitOpcode(host, GamePackets.SERVER_WEATHER);
            assertEquals(0, weather.u16());
            assertEquals(0, weather.u8());
            PacketReader wind = awaitOpcode(host, GamePackets.SERVER_WIND);
            assertEquals(0, wind.u8());
            assertEquals(0, wind.u8());
            assertEquals(0, wind.u16());
            assertEquals(1, wind.u8());
            PacketReader holeTurn = awaitOpcode(host, GamePackets.SERVER_HOLE_TURN);
            int firstOid = holeTurn.i32();
            assertTrue(firstOid > 0);
            host.sendPlain(GamePackets.clientContinueVersus(GamePackets.CONTINUE_GO));
            PacketReader turnWind = awaitOpcode(host, GamePackets.SERVER_WIND);
            assertEquals(0, turnWind.u8());
            PacketReader playerTurn = awaitOpcode(host, GamePackets.SERVER_PLAYER_TURN);
            int nextOid = playerTurn.i32();
            assertTrue(nextOid > 0);
            assertTrue(nextOid != firstOid);

            host.sendPlain(GamePackets.clientCamera(0.5f));
            PacketReader mira = awaitOpcode(guest, GamePackets.SERVER_CAMERA);
            mira.i32();
            assertEquals(0.5f, mira.f32());
            host.sendPlain(GamePackets.clientPause(GamePackets.PAUSE_PAUSE));
            PacketReader paused = awaitOpcode(guest, GamePackets.SERVER_PAUSE);
            assertTrue(paused.i32() > 0);
            assertEquals(GamePackets.PAUSE_PAUSE, paused.u8());
            host.sendPlain(GamePackets.clientPause(GamePackets.PAUSE_RESUME));
            PacketReader resumed = awaitOpcode(guest, GamePackets.SERVER_PAUSE);
            resumed.i32();
            assertEquals(GamePackets.PAUSE_RESUME, resumed.u8());

            host.sendPlain(GamePackets.clientTeamFinishHole(9));
            host.sendPlain(GamePackets.clientReport());
            PacketReader reportOk = awaitOpcode(host, GamePackets.SERVER_REPORT);
            assertEquals(GamePackets.REPORT_OK, reportOk.u8());
            host.sendPlain(GamePackets.clientReport());
            PacketReader reportAgain = awaitOpcode(host, GamePackets.SERVER_REPORT);
            assertEquals(GamePackets.REPORT_ALREADY, reportAgain.u8());

            host.sendPlain(GamePackets.clientChatPenalty(1));
            PacketReader block = awaitOpcode(guest, GamePackets.SERVER_CHAT_PENALITY);
            assertTrue(block.i32() > 0);
            assertEquals(1, block.u8());
            host.sendPlain(GamePackets.clientSpeedRate(1.5f));
            PacketReader boost = awaitOpcode(guest, GamePackets.SERVER_SPEED_RATE);
            assertEquals(1.5f, boost.f32());
            assertTrue(boost.i32() > 0);

            host.sendPlain(GamePackets.clientShotArrows(1, 2, 3));
            host.sendPlain(GamePackets.clientReplay(0));

            host.sendPlain(GamePackets.clientTeeshotReady());
            guest.sendPlain(GamePackets.clientTeeshotReady());
            PacketReader teeshot = awaitOpcode(host, GamePackets.SERVER_TEESHOT_READY_ACK);
            assertEquals(0, teeshot.remaining());
            PacketReader guestTeeshot = awaitOpcode(guest, GamePackets.SERVER_TEESHOT_READY_ACK);
            assertEquals(0, guestTeeshot.remaining());

            host.sendPlain(GamePackets.clientEndStroke());
            PacketReader prizes = awaitOpcode(host, GamePackets.SERVER_PRIZE_LIST);
            assertEquals(0, prizes.u8());
            assertEquals(0, prizes.u16());
            awaitOpcode(host, GamePackets.SERVER_GAME_RESULT);
            awaitOpcode(host, GamePackets.SERVER_MY_STATISTICS);
            awaitOpcode(host, GamePackets.SERVER_UPDATE_TREASURE_GIFT_LIST);
            awaitOpcode(host, GamePackets.SERVER_PANG_SPENT);
            PacketReader wait = awaitOpcode(guest, GamePackets.SERVER_ROOM_UPDATE);
            assertEquals(-1, wait.i16());
            assertEquals(GamePackets.TIPO_STROKE, wait.u8());
        }
    }

    @Test
    void versusTurnTimeoutBroadcastsPacote5C() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient host = new PangyaFakeClient();
             PangyaFakeClient guest = new PangyaFakeClient()) {
            loginTwoPlayers(ds, keys, host, guest, runtime.port());
            host.sendPlain(GamePackets.clientCreateRoom(GamePackets.TIPO_STROKE, "VS-T", "", 250, 0));
            PacketReader created = awaitOpcode(host, GamePackets.SERVER_ROOM_ENTER_RESULT);
            assertEquals(0, created.i16());
            int numero = roomNumberFromInfo(created.readBytes(GamePackets.ROOM_INFO_BYTES));
            guest.sendPlain(GamePackets.clientJoinRoom(numero, ""));
            assertEquals(0, awaitOpcode(guest, GamePackets.SERVER_ROOM_ENTER_RESULT).i16());

            host.sendPlain(GamePackets.clientStartGame());
            awaitOpcode(host, GamePackets.SERVER_START_GAME_FLAG);
            awaitOpcode(host, GamePackets.SERVER_START_GAME_FLAG2);
            awaitOpcode(host, GamePackets.SERVER_PANG_RATE);
            awaitOpcode(host, GamePackets.SERVER_GAME_INIT);
            awaitOpcode(host, GamePackets.SERVER_COURSE);
            awaitOpcode(host, GamePackets.SERVER_MASCOT_SEED);
            awaitOpcode(guest, GamePackets.SERVER_GAME_INIT);

            host.sendPlain(GamePackets.clientInitHole(1, 0, 0, 4, 1.5f, 2.5f, 10f, 20f));
            guest.sendPlain(GamePackets.clientInitHole(1, 0, 0, 4, 1.5f, 2.5f, 10f, 20f));
            host.sendPlain(GamePackets.clientLoadOk());
            guest.sendPlain(GamePackets.clientLoadOk());
            awaitOpcode(host, GamePackets.SERVER_WEATHER);
            awaitOpcode(host, GamePackets.SERVER_WIND);
            awaitOpcode(host, GamePackets.SERVER_HOLE_TURN);
            host.sendPlain(GamePackets.clientContinueVersus(GamePackets.CONTINUE_GO));
            awaitOpcode(host, GamePackets.SERVER_WIND);
            awaitOpcode(host, GamePackets.SERVER_PLAYER_TURN);

            host.sendPlain(GamePackets.clientClick(1, 0.5f));
            host.sendPlain(GamePackets.clientTimeCheck());
            PacketReader timed = awaitOpcode(host, GamePackets.SERVER_TIMEOUT);
            assertTrue(timed.i32() > 0);
            host.sendPlain(GamePackets.clientClick(0, 0f));
            PacketReader clickTimeout = awaitOpcode(host, GamePackets.SERVER_TIMEOUT);
            assertTrue(clickTimeout.i32() > 0);
        }
    }

    @Test
    void gmVisibleBroadcastsLobbyUpdate() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient host = new PangyaFakeClient()) {
            LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(ds));
            repo.setCapability(10001, GamePackets.CAPABILITY_GM);
            try {
                String loginKey = repo.generateAuthKeyLogin(10001);
                String gameKey = repo.generateAuthKeyGame(10001, 20202);
                keys.putLoginKey(10001, loginKey);
                keys.putGameKey(10001, 20202, gameKey);
                loginToChannel(host, runtime.port(), "testuser", 10001, loginKey, gameKey);
                host.sendPlain(GamePackets.clientGmVisible(1));
                PacketReader update = awaitOpcode(host, GamePackets.SERVER_USERLIST);
                assertEquals(GamePackets.LOBBY_USER_UPDATE, update.u8());
                assertEquals(1, update.u8());
                assertEquals(10001, update.u32());
                update.i32();
                update.u16();
                update.readBytes(22);
                update.u8();
                assertEquals(GamePackets.CAPABILITY_GM, update.i32());
                PacketReader chat = awaitOpcode(host, GamePackets.SERVER_CHAT);
                assertEquals(GamePackets.CHAT_NOTICE, chat.u8());
                assertEquals("TestNick", chat.pstr());
                assertEquals(
                        GamePackets.chatColor(GamePackets.CHAT_GREEN_HEX, GamePackets.GM_CMD_OK),
                        chat.pstr());
                host.sendPlain(GamePackets.clientGmVisible(0));
                PacketReader hidden = awaitOpcode(host, GamePackets.SERVER_USERLIST);
                assertEquals(GamePackets.LOBBY_USER_UPDATE, hidden.u8());
                hidden.u8();
                hidden.u32();
                hidden.i32();
                hidden.u16();
                hidden.readBytes(22);
                hidden.u8();
                assertEquals(0, hidden.i32());
                PacketReader hiddenChat = awaitOpcode(host, GamePackets.SERVER_CHAT);
                assertEquals(GamePackets.CHAT_NOTICE, hiddenChat.u8());
                hiddenChat.pstr();
                assertEquals(
                        GamePackets.chatColor(GamePackets.CHAT_GREEN_HEX, GamePackets.GM_CMD_OK),
                        hiddenChat.pstr());

                host.sendPlain(GamePackets.clientGmU16(GamePackets.GM_CMD_WHISPER, 1));
                PacketReader whisper = awaitOpcode(host, GamePackets.SERVER_CHAT);
                assertEquals(
                        GamePackets.chatColor(GamePackets.CHAT_GREEN_HEX, GamePackets.GM_CMD_OK),
                        skipChatNotice(whisper));
                host.sendPlain(GamePackets.clientGmU16(GamePackets.GM_CMD_CHANNEL, 1));
                PacketReader channel = awaitOpcode(host, GamePackets.SERVER_CHAT);
                assertEquals(
                        GamePackets.chatColor(GamePackets.CHAT_GREEN_HEX, GamePackets.GM_CMD_OK),
                        skipChatNotice(channel));
                host.sendPlain(GamePackets.clientGmWeather(1));
                PacketReader weatherFail = awaitOpcode(host, GamePackets.SERVER_CHAT);
                assertEquals(
                        GamePackets.chatColor(GamePackets.CHAT_RED_HEX, GamePackets.GM_CMD_FAIL),
                        skipChatNotice(weatherFail));
                host.sendPlain(GamePackets.clientCreateRoom(GamePackets.TIPO_LOUNGE, "WX", ""));
                PacketReader lounge = awaitOpcode(host, GamePackets.SERVER_ROOM_ENTER_RESULT);
                assertEquals(0, lounge.i16());
                host.sendPlain(GamePackets.clientGmWeather(1));
                PacketReader weather = awaitOpcode(host, GamePackets.SERVER_WEATHER);
                assertEquals(1, weather.u16());
                assertEquals(GamePackets.WEATHER_GM, weather.u8());
                PacketReader weatherOk = awaitOpcode(host, GamePackets.SERVER_CHAT);
                assertEquals(
                        GamePackets.chatColor(GamePackets.CHAT_GREEN_HEX, GamePackets.GM_CMD_OK),
                        skipChatNotice(weatherOk));
                host.sendPlain(GamePackets.clientGmVisible(1));
                PacketReader shown = awaitOpcode(host, GamePackets.SERVER_USERLIST);
                assertEquals(GamePackets.LOBBY_USER_UPDATE, shown.u8());
                awaitOpcode(host, GamePackets.SERVER_CHAT);
                host.sendPlain(GamePackets.clientGmIdentity(
                        GamePackets.CAPABILITY_GM_NORMAL, "TestNick"));
                PacketReader ident = awaitOpcode(host, GamePackets.SERVER_ADMIT_IDENTITY);
                assertEquals(GamePackets.CAPABILITY_GM_NORMAL, ident.i32());
                PacketReader identLobby = awaitOpcode(host, GamePackets.SERVER_USERLIST);
                assertEquals(GamePackets.CAPABILITY_GM_NORMAL, lobbyCapability(identLobby));
                PacketReader identOk = awaitOpcode(host, GamePackets.SERVER_CHAT);
                assertEquals(
                        GamePackets.chatColor(GamePackets.CHAT_GREEN_HEX, GamePackets.GM_CMD_OK),
                        skipChatNotice(identOk));
                host.sendPlain(GamePackets.clientGmIdentity(-1, "TestNick"));
                PacketReader identFail = awaitOpcode(host, GamePackets.SERVER_CHAT);
                assertEquals(
                        GamePackets.chatColor(GamePackets.CHAT_RED_HEX, GamePackets.GM_CMD_FAIL),
                        skipChatNotice(identFail));
                host.sendPlain(GamePackets.clientIdentity(-1, "TestNick"));
                PacketReader restored = awaitOpcode(host, GamePackets.SERVER_ADMIT_IDENTITY);
                assertEquals(
                        GamePackets.CAPABILITY_GM | GamePackets.CAPABILITY_TITLE_GM,
                        restored.i32());
                PacketReader restLobby = awaitOpcode(host, GamePackets.SERVER_USERLIST);
                assertEquals(
                        GamePackets.CAPABILITY_GM | GamePackets.CAPABILITY_TITLE_GM,
                        lobbyCapability(restLobby));
                host.sendPlain(GamePackets.clientGmIdentity(
                        GamePackets.CAPABILITY_GM_NORMAL, "WrongNick"));
                PacketReader nickFail = awaitOpcode(host, GamePackets.SERVER_CHAT);
                assertEquals(
                        GamePackets.chatColor(GamePackets.CHAT_RED_HEX, GamePackets.GM_CMD_FAIL),
                        skipChatNotice(nickFail));
                host.sendPlain(GamePackets.clientGmCommand(GamePackets.GM_CMD_DESTROY));
                PacketReader destroyOk = awaitOpcode(host, GamePackets.SERVER_CHAT);
                assertEquals(
                        GamePackets.chatColor(GamePackets.CHAT_GREEN_HEX, GamePackets.GM_CMD_OK),
                        skipChatNotice(destroyOk));
            } finally {
                repo.setCapability(10001, 0);
            }
        }
    }

    @Test
    void gmGiveitemGoldenbellAndLoungeWindFail() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient host = new PangyaFakeClient();
             PangyaFakeClient guest = new PangyaFakeClient()) {
            LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(ds));
            repo.setCapability(10001, GamePackets.CAPABILITY_GM);
            try {
                loginTwoPlayers(ds, keys, host, guest, runtime.port());
                int guestOid = oidOf(runtime, 10002);
                host.sendPlain(GamePackets.clientGmGiveitem(
                        guestOid, GamePackets.TYPEID_SHOP_PANG_ITEM, 1));
                PacketReader mail = awaitOpcode(guest, GamePackets.SERVER_NEW_MAIL);
                assertEquals(0, mail.i32());
                assertEquals(1, mail.i32());
                PacketReader giveOk = awaitOpcode(host, GamePackets.SERVER_CHAT);
                assertEquals(
                        GamePackets.chatColor(GamePackets.CHAT_GREEN_HEX, GamePackets.GM_CMD_OK),
                        skipChatNotice(giveOk));
                host.sendPlain(GamePackets.clientGmGiveitem(guestOid, 0, 1));
                PacketReader typeidFail = awaitOpcode(host, GamePackets.SERVER_CHAT);
                assertEquals(
                        GamePackets.chatColor(GamePackets.CHAT_RED_HEX, GamePackets.GM_CMD_FAIL),
                        skipChatNotice(typeidFail));
                host.sendPlain(GamePackets.clientGmGiveitem(
                        guestOid, GamePackets.TYPEID_SHOP_PANG_ITEM, GamePackets.GM_GIVEITEM_MAX + 1));
                PacketReader qntdFail = awaitOpcode(host, GamePackets.SERVER_CHAT);
                assertEquals(
                        GamePackets.chatColor(GamePackets.CHAT_RED_HEX, GamePackets.GM_CMD_FAIL),
                        skipChatNotice(qntdFail));
                host.sendPlain(GamePackets.clientGmGiveitem(guestOid, 0x7FFF0001, 1));
                PacketReader iffFail = awaitOpcode(host, GamePackets.SERVER_CHAT);
                assertEquals(
                        GamePackets.chatColor(GamePackets.CHAT_RED_HEX, GamePackets.GM_CMD_FAIL),
                        skipChatNotice(iffFail));

                host.sendPlain(GamePackets.clientCreateRoom(GamePackets.TIPO_LOUNGE, "GB", ""));
                PacketReader lounge = awaitOpcode(host, GamePackets.SERVER_ROOM_ENTER_RESULT);
                assertEquals(0, lounge.i16());
                int numero = roomNumberFromInfo(lounge.readBytes(GamePackets.ROOM_INFO_BYTES));
                guest.sendPlain(GamePackets.clientJoinRoom(numero, ""));
                assertEquals(0, awaitOpcode(guest, GamePackets.SERVER_ROOM_ENTER_RESULT).i16());
                host.sendPlain(GamePackets.clientGmGoldenbell(GamePackets.TYPEID_SHOP_PANG_ITEM, 1));
                PacketReader hostMail = awaitOpcode(host, GamePackets.SERVER_NEW_MAIL);
                assertEquals(0, hostMail.i32());
                assertTrue(hostMail.i32() >= 1);
                PacketReader guestMail = awaitOpcode(guest, GamePackets.SERVER_NEW_MAIL);
                assertEquals(0, guestMail.i32());
                assertTrue(guestMail.i32() >= 1);
                PacketReader bellOk = awaitOpcode(host, GamePackets.SERVER_CHAT);
                assertEquals(
                        GamePackets.chatColor(GamePackets.CHAT_GREEN_HEX, GamePackets.GM_CMD_OK),
                        skipChatNotice(bellOk));
                host.sendPlain(GamePackets.clientGmWind(5, 90));
                PacketReader windFail = awaitOpcode(host, GamePackets.SERVER_CHAT);
                assertEquals(
                        GamePackets.chatColor(GamePackets.CHAT_RED_HEX, GamePackets.GM_CMD_FAIL),
                        skipChatNotice(windFail));
            } finally {
                repo.setCapability(10001, 0);
            }
        }
    }

    @Test
    void gmVersusWindBroadcastsPacote05B() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient host = new PangyaFakeClient();
             PangyaFakeClient guest = new PangyaFakeClient()) {
            LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(ds));
            repo.setCapability(10001, GamePackets.CAPABILITY_GM);
            try {
                loginTwoPlayers(ds, keys, host, guest, runtime.port());
                host.sendPlain(GamePackets.clientCreateRoom(GamePackets.TIPO_STROKE, "VS-W", ""));
                PacketReader created = awaitOpcode(host, GamePackets.SERVER_ROOM_ENTER_RESULT);
                assertEquals(0, created.i16());
                int numero = roomNumberFromInfo(created.readBytes(GamePackets.ROOM_INFO_BYTES));
                guest.sendPlain(GamePackets.clientJoinRoom(numero, ""));
                assertEquals(0, awaitOpcode(guest, GamePackets.SERVER_ROOM_ENTER_RESULT).i16());
                host.sendPlain(GamePackets.clientStartGame());
                awaitOpcode(host, GamePackets.SERVER_START_GAME_FLAG);
                awaitOpcode(host, GamePackets.SERVER_START_GAME_FLAG2);
                awaitOpcode(host, GamePackets.SERVER_PANG_RATE);
                awaitOpcode(host, GamePackets.SERVER_GAME_INIT);
                awaitOpcode(host, GamePackets.SERVER_COURSE);
                awaitOpcode(host, GamePackets.SERVER_MASCOT_SEED);
                awaitOpcode(guest, GamePackets.SERVER_GAME_INIT);
                host.sendPlain(GamePackets.clientInitHole(1, 0, 0, 4, 1.5f, 2.5f, 10f, 20f));
                guest.sendPlain(GamePackets.clientInitHole(1, 0, 0, 4, 1.5f, 2.5f, 10f, 20f));
                host.sendPlain(GamePackets.clientLoadOk());
                guest.sendPlain(GamePackets.clientLoadOk());
                awaitOpcode(host, GamePackets.SERVER_WEATHER);
                awaitOpcode(host, GamePackets.SERVER_WIND);
                awaitOpcode(host, GamePackets.SERVER_HOLE_TURN);
                host.sendPlain(GamePackets.clientGmWind(5, 90));
                PacketReader wind = awaitOpcode(host, GamePackets.SERVER_WIND);
                assertEquals(5, wind.u8());
                assertEquals(0, wind.u8());
                assertEquals(90, wind.u16());
                assertEquals(1, wind.u8());
                PacketReader windOk = awaitOpcode(host, GamePackets.SERVER_CHAT);
                assertEquals(
                        GamePackets.chatColor(GamePackets.CHAT_GREEN_HEX, GamePackets.GM_CMD_OK),
                        skipChatNotice(windOk));
                host.sendPlain(GamePackets.clientGmWeather(2));
                PacketReader holeWeather = awaitOpcode(host, GamePackets.SERVER_WEATHER);
                assertEquals(2, holeWeather.u16());
                assertEquals(GamePackets.WEATHER_GM, holeWeather.u8());
                PacketReader weatherOk = awaitOpcode(host, GamePackets.SERVER_CHAT);
                assertEquals(
                        GamePackets.chatColor(GamePackets.CHAT_GREEN_HEX, GamePackets.GM_CMD_OK),
                        skipChatNotice(weatherOk));
            } finally {
                repo.setCapability(10001, 0);
            }
        }
    }

    @Test
    void gmWhisperListSpyAndDisconnect() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient host = new PangyaFakeClient();
             PangyaFakeClient guest = new PangyaFakeClient()) {
            LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(ds));
            repo.setCapability(10001, GamePackets.CAPABILITY_GM);
            try {
                loginTwoPlayers(ds, keys, host, guest, runtime.port());
                host.sendPlain(GamePackets.clientCreateRoom(GamePackets.TIPO_LOUNGE, "SPY", ""));
                assertEquals(0, awaitOpcode(host, GamePackets.SERVER_ROOM_ENTER_RESULT).i16());
                guest.sendPlain(GamePackets.clientChat("TestNick2", "hello gm"));
                PacketReader spy = awaitOpcode(host, GamePackets.SERVER_CHAT);
                assertEquals(GamePackets.CHAT_NORMAL, spy.u8());
                assertEquals(
                        GamePackets.gmChatSpyFrom("Channel (Rookies)", 0xFFFF),
                        spy.pstr());
                assertEquals(GamePackets.gmChatSpyMsg("TestNick2", "hello gm"), spy.pstr());
                PacketReader echo = awaitOpcode(host, GamePackets.SERVER_CHAT);
                assertEquals(GamePackets.CHAT_NORMAL, echo.u8());
                assertEquals("TestNick2", echo.pstr());
                assertEquals("hello gm", echo.pstr());

                host.sendPlain(GamePackets.clientGmU16(GamePackets.GM_CMD_WHISPER, 0));
                assertEquals(
                        GamePackets.chatColor(GamePackets.CHAT_GREEN_HEX, GamePackets.GM_CMD_OK),
                        skipChatNotice(awaitOpcode(host, GamePackets.SERVER_CHAT)));
                guest.sendPlain(GamePackets.clientChat("TestNick2", "no spy"));
                PacketReader channelChat = awaitOpcode(host, GamePackets.SERVER_CHAT);
                assertEquals(GamePackets.CHAT_NORMAL, channelChat.u8());
                assertEquals("TestNick2", channelChat.pstr());
                assertEquals("no spy", channelChat.pstr());

                host.sendPlain(GamePackets.clientGmWhisperList(
                        GamePackets.GM_CMD_OPEN_WHISPER, "TestNick2"));
                assertEquals(
                        GamePackets.chatColor(GamePackets.CHAT_GREEN_HEX, GamePackets.GM_CMD_OK),
                        skipChatNotice(awaitOpcode(host, GamePackets.SERVER_CHAT)));
                guest.sendPlain(GamePackets.clientChat("TestNick2", "listed"));
                PacketReader listed = awaitOpcode(host, GamePackets.SERVER_CHAT);
                assertEquals(GamePackets.CHAT_NORMAL, listed.u8());
                assertEquals(
                        GamePackets.gmChatSpyFrom("Channel (Rookies)", 0xFFFF),
                        listed.pstr());
                assertEquals(GamePackets.gmChatSpyMsg("TestNick2", "listed"), listed.pstr());
                PacketReader listedEcho = awaitOpcode(host, GamePackets.SERVER_CHAT);
                assertEquals(GamePackets.CHAT_NORMAL, listedEcho.u8());
                assertEquals("TestNick2", listedEcho.pstr());
                assertEquals("listed", listedEcho.pstr());

                host.sendPlain(GamePackets.clientGmWhisperList(
                        GamePackets.GM_CMD_CLOSE_WHISPER, "TestNick2"));
                assertEquals(
                        GamePackets.chatColor(GamePackets.CHAT_GREEN_HEX, GamePackets.GM_CMD_OK),
                        skipChatNotice(awaitOpcode(host, GamePackets.SERVER_CHAT)));
                host.sendPlain(GamePackets.clientGmWhisperList(
                        GamePackets.GM_CMD_OPEN_WHISPER, ""));
                assertEquals(
                        GamePackets.chatColor(GamePackets.CHAT_RED_HEX, GamePackets.GM_CMD_FAIL),
                        skipChatNotice(awaitOpcode(host, GamePackets.SERVER_CHAT)));
                host.sendPlain(GamePackets.clientGmWhisperList(
                        GamePackets.GM_CMD_OPEN_WHISPER, "Nobody"));
                assertEquals(
                        GamePackets.chatColor(GamePackets.CHAT_RED_HEX, GamePackets.GM_CMD_BLOCKED),
                        skipChatNotice(awaitOpcode(host, GamePackets.SERVER_CHAT)));

                int guestOid = oidOf(runtime, 10002);
                host.sendPlain(GamePackets.clientGmDisconnect(guestOid));
                assertEquals(
                        GamePackets.chatColor(GamePackets.CHAT_GREEN_HEX, GamePackets.GM_CMD_OK),
                        skipChatNotice(awaitOpcode(host, GamePackets.SERVER_CHAT)));
                assertTrue(awaitSessionCount(runtime, 1, 3, TimeUnit.SECONDS));
            } finally {
                repo.setCapability(10001, 0);
            }
        }
    }

    @Test
    void versusMarkerAndPawsBroadcast() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient host = new PangyaFakeClient();
             PangyaFakeClient guest = new PangyaFakeClient()) {
            loginTwoPlayers(ds, keys, host, guest, runtime.port());
            host.sendPlain(GamePackets.clientCreateRoom(GamePackets.TIPO_STROKE, "MK", ""));
            PacketReader created = awaitOpcode(host, GamePackets.SERVER_ROOM_ENTER_RESULT);
            assertEquals(0, created.i16());
            int numero = roomNumberFromInfo(created.readBytes(GamePackets.ROOM_INFO_BYTES));
            guest.sendPlain(GamePackets.clientJoinRoom(numero, ""));
            assertEquals(0, awaitOpcode(guest, GamePackets.SERVER_ROOM_ENTER_RESULT).i16());
            host.sendPlain(GamePackets.clientStartGame());
            awaitOpcode(host, GamePackets.SERVER_START_GAME_FLAG);
            awaitOpcode(host, GamePackets.SERVER_START_GAME_FLAG2);
            awaitOpcode(host, GamePackets.SERVER_PANG_RATE);
            awaitOpcode(host, GamePackets.SERVER_GAME_INIT);
            awaitOpcode(host, GamePackets.SERVER_COURSE);
            awaitOpcode(host, GamePackets.SERVER_MASCOT_SEED);
            awaitOpcode(guest, GamePackets.SERVER_GAME_INIT);
            host.sendPlain(GamePackets.clientInitHole(1, 0, 0, 4, 1.5f, 2.5f, 10f, 20f));
            guest.sendPlain(GamePackets.clientInitHole(1, 0, 0, 4, 1.5f, 2.5f, 10f, 20f));
            host.sendPlain(GamePackets.clientLoadOk());
            guest.sendPlain(GamePackets.clientLoadOk());
            awaitOpcode(host, GamePackets.SERVER_WEATHER);
            awaitOpcode(host, GamePackets.SERVER_WIND);
            awaitOpcode(host, GamePackets.SERVER_HOLE_TURN);
            int hostOid = oidOf(runtime, 10001);
            host.sendPlain(GamePackets.clientMarker(1.5f, 2.5f, 3.5f));
            PacketReader marker = awaitOpcode(host, GamePackets.SERVER_MARKER);
            assertEquals(hostOid, marker.i32());
            assertEquals(1.5f, marker.f32());
            assertEquals(2.5f, marker.f32());
            assertEquals(3.5f, marker.f32());
            PacketReader guestMarker = awaitOpcode(guest, GamePackets.SERVER_MARKER);
            assertEquals(hostOid, guestMarker.i32());
            host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_ACTIVE_PAWS));
            PacketReader paws = awaitOpcode(host, GamePackets.SERVER_ACTIVE_PAWS);
            assertEquals(10001, paws.u32());
            PacketReader guestPaws = awaitOpcode(guest, GamePackets.SERVER_ACTIVE_PAWS);
            assertEquals(10001, guestPaws.u32());
        }
    }

    @Test
    void versusShotEndBroadcastsTurnOidAndEcho() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient host = new PangyaFakeClient();
             PangyaFakeClient guest = new PangyaFakeClient()) {
            loginTwoPlayers(ds, keys, host, guest, runtime.port());
            host.sendPlain(GamePackets.clientCreateRoom(GamePackets.TIPO_STROKE, "SE", ""));
            PacketReader created = awaitOpcode(host, GamePackets.SERVER_ROOM_ENTER_RESULT);
            assertEquals(0, created.i16());
            int numero = roomNumberFromInfo(created.readBytes(GamePackets.ROOM_INFO_BYTES));
            guest.sendPlain(GamePackets.clientJoinRoom(numero, ""));
            assertEquals(0, awaitOpcode(guest, GamePackets.SERVER_ROOM_ENTER_RESULT).i16());
            host.sendPlain(GamePackets.clientStartGame());
            awaitOpcode(host, GamePackets.SERVER_START_GAME_FLAG);
            awaitOpcode(host, GamePackets.SERVER_START_GAME_FLAG2);
            awaitOpcode(host, GamePackets.SERVER_PANG_RATE);
            awaitOpcode(host, GamePackets.SERVER_GAME_INIT);
            awaitOpcode(host, GamePackets.SERVER_COURSE);
            awaitOpcode(host, GamePackets.SERVER_MASCOT_SEED);
            awaitOpcode(guest, GamePackets.SERVER_GAME_INIT);
            host.sendPlain(GamePackets.clientInitHole(1, 0, 0, 4, 1.5f, 2.5f, 10f, 20f));
            guest.sendPlain(GamePackets.clientInitHole(1, 0, 0, 4, 1.5f, 2.5f, 10f, 20f));
            host.sendPlain(GamePackets.clientLoadOk());
            guest.sendPlain(GamePackets.clientLoadOk());
            awaitOpcode(host, GamePackets.SERVER_WEATHER);
            awaitOpcode(host, GamePackets.SERVER_WIND);
            awaitOpcode(host, GamePackets.SERVER_HOLE_TURN);
            int hostOid = oidOf(runtime, 10001);
            byte[] shotBody = GamePackets.shotEndLocationSample();
            host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_SHOT_END));
            guest.sendPlain(GamePackets.clientShotEnd(shotBody));
            PacketReader hostFromGuest = awaitOpcode(host, GamePackets.SERVER_SHOT_END);
            assertShotEnd(hostFromGuest, hostOid, 1, shotBody);
            PacketReader guestFromGuest = awaitOpcode(guest, GamePackets.SERVER_SHOT_END);
            assertShotEnd(guestFromGuest, hostOid, 1, shotBody);
            byte[] hostBody = GamePackets.shotEndLocation(
                    0.5f,
                    1.5f, 2.5f, 3.5f,
                    1,
                    4.5f, 5.5f, 6.5f,
                    7.5f, 8.5f, 9.5f,
                    10.5f, 11.5f,
                    12,
                    13.5f, 14.5f,
                    0, 3,
                    15.5f, 16.5f, 17.5f, 18.5f, 19.5f,
                    20);
            host.sendPlain(GamePackets.clientShotEnd(hostBody));
            PacketReader hostAck = awaitOpcode(host, GamePackets.SERVER_SHOT_END);
            assertShotEnd(hostAck, hostOid, 1, hostBody);
            PacketReader guestAck = awaitOpcode(guest, GamePackets.SERVER_SHOT_END);
            assertShotEnd(guestAck, hostOid, 1, hostBody);
            host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_LEAVE_CHIP_IN));
            host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_CUTIN));
            PacketReader cutinTrunc = awaitOpcode(host, GamePackets.SERVER_CUTIN);
            assertEquals(0, cutinTrunc.u8());
            assertEquals(GamePackets.CUTIN_ERR, cutinTrunc.u16());
            host.sendPlain(GamePackets.clientCutin(10001, 1, 0, 0x04000000, 1));
            PacketReader cutin = awaitOpcode(host, GamePackets.SERVER_CUTIN);
            assertEquals(0, cutin.u8());
            assertEquals(GamePackets.CUTIN_ERR, cutin.u16());
        }
    }

    @Test
    void skinCutinBroadcastsIffPayloadInPractice() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient client = new PangyaFakeClient()) {
            InventoryRepository inv = new JdbiInventoryRepository(DatabaseSupport.jdbi(ds));
            inv.deleteCutinIff(GamePackets.TYPEID_CUTIN_SKIN);
            try {
                inv.upsertCutinIff(
                        GamePackets.TYPEID_CUTIN_SKIN,
                        2,
                        1,
                        new int[] {10, 11, 12, 13},
                        7,
                        new String[] {"char", "bg", "pattern", "text"});
                LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(ds));
                String loginKey = repo.generateAuthKeyLogin(10001);
                String gameKey = repo.generateAuthKeyGame(10001, 20202);
                keys.putLoginKey(10001, loginKey);
                keys.putGameKey(10001, 20202, gameKey);
                loginToChannel(client, runtime.port(), "testuser", 10001, loginKey, gameKey);

                client.sendPlain(GamePackets.clientCreatePractice("cutin", "secret"));
                assertEquals(0, awaitOpcode(client, GamePackets.SERVER_ROOM_ENTER_RESULT).i16());
                client.sendPlain(GamePackets.clientStartGame());
                awaitOpcode(client, GamePackets.SERVER_START_GAME_FLAG);
                awaitOpcode(client, GamePackets.SERVER_START_GAME_FLAG2);
                awaitOpcode(client, GamePackets.SERVER_PANG_RATE);
                awaitOpcode(client, GamePackets.SERVER_GAME_INIT);
                awaitOpcode(client, GamePackets.SERVER_COURSE);

                client.sendPlain(GamePackets.clientCutin(
                        10001, 1, 0, GamePackets.TYPEID_CUTIN_SKIN, 0));
                PacketReader ok = awaitOpcode(client, GamePackets.SERVER_CUTIN);
                assertEquals(1, ok.u8());
                assertEquals(GamePackets.TYPEID_CUTIN_SKIN, ok.u32());
                assertEquals(2, ok.u32());
                assertEquals(1, ok.u32());
                for (int i = 0; i < 4; i++) {
                    assertEquals(10 + i, ok.u32());
                }
                assertEquals(7, ok.u32());
                assertEquals("char", ok.fixedStr(40));
                assertEquals("bg", ok.fixedStr(40));
                assertEquals("pattern", ok.fixedStr(40));
                assertEquals("text", ok.fixedStr(40));
                assertEquals(0, ok.remaining());

                client.sendPlain(GamePackets.clientCutin(
                        10002, 1, 0, GamePackets.TYPEID_CUTIN_SKIN, 0));
                PacketReader wrongUid = awaitOpcode(client, GamePackets.SERVER_CUTIN);
                assertEquals(0, wrongUid.u8());
                assertEquals(GamePackets.CUTIN_ERR, wrongUid.u16());
            } finally {
                inv.deleteCutinIff(GamePackets.TYPEID_CUTIN_SKIN);
            }
        }
    }

    @Test
    void versusUseItemBroadcastsActiveItem() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient host = new PangyaFakeClient();
             PangyaFakeClient guest = new PangyaFakeClient()) {
            InventoryRepository inv = new JdbiInventoryRepository(DatabaseSupport.jdbi(ds));
            try {
                setItemSlot1(ds, 10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
                inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
                inv.addWarehouseItem(10001, GamePackets.TYPEID_SHOP_PANG_ITEM, 1);
                inv.deleteWarehouseByTypeid(10002, GamePackets.TYPEID_SHOP_PANG_ITEM);
                inv.addWarehouseItem(10002, GamePackets.TYPEID_SHOP_PANG_ITEM, 1);
                loginTwoPlayers(ds, keys, host, guest, runtime.port());
                host.sendPlain(GamePackets.clientCreateRoom(GamePackets.TIPO_STROKE, "UI", ""));
                PacketReader created = awaitOpcode(host, GamePackets.SERVER_ROOM_ENTER_RESULT);
                assertEquals(0, created.i16());
                int numero = roomNumberFromInfo(created.readBytes(GamePackets.ROOM_INFO_BYTES));
                guest.sendPlain(GamePackets.clientJoinRoom(numero, ""));
                assertEquals(0, awaitOpcode(guest, GamePackets.SERVER_ROOM_ENTER_RESULT).i16());
                host.sendPlain(GamePackets.clientStartGame());
                awaitOpcode(host, GamePackets.SERVER_START_GAME_FLAG);
                awaitOpcode(host, GamePackets.SERVER_START_GAME_FLAG2);
                awaitOpcode(host, GamePackets.SERVER_PANG_RATE);
                awaitOpcode(host, GamePackets.SERVER_GAME_INIT);
                awaitOpcode(host, GamePackets.SERVER_COURSE);
                awaitOpcode(host, GamePackets.SERVER_MASCOT_SEED);
                awaitOpcode(guest, GamePackets.SERVER_GAME_INIT);
                host.sendPlain(GamePackets.clientInitHole(1, 0, 0, 4, 1.5f, 2.5f, 10f, 20f));
                guest.sendPlain(GamePackets.clientInitHole(1, 0, 0, 4, 1.5f, 2.5f, 10f, 20f));
                host.sendPlain(GamePackets.clientLoadOk());
                guest.sendPlain(GamePackets.clientLoadOk());
                awaitOpcode(host, GamePackets.SERVER_WEATHER);
                awaitOpcode(host, GamePackets.SERVER_WIND);
                awaitOpcode(host, GamePackets.SERVER_HOLE_TURN);
                int hostOid = oidOf(runtime, 10001);
                host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_USE_ITEM));
                host.sendPlain(GamePackets.clientUseItem(0));
                host.sendPlain(GamePackets.clientUseItem(0x1A000099));
                host.sendPlain(GamePackets.clientUseItem(GamePackets.TYPEID_NURI));
                host.sendPlain(GamePackets.clientUseItem(GamePackets.TYPEID_MULLIGAN_ROSE));
                guest.sendPlain(GamePackets.clientUseItem(GamePackets.TYPEID_SHOP_PANG_ITEM));
                host.sendPlain(GamePackets.clientUseItem(GamePackets.TYPEID_SHOP_PANG_ITEM));
                PacketReader used = awaitOpcode(host, GamePackets.SERVER_ACTIVE_ITEM);
                assertEquals(GamePackets.TYPEID_SHOP_PANG_ITEM, used.u32());
                used.i32();
                assertEquals(hostOid, used.i32());
                PacketReader guestUsed = awaitOpcode(guest, GamePackets.SERVER_ACTIVE_ITEM);
                assertEquals(GamePackets.TYPEID_SHOP_PANG_ITEM, guestUsed.u32());
                guestUsed.i32();
                assertEquals(hostOid, guestUsed.i32());
                host.sendPlain(GamePackets.clientUseItem(GamePackets.TYPEID_SHOP_PANG_ITEM));
                host.sendPlain(GamePackets.clientCamera(1.25f));
                PacketReader mira = new PacketReader(host.awaitPlain(5, TimeUnit.SECONDS));
                assertEquals(GamePackets.SERVER_CAMERA, mira.opcode());
                assertEquals(hostOid, mira.i32());
                assertEquals(1.25f, mira.f32());
            } finally {
                setItemSlot1(ds, 10001, 0);
                inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
                inv.deleteWarehouseByTypeid(10002, GamePackets.TYPEID_SHOP_PANG_ITEM);
            }
        }
    }

    @Test
    void versusFinishShotBroadcastsEndShot() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient host = new PangyaFakeClient();
             PangyaFakeClient guest = new PangyaFakeClient()) {
            loginTwoPlayers(ds, keys, host, guest, runtime.port());
            host.sendPlain(GamePackets.clientCreateRoom(GamePackets.TIPO_STROKE, "FS", ""));
            PacketReader created = awaitOpcode(host, GamePackets.SERVER_ROOM_ENTER_RESULT);
            assertEquals(0, created.i16());
            int numero = roomNumberFromInfo(created.readBytes(GamePackets.ROOM_INFO_BYTES));
            guest.sendPlain(GamePackets.clientJoinRoom(numero, ""));
            assertEquals(0, awaitOpcode(guest, GamePackets.SERVER_ROOM_ENTER_RESULT).i16());
            host.sendPlain(GamePackets.clientStartGame());
            awaitOpcode(host, GamePackets.SERVER_START_GAME_FLAG);
            awaitOpcode(host, GamePackets.SERVER_START_GAME_FLAG2);
            awaitOpcode(host, GamePackets.SERVER_PANG_RATE);
            awaitOpcode(host, GamePackets.SERVER_GAME_INIT);
            awaitOpcode(host, GamePackets.SERVER_COURSE);
            awaitOpcode(host, GamePackets.SERVER_MASCOT_SEED);
            awaitOpcode(guest, GamePackets.SERVER_GAME_INIT);
            host.sendPlain(GamePackets.clientInitHole(1, 0, 0, 4, 1.5f, 2.5f, 10f, 20f));
            guest.sendPlain(GamePackets.clientInitHole(1, 0, 0, 4, 1.5f, 2.5f, 10f, 20f));
            host.sendPlain(GamePackets.clientLoadOk());
            guest.sendPlain(GamePackets.clientLoadOk());
            awaitOpcode(host, GamePackets.SERVER_WEATHER);
            awaitOpcode(host, GamePackets.SERVER_WIND);
            awaitOpcode(host, GamePackets.SERVER_HOLE_TURN);
            int hostOid = oidOf(runtime, 10001);
            host.sendPlain(GamePackets.clientShotAckCubes(1, 99));
            PacketReader hostEnd = awaitOpcode(host, GamePackets.SERVER_END_SHOT);
            assertEquals(hostOid, hostEnd.i32());
            assertEquals(0, hostEnd.u8());
            PacketReader guestEnd = awaitOpcode(guest, GamePackets.SERVER_END_SHOT);
            assertEquals(hostOid, guestEnd.i32());
            assertEquals(0, guestEnd.u8());
            host.sendPlain(GamePackets.clientShotAck());
            host.sendPlain(GamePackets.clientCamera(1.25f));
            PacketReader mira = new PacketReader(host.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(GamePackets.SERVER_CAMERA, mira.opcode());
            assertEquals(hostOid, mira.i32());
            assertEquals(1.25f, mira.f32());
        }
    }

    @Test
    void versusReplayBroadcastsRemaining() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient host = new PangyaFakeClient();
             PangyaFakeClient guest = new PangyaFakeClient()) {
            InventoryRepository inv = new JdbiInventoryRepository(DatabaseSupport.jdbi(ds));
            try {
                inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
                inv.addWarehouseItem(10001, GamePackets.TYPEID_SHOP_PANG_ITEM, 2);
                loginTwoPlayers(ds, keys, host, guest, runtime.port());
                host.sendPlain(GamePackets.clientCreateRoom(GamePackets.TIPO_STROKE, "RP", ""));
                PacketReader created = awaitOpcode(host, GamePackets.SERVER_ROOM_ENTER_RESULT);
                assertEquals(0, created.i16());
                int numero = roomNumberFromInfo(created.readBytes(GamePackets.ROOM_INFO_BYTES));
                guest.sendPlain(GamePackets.clientJoinRoom(numero, ""));
                assertEquals(0, awaitOpcode(guest, GamePackets.SERVER_ROOM_ENTER_RESULT).i16());
                host.sendPlain(GamePackets.clientStartGame());
                awaitOpcode(host, GamePackets.SERVER_START_GAME_FLAG);
                awaitOpcode(host, GamePackets.SERVER_START_GAME_FLAG2);
                awaitOpcode(host, GamePackets.SERVER_PANG_RATE);
                awaitOpcode(host, GamePackets.SERVER_GAME_INIT);
                awaitOpcode(host, GamePackets.SERVER_COURSE);
                awaitOpcode(host, GamePackets.SERVER_MASCOT_SEED);
                awaitOpcode(guest, GamePackets.SERVER_GAME_INIT);
                host.sendPlain(GamePackets.clientInitHole(1, 0, 0, 4, 1.5f, 2.5f, 10f, 20f));
                guest.sendPlain(GamePackets.clientInitHole(1, 0, 0, 4, 1.5f, 2.5f, 10f, 20f));
                host.sendPlain(GamePackets.clientLoadOk());
                guest.sendPlain(GamePackets.clientLoadOk());
                awaitOpcode(host, GamePackets.SERVER_WEATHER);
                awaitOpcode(host, GamePackets.SERVER_WIND);
                awaitOpcode(host, GamePackets.SERVER_HOLE_TURN);
                int hostOid = oidOf(runtime, 10001);
                host.sendPlain(GamePackets.clientReplay(0));
                host.sendPlain(GamePackets.clientReplay(0x1A000099));
                host.sendPlain(GamePackets.clientReplay(GamePackets.TYPEID_SHOP_PANG_ITEM));
                PacketReader hostReplay = awaitOpcode(host, GamePackets.SERVER_REPLAY);
                assertEquals(1, hostReplay.u16());
                PacketReader guestReplay = awaitOpcode(guest, GamePackets.SERVER_REPLAY);
                assertEquals(1, guestReplay.u16());
                host.sendPlain(GamePackets.clientReplay(GamePackets.TYPEID_SHOP_PANG_ITEM));
                PacketReader hostLast = awaitOpcode(host, GamePackets.SERVER_REPLAY);
                assertEquals(0, hostLast.u16());
                PacketReader guestLast = awaitOpcode(guest, GamePackets.SERVER_REPLAY);
                assertEquals(0, guestLast.u16());
                host.sendPlain(GamePackets.clientReplay(GamePackets.TYPEID_SHOP_PANG_ITEM));
                host.sendPlain(GamePackets.clientCamera(1.25f));
                PacketReader mira = new PacketReader(host.awaitPlain(5, TimeUnit.SECONDS));
                assertEquals(GamePackets.SERVER_CAMERA, mira.opcode());
                assertEquals(hostOid, mira.i32());
                assertEquals(1.25f, mira.f32());
            } finally {
                inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
            }
        }
    }

    @Test
    void tourneyReplaySendsRemainingToSender() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient host = new PangyaFakeClient();
             PangyaFakeClient guest = new PangyaFakeClient()) {
            InventoryRepository inv = new JdbiInventoryRepository(DatabaseSupport.jdbi(ds));
            try {
                inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
                inv.addWarehouseItem(10001, GamePackets.TYPEID_SHOP_PANG_ITEM, 1);
                loginTwoPlayers(ds, keys, host, guest, runtime.port());
                host.sendPlain(GamePackets.clientCreateRoom(GamePackets.TIPO_TOURNEY, "RP-T", ""));
                PacketReader created = awaitOpcode(host, GamePackets.SERVER_ROOM_ENTER_RESULT);
                assertEquals(0, created.i16());
                int numero = roomNumberFromInfo(created.readBytes(GamePackets.ROOM_INFO_BYTES));
                guest.sendPlain(GamePackets.clientJoinRoom(numero, ""));
                assertEquals(0, awaitOpcode(guest, GamePackets.SERVER_ROOM_ENTER_RESULT).i16());
                host.sendPlain(GamePackets.clientStartGame());
                awaitOpcode(host, GamePackets.SERVER_START_GAME_FLAG);
                awaitOpcode(host, GamePackets.SERVER_START_GAME_FLAG2);
                awaitOpcode(host, GamePackets.SERVER_PANG_RATE);
                awaitOpcode(host, GamePackets.SERVER_GAME_INIT);
                awaitOpcode(host, GamePackets.SERVER_COURSE);
                awaitOpcode(guest, GamePackets.SERVER_GAME_INIT);
                awaitOpcode(guest, GamePackets.SERVER_COURSE);
                host.sendPlain(GamePackets.clientReplay(GamePackets.TYPEID_SHOP_PANG_ITEM));
                PacketReader hostReplay = awaitOpcode(host, GamePackets.SERVER_REPLAY);
                assertEquals(0, hostReplay.u16());
                for (byte[] leftover : guest.drainPlain(400)) {
                    assertTrue(new PacketReader(leftover).opcode() != GamePackets.SERVER_REPLAY);
                }
            } finally {
                inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
            }
        }
    }

    @Test
    void tourneyTicketReportSendsNewItemAndLeavesGuestInGame() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient host = new PangyaFakeClient();
             PangyaFakeClient guest = new PangyaFakeClient()) {
            InventoryRepository inv = new JdbiInventoryRepository(DatabaseSupport.jdbi(ds));
            try {
                inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_TICKET_REPORT);
                inv.setLevel(10001, GamePackets.GIFT_MIN_LEVEL);
                loginTwoPlayers(ds, keys, host, guest, runtime.port());
                host.sendPlain(GamePackets.clientCreateRoom(GamePackets.TIPO_TOURNEY, "TR", ""));
                PacketReader created = awaitOpcode(host, GamePackets.SERVER_ROOM_ENTER_RESULT);
                assertEquals(0, created.i16());
                int numero = roomNumberFromInfo(created.readBytes(GamePackets.ROOM_INFO_BYTES));
                host.drainPlain(200);
                host.sendPlain(GamePackets.clientChangeRoomHoles(numero, 1));
                PacketReader holesUpd = awaitOpcode(host, GamePackets.SERVER_ROOM_UPDATE);
                assertEquals(-1, holesUpd.i16());
                holesUpd.u8();
                holesUpd.u8();
                assertEquals(1, holesUpd.u8());
                guest.sendPlain(GamePackets.clientJoinRoom(numero, ""));
                assertEquals(0, awaitOpcode(guest, GamePackets.SERVER_ROOM_ENTER_RESULT).i16());
                host.sendPlain(GamePackets.clientStartGame());
                awaitOpcode(host, GamePackets.SERVER_START_GAME_FLAG);
                awaitOpcode(host, GamePackets.SERVER_START_GAME_FLAG2);
                awaitOpcode(host, GamePackets.SERVER_PANG_RATE);
                awaitOpcode(host, GamePackets.SERVER_GAME_INIT);
                awaitOpcode(host, GamePackets.SERVER_COURSE);
                awaitOpcode(guest, GamePackets.SERVER_GAME_INIT);
                awaitOpcode(guest, GamePackets.SERVER_COURSE);
                host.sendPlain(GamePackets.clientInitHole(1, 0, 0, 4, 1.5f, 2.5f, 10f, 20f));
                awaitOpcode(host, GamePackets.SERVER_WEATHER);
                host.sendPlain(GamePackets.clientHoleStat());
                host.sendPlain(GamePackets.clientUseTicketReport());
                for (byte[] leftover : host.drainPlain(400)) {
                    assertTrue(new PacketReader(leftover).opcode() != GamePackets.SERVER_NEW_ITEM);
                }
                inv.addWarehouseItem(10001, GamePackets.TYPEID_TICKET_REPORT, 1);
                int hostOid = oidOf(runtime, 10001);
                host.sendPlain(GamePackets.clientUseTicketReport());
                PacketReader added = awaitOpcode(host, GamePackets.SERVER_NEW_ITEM);
                assertEquals(1, added.u16());
                assertEquals(GamePackets.TYPEID_TICKET_REPORT, added.u32());
                added.i32();
                added.u16();
                added.u8();
                assertEquals(0, added.u16());
                PacketReader notice = awaitOpcode(host, GamePackets.SERVER_TICKET_REPORT_NOTICE);
                assertEquals(0, notice.u32());
                PacketReader exit = awaitOpcode(host, GamePackets.SERVER_EXIT_ROOM);
                assertEquals(-1, exit.i16());
                PacketReader score = awaitOpcode(guest, GamePackets.SERVER_SCORE_LEAVE);
                assertEquals(hostOid, score.i32());
                PacketReader leaveUser = awaitOpcode(guest, GamePackets.SERVER_TICKET_REPORT_LEAVE);
                assertEquals(hostOid, leaveUser.i32());
                for (byte[] leftover : guest.drainPlain(400)) {
                    assertTrue(new PacketReader(leftover).opcode() != GamePackets.SERVER_NEW_ITEM);
                }
                guest.sendPlain(GamePackets.clientCamera(1.25f));
                PacketReader mira = awaitOpcode(guest, GamePackets.SERVER_CAMERA);
                assertEquals(oidOf(runtime, 10002), mira.i32());
                assertEquals(1.25f, mira.f32());
            } finally {
                inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_TICKET_REPORT);
                inv.setLevel(10001, 1);
            }
        }

        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient host = new PangyaFakeClient();
             PangyaFakeClient guest = new PangyaFakeClient()) {
            InventoryRepository inv = new JdbiInventoryRepository(DatabaseSupport.jdbi(ds));
            try {
                inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_TICKET_REPORT);
                inv.addWarehouseItem(10001, GamePackets.TYPEID_TICKET_REPORT, 1);
                inv.setLevel(10001, GamePackets.GIFT_MIN_LEVEL);
                loginTwoPlayers(ds, keys, host, guest, runtime.port());
                host.sendPlain(GamePackets.clientCreateRoom(GamePackets.TIPO_STROKE, "TR-VS", ""));
                PacketReader created = awaitOpcode(host, GamePackets.SERVER_ROOM_ENTER_RESULT);
                assertEquals(0, created.i16());
                int numero = roomNumberFromInfo(created.readBytes(GamePackets.ROOM_INFO_BYTES));
                guest.sendPlain(GamePackets.clientJoinRoom(numero, ""));
                assertEquals(0, awaitOpcode(guest, GamePackets.SERVER_ROOM_ENTER_RESULT).i16());
                host.sendPlain(GamePackets.clientStartGame());
                awaitOpcode(host, GamePackets.SERVER_START_GAME_FLAG);
                awaitOpcode(host, GamePackets.SERVER_START_GAME_FLAG2);
                awaitOpcode(host, GamePackets.SERVER_PANG_RATE);
                awaitOpcode(host, GamePackets.SERVER_GAME_INIT);
                awaitOpcode(host, GamePackets.SERVER_COURSE);
                awaitOpcode(host, GamePackets.SERVER_MASCOT_SEED);
                awaitOpcode(guest, GamePackets.SERVER_GAME_INIT);
                host.sendPlain(GamePackets.clientUseTicketReport());
                host.sendPlain(GamePackets.clientCamera(1.5f));
                PacketReader mira = awaitOpcode(host, GamePackets.SERVER_CAMERA);
                assertEquals(oidOf(runtime, 10001), mira.i32());
                assertEquals(1.5f, mira.f32());
            } finally {
                inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_TICKET_REPORT);
                inv.setLevel(10001, 1);
            }
        }
    }

    @Test
    void pcbangMascotAcksFailAndEchoesMode() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient client = new PangyaFakeClient()) {
            InventoryRepository inv = new JdbiInventoryRepository(DatabaseSupport.jdbi(ds));
            LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(ds));
            String loginKey = repo.generateAuthKeyLogin(10001);
            String gameKey = repo.generateAuthKeyGame(10001, 20202);
            keys.putLoginKey(10001, loginKey);
            keys.putGameKey(10001, 20202, gameKey);
            loginToChannel(client, runtime.port(), "testuser", 10001, loginKey, gameKey);
            client.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_UPDATE_PCBANG_MASCOT));
            client.sendPlain(GamePackets.clientPcbangMascot(
                    GamePackets.MASCOT_MSG_OK, -1, "hi"));
            PacketReader miss = awaitOpcode(client, GamePackets.SERVER_CHANGE_MASCOT);
            assertEquals(GamePackets.PCBANG_MASCOT_ERR_INVALID, miss.u8());
            assertEquals(0, miss.remaining());
            int mascotId = inv.mascots(10001).getFirst().id;
            client.sendPlain(GamePackets.clientPcbangMascot(
                    GamePackets.MASCOT_MSG_OK, mascotId, "12345678901234567"));
            PacketReader tooLong = awaitOpcode(client, GamePackets.SERVER_CHANGE_MASCOT);
            assertEquals(GamePackets.PCBANG_MASCOT_ERR_LONG, tooLong.u8());
            assertEquals(0, tooLong.remaining());
            long pang = inv.pang(10001);
            String saved = inv.mascots(10001).getFirst().message;
            client.sendPlain(GamePackets.clientPcbangMascot(
                    GamePackets.MASCOT_MSG_OK, mascotId, "PangYa!"));
            PacketReader ok = awaitOpcode(client, GamePackets.SERVER_CHANGE_MASCOT);
            assertEquals(GamePackets.MASCOT_MSG_OK, ok.u8());
            assertEquals(mascotId, ok.i32());
            assertEquals("PangYa!", ok.pstr());
            assertEquals(pang, ok.u64());
            assertEquals(pang, inv.pang(10001));
            assertEquals(saved, inv.mascots(10001).getFirst().message);
            client.sendPlain(GamePackets.clientPcbangMascot(3, mascotId, "x"));
            PacketReader mode3 = awaitOpcode(client, GamePackets.SERVER_CHANGE_MASCOT);
            assertEquals(3, mode3.u8());
            assertEquals(0, mode3.remaining());
        }
    }

    @Test
    void dolfiniLockerPangDepositsAndWithdraws() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient client = new PangyaFakeClient()) {
            InventoryRepository inv = new JdbiInventoryRepository(DatabaseSupport.jdbi(ds));
            try {
                inv.setPangCookie(10001, 100000, 0);
                long leftover = inv.dolfiniLockerPang(10001);
                if (leftover > 0) {
                    inv.updateDolfiniLockerPang(10001, GamePackets.LOCKER_PANG_WITHDRAW, leftover);
                }
                LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(ds));
                String loginKey = repo.generateAuthKeyLogin(10001);
                String gameKey = repo.generateAuthKeyGame(10001, 20202);
                keys.putLoginKey(10001, loginKey);
                keys.putGameKey(10001, 20202, gameKey);
                loginToChannel(client, runtime.port(), "testuser", 10001, loginKey, gameKey);

                client.sendPlain(GamePackets.clientLockerPang());
                PacketReader empty = awaitOpcode(client, GamePackets.SERVER_LOCKER_PANG);
                assertEquals(0, empty.u64());

                client.sendPlain(GamePackets.clientLockerUpdatePang(GamePackets.LOCKER_PANG_DEPOSIT, 1000));
                PacketReader ack = awaitOpcode(client, GamePackets.SERVER_LOCKER_UPDATE_PANG);
                assertEquals(0, ack.u32());
                PacketReader spent = awaitOpcode(client, GamePackets.SERVER_PANG_SPENT);
                assertEquals(99000, spent.u64());
                assertEquals(1000, spent.u64());
                PacketReader locker = awaitOpcode(client, GamePackets.SERVER_LOCKER_PANG);
                assertEquals(1000, locker.u64());
                assertEquals(99000, inv.pang(10001));
                assertEquals(1000, inv.dolfiniLockerPang(10001));

                client.sendPlain(GamePackets.clientLockerPang());
                PacketReader query = awaitOpcode(client, GamePackets.SERVER_LOCKER_PANG);
                assertEquals(1000, query.u64());

                client.sendPlain(GamePackets.clientLockerUpdatePang(GamePackets.LOCKER_PANG_WITHDRAW, 400));
                PacketReader wack = awaitOpcode(client, GamePackets.SERVER_LOCKER_UPDATE_PANG);
                assertEquals(0, wack.u32());
                PacketReader wspent = awaitOpcode(client, GamePackets.SERVER_PANG_SPENT);
                assertEquals(99400, wspent.u64());
                assertEquals(400, wspent.u64());
                PacketReader wlocker = awaitOpcode(client, GamePackets.SERVER_LOCKER_PANG);
                assertEquals(600, wlocker.u64());

                client.sendPlain(GamePackets.clientLockerUpdatePang(GamePackets.LOCKER_PANG_DEPOSIT, 200000));
                PacketReader funds = awaitOpcode(client, GamePackets.SERVER_LOCKER_UPDATE_PANG);
                assertEquals(GamePackets.shopSys(GamePackets.LOCKER_PANG_DEPOSIT_ERR), funds.u32());
                client.sendPlain(GamePackets.clientLockerUpdatePang(2, 1));
                PacketReader opt = awaitOpcode(client, GamePackets.SERVER_LOCKER_UPDATE_PANG);
                assertEquals(GamePackets.shopSys(GamePackets.LOCKER_PANG_OPT_ERR), opt.u32());
                assertEquals(99400, inv.pang(10001));
                assertEquals(600, inv.dolfiniLockerPang(10001));
            } finally {
                long left = inv.dolfiniLockerPang(10001);
                if (left > 0) {
                    inv.updateDolfiniLockerPang(10001, GamePackets.LOCKER_PANG_WITHDRAW, left);
                }
                inv.setPangCookie(10001, 100000, 0);
            }
        }
    }

    @Test
    void dolfiniLockerAddAndRemoveMovesPart() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);
        final int partTypeid = (GamePackets.IFF_GROUP_PART << 26) | 0x99;

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient client = new PangyaFakeClient()) {
            InventoryRepository inv = new JdbiInventoryRepository(DatabaseSupport.jdbi(ds));
            int partId = 0;
            try {
                inv.deleteWarehouseByTypeid(10001, partTypeid);
                final int storedId = inv.addWarehouseItem(10001, partTypeid, 1);
                partId = storedId;
                LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(ds));
                String loginKey = repo.generateAuthKeyLogin(10001);
                String gameKey = repo.generateAuthKeyGame(10001, 20202);
                keys.putLoginKey(10001, loginKey);
                keys.putGameKey(10001, 20202, gameKey);
                loginToChannel(client, runtime.port(), "testuser", 10001, loginKey, gameKey);

                client.sendPlain(GamePackets.clientLockerMove(
                        GamePackets.CLIENT_LOCKER_ADD, 0, partTypeid, storedId, 1));
                PacketReader prelude = awaitOpcode(client, GamePackets.SERVER_DELETE_CARD);
                assertEquals(0, prelude.u16());
                PacketReader moved = awaitOpcode(client, GamePackets.SERVER_SHOP_BUY);
                assertEquals(1, moved.u32());
                assertEquals(GamePackets.LOCKER_MOVE_ADD, moved.u8());
                assertEquals(0, moved.u64());
                assertEquals(0, moved.u32());
                assertEquals(partTypeid, moved.u32());
                assertEquals(storedId, moved.i32());
                PacketReader addOk = awaitOpcode(client, GamePackets.SERVER_LOCKER_ADD);
                assertEquals(0, addOk.u32());
                assertEquals(0, addOk.u64());
                assertEquals(partTypeid, addOk.u32());
                assertEquals(storedId, addOk.i32());
                assertTrue(inv.warehouse(10001).stream().noneMatch(w -> w.id == storedId));

                client.sendPlain(GamePackets.clientLockerMove(
                        GamePackets.CLIENT_LOCKER_ADD, 0, GamePackets.TYPEID_SHOP_PANG_ITEM, 1, 1));
                PacketReader group = awaitOpcode(client, GamePackets.SERVER_LOCKER_ADD);
                assertEquals(GamePackets.shopSys(GamePackets.LOCKER_ADD_ERR_GROUP), group.u32());

                long idx = inv.dolfiniLockerIndex(10001, storedId).orElseThrow();
                client.sendPlain(GamePackets.clientLockerMove(
                        GamePackets.CLIENT_LOCKER_REMOVE, idx, partTypeid, storedId, 1));
                PacketReader back = awaitOpcode(client, GamePackets.SERVER_SHOP_BUY);
                assertEquals(1, back.u32());
                assertEquals(GamePackets.LOCKER_MOVE_REMOVE, back.u8());
                assertEquals(inv.pang(10001), back.u64());
                assertEquals(0, back.u32());
                assertEquals(partTypeid, back.u32());
                assertEquals(storedId, back.i32());
                PacketReader rmOk = awaitOpcode(client, GamePackets.SERVER_LOCKER_REMOVE);
                assertEquals(0, rmOk.u32());
                assertEquals(idx, rmOk.u64());
                assertEquals(partTypeid, rmOk.u32());
                assertEquals(storedId, rmOk.i32());
                assertTrue(inv.warehouse(10001).stream().anyMatch(w -> w.id == storedId));
            } finally {
                if (partId > 0) {
                    inv.deleteDolfiniLockerByItemId(10001, partId);
                }
                inv.deleteWarehouseByTypeid(10001, partTypeid);
            }
        }
    }

    @Test
    void deleteItemConsumesWarehouseItem() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient client = new PangyaFakeClient()) {
            InventoryRepository inv = new JdbiInventoryRepository(DatabaseSupport.jdbi(ds));
            try {
                inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
                final int storedId = inv.addWarehouseItem(10001, GamePackets.TYPEID_SHOP_PANG_ITEM, 2);
                LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(ds));
                String loginKey = repo.generateAuthKeyLogin(10001);
                String gameKey = repo.generateAuthKeyGame(10001, 20202);
                keys.putLoginKey(10001, loginKey);
                keys.putGameKey(10001, 20202, gameKey);
                loginToChannel(client, runtime.port(), "testuser", 10001, loginKey, gameKey);

                client.sendPlain(GamePackets.clientDeleteItem(GamePackets.TYPEID_SHOP_PANG_ITEM, 1));
                PacketReader ok = awaitOpcode(client, GamePackets.SERVER_DELETE_ITEM);
                assertEquals(GamePackets.DELETE_ITEM_OK, ok.u8());
                assertEquals(GamePackets.TYPEID_SHOP_PANG_ITEM, ok.u32());
                assertEquals(1, ok.u32());
                assertEquals(storedId, ok.i32());
                GamePackets.WarehouseItem leftover = inv.warehouse(10001).stream()
                        .filter(w -> w.typeid == GamePackets.TYPEID_SHOP_PANG_ITEM)
                        .findFirst()
                        .orElseThrow();
                assertEquals(storedId, leftover.id);
                assertEquals(1, leftover.c[0] & 0xffff);

                client.sendPlain(GamePackets.clientDeleteItem(GamePackets.TYPEID_SHOP_PANG_ITEM, 1));
                PacketReader last = awaitOpcode(client, GamePackets.SERVER_DELETE_ITEM);
                assertEquals(GamePackets.DELETE_ITEM_OK, last.u8());
                assertEquals(GamePackets.TYPEID_SHOP_PANG_ITEM, last.u32());
                assertEquals(1, last.u32());
                assertEquals(storedId, last.i32());
                assertTrue(inv.warehouse(10001).stream()
                        .noneMatch(w -> w.typeid == GamePackets.TYPEID_SHOP_PANG_ITEM));

                client.sendPlain(GamePackets.clientDeleteItem(GamePackets.TYPEID_SHOP_PANG_ITEM, 1));
                PacketReader missing = awaitOpcode(client, GamePackets.SERVER_DELETE_ITEM);
                assertEquals(GamePackets.DELETE_ITEM_FAIL, missing.u8());

                client.sendPlain(GamePackets.clientDeleteItem(1, 1));
                PacketReader group = awaitOpcode(client, GamePackets.SERVER_DELETE_ITEM);
                assertEquals(GamePackets.DELETE_ITEM_FAIL, group.u8());
            } finally {
                inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
            }
        }
    }

    @Test
    void cometRefillAddsBallC0AndConsumesItem() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient client = new PangyaFakeClient()) {
            InventoryRepository inv = new JdbiInventoryRepository(DatabaseSupport.jdbi(ds));
            final int draw = 4;
            int ballBefore = 0;
            try {
                inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
                inv.upsertCometRefill(GamePackets.TYPEID_SHOP_PANG_ITEM, draw, draw);
                inv.addWarehouseItem(10001, GamePackets.TYPEID_SHOP_PANG_ITEM, 1);
                GamePackets.WarehouseItem ball = inv.warehouse(10001).stream()
                        .filter(w -> w.typeid == GamePackets.TYPEID_DEFAULT_BALL)
                        .findFirst()
                        .orElseThrow();
                ballBefore = ball.c[0] & 0xffff;
                LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(ds));
                String loginKey = repo.generateAuthKeyLogin(10001);
                String gameKey = repo.generateAuthKeyGame(10001, 20202);
                keys.putLoginKey(10001, loginKey);
                keys.putGameKey(10001, 20202, gameKey);
                loginToChannel(client, runtime.port(), "testuser", 10001, loginKey, gameKey);

                client.sendPlain(GamePackets.clientCometRefill(
                        GamePackets.TYPEID_SHOP_PANG_ITEM, GamePackets.TYPEID_DEFAULT_BALL));
                PacketReader ok = awaitOpcode(client, GamePackets.SERVER_COMET_REFILL);
                assertEquals(GamePackets.COMET_REFILL_OK, ok.u8());
                assertEquals(GamePackets.TYPEID_SHOP_PANG_ITEM, ok.u32());
                assertEquals(GamePackets.TYPEID_DEFAULT_BALL, ok.u32());
                assertEquals(ballBefore + draw, ok.u16());
                assertTrue(inv.warehouse(10001).stream()
                        .noneMatch(w -> w.typeid == GamePackets.TYPEID_SHOP_PANG_ITEM));
                assertEquals(ballBefore + draw, inv.warehouse(10001).stream()
                        .filter(w -> w.typeid == GamePackets.TYPEID_DEFAULT_BALL)
                        .findFirst()
                        .orElseThrow()
                        .c[0] & 0xffff);

                client.sendPlain(GamePackets.clientCometRefill(
                        GamePackets.TYPEID_SHOP_PANG_ITEM, GamePackets.TYPEID_DEFAULT_BALL));
                PacketReader missing = awaitOpcode(client, GamePackets.SERVER_COMET_REFILL);
                assertEquals(0, missing.u8());
                assertEquals(10, missing.remaining());

                client.sendPlain(GamePackets.clientCometRefill(0, 0));
                PacketReader group = awaitOpcode(client, GamePackets.SERVER_COMET_REFILL);
                assertEquals(0, group.u8());
                assertEquals(10, group.remaining());
            } finally {
                inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
                inv.deleteCometRefill(GamePackets.TYPEID_SHOP_PANG_ITEM);
                GamePackets.WarehouseItem after = inv.warehouse(10001).stream()
                        .filter(w -> w.typeid == GamePackets.TYPEID_DEFAULT_BALL)
                        .findFirst()
                        .orElse(null);
                if (after != null && ballBefore > 0) {
                    int extra = (after.c[0] & 0xffff) - ballBefore;
                    if (extra > 0) {
                        inv.consumeWarehouseByTypeid(10001, GamePackets.TYPEID_DEFAULT_BALL, extra);
                    }
                }
            }
        }
    }

    @Test
    void attendanceCheckAndLoginCountDrawFromSqlCatalog() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient client = new PangyaFakeClient()) {
            InventoryRepository inv = new JdbiInventoryRepository(DatabaseSupport.jdbi(ds));
            final int qntd = 3;
            try {
                inv.deleteAttendanceReward(10001);
                inv.upsertAttendanceCatalog(
                        GamePackets.TYPEID_SHOP_PANG_ITEM,
                        qntd,
                        GamePackets.ATTENDANCE_TIPO_NORMAL);
                LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(ds));
                String loginKey = repo.generateAuthKeyLogin(10001);
                String gameKey = repo.generateAuthKeyGame(10001, 20202);
                keys.putLoginKey(10001, loginKey);
                keys.putGameKey(10001, 20202, gameKey);
                loginToChannel(client, runtime.port(), "testuser", 10001, loginKey, gameKey);

                client.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_ATTENDANCE));
                PacketReader first = awaitOpcode(client, GamePackets.SERVER_ATTENDANCE);
                assertEquals(GamePackets.ATTENDANCE_OK, first.i32());
                assertEquals(GamePackets.ATTENDANCE_LOGIN_NEW_DAY, first.u8());
                assertEquals(GamePackets.TYPEID_SHOP_PANG_ITEM, first.u32());
                assertEquals(qntd, first.u32());
                assertEquals(0, first.u32());
                assertEquals(0, first.u32());
                assertEquals(1, first.u32());
                assertEquals(0, first.remaining());

                client.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_ATTENDANCE));
                PacketReader sameDay = awaitOpcode(client, GamePackets.SERVER_ATTENDANCE);
                assertEquals(GamePackets.ATTENDANCE_OK, sameDay.i32());
                assertEquals(GamePackets.ATTENDANCE_LOGIN_SAME_DAY, sameDay.u8());
                assertEquals(GamePackets.TYPEID_SHOP_PANG_ITEM, sameDay.u32());
                assertEquals(qntd, sameDay.u32());
                assertEquals(0, sameDay.u32());
                assertEquals(0, sameDay.u32());
                assertEquals(1, sameDay.u32());

                client.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_ATTENDANCE_LOGIN));
                PacketReader loginCount = awaitOpcode(client, GamePackets.SERVER_ATTENDANCE_LOGIN);
                assertEquals(GamePackets.ATTENDANCE_OK, loginCount.i32());
                assertEquals(GamePackets.ATTENDANCE_LOGIN_SAME_DAY, loginCount.u8());
                assertEquals(GamePackets.TYPEID_SHOP_PANG_ITEM, loginCount.u32());
                assertEquals(qntd, loginCount.u32());
                assertEquals(GamePackets.TYPEID_SHOP_PANG_ITEM, loginCount.u32());
                assertEquals(qntd, loginCount.u32());
                assertEquals(1, loginCount.u32());
                assertEquals(0, loginCount.remaining());

                var stored = inv.attendanceReward(10001).orElseThrow();
                assertEquals(1, stored.counter());
                assertEquals(GamePackets.TYPEID_SHOP_PANG_ITEM, stored.nowTypeid());
                assertEquals(qntd, stored.nowQntd());
                assertEquals(GamePackets.TYPEID_SHOP_PANG_ITEM, stored.afterTypeid());
                assertEquals(qntd, stored.afterQntd());

                inv.deleteAttendanceCatalog(GamePackets.TYPEID_SHOP_PANG_ITEM);
                inv.deleteAttendanceReward(10001);
                client.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_ATTENDANCE));
                PacketReader emptyCheck = awaitOpcode(client, GamePackets.SERVER_ATTENDANCE);
                assertEquals(GamePackets.ATTENDANCE_FAIL, emptyCheck.u32());
                client.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_ATTENDANCE_LOGIN));
                PacketReader emptyLogin = awaitOpcode(client, GamePackets.SERVER_ATTENDANCE_LOGIN);
                assertEquals(GamePackets.ATTENDANCE_FAIL, emptyLogin.u32());
            } finally {
                inv.deleteAttendanceCatalog(GamePackets.TYPEID_SHOP_PANG_ITEM);
                inv.deleteAttendanceReward(10001);
            }
        }
    }

    @Test
    void itemBuffConsumesWarehouseAndSendsPacote181() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient client = new PangyaFakeClient()) {
            InventoryRepository inv = new JdbiInventoryRepository(DatabaseSupport.jdbi(ds));
            try {
                inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
                inv.deleteItemBuff(10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
                inv.upsertTimeLimitItem(
                        GamePackets.TYPEID_SHOP_PANG_ITEM,
                        GamePackets.ITEM_BUFF_TIPO_YAM,
                        10,
                        1);
                inv.addWarehouseItem(10001, GamePackets.TYPEID_SHOP_PANG_ITEM, 2);
                LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(ds));
                String loginKey = repo.generateAuthKeyLogin(10001);
                String gameKey = repo.generateAuthKeyGame(10001, 20202);
                keys.putLoginKey(10001, loginKey);
                keys.putGameKey(10001, 20202, gameKey);
                loginToChannel(client, runtime.port(), "testuser", 10001, loginKey, gameKey);

                client.sendPlain(GamePackets.clientItemBuff(GamePackets.TYPEID_SHOP_PANG_ITEM));
                PacketReader first = awaitOpcode(client, GamePackets.SERVER_ITEM_BUFF);
                assertEquals(GamePackets.ITEM_BUFF_OK, first.u32());
                assertEquals(1, first.u32());
                assertEquals(GamePackets.TYPEID_SHOP_PANG_ITEM, first.u32());
                assertEquals(0, first.u32());
                assertEquals(GamePackets.TYPEID_SHOP_PANG_ITEM, first.u32());
                assertEquals(0, first.u32());
                assertEquals(0, first.u32());
                assertEquals(0, first.u32());
                assertEquals(0, first.u32());
                assertEquals(0, first.u32());
                assertTrue(first.u16() >= 2026);
                for (int i = 0; i < 7; i++) {
                    first.u16();
                }
                for (int i = 0; i < 6; i++) {
                    assertEquals(0, first.u16());
                }
                assertEquals(0, first.u16());
                assertEquals(60, first.u16());
                assertEquals(GamePackets.ITEM_BUFF_TIPO_YAM, first.u32());
                assertEquals(GamePackets.ITEM_BUFF_USE_YN, first.u8());
                assertEquals(0, first.remaining());

                client.sendPlain(GamePackets.clientItemBuff(GamePackets.TYPEID_SHOP_PANG_ITEM));
                PacketReader extend = awaitOpcode(client, GamePackets.SERVER_ITEM_BUFF);
                assertEquals(GamePackets.ITEM_BUFF_OK, extend.u32());
                assertEquals(1, extend.u32());
                assertEquals(GamePackets.TYPEID_SHOP_PANG_ITEM, extend.u32());
                for (int i = 0; i < 7; i++) {
                    extend.u32();
                }
                for (int i = 0; i < 8; i++) {
                    extend.u16();
                }
                for (int i = 0; i < 6; i++) {
                    assertEquals(0, extend.u16());
                }
                assertEquals(0, extend.u16());
                assertEquals(120, extend.u16());
                assertEquals(GamePackets.ITEM_BUFF_TIPO_YAM, extend.u32());
                assertEquals(GamePackets.ITEM_BUFF_USE_YN, extend.u8());

                client.sendPlain(GamePackets.clientItemBuff(GamePackets.TYPEID_SHOP_PANG_ITEM));
                PacketReader missing = awaitOpcode(client, GamePackets.SERVER_ITEM_BUFF);
                assertEquals(GamePackets.shopSys(GamePackets.BUFF_ERR_MISSING), missing.u32());
                client.sendPlain(GamePackets.clientItemBuff(GamePackets.TYPEID_DEFAULT_BALL));
                PacketReader group = awaitOpcode(client, GamePackets.SERVER_ITEM_BUFF);
                assertEquals(GamePackets.shopSys(GamePackets.BUFF_ERR_IFF_ITEM), group.u32());
                client.sendPlain(GamePackets.clientItemBuff(0));
                PacketReader zero = awaitOpcode(client, GamePackets.SERVER_ITEM_BUFF);
                assertEquals(GamePackets.shopSys(GamePackets.BUFF_ERR_TYPEID), zero.u32());
            } finally {
                inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
                inv.deleteItemBuff(10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
                inv.deleteTimeLimitItem(GamePackets.TYPEID_SHOP_PANG_ITEM);
            }
        }
    }

    @Test
    void workshopRecoveryConsumesItemAndClearsPts() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient client = new PangyaFakeClient()) {
            InventoryRepository inv = new JdbiInventoryRepository(DatabaseSupport.jdbi(ds));
            GamePackets.WarehouseItem club = inv.warehouse(10001).stream()
                    .filter(w -> w.typeid == GamePackets.TYPEID_AIR_KNIGHT)
                    .findFirst()
                    .orElseThrow();
            int clubId = club.id;
            try {
                inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
                inv.deleteClubSetIff(GamePackets.TYPEID_AIR_KNIGHT);
                inv.setClubSetRecoveryPts(10001, clubId, 5);
                inv.addWarehouseItem(10001, GamePackets.TYPEID_SHOP_PANG_ITEM, 2);
                LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(ds));
                String loginKey = repo.generateAuthKeyLogin(10001);
                String gameKey = repo.generateAuthKeyGame(10001, 20202);
                keys.putLoginKey(10001, loginKey);
                keys.putGameKey(10001, 20202, gameKey);
                loginToChannel(client, runtime.port(), "testuser", 10001, loginKey, gameKey);

                client.sendPlain(GamePackets.clientWorkshopTypeidClub(
                        GamePackets.CLIENT_WORKSHOP_RECOVERY,
                        GamePackets.TYPEID_SHOP_PANG_ITEM,
                        clubId));
                PacketReader missingIff = awaitOpcode(client, GamePackets.SERVER_WORKSHOP_RECOVERY);
                assertEquals(GamePackets.shopSys(GamePackets.WORKSHOP_RECOVERY_ERR_IFF), missingIff.u32());

                inv.upsertClubSetWorkShopTipo(
                        GamePackets.TYPEID_AIR_KNIGHT, GamePackets.WORKSHOP_TIPO_BLOCKED);
                client.sendPlain(GamePackets.clientWorkshopTypeidClub(
                        GamePackets.CLIENT_WORKSHOP_RECOVERY,
                        GamePackets.TYPEID_SHOP_PANG_ITEM,
                        clubId));
                PacketReader blocked = awaitOpcode(client, GamePackets.SERVER_WORKSHOP_RECOVERY);
                assertEquals(GamePackets.shopSys(GamePackets.WORKSHOP_RECOVERY_ERR_TIPO), blocked.u32());

                inv.upsertClubSetWorkShopTipo(GamePackets.TYPEID_AIR_KNIGHT, 0);
                client.sendPlain(GamePackets.clientWorkshopTypeidClub(
                        GamePackets.CLIENT_WORKSHOP_RECOVERY,
                        GamePackets.TYPEID_SHOP_PANG_ITEM,
                        0));
                PacketReader missingClub = awaitOpcode(client, GamePackets.SERVER_WORKSHOP_RECOVERY);
                assertEquals(GamePackets.shopSys(GamePackets.WORKSHOP_RECOVERY_ERR_CLUB), missingClub.u32());

                int consumeId = inv.warehouse(10001).stream()
                        .filter(w -> w.typeid == GamePackets.TYPEID_SHOP_PANG_ITEM)
                        .findFirst()
                        .orElseThrow()
                        .id;
                int before = GamePackets.unixNow();
                client.sendPlain(GamePackets.clientWorkshopTypeidClub(
                        GamePackets.CLIENT_WORKSHOP_RECOVERY,
                        GamePackets.TYPEID_SHOP_PANG_ITEM,
                        clubId));
                PacketReader awards = awaitOpcode(client, GamePackets.SERVER_DAILY_QUEST_STAMP);
                int unix = awards.u32();
                assertTrue(unix >= before - 1 && unix <= GamePackets.unixNow() + 1);
                assertEquals(2, awards.u32());
                assertEquals(GamePackets.PAPEL_AWARD_TYPE, awards.u8());
                assertEquals(GamePackets.TYPEID_SHOP_PANG_ITEM, awards.u32());
                assertEquals(consumeId, awards.i32());
                assertEquals(0, awards.u32());
                assertEquals(2, awards.i32());
                assertEquals(1, awards.i32());
                assertEquals(-1, awards.i32());
                awards.readBytes(GamePackets.PAPEL_AWARD_PAD);
                assertEquals(GamePackets.WORKSHOP_AWARD_TYPE, awards.u8());
                assertEquals(GamePackets.TYPEID_AIR_KNIGHT, awards.u32());
                assertEquals(clubId, awards.i32());
                assertEquals(0, awards.u32());
                assertEquals(0, awards.i32());
                assertEquals(0, awards.i32());
                assertEquals(0, awards.i32());
                awards.readBytes(GamePackets.PAPEL_AWARD_PAD);
                for (int i = 0; i < 5; i++) {
                    assertEquals(club.workshopC[i], (short) awards.i16());
                }
                assertEquals(club.workshopMastery, awards.u32());
                assertEquals(club.workshopLevel & 0xFF, awards.u8());
                assertEquals(club.workshopRank, awards.u32());
                assertEquals(0, awards.u32());
                assertEquals(0, awards.remaining());
                PacketReader ok = awaitOpcode(client, GamePackets.SERVER_WORKSHOP_RECOVERY);
                assertEquals(GamePackets.WORKSHOP_RECOVERY_OK, ok.u32());
                assertEquals(0, inv.warehouse(10001).stream()
                        .filter(w -> w.id == clubId)
                        .findFirst()
                        .orElseThrow()
                        .workshopRecovery);
                assertEquals(1, inv.warehouse(10001).stream()
                        .filter(w -> w.typeid == GamePackets.TYPEID_SHOP_PANG_ITEM)
                        .findFirst()
                        .orElseThrow()
                        .c[0] & 0xffff);

                client.sendPlain(GamePackets.clientWorkshopTypeidClub(
                        GamePackets.CLIENT_WORKSHOP_RECOVERY,
                        GamePackets.TYPEID_SHOP_PANG_ITEM,
                        clubId));
                PacketReader done = awaitOpcode(client, GamePackets.SERVER_WORKSHOP_RECOVERY);
                assertEquals(GamePackets.shopSys(GamePackets.WORKSHOP_RECOVERY_ERR_DONE), done.u32());
            } finally {
                inv.setClubSetRecoveryPts(10001, clubId, 0);
                inv.deleteClubSetIff(GamePackets.TYPEID_AIR_KNIGHT);
                inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
            }
        }
    }

    @Test
    void workshopTransferMovesMasteryPts() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient client = new PangyaFakeClient()) {
            InventoryRepository inv = new JdbiInventoryRepository(DatabaseSupport.jdbi(ds));
            GamePackets.WarehouseItem src = inv.warehouse(10001).stream()
                    .filter(w -> w.typeid == GamePackets.TYPEID_AIR_KNIGHT)
                    .findFirst()
                    .orElseThrow();
            int srcId = src.id;
            inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
            inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_AIR_KNIGHT_LUCKY);
            inv.deleteClubSetIff(GamePackets.TYPEID_AIR_KNIGHT);
            inv.deleteClubSetIff(GamePackets.TYPEID_AIR_KNIGHT_LUCKY);
            inv.setClubSetMasteryPts(10001, srcId, 300);
            int dstId = inv.addWarehouseItem(10001, GamePackets.TYPEID_AIR_KNIGHT_LUCKY, 1);
            try {
                inv.addWarehouseItem(10001, GamePackets.TYPEID_SHOP_PANG_ITEM, 2);
                LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(ds));
                String loginKey = repo.generateAuthKeyLogin(10001);
                String gameKey = repo.generateAuthKeyGame(10001, 20202);
                keys.putLoginKey(10001, loginKey);
                keys.putGameKey(10001, 20202, gameKey);
                loginToChannel(client, runtime.port(), "testuser", 10001, loginKey, gameKey);

                client.sendPlain(GamePackets.clientWorkshopTransfer(
                        GamePackets.TYPEID_SHOP_PANG_ITEM, srcId, dstId, 1));
                PacketReader missingIff = awaitOpcode(client, GamePackets.SERVER_WORKSHOP_TRANSFER);
                assertEquals(GamePackets.shopSys(GamePackets.WORKSHOP_TRANSFER_ERR_IFF), missingIff.u32());

                inv.upsertClubSetWorkShopTipo(GamePackets.TYPEID_AIR_KNIGHT, 0);
                inv.upsertClubSetWorkShopTipo(
                        GamePackets.TYPEID_AIR_KNIGHT_LUCKY, GamePackets.WORKSHOP_TIPO_BLOCKED);
                client.sendPlain(GamePackets.clientWorkshopTransfer(
                        GamePackets.TYPEID_SHOP_PANG_ITEM, srcId, dstId, 1));
                PacketReader blocked = awaitOpcode(client, GamePackets.SERVER_WORKSHOP_TRANSFER);
                assertEquals(GamePackets.shopSys(GamePackets.WORKSHOP_TRANSFER_ERR_TIPO), blocked.u32());

                inv.upsertClubSetWorkShopTipo(GamePackets.TYPEID_AIR_KNIGHT_LUCKY, 0);
                client.sendPlain(GamePackets.clientWorkshopTransfer(
                        GamePackets.TYPEID_SHOP_PANG_ITEM, srcId, 0, 1));
                PacketReader missingClub = awaitOpcode(client, GamePackets.SERVER_WORKSHOP_TRANSFER);
                assertEquals(GamePackets.shopSys(GamePackets.WORKSHOP_TRANSFER_ERR_CLUB), missingClub.u32());

                int consumeId = inv.warehouse(10001).stream()
                        .filter(w -> w.typeid == GamePackets.TYPEID_SHOP_PANG_ITEM)
                        .findFirst()
                        .orElseThrow()
                        .id;
                int before = GamePackets.unixNow();
                client.sendPlain(GamePackets.clientWorkshopTransfer(
                        GamePackets.TYPEID_SHOP_PANG_ITEM, srcId, dstId, 1));
                PacketReader awards = awaitOpcode(client, GamePackets.SERVER_DAILY_QUEST_STAMP);
                int unix = awards.u32();
                assertTrue(unix >= before - 1 && unix <= GamePackets.unixNow() + 1);
                assertEquals(3, awards.u32());
                assertEquals(GamePackets.PAPEL_AWARD_TYPE, awards.u8());
                assertEquals(GamePackets.TYPEID_SHOP_PANG_ITEM, awards.u32());
                assertEquals(consumeId, awards.i32());
                assertEquals(0, awards.u32());
                assertEquals(2, awards.i32());
                assertEquals(1, awards.i32());
                assertEquals(-1, awards.i32());
                awards.readBytes(GamePackets.PAPEL_AWARD_PAD);
                assertEquals(GamePackets.WORKSHOP_AWARD_TYPE, awards.u8());
                assertEquals(GamePackets.TYPEID_AIR_KNIGHT, awards.u32());
                assertEquals(srcId, awards.i32());
                assertEquals(0, awards.u32());
                assertEquals(0, awards.i32());
                assertEquals(0, awards.i32());
                assertEquals(0, awards.i32());
                awards.readBytes(GamePackets.PAPEL_AWARD_PAD);
                for (int i = 0; i < 5; i++) {
                    assertEquals(0, (short) awards.i16());
                }
                assertEquals(0, awards.u32());
                assertEquals(0, awards.u8());
                assertEquals(0, awards.u32());
                assertEquals(0, awards.u32());
                assertEquals(GamePackets.WORKSHOP_AWARD_TYPE, awards.u8());
                assertEquals(GamePackets.TYPEID_AIR_KNIGHT_LUCKY, awards.u32());
                assertEquals(dstId, awards.i32());
                assertEquals(0, awards.u32());
                assertEquals(0, awards.i32());
                assertEquals(0, awards.i32());
                assertEquals(0, awards.i32());
                awards.readBytes(GamePackets.PAPEL_AWARD_PAD);
                for (int i = 0; i < 5; i++) {
                    assertEquals(0, (short) awards.i16());
                }
                assertEquals(300, awards.u32());
                assertEquals(0, awards.u8());
                assertEquals(0, awards.u32());
                assertEquals(0, awards.u32());
                assertEquals(0, awards.remaining());
                PacketReader ok = awaitOpcode(client, GamePackets.SERVER_WORKSHOP_TRANSFER);
                assertEquals(GamePackets.WORKSHOP_TRANSFER_OK, ok.u32());
                assertEquals(0, inv.warehouse(10001).stream()
                        .filter(w -> w.id == srcId)
                        .findFirst()
                        .orElseThrow()
                        .workshopMastery);
                assertEquals(300, inv.warehouse(10001).stream()
                        .filter(w -> w.id == dstId)
                        .findFirst()
                        .orElseThrow()
                        .workshopMastery);

                client.sendPlain(GamePackets.clientWorkshopTransfer(0, srcId, dstId, 1));
                PacketReader missingUcim = awaitOpcode(client, GamePackets.SERVER_WORKSHOP_TRANSFER);
                assertEquals(GamePackets.shopSys(GamePackets.WORKSHOP_TRANSFER_ERR), missingUcim.u32());
            } finally {
                inv.setClubSetMasteryPts(10001, srcId, 0);
                inv.setClubSetMasteryPts(10001, dstId, 0);
                inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_AIR_KNIGHT_LUCKY);
                inv.deleteClubSetIff(GamePackets.TYPEID_AIR_KNIGHT);
                inv.deleteClubSetIff(GamePackets.TYPEID_AIR_KNIGHT_LUCKY);
                inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
            }
        }
    }

    @Test
    void rentalExtendAndDeletePart() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient client = new PangyaFakeClient()) {
            InventoryRepository inv = new JdbiInventoryRepository(DatabaseSupport.jdbi(ds));
            long pang = inv.pang(10001);
            long cookie = inv.cookie(10001);
            inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_RENTAL_PART);
            inv.deletePartIff(GamePackets.TYPEID_RENTAL_PART);
            int partId = inv.addWarehouseItem(10001, GamePackets.TYPEID_RENTAL_PART, 1);
            try {
                LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(ds));
                String loginKey = repo.generateAuthKeyLogin(10001);
                String gameKey = repo.generateAuthKeyGame(10001, 20202);
                keys.putLoginKey(10001, loginKey);
                keys.putGameKey(10001, 20202, gameKey);
                loginToChannel(client, runtime.port(), "testuser", 10001, loginKey, gameKey);

                client.sendPlain(GamePackets.clientRental(GamePackets.CLIENT_EXTEND_RENTAL, partId));
                PacketReader missingIff = awaitOpcode(client, GamePackets.SERVER_EXTEND_RENTAL);
                assertEquals(GamePackets.RENTAL_FAIL, missingIff.u8());

                inv.upsertPartValorRental(GamePackets.TYPEID_RENTAL_PART, 100);
                client.sendPlain(GamePackets.clientRental(GamePackets.CLIENT_EXTEND_RENTAL, partId));
                PacketReader spent = awaitOpcode(client, GamePackets.SERVER_PANG_SPENT);
                assertEquals(pang - 100, spent.u64());
                assertEquals(100, spent.u64());
                PacketReader extendOk = awaitOpcode(client, GamePackets.SERVER_EXTEND_RENTAL);
                assertEquals(GamePackets.RENTAL_OK, extendOk.u8());
                assertEquals(GamePackets.TYPEID_RENTAL_PART, extendOk.u32());
                assertEquals(partId, extendOk.i32());
                assertEquals(pang - 100, inv.pang(10001));

                client.sendPlain(GamePackets.clientRental(GamePackets.CLIENT_DELETE_RENTAL, partId));
                PacketReader deleteOk = awaitOpcode(client, GamePackets.SERVER_DELETE_RENTAL);
                assertEquals(GamePackets.RENTAL_OK, deleteOk.u8());
                assertEquals(GamePackets.TYPEID_RENTAL_PART, deleteOk.u32());
                assertEquals(partId, deleteOk.i32());
                assertTrue(inv.warehouse(10001).stream().noneMatch(w -> w.id == partId));

                client.sendPlain(GamePackets.clientRental(GamePackets.CLIENT_DELETE_RENTAL, partId));
                PacketReader missing = awaitOpcode(client, GamePackets.SERVER_DELETE_RENTAL);
                assertEquals(GamePackets.RENTAL_FAIL, missing.u8());
                client.sendPlain(GamePackets.clientRental(GamePackets.CLIENT_EXTEND_RENTAL, 0));
                PacketReader zero = awaitOpcode(client, GamePackets.SERVER_EXTEND_RENTAL);
                assertEquals(GamePackets.RENTAL_FAIL, zero.u8());
            } finally {
                inv.setPangCookie(10001, pang, cookie);
                inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_RENTAL_PART);
                inv.deletePartIff(GamePackets.TYPEID_RENTAL_PART);
            }
        }
    }

    @Test
    void clubSetEnchantUpAndDownPower() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient client = new PangyaFakeClient()) {
            InventoryRepository inv = new JdbiInventoryRepository(DatabaseSupport.jdbi(ds));
            long pang = inv.pang(10001);
            long cookie = inv.cookie(10001);
            GamePackets.WarehouseItem club = inv.warehouse(10001).stream()
                    .filter(w -> w.typeid == GamePackets.TYPEID_AIR_KNIGHT)
                    .findFirst()
                    .orElseThrow();
            int clubId = club.id;
            short[] origC = club.c.clone();
            inv.deleteClubSetIff(GamePackets.TYPEID_AIR_KNIGHT);
            try {
                LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(ds));
                String loginKey = repo.generateAuthKeyLogin(10001);
                String gameKey = repo.generateAuthKeyGame(10001, 20202);
                keys.putLoginKey(10001, loginKey);
                keys.putGameKey(10001, 20202, gameKey);
                loginToChannel(client, runtime.port(), "testuser", 10001, loginKey, gameKey);

                client.sendPlain(GamePackets.clientEnchant(
                        1, GamePackets.CHAR_STATS_POWER, clubId));
                PacketReader missingIff = awaitOpcode(client, GamePackets.SERVER_CLUB_STATS);
                assertEquals(GamePackets.CLUB_STATS_ERR, missingIff.u8());

                inv.upsertClubSetIff(
                        GamePackets.TYPEID_AIR_KNIGHT,
                        0,
                        new short[5],
                        new short[] {1, 0, 0, 0, 0});
                client.sendPlain(GamePackets.clientEnchant(
                        1, GamePackets.CHAR_STATS_POWER, clubId));
                PacketReader up = awaitOpcode(client, GamePackets.SERVER_CLUB_STATS);
                assertEquals(GamePackets.CLUB_STATS_UP, up.u8());
                assertEquals(GamePackets.CLUB_STATS_CLUBSET, up.u8());
                assertEquals(GamePackets.CHAR_STATS_POWER, up.u8());
                assertEquals(clubId, up.i32());
                assertEquals(GamePackets.CHAR_STATS_ENCHANT_PANG, up.u64());
                assertEquals(pang - GamePackets.CHAR_STATS_ENCHANT_PANG, inv.pang(10001));
                assertEquals(1, inv.warehouse(10001).stream()
                        .filter(w -> w.id == clubId)
                        .findFirst()
                        .orElseThrow()
                        .c[0]);

                client.sendPlain(GamePackets.clientEnchant(
                        3, GamePackets.CHAR_STATS_POWER, clubId));
                PacketReader down = awaitOpcode(client, GamePackets.SERVER_CLUB_STATS);
                assertEquals(GamePackets.CLUB_STATS_DOWN, down.u8());
                assertEquals(GamePackets.CLUB_STATS_CLUBSET, down.u8());
                assertEquals(GamePackets.CHAR_STATS_POWER, down.u8());
                assertEquals(clubId, down.i32());
                assertEquals(0, down.u64());
                assertEquals(0, inv.warehouse(10001).stream()
                        .filter(w -> w.id == clubId)
                        .findFirst()
                        .orElseThrow()
                        .c[0]);

                client.sendPlain(GamePackets.clientEnchant(1, GamePackets.CHAR_STATS_POWER, 0));
                PacketReader zero = awaitOpcode(client, GamePackets.SERVER_CLUB_STATS);
                assertEquals(GamePackets.CLUB_STATS_ERR, zero.u8());
            } finally {
                inv.setPangCookie(10001, pang, cookie);
                inv.setWarehouseClubC(10001, clubId, origC);
                inv.deleteClubSetIff(GamePackets.TYPEID_AIR_KNIGHT);
            }
        }
    }

    @Test
    void clubSetResetSoftClearsWorkshop() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient client = new PangyaFakeClient()) {
            InventoryRepository inv = new JdbiInventoryRepository(DatabaseSupport.jdbi(ds));
            GamePackets.WarehouseItem club = inv.warehouse(10001).stream()
                    .filter(w -> w.typeid == GamePackets.TYPEID_AIR_KNIGHT)
                    .findFirst()
                    .orElseThrow();
            int clubId = club.id;
            short[] origC = club.c.clone();
            short[] origW = club.workshopC.clone();
            int origLevel = club.workshopLevel;
            int origRank = club.workshopRank;
            int origRecovery = club.workshopRecovery;
            int origMastery = club.workshopMastery;
            inv.deleteClubSetIff(GamePackets.TYPEID_AIR_KNIGHT);
            inv.deleteClubSetRankExp(0);
            inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_CLUBSET_RESET_SOFT);
            inv.addWarehouseItem(10001, GamePackets.TYPEID_CLUBSET_RESET_SOFT, 1);
            inv.setWarehouseClubC(10001, clubId, new short[] {1, 0, 0, 0, 0});
            inv.setClubSetWorkshop(10001, clubId, new short[] {1, 0, 0, 0, 0}, 2, 1, 5);
            inv.setClubSetMasteryPts(10001, clubId, 300);
            int consumeId = inv.warehouse(10001).stream()
                    .filter(w -> w.typeid == GamePackets.TYPEID_CLUBSET_RESET_SOFT)
                    .findFirst()
                    .orElseThrow()
                    .id;
            try {
                LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(ds));
                String loginKey = repo.generateAuthKeyLogin(10001);
                String gameKey = repo.generateAuthKeyGame(10001, 20202);
                keys.putLoginKey(10001, loginKey);
                keys.putGameKey(10001, 20202, gameKey);
                loginToChannel(client, runtime.port(), "testuser", 10001, loginKey, gameKey);

                client.sendPlain(GamePackets.clientWorkshopTypeidClub(
                        GamePackets.CLIENT_CLUBSET_RESET,
                        GamePackets.TYPEID_CLUBSET_RESET_SOFT,
                        clubId));
                PacketReader missingIff = awaitOpcode(client, GamePackets.SERVER_CLUBSET_RESET);
                assertEquals(GamePackets.shopSys(GamePackets.CLUBSET_RESET_ERR_IFF), missingIff.u32());

                inv.upsertClubSetIff(
                        GamePackets.TYPEID_AIR_KNIGHT,
                        0,
                        new short[5],
                        new short[] {6, 6, 6, 6, 6},
                        0);
                client.sendPlain(GamePackets.clientWorkshopTypeidClub(
                        GamePackets.CLIENT_CLUBSET_RESET,
                        GamePackets.TYPEID_CLUBSET_RESET_SOFT,
                        clubId));
                PacketReader missingExp = awaitOpcode(client, GamePackets.SERVER_CLUBSET_RESET);
                assertEquals(GamePackets.shopSys(GamePackets.CLUBSET_RESET_ERR_RANK_EXP), missingExp.u32());

                inv.upsertClubSetRankExp(0);
                int before = GamePackets.unixNow();
                client.sendPlain(GamePackets.clientWorkshopTypeidClub(
                        GamePackets.CLIENT_CLUBSET_RESET,
                        GamePackets.TYPEID_CLUBSET_RESET_SOFT,
                        clubId));
                PacketReader awards = awaitOpcode(client, GamePackets.SERVER_DAILY_QUEST_STAMP);
                int unix = awards.u32();
                assertTrue(unix >= before - 1 && unix <= GamePackets.unixNow() + 1);
                assertEquals(3, awards.u32());
                assertEquals(GamePackets.PAPEL_AWARD_TYPE, awards.u8());
                assertEquals(GamePackets.TYPEID_CLUBSET_RESET_SOFT, awards.u32());
                assertEquals(consumeId, awards.i32());
                assertEquals(0, awards.u32());
                assertEquals(1, awards.i32());
                assertEquals(0, awards.i32());
                assertEquals(-1, awards.i32());
                awards.readBytes(GamePackets.PAPEL_AWARD_PAD);
                assertEquals(GamePackets.WORKSHOP_AWARD_TYPE, awards.u8());
                assertEquals(GamePackets.TYPEID_AIR_KNIGHT, awards.u32());
                assertEquals(clubId, awards.i32());
                assertEquals(0, awards.u32());
                assertEquals(0, awards.i32());
                assertEquals(0, awards.i32());
                assertEquals(0, awards.i32());
                awards.readBytes(GamePackets.PAPEL_AWARD_PAD);
                for (int i = 0; i < 5; i++) {
                    assertEquals(0, awards.i16());
                }
                assertEquals(300, awards.u32());
                assertEquals(0, awards.u8());
                assertEquals(0, awards.u32());
                assertEquals(0, awards.u32());
                assertEquals(GamePackets.CHAR_STATS_AWARD_TYPE, awards.u8());
                assertEquals(GamePackets.TYPEID_AIR_KNIGHT, awards.u32());
                assertEquals(clubId, awards.i32());
                assertEquals(0, awards.u32());
                assertEquals(0, awards.i32());
                assertEquals(0, awards.i32());
                assertEquals(0, awards.i32());
                awards.readBytes(GamePackets.PAPEL_AWARD_PAD);
                assertEquals(0, awards.remaining());
                PacketReader ok = awaitOpcode(client, GamePackets.SERVER_CLUBSET_RESET);
                assertEquals(GamePackets.CLUBSET_RESET_OK, ok.u32());
                assertEquals(GamePackets.TYPEID_AIR_KNIGHT, ok.u32());
                assertEquals(clubId, ok.i32());
                GamePackets.WarehouseItem after = inv.warehouse(10001).stream()
                        .filter(w -> w.id == clubId)
                        .findFirst()
                        .orElseThrow();
                assertEquals(0, after.c[0]);
                assertEquals(0, after.workshopRecovery);
                assertEquals(0, after.workshopLevel);
                assertEquals(0, after.workshopRank);
                assertEquals(300, after.workshopMastery);

                client.sendPlain(GamePackets.clientWorkshopTypeidClub(
                        GamePackets.CLIENT_CLUBSET_RESET,
                        GamePackets.TYPEID_CLUBSET_RESET_SOFT,
                        clubId));
                PacketReader missingItem = awaitOpcode(client, GamePackets.SERVER_CLUBSET_RESET);
                assertEquals(GamePackets.shopSys(GamePackets.CLUBSET_RESET_ERR_ITEM), missingItem.u32());
                client.sendPlain(GamePackets.clientWorkshopTypeidClub(
                        GamePackets.CLIENT_CLUBSET_RESET, 0, 0));
                PacketReader zero = awaitOpcode(client, GamePackets.SERVER_CLUBSET_RESET);
                assertEquals(GamePackets.shopSys(GamePackets.CLUBSET_RESET_ERR), zero.u32());
            } finally {
                inv.setWarehouseClubC(10001, clubId, origC);
                inv.setClubSetWorkshop(10001, clubId, origW, origLevel, origRank, origRecovery);
                inv.setClubSetMasteryPts(10001, clubId, origMastery);
                inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_CLUBSET_RESET_SOFT);
                inv.deleteClubSetIff(GamePackets.TYPEID_AIR_KNIGHT);
                inv.deleteClubSetRankExp(0);
            }
        }
    }

    @Test
    void workshopUpLevelThenConfirmAndCancel() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient client = new PangyaFakeClient()) {
            InventoryRepository inv = new JdbiInventoryRepository(DatabaseSupport.jdbi(ds));
            GamePackets.WarehouseItem club = inv.warehouse(10001).stream()
                    .filter(w -> w.typeid == GamePackets.TYPEID_AIR_KNIGHT)
                    .findFirst()
                    .orElseThrow();
            int clubId = club.id;
            short[] origC = club.c.clone();
            short[] origW = club.workshopC.clone();
            int origLevel = club.workshopLevel;
            int origRank = club.workshopRank;
            int origRecovery = club.workshopRecovery;
            int origMastery = club.workshopMastery;
            inv.deleteClubSetIff(GamePackets.TYPEID_AIR_KNIGHT);
            inv.deleteItemIff(GamePackets.TYPEID_SHOP_PANG_ITEM);
            inv.deleteClubSetLevelUpLimit(0, 0);
            inv.deleteClubSetLevelUpProb(0);
            inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
            inv.addWarehouseItem(10001, GamePackets.TYPEID_SHOP_PANG_ITEM, 2);
            inv.setClubSetWorkshop(10001, clubId, new short[5], 0, 0, 0);
            int consumeId = inv.warehouse(10001).stream()
                    .filter(w -> w.typeid == GamePackets.TYPEID_SHOP_PANG_ITEM)
                    .findFirst()
                    .orElseThrow()
                    .id;
            try {
                LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(ds));
                String loginKey = repo.generateAuthKeyLogin(10001);
                String gameKey = repo.generateAuthKeyGame(10001, 20202);
                keys.putLoginKey(10001, loginKey);
                keys.putGameKey(10001, 20202, gameKey);
                loginToChannel(client, runtime.port(), "testuser", 10001, loginKey, gameKey);

                client.sendPlain(GamePackets.clientClubWorkshopLevel(
                        GamePackets.TYPEID_SHOP_PANG_ITEM, 1, clubId));
                PacketReader missingIff = awaitOpcode(client, GamePackets.SERVER_CLUB_WORKSHOP_LEVEL);
                assertEquals(GamePackets.shopSys(GamePackets.WORKSHOP_ERR_IFF_ITEM), missingIff.u32());

                inv.upsertItemIff(GamePackets.TYPEID_SHOP_PANG_ITEM);
                client.sendPlain(GamePackets.clientClubWorkshopLevel(
                        GamePackets.TYPEID_SHOP_PANG_ITEM, 1, clubId));
                PacketReader missingClubIff = awaitOpcode(client, GamePackets.SERVER_CLUB_WORKSHOP_LEVEL);
                assertEquals(GamePackets.shopSys(GamePackets.WORKSHOP_ERR_IFF_CLUB), missingClubIff.u32());

                inv.upsertClubSetIff(
                        GamePackets.TYPEID_AIR_KNIGHT,
                        0,
                        new short[5],
                        new short[] {6, 6, 6, 6, 6},
                        0,
                        5);
                client.sendPlain(GamePackets.clientClubWorkshopLevel(
                        GamePackets.TYPEID_SHOP_PANG_ITEM, 1, clubId));
                PacketReader missingLimit = awaitOpcode(client, GamePackets.SERVER_CLUB_WORKSHOP_LEVEL);
                assertEquals(GamePackets.shopSys(GamePackets.WORKSHOP_ERR_LIMIT), missingLimit.u32());

                inv.upsertClubSetLevelUpProb(0, new int[] {100, 0, 0, 0, 0});
                inv.upsertClubSetLevelUpLimit(0, 0, new short[] {7, 0, 0, 0, 0});
                int before = GamePackets.unixNow();
                client.sendPlain(GamePackets.clientClubWorkshopLevel(
                        GamePackets.TYPEID_SHOP_PANG_ITEM, 1, clubId));
                PacketReader awards = awaitOpcode(client, GamePackets.SERVER_DAILY_QUEST_STAMP);
                int unix = awards.u32();
                assertTrue(unix >= before - 1 && unix <= GamePackets.unixNow() + 1);
                assertEquals(1, awards.u32());
                assertEquals(GamePackets.PAPEL_AWARD_TYPE, awards.u8());
                assertEquals(GamePackets.TYPEID_SHOP_PANG_ITEM, awards.u32());
                assertEquals(consumeId, awards.i32());
                assertEquals(0, awards.u32());
                assertEquals(2, awards.i32());
                assertEquals(1, awards.i32());
                assertEquals(-1, awards.i32());
                awards.readBytes(GamePackets.PAPEL_AWARD_PAD);
                assertEquals(0, awards.remaining());
                PacketReader levelOk = awaitOpcode(client, GamePackets.SERVER_CLUB_WORKSHOP_LEVEL);
                assertEquals(GamePackets.WORKSHOP_OK, levelOk.u32());
                assertEquals(0, levelOk.u32());
                GamePackets.WarehouseItem afterLevel = inv.warehouse(10001).stream()
                        .filter(w -> w.id == clubId)
                        .findFirst()
                        .orElseThrow();
                assertEquals(1, afterLevel.workshopC[0]);

                client.sendPlain(GamePackets.clientClubWorkshopEmpty(
                        GamePackets.CLIENT_CLUB_WORKSHOP_CONFIRM));
                PacketReader confirmUpd = awaitOpcode(client, GamePackets.SERVER_DAILY_QUEST_STAMP);
                confirmUpd.u32();
                assertEquals(1, confirmUpd.u32());
                assertEquals(GamePackets.WORKSHOP_AWARD_TYPE, confirmUpd.u8());
                assertEquals(GamePackets.TYPEID_AIR_KNIGHT, confirmUpd.u32());
                assertEquals(clubId, confirmUpd.i32());
                assertEquals(0, confirmUpd.u32());
                assertEquals(0, confirmUpd.i32());
                assertEquals(0, confirmUpd.i32());
                assertEquals(0, confirmUpd.i32());
                confirmUpd.readBytes(GamePackets.PAPEL_AWARD_PAD);
                assertEquals(1, confirmUpd.i16());
                for (int i = 0; i < 4; i++) {
                    assertEquals(0, confirmUpd.i16());
                }
                assertEquals(origMastery, confirmUpd.u32());
                assertEquals(0, confirmUpd.u8());
                assertEquals(0, confirmUpd.u32());
                assertEquals(0, confirmUpd.u32());
                assertEquals(0, confirmUpd.remaining());
                PacketReader confirmOk = awaitOpcode(client, GamePackets.SERVER_CLUB_WORKSHOP_CONFIRM);
                assertEquals(GamePackets.WORKSHOP_CONFIRM_OK, confirmOk.u32());
                assertEquals(0, confirmOk.u32());
                assertEquals(clubId, confirmOk.i32());

                inv.setClubSetWorkshop(10001, clubId, new short[5], 0, 0, 0);
                client.sendPlain(GamePackets.clientClubWorkshopLevel(
                        GamePackets.TYPEID_SHOP_PANG_ITEM, 1, clubId));
                PacketReader awards2 = awaitOpcode(client, GamePackets.SERVER_DAILY_QUEST_STAMP);
                awards2.u32();
                assertEquals(1, awards2.u32());
                PacketReader levelOk2 = awaitOpcode(client, GamePackets.SERVER_CLUB_WORKSHOP_LEVEL);
                assertEquals(GamePackets.WORKSHOP_OK, levelOk2.u32());
                assertEquals(0, levelOk2.u32());
                client.sendPlain(GamePackets.clientClubWorkshopEmpty(
                        GamePackets.CLIENT_CLUB_WORKSHOP_CANCEL));
                PacketReader cancelUpd = awaitOpcode(client, GamePackets.SERVER_DAILY_QUEST_STAMP);
                cancelUpd.u32();
                assertEquals(1, cancelUpd.u32());
                assertEquals(GamePackets.WORKSHOP_AWARD_TYPE, cancelUpd.u8());
                assertEquals(GamePackets.TYPEID_AIR_KNIGHT, cancelUpd.u32());
                assertEquals(clubId, cancelUpd.i32());
                assertEquals(0, cancelUpd.u32());
                assertEquals(0, cancelUpd.i32());
                assertEquals(0, cancelUpd.i32());
                assertEquals(0, cancelUpd.i32());
                cancelUpd.readBytes(GamePackets.PAPEL_AWARD_PAD);
                for (int i = 0; i < 5; i++) {
                    assertEquals(0, cancelUpd.i16());
                }
                assertEquals(origMastery, cancelUpd.u32());
                assertEquals(0, cancelUpd.u8());
                assertEquals(0, cancelUpd.u32());
                assertEquals(1, cancelUpd.u32());
                assertEquals(0, cancelUpd.remaining());
                PacketReader cancelOk = awaitOpcode(client, GamePackets.SERVER_CLUB_WORKSHOP_CANCEL);
                assertEquals(GamePackets.WORKSHOP_CANCEL_OK, cancelOk.u32());
                assertEquals(clubId, cancelOk.i32());
                GamePackets.WarehouseItem afterCancel = inv.warehouse(10001).stream()
                        .filter(w -> w.id == clubId)
                        .findFirst()
                        .orElseThrow();
                assertEquals(0, afterCancel.workshopC[0]);
                assertEquals(1, afterCancel.workshopRecovery);

                client.sendPlain(GamePackets.clientClubWorkshopLevel(0, 1, 0));
                PacketReader zero = awaitOpcode(client, GamePackets.SERVER_CLUB_WORKSHOP_LEVEL);
                assertEquals(GamePackets.shopSys(GamePackets.WORKSHOP_ERR_GROUP), zero.u32());
            } finally {
                inv.setWarehouseClubC(10001, clubId, origC);
                inv.setClubSetWorkshop(10001, clubId, origW, origLevel, origRank, origRecovery);
                inv.setClubSetMasteryPts(10001, clubId, origMastery);
                inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
                inv.deleteClubSetIff(GamePackets.TYPEID_AIR_KNIGHT);
                inv.deleteItemIff(GamePackets.TYPEID_SHOP_PANG_ITEM);
                inv.deleteClubSetLevelUpLimit(0, 0);
                inv.deleteClubSetLevelUpProb(0);
            }
        }
    }

    @Test
    void workshopUpRankPersistsCcThen240() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient client = new PangyaFakeClient()) {
            InventoryRepository inv = new JdbiInventoryRepository(DatabaseSupport.jdbi(ds));
            GamePackets.WarehouseItem club = inv.warehouse(10001).stream()
                    .filter(w -> w.typeid == GamePackets.TYPEID_AIR_KNIGHT)
                    .findFirst()
                    .orElseThrow();
            int clubId = club.id;
            short[] origC = club.c.clone();
            short[] origW = club.workshopC.clone();
            int origLevel = club.workshopLevel;
            int origRank = club.workshopRank;
            int origRecovery = club.workshopRecovery;
            int origMastery = club.workshopMastery;
            inv.deleteClubSetIff(GamePackets.TYPEID_AIR_KNIGHT);
            inv.deleteClubSetLevelUpLimit(0, 0);
            inv.deleteClubSetLevelUpLimit(0, 1);
            inv.deleteClubSetRankExp(0);
            inv.setClubSetWorkshop(10001, clubId, new short[5], 0, 0, 0);
            inv.setClubSetMasteryPts(10001, clubId, 300);
            try {
                LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(ds));
                String loginKey = repo.generateAuthKeyLogin(10001);
                String gameKey = repo.generateAuthKeyGame(10001, 20202);
                keys.putLoginKey(10001, loginKey);
                keys.putGameKey(10001, 20202, gameKey);
                loginToChannel(client, runtime.port(), "testuser", 10001, loginKey, gameKey);

                client.sendPlain(GamePackets.clientClubWorkshopRank(0, 0, 0));
                PacketReader missingClub = awaitOpcode(client, GamePackets.SERVER_CLUB_WORKSHOP_RANK);
                assertEquals(GamePackets.shopSys(GamePackets.WORKSHOP_RANK_ERR_CLUB), missingClub.u32());

                client.sendPlain(GamePackets.clientClubWorkshopRank(0, 0, clubId));
                PacketReader missingIff = awaitOpcode(client, GamePackets.SERVER_CLUB_WORKSHOP_RANK);
                assertEquals(GamePackets.shopSys(GamePackets.WORKSHOP_RANK_ERR_IFF), missingIff.u32());

                inv.upsertClubSetIff(
                        GamePackets.TYPEID_AIR_KNIGHT,
                        0,
                        new short[5],
                        new short[] {6, 6, 6, 6, 6},
                        0,
                        5);
                client.sendPlain(GamePackets.clientClubWorkshopRank(0, 0, clubId));
                PacketReader missingLimit = awaitOpcode(client, GamePackets.SERVER_CLUB_WORKSHOP_RANK);
                assertEquals(GamePackets.shopSys(GamePackets.WORKSHOP_ERR_LIMIT), missingLimit.u32());

                inv.upsertClubSetLevelUpLimit(0, 0, new short[] {7, 0, 0, 0, 0});
                client.sendPlain(GamePackets.clientClubWorkshopRank(0, 0, clubId));
                PacketReader wrongRank = awaitOpcode(client, GamePackets.SERVER_CLUB_WORKSHOP_RANK);
                assertEquals(GamePackets.shopSys(GamePackets.WORKSHOP_ERR_LIMIT_RANK), wrongRank.u32());

                inv.deleteClubSetLevelUpLimit(0, 0);
                inv.upsertClubSetLevelUpLimit(0, 1, new short[] {0, 0, 7, 0, 0});
                client.sendPlain(GamePackets.clientClubWorkshopRank(0, 0, clubId));
                PacketReader missingExp = awaitOpcode(client, GamePackets.SERVER_CLUB_WORKSHOP_RANK);
                assertEquals(GamePackets.shopSys(GamePackets.WORKSHOP_RANK_ERR_EXP), missingExp.u32());

                inv.upsertClubSetRankExp(0, new int[] {0, 50, 0, 0, 0, 0});
                int before = GamePackets.unixNow();
                client.sendPlain(GamePackets.clientClubWorkshopRank(0, 0, clubId));
                PacketReader upd = awaitOpcode(client, GamePackets.SERVER_DAILY_QUEST_STAMP);
                int unix = upd.u32();
                assertTrue(unix >= before - 1 && unix <= GamePackets.unixNow() + 1);
                assertEquals(1, upd.u32());
                assertEquals(GamePackets.WORKSHOP_AWARD_TYPE, upd.u8());
                assertEquals(GamePackets.TYPEID_AIR_KNIGHT, upd.u32());
                assertEquals(clubId, upd.i32());
                assertEquals(0, upd.u32());
                assertEquals(0, upd.i32());
                assertEquals(0, upd.i32());
                assertEquals(0, upd.i32());
                upd.readBytes(GamePackets.PAPEL_AWARD_PAD);
                assertEquals(0, upd.i16());
                assertEquals(0, upd.i16());
                assertEquals(1, upd.i16());
                assertEquals(0, upd.i16());
                assertEquals(0, upd.i16());
                assertEquals(250, upd.u32());
                assertEquals(1, upd.u8());
                assertEquals(1, upd.u32());
                assertEquals(0, upd.u32());
                assertEquals(0, upd.remaining());
                PacketReader rankOk = awaitOpcode(client, GamePackets.SERVER_CLUB_WORKSHOP_RANK);
                assertEquals(GamePackets.WORKSHOP_RANK_OK, rankOk.u32());
                assertEquals(2, rankOk.u32());
                assertEquals(clubId, rankOk.i32());
                GamePackets.WarehouseItem after = inv.warehouse(10001).stream()
                        .filter(w -> w.id == clubId)
                        .findFirst()
                        .orElseThrow();
                assertEquals(1, after.workshopC[2]);
                assertEquals(1, after.workshopLevel);
                assertEquals(1, after.workshopRank);
                assertEquals(0, after.workshopRecovery);
                assertEquals(250, after.workshopMastery);

                client.sendPlain(GamePackets.clientClubWorkshopRank(0, 1, 0));
                PacketReader mega = awaitOpcode(client, GamePackets.SERVER_CLUB_WORKSHOP_RANK);
                assertEquals(GamePackets.shopSys(GamePackets.WORKSHOP_RANK_ERR), mega.u32());
            } finally {
                inv.setWarehouseClubC(10001, clubId, origC);
                inv.setClubSetWorkshop(10001, clubId, origW, origLevel, origRank, origRecovery);
                inv.setClubSetMasteryPts(10001, clubId, origMastery);
                inv.deleteClubSetIff(GamePackets.TYPEID_AIR_KNIGHT);
                inv.deleteClubSetLevelUpLimit(0, 0);
                inv.deleteClubSetLevelUpLimit(0, 1);
                inv.deleteClubSetRankExp(0);
            }
        }
    }

    @Test
    void workshopTransformCancelThenConfirm() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient client = new PangyaFakeClient()) {
            InventoryRepository inv = new JdbiInventoryRepository(DatabaseSupport.jdbi(ds));
            inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_WORKSHOP_TRANSFORM_SRC);
            inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_WORKSHOP_TRANSFORM_ORIGINAL);
            int srcId = inv.addWarehouseItem(10001, GamePackets.TYPEID_WORKSHOP_TRANSFORM_SRC, 1);
            inv.deleteClubSetIff(GamePackets.TYPEID_WORKSHOP_TRANSFORM_SRC);
            inv.deleteClubSetIff(GamePackets.TYPEID_WORKSHOP_TRANSFORM_ORIGINAL);
            inv.deleteClubSetLevelUpLimit(1, 1);
            inv.deleteClubSetRankExp(1);
            inv.deleteClubSetOriginal(GamePackets.TYPEID_WINGTROSS_EVO);
            inv.setClubSetWorkshop(10001, srcId, new short[5], 0, 0, 0);
            inv.setClubSetMasteryPts(10001, srcId, 300);
            try {
                LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(ds));
                String loginKey = repo.generateAuthKeyLogin(10001);
                String gameKey = repo.generateAuthKeyGame(10001, 20202);
                keys.putLoginKey(10001, loginKey);
                keys.putGameKey(10001, 20202, gameKey);
                loginToChannel(client, runtime.port(), "testuser", 10001, loginKey, gameKey);

                inv.upsertClubSetIff(
                        GamePackets.TYPEID_WORKSHOP_TRANSFORM_SRC,
                        1,
                        new short[5],
                        new short[] {6, 6, 6, 6, 6},
                        1,
                        5,
                        1);
                inv.upsertClubSetLevelUpLimit(1, 1, new short[] {0, 0, 7, 0, 0});
                inv.upsertClubSetRankExp(1, new int[] {0, 50, 0, 0, 0, 0});
                inv.upsertClubSetOriginal(
                        GamePackets.TYPEID_WINGTROSS_EVO,
                        GamePackets.TYPEID_WORKSHOP_TRANSFORM_ORIGINAL,
                        new short[] {7, 7, 7, 7, 7});

                client.sendPlain(GamePackets.clientClubWorkshopRank(0, 0, srcId));
                PacketReader upd = awaitOpcode(client, GamePackets.SERVER_DAILY_QUEST_STAMP);
                upd.u32();
                assertEquals(1, upd.u32());
                PacketReader dialog = awaitOpcode(client, GamePackets.SERVER_CLUB_WORKSHOP_TRANSFORM);
                assertEquals(0, dialog.remaining());

                client.sendPlain(GamePackets.clientClubWorkshopEmpty(
                        GamePackets.CLIENT_WORKSHOP_TRANSFORM_CANCEL));
                PacketReader cancelOk = awaitOpcode(client, GamePackets.SERVER_WORKSHOP_TRANSFORM_CANCEL);
                assertEquals(GamePackets.WORKSHOP_TRANSFORM_CANCEL_OK, cancelOk.u32());
                assertEquals(2, cancelOk.u32());
                assertEquals(srcId, cancelOk.i32());
                assertTrue(inv.warehouse(10001).stream().anyMatch(w -> w.id == srcId));

                inv.setClubSetWorkshop(10001, srcId, new short[5], 0, 0, 0);
                inv.setClubSetMasteryPts(10001, srcId, 300);
                client.sendPlain(GamePackets.clientClubWorkshopRank(0, 0, srcId));
                PacketReader upd2 = awaitOpcode(client, GamePackets.SERVER_DAILY_QUEST_STAMP);
                upd2.u32();
                assertEquals(1, upd2.u32());
                PacketReader dialog2 = awaitOpcode(client, GamePackets.SERVER_CLUB_WORKSHOP_TRANSFORM);
                assertEquals(0, dialog2.remaining());

                client.sendPlain(GamePackets.clientClubWorkshopEmpty(
                        GamePackets.CLIENT_WORKSHOP_TRANSFORM_CONFIRM));
                PacketReader missingSpecial = awaitOpcode(
                        client, GamePackets.SERVER_WORKSHOP_TRANSFORM_CONFIRM);
                assertEquals(
                        GamePackets.shopSys(GamePackets.WORKSHOP_TRANSFORM_CONFIRM_ERR_SPECIAL),
                        missingSpecial.u32());

                inv.upsertClubSetIff(
                        GamePackets.TYPEID_WORKSHOP_TRANSFORM_ORIGINAL,
                        1,
                        new short[5],
                        new short[] {7, 7, 7, 7, 7});
                int before = GamePackets.unixNow();
                client.sendPlain(GamePackets.clientClubWorkshopEmpty(
                        GamePackets.CLIENT_WORKSHOP_TRANSFORM_CONFIRM));
                PacketReader awards = awaitOpcode(client, GamePackets.SERVER_DAILY_QUEST_STAMP);
                int unix = awards.u32();
                assertTrue(unix >= before - 1 && unix <= GamePackets.unixNow() + 1);
                assertEquals(2, awards.u32());
                assertEquals(GamePackets.PAPEL_AWARD_TYPE, awards.u8());
                assertEquals(GamePackets.TYPEID_WORKSHOP_TRANSFORM_SRC, awards.u32());
                assertEquals(srcId, awards.i32());
                assertEquals(0, awards.u32());
                assertEquals(0, awards.i32());
                assertEquals(0, awards.i32());
                assertEquals(-1, awards.i32());
                awards.readBytes(GamePackets.PAPEL_AWARD_PAD);
                assertEquals(GamePackets.PAPEL_AWARD_TYPE, awards.u8());
                assertEquals(GamePackets.TYPEID_WORKSHOP_TRANSFORM_ORIGINAL, awards.u32());
                int newId = awards.i32();
                assertEquals(0, awards.u32());
                assertEquals(0, awards.i32());
                assertEquals(0, awards.i32());
                assertEquals(1, awards.i32());
                awards.readBytes(GamePackets.PAPEL_AWARD_PAD);
                assertEquals(0, awards.remaining());
                PacketReader confirmOk = awaitOpcode(client, GamePackets.SERVER_WORKSHOP_TRANSFORM_CONFIRM);
                assertEquals(GamePackets.WORKSHOP_TRANSFORM_CONFIRM_OK, confirmOk.u32());
                assertEquals(GamePackets.TYPEID_WORKSHOP_TRANSFORM_ORIGINAL, confirmOk.u32());
                assertEquals(newId, confirmOk.i32());
                assertTrue(inv.warehouse(10001).stream().noneMatch(w -> w.id == srcId));
                assertTrue(inv.warehouse(10001).stream().anyMatch(
                        w -> w.id == newId && w.typeid == GamePackets.TYPEID_WORKSHOP_TRANSFORM_ORIGINAL));
                assertTrue(inv.warehouse(10001).stream().anyMatch(
                        w -> w.typeid == GamePackets.TYPEID_AIR_KNIGHT));
            } finally {
                inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_WORKSHOP_TRANSFORM_SRC);
                inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_WORKSHOP_TRANSFORM_ORIGINAL);
                inv.deleteClubSetIff(GamePackets.TYPEID_WORKSHOP_TRANSFORM_SRC);
                inv.deleteClubSetIff(GamePackets.TYPEID_WORKSHOP_TRANSFORM_ORIGINAL);
                inv.deleteClubSetLevelUpLimit(1, 1);
                inv.deleteClubSetRankExp(1);
                inv.deleteClubSetOriginal(GamePackets.TYPEID_WINGTROSS_EVO);
            }
        }
    }

    @Test
    void makeTutorialRookieAcksFlagsAndMailsReward() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient client = new PangyaFakeClient()) {
            InventoryRepository inv = new JdbiInventoryRepository(DatabaseSupport.jdbi(ds));
            try {
                inv.updateTutorial(10001, 0, 0, 0);
                inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_PANG_MASTERY);
                LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(ds));
                String loginKey = repo.generateAuthKeyLogin(10001);
                String gameKey = repo.generateAuthKeyGame(10001, 20202);
                keys.putLoginKey(10001, loginKey);
                keys.putGameKey(10001, 20202, gameKey);
                loginToChannel(client, runtime.port(), "testuser", 10001, loginKey, gameKey);

                client.sendPlain(GamePackets.clientCompleteQuest(
                        GamePackets.TUTORIAL_TIPO_ROOKIE, 4));
                PacketReader order = awaitOpcode(client, GamePackets.SERVER_LOGIN_ACK);
                assertEquals(GamePackets.GACHA_ERR_MARKER, order.u8());
                assertEquals(GamePackets.shopSys(GamePackets.TUTORIAL_ERR_ORDER), order.u32());

                client.sendPlain(GamePackets.clientCompleteQuest(
                        GamePackets.TUTORIAL_TIPO_ROOKIE, 1));
                PacketReader ok = awaitOpcode(client, GamePackets.SERVER_MAKE_TUTORIAL);
                assertEquals(GamePackets.TUTORIAL_TIPO_ROOKIE, ok.u8());
                assertEquals(GamePackets.TUTORIAL_OK, ok.u8());
                assertEquals(1, ok.u32());
                assertEquals(1, inv.tutorial(10001).rookie());

                client.sendPlain(GamePackets.clientCompleteQuest(
                        GamePackets.TUTORIAL_TIPO_ROOKIE, 1));
                PacketReader done = awaitOpcode(client, GamePackets.SERVER_LOGIN_ACK);
                assertEquals(GamePackets.GACHA_ERR_MARKER, done.u8());
                assertEquals(GamePackets.shopSys(GamePackets.TUTORIAL_ERR_DONE), done.u32());

                client.sendPlain(GamePackets.clientOpenMailBox(1));
                PacketReader page = awaitOpcode(client, GamePackets.SERVER_MAILBOX);
                assertEquals(0, page.i32());
                assertEquals(1, page.i32());
                assertEquals(1, page.i32());
                assertEquals(1, page.i32());
                int mailId = page.i32();
                assertTrue(mailId > 0);
                assertEquals(GamePackets.MAIL_FROM_ADM, page.fixedStr(GamePackets.MAIL_FROM_BYTES));
                assertEquals(GamePackets.TUTORIAL_ROOKIE_MSG,
                        page.fixedStr(GamePackets.MAIL_MSG_PREVIEW_BYTES));
                page.readBytes(GamePackets.MAIL_UNKNOWN2_BYTES);
                assertEquals(0, page.u32());
                assertEquals(0, page.u8());
                assertEquals(1, page.u32());

                client.sendPlain(GamePackets.clientTakeMail(mailId));
                PacketReader awards = awaitOpcode(client, GamePackets.SERVER_DAILY_QUEST_STAMP);
                assertTrue(awards.u32() > 0);
                assertEquals(1, awards.u32());
                assertEquals(GamePackets.PAPEL_AWARD_TYPE, awards.u8());
                assertEquals(GamePackets.TYPEID_PANG_MASTERY, awards.u32());
                assertTrue(awards.i32() > 0);
                assertEquals(0, awards.u32());
                assertEquals(0, awards.i32());
                assertEquals(3, awards.i32());
                assertEquals(3, awards.i32());
                assertEquals(0, awards.u16());
                assertEquals(0, awards.u32());
                assertEquals(0, awards.u32());
                assertEquals(5, awards.remaining());
                assertEquals(0, awaitOpcode(client, GamePackets.SERVER_MAIL_TAKE).u32());
                assertEquals(3, inv.warehouse(10001).stream()
                        .filter(w -> w.typeid == GamePackets.TYPEID_PANG_MASTERY)
                        .mapToInt(w -> w.c[0] & 0xffff)
                        .findFirst()
                        .orElse(-1));

                client.sendPlain(GamePackets.clientTakeMail(mailId));
                assertEquals(GamePackets.MAIL_ERR_TAKE_EMPTY,
                        awaitOpcode(client, GamePackets.SERVER_MAIL_TAKE).u32());
            } finally {
                inv.updateTutorial(10001, 0, 0, 0);
                inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_PANG_MASTERY);
            }
        }
    }

    @Test
    void versusAutoCommandAcksFailAndCountsUses() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient host = new PangyaFakeClient();
             PangyaFakeClient guest = new PangyaFakeClient()) {
            InventoryRepository inv = new JdbiInventoryRepository(DatabaseSupport.jdbi(ds));
            try {
                inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_AUTO_COMMAND);
                loginTwoPlayers(ds, keys, host, guest, runtime.port());
                host.sendPlain(GamePackets.clientCreateRoom(GamePackets.TIPO_STROKE, "AC", ""));
                PacketReader created = awaitOpcode(host, GamePackets.SERVER_ROOM_ENTER_RESULT);
                assertEquals(0, created.i16());
                int numero = roomNumberFromInfo(created.readBytes(GamePackets.ROOM_INFO_BYTES));
                guest.sendPlain(GamePackets.clientJoinRoom(numero, ""));
                assertEquals(0, awaitOpcode(guest, GamePackets.SERVER_ROOM_ENTER_RESULT).i16());
                host.sendPlain(GamePackets.clientStartGame());
                awaitOpcode(host, GamePackets.SERVER_START_GAME_FLAG);
                awaitOpcode(host, GamePackets.SERVER_START_GAME_FLAG2);
                awaitOpcode(host, GamePackets.SERVER_PANG_RATE);
                awaitOpcode(host, GamePackets.SERVER_GAME_INIT);
                awaitOpcode(host, GamePackets.SERVER_COURSE);
                awaitOpcode(host, GamePackets.SERVER_MASCOT_SEED);
                awaitOpcode(guest, GamePackets.SERVER_GAME_INIT);
                host.sendPlain(GamePackets.clientInitHole(1, 0, 0, 4, 1.5f, 2.5f, 10f, 20f));
                guest.sendPlain(GamePackets.clientInitHole(1, 0, 0, 4, 1.5f, 2.5f, 10f, 20f));
                host.sendPlain(GamePackets.clientLoadOk());
                guest.sendPlain(GamePackets.clientLoadOk());
                awaitOpcode(host, GamePackets.SERVER_WEATHER);
                awaitOpcode(host, GamePackets.SERVER_WIND);
                awaitOpcode(host, GamePackets.SERVER_HOLE_TURN);
                host.sendPlain(GamePackets.clientAutoCommand());
                PacketReader miss = awaitOpcode(host, GamePackets.SERVER_AUTO_COMMAND_ACK);
                assertEquals(GamePackets.STDA_ERROR_TYPE_GAME, miss.u32());
            } finally {
                inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_AUTO_COMMAND);
            }
        }

        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient host = new PangyaFakeClient();
             PangyaFakeClient guest = new PangyaFakeClient()) {
            InventoryRepository inv = new JdbiInventoryRepository(DatabaseSupport.jdbi(ds));
            try {
                inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_AUTO_COMMAND);
                inv.addWarehouseItem(10001, GamePackets.TYPEID_AUTO_COMMAND, 2);
                loginTwoPlayers(ds, keys, host, guest, runtime.port());
                host.sendPlain(GamePackets.clientCreateRoom(GamePackets.TIPO_STROKE, "AC2", ""));
                PacketReader created = awaitOpcode(host, GamePackets.SERVER_ROOM_ENTER_RESULT);
                assertEquals(0, created.i16());
                int numero = roomNumberFromInfo(created.readBytes(GamePackets.ROOM_INFO_BYTES));
                guest.sendPlain(GamePackets.clientJoinRoom(numero, ""));
                assertEquals(0, awaitOpcode(guest, GamePackets.SERVER_ROOM_ENTER_RESULT).i16());
                host.sendPlain(GamePackets.clientStartGame());
                awaitOpcode(host, GamePackets.SERVER_START_GAME_FLAG);
                awaitOpcode(host, GamePackets.SERVER_START_GAME_FLAG2);
                awaitOpcode(host, GamePackets.SERVER_PANG_RATE);
                awaitOpcode(host, GamePackets.SERVER_GAME_INIT);
                awaitOpcode(host, GamePackets.SERVER_COURSE);
                awaitOpcode(host, GamePackets.SERVER_MASCOT_SEED);
                awaitOpcode(guest, GamePackets.SERVER_GAME_INIT);
                host.sendPlain(GamePackets.clientInitHole(1, 0, 0, 4, 1.5f, 2.5f, 10f, 20f));
                guest.sendPlain(GamePackets.clientInitHole(1, 0, 0, 4, 1.5f, 2.5f, 10f, 20f));
                host.sendPlain(GamePackets.clientLoadOk());
                guest.sendPlain(GamePackets.clientLoadOk());
                awaitOpcode(host, GamePackets.SERVER_WEATHER);
                awaitOpcode(host, GamePackets.SERVER_WIND);
                awaitOpcode(host, GamePackets.SERVER_HOLE_TURN);
                int hostOid = oidOf(runtime, 10001);
                host.sendPlain(GamePackets.clientAutoCommand());
                host.sendPlain(GamePackets.clientAutoCommand());
                host.sendPlain(GamePackets.clientAutoCommand());
                PacketReader spent = awaitOpcode(host, GamePackets.SERVER_AUTO_COMMAND_ACK);
                assertEquals(GamePackets.AUTO_COMMAND_ERR_USED, spent.u32());
                host.sendPlain(GamePackets.clientCamera(1.25f));
                PacketReader mira = new PacketReader(host.awaitPlain(5, TimeUnit.SECONDS));
                assertEquals(GamePackets.SERVER_CAMERA, mira.opcode());
                assertEquals(hostOid, mira.i32());
                assertEquals(1.25f, mira.f32());
            } finally {
                inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_AUTO_COMMAND);
            }
        }
    }

    @Test
    void toggleAssistWingAndAssistGreen() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);
        final int wingTypeid = 0x08000099;

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient host = new PangyaFakeClient();
             PangyaFakeClient guest = new PangyaFakeClient()) {
            InventoryRepository inv = new JdbiInventoryRepository(DatabaseSupport.jdbi(ds));
            var nuri = inv.characters(10001).getFirst();
            int[] savedParts = nuri.partsTypeid.clone();
            try {
                loginTwoPlayers(ds, keys, host, guest, runtime.port());
                host.sendPlain(GamePackets.clientCreateRoom(GamePackets.TIPO_STROKE, "AS", ""));
                PacketReader created = awaitOpcode(host, GamePackets.SERVER_ROOM_ENTER_RESULT);
                assertEquals(0, created.i16());
                int numero = roomNumberFromInfo(created.readBytes(GamePackets.ROOM_INFO_BYTES));
                guest.sendPlain(GamePackets.clientJoinRoom(numero, ""));
                assertEquals(0, awaitOpcode(guest, GamePackets.SERVER_ROOM_ENTER_RESULT).i16());

                int beforeOn = GamePackets.unixNow();
                host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_TOGGLE_ASSIST));
                PacketReader awardsOn = awaitOpcode(host, GamePackets.SERVER_DAILY_QUEST_STAMP);
                int unixOn = awardsOn.u32();
                assertTrue(unixOn >= beforeOn - 1 && unixOn <= GamePackets.unixNow() + 1);
                assertEquals(1, awardsOn.u32());
                assertEquals(GamePackets.PAPEL_AWARD_TYPE, awardsOn.u8());
                assertEquals(GamePackets.TYPEID_ASSIST, awardsOn.u32());
                int assistId = awardsOn.i32();
                assertTrue(assistId > 0);
                assertEquals(0, awardsOn.u32());
                assertEquals(0, awardsOn.i32());
                assertEquals(0, awardsOn.i32());
                assertEquals(1, awardsOn.i32());
                PacketReader toggleOn = awaitOpcode(host, GamePackets.SERVER_TOGGLE_ASSIST);
                assertEquals(0, toggleOn.u32());
                assertEquals(GamePackets.TYPEID_ASSIST, toggleOn.u32());
                assertEquals(10001, toggleOn.u32());
                assertTrue(inv.warehouse(10001).stream().anyMatch(w -> w.typeid == GamePackets.TYPEID_ASSIST));

                int beforeOff = GamePackets.unixNow();
                host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_TOGGLE_ASSIST));
                PacketReader awardsOff = awaitOpcode(host, GamePackets.SERVER_DAILY_QUEST_STAMP);
                int unixOff = awardsOff.u32();
                assertTrue(unixOff >= beforeOff - 1 && unixOff <= GamePackets.unixNow() + 1);
                assertEquals(1, awardsOff.u32());
                assertEquals(GamePackets.PAPEL_AWARD_TYPE, awardsOff.u8());
                assertEquals(GamePackets.TYPEID_ASSIST, awardsOff.u32());
                assertEquals(assistId, awardsOff.i32());
                awardsOff.u32();
                awardsOff.i32();
                awardsOff.i32();
                assertEquals(-1, awardsOff.i32());
                PacketReader toggleOff = awaitOpcode(host, GamePackets.SERVER_TOGGLE_ASSIST);
                assertEquals(0, toggleOff.u32());
                assertEquals(GamePackets.TYPEID_ASSIST, toggleOff.u32());
                assertEquals(10001, toggleOff.u32());
                assertFalse(inv.warehouse(10001).stream().anyMatch(w -> w.typeid == GamePackets.TYPEID_ASSIST));

                host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_TOGGLE_ASSIST));
                awaitOpcode(host, GamePackets.SERVER_DAILY_QUEST_STAMP);
                PacketReader toggleOn2 = awaitOpcode(host, GamePackets.SERVER_TOGGLE_ASSIST);
                assertEquals(0, toggleOn2.u32());

                host.sendPlain(GamePackets.clientU32(
                        GamePackets.CLIENT_ASSIST_GREEN, GamePackets.TYPEID_ASSIST));
                host.sendPlain(GamePackets.clientU32(GamePackets.CLIENT_WING, wingTypeid));

                host.sendPlain(GamePackets.clientStartGame());
                awaitOpcode(host, GamePackets.SERVER_START_GAME_FLAG);
                awaitOpcode(host, GamePackets.SERVER_START_GAME_FLAG2);
                awaitOpcode(host, GamePackets.SERVER_PANG_RATE);
                awaitOpcode(host, GamePackets.SERVER_GAME_INIT);
                awaitOpcode(host, GamePackets.SERVER_COURSE);
                awaitOpcode(host, GamePackets.SERVER_MASCOT_SEED);
                awaitOpcode(guest, GamePackets.SERVER_GAME_INIT);
                host.sendPlain(GamePackets.clientInitHole(1, 0, 0, 4, 1.5f, 2.5f, 10f, 20f));
                guest.sendPlain(GamePackets.clientInitHole(1, 0, 0, 4, 1.5f, 2.5f, 10f, 20f));
                host.sendPlain(GamePackets.clientLoadOk());
                guest.sendPlain(GamePackets.clientLoadOk());
                awaitOpcode(host, GamePackets.SERVER_WEATHER);
                awaitOpcode(host, GamePackets.SERVER_WIND);
                awaitOpcode(host, GamePackets.SERVER_HOLE_TURN);

                host.sendPlain(GamePackets.clientU32(
                        GamePackets.CLIENT_ASSIST_GREEN, GamePackets.TYPEID_ASSIST));
                PacketReader green = awaitOpcode(host, GamePackets.SERVER_ASSIST_GREEN);
                assertEquals(0, green.u32());
                assertEquals(GamePackets.TYPEID_ASSIST, green.u32());
                assertEquals(10001, green.u32());

                host.sendPlain(GamePackets.clientU32(GamePackets.CLIENT_ASSIST_GREEN, 1));
                PacketReader greenBad = awaitOpcode(host, GamePackets.SERVER_ASSIST_GREEN);
                assertEquals(GamePackets.ASSIST_GREEN_ERR_TYPEID, greenBad.u32());
                host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_ASSIST_GREEN));
                PacketReader greenTrunc = awaitOpcode(host, GamePackets.SERVER_ASSIST_GREEN);
                assertEquals(GamePackets.ASSIST_GREEN_ERR_DEFAULT, greenTrunc.u32());

                host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_TOGGLE_ASSIST));
                PacketReader inGame = awaitOpcode(host, GamePackets.SERVER_ASSIST_INGAME);
                assertEquals(0, inGame.u32());
                assertTrue(inv.warehouse(10001).stream().anyMatch(w -> w.typeid == GamePackets.TYPEID_ASSIST));

                inv.addWarehouseItem(10001, wingTypeid, 1);
                nuri.partsTypeid[0] = wingTypeid;
                inv.updateCharacterParts(10001, nuri);
                host.sendPlain(GamePackets.clientU32(GamePackets.CLIENT_WING, 0));
                host.sendPlain(GamePackets.clientU32(GamePackets.CLIENT_WING, wingTypeid));
                PacketReader wing = awaitOpcode(host, GamePackets.SERVER_ACTIVE_WING);
                assertEquals(10001, wing.u32());
                assertEquals(wingTypeid, wing.u32());
                PacketReader guestWing = awaitOpcode(guest, GamePackets.SERVER_ACTIVE_WING);
                assertEquals(10001, guestWing.u32());
                assertEquals(wingTypeid, guestWing.u32());
            } finally {
                nuri.partsTypeid[0] = savedParts[0];
                inv.updateCharacterParts(10001, nuri);
                inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_ASSIST);
                inv.deleteWarehouseByTypeid(10001, wingTypeid);
            }
        }
    }

    @Test
    void versusRingGloveEarcuffAndJpRings() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);
        final int partTypeid = (GamePackets.IFF_GROUP_PART << 26) | 0x99;
        final int aux0 = (GamePackets.IFF_GROUP_AUX_PART << 26) | 1;
        final int aux1 = (GamePackets.IFF_GROUP_AUX_PART << 26) | 2;

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient host = new PangyaFakeClient();
             PangyaFakeClient guest = new PangyaFakeClient()) {
            InventoryRepository inv = new JdbiInventoryRepository(DatabaseSupport.jdbi(ds));
            var nuri = inv.characters(10001).getFirst();
            int[] savedParts = nuri.partsTypeid.clone();
            int[] savedAux = nuri.auxparts.clone();
            try {
                loginTwoPlayers(ds, keys, host, guest, runtime.port());
                inv.addWarehouseItem(10001, partTypeid, 1);
                inv.addWarehouseItem(10001, aux0, 1);
                inv.addWarehouseItem(10001, aux1, 1);
                nuri.partsTypeid[0] = partTypeid;
                nuri.auxparts[0] = aux0;
                nuri.auxparts[1] = aux1;
                inv.updateCharacterParts(10001, nuri);

                host.sendPlain(GamePackets.clientCreateRoom(GamePackets.TIPO_STROKE, "RG", ""));
                PacketReader created = awaitOpcode(host, GamePackets.SERVER_ROOM_ENTER_RESULT);
                assertEquals(0, created.i16());
                int numero = roomNumberFromInfo(created.readBytes(GamePackets.ROOM_INFO_BYTES));
                guest.sendPlain(GamePackets.clientJoinRoom(numero, ""));
                assertEquals(0, awaitOpcode(guest, GamePackets.SERVER_ROOM_ENTER_RESULT).i16());
                host.sendPlain(GamePackets.clientStartGame());
                awaitOpcode(host, GamePackets.SERVER_START_GAME_FLAG);
                awaitOpcode(host, GamePackets.SERVER_START_GAME_FLAG2);
                awaitOpcode(host, GamePackets.SERVER_PANG_RATE);
                awaitOpcode(host, GamePackets.SERVER_GAME_INIT);
                awaitOpcode(host, GamePackets.SERVER_COURSE);
                awaitOpcode(host, GamePackets.SERVER_MASCOT_SEED);
                awaitOpcode(guest, GamePackets.SERVER_GAME_INIT);
                host.sendPlain(GamePackets.clientInitHole(1, 0, 0, 4, 1.5f, 2.5f, 10f, 20f));
                guest.sendPlain(GamePackets.clientInitHole(1, 0, 0, 4, 1.5f, 2.5f, 10f, 20f));
                host.sendPlain(GamePackets.clientLoadOk());
                guest.sendPlain(GamePackets.clientLoadOk());
                awaitOpcode(host, GamePackets.SERVER_WEATHER);
                awaitOpcode(host, GamePackets.SERVER_WIND);
                awaitOpcode(host, GamePackets.SERVER_HOLE_TURN);

                host.sendPlain(GamePackets.clientActiveRing(aux0, 7, 2));
                PacketReader ring = awaitOpcode(host, GamePackets.SERVER_ACTIVE_RING);
                assertEquals(0, ring.u32());
                assertEquals(10001, ring.u32());
                assertEquals(aux0, ring.u32());
                assertEquals(2, ring.u8());
                PacketReader guestRing = awaitOpcode(guest, GamePackets.SERVER_ACTIVE_RING);
                assertEquals(0, guestRing.u32());
                host.sendPlain(GamePackets.clientActiveRing(0, 0, 0));
                PacketReader ringZero = awaitOpcode(host, GamePackets.SERVER_ACTIVE_RING);
                assertEquals(GamePackets.RING_ERR_TYPEID, ringZero.u32());
                host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_ACTIVE_RING));
                PacketReader ringTrunc = awaitOpcode(host, GamePackets.SERVER_ACTIVE_RING);
                assertEquals(GamePackets.RING_ERR_DEFAULT, ringTrunc.u32());

                host.sendPlain(GamePackets.clientU32(GamePackets.CLIENT_GLOVE, partTypeid));
                PacketReader glove = awaitOpcode(host, GamePackets.SERVER_ACTIVE_GLOVE);
                assertEquals(0, glove.u32());
                assertEquals(partTypeid, glove.u32());
                assertEquals(10001, glove.u32());
                PacketReader guestGlove = awaitOpcode(guest, GamePackets.SERVER_ACTIVE_GLOVE);
                assertEquals(0, guestGlove.u32());

                host.sendPlain(GamePackets.clientEarcuff(partTypeid, 1, 1.25f));
                PacketReader earcuff = awaitOpcode(host, GamePackets.SERVER_ACTIVE_EARCUFF);
                assertEquals(0, earcuff.u32());
                assertEquals(partTypeid, earcuff.u32());
                assertEquals(10001, earcuff.u32());
                assertEquals(1, earcuff.u8());
                assertEquals(1.25f, earcuff.f32());
                PacketReader guestEarcuff = awaitOpcode(guest, GamePackets.SERVER_ACTIVE_EARCUFF);
                assertEquals(0, guestEarcuff.u32());

                host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_RING_PAWS_RAINBOW));
                PacketReader rainbow = awaitOpcode(host, GamePackets.SERVER_RING_PAWS_RAINBOW);
                assertEquals(10001, rainbow.u32());
                PacketReader guestRainbow = awaitOpcode(guest, GamePackets.SERVER_RING_PAWS_RAINBOW);
                assertEquals(10001, guestRainbow.u32());
                host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_RING_PAWS_SET));
                PacketReader pawsSet = awaitOpcode(host, GamePackets.SERVER_RING_PAWS_SET);
                assertEquals(10001, pawsSet.u32());
                PacketReader guestSet = awaitOpcode(guest, GamePackets.SERVER_RING_PAWS_SET);
                assertEquals(10001, guestSet.u32());

                host.sendPlain(GamePackets.clientU32(GamePackets.CLIENT_RING_MIRACLE, aux0));
                PacketReader miracle = awaitOpcode(host, GamePackets.SERVER_RING_MIRACLE);
                assertEquals(0, miracle.u32());
                assertEquals(aux0, miracle.u32());
                assertEquals(10001, miracle.u32());
                PacketReader guestMiracle = awaitOpcode(guest, GamePackets.SERVER_RING_MIRACLE);
                assertEquals(0, guestMiracle.u32());

                host.sendPlain(GamePackets.clientRingPair(
                        GamePackets.CLIENT_RING_POWER, 1, aux0, aux1, 0));
                PacketReader power = awaitOpcode(host, GamePackets.SERVER_RING_POWER);
                assertEquals(10001, power.u32());
                PacketReader guestPower = awaitOpcode(guest, GamePackets.SERVER_RING_POWER);
                assertEquals(10001, guestPower.u32());

                host.sendPlain(GamePackets.clientRingPair(
                        GamePackets.CLIENT_RING_GROUND, 8, aux0, aux1, 1));
                PacketReader ground = awaitOpcode(host, GamePackets.SERVER_ACTIVE_RING_GROUND);
                assertEquals(0, ground.u32());
                assertEquals(8, ground.u32());
                assertEquals(aux0, ground.u32());
                assertEquals(aux1, ground.u32());
                assertEquals(1, ground.u32());
                assertEquals(10001, ground.u32());
            } finally {
                System.arraycopy(savedParts, 0, nuri.partsTypeid, 0, savedParts.length);
                System.arraycopy(savedAux, 0, nuri.auxparts, 0, savedAux.length);
                inv.updateCharacterParts(10001, nuri);
                inv.deleteWarehouseByTypeid(10001, partTypeid);
                inv.deleteWarehouseByTypeid(10001, aux0);
                inv.deleteWarehouseByTypeid(10001, aux1);
            }
        }
    }

    @Test
    void gpExitRoomSendsPacote254() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient host = new PangyaFakeClient();
             PangyaFakeClient guest = new PangyaFakeClient()) {
            loginTwoPlayers(ds, keys, host, guest, runtime.port());
            host.sendPlain(GamePackets.clientCreateRoom(GamePackets.TIPO_LOUNGE, "GPX", ""));
            assertEquals(0, awaitOpcode(host, GamePackets.SERVER_ROOM_ENTER_RESULT).i16());
            host.sendPlain(GamePackets.clientGpExitRoom());
            PacketReader left = awaitOpcode(host, GamePackets.SERVER_GP_EXIT_ROOM);
            assertEquals(0, left.u32());
            assertEquals(-1, left.i16());
            assertEquals(-1, runtime.sessions().snapshot().stream()
                    .filter(s -> s.player().uid == 10001)
                    .findFirst()
                    .orElseThrow()
                    .player().roomNumber);
        }
    }

    @Test
    void spyEntersLockedRoomWithPassword() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient host = new PangyaFakeClient();
             PangyaFakeClient guest = new PangyaFakeClient()) {
            loginTwoPlayers(ds, keys, host, guest, runtime.port());
            host.sendPlain(GamePackets.clientCreateRoom(GamePackets.TIPO_LOUNGE, "LG", "pw"));
            PacketReader created = awaitOpcode(host, GamePackets.SERVER_ROOM_ENTER_RESULT);
            assertEquals(0, created.i16());
            int numero = roomNumberFromInfo(created.readBytes(GamePackets.ROOM_INFO_BYTES));
            guest.sendPlain(GamePackets.clientJoinGallery(numero, "no"));
            guest.sendPlain(GamePackets.clientRequestCash());
            awaitOpcode(guest, GamePackets.SERVER_COOKIE);
            guest.sendPlain(GamePackets.clientJoinGallery(numero, "pw"));
            assertEquals(0, awaitOpcode(guest, GamePackets.SERVER_ROOM_ENTER_RESULT).i16());
        }
    }

    @Test
    void twoPlayersStartMatchAndReceiveVersusDump() throws Exception {
        startTwoPlayerVersusMode(GamePackets.TIPO_MATCH, "MATCH");
    }

    @Test
    void twoPlayersStartTourneyAndReceiveCourse() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient host = new PangyaFakeClient();
             PangyaFakeClient guest = new PangyaFakeClient()) {
            loginTwoPlayers(ds, keys, host, guest, runtime.port());
            host.sendPlain(GamePackets.clientCreateRoom(GamePackets.TIPO_TOURNEY, "TN", ""));
            PacketReader created = awaitOpcode(host, GamePackets.SERVER_ROOM_ENTER_RESULT);
            assertEquals(0, created.i16());
            int numero = roomNumberFromInfo(created.readBytes(GamePackets.ROOM_INFO_BYTES));
            guest.sendPlain(GamePackets.clientJoinRoom(numero, ""));
            assertEquals(0, awaitOpcode(guest, GamePackets.SERVER_ROOM_ENTER_RESULT).i16());

            host.sendPlain(GamePackets.clientStartGame());
            awaitOpcode(host, GamePackets.SERVER_START_GAME_FLAG);
            awaitOpcode(host, GamePackets.SERVER_START_GAME_FLAG2);
            awaitOpcode(host, GamePackets.SERVER_PANG_RATE);
            PacketReader init = awaitOpcode(host, GamePackets.SERVER_GAME_INIT);
            assertEquals(GamePackets.TIPO_TOURNEY, init.u8());
            assertEquals(1, init.u32());
            PacketReader course = awaitOpcode(host, GamePackets.SERVER_COURSE);
            assertEquals(0, course.u8());
            assertEquals(GamePackets.TIPO_TOURNEY, course.u8());
            PacketReader guestInit = awaitOpcode(guest, GamePackets.SERVER_GAME_INIT);
            assertEquals(GamePackets.TIPO_TOURNEY, guestInit.u8());
        }
    }

    @Test
    void soloGrandPrixSendsTourneyInit() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient client = new PangyaFakeClient()) {
            LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(ds));
            String loginKey = repo.generateAuthKeyLogin(10001);
            String gameKey = repo.generateAuthKeyGame(10001, 20202);
            keys.putLoginKey(10001, loginKey);
            keys.putGameKey(10001, 20202, gameKey);
            loginToChannel(client, runtime.port(), "testuser", 10001, loginKey, gameKey);
            client.sendPlain(GamePackets.clientCreateRoom(GamePackets.TIPO_GRAND_PRIX, "GP", ""));
            assertEquals(0, awaitOpcode(client, GamePackets.SERVER_ROOM_ENTER_RESULT).i16());
            client.sendPlain(GamePackets.clientStartGame());
            awaitOpcode(client, GamePackets.SERVER_START_GAME_FLAG);
            awaitOpcode(client, GamePackets.SERVER_START_GAME_FLAG2);
            awaitOpcode(client, GamePackets.SERVER_PANG_RATE);
            PacketReader init = awaitOpcode(client, GamePackets.SERVER_GAME_INIT);
            assertEquals(GamePackets.TIPO_TOURNEY, init.u8());
            assertEquals(1, init.u32());
            PacketReader course = awaitOpcode(client, GamePackets.SERVER_COURSE);
            assertEquals(0, course.u8());
            assertEquals(GamePackets.TIPO_TOURNEY, course.u8());
        }
    }

    @Test
    void shopBuySendsNewItemThenBuyOk() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient client = new PangyaFakeClient()) {
            var inventory = new org.pangya.db.JdbiInventoryRepository(org.pangya.db.DatabaseSupport.jdbi(ds));
            inventory.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
            inventory.setPangCookie(10001, 100000, 0);
            LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(ds));
            String loginKey = repo.generateAuthKeyLogin(10001);
            String gameKey = repo.generateAuthKeyGame(10001, 20202);
            keys.putLoginKey(10001, loginKey);
            keys.putGameKey(10001, 20202, gameKey);
            loginToChannel(client, runtime.port(), "testuser", 10001, loginKey, gameKey);

            client.sendPlain(GamePackets.clientBuyEmpty());
            PacketReader empty = awaitOpcode(client, GamePackets.SERVER_BUY_ACK);
            assertEquals(GamePackets.BUY_FAIL_EMPTY, empty.u32());

            client.sendPlain(GamePackets.clientBuyItem(0x7FFF0001, 1, 1, 0));
            PacketReader missing = awaitOpcode(client, GamePackets.SERVER_BUY_ACK);
            assertEquals(GamePackets.BUY_FAIL_NOT_BUYABLE, missing.u32());

            client.sendPlain(GamePackets.clientBuyItem(
                    GamePackets.TYPEID_SHOP_PANG_ITEM, 1, GamePackets.SHOP_PANG_PRICE, 0));
            PacketReader spent = awaitOpcode(client, GamePackets.SERVER_PANG_SPENT);
            assertEquals(99900, spent.u64());
            assertEquals(GamePackets.SHOP_PANG_PRICE, spent.u64());
            PacketReader added = awaitOpcode(client, GamePackets.SERVER_NEW_ITEM);
            assertEquals(1, added.u16());
            assertEquals(GamePackets.TYPEID_SHOP_PANG_ITEM, added.u32());
            assertTrue(added.i32() > 0);
            assertEquals(0, added.u16());
            assertEquals(0, added.u8());
            assertEquals(1, added.u16());
            added.readBytes(GamePackets.SYSTEMTIME_BYTES + GamePackets.UCC_IDX_BYTES);
            assertEquals(99900, added.u64());
            assertEquals(0, added.u64());
            PacketReader ok = awaitOpcode(client, GamePackets.SERVER_BUY_ACK);
            assertEquals(0, ok.u32());
            assertEquals(99900, ok.u64());
            assertEquals(0, ok.u64());

            client.sendPlain(GamePackets.clientGiftEmpty(10002));
            PacketReader giftEmpty = awaitOpcode(client, GamePackets.SERVER_RESPONSE_GIFT_ITEM);
            assertEquals(GamePackets.BUY_FAIL_INIT, giftEmpty.u32());
            assertEquals(99900, giftEmpty.u64());
            assertEquals(0, giftEmpty.u64());

            client.sendPlain(GamePackets.clientGiftItem(
                    10002, GamePackets.TYPEID_SHOP_PANG_ITEM, 1, GamePackets.SHOP_PANG_PRICE, 0));
            PacketReader giftInit = awaitOpcode(client, GamePackets.SERVER_RESPONSE_GIFT_ITEM);
            assertEquals(GamePackets.BUY_FAIL_INIT, giftInit.u32());
            assertEquals(99900, giftInit.u64());
            assertEquals(0, giftInit.u64());

            client.sendPlain(GamePackets.clientPayCaddieHoliday(0));
            PacketReader holiday = awaitOpcode(client, GamePackets.SERVER_REEMPLOY_CADDIE_ACK);
            assertEquals(GamePackets.CADDIE_HOLIDAY_FAIL, holiday.u8());
            int caddieId = inventory.caddies(10001).getFirst().id;
            client.sendPlain(GamePackets.clientPayCaddieHoliday(caddieId));
            PacketReader holidayOk = awaitOpcode(client, GamePackets.SERVER_REEMPLOY_CADDIE_ACK);
            assertEquals(GamePackets.CADDIE_HOLIDAY_OK, holidayOk.u8());
            assertEquals(caddieId, holidayOk.i32());
            assertEquals(99900 - GamePackets.CADDIE_HOLIDAY_PANG, holidayOk.u64());
            inventory.setPangCookie(10001, 99900, 0);

            client.sendPlain(GamePackets.clientTickerQuery());
            PacketReader tickerQ = awaitOpcode(client, GamePackets.SERVER_ONELINE_QUERY);
            assertEquals(0, tickerQ.u16());
            assertEquals(0, tickerQ.u32());
            client.sendPlain(GamePackets.clientTicker(""));
            assertEquals(GamePackets.TICKER_FAIL_GENERIC,
                    awaitOpcode(client, GamePackets.SERVER_CHANGE_NICK_ACK).u32());
            client.sendPlain(GamePackets.clientTicker("hi"));
            assertEquals(GamePackets.TICKER_FAIL_FUNDS,
                    awaitOpcode(client, GamePackets.SERVER_CHANGE_NICK_ACK).u32());
            inventory.setPangCookie(10001, 99900, 1);
            client.sendPlain(GamePackets.clientTicker("hello"));
            assertEquals(0, awaitOpcode(client, GamePackets.SERVER_COOKIE).u64());
            PacketReader line = awaitOpcode(client, GamePackets.SERVER_ONELINE_MSG);
            assertEquals("TestNick", line.pstr());
            assertEquals("hello", line.pstr());
            client.sendPlain(GamePackets.clientTickerQuery());
            PacketReader queued = awaitOpcode(client, GamePackets.SERVER_ONELINE_QUERY);
            assertEquals(1, queued.u16());
            assertEquals(GamePackets.TICKER_WAIT_MS, queued.u32());

            client.sendPlain(GamePackets.clientMascotMessage(0, "hi"));
            PacketReader mascot = awaitOpcode(client, GamePackets.SERVER_CHANGE_MASCOT);
            assertEquals(0xff, mascot.u8());
            assertEquals(-1, mascot.i32());
            assertEquals(0, mascot.u16());
            assertEquals(99900, mascot.u64());
            int mascotId = inventory.mascots(10001).getFirst().id;
            client.sendPlain(GamePackets.clientMascotMessage(mascotId, "hello"));
            PacketReader mascotOk = awaitOpcode(client, GamePackets.SERVER_CHANGE_MASCOT);
            assertEquals(GamePackets.MASCOT_MSG_OK, mascotOk.u8());
            assertEquals(mascotId, mascotOk.i32());
            assertEquals("hello", mascotOk.pstr());
            assertEquals(99900 - GamePackets.MASCOT_MSG_PRICE, mascotOk.u64());
            inventory.setPangCookie(10001, 99900, 0);

            client.sendPlain(GamePackets.clientNotice("gm"));
            PacketReader notice = awaitOpcode(client, GamePackets.SERVER_CHAT);
            assertEquals(GamePackets.CHAT_NOTICE, notice.u8());
            notice.pstr();
            assertEquals("Command no Executed", notice.pstr());
            client.sendPlain(GamePackets.clientDestroyRoom(1));
            PacketReader destroyed = awaitOpcode(client, GamePackets.SERVER_CHAT);
            assertEquals(GamePackets.CHAT_NOTICE, destroyed.u8());
            destroyed.pstr();
            assertEquals("Command no executed!", destroyed.pstr());

            client.sendPlain(GamePackets.clientMsnFriendList());
            PacketReader friends = awaitOpcode(client, GamePackets.SERVER_MSN_ACK);
            assertEquals(GamePackets.MSN_FRIEND_LIST, friends.u16());
            assertEquals(GamePackets.MSN_ERR_FUNDS, friends.u32());
            client.sendPlain(GamePackets.clientMsnMsgOff(0, "x", 0));
            PacketReader badUid = awaitOpcode(client, GamePackets.SERVER_MSN_ACK);
            assertEquals(GamePackets.MSN_MSG_OFF, badUid.u16());
            assertEquals(GamePackets.MSN_ERR_UID, badUid.u32());
            client.sendPlain(GamePackets.clientMsnMsgOff(10002, "", 0));
            PacketReader emptyMsg = awaitOpcode(client, GamePackets.SERVER_MSN_ACK);
            assertEquals(GamePackets.MSN_MSG_OFF, emptyMsg.u16());
            assertEquals(GamePackets.MSN_ERR_EMPTY, emptyMsg.u32());
            client.sendPlain(GamePackets.clientMsnMsgOff(10002, "offline", 0));
            PacketReader msgOff = awaitOpcode(client, GamePackets.SERVER_MSN_ACK);
            assertEquals(GamePackets.MSN_MSG_OFF, msgOff.u16());
            assertEquals(0, msgOff.u32());
            assertEquals(99890, msgOff.u64());

            inventory.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
            inventory.setPangCookie(10001, 100000, 0);
        }
    }

    @Test
    void shopGiftAtBeginnerEChargesSenderAndMailsRecipient() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient host = new PangyaFakeClient();
             PangyaFakeClient guest = new PangyaFakeClient()) {
            var inventory = new JdbiInventoryRepository(DatabaseSupport.jdbi(ds));
            inventory.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
            inventory.setPangCookie(10001, 100000, 0);
            inventory.setLevel(10001, GamePackets.GIFT_MIN_LEVEL);
            try {
                loginTwoPlayers(ds, keys, host, guest, runtime.port());

                host.sendPlain(GamePackets.clientGiftEmpty(10002));
                PacketReader empty = awaitOpcode(host, GamePackets.SERVER_RESPONSE_GIFT_ITEM);
                assertEquals(GamePackets.BUY_FAIL_EMPTY, empty.u32());
                assertEquals(100000, empty.u64());

                host.sendPlain(GamePackets.clientGiftItem(10002, 0x7FFF0001, 1, 1, 0));
                PacketReader missing = awaitOpcode(host, GamePackets.SERVER_RESPONSE_GIFT_ITEM);
                assertEquals(GamePackets.BUY_FAIL_NOT_BUYABLE, missing.u32());

                inventory.setPangCookie(10001, 0, 0);
                host.sendPlain(GamePackets.clientGiftItem(
                        10002, GamePackets.TYPEID_SHOP_PANG_ITEM, 1, GamePackets.SHOP_PANG_PRICE, 0));
                PacketReader funds = awaitOpcode(host, GamePackets.SERVER_RESPONSE_GIFT_ITEM);
                assertEquals(GamePackets.BUY_FAIL_FUNDS, funds.u32());

                inventory.setPangCookie(10001, 100000, 0);
                host.sendPlain(GamePackets.clientGiftItem(
                        10002, GamePackets.TYPEID_SHOP_PANG_ITEM, 1, GamePackets.SHOP_PANG_PRICE, 0));
                PacketReader spent = awaitOpcode(host, GamePackets.SERVER_PANG_SPENT);
                assertEquals(99900, spent.u64());
                assertEquals(GamePackets.SHOP_PANG_PRICE, spent.u64());
                PacketReader ok = awaitOpcode(host, GamePackets.SERVER_RESPONSE_GIFT_ITEM);
                assertEquals(0, ok.u32());
                assertEquals(99900, ok.u64());
                assertEquals(0, ok.u64());
                PacketReader mail = awaitOpcode(guest, GamePackets.SERVER_NEW_MAIL);
                assertEquals(0, mail.i32());
                assertEquals(1, mail.i32());
                assertFalse(inventory.warehouse(10001).stream()
                        .anyMatch(w -> w.typeid == GamePackets.TYPEID_SHOP_PANG_ITEM));
            } finally {
                inventory.setLevel(10001, 1);
                inventory.setPangCookie(10001, 100000, 0);
                inventory.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
            }
        }
    }

    @Test
    void loungeStateBroadcastsPacote196() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient client = new PangyaFakeClient()) {
            LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(ds));
            String loginKey = repo.generateAuthKeyLogin(10001);
            String gameKey = repo.generateAuthKeyGame(10001, 20202);
            keys.putLoginKey(10001, loginKey);
            keys.putGameKey(10001, 20202, gameKey);
            loginToChannel(client, runtime.port(), "testuser", 10001, loginKey, gameKey);
            client.sendPlain(GamePackets.clientCreateRoom(GamePackets.TIPO_LOUNGE, "LG", ""));
            assertEquals(0, awaitOpcode(client, GamePackets.SERVER_ROOM_ENTER_RESULT).i16());
            client.sendPlain(GamePackets.clientLoungeState());
            PacketReader state = awaitOpcode(client, GamePackets.SERVER_LOUNGE_STATE);
            state.i32();
            assertEquals(1.0f, state.f32());
            assertEquals(1.0f, state.f32());
            assertEquals(1.0f, state.f32());
            assertEquals(1.0f, state.f32());

            client.sendPlain(GamePackets.clientSyncActivityLocation(
                    GamePackets.ACTION_LOUNGER_LOC, 10.0f, 20.0f, 1.5f));
            PacketReader loc = awaitOpcode(client, GamePackets.SERVER_SYNC_ACTIVITY);
            assertTrue(loc.i32() > 0);
            assertEquals(GamePackets.ACTION_LOUNGER_LOC, loc.u8());
            assertEquals(10.0f, loc.f32());
            assertEquals(20.0f, loc.f32());
            assertEquals(1.5f, loc.f32());

            client.sendPlain(GamePackets.clientSyncActivityRotation(2.0f));
            PacketReader rot = awaitOpcode(client, GamePackets.SERVER_SYNC_ACTIVITY);
            rot.i32();
            assertEquals(GamePackets.ACTION_ROTATION, rot.u8());
            assertEquals(2.0f, rot.f32());

            client.sendPlain(GamePackets.clientSleep(1));
            PacketReader sleep = awaitOpcode(client, GamePackets.SERVER_SLEEP);
            assertTrue(sleep.i32() > 0);
            assertEquals(1, sleep.u8());

            client.sendPlain(GamePackets.clientShopCancel());
            assertEquals(GamePackets.shopSys(GamePackets.SHOP_ERR_CANCEL_NONE),
                    awaitOpcode(client, GamePackets.SERVER_SHOP_CANCEL).u32());
            client.sendPlain(GamePackets.clientShopOpenEdit());
            PacketReader opened = awaitOpcode(client, GamePackets.SERVER_SHOP_EDIT);
            assertEquals(GamePackets.SHOP_OK, opened.u32());
            assertEquals("TestNick", opened.pstr());
            assertEquals(10001, opened.u32());
            client.sendPlain(GamePackets.clientShopVisit());
            PacketReader visit0 = awaitOpcode(client, GamePackets.SERVER_SHOP_VISIT);
            assertEquals(GamePackets.SHOP_OK, visit0.u32());
            assertEquals(0, visit0.u32());
            client.sendPlain(GamePackets.clientShopPang());
            PacketReader pang0 = awaitOpcode(client, GamePackets.SERVER_SHOP_PANG);
            assertEquals(GamePackets.SHOP_OK, pang0.u32());
            assertEquals(0, pang0.u64());
            client.sendPlain(GamePackets.clientShopName(""));
            assertEquals(GamePackets.shopSys(GamePackets.SHOP_ERR_NAME_EMPTY),
                    awaitOpcode(client, GamePackets.SERVER_SHOP_NAME).u32());
            client.sendPlain(GamePackets.clientShopName("MyShop"));
            PacketReader named = awaitOpcode(client, GamePackets.SERVER_SHOP_NAME);
            assertEquals(GamePackets.SHOP_OK, named.u32());
            assertEquals("MyShop", named.pstr());
            assertEquals(10001, named.u32());
            assertEquals("TestNick", named.pstr());
            client.sendPlain(GamePackets.clientShopView(10001));
            assertEquals(GamePackets.SHOP_ERR_VIEW_DEFAULT,
                    awaitOpcode(client, GamePackets.SERVER_SHOP_VIEW).u32());
            client.sendPlain(GamePackets.clientShopVisit());
            PacketReader visit1 = awaitOpcode(client, GamePackets.SERVER_SHOP_VISIT);
            assertEquals(GamePackets.SHOP_OK, visit1.u32());
            assertEquals(0, visit1.u32());
            client.sendPlain(GamePackets.clientShopCloseView(10001));
            assertEquals(GamePackets.SHOP_ERR_CLOSE_VIEW_DEFAULT,
                    awaitOpcode(client, GamePackets.SERVER_SHOP_CLOSE_VIEW).u32());
            client.sendPlain(GamePackets.clientShopOpenItems(0));
            assertEquals(GamePackets.shopSys(GamePackets.SHOP_ERR_OPEN_COUNT),
                    awaitOpcode(client, GamePackets.SERVER_SHOP_ITEMS).u32());
            client.sendPlain(GamePackets.clientShopOpenItems(1));
            assertEquals(GamePackets.SHOP_ERR_OPEN_DEFAULT,
                    awaitOpcode(client, GamePackets.SERVER_SHOP_ITEMS).u32());
            client.sendPlain(GamePackets.clientShopBuy(10001));
            assertEquals(GamePackets.SHOP_ERR_BUY_DEFAULT,
                    awaitOpcode(client, GamePackets.SERVER_SHOP_BUY).u32());
            client.sendPlain(GamePackets.clientShopClose());
            PacketReader closed = awaitOpcode(client, GamePackets.SERVER_SHOP_CLOSE);
            assertEquals(GamePackets.SHOP_OK, closed.u32());
            assertEquals("TestNick", closed.pstr());
            assertEquals(10001, closed.u32());
            client.sendPlain(GamePackets.clientPapelShop());
            PacketReader papel = awaitOpcode(client, GamePackets.SERVER_PAPEL_SHOP);
            assertEquals(0, papel.u32());
            assertEquals(0, papel.u64());
            client.sendPlain(GamePackets.clientEnterShop());
            PacketReader shop = awaitOpcode(client, GamePackets.SERVER_ENTER_SHOP);
            assertEquals(0, shop.u32());
            assertEquals(0, shop.u32());
        }
    }

    @Test
    void personalShopListsAndSellsPangItem() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient host = new PangyaFakeClient();
             PangyaFakeClient guest = new PangyaFakeClient()) {
            var inventory = new JdbiInventoryRepository(DatabaseSupport.jdbi(ds));
            inventory.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
            inventory.deleteWarehouseByTypeid(10002, GamePackets.TYPEID_SHOP_PANG_ITEM);
            inventory.setPangCookie(10001, 100000, 0);
            inventory.setPangCookie(10002, 100000, 0);
            try {
                var stock = inventory.buyShopItem(
                        10001, GamePackets.TYPEID_SHOP_PANG_ITEM, 1, GamePackets.SHOP_PANG_PRICE, 0);
                assertEquals(0, stock.code());
                inventory.setPangCookie(10001, 100000, 0);
                loginTwoPlayers(ds, keys, host, guest, runtime.port());

                host.sendPlain(GamePackets.clientCreateRoom(GamePackets.TIPO_LOUNGE, "LG", ""));
                PacketReader created = awaitOpcode(host, GamePackets.SERVER_ROOM_ENTER_RESULT);
                assertEquals(0, created.i16());
                int numero = roomNumberFromInfo(created.readBytes(GamePackets.ROOM_INFO_BYTES));
                guest.sendPlain(GamePackets.clientJoinRoom(numero, ""));
                assertEquals(0, awaitOpcode(guest, GamePackets.SERVER_ROOM_ENTER_RESULT).i16());

                host.sendPlain(GamePackets.clientShopOpenEdit());
                PacketReader opened = awaitOpcode(host, GamePackets.SERVER_SHOP_EDIT);
                assertEquals(GamePackets.SHOP_OK, opened.u32());
                host.sendPlain(GamePackets.clientShopName("MyShop"));
                PacketReader named = awaitOpcode(host, GamePackets.SERVER_SHOP_NAME);
                assertEquals(GamePackets.SHOP_OK, named.u32());

                GamePackets.PersonalShopItem listed = new GamePackets.PersonalShopItem();
                listed.index = 1;
                listed.typeid = GamePackets.TYPEID_SHOP_PANG_ITEM;
                listed.id = stock.itemId();
                listed.qntd = 1;
                listed.pang = 1000;
                host.sendPlain(GamePackets.clientShopOpenItems(List.of(listed)));
                PacketReader items = awaitOpcode(host, GamePackets.SERVER_SHOP_ITEMS);
                assertEquals(GamePackets.SHOP_OK, items.u32());
                assertEquals("TestNick", items.fixedStr(GamePackets.SHOP_NICK_BYTES));
                assertEquals(10001, items.u32());
                assertEquals(1, items.u32());

                guest.sendPlain(GamePackets.clientShopView(10001));
                PacketReader view = awaitOpcode(guest, GamePackets.SERVER_SHOP_VIEW);
                assertEquals(GamePackets.SHOP_OK, view.u32());
                assertEquals("TestNick", view.fixedStr(GamePackets.SHOP_NICK_BYTES));
                assertEquals("MyShop", view.pstr());
                assertEquals(10001, view.u32());
                assertEquals(1, view.u32());

                guest.sendPlain(GamePackets.clientShopBuy(10001, listed));
                PacketReader buyerOk = awaitOpcode(guest, GamePackets.SERVER_SHOP_BUY);
                assertEquals(GamePackets.SHOP_OK, buyerOk.u32());
                assertEquals(0, buyerOk.u8());
                assertEquals(99000, buyerOk.u64());
                buyerOk.readBytes(GamePackets.PERSONAL_SHOP_ITEM_BYTES);
                assertEquals(GamePackets.SHOP_GROUP_ITEM_BYTE, buyerOk.u8());
                assertEquals(GamePackets.WAREHOUSE_ITEM_BYTES, buyerOk.remaining());
                PacketReader soldGuest = awaitOpcode(guest, GamePackets.SERVER_SHOP_SOLD);
                assertEquals("TestNick", soldGuest.pstr());
                assertEquals(10001, soldGuest.u32());
                soldGuest.readBytes(GamePackets.PERSONAL_SHOP_ITEM_BYTES);
                assertEquals(GamePackets.SHOP_SOLD_EMPTY, soldGuest.i32());

                PacketReader sellerOk = awaitOpcode(host, GamePackets.SERVER_SHOP_BUY);
                assertEquals(GamePackets.SHOP_OK, sellerOk.u32());
                assertEquals(1, sellerOk.u8());
                assertEquals(950, sellerOk.u64());
                PacketReader soldHost = awaitOpcode(host, GamePackets.SERVER_SHOP_SOLD);
                assertEquals("TestNick", soldHost.pstr());
                assertEquals(10001, soldHost.u32());
                soldHost.readBytes(GamePackets.PERSONAL_SHOP_ITEM_BYTES);
                assertEquals(GamePackets.SHOP_SOLD_EMPTY, soldHost.i32());
                PacketReader notice = awaitOpcode(host, GamePackets.SERVER_CHAT);
                assertEquals(GamePackets.CHAT_NOTICE, notice.u8());
                assertEquals(GamePackets.SHOP_SALE_NICK, notice.pstr());
                PacketReader saleMsg = new PacketReader(GamePackets.chat(
                        GamePackets.CHAT_NOTICE, GamePackets.SHOP_SALE_NICK, GamePackets.SHOP_SALE_MSG));
                saleMsg.opcode();
                saleMsg.u8();
                saleMsg.pstr();
                assertEquals(saleMsg.pstr(), notice.pstr());

                assertEquals(100950, inventory.pang(10001));
                assertEquals(99000, inventory.pang(10002));
                assertFalse(inventory.warehouse(10001).stream()
                        .anyMatch(w -> w.typeid == GamePackets.TYPEID_SHOP_PANG_ITEM));
                assertTrue(inventory.warehouse(10002).stream()
                        .anyMatch(w -> w.typeid == GamePackets.TYPEID_SHOP_PANG_ITEM));
            } finally {
                inventory.setPangCookie(10001, 100000, 0);
                inventory.setPangCookie(10002, 100000, 0);
                inventory.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
                inventory.deleteWarehouseByTypeid(10002, GamePackets.TYPEID_SHOP_PANG_ITEM);
            }
        }
    }

    @Test
    void papelPlayAwardsItemAndChargesPang() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient client = new PangyaFakeClient()) {
            var inventory = new JdbiInventoryRepository(DatabaseSupport.jdbi(ds));
            inventory.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
            inventory.setPangCookie(10001, 0, 0);
            try {
                LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(ds));
                String loginKey = repo.generateAuthKeyLogin(10001);
                String gameKey = repo.generateAuthKeyGame(10001, 20202);
                keys.putLoginKey(10001, loginKey);
                keys.putGameKey(10001, 20202, gameKey);
                loginToChannel(client, runtime.port(), "testuser", 10001, loginKey, gameKey);

                client.sendPlain(GamePackets.clientPapelPlay());
                PacketReader funds = awaitOpcode(client, GamePackets.SERVER_PAPEL_PLAY);
                assertEquals(GamePackets.shopSys(GamePackets.PAPEL_PLAY_ERR_FUNDS), funds.u32());

                inventory.setPangCookie(10001, 100000, 0);
                client.sendPlain(GamePackets.clientPapelPlay());
                PacketReader awards = awaitOpcode(client, GamePackets.SERVER_DAILY_QUEST_STAMP);
                assertTrue(awards.u32() > 0);
                assertEquals(1, awards.u32());
                assertEquals(GamePackets.PAPEL_AWARD_TYPE, awards.u8());
                assertEquals(GamePackets.TYPEID_SHOP_PANG_ITEM, awards.u32());
                assertTrue(awards.i32() > 0);
                assertEquals(0, awards.u32());
                assertEquals(0, awards.i32());
                int dep = awards.i32();
                int qntd = awards.i32();
                assertEquals(dep, qntd);
                assertTrue(qntd >= GamePackets.PAPEL_MIN_BALL && qntd <= GamePackets.PAPEL_MAX_BALL * GamePackets.PAPEL_ITEM_MAX_QNTD);
                assertEquals(GamePackets.PAPEL_AWARD_PAD, awards.remaining());
                PacketReader remain = awaitOpcode(client, GamePackets.SERVER_PAPEL_REMAIN);
                assertEquals(GamePackets.PAPEL_UNLIMITED_REMAIN, remain.i32());
                assertEquals(GamePackets.PAPEL_UNLIMITED_FLAG, remain.i32());
                PacketReader play = awaitOpcode(client, GamePackets.SERVER_PAPEL_PLAY);
                assertEquals(0, play.u32());
                assertEquals(0, play.i32());
                int balls = play.u32();
                assertTrue(balls >= GamePackets.PAPEL_MIN_BALL && balls <= GamePackets.PAPEL_MAX_BALL);
                for (int i = 0; i < balls; i++) {
                    int color = play.u32();
                    assertTrue(color >= 0 && color < GamePackets.PAPEL_COLOR_COUNT);
                    assertEquals(GamePackets.TYPEID_SHOP_PANG_ITEM, play.u32());
                    assertEquals(0, play.u32());
                    int ballQntd = play.u32();
                    assertTrue(ballQntd >= GamePackets.PAPEL_ITEM_MIN_QNTD
                            && ballQntd <= GamePackets.PAPEL_ITEM_MAX_QNTD);
                    assertEquals(GamePackets.PAPEL_TYPE_COMMUN, play.u32());
                }
                assertEquals(100000 - GamePackets.PAPEL_PRICE_NORMAL, play.u64());
                assertEquals(0, play.u64());
                assertEquals(100000 - GamePackets.PAPEL_PRICE_NORMAL, inventory.pang(10001));
                assertTrue(inventory.warehouse(10001).stream()
                        .anyMatch(w -> w.typeid == GamePackets.TYPEID_SHOP_PANG_ITEM));
            } finally {
                inventory.setPangCookie(10001, 100000, 0);
                inventory.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
            }
        }
    }

    @Test
    void mailboxOpenSendDeleteMatchCsharp() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient host = new PangyaFakeClient();
             PangyaFakeClient guest = new PangyaFakeClient()) {
            var inventory = new org.pangya.db.JdbiInventoryRepository(org.pangya.db.DatabaseSupport.jdbi(ds));
            inventory.setPangCookie(10001, 100000, 0);
            loginTwoPlayers(ds, keys, host, guest, runtime.port());

            host.sendPlain(GamePackets.clientOpenMailBox(1));
            PacketReader empty = awaitOpcode(host, GamePackets.SERVER_MAILBOX);
            assertEquals(0, empty.i32());
            assertEquals(1, empty.i32());
            assertEquals(1, empty.i32());
            assertEquals(0, empty.i32());

            host.sendPlain(GamePackets.clientOpenMailBox(0));
            assertEquals(GamePackets.MAIL_ERR_PAGE,
                    awaitOpcode(host, GamePackets.SERVER_MAILBOX).u32());

            host.sendPlain(GamePackets.clientOpenMail(1));
            assertEquals(GamePackets.MAIL_ERR_CHANNEL,
                    awaitOpcode(host, GamePackets.SERVER_MAIL_INFO).u32());

            host.sendPlain(GamePackets.clientTakeMail(1));
            assertEquals(GamePackets.MAIL_ERR_TAKE_DEFAULT,
                    awaitOpcode(host, GamePackets.SERVER_MAIL_TAKE).u32());

            host.sendPlain(GamePackets.clientDeleteMail(1, 1));
            assertEquals(GamePackets.MAIL_ERR_DELETE_DEFAULT,
                    awaitOpcode(host, GamePackets.SERVER_MAIL_DELETE).u32());

            host.sendPlain(GamePackets.clientSendMail(
                    10001, 10002, "", 0, "hello", GamePackets.MAIL_SEND_PANG, 0, null));
            assertEquals(GamePackets.MAIL_ERR_CHANNEL,
                    awaitOpcode(host, GamePackets.SERVER_MAIL_SEND).u32());

            host.sendPlain(GamePackets.clientSendMail(
                    10001, 10002, "TestNick2", 0, "", GamePackets.MAIL_SEND_PANG, 0, null));
            assertEquals(GamePackets.MAIL_ERR_CHANNEL,
                    awaitOpcode(host, GamePackets.SERVER_MAIL_SEND).u32());

            host.sendPlain(GamePackets.clientSendMail(
                    10001, 10002, "TestNick2", 0, "hello", 50, 0, null));
            assertEquals(GamePackets.MAIL_ERR_SEND_DEFAULT,
                    awaitOpcode(host, GamePackets.SERVER_MAIL_SEND).u32());

            host.sendPlain(GamePackets.clientSendMail(
                    10001, 10002, "TestNick2", 0, "hello", GamePackets.MAIL_SEND_ITEM_PANG, 1,
                    new byte[GamePackets.MAIL_ITEM_BYTES]));
            assertEquals(GamePackets.MAIL_ERR_SEND_DEFAULT,
                    awaitOpcode(host, GamePackets.SERVER_MAIL_SEND).u32());

            host.sendPlain(GamePackets.clientSendMail(
                    10001, 10002, "TestNick2", 0, "hello", GamePackets.MAIL_SEND_PANG, 0, null));
            PacketReader spent = awaitOpcode(host, GamePackets.SERVER_PANG_SPENT);
            assertEquals(99900, spent.u64());
            assertEquals(GamePackets.MAIL_SEND_PANG, spent.u64());
            assertEquals(0, awaitOpcode(host, GamePackets.SERVER_MAIL_SEND).u32());

            guest.sendPlain(GamePackets.clientOpenMailBox(1));
            PacketReader page = awaitOpcode(guest, GamePackets.SERVER_MAILBOX);
            assertEquals(0, page.i32());
            assertEquals(1, page.i32());
            assertEquals(1, page.i32());
            assertEquals(1, page.i32());
            assertEquals(GamePackets.MAIL_BOX_ENTRY_BYTES, page.remaining());
            int mailId = page.i32();
            assertTrue(mailId > 0);
            assertEquals("TestNick", page.fixedStr(GamePackets.MAIL_FROM_BYTES));
            assertEquals("hello", page.fixedStr(GamePackets.MAIL_MSG_PREVIEW_BYTES));
            page.readBytes(GamePackets.MAIL_UNKNOWN2_BYTES);
            assertEquals(0, page.u32());
            assertEquals(0, page.u8());
            assertEquals(0, page.u32());
            assertEquals(GamePackets.MAIL_ITEM_BYTES, page.remaining());

            guest.sendPlain(GamePackets.clientOpenMail(mailId));
            PacketReader info = awaitOpcode(guest, GamePackets.SERVER_MAIL_INFO);
            assertEquals(0, info.u32());
            assertEquals(mailId, info.i32());
            assertEquals("TestNick", info.pstr());
            assertFalse(info.pstr().isEmpty());
            assertEquals("hello", info.pstr());
            assertEquals(1, info.u8());
            assertEquals(0, info.i32());
            assertEquals(GamePackets.MAIL_ITEM_BYTES, info.remaining());

            guest.sendPlain(GamePackets.clientTakeMail(mailId));
            assertEquals(GamePackets.MAIL_ERR_TAKE_EMPTY,
                    awaitOpcode(guest, GamePackets.SERVER_MAIL_TAKE).u32());

            guest.sendPlain(GamePackets.clientDeleteMail(0, mailId));
            assertEquals(GamePackets.MAIL_ERR_PAGE,
                    awaitOpcode(guest, GamePackets.SERVER_MAIL_DELETE).u32());

            guest.sendPlain(GamePackets.clientDeleteMail(1, mailId));
            PacketReader deleted = awaitOpcode(guest, GamePackets.SERVER_MAIL_DELETE);
            assertEquals(0, deleted.i32());
            assertEquals(1, deleted.i32());
            assertEquals(1, deleted.i32());
            assertEquals(0, deleted.i32());

            inventory.setPangCookie(10001, 100000, 0);
        }
    }

    @Test
    void genericBoxMailConsumesBoxAndMailsReward() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient client = new PangyaFakeClient()) {
            InventoryRepository inv = new JdbiInventoryRepository(DatabaseSupport.jdbi(ds));
            inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_BOX_MAIL_TEST);
            inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_BOX_MAIL_OPENED_TEST);
            inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_BOX_MAIL_REWARD_TEST);
            inv.deleteItemIff(GamePackets.TYPEID_BOX_MAIL_TEST);
            inv.deleteBoxMailReward(GamePackets.TYPEID_BOX_MAIL_TEST);
            int boxId = inv.addWarehouseItem(10001, GamePackets.TYPEID_BOX_MAIL_TEST, 2);
            try {
                inv.upsertItemIff(GamePackets.TYPEID_BOX_MAIL_TEST);
                inv.upsertBoxMailReward(
                        GamePackets.TYPEID_BOX_MAIL_TEST,
                        GamePackets.TYPEID_BOX_MAIL_REWARD_TEST,
                        3,
                        GamePackets.TYPEID_BOX_MAIL_OPENED_TEST,
                        "Box reward");
                LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(ds));
                String loginKey = repo.generateAuthKeyLogin(10001);
                String gameKey = repo.generateAuthKeyGame(10001, 20202);
                keys.putLoginKey(10001, loginKey);
                keys.putGameKey(10001, 20202, gameKey);
                loginToChannel(client, runtime.port(), "testuser", 10001, loginKey, gameKey);

                client.sendPlain(GamePackets.clientBoxMail(GamePackets.TYPEID_BOX_MAIL_TEST));
                PacketReader opened = awaitOpcode(client, GamePackets.SERVER_DAILY_QUEST_STAMP);
                opened.u32();
                assertEquals(1, opened.u32());
                assertEquals(GamePackets.PAPEL_AWARD_TYPE, opened.u8());
                assertEquals(GamePackets.TYPEID_BOX_MAIL_OPENED_TEST, opened.u32());
                int openedId = opened.i32();
                assertTrue(openedId > 0);
                assertEquals(0, opened.u32());
                assertEquals(0, opened.i32());
                assertEquals(1, opened.i32());
                assertEquals(1, opened.i32());
                opened.readBytes(GamePackets.PAPEL_AWARD_PAD);
                assertEquals(0, opened.remaining());

                PacketReader consumed = awaitOpcode(client, GamePackets.SERVER_BOX_CONSUME);
                assertEquals(1, consumed.u8());
                assertEquals(GamePackets.TYPEID_BOX_MAIL_TEST, consumed.u32());
                assertEquals(boxId, consumed.i32());
                assertEquals(1, consumed.u16());
                PacketReader balance = awaitOpcode(client, GamePackets.SERVER_NEW_ITEM);
                assertEquals(0, balance.u16());
                assertEquals(inv.pang(10001), balance.u64());
                assertEquals(inv.cookie(10001), balance.u64());
                PacketReader ok = awaitOpcode(client, GamePackets.SERVER_BOX_MAIL);
                assertEquals(GamePackets.BOX_MAIL_OK, ok.u32());
                assertEquals(GamePackets.TYPEID_BOX_MAIL_TEST, ok.u32());
                assertEquals(GamePackets.TYPEID_BOX_MAIL_REWARD_TEST, ok.u32());
                assertEquals(3, ok.i32());

                client.sendPlain(GamePackets.clientOpenMailBox(1));
                PacketReader page = awaitOpcode(client, GamePackets.SERVER_MAILBOX);
                assertEquals(0, page.i32());
                assertEquals(1, page.i32());
                assertEquals(1, page.i32());
                assertEquals(1, page.i32());
                int mailId = page.i32();
                assertTrue(mailId > 0);
                page.readBytes(GamePackets.MAIL_BOX_ENTRY_BYTES - 4);
                client.sendPlain(GamePackets.clientTakeMail(mailId));
                PacketReader reward = awaitOpcode(client, GamePackets.SERVER_DAILY_QUEST_STAMP);
                reward.u32();
                assertEquals(1, reward.u32());
                assertEquals(GamePackets.PAPEL_AWARD_TYPE, reward.u8());
                assertEquals(GamePackets.TYPEID_BOX_MAIL_REWARD_TEST, reward.u32());
                reward.i32();
                reward.u32();
                assertEquals(0, reward.i32());
                assertEquals(3, reward.i32());
                assertEquals(3, reward.i32());
                reward.readBytes(15);
                assertEquals(0, reward.remaining());
                assertEquals(0, awaitOpcode(client, GamePackets.SERVER_MAIL_TAKE).u32());
                assertEquals(3, inv.warehouse(10001).stream()
                        .filter(w -> w.typeid == GamePackets.TYPEID_BOX_MAIL_REWARD_TEST)
                        .findFirst()
                        .orElseThrow()
                        .c[0]);
                assertTrue(inv.warehouse(10001).stream().anyMatch(
                        w -> w.typeid == GamePackets.TYPEID_AIR_KNIGHT));
            } finally {
                inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_BOX_MAIL_TEST);
                inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_BOX_MAIL_OPENED_TEST);
                inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_BOX_MAIL_REWARD_TEST);
                inv.deleteItemIff(GamePackets.TYPEID_BOX_MAIL_TEST);
                inv.deleteBoxMailReward(GamePackets.TYPEID_BOX_MAIL_TEST);
            }
        }
    }

    @Test
    void specialPangCardConsumesAndSends160() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient client = new PangyaFakeClient()) {
            InventoryRepository inv = new JdbiInventoryRepository(DatabaseSupport.jdbi(ds));
            inv.deleteCardByTypeid(10001, GamePackets.TYPEID_CARD_SPECIAL_PANG);
            inv.deleteCardIff(GamePackets.TYPEID_CARD_SPECIAL_PANG);
            inv.setPangCookie(10001, 100000, 0);
            int cardId = inv.addCard(10001, GamePackets.TYPEID_CARD_SPECIAL_PANG, 1);
            try {
                inv.upsertCardSpecialIff(
                        GamePackets.TYPEID_CARD_SPECIAL_PANG,
                        0,
                        100,
                        GamePackets.CARD_EFFECT_PANG,
                        500,
                        0);
                LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(ds));
                String loginKey = repo.generateAuthKeyLogin(10001);
                String gameKey = repo.generateAuthKeyGame(10001, 20202);
                keys.putLoginKey(10001, loginKey);
                keys.putGameKey(10001, 20202, gameKey);
                loginToChannel(client, runtime.port(), "testuser", 10001, loginKey, gameKey);

                client.sendPlain(GamePackets.clientUseCard(GamePackets.TYPEID_CARD_SPECIAL_PANG));
                PacketReader ok = awaitOpcode(client, GamePackets.SERVER_USE_CARD);
                assertEquals(GamePackets.CARD_SPECIAL_OK, ok.u32());
                assertEquals(cardId, ok.u32());
                assertEquals(GamePackets.TYPEID_CARD_SPECIAL_PANG, ok.u32());
                assertEquals(0, ok.u32());
                assertEquals(0, ok.u32());
                assertEquals(0, ok.u32());
                assertEquals(1, ok.u32());
                ok.readBytes(GamePackets.SYSTEMTIME_BYTES * 2);
                assertEquals(0, ok.u16());
                assertEquals(0, ok.remaining());
                assertEquals(100500, inv.pang(10001));
                assertTrue(inv.cards(10001).stream().noneMatch(
                        c -> c.typeid == GamePackets.TYPEID_CARD_SPECIAL_PANG));

                client.sendPlain(GamePackets.clientUseCard(0));
                PacketReader zero = awaitOpcode(client, GamePackets.SERVER_USE_CARD);
                assertEquals(GamePackets.shopSys(GamePackets.CARD_ERR_TYPEID), zero.u32());
            } finally {
                inv.setPangCookie(10001, 100000, 0);
                inv.deleteCardByTypeid(10001, GamePackets.TYPEID_CARD_SPECIAL_PANG);
                inv.deleteCardIff(GamePackets.TYPEID_CARD_SPECIAL_PANG);
            }
        }
    }

    @Test
    void cardPackConsumesAndReturnsThreeCards() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient client = new PangyaFakeClient()) {
            InventoryRepository inv = new JdbiInventoryRepository(DatabaseSupport.jdbi(ds));
            int[] typeids = {
                GamePackets.TYPEID_CARD_PACK_TEST,
                GamePackets.TYPEID_CARD_PACK_REWARD_1,
                GamePackets.TYPEID_CARD_PACK_REWARD_2,
                GamePackets.TYPEID_CARD_PACK_REWARD_3
            };
            for (int typeid : typeids) {
                inv.deleteCardByTypeid(10001, typeid);
            }
            inv.deleteCardPackRewards(GamePackets.TYPEID_CARD_PACK_TEST);
            int packId = inv.addCard(10001, GamePackets.TYPEID_CARD_PACK_TEST, 1);
            try {
                inv.upsertCardPackReward(
                        GamePackets.TYPEID_CARD_PACK_TEST, 0, GamePackets.TYPEID_CARD_PACK_REWARD_1);
                inv.upsertCardPackReward(
                        GamePackets.TYPEID_CARD_PACK_TEST, 1, GamePackets.TYPEID_CARD_PACK_REWARD_2);
                inv.upsertCardPackReward(
                        GamePackets.TYPEID_CARD_PACK_TEST, 2, GamePackets.TYPEID_CARD_PACK_REWARD_3);
                LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(ds));
                String loginKey = repo.generateAuthKeyLogin(10001);
                String gameKey = repo.generateAuthKeyGame(10001, 20202);
                keys.putLoginKey(10001, loginKey);
                keys.putGameKey(10001, 20202, gameKey);
                loginToChannel(client, runtime.port(), "testuser", 10001, loginKey, gameKey);

                client.sendPlain(GamePackets.clientOpenCardPack(
                        GamePackets.TYPEID_CARD_PACK_TEST, packId));
                PacketReader ok = awaitOpcode(client, GamePackets.SERVER_OPEN_CARD_PACK);
                assertEquals(0, ok.u32());
                assertEquals(packId, readCardPackRow(
                        ok, GamePackets.TYPEID_CARD_PACK_TEST, 1, 3));
                assertTrue(readCardPackRow(
                        ok, GamePackets.TYPEID_CARD_PACK_REWARD_1, 1, 1) > 0);
                assertTrue(readCardPackRow(
                        ok, GamePackets.TYPEID_CARD_PACK_REWARD_2, 1, 1) > 0);
                assertTrue(readCardPackRow(
                        ok, GamePackets.TYPEID_CARD_PACK_REWARD_3, 1, 1) > 0);
                assertEquals(0, ok.remaining());
                assertTrue(inv.cards(10001).stream().noneMatch(
                        c -> c.typeid == GamePackets.TYPEID_CARD_PACK_TEST));
                for (int i = 1; i < typeids.length; i++) {
                    int rewardTypeid = typeids[i];
                    assertEquals(1, inv.cards(10001).stream()
                            .filter(c -> c.typeid == rewardTypeid)
                            .findFirst()
                            .orElseThrow()
                            .qntd);
                }

                client.sendPlain(GamePackets.clientOpenCardPack(
                        GamePackets.TYPEID_CARD_PACK_TEST, packId));
                assertEquals(GamePackets.CARD_PACK_ERR,
                        awaitOpcode(client, GamePackets.SERVER_OPEN_CARD_PACK).u32());
            } finally {
                for (int typeid : typeids) {
                    inv.deleteCardByTypeid(10001, typeid);
                }
                inv.deleteCardPackRewards(GamePackets.TYPEID_CARD_PACK_TEST);
            }
        }
    }

    @Test
    void memorialCoinAddsRewardAndSends264() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient client = new PangyaFakeClient()) {
            InventoryRepository inv = new JdbiInventoryRepository(DatabaseSupport.jdbi(ds));
            inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_MEMORIAL_COIN_TEST);
            inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_MEMORIAL_REWARD_TEST);
            inv.deleteItemIff(GamePackets.TYPEID_MEMORIAL_COIN_TEST);
            inv.deleteMemorialRewards(GamePackets.TYPEID_MEMORIAL_COIN_TEST);
            int coinId = inv.addWarehouseItem(10001, GamePackets.TYPEID_MEMORIAL_COIN_TEST, 1);
            try {
                inv.upsertItemIff(GamePackets.TYPEID_MEMORIAL_COIN_TEST);
                inv.upsertMemorialReward(
                        GamePackets.TYPEID_MEMORIAL_COIN_TEST,
                        0,
                        2,
                        GamePackets.TYPEID_MEMORIAL_REWARD_TEST,
                        3);
                LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(ds));
                String loginKey = repo.generateAuthKeyLogin(10001);
                String gameKey = repo.generateAuthKeyGame(10001, 20202);
                keys.putLoginKey(10001, loginKey);
                keys.putGameKey(10001, 20202, gameKey);
                loginToChannel(client, runtime.port(), "testuser", 10001, loginKey, gameKey);

                client.sendPlain(GamePackets.clientMemorial(GamePackets.TYPEID_MEMORIAL_COIN_TEST));
                PacketReader update = awaitOpcode(client, GamePackets.SERVER_DAILY_QUEST_STAMP);
                update.u32();
                assertEquals(2, update.u32());
                assertEquals(GamePackets.PAPEL_AWARD_TYPE, update.u8());
                assertEquals(GamePackets.TYPEID_MEMORIAL_REWARD_TEST, update.u32());
                int rewardId = update.i32();
                assertTrue(rewardId > 0);
                update.u32();
                assertEquals(0, update.i32());
                assertEquals(3, update.i32());
                assertEquals(3, update.i32());
                update.readBytes(GamePackets.PAPEL_AWARD_PAD);
                assertEquals(GamePackets.PAPEL_AWARD_TYPE, update.u8());
                assertEquals(GamePackets.TYPEID_MEMORIAL_COIN_TEST, update.u32());
                assertEquals(coinId, update.i32());
                update.u32();
                assertEquals(1, update.i32());
                assertEquals(0, update.i32());
                assertEquals(-1, update.i32());
                update.readBytes(GamePackets.PAPEL_AWARD_PAD);
                assertEquals(0, update.remaining());

                PacketReader ok = awaitOpcode(client, GamePackets.SERVER_MEMORIAL);
                assertEquals(GamePackets.MEMORIAL_OK, ok.u32());
                assertEquals(1, ok.u32());
                assertEquals(2, ok.i32());
                assertEquals(GamePackets.TYPEID_MEMORIAL_REWARD_TEST, ok.u32());
                assertEquals(3, ok.u32());
                assertEquals(0, ok.remaining());
                assertTrue(inv.warehouse(10001).stream().noneMatch(
                        w -> w.typeid == GamePackets.TYPEID_MEMORIAL_COIN_TEST));
                assertEquals(3, inv.warehouse(10001).stream()
                        .filter(w -> w.typeid == GamePackets.TYPEID_MEMORIAL_REWARD_TEST)
                        .findFirst()
                        .orElseThrow()
                        .c[0]);
                assertTrue(inv.warehouse(10001).stream().anyMatch(
                        w -> w.typeid == GamePackets.TYPEID_AIR_KNIGHT));
            } finally {
                inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_MEMORIAL_COIN_TEST);
                inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_MEMORIAL_REWARD_TEST);
                inv.deleteItemIff(GamePackets.TYPEID_MEMORIAL_COIN_TEST);
                inv.deleteMemorialRewards(GamePackets.TYPEID_MEMORIAL_COIN_TEST);
            }
        }
    }

    @Test
    void ticketReportScrollValidatesEncodedIdAndSends11a() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient client = new PangyaFakeClient()) {
            InventoryRepository inv = new JdbiInventoryRepository(DatabaseSupport.jdbi(ds));
            inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_TICKET_SCROLL_TEST);
            inv.deleteTicketReport(0x1234);
            int itemId = inv.addWarehouseItem(10001, GamePackets.TYPEID_TICKET_SCROLL_TEST, 1);
            DatabaseSupport.jdbi(ds).useHandle(h -> h.createUpdate("""
                            UPDATE pangya.pangya_item_warehouse
                               SET "C1" = 2, "C2" = 564
                             WHERE "UID" = 10001 AND item_id = :id
                            """)
                    .bind("id", itemId)
                    .execute());
            try {
                inv.upsertTicketReport(0x1234, java.time.Instant.EPOCH);
                LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(ds));
                String loginKey = repo.generateAuthKeyLogin(10001);
                String gameKey = repo.generateAuthKeyGame(10001, 20202);
                keys.putLoginKey(10001, loginKey);
                keys.putGameKey(10001, 20202, gameKey);
                loginToChannel(client, runtime.port(), "testuser", 10001, loginKey, gameKey);

                client.sendPlain(GamePackets.clientOpenTicketReport(itemId, 0x1235));
                PacketReader wrong = awaitOpcode(client, GamePackets.SERVER_TICKET_REPORT);
                assertEquals(GamePackets.TICKET_REPORT_ERR, wrong.i32());
                assertEquals(16, wrong.remaining());

                client.sendPlain(GamePackets.clientOpenTicketReport(itemId, 0x1234));
                PacketReader ok = awaitOpcode(client, GamePackets.SERVER_TICKET_REPORT);
                assertEquals(0, ok.u32());
                assertEquals(1970, ok.u16());
                assertEquals(1, ok.u16());
                assertEquals(4, ok.u16());
                ok.readBytes(10);
                assertEquals(0, ok.remaining());
                assertTrue(inv.warehouse(10001).stream().noneMatch(w -> w.id == itemId));
                assertTrue(inv.warehouse(10001).stream().anyMatch(
                        w -> w.typeid == GamePackets.TYPEID_AIR_KNIGHT));
            } finally {
                inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_TICKET_SCROLL_TEST);
                inv.deleteTicketReport(0x1234);
            }
        }
    }

    @Test
    void grandPrixEnterCreatesConfiguredRoom() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient client = new PangyaFakeClient()) {
            InventoryRepository inv = new JdbiInventoryRepository(DatabaseSupport.jdbi(ds));
            inv.deleteGrandPrixEvent(GamePackets.TYPEID_GP_EVENT_TEST);
            try {
                LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(ds));
                String loginKey = repo.generateAuthKeyLogin(10001);
                String gameKey = repo.generateAuthKeyGame(10001, 20202);
                keys.putLoginKey(10001, loginKey);
                keys.putGameKey(10001, 20202, gameKey);
                loginToChannel(client, runtime.port(), "testuser", 10001, loginKey, gameKey);

                client.sendPlain(GamePackets.clientGpEnter(GamePackets.TYPEID_GP_EVENT_TEST));
                assertEquals(GamePackets.shopSys(GamePackets.GP_ENTER_ERR_IFF),
                        awaitOpcode(client, GamePackets.SERVER_START_GAME_FAIL).u32());

                inv.upsertGrandPrixEvent(
                        GamePackets.TYPEID_GP_EVENT_TEST, "Test GP", 18, 0, 0, 0, 0, 1, 10);
                client.sendPlain(GamePackets.clientGpEnter(GamePackets.TYPEID_GP_EVENT_TEST));
                PacketReader entered = awaitOpcode(client, GamePackets.SERVER_ROOM_ENTER_RESULT);
                assertEquals(0, entered.i16());
                assertEquals(GamePackets.ROOM_INFO_BYTES, entered.remaining());
                entered.readBytes(GamePackets.ROOM_INFO_BYTES);

                client.sendPlain(GamePackets.clientStartGame());
                awaitOpcode(client, GamePackets.SERVER_START_GAME_FLAG);
                awaitOpcode(client, GamePackets.SERVER_START_GAME_FLAG2);
                awaitOpcode(client, GamePackets.SERVER_PANG_RATE);
                PacketReader init = awaitOpcode(client, GamePackets.SERVER_GAME_INIT);
                assertEquals(GamePackets.TIPO_TOURNEY, init.u8());
                assertEquals(1, init.u32());
                PacketReader course = awaitOpcode(client, GamePackets.SERVER_COURSE);
                assertEquals(0, course.u8());
                assertEquals(GamePackets.TIPO_TOURNEY, course.u8());
            } finally {
                inv.deleteGrandPrixEvent(GamePackets.TYPEID_GP_EVENT_TEST);
            }
        }
    }

    @Test
    void legacyTikiExchangesItemsAndPoints() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient client = new PangyaFakeClient()) {
            InventoryRepository inv = new JdbiInventoryRepository(DatabaseSupport.jdbi(ds));
            inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_TIKI_VALUE_TEST);
            inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_TIKI_REWARD_TEST);
            inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_TIKI_NEW_TEST);
            inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_MILEAGE_POINT);
            inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_TIKI_POINT);
            inv.deleteTikiItemValue(GamePackets.TYPEID_TIKI_VALUE_TEST);
            inv.deleteTikiItemValue(GamePackets.TYPEID_TIKI_NEW_TEST);
            inv.deleteTikiPointShopItem(GamePackets.TYPEID_TIKI_REWARD_TEST);
            inv.setLegacyTikiPoints(10001, 0);
            inv.setPangCookie(10001, 100000, 0);
            int valueId = inv.addWarehouseItem(10001, GamePackets.TYPEID_TIKI_VALUE_TEST, 4);
            int newId = inv.addWarehouseItem(10001, GamePackets.TYPEID_TIKI_NEW_TEST, 2);
            try {
                inv.upsertTikiItemValue(GamePackets.TYPEID_TIKI_VALUE_TEST, 2, 10);
                inv.upsertTikiNewValue(GamePackets.TYPEID_TIKI_NEW_TEST, 100, 600, 0, 0, 0);
                inv.upsertTikiPointShopItem(GamePackets.TYPEID_TIKI_REWARD_TEST, 3, 5);
                LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(ds));
                String loginKey = repo.generateAuthKeyLogin(10001);
                String gameKey = repo.generateAuthKeyGame(10001, 20202);
                keys.putLoginKey(10001, loginKey);
                keys.putGameKey(10001, 20202, gameKey);
                loginToChannel(client, runtime.port(), "testuser", 10001, loginKey, gameKey);

                client.sendPlain(GamePackets.clientTikiItemsToPoints(
                        GamePackets.TYPEID_TIKI_VALUE_TEST, valueId, 2, 999));
                PacketReader sold = awaitOpcode(client, GamePackets.SERVER_DAILY_QUEST_STAMP);
                sold.u32();
                assertEquals(1, sold.u32());
                PacketReader tp = awaitOpcode(client, GamePackets.SERVER_TIKI_EXCHANGE_TP);
                assertEquals(GamePackets.TIKI_EXCHANGE_OK, tp.u32());
                assertEquals(20, tp.u32());
                assertEquals(20, inv.legacyTikiPoints(10001));
                assertTrue(inv.warehouse(10001).stream().noneMatch(w -> w.id == valueId));

                client.sendPlain(GamePackets.clientTikiPoints());
                PacketReader points = awaitOpcode(client, GamePackets.SERVER_TIKI_POINTS);
                assertEquals(0, points.u32());
                assertEquals(20, points.u32());

                client.sendPlain(GamePackets.clientTikiPointsToItem(
                        GamePackets.TYPEID_TIKI_REWARD_TEST, 2, 999));
                PacketReader bought = awaitOpcode(client, GamePackets.SERVER_DAILY_QUEST_STAMP);
                bought.u32();
                assertEquals(1, bought.u32());
                PacketReader item = awaitOpcode(client, GamePackets.SERVER_TIKI_EXCHANGE_ITEM);
                assertEquals(GamePackets.TIKI_EXCHANGE_OK, item.u32());
                assertEquals(10, item.u32());
                assertEquals(10, inv.legacyTikiPoints(10001));
                assertEquals(6, inv.warehouse(10001).stream()
                        .filter(w -> w.typeid == GamePackets.TYPEID_TIKI_REWARD_TEST)
                        .findFirst()
                        .orElseThrow()
                        .c[0]);

                client.sendPlain(GamePackets.clientTikiShopExchange(
                        GamePackets.TYPEID_TIKI_NEW_TEST, newId, 2));
                PacketReader pang = awaitOpcode(client, GamePackets.SERVER_PANG_SPENT);
                assertEquals(99900, pang.u64());
                assertEquals(100, pang.u64());
                PacketReader tikiUpdate = awaitOpcode(client, GamePackets.SERVER_DAILY_QUEST_STAMP);
                tikiUpdate.u32();
                assertEquals(3, tikiUpdate.u32());
                PacketReader newTiki = awaitOpcode(client, GamePackets.SERVER_TIKI_SHOP_EXCHANGE);
                assertEquals(GamePackets.TIKI_SHOP_EXCHANGE_OK, newTiki.u32());
                assertEquals(1200, newTiki.u32());
                assertEquals(0, newTiki.u32());
                assertEquals(200, inv.warehouse(10001).stream()
                        .filter(w -> w.typeid == GamePackets.TYPEID_MILEAGE_POINT)
                        .findFirst()
                        .orElseThrow()
                        .c[0]);
                assertEquals(1, inv.warehouse(10001).stream()
                        .filter(w -> w.typeid == GamePackets.TYPEID_TIKI_POINT)
                        .findFirst()
                        .orElseThrow()
                        .c[0]);
                assertTrue(inv.warehouse(10001).stream().anyMatch(
                        w -> w.typeid == GamePackets.TYPEID_AIR_KNIGHT));
            } finally {
                inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_TIKI_VALUE_TEST);
                inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_TIKI_REWARD_TEST);
                inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_TIKI_NEW_TEST);
                inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_MILEAGE_POINT);
                inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_TIKI_POINT);
                inv.deleteTikiItemValue(GamePackets.TYPEID_TIKI_VALUE_TEST);
                inv.deleteTikiItemValue(GamePackets.TYPEID_TIKI_NEW_TEST);
                inv.deleteTikiPointShopItem(GamePackets.TYPEID_TIKI_REWARD_TEST);
                inv.setLegacyTikiPoints(10001, 0);
                inv.setPangCookie(10001, 100000, 0);
            }
        }
    }

    @Test
    void soloGrandZodiacSendsTourneyInit() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient client = new PangyaFakeClient()) {
            LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(ds));
            String loginKey = repo.generateAuthKeyLogin(10001);
            String gameKey = repo.generateAuthKeyGame(10001, 20202);
            keys.putLoginKey(10001, loginKey);
            keys.putGameKey(10001, 20202, gameKey);
            loginToChannel(client, runtime.port(), "testuser", 10001, loginKey, gameKey);
            client.sendPlain(GamePackets.clientCreateRoom(GamePackets.TIPO_GRAND_ZODIAC_INT, "GZ", ""));
            assertEquals(0, awaitOpcode(client, GamePackets.SERVER_ROOM_ENTER_RESULT).i16());
            client.sendPlain(GamePackets.clientStartGame());
            awaitOpcode(client, GamePackets.SERVER_START_GAME_FLAG);
            awaitOpcode(client, GamePackets.SERVER_START_GAME_FLAG2);
            awaitOpcode(client, GamePackets.SERVER_PANG_RATE);
            PacketReader init = awaitOpcode(client, GamePackets.SERVER_GAME_INIT);
            assertEquals(GamePackets.TIPO_GRAND_ZODIAC_INT, init.u8());
            assertEquals(1, init.u32());
            PacketReader course = awaitOpcode(client, GamePackets.SERVER_COURSE);
            assertEquals(0, course.u8());
            assertEquals(GamePackets.TIPO_GRAND_ZODIAC_INT, course.u8());
            client.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_CUTIN));
            PacketReader cutinTrunc = awaitOpcode(client, GamePackets.SERVER_CUTIN);
            assertEquals(0, cutinTrunc.u8());
            assertEquals(GamePackets.CUTIN_ERR, cutinTrunc.u16());
            client.sendPlain(GamePackets.clientCutin(10001, 1, 0, 0x04000000, 1));
            PacketReader cutinGz = awaitOpcode(client, GamePackets.SERVER_CUTIN);
            assertEquals(0, cutinGz.u8());
            assertEquals(GamePackets.CUTIN_GZ_DISABLED, cutinGz.u16());
        }
    }

    @Test
    void chipInPracticeLeaveSendsEndGame() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient client = new PangyaFakeClient()) {
            LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(ds));
            String loginKey = repo.generateAuthKeyLogin(10001);
            String gameKey = repo.generateAuthKeyGame(10001, 20202);
            keys.putLoginKey(10001, loginKey);
            keys.putGameKey(10001, 20202, gameKey);
            loginToChannel(client, runtime.port(), "testuser", 10001, loginKey, gameKey);
            client.sendPlain(GamePackets.clientCreateRoom(
                    GamePackets.TIPO_GRAND_ZODIAC_PRACTICE, "CHIP", ""));
            assertEquals(0, awaitOpcode(client, GamePackets.SERVER_ROOM_ENTER_RESULT).i16());
            client.sendPlain(GamePackets.clientStartGame());
            awaitOpcode(client, GamePackets.SERVER_START_GAME_FLAG);
            awaitOpcode(client, GamePackets.SERVER_START_GAME_FLAG2);
            awaitOpcode(client, GamePackets.SERVER_PANG_RATE);
            awaitOpcode(client, GamePackets.SERVER_GAME_INIT);
            awaitOpcode(client, GamePackets.SERVER_COURSE);
            client.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_LEAVE_CHIP_IN));
            PacketReader end = awaitOpcode(client, GamePackets.SERVER_GZ_END_GAME);
            assertEquals(0, end.remaining());
            PacketReader prize = awaitOpcode(client, GamePackets.SERVER_PRIZE_LIST);
            assertEquals(0, prize.u8());
        }
    }

    @Test
    void twoPlayersStartSscAndReceiveCourse() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient host = new PangyaFakeClient();
             PangyaFakeClient guest = new PangyaFakeClient()) {
            loginTwoPlayers(ds, keys, host, guest, runtime.port());
            host.sendPlain(GamePackets.clientCreateRoom(GamePackets.TIPO_SPECIAL_SHUFFLE_COURSE, "SSC", ""));
            PacketReader created = awaitOpcode(host, GamePackets.SERVER_ROOM_ENTER_RESULT);
            assertEquals(0, created.i16());
            int numero = roomNumberFromInfo(created.readBytes(GamePackets.ROOM_INFO_BYTES));
            guest.sendPlain(GamePackets.clientJoinRoom(numero, ""));
            assertEquals(0, awaitOpcode(guest, GamePackets.SERVER_ROOM_ENTER_RESULT).i16());
            host.sendPlain(GamePackets.clientStartGame());
            awaitOpcode(host, GamePackets.SERVER_START_GAME_FLAG);
            awaitOpcode(host, GamePackets.SERVER_START_GAME_FLAG2);
            awaitOpcode(host, GamePackets.SERVER_PANG_RATE);
            PacketReader init = awaitOpcode(host, GamePackets.SERVER_GAME_INIT);
            assertEquals(GamePackets.TIPO_TOURNEY, init.u8());
            assertEquals(1, init.u32());
            PacketReader course = awaitOpcode(host, GamePackets.SERVER_COURSE);
            assertEquals(0, course.u8());
            assertEquals(GamePackets.TIPO_TOURNEY, course.u8());
        }
    }

    private static void startTwoPlayerVersusMode(int tipo, String name) throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient host = new PangyaFakeClient();
             PangyaFakeClient guest = new PangyaFakeClient()) {
            loginTwoPlayers(ds, keys, host, guest, runtime.port());
            host.sendPlain(GamePackets.clientCreateRoom(tipo, name, ""));
            PacketReader created = awaitOpcode(host, GamePackets.SERVER_ROOM_ENTER_RESULT);
            assertEquals(0, created.i16());
            int numero = roomNumberFromInfo(created.readBytes(GamePackets.ROOM_INFO_BYTES));
            guest.sendPlain(GamePackets.clientJoinRoom(numero, ""));
            assertEquals(0, awaitOpcode(guest, GamePackets.SERVER_ROOM_ENTER_RESULT).i16());
            if (tipo == GamePackets.TIPO_MATCH) {
                host.sendPlain(GamePackets.clientTeamChat("go red"));
                PacketReader teamChat = awaitOpcode(host, GamePackets.SERVER_TEAM_CHAT);
                assertEquals("TestNick", teamChat.pstr());
                assertEquals("go red", teamChat.pstr());
            }
            host.sendPlain(GamePackets.clientStartGame());
            awaitOpcode(host, GamePackets.SERVER_START_GAME_FLAG);
            awaitOpcode(host, GamePackets.SERVER_START_GAME_FLAG2);
            awaitOpcode(host, GamePackets.SERVER_PANG_RATE);
            PacketReader init = awaitOpcode(host, GamePackets.SERVER_GAME_INIT);
            assertEquals(tipo, init.u8());
            assertEquals(2, init.u8());
            awaitOpcode(host, GamePackets.SERVER_COURSE);
            awaitOpcode(host, GamePackets.SERVER_MASCOT_SEED);
            PacketReader guestInit = awaitOpcode(guest, GamePackets.SERVER_GAME_INIT);
            assertEquals(tipo, guestInit.u8());
            assertEquals(2, guestInit.u8());
            host.sendPlain(GamePackets.clientLoadPercent(40));
            PacketReader load = awaitOpcode(host, GamePackets.SERVER_LOAD_PERCENT);
            assertTrue(load.i32() > 0);
            assertEquals(40, load.u8());
        }
    }

    @Test
    void lobbyChatReadyAndRoomInfoMatchCsharp() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient host = new PangyaFakeClient();
             PangyaFakeClient guest = new PangyaFakeClient()) {
            loginTwoPlayers(ds, keys, host, guest, runtime.port());

            host.sendPlain(GamePackets.clientRequestCash());
            PacketReader cash = awaitOpcode(host, GamePackets.SERVER_COOKIE);
            assertTrue(cash.u64() >= 0);

            host.sendPlain(GamePackets.clientRequestServerTime());
            PacketReader serverTime = awaitOpcode(host, GamePackets.SERVER_RESPONSE_SERVER_TIME);
            assertEquals(16, serverTime.remaining());
            assertTrue(serverTime.u16() >= 2026);

            host.sendPlain(GamePackets.clientWhisper("nobody", "are you there"));
            PacketReader offline = awaitOpcode(host, GamePackets.SERVER_CHAT);
            assertEquals(GamePackets.CHAT_OFFLINE, offline.u8());
            assertEquals("nobody", offline.pstr());

            host.sendPlain(GamePackets.clientWhisper("TestNick2", "hi guest"));
            PacketReader from = awaitOpcode(host, GamePackets.SERVER_WHISPER);
            assertEquals(GamePackets.WHISPER_FROM, from.u8());
            assertEquals("TestNick2", from.pstr());
            assertEquals("hi guest", from.pstr());
            PacketReader to = awaitOpcode(guest, GamePackets.SERVER_WHISPER);
            assertEquals(GamePackets.WHISPER_TO, to.u8());
            assertEquals("TestNick", to.pstr());
            assertEquals("hi guest", to.pstr());

            guest.sendPlain(GamePackets.clientAllowWhisper(0));
            guest.sendPlain(GamePackets.clientRequestCash());
            awaitOpcode(guest, GamePackets.SERVER_COOKIE);
            host.sendPlain(GamePackets.clientWhisper("TestNick2", "blocked"));
            PacketReader blocked = awaitOpcode(host, GamePackets.SERVER_CHAT);
            assertEquals(GamePackets.CHAT_OFFLINE, blocked.u8());
            guest.sendPlain(GamePackets.clientAllowWhisper(1));
            guest.sendPlain(GamePackets.clientRequestCash());
            awaitOpcode(guest, GamePackets.SERVER_COOKIE);

            host.sendPlain(GamePackets.clientKeepalive());
            host.sendPlain(GamePackets.clientEnterLobby());
            PacketReader clear = awaitOpcode(host, GamePackets.SERVER_USERLIST);
            assertEquals(GamePackets.LOBBY_USER_CLEAR, clear.u8());
            assertEquals(1, clear.u8());
            assertEquals(GamePackets.PLAYER_LOBBY_INFO_BYTES, clear.remaining());
            assertEquals(10001, clear.u32());
            clear.i32();
            assertEquals(0xFFFF, clear.u16());

            PacketReader list = awaitOpcode(host, GamePackets.SERVER_USERLIST);
            assertEquals(GamePackets.LOBBY_USER_LIST, list.u8());
            assertEquals(1, list.u8());
            assertEquals(GamePackets.PLAYER_LOBBY_INFO_BYTES, list.remaining());

            PacketReader rooms = awaitOpcode(host, GamePackets.SERVER_ROOMLIST);
            assertEquals(0, rooms.u8());
            assertEquals(GamePackets.ROOM_LIST_FULL, rooms.u8());
            assertEquals(-1, rooms.i16());
            assertEquals(0, rooms.remaining());

            PacketReader join = awaitOpcode(host, GamePackets.SERVER_USERLIST);
            assertEquals(GamePackets.LOBBY_USER_JOIN, join.u8());
            assertEquals(1, join.u8());
            PacketReader entered = awaitOpcode(host, GamePackets.SERVER_ENTER_LOBBY);
            assertEquals(0, entered.remaining());

            host.sendPlain(GamePackets.clientChat("TestNick", "hello lobby"));
            PacketReader chat = awaitOpcode(host, GamePackets.SERVER_CHAT);
            assertEquals(GamePackets.CHAT_NORMAL, chat.u8());
            assertEquals("TestNick", chat.pstr());
            assertEquals("hello lobby", chat.pstr());

            host.sendPlain(GamePackets.clientLeaveLobby());
            awaitOpcode(host, GamePackets.SERVER_LEAVE_LOBBY);

            host.sendPlain(GamePackets.clientCreateRoom(GamePackets.TIPO_STROKE, "VS-L", ""));
            PacketReader created = awaitOpcode(host, GamePackets.SERVER_ROOM_ENTER_RESULT);
            assertEquals(0, created.i16());
            int numero = roomNumberFromInfo(created.readBytes(GamePackets.ROOM_INFO_BYTES));
            host.sendPlain(GamePackets.clientInvite("TestNick2", 10002));
            PacketReader inviteAck = awaitOpcode(host, GamePackets.SERVER_INVITE_REPLY);
            assertEquals(0, inviteAck.u16());
            assertEquals(20202, inviteAck.u32());
            assertEquals(0, inviteAck.u8());
            assertEquals(numero, inviteAck.u16());
            assertEquals(10001, inviteAck.u32());
            assertEquals("TestNick", inviteAck.pstr());
            assertEquals(10002, inviteAck.u32());
            PacketReader invite = awaitOpcode(guest, GamePackets.SERVER_INVITE);
            assertEquals(0, invite.u16());
            assertEquals(20202, invite.u32());
            assertEquals(0, invite.u8());
            assertEquals(numero, invite.u16());
            assertEquals(10001, invite.u32());
            assertEquals("TestNick", invite.pstr());
            assertEquals(10002, invite.u32());
            guest.sendPlain(GamePackets.clientJoinRoom(numero, ""));
            assertEquals(0, awaitOpcode(guest, GamePackets.SERVER_ROOM_ENTER_RESULT).i16());

            guest.sendPlain(GamePackets.clientSetReady(1));
            PacketReader ready = awaitOpcode(host, GamePackets.SERVER_READY);
            assertTrue(ready.i32() > 0);
            assertEquals(1, ready.u8());

            host.sendPlain(GamePackets.clientChangeRoomCourse(numero, 5));
            PacketReader updated = awaitOpcode(host, GamePackets.SERVER_ROOM_UPDATE);
            assertEquals(-1, updated.i16());
            assertEquals(GamePackets.tipoShow(GamePackets.TIPO_STROKE), updated.u8());
            assertEquals(5, updated.u8());

            host.sendPlain(GamePackets.clientChangeTeam(1));
            PacketReader team = awaitOpcode(host, GamePackets.SERVER_TEAM);
            assertTrue(team.i32() > 0);
            assertEquals(1, team.u8());
            PacketReader guestTeam = awaitOpcode(guest, GamePackets.SERVER_TEAM);
            assertTrue(guestTeam.i32() > 0);
            assertEquals(1, guestTeam.u8());

            host.sendPlain(GamePackets.clientRequestRoomDetail(numero));
            PacketReader detail = awaitOpcode(host, GamePackets.SERVER_ROOM_DETAIL);
            assertEquals(2, detail.u32());
            assertEquals(18, detail.u8());
            detail.u32();
            detail.u8();
            assertEquals(GamePackets.TIPO_STROKE, detail.u8());

            guest.sendPlain(GamePackets.clientExitRoom());
            PacketReader left = awaitRoomPlayers(host, 2);
            assertEquals(-1, left.i16());
            assertTrue(left.i32() > 0);
            PacketReader exit = awaitOpcode(guest, GamePackets.SERVER_EXIT_ROOM);
            assertEquals(-1, exit.i16());

            guest.sendPlain(GamePackets.clientJoinRoom(numero, ""));
            assertEquals(0, awaitOpcode(guest, GamePackets.SERVER_ROOM_ENTER_RESULT).i16());
            host.sendPlain(GamePackets.clientExitRoom());
            PacketReader master = awaitOpcode(guest, GamePackets.SERVER_DECISION_ROOM_MASTER);
            assertTrue(master.i32() > 0);
            assertEquals(0, master.i16());
            PacketReader hostExit = awaitOpcode(host, GamePackets.SERVER_EXIT_ROOM);
            assertEquals(-1, hostExit.i16());
            host.sendPlain(GamePackets.clientJoinRoom(numero, ""));
            assertEquals(0, awaitOpcode(host, GamePackets.SERVER_ROOM_ENTER_RESULT).i16());
            guest.sendPlain(GamePackets.clientBanish(10001));
            PacketReader kicked = awaitOpcode(host, GamePackets.SERVER_EXIT_ROOM);
            assertEquals(-1, kicked.i16());
            PacketReader kickedList = awaitRoomPlayers(guest, 2);
            assertEquals(-1, kickedList.i16());
            assertTrue(kickedList.i32() > 0);
        }
    }

    @Test
    void playerInfoMacrosServerListAndRankMatchCsharp() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient host = new PangyaFakeClient();
             PangyaFakeClient guest = new PangyaFakeClient()) {
            LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(ds));
            repo.upsertServer(new LoginRepository.ServerListRow(
                    "RANK", 4774, "127.0.0.1", 4774, 100, 0, 4,
                    0, 0, 0, (short) 0, (short) 0, (short) 0, (short) 0,
                    "Release.JP.983.01", "JP.R7.983.01"));
            repo.upsertServer(new LoginRepository.ServerListRow(
                    "MSN", 30201, "127.0.0.1", 30201, 100, 0, 3,
                    0, 0, 0, (short) 0, (short) 0, (short) 0, (short) 0,
                    "Release.JP.983.01", "JP.R7.983.01"));
            loginTwoPlayers(ds, keys, host, guest, runtime.port());

            host.sendPlain(GamePackets.clientRequestUserInfo(10001, 0));
            List<byte[]> dump = collect(host, GamePackets.PLAYER_INFO_DUMP_COUNT + 1, 8, TimeUnit.SECONDS);
            assertEquals(GamePackets.PLAYER_INFO_DUMP_COUNT + 1, dump.size());
            int[] opcodes = {0x157, 0x15E, 0x156, 0x158, 0x15D, 0x15C, 0x15C, 0x15B, 0x15A, 0x159, 0x15C, 0x257};
            for (int i = 0; i < opcodes.length; i++) {
                assertEquals(opcodes[i], new PacketReader(dump.get(i)).opcode());
            }
            PacketReader ack = new PacketReader(dump.get(GamePackets.PLAYER_INFO_DUMP_COUNT));
            assertEquals(GamePackets.SERVER_PLAYER_INFO, ack.opcode());
            assertEquals(GamePackets.PLAYER_INFO_OK, ack.u32());
            assertEquals(0, ack.u8());
            assertEquals(10001, ack.u32());
            PacketReader member = new PacketReader(dump.get(0));
            member.opcode();
            member.u8();
            member.u32();
            assertEquals(0xFFFF, member.u16());

            host.sendPlain(GamePackets.clientRequestUserInfo(99999, 0));
            PacketReader missing = awaitOpcode(host, GamePackets.SERVER_PLAYER_INFO);
            assertEquals(GamePackets.PLAYER_INFO_OK, missing.u32());
            assertEquals(0, missing.u8());
            assertEquals(0, missing.u32());

            host.sendPlain(GamePackets.clientUserInfoOffline(0, "TestNick2"));
            PacketReader offline = awaitOpcode(host, GamePackets.SERVER_USERINFO_OFFLINE);
            assertEquals(GamePackets.USERINFO_OFFLINE_FOUND, offline.u8());
            assertEquals(10002, offline.u32());
            assertEquals(GamePackets.MEMBER_INFO_EX_BYTES, offline.remaining());
            host.sendPlain(GamePackets.clientUserInfoOffline(0, "NobodyNick"));
            PacketReader offlineMiss = awaitOpcode(host, GamePackets.SERVER_USERINFO_OFFLINE);
            assertEquals(GamePackets.USERINFO_OFFLINE_MISSING, offlineMiss.u8());
            assertEquals(0, offlineMiss.remaining());

            host.sendPlain(GamePackets.clientUpdateMacros(new String[] {
                    "Nice!", "Good!", "OK", "Thanks", "Sorry", "Go", "Nice shot!", "Wow", "GG"}));
            Thread.sleep(200);
            String[] macros = repo.macros(10001);
            assertEquals("Nice!", macros[0]);
            assertEquals("GG", macros[8]);

            host.sendPlain(GamePackets.clientRequestServerList());
            PacketReader servers = awaitOpcode(host, GamePackets.SERVER_SERVER_LIST);
            int gsCount = servers.u8();
            assertTrue(gsCount >= 1);
            servers.readBytes(gsCount * GamePackets.SERVER_INFO_BYTES);
            assertEquals(2, servers.u8());
            assertEquals(2 * GamePackets.CHANNEL_INFO_BYTES, servers.remaining());

            host.sendPlain(GamePackets.clientRequestRank());
            PacketReader rank = awaitOpcode(host, GamePackets.SERVER_RANK_ADDRESS);
            assertEquals("127.0.0.1", rank.pstr());
            assertEquals(4774, rank.i32());

            host.sendPlain(GamePackets.clientMessengerList());
            PacketReader msn = awaitOpcode(host, GamePackets.SERVER_MESSENGER_LIST);
            assertEquals(1, msn.u8());
            msn.readBytes(GamePackets.SERVER_INFO_BYTES);
            assertEquals(0, msn.remaining());

            host.sendPlain(GamePackets.clientLast5());
            PacketReader last5 = awaitOpcode(host, GamePackets.SERVER_LAST5);
            assertEquals(GamePackets.LAST5_COUNT * GamePackets.LAST5_PLAYER_BYTES, last5.remaining());
            for (int i = 0; i < GamePackets.LAST5_COUNT; i++) {
                assertEquals(0, last5.u32());
                assertEquals("", last5.fixedStr(22));
                assertEquals("", last5.fixedStr(22));
                assertEquals(0, last5.u32());
            }
        }
    }

    @Test
    void dailyQuestAcceptLeaveAndRewardRoundtrip() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient client = new PangyaFakeClient()) {
            InventoryRepository inv = new JdbiInventoryRepository(DatabaseSupport.jdbi(ds));
            cleanupDailyQuest(ds);
            inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_DAILY_REWARD_TEST);
            inv.deleteDailyQuestStuff(GamePackets.TYPEID_DAILY_QUEST_STUFF_TEST);
            inv.deleteDailyQuestRewards(GamePackets.TYPEID_DAILY_ACHIEVEMENT_TEST);
            try {
                inv.upsertDailyQuestStuff(
                        GamePackets.TYPEID_DAILY_QUEST_STUFF_TEST,
                        GamePackets.TYPEID_DAILY_COUNTER_TEST);
                inv.upsertDailyQuestReward(
                        GamePackets.TYPEID_DAILY_ACHIEVEMENT_TEST,
                        0,
                        GamePackets.TYPEID_DAILY_REWARD_TEST,
                        2,
                        0);
                int leaveId = insertDailyAchievement(ds);
                LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(ds));
                String loginKey = repo.generateAuthKeyLogin(10001);
                String gameKey = repo.generateAuthKeyGame(10001, 20202);
                keys.putLoginKey(10001, loginKey);
                keys.putGameKey(10001, 20202, gameKey);
                loginToChannel(client, runtime.port(), "testuser", 10001, loginKey, gameKey);

                client.sendPlain(GamePackets.clientAcceptDailyQuest(leaveId));
                PacketReader counterAdd = awaitOpcode(client, GamePackets.SERVER_DAILY_QUEST_STAMP);
                counterAdd.u32();
                assertEquals(1, counterAdd.u32());
                PacketReader accepted = awaitOpcode(client, GamePackets.SERVER_DAILY_QUEST_ACCEPT);
                assertEquals(0, accepted.i32());
                assertEquals(1, accepted.i32());
                assertEquals(1, accepted.u8());
                assertEquals(GamePackets.TYPEID_DAILY_ACHIEVEMENT_TEST, accepted.u32());
                assertEquals(leaveId, accepted.i32());
                assertEquals(3, accepted.i32());
                assertEquals(1, accepted.u32());
                assertEquals(GamePackets.TYPEID_DAILY_QUEST_STUFF_TEST, accepted.u32());
                assertEquals(GamePackets.TYPEID_DAILY_COUNTER_TEST, accepted.u32());
                assertTrue(accepted.i32() > 0);
                assertEquals(0, accepted.u32());

                client.sendPlain(GamePackets.clientLeaveDailyQuest(leaveId));
                PacketReader counterRemove = awaitOpcode(client, GamePackets.SERVER_DAILY_QUEST_STAMP);
                counterRemove.u32();
                assertEquals(1, counterRemove.u32());
                PacketReader left = awaitOpcode(client, GamePackets.SERVER_DAILY_QUEST_LEAVE);
                assertEquals(0, left.i32());
                assertEquals(1, left.i32());
                assertEquals(leaveId, left.i32());
                assertTrue(inv.achievements(10001).stream().noneMatch(a -> a.id() == leaveId));

                int rewardId = insertDailyAchievement(ds);
                client.sendPlain(GamePackets.clientAcceptDailyQuest(rewardId));
                awaitOpcode(client, GamePackets.SERVER_DAILY_QUEST_STAMP);
                awaitOpcode(client, GamePackets.SERVER_DAILY_QUEST_ACCEPT);
                client.sendPlain(GamePackets.clientRewardDailyQuest(rewardId));
                PacketReader rewardUpdate = awaitOpcode(client, GamePackets.SERVER_DAILY_QUEST_STAMP);
                rewardUpdate.u32();
                assertEquals(2, rewardUpdate.u32());
                PacketReader rewarded = awaitOpcode(client, GamePackets.SERVER_DAILY_QUEST_REWARD);
                assertEquals(0, rewarded.i32());
                assertEquals(1, rewarded.i32());
                assertEquals(rewardId, rewarded.i32());
                assertEquals(2, inv.warehouse(10001).stream()
                        .filter(w -> w.typeid == GamePackets.TYPEID_DAILY_REWARD_TEST)
                        .findFirst()
                        .orElseThrow()
                        .c[0]);

                client.sendPlain(GamePackets.clientAcceptDailyQuest());
                assertEquals(GamePackets.DAILY_QUEST_ACCEPT_FAIL,
                        awaitOpcode(client, GamePackets.SERVER_DAILY_QUEST_ACCEPT).i32());
                assertTrue(inv.warehouse(10001).stream().anyMatch(
                        w -> w.typeid == GamePackets.TYPEID_AIR_KNIGHT));
            } finally {
                cleanupDailyQuest(ds);
                inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_DAILY_REWARD_TEST);
                inv.deleteDailyQuestStuff(GamePackets.TYPEID_DAILY_QUEST_STUFF_TEST);
                inv.deleteDailyQuestRewards(GamePackets.TYPEID_DAILY_ACHIEVEMENT_TEST);
            }
        }
    }

    @Test
    void achievementGuiSends22dThen22c() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient client = new PangyaFakeClient()) {
            InventoryRepository inv = new JdbiInventoryRepository(DatabaseSupport.jdbi(ds));
            cleanupDailyQuest(ds);
            inv.deleteDailyQuestStuff(GamePackets.TYPEID_DAILY_QUEST_STUFF_TEST);
            try {
                inv.upsertDailyQuestStuff(
                        GamePackets.TYPEID_DAILY_QUEST_STUFF_TEST,
                        GamePackets.TYPEID_DAILY_COUNTER_TEST);
                int achievementId = insertDailyAchievement(ds);
                InventoryRepository.DailyQuestMutation accepted =
                        inv.acceptDailyQuests(10001, new int[] {achievementId});
                int counterId = accepted.counters().get(0).id();
                DatabaseSupport.jdbi(ds).useHandle(h -> h.createUpdate("""
                                UPDATE pangya.pangya_counter_item SET "Count_Num_Item" = 7
                                 WHERE "UID" = 10001 AND "Count_ID" = :id
                                """)
                        .bind("id", counterId)
                        .execute());
                LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(ds));
                String loginKey = repo.generateAuthKeyLogin(10001);
                String gameKey = repo.generateAuthKeyGame(10001, 20202);
                keys.putLoginKey(10001, loginKey);
                keys.putGameKey(10001, 20202, gameKey);
                loginToChannel(client, runtime.port(), "testuser", 10001, loginKey, gameKey);

                client.sendPlain(GamePackets.clientAchievement(10001));
                PacketReader data = awaitOpcode(client, GamePackets.SERVER_ACHIEVEMENT_GUI_DATA);
                assertEquals(0, data.u32());
                assertEquals(1, data.u32());
                assertEquals(1, data.u32());
                assertEquals(GamePackets.TYPEID_DAILY_ACHIEVEMENT_TEST, data.u32());
                assertEquals(achievementId, data.i32());
                assertEquals(1, data.u32());
                assertEquals(GamePackets.TYPEID_DAILY_QUEST_STUFF_TEST, data.u32());
                assertEquals(7, data.i32());
                assertEquals(0, data.u32());
                assertEquals(0, data.remaining());
                assertEquals(0, awaitOpcode(client, GamePackets.SERVER_ACHIEVEMENT_GUI).i32());
            } finally {
                cleanupDailyQuest(ds);
                inv.deleteDailyQuestStuff(GamePackets.TYPEID_DAILY_QUEST_STUFF_TEST);
            }
        }
    }

    @Test
    void dailyQuestDeleteItemOtherChannelMatchCsharp() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient host = new PangyaFakeClient();
             PangyaFakeClient guest = new PangyaFakeClient()) {
            loginTwoPlayers(ds, keys, host, guest, runtime.port());
            InventoryRepository inv = new JdbiInventoryRepository(DatabaseSupport.jdbi(ds));
            long pang = inv.pang(10001);
            long cookie = inv.cookie(10001);

            int before = GamePackets.unixNow();
            host.sendPlain(GamePackets.clientDailyQuest());
            PacketReader stamp = awaitOpcode(host, GamePackets.SERVER_DAILY_QUEST_STAMP);
            int stampUnix = stamp.i32();
            assertEquals(0, stamp.i32());
            assertTrue(stampUnix >= before - 1 && stampUnix <= GamePackets.unixNow() + 1);
            PacketReader info = awaitOpcode(host, GamePackets.SERVER_DAILY_QUEST_INFO);
            assertEquals(0, info.i32());
            int current = info.u32();
            assertEquals(0, info.u32());
            assertEquals(0, info.u32());
            assertEquals(0, info.u32());
            assertEquals(0, info.u32());
            assertEquals(0, info.u32());
            assertEquals(0, info.i32());
            assertTrue(current >= before - 1 && current <= GamePackets.unixNow() + 1);

            host.sendPlain(GamePackets.clientDailyQuest());
            awaitOpcode(host, GamePackets.SERVER_DAILY_QUEST_STAMP);
            PacketReader again = awaitOpcode(host, GamePackets.SERVER_DAILY_QUEST_INFO);
            assertEquals(0, again.i32());
            assertEquals(current, again.u32());

            host.sendPlain(GamePackets.clientAcceptDailyQuest());
            PacketReader accept = awaitOpcode(host, GamePackets.SERVER_DAILY_QUEST_ACCEPT);
            assertEquals(GamePackets.DAILY_QUEST_ACCEPT_FAIL, accept.i32());
            assertEquals(0, accept.i32());

            host.sendPlain(GamePackets.clientLeaveDailyQuest());
            PacketReader leave = awaitOpcode(host, GamePackets.SERVER_DAILY_QUEST_LEAVE);
            assertEquals(GamePackets.DAILY_QUEST_LEAVE_FAIL, leave.i32());
            assertEquals(0, leave.remaining());

            host.sendPlain(GamePackets.clientRewardDailyQuest());
            PacketReader reward = awaitOpcode(host, GamePackets.SERVER_DAILY_QUEST_REWARD);
            assertEquals(GamePackets.DAILY_QUEST_REWARD_FAIL, reward.i32());
            assertEquals(0, reward.i32());

            host.sendPlain(GamePackets.clientCadie(0, 1, 0));
            PacketReader cadie = awaitOpcode(host, GamePackets.SERVER_CADIE);
            assertEquals(GamePackets.shopSys(GamePackets.CADIE_ERR_COUNT), cadie.u32());
            host.sendPlain(GamePackets.clientCadie(0, 1, 1));
            PacketReader cadieIff = awaitOpcode(host, GamePackets.SERVER_CADIE);
            assertEquals(GamePackets.shopSys(GamePackets.CADIE_ERR_IFF), cadieIff.u32());
            host.sendPlain(GamePackets.clientLolo(0, 0, 0, 0));
            PacketReader lolo = awaitOpcode(host, GamePackets.SERVER_LOLO);
            assertEquals(GamePackets.shopSys(GamePackets.LOLO_ERR_IFF), lolo.u32());

            inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
            inv.setPangCookie(10001, 100000, 0);
            var bought = inv.buyShopItem(
                    10001, GamePackets.TYPEID_SHOP_PANG_ITEM, 1, GamePackets.SHOP_PANG_PRICE, 0);
            assertEquals(0, bought.code());
            int beforeCadie = GamePackets.unixNow();
            host.sendPlain(GamePackets.clientCadieItems(
                    0, 1, GamePackets.TYPEID_SHOP_PANG_ITEM, bought.itemId()));
            PacketReader cadieAwards = awaitOpcode(host, GamePackets.SERVER_DAILY_QUEST_STAMP);
            int cadieUnix = cadieAwards.u32();
            assertTrue(cadieUnix >= beforeCadie - 1 && cadieUnix <= GamePackets.unixNow() + 1);
            assertEquals(2, cadieAwards.u32());
            PacketReader cadieOk = awaitOpcode(host, GamePackets.SERVER_CADIE);
            assertEquals(0, cadieOk.u32());
            assertEquals(0, cadieOk.u32());
            assertEquals(1, cadieOk.u32());
            assertEquals(GamePackets.TYPEID_SHOP_PANG_ITEM, cadieOk.u32());
            assertEquals(bought.itemId(), cadieOk.i32());
            assertEquals(1, cadieOk.i32());
            assertEquals(1, cadieOk.i32());
            assertEquals(0, cadieOk.u32());
            inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
            inv.setPangCookie(10001, pang, cookie);

            int card = GamePackets.TYPEID_CARD_NORMAL;
            inv.addCard(10001, card, GamePackets.LOLO_CARD_COUNT);
            inv.setPangCookie(10001, 100000, 0);
            int beforeLolo = GamePackets.unixNow();
            host.sendPlain(GamePackets.clientLolo(
                    3L * GamePackets.LOLO_PANG_NORMAL, card, card, card));
            PacketReader loloSpent = awaitOpcode(host, GamePackets.SERVER_PANG_SPENT);
            assertEquals(100000 - 3L * GamePackets.LOLO_PANG_NORMAL, loloSpent.u64());
            assertEquals(3L * GamePackets.LOLO_PANG_NORMAL, loloSpent.u64());
            PacketReader loloAwards = awaitOpcode(host, GamePackets.SERVER_DAILY_QUEST_STAMP);
            int loloUnix = loloAwards.u32();
            assertTrue(loloUnix >= beforeLolo - 1 && loloUnix <= GamePackets.unixNow() + 1);
            assertEquals(2, loloAwards.u32());
            PacketReader loloTipo = awaitOpcode(host, GamePackets.SERVER_LOLO_TIPO);
            assertEquals(GamePackets.CARD_TYPE_NORMAL, loloTipo.u32());
            PacketReader loloOk = awaitOpcode(host, GamePackets.SERVER_LOLO);
            assertEquals(0, loloOk.u32());
            assertEquals(card, loloOk.u32());
            inv.deleteCardByTypeid(10001, card);
            inv.setPangCookie(10001, pang, cookie);

            host.sendPlain(GamePackets.clientRefreshGacha());
            PacketReader gacha = awaitOpcode(host, GamePackets.SERVER_GACHA_COUPON);
            assertEquals(0, gacha.i32());
            assertEquals(0, gacha.i32());
            assertEquals(pang, gacha.u64());
            assertEquals(cookie, gacha.u64());

            host.sendPlain(GamePackets.clientEnchant(1, 0, 99999));
            PacketReader club = awaitOpcode(host, GamePackets.SERVER_CLUB_STATS);
            assertEquals(GamePackets.CLUB_STATS_ERR, club.u8());

            host.sendPlain(GamePackets.clientIntrusion(0, 99));
            PacketReader intrusion = awaitOpcode(host, GamePackets.SERVER_INTRUSION);
            assertEquals(GamePackets.INTRUSION_ERR, intrusion.u8());
            assertEquals(GamePackets.INTRUSION_SYS, intrusion.u8());

            host.sendPlain(GamePackets.clientPapelPlay());
            PacketReader papelPlay = awaitOpcode(host, GamePackets.SERVER_PAPEL_PLAY);
            assertEquals(0, papelPlay.u32());
            assertEquals(0, papelPlay.i32());
            int papelBalls = papelPlay.u32();
            assertTrue(papelBalls >= GamePackets.PAPEL_MIN_BALL && papelBalls <= GamePackets.PAPEL_MAX_BALL);

            host.sendPlain(GamePackets.clientWebLink(70));
            host.sendPlain(GamePackets.clientJoinGallery(99, "x"));
            host.sendPlain(GamePackets.clientGmCommand(3));
            host.sendPlain(GamePackets.clientAutoCommand());
            host.sendPlain(GamePackets.clientRequestKick());
            host.sendPlain(GamePackets.clientPangInfo());

            host.sendPlain(GamePackets.clientWebAuthKey());
            PacketReader webKeyPkt = awaitOpcode(host, GamePackets.SERVER_WEB_AUTH_KEY);
            assertEquals(GamePackets.WEB_KEY_OK, webKeyPkt.i32());
            String webKey = webKeyPkt.pstr();
            assertEquals(6, webKey.length());
            assertEquals(0, webKeyPkt.remaining());

            host.sendPlain(GamePackets.clientChangeGameServer(99999));
            PacketReader gsUnknown = awaitOpcode(host, GamePackets.SERVER_SERVER_LIST);
            assertTrue(gsUnknown.u8() >= 1);

            host.sendPlain(GamePackets.clientChangeGameServer(20202));
            PacketReader gsSwitch = awaitOpcode(host, GamePackets.SERVER_CHANGE_GAME_SERVER);
            assertEquals(GamePackets.CHANGE_GS_OK, gsSwitch.i32());
            assertFalse(gsSwitch.pstr().isEmpty());

            host.sendPlain(GamePackets.clientOpenTicketReport(0, 0));
            PacketReader ticketFail = awaitOpcode(host, GamePackets.SERVER_TICKET_REPORT);
            assertEquals(GamePackets.TICKET_REPORT_ERR, ticketFail.i32());
            assertEquals(16, ticketFail.remaining());

            host.sendPlain(GamePackets.clientTikiShop());
            PacketReader tikiOpen = awaitOpcode(host, GamePackets.SERVER_TIKI_SHOP);
            assertEquals(0, tikiOpen.u32());

            host.sendPlain(GamePackets.clientLockerAccess(""));
            PacketReader lockerEmpty = awaitOpcode(host, GamePackets.SERVER_LOCKER_ACCESS);
            assertEquals(GamePackets.LOCKER_ERR_EMPTY, lockerEmpty.u32());
            host.sendPlain(GamePackets.clientLockerAccess("1234"));
            PacketReader lockerWrong = awaitOpcode(host, GamePackets.SERVER_LOCKER_ACCESS);
            assertEquals(GamePackets.LOCKER_ERR_WRONG, lockerWrong.u32());

            host.sendPlain(GamePackets.clientLockerState());
            PacketReader lockerSt = awaitOpcode(host, GamePackets.SERVER_LOCKER_STATE);
            assertEquals(0, lockerSt.u32());
            assertEquals(GamePackets.LOCKER_STATE_NO_PASS, lockerSt.u32());

            host.sendPlain(GamePackets.clientClubWorkshopLevel(0, 1, 0));
            PacketReader workshopFail = awaitOpcode(host, GamePackets.SERVER_CLUB_WORKSHOP_LEVEL);
            assertEquals(GamePackets.shopSys(GamePackets.WORKSHOP_ERR_GROUP), workshopFail.u32());

            host.sendPlain(GamePackets.clientLuckyPouch(0));
            PacketReader pouchFail = awaitOpcode(host, GamePackets.SERVER_LUCKY_POUCH);
            assertEquals(GamePackets.LUCKY_POUCH_ERR, pouchFail.u8());
            assertEquals(12, pouchFail.remaining());

            host.sendPlain(GamePackets.clientCompleteQuest(99, 0));
            PacketReader tutoFail = awaitOpcode(host, GamePackets.SERVER_LOGIN_ACK);
            assertEquals(GamePackets.GACHA_ERR_MARKER, tutoFail.u8());
            assertEquals(GamePackets.shopSys(GamePackets.TUTORIAL_ERR_TIPO), tutoFail.u32());

            host.sendPlain(GamePackets.clientHeartbeat());
            host.sendPlain(GamePackets.clientUpdatePlace(1));
            host.sendPlain(GamePackets.clientUseTicketReport());
            host.sendPlain(GamePackets.clientActivePaws());
            host.sendPlain(GamePackets.clientActiveRing());

            host.sendPlain(GamePackets.clientTikiPoints());
            PacketReader tikiPts = awaitOpcode(host, GamePackets.SERVER_TIKI_POINTS);
            assertEquals(0, tikiPts.u32());
            assertEquals(0, tikiPts.u32());
            host.sendPlain(GamePackets.clientTikiExchange(GamePackets.CLIENT_TIKI_EXCHANGE_TP, 0));
            PacketReader tikiTp = awaitOpcode(host, GamePackets.SERVER_TIKI_EXCHANGE_TP);
            assertEquals(GamePackets.shopSys(GamePackets.TIKI_EXCHANGE_ERR_PTS), tikiTp.u32());
            host.sendPlain(GamePackets.clientTikiExchange(GamePackets.CLIENT_TIKI_EXCHANGE_ITEM, 0));
            PacketReader tikiItem = awaitOpcode(host, GamePackets.SERVER_TIKI_EXCHANGE_ITEM);
            assertEquals(GamePackets.shopSys(GamePackets.TIKI_EXCHANGE_ERR_PTS), tikiItem.u32());

            host.sendPlain(GamePackets.clientClubWorkshopEmpty(GamePackets.CLIENT_CLUB_WORKSHOP_CONFIRM));
            PacketReader wsConfirm = awaitOpcode(host, GamePackets.SERVER_CLUB_WORKSHOP_CONFIRM);
            assertEquals(GamePackets.shopSys(GamePackets.WORKSHOP_CONFIRM_ERR), wsConfirm.u32());
            host.sendPlain(GamePackets.clientClubWorkshopEmpty(GamePackets.CLIENT_CLUB_WORKSHOP_CANCEL));
            PacketReader wsCancel = awaitOpcode(host, GamePackets.SERVER_CLUB_WORKSHOP_CANCEL);
            assertEquals(GamePackets.shopSys(GamePackets.WORKSHOP_CANCEL_ERR), wsCancel.u32());
            host.sendPlain(GamePackets.clientClubWorkshopRank(0, 1, 0));
            PacketReader wsRank = awaitOpcode(host, GamePackets.SERVER_CLUB_WORKSHOP_RANK);
            assertEquals(GamePackets.shopSys(GamePackets.WORKSHOP_RANK_ERR), wsRank.u32());

            host.sendPlain(GamePackets.clientItemBuff(0));
            PacketReader buffFail = awaitOpcode(host, GamePackets.SERVER_ITEM_BUFF);
            assertEquals(GamePackets.shopSys(GamePackets.BUFF_ERR_TYPEID), buffFail.u32());
            host.sendPlain(GamePackets.clientCometRefill(0, 0));
            PacketReader cometFail = awaitOpcode(host, GamePackets.SERVER_COMET_REFILL);
            assertEquals(0, cometFail.u8());
            assertEquals(10, cometFail.remaining());
            host.sendPlain(GamePackets.clientBoxMail(0));
            PacketReader boxFail = awaitOpcode(host, GamePackets.SERVER_BOX_MAIL);
            assertEquals(GamePackets.shopSys(GamePackets.BOX_MAIL_ERR_TYPEID), boxFail.u32());

            host.sendPlain(GamePackets.clientLockerItems(0, 1));
            PacketReader lockerPage = awaitOpcode(host, GamePackets.SERVER_LOCKER_ITEMS);
            assertEquals(0, lockerPage.u16());
            assertEquals(0, lockerPage.u16());
            assertEquals(0, lockerPage.u8());
            host.sendPlain(GamePackets.clientLockerPang());
            PacketReader lockerPang = awaitOpcode(host, GamePackets.SERVER_LOCKER_PANG);
            assertEquals(0, lockerPang.u64());

            guest.sendPlain(GamePackets.clientRefuseWhisper("TestNick"));
            PacketReader refuse = awaitOpcode(host, GamePackets.SERVER_CHAT);
            assertEquals(GamePackets.CHAT_REFUSE_WHISPER, refuse.u8());
            assertEquals("TestNick", refuse.pstr());
            host.sendPlain(GamePackets.clientIdentity(-1, "TestNick"));

            host.sendPlain(GamePackets.clientMyRoom(10001, 10001));
            PacketReader myRoom = awaitOpcode(host, GamePackets.SERVER_MY_ROOM);
            assertEquals(GamePackets.MY_ROOM_DENY, myRoom.u32());
            assertEquals(10001, myRoom.u32());
            host.sendPlain(GamePackets.clientLockerMakePass(""));
            PacketReader makePass = awaitOpcode(host, GamePackets.SERVER_LOCKER_MAKE_PASS);
            assertEquals(GamePackets.LOCKER_MAKE_PASS_EMPTY, makePass.u32());
            host.sendPlain(GamePackets.clientLockerChangePass("", "ab"));
            PacketReader changePass = awaitOpcode(host, GamePackets.SERVER_LOCKER_CHANGE_PASS);
            assertEquals(GamePackets.LOCKER_CHANGE_PASS_WRONG, changePass.u32());
            host.sendPlain(GamePackets.clientLockerMode(1, ""));
            PacketReader lockerMode = awaitOpcode(host, GamePackets.SERVER_LOCKER_MODE);
            assertEquals(GamePackets.shopSys(GamePackets.LOCKER_MODE_EMPTY), lockerMode.u32());
            host.sendPlain(GamePackets.clientLockerCount(GamePackets.CLIENT_LOCKER_ADD, 0));
            PacketReader lockerAdd = awaitOpcode(host, GamePackets.SERVER_LOCKER_ADD);
            assertEquals(GamePackets.shopSys(GamePackets.LOCKER_ADD_ERR_NONE), lockerAdd.u32());
            host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_LOCKER_REMOVE));
            PacketReader lockerRm = awaitOpcode(host, GamePackets.SERVER_LOCKER_REMOVE);
            assertEquals(GamePackets.LOCKER_REMOVE_ERR_DEFAULT, lockerRm.u32());
            host.sendPlain(GamePackets.clientLockerUpdatePang(0, 1));
            PacketReader lockerUp = awaitOpcode(host, GamePackets.SERVER_LOCKER_UPDATE_PANG);
            assertEquals(GamePackets.shopSys(GamePackets.LOCKER_PANG_WITHDRAW_ERR), lockerUp.u32());
            host.sendPlain(GamePackets.clientOpenCardPack(0, 0));
            PacketReader cardPack = awaitOpcode(host, GamePackets.SERVER_OPEN_CARD_PACK);
            assertEquals(GamePackets.CARD_PACK_ERR, cardPack.u32());
            host.sendPlain(GamePackets.clientUseCard(0));
            PacketReader useCard = awaitOpcode(host, GamePackets.SERVER_USE_CARD);
            assertEquals(GamePackets.shopSys(GamePackets.CARD_ERR_TYPEID), useCard.u32());
            host.sendPlain(GamePackets.clientRental(GamePackets.CLIENT_EXTEND_RENTAL, 0));
            PacketReader extend = awaitOpcode(host, GamePackets.SERVER_EXTEND_RENTAL);
            assertEquals(GamePackets.RENTAL_FAIL, extend.u8());
            host.sendPlain(GamePackets.clientRental(GamePackets.CLIENT_DELETE_RENTAL, 0));
            PacketReader deleteRental = awaitOpcode(host, GamePackets.SERVER_DELETE_RENTAL);
            assertEquals(GamePackets.RENTAL_FAIL, deleteRental.u8());
            host.sendPlain(GamePackets.clientClubWorkshopEmpty(
                    GamePackets.CLIENT_WORKSHOP_TRANSFORM_CONFIRM));
            PacketReader xfConfirm = awaitOpcode(host, GamePackets.SERVER_WORKSHOP_TRANSFORM_CONFIRM);
            assertEquals(
                    GamePackets.shopSys(GamePackets.WORKSHOP_TRANSFORM_CONFIRM_ERR), xfConfirm.u32());
            host.sendPlain(GamePackets.clientClubWorkshopEmpty(
                    GamePackets.CLIENT_WORKSHOP_TRANSFORM_CANCEL));
            PacketReader xfCancel = awaitOpcode(host, GamePackets.SERVER_WORKSHOP_TRANSFORM_CANCEL);
            assertEquals(
                    GamePackets.shopSys(GamePackets.WORKSHOP_TRANSFORM_CANCEL_ERR), xfCancel.u32());
            host.sendPlain(GamePackets.clientWorkshopTypeidClub(
                    GamePackets.CLIENT_WORKSHOP_RECOVERY, 0, 0));
            PacketReader recovery = awaitOpcode(host, GamePackets.SERVER_WORKSHOP_RECOVERY);
            assertEquals(GamePackets.shopSys(GamePackets.WORKSHOP_RECOVERY_ERR), recovery.u32());
            host.sendPlain(GamePackets.clientWorkshopTransfer(0, 0, 0, 1));
            PacketReader transfer = awaitOpcode(host, GamePackets.SERVER_WORKSHOP_TRANSFER);
            assertEquals(GamePackets.shopSys(GamePackets.WORKSHOP_TRANSFER_ERR), transfer.u32());
            host.sendPlain(GamePackets.clientWorkshopTypeidClub(
                    GamePackets.CLIENT_CLUBSET_RESET, 0, 0));
            PacketReader reset = awaitOpcode(host, GamePackets.SERVER_CLUBSET_RESET);
            assertEquals(GamePackets.shopSys(GamePackets.CLUBSET_RESET_ERR), reset.u32());
            host.sendPlain(GamePackets.clientMemorial(0));
            PacketReader memorial = awaitOpcode(host, GamePackets.SERVER_MEMORIAL);
            assertEquals(GamePackets.shopSys(GamePackets.MEMORIAL_ERR_COIN), memorial.u32());
            host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_CUTIN));
            host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_UCC_LOAD));

            host.sendPlain(GamePackets.clientUccWebKey(0, 0, 0, 0));
            PacketReader uccKey = awaitOpcode(host, GamePackets.SERVER_UCC_WEB_KEY);
            assertEquals(1, uccKey.u8());
            assertEquals(1, uccKey.u8());
            assertEquals(GamePackets.shopSys(GamePackets.UCC_WEB_KEY_ERR_UID), uccKey.u32());
            host.sendPlain(GamePackets.clientUccOpt(99));
            PacketReader ucc = awaitOpcode(host, GamePackets.SERVER_UCC);
            assertEquals(GamePackets.UCC_FAIL, ucc.u8());
            host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_WORKSHOP_EVENT));
            PacketReader workshopEv = awaitOpcode(host, GamePackets.SERVER_WORKSHOP_EVENT);
            assertEquals(0, workshopEv.i32());
            assertEquals(GamePackets.WORKSHOP_EVENT_HOLES, workshopEv.i32());
            assertEquals(0, workshopEv.i32());
            assertEquals(GamePackets.WORKSHOP_EVENT_BARRA_MAX, workshopEv.u8());
            assertEquals(0, workshopEv.u8());
            assertEquals(GamePackets.WORKSHOP_EVENT_BARRA, workshopEv.u8());
            assertEquals(GamePackets.WORKSHOP_EVENT_BARRA, workshopEv.u8());
            host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_WORKSHOP_EVENT_COUNT));
            PacketReader workshopCount = awaitOpcode(host, GamePackets.SERVER_WORKSHOP_EVENT_COUNT);
            assertEquals(0, workshopCount.i32());
            for (int i = 1; i <= GamePackets.WORKSHOP_EVENT_COUNT_SLOTS; i++) {
                assertEquals(i, workshopCount.u8());
            }
            host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_ATTENDANCE));
            PacketReader attend = awaitOpcode(host, GamePackets.SERVER_ATTENDANCE);
            assertEquals(GamePackets.ATTENDANCE_FAIL, attend.u32());
            host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_ATTENDANCE_LOGIN));
            PacketReader attendLogin = awaitOpcode(host, GamePackets.SERVER_ATTENDANCE_LOGIN);
            assertEquals(GamePackets.ATTENDANCE_FAIL, attendLogin.u32());
            host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_GZ_INITIAL));
            host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_MARKER));
            host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_SHOT_END));
            host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_USE_ITEM));
            host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_REPLAY_ONLINE));
            host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_LEAVE_CHIP_IN));
            host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_GZ_FIRST_HOLE));
            host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_WING));
            host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_EARCUFF));
            host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_GLOVE));
            host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_RING_GROUND));
            host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_TOGGLE_ASSIST));
            host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_ASSIST_GREEN));
            host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_EVENT_ARIN));
            host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_GP_EXIT_ROOM));
            host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_GP_LOBBY));
            PacketReader gpLobby = awaitOpcode(host, GamePackets.SERVER_GP_LOBBY);
            assertEquals(0, gpLobby.u32());
            assertEquals(1, gpLobby.u32());
            assertEquals(1, gpLobby.u32());
            assertEquals(0, gpLobby.u32());
            assertEquals(0f, gpLobby.f32());
            host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_GP_LEAVE));
            PacketReader gpLeave = awaitOpcode(host, GamePackets.SERVER_GP_LEAVE);
            assertEquals(0, gpLeave.u32());
            host.sendPlain(GamePackets.clientGpEnter(0));
            PacketReader gpRoom = awaitOpcode(host, GamePackets.SERVER_START_GAME_FAIL);
            assertEquals(GamePackets.shopSys(GamePackets.GP_ENTER_ERR_IFF), gpRoom.u32());

            host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_ENTER_MY_ROOM));
            PacketReader myRoomChar = awaitOpcode(host, GamePackets.SERVER_MY_ROOM_CHAR);
            assertEquals(GamePackets.PLAYER_ROOM_INFO_EX_BYTES, myRoomChar.remaining());
            PacketReader myRoomPosters = awaitOpcode(host, GamePackets.SERVER_MY_ROOM_POSTERS);
            assertEquals(GamePackets.MY_ROOM_POSTERS_OPTION, myRoomPosters.u32());
            assertEquals(0, myRoomPosters.u16());

            host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_BIG_PAPEL));
            PacketReader bigPapel = awaitOpcode(host, GamePackets.SERVER_BIG_PAPEL);
            assertEquals(0, bigPapel.u32());
            assertEquals(0, bigPapel.i32());
            assertEquals(GamePackets.PAPEL_BIG_BALLS, bigPapel.u32());

            host.sendPlain(GamePackets.clientCharMastery(0, 0));
            PacketReader mastery = awaitOpcode(host, GamePackets.SERVER_CHAR_MASTERY);
            assertEquals(GamePackets.shopSys(GamePackets.CHAR_MASTERY_ERR_CHAR), mastery.u32());
            host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_CHAR_MASTERY));
            PacketReader masteryTrunc = awaitOpcode(host, GamePackets.SERVER_CHAR_MASTERY);
            assertEquals(GamePackets.CHAR_MASTERY_ERR_DEFAULT, masteryTrunc.u32());
            inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
            var stockMastery = inv.buyShopItem(
                    10001, GamePackets.TYPEID_SHOP_PANG_ITEM, 1, GamePackets.SHOP_PANG_PRICE, 0);
            assertEquals(0, stockMastery.code());
            var nuri = inv.characters(10001).getFirst();
            int beforeMastery = GamePackets.unixNow();
            host.sendPlain(GamePackets.clientCharMastery(nuri.typeid, nuri.id));
            PacketReader masteryAwards = awaitOpcode(host, GamePackets.SERVER_DAILY_QUEST_STAMP);
            int masteryUnix = masteryAwards.u32();
            assertTrue(masteryUnix >= beforeMastery - 1 && masteryUnix <= GamePackets.unixNow() + 1);
            assertEquals(2, masteryAwards.u32());
            assertEquals(GamePackets.PAPEL_AWARD_TYPE, masteryAwards.u8());
            assertEquals(GamePackets.TYPEID_SHOP_PANG_ITEM, masteryAwards.u32());
            masteryAwards.i32();
            masteryAwards.u32();
            masteryAwards.i32();
            masteryAwards.i32();
            assertEquals(-1, masteryAwards.i32());
            masteryAwards.readBytes(GamePackets.PAPEL_AWARD_PAD);
            assertEquals(GamePackets.CHAR_MASTERY_AWARD_TYPE, masteryAwards.u8());
            assertEquals(nuri.typeid, masteryAwards.u32());
            assertEquals(nuri.id, masteryAwards.i32());
            masteryAwards.u32();
            masteryAwards.i32();
            masteryAwards.i32();
            masteryAwards.i32();
            masteryAwards.readBytes(GamePackets.PAPEL_AWARD_PAD);
            assertEquals(1, masteryAwards.u32());
            PacketReader masteryOk = awaitOpcode(host, GamePackets.SERVER_CHAR_MASTERY);
            assertEquals(0, masteryOk.u32());
            nuri.mastery = 0;
            inv.updateCharacterParts(10001, nuri);
            inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
            inv.setPangCookie(10001, pang, cookie);

            host.sendPlain(GamePackets.clientCharStats(GamePackets.CLIENT_CHAR_STATS_UP, 0));
            PacketReader statsUp = awaitOpcode(host, GamePackets.SERVER_CHAR_STATS_UP);
            assertEquals(GamePackets.shopSys(GamePackets.CHAR_STATS_UP_ERR_CHAR), statsUp.u32());
            host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_CHAR_STATS_UP));
            PacketReader statsUpTrunc = awaitOpcode(host, GamePackets.SERVER_CHAR_STATS_UP);
            assertEquals(GamePackets.CHAR_STATS_UP_ERR_DEFAULT, statsUpTrunc.u32());
            var statsNuri = inv.characters(10001).getFirst();
            int beforeStats = GamePackets.unixNow();
            host.sendPlain(GamePackets.clientCharStats(
                    GamePackets.CLIENT_CHAR_STATS_UP, 0, statsNuri));
            PacketReader statsSpent = awaitOpcode(host, GamePackets.SERVER_PANG_SPENT);
            assertEquals(pang - GamePackets.CHAR_STATS_ENCHANT_PANG, statsSpent.u64());
            assertEquals(GamePackets.CHAR_STATS_ENCHANT_PANG, statsSpent.u64());
            PacketReader statsAwards = awaitOpcode(host, GamePackets.SERVER_DAILY_QUEST_STAMP);
            int statsUnix = statsAwards.u32();
            assertTrue(statsUnix >= beforeStats - 1 && statsUnix <= GamePackets.unixNow() + 1);
            assertEquals(1, statsAwards.u32());
            assertEquals(GamePackets.CHAR_STATS_AWARD_TYPE, statsAwards.u8());
            assertEquals(statsNuri.typeid, statsAwards.u32());
            assertEquals(statsNuri.id, statsAwards.i32());
            statsAwards.u32();
            statsAwards.u32();
            statsAwards.u32();
            statsAwards.u32();
            assertEquals(1, statsAwards.u16());
            assertEquals(0, statsAwards.u16());
            assertEquals(0, statsAwards.u16());
            assertEquals(0, statsAwards.u16());
            assertEquals(0, statsAwards.u16());
            statsAwards.readBytes(GamePackets.CHAR_STATS_PCL_PAD);
            PacketReader statsOk = awaitOpcode(host, GamePackets.SERVER_CHAR_STATS_UP);
            assertEquals(0, statsOk.u32());
            assertEquals(0, statsOk.u32());

            host.sendPlain(GamePackets.clientCharStats(GamePackets.CLIENT_CHAR_STATS_DOWN, 0));
            PacketReader statsDown = awaitOpcode(host, GamePackets.SERVER_CHAR_STATS_DOWN);
            assertEquals(GamePackets.shopSys(GamePackets.CHAR_STATS_DOWN_ERR_CHAR), statsDown.u32());
            host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_CHAR_STATS_DOWN));
            PacketReader statsDownTrunc = awaitOpcode(host, GamePackets.SERVER_CHAR_STATS_DOWN);
            assertEquals(GamePackets.CHAR_STATS_DOWN_ERR_DEFAULT, statsDownTrunc.u32());
            host.sendPlain(GamePackets.clientCharStats(
                    GamePackets.CLIENT_CHAR_STATS_DOWN, 0, statsNuri));
            PacketReader statsDownAwards = awaitOpcode(host, GamePackets.SERVER_DAILY_QUEST_STAMP);
            assertTrue(statsDownAwards.u32() > 0);
            assertEquals(1, statsDownAwards.u32());
            assertEquals(GamePackets.CHAR_STATS_AWARD_TYPE, statsDownAwards.u8());
            PacketReader statsDownOk = awaitOpcode(host, GamePackets.SERVER_CHAR_STATS_DOWN);
            assertEquals(0, statsDownOk.u32());
            assertEquals(0, statsDownOk.u32());
            inv.setPangCookie(10001, pang, cookie);
            assertEquals(0, inv.characters(10001).getFirst().pcl[0] & 0xff);

            host.sendPlain(GamePackets.clientCardEquip(GamePackets.CLIENT_CHAR_CARD_EQUIP));
            PacketReader cardEquip = awaitOpcode(host, GamePackets.SERVER_CHAR_CARD_EQUIP);
            assertEquals(GamePackets.shopSys(GamePackets.CHAR_CARD_ERR_IFF), cardEquip.u32());
            host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_CHAR_CARD_EQUIP));
            PacketReader cardEquipTrunc = awaitOpcode(host, GamePackets.SERVER_CHAR_CARD_EQUIP);
            assertEquals(GamePackets.CHAR_CARD_ERR_DEFAULT, cardEquipTrunc.u32());
            var cardNuri = inv.characters(10001).getFirst();
            int cardId = inv.addCard(10001, GamePackets.TYPEID_CARD_NORMAL, 1);
            int beforeCard = GamePackets.unixNow();
            host.sendPlain(GamePackets.clientCardEquip(
                    GamePackets.CLIENT_CHAR_CARD_EQUIP, cardNuri.typeid, cardNuri.id,
                    GamePackets.TYPEID_CARD_NORMAL, cardId, GamePackets.CHAR_CARD_SLOT));
            PacketReader cardAwards = awaitOpcode(host, GamePackets.SERVER_DAILY_QUEST_STAMP);
            int cardUnix = cardAwards.u32();
            assertTrue(cardUnix >= beforeCard - 1 && cardUnix <= GamePackets.unixNow() + 1);
            assertEquals(2, cardAwards.u32());
            PacketReader cardEqOk = awaitOpcode(host, GamePackets.SERVER_CHAR_CARD_EQUIP);
            assertEquals(0, cardEqOk.u32());
            assertEquals(GamePackets.TYPEID_CARD_NORMAL, cardEqOk.u32());

            host.sendPlain(GamePackets.clientCardEquip(GamePackets.CLIENT_CHAR_CARD_PATCHER));
            PacketReader patcher = awaitOpcode(host, GamePackets.SERVER_CHAR_CARD_PATCHER);
            assertEquals(GamePackets.shopSys(GamePackets.CHAR_CARD_PATCHER_ERR), patcher.u32());
            host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_CHAR_CARD_PATCHER));
            PacketReader patcherTrunc = awaitOpcode(host, GamePackets.SERVER_CHAR_CARD_PATCHER);
            assertEquals(GamePackets.CHAR_CARD_PATCHER_DEFAULT, patcherTrunc.u32());
            inv.addWarehouseItem(10001, GamePackets.TYPEID_CLUB_PATCHER, 1);
            int patcherCardId = inv.addCard(10001, GamePackets.TYPEID_CARD_NORMAL, 1);
            host.sendPlain(GamePackets.clientCardEquip(
                    GamePackets.CLIENT_CHAR_CARD_PATCHER, cardNuri.typeid, cardNuri.id,
                    GamePackets.TYPEID_CARD_NORMAL, patcherCardId, GamePackets.CHAR_CARD_PATCHER_SLOT));
            PacketReader patcherAwards = awaitOpcode(host, GamePackets.SERVER_DAILY_QUEST_STAMP);
            assertTrue(patcherAwards.u32() > 0);
            assertEquals(3, patcherAwards.u32());
            PacketReader patcherOk = awaitOpcode(host, GamePackets.SERVER_CHAR_CARD_PATCHER);
            assertEquals(0, patcherOk.u32());
            assertEquals(GamePackets.TYPEID_CARD_NORMAL, patcherOk.u32());

            host.sendPlain(GamePackets.clientCardEquip(GamePackets.CLIENT_CHAR_CARD_REMOVE));
            PacketReader cardRemove = awaitOpcode(host, GamePackets.SERVER_CHAR_CARD_REMOVE);
            assertEquals(GamePackets.shopSys(GamePackets.CHAR_CARD_REMOVE_ERR_CHAR), cardRemove.u32());
            host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_CHAR_CARD_REMOVE));
            PacketReader cardRemoveTrunc = awaitOpcode(host, GamePackets.SERVER_CHAR_CARD_REMOVE);
            assertEquals(GamePackets.CHAR_CARD_REMOVE_DEFAULT, cardRemoveTrunc.u32());
            inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
            var cardRemover = inv.buyShopItem(
                    10001, GamePackets.TYPEID_SHOP_PANG_ITEM, 2, GamePackets.SHOP_PANG_PRICE * 2, 0);
            assertEquals(0, cardRemover.code());
            host.sendPlain(GamePackets.clientCardEquip(
                    GamePackets.CLIENT_CHAR_CARD_REMOVE, cardNuri.typeid, cardNuri.id,
                    GamePackets.TYPEID_SHOP_PANG_ITEM, cardRemover.itemId(), GamePackets.CHAR_CARD_SLOT));
            PacketReader cardRmAwards = awaitOpcode(host, GamePackets.SERVER_DAILY_QUEST_STAMP);
            assertTrue(cardRmAwards.u32() > 0);
            assertEquals(3, cardRmAwards.u32());
            PacketReader cardRmOk = awaitOpcode(host, GamePackets.SERVER_CHAR_CARD_REMOVE);
            assertEquals(0, cardRmOk.u32());
            assertEquals(GamePackets.TYPEID_CARD_NORMAL, cardRmOk.u32());
            host.sendPlain(GamePackets.clientCardEquip(
                    GamePackets.CLIENT_CHAR_CARD_REMOVE, cardNuri.typeid, cardNuri.id,
                    GamePackets.TYPEID_SHOP_PANG_ITEM, cardRemover.itemId(), GamePackets.CHAR_CARD_PATCHER_SLOT));
            PacketReader patcherRmAwards = awaitOpcode(host, GamePackets.SERVER_DAILY_QUEST_STAMP);
            assertTrue(patcherRmAwards.u32() > 0);
            assertEquals(3, patcherRmAwards.u32());
            PacketReader patcherRmOk = awaitOpcode(host, GamePackets.SERVER_CHAR_CARD_REMOVE);
            assertEquals(0, patcherRmOk.u32());
            assertEquals(GamePackets.TYPEID_CARD_NORMAL, patcherRmOk.u32());
            inv.deleteCardByTypeid(10001, GamePackets.TYPEID_CARD_NORMAL);
            inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
            inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_CLUB_PATCHER);
            inv.setPangCookie(10001, pang, cookie);

            host.sendPlain(GamePackets.clientTikiShopCount(0));
            PacketReader tikiCount = awaitOpcode(host, GamePackets.SERVER_TIKI_SHOP_EXCHANGE);
            assertEquals(GamePackets.shopSys(GamePackets.TIKI_SHOP_EXCHANGE_ERR_COUNT), tikiCount.u32());
            host.sendPlain(GamePackets.clientTikiShopCount(6));
            PacketReader tikiOver = awaitOpcode(host, GamePackets.SERVER_TIKI_SHOP_EXCHANGE);
            assertEquals(GamePackets.shopSys(GamePackets.TIKI_SHOP_EXCHANGE_ERR_COUNT), tikiOver.u32());
            host.sendPlain(GamePackets.clientTikiShopCount(1));
            PacketReader tikiTrunc = awaitOpcode(host, GamePackets.SERVER_TIKI_SHOP_EXCHANGE);
            assertEquals(GamePackets.shopSys(GamePackets.TIKI_SHOP_EXCHANGE_ERR_TRUNCATED), tikiTrunc.u32());
            host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_TIKI_SHOP_EXCHANGE));
            PacketReader tikiDefault = awaitOpcode(host, GamePackets.SERVER_TIKI_SHOP_EXCHANGE);
            assertEquals(GamePackets.TIKI_SHOP_EXCHANGE_ERR_DEFAULT, tikiDefault.u32());

            host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_RING_PAWS_RAINBOW));
            host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_RING_POWER));
            host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_RING_MIRACLE));
            host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_RING_PAWS_SET));
            host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_FINISH_GAME_CB));
            host.sendPlain(GamePackets.clientEmpty(GamePackets.CLIENT_FINISH_GAME_12C));

            host.sendPlain(GamePackets.clientDeleteItem(1, 1));
            PacketReader deleted = awaitOpcode(host, GamePackets.SERVER_DELETE_ITEM);
            assertEquals(GamePackets.DELETE_ITEM_FAIL, deleted.u8());

            host.sendPlain(GamePackets.clientAchievementEmpty());
            PacketReader ach = awaitOpcode(host, GamePackets.SERVER_ACHIEVEMENT_GUI);
            assertEquals(GamePackets.ACHIEVEMENT_GUI_FAIL, ach.i32());

            host.sendPlain(GamePackets.clientAchievement(10001));
            host.sendPlain(GamePackets.clientGameGuard());
            host.sendPlain(GamePackets.clientWindNextHole());
            host.sendPlain(GamePackets.clientCaddieHolidayNotice(0, 1));
            host.sendPlain(GamePackets.clientInviteRelog(1, 0));
            host.sendPlain(GamePackets.clientRequestCash());
            PacketReader cookiePkt = new PacketReader(host.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(GamePackets.SERVER_COOKIE, cookiePkt.opcode());

            host.sendPlain(GamePackets.clientEnterOtherChannel(1));
            PacketReader switched = awaitOpcode(host, GamePackets.SERVER_CHANNEL_ENTER_ACK);
            assertEquals(GamePackets.CHANNEL_ENTER_OK, switched.u8());

            host.sendPlain(GamePackets.clientEnterOtherChannel(99));
            PacketReader missing = awaitOpcode(host, GamePackets.SERVER_CHANNEL_ENTER_ACK);
            assertEquals(GamePackets.CHANNEL_NOT_FOUND, missing.u8());
            assertTrue(awaitSessionCount(runtime, 1, 5, TimeUnit.SECONDS));
            inv.setPangCookie(10001, pang, cookie);
            inv.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
        }
    }

    private static void loginTwoPlayers(
            javax.sql.DataSource ds,
            SessionKeyStore keys,
            PangyaFakeClient host,
            PangyaFakeClient guest,
            int port) throws Exception {
        LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(ds));
        String hostLogin = repo.generateAuthKeyLogin(10001);
        String hostGame = repo.generateAuthKeyGame(10001, 20202);
        String guestLogin = repo.generateAuthKeyLogin(10002);
        String guestGame = repo.generateAuthKeyGame(10002, 20202);
        keys.putLoginKey(10001, hostLogin);
        keys.putGameKey(10001, 20202, hostGame);
        keys.putLoginKey(10002, guestLogin);
        keys.putGameKey(10002, 20202, guestGame);
        loginToChannel(host, port, "testuser", 10001, hostLogin, hostGame);
        loginToChannel(guest, port, "testuser2", 10002, guestLogin, guestGame);
    }

    private static void loginToChannel(
            PangyaFakeClient client, int port, String id, int uid, String loginKey, String gameKey)
            throws Exception {
        client.connect("127.0.0.1", port, PangyaFakeClient.HelloKind.GAME);
        client.awaitHello(5, TimeUnit.SECONDS);
        client.sendPlain(GamePackets.clientLogin(
                id, uid, loginKey, GamePackets.JP_CLIENT_VERSION, GamePackets.xorPacketVersion(GamePackets.JP_PACKET_VERSION), gameKey));
        List<byte[]> loginPkts = collect(client, GamePackets.LOGIN_DUMP_PACKET_COUNT, 8, TimeUnit.SECONDS);
        assertEquals(GamePackets.LOGIN_DUMP_PACKET_COUNT, loginPkts.size());
        client.sendPlain(GamePackets.clientEnterChannel(0));
        PacketReader entered = awaitOpcode(client, GamePackets.SERVER_CHANNEL_ENTER_ACK);
        assertEquals(GamePackets.CHANNEL_ENTER_OK, entered.u8());
    }

    @Test
    void badGameKeySendsSecurityAck() throws Exception {
        String jdbc = env("PANGYA_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/pangya");
        String user = env("PANGYA_TEST_JDBC_USER", "pangya");
        String password = env("PANGYA_TEST_JDBC_PASSWORD", "pangya");
        String redisUri = env("REDIS_URI", "redis://localhost:6379");
        DatabaseSupport.migrate(jdbc, user, password);

        AppConfig config = new AppConfig(testYaml(jdbc, user, password, redisUri));
        try (var ds = DatabaseSupport.dataSource(jdbc, user, password);
             SessionKeyStore keys = new SessionKeyStore(redisUri);
             GameRuntime runtime = new GameRuntime(config);
             PangyaFakeClient client = new PangyaFakeClient()) {
            LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(ds));
            String loginKey = repo.generateAuthKeyLogin(10001);
            String gameKey = repo.generateAuthKeyGame(10001, 20202);
            keys.putLoginKey(10001, loginKey);
            keys.putGameKey(10001, 20202, gameKey);

            client.connect("127.0.0.1", runtime.port(), PangyaFakeClient.HelloKind.GAME);
            client.awaitHello(5, TimeUnit.SECONDS);
            client.sendPlain(GamePackets.clientLogin(
                    "testuser",
                    10001,
                    loginKey,
                    GamePackets.JP_CLIENT_VERSION,
                    GamePackets.xorPacketVersion(GamePackets.JP_PACKET_VERSION),
                    "DEADBEEF"));
            PacketReader r = new PacketReader(client.awaitPlain(5, TimeUnit.SECONDS));
            assertEquals(GamePackets.SERVER_LOGIN_ACK, r.opcode());
            assertEquals(GamePackets.ACK_SECURITY_KEY, r.u32());
            assertFalse(runtime.sessions().snapshot().stream().anyMatch(s -> s.authorized()));
        }
    }

    private static String nameFromRoomInfo(byte[] info) {
        int end = 0;
        while (end < 40 && info[end] != 0) {
            end++;
        }
        return new String(info, 0, end, org.pangya.protocol.packet.PacketIo.SHIFT_JIS);
    }

    private static int roomNumberFromInfo(byte[] info) {
        return org.pangya.protocol.packet.PacketIo.readU16le(info, 89);
    }

    private static boolean awaitSessionCount(GameRuntime runtime, int expected, long timeout, TimeUnit unit)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
        while (System.currentTimeMillis() < deadline) {
            if (runtime.sessions().size() == expected) {
                return true;
            }
            Thread.sleep(50);
        }
        return runtime.sessions().size() == expected;
    }

    private static PacketReader awaitOpcode(PangyaFakeClient client, int opcode) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(8);
        while (System.currentTimeMillis() < deadline) {
            long left = Math.max(1, deadline - System.currentTimeMillis());
            PacketReader r = new PacketReader(client.awaitPlain(left, TimeUnit.MILLISECONDS));
            if (r.opcode() == opcode) {
                return r;
            }
        }
        throw new IllegalStateException("missing opcode 0x" + Integer.toHexString(opcode));
    }

    /**
     * {@code 0x48} option after join can still be queued; skip until the
     * requested option (2 = leave).
     */
    private static PacketReader awaitRoomPlayers(PangyaFakeClient client, int option)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(8);
        while (System.currentTimeMillis() < deadline) {
            PacketReader r = awaitOpcode(client, GamePackets.SERVER_ROOM_PLAYERS);
            if (r.u8() == option) {
                return r;
            }
        }
        throw new IllegalStateException("missing room players option " + option);
    }

    private static List<byte[]> collect(PangyaFakeClient client, int n, long timeout, TimeUnit unit)
            throws InterruptedException {
        List<byte[]> out = new ArrayList<>();
        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
        while (out.size() < n && System.currentTimeMillis() < deadline) {
            long left = Math.max(1, deadline - System.currentTimeMillis());
            try {
                out.add(client.awaitPlain(left, TimeUnit.MILLISECONDS));
            } catch (IllegalStateException e) {
                break;
            }
        }
        return out;
    }

    private static Map<String, Object> testYaml(String jdbc, String user, String password, String redis) {
        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> server = new LinkedHashMap<>();
        server.put("name", "game-test");
        server.put("uid", 20202);
        server.put("tipo", 1);
        server.put("port", 0);
        server.put("healthPort", freePort());
        server.put("ip", "127.0.0.1");
        server.put("maxUser", 2001);
        server.put("property", 2048);
        server.put("rateGrandPrixEvent", 1);
        server.put("version", "Release.JP.983.00");
        server.put("clientVersion", GamePackets.JP_CLIENT_VERSION);
        server.put("packetVersion", GamePackets.JP_PACKET_VERSION);
        root.put("server", server);
        root.put("database", Map.of("url", jdbc, "user", user, "password", password));
        root.put("redis", Map.of("uri", redis));
        root.put("auth", Map.of("enabled", false, "host", "127.0.0.1", "port", 1));
        root.put("channels", List.of(
                Map.of("name", "Channel (Rookies)", "maxUser", 500, "flag", 0),
                Map.of("name", "Channel (Geral)", "maxUser", 500, "flag", 0)
        ));
        return root;
    }

    private static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String skipChatNotice(PacketReader chat) {
        assertEquals(GamePackets.CHAT_NOTICE, chat.u8());
        chat.pstr();
        return chat.pstr();
    }

    private static int lobbyCapability(PacketReader update) {
        assertEquals(GamePackets.LOBBY_USER_UPDATE, update.u8());
        update.u8();
        update.u32();
        update.i32();
        update.u16();
        update.readBytes(22);
        update.u8();
        return update.i32();
    }

    private static void assertShotEnd(PacketReader r, int oid, int hole, byte[] body) {
        assertEquals(oid, r.i32());
        assertEquals(hole, r.u8());
        assertArrayEquals(body, r.readBytes(GamePackets.SHOT_END_LOCATION_BYTES));
        assertEquals(0, r.remaining());
    }

    private static void setItemSlot1(javax.sql.DataSource ds, long uid, int typeid) {
        DatabaseSupport.jdbi(ds).useHandle(h -> h.createUpdate("""
                        UPDATE pangya.pangya_user_equip
                           SET item_slot_1 = :t
                         WHERE "UID" = :uid
                        """)
                .bind("t", typeid)
                .bind("uid", uid)
                .execute());
    }

    private static int readCardPackRow(
            PacketReader reader, int typeid, int qntdDep, int tail) {
        int id = reader.i32();
        assertEquals(typeid, reader.u32());
        reader.readBytes(12);
        assertEquals(qntdDep, reader.i32());
        reader.readBytes(32);
        assertEquals(1, reader.u16());
        int subgroup = GamePackets.itemSubGroupIdentify22(typeid);
        if (subgroup == 3 || subgroup == 4) {
            assertEquals(tail, reader.u8());
        } else {
            assertEquals(tail, reader.u32());
        }
        return id;
    }

    private static int insertDailyAchievement(javax.sql.DataSource ds) {
        return DatabaseSupport.jdbi(ds).inTransaction(h -> {
            int id = h.createQuery("""
                            INSERT INTO pangya.pangya_achievement (
                                "UID", "Nome", "TypeID", active, status)
                            VALUES (10001, 'Daily test', :typeid, 1, 2)
                            RETURNING "ID_ACHIEVEMENT"
                            """)
                    .bind("typeid", GamePackets.TYPEID_DAILY_ACHIEVEMENT_TEST)
                    .mapTo(Integer.class)
                    .one();
            h.createUpdate("""
                            INSERT INTO pangya.pangya_quest (
                                achievement_id, uid, "name", typeid, counter_item_id, "Date")
                            VALUES (:achievement, 10001, 'Daily stuff', :typeid, 0, NULL)
                            """)
                    .bind("achievement", id)
                    .bind("typeid", GamePackets.TYPEID_DAILY_QUEST_STUFF_TEST)
                    .execute();
            return id;
        });
    }

    private static void cleanupDailyQuest(javax.sql.DataSource ds) {
        DatabaseSupport.jdbi(ds).useTransaction(h -> {
            List<Integer> ids = h.createQuery("""
                            SELECT "ID_ACHIEVEMENT" FROM pangya.pangya_achievement
                             WHERE "UID" = 10001 AND "TypeID" = :typeid
                            """)
                    .bind("typeid", GamePackets.TYPEID_DAILY_ACHIEVEMENT_TEST)
                    .mapTo(Integer.class)
                    .list();
            if (!ids.isEmpty()) {
                h.createUpdate("""
                                DELETE FROM pangya.pangya_quest
                                 WHERE uid = 10001 AND achievement_id IN (<ids>)
                                """)
                        .bindList("ids", ids)
                        .execute();
                h.createUpdate("""
                                DELETE FROM pangya.pangya_achievement
                                 WHERE "UID" = 10001 AND "ID_ACHIEVEMENT" IN (<ids>)
                                """)
                        .bindList("ids", ids)
                        .execute();
            }
            h.createUpdate("""
                            DELETE FROM pangya.pangya_counter_item
                             WHERE "UID" = 10001 AND "TypeID" = :typeid
                            """)
                    .bind("typeid", GamePackets.TYPEID_DAILY_COUNTER_TEST)
                    .execute();
            h.createUpdate("DELETE FROM pangya.pangya_daily_quest_player WHERE uid = 10001")
                    .execute();
        });
    }

    private static int oidOf(GameRuntime runtime, long uid) {
        for (var session : runtime.sessions().snapshot()) {
            if (session.player().uid == uid) {
                return session.oid();
            }
        }
        throw new IllegalStateException("missing uid " + uid);
    }

    private static String env(String name, String fallback) {
        String v = System.getenv(name);
        return (v == null || v.isBlank()) ? fallback : v;
    }
}
