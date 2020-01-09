package uk.gov.hmcts.ccd.validators.annotations;

import uk.gov.hmcts.ccd.validators.UuIdValidator;

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
@Constraint(validatedBy = UuIdValidator.class)
@Documented
public @interface UuId {

    String message() default "This is not a valid UUID.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
