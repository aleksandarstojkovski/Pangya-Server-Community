package org.pangya.game;

import org.junit.jupiter.api.Test;
import org.pangya.db.DatabaseSupport;
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

                PacketReader ack = new PacketReader(loginPkts.get(0));
                assertEquals(GamePackets.SERVER_LOGIN_ACK, ack.opcode());
                assertEquals(GamePackets.ACK_LOGIN_OK, ack.u8());
                assertEquals(GamePackets.PRINCIPAL_PAYLOAD_BYTES, ack.remaining());

                PacketReader chars = new PacketReader(loginPkts.get(1));
                assertEquals(0x70, chars.opcode());
                assertEquals(1, chars.i16());
                assertEquals(1, chars.i16());
                assertEquals(GamePackets.CHARACTER_INFO_BYTES, chars.remaining());

                assertEquals(0x71, new PacketReader(loginPkts.get(2)).opcode());
                PacketReader warehouse = new PacketReader(loginPkts.get(3));
                assertEquals(0x73, warehouse.opcode());
                assertEquals(2, warehouse.u16());
                assertEquals(2, warehouse.u16());
                assertEquals(2 * GamePackets.WAREHOUSE_ITEM_BYTES, warehouse.remaining());

                assertEquals(0xE1, new PacketReader(loginPkts.get(4)).opcode());
                PacketReader equip = new PacketReader(loginPkts.get(5));
                assertEquals(0x72, equip.opcode());
                assertEquals(GamePackets.USER_EQUIP_BYTES, equip.remaining());

                PacketReader channels = new PacketReader(loginPkts.get(6));
                assertEquals(GamePackets.SERVER_CHANNEL_LIST, channels.opcode());
                assertEquals(2, channels.u8());
                assertEquals(2 * 77, channels.remaining());

                client.sendPlain(GamePackets.clientEnterChannel(0));
                PacketReader entered = new PacketReader(client.awaitPlain(5, TimeUnit.SECONDS));
                assertEquals(GamePackets.SERVER_CHANNEL_ENTER_ACK, entered.opcode());
                assertEquals(GamePackets.CHANNEL_ENTER_OK, entered.u8());

                client.sendPlain(GamePackets.clientCreatePractice("practice", "secret"));
                PacketReader room = awaitOpcode(client, GamePackets.SERVER_ROOM_ENTER_RESULT);
                assertEquals(0, room.i16());
                assertEquals(GamePackets.ROOM_INFO_BYTES, room.remaining());
                byte[] practiceInfo = room.readBytes(GamePackets.ROOM_INFO_BYTES);
                assertEquals("Single Player Practice Mode", nameFromRoomInfo(practiceInfo));
                int practiceNum = roomNumberFromInfo(practiceInfo);
                assertTrue(practiceNum >= 1);

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
                assertEquals(GamePackets.SERVER_LOGIN_ACK, new PacketReader(again.get(0)).opcode());
                assertEquals(0x70, new PacketReader(again.get(1)).opcode());
                assertEquals(GamePackets.SERVER_CHANNEL_LIST, new PacketReader(again.get(6)).opcode());
                assertEquals(0xF1, new PacketReader(again.get(11)).opcode());
                assertEquals(0x25D, new PacketReader(again.get(26)).opcode());
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
            inventory.deleteWarehouseByTypeid(10001, GamePackets.TYPEID_SHOP_PANG_ITEM);
            inventory.setPangCookie(10001, 100000, 0);
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

    private static String env(String name, String fallback) {
        String v = System.getenv(name);
        return (v == null || v.isBlank()) ? fallback : v;
    }
}
