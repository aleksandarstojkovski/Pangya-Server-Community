package org.pangya.network.ddos;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Equivalent of C# {@code ConfigDDos.IsBlocked}: per-IP connection cap and sliding-window rate.
 */
public final class IpDdosFilter {

    public static final class Config {
        public boolean enabled = true;
        public int limitConnectionPerIp = 10;
        public int ddosIntervalMs = 3000;
        public int ddosCount = 5;
        public int autoResetMs = 600_000;
    }

    private final Config config;
    private final ConcurrentHashMap<String, Integer> connections = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Deque<Long>> window = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> blockedUntil = new ConcurrentHashMap<>();

    public IpDdosFilter(Config config) {
        this.config = config;
    }

    public IpDdosFilter() {
        this(new Config());
    }

    public boolean isBlocked(String ip) {
        if (!config.enabled) {
            return false;
        }
        Long until = blockedUntil.get(ip);
        long now = System.currentTimeMillis();
        if (until != null && until > now) {
            return true;
        }
        if (until != null) {
            blockedUntil.remove(ip);
        }
        Deque<Long> hits = window.computeIfAbsent(ip, k -> new ArrayDeque<>());
        synchronized (hits) {
            hits.addLast(now);
            while (!hits.isEmpty() && now - hits.peekFirst() > config.ddosIntervalMs) {
                hits.removeFirst();
            }
            if (hits.size() >= config.ddosCount) {
                blockedUntil.put(ip, now + config.autoResetMs);
                return true;
            }
        }
        int current = connections.getOrDefault(ip, 0);
        return current >= config.limitConnectionPerIp;
    }

    public void onConnect(String ip) {
        connections.merge(ip, 1, Integer::sum);
    }

    public void onDisconnect(String ip) {
        connections.computeIfPresent(ip, (k, v) -> v <= 1 ? null : v - 1);
    }

    public Map<String, Integer> snapshot() {
        return Map.copyOf(connections);
    }
}
