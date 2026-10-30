package uk.gov.hmcts.ccd.endpoint.exceptions;

import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import feign.FeignException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import uk.gov.hmcts.ccd.error.ErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.gov.hmcts.ccd.appinsights.AppInsights;
import uk.gov.hmcts.ccd.domain.model.common.HttpError;
import uk.gov.hmcts.ccd.domain.model.std.validator.ValidationError;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(RestExceptionHandler.class);

    private final AppInsights appInsights;
    private final boolean explicitExceptionTelemetry;

    @Autowired
    public RestExceptionHandler(AppInsights appInsights,
                                @Value("${ccd.errors.explicit-telemetry-enabled:false}")
                                boolean explicitExceptionTelemetry) {
        this.appInsights = appInsights;
        this.explicitExceptionTelemetry = explicitExceptionTelemetry;
    }

    @ExceptionHandler(ApiException.class)
    @ResponseBody
    public ResponseEntity<HttpError> handleApiException(final HttpServletRequest request,
                                                        final ApiException exception) {
        final HttpError<Serializable> error = new HttpError<>(exception, request)
            .withDetails(exception.getDetails())
            .withCallbackErrors(exception.getCallbackErrors())
            .withCallbackWarnings(exception.getCallbackWarnings());
        logFailure(exception, error.getStatus(), false);
        return ResponseEntity
            .status(error.getStatus())
            .body(error);
    }

    @ExceptionHandler(BadSearchRequest.class)
    @ResponseBody
    public ResponseEntity<HttpError> handleSearchRequestException(HttpServletRequest request, Exception exception) {
        final HttpError<Serializable> error = new HttpError<>(exception, request);
        logFailure(exception, error.getStatus(), true);
        return ResponseEntity
            .status(error.getStatus())
            .body(error);
    }

    @ExceptionHandler(CaseValidationException.class)
    @ResponseBody
    public ResponseEntity<HttpError> handleCaseValidationException(HttpServletRequest request,
                                                                   CaseValidationException exception) {

        // NB: only recording field IDs as some validation messages contain user data
        Map<String, String> customProperties = new HashMap<>();
        customProperties.put("CaseValidationError field IDs", StringUtils.join(exception.getFields(), ", "));

        final HttpError<Serializable> error = new HttpError<>(exception, request)
            .withDetails(exception.getDetails());
        logFailure(exception, error.getStatus(), true, customProperties);
        return ResponseEntity
            .status(error.getStatus())
            .body(error);
    }

    @ExceptionHandler(SQLException.class)
    @ResponseBody
    public ResponseEntity<Map> handleSQLException(final SQLException exception) {
        final String errorMsg = HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase();
        logFailure(exception, HttpStatus.INTERNAL_SERVER_ERROR.value(), false);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("errorMessage", errorMsg, "code", ErrorCode.INTERNAL_ERROR));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseBody
    public ResponseEntity<HttpError> handleConstraintViolationException(final HttpServletRequest request,
                                                                        final Exception exception) {

        final HttpError<Serializable> error = new HttpError<>(exception, request, HttpStatus.BAD_REQUEST);
        logFailure(exception, HttpStatus.BAD_REQUEST.value(), false);
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(error);
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<HttpError> handleException(final HttpServletRequest request, final Exception exception) {

        Throwable targetException;

        if (exception instanceof HttpStatusCodeException || exception instanceof FeignException) {
            targetException = exception;
        } else {
            targetException = exception.getCause();
        }

        HttpStatus httpStatus = (targetException != null) ? getHttpStatus(targetException) : null;

        final HttpError<Serializable> error = new HttpError<>(exception, request, httpStatus);
        logFailure(exception, error.getStatus(), false);

        return ResponseEntity
            .status(error.getStatus())
            .body(error);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        String[] errors = exception.getBindingResult().getFieldErrors().stream()
            .map(DefaultMessageSourceResolvable::getDefaultMessage)
            .toArray(String[]::new);
        LOG.debug("MethodArgumentNotValidException:{}", exception.getLocalizedMessage());
        final HttpError<Serializable> error = new HttpError<>(exception, request, HttpStatus.BAD_REQUEST)
            .withMessage(ValidationError.ARGUMENT_INVALID)
            .withDetails(errors);
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(error);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body,
                                                              HttpHeaders headers, HttpStatusCode status,
                                                              WebRequest request) {
        logFailure(exception, status.value(), false);
        return super.handleExceptionInternal(exception,
            new HttpError<>(exception, request, HttpStatus.valueOf(status.value())), headers, status, request);
    }

    private void logFailure(Exception exception, int status, boolean warning) {
        logFailure(exception, status, warning, null);
    }

    private void logFailure(Exception exception, int status, boolean warning, Map<String, String> customProperties) {
        if (explicitExceptionTelemetry) {
            if (customProperties == null) {
                appInsights.trackException(exception);
            } else {
                appInsights.trackException(exception, customProperties,
                    warning ? SeverityLevel.Warning : SeverityLevel.Error);
            }
        }
        if (warning) {
            LOG.warn("Request failed: code={} status={}", ErrorCode.fromStatus(status), status, exception);
        } else {
            LOG.error("Request failed: code={} status={}", ErrorCode.fromStatus(status), status, exception);
        }
    }

    private HttpStatus getHttpStatus(final Throwable causeOfException) {
        HttpStatus httpStatus = checkAndRetrieveExceptionStatusCode(causeOfException);
        if (httpStatus != null) {
            return assignExceptionHttpCode(httpStatus);
        }

        if (isReadTimeoutException(causeOfException)) {
            return HttpStatus.GATEWAY_TIMEOUT;
        }

        if (isUnknownHostException(causeOfException)) {
            return HttpStatus.BAD_GATEWAY;
        }

        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private HttpStatus checkAndRetrieveExceptionStatusCode(final Throwable causeOfException) {
        HttpStatus httpStatus = null;
        if (causeOfException instanceof HttpStatusCodeException) {
            httpStatus = HttpStatus.valueOf(((HttpStatusCodeException) causeOfException).getStatusCode().value());
        } else if (causeOfException instanceof FeignException.FeignClientException
            || causeOfException instanceof FeignException.FeignServerException) {
            httpStatus = HttpStatus.valueOf(((FeignException) causeOfException).status());
        }

        return httpStatus;
    }

    // return BAD_GATEWAY for INTERNAL_SERVER_ERROR status code, other ServerErrors will be kept as is
    // return UNAUTHORIZED for UNAUTHORIZED status code, other ClientErrors will return 500
    private HttpStatus assignExceptionHttpCode(final HttpStatus httpStatus) {
        if (httpStatus.is5xxServerError()) {
            if (httpStatus == HttpStatus.INTERNAL_SERVER_ERROR) {
                return HttpStatus.BAD_GATEWAY;
            }

            return httpStatus;
        }

        if (httpStatus == HttpStatus.UNAUTHORIZED) {
            return HttpStatus.UNAUTHORIZED;
        }

        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private boolean isReadTimeoutException(final Throwable causeOfException) {
        Throwable innerException = causeOfException.getCause();
        return innerException instanceof java.net.SocketTimeoutException
            && innerException.getMessage().contains("Read timed out");
    }

    private boolean isUnknownHostException(final Throwable causeOfException) {
        return causeOfException instanceof java.net.UnknownHostException;
    }

}
