package uk.gov.hmcts.ccd.security;

import ch.qos.logback.classic.Level;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.awaitility.Awaitility.await;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.CapturedLogs;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.JWKS_PATH;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.ORIGINAL_KEY_ID;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.RecordingAppInsights;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.jwksUri;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.originalJwkSet;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.startWireMock;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.stubDown;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.stubHealthy;

class JwkSourceConfigurationTest {

    private static final long CACHE_TTL_MS = 500L;

    private static final String RSA_FAMILY = "PS256,PS384,PS512,RS256,RS384,RS512";

    private static final String RECOVERED_AFTER_FALLBACK =
        "IDAM JWK set retrieved; key source recovered after start-up fallback";

    private WireMockServer wireMock;
    private JWKSource<SecurityContext> jwkSource;
    private RecordingAppInsights appInsights;
    private CapturedLogs telemetryLogs;
    private CapturedLogs configurationLogs;

    @BeforeEach
    void setUp() {
        wireMock = startWireMock();
        appInsights = new RecordingAppInsights();
        telemetryLogs = new CapturedLogs(JwkSourceTelemetry.class);
        configurationLogs = new CapturedLogs(JwkSourceConfiguration.class);
    }

    @AfterEach
    void tearDown() throws IOException {
        telemetryLogs.close();
        configurationLogs.close();
        if (jwkSource instanceof Closeable closeable) {
            closeable.close();
        }
        wireMock.stop();
    }

    @Test
    @DisplayName("retrieves the signing keys from the JWK set endpoint")
    void retrievesSigningKeys() throws Exception {
        stubHealthy(wireMock, originalJwkSet());
        jwkSource = buildSource();

        assertThat(signingKeys(jwkSource)).extracting(JWK::getKeyID).containsExactly(ORIGINAL_KEY_ID);
    }

    @Test
    @DisplayName("derives the accepted signature algorithms from the JWK set, and emits no fallback marker")
    void derivesSignatureAlgorithms() {
        stubHealthy(wireMock, originalJwkSet());
        // Generous timeouts: this test is about the outcome of healthy start-up retrieval, and the 50/100 ms
        // used elsewhere in this class can time out on a loaded build agent and take the fallback path instead.
        JwkSourceConfiguration configuration = new JwkSourceConfiguration(new JwksProperties(
            jwksUri(wireMock), 1_000, 2_000, 51200, 60_000L, 6_100L, 100L, 50L, 120_000L, true));
        JwkSourceTelemetry telemetry = new JwkSourceTelemetry(appInsights);
        jwkSource = configuration.idamJwkSource(telemetry);

        JWSKeySelector<SecurityContext> keySelector = configuration.idamJwsKeySelector(jwkSource, telemetry);

        assertThat(keySelector).isInstanceOf(JWSVerificationKeySelector.class);
        assertThat(allows(keySelector, JWSAlgorithm.RS256)).isTrue();
        assertThat(allows(keySelector, JWSAlgorithm.PS256))
            .as("the key advertises RS256, so only RS256 is derived; the RSA-family fallback would allow PS256")
            .isFalse();
        assertThat(telemetry.algorithmSource()).isEqualTo(JwkSourceTelemetry.AlgorithmSource.DERIVED);
        assertThat(appInsights.eventsOfType(JwkSourceTelemetry.HEALTH))
            .as("the start-up retrieval must have reported, or the absence of a marker proves nothing")
            .isNotEmpty();
        assertThat(appInsights.eventsOfType(JwkSourceTelemetry.ALGORITHMS_FALLBACK)).isEmpty();

        // An unknown kid makes the source go back to IDAM (or be refused by the rate limiter), and either way
        // it reports. Everything reported from here on was raised after the algorithms were chosen.
        appInsights.clear();
        JWKSelector unknownKid = new JWKSelector(new JWKMatcher.Builder().keyID("no-such-key").build());
        await().atMost(Duration.ofSeconds(10))
            .pollInterval(Duration.ofMillis(25))
            .untilAsserted(() -> {
                try {
                    jwkSource.get(unknownKid, null);
                } catch (Exception e) {
                    // a rate-limited lookup still reports, which is all this needs
                }
                assertThat(appInsights.events()).isNotEmpty();
            });
        assertThat(appInsights.events())
            .as("events raised after start-up carry the algorithm source")
            .allSatisfy(event -> assertThat(event.properties())
                .containsEntry(JwkSourceTelemetry.ALGORITHM_SOURCE, "DERIVED"));
        assertThat(telemetryLogs.startingWith(RECOVERED_AFTER_FALLBACK))
            .as("a pod that derived its algorithms never fell back, so has nothing to recover from")
            .isEmpty();
    }

    @Test
    @DisplayName("derives the RSA family of algorithms when a key does not advertise an explicit one")
    void derivesRsaFamilyWhenAlgorithmIsMissing() throws Exception {
        RSAKey keyWithoutAlgorithm = new RSAKeyGenerator(2048)
            .keyID("rsa-no-alg")
            .keyUse(KeyUse.SIGNATURE)
            .generate();
        JWKSource<SecurityContext> source = new ImmutableJWKSet<>(new JWKSet(keyWithoutAlgorithm.toPublicJWK()));
        JwkSourceConfiguration configuration = configuration();
        JwkSourceTelemetry telemetry = new JwkSourceTelemetry(appInsights);

        JWSKeySelector<SecurityContext> keySelector = configuration.idamJwsKeySelector(source, telemetry);

        assertThat(telemetry.algorithmSource()).isEqualTo(JwkSourceTelemetry.AlgorithmSource.DERIVED);
        assertThat(appInsights.eventsOfType(JwkSourceTelemetry.ALGORITHMS_FALLBACK)).isEmpty();
        assertThat(JWSAlgorithm.Family.RSA).allSatisfy(algorithm -> assertThat(allows(keySelector, algorithm))
            .as("%s is in the RSA family", algorithm)
            .isTrue());
        assertThat(allows(keySelector, JWSAlgorithm.ES256)).isFalse();
    }

    @Test
    @DisplayName("derives the EC family of algorithms when a key does not advertise an explicit one")
    void derivesEcFamilyWhenAlgorithmIsMissing() throws Exception {
        ECKey keyWithoutAlgorithm = new ECKeyGenerator(Curve.P_256)
            .keyID("ec-no-alg")
            .keyUse(KeyUse.SIGNATURE)
            .generate();
        JWKSource<SecurityContext> source = new ImmutableJWKSet<>(new JWKSet(keyWithoutAlgorithm.toPublicJWK()));
        JwkSourceConfiguration configuration = configuration();
        JwkSourceTelemetry telemetry = new JwkSourceTelemetry(appInsights);

        JWSKeySelector<SecurityContext> keySelector = configuration.idamJwsKeySelector(source, telemetry);

        assertThat(telemetry.algorithmSource()).isEqualTo(JwkSourceTelemetry.AlgorithmSource.DERIVED);
        assertThat(appInsights.eventsOfType(JwkSourceTelemetry.ALGORITHMS_FALLBACK)).isEmpty();
        assertThat(JWSAlgorithm.Family.EC).allSatisfy(algorithm -> assertThat(allows(keySelector, algorithm))
            .as("%s is in the EC family", algorithm)
            .isTrue());
        assertThat(allows(keySelector, JWSAlgorithm.RS256)).isFalse();
    }

    @Test
    @DisplayName("falls back to the RSA family when the JWK set advertises no usable signature algorithms")
    void fallsBackWhenNoUsableAlgorithmsAdvertised() {
        JWKSource<SecurityContext> source = new ImmutableJWKSet<>(new JWKSet());
        JwkSourceConfiguration configuration = configuration();

        JwkSourceTelemetry telemetry = new JwkSourceTelemetry(appInsights);

        JWSKeySelector<SecurityContext> keySelector = configuration.idamJwsKeySelector(source, telemetry);

        assertAcceptsTheRsaFamilyButNotEc(keySelector);
        assertThat(telemetry.algorithmSource()).isEqualTo(JwkSourceTelemetry.AlgorithmSource.FALLBACK);
        assertThat(appInsights.eventsOfType(JwkSourceTelemetry.ALGORITHMS_FALLBACK))
            .singleElement()
            .satisfies(event -> assertThat(event.properties())
                .containsEntry(JwkSourceTelemetry.EXCEPTION_TYPE, "NONE")
                .containsEntry(JwkSourceTelemetry.ALGORITHMS, RSA_FAMILY));
    }

    @Test
    @DisplayName("falls back to the RSA family rather than failing start-up when the JWK set cannot be retrieved, "
        + "and keeps marking events FALLBACK after IDAM recovers")
    void fallsBackToRsaFamilyWhenJwkSetUnavailable() throws Exception {
        stubDown(wireMock);
        JwkSourceConfiguration configuration = configuration();
        JwkSourceTelemetry telemetry = new JwkSourceTelemetry(appInsights);
        jwkSource = configuration.idamJwkSource(telemetry);

        JWSKeySelector<SecurityContext> keySelector = configuration.idamJwsKeySelector(jwkSource, telemetry);

        assertAcceptsTheRsaFamilyButNotEc(keySelector);
        assertThat(JwkTestSupport.retrievalAttempts(wireMock))
            .as("expected the start-up retrieval to have actually been attempted")
            .isPositive();
        assertThat(appInsights.eventsOfType(JwkSourceTelemetry.ALGORITHMS_FALLBACK))
            .singleElement()
            .satisfies(event -> assertThat(event.properties())
                .containsEntry(JwkSourceTelemetry.ALGORITHM_SOURCE, "FALLBACK")
                .containsEntry(JwkSourceTelemetry.ALGORITHMS, RSA_FAMILY)
                .containsEntry(JwkSourceTelemetry.EXCEPTION_TYPE, "JWKSetRetrievalException"));

        // The key source recovers; the algorithm set does not, so the events must go on saying so.
        appInsights.clear();
        stubHealthy(wireMock, originalJwkSet());
        await().atMost(Duration.ofSeconds(10))
            .pollInterval(Duration.ofMillis(25))
            .ignoreExceptions()
            .untilAsserted(() -> assertThat(signingKeys(jwkSource)).hasSize(1));
        assertThat(appInsights.eventsOfType(JwkSourceTelemetry.HEALTH))
            .as("the recovery retrieval must have reported")
            .isNotEmpty()
            .allSatisfy(event -> assertThat(event.properties())
                .containsEntry(JwkSourceTelemetry.ALGORITHM_SOURCE, "FALLBACK"));
    }

    @Test
    @DisplayName("logs the start-up fallback with its stack trace, then logs recovery once, on the first real "
        + "retrieval and not on later ones")
    void logsRecoveryOnceAfterAStartUpFallback() throws Exception {
        stubDown(wireMock);
        JwkSourceConfiguration configuration = configuration();
        JwkSourceTelemetry telemetry = new JwkSourceTelemetry(appInsights);
        jwkSource = configuration.idamJwkSource(telemetry);

        configuration.idamJwsKeySelector(jwkSource, telemetry);

        assertThat(configurationLogs.startingWith("Unable to retrieve IDAM JWK set from"))
            .singleElement()
            .satisfies(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                assertThat(event.getThrowableProxy())
                    .as("fires once per pod, so keeps the full detail")
                    .isNotNull();
            });
        assertThat(telemetryLogs.startingWith(RECOVERED_AFTER_FALLBACK)).isEmpty();

        stubHealthy(wireMock, originalJwkSet());
        await().atMost(Duration.ofSeconds(10))
            .pollInterval(Duration.ofMillis(25))
            .ignoreExceptions()
            .untilAsserted(() -> {
                assertThat(signingKeys(jwkSource)).hasSize(1);
                assertThat(JwkTestSupport.retrievalAttempts(wireMock))
                    .as("a further successful retrieval after the one that recovered")
                    .isGreaterThanOrEqualTo(2);
            });

        assertThat(telemetryLogs.startingWith(RECOVERED_AFTER_FALLBACK))
            .singleElement()
            .satisfies(event -> assertThat(event.getLevel()).isEqualTo(Level.INFO));
    }

    @Test
    @DisplayName("keeps serving the cached keys once the JWK set endpoint stops responding")
    void servesCachedKeysThroughAnOutage() throws Exception {
        stubHealthy(wireMock, originalJwkSet());
        jwkSource = buildSource();

        assertThat(signingKeys(jwkSource)).hasSize(1);

        stubDown(wireMock);

        await().atMost(Duration.ofSeconds(10))
            .pollInterval(Duration.ofMillis(25))
            .untilAsserted(() -> {
                assertThat(signingKeys(jwkSource)).extracting(JWK::getKeyID).containsExactly(ORIGINAL_KEY_ID);
                assertThat(appInsights.eventsOfType(JwkSourceTelemetry.OUTAGE_TOLERATED)).isNotEmpty();
            });

        assertThat(JwkTestSupport.retrievalAttempts(wireMock))
            .as("expected at least one retrieval attempt against the failing endpoint")
            .isPositive();
    }

    @Test
    @DisplayName("reports the remaining outage tolerance to Application Insights")
    void reportsRemainingOutageTolerance() throws Exception {
        stubHealthy(wireMock, originalJwkSet());
        JwkSourceTelemetry telemetry = new JwkSourceTelemetry(appInsights);
        jwkSource = configuration().idamJwkSource(telemetry);

        assertThat(signingKeys(jwkSource)).hasSize(1);
        assertThat(telemetry.remainingToleranceMs())
            .as("no outage yet, so nothing to report")
            .isEqualTo(-1L);

        stubDown(wireMock);

        await().atMost(Duration.ofSeconds(10))
            .pollInterval(Duration.ofMillis(25))
            .untilAsserted(() -> {
                signingKeys(jwkSource);
                assertThat(appInsights.eventsOfType(JwkSourceTelemetry.OUTAGE_TOLERATED)).isNotEmpty();
            });

        JwkTestSupport.TrackedEvent outage =
            appInsights.eventsOfType(JwkSourceTelemetry.OUTAGE_TOLERATED).getFirst();

        assertThat(outage.name()).isEqualTo(JwkSourceTelemetry.EVENT_NAME);
        assertThat(outage.metrics())
            .as("the remaining tolerance must be a measurement, so a KQL query can aggregate it directly")
            .containsKey(JwkSourceTelemetry.REMAINING_TOLERANCE_MS);
        assertThat(outage.metrics().get(JwkSourceTelemetry.REMAINING_TOLERANCE_MS))
            .isPositive()
            .isLessThanOrEqualTo((double) OUTAGE_TOLERANCE_MS);
        assertThat(outage.properties()).containsEntry(JwkSourceTelemetry.EXCEPTION_TYPE, "JWKSetRetrievalException");
        assertThat(telemetry.remainingToleranceMs()).isPositive();
    }

    @Test
    @DisplayName("reports HEALTHY throughout a tolerated outage, because health sits above outage tolerance")
    void reportsHealthyWhileTheOutageIsAbsorbed() throws Exception {
        stubHealthy(wireMock, originalJwkSet());
        jwkSource = buildSource();

        assertThat(signingKeys(jwkSource)).hasSize(1);
        assertThat(appInsights.eventsOfType(JwkSourceTelemetry.HEALTH))
            .isNotEmpty()
            .allSatisfy(event -> assertThat(event.properties()).containsEntry("healthStatus", "HEALTHY"));

        appInsights.clear();
        stubDown(wireMock);

        await().atMost(Duration.ofSeconds(10))
            .pollInterval(Duration.ofMillis(25))
            .untilAsserted(() -> {
                signingKeys(jwkSource);
                assertThat(appInsights.eventsOfType(JwkSourceTelemetry.OUTAGE_TOLERATED)).isNotEmpty();
            });

        assertThat(appInsights.eventsOfType(JwkSourceTelemetry.HEALTH))
            .isNotEmpty()
            .allSatisfy(event -> assertThat(event.properties()).containsEntry("healthStatus", "HEALTHY"));
    }

    @Test
    @DisplayName("clears the outage state only when IDAM has actually answered again")
    void clearsOutageStateOnlyOnARealRetrieval() throws Exception {
        stubHealthy(wireMock, originalJwkSet());
        JwkSourceTelemetry telemetry = new JwkSourceTelemetry(appInsights);
        jwkSource = configuration().idamJwkSource(telemetry);

        assertThat(signingKeys(jwkSource)).hasSize(1);

        stubDown(wireMock);
        await().atMost(Duration.ofSeconds(10))
            .pollInterval(Duration.ofMillis(25))
            .untilAsserted(() -> {
                signingKeys(jwkSource);
                assertThat(telemetry.remainingToleranceMs())
                    .as("should stay set for as long as stale keys are being served")
                    .isPositive();
            });

        stubHealthy(wireMock, originalJwkSet());
        await().atMost(Duration.ofSeconds(10))
            .pollInterval(Duration.ofMillis(25))
            .untilAsserted(() -> {
                signingKeys(jwkSource);
                assertThat(telemetry.remainingToleranceMs()).isEqualTo(-1L);
            });

        assertThat(appInsights.eventsOfType(JwkSourceTelemetry.OUTAGE_ENDED))
            .as("the recovery must be reported, not just the outage")
            .isNotEmpty();
    }

    @ParameterizedTest(name = "body: {0}")
    @ValueSource(strings = {"{\"error\":\"maintenance\"}", "<html><body>Service Unavailable</body></html>"})
    @DisplayName("does not report recovery when IDAM answers HTTP 200 with a body that is not a JWK set")
    void doesNotReportRecoveryForAnInvalidJwkSetBody(String body) throws Exception {
        stubHealthy(wireMock, originalJwkSet());
        JwkSourceTelemetry telemetry = new JwkSourceTelemetry(appInsights);
        jwkSource = configuration().idamJwkSource(telemetry);

        assertThat(signingKeys(jwkSource)).hasSize(1);

        stubDown(wireMock);
        await().atMost(Duration.ofSeconds(10))
            .pollInterval(Duration.ofMillis(25))
            .untilAsserted(() -> {
                signingKeys(jwkSource);
                assertThat(telemetry.remainingToleranceMs()).isPositive();
            });

        appInsights.clear();
        wireMock.resetAll();
        wireMock.stubFor(get(urlEqualTo(JWKS_PATH)).willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(body)));

        await().atMost(Duration.ofSeconds(10))
            .pollInterval(Duration.ofMillis(25))
            .untilAsserted(() -> {
                signingKeys(jwkSource);
                assertThat(appInsights.eventsOfType(JwkSourceTelemetry.OUTAGE_TOLERATED))
                    .hasSizeGreaterThanOrEqualTo(3)
                    .allSatisfy(event -> assertThat(event.properties())
                        .containsEntry(JwkSourceTelemetry.EXCEPTION_TYPE, "JWKSetParseException"));
            });

        assertThat(appInsights.eventsOfType(JwkSourceTelemetry.OUTAGE_ENDED))
            .as("an unparseable 200 is a failed retrieval, not IDAM recovering")
            .isEmpty();
        assertThat(telemetry.remainingToleranceMs())
            .as("stale keys are still being served, so the outage state must not be cleared")
            .isPositive();
        assertThat(signingKeys(jwkSource)).extracting(JWK::getKeyID).containsExactly(ORIGINAL_KEY_ID);
    }

    @Test
    @DisplayName("makes exactly the number of attempts per retrieval that the refresh timeout is validated against")
    void makesTheAttemptsTheValidationBudgetsFor() {
        stubDown(wireMock);

        for (boolean retrying : new boolean[] {true, false}) {
            wireMock.resetRequests();
            JwksProperties properties = properties(retrying);
            JWKSource<SecurityContext> source = new JwkSourceConfiguration(properties)
                .idamJwkSource(new JwkSourceTelemetry(appInsights));
            try {
                assertThatThrownBy(() -> signingKeys(source)).isInstanceOf(Exception.class);

                assertThat(JwkTestSupport.retrievalAttempts(wireMock))
                    .as("retrying=%s: JwksProperties.NIMBUS_RETRIES must match RetryingJWKSetSource", retrying)
                    .isEqualTo(properties.retrievalAttempts());
            } finally {
                closeQuietly(source);
            }
        }
    }

    private static void closeQuietly(JWKSource<SecurityContext> source) {
        if (source instanceof Closeable closeable) {
            try {
                closeable.close();
            } catch (IOException e) {
                // nothing further to release
            }
        }
    }

    private static final long OUTAGE_TOLERANCE_MS = 60_000L;

    private JWKSource<SecurityContext> buildSource() {
        return configuration().idamJwkSource(new JwkSourceTelemetry(appInsights));
    }

    private JwkSourceConfiguration configuration() {
        return new JwkSourceConfiguration(properties(true));
    }

    private JwksProperties properties(boolean retrying) {
        return new JwksProperties(
            jwksUri(wireMock),
            50,     // connect timeout, ms
            100,                    // read timeout, ms
            51200,                  // size limit, bytes
            CACHE_TTL_MS,           // cache time to live
            320L,                   // cache refresh timeout: above 2 attempts x (connect + read) = 300
            100L,                   // refresh ahead time
            50L,                    // rate limit minimum interval
            OUTAGE_TOLERANCE_MS,    // outage tolerance
            retrying
        );
    }

    /**
     * What a healthy start-up derives from IDAM's real signing key, which publishes no {@code alg}: every RSA-family
     * algorithm, PS256 and RS512 included, is inside the boundary, and every EC one is outside it.
     */
    private void assertAcceptsTheRsaFamilyButNotEc(JWSKeySelector<SecurityContext> keySelector) {
        assertThat(JWSAlgorithm.Family.RSA).allSatisfy(algorithm -> assertThat(allows(keySelector, algorithm))
            .as("%s is in the RSA family", algorithm)
            .isTrue());
        assertThat(JWSAlgorithm.Family.EC).allSatisfy(algorithm -> assertThat(allows(keySelector, algorithm))
            .as("%s is not in the RSA family", algorithm)
            .isFalse());
    }

    @SuppressWarnings("unchecked")
    private boolean allows(JWSKeySelector<SecurityContext> keySelector, JWSAlgorithm algorithm) {
        return ((JWSVerificationKeySelector<SecurityContext>) keySelector).isAllowed(algorithm);
    }

    private List<JWK> signingKeys(JWKSource<SecurityContext> source) throws Exception {
        JWKMatcher matcher = new JWKMatcher.Builder().keyUses(KeyUse.SIGNATURE, null).build();
        return source.get(new JWKSelector(matcher), null);
    }
}
