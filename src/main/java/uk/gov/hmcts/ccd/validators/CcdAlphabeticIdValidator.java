package uk.gov.hmcts.ccd.validators;

import uk.gov.hmcts.ccd.validators.annotations.CcdAlphabeticId;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class CcdAlphabeticIdValidator implements ConstraintValidator<CcdAlphabeticId, String> {

    final String alphabeticExpression = "[a-zA-Z]+";

    @Override
    public boolean isValid(final String value, final ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        if (value == null || value.isEmpty()) {
            context.buildConstraintViolationWithTemplate("The id cannot be empty or null and it must be alphabetical.")
                .addConstraintViolation();
            return false;
        }
        if (!value.matches(alphabeticExpression)) {
            context.buildConstraintViolationWithTemplate("The id have to be a character sequence of [a-zA-Z]")
                .addConstraintViolation();
            return false;
        }
        return true;
    }
}

