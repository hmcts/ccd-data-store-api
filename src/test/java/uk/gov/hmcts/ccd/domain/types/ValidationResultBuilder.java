package uk.gov.hmcts.ccd.domain.types;

public class ValidationResultBuilder {

    private String errorMessage;
    private String fieldId;

    public ValidationResultBuilder() {
        errorMessage = null;
        fieldId = null;
    }

    public ValidationResultBuilder setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    public ValidationResultBuilder setFieldId(String fieldId) {
        this.fieldId = fieldId;
        return this;
    }

    public ValidationResult build() {
        return new ValidationResult(this.errorMessage, this.fieldId);
    }

}
