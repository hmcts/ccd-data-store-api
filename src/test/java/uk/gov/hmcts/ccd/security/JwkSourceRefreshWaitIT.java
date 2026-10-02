package uk.gov.hmcts.ccd.security;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.JWKS_PATH;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.ORIGINAL_KEY_ID;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.RecordingAppInsights;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.jwksUri;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.originalJwkSet;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.retrievalAttempts;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.startWireMock;

/**
 * A request thread that finds another thread's retrieval in flight waits on the cache lock for
 * {@code cache-refresh-timeout-ms}. The retry runs inside that lock, so the wait has to outlast every attempt the
 * retrieval can make, not just one.
 *
 * <p>Request threads only reach that lock when the cache cannot answer on its own: at a cold start, after the outage
 * tolerance window has closed, or for a token whose {@code kid} is not in the cached set. The cold start is used here
 * because it needs no set-up.
 *
 * <p>The endpoint makes the first attempt run to its read timeout and answers the retry slowly but successfully, so
 * the retrieval takes longer than one attempt's worst case but succeeds. Timings are scaled down from production's
 * {@code 2 x (2000 + 5000)} so the test runs in seconds.
 */
class JwkSourceRefreshWaitIT {

    private static final int CONNECT_TIMEOUT_MS = 250;
    private static final int READ_TIMEOUT_MS = 1_000;

    /** Above one attempt (250 + 1000 = 1250) but below two: what the previous validation rule accepted. */
    private static final long SINGLE_ATTEMPT_WAIT_MS = 1_300L;

    /** Just above two attempts, 2 x (250 + 1000) = 2500: the smallest wait the current rule accepts. */
    private static final long RETRYING_WAIT_MS = 2_600L;

    /** Longer than the read timeout, so the first attempt fails and is retried. */
    private static final int FIRST_ATTEMPT_DELAY_MS = 3_000;

    /**
     * Under the read timeout, so the retry succeeds. The retrieval then takes roughly 1000 + 900 = 1900ms, which is
     * past the single-attempt wait and inside the retrying one, with about 600ms of margin on each side.
     */
    private static final int RETRY_DELAY_MS = 900;

    private static final String RETRY_STATE = "retry";

    private WireMockServer wireMock;
    private RecordingAppInsights appInsights;
    private JwkSourceTelemetry telemetry;
    private JWKSource<SecurityContext> jwkSource;
    private ExecutorService requestThreads;

    @BeforeEach
    void setUp() {
        wireMock = startWireMock();
        appInsights = new RecordingAppInsights();
        telemetry = new JwkSourceTelemetry(appInsights);
        requestThreads = Executors.newFixedThreadPool(2);
        stubFirstAttemptTimesOutAndRetrySucceeds();
    }

    @AfterEach
    void tearDown() throws IOException {
        requestThreads.shutdownNow();
        if (jwkSource instanceof Closeable closeable) {
            closeable.close();
        }
        wireMock.stop();
    }

    @Test
    @DisplayName("a waiter sized for a single attempt gives up on a retrieval that the retry then completes")
    void waiterSizedForOneAttemptTimesOutDuringARetry() throws Exception {
        assertThatThrownBy(() -> properties(SINGLE_ATTEMPT_WAIT_MS))
            .as("the current rule must reject this wait, which is why the source is built without JwksProperties")
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("2 x (250 + 1000) = 2500");

        // The same layers JwkSourceConfiguration builds, with the wait the previous rule allowed.
        jwkSource = JWKSourceBuilder.<SecurityContext>create(
                URI.create(jwksUri(wireMock)).toURL(),
                new DefaultResourceRetriever(CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS, 51_200))
            .retrying(telemetry.retryingEventListener())
            .outageTolerant(300_000L, telemetry.outageEventListener())
            .rateLimited(1_000L, telemetry.rateLimitedEventListener())
            .cache(60_000L, SINGLE_ATTEMPT_WAIT_MS)
            .refreshAheadCache(5_000L, true, telemetry.cachingEventListener())
            .build();

        Outcome outcome = holderThenWaiter();

        assertThat(outcome.holder()).as("the retrieval itself succeeds, on the retry").isEqualTo(ORIGINAL_KEY_ID);
        assertThat(outcome.waiter())
            .as("but the thread waiting on it has already given up")
            .contains("Timeout while waiting for cache refresh (" + SINGLE_ATTEMPT_WAIT_MS + "ms exceeded)");
        assertThat(appInsights.eventsOfType(JwkSourceTelemetry.REFRESH_TIMED_OUT)).hasSize(1);
        assertRetrievalWasRetried();
    }

    @Test
    @DisplayName("a waiter given the validated wait outlasts a retrieval that needs its retry")
    void waiterSizedForEveryAttemptOutlastsTheRetry() throws Exception {
        jwkSource = new JwkSourceConfiguration(properties(RETRYING_WAIT_MS)).idamJwkSource(telemetry);

        Outcome outcome = holderThenWaiter();

        assertThat(outcome.holder()).isEqualTo(ORIGINAL_KEY_ID);
        assertThat(outcome.waiter())
            .as("the waiter receives the key set the holder retrieved")
            .isEqualTo(ORIGINAL_KEY_ID);
        assertThat(appInsights.eventsOfType(JwkSourceTelemetry.WAITING_FOR_REFRESH))
            .as("the second thread must actually have queued on the lock, or this proves nothing")
            .isNotEmpty();
        assertThat(appInsights.eventsOfType(JwkSourceTelemetry.REFRESH_TIMED_OUT)).isEmpty();
        assertRetrievalWasRetried();
    }

    /**
     * Starts one retrieval, waits until it has reached the endpoint so it holds the cache lock, then starts a second.
     */
    private Outcome holderThenWaiter() throws Exception {
        CompletableFuture<String> holder = CompletableFuture.supplyAsync(this::signingKeyId, requestThreads);

        await().atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(10))
            .until(() -> retrievalAttempts(wireMock) >= 1);

        CompletableFuture<String> waiter = CompletableFuture.supplyAsync(this::signingKeyId, requestThreads);

        return new Outcome(holder.get(10, TimeUnit.SECONDS), waiter.get(10, TimeUnit.SECONDS));
    }

    private void assertRetrievalWasRetried() {
        assertThat(retrievalAttempts(wireMock))
            .as("the first attempt timed out and the retry reached the endpoint; nothing else did")
            .isEqualTo(2);
        assertThat(appInsights.eventsOfType(JwkSourceTelemetry.RETRIAL)).hasSize(1);
    }

    private String signingKeyId() {
        try {
            JWKMatcher matcher = new JWKMatcher.Builder().keyUses(KeyUse.SIGNATURE, null).build();
            List<JWK> keys = jwkSource.get(new JWKSelector(matcher), null);
            return keys.getFirst().getKeyID();
        } catch (Exception e) {
            return e.toString();
        }
    }

    private void stubFirstAttemptTimesOutAndRetrySucceeds() {
        wireMock.stubFor(get(urlEqualTo(JWKS_PATH)).inScenario("slow IDAM")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(jwkSetResponse().withFixedDelay(FIRST_ATTEMPT_DELAY_MS))
            .willSetStateTo(RETRY_STATE));
        wireMock.stubFor(get(urlEqualTo(JWKS_PATH)).inScenario("slow IDAM")
            .whenScenarioStateIs(RETRY_STATE)
            .willReturn(jwkSetResponse().withFixedDelay(RETRY_DELAY_MS)));
    }

    private static ResponseDefinitionBuilder jwkSetResponse() {
        return aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(originalJwkSet());
    }

    private JwksProperties properties(long cacheRefreshTimeoutMs) {
        return new JwksProperties(
            jwksUri(wireMock),
            CONNECT_TIMEOUT_MS,
            READ_TIMEOUT_MS,
            51_200,                 // size limit, bytes
            60_000L,                // cache time to live: far longer than the test
            cacheRefreshTimeoutMs,
            5_000L,                 // refresh ahead time
            1_000L,                 // rate limit minimum interval: the retry sits below it, so uses no permit
            300_000L,               // outage tolerance
            true                    // retrying, as in production
        );
    }

    private record Outcome(String holder, String waiter) {
    }
}
