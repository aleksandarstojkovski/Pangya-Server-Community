package org.pangya.auth;

import org.pangya.db.DatabaseSupport;
import org.pangya.network.AppConfig;
import org.pangya.network.PangyaProcess;

public final class AuthServerMain {

    public static void main(String[] args) {
        AppConfig config = AppConfig.load("application.yml");
        if (config.migrateOnStart()) {
            DatabaseSupport.migrate(config.jdbcUrl(), config.dbUser(), config.dbPassword());
        }
        PangyaProcess.runBlocking(config);
    }
}
