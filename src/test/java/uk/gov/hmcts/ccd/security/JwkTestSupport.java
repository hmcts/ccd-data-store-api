package uk.gov.hmcts.ccd.security;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import uk.gov.hmcts.ccd.appinsights.AppInsights;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

/**
 * Shared JWK source test fixtures: signing keys, tokens, a stubbed JWK endpoint, and a recording
 * {@link AppInsights}.
 *
 */
final class JwkTestSupport {

    static final String JWKS_PATH = "/o/jwks";
    static final String ORIGINAL_KEY_ID = "original-signing-key";
    static final String ROTATED_KEY_ID = "rotated-signing-key";
    static final String SUBJECT = "user@example.com";

    private static final RSAKey ORIGINAL_KEY = generateKey(ORIGINAL_KEY_ID);
    private static final RSAKey ROTATED_KEY = generateKey(ROTATED_KEY_ID);

    private JwkTestSupport() {
    }

    static RSAKey originalKey() {
        return ORIGINAL_KEY;
    }

    static RSAKey rotatedKey() {
        return ROTATED_KEY;
    }

    static String originalJwkSet() {
        return new JWKSet(ORIGINAL_KEY.toPublicJWK()).toString();
    }

    static String rotatedJwkSet() {
        return new JWKSet(ROTATED_KEY.toPublicJWK()).toString();
    }

    static String tokenSignedWith(RSAKey key) {
        try {
            Instant now = Instant.now();
            SignedJWT signedJwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(),
                new JWTClaimsSet.Builder()
                    .subject(SUBJECT)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plus(1, ChronoUnit.HOURS)))
                    .build());
            signedJwt.sign(new RSASSASigner(key));
            return signedJwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to sign test token", e);
        }
    }

    static WireMockServer startWireMock() {
        WireMockServer wireMock = new WireMockServer(WireMockConfiguration.options()
            .dynamicPort()
            .usingFilesUnderDirectory(emptyRoot()));
        wireMock.start();
        return wireMock;
    }

    private static String emptyRoot() {
        try {
            Path root = Files.createTempDirectory("jwk-wiremock-root");
            Files.createDirectories(root.resolve("mappings"));
            Files.createDirectories(root.resolve("__files"));
            root.toFile().deleteOnExit();
            return root.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create an empty WireMock root", e);
        }
    }

    static String jwksUri(WireMockServer wireMock) {
        return "http://localhost:" + wireMock.port() + JWKS_PATH;
    }

    static void stubHealthy(WireMockServer wireMock, String jwkSet) {
        wireMock.resetAll();
        wireMock.stubFor(get(urlEqualTo(JWKS_PATH)).willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(jwkSet)));
    }

    static void stubDown(WireMockServer wireMock) {
        wireMock.resetAll();
        wireMock.stubFor(get(urlEqualTo(JWKS_PATH)).willReturn(aResponse()
            .withStatus(503)
            .withHeader("Content-Type", "application/json")
            .withBody("{\"error\":\"unavailable\"}")));
    }

    static void stubSlow(WireMockServer wireMock, String jwkSet, int delayMs) {
        wireMock.resetAll();
        wireMock.stubFor(get(urlEqualTo(JWKS_PATH)).willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(jwkSet)
            .withFixedDelay(delayMs)));
    }

    static int retrievalAttempts(WireMockServer wireMock) {
        return wireMock.getAllServeEvents().size();
    }

    private static RSAKey generateKey(String keyId) {
        try {
            return new RSAKeyGenerator(2048)
                .keyID(keyId)
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .generate();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to generate test signing key " + keyId, e);
        }
    }

    static final class RecordingAppInsights extends AppInsights {

        private final List<TrackedEvent> events = new CopyOnWriteArrayList<>();

        RecordingAppInsights() {
            super(null);
        }

        @Override
        public void trackEvent(String name, Map<String, String> properties, Map<String, Double> metrics) {
            events.add(new TrackedEvent(name, Map.copyOf(properties), Map.copyOf(metrics)));
        }

        @Override
        public void trackEvent(String name, Map<String, String> properties) {
            events.add(new TrackedEvent(name, Map.copyOf(properties), Map.of()));
        }

        @Override
        public void trackTrace(String message, Map<String, String> customProperties, SeverityLevel severityLevel) {
            // Intentionally a no-op: these tests only assert on trackEvent calls, so traces are discarded
            // rather than accumulated in an unused collection.
        }

        List<TrackedEvent> events() {
            return List.copyOf(events);
        }

        List<TrackedEvent> eventsOfType(String jwksEventType) {
            return events().stream()
                .filter(event -> JwkSourceTelemetry.EVENT_NAME.equals(event.name()))
                .filter(event -> jwksEventType.equals(event.properties().get(JwkSourceTelemetry.EVENT_TYPE)))
                .toList();
        }

        void clear() {
            events.clear();
        }
    }

    record TrackedEvent(String name, Map<String, String> properties, Map<String, Double> metrics) {
    }
}
