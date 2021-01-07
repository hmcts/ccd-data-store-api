package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;

import static uk.gov.hmcts.ccd.domain.types.TextValidator.checkMax;
import static uk.gov.hmcts.ccd.domain.types.TextValidator.checkMin;
import static uk.gov.hmcts.ccd.domain.types.TextValidator.checkRegex;

@Named("RegionValidator")
@Singleton
public class RegionValidator implements BaseTypeValidator {
    public static final String TYPE_ID = "Region";

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

        if (!checkRegex(caseFieldDefinition.getFieldTypeDefinition().getRegularExpression(), value)) {
            return Collections.singletonList(new ValidationResult(REGEX_GUIDANCE, dataFieldId));
        }

        if (!checkMin(caseFieldDefinition.getFieldTypeDefinition().getMin(), value)) {
            return Collections.singletonList(
                new ValidationResult("Region '" + value
                    + "' requires minimum length " + caseFieldDefinition.getFieldTypeDefinition().getMin(), dataFieldId)
            );
        }

        if (!checkMax(caseFieldDefinition.getFieldTypeDefinition().getMax(), value)) {
            return Collections.singletonList(
                new ValidationResult("Region '" + value
                    + "' exceeds maximum length " + caseFieldDefinition.getFieldTypeDefinition().getMax(), dataFieldId)
            );
        }

        return Collections.emptyList();
    }
}
