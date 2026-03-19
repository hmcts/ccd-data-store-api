package uk.gov.hmcts.ccd;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Validator-level coverage for issuer and timestamp enforcement.
class SecurityConfigurationTest {

    private static final String VALID_ISSUER = "http://fr-am:8080/openam/oauth2/hmcts";
    private static final String INVALID_ISSUER = "http://unexpected-issuer";

    @Test
    void shouldAcceptJwtFromConfiguredIssuer() {
        Instant now = Instant.now();
        assertFalse(
            validator().validate(buildJwt(VALID_ISSUER, now.minusSeconds(60), now.plusSeconds(300))).hasErrors()
        );
    }

    @Test
    void shouldRejectJwtFromUnexpectedIssuer() {
        Instant now = Instant.now();
        assertTrue(
            validator().validate(buildJwt(INVALID_ISSUER, now.minusSeconds(60), now.plusSeconds(300))).hasErrors()
        );
    }

    @Test
    void shouldRejectExpiredJwtEvenWhenIssuerMatches() {
        Instant now = Instant.now();
        // Keep expiry clearly outside the default clock-skew allowance to avoid boundary flakiness.
        assertTrue(
            validator().validate(buildJwt(VALID_ISSUER, now.minusSeconds(300), now.minusSeconds(121))).hasErrors()
        );
    }

    private OAuth2TokenValidator<Jwt> validator() {
        return new DelegatingOAuth2TokenValidator<>(
            new JwtTimestampValidator(),
            new JwtIssuerValidator(VALID_ISSUER)
        );
    }

    private Jwt buildJwt(String issuer, Instant issuedAt, Instant expiresAt) {
        return Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .issuer(issuer)
            .subject("user")
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .build();
    }
}
