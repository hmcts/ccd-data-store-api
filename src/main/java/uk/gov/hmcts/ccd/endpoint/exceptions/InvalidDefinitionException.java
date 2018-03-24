package uk.gov.hmcts.ccd.endpoint.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.UNPROCESSABLE_ENTITY)
public class InvalidDefinitionException extends ApiException {
    public InvalidDefinitionException(final String message) {
        super(message);
    }
}
