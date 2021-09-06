package uk.gov.hmcts.ccd.domain.model.std.validator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Constraint(validatedBy = {PartiesValidator.class})
@Target({PARAMETER, FIELD})
@Retention(RUNTIME)
public @interface ValidPartiesItems {


    String message() default ValidationError.PARTIES_INVALID;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};


}
