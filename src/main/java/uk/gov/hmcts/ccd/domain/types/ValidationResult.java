package uk.gov.hmcts.ccd.domain.types;

import lombok.ToString;

@ToString
public class ValidationResult {
    private final String errorMessage;
    private final String fieldId;

    ValidationResult(final String errorMessage, final String fieldId) {
        this.errorMessage = errorMessage;
        this.fieldId = fieldId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getFieldId() {
        return fieldId;
    }

}
