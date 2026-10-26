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
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.function.IntSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.RecordingAppInsights;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.SUBJECT;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.jwksUri;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.originalJwkSet;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.originalKey;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.retrievalAttempts;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.startWireMock;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.stubHealthy;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.stubTrickling;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.tokenSignedWith;

/**
 * A ready pod with a warm cache: the scheduled refresh reaches an IDAM that streams a valid key set slowly, each chunk
 * inside the read timeout, so the per-read timeout never fires, but the whole response well past the cache's expiry.
 * The refresh holds the cache lock for as long as the retrieval runs, so without a deadline a request that arrives
 * after the expiry queues on that lock and fails with "Timeout while waiting for cache refresh", even though its
 * {@code kid} is in the cached set.
 *
 * <p>With the deadline, each attempt is abandoned after {@value #DEADLINE_MS}ms, the retry is too, and the
 * outage-tolerant layer serves the cached keys, which the cache stores again. The request finds a fresh entry.
 *
 * <p>The second test is the same, but IDAM sends the chunk framing slowly: the extension of the key set's chunk-size
 * line, a byte every 100ms for 10s. The JDK reads that line inside one {@code read()} that returns no data until it
 * ends; the deadline ({@link TimeLimitedResourceRetriever}) ends the attempt all the same.
 *
 * <p>Timeline from the first retrieval, in ms: the scheduled refresh starts at 500 (TTL 4400 - refresh ahead 500 -
 * refresh timeout 3400). Each attempt is abandoned at its 600ms deadline: by about 1100 and 1700. The re-cached set
 * lasts until about 6100. The request is made at 4700, after the first entry has expired, and 1400ms before the
 * re-cached entry expires. If the scheduler runs late, the request finds the refresh still in flight and waits for
 * it, for at most the validated worst case {@code 2 x 600 = 1200ms}, inside the 3400ms it is allowed to wait.
 */
class JwkSourceRetrievalDeadlineIT {

    private static final int CONNECT_TIMEOUT_MS = 200;
    private static final int READ_TIMEOUT_MS = 1_000;
    /** Room for a healthy retrieval on a loaded build, and low enough for the wait below to be validated. */
    private static final int DEADLINE_MS = 600;
    private static final long CACHE_TTL_MS = 4_400L;
    /** Above 2 x deadline = 1200, and short enough for a 10s trickle to outlast it. */
    private static final long CACHE_REFRESH_TIMEOUT_MS = 3_400L;
    private static final long REFRESH_AHEAD_MS = 500L;

    /**
     * 20 chunks over 10s: one every 500ms, half the read timeout, so no single read would time out. Without the
     * deadline, one attempt runs past the request's 3400ms wait (500 + 10000 > 4700 + 3400).
     */
    private static final int TRICKLE_CHUNKS = 20;
    private static final int TRICKLE_TOTAL_MS = 10_000;
    /** 100 bytes, one every 100ms: 10s, like the body trickle. */
    private static final int TRICKLE_FRAMING_BYTES = 100;

    private WireMockServer wireMock;
    private RecordingAppInsights appInsights;
    private JwkSourceTelemetry telemetry;
    private JWKSource<SecurityContext> jwkSource;

    @BeforeEach
    void setUp() {
        wireMock = startWireMock();
        appInsights = new RecordingAppInsights();
        telemetry = new JwkSourceTelemetry(appInsights);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (jwkSource instanceof Closeable closeable) {
            closeable.close();
        }
        wireMock.stop();
    }

    @Test
    @DisplayName("a valid kid is served from the cached keys after expiry while the scheduled refresh trickles")
    void validKidIsServedWhileTheScheduledRefreshTrickles() {
        stubHealthy(wireMock, originalJwkSet());

        validKidIsServedWhileTheScheduledRefreshIsStuck(jwksUri(wireMock),
            () -> stubTrickling(wireMock, originalJwkSet(), TRICKLE_CHUNKS, TRICKLE_TOTAL_MS),
            () -> retrievalAttempts(wireMock));
    }

    @Test
    @DisplayName("a valid kid is served from the cached keys after expiry while the scheduled refresh is stuck on "
        + "slow chunk framing")
    void validKidIsServedWhileTheScheduledRefreshIsStuckOnSlowChunkFraming() throws IOException {
        try (RawJwksServer server = new RawJwksServer(RawJwksServer.healthy())) {
            validKidIsServedWhileTheScheduledRefreshIsStuck(server.uri(),
                () -> server.respondWith(RawJwksServer.slowChunkExtension(TRICKLE_FRAMING_BYTES)),
                () -> server.requests() - 1);
        }
    }

    /**
     * Loads the key set, makes IDAM answer slowly, and decodes a token with a valid {@code kid} after the loaded set
     * has expired.
     *
     * @param retrievalAttempts the requests made since the load
     */
    private void validKidIsServedWhileTheScheduledRefreshIsStuck(String uri, Runnable makeIdamSlow,
                                                                 IntSupplier retrievalAttempts) {
        String token = tokenSignedWith(originalKey());
        NimbusJwtDecoder decoder = decoder(properties(uri));

        assertThat(decoder.decode(token).getSubject()).isEqualTo(SUBJECT);
        long loadedAt = System.nanoTime();
        int loads = retrievalAttempts.getAsInt();
        makeIdamSlow.run();

        await().atMost(Duration.ofSeconds(5)).until(() -> retrievalAttempts.getAsInt() > loads);
        // Past the expiry of the set cached at start, so the request cannot be answered from the original entry.
        await().pollDelay(Duration.ofMillis(CACHE_TTL_MS + 300)).until(() ->
            System.nanoTime() - loadedAt > Duration.ofMillis(CACHE_TTL_MS + 300).toNanos());

        long start = System.nanoTime();
        String outcome;
        try {
            outcome = decoder.decode(token).getSubject();
        } catch (RuntimeException e) {
            outcome = e.toString();
        }
        long decodeMs = (System.nanoTime() - start) / 1_000_000L;

        assertThat(outcome)
            .as("decode of a valid kid 300ms after the cache expired took %dms", decodeMs)
            .isEqualTo(SUBJECT);
        assertThat(decodeMs).as("served without waiting out the cache lock").isLessThan(CACHE_REFRESH_TIMEOUT_MS);
        assertThat(appInsights.eventsOfType(JwkSourceTelemetry.REFRESH_TIMED_OUT)).isEmpty();

        assertThat(appInsights.eventsOfType(JwkSourceTelemetry.RETRIAL))
            .as("the first attempt was cut off at the deadline and retried")
            .isNotEmpty()
            .first()
            .satisfies(event -> assertThat(event.properties().get(JwkSourceTelemetry.DETAIL))
                .contains("IDAM JWK set retrieval exceeded its " + DEADLINE_MS + "ms deadline"));
        assertThat(appInsights.eventsOfType(JwkSourceTelemetry.OUTAGE_TOLERATED))
            .as("the retry was cut off too, so the cached keys were served")
            .isNotEmpty()
            .first()
            .satisfies(event -> assertThat(event.properties().get(JwkSourceTelemetry.DETAIL))
                .contains("IDAM JWK set retrieval exceeded its " + DEADLINE_MS + "ms deadline"));
        assertThat(retrievalAttempts.getAsInt() - loads).as("both attempts reached the slow endpoint")
            .isGreaterThan(1);
    }

    private NimbusJwtDecoder decoder(JwksProperties properties) {
        jwkSource = new JwkSourceConfiguration(properties).idamJwkSource(telemetry);
        return NimbusJwtDecoder.withJwkSetUri(properties.uri())
            .jwtProcessorCustomizer(processor -> processor.setJWSKeySelector(
                new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource)))
            .build();
    }

    private JwksProperties properties(String uri) {
        return new JwksProperties(
            uri,
            CONNECT_TIMEOUT_MS,
            READ_TIMEOUT_MS,
            DEADLINE_MS,
            51200,
            CACHE_TTL_MS,
            CACHE_REFRESH_TIMEOUT_MS,
            REFRESH_AHEAD_MS,
            100L,
            60_000L,
            true);
    }
}
