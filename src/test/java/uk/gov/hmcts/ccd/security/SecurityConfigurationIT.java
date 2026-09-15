package uk.gov.hmcts.ccd.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import jakarta.inject.Inject;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.TestPropertySource;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.security.filters.ExceptionHandlingFilter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the wiring shared by {@code SecurityConfiguration} and {@link JwkSourceConfiguration}.
 *
 * <p>The {@code jwtDecoder} now receives a {@link JWSKeySelector} from {@link JwkSourceConfiguration}
 * instead of building one from the issuer. This test ensures that wiring is correct.
 *
 * <p>The JWK endpoint points to an unused port, verifying that the application can start without IDAM.
 * Previously, {@code JwtDecoders.fromOidcIssuerLocation} fetched the OpenID configuration during startup
 * and failed the application if IDAM was unavailable.
 */
@TestPropertySource(properties = {
    "oidc.jwks.uri=http://localhost:1/o/jwks",
    "oidc.jwks.connect-timeout-ms=250",
    "oidc.jwks.read-timeout-ms=250",
    "oidc.jwks.cache-ttl-ms=60000",
    "oidc.jwks.cache-refresh-timeout-ms=1000",
    "oidc.jwks.refresh-ahead-time-ms=5000",
    "oidc.jwks.rate-limit-min-interval-ms=1000",
    "oidc.jwks.outage-tolerance-ms=300000"
})
class SecurityConfigurationIT extends WireMockBaseTest {

    @Inject
    private JWKSource<SecurityContext> idamJwkSource;

    @Inject
    private JWSKeySelector<SecurityContext> idamJwsKeySelector;

    @Inject
    private JwtDecoder jwtDecoder;

    @Inject
    private JwkSourceTelemetry jwkSourceTelemetry;

    @Inject
    private List<SecurityFilterChain> securityFilterChains;

    @Test
    @DisplayName("starts with the JWK set endpoint unreachable")
    void contextStartsWithTheJwkSetEndpointDead() {
        assertThat(idamJwkSource).isNotNull();
        assertThat(jwtDecoder).isNotNull();
    }

    @Test
    @DisplayName("wires the JWK source into the key selector and the key selector into the decoder")
    void wiresTheJwkSourceThroughToTheDecoder() {
        assertThat(idamJwsKeySelector).isInstanceOf(JWSVerificationKeySelector.class);
        assertThat(jwtDecoder)
            .as("the decoder is wrapped so JWT failures reach Application Insights")
            .isInstanceOf(AppInsightsJwtDecoder.class);
    }

    @Test
    @DisplayName("falls back to RS256 when the algorithms cannot be derived from an unreachable JWK set")
    void fallsBackToRs256WhenTheJwkSetCannotBeRead() {
        @SuppressWarnings("unchecked")
        JWSVerificationKeySelector<SecurityContext> keySelector =
            (JWSVerificationKeySelector<SecurityContext>) idamJwsKeySelector;

        assertThat(JwkSourceConfiguration.FALLBACK_ALGORITHMS).containsExactly(JWSAlgorithm.RS256);
        assertThat(keySelector.isAllowed(JWSAlgorithm.RS256)).isTrue();
        assertThat(keySelector.isAllowed(JWSAlgorithm.ES256))
            .as("only the fallback algorithms are accepted when the key set could not be read")
            .isFalse();
    }

    @Test
    @DisplayName("publishes JWK source telemetry through a live AppInsights bean")
    void telemetryIsWired() {
        assertThat(jwkSourceTelemetry).isNotNull();
        assertThat(jwkSourceTelemetry.remainingToleranceMs()).isEqualTo(-1L);
    }

    @Test
    @DisplayName("keeps the exception handling filter outside the bearer token filter")
    void exceptionHandlingFilterRunsOutsideTheBearerTokenFilter() {
        List<Filter> filters = securityFilterChains.stream()
            .flatMap(chain -> chain.getFilters().stream())
            .toList();

        int exceptionHandlingIndex = indexOf(filters, ExceptionHandlingFilter.class);
        int bearerTokenIndex = indexOf(filters, BearerTokenAuthenticationFilter.class);

        assertThat(exceptionHandlingIndex).as("ExceptionHandlingFilter must be in the chain").isNotNegative();
        assertThat(bearerTokenIndex).as("BearerTokenAuthenticationFilter must be in the chain").isNotNegative();
        assertThat(exceptionHandlingIndex)
            .as("ExceptionHandlingFilter must run before BearerTokenAuthenticationFilter, so that an "
                + "AuthenticationServiceException rethrown by the failure handler is caught and turned into a 500")
            .isLessThan(bearerTokenIndex);
    }

    private static int indexOf(List<Filter> filters, Class<? extends Filter> type) {
        for (int i = 0; i < filters.size(); i++) {
            if (type.isInstance(filters.get(i))) {
                return i;
            }
        }
        return -1;
    }
}
