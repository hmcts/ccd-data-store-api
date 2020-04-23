package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.hmcts.ccd.endpoint.exceptions.ApiException;

@ResponseStatus(value = HttpStatus.FORBIDDEN)
public class CaseDocumentNotAccessibleException extends ApiException {

    public CaseDocumentNotAccessibleException(String message) {
        super(message);
    }

}
