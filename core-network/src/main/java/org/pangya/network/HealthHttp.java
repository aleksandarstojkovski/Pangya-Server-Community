package org.pangya.network;

import com.sun.net.httpserver.HttpServer;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/** HTTP {@code /health} used by compose healthchecks; {@code /metrics} is Prometheus text. */
public final class HealthHttp implements AutoCloseable {

    private final HttpServer http;

    public HealthHttp(int port, String name) {
        this(port, name, null);
    }

    public HealthHttp(int port, String name, PangyaMetrics metrics) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
            server.createContext("/health", exchange -> {
                byte[] body = ("ok " + name).getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });
            if (metrics != null) {
                server.createContext("/metrics", exchange -> {
                    byte[] body = metrics.scrape().getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "text/plain; version=0.0.4; charset=utf-8");
                    exchange.sendResponseHeaders(200, body.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(body);
                    }
                });
            }
            server.start();
            this.http = server;
        } catch (Exception e) {
            throw new IllegalStateException("failed to bind health port " + port, e);
        }
    }

    @Override
    public void close() {
        http.stop(0);
    }
}
