package uk.gov.hmcts.ccd.security;

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

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.ORIGINAL_KEY_ID;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.RecordingAppInsights;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.jwksUri;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.originalJwkSet;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.startWireMock;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.stubDown;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.stubHealthy;

class JwkSourceConfigurationTest {

    private static final long CACHE_TTL_MS = 300L;

    private WireMockServer wireMock;
    private JWKSource<SecurityContext> jwkSource;
    private RecordingAppInsights appInsights;

    @BeforeEach
    void setUp() {
        wireMock = startWireMock();
        appInsights = new RecordingAppInsights();
    }

    @AfterEach
    void tearDown() throws IOException {
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
    @DisplayName("derives the accepted signature algorithms from the JWK set")
    void derivesSignatureAlgorithms() {
        stubHealthy(wireMock, originalJwkSet());
        JwkSourceConfiguration configuration = configuration();
        jwkSource = configuration.idamJwkSource(new JwkSourceTelemetry(appInsights));

        JWSKeySelector<SecurityContext> keySelector = configuration.idamJwsKeySelector(jwkSource);

        assertThat(keySelector).isInstanceOf(JWSVerificationKeySelector.class);
        assertThat(allows(keySelector, JWSAlgorithm.RS256)).isTrue();
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

        JWSKeySelector<SecurityContext> keySelector = configuration.idamJwsKeySelector(source);

        assertThat(allows(keySelector, JWSAlgorithm.RS256)).isTrue();
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

        JWSKeySelector<SecurityContext> keySelector = configuration.idamJwsKeySelector(source);

        assertThat(allows(keySelector, JWSAlgorithm.ES256)).isTrue();
    }

    @Test
    @DisplayName("falls back to RS256 when the JWK set advertises no usable signature algorithms")
    void fallsBackWhenNoUsableAlgorithmsAdvertised() {
        JWKSource<SecurityContext> source = new ImmutableJWKSet<>(new JWKSet());
        JwkSourceConfiguration configuration = configuration();

        JWSKeySelector<SecurityContext> keySelector = configuration.idamJwsKeySelector(source);

        assertThat(allows(keySelector, JWSAlgorithm.RS256)).isTrue();
    }

    @Test
    @DisplayName("falls back to RS256 rather than failing start-up when the JWK set cannot be retrieved")
    void fallsBackToRs256WhenJwkSetUnavailable() {
        stubDown(wireMock);
        JwkSourceConfiguration configuration = configuration();
        jwkSource = configuration.idamJwkSource(new JwkSourceTelemetry(appInsights));

        JWSKeySelector<SecurityContext> keySelector = configuration.idamJwsKeySelector(jwkSource);

        assertThat(allows(keySelector, JWSAlgorithm.RS256)).isTrue();
        assertThat(JwkTestSupport.retrievalAttempts(wireMock))
            .as("expected the start-up retrieval to have actually been attempted")
            .isPositive();
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

    private static final long OUTAGE_TOLERANCE_MS = 60_000L;

    private JWKSource<SecurityContext> buildSource() {
        return configuration().idamJwkSource(new JwkSourceTelemetry(appInsights));
    }

    private JwkSourceConfiguration configuration() {
        return new JwkSourceConfiguration(new JwksProperties(
            jwksUri(wireMock),
            50,                     // connect timeout, ms
            100,                    // read timeout, ms
            51200,                  // size limit, bytes
            CACHE_TTL_MS,           // cache time to live
            180L,                   // cache refresh timeout: above connect + read, below the TTL
            100L,                   // refresh ahead time
            50L,                    // rate limit minimum interval
            OUTAGE_TOLERANCE_MS     // outage tolerance
        ));
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
