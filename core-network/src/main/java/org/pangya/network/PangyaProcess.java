package org.pangya.network;

import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * S0 process stub: bind the Pangya TCP port and an HTTP health endpoint.
 * Game protocol is implemented from S1 onward; this only proves compose + healthchecks.
 */
public final class PangyaProcess implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(PangyaProcess.class);

    private final AtomicBoolean running = new AtomicBoolean(true);
    private final CountDownLatch bound = new CountDownLatch(1);
    private volatile ServerSocket serverSocket;
    private volatile HttpServer http;

    public static PangyaProcess start(AppConfig config) {
        PangyaProcess process = new PangyaProcess();
        process.bind(config);
        return process;
    }

    public static void runBlocking(AppConfig config) {
        try (PangyaProcess ignored = start(config)) {
            CountDownLatch done = new CountDownLatch(1);
            Runtime.getRuntime().addShutdownHook(new Thread(done::countDown, "pangya-shutdown"));
            done.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void bind(AppConfig config) {
        int port = config.port();
        int healthPort = config.healthPort();
        try {
            ServerSocket ss = new ServerSocket();
            ss.setReuseAddress(true);
            ss.bind(new InetSocketAddress("0.0.0.0", port));
            this.serverSocket = ss;
            Thread.ofVirtual().name("pangya-accept-" + config.serverName()).start(this::acceptLoop);
            HttpServer httpServer = HttpServer.create(new InetSocketAddress("0.0.0.0", healthPort), 0);
            httpServer.createContext("/health", exchange -> {
                byte[] body = ("ok " + config.serverName()).getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });
            httpServer.start();
            this.http = httpServer;
            bound.countDown();
            log.info("pangya process started name={} port={} healthPort={}", config.serverName(), port, healthPort);
        } catch (Exception e) {
            bound.countDown();
            throw new IllegalStateException("failed to start " + config.serverName(), e);
        }
    }

    public boolean awaitBound(long timeout, TimeUnit unit) throws InterruptedException {
        return bound.await(timeout, unit);
    }

    private void acceptLoop() {
        ServerSocket ss = serverSocket;
        while (running.get() && ss != null && !ss.isClosed()) {
            try {
                Socket client = ss.accept();
                client.close();
            } catch (Exception e) {
                if (running.get()) {
                    log.debug("accept ended: {}", e.toString());
                }
                break;
            }
        }
    }

    @Override
    public void close() {
        running.set(false);
        if (http != null) {
            http.stop(0);
        }
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (Exception ignored) {
                // closing
            }
        }
    }
}
