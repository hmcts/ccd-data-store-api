package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;

import javax.inject.Named;
import javax.inject.Singleton;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;

/**
 * Money GBP is represented in pence.
 */
@Named
@Singleton
public class MoneyGBPValidator implements BaseTypeValidator {
    static final String TYPE_ID = "MoneyGBP";

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

        final String value = dataValue.textValue();
        final Long numberValue;
        try {
            if (null == value) {
                // dataValue may be a boolean, array or pojo node
                return Collections.singletonList(new ValidationResult(dataValue + " is not valid " + TYPE_ID,
                                                                      dataFieldId));
            }
            numberValue = Long.valueOf(value);
            if (!checkMax(caseFieldDefinition.getFieldTypeDefinition().getMax(), numberValue)) {
                return Collections.singletonList(
                    new ValidationResult("Should be less than or equal to "
                        + convertToSterlingString(caseFieldDefinition.getFieldTypeDefinition().getMax()), dataFieldId)
                );
            }

            if (!checkMin(caseFieldDefinition.getFieldTypeDefinition().getMin(), numberValue)) {
                return Collections.singletonList(
                    new ValidationResult("Should be more than or equal to "
                        + convertToSterlingString(caseFieldDefinition.getFieldTypeDefinition().getMin()), dataFieldId)
                );
            }
        } catch (NumberFormatException e) {
            return Collections.singletonList(new ValidationResult(value + " is not a valid value.  "
                + "Money GBP needs to be expressed in pence", dataFieldId));
        }

        return Collections.emptyList();
    }

    private String convertToSterlingString(BigDecimal amountInPence) {
        return new DecimalFormat("£#,##0.00").format(amountInPence.divide(new BigDecimal(100)));
    }

    private Boolean checkMax(final BigDecimal max, final Long numberValue) {
        return max == null || ((Long) max.longValue()).compareTo(numberValue) >= 0;
    }

    private Boolean checkMin(final BigDecimal min, final Long numberValue) {
        return min == null || numberValue.compareTo(min.longValue()) >= 0;
    }
}
