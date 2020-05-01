package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FixedListItemDefinition;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;

import static uk.gov.hmcts.ccd.domain.types.TextValidator.checkRegex;

@Named
@Singleton
public class FixedListValidator implements BaseTypeValidator {
    static final String TYPE_ID = "FixedList";

    @Override
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
        final List<FixedListItemDefinition> validValues = caseFieldDefinition.getFieldTypeDefinition().getFixedListItemDefinitions();

        if (!checkRegex(getType().getRegularExpression(), value)) {
            return Collections.singletonList(new ValidationResult("'" + value + "' failed FixedList Type Regex check: "
                + getType().getRegularExpression(), dataFieldId));
        }

        return validValues.stream().anyMatch(fixedListItem -> fixedListItem.getCode().equals(value))
            ? typeChecks(dataFieldId, value, caseFieldDefinition)
            : Collections.singletonList(new ValidationResult(value + " is not a valid value", dataFieldId));
    }

    private List<ValidationResult> typeChecks(final String dataFieldId,
                                              final String value,
                                              final CaseFieldDefinition caseFieldDefinition) {
        if (!checkRegex(caseFieldDefinition.getFieldTypeDefinition().getRegularExpression(), value)) {
            return Collections.singletonList(new ValidationResult(REGEX_GUIDANCE, dataFieldId));
        }
        return Collections.emptyList();
    }
}
