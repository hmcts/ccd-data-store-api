package uk.gov.hmcts.ccd.endpoint.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.hmcts.ccd.domain.model.std.CaseFieldValidationError;
import uk.gov.hmcts.ccd.domain.model.std.CaseValidationError;

import java.util.List;

@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY)
public class CaseValidationException extends ValidationException {

    private final String[] fields;

    public CaseValidationException(List<CaseFieldValidationError> fieldErrors) {
        super("Case data validation failed");

        String[] fieldIds = new String[0];

        if (fieldErrors != null && !fieldErrors.isEmpty()) {
            // prepare APiException.Details for use in HttpError response
            super.withDetails(new CaseValidationError(fieldErrors));

            // record field IDs for use with AppInsights
            fieldIds = fieldErrors.stream()
                .map(CaseFieldValidationError::getId)
                .toArray(String[]::new);
        }

        this.fields = fieldIds;

    }

    public String[] getFields() {
        return fields;
    }
}
