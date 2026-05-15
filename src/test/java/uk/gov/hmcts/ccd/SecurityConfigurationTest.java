package uk.gov.hmcts.ccd;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import uk.gov.hmcts.ccd.security.OidcIssuerConfiguration;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
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

    @Test
    void shouldAcceptJwtFromConfiguredIssuer() {
        Instant now = Instant.now();
        assertFalse(
            validator().validate(buildJwt(VALID_ISSUER, now.minusSeconds(60), now.plusSeconds(300))).hasErrors()
        );
    }

    @Test
    void shouldAcceptJwtFromAdditionalAllowedIssuer() {
        Instant now = Instant.now();
        assertFalse(
            validator(ADDITIONAL_ISSUER)
                .validate(buildJwt(ADDITIONAL_ISSUER, now.minusSeconds(60), now.plusSeconds(300)))
                .hasErrors()
        );
    }

    @Test
    void shouldKeepPrimaryIssuerWhenAdditionalAllowedIssuersConfigured() {
        Instant now = Instant.now();
        assertFalse(
            validator(ADDITIONAL_ISSUER)
                .validate(buildJwt(VALID_ISSUER, now.minusSeconds(60), now.plusSeconds(300)))
                .hasErrors()
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
    void shouldRejectDecodedJwtFromUnexpectedIssClaim() throws JOSEException, ParseException {
        JwtValidationException exception = assertThrows(
            JwtValidationException.class,
            () -> decoder().decode(signedJwt(INVALID_ISSUER))
        );

        assertThat(exception.getMessage()).contains("iss");
    }

    @Test
    void shouldAcceptDecodedJwtFromAdditionalAllowedIssClaim() throws JOSEException, ParseException {
        assertThat(decoder(ADDITIONAL_ISSUER).decode(signedJwt(ADDITIONAL_ISSUER)).getIssuer().toString())
            .isEqualTo(ADDITIONAL_ISSUER);
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
        return validator(null);
    }

    private OAuth2TokenValidator<Jwt> validator(String allowedIssuers) {
        Set<String> configuredIssuers = OidcIssuerConfiguration.allowedIssuers(VALID_ISSUER, allowedIssuers);
        return new DelegatingOAuth2TokenValidator<>(
            new JwtTimestampValidator(),
            new JwtClaimValidator<>(
                "iss",
                issuer -> issuer != null && configuredIssuers.contains(issuer.toString())
            )
        );
    }

    private NimbusJwtDecoder decoder() throws JOSEException {
        return decoder(null);
    }

    private NimbusJwtDecoder decoder(String allowedIssuers) throws JOSEException {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(getRsaJWK().toRSAPublicKey()).build();
        decoder.setJwtValidator(validator(allowedIssuers));
        return decoder;
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
                .subject("user")
                .issueTime(Date.from(now.minusSeconds(60)))
                .expirationTime(Date.from(now.plusSeconds(300)))
                .build()
        );
        signedJwt.sign(new RSASSASigner(getRsaJWK().toPrivateKey()));
        return signedJwt.serialize();
    }
}
