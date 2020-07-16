package uk.gov.hmcts.ccd.endpoint.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(
    code = HttpStatus.FORBIDDEN
)
public class CaseAccessException extends ApiException {

    public CaseAccessException(String message) {
        super(message);
    }
}
