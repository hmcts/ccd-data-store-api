package uk.gov.hmcts.ccd.domain.types;

import org.apache.commons.lang3.builder.ToStringBuilder;

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
        return new ToStringBuilder(this)
            .append("errorMessage", errorMessage)
            .append("fieldId", fieldId)
            .toString();
    }
}
