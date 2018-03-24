package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;

import java.util.List;

public interface BaseTypeValidator {
    BaseType getType();

    List<ValidationResult> validate(final String dataFieldId,
                                    final JsonNode dataValue,
                                    final CaseField caseFieldDefinition);

    default Boolean isNullOrEmpty(final JsonNode dataValue) {
        return dataValue == null
            || dataValue.isNull()
            || (dataValue.isTextual() && (null == dataValue.asText() || dataValue.asText().trim().length() == 0))
            || (dataValue.isObject() && dataValue.toString().equals("{}"));
    }
}
