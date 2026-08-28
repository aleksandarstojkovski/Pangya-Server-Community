package org.pangya.network;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.LinkedHashMap;
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
