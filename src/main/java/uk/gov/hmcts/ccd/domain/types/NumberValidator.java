package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;

import static uk.gov.hmcts.ccd.domain.types.TextValidator.checkRegex;

public class NumberValidator implements BaseTypeValidator {
    private static final Logger LOG = LoggerFactory.getLogger(NumberValidator.class);

    static final String TYPE_ID = "Number";

    public BaseType getType() {
        return BaseType.get(TYPE_ID);
    }

    @Override
    public List<ValidationResult> validate(final String dataFieldId,
                                           final JsonNode dataValue,
                                           final CaseFieldDefinition caseFieldDefinition) {
        if (isNullOrEmpty(dataValue)) {
            return Collections.emptyList();
        }
        LOG.info("Validating Number field id {}", caseFieldDefinition.getId());

        final String value = dataValue.textValue();
        final BigDecimal numberValue;

        try {
            if (dataValue.isNumber()) {
                numberValue = dataValue.decimalValue();
            } else if (null == value) {
                // dataValue may be a boolean, array or pojo node
                return Collections.singletonList(new ValidationResult(dataValue + " is not a number",
                    dataFieldId));
            } else {
                numberValue = new BigDecimal(value);
            }
            LOG.info("Number field text value {} & number value {}", value, numberValue);
            if (!checkMax(caseFieldDefinition.getFieldTypeDefinition().getMax(), numberValue)) {
                return Collections.singletonList(
                    new ValidationResult("Should be less than or equal to " + caseFieldDefinition
                        .getFieldTypeDefinition().getMax(), dataFieldId)
                );
            }

            if (!checkMin(caseFieldDefinition.getFieldTypeDefinition().getMin(), numberValue)) {
                return Collections.singletonList(
                    new ValidationResult("Should be more than or equal to " + caseFieldDefinition
                        .getFieldTypeDefinition().getMin(), dataFieldId)
                );
            }

            if (!checkRegex(caseFieldDefinition.getFieldTypeDefinition().getRegularExpression(), value)) {
                return Collections.singletonList(new ValidationResult(REGEX_GUIDANCE, dataFieldId));
            }

            if (!checkRegex(getType().getRegularExpression(), numberValue.toString())) {
                return Collections.singletonList(new ValidationResult("'" + numberValue
                    + "' failed number Type Regex check: " + getType().getRegularExpression(), dataFieldId));
            }
        } catch (NumberFormatException e) {
            LOG.info("Number field exception {} ", e);
            return Collections.singletonList(new ValidationResult(value + " is not a number", dataFieldId));
        }

        return Collections.emptyList();
    }

    private Boolean checkMax(final BigDecimal max, final BigDecimal numberValue) {
        return max == null || max.compareTo(numberValue) >= 0;
    }

    private Boolean checkMin(final BigDecimal min, final BigDecimal numberValue) {
        return min == null || numberValue.compareTo(min) >= 0;
    }
}
