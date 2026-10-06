package uk.gov.hmcts.ccd.integrations;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.security.AppInsightsJwtDecoder;

import java.security.Principal;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.ccd.util.KeyGenerator.getRsaJWK;

// Proves SecurityConfiguration wires the real OIDC-discovered JwtDecoder with issuer validation.
@TestPropertySource(properties = "idam.s2s-authorised.services=ccd_gw")
@Import(SecurityConfigurationJwtDecoderIssuerValidationIT.AuthenticationProbe.class)
class SecurityConfigurationJwtDecoderIssuerValidationIT extends WireMockBaseTest {

    private static final String INVALID_ISSUER = "http://unexpected-issuer";
    private static final String PROBE_URL = "/jwt-issuer-test";
    private static final Instant VALID_ISSUED_AT = Instant.parse("2024-01-01T00:00:00Z");
    private static final Instant VALID_EXPIRES_AT = Instant.parse("2999-01-01T00:00:00Z");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Value("${oidc.issuer}")
    private String expectedIssuer;

    @Test
    void shouldAcceptJwtWithExpectedIssuerThroughConfiguredDecoder() {
        ResponseEntity<String> response = authenticate(expectedIssuer);

        assertThat(response.getStatusCode().value())
            .withFailMessage("Expected authenticated response, got %s: %s",
                response.getStatusCode(), response.getBody())
            .isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("123");
        WireMock.verify(1, getRequestedFor(urlEqualTo("/s2s/details")));
    }

    @Test
    void shouldRejectJwtWithUnexpectedIssuerThroughConfiguredDecoder() {
        ResponseEntity<String> response = authenticate(INVALID_ISSUER);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE))
            .contains("invalid_token", "iss");
        WireMock.verify(1, getRequestedFor(urlEqualTo("/s2s/details")));
        WireMock.verify(0, getRequestedFor(urlEqualTo("/o/userinfo")));
    }

    private ResponseEntity<String> authenticate(String issuer) {
        assertThat(jwtDecoder).isInstanceOf(AppInsightsJwtDecoder.class);
        stubUserInfo("123");
        wireMockServer.resetRequests();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + signedJwt(issuer));
        headers.add("ServiceAuthorization", "ServiceToken");
        headers.add(HttpHeaders.CONTENT_TYPE, "application/json");

        return restTemplate.exchange(
            PROBE_URL,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class
        );
    }

    @RestController
    @TestComponent
    static class AuthenticationProbe {

        @GetMapping(PROBE_URL)
        String authenticatedSubject(Principal principal) {
            return principal.getName();
        }
    }

    private String signedJwt(String issuer) {
        try {
            SignedJWT signedJwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .type(JOSEObjectType.JWT)
                    .keyID(getRsaJWK().getKeyID())
                    .build(),
                new JWTClaimsSet.Builder()
                    .jwtID(UUID.randomUUID().toString())
                    .issuer(issuer)
                    .subject("123")
                    .claim("tokenName", "access_token")
                    .issueTime(Date.from(VALID_ISSUED_AT))
                    .expirationTime(Date.from(VALID_EXPIRES_AT))
                    .build()
            );
            signedJwt.sign(new RSASSASigner(getRsaJWK().toPrivateKey()));
            return signedJwt.serialize();
        } catch (JOSEException exception) {
            throw new IllegalStateException("Failed to sign test JWT", exception);
        }
    }
}
