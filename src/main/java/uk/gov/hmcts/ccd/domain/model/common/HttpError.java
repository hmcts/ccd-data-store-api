package uk.gov.hmcts.ccd.domain.model.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.hmcts.ccd.endpoint.exceptions.ApiException;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

public class HttpError<T extends Serializable> implements Serializable {
    public static final Integer DEFAULT_STATUS = HttpStatus.INTERNAL_SERVER_ERROR.value();
    public static final String DEFAULT_ERROR = "Unexpected Error";

    private final String exception;
    private final transient LocalDateTime timestamp;
    private final Integer status;
    private final String error;
    private final String message;
    private final String path;
    private T details;
    private List<String> callbackErrors;
    private List<String> callbackWarnings;

    private final transient CatalogueResponse catalogueResponse;


    public HttpError(final Exception exception, final HttpServletRequest request) {

        this(exception, request, null, null);
    }

    public HttpError(final ApiException exception, final HttpServletRequest request) {

        this(exception, request, exception.getCatalogueResponse(),null);
    }

    public HttpError(
        final Exception exception,
        final HttpServletRequest request,
        final CatalogueResponse catalogueResponse,
        final Integer status) {

        final ResponseStatus responseStatus = exception.getClass().getAnnotation(ResponseStatus.class);
        this.exception = exception.getClass().getName();
        this.timestamp = LocalDateTime.now(ZoneOffset.UTC);
        this.status = getStatusValue(status, responseStatus);
        this.error = getErrorReason(status, responseStatus);
        this.message = exception.getMessage();
        this.path = request.getRequestURI();
        this.catalogueResponse = catalogueResponse;
    }

    private Integer getStatusValue(final Integer status, final ResponseStatus responseStatus) {
        // Option 1: use status as it may be an override value for responseStatus
        if (status != null) {
            return status;

        // Option 2: use responseStatus
        } else if (null != responseStatus) {
            final HttpStatus httpStatus = getHttpStatus(responseStatus);
            if (null != httpStatus) {
                return httpStatus.value();
            }
        }

        // Option 3: fallback to use default
        return DEFAULT_STATUS;
    }

    private HttpStatus getHttpStatus(final Integer status) {
        // NB: don't return value if matches default
        if (HttpStatus.INTERNAL_SERVER_ERROR.value() != status.intValue()) {
            return HttpStatus.resolve(status.intValue());
        }

        return null;
    }

    private HttpStatus getHttpStatus(final ResponseStatus responseStatus) {
        // NB: don't return value if matches default
        if (!HttpStatus.INTERNAL_SERVER_ERROR.equals(responseStatus.value())) {
            return responseStatus.value();
        } else if (!HttpStatus.INTERNAL_SERVER_ERROR.equals(responseStatus.code())) {
            return responseStatus.code();
        }

        return null;
    }

    private String getErrorReason(final Integer status, final ResponseStatus responseStatus) {
        // Option 1: use status as it may be an override value for responseStatus
        if (status != null) {
            final HttpStatus httpStatus = getHttpStatus(status);
            if (null != httpStatus) {
                return httpStatus.getReasonPhrase();
            }

        // Option 2: use responseStatus
        } else if (null != responseStatus) {
            if (!responseStatus.reason().isEmpty()) {
                return responseStatus.reason();
            }

            final HttpStatus httpStatus = getHttpStatus(responseStatus);
            if (null != httpStatus) {
                return httpStatus.getReasonPhrase();
            }
        }

        // Option 3: fallback to use default
        return DEFAULT_ERROR;
    }

    public String getException() {
        return exception;
    }

    public Integer getStatus() {
        return status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }

    public T getDetails() {
        return details;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public CatalogueResponse getCatalogueResponse() {
        return this.catalogueResponse;
    }

    public HttpError<T> withDetails(final T details) {
        this.details = details;
        return this;
    }

    public List<String> getCallbackErrors() {
        return callbackErrors;
    }

    public HttpError withCallbackErrors(final List<String> callbackErrors) {
        this.callbackErrors = callbackErrors;
        return this;
    }

    public List<String> getCallbackWarnings() {
        return callbackWarnings;
    }

    public HttpError withCallbackWarnings(final List<String> callbackWarnings) {
        this.callbackWarnings = callbackWarnings;
        return this;
    }
}
