package uk.gov.hmcts.ccd;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtIssuerValidatorTest {

    private static final String VALID_ISSUER = "http://localhost:5000/o";
    private static final String ADDITIONAL_ISSUER = "http://additional-issuer";
    private static final String INVALID_ISSUER = "http://unexpected-issuer";
    private static final Instant VALID_ISSUED_AT = Instant.parse("2024-01-01T00:00:00Z");
    private static final Instant VALID_EXPIRES_AT = Instant.parse("2999-01-01T00:00:00Z");
    private static final Instant EXPIRED_ISSUED_AT = Instant.parse("2000-01-01T00:00:00Z");
    private static final Instant EXPIRED_AT = Instant.parse("2000-01-01T00:01:00Z");

    @Test
    void shouldAcceptJwtFromConfiguredIssuer() {
        assertFalse(
            validator().validate(buildJwt(VALID_ISSUER, VALID_ISSUED_AT, VALID_EXPIRES_AT)).hasErrors()
        );
    }

    @Test
    void shouldAcceptJwtFromAdditionalAllowedIssuer() {
        assertFalse(
            validator(ADDITIONAL_ISSUER)
                .validate(buildJwt(ADDITIONAL_ISSUER, VALID_ISSUED_AT, VALID_EXPIRES_AT))
                .hasErrors()
        );
    }

    @Test
    void shouldKeepPrimaryIssuerWhenAdditionalAllowedIssuersConfigured() {
        assertFalse(
            validator(ADDITIONAL_ISSUER)
                .validate(buildJwt(VALID_ISSUER, VALID_ISSUED_AT, VALID_EXPIRES_AT))
                .hasErrors()
        );
    }

    @Test
    void shouldRejectJwtFromUnexpectedIssuer() {
        assertTrue(
            validator().validate(buildJwt(INVALID_ISSUER, VALID_ISSUED_AT, VALID_EXPIRES_AT)).hasErrors()
        );
    }

    @Test
    void shouldRejectJwtWhenIssuerIsMissing() {
        assertTrue(
            validator().validate(buildJwtWithoutIssuer(VALID_ISSUED_AT, VALID_EXPIRES_AT)).hasErrors()
        );
    }

    @Test
    void shouldRejectExpiredJwtEvenWhenIssuerMatches() {
        assertTrue(
            validator().validate(buildJwt(VALID_ISSUER, EXPIRED_ISSUED_AT, EXPIRED_AT)).hasErrors()
        );
    }

    private OAuth2TokenValidator<Jwt> validator() {
        return validator(null);
    }

    private OAuth2TokenValidator<Jwt> validator(String allowedIssuers) {
        return SecurityConfiguration.jwtValidator(VALID_ISSUER, allowedIssuers);
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

    private Jwt buildJwtWithoutIssuer(Instant issuedAt, Instant expiresAt) {
        return Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .subject("user")
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .build();
    }
}
