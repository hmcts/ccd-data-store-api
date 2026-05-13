package uk.gov.hmcts.ccd.security;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.gov.hmcts.ccd.appinsights.AppInsights;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.security.AppInsightsJwtDecoder.FAILURE_MESSAGE;
import static uk.gov.hmcts.ccd.security.AppInsightsJwtDecoder.FAILURE_TYPE;
import static uk.gov.hmcts.ccd.security.AppInsightsJwtDecoder.JWT_VALIDATION_FAILURE_MESSAGE;
import static uk.gov.hmcts.ccd.security.AppInsightsJwtDecoder.METHOD;
import static uk.gov.hmcts.ccd.security.AppInsightsJwtDecoder.PATH;
import static uk.gov.hmcts.ccd.security.AppInsightsJwtDecoder.VALIDATION_ERRORS;

@ExtendWith(MockitoExtension.class)
class AppInsightsJwtDecoderTest {

    private static final String TOKEN = "jwt-token";
    private static final String UNKNOWN = "UNKNOWN";

    @Mock
    private JwtDecoder jwtDecoder;

    @Mock
    private AppInsights appInsights;

    @Mock
    private Jwt jwt;

    private AppInsightsJwtDecoder appInsightsJwtDecoder;
    private Logger logger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        appInsightsJwtDecoder = new AppInsightsJwtDecoder(jwtDecoder, appInsights);

        logger = (Logger) LoggerFactory.getLogger(AppInsightsJwtDecoder.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        listAppender.stop();
        logger.detachAppender(listAppender);
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void decodeShouldReturnJwtWhenDelegateDecodesToken() {
        when(jwtDecoder.decode(TOKEN)).thenReturn(jwt);

        Jwt decodedJwt = appInsightsJwtDecoder.decode(TOKEN);

        assertThat(decodedJwt).isEqualTo(jwt);
        verifyNoInteractions(appInsights);
    }

    @Test
    void decodeShouldLogJwtFailureToAppInsightsAndRethrowException() {
        BadJwtException exception = new BadJwtException("Signed JWT rejected: Invalid signature");
        when(jwtDecoder.decode(TOKEN)).thenThrow(exception);
        setCurrentRequest("GET", "/cases/123");

        assertThatThrownBy(() -> appInsightsJwtDecoder.decode(TOKEN)).isSameAs(exception);

        Map<String, String> properties = captureAppInsightsProperties(JWT_VALIDATION_FAILURE_MESSAGE);

        assertThat(properties.get(FAILURE_TYPE)).isEqualTo("INVALID_SIGNATURE");
        assertThat(properties.get(FAILURE_MESSAGE)).isEqualTo("Signed JWT rejected: Invalid signature");
        assertThat(properties.get(METHOD)).isEqualTo("GET");
        assertThat(properties.get(PATH)).isEqualTo("/cases/123");
        assertThat(properties).doesNotContainKey(VALIDATION_ERRORS);

        assertThat(listAppender.list).hasSize(1);
        assertThat(listAppender.list.get(0).getLevel()).isEqualTo(Level.WARN);
        assertThat(listAppender.list.get(0).getFormattedMessage())
            .contains("JWT validation failed: INVALID_SIGNATURE");
    }

    @Test
    void decodeShouldIncludeValidationErrorDescriptionsInAppInsightsProperties() {
        OAuth2Error expiredToken = new OAuth2Error("invalid_token", "Jwt expired at 2026-04-28T10:00:00Z", null);
        OAuth2Error invalidClaim = new OAuth2Error("invalid_token", "The iss claim is not valid", null);
        JwtValidationException exception = new JwtValidationException(
            "Jwt validation failed",
            List.of(expiredToken, invalidClaim)
        );
        when(jwtDecoder.decode(TOKEN)).thenThrow(exception);
        setCurrentRequest("POST", "/caseworkers/abc/jurisdictions");

        assertThatThrownBy(() -> appInsightsJwtDecoder.decode(TOKEN)).isSameAs(exception);

        Map<String, String> properties = captureAppInsightsProperties(JWT_VALIDATION_FAILURE_MESSAGE);

        assertThat(properties.get(FAILURE_TYPE)).isEqualTo("TOKEN_EXPIRED");
        assertThat(properties.get(FAILURE_MESSAGE)).isEqualTo("Jwt validation failed");
        assertThat(properties.get(METHOD)).isEqualTo("POST");
        assertThat(properties.get(PATH)).isEqualTo("/caseworkers/abc/jurisdictions");
        assertThat(properties.get(VALIDATION_ERRORS))
            .isEqualTo("Jwt expired at 2026-04-28T10:00:00Z; The iss claim is not valid");
    }

    @ParameterizedTest
    @CsvSource({
        "The aud claim is not valid, INVALID_AUDIENCE",
        "The iss claim is not valid, INVALID_ISSUER",
        "Malformed JWT, OTHER"
    })
    void decodeShouldClassifyJwtFailures(String failureMessage, String expectedFailureType) {
        BadJwtException exception = new BadJwtException(failureMessage);
        when(jwtDecoder.decode(TOKEN)).thenThrow(exception);

        assertThatThrownBy(() -> appInsightsJwtDecoder.decode(TOKEN)).isSameAs(exception);

        Map<String, String> properties = captureAppInsightsProperties(JWT_VALIDATION_FAILURE_MESSAGE);

        assertThat(properties.get(FAILURE_TYPE)).isEqualTo(expectedFailureType);
        assertThat(properties.get(FAILURE_MESSAGE)).isEqualTo(failureMessage);
        assertThat(properties.get(METHOD)).isEqualTo(UNKNOWN);
        assertThat(properties.get(PATH)).isEqualTo(UNKNOWN);
    }

    @Test
    void decodeShouldClassifyJwtFailureWithNoMessageAsUnknown() {
        BadJwtException exception = new BadJwtException(null);
        when(jwtDecoder.decode(TOKEN)).thenThrow(exception);

        assertThatThrownBy(() -> appInsightsJwtDecoder.decode(TOKEN)).isSameAs(exception);

        Map<String, String> properties = captureAppInsightsProperties(JWT_VALIDATION_FAILURE_MESSAGE);

        assertThat(properties.get(FAILURE_TYPE)).isEqualTo(UNKNOWN);
        assertThat(properties.get(FAILURE_MESSAGE)).isEqualTo("No failure message provided");
    }

    @Test
    void decodeShouldUseValidationErrorCodeWhenDescriptionIsMissing() {
        OAuth2Error validationError = new OAuth2Error("invalid_token", null, null);
        JwtValidationException exception = new JwtValidationException(
            "Jwt validation failed",
            List.of(validationError)
        );
        when(jwtDecoder.decode(TOKEN)).thenThrow(exception);

        assertThatThrownBy(() -> appInsightsJwtDecoder.decode(TOKEN)).isSameAs(exception);

        Map<String, String> properties = captureAppInsightsProperties(JWT_VALIDATION_FAILURE_MESSAGE);

        assertThat(properties.get(FAILURE_TYPE)).isEqualTo("OTHER");
        assertThat(properties.get(VALIDATION_ERRORS)).isEqualTo("invalid_token");
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> captureAppInsightsProperties(String expectedMessage) {
        ArgumentCaptor<Map<String, String>> propertiesCaptor = ArgumentCaptor.forClass(Map.class);

        verify(appInsights).trackTrace(
            eq(expectedMessage),
            propertiesCaptor.capture(),
            eq(SeverityLevel.Warning)
        );

        return propertiesCaptor.getValue();
    }

    private void setCurrentRequest(String method, String requestUri) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, requestUri);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
