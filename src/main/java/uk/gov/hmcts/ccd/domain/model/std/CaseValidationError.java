package uk.gov.hmcts.ccd.domain.model.std;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

public class CaseValidationError implements Serializable {

    @JsonProperty("field_errors")
    private final List<CaseFieldValidationError> fieldErrors;

    public CaseValidationError(List<CaseFieldValidationError> fieldErrors) {
        this.fieldErrors = fieldErrors;
    }
}
