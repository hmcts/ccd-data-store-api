package uk.gov.hmcts.ccd.security;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.EventTelemetry;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import jakarta.inject.Inject;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.mockito.ArgumentCaptor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockReset;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.security.filters.ExceptionHandlingFilter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * Covers the wiring shared by {@code SecurityConfiguration} and {@link JwkSourceConfiguration}.
 *
 * <p>The {@code jwtDecoder} now receives a {@link JWSKeySelector} from {@link JwkSourceConfiguration}
 * instead of building one from the issuer. This test ensures that wiring is correct.
 *
 * <p>The JWK endpoint points to an unused port, verifying that the decoder no longer needs IDAM at start-up.
 * Previously, {@code JwtDecoders.fromOidcIssuerLocation} fetched the OpenID configuration while the decoder
 * was created and failed the application if IDAM was unavailable.
 *
 * <p>This does not show that the application starts without IDAM: {@code TestIdamConfiguration} replaces the
 * {@code ClientRegistrationRepository} in every Spring test. {@link ClientRegistrationStartupTest} covers that, by
 * building Boot's own repository from the production properties. See CCD-8077.
 */
@TestPropertySource(properties = {
    "oidc.jwks.uri=http://localhost:1/o/jwks",
    "oidc.jwks.connect-timeout-ms=250",
    "oidc.jwks.read-timeout-ms=250",
    "oidc.jwks.cache-ttl-ms=60000",
    "oidc.jwks.cache-refresh-timeout-ms=1100",
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

    /**
     * Spied rather than replaced, so the start-up marker is shown to reach the real client that the
     * {@code AppInsights} bean publishes through. The fallback happens while the context starts, before any test
     * runs, so the recorded calls must not be reset between tests.
     */
    @MockitoSpyBean(reset = MockReset.NONE)
    private TelemetryClient telemetryClient;

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

        assertThat(jwkSourceTelemetry.algorithmSource()).isEqualTo(JwkSourceTelemetry.AlgorithmSource.FALLBACK);
        assertThat(standaloneJwkSourceEvents())
            .as("the fallback is reported once, at start-up, through the live telemetry client, outside any trace")
            .filteredOn(event -> JwkSourceTelemetry.ALGORITHMS_FALLBACK.equals(
                event.getProperties().get(JwkSourceTelemetry.EVENT_TYPE)))
            .singleElement()
            .satisfies(event -> {
                assertThat(event.getProperties())
                    .containsEntry(JwkSourceTelemetry.ALGORITHM_SOURCE, "FALLBACK")
                    .containsEntry(JwkSourceTelemetry.ALGORITHMS, "RS256")
                    .containsEntry(JwkSourceTelemetry.EXCEPTION_TYPE, "JWKSetRetrievalException");
                assertThat(event.getContext().getOperation().getId())
                    .as("its own operation id, so the agent does not sample it with a request")
                    .matches("[0-9a-f]{32}");
            });
    }

    private List<EventTelemetry> standaloneJwkSourceEvents() {
        ArgumentCaptor<EventTelemetry> events = ArgumentCaptor.forClass(EventTelemetry.class);
        verify(telemetryClient, atLeastOnce()).trackEvent(events.capture());
        return events.getAllValues().stream()
            .filter(event -> JwkSourceTelemetry.EVENT_NAME.equals(event.getName()))
            .toList();
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

    @Test
    @DisplayName("passes /oauth2/authorization/** on to authentication instead of redirecting to IDAM")
    void doesNotStartAnAuthorizationCodeFlow() throws Exception {
        Filter redirectFilter = securityFilterChains.stream()
            .flatMap(chain -> chain.getFilters().stream())
            .filter(OAuth2AuthorizationRequestRedirectFilter.class::isInstance)
            .findFirst()
            .orElseThrow(() -> new AssertionError(".oauth2Client() should install the redirect filter"));

        // TestIdamConfiguration registers "oidc" as an authorization_code client, so the default resolver would
        // redirect this request; production's client_credentials registration would fail it with a 500.
        for (String registrationId : List.of("oidc", "unknown")) {
            MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/oauth2/authorization/" + registrationId);
            request.setServletPath("/oauth2/authorization/" + registrationId);
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            redirectFilter.doFilter(request, response, chain);

            assertThat(chain.getRequest()).as("%s: passed on down the chain", registrationId).isSameAs(request);
            assertThat(response.getStatus()).as("%s: no response written", registrationId).isEqualTo(200);
            assertThat(response.getRedirectedUrl()).as("%s: no redirect", registrationId).isNull();
            assertThat(request.getSession(false)).as("%s: no session created", registrationId).isNull();
        }
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
