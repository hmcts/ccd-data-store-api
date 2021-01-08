package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

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

    default List<ValidationResult> validateRegex(final CaseFieldDefinition field, final String value,
                                                 final String data) {
        String regex = field.getFieldTypeDefinition().getRegularExpression();
        if (!(regex == null || regex.length() == 0 || value.matches(regex))) {
            return Collections.singletonList(new ValidationResult(REGEX_GUIDANCE, data)
            );
        } else {
            return null;
        }
    }

    default List<ValidationResult> validateMin(final CaseFieldDefinition field, final String value, final String data) {
        BigDecimal min = field.getFieldTypeDefinition().getMin();
        if (!(min == null || value.length() >= min.intValue())) {
            return Collections.singletonList(new ValidationResult(value + " require minimum length "
                + min, data)
            );
        } else {
            return null;
        }
    }

    default List<ValidationResult> validateMax(final CaseFieldDefinition field, final String value, final String data) {
        BigDecimal max = field.getFieldTypeDefinition().getMax();
        if (!(max == null || value.length() <= max.intValue())) {
            return Collections.singletonList(
                new ValidationResult(value + " exceed maximum length " + max, data)
            );
        } else {
            return null;
        }
    }
}

