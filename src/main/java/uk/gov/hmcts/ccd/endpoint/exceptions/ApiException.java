package uk.gov.hmcts.ccd.endpoint.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serializable;
import java.util.List;

@ResponseStatus(
    code = HttpStatus.UNPROCESSABLE_ENTITY
)
public class ApiException extends RuntimeException {

    private Serializable details;
    private List<String> callbackWarnings;
    private List<String> callbackErrors;

    public ApiException(String message) {
        super(message);
    }

    public ApiException(final String message, final Throwable e) {
        super(message, e);
    }

    public ApiException withDetails(Serializable details) {
        this.details = details;
        return this;
    }

    public Serializable getDetails() {
        return details;
    }

    public List<String> getCallbackWarnings() {
        return callbackWarnings;
    }

    public ApiException withWarnings(List<String> warnings) {
        this.callbackWarnings = warnings;
        return this;
    }

    public List<String> getCallbackErrors() {
        return callbackErrors;
    }

    public ApiException withErrors(List<String> errors) {
        this.callbackErrors = errors;
        return this;
    }
}
