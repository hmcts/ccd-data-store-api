package uk.gov.hmcts.ccd.validators;

import uk.gov.hmcts.ccd.validators.annotations.UuId;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.UUID;

public class UuIdValidator implements ConstraintValidator<UuId, String> {

    @Override
    public boolean isValid(final String value, final ConstraintValidatorContext context) {

        try {
            UUID.fromString(value);
            return true;
        } catch (Exception exception) {
            context.buildConstraintViolationWithTemplate("The value " + value + " does not represent a valid id.")
                .addConstraintViolation();
            return false;
        }
    }
}
