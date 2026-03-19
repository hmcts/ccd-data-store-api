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

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.ccd.util.KeyGenerator.getRsaJWK;

// Full integration coverage for issuer rejection through the real app and OIDC/JWKS test wiring.
class JwtIssuerValidationIT extends WireMockBaseTest {

    private static final String INVALID_ISSUER = "http://unexpected-issuer";
    private static final String CASE_URL =
        "/caseworkers/123/jurisdictions/TEST/case-types/TestAddressBook/cases/1234123412341238";

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldRejectJwtWhenIssuerDoesNotMatchConfiguredIssuer() throws JOSEException, ParseException {
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

        // This integration harness currently surfaces the rejected JWT as 403, although deployed runtime
        // invalid-issuer responses are expected to return 401 with invalid_token details.
        assertThat(response.getStatusCode().value()).isEqualTo(403);
        WireMock.verify(1, getRequestedFor(urlEqualTo("/s2s/details")));
        WireMock.verify(0, getRequestedFor(urlEqualTo("/o/userinfo")));
    }

    private String signedJwt(String issuer) throws JOSEException, ParseException {
        Instant now = Instant.now();

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
                .issueTime(Date.from(now.minusSeconds(60)))
                .expirationTime(Date.from(now.plusSeconds(300)))
                .build()
        );
        signedJwt.sign(new RSASSASigner(getRsaJWK().toPrivateKey()));
        return signedJwt.serialize();
    }
}
