package uk.gov.hmcts.ccd.domain.model.std.validator.globalsearch;

import uk.gov.hmcts.ccd.domain.model.std.validator.ValidationError;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Constraint(validatedBy = {SearchCriteriaValidator.class})
@Target({PARAMETER, FIELD})
@Retention(RUNTIME)
public @interface ValidSearchCriteria {

    String message() default ValidationError.GLOBAL_SEARCH_CRITERIA_INVALID;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};


}
