package org.pangya.network.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * C# {@code Server.shutdown_time} / {@code PangyaSyncTimer m_shutdown} used by
 * {@code authCmdShutdown} on child servers.
 */
public final class AuthShutdownScheduler implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(AuthShutdownScheduler.class);

    private final Runnable onShutdown;
    private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(
            r -> Thread.ofVirtual().name("auth-shutdown").unstarted(r));
    private volatile ScheduledFuture<?> future;

    public AuthShutdownScheduler(Runnable onShutdown) {
        this.onShutdown = onShutdown;
    }

    public void schedule(int timeSec) {
        if (timeSec <= 0) {
            log.warn("auth shutdown immediate");
            onShutdown.run();
            return;
        }
        synchronized (this) {
            if (future != null && !future.isDone()) {
                log.warn("auth shutdown timer already active, ignoring {} sec request", timeSec);
                return;
            }
            log.warn("auth shutdown scheduled in {} sec", timeSec);
            future = exec.schedule(onShutdown, timeSec, TimeUnit.SECONDS);
        }
    }

    @Override
    public void close() {
        if (future != null) {
            future.cancel(false);
        }
        exec.shutdownNow();
    }
}
