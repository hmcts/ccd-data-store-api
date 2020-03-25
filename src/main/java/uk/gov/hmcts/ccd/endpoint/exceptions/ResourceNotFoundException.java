package uk.gov.hmcts.ccd.endpoint.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(final String message) {
        this(message, null);
    }

    public ResourceNotFoundException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
