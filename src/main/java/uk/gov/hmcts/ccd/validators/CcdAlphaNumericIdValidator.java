package uk.gov.hmcts.ccd.validators;

import uk.gov.hmcts.ccd.validators.annotations.CcdAlphaNumericId;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class CcdAlphaNumericIdValidator implements ConstraintValidator<CcdAlphaNumericId, String> {

    final String alphaNumericExpression = "[a-zA-Z0-9]+";

    @Override
    public boolean isValid(final String value, final ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        if (value == null || value.isEmpty()) {
            context.buildConstraintViolationWithTemplate("The id cannot be empty or null and it must be alphabetical.")
                .addConstraintViolation();
            return false;
        }
        if (!value.matches(alphaNumericExpression)) {
            context.buildConstraintViolationWithTemplate("The id have to be a character sequence of [a-zA-Z0-9]")
                .addConstraintViolation();
            return false;
        }
        return true;
    }
}

