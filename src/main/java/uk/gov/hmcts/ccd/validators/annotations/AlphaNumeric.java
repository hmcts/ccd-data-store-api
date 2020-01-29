package uk.gov.hmcts.ccd.validators.annotations;

import uk.gov.hmcts.ccd.validators.AlphaNumericValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({PARAMETER, FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = AlphaNumericValidator.class)
@Documented
public @interface AlphaNumeric {

    String message() default "This is not a valid alphanumeric id.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
