package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;

public interface BaseTypeValidator extends FieldValidator {

    String REGEX_GUIDANCE
        = "The data entered is not valid for this type of field, please delete and re-enter using only valid data";

    BaseType getType();

    default Boolean isNullOrEmpty(final JsonNode dataValue) {
        return dataValue == null
            || dataValue.isNull()
            || (dataValue.isTextual() && (null == dataValue.asText() || dataValue.asText().trim().length() == 0))
            || (dataValue.isObject() && dataValue.toString().equals("{}"));
    }
}
