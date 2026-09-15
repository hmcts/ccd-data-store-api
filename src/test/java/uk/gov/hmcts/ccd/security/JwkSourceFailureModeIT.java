package uk.gov.hmcts.ccd.security;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSetUnavailableException;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RateLimitReachedException;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import uk.gov.hmcts.ccd.security.filters.ExceptionHandlingFilter;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.RecordingAppInsights;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.jwksUri;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.originalJwkSet;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.originalKey;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.retrievalAttempts;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.rotatedKey;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.startWireMock;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.stubDown;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.stubHealthy;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.tokenSignedWith;

/**
 * Verifies the HTTP response when the JWK source cannot provide a key.
 *
 * <p>There are three relevant outcomes depending on the exception raised by Nimbus:
 * <ul>
 *     <li>{@code BadJOSEException} - no matching key was found, resulting in a <b>401</b>.</li>
 *     <li>{@code JWKSetUnavailableException} - the JWK set could not be retrieved, resulting in a <b>500</b>.</li>
 *     <li>{@code RateLimitReachedException} - the retrieval rate limited, resulting in a <b>500</b>.</li>
 * </ul>
 *
 * <p>The retrieval exceptions are rethrown by the authentication failure handler and caught by
 * {@link ExceptionHandlingFilter}, which is registered outside the security filter chain.
 * {@code SecurityConfigurationIT} verifies the filter ordering.
 */
class JwkSourceFailureModeIT {

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
        SecurityContextHolder.clearContext();
        if (jwkSource instanceof Closeable closeable) {
            closeable.close();
        }
        wireMock.stop();
    }

    @Test
    @DisplayName("returns 500 once the outage tolerance window has expired")
    void returnsServerErrorWhenToleranceWindowExpires() throws Exception {
        NimbusJwtDecoder decoder = decoder(propertiesWith(400L, 2_500L, 10L));

        stubHealthy(wireMock, originalJwkSet());
        assertThat(statusFor(decoder, originalToken)).isEqualTo(HttpStatus.OK.value());

        stubDown(wireMock);

        await().atMost(SETTLE_TIMEOUT).pollInterval(POLL).untilAsserted(() -> {
            assertThat(statusFor(decoder, originalToken)).isEqualTo(HttpStatus.OK.value());
            assertThat(appInsights.eventsOfType(JwkSourceTelemetry.OUTAGE_TOLERATED)).isNotEmpty();
        });

        await().atMost(SETTLE_TIMEOUT).pollInterval(POLL).untilAsserted(() ->
            assertThat(statusFor(decoder, originalToken))
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value()));

        // The mechanism behind that status, asserted directly so a change in either layer is caught.
        assertThatThrownBy(() -> decoder.decode(originalToken))
            .isInstanceOf(JwtException.class)
            .isNotInstanceOf(BadJwtException.class)
            .hasCauseInstanceOf(JWKSetUnavailableException.class)
            .hasMessageContaining("Couldn't retrieve JWK set from URL");

        assertThat(retrievalAttempts(wireMock))
            .as("expected the source to have kept trying the failing endpoint")
            .isPositive();
    }

    @Test
    @DisplayName("returns 401 for a key rotated in while IDAM is unreachable")
    void returnsUnauthorisedForAKeyRotatedInDuringAnOutage() throws Exception {
        NimbusJwtDecoder decoder = decoder(propertiesWith(400L, 60_000L, 10L));

        stubHealthy(wireMock, originalJwkSet());
        assertThat(statusFor(decoder, originalToken)).isEqualTo(HttpStatus.OK.value());

        stubDown(wireMock);
        await().atMost(SETTLE_TIMEOUT).pollInterval(POLL).untilAsserted(() -> {
            assertThat(statusFor(decoder, originalToken)).isEqualTo(HttpStatus.OK.value());
            assertThat(appInsights.eventsOfType(JwkSourceTelemetry.OUTAGE_TOLERATED)).isNotEmpty();
        });

        assertThat(statusFor(decoder, rotatedToken))
            .as("an unknown kid is an invalid token, not a server fault")
            .isEqualTo(HttpStatus.UNAUTHORIZED.value());

        assertThatThrownBy(() -> decoder.decode(rotatedToken))
            .isInstanceOf(BadJwtException.class)
            .hasCauseInstanceOf(BadJOSEException.class)
            .hasMessageContaining("Another algorithm expected, or no matching key(s) found");

        assertThat(statusFor(decoder, originalToken)).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    @DisplayName("returns 500 once the rate limiter refuses a retrieval, for a request that otherwise gets 401")
    void returnsServerErrorWhenTheRateLimiterIsExhausted() throws Exception {
        NimbusJwtDecoder decoder = decoder(propertiesWith(11_000L, 60_000L, 10_000L));

        stubHealthy(wireMock, originalJwkSet());
        assertThat(statusFor(decoder, originalToken)).isEqualTo(HttpStatus.OK.value());

        stubDown(wireMock);

        assertThat(statusFor(decoder, rotatedToken))
            .as("first unknown-kid request: a retrieval permit is still available")
            .isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(appInsights.eventsOfType(JwkSourceTelemetry.RATE_LIMIT_REACHED)).isEmpty();

        assertThat(statusFor(decoder, rotatedToken))
            .as("second unknown-kid request: the rate limiter refuses, and the status changes")
            .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());

        assertThat(appInsights.eventsOfType(JwkSourceTelemetry.RATE_LIMIT_REACHED))
            .as("the rate limiter must report the refusal, since the caller's 500 says nothing about the cause")
            .isNotEmpty();

        assertThatThrownBy(() -> decoder.decode(rotatedToken))
            .isInstanceOf(JwtException.class)
            .isNotInstanceOf(BadJwtException.class)
            .hasCauseInstanceOf(RateLimitReachedException.class);

        assertThat(statusFor(decoder, originalToken))
            .as("cached keys keep working even with the rate limiter exhausted")
            .isEqualTo(HttpStatus.OK.value());
        assertThat(telemetry.remainingToleranceMs())
            .as("still inside the outage window throughout")
            .isPositive();
    }

    /**
     * Runs a request through the same filters and ordering used by {@code SecurityConfiguration}
     * and returns the status seen by the caller.
     */
    private int statusFor(NimbusJwtDecoder decoder, String token) throws Exception {
        Filter bearerTokenFilter = new BearerTokenAuthenticationFilter(
            new ProviderManager(new JwtAuthenticationProvider(decoder)));
        Filter exceptionHandlingFilter = new ExceptionHandlingFilter();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/cases/1111111111111111");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        new MockFilterChain(okServlet(), exceptionHandlingFilter, bearerTokenFilter)
            .doFilter(request, response);

        SecurityContextHolder.clearContext();
        return response.getStatus();
    }

    private static HttpServlet okServlet() {
        return new HttpServlet() {
            @Override
            protected void service(HttpServletRequest request, HttpServletResponse response) {
                response.setStatus(HttpStatus.OK.value());
            }
        };
    }

    private NimbusJwtDecoder decoder(JwksProperties properties) {
        jwkSource = new JwkSourceConfiguration(properties).idamJwkSource(telemetry);
        return NimbusJwtDecoder.withJwkSetUri(properties.uri())
            .jwtProcessorCustomizer(processor -> processor.setJWSKeySelector(
                new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource)))
            .build();
    }

    private JwksProperties propertiesWith(long cacheTtlMs, long outageToleranceMs, long rateLimitMinIntervalMs) {
        return new JwksProperties(
            jwksUri(wireMock),
            50,       // connect timeout, ms
            100,                      // read timeout, ms
            51200,                    // size limit, bytes
            cacheTtlMs,
            200L, // cache refresh timeout: above connect + read
            100L,                     // refresh ahead time
            rateLimitMinIntervalMs,
            outageToleranceMs
        );
    }
}
