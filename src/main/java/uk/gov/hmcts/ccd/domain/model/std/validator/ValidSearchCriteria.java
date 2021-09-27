package uk.gov.hmcts.ccd.domain.model.std.validator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Constraint(validatedBy = {SearchCriteriaValidator.class})
@Target({PARAMETER, FIELD})
@Retention(RUNTIME)
public @interface ValidSearchCriteria {


    String message() default ValidationError.SEARCH_CRITERIA_MISSING;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};


}
