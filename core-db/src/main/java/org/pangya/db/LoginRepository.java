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

    String generateAuthServerKey(int serverUid);

    Optional<AuthServerKey> authServerKey(int serverUid);

    void invalidateAuthServerKey(int serverUid);

    String[] macros(long uid);

    List<ServerListRow> serverList(int type);

    void upsertServer(ServerListRow server);

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
