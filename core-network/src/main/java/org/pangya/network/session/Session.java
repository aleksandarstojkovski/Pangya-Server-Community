package org.pangya.network.session;

import io.netty.channel.Channel;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Session {

    private final Channel channel;
    private final int key;
    private final int oid;
    private final String ip;
    private final AtomicBoolean authorized = new AtomicBoolean(false);

    public Session(Channel channel, int key, int oid) {
        this.channel = channel;
        this.key = key;
        this.oid = oid;
        if (channel.remoteAddress() instanceof InetSocketAddress addr) {
            this.ip = addr.getAddress().getHostAddress();
        } else {
            this.ip = "0.0.0.0";
        }
    }

    public Channel channel() {
        return channel;
    }

    public int key() {
        return key;
    }

    public int oid() {
        return oid;
    }

    public String ip() {
        return ip;
    }

    public void setAuthorized(boolean value) {
        authorized.set(value);
    }

    public boolean authorized() {
        return authorized.get();
    }
}
