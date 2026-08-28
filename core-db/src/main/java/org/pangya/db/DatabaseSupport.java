package org.pangya.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.postgres.PostgresPlugin;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

import javax.sql.DataSource;

public final class DatabaseSupport {

    private DatabaseSupport() {}

    public static Flyway flyway(String jdbcUrl, String user, String password) {
        return Flyway.configure()
                .dataSource(jdbcUrl, user, password)
                .locations("classpath:db/migration")
                .schemas("public", "pangya")
                .createSchemas(true)
                .load();
    }

    /**
     * Runs Flyway migrate. Connection timeouts (nested Docker published-port path)
     * are retried; Flyway checksum/SQL errors fail immediately.
     */
    public static int migrate(String jdbcUrl, String user, String password) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= 15; attempt++) {
            try {
                return flyway(jdbcUrl, user, password).migrate().migrationsExecuted;
            } catch (RuntimeException e) {
                last = e;
                if (!isTransientConnectFailure(e) || attempt == 15) {
                    break;
                }
                try {
                    Thread.sleep(2_000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
        throw last;
    }

    static boolean isTransientConnectFailure(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof java.net.SocketTimeoutException
                    || t instanceof java.net.ConnectException) {
                return true;
            }
            String msg = t.getMessage();
            if (msg != null && (msg.contains("The connection attempt failed")
                    || msg.contains("Connect timed out")
                    || msg.contains("Connection refused"))) {
                return true;
            }
        }
        return false;
    }

    public static HikariDataSource dataSource(String jdbcUrl, String user, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setPoolName("pangya");
        config.setConnectionTimeout(15_000);
        config.setInitializationFailTimeout(30_000);
        return new HikariDataSource(config);
    }

    public static Jdbi jdbi(DataSource dataSource) {
        return Jdbi.create(dataSource)
                .installPlugin(new PostgresPlugin())
                .installPlugin(new SqlObjectPlugin());
    }
}
