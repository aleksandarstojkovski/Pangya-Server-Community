package org.pangya.network.session;

import io.netty.channel.Channel;
import org.pangya.network.ddos.IpDdosFilter;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public final class SessionManager {

    private final ConcurrentHashMap<Integer, Session> byOid = new ConcurrentHashMap<>();
    private final AtomicInteger nextOid = new AtomicInteger(1);
    private final IpDdosFilter ddos;

    public SessionManager(IpDdosFilter ddos) {
        this.ddos = ddos;
    }

    public Session create(Channel channel) {
        int key = ThreadLocalRandom.current().nextInt(16);
        int oid = nextOid.getAndIncrement();
        Session session = new Session(channel, key, oid);
        byOid.put(oid, session);
        return session;
    }

    public void remove(Session session) {
        if (session == null) {
            return;
        }
        byOid.remove(session.oid());
        ddos.onDisconnect(session.ip());
    }

    public int size() {
        return byOid.size();
    }

    public Collection<Session> snapshot() {
        return List.copyOf(byOid.values());
    }

    public Session findByUid(long uid) {
        if (uid <= 0) {
            return null;
        }
        for (Session session : snapshot()) {
            if (session.authorized() && session.player().uid == uid) {
                return session;
            }
        }
        return null;
    }

    public Session findByNickname(String nickname) {
        if (nickname == null || nickname.isEmpty()) {
            return null;
        }
        for (Session session : snapshot()) {
            if (session.authorized() && nickname.equals(session.player().nickname)) {
                return session;
            }
        }
        return null;
    }

    public Session findByOid(int oid) {
        return byOid.get(oid);
    }

    public void disconnectOthersWithUid(long uid, Session keep) {
        if (uid <= 0) {
            return;
        }
        for (Session other : snapshot()) {
            if (other != keep && other.player().uid == uid) {
                other.disconnect();
            }
        }
    }

    public IpDdosFilter ddos() {
        return ddos;
    }
}
