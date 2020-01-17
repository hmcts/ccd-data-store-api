package uk.gov.hmcts.ccd.endpoint.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(
    code = HttpStatus.BAD_REQUEST
)
public class InvalidCaseRoleException extends ApiException {
    public InvalidCaseRoleException(String caseRole) {
        super("Invalid case role: " + caseRole);
    }
}
