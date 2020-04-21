package uk.gov.hmcts.ccd.endpoint.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.FORBIDDEN)
public class DocumentTokenException extends RuntimeException {
    public DocumentTokenException(final String message) {
        super(message);
    }
}
