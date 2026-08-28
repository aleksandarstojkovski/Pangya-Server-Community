package org.pangya.network.redis;

import redis.clients.jedis.JedisPooled;

import java.net.URI;

/** Redis session keys: login/game auth keys with a 10-minute TTL (C# keys live in SQL + process memory). */
public final class SessionKeyStore implements AutoCloseable {

    public static final int TTL_SECONDS = 600;

    private final JedisPooled jedis;

    public SessionKeyStore(String redisUri) {
        this.jedis = new JedisPooled(URI.create(redisUri));
    }

    public void putLoginKey(long uid, String key) {
        jedis.setex(loginKey(uid), TTL_SECONDS, key);
    }

    public String getLoginKey(long uid) {
        return jedis.get(loginKey(uid));
    }

    public void putGameKey(long uid, int serverUid, String key) {
        jedis.setex(gameKey(uid, serverUid), TTL_SECONDS, key);
    }

    public String getGameKey(long uid, int serverUid) {
        return jedis.get(gameKey(uid, serverUid));
    }

    public void putPlayerIp(long uid, String ip) {
        jedis.setex("pangya:session:ip:" + uid, TTL_SECONDS, ip);
    }

    static String loginKey(long uid) {
        return "pangya:authkey:login:" + uid;
    }

    static String gameKey(long uid, int serverUid) {
        return "pangya:authkey:game:" + uid + ":" + serverUid;
    }

    @Override
    public void close() {
        jedis.close();
    }
}
