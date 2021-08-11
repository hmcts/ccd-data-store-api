package uk.gov.hmcts.ccd.domain.model.std.validator;

import uk.gov.hmcts.ccd.domain.model.std.GlobalSearchSortDirection;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class SortDirectionValidator implements ConstraintValidator<ValidSortDirection, String> {

    @Override
    public boolean isValid(final String sortDirection, final ConstraintValidatorContext context) {

        if (sortDirection == null) {
            return true;
        }

        for (GlobalSearchSortDirection value : GlobalSearchSortDirection.values()) {
            if (sortDirection.equalsIgnoreCase(value.toString())) {
                return true;
            }
        }
        return false;
    }

}
