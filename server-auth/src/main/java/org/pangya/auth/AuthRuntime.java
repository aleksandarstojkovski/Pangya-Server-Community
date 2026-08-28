package org.pangya.auth;

import com.zaxxer.hikari.HikariDataSource;
import org.pangya.db.DatabaseSupport;
import org.pangya.db.JdbiLoginRepository;
import org.pangya.network.AppConfig;
import org.pangya.network.HealthHttp;
import org.pangya.network.ddos.IpDdosFilter;
import org.pangya.network.netty.PangyaNettyServer;
import org.pangya.network.netty.ServerKind;
import org.pangya.network.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

public final class AuthRuntime implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(AuthRuntime.class);

    private final HikariDataSource dataSource;
    private final PangyaNettyServer netty;
    private final HealthHttp health;

    public AuthRuntime(AppConfig config) {
        if (config.migrateOnStart()) {
            DatabaseSupport.migrate(config.jdbcUrl(), config.dbUser(), config.dbPassword());
        }
        this.dataSource = DatabaseSupport.dataSource(config.jdbcUrl(), config.dbUser(), config.dbPassword());
        AuthHandler handler = new AuthHandler(new JdbiLoginRepository(DatabaseSupport.jdbi(dataSource)));
        SessionManager sessions = new SessionManager(new IpDdosFilter());
        this.netty = new PangyaNettyServer(ServerKind.AUTH, sessions, handler::onPacket, config.authGuid());
        this.netty.bind(config.port());
        this.health = new HealthHttp(config.healthPort(), config.serverName());
        log.info("auth server guid={} port={}", config.authGuid(), config.port());
    }

    public int port() {
        return netty.localPort();
    }

    @Override
    public void close() {
        health.close();
        netty.close();
        dataSource.close();
    }

    public static void runBlocking(AppConfig config) {
        try (AuthRuntime ignored = new AuthRuntime(config)) {
            CountDownLatch done = new CountDownLatch(1);
            Runtime.getRuntime().addShutdownHook(new Thread(done::countDown, "auth-shutdown"));
            done.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
