package uk.gov.hmcts.ccd.domain.model.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import uk.gov.hmcts.ccd.error.ErrorCode;
import uk.gov.hmcts.ccd.error.PublicValidationMessages;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.util.UriUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

public class HttpError<T extends Serializable> implements Serializable {
    public static final Integer DEFAULT_STATUS = HttpStatus.INTERNAL_SERVER_ERROR.value();
    public static final String DEFAULT_ERROR = "Internal Server Error";

    private final String exception;
    private final transient LocalDateTime timestamp;
    private final Integer status;
    private final String error;
    private String message;
    private final String path;
    private T details;
    private List<String> callbackErrors;
    private List<String> callbackWarnings;

    public HttpError(Exception exception, HttpServletRequest request) {
        this(exception, request.getRequestURI(), null);
    }

    public HttpError(Exception exception, String path, HttpStatus status) {
        final var responseStatus = exception.getClass().getAnnotation(ResponseStatus.class);

        this.exception = exception.getClass().getName();
        this.timestamp = LocalDateTime.now(ZoneOffset.UTC);
        this.status = getStatusFromResponseStatus(responseStatus, status);
        this.error = HttpStatus.valueOf(this.status).getReasonPhrase();
        this.message = PublicValidationMessages.messageFor(exception, this.status);
        this.path = UriUtils.encodePath(path, StandardCharsets.UTF_8);
    }

    public HttpError(Exception exception, HttpServletRequest request, HttpStatus upstreamPreferredHttpStatus) {
        this(exception, request.getRequestURI(), upstreamPreferredHttpStatus);
    }

    private Integer getStatusFromResponseStatus(ResponseStatus responseStatus, HttpStatus status) {
        if (status != null) {
            return status.value();
        }
        if (null != responseStatus) {
            final var httpStatus = getHttpStatus(responseStatus);
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

    @JsonIgnore
    public String getException() {
        return exception;
    }

    public ErrorCode getCode() {
        return ErrorCode.fromStatus(status);
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

    @JsonIgnore
    public String getPath() {
        return path;
    }

    public T getDetails() {
        return details;
    }

    public HttpError<T> withDetails(T details) {
        this.details = details instanceof Throwable ? null : details;
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

    public HttpError(Exception exception, WebRequest request, HttpStatus status) {
        this(exception, ((ServletWebRequest) request).getRequest().getRequestURI(), status);
    }

    public HttpError<T> withMessage(String message) {
        this.message = message;
        return this;
    }
}
