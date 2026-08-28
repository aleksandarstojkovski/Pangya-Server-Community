package org.pangya.ranking;

import com.zaxxer.hikari.HikariDataSource;
import org.pangya.db.DatabaseSupport;
import org.pangya.db.JdbiLoginRepository;
import org.pangya.db.JdbiRankRepository;
import org.pangya.db.LoginRepository;
import org.pangya.db.RankRepository;
import org.pangya.network.AppConfig;
import org.pangya.network.HealthHttp;
import org.pangya.network.PangyaMetrics;
import org.pangya.network.auth.AuthOutbound;
import org.pangya.network.auth.AuthServerConnector;
import org.pangya.network.ddos.IpDdosFilter;
import org.pangya.network.netty.PangyaNettyServer;
import org.pangya.network.netty.ServerKind;
import org.pangya.network.session.SessionManager;
import org.pangya.protocol.auth.AuthS2s;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

public final class RankingRuntime implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(RankingRuntime.class);

    private final HikariDataSource dataSource;
    private final PangyaNettyServer netty;
    private final HealthHttp health;
    private final AuthServerConnector auth;
    private final RankingAuthHandler authHandler;

    public RankingRuntime(AppConfig config) {
        this(config, null);
    }

    /** Tests may override auth outbound when {@code authEnabled} is false. */
    RankingRuntime(AppConfig config, AuthOutbound authOutOverride) {
        this.dataSource = DatabaseSupport.dataSource(config.jdbcUrl(), config.dbUser(), config.dbPassword());
        LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(dataSource));
        RankRepository ranks = new JdbiRankRepository(DatabaseSupport.jdbi(dataSource));
        SessionManager sessions = new SessionManager(new IpDdosFilter());
        RankingHandler handler = new RankingHandler(repo, ranks, sessions);
        AuthOutbound outbound;
        if (config.authEnabled()) {
            this.auth = new AuthServerConnector(config, repo::generateAuthServerKey);
            outbound = this.auth;
            this.authHandler = new RankingAuthHandler(config, repo, sessions, outbound);
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
            this.authHandler = new RankingAuthHandler(config, repo, sessions, outbound);
        }
        this.netty = new PangyaNettyServer(ServerKind.RANKING, sessions, handler::onPacket);
        this.netty.bind(config.port());
        PangyaMetrics metrics = new PangyaMetrics(config.serverName(), sessions::size);
        this.health = new HealthHttp(config.healthPort(), config.serverName(), metrics);
        log.info("ranking server uid={} port={}", config.uid(), config.port());
    }

    public int port() {
        return netty.localPort();
    }

    RankingAuthHandler authHandler() {
        return authHandler;
    }

    @Override
    public void close() {
        if (auth != null) {
            auth.close();
        }
        health.close();
        netty.close();
        dataSource.close();
    }

    public static void runBlocking(AppConfig config) {
        try (RankingRuntime ignored = new RankingRuntime(config)) {
            CountDownLatch done = new CountDownLatch(1);
            Runtime.getRuntime().addShutdownHook(new Thread(done::countDown, "ranking-shutdown"));
            done.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
