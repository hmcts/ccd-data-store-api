package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.Collections;
import java.util.List;

import static uk.gov.hmcts.ccd.domain.types.TextValidator.checkMax;
import static uk.gov.hmcts.ccd.domain.types.TextValidator.checkMin;
import static uk.gov.hmcts.ccd.domain.types.TextValidator.checkRegex;

@Named
@Singleton
public class EmailValidator implements BaseTypeValidator {
    static final String TYPE_ID = "Email";

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

        if (null == value) {
            return Collections.singletonList(new ValidationResult(dataValue + " is not a valid email",
                dataFieldId));
        }

        if (!checkMax(caseFieldDefinition.getFieldTypeDefinition().getMax(), value)) {
            return Collections.singletonList(new ValidationResult("Email '" + value
                + "' exceeds maximum length " + caseFieldDefinition.getFieldTypeDefinition().getMax(), dataFieldId));
        }

        if (!checkMin(caseFieldDefinition.getFieldTypeDefinition().getMin(), value)) {
            return Collections.singletonList(new ValidationResult("Email '" + value
                + "' requires minimum length " + caseFieldDefinition.getFieldTypeDefinition().getMin(), dataFieldId));
        }

        if (!checkRegex(caseFieldDefinition.getFieldTypeDefinition().getRegularExpression(), value)) {
            return Collections.singletonList(
                new ValidationResult(REGEX_GUIDANCE, dataFieldId)
            );
        }

        if (!checkRegex(getType().getRegularExpression(), value)) {
            return Collections.singletonList(
                new ValidationResult(REGEX_GUIDANCE, dataFieldId)
            );
        }

        if (!isValidEmailAddress(value)) {
            return Collections.singletonList(new ValidationResult(value + " is not a valid Email address",
                dataFieldId));
        }

        return Collections.emptyList();
    }

    private boolean isValidEmailAddress(final String email) {
        try {
            new InternetAddress(email).validate();
            return true;
        } catch (AddressException ex) {
            return false;
        }
    }
}
