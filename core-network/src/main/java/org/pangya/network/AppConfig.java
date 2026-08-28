package org.pangya.network;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads {@code application.yml} then overlays environment variables.
 * Env names: {@code PANGYA_PORT}, {@code PANGYA_HEALTH_PORT}, {@code DATABASE_URL},
 * {@code DATABASE_USER}, {@code DATABASE_PASSWORD}, {@code REDIS_URI}.
 */
public final class AppConfig {

    private final Map<String, Object> root;

    public AppConfig(Map<String, Object> root) {
        this.root = root;
    }

    @SuppressWarnings("unchecked")
    public static AppConfig load(String resource) {
        Map<String, Object> yaml = new LinkedHashMap<>();
        try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream(resource)) {
            if (in != null) {
                Object loaded = new Yaml().load(in);
                if (loaded instanceof Map<?, ?> map) {
                    yaml = (Map<String, Object>) map;
                }
            }
        } catch (Exception ignored) {
            // missing yaml is fine; env vars still apply
        }
        return new AppConfig(yaml);
    }

    public String serverName() {
        return envOr("PANGYA_NAME", nested("server", "name", "pangya"));
    }

    public int port() {
        return envInt("PANGYA_PORT", nestedInt("server", "port", 0));
    }

    public int healthPort() {
        return envInt("PANGYA_HEALTH_PORT", nestedInt("server", "healthPort", 0));
    }

    public String jdbcUrl() {
        return envOr("DATABASE_URL", nested("database", "url", "jdbc:postgresql://localhost:5432/pangya"));
    }

    public String dbUser() {
        return envOr("DATABASE_USER", nested("database", "user", "pangya"));
    }

    public String dbPassword() {
        return envOr("DATABASE_PASSWORD", nested("database", "password", "pangya"));
    }

    public boolean migrateOnStart() {
        String v = System.getenv("PANGYA_MIGRATE_ON_START");
        if (v != null) {
            return Boolean.parseBoolean(v);
        }
        return nestedBoolean("database", "migrateOnStart", false);
    }

    public String redisUri() {
        return envOr("REDIS_URI", nested("redis", "uri", "redis://localhost:6379"));
    }

    public int uid() {
        return envInt("PANGYA_UID", nestedInt("server", "uid", 0));
    }

    public String advertisedIp() {
        return envOr("PANGYA_IP", nested("server", "ip", "127.0.0.1"));
    }

    public int maxUser() {
        return nestedInt("server", "maxUser", 2001);
    }

    public int property() {
        return nestedInt("server", "property", 0);
    }

    public String version() {
        return nested("server", "version", "Java.S2");
    }

    public String clientVersion() {
        return nested("server", "clientVersion", "852.00");
    }

    public int packetVersion() {
        return nestedInt("server", "packetVersion", 2016110200);
    }

    /** C# {@code ServerInfo.rate.pang}; default 100. */
    public int ratePang() {
        return nestedInt("server", "ratePang", 100);
    }

    /** C# {@code ServerInfo.rate.exp}; default 100. */
    public int rateExp() {
        return nestedInt("server", "rateExp", 100);
    }

    public int tipo() {
        return nestedInt("server", "tipo", 0);
    }

    public boolean maintenance() {
        return nestedBoolean("server", "maintenance", false);
    }

    public String authHost() {
        return envOr("PANGYA_AUTH_HOST", nested("auth", "host", "127.0.0.1"));
    }

    public int authPort() {
        return envInt("PANGYA_AUTH_PORT", nestedInt("auth", "port", 7777));
    }

    public int authGuid() {
        return envInt("PANGYA_AUTH_GUID", nestedInt("auth", "guid", 8888));
    }

    public boolean authEnabled() {
        String v = System.getenv("PANGYA_AUTH_ENABLED");
        if (v != null) {
            return Boolean.parseBoolean(v);
        }
        return nestedBoolean("auth", "enabled", true);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> list(String name) {
        Object s = root.get(name);
        if (s instanceof List<?> list) {
            List<Map<String, Object>> out = new ArrayList<>();
            for (Object o : list) {
                if (o instanceof Map<?, ?> map) {
                    out.add((Map<String, Object>) map);
                }
            }
            return out;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> section(String name) {
        Object s = root.get(name);
        if (s instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    public int nestedIntFrom(Map<String, Object> map, String key, int fallback) {
        Object v = map.get(key);
        if (v instanceof Number n) {
            return n.intValue();
        }
        if (v != null) {
            return Integer.parseInt(String.valueOf(v));
        }
        return fallback;
    }

    public String nestedFrom(Map<String, Object> map, String key, String fallback) {
        Object v = map.get(key);
        return v == null ? fallback : String.valueOf(v);
    }

    private static String envOr(String env, String fallback) {
        String v = System.getenv(env);
        return (v == null || v.isBlank()) ? fallback : v;
    }

    private static int envInt(String env, int fallback) {
        String v = System.getenv(env);
        if (v == null || v.isBlank()) {
            return fallback;
        }
        return Integer.parseInt(v);
    }

    @SuppressWarnings("unchecked")
    private String nested(String section, String key, String fallback) {
        Object s = root.get(section);
        if (s instanceof Map<?, ?> map) {
            Object v = map.get(key);
            if (v != null) {
                return String.valueOf(v);
            }
        }
        return fallback;
    }

    @SuppressWarnings("unchecked")
    private int nestedInt(String section, String key, int fallback) {
        Object s = root.get(section);
        if (s instanceof Map<?, ?> map) {
            Object v = map.get(key);
            if (v instanceof Number n) {
                return n.intValue();
            }
            if (v != null) {
                return Integer.parseInt(String.valueOf(v));
            }
        }
        return fallback;
    }

    @SuppressWarnings("unchecked")
    private boolean nestedBoolean(String section, String key, boolean fallback) {
        Object s = root.get(section);
        if (s instanceof Map<?, ?> map) {
            Object v = map.get(key);
            if (v instanceof Boolean b) {
                return b;
            }
            if (v != null) {
                return Boolean.parseBoolean(String.valueOf(v));
            }
        }
        return fallback;
    }
}
