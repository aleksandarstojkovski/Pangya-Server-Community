package org.pangya.login;

import com.zaxxer.hikari.HikariDataSource;
import org.pangya.db.DatabaseSupport;
import org.pangya.db.JdbiLoginRepository;
import org.pangya.db.LoginRepository;
import org.pangya.network.AppConfig;
import org.pangya.network.HealthHttp;
import org.pangya.network.PangyaMetrics;
import org.pangya.network.auth.AuthOutbound;
import org.pangya.network.auth.AuthServerConnector;
import org.pangya.network.auth.AuthShutdownScheduler;
import org.pangya.network.ddos.IpDdosFilter;
import org.pangya.network.netty.PangyaNettyServer;
import org.pangya.network.netty.ServerKind;
import org.pangya.network.redis.SessionKeyStore;
import org.pangya.network.session.SessionManager;
import org.pangya.protocol.auth.AuthS2s;
import org.pangya.protocol.iff.PangyaIffLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntConsumer;

public final class LoginRuntime implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(LoginRuntime.class);

    private final AppConfig config;
    private final HikariDataSource dataSource;
    private final SessionKeyStore redis;
    private final PangyaNettyServer netty;
    private final HealthHttp health;
    private final AuthServerConnector auth;
    private final LoginHandler handler;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Thread heartbeat;
    private final AuthShutdownScheduler shutdownScheduler;

    public LoginRuntime(AppConfig config) {
        this(config, null, null);
    }

    /** Tests may override auth outbound when {@code authEnabled} is false. */
    LoginRuntime(AppConfig config, AuthOutbound authOutOverride) {
        this(config, authOutOverride, null);
    }

    /** Tests may override shutdown scheduling to avoid stopping the IT runtime. */
    LoginRuntime(AppConfig config, AuthOutbound authOutOverride, IntConsumer shutdownOverride) {
        this.config = config;
        if (!config.pangyaIffPath().isBlank()) {
            PangyaIffLoader.reload(java.nio.file.Path.of(config.pangyaIffPath()));
        }
        this.dataSource = DatabaseSupport.dataSource(config.jdbcUrl(), config.dbUser(), config.dbPassword());
        LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(dataSource));
        this.redis = new SessionKeyStore(config.redisUri());
        SessionManager sessions = new SessionManager(new IpDdosFilter());
        AuthOutbound outbound;
        if (config.authEnabled()) {
            this.auth = new AuthServerConnector(config, repo::generateAuthServerKey);
            outbound = this.auth;
            this.handler = new LoginHandler(config, repo, redis, sessions, outbound);
            this.auth.setAuthInboundListener(handler::onAuthPacket);
            this.auth.start();
        } else {
            this.auth = null;
            outbound = authOutOverride != null
                    ? authOutOverride
                    : new AuthOutbound() {
                        @Override
                        public void sendInfoPlayerOnline(int reqServerUid, AuthS2s.AuthServerPlayerInfo info) {}
                    };
            this.handler = new LoginHandler(config, repo, redis, sessions, outbound);
        }
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
        this.netty = new PangyaNettyServer(ServerKind.LOGIN, sessions, handler::onPacket, config.uid());
        this.netty.bind(config.port());
        PangyaMetrics metrics = new PangyaMetrics(config.serverName(), sessions::size);
        this.health = new HealthHttp(config.healthPort(), config.serverName(), metrics);
        heartbeatOnce(repo);
        this.heartbeat = Thread.ofVirtual().name("login-heartbeat").start(() -> heartbeatLoop(repo));
        log.info("login server uid={} port={}", config.uid(), config.port());
    }

    public int port() {
        return netty.localPort();
    }

    public AuthServerConnector auth() {
        return auth;
    }

    LoginHandler handler() {
        return handler;
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
            upsert(repo, config.serverName(), config.uid(), config.advertisedIp(), netty.localPort(),
                    0, config.maxUser(), config.property(), config.version(), config.clientVersion());
            Map<String, Object> game = config.section("game");
            if (!game.isEmpty()) {
                upsert(repo,
                        config.nestedFrom(game, "name", "Kuma(2.0)"),
                        config.nestedIntFrom(game, "uid", 20202),
                        config.nestedFrom(game, "ip", config.advertisedIp()),
                        config.nestedIntFrom(game, "port", 20202),
                        1,
                        config.nestedIntFrom(game, "maxUser", 2001),
                        config.nestedIntFrom(game, "property", 2048),
                        config.nestedFrom(game, "version", "Release.JP.983.00"),
                        config.nestedFrom(game, "clientVersion", "JP.R7.983.00"));
            }
            Map<String, Object> msn = config.section("messenger");
            if (!msn.isEmpty()) {
                upsert(repo,
                        config.nestedFrom(msn, "name", "Messenger Server"),
                        config.nestedIntFrom(msn, "uid", 30201),
                        config.nestedFrom(msn, "ip", config.advertisedIp()),
                        config.nestedIntFrom(msn, "port", 30201),
                        3,
                        config.nestedIntFrom(msn, "maxUser", 2001),
                        config.nestedIntFrom(msn, "property", 4096),
                        config.nestedFrom(msn, "version", "MS.Release.2.0"),
                        config.nestedFrom(msn, "clientVersion", "JP.R7.983.01"));
            }
        } catch (RuntimeException e) {
            log.warn("server-list heartbeat: {}", e.toString());
        }
    }

    private static void upsert(
            LoginRepository repo,
            String name,
            int uid,
            String ip,
            int port,
            int type,
            int maxUser,
            int property,
            String version,
            String clientVersion) {
        repo.upsertServer(new LoginRepository.ServerListRow(
                name, uid, ip, port, maxUser, 0, type, property,
                0, 0, (short) 0, (short) 0, (short) 0, (short) 0, version, clientVersion));
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
        try (LoginRuntime ignored = new LoginRuntime(config)) {
            CountDownLatch done = new CountDownLatch(1);
            Runtime.getRuntime().addShutdownHook(new Thread(done::countDown, "login-shutdown"));
            done.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
