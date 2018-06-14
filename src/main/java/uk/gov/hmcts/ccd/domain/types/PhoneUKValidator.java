package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;

import static uk.gov.hmcts.ccd.domain.types.TextValidator.checkMax;
import static uk.gov.hmcts.ccd.domain.types.TextValidator.checkMin;

@Named
@Singleton
public class PhoneUKValidator implements BaseTypeValidator {
    private static final String TYPE_ID = "PhoneUK";

    @Override
    public BaseType getType() {
        return BaseType.get(TYPE_ID);
    }

    @Override
    public List<ValidationResult> validate(final String dataFieldId,
                                           final JsonNode dataValue,
                                           final CaseField caseFieldDefinition) {
        if (isNullOrEmpty(dataValue)) {
            return Collections.emptyList();
        }

        final String value = dataValue.textValue();
        if (!checkMax(caseFieldDefinition.getFieldType().getMax(), value)) {
            return Collections.singletonList(new ValidationResult("Phone no '" + value
                + "' exceeds maximum length " + caseFieldDefinition.getFieldType().getMax(), dataFieldId));
        }

        if (!checkMin(caseFieldDefinition.getFieldType().getMin(), value)) {
            return Collections.singletonList(new ValidationResult("Phone no '" + value
                + "' requires minimum length " + caseFieldDefinition.getFieldType().getMin(), dataFieldId));
        }

        final String userRegex = caseFieldDefinition.getFieldType().getRegularExpression();
        if (userRegex != null && userRegex.length() > 0) {
            return (value.matches(userRegex)) ?
                Collections.emptyList() :
                Collections.singletonList((new ValidationResult(REGEX_GUIDANCE, dataFieldId)));
        }

        final String baseTypeRegex = getType().getRegularExpression();
        return (value.matches(baseTypeRegex)) ?
            Collections.emptyList() :
            Collections.singletonList(
                new ValidationResult(REGEX_GUIDANCE, dataFieldId)
            );
    }
}
