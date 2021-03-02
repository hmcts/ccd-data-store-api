package uk.gov.hmcts.ccd.domain.service.getcase;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class CaseNotFoundException extends ResourceNotFoundException {
    public CaseNotFoundException(String jurisdictionId, String caseTypeId, String caseReference) {
        super(String.format("Cannot find case for given criteria: %s, %s, %s", jurisdictionId, caseTypeId,
            caseReference));
    }

    public CaseNotFoundException(String caseReference) {
        super(String.format("No case found for reference: %s", caseReference));
    }
}
