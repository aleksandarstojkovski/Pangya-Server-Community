package org.pangya.db;

import org.jdbi.v3.core.Jdbi;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public final class JdbiLoginRepository implements LoginRepository {

    private static final char[] KEY_ALPHABET = "0123456789ABCDEF".toCharArray();
    private static final SecureRandom RNG = new SecureRandom();

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
        return jdbi.withHandle(h -> h.createQuery("""
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
                        WHERE a."UID" = :uid
                        """)
                .bind("uid", uid)
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
                    .bind("cver", nz(server.clientVersion(), "852.00"))
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
                        .bind("cver", nz(server.clientVersion(), "852.00"))
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

    private record IpBan(String ip, String mask) {}
}
