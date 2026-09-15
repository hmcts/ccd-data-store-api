package uk.gov.hmcts.ccd.security;

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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

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

    private static final String NONE = "NONE";

    private static final int MAX_DETAIL_LENGTH = 256;

    private final AppInsights appInsights;

    private final AtomicLong remainingToleranceMs = new AtomicLong(-1L);

    public JwkSourceTelemetry(AppInsights appInsights) {
        this.appInsights = appInsights;
    }

    /**
     * Returns the remaining outage tolerance, or {@code -1} when stale keys are not currently being served.
     * This allows a health indicator or test to check the current state without waiting for another event.
     */
    public long remainingToleranceMs() {
        return remainingToleranceMs.get();
    }

    /**
     * Called by {@link ObservedResourceRetriever} when IDAM successfully responds.
     * This confirms that the keys were freshly retrieved from IDAM rather than served from the outage cache.
     * Nimbus listeners cannot make this distinction because they sit above the outage-tolerant layer and see
     * both cases as a successful result.
     */
    void retrievalSucceeded() {
        long previous = remainingToleranceMs.getAndSet(-1L);
        if (previous >= 0L) {
            log.info("IDAM JWK set retrieved successfully; no longer serving stale signing keys");
            track(OUTAGE_ENDED);
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
                remainingToleranceMs.set(remaining);

                log.warn("IDAM JWK set unavailable; serving cached signing keys for a further {}ms. Cause: {}",
                    remaining, outage.getException().toString());

                track(OUTAGE_TOLERATED, causeOf(outage.getException()),
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
                log.warn("IDAM JWK set retrieval refused by the rate limiter; the caller will see a failure "
                    + "rather than the cached keys");
                track(RATE_LIMIT_REACHED);
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
                    log.error("Timed out waiting for an in-flight IDAM JWK set retrieval; {} thread(s) queued",
                        timedOut.getThreadQueueLength());
                    track(REFRESH_TIMED_OUT, Map.of(), threadQueue(timedOut.getThreadQueueLength()));
                }
                case CachingJWKSetSource.WaitingForRefreshEvent<SecurityContext> waiting -> {
                    log.info("Waiting on an in-flight IDAM JWK set retrieval; {} thread(s) queued",
                        waiting.getThreadQueueLength());
                    track(WAITING_FOR_REFRESH, Map.of(), threadQueue(waiting.getThreadQueueLength()));
                }
                case CachingJWKSetSource.UnableToRefreshEvent<SecurityContext> ignored -> {
                    log.error("Unable to refresh the IDAM JWK set cache");
                    track(UNABLE_TO_REFRESH);
                }
                case RefreshAheadCachingJWKSetSource.UnableToRefreshAheadOfExpirationEvent<SecurityContext>
                    ignored -> {
                    log.warn("Unable to refresh the IDAM JWK set ahead of expiry; the cache will be refreshed on "
                        + "the request path instead");
                    track(UNABLE_TO_REFRESH_AHEAD);
                }
                case RefreshAheadCachingJWKSetSource.ScheduledRefreshFailed<SecurityContext> failed -> {
                    log.warn("Scheduled IDAM JWK set refresh failed", failed.getException());
                    track(SCHEDULED_REFRESH_FAILED, causeOf(failed.getException()), Map.of());
                }
                case RefreshAheadCachingJWKSetSource.ScheduledRefreshCompletedEvent<SecurityContext> ignored -> {
                    log.info("Scheduled IDAM JWK set refresh completed");
                    track(SCHEDULED_REFRESH_COMPLETED);
                }
                case RefreshAheadCachingJWKSetSource.RefreshNotScheduledEvent<SecurityContext> ignored -> {
                    log.warn("No IDAM JWK set refresh scheduled ahead of expiry");
                    track(REFRESH_NOT_SCHEDULED);
                }
                case CachingJWKSetSource.RefreshCompletedEvent<SecurityContext> completed -> {
                    log.debug("IDAM JWK set cache refreshed; {} thread(s) queued",
                        completed.getThreadQueueLength());
                    track(REFRESH_COMPLETED, Map.of(), threadQueue(completed.getThreadQueueLength()));
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
                log.warn("IDAM JWK set endpoint unhealthy", exception);
            }

            Map<String, String> dimensions = new LinkedHashMap<>();
            dimensions.put(HEALTH_STATUS, String.valueOf(report.getHealthStatus()));
            dimensions.putAll(causeOf(exception));

            track(HEALTH, dimensions, Map.of());
        };
    }

    public EventListener<RetryingJWKSetSource<SecurityContext>, SecurityContext> retryingEventListener() {
        return event -> {
            if (event instanceof RetryingJWKSetSource.RetrialEvent<SecurityContext> retrial) {
                log.warn("Retrying IDAM JWK set retrieval after {}", retrial.getException().toString());
                track(RETRIAL, causeOf(retrial.getException()), Map.of());
            } else {
                trackUnknown(event);
            }
        };
    }

    private void trackUnknown(Event<?, ?> event) {
        log.debug("Unhandled IDAM JWK set event {}", event == null ? NONE : event.getClass().getSimpleName());
    }

    private void track(String type) {
        track(type, Map.of(), Map.of());
    }

    private void track(String type, Map<String, String> dimensions, Map<String, Double> measurements) {
        try {
            Map<String, String> properties = new LinkedHashMap<>();
            properties.put(EVENT_TYPE, type);
            properties.putAll(dimensions);
            appInsights.trackEvent(EVENT_NAME, properties, measurements);
        } catch (Exception e) {
            log.warn("Unable to publish IDAM JWK set telemetry for {}", type, e);
        }
    }

    private static Map<String, String> causeOf(Exception exception) {
        return exception == null
            ? Map.of(EXCEPTION_TYPE, NONE, DETAIL, NONE)
            : Map.of(EXCEPTION_TYPE, exception.getClass().getSimpleName(), DETAIL, message(exception));
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
