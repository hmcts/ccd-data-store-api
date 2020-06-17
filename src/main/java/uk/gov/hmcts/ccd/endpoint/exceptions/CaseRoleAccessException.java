package uk.gov.hmcts.ccd.endpoint.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(
    code = HttpStatus.FORBIDDEN
)
public class CaseRoleAccessException extends ApiException {

    public CaseRoleAccessException(String message) {
        super(message);
    }
}
