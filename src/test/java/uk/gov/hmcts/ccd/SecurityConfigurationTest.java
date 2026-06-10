package uk.gov.hmcts.ccd;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.ccd.util.KeyGenerator.getRsaJWK;

// Validator-level coverage for issuer and timestamp enforcement.
class SecurityConfigurationTest {

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
    void shouldRejectDecodedJwtFromUnexpectedIssuer() {
        NimbusJwtDecoder jwtDecoder = decoder();
        String jwt = signedJwt(INVALID_ISSUER);

        JwtValidationException exception = assertThrows(
            JwtValidationException.class,
            () -> jwtDecoder.decode(jwt)
        );

        assertThat(exception.getMessage()).contains("iss");
    }

    @Test
    void shouldAcceptDecodedJwtFromAdditionalAllowedIssuer() {
        assertThat(decoder(ADDITIONAL_ISSUER).decode(signedJwt(ADDITIONAL_ISSUER)).getIssuer())
            .hasToString(ADDITIONAL_ISSUER);
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

    private NimbusJwtDecoder decoder() {
        return decoder(null);
    }

    private NimbusJwtDecoder decoder(String allowedIssuers) {
        try {
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(getRsaJWK().toRSAPublicKey()).build();
            decoder.setJwtValidator(validator(allowedIssuers));
            return decoder;
        } catch (JOSEException exception) {
            throw new IllegalStateException("Failed to build test JWT decoder", exception);
        }
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
                    .subject("user")
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
