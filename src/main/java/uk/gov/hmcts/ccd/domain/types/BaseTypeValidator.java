package uk.gov.hmcts.ccd.domain.types;

import tools.jackson.databind.JsonNode;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;

import java.util.List;

import static uk.gov.hmcts.ccd.config.JacksonUtils.asText;

public interface BaseTypeValidator extends FieldValidator {

    String REGEX_GUIDANCE
        = "The data entered is not valid for this type of field, please delete and re-enter using only valid data";

    BaseType getType();

    default Boolean isNullOrEmpty(final JsonNode dataValue) {
        return dataValue == null
            || dataValue.isNull()
            || (dataValue.isString() && (null == asText(dataValue) || asText(dataValue).trim().length() == 0))
            || (dataValue.isObject() && dataValue.toString().equals("{}"));
    }

    default List<ValidationResult> validate(ValidationContext validationContext) {
        return validate(
            validationContext.getFieldId(),
            validationContext.getFieldValue(),
            validationContext.getFieldDefinition()
        );
    }

    List<ValidationResult> validate(final String dataFieldId,
                                    final JsonNode dataValue,
                                    final CaseFieldDefinition caseFieldDefinition);
}
