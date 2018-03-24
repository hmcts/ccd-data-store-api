package uk.gov.hmcts.ccd.domain.types;

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

    @Override
    public String toString() {
        return "ValidationResult{" +
            "errorMessage='" + errorMessage + '\'' +
            ", fieldId='" + fieldId + '\'' +
            '}';
    }
}
