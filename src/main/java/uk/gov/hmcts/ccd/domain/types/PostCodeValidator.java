package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.hmcts.ccd.domain.types.TextValidator.checkMax;
import static uk.gov.hmcts.ccd.domain.types.TextValidator.checkMin;

@Named
@Singleton
public class PostCodeValidator implements BaseTypeValidator {
    static final String TYPE_ID = "PostCode";

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

        if (!dataValue.isTextual()) {
            return Collections.singletonList(new ValidationResult(dataValue + " needs to be a valid " + TYPE_ID,
                                                                  dataFieldId));
        }

        final String value = dataValue.textValue();

        if (isBlank(value)) {
            return Collections.singletonList(new ValidationResult("post code is empty", dataFieldId));
        }

        if (!checkMax(caseFieldDefinition.getFieldTypeDefinition().getMax(), value)) {
            return Collections.singletonList(new ValidationResult("Post code '" + value + "' exceeds maximum length "
                + caseFieldDefinition.getFieldTypeDefinition().getMax(), dataFieldId));
        }

        if (!checkMin(caseFieldDefinition.getFieldTypeDefinition().getMin(), value)) {
            return Collections.singletonList(new ValidationResult("Post code '" + value + "' requires minimum length "
                + caseFieldDefinition.getFieldTypeDefinition().getMin(), dataFieldId));
        }

        final String userRegex = caseFieldDefinition.getFieldTypeDefinition().getRegularExpression();
        if (userRegex != null && userRegex.length() > 0) {
            return (value.matches(userRegex))
                ? Collections.emptyList()
                : Collections.singletonList((new ValidationResult(REGEX_GUIDANCE, dataFieldId)));
        }

        final String baseTypeRegex = getType().getRegularExpression();
        if (!checkRegex(baseTypeRegex, value)) {
            return Collections.singletonList(
                new ValidationResult(REGEX_GUIDANCE, dataFieldId)
            );
        }
        return Collections.emptyList();
    }

    private Boolean checkRegex(final String regex, final String value) {
        return value.toUpperCase().matches(regex);
    }
}
