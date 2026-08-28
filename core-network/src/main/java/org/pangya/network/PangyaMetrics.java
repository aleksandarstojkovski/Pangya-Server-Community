package org.pangya.network;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntSupplier;

/** Prometheus scrape used by compose {@code /metrics} and S6 load evidence. */
public final class PangyaMetrics {

    private final PrometheusMeterRegistry registry;
    private final AtomicLong startedMs = new AtomicLong(System.currentTimeMillis());

    public PangyaMetrics(String serverName, IntSupplier sessions) {
        this.registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        Gauge.builder("pangya.sessions", sessions, IntSupplier::getAsInt)
                .description("Active Pangya TCP sessions")
                .tag("server", serverName)
                .register(registry);
        Gauge.builder("pangya.uptime.seconds", this, m -> (System.currentTimeMillis() - m.startedMs.get()) / 1000.0)
                .description("Process uptime")
                .tag("server", serverName)
                .register(registry);
    }

    public String scrape() {
        return registry.scrape();
    }
}
