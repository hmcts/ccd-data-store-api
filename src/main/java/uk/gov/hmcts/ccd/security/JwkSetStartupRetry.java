package uk.gov.hmcts.ccd.security;

import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Retries the JWK set retrieval in the background until a key set with a usable signing key
 * ({@link JwkSourceConfiguration#USABLE_SIGNING_KEYS}) has been retrieved and stops for good after that.
 *
 * <p>An instance whose start-up retrieval failed is not ready ({@link IdamJwksHealthIndicator}), so it gets no
 * requests, and with nothing cached, the refresh-ahead schedule is not armed. Without this nothing would retrieve the
 * keys again, and the instance would never become ready.
 *
 * <p>It goes through the same {@code idamJwkSource} as token verification, so the first success fills the cache,
 * arms the scheduled refresh, and logs the recovery after the start-up fallback, exactly as a request would. Retries
 * run one at a time, {@link JwksProperties#startupRetryIntervalMs()} apart. The rate limiter can occasionally refuse
 * an attempt, for example the first one after a start-up that got a key set with no usable signing key, or one after
 * an attempt that loaded twice; the next one then gets through. It does not start at all when the start-up retrieval
 * got a usable signing key.
 *
 * <p>It selects with the same matcher as readiness. A key set already cached with no usable signing key, for example,
 * an empty one or one with only {@code enc} keys, is then a miss, and Nimbus refreshes it from IDAM instead of
 * returning it.
 *
 * <p>Logging per attempt:
 * <ul>
 *     <li>retrieval failed: DEBUG here, and the one WARN that the health layer logs for each failed retrieval;</li>
 *     <li>IDAM returned a key set with no usable signing key: one WARN here, since nothing else reports it;</li>
 *     <li>otherwise (for example, refused by the rate limiter): DEBUG only.</li>
 * </ul>
 * The start-up fallback WARN says that this retry is running.
 */
@Slf4j
public class JwkSetStartupRetry implements SmartLifecycle {

    private static final JWKSelector USABLE_SIGNING_KEY = new JWKSelector(JwkSourceConfiguration.USABLE_SIGNING_KEYS);

    private final JWKSource<SecurityContext> jwkSource;
    private final JwkSourceTelemetry telemetry;
    private final long intervalMs;

    private ScheduledExecutorService executor;
    // Only the retry thread reads or writes it.
    private boolean errorLogged;

    public JwkSetStartupRetry(JWKSource<SecurityContext> jwkSource, JwkSourceTelemetry telemetry, long intervalMs) {
        this.jwkSource = jwkSource;
        this.telemetry = telemetry;
        this.intervalMs = intervalMs;
    }

    /**
     * Called once the application context has started, after the start-up retrieval in
     * {@link JwkSourceConfiguration} has run.
     *
     * <p>One-shot: once stopped, by {@link #stop()} or after a usable signing key, an instance does not start again.
     */
    @Override
    public synchronized void start() {
        if (executor != null || telemetry.hasRetrievedUsableSigningKey()) {
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "idam-jwks-startup-retry");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(this::retry, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
        log.debug("No usable IDAM signing key retrieved at start-up; retrying every {}ms in the background",
            intervalMs);
    }

    /**
     * Stops retrying. A retry already in progress is interrupted but runs until its retrieval returns, which can be
     * well past the configured timeouts: they bound each read, not the whole retrieval. The thread is a daemon, so it
     * cannot hold up the JVM's exit.
     */
    @Override
    public synchronized void stop() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    /**
     * True from {@link #start()} until the retry thread has finished, after a usable signing key or {@link #stop()}.
     */
    @Override
    public synchronized boolean isRunning() {
        return executor != null && !executor.isTerminated();
    }

    private void retry() {
        if (!telemetry.hasRetrievedUsableSigningKey()) {
            long keySetsBefore = telemetry.keySetsRetrieved();
            try {
                jwkSource.get(USABLE_SIGNING_KEY, null);
            } catch (Throwable e) {
                if (e instanceof Error && !errorLogged) {
                    errorLogged = true;
                    log.warn("Background IDAM JWK set retrieval threw {}; still retrying every {}ms", e,
                        intervalMs);
                } else {
                    log.debug("Background IDAM JWK set retrieval failed; retrying in {}ms: {}", intervalMs,
                        e.toString());
                }
            }
            if (!telemetry.hasRetrievedUsableSigningKey() && telemetry.keySetsRetrieved() != keySetsBefore) {
                log.warn("IDAM JWK set contains no usable signing key; instance stays not ready. Retrying in {}ms",
                    intervalMs);
            }
        }
        if (telemetry.hasRetrievedUsableSigningKey()) {
            log.debug("Usable IDAM signing key retrieved; background retrieval stopped");
            stopRetrying();
        }
    }

    private synchronized void stopRetrying() {
        // Shutting down cancels the periodic task, so this run is the last one.
        executor.shutdown();
    }
}
