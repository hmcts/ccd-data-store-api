package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;

import java.util.List;

public interface BaseTypeValidator {

    String REGEX_GUIDANCE
        = "The data entered is not valid for this type of field, please delete and re-enter using only valid data";

    BaseType getType();

    List<ValidationResult> validate(final String dataFieldId,
                                    final JsonNode dataValue,
                                    final CaseField caseFieldDefinition);

    default Boolean isNullOrEmpty(final JsonNode dataValue) {
        return dataValue == null
            || dataValue.isNull()
            || (dataValue.isTextual() && (null == dataValue.asText() || dataValue.asText().length() == 0))
            || (dataValue.isObject() && dataValue.toString().equals("{}"));
    }

    default Boolean isNull(final JsonNode dataValue) {
        return dataValue == null
            || dataValue.isNull()
            || (dataValue.isTextual() && (null == dataValue.asText()));
    }
}
