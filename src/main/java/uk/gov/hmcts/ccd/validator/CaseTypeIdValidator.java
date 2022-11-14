package uk.gov.hmcts.ccd.validator;

import uk.gov.hmcts.ccd.validator.annotation.ValidCaseTypeId;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.PatternSyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CaseTypeIdValidator implements ConstraintValidator<ValidCaseTypeId, String> {

    private static final Logger LOG = LoggerFactory.getLogger(CaseTypeIdValidator.class);

    private static final String REG_EXP_VALID_CASE_TYPE_ID = "^[a-zA-Z0-9]+[a-zA-Z_0-9\\-.]*$";

    @Override
    public boolean isValid(String caseTypeId, ConstraintValidatorContext context) {
        if (caseTypeId != null) {
            try {
                return caseTypeId.matches(REG_EXP_VALID_CASE_TYPE_ID);
            } catch (final PatternSyntaxException e) {
                LOG.error(e.getMessage());
            }
        }
        return false;
    }
}
