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
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.ccd.WireMockBaseTest;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.ccd.util.KeyGenerator.getRsaJWK;

// Proves SecurityConfiguration wires the real OIDC-discovered JwtDecoder with issuer validation.
class SecurityConfigurationJwtDecoderIssuerValidationIT extends WireMockBaseTest {

    private static final String INVALID_ISSUER = "http://unexpected-issuer";
    private static final String CASE_URL =
        "/caseworkers/123/jurisdictions/TEST/case-types/TestAddressBook/cases/1234123412341238";
    private static final Instant VALID_ISSUED_AT = Instant.parse("2024-01-01T00:00:00Z");
    private static final Instant VALID_EXPIRES_AT = Instant.parse("2999-01-01T00:00:00Z");

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldRejectJwtWithUnexpectedIssuerThroughConfiguredDecoder() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + signedJwt(INVALID_ISSUER));
        headers.add("ServiceAuthorization", "ServiceToken");
        headers.add(HttpHeaders.CONTENT_TYPE, "application/json");

        ResponseEntity<String> response = restTemplate.exchange(
            CASE_URL,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class
        );

        // The full integration harness runs through CCD's S2S and V1 security filters, so the rejected JWT
        // currently surfaces as 403 here. Deployed invalid-issuer responses are expected to return 401 with
        // invalid_token details.
        assertThat(response.getStatusCode().value()).isEqualTo(403);
        WireMock.verify(1, getRequestedFor(urlEqualTo("/s2s/details")));
        WireMock.verify(0, getRequestedFor(urlEqualTo("/o/userinfo")));
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
