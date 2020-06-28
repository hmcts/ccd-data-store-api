package uk.gov.hmcts.ccd.domain.model.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

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

    public HttpError(Exception exception, HttpServletRequest request) {
        final ResponseStatus responseStatus = exception.getClass().getAnnotation(ResponseStatus.class);

        this.exception = exception.getClass().getName();
        this.timestamp = LocalDateTime.now(ZoneOffset.UTC);
        this.status = getStatusFromResponseStatus(responseStatus);
        this.error = getErrorReason(responseStatus);
        this.message = exception.getMessage();
        this.path = request.getRequestURI();
    }

    private Integer getStatusFromResponseStatus(ResponseStatus responseStatus) {
        if (null != responseStatus) {
            final HttpStatus httpStatus = getHttpStatus(responseStatus);
            if (null != httpStatus) {
                return httpStatus.value();
            }
        }

        return DEFAULT_STATUS;
    }

    private HttpStatus getHttpStatus(ResponseStatus responseStatus) {
        if (!HttpStatus.INTERNAL_SERVER_ERROR.equals(responseStatus.value())) {
            return responseStatus.value();
        } else if (!HttpStatus.INTERNAL_SERVER_ERROR.equals(responseStatus.code())) {
            return responseStatus.code();
        }

        return null;
    }

    private String getErrorReason(ResponseStatus responseStatus) {
        if (null != responseStatus) {
            if (!responseStatus.reason().isEmpty()) {
                return responseStatus.reason();
            }

            final HttpStatus httpStatus = getHttpStatus(responseStatus);
            if (null != httpStatus) {
                return httpStatus.getReasonPhrase();
            }
        }

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

    public HttpError<T> withDetails(T details) {
        this.details = details;
        return this;
    }

    public List<String> getCallbackErrors() {
        return callbackErrors;
    }

    public HttpError withCallbackErrors(List<String> callbackErrors) {
        this.callbackErrors = callbackErrors;
        return this;
    }

    public List<String> getCallbackWarnings() {
        return callbackWarnings;
    }

    public HttpError withCallbackWarnings(List<String> callbackWarnings) {
        this.callbackWarnings = callbackWarnings;
        return this;
    }
}
