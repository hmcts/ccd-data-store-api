package uk.gov.hmcts.ccd.endpoint.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.hmcts.ccd.domain.model.std.CaseFieldValidationError;
import uk.gov.hmcts.ccd.domain.model.std.CaseValidationError;

import java.util.List;
import java.util.stream.Collectors;

@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY)
public class CaseValidationException extends ValidationException {

    private final List<String> fields;

    public CaseValidationException(List<CaseFieldValidationError> fieldErrors) {
        super("Case data validation failed");

        // prepare APiException.Details for use in HttpError response
        super.withDetails(new CaseValidationError(fieldErrors));

        // record field IDs for use with AppInsights
        this.fields = fieldErrors.stream()
            .map(CaseFieldValidationError::getId)
            .collect(Collectors.toList());
    }

    public List<String> getFields() {
        return fields;
    }
}
