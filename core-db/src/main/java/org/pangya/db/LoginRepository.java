package org.pangya.db;

import java.util.List;
import java.util.Optional;

/** Login / Auth SQL replacing C# {@code ProcVerify*} / {@code ProcGeraAuthKey*} / {@code ProcRegServer_New}. */
public interface LoginRepository {

    Optional<Long> verifyId(String id);

    boolean verifyPass(long uid, String password);

    Optional<PlayerLoginInfo> playerInfo(long uid);

    boolean isBannedIp(String ip);

    boolean isBannedMac(String mac);

    boolean isFirstLoginDone(long uid);

    boolean isFirstSetDone(long uid);

    boolean isLogon(long uid);

    void registerPlayerLogin(long uid, String ip, int serverUid);

    void registerLogonServer(long uid, int gameServerUid);

    String generateAuthKeyLogin(long uid);

    String generateAuthKeyGame(long uid, int serverUid);

    /** JP {@code ProcGeraWeblinkCookiesKey}: 6-char hex, stored in {@code pangya_weblink_cookies_key}. */
    String generateWebKey(long uid);

    String generateAuthServerKey(int serverUid);

    Optional<AuthServerKey> authServerKey(int serverUid);

    void invalidateAuthServerKey(int serverUid);

    String[] macros(long uid);

    Optional<String> loadAuthKeyLogin(long uid);

    Optional<String> loadAuthKeyGame(long uid, int serverUid);

    List<ServerListRow> serverList(int type);

    void upsertServer(ServerListRow server);

    /** True when another account already uses this nick (C# {@code VerifyNick}). */
    boolean nickInUse(String nick);

    void saveNick(long uid, String nick);

    void markFirstLogin(long uid);

    /**
     * JP {@code ProcAddCharacter}: insert {@code pangya_character_information} and return item_id.
     * Parts stay 0 when IFF {@code initComboDef} cannot run.
     */
    int insertCharacter(long uid, int typeid, int hair, int shirts);

    /**
     * JP {@code ProcFirstSet} essentials: Air Knight + default ball, equip them,
     * {@code FIRST_SET=1}, closed-beta pang/cookie bump.
     */
    void applyFirstSet(long uid, int characterId);

    record PlayerLoginInfo(
            long uid,
            String id,
            String nickname,
            String password,
            int capability,
            int level,
            long idState,
            int blockTimeSeconds) {}

    record AuthServerKey(int serverUid, String key, boolean valid) {}

    record ServerListRow(
            String name,
            int uid,
            String ip,
            int port,
            int maxUser,
            int currUser,
            int type,
            int property,
            int angelicWings,
            int eventFlag,
            short eventMap,
            short appRate,
            short scratchRate,
            short imgNo,
            String version,
            String clientVersion) {}
}
