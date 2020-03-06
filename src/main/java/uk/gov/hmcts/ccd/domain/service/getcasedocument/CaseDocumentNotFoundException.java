package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class CaseDocumentNotFoundException extends ResourceNotFoundException {

    public CaseDocumentNotFoundException(String message) {
        //super(String.format("No document found for this case reference: %s", caseReference));
        super(message);
    }

}
