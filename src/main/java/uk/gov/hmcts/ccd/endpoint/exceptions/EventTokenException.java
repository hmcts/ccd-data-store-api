package uk.gov.hmcts.ccd.endpoint.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.FORBIDDEN)
public class EventTokenException extends RuntimeException {
    public EventTokenException(final String message) {
        super(message);
    }
}
