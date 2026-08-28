package org.pangya.network;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PangyaProcessTest {

    @Test
    void healthEndpointReturnsOk() throws Exception {
        int health = freePort();
        int port = freePort();
        AppConfig config = new AppConfig(Map.of(
                "server", Map.of(
                        "name", "test",
                        "port", port,
                        "healthPort", health
                )
        ));
        try (PangyaProcess process = PangyaProcess.start(config)) {
            assertTrue(process.awaitBound(5, TimeUnit.SECONDS));
            HttpResponse<String> response = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + health + "/health"))
                            .GET()
                            .timeout(Duration.ofSeconds(2))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            assertEquals("ok test", response.body());
        }
    }

    @Test
    void yamlDefaultsLoad() {
        AppConfig config = AppConfig.load("does-not-exist.yml");
        assertEquals("pangya", config.serverName());
    }

    private static int freePort() throws Exception {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
