package uk.gov.hmcts.ccd.validators.annotations;

import uk.gov.hmcts.ccd.validators.CaseIDValidator;

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
@Constraint(validatedBy = CaseIDValidator.class)
@Documented
public @interface CaseID {

    String message() default "This case id is not valid.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
