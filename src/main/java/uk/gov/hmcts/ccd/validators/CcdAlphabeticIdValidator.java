package uk.gov.hmcts.ccd.validators;

import uk.gov.hmcts.ccd.validators.annotations.CcdAlphabeticId;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class CcdAlphabeticIdValidator implements ConstraintValidator<CcdAlphabeticId, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        if (value == null || value.isEmpty()) {
            context.buildConstraintViolationWithTemplate("The id cannot be empty or null and it must be alphabetical.")
                .addConstraintViolation();
            return false;
        }
        if (value.contains(" ")) {
            context.buildConstraintViolationWithTemplate("The id cannot spaces.")
                .addConstraintViolation();
            return false;
        }
        return true;
    }
}
