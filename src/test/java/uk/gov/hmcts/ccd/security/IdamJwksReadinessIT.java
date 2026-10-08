package uk.gov.hmcts.ccd.security;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.HealthEndpointGroups;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.WireMockBaseTest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.jwksUri;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.originalJwkSet;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.startWireMock;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.stubDown;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.stubHealthy;

/**
 * Starts the application with the JWK set endpoint down and checks the probes the chart uses: the instance is not
 * ready until the background retry retrieves the key set, and it is live throughout, so the start-up probe, which
 * checks liveness, does not restart it while it waits.
 *
 * <p>The JWK set endpoint is a WireMock of its own, so it can be down at start-up while the shared one serves
 * everything else.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = {
    // On AKS, Boot enables the liveness and readiness groups because it detects Kubernetes.
    "management.endpoint.health.probes.enabled=true",
    "oidc.jwks.connect-timeout-ms=250",
    "oidc.jwks.read-timeout-ms=250",
    "oidc.jwks.cache-ttl-ms=60000",
    "oidc.jwks.cache-refresh-timeout-ms=1100",
    "oidc.jwks.refresh-ahead-time-ms=5000",
    // Background retry every 500ms.
    "oidc.jwks.rate-limit-min-interval-ms=1000",
    "oidc.jwks.outage-tolerance-ms=300000"
})
class IdamJwksReadinessIT extends WireMockBaseTest {

    private static final WireMockServer JWKS = startWireMock();

    @Inject
    private WebApplicationContext wac;

    @Inject
    private HealthEndpointGroups healthEndpointGroups;

    @Inject
    private JwkSetStartupRetry jwkSetStartupRetry;

    @Inject
    private JWKSource<SecurityContext> idamJwkSource;

    @DynamicPropertySource
    static void jwksEndpoint(DynamicPropertyRegistry registry) {
        stubDown(JWKS);
        registry.add("oidc.jwks.uri", () -> jwksUri(JWKS));
    }

    @AfterAll
    static void stopJwks() {
        JWKS.stop();
    }

    @Test
    @DisplayName("is in the readiness group and not the liveness group")
    void readinessOnly() {
        assertThat(healthEndpointGroups.get("readiness").isMember("idamJwks")).isTrue();
        assertThat(healthEndpointGroups.get("liveness").isMember("idamJwks")).isFalse();
    }

    @Test
    @DisplayName("is not ready while the JWK set endpoint is down, ready once it is up and from then on, and live "
        + "throughout")
    void notReadyUntilTheKeySetIsRetrieved() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();

        mockMvc.perform(get("/health/readiness"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.components.idamJwks.status").value("DOWN"))
            .andExpect(jsonPath("$.components.db.status").value("UP"));
        mockMvc.perform(get("/health/liveness")).andExpect(status().isOk());
        mockMvc.perform(get("/health"))
            .andExpect(jsonPath("$.components.idamJwks.status").value("DOWN"));
        assertThat(jwkSetStartupRetry.isRunning()).isTrue();

        stubHealthy(JWKS, originalJwkSet());

        await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(100)).untilAsserted(() -> {
            mockMvc.perform(get("/health/liveness")).andExpect(status().isOk());
            mockMvc.perform(get("/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.idamJwks.status").value("UP"));
            // Inside the wait: each path's response is cached for management.endpoint.health.cache.time-to-live.
            mockMvc.perform(get("/health"))
                .andExpect(jsonPath("$.components.idamJwks.status").value("UP"));
        });
        await().atMost(Duration.ofSeconds(30)).until(() -> !jwkSetStartupRetry.isRunning());

        // IDAM goes down again. Lookups for an unknown key id make the source go back to it and fail; readiness is
        // checked for longer than its response is cached.
        stubDown(JWKS);
        JWKSelector unknownKid = new JWKSelector(new JWKMatcher.Builder().keyID("no-such-key").build());
        await().during(Duration.ofMillis(2_500)).atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(100))
            .until(() -> {
                try {
                    idamJwkSource.get(unknownKid, null);
                } catch (KeySourceException e) {
                    // expected while the endpoint is down
                }
                return mockMvc.perform(get("/health/readiness")).andReturn().getResponse().getStatus() == 200;
            });
        assertThat(JWKS.getAllServeEvents()).as("the source did go back to the dead endpoint").isNotEmpty();
    }
}
