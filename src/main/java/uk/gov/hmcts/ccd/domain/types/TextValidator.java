package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;

import javax.inject.Named;
import javax.inject.Singleton;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Named("TextValidator")
@Singleton
public class TextValidator implements BaseTypeValidator {
    public static final String TYPE_ID = "Text";

    private static final int FIELD_VALUE_SUMMARY_MAX_LENGTH = 50;

    @Override
    public BaseType getType() {
        return BaseType.get(TYPE_ID);
    }

    @Override
    public List<ValidationResult> validate(final String dataFieldId,
                                           final JsonNode dataValue,
                                           final CaseFieldDefinition caseFieldDefinition) {

        // Empty text should still check against MIN - MIN may or may not be 0
        if (isNullOrEmpty(dataValue)) {
            return Collections.emptyList();
        }

        if (!dataValue.isTextual()) {
            final String nodeType = dataValue.getNodeType().toString().toLowerCase();
            return Collections.singletonList(new ValidationResult(nodeType + " is not a string",
                dataFieldId));
        }

        final String value = dataValue.textValue();

        if (!checkMax(caseFieldDefinition.getFieldTypeDefinition().getMax(), value)) {
            String fieldValueSummary = getFieldValueSummary(value);
            return Collections.singletonList(
                new ValidationResult(fieldValueSummary + " exceeds the maximum length of " + caseFieldDefinition
                    .getFieldTypeDefinition().getMax(), dataFieldId)
            );
        }

        if (!checkMin(caseFieldDefinition.getFieldTypeDefinition().getMin(), value)) {
            String fieldValueSummary = getFieldValueSummary(value);
            return Collections.singletonList(
                new ValidationResult(fieldValueSummary + " requires a minimum length of " + caseFieldDefinition
                    .getFieldTypeDefinition().getMin(), dataFieldId)
            );
        }

        if (!checkRegex(caseFieldDefinition.getFieldTypeDefinition().getRegularExpression(), value)) {
            return Collections.singletonList(new ValidationResult(REGEX_GUIDANCE, dataFieldId));
        }

        return Collections.emptyList();
    }

    private static String getFieldValueSummary(String value) {
        if (value == null) {
            return null;
        }

        int valueLength = value.length();

        String truncatedValue = valueLength > FIELD_VALUE_SUMMARY_MAX_LENGTH
            ? value.substring(0, FIELD_VALUE_SUMMARY_MAX_LENGTH) + "..." : value;

        return "\"%s\" (%d %s)"
            .formatted(truncatedValue, valueLength, valueLength == 1 ? "character" : "characters");
    }

    static Boolean checkMax(final BigDecimal max, final String value) {
        return max == null || value.length() <= max.intValue();
    }

    static Boolean checkMin(final BigDecimal min, final String value) {
        return min == null || value.length() >= min.intValue();
    }

    static Boolean checkRegex(final String regex, final String value) {
        return regex == null || regex.length() == 0 || value.matches(regex);
    }
}
