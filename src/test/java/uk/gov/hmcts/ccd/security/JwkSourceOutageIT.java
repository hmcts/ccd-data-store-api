package uk.gov.hmcts.ccd.security;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
import static uk.gov.hmcts.ccd.security.JwkTestSupport.stubSlow;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.tokenSignedWith;

/**
 * Reproduces the production incident where an unreachable IDAM JWK endpoint caused concurrent requests
 * to fail and verifies that {@link JwkSourceConfiguration} handles the same conditions safely.
 *
 * <p>Both tests follow the same scenario:
 * <ol>
 *     <li>retrieve and cache the JWK set;</li>
 *     <li>make the endpoint unavailable;</li>
 *     <li>let the cache expire;</li>
 *     <li>verify a token from several concurrent requests.</li>
 * </ol>
 *
 * <p>Production timings are scaled down so the tests run in seconds. The cache-lock behaviour remains the same:
 * one thread performs the retrieval while others wait for it.
 *
 * <p>The test poll for the required conditions rather than using fixed sleeps. This avoids flaky timing when
 * tests run in parallel and a thread is not scheduled immediately after the TTL expires.
 */
class JwkSourceOutageIT {

    private static final int CONCURRENT_REQUESTS = 4;

    private static final int OUTAGE_DELAY_MS = 3_000;

    private static final long LEGACY_CACHE_TTL_MS = 300L;
    private static final long LEGACY_CACHE_REFRESH_TIMEOUT_MS = 500L;

    private static final long CURRENT_CACHE_TTL_MS = 2_000L;

    private static final Duration SETTLE_TIMEOUT = Duration.ofSeconds(60);

    private WireMockServer wireMock;
    private JWKSource<SecurityContext> jwkSource;
    private RecordingAppInsights appInsights;
    private String token;

    @BeforeEach
    void setUp() {
        wireMock = startWireMock();
        appInsights = new RecordingAppInsights();
        token = tokenSignedWith(originalKey());
    }

    @AfterEach
    void tearDown() throws IOException {
        if (jwkSource instanceof Closeable closeable) {
            closeable.close();
        }
        wireMock.stop();
    }

    @Test
    @DisplayName("the previous configuration fails concurrent requests with the production error once IDAM stops "
        + "responding")
    void previousConfigurationFailsDuringOutage() {
        stubHealthy(wireMock, originalJwkSet());
        jwkSource = legacyJwkSource();
        NimbusJwtDecoder decoder = decoder(jwkSource);

        assertThat(decoder.decode(token).getSubject()).isEqualTo(SUBJECT);

        stubSlow(wireMock, originalJwkSet(), OUTAGE_DELAY_MS);

        await().atMost(SETTLE_TIMEOUT)
            .pollInterval(Duration.ofMillis(50))
            .untilAsserted(() -> assertThat(decodeConcurrently(decoder).failures())
                .anySatisfy(message -> assertThat(message)
                    .contains("An error occurred while attempting to decode the Jwt")
                    .contains("Timeout while waiting for cache refresh")));
    }

    @Test
    @DisplayName("the current configuration serves every concurrent request from cache through the same outage")
    void currentConfigurationSurvivesOutage() throws Exception {
        stubHealthy(wireMock, originalJwkSet());
        jwkSource = currentConfiguration().idamJwkSource(new JwkSourceTelemetry(appInsights));
        NimbusJwtDecoder decoder = decoder(jwkSource);

        assertThat(decoder.decode(token).getSubject()).isEqualTo(SUBJECT);

        stubSlow(wireMock, originalJwkSet(), OUTAGE_DELAY_MS);

        await().atMost(SETTLE_TIMEOUT)
            .pollInterval(Duration.ofMillis(50))
            .untilAsserted(() -> assertThat(appInsights.eventsOfType(JwkSourceTelemetry.OUTAGE_TOLERATED))
                .as("expected the source to have fallen back to the cached keys")
                .isNotEmpty());

        Outcomes outcomes = decodeConcurrently(decoder);

        assertThat(outcomes.failures()).isEmpty();
        assertThat(outcomes.successes()).hasValue(CONCURRENT_REQUESTS);

        assertThat(retrievalAttempts(wireMock))
            .as("expected at least one retrieval attempt against the unresponsive endpoint")
            .isPositive();
    }

    /**
     * Represents the previous Spring Security configuration: caching without refresh-ahead, rate limiting, retry,
     * or outage tolerance, and no HTTP timeouts on the JWK retriever (0 means unlimited).
     */
    private JWKSource<SecurityContext> legacyJwkSource() {
        return JWKSourceBuilder.<SecurityContext>create(jwksUrl(), new DefaultResourceRetriever(0, 0, 51200))
            .refreshAheadCache(false)
            .rateLimited(false)
            .cache(LEGACY_CACHE_TTL_MS, LEGACY_CACHE_REFRESH_TIMEOUT_MS)
            .build();
    }

    private JwkSourceConfiguration currentConfiguration() {
        return new JwkSourceConfiguration(new JwksProperties(
            jwksUri(wireMock),
            200,                    // connect timeout, ms
            300,                    // read timeout, ms
            51200,                  // size limit, bytes
            CURRENT_CACHE_TTL_MS,   // cache time to live
            1_000L,                 // cache refresh timeout
            500L,                   // refresh ahead time
            50L,                    // rate limit minimum interval
            60_000L                 // outage tolerance
        ));
    }

    private NimbusJwtDecoder decoder(JWKSource<SecurityContext> source) {
        return NimbusJwtDecoder.withJwkSetUri(jwksUri(wireMock))
            .jwtProcessorCustomizer(processor -> processor.setJWSKeySelector(
                new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, source)))
            .build();
    }

    private Outcomes decodeConcurrently(NimbusJwtDecoder decoder) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);
        CountDownLatch startTogether = new CountDownLatch(1);
        List<String> failures = new CopyOnWriteArrayList<>();
        AtomicInteger successes = new AtomicInteger();

        try {
            List<Future<?>> running = new ArrayList<>();
            for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
                running.add(executor.submit(() -> {
                    startTogether.await();
                    try {
                        Jwt jwt = decoder.decode(token);
                        assertThat(jwt.getSubject()).isEqualTo(SUBJECT);
                        successes.incrementAndGet();
                    } catch (Exception e) {
                        failures.add(e.getMessage());
                    }
                    return null;
                }));
            }

            startTogether.countDown();
            for (Future<?> future : running) {
                future.get(OUTAGE_DELAY_MS * 4L, TimeUnit.MILLISECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        return new Outcomes(failures, successes);
    }

    private java.net.URL jwksUrl() {
        try {
            return new URI(jwksUri(wireMock)).toURL();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private record Outcomes(List<String> failures, AtomicInteger successes) {
    }
}
