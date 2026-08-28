package org.pangya.game;

import com.zaxxer.hikari.HikariDataSource;
import org.pangya.db.DatabaseSupport;
import org.pangya.db.InventoryRepository;
import org.pangya.db.JdbiInventoryRepository;
import org.pangya.db.JdbiLoginRepository;
import org.pangya.db.LoginRepository;
import org.pangya.network.AppConfig;
import org.pangya.network.HealthHttp;
import org.pangya.network.PangyaMetrics;
import org.pangya.network.auth.AuthOutbound;
import org.pangya.network.auth.AuthServerConnector;
import org.pangya.network.auth.AuthShutdownScheduler;
import org.pangya.protocol.auth.AuthS2s;
import org.pangya.network.ddos.IpDdosFilter;
import org.pangya.network.netty.PangyaNettyServer;
import org.pangya.network.netty.ServerKind;
import org.pangya.network.redis.SessionKeyStore;
import org.pangya.network.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntConsumer;

public final class GameRuntime implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(GameRuntime.class);

    private final AppConfig config;
    private final HikariDataSource dataSource;
    private final SessionKeyStore redis;
    private final SessionManager sessions;
    private final PangyaNettyServer netty;
    private final HealthHttp health;
    private final AuthServerConnector auth;
    private final GameAuthHandler authHandler;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Thread heartbeat;
    private final AuthShutdownScheduler shutdownScheduler;

    public GameRuntime(AppConfig config) {
        this(config, null, null);
    }

    /** Tests may override auth outbound when {@code authEnabled} is false. */
    GameRuntime(AppConfig config, AuthOutbound authOutOverride) {
        this(config, authOutOverride, null);
    }

    /** Tests may override shutdown scheduling to avoid stopping the JVM server. */
    GameRuntime(AppConfig config, AuthOutbound authOutOverride, IntConsumer shutdownOverride) {
        this.config = config;
        this.dataSource = DatabaseSupport.dataSource(config.jdbcUrl(), config.dbUser(), config.dbPassword());
        LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(dataSource));
        InventoryRepository inventory = new JdbiInventoryRepository(DatabaseSupport.jdbi(dataSource));
        this.redis = new SessionKeyStore(config.redisUri());
        this.sessions = new SessionManager(new IpDdosFilter());
        GameHandler handler = new GameHandler(config, repo, inventory, redis, sessions, GameHandler.loadChannels(config));
        AuthOutbound outbound;
        if (config.authEnabled()) {
            this.auth = new AuthServerConnector(config, repo::generateAuthServerKey);
            outbound = this.auth;
            this.authHandler = new GameAuthHandler(config, repo, sessions, outbound, handler);
            this.auth.setAuthInboundListener(authHandler::onAuthPacket);
            this.auth.start();
        } else {
            this.auth = null;
            outbound = authOutOverride != null
                    ? authOutOverride
                    : new AuthOutbound() {
                        @Override
                        public void sendInfoPlayerOnline(int reqServerUid, AuthS2s.AuthServerPlayerInfo info) {}
                    };
            this.authHandler = new GameAuthHandler(config, repo, sessions, outbound, handler);
        }
        this.netty = new PangyaNettyServer(ServerKind.GAME, sessions, handler::onPacket);
        this.netty.bind(config.port());
        handler.setBindPort(netty.localPort());
        AuthShutdownScheduler sched = null;
        if (shutdownOverride != null) {
            handler.setShutdownScheduler(shutdownOverride);
        } else {
            sched = new AuthShutdownScheduler(() -> {
                running.set(false);
                close();
            });
            handler.setShutdownScheduler(sched::schedule);
        }
        this.shutdownScheduler = sched;
        PangyaMetrics metrics = new PangyaMetrics(config.serverName(), sessions::size);
        this.health = new HealthHttp(config.healthPort(), config.serverName(), metrics);
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

    /** Integration tests invoke auth callbacks through the live session manager. */
    GameAuthHandler authHandler() {
        return authHandler;
    }

    /** Package tests reach {@link GameHandler} helpers (e.g. {@code reloadFiles}). */
    GameHandler gameHandler() {
        return authHandler.game();
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
        if (shutdownScheduler != null) {
            shutdownScheduler.close();
        }
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
