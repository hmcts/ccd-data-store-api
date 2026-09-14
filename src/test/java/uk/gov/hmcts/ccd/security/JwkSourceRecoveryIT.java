package uk.gov.hmcts.ccd.security;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.RecordingAppInsights;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.SUBJECT;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.jwksUri;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.originalJwkSet;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.originalKey;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.retrievalAttempts;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.rotatedJwkSet;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.rotatedKey;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.startWireMock;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.stubDown;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.stubHealthy;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.stubSlow;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.tokenSignedWith;

/**
 * Verifies recovery after an outage: fresh keys are retrieved when IDAM returns, the tolerance window
 * resets from that successful retrieval, and the refresh happens on a background thread.
 *
 * <p>{@link JwkSourceOutageIT} covers the outage itself; this covers recovery, which was previously untested.
 */
class JwkSourceRecoveryIT {

    private static final Duration SETTLE_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration POLL = Duration.ofMillis(25);

    private WireMockServer wireMock;
    private JWKSource<SecurityContext> jwkSource;
    private RecordingAppInsights appInsights;
    private JwkSourceTelemetry telemetry;
    private String originalToken;
    private String rotatedToken;

    @BeforeEach
    void setUp() {
        wireMock = startWireMock();
        appInsights = new RecordingAppInsights();
        telemetry = new JwkSourceTelemetry(appInsights);
        originalToken = tokenSignedWith(originalKey());
        rotatedToken = tokenSignedWith(rotatedKey());
    }

    @AfterEach
    void tearDown() throws IOException {
        if (jwkSource instanceof Closeable closeable) {
            closeable.close();
        }
        wireMock.stop();
    }

    @Test
    @DisplayName("picks up the rotated signing key once IDAM comes back, and stops accepting the stale one")
    void refreshesToTheNewKeyAfterRecovery() {
        stubHealthy(wireMock, originalJwkSet());
        NimbusJwtDecoder decoder = decoder(shortLivedCacheProperties());

        assertThat(decoder.decode(originalToken).getSubject()).isEqualTo(SUBJECT);

        stubDown(wireMock);

        await().atMost(SETTLE_TIMEOUT).pollInterval(POLL).ignoreExceptions().untilAsserted(() -> {
            assertThat(decoder.decode(originalToken).getSubject()).isEqualTo(SUBJECT);
            assertThat(appInsights.eventsOfType(JwkSourceTelemetry.OUTAGE_TOLERATED)).isNotEmpty();
        });
        assertThatThrownBy(() -> decoder.decode(rotatedToken)).isInstanceOf(JwtException.class);

        int attemptsDuringOutage = retrievalAttempts(wireMock);
        assertThat(attemptsDuringOutage)
            .as("expected the source to have actually gone back to the failing endpoint")
            .isPositive();

        stubHealthy(wireMock, rotatedJwkSet());

        await().atMost(SETTLE_TIMEOUT).pollInterval(POLL).ignoreExceptions().untilAsserted(() ->
            assertThat(decoder.decode(rotatedToken).getSubject())
                .as("expected the refreshed key set to be in use")
                .isEqualTo(SUBJECT));

        assertThatThrownBy(() -> decoder.decode(originalToken))
            .as("the withdrawn key must not still be accepted from the stale cache")
            .isInstanceOf(JwtException.class);

        assertThat(retrievalAttempts(wireMock))
            .as("expected a retrieval against the recovered endpoint")
            .isPositive();
        assertThat(telemetry.remainingToleranceMs())
            .as("no longer serving stale keys once the refresh has succeeded")
            .isEqualTo(-1L);
    }

    @Test
    @DisplayName("re-anchors the tolerance window to the recovery, so a second outage gets a full window")
    void toleranceWindowIsResetByASuccessfulRetrieval() {
        final long toleranceMs = 2_500L;
        final long runDownTarget = toleranceMs * 3 / 10;
        stubHealthy(wireMock, originalJwkSet());
        NimbusJwtDecoder decoder = decoder(propertiesWithOutageTolerance(toleranceMs));

        assertThat(decoder.decode(originalToken).getSubject()).isEqualTo(SUBJECT);
        final long firstSuccessAt = System.nanoTime();

        // First outage: run it down to under half the window.
        stubDown(wireMock);
        long remainingAfterFirstOutage = runOutageDownTo(decoder, runDownTarget);
        assertThat(remainingAfterFirstOutage).isLessThan(runDownTarget);

        // Recovery.
        appInsights.clear();
        stubHealthy(wireMock, originalJwkSet());
        await().atMost(SETTLE_TIMEOUT).pollInterval(POLL).ignoreExceptions().untilAsserted(() -> {
            decoder.decode(originalToken);
            assertThat(telemetry.remainingToleranceMs())
                .as("expected a successful retrieval to have cleared the outage state")
                .isEqualTo(-1L);
        });

        // Second outage, run down by the same amount again.
        appInsights.clear();
        stubDown(wireMock);
        long firstRemainingOfSecondOutage = firstReportedRemainingTolerance(decoder);

        assertThat(firstRemainingOfSecondOutage)
            .as("the second outage must start from a full window, not from what was left of the first")
            .isGreaterThan(remainingAfterFirstOutage);

        long remainingAfterSecondOutage = runOutageDownTo(decoder, runDownTarget);

        long elapsedSinceFirstSuccessMs = (System.nanoTime() - firstSuccessAt) / 1_000_000L;
        assertThat(elapsedSinceFirstSuccessMs)
            .as("the test must actually outlive one whole tolerance window for this to prove anything")
            .isGreaterThan(toleranceMs);
        assertThat(remainingAfterSecondOutage).isPositive();
        assertThatCode(() -> decoder.decode(originalToken))
            .as("still serving stale keys %dms after the first retrieval, with a %dms window",
                elapsedSinceFirstSuccessMs, toleranceMs)
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("refreshes on the scheduled background thread, with no request thread waiting on the cache lock")
    void refreshHappensOffTheRequestPath() {
        int endpointDelayMs = 2_000;

        stubHealthy(wireMock, originalJwkSet());
        NimbusJwtDecoder decoder = decoder(slowEndpointProperties());

        assertThat(decoder.decode(originalToken).getSubject()).isEqualTo(SUBJECT);

        stubSlow(wireMock, originalJwkSet(), endpointDelayMs);

        await().atMost(SETTLE_TIMEOUT).pollInterval(POLL).untilAsserted(() ->
            assertThat(wireMock.getAllServeEvents())
                .as("expected the scheduled refresh to reach the endpoint without any request being made")
                .isNotEmpty());

        List<Long> decodeDurationsMs = new ArrayList<>();
        await().atMost(SETTLE_TIMEOUT).pollInterval(Duration.ofMillis(10)).untilAsserted(() -> {
            long start = System.nanoTime();
            assertThat(decoder.decode(originalToken).getSubject()).isEqualTo(SUBJECT);
            decodeDurationsMs.add((System.nanoTime() - start) / 1_000_000L);

            assertThat(appInsights.eventsOfType(JwkSourceTelemetry.SCHEDULED_REFRESH_COMPLETED))
                .as("expected the background refresh to complete")
                .isNotEmpty();
        });

        assertThat(decodeDurationsMs)
            .as("expected enough requests to have been served while the slow retrieval was in flight")
            .hasSizeGreaterThan(5);
        assertThat(decodeDurationsMs)
            .as("no request may wait on the %dms retrieval; durations were %s", endpointDelayMs, decodeDurationsMs)
            .allSatisfy(duration -> assertThat(duration).isLessThan(endpointDelayMs / 2L));

        assertThat(appInsights.eventsOfType(JwkSourceTelemetry.WAITING_FOR_REFRESH))
            .as("no request thread should ever have queued on the cache lock")
            .isEmpty();
        assertThat(appInsights.eventsOfType(JwkSourceTelemetry.REFRESH_TIMED_OUT))
            .as("no request thread should ever have timed out waiting for a refresh")
            .isEmpty();
    }

    @Test
    @DisplayName("keeps retrying in the background while the outage is tolerated, and stops once the window closes")
    void backgroundRefreshContinuesOnlyWhileTheOutageIsTolerated() {
        long toleranceMs = 2_000L;
        stubHealthy(wireMock, originalJwkSet());
        NimbusJwtDecoder decoder = decoder(propertiesWithOutageTolerance(toleranceMs));

        assertThat(decoder.decode(originalToken).getSubject()).isEqualTo(SUBJECT);

        stubDown(wireMock);

        await().atMost(SETTLE_TIMEOUT).pollInterval(POLL).until(() -> retrievalAttempts(wireMock) >= 4);

        AtomicInteger lastSample = new AtomicInteger(-1);
        await().atMost(SETTLE_TIMEOUT).pollInterval(Duration.ofMillis(400)).until(() -> {
            int current = retrievalAttempts(wireMock);
            return current == lastSample.getAndSet(current);
        });

        int attemptsAfterWindowClosed = retrievalAttempts(wireMock);
        await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(5))
            .until(() -> retrievalAttempts(wireMock) == attemptsAfterWindowClosed);

        assertThatThrownBy(() -> decoder.decode(originalToken)).isInstanceOf(JwtException.class);
        assertThat(retrievalAttempts(wireMock))
            .as("a request must still drive a retrieval attempt once the scheduler has given up")
            .isGreaterThan(attemptsAfterWindowClosed);
    }

    /**
     * Sends requests during an ongoing outage until the remaining tolerance drops below {@code thresholdMs},
     * returning the last reported value.
     * The background scheduler also continues refresh attempts while the outage is tolerated.
     */
    private long runOutageDownTo(NimbusJwtDecoder decoder, long thresholdMs) {
        await().atMost(SETTLE_TIMEOUT).pollInterval(POLL).ignoreExceptions().untilAsserted(() -> {
            decoder.decode(originalToken);
            assertThat(lastReportedRemainingTolerance()).isNotNegative().isLessThan(thresholdMs);
        });
        return lastReportedRemainingTolerance();
    }

    private long firstReportedRemainingTolerance(NimbusJwtDecoder decoder) {
        await().atMost(SETTLE_TIMEOUT).pollInterval(POLL).ignoreExceptions().untilAsserted(() -> {
            decoder.decode(originalToken);
            assertThat(appInsights.eventsOfType(JwkSourceTelemetry.OUTAGE_TOLERATED)).isNotEmpty();
        });
        return remainingToleranceOf(appInsights.eventsOfType(JwkSourceTelemetry.OUTAGE_TOLERATED).getFirst());
    }

    private long lastReportedRemainingTolerance() {
        List<JwkTestSupport.TrackedEvent> outages =
            appInsights.eventsOfType(JwkSourceTelemetry.OUTAGE_TOLERATED);
        return outages.isEmpty() ? Long.MAX_VALUE : remainingToleranceOf(outages.getLast());
    }

    private static long remainingToleranceOf(JwkTestSupport.TrackedEvent event) {
        return event.metrics().get(JwkSourceTelemetry.REMAINING_TOLERANCE_MS).longValue();
    }

    private NimbusJwtDecoder decoder(JwksProperties properties) {
        jwkSource = new JwkSourceConfiguration(properties).idamJwkSource(telemetry);
        return NimbusJwtDecoder.withJwkSetUri(properties.uri())
            .jwtProcessorCustomizer(processor -> processor.setJWSKeySelector(
                new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource)))
            .build();
    }

    private JwksProperties shortLivedCacheProperties() {
        return new JwksProperties(
            jwksUri(wireMock),
            50,             // connect timeout, ms
            100,            // read timeout, ms
            51200,          // size limit, bytes
            400L,           // cache time to live
            200L,           // cache refresh timeout: above connect + read
            150L,           // refresh ahead time
            // Rate limit interval below the poll interval, so polling cannot exhaust the two retrievals it
            // allows per window.
            10L,
            30_000L         // outage tolerance
        );
    }

    private JwksProperties propertiesWithOutageTolerance(long outageToleranceMs) {
        return new JwksProperties(
            jwksUri(wireMock),
            50,
            100,
            51200,
            400L,
            200L,
            150L,
            10L,
            outageToleranceMs);
    }

    private JwksProperties slowEndpointProperties() {
        return new JwksProperties(
            jwksUri(wireMock),
            200,            // connect timeout, ms
            2_500,          // read timeout, ms: must outlast the 2s endpoint delay
            51200,          // size limit, bytes
            4_000L,         // cache time to live
            2_800L,         // cache refresh timeout: above connect + read (2700)
            900L,           // refresh ahead time
            100L,           // rate limit minimum interval
            60_000L         // outage tolerance
        );
    }
}
