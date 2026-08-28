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

    public static int migrate(String jdbcUrl, String user, String password) {
        return flyway(jdbcUrl, user, password).migrate().migrationsExecuted;
    }

    public static HikariDataSource dataSource(String jdbcUrl, String user, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setPoolName("pangya");
        return new HikariDataSource(config);
    }

    public static Jdbi jdbi(DataSource dataSource) {
        return Jdbi.create(dataSource)
                .installPlugin(new PostgresPlugin())
                .installPlugin(new SqlObjectPlugin());
    }
}
