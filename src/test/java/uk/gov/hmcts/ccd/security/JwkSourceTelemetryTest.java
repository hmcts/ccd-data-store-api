package uk.gov.hmcts.ccd.security;

import com.nimbusds.jose.jwk.source.CachingJWKSetSource;
import com.nimbusds.jose.jwk.source.JWKSetSourceWithHealthStatusReporting;
import com.nimbusds.jose.jwk.source.OutageTolerantJWKSetSource;
import com.nimbusds.jose.jwk.source.RateLimitedJWKSetSource;
import com.nimbusds.jose.jwk.source.RefreshAheadCachingJWKSetSource;
import com.nimbusds.jose.jwk.source.RetryingJWKSetSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.events.Event;
import com.nimbusds.jose.util.health.HealthReport;
import com.nimbusds.jose.util.health.HealthStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.RecordingAppInsights;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.TrackedEvent;

class JwkSourceTelemetryTest {

    private RecordingAppInsights appInsights;
    private JwkSourceTelemetry telemetry;

    @BeforeEach
    void setUp() {
        appInsights = new RecordingAppInsights();
        telemetry = new JwkSourceTelemetry(appInsights);
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

    @Test
    @DisplayName("does not propagate a failure to publish telemetry")
    void telemetryFailureDoesNotPropagate() {
        JwkSourceTelemetry faultyTelemetry = new JwkSourceTelemetry(new ThrowingAppInsights());

        assertThatCode(() -> faultyTelemetry.rateLimitedEventListener()
            .notify(mock(RateLimitedJWKSetSource.RateLimitedEvent.class)))
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
    }
}
