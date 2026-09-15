package uk.gov.hmcts.ccd.security;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class MultiIssuerValidatorTest {

    private static final String ISSUER_VALID = "http://validIssuer";
    private static final String ISSUER_VALID_OTHER = "http://otherValidIssuer";
    private static final String ISSUER_EMPTY = "";
    private static final String ISSUER_INVALID = "http://invalidIssuer";

    private static final String AUTH_ERROR_INVALID_TOKEN = "invalid_token";

    private static final String ZONE_EUROPE_LONDON = "Europe/London";
    private static final String TOKEN_VALUE = "testTokenValue";

    @Test
    void initialisationShouldFailWithNullList() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                                                          () -> new MultiIssuerValidator(null),
                                                          "Exception should be thrown if list is null");
        assertIllegalArgumentException(exception, "Valid issuers list should not be null or empty");
    }

    @Test
    void initialisationShouldFailWithEmptyList() {
        List<String> emptyList = Collections.emptyList();
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                                                          () -> new MultiIssuerValidator(emptyList),
                                                          "Exception should be thrown is list is empty");
        assertIllegalArgumentException(exception, "Valid issuers list should not be null or empty");
    }

    @ParameterizedTest
    @MethodSource("listsWithNullIssuer")
    void initialisationShouldFailWithNullIssuer(List<String> issuers) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                                                          () -> new MultiIssuerValidator(issuers),
                                                          "Exception should be thrown if list contains a null issuer");
        assertIllegalArgumentException(exception, "Valid issuers list should not contain any null elements");
    }

    @Test
    void initialisationShouldSucceed() {
        List<String> listWithIssuer = List.of(ISSUER_VALID);
        assertDoesNotThrow(() -> new MultiIssuerValidator(listWithIssuer),
                           "Exception should not be thrown for non-null, non-empty list of non-null issuers");
    }

    @Test
    void validateShouldFailForNoIssuer() {
        MultiIssuerValidator validator = new MultiIssuerValidator(List.of(ISSUER_VALID));

        Jwt jwtWithoutIssuer = createJwtWithoutIssuer();
        OAuth2TokenValidatorResult result = validator.validate(jwtWithoutIssuer);

        assertValidateFailure(result);
    }

    @Test
    void validateShouldFailForEmptyIssuer() {
        MultiIssuerValidator validator = new MultiIssuerValidator(List.of(ISSUER_VALID));

        Jwt jwtWithEmptyIssuer = createJwt(ISSUER_EMPTY);
        OAuth2TokenValidatorResult result = validator.validate(jwtWithEmptyIssuer);

        assertValidateFailure(result);
    }

    @Test
    void validateShouldFailForInvalidIssuer() {
        MultiIssuerValidator validator = new MultiIssuerValidator(List.of(ISSUER_VALID));

        Jwt jwtIssuerInvalid = createJwt(ISSUER_INVALID);
        OAuth2TokenValidatorResult result = validator.validate(jwtIssuerInvalid);

        assertValidateFailure(result);
    }

    @ParameterizedTest
    @ValueSource(strings = {ISSUER_VALID_OTHER, ISSUER_VALID})
    void validateShouldSucceed(String issuer) {
        MultiIssuerValidator validator = new MultiIssuerValidator(List.of(ISSUER_VALID, ISSUER_VALID_OTHER));

        Jwt jwtValid = createJwt(issuer);
        OAuth2TokenValidatorResult result = validator.validate(jwtValid);

        assertValidateSuccess(result);
    }

    private static Stream<Arguments> listsWithNullIssuer() {
        List<String> onlyNullIssuer = new ArrayList<>();
        onlyNullIssuer.add(null);

        List<String> nonNullAndNullIssuer = new ArrayList<>();
        nonNullAndNullIssuer.add(ISSUER_VALID);
        nonNullAndNullIssuer.add(null);

        return Stream.of(
            arguments(onlyNullIssuer),
            arguments(nonNullAndNullIssuer)
        );
    }

    private void assertIllegalArgumentException(IllegalArgumentException exception, String expectedMessage) {
        assertEquals(expectedMessage, exception.getMessage(), "Exception has unexpected message");
    }

    private void assertValidateFailure(OAuth2TokenValidatorResult result) {
        assertTrue(result.hasErrors(), "Validator result should contain errors");

        Collection<OAuth2Error> errors = result.getErrors();
        assertEquals(1, errors.size(), "Validator result has unexpected number of errors");

        Optional<OAuth2Error> errorOptional = errors.stream().findFirst();
        assertTrue(errorOptional.isPresent(), "Validator result error should be present");

        OAuth2Error error = errorOptional.get();
        assertEquals(AUTH_ERROR_INVALID_TOKEN,
                     error.getErrorCode(),
                     "Validator result error has unexpected error code");
        assertEquals("The issuer is missing or invalid",
                     error.getDescription(),
                     "Validator result error has unexpected description");
    }

    private void assertValidateSuccess(OAuth2TokenValidatorResult result) {
        assertFalse(result.hasErrors(), "Validator result should not contain errors");
    }

    private Jwt createJwtWithoutIssuer() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("token_type", "Bearer");

        return createJwt(claims);
    }

    private Jwt createJwt(String issuer) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("iss", issuer);
        claims.put("token_type", "Bearer");

        return createJwt(claims);
    }

    private Jwt createJwt(Map<String, Object> claims) {
        LocalDateTime currentDateTime = LocalDateTime.now();
        Instant issuedAt = currentDateTime.atZone(ZoneId.of(ZONE_EUROPE_LONDON)).toInstant();
        Instant expiresAt = currentDateTime.plusHours(8L).atZone(ZoneId.of(ZONE_EUROPE_LONDON)).toInstant();

        Map<String, Object> headers = new HashMap<>();
        headers.put("typ", "JWT");

        return new Jwt(TOKEN_VALUE, issuedAt, expiresAt, headers, claims);
    }
}