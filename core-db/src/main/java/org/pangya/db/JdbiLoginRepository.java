package org.pangya.db;

import org.jdbi.v3.core.Jdbi;
import org.pangya.protocol.game.GamePackets;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public final class JdbiLoginRepository implements LoginRepository {

    private static final char[] KEY_ALPHABET = "0123456789ABCDEF".toCharArray();
    private static final SecureRandom RNG = new SecureRandom();
    private static final String PLAYER_INFO_SELECT = """
            SELECT a."UID", a."ID", a."NICK", a."PASSWORD", a.capability,
                   COALESCE(b."level", 0) AS level, a."IDState",
                   CASE
                     WHEN a."BlockRegDate" IS NULL THEN -1
                     ELSE FLOOR(EXTRACT(EPOCH FROM (
                       a."BlockRegDate" + (a."BlockTime" * INTERVAL '1 minute') - NOW()
                     )))::int
                   END AS block_time
            FROM pangya.account a
            LEFT JOIN pangya.user_info b ON a."UID" = b."UID"
            """;

    private final Jdbi jdbi;

    public JdbiLoginRepository(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    @Override
    public Optional<Long> verifyId(String id) {
        return jdbi.withHandle(h -> h.createQuery(
                        "SELECT \"UID\" FROM pangya.account WHERE \"ID\" = :id LIMIT 1")
                .bind("id", id)
                .mapTo(Long.class)
                .findOne());
    }

    @Override
    public boolean verifyPass(long uid, String password) {
        return jdbi.withHandle(h -> h.createQuery(
                        "SELECT 1 FROM pangya.account WHERE \"UID\" = :uid AND \"PASSWORD\" = :pass LIMIT 1")
                .bind("uid", uid)
                .bind("pass", password)
                .mapTo(Integer.class)
                .findOne()
                .isPresent());
    }

    @Override
    public Optional<PlayerLoginInfo> playerInfo(long uid) {
        return loadPlayerInfo(PLAYER_INFO_SELECT + " WHERE a.\"UID\" = :key", uid);
    }

    @Override
    public Optional<PlayerLoginInfo> playerInfoByNick(String nick) {
        if (nick == null || nick.isBlank()) {
            return Optional.empty();
        }
        return loadPlayerInfo(PLAYER_INFO_SELECT + " WHERE a.\"NICK\" = :key", nick);
    }

    private Optional<PlayerLoginInfo> loadPlayerInfo(String sql, Object key) {
        return jdbi.withHandle(h -> h.createQuery(sql)
                .bind("key", key)
                .map((rs, ctx) -> new PlayerLoginInfo(
                        rs.getLong("UID"),
                        rs.getString("ID"),
                        rs.getString("NICK"),
                        rs.getString("PASSWORD"),
                        rs.getInt("capability"),
                        rs.getInt("level"),
                        rs.getLong("IDState"),
                        rs.getInt("block_time")))
                .findOne());
    }

    @Override
    public boolean isBannedIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return true;
        }
        List<IpBan> bans = jdbi.withHandle(h -> h.createQuery(
                        "SELECT ip, mask FROM pangya.pangya_ip_table")
                .map((rs, ctx) -> new IpBan(rs.getString("ip"), rs.getString("mask")))
                .list());
        long addr = ipv4(ip);
        if (addr < 0) {
            return bans.stream().anyMatch(b -> ip.equalsIgnoreCase(b.ip));
        }
        for (IpBan ban : bans) {
            long banIp = ipv4(ban.ip);
            long mask = ipv4(ban.mask);
            if (banIp < 0) {
                continue;
            }
            if (mask < 0) {
                mask = 0xffff_ffffL;
            }
            if ((addr & mask) == (banIp & mask)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isBannedMac(String mac) {
        if (mac == null || mac.isBlank()) {
            return false;
        }
        return jdbi.withHandle(h -> h.createQuery(
                        "SELECT 1 FROM pangya.pangya_mac_table WHERE lower(mac) = lower(:mac) LIMIT 1")
                .bind("mac", mac)
                .mapTo(Integer.class)
                .findOne()
                .isPresent());
    }

    @Override
    public boolean isFirstLoginDone(long uid) {
        return jdbi.withHandle(h -> h.createQuery(
                        "SELECT COALESCE(\"FIRST_LOGIN\", 0) FROM pangya.account WHERE \"UID\" = :uid")
                .bind("uid", uid)
                .mapTo(Integer.class)
                .findOne()
                .orElse(0) != 0);
    }

    @Override
    public boolean isFirstSetDone(long uid) {
        return jdbi.withHandle(h -> h.createQuery(
                        "SELECT COALESCE(\"FIRST_SET\", 0) FROM pangya.account WHERE \"UID\" = :uid")
                .bind("uid", uid)
                .mapTo(Integer.class)
                .findOne()
                .orElse(0) != 0);
    }

    @Override
    public boolean isLogon(long uid) {
        return jdbi.withHandle(h -> h.createQuery(
                        "SELECT COALESCE(\"Logon\", 0) FROM pangya.account WHERE \"UID\" = :uid")
                .bind("uid", uid)
                .mapTo(Integer.class)
                .findOne()
                .orElse(0) != 0);
    }

    @Override
    public void registerPlayerLogin(long uid, String ip, int serverUid) {
        jdbi.useHandle(h -> h.createUpdate("""
                        UPDATE pangya.account
                           SET "LastLogonTime" = NOW(),
                               "UserIp" = :ip,
                               "ServerID" = :sid,
                               "LogonCount" = "LogonCount" + 1
                         WHERE "UID" = :uid
                        """)
                .bind("ip", ip)
                .bind("sid", Integer.toString(serverUid))
                .bind("uid", uid)
                .execute());
    }

    @Override
    public void registerLogonServer(long uid, int gameServerUid) {
        jdbi.useHandle(h -> h.createUpdate(
                        "UPDATE pangya.account SET game_server_id = :gs WHERE \"UID\" = :uid")
                .bind("gs", Integer.toString(gameServerUid))
                .bind("uid", uid)
                .execute());
    }

    @Override
    public String generateAuthKeyLogin(long uid) {
        String key = randomKey(8);
        jdbi.useHandle(h -> {
            int updated = h.createUpdate(
                            "UPDATE pangya.authkey_login SET \"AuthKey\" = :k, valid = 1 WHERE \"UID\" = :uid")
                    .bind("k", key)
                    .bind("uid", uid)
                    .execute();
            if (updated == 0) {
                h.createUpdate(
                                "INSERT INTO pangya.authkey_login (\"UID\", \"AuthKey\", valid) VALUES (:uid, :k, 1)")
                        .bind("uid", uid)
                        .bind("k", key)
                        .execute();
            }
        });
        return key;
    }

    @Override
    public String generateAuthKeyGame(long uid, int serverUid) {
        String key = randomKey(8);
        jdbi.useHandle(h -> {
            int updated = h.createUpdate("""
                            UPDATE pangya.authkey_game
                               SET \"AuthKey\" = :k, valid = 1
                             WHERE \"UID\" = :uid AND \"ServerID\" = :sid
                            """)
                    .bind("k", key)
                    .bind("uid", uid)
                    .bind("sid", serverUid)
                    .execute();
            if (updated == 0) {
                h.createUpdate("""
                                INSERT INTO pangya.authkey_game (\"UID\", \"AuthKey\", \"ServerID\", valid)
                                VALUES (:uid, :k, :sid, 1)
                                """)
                        .bind("uid", uid)
                        .bind("k", key)
                        .bind("sid", serverUid)
                        .execute();
            }
        });
        return key;
    }

    @Override
    public String generateWebKey(long uid) {
        String key = randomKey(6);
        jdbi.useHandle(h -> {
            int updated = h.createUpdate(
                            "UPDATE pangya.pangya_weblink_cookies_key SET \"key\" = :k, valid = 1 WHERE uid = :uid")
                    .bind("k", key)
                    .bind("uid", uid)
                    .execute();
            if (updated == 0) {
                h.createUpdate(
                                "INSERT INTO pangya.pangya_weblink_cookies_key (uid, \"key\", valid) VALUES (:uid, :k, 1)")
                        .bind("uid", uid)
                        .bind("k", key)
                        .execute();
            }
        });
        return key;
    }

    @Override
    public String generateAuthServerKey(int serverUid) {
        String key = randomKey(16);
        jdbi.useHandle(h -> {
            int updated = h.createUpdate(
                            "UPDATE pangya.pangya_auth_key SET \"key\" = :k, valid = 1 WHERE \"Server_UID\" = :uid")
                    .bind("k", key)
                    .bind("uid", serverUid)
                    .execute();
            if (updated == 0) {
                h.createUpdate(
                                "INSERT INTO pangya.pangya_auth_key (\"Server_UID\", \"key\", valid) VALUES (:uid, :k, 1)")
                        .bind("uid", serverUid)
                        .bind("k", key)
                        .execute();
            }
        });
        return key;
    }

    @Override
    public Optional<AuthServerKey> authServerKey(int serverUid) {
        return jdbi.withHandle(h -> h.createQuery(
                        "SELECT \"Server_UID\", \"key\", valid FROM pangya.pangya_auth_key WHERE \"Server_UID\" = :uid")
                .bind("uid", serverUid)
                .map((rs, ctx) -> new AuthServerKey(
                        rs.getInt("Server_UID"),
                        trim(rs.getString("key")),
                        rs.getInt("valid") == 1))
                .findOne());
    }

    @Override
    public void invalidateAuthServerKey(int serverUid) {
        jdbi.useHandle(h -> h.createUpdate(
                        "UPDATE pangya.pangya_auth_key SET valid = 0 WHERE \"Server_UID\" = :uid")
                .bind("uid", serverUid)
                .execute());
    }

    @Override
    public String[] macros(long uid) {
        String[] out = new String[9];
        jdbi.useHandle(h -> h.createQuery("""
                        SELECT \"Macro1\", \"Macro2\", \"Macro3\", \"Macro4\", \"Macro5\",
                               \"Macro6\", \"Macro7\", \"Macro8\", \"Macro9\"
                          FROM pangya.pangya_user_macro WHERE \"UID\" = :uid
                        """)
                .bind("uid", uid)
                .map((rs, ctx) -> {
                    for (int i = 0; i < 9; i++) {
                        String v = rs.getString(i + 1);
                        out[i] = v == null ? "" : v;
                    }
                    return 0;
                })
                .findOne());
        for (int i = 0; i < 9; i++) {
            if (out[i] == null) {
                out[i] = "";
            }
        }
        return out;
    }

    @Override
    public void saveMacros(long uid, String[] macros) {
        String[] slots = new String[9];
        for (int i = 0; i < 9; i++) {
            slots[i] = (macros != null && i < macros.length && macros[i] != null) ? macros[i] : "";
        }
        jdbi.useHandle(h -> {
            int updated = h.createUpdate("""
                            UPDATE pangya.pangya_user_macro
                               SET \"Macro1\" = :m1, \"Macro2\" = :m2, \"Macro3\" = :m3,
                                   \"Macro4\" = :m4, \"Macro5\" = :m5, \"Macro6\" = :m6,
                                   \"Macro7\" = :m7, \"Macro8\" = :m8, \"Macro9\" = :m9
                             WHERE \"UID\" = :uid
                            """)
                    .bind("uid", uid)
                    .bind("m1", slots[0])
                    .bind("m2", slots[1])
                    .bind("m3", slots[2])
                    .bind("m4", slots[3])
                    .bind("m5", slots[4])
                    .bind("m6", slots[5])
                    .bind("m7", slots[6])
                    .bind("m8", slots[7])
                    .bind("m9", slots[8])
                    .execute();
            if (updated == 0) {
                h.createUpdate("""
                                INSERT INTO pangya.pangya_user_macro (
                                    \"UID\", \"Macro1\", \"Macro2\", \"Macro3\", \"Macro4\", \"Macro5\",
                                    \"Macro6\", \"Macro7\", \"Macro8\", \"Macro9\", \"Macro10\")
                                VALUES (:uid, :m1, :m2, :m3, :m4, :m5, :m6, :m7, :m8, :m9, '')
                                """)
                        .bind("uid", uid)
                        .bind("m1", slots[0])
                        .bind("m2", slots[1])
                        .bind("m3", slots[2])
                        .bind("m4", slots[3])
                        .bind("m5", slots[4])
                        .bind("m6", slots[5])
                        .bind("m7", slots[6])
                        .bind("m8", slots[7])
                        .bind("m9", slots[8])
                        .execute();
            }
        });
    }

    @Override
    public Optional<String> loadAuthKeyLogin(long uid) {
        return jdbi.withHandle(h -> h.createQuery(
                        "SELECT \"AuthKey\" FROM pangya.authkey_login WHERE \"UID\" = :uid AND valid = 1 LIMIT 1")
                .bind("uid", uid)
                .mapTo(String.class)
                .findOne()
                .map(JdbiLoginRepository::trim));
    }

    @Override
    public Optional<String> loadAuthKeyGame(long uid, int serverUid) {
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT \"AuthKey\" FROM pangya.authkey_game
                         WHERE \"UID\" = :uid AND \"ServerID\" = :sid AND valid = 1
                         LIMIT 1
                        """)
                .bind("uid", uid)
                .bind("sid", serverUid)
                .mapTo(String.class)
                .findOne()
                .map(JdbiLoginRepository::trim));
    }

    @Override
    public List<ServerListRow> serverList(int type) {
        // C# ProcGetServerList: Game/MSN liveness window is 8s; Login/Rank/Auth 11s.
        int windowSec = (type == 1 || type == 3) ? 8 : 11;
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT \"Name\", \"UID\", \"IP\", \"Port\", \"MaxUser\", \"CurrUser\", \"Type\",
                               property, \"AngelicWingsNum\", \"EventFlag\", \"EventMap\",
                               \"ImgNo\", \"AppRate\", \"ScratchRate\", \"ServerVersion\", \"ClientVersion\"
                          FROM pangya.pangya_server_list
                         WHERE \"Type\" = :type
                           AND \"State\" = 1
                           AND \"UpdateTime\" > NOW() - (:window * INTERVAL '1 second')
                        """)
                .bind("type", type)
                .bind("window", windowSec)
                .map((rs, ctx) -> new ServerListRow(
                        rs.getString("Name"),
                        rs.getInt("UID"),
                        rs.getString("IP"),
                        rs.getInt("Port"),
                        rs.getInt("MaxUser"),
                        rs.getInt("CurrUser"),
                        rs.getInt("Type"),
                        rs.getInt("property"),
                        rs.getInt("AngelicWingsNum"),
                        rs.getInt("EventFlag"),
                        (short) rs.getInt("EventMap"),
                        rs.getShort("AppRate"),
                        rs.getShort("ScratchRate"),
                        rs.getShort("ImgNo"),
                        rs.getString("ServerVersion"),
                        rs.getString("ClientVersion")))
                .list());
    }

    @Override
    public void upsertServer(ServerListRow server) {
        jdbi.useHandle(h -> {
            int updated = h.createUpdate("""
                            UPDATE pangya.pangya_server_list SET
                                \"Name\" = :name, \"IP\" = :ip, \"Port\" = :port, \"Type\" = :type,
                                \"MaxUser\" = :maxu, \"CurrUser\" = :curr, \"State\" = 1,
                                \"UpdateTime\" = :now, \"PangRate\" = 100,
                                \"ServerVersion\" = :ver, \"ClientVersion\" = :cver,
                                property = :prop, \"AngelicWingsNum\" = :angel, \"EventFlag\" = :eflag,
                                \"ExpRate\" = 100, \"ImgNo\" = :img, \"ScratchRate\" = :scratch,
                                \"MasteryRate\" = 100, \"TreasureRate\" = 100, \"ChuvaRate\" = 100,
                                \"RareItemRate\" = 100, \"CookieItemRate\" = 100, \"AppRate\" = :app,
                                \"EventMap\" = :emap
                            WHERE \"UID\" = :uid
                            """)
                    .bind("name", nz(server.name(), "server"))
                    .bind("ip", nz(server.ip(), "127.0.0.1"))
                    .bind("port", server.port())
                    .bind("type", server.type())
                    .bind("maxu", server.maxUser())
                    .bind("curr", server.currUser())
                    .bind("now", OffsetDateTime.now())
                    .bind("ver", nz(server.version(), "Java.S2"))
                    .bind("cver", nz(server.clientVersion(), "JP.R7.983.00"))
                    .bind("prop", server.property())
                    .bind("angel", server.angelicWings())
                    .bind("eflag", server.eventFlag())
                    .bind("img", (int) server.imgNo())
                    .bind("scratch", (int) server.scratchRate())
                    .bind("app", (int) server.appRate())
                    .bind("emap", (int) server.eventMap())
                    .bind("uid", server.uid())
                    .execute();
            if (updated == 0) {
                h.createUpdate("""
                                INSERT INTO pangya.pangya_server_list (
                                    \"Name\", \"UID\", \"IP\", \"Port\", \"MaxUser\", \"CurrUser\", \"Type\",
                                    \"UpdateTime\", \"State\", \"PCBangUser\", \"PangRate\", \"ServerVersion\",
                                    \"ClientVersion\", property, \"AngelicWingsNum\", \"EventFlag\", \"ExpRate\",
                                    \"RareItemRate\", \"CookieItemRate\", \"ServiceControl\", \"ImgNo\",
                                    \"AppRate\", \"ScratchRate\", \"EventMap\", \"EventDropRate\",
                                    \"HanbitUser\", \"ParanUser\", \"AuthState\", \"MasteryRate\",
                                    \"TreasureRate\", \"ChuvaRate\"
                                ) VALUES (
                                    :name, :uid, :ip, :port, :maxu, :curr, :type,
                                    :now, 1, 0, 100, :ver,
                                    :cver, :prop, :angel, :eflag, 100,
                                    100, 100, 0, :img,
                                    :app, :scratch, :emap, 0,
                                    0, 0, 0, 100,
                                    100, 100
                                )
                                """)
                        .bind("name", nz(server.name(), "server"))
                        .bind("uid", server.uid())
                        .bind("ip", nz(server.ip(), "127.0.0.1"))
                        .bind("port", server.port())
                        .bind("maxu", server.maxUser())
                        .bind("curr", server.currUser())
                        .bind("type", server.type())
                        .bind("now", OffsetDateTime.now())
                        .bind("ver", nz(server.version(), "Java.S2"))
                        .bind("cver", nz(server.clientVersion(), "JP.R7.983.00"))
                        .bind("prop", server.property())
                        .bind("angel", server.angelicWings())
                        .bind("eflag", server.eventFlag())
                        .bind("img", (int) server.imgNo())
                        .bind("app", (int) server.appRate())
                        .bind("scratch", (int) server.scratchRate())
                        .bind("emap", (int) server.eventMap())
                        .execute();
            }
        });
    }

    @Override
    public boolean nickInUse(String nick) {
        if (nick == null || nick.isBlank()) {
            return false;
        }
        return jdbi.withHandle(h -> h.createQuery(
                        "SELECT 1 FROM pangya.account WHERE \"NICK\" = :nick LIMIT 1")
                .bind("nick", nick)
                .mapTo(Integer.class)
                .findOne()
                .isPresent());
    }

    @Override
    public void saveNick(long uid, String nick) {
        jdbi.useHandle(h -> h.createUpdate(
                        "UPDATE pangya.account SET \"NICK\" = :nick WHERE \"UID\" = :uid")
                .bind("nick", nick)
                .bind("uid", uid)
                .execute());
    }

    @Override
    public void markFirstLogin(long uid) {
        jdbi.useHandle(h -> h.createUpdate(
                        "UPDATE pangya.account SET \"FIRST_LOGIN\" = 1 WHERE \"UID\" = :uid")
                .bind("uid", uid)
                .execute());
    }

    @Override
    public int insertCharacter(long uid, int typeid, int hair, int shirts) {
        return jdbi.withHandle(h -> h.createQuery("""
                        INSERT INTO pangya.pangya_character_information (
                            typeid, "UID",
                            parts_1, parts_2, parts_3, parts_4, parts_5, parts_6, parts_7, parts_8,
                            parts_9, parts_10, parts_11, parts_12, parts_13, parts_14, parts_15, parts_16,
                            parts_17, parts_18, parts_19, parts_20, parts_21, parts_22, parts_23, parts_24,
                            default_hair, default_shirts, gift_flag,
                            "PCL0", "PCL1", "PCL2", "PCL3", "PCL4", "Purchase",
                            auxparts_1, auxparts_2, auxparts_3, auxparts_4, auxparts_5,
                            "CutIn_1", "CutIn_2", "CutIn_3", "CutIn_4", "Mastery"
                        ) VALUES (
                            :typeid, :uid,
                            0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0,
                            :hair, :shirts, 0,
                            0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0
                        )
                        RETURNING item_id
                        """)
                .bind("typeid", typeid)
                .bind("uid", uid)
                .bind("hair", hair)
                .bind("shirts", shirts)
                .mapTo(Integer.class)
                .one());
    }

    @Override
    public void applyFirstSet(long uid, int characterId) {
        jdbi.useTransaction(h -> {
            Integer clubId = h.createQuery("""
                            SELECT item_id FROM pangya.pangya_item_warehouse
                             WHERE "UID" = :uid AND typeid = :typeid LIMIT 1
                            """)
                    .bind("uid", uid)
                    .bind("typeid", GamePackets.TYPEID_AIR_KNIGHT)
                    .mapTo(Integer.class)
                    .findOne()
                    .orElse(null);
            if (clubId == null) {
                clubId = h.createQuery("""
                                INSERT INTO pangya.pangya_item_warehouse (
                                    "UID", typeid, valid, "Gift_flag", flag,
                                    "C0", "C1", "C2", "C3", "C4", "Purchase", "ItemType",
                                    "ClubSet_WorkShop_Flag", "ClubSet_WorkShop_C0", "ClubSet_WorkShop_C1",
                                    "ClubSet_WorkShop_C2", "ClubSet_WorkShop_C3", "ClubSet_WorkShop_C4",
                                    "Mastery_Pts", "Recovery_Pts", "Level", "Up",
                                    "Total_Mastery_Pts", "Mastery_Gasto"
                                ) VALUES (
                                    :uid, :typeid, 1, 0, 0,
                                    0, 0, 0, 0, 0, 0, 2,
                                    0, 0, 0, 0, 0, 0,
                                    0, 0, 0, 0, 0, 0
                                )
                                RETURNING item_id
                                """)
                        .bind("uid", uid)
                        .bind("typeid", GamePackets.TYPEID_AIR_KNIGHT)
                        .mapTo(Integer.class)
                        .one();
            }
            int ballExists = h.createQuery("""
                            SELECT count(*) FROM pangya.pangya_item_warehouse
                             WHERE "UID" = :uid AND typeid = :typeid
                            """)
                    .bind("uid", uid)
                    .bind("typeid", GamePackets.TYPEID_DEFAULT_BALL)
                    .mapTo(Integer.class)
                    .one();
            if (ballExists == 0) {
                h.createUpdate("""
                                INSERT INTO pangya.pangya_item_warehouse (
                                    "UID", typeid, valid, "Gift_flag", flag,
                                    "C0", "C1", "C2", "C3", "C4", "Purchase", "ItemType",
                                    "ClubSet_WorkShop_Flag", "ClubSet_WorkShop_C0", "ClubSet_WorkShop_C1",
                                    "ClubSet_WorkShop_C2", "ClubSet_WorkShop_C3", "ClubSet_WorkShop_C4",
                                    "Mastery_Pts", "Recovery_Pts", "Level", "Up",
                                    "Total_Mastery_Pts", "Mastery_Gasto"
                                ) VALUES (
                                    :uid, :typeid, 1, 0, 0,
                                    1, 0, 0, 0, 0, 0, 2,
                                    0, 0, 0, 0, 0, 0,
                                    0, 0, 0, 0, 0, 0
                                )
                                """)
                        .bind("uid", uid)
                        .bind("typeid", GamePackets.TYPEID_DEFAULT_BALL)
                        .execute();
            }
            int equipped = h.createUpdate("""
                            UPDATE pangya.pangya_user_equip
                               SET character_id = :cid, club_id = :club, ball_type = :ball
                             WHERE "UID" = :uid
                            """)
                    .bind("cid", characterId)
                    .bind("club", clubId)
                    .bind("ball", GamePackets.TYPEID_DEFAULT_BALL)
                    .bind("uid", uid)
                    .execute();
            if (equipped == 0) {
                h.createUpdate("""
                                INSERT INTO pangya.pangya_user_equip (
                                    "UID", caddie_id, character_id, club_id, ball_type,
                                    item_slot_1, item_slot_2, item_slot_3, item_slot_4, item_slot_5,
                                    item_slot_6, item_slot_7, item_slot_8, item_slot_9, item_slot_10,
                                    "Skin_1", "Skin_2", "Skin_3", "Skin_4", "Skin_5", "Skin_6",
                                    mascot_id, poster_1, poster_2
                                ) VALUES (
                                    :uid, 0, :cid, :club, :ball,
                                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                                    0, 0, 0, 0, 0, 0,
                                    0, 0, 0
                                )
                                """)
                        .bind("uid", uid)
                        .bind("cid", characterId)
                        .bind("club", clubId)
                        .bind("ball", GamePackets.TYPEID_DEFAULT_BALL)
                        .execute();
            }
            h.createUpdate("""
                            UPDATE pangya.user_info
                               SET "Pang" = "Pang" + 100000, "Cookie" = 120
                             WHERE "UID" = :uid
                            """)
                    .bind("uid", uid)
                    .execute();
            h.createUpdate("""
                            UPDATE pangya.account
                               SET "FIRST_SET" = 1, "Event" = 1, "IDState" = 0
                             WHERE "UID" = :uid
                            """)
                    .bind("uid", uid)
                    .execute();
        });
    }

    private static String randomKey(int len) {
        char[] buf = new char[len];
        for (int i = 0; i < len; i++) {
            buf[i] = KEY_ALPHABET[RNG.nextInt(KEY_ALPHABET.length)];
        }
        return new String(buf);
    }

    private static String nz(String v, String fallback) {
        return (v == null || v.isBlank()) ? fallback : v;
    }

    private static String trim(String v) {
        return v == null ? "" : v.trim();
    }

    private static long ipv4(String dotted) {
        if (dotted == null) {
            return -1;
        }
        String[] parts = dotted.split("\\.");
        if (parts.length != 4) {
            return -1;
        }
        try {
            long v = 0;
            for (String p : parts) {
                int n = Integer.parseInt(p);
                if (n < 0 || n > 255) {
                    return -1;
                }
                v = (v << 8) | n;
            }
            return v;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    public void insertMsgOff(long fromUid, long toUid, String msg) {
        jdbi.useHandle(h -> h.createUpdate("""
                        INSERT INTO pangya.pangya_msg_user (uid, uid_from, valid, msg, reg_date)
                        VALUES (:to, :from, 1, :msg, NOW())
                        """)
                .bind("to", toUid)
                .bind("from", fromUid)
                .bind("msg", msg)
                .execute());
    }

    private record IpBan(String ip, String mask) {}
}
