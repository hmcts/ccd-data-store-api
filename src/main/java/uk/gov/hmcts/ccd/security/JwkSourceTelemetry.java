package uk.gov.hmcts.ccd.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.CachingJWKSetSource;
import com.nimbusds.jose.jwk.source.JWKSetSourceWithHealthStatusReporting;
import com.nimbusds.jose.jwk.source.OutageTolerantJWKSetSource;
import com.nimbusds.jose.jwk.source.RateLimitedJWKSetSource;
import com.nimbusds.jose.jwk.source.RefreshAheadCachingJWKSetSource;
import com.nimbusds.jose.jwk.source.RetryingJWKSetSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.events.Event;
import com.nimbusds.jose.util.events.EventListener;
import com.nimbusds.jose.util.health.HealthReportListener;
import com.nimbusds.jose.util.health.HealthStatus;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.ccd.appinsights.AppInsights;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Publishes Nimbus JWK set source lifecycle events to Application Insights.
 *
 * <p>The JWK set source continues to serve the last retrieved keys during an IDAM outage, so requests
 * can continue to succeed even though IDAM is unavailable. This behaviour is intentionally bounded:
 * once the outage tolerance window expires, authentication can no longer be completed using the stale keys.
 *
 * <p>This class makes that state visible in telemetry, including how much of the outage tolerance window
 * remains, rather than only reporting a problem after the window has expired.
 *
 * <p>All events are published as a single {@code IDAM_JWKS} custom event and distinguished by the
 * {@code jwksEvent} dimension. This allows the whole JWK source to be monitored through a single query.
 * For {@code OUTAGE_TOLERATED}, {@code remainingToleranceMs} shows how long stale keys can continue to
 * be served.
 *
 * <p>{@code threadQueueLength} is also reported for {@code WAITING_FOR_REFRESH}, {@code REFRESH_COMPLETED}
 * and {@code REFRESH_TIMED_OUT}. These events represent the lifecycle of retrieval that can cause
 * request threads to wait. The value shows how much request traffic is being held up by a slow IDAM
 * retrieval, which is the failure mode addressed by CCD-8077.
 *
 * <p>Every event also carries {@code algorithmSource}, which records how the accepted signature algorithms were
 * chosen at start-up (see {@link AlgorithmSource}). The choice is fixed for the lifetime of the JVM, so carrying
 * it on every event, including the routine scheduled refreshes, keeps an instance running on the fallback set
 * visible for as long as it runs, not just in the minutes after it started.
 *
 * <p><b>Sampling.</b> The App Insights agent drops an event raised inside a request's trace when that request is
 * sampled out, and Nimbus's refresh executors inherit the trace of whichever thread last scheduled a refresh, which
 * can be a request. The events are therefore sent in two ways:
 * <ul>
 *     <li><b>State events</b> ({@code OUTAGE_TOLERATED}, {@code OUTAGE_ENDED}, {@code HEALTH}, {@code RETRIAL},
 *         {@code REFRESH_COMPLETED}, {@code SCHEDULED_REFRESH_COMPLETED}, {@code SCHEDULED_REFRESH_FAILED},
 *         {@code REFRESH_NOT_SCHEDULED}, {@code ALGORITHMS_FALLBACK}) are sent standalone through
 *         {@link AppInsights#trackStandaloneEvent}, so the monitoring queries see every one of them. Each is raised
 *         at or below the rate limiter, only when retrieval passes it, or once per scheduled refresh, so their rate
 *         is bounded (about two retrievals per rate limit interval) whatever the request rate. The trace they were
 *         raised in, if any, is kept as {@code triggeringOperationId}. They are kept at 100% only while the agent
 *         has no fixed {@code sampling.percentage}; if one is set, they are sampled at it.</li>
 *     <li><b>Per-request events</b> ({@code RATE_LIMIT_REACHED}, {@code WAITING_FOR_REFRESH},
 *         {@code REFRESH_TIMED_OUT}, {@code UNABLE_TO_REFRESH}, {@code UNABLE_TO_REFRESH_AHEAD}) are raised above
 *         the rate limiter and can fire once per request during an incident. They stay in the request's trace and
 *         are sampled with it. Sending them standalone would send one unsampled event per request.</li>
 * </ul>
 *
 * <p><b>Log levels.</b> While IDAM is healthy, nothing is logged above DEBUG apart from start-up. During an outage,
 *  failed retrieval logs at most one WARN, the outage WARN, and nothing above DEBUG is logged per request: each of
 * those requests fails with a 500 that {@code ExceptionHandlingFilter} logs, and the event above records it. The
 * end of an outage, and the first retrieval after a start-up fallback, are logged at INFO.
 */
@Slf4j
public class JwkSourceTelemetry {

    public static final String EVENT_NAME = "IDAM_JWKS";

    static final String EVENT_TYPE = "jwksEvent";
    static final String DETAIL = "detail";
    static final String EXCEPTION_TYPE = "exceptionType";
    static final String HEALTH_STATUS = "healthStatus";
    static final String REMAINING_TOLERANCE_MS = "remainingToleranceMs";
    static final String THREAD_QUEUE_LENGTH = "threadQueueLength";
    static final String ALGORITHM_SOURCE = "algorithmSource";
    static final String ALGORITHMS = "algorithms";

    static final String OUTAGE_TOLERATED = "OUTAGE_TOLERATED";
    static final String OUTAGE_ENDED = "OUTAGE_ENDED";
    static final String RATE_LIMIT_REACHED = "RATE_LIMIT_REACHED";
    static final String RETRIAL = "RETRIAL";
    static final String HEALTH = "HEALTH";
    static final String REFRESH_TIMED_OUT = "REFRESH_TIMED_OUT";
    static final String UNABLE_TO_REFRESH = "UNABLE_TO_REFRESH";
    static final String UNABLE_TO_REFRESH_AHEAD = "UNABLE_TO_REFRESH_AHEAD";
    static final String SCHEDULED_REFRESH_FAILED = "SCHEDULED_REFRESH_FAILED";
    static final String SCHEDULED_REFRESH_COMPLETED = "SCHEDULED_REFRESH_COMPLETED";
    static final String REFRESH_COMPLETED = "REFRESH_COMPLETED";
    static final String WAITING_FOR_REFRESH = "WAITING_FOR_REFRESH";
    static final String REFRESH_NOT_SCHEDULED = "REFRESH_NOT_SCHEDULED";
    static final String ALGORITHMS_FALLBACK = "ALGORITHMS_FALLBACK";

    private static final String NONE = "NONE";

    private static final long NOT_IN_OUTAGE = -1L;

    private static final int MAX_DETAIL_LENGTH = 256;

    /**
     * How the signature algorithms accepted by the key selector were chosen at start-up.
     */
    public enum AlgorithmSource {
        /** Start-up has not yet chosen the algorithms; only seen on events raised by the start-up retrieval. */
        PENDING,
        /** Derived from the key set IDAM returned at start-up. */
        DERIVED,
        /** The key set could not be used at start-up, so a fixed set is in force until the JVM restarts. */
        FALLBACK
    }

    private final AppInsights appInsights;

    private final Clock clock;

    /**
     * When the outage-tolerant layer stops serving the cached keys, in epoch milliseconds, or
     * {@link #NOT_IN_OUTAGE} once IDAM has supplied a key set. Stored as an instant rather than as the
     * remaining time because no further outage event arrives once the window has closed, so the remaining time
     * would never be corrected.
     */
    private final AtomicLong toleranceExpiresAtMs = new AtomicLong(NOT_IN_OUTAGE);

    private volatile AlgorithmSource algorithmSource = AlgorithmSource.PENDING;

    /**
     * The fallback algorithms, set when start-up fell back because the key set could not be retrieved and cleared by
     * the first retrieval that succeeds after it. Nothing else reports that recovery: the outage cache was empty, so
     * no {@code OUTAGE_ENDED} follows.
     */
    private final AtomicReference<String> recoveryPendingAlgorithms = new AtomicReference<>();

    public JwkSourceTelemetry(AppInsights appInsights) {
        // Nimbus measures the outage window with System.currentTimeMillis(), so the system clock is used here
        // rather than an application Clock bean that a test could replace.
        this(appInsights, Clock.systemUTC());
    }

    JwkSourceTelemetry(AppInsights appInsights, Clock clock) {
        this.appInsights = appInsights;
        this.clock = clock;
    }

    /**
     * Returns how long the outage-tolerant layer will continue to serve the cached keys, calculated when called:
     * <ul>
     *     <li>{@code -1} when IDAM has supplied a key set since the last outage, or no outage has occurred;</li>
     *     <li>{@code 0} when the outage continues but the tolerance window has closed;</li>
     *     <li>otherwise the time remaining, in milliseconds.</li>
     * </ul>
     *
     * <p>The expiry is taken as the time the outage event arrived plus the remaining time it reported. Nimbus
     * measures that remaining time from the start of the failed call, so the expiry can be late by up to the
     * duration of one retrieval, including its retry.
     */
    public long remainingToleranceMs() {
        long expiresAt = toleranceExpiresAtMs.get();
        return expiresAt == NOT_IN_OUTAGE ? NOT_IN_OUTAGE : Math.max(0L, expiresAt - clock.millis());
    }

    public AlgorithmSource algorithmSource() {
        return algorithmSource;
    }

    /**
     * Called by {@link JwkSourceConfiguration} when the accepted algorithms were derived from IDAM's key set.
     */
    void algorithmsDerived(Set<JWSAlgorithm> algorithms) {
        algorithmSource = AlgorithmSource.DERIVED;
        log.info("Accepting IDAM token signature algorithms {} derived from the JWK set", names(algorithms));
    }

    /**
     * Called by {@link JwkSourceConfiguration} when start-up could not derive the accepted algorithms and the
     * fixed fallback set is in force for the lifetime of the JVM.
     *
     * @param cause the retrieval failure, or {@code null} when IDAM returned a key set without usable keys
     */
    void algorithmsFellBack(Set<JWSAlgorithm> algorithms, Exception cause) {
        algorithmSource = AlgorithmSource.FALLBACK;
        if (cause != null) {
            recoveryPendingAlgorithms.set(names(algorithms));
        }

        Map<String, String> dimensions = new LinkedHashMap<>();
        dimensions.put(ALGORITHMS, names(algorithms));
        dimensions.putAll(cause == null
            ? Map.of(EXCEPTION_TYPE, NONE, DETAIL, "IDAM JWK set advertised no usable signature algorithms")
            : causeOf(cause));

        trackState(ALGORITHMS_FALLBACK, dimensions, Map.of());
    }

    /**
     * Called by {@link ObservedJWKSetSource} when IDAM returns a key set that parses.
     * This confirms that the keys were freshly retrieved from IDAM rather than served from the outage cache.
     * Nimbus listeners cannot make this distinction because they sit above the outage-tolerant layer and see
     * both cases as a successful result.
     *
     * <p>The first success after a start-up fallback caused by a failed retrieval is logged once. The accepted
     * algorithms stay the fallback set, so the log says so.
     */
    void retrievalSucceeded() {
        String fallbackAlgorithms = recoveryPendingAlgorithms.getAndSet(null);
        if (fallbackAlgorithms != null) {
            log.info("IDAM JWK set retrieved; key source recovered after start-up fallback. Accepted algorithms "
                + "remain the RSA family {} for this instance", fallbackAlgorithms);
        }

        long previous = toleranceExpiresAtMs.getAndSet(NOT_IN_OUTAGE);
        if (previous != NOT_IN_OUTAGE) {
            log.info("IDAM JWK set retrieved successfully; no longer serving stale signing keys");
            trackState(OUTAGE_ENDED, Map.of(), Map.of());
        }
    }

    /**
     * Emitted when IDAM is unavailable and the previously retrieved keys are served from the outage cache.
     * The event is emitted for each fallback and includes the time remaining before the cached keys expire.
     */
    public EventListener<OutageTolerantJWKSetSource<SecurityContext>, SecurityContext> outageEventListener() {
        return event -> {
            if (event instanceof OutageTolerantJWKSetSource.OutageEvent<SecurityContext> outage) {
                long remaining = outage.getRemainingTime();
                toleranceExpiresAtMs.set(clock.millis() + remaining);

                log.warn("IDAM JWK set unavailable; serving cached signing keys for a further {}ms. Cause: {}",
                    remaining, outage.getException().toString());

                trackState(OUTAGE_TOLERATED, causeOf(outage.getException()),
                    Map.of(REMAINING_TOLERANCE_MS, (double) remaining));
            } else {
                trackUnknown(event);
            }
        };
    }

    /**
     * Emitted when retrieval is refused because another retrieval occurred within the rate limit interval.
     * Nimbus raises a {@code RateLimitReachedException} outside the outage-tolerant layer, so the outage cache
     * does not handle it, and the exception is propagated to the caller.
     */
    public EventListener<RateLimitedJWKSetSource<SecurityContext>, SecurityContext> rateLimitedEventListener() {
        return event -> {
            if (event instanceof RateLimitedJWKSetSource.RateLimitedEvent) {
                log.debug("IDAM JWK set retrieval refused by the rate limiter; the caller will see a failure "
                    + "rather than the cached keys");
                trackPerRequest(RATE_LIMIT_REACHED, Map.of(), Map.of());
            } else {
                trackUnknown(event);
            }
        };
    }

    /**
     * Reports the cache lifecycle events that are useful for monitoring. Error events and the events that
     * indicate refresh-ahead activity are reported, while routine refresh events are only logged at trace
     * level to avoid unnecessary telemetry.
     *
     * <p>All events currently supported by the two sources are handled explicitly. Less relevant events
     * are sent to trace logging rather than being ignored. This also means that if a future Nimbus release adds
     * an event, and it is not added here, it will reach the {@code default} branch and remain visible in the logs.
     */
    public EventListener<CachingJWKSetSource<SecurityContext>, SecurityContext> cachingEventListener() {
        return event -> {
            switch (event) {
                // The production symptom: a request thread gave up waiting for another thread's retrieval.
                case CachingJWKSetSource.RefreshTimedOutEvent<SecurityContext> timedOut -> {
                    log.debug("Timed out waiting for an in-flight IDAM JWK set retrieval; {} thread(s) queued",
                        timedOut.getThreadQueueLength());
                    trackPerRequest(REFRESH_TIMED_OUT, Map.of(), threadQueue(timedOut.getThreadQueueLength()));
                }
                case CachingJWKSetSource.WaitingForRefreshEvent<SecurityContext> waiting -> {
                    log.debug("Waiting on an in-flight IDAM JWK set retrieval; {} thread(s) queued",
                        waiting.getThreadQueueLength());
                    trackPerRequest(WAITING_FOR_REFRESH, Map.of(), threadQueue(waiting.getThreadQueueLength()));
                }
                case CachingJWKSetSource.UnableToRefreshEvent<SecurityContext> ignored -> {
                    log.debug("Unable to refresh the IDAM JWK set cache");
                    trackPerRequest(UNABLE_TO_REFRESH, Map.of(), Map.of());
                }
                case RefreshAheadCachingJWKSetSource.UnableToRefreshAheadOfExpirationEvent<SecurityContext>
                    ignored -> {
                    log.debug("Unable to refresh the IDAM JWK set ahead of expiry; the cache will be refreshed on "
                        + "the request path instead");
                    trackPerRequest(UNABLE_TO_REFRESH_AHEAD, Map.of(), Map.of());
                }
                case RefreshAheadCachingJWKSetSource.ScheduledRefreshFailed<SecurityContext> failed -> {
                    log.warn("Scheduled IDAM JWK set refresh failed: {}", summary(failed.getException()));
                    trackState(SCHEDULED_REFRESH_FAILED, causeOf(failed.getException()), Map.of());
                }
                case RefreshAheadCachingJWKSetSource.ScheduledRefreshCompletedEvent<SecurityContext> ignored -> {
                    log.debug("Scheduled IDAM JWK set refresh completed");
                    trackState(SCHEDULED_REFRESH_COMPLETED, Map.of(), Map.of());
                }
                case RefreshAheadCachingJWKSetSource.RefreshNotScheduledEvent<SecurityContext> ignored -> {
                    log.debug("No IDAM JWK set refresh scheduled ahead of expiry");
                    trackState(REFRESH_NOT_SCHEDULED, Map.of(), Map.of());
                }
                case CachingJWKSetSource.RefreshCompletedEvent<SecurityContext> completed -> {
                    log.debug("IDAM JWK set cache refreshed; {} thread(s) queued",
                        completed.getThreadQueueLength());
                    trackState(REFRESH_COMPLETED, Map.of(), threadQueue(completed.getThreadQueueLength()));
                }
                case CachingJWKSetSource.RefreshInitiatedEvent<SecurityContext> ignored ->
                    log.trace("IDAM JWK set cache refresh initiated");
                case RefreshAheadCachingJWKSetSource.RefreshScheduledEvent<SecurityContext> ignored ->
                    log.trace("IDAM JWK set refresh scheduled ahead of expiry");
                case RefreshAheadCachingJWKSetSource.ScheduledRefreshInitiatedEvent<SecurityContext> ignored ->
                    log.trace("Scheduled IDAM JWK set refresh initiated");
                case null, default -> trackUnknown(event);
            }
        };
    }

    /**
     * Reports whether the JWK source can currently produce a key set.
     *
     * <p>This does not indicate whether IDAM was reachable. Because the health layer sits above
     * {@code OutageTolerantJWKSetSource}, it remains HEALTHY while stale keys are being served.
     * {@link #OUTAGE_TOLERATED} is therefore the leading indicator; this is the lagging one.
     */
    public HealthReportListener<JWKSetSourceWithHealthStatusReporting<SecurityContext>, SecurityContext>
        healthReportListener() {

        return report -> {
            boolean healthy = HealthStatus.HEALTHY.equals(report.getHealthStatus());
            Exception exception = report.getException();

            if (healthy) {
                log.debug("IDAM JWK set endpoint healthy");
            } else {
                log.warn("IDAM JWK set endpoint unhealthy: {}", summary(exception));
            }

            Map<String, String> dimensions = new LinkedHashMap<>();
            dimensions.put(HEALTH_STATUS, String.valueOf(report.getHealthStatus()));
            dimensions.putAll(causeOf(exception));

            trackState(HEALTH, dimensions, Map.of());
        };
    }

    public EventListener<RetryingJWKSetSource<SecurityContext>, SecurityContext> retryingEventListener() {
        return event -> {
            if (event instanceof RetryingJWKSetSource.RetrialEvent<SecurityContext> retrial) {
                log.debug("Retrying IDAM JWK set retrieval after {}", retrial.getException().toString());
                trackState(RETRIAL, causeOf(retrial.getException()), Map.of());
            } else {
                trackUnknown(event);
            }
        };
    }

    private void trackUnknown(Event<?, ?> event) {
        log.debug("Unhandled IDAM JWK set event {}", event == null ? NONE : event.getClass().getSimpleName());
    }

    /**
     * Tracks an event that describes the state of the key source. Its rate is bounded without sampling, so it is
     * sent standalone and is not dropped with the request that happened to raise it. See the class comment.
     */
    private void trackState(String type, Map<String, String> dimensions, Map<String, Double> measurements) {
        track(type, dimensions, measurements, true);
    }

    /**
     * Tracks an event that can be raised once per request during an incident. It stays in the raising request's
     * trace and is sampled with it, which is what keeps its volume affordable. See the class comment.
     */
    private void trackPerRequest(String type, Map<String, String> dimensions, Map<String, Double> measurements) {
        track(type, dimensions, measurements, false);
    }

    private void track(String type, Map<String, String> dimensions, Map<String, Double> measurements,
                       boolean standalone) {
        try {
            Map<String, String> properties = new LinkedHashMap<>();
            properties.put(EVENT_TYPE, type);
            properties.put(ALGORITHM_SOURCE, algorithmSource.name());
            properties.putAll(dimensions);
            if (standalone) {
                appInsights.trackStandaloneEvent(EVENT_NAME, properties, measurements);
            } else {
                appInsights.trackEvent(EVENT_NAME, properties, measurements);
            }
        } catch (Exception e) {
            // A per-request event can fail once per request, so only a state event's failure is logged at WARN.
            if (standalone) {
                log.warn("Unable to publish IDAM JWK set telemetry for {}: {}", type, summary(e));
            } else {
                log.debug("Unable to publish IDAM JWK set telemetry for {}: {}", type, summary(e));
            }
        }
    }

    private static Map<String, String> causeOf(Exception exception) {
        return exception == null
            ? Map.of(EXCEPTION_TYPE, NONE, DETAIL, NONE)
            : Map.of(EXCEPTION_TYPE, exception.getClass().getSimpleName(), DETAIL, message(exception));
    }

    /**
     * The exception's one-line summary, for log lines that can repeat once per retrieval or more often. Attaching the
     * exception itself would log a stack trace and add an App Insights {@code exceptions} entry each time.
     */
    private static String summary(Exception exception) {
        return exception == null ? NONE : exception.toString();
    }

    private static String names(Set<JWSAlgorithm> algorithms) {
        return algorithms.stream().map(JWSAlgorithm::getName).sorted().collect(Collectors.joining(","));
    }

    private static Map<String, Double> threadQueue(int threadQueueLength) {
        return Map.of(THREAD_QUEUE_LENGTH, (double) threadQueueLength);
    }

    private static String message(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return NONE;
        }
        String singleLine = message.replaceAll("\\s+", " ").trim();
        return singleLine.length() <= MAX_DETAIL_LENGTH
            ? singleLine
            : singleLine.substring(0, MAX_DETAIL_LENGTH) + "...";
    }
}
