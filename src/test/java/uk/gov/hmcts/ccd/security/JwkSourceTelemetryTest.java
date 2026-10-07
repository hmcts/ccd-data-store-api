package uk.gov.hmcts.ccd.security;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
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
import com.nimbusds.jose.util.health.HealthReport;
import com.nimbusds.jose.util.health.HealthStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.CapturedLogs;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.MutableClock;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.RecordingAppInsights;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.TrackedEvent;

class JwkSourceTelemetryTest {

    private RecordingAppInsights appInsights;
    private MutableClock clock;
    private JwkSourceTelemetry telemetry;

    @BeforeEach
    void setUp() {
        appInsights = new RecordingAppInsights();
        clock = new MutableClock(Instant.parse("2026-09-23T12:00:00Z"));
        telemetry = new JwkSourceTelemetry(appInsights, clock);
    }

    @Test
    @DisplayName("reports no outage tolerance remaining before any outage occurs")
    void reportsNoOutageInitially() {
        assertThat(telemetry.remainingToleranceMs()).isEqualTo(-1L);
    }

    @Nested
    @DisplayName("outage events")
    class OutageEvents {

        @Test
        @DisplayName("reports the remaining tolerance and cause while stale keys are served")
        void reportsOutageTolerated() {
            IllegalStateException cause = new IllegalStateException("IDAM unreachable");

            telemetry.outageEventListener().notify(outageEvent(5_000L, cause));

            assertThat(telemetry.remainingToleranceMs()).isEqualTo(5_000L);
            TrackedEvent tracked = onlyEventOfType(JwkSourceTelemetry.OUTAGE_TOLERATED).getFirst();
            assertThat(tracked.metrics()).containsEntry(JwkSourceTelemetry.REMAINING_TOLERANCE_MS, 5_000.0);
            assertThat(tracked.properties())
                .containsEntry(JwkSourceTelemetry.EXCEPTION_TYPE, "IllegalStateException")
                .containsEntry(JwkSourceTelemetry.DETAIL, "IDAM unreachable");
        }

        @Test
        @DisplayName("does not track an event of an unexpected type")
        void ignoresUnknownEvent() {
            telemetry.outageEventListener().notify(null);

            assertThat(appInsights.events()).isEmpty();
        }
    }

    @Nested
    @DisplayName("remaining outage tolerance")
    class RemainingTolerance {

        @Test
        @DisplayName("counts down between events and reports zero once the window closes with no further event")
        void reportsZeroOnceTheWindowHasClosed() {
            telemetry.outageEventListener().notify(outageEvent(5_000L, new RuntimeException("down")));

            clock.advance(Duration.ofMillis(3_000));
            assertThat(telemetry.remainingToleranceMs())
                .as("calculated when read, not repeated from the event")
                .isEqualTo(2_000L);

            clock.advance(Duration.ofMillis(2_000));
            assertThat(telemetry.remainingToleranceMs()).isZero();

            clock.advance(Duration.ofHours(6));
            assertThat(telemetry.remainingToleranceMs())
                .as("hours after the window closed, it must not report the last event's value")
                .isZero();
            assertThat(appInsights.eventsOfType(JwkSourceTelemetry.OUTAGE_TOLERATED))
                .as("exactly one outage event fired, so nothing but the clock moved the value")
                .hasSize(1);
        }

        @Test
        @DisplayName("reports no outage after IDAM recovers, including after the old window would have closed")
        void reportsNoOutageAfterRecovery() {
            telemetry.outageEventListener().notify(outageEvent(5_000L, new RuntimeException("down")));
            clock.advance(Duration.ofMillis(1_000));
            assertThat(telemetry.remainingToleranceMs()).isEqualTo(4_000L);

            telemetry.retrievalSucceeded();

            assertThat(telemetry.remainingToleranceMs()).isEqualTo(-1L);
            clock.advance(Duration.ofMillis(10_000));
            assertThat(telemetry.remainingToleranceMs()).isEqualTo(-1L);
            assertThat(appInsights.eventsOfType(JwkSourceTelemetry.OUTAGE_TOLERATED)).hasSize(1);
            assertThat(appInsights.eventsOfType(JwkSourceTelemetry.OUTAGE_ENDED)).hasSize(1);
        }

        @Test
        @DisplayName("reports the new window for a second outage after a recovery")
        void reportsTheNewWindowForASecondOutage() {
            telemetry.outageEventListener().notify(outageEvent(5_000L, new RuntimeException("first")));
            telemetry.retrievalSucceeded();
            clock.advance(Duration.ofMillis(10_000));

            telemetry.outageEventListener().notify(outageEvent(8_000L, new RuntimeException("second")));

            assertThat(telemetry.remainingToleranceMs()).isEqualTo(8_000L);
            clock.advance(Duration.ofMillis(3_000));
            assertThat(telemetry.remainingToleranceMs()).isEqualTo(5_000L);
            assertThat(appInsights.eventsOfType(JwkSourceTelemetry.OUTAGE_TOLERATED)).hasSize(2);
            assertThat(appInsights.eventsOfType(JwkSourceTelemetry.OUTAGE_ENDED)).hasSize(1);
        }

        @Test
        @DisplayName("still reports recovery when IDAM answers after the window has closed")
        void reportsRecoveryAfterTheWindowHasClosed() {
            telemetry.outageEventListener().notify(outageEvent(1_000L, new RuntimeException("down")));
            clock.advance(Duration.ofMillis(5_000));
            assertThat(telemetry.remainingToleranceMs()).isZero();

            telemetry.retrievalSucceeded();

            assertThat(telemetry.remainingToleranceMs()).isEqualTo(-1L);
            assertThat(appInsights.eventsOfType(JwkSourceTelemetry.OUTAGE_ENDED))
                .as("a closed window is still an outage, and its end must be reported")
                .hasSize(1);
        }
    }

    @Nested
    @DisplayName("algorithm source")
    class AlgorithmSourceDimension {

        @Test
        @DisplayName("marks events raised before start-up has chosen the algorithms as PENDING")
        void marksEventsBeforeTheChoiceAsPending() {
            telemetry.retryingEventListener().notify(retrialEvent(new RuntimeException("down")));

            assertThat(telemetry.algorithmSource()).isEqualTo(JwkSourceTelemetry.AlgorithmSource.PENDING);
            assertThat(onlyEventOfType(JwkSourceTelemetry.RETRIAL).getFirst().properties())
                .containsEntry(JwkSourceTelemetry.ALGORITHM_SOURCE, "PENDING");
        }

        @Test
        @DisplayName("emits a fallback marker with its cause, and marks every later event FALLBACK")
        void emitsAMarkerAndMarksLaterEventsOnFallback() {
            telemetry.algorithmsFellBack(JwkSourceConfiguration.FALLBACK_ALGORITHMS,
                new IllegalStateException("Connection refused"));
            telemetry.rateLimitedEventListener().notify(mock(RateLimitedJWKSetSource.RateLimitedEvent.class));

            assertThat(telemetry.algorithmSource()).isEqualTo(JwkSourceTelemetry.AlgorithmSource.FALLBACK);
            assertThat(onlyEventOfType(JwkSourceTelemetry.ALGORITHMS_FALLBACK).getFirst().properties())
                .containsEntry(JwkSourceTelemetry.ALGORITHM_SOURCE, "FALLBACK")
                .as("the names are sorted and comma-separated, so a KQL query can match the whole value")
                .containsEntry(JwkSourceTelemetry.ALGORITHMS, "PS256,PS384,PS512,RS256,RS384,RS512")
                .containsEntry(JwkSourceTelemetry.EXCEPTION_TYPE, "IllegalStateException")
                .containsEntry(JwkSourceTelemetry.DETAIL, "Connection refused");
            assertThat(onlyEventOfType(JwkSourceTelemetry.RATE_LIMIT_REACHED).getFirst().properties())
                .containsEntry(JwkSourceTelemetry.ALGORITHM_SOURCE, "FALLBACK");
        }

        @Test
        @DisplayName("says why when the key set was retrieved but had no usable keys")
        void explainsAFallbackWithoutAnException() {
            telemetry.algorithmsFellBack(JwkSourceConfiguration.FALLBACK_ALGORITHMS, null);

            assertThat(onlyEventOfType(JwkSourceTelemetry.ALGORITHMS_FALLBACK).getFirst().properties())
                .containsEntry(JwkSourceTelemetry.EXCEPTION_TYPE, "NONE")
                .containsEntry(JwkSourceTelemetry.DETAIL, "IDAM JWK set advertised no usable signature algorithms");
        }

        @Test
        @DisplayName("emits no marker when the algorithms were derived, and marks every later event DERIVED")
        void emitsNoMarkerAndMarksLaterEventsWhenDerived() {
            telemetry.algorithmsDerived(Set.of(JWSAlgorithm.RS256, JWSAlgorithm.ES256));
            telemetry.rateLimitedEventListener().notify(mock(RateLimitedJWKSetSource.RateLimitedEvent.class));

            assertThat(telemetry.algorithmSource()).isEqualTo(JwkSourceTelemetry.AlgorithmSource.DERIVED);
            assertThat(appInsights.eventsOfType(JwkSourceTelemetry.ALGORITHMS_FALLBACK)).isEmpty();
            assertThat(onlyEventOfType(JwkSourceTelemetry.RATE_LIMIT_REACHED).getFirst().properties())
                .as("the event this assertion depends on fired, and carries the dimension")
                .containsEntry(JwkSourceTelemetry.ALGORITHM_SOURCE, "DERIVED");
        }
    }

    @Nested
    @DisplayName("rate limit events")
    class RateLimitEvents {

        @Test
        @DisplayName("reports that a retrieval was refused by the rate limiter")
        void reportsRateLimitReached() {
            telemetry.rateLimitedEventListener().notify(mock(RateLimitedJWKSetSource.RateLimitedEvent.class));

            assertThat(onlyEventOfType(JwkSourceTelemetry.RATE_LIMIT_REACHED)).hasSize(1);
        }

        @Test
        @DisplayName("does not track an event of an unexpected type")
        void ignoresUnknownEvent() {
            telemetry.rateLimitedEventListener().notify(null);

            assertThat(appInsights.events()).isEmpty();
        }
    }

    @Nested
    @DisplayName("retrying events")
    class RetryingEvents {

        @Test
        @DisplayName("reports the cause of a retrial")
        void reportsRetrial() {
            RuntimeException cause = new RuntimeException("connection reset");

            telemetry.retryingEventListener().notify(retrialEvent(cause));

            TrackedEvent tracked = onlyEventOfType(JwkSourceTelemetry.RETRIAL).getFirst();
            assertThat(tracked.properties())
                .containsEntry(JwkSourceTelemetry.EXCEPTION_TYPE, "RuntimeException")
                .containsEntry(JwkSourceTelemetry.DETAIL, "connection reset");
        }

        @Test
        @DisplayName("does not track an event of an unexpected type")
        void ignoresUnknownEvent() {
            telemetry.retryingEventListener().notify(null);

            assertThat(appInsights.events()).isEmpty();
        }
    }

    @Nested
    @DisplayName("health reports")
    class HealthReports {

        @Test
        @DisplayName("reports a healthy endpoint with no exception detail")
        void reportsHealthy() {
            telemetry.healthReportListener().notify(
                new HealthReport<>(healthSource(), HealthStatus.HEALTHY, System.currentTimeMillis(), null));

            TrackedEvent tracked = onlyEventOfType(JwkSourceTelemetry.HEALTH).getFirst();
            assertThat(tracked.properties())
                .containsEntry(JwkSourceTelemetry.HEALTH_STATUS, "HEALTHY")
                .containsEntry(JwkSourceTelemetry.EXCEPTION_TYPE, "NONE")
                .containsEntry(JwkSourceTelemetry.DETAIL, "NONE");
        }

        @Test
        @DisplayName("reports an unhealthy endpoint together with the cause")
        void reportsUnhealthy() {
            IllegalStateException cause = new IllegalStateException("no keys available");

            telemetry.healthReportListener().notify(new HealthReport<>(
                healthSource(), HealthStatus.NOT_HEALTHY, cause, System.currentTimeMillis(), null));

            TrackedEvent tracked = onlyEventOfType(JwkSourceTelemetry.HEALTH).getFirst();
            assertThat(tracked.properties())
                .containsEntry(JwkSourceTelemetry.HEALTH_STATUS, "NOT_HEALTHY")
                .containsEntry(JwkSourceTelemetry.EXCEPTION_TYPE, "IllegalStateException")
                .containsEntry(JwkSourceTelemetry.DETAIL, "no keys available");
        }
    }

    @Nested
    @DisplayName("caching events")
    class CachingEvents {

        @Test
        @DisplayName("reports how many threads were queued when a refresh timed out")
        void reportsRefreshTimedOut() {
            CachingJWKSetSource.RefreshTimedOutEvent<SecurityContext> event =
                mock(CachingJWKSetSource.RefreshTimedOutEvent.class);
            when(event.getThreadQueueLength()).thenReturn(3);

            telemetry.cachingEventListener().notify(event);

            assertThat(onlyEventOfType(JwkSourceTelemetry.REFRESH_TIMED_OUT).getFirst().metrics())
                .containsEntry(JwkSourceTelemetry.THREAD_QUEUE_LENGTH, 3.0);
        }

        @Test
        @DisplayName("reports how many threads were queued while waiting on an in-flight refresh")
        void reportsWaitingForRefresh() {
            CachingJWKSetSource.WaitingForRefreshEvent<SecurityContext> event =
                mock(CachingJWKSetSource.WaitingForRefreshEvent.class);
            when(event.getThreadQueueLength()).thenReturn(2);

            telemetry.cachingEventListener().notify(event);

            assertThat(onlyEventOfType(JwkSourceTelemetry.WAITING_FOR_REFRESH).getFirst().metrics())
                .containsEntry(JwkSourceTelemetry.THREAD_QUEUE_LENGTH, 2.0);
        }

        @Test
        @DisplayName("reports an unrecoverable cache refresh failure")
        void reportsUnableToRefresh() {
            telemetry.cachingEventListener().notify(mock(CachingJWKSetSource.UnableToRefreshEvent.class));

            assertThat(onlyEventOfType(JwkSourceTelemetry.UNABLE_TO_REFRESH)).hasSize(1);
        }

        @Test
        @DisplayName("reports that the refresh-ahead attempt failed")
        void reportsUnableToRefreshAhead() {
            telemetry.cachingEventListener().notify(
                mock(RefreshAheadCachingJWKSetSource.UnableToRefreshAheadOfExpirationEvent.class));

            assertThat(onlyEventOfType(JwkSourceTelemetry.UNABLE_TO_REFRESH_AHEAD)).hasSize(1);
        }

        @Test
        @DisplayName("reports the cause of a failed scheduled refresh")
        void reportsScheduledRefreshFailed() {
            RuntimeException cause = new RuntimeException("scheduled boom");
            RefreshAheadCachingJWKSetSource.ScheduledRefreshFailed<SecurityContext> event =
                mock(RefreshAheadCachingJWKSetSource.ScheduledRefreshFailed.class);
            when(event.getException()).thenReturn(cause);

            telemetry.cachingEventListener().notify(event);

            assertThat(onlyEventOfType(JwkSourceTelemetry.SCHEDULED_REFRESH_FAILED).getFirst().properties())
                .containsEntry(JwkSourceTelemetry.EXCEPTION_TYPE, "RuntimeException")
                .containsEntry(JwkSourceTelemetry.DETAIL, "scheduled boom");
        }

        @Test
        @DisplayName("reports a completed scheduled refresh")
        void reportsScheduledRefreshCompleted() {
            telemetry.cachingEventListener().notify(
                mock(RefreshAheadCachingJWKSetSource.ScheduledRefreshCompletedEvent.class));

            assertThat(onlyEventOfType(JwkSourceTelemetry.SCHEDULED_REFRESH_COMPLETED)).hasSize(1);
        }

        @Test
        @DisplayName("reports that no refresh-ahead was scheduled")
        void reportsRefreshNotScheduled() {
            telemetry.cachingEventListener().notify(
                mock(RefreshAheadCachingJWKSetSource.RefreshNotScheduledEvent.class));

            assertThat(onlyEventOfType(JwkSourceTelemetry.REFRESH_NOT_SCHEDULED)).hasSize(1);
        }

        @Test
        @DisplayName("reports a completed cache refresh with the queue length")
        void reportsRefreshCompleted() {
            CachingJWKSetSource.RefreshCompletedEvent<SecurityContext> event =
                mock(CachingJWKSetSource.RefreshCompletedEvent.class);
            when(event.getThreadQueueLength()).thenReturn(1);

            telemetry.cachingEventListener().notify(event);

            assertThat(onlyEventOfType(JwkSourceTelemetry.REFRESH_COMPLETED).getFirst().metrics())
                .containsEntry(JwkSourceTelemetry.THREAD_QUEUE_LENGTH, 1.0);
        }

        @Test
        @DisplayName("does not track the routine refresh lifecycle events, only logging them at trace level")
        void doesNotTrackRoutineEvents() {
            telemetry.cachingEventListener().notify(mock(CachingJWKSetSource.RefreshInitiatedEvent.class));
            telemetry.cachingEventListener().notify(
                mock(RefreshAheadCachingJWKSetSource.RefreshScheduledEvent.class));
            telemetry.cachingEventListener().notify(
                mock(RefreshAheadCachingJWKSetSource.ScheduledRefreshInitiatedEvent.class));

            assertThat(appInsights.events()).isEmpty();
        }

        @Test
        @DisplayName("does not track an unrecognised event, so a future Nimbus event stays visible only in logs")
        void ignoresUnknownEvent() {
            Event<CachingJWKSetSource<SecurityContext>, SecurityContext> unknown =
                new Event<>() {
                    @Override
                    public CachingJWKSetSource<SecurityContext> getSource() {
                        return null;
                    }

                    @Override
                    public SecurityContext getContext() {
                        return null;
                    }
                };

            telemetry.cachingEventListener().notify(unknown);
            telemetry.cachingEventListener().notify(null);

            assertThat(appInsights.events()).isEmpty();
        }
    }

    @Nested
    @DisplayName("exception detail formatting")
    class ExceptionDetailFormatting {

        @Test
        @DisplayName("reports NONE when the exception has no message")
        void reportsNoneForMissingMessage() {
            telemetry.retryingEventListener().notify(retrialEvent(new RuntimeException()));

            assertThat(onlyEventOfType(JwkSourceTelemetry.RETRIAL).getFirst().properties())
                .containsEntry(JwkSourceTelemetry.DETAIL, "NONE");
        }

        @Test
        @DisplayName("reports NONE when the exception message is blank")
        void reportsNoneForBlankMessage() {
            telemetry.retryingEventListener().notify(retrialEvent(new RuntimeException("   ")));

            assertThat(onlyEventOfType(JwkSourceTelemetry.RETRIAL).getFirst().properties())
                .containsEntry(JwkSourceTelemetry.DETAIL, "NONE");
        }

        @Test
        @DisplayName("collapses a multi-line message onto a single line")
        void collapsesMultilineMessage() {
            telemetry.retryingEventListener().notify(retrialEvent(new RuntimeException("line one\n  line two")));

            assertThat(onlyEventOfType(JwkSourceTelemetry.RETRIAL).getFirst().properties())
                .containsEntry(JwkSourceTelemetry.DETAIL, "line one line two");
        }

        @Test
        @DisplayName("truncates a message longer than the maximum detail length")
        void truncatesLongMessage() {
            String longMessage = "x".repeat(300);

            telemetry.retryingEventListener().notify(retrialEvent(new RuntimeException(longMessage)));

            String detail = onlyEventOfType(JwkSourceTelemetry.RETRIAL).getFirst()
                .properties().get(JwkSourceTelemetry.DETAIL);
            assertThat(detail).hasSize(256 + 3).endsWith("...");
        }
    }

    @Nested
    @DisplayName("retrieval succeeded")
    class RetrievalSucceeded {

        @Test
        @DisplayName("does not report recovery when no outage was ever tolerated")
        void doesNothingWithoutAPriorOutage() {
            telemetry.retrievalSucceeded();

            assertThat(appInsights.events()).isEmpty();
            assertThat(telemetry.remainingToleranceMs()).isEqualTo(-1L);
        }

        @Test
        @DisplayName("reports recovery and clears the tolerance once IDAM answers again")
        void reportsRecoveryAfterAnOutage() {
            telemetry.outageEventListener().notify(outageEvent(1_000L, new RuntimeException("down")));

            telemetry.retrievalSucceeded();

            assertThat(telemetry.remainingToleranceMs()).isEqualTo(-1L);
            assertThat(onlyEventOfType(JwkSourceTelemetry.OUTAGE_ENDED)).hasSize(1);
        }
    }

    @Nested
    @DisplayName("log output")
    class LogOutput {

        private static final String RECOVERED_AFTER_FALLBACK =
            "IDAM JWK set retrieved; key source recovered after start-up fallback";

        private CapturedLogs logs;

        @BeforeEach
        void captureLogs() {
            logs = new CapturedLogs(JwkSourceTelemetry.class);
        }

        @AfterEach
        void releaseLogs() {
            logs.close();
        }

        @Test
        @DisplayName("logs a completed scheduled refresh at DEBUG, because it fires every few minutes per pod")
        void logsScheduledRefreshCompletedAtDebug() {
            telemetry.cachingEventListener().notify(
                mock(RefreshAheadCachingJWKSetSource.ScheduledRefreshCompletedEvent.class));

            assertThat(logs.startingWith("Scheduled IDAM JWK set refresh completed"))
                .singleElement()
                .satisfies(event -> assertThat(event.getLevel()).isEqualTo(Level.DEBUG));
        }

        @Test
        @DisplayName("logs an unhealthy endpoint at WARN with the cause on one line and no stack trace")
        void logsUnhealthyAtDebug() {
            telemetry.healthReportListener().notify(new HealthReport<>(healthSource(), HealthStatus.NOT_HEALTHY,
                new IllegalStateException("no keys available"), System.currentTimeMillis(), null));

            assertThat(logs.startingWith("IDAM JWK set endpoint unhealthy"))
                .singleElement()
                .satisfies(event -> assertLoggedWithoutStackTrace(event, Level.WARN,
                    "IDAM JWK set endpoint unhealthy: java.lang.IllegalStateException: no keys available"));
        }

        @Test
        @DisplayName("logs a retry at DEBUG, because the outage WARN already reports the failed retrieval")
        void logsRetrialAtDebug() {
            telemetry.retryingEventListener().notify(retrialEvent(new IllegalStateException("Connection refused")));

            assertThat(logs.startingWith("Retrying IDAM JWK set retrieval"))
                .singleElement()
                .satisfies(event -> assertLoggedWithoutStackTrace(event, Level.DEBUG,
                    "Retrying IDAM JWK set retrieval after java.lang.IllegalStateException: Connection refused"));
        }

        @Test
        @DisplayName("logs every per-request line at DEBUG, as each of those requests fails with a logged 500")
        void logsPerRequestLinesAtDebug() {
            EventListener<CachingJWKSetSource<SecurityContext>, SecurityContext> caching =
                telemetry.cachingEventListener();
            telemetry.rateLimitedEventListener().notify(mock(RateLimitedJWKSetSource.RateLimitedEvent.class));
            caching.notify(mock(CachingJWKSetSource.WaitingForRefreshEvent.class));
            caching.notify(mock(CachingJWKSetSource.RefreshTimedOutEvent.class));
            caching.notify(mock(CachingJWKSetSource.UnableToRefreshEvent.class));
            caching.notify(mock(RefreshAheadCachingJWKSetSource.UnableToRefreshAheadOfExpirationEvent.class));

            assertThat(logs.startingWith(""))
                .extracting(ILoggingEvent::getLevel, event -> event.getFormattedMessage().split(";")[0])
                .containsExactly(
                    tuple(Level.DEBUG, "IDAM JWK set retrieval refused by the rate limiter"),
                    tuple(Level.DEBUG, "Waiting on an in-flight IDAM JWK set retrieval"),
                    tuple(Level.DEBUG, "Timed out waiting for an in-flight IDAM JWK set retrieval"),
                    tuple(Level.DEBUG, "Unable to refresh the IDAM JWK set cache"),
                    tuple(Level.DEBUG, "Unable to refresh the IDAM JWK set ahead of expiry"));
        }

        @Test
        @DisplayName("logs a refresh that could not be scheduled ahead of expiry at DEBUG, as it repeats per "
            + "retrieval")
        void logsRefreshNotScheduledAtDebug() {
            telemetry.cachingEventListener().notify(
                mock(RefreshAheadCachingJWKSetSource.RefreshNotScheduledEvent.class));

            assertThat(logs.startingWith("No IDAM JWK set refresh scheduled ahead of expiry"))
                .singleElement()
                .satisfies(event -> assertThat(event.getLevel()).isEqualTo(Level.DEBUG));
        }

        @Test
        @DisplayName("logs serving cached keys at WARN with the cause on one line and no stack trace")
        void logsOutageToleratedAtWarn() {
            telemetry.outageEventListener().notify(outageEvent(1_000L, new IllegalStateException("down")));

            assertThat(logs.startingWith("IDAM JWK set unavailable"))
                .singleElement()
                .satisfies(event -> assertLoggedWithoutStackTrace(event, Level.WARN,
                    "IDAM JWK set unavailable; serving cached signing keys for a further 1000ms. "
                        + "Cause: java.lang.IllegalStateException: down"));
        }

        @Test
        @DisplayName("logs a failed scheduled refresh at WARN with the cause on one line and no stack trace")
        void logsScheduledRefreshFailedWithoutAStackTrace() {
            RefreshAheadCachingJWKSetSource.ScheduledRefreshFailed<SecurityContext> event =
                mock(RefreshAheadCachingJWKSetSource.ScheduledRefreshFailed.class);
            when(event.getException()).thenReturn(new RuntimeException("scheduled boom"));

            telemetry.cachingEventListener().notify(event);

            assertThat(logs.startingWith("Scheduled IDAM JWK set refresh failed"))
                .singleElement()
                .satisfies(logged -> assertLoggedWithoutStackTrace(logged, Level.WARN,
                    "Scheduled IDAM JWK set refresh failed: java.lang.RuntimeException: scheduled boom"));
        }

        @Test
        @DisplayName("logs a failure to publish a per-request event at DEBUG, as it can repeat per request")
        void logsPerRequestTelemetryFailureAtDebug() {
            new JwkSourceTelemetry(new ThrowingAppInsights()).rateLimitedEventListener()
                .notify(mock(RateLimitedJWKSetSource.RateLimitedEvent.class));

            assertThat(logs.startingWith("Unable to publish IDAM JWK set telemetry"))
                .singleElement()
                .satisfies(logged -> assertLoggedWithoutStackTrace(logged, Level.DEBUG,
                    "Unable to publish IDAM JWK set telemetry for RATE_LIMIT_REACHED: "
                        + "java.lang.RuntimeException: telemetry backend unavailable"));
        }

        @Test
        @DisplayName("logs a failure to publish a state event at WARN with the cause on one line and no stack trace")
        void logsStateTelemetryFailureAtWarn() {
            new JwkSourceTelemetry(new ThrowingAppInsights()).cachingEventListener()
                .notify(mock(RefreshAheadCachingJWKSetSource.ScheduledRefreshCompletedEvent.class));

            assertThat(logs.startingWith("Unable to publish IDAM JWK set telemetry"))
                .singleElement()
                .satisfies(logged -> assertLoggedWithoutStackTrace(logged, Level.WARN,
                    "Unable to publish IDAM JWK set telemetry for SCHEDULED_REFRESH_COMPLETED: "
                        + "java.lang.RuntimeException: telemetry backend unavailable"));
        }

        @Test
        @DisplayName("logs nothing above DEBUG for a healthy refresh cycle")
        void logsNothingAboveDebugWhenHealthy() {
            EventListener<CachingJWKSetSource<SecurityContext>, SecurityContext> caching =
                telemetry.cachingEventListener();
            caching.notify(mock(RefreshAheadCachingJWKSetSource.ScheduledRefreshInitiatedEvent.class));
            caching.notify(mock(CachingJWKSetSource.RefreshInitiatedEvent.class));
            telemetry.retrievalSucceeded();
            telemetry.healthReportListener().notify(
                new HealthReport<>(healthSource(), HealthStatus.HEALTHY, System.currentTimeMillis(), null));
            caching.notify(mock(CachingJWKSetSource.RefreshCompletedEvent.class));
            caching.notify(mock(RefreshAheadCachingJWKSetSource.RefreshScheduledEvent.class));
            caching.notify(mock(RefreshAheadCachingJWKSetSource.ScheduledRefreshCompletedEvent.class));

            assertThat(logs.startingWith("")).isNotEmpty();
            assertThat(aboveDebug()).isEmpty();
        }

        @Test
        @DisplayName("logs one WARN for a failed retrieval while cached keys are served, and nothing per request")
        void logsOneWarnPerFailedRetrievalDuringATolerableOutage() {
            EventListener<CachingJWKSetSource<SecurityContext>, SecurityContext> caching =
                telemetry.cachingEventListener();
            // One failed retrieval: the retry fails too, the outage layer serves the cached keys, so the layers
            // above it see a success.
            caching.notify(mock(CachingJWKSetSource.RefreshInitiatedEvent.class));
            telemetry.retryingEventListener().notify(retrialEvent(new IllegalStateException("Connection refused")));
            telemetry.outageEventListener().notify(outageEvent(1_000L, new IllegalStateException("Read timed out")));
            telemetry.healthReportListener().notify(
                new HealthReport<>(healthSource(), HealthStatus.HEALTHY, System.currentTimeMillis(), null));
            caching.notify(mock(CachingJWKSetSource.RefreshCompletedEvent.class));
            // Requests that arrived while it was in flight, or found the rate limit used up.
            caching.notify(mock(CachingJWKSetSource.WaitingForRefreshEvent.class));
            caching.notify(mock(CachingJWKSetSource.WaitingForRefreshEvent.class));
            telemetry.rateLimitedEventListener().notify(mock(RateLimitedJWKSetSource.RateLimitedEvent.class));
            telemetry.rateLimitedEventListener().notify(mock(RateLimitedJWKSetSource.RateLimitedEvent.class));

            assertThat(aboveDebug())
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.getLevel()).isEqualTo(Level.WARN);
                    assertThat(event.getFormattedMessage())
                        .startsWith("IDAM JWK set unavailable; serving cached signing keys for a further 1000ms");
                });
        }

        @Test
        @DisplayName("logs nothing above WARN once no usable keys are left, as each failed request is logged "
            + "elsewhere")
        void logsNothingAboveWarnOnceNoUsableKeysAreLeft() {
            EventListener<CachingJWKSetSource<SecurityContext>, SecurityContext> caching =
                telemetry.cachingEventListener();
            telemetry.retryingEventListener().notify(retrialEvent(new IllegalStateException("Connection refused")));
            telemetry.healthReportListener().notify(new HealthReport<>(healthSource(), HealthStatus.NOT_HEALTHY,
                new IllegalStateException("Connection refused"), System.currentTimeMillis(), null));
            caching.notify(mock(RefreshAheadCachingJWKSetSource.UnableToRefreshAheadOfExpirationEvent.class));
            caching.notify(mock(CachingJWKSetSource.UnableToRefreshEvent.class));
            caching.notify(mock(CachingJWKSetSource.UnableToRefreshEvent.class));
            caching.notify(mock(CachingJWKSetSource.RefreshTimedOutEvent.class));
            telemetry.rateLimitedEventListener().notify(mock(RateLimitedJWKSetSource.RateLimitedEvent.class));

            assertThat(logs.startingWith("")).hasSize(7);
            assertThat(aboveWarn()).isEmpty();
        }

        @Test
        @DisplayName("logs recovery once at INFO when retrieval succeeds after a start-up fallback")
        void logsRecoveryAfterAFallbackOnce() {
            telemetry.algorithmsFellBack(JwkSourceConfiguration.FALLBACK_ALGORITHMS,
                new IllegalStateException("Connection refused"));

            telemetry.retrievalSucceeded();
            telemetry.retrievalSucceeded();

            assertThat(logs.startingWith(RECOVERED_AFTER_FALLBACK))
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.getLevel()).isEqualTo(Level.INFO);
                    assertThat(event.getFormattedMessage())
                        .contains("remain the RSA family PS256,PS384,PS512,RS256,RS384,RS512 for this instance");
                });
        }

        @Test
        @DisplayName("never logs recovery after start-up derived the algorithms")
        void doesNotLogRecoveryWhenDerived() {
            // The start-up retrieval itself succeeds before the algorithms are chosen.
            telemetry.retrievalSucceeded();
            telemetry.algorithmsDerived(Set.of(JWSAlgorithm.RS256));
            telemetry.retrievalSucceeded();
            telemetry.retrievalSucceeded();

            assertThat(logs.startingWith(RECOVERED_AFTER_FALLBACK)).isEmpty();
        }

        @Test
        @DisplayName("does not log recovery after a fallback for a key set with no usable keys, as nothing failed")
        void doesNotLogRecoveryWhenTheKeySetHadNoUsableKeys() {
            telemetry.retrievalSucceeded();
            telemetry.algorithmsFellBack(JwkSourceConfiguration.FALLBACK_ALGORITHMS, null);
            telemetry.retrievalSucceeded();

            assertThat(logs.startingWith(RECOVERED_AFTER_FALLBACK)).isEmpty();
        }

        @Test
        @DisplayName("logs the end of a tolerated outage at INFO, separately from start-up recovery")
        void logsOutageEndedAtInfo() {
            telemetry.outageEventListener().notify(outageEvent(1_000L, new RuntimeException("down")));

            telemetry.retrievalSucceeded();

            assertThat(logs.startingWith("IDAM JWK set retrieved successfully; no longer serving stale signing keys"))
                .singleElement()
                .satisfies(event -> assertThat(event.getLevel()).isEqualTo(Level.INFO));
            assertThat(logs.startingWith(RECOVERED_AFTER_FALLBACK)).isEmpty();
        }

        private List<ILoggingEvent> aboveDebug() {
            return logs.startingWith("").stream()
                .filter(event -> event.getLevel().isGreaterOrEqual(Level.INFO))
                .toList();
        }

        private List<ILoggingEvent> aboveWarn() {
            return logs.startingWith("").stream()
                .filter(event -> event.getLevel().isGreaterOrEqual(Level.ERROR))
                .toList();
        }

        private static void assertLoggedWithoutStackTrace(ILoggingEvent event, Level level, String message) {
            assertThat(event.getLevel()).isEqualTo(level);
            assertThat(event.getFormattedMessage()).isEqualTo(message);
            assertThat(event.getThrowableProxy())
                .as("an attached exception logs a stack trace and adds an App Insights exceptions entry")
                .isNull();
        }
    }

    @Nested
    @DisplayName("sampling treatment")
    class SamplingTreatment {

        /**
         * Bounded by the rate limiter or the refresh schedule, and needed by the monitoring queries.
         */
        private static final Set<String> STATE_EVENTS = Set.of(
            JwkSourceTelemetry.OUTAGE_TOLERATED, JwkSourceTelemetry.OUTAGE_ENDED, JwkSourceTelemetry.HEALTH,
            JwkSourceTelemetry.RETRIAL, JwkSourceTelemetry.REFRESH_COMPLETED,
            JwkSourceTelemetry.SCHEDULED_REFRESH_COMPLETED, JwkSourceTelemetry.SCHEDULED_REFRESH_FAILED,
            JwkSourceTelemetry.REFRESH_NOT_SCHEDULED, JwkSourceTelemetry.ALGORITHMS_FALLBACK);

        /**
         * Raised above the rate limiter, so they can fire once per request during an incident.
         */
        private static final Set<String> PER_REQUEST_EVENTS = Set.of(
            JwkSourceTelemetry.RATE_LIMIT_REACHED, JwkSourceTelemetry.WAITING_FOR_REFRESH,
            JwkSourceTelemetry.REFRESH_TIMED_OUT, JwkSourceTelemetry.UNABLE_TO_REFRESH,
            JwkSourceTelemetry.UNABLE_TO_REFRESH_AHEAD);

        @Test
        @DisplayName("sends state events standalone, so they are not sampled out with a request")
        void sendsStateEventsStandalone() {
            raiseEveryEvent();

            assertThat(appInsights.events())
                .filteredOn(event -> STATE_EVENTS.contains(event.properties().get(JwkSourceTelemetry.EVENT_TYPE)))
                .extracting(event -> event.properties().get(JwkSourceTelemetry.EVENT_TYPE))
                .as("every state event is raised once")
                .containsExactlyInAnyOrderElementsOf(STATE_EVENTS);
            assertThat(appInsights.events())
                .filteredOn(event -> STATE_EVENTS.contains(event.properties().get(JwkSourceTelemetry.EVENT_TYPE)))
                .allSatisfy(event -> assertThat(event.standalone())
                    .as("%s is standalone", event.properties().get(JwkSourceTelemetry.EVENT_TYPE))
                    .isTrue());
        }

        @Test
        @DisplayName("keeps per-request events in the request's trace, so they are sampled with it")
        void keepsPerRequestEventsInTheRequestTrace() {
            raiseEveryEvent();

            assertThat(appInsights.events())
                .filteredOn(event -> PER_REQUEST_EVENTS.contains(
                    event.properties().get(JwkSourceTelemetry.EVENT_TYPE)))
                .extracting(event -> event.properties().get(JwkSourceTelemetry.EVENT_TYPE))
                .as("every per-request event is raised once")
                .containsExactlyInAnyOrderElementsOf(PER_REQUEST_EVENTS);
            assertThat(appInsights.events())
                .filteredOn(event -> PER_REQUEST_EVENTS.contains(
                    event.properties().get(JwkSourceTelemetry.EVENT_TYPE)))
                .allSatisfy(event -> assertThat(event.standalone())
                    .as("%s stays in the request's trace", event.properties().get(JwkSourceTelemetry.EVENT_TYPE))
                    .isFalse());
        }

        @Test
        @DisplayName("classifies every event type, so a new one cannot be added without choosing its sampling")
        void classifiesEveryEventType() throws IllegalAccessException {
            Set<String> eventTypes = new HashSet<>();
            for (Field field : JwkSourceTelemetry.class.getDeclaredFields()) {
                // The event type constants are the String constants named after their own value
                if (Modifier.isStatic(field.getModifiers()) && field.getType() == String.class
                    && !Modifier.isPrivate(field.getModifiers())) {
                    field.setAccessible(true);
                    if (field.getName().equals(field.get(null))) {
                        eventTypes.add(field.getName());
                    }
                }
            }

            assertThat(eventTypes).hasSize(STATE_EVENTS.size() + PER_REQUEST_EVENTS.size());
            Set<String> classified = new HashSet<>(STATE_EVENTS);
            classified.addAll(PER_REQUEST_EVENTS);
            assertThat(classified).isEqualTo(eventTypes);

            raiseEveryEvent();
            assertThat(appInsights.events())
                .extracting(event -> event.properties().get(JwkSourceTelemetry.EVENT_TYPE))
                .as("every event type can be raised")
                .containsExactlyInAnyOrderElementsOf(eventTypes);
        }

        @SuppressWarnings("unchecked")
        private void raiseEveryEvent() {
            telemetry.outageEventListener().notify(outageEvent(5_000L, new RuntimeException("down")));
            telemetry.retrievalSucceeded();
            telemetry.healthReportListener().notify(
                new HealthReport<>(healthSource(), HealthStatus.HEALTHY, System.currentTimeMillis(), null));
            telemetry.retryingEventListener().notify(retrialEvent(new RuntimeException("down")));
            telemetry.algorithmsFellBack(JwkSourceConfiguration.FALLBACK_ALGORITHMS, null);
            telemetry.rateLimitedEventListener().notify(mock(RateLimitedJWKSetSource.RateLimitedEvent.class));

            RefreshAheadCachingJWKSetSource.ScheduledRefreshFailed<SecurityContext> failed =
                mock(RefreshAheadCachingJWKSetSource.ScheduledRefreshFailed.class);
            when(failed.getException()).thenReturn(new RuntimeException("rejected"));
            EventListener<CachingJWKSetSource<SecurityContext>, SecurityContext> caching =
                telemetry.cachingEventListener();
            caching.notify(mock(CachingJWKSetSource.RefreshCompletedEvent.class));
            caching.notify(mock(RefreshAheadCachingJWKSetSource.ScheduledRefreshCompletedEvent.class));
            caching.notify(failed);
            caching.notify(mock(RefreshAheadCachingJWKSetSource.RefreshNotScheduledEvent.class));
            caching.notify(mock(CachingJWKSetSource.WaitingForRefreshEvent.class));
            caching.notify(mock(CachingJWKSetSource.RefreshTimedOutEvent.class));
            caching.notify(mock(CachingJWKSetSource.UnableToRefreshEvent.class));
            caching.notify(mock(RefreshAheadCachingJWKSetSource.UnableToRefreshAheadOfExpirationEvent.class));
        }
    }

    @Test
    @DisplayName("does not propagate a failure to publish telemetry")
    void telemetryFailureDoesNotPropagate() {
        JwkSourceTelemetry faultyTelemetry = new JwkSourceTelemetry(new ThrowingAppInsights());

        assertThatCode(() -> faultyTelemetry.rateLimitedEventListener()
            .notify(mock(RateLimitedJWKSetSource.RateLimitedEvent.class)))
            .doesNotThrowAnyException();
        assertThatCode(() -> faultyTelemetry.outageEventListener()
            .notify(outageEvent(5_000L, new RuntimeException("down"))))
            .as("a standalone state event")
            .doesNotThrowAnyException();
    }

    @SuppressWarnings("unchecked")
    private static OutageTolerantJWKSetSource.OutageEvent<SecurityContext> outageEvent(long remaining,
                                                                                        Exception cause) {
        OutageTolerantJWKSetSource.OutageEvent<SecurityContext> event =
            mock(OutageTolerantJWKSetSource.OutageEvent.class);
        when(event.getRemainingTime()).thenReturn(remaining);
        when(event.getException()).thenReturn(cause);
        return event;
    }

    @SuppressWarnings("unchecked")
    private static RetryingJWKSetSource.RetrialEvent<SecurityContext> retrialEvent(Exception cause) {
        RetryingJWKSetSource.RetrialEvent<SecurityContext> event = mock(RetryingJWKSetSource.RetrialEvent.class);
        when(event.getException()).thenReturn(cause);
        return event;
    }

    @SuppressWarnings("unchecked")
    private static JWKSetSourceWithHealthStatusReporting<SecurityContext> healthSource() {
        return mock(JWKSetSourceWithHealthStatusReporting.class);
    }

    private List<TrackedEvent> onlyEventOfType(String type) {
        List<TrackedEvent> events = appInsights.eventsOfType(type);
        assertThat(events).as("expected exactly one %s event", type).hasSize(1);
        return events;
    }

    private static final class ThrowingAppInsights extends uk.gov.hmcts.ccd.appinsights.AppInsights {

        ThrowingAppInsights() {
            super(null);
        }

        @Override
        public void trackEvent(String name, Map<String, String> properties, Map<String, Double> metrics) {
            throw new RuntimeException("telemetry backend unavailable");
        }

        @Override
        public void trackStandaloneEvent(String name, Map<String, String> properties, Map<String, Double> metrics) {
            throw new RuntimeException("telemetry backend unavailable");
        }
    }
}
