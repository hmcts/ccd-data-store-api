package uk.gov.hmcts.ccd.security;

import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.gov.hmcts.ccd.appinsights.AppInsights;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class AppInsightsJwtDecoder implements JwtDecoder {

    static final String JWT_VALIDATION_FAILURE_MESSAGE = "JWT validation failed";
    static final String FAILURE_TYPE = "failureType";
    static final String FAILURE_MESSAGE = "failureMessage";
    static final String METHOD = "method";
    static final String PATH = "path";
    static final String VALIDATION_ERRORS = "JWT validation errors";

    private static final String NO_FAILURE_MESSAGE = "No failure message provided";
    private static final String UNKNOWN_REQUEST_VALUE = "UNKNOWN";

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
        String failureType = classifyJwtFailure(exception);
        String failureMessage = sanitise(exception.getMessage());

        log.warn("{}: {}", JWT_VALIDATION_FAILURE_MESSAGE, failureType);
        appInsights.trackTrace(
            JWT_VALIDATION_FAILURE_MESSAGE,
            buildTelemetryProperties(exception, failureType, failureMessage),
            SeverityLevel.Warning
        );
    }

    private Map<String, String> buildTelemetryProperties(
        JwtException exception,
        String failureType,
        String failureMessage
    ) {
        Map<String, String> properties = new HashMap<>();
        properties.put(FAILURE_TYPE, failureType);
        properties.put(FAILURE_MESSAGE, failureMessage);
        properties.put(METHOD, currentRequestMethod());
        properties.put(PATH, currentRequestPath());

        if (exception instanceof JwtValidationException jwtValidationException) {
            properties.put(VALIDATION_ERRORS, validationErrors(jwtValidationException));
        }

        return properties;
    }

    private String currentRequestMethod() {
        HttpServletRequest request = currentRequest();
        return requestValue(request == null ? null : request.getMethod());
    }

    private String currentRequestPath() {
        HttpServletRequest request = currentRequest();
        return requestValue(request == null ? null : request.getRequestURI());
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes requestAttributes) {
            return requestAttributes.getRequest();
        }

        return null;
    }

    private String requestValue(String value) {
        return value == null || value.isBlank() ? UNKNOWN_REQUEST_VALUE : value;
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

    private String classifyJwtFailure(Exception e) {
        String msg = jwtFailureDetails(e).toLowerCase(Locale.ROOT);

        if (msg.isBlank()) {
            return "UNKNOWN";
        }

        if (msg.contains("expired")) {
            return "TOKEN_EXPIRED";
        }
        if (msg.contains("signature")) {
            return "INVALID_SIGNATURE";
        }
        if (msg.contains("audience") || msg.contains("aud claim") || msg.contains("\"aud\"")) {
            return "INVALID_AUDIENCE";
        }
        if (msg.contains("issuer") || msg.contains("iss claim") || msg.contains("\"iss\"")) {
            return "INVALID_ISSUER";
        }

        return "OTHER";
    }

    private String jwtFailureDetails(Exception exception) {
        StringBuilder details = new StringBuilder();
        appendIfPresent(details, exception.getMessage());

        if (exception instanceof JwtValidationException jwtValidationException) {
            jwtValidationException.getErrors().forEach(error -> {
                appendIfPresent(details, error.getDescription());
                appendIfPresent(details, error.getErrorCode());
            });
        }

        return details.toString();
    }

    private void appendIfPresent(StringBuilder details, String value) {
        if (value != null && !value.isBlank()) {
            if (details.length() > 0) {
                details.append(' ');
            }
            details.append(value);
        }
    }
}
