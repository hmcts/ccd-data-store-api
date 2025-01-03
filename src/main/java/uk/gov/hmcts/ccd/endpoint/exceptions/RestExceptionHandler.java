package uk.gov.hmcts.ccd.endpoint.exceptions;

import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import feign.FeignException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(RestExceptionHandler.class);

    private final AppInsights appInsights;

    @Autowired
    public RestExceptionHandler(AppInsights appInsights) {
        this.appInsights = appInsights;
    }

    @ExceptionHandler(ApiException.class)
    @ResponseBody
    public ResponseEntity<HttpError> handleApiException(final HttpServletRequest request,
                                                        final ApiException exception) {
        LOG.error(exception.getMessage(), exception);
        appInsights.trackException(exception);
        final HttpError<Serializable> error = new HttpError<>(exception, request)
            .withDetails(exception.getDetails())
            .withCallbackErrors(exception.getCallbackErrors())
            .withCallbackWarnings(exception.getCallbackWarnings());
        return ResponseEntity
            .status(error.getStatus())
            .body(error);
    }

    @ExceptionHandler(BadSearchRequest.class)
    @ResponseBody
    public ResponseEntity<HttpError> handleSearchRequestException(HttpServletRequest request, Exception exception) {
        LOG.warn(exception.getMessage(), exception);
        appInsights.trackException(exception);
        final HttpError<Serializable> error = new HttpError<>(exception, request);
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

        LOG.warn("{}: The following list of fields are in an invalid state: {}", exception.getMessage(),
            exception.getFields(), exception);
        appInsights.trackException(exception, customProperties, SeverityLevel.Warning);
        final HttpError<Serializable> error = new HttpError<>(exception, request)
            .withDetails(exception.getDetails());
        return ResponseEntity
            .status(error.getStatus())
            .body(error);
    }

    @ExceptionHandler(SQLException.class)
    @ResponseBody
    public ResponseEntity<Map> handleSQLException(final SQLException exception) {
        final String errorMsg = "SQL Exception thrown during API operation";
        appInsights.trackException(exception);

        LOG.error(errorMsg);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Collections.singletonMap("errorMessage", errorMsg));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseBody
    public ResponseEntity<HttpError> handleConstraintViolationException(final HttpServletRequest request,
                                                                        final Exception exception) {
        LOG.error(exception.getMessage());
        appInsights.trackException(exception);

        final HttpError<Serializable> error = new HttpError<>(exception, request, HttpStatus.BAD_REQUEST)
            .withDetails(exception.getCause());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(error);
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<HttpError> handleException(final HttpServletRequest request, final Exception exception) {
        LOG.error(exception.getMessage(), exception);
        appInsights.trackException(exception);

        Throwable targetException;

        if (exception instanceof HttpStatusCodeException || exception instanceof FeignException) {
            targetException = exception;
        } else {
            targetException = exception.getCause();
        }

        HttpStatus httpStatus = (targetException != null) ? getHttpStatus(targetException) : null;

        final HttpError<Serializable> error = new HttpError<>(exception, request, httpStatus);

        return ResponseEntity
            .status(error.getStatus())
            .body(error);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
                                                                  HttpHeaders headers,
                                                                  HttpStatus status,
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
            httpStatus = HttpStatus.valueOf(((HttpStatusCodeException) causeOfException).getRawStatusCode());
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
