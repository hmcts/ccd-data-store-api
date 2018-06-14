package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;

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
    private static final String TYPE_ID = "Email";

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

        if (null == value) {
            return Collections.singletonList(new ValidationResult(dataValue + " is not a valid email", dataFieldId));
        }

        if (!checkMax(caseFieldDefinition.getFieldType().getMax(), value)) {
            return Collections.singletonList(new ValidationResult("Email '" + value + "' exceeds maximum length "
                + caseFieldDefinition.getFieldType().getMax(), dataFieldId));
        }

        if (!checkMin(caseFieldDefinition.getFieldType().getMin(), value)) {
            return Collections.singletonList(new ValidationResult("Email '" + value + "' requires minimum length "
                + caseFieldDefinition.getFieldType().getMin(), dataFieldId));
        }

        if (!checkRegex(caseFieldDefinition.getFieldType().getRegularExpression(), value)) {
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
            return Collections.singletonList(new ValidationResult(value + " is not a valid Email address", dataFieldId));
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
