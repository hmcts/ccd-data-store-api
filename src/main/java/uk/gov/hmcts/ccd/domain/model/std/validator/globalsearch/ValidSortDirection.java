package uk.gov.hmcts.ccd.domain.model.std.validator.globalsearch;

import uk.gov.hmcts.ccd.domain.model.std.validator.ValidationError;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Constraint(validatedBy = {SortDirectionValidator.class})
@Target({PARAMETER, FIELD})
@Retention(RUNTIME)
public @interface ValidSortDirection {

    String message() default ValidationError.SORT_DIRECTION_INVALID;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
