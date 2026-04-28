package uk.gov.hmcts.ccd.security;

import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import uk.gov.hmcts.ccd.appinsights.AppInsights;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class AppInsightsJwtDecoder implements JwtDecoder {

    static final String JWT_VALIDATION_FAILURE_MESSAGE = "JWT validation failed";
    static final String FAILURE_TYPE = "JWT validation failure type";
    static final String FAILURE_MESSAGE = "JWT validation failure message";
    static final String VALIDATION_ERRORS = "JWT validation errors";

    private static final String NO_FAILURE_MESSAGE = "No failure message provided";

    private final JwtDecoder jwtDecoder;
    private final AppInsights appInsights;

    public AppInsightsJwtDecoder(JwtDecoder jwtDecoder, AppInsights appInsights) {
        this.jwtDecoder = jwtDecoder;
        this.appInsights = appInsights;
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        try {
            return jwtDecoder.decode(token);
        } catch (JwtException exception) {
            logJwtValidationFailure(exception);
            throw exception;
        }
    }

    private void logJwtValidationFailure(JwtException exception) {
        String failureMessage = sanitise(exception.getMessage());

        log.warn("{}: {}", JWT_VALIDATION_FAILURE_MESSAGE, failureMessage, exception);
        appInsights.trackTrace(
            JWT_VALIDATION_FAILURE_MESSAGE + ": " + failureMessage,
            buildTelemetryProperties(exception, failureMessage),
            SeverityLevel.Warning
        );
    }

    private Map<String, String> buildTelemetryProperties(JwtException exception, String failureMessage) {
        Map<String, String> properties = new HashMap<>();
        properties.put(FAILURE_TYPE, exception.getClass().getSimpleName());
        properties.put(FAILURE_MESSAGE, failureMessage);

        if (exception instanceof JwtValidationException jwtValidationException) {
            properties.put(VALIDATION_ERRORS, validationErrors(jwtValidationException));
        }

        return properties;
    }

    private String validationErrors(JwtValidationException exception) {
        return exception.getErrors()
            .stream()
            .map(this::errorDescription)
            .collect(Collectors.joining("; "));
    }

    private String errorDescription(OAuth2Error error) {
        if (error.getDescription() != null && !error.getDescription().isBlank()) {
            return sanitise(error.getDescription());
        }

        return sanitise(error.getErrorCode());
    }

    private String sanitise(String message) {
        if (message == null || message.isBlank()) {
            return NO_FAILURE_MESSAGE;
        }

        return message.replaceAll("\\s+", " ");
    }
}
