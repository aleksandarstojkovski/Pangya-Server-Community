package org.pangya.game;

import com.zaxxer.hikari.HikariDataSource;
import org.pangya.db.DatabaseSupport;
import org.pangya.db.JdbiLoginRepository;
import org.pangya.db.LoginRepository;
import org.pangya.network.AppConfig;
import org.pangya.network.HealthHttp;
import org.pangya.network.auth.AuthServerConnector;
import org.pangya.network.ddos.IpDdosFilter;
import org.pangya.network.netty.PangyaNettyServer;
import org.pangya.network.netty.ServerKind;
import org.pangya.network.redis.SessionKeyStore;
import org.pangya.network.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public final class GameRuntime implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(GameRuntime.class);

    private final AppConfig config;
    private final HikariDataSource dataSource;
    private final SessionKeyStore redis;
    private final SessionManager sessions;
    private final PangyaNettyServer netty;
    private final HealthHttp health;
    private final AuthServerConnector auth;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Thread heartbeat;

    public GameRuntime(AppConfig config) {
        this.config = config;
        this.dataSource = DatabaseSupport.dataSource(config.jdbcUrl(), config.dbUser(), config.dbPassword());
        LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(dataSource));
        this.redis = new SessionKeyStore(config.redisUri());
        this.sessions = new SessionManager(new IpDdosFilter());
        GameHandler handler = new GameHandler(config, repo, redis, sessions, GameHandler.loadChannels(config));
        this.netty = new PangyaNettyServer(ServerKind.GAME, sessions, handler::onPacket);
        this.netty.bind(config.port());
        this.health = new HealthHttp(config.healthPort(), config.serverName());
        if (config.authEnabled()) {
            this.auth = new AuthServerConnector(config, repo::generateAuthServerKey);
            this.auth.start();
        } else {
            this.auth = null;
        }
        heartbeatOnce(repo);
        this.heartbeat = Thread.ofVirtual().name("game-heartbeat").start(() -> heartbeatLoop(repo));
        log.info("game server uid={} port={}", config.uid(), config.port());
    }

    public int port() {
        return netty.localPort();
    }

    public SessionManager sessions() {
        return sessions;
    }

    public AuthServerConnector auth() {
        return auth;
    }

    private void heartbeatLoop(LoginRepository repo) {
        while (running.get()) {
            heartbeatOnce(repo);
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void heartbeatOnce(LoginRepository repo) {
        try {
            repo.upsertServer(new LoginRepository.ServerListRow(
                    config.serverName(),
                    config.uid(),
                    config.advertisedIp(),
                    netty.localPort(),
                    config.maxUser(),
                    sessions.size(),
                    1,
                    config.property(),
                    0,
                    0,
                    (short) 0,
                    (short) 0,
                    (short) 0,
                    (short) 0,
                    config.version(),
                    config.clientVersion()));
        } catch (RuntimeException e) {
            log.warn("game server-list heartbeat: {}", e.toString());
        }
    }

    @Override
    public void close() {
        running.set(false);
        heartbeat.interrupt();
        if (auth != null) {
            auth.close();
        }
        health.close();
        netty.close();
        redis.close();
        dataSource.close();
    }

    public static void runBlocking(AppConfig config) {
        try (GameRuntime ignored = new GameRuntime(config)) {
            CountDownLatch done = new CountDownLatch(1);
            Runtime.getRuntime().addShutdownHook(new Thread(done::countDown, "game-shutdown"));
            done.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
