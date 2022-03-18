package uk.gov.hmcts.ccd.domain.model.std.validator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Constraint(validatedBy = {SortByValidator.class})
@Target({PARAMETER, FIELD})
@Retention(RUNTIME)
public @interface ValidSortBy {

    String message() default ValidationError.SORT_BY_INVALID;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};


}
