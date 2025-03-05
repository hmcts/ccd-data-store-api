package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;

import static uk.gov.hmcts.ccd.domain.types.TextValidator.checkMax;
import static uk.gov.hmcts.ccd.domain.types.TextValidator.checkMin;
import static uk.gov.hmcts.ccd.domain.types.TextValidator.checkRegex;

@Named
@Singleton
public class EmailValidator implements BaseTypeValidator {
    static final String TYPE_ID = "Email";

    private static final Logger LOG = LoggerFactory.getLogger(EmailValidator.class);

    private void jclog(final String message) {
        //System.out.println("JCDEBUG: " + message);
        LOG.debug("JCDEBUG: debug: {}", message);
        LOG.info("JCDEBUG: info: {}", message);
        LOG.warn("JCDEBUG: warn: {}", message);
        LOG.error("JCDEBUG: error: {}", message);
    }

    @Override
    public BaseType getType() {
        return BaseType.get(TYPE_ID);
    }

    @Override
    public List<ValidationResult> validate(final String dataFieldId,
                                           final JsonNode dataValue,
                                           final CaseFieldDefinition caseFieldDefinition) {
        jclog("validate()");

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

        final String regex1 = caseFieldDefinition.getFieldTypeDefinition().getRegularExpression();
        if (regex1 == null) {
            jclog(regex1 + "  ,  " + value + "  ,  " + checkRegex(regex1, value) + "  ,  NULL");
        } else {
            final boolean match1 = value.matches(regex1);
            jclog(regex1 + "  ,  " + value + "  ,  " + checkRegex(regex1, value) + "  ,  " + match1);
        }
        if (!checkRegex(regex1, value)) {
            jclog("caseFieldDefinition.getFieldTypeDefinition().getRegularExpression() = "
                + caseFieldDefinition.getFieldTypeDefinition().getRegularExpression());
            return Collections.singletonList(
                new ValidationResult(REGEX_GUIDANCE, dataFieldId)
            );
        }

        final String regex2 = getType().getRegularExpression();
        if (regex2 == null) {
            jclog(regex2 + "  ,  " + value + "  ,  " + checkRegex(regex2, value) + "  ,  NULL");
        } else {
            final boolean match2 = value.matches(regex2);
            jclog(regex2 + "  ,  " + value + "  ,  " + checkRegex(regex2, value) + "  ,  " + match2);
        }
        if (!checkRegex(regex2, value)) {
            jclog("getType().getRegularExpression() = " + getType().getRegularExpression());
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
        /*
        if (email.contains("?")) {
            return false;
        }
        */
        return org.apache.commons.validator.routines.EmailValidator.getInstance().isValid(email);
    }
}
