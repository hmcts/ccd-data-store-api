package uk.gov.hmcts.ccd.endpoint.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.GATEWAY_TIMEOUT)
public class CallbackException extends RuntimeException {
    public CallbackException(final String message) {
        super(message);
    }
}
