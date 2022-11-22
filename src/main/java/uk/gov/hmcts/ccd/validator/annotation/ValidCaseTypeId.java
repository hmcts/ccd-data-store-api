package uk.gov.hmcts.ccd.validator.annotation;

import uk.gov.hmcts.ccd.validator.CaseTypeIdValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;

@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = CaseTypeIdValidator.class)
public @interface ValidCaseTypeId {
    String message() default "Case Type Id is invalid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
