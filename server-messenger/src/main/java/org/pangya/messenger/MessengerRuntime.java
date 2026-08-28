package org.pangya.messenger;

import com.zaxxer.hikari.HikariDataSource;
import org.pangya.db.DatabaseSupport;
import org.pangya.db.FriendRepository;
import org.pangya.db.JdbiFriendRepository;
import org.pangya.db.JdbiLoginRepository;
import org.pangya.db.LoginRepository;
import org.pangya.network.AppConfig;
import org.pangya.network.HealthHttp;
import org.pangya.network.PangyaMetrics;
import org.pangya.network.auth.AuthServerConnector;
import org.pangya.network.ddos.IpDdosFilter;
import org.pangya.protocol.packet.PacketIo;
import org.pangya.network.netty.PangyaNettyServer;
import org.pangya.network.netty.ServerKind;
import org.pangya.network.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

public final class MessengerRuntime implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(MessengerRuntime.class);

    private final HikariDataSource dataSource;
    private final PangyaNettyServer netty;
    private final HealthHttp health;
    private final AuthServerConnector auth;
    private final MessengerHandler handler;

    public MessengerRuntime(AppConfig config) {
        this.dataSource = DatabaseSupport.dataSource(config.jdbcUrl(), config.dbUser(), config.dbPassword());
        LoginRepository repo = new JdbiLoginRepository(DatabaseSupport.jdbi(dataSource));
        FriendRepository friends = new JdbiFriendRepository(DatabaseSupport.jdbi(dataSource));
        SessionManager sessions = new SessionManager(new IpDdosFilter());
        this.handler = new MessengerHandler(repo, friends, sessions);
        this.netty = new PangyaNettyServer(
                ServerKind.MESSENGER, sessions, handler::onPacket, PacketIo.DEFAULT_LOGIN_UID, handler::onDisconnect);
        this.netty.bind(config.port());
        PangyaMetrics metrics = new PangyaMetrics(config.serverName(), sessions::size);
        this.health = new HealthHttp(config.healthPort(), config.serverName(), metrics);
        if (config.authEnabled()) {
            this.auth = new AuthServerConnector(config, repo::generateAuthServerKey);
            this.auth.setAuthCommandListener(handler::onAuthCommand);
            this.auth.start();
        } else {
            this.auth = null;
        }
        log.info("messenger server uid={} port={}", config.uid(), config.port());
    }

    public int port() {
        return netty.localPort();
    }

    /** Integration tests invoke auth guild callbacks through the live session manager. */
    MessengerHandler handler() {
        return handler;
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
        try (MessengerRuntime ignored = new MessengerRuntime(config)) {
            CountDownLatch done = new CountDownLatch(1);
            Runtime.getRuntime().addShutdownHook(new Thread(done::countDown, "messenger-shutdown"));
            done.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
