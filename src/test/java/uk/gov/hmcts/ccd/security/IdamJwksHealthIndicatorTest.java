package uk.gov.hmcts.ccd.security;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.RecordingAppInsights;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.jwksUri;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.originalJwkSet;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.startWireMock;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.stubDown;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.stubHealthy;

class IdamJwksHealthIndicatorTest {

    private static final JWKSelector ANY_KEY = new JWKSelector(new JWKMatcher.Builder().build());
    private static final Duration SETTLE_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration POLL = Duration.ofMillis(25);

    private WireMockServer wireMock;
    private JWKSource<SecurityContext> jwkSource;
    private RecordingAppInsights appInsights;
    private JwkSourceTelemetry telemetry;
    private IdamJwksHealthIndicator indicator;

    @BeforeEach
    void setUp() {
        wireMock = startWireMock();
        appInsights = new RecordingAppInsights();
        telemetry = new JwkSourceTelemetry(appInsights);
        indicator = new IdamJwksHealthIndicator(telemetry);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (jwkSource instanceof Closeable closeable) {
            closeable.close();
        }
        wireMock.stop();
    }

    @Test
    @DisplayName("is DOWN until a usable signing key has been retrieved, then UP")
    void downUntilTheFirstUsableSigningKeyThenUp() {
        assertThat(indicator.health().getStatus()).isEqualTo(Status.DOWN);
        assertThat(indicator.health().getDetails())
            .containsEntry("reason", "No usable IDAM signing key retrieved since start-up");

        telemetry.retrievalSucceeded();

        assertThat(indicator.health().getStatus())
            .as("a key set without a usable signing key")
            .isEqualTo(Status.DOWN);

        telemetry.usableSigningKeyRetrieved();

        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
        assertThat(indicator.health().getDetails()).isEmpty();
    }

    @Test
    @DisplayName("is DOWN after a start-up fallback, and UP once the source retrieves a key set")
    void downAfterAStartUpFallbackUntilTheSourceRetrieves() throws Exception {
        stubDown(wireMock);
        JwkSourceConfiguration configuration = new JwkSourceConfiguration(properties(30_000L));
        jwkSource = configuration.idamJwkSource(telemetry);
        configuration.idamJwsKeySelector(jwkSource, telemetry);

        assertThat(telemetry.algorithmSource()).isEqualTo(JwkSourceTelemetry.AlgorithmSource.FALLBACK);
        assertThat(indicator.health().getStatus()).isEqualTo(Status.DOWN);

        stubHealthy(wireMock, originalJwkSet());
        jwkSource.get(ANY_KEY, null);

        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
    }

    @Test
    @DisplayName("stays UP through an IDAM outage, and after the outage tolerance window has closed")
    void staysUpThroughAnOutageAndPastToleranceExpiry() throws Exception {
        stubHealthy(wireMock, originalJwkSet());
        JwkSourceConfiguration configuration = new JwkSourceConfiguration(properties(1_500L));
        jwkSource = configuration.idamJwkSource(telemetry);
        configuration.idamJwsKeySelector(jwkSource, telemetry);
        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);

        stubDown(wireMock);

        // Every poll checks the indicator, so a single DOWN anywhere in the outage fails the test.
        await().atMost(SETTLE_TIMEOUT).pollInterval(POLL).untilAsserted(() -> {
            assertThat(indicator.health().getStatus()).as("during the outage").isEqualTo(Status.UP);
            assertThatThrownBy(() -> jwkSource.get(ANY_KEY, null))
                .as("the tolerance window has closed, so the source no longer serves the cached keys")
                .isInstanceOf(KeySourceException.class);
        });

        assertThat(appInsights.eventsOfType(JwkSourceTelemetry.OUTAGE_TOLERATED))
            .as("the cached keys were served during the outage before the window closed")
            .isNotEmpty();
        // The telemetry's view of the expiry can trail Nimbus's by up to one retrieval; see remainingToleranceMs().
        await().atMost(SETTLE_TIMEOUT).pollInterval(POLL)
            .untilAsserted(() -> assertThat(telemetry.remainingToleranceMs()).isZero());
        assertThat(indicator.health().getStatus()).as("after the tolerance window").isEqualTo(Status.UP);
    }

    private JwksProperties properties(long outageToleranceMs) {
        return new JwksProperties(
            jwksUri(wireMock),
            50,             // connect timeout, ms
            100,            // read timeout, ms
            51200,          // size limit, bytes
            500L,           // cache time to live
            320L,           // cache refresh timeout: above 2 attempts x (connect + read) = 300
            150L,           // refresh ahead time
            10L,            // rate limit interval, below the poll interval
            outageToleranceMs,
            true);
    }
}
