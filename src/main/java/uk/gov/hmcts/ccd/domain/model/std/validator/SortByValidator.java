package uk.gov.hmcts.ccd.domain.model.std.validator;

import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchSortCategory;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class SortByValidator implements ConstraintValidator<ValidSortBy, String> {

    @Override
    public boolean isValid(final String sortBy, final ConstraintValidatorContext context) {

        if (sortBy == null) {
            return true;
        }

        for (GlobalSearchSortCategory value : GlobalSearchSortCategory.values()) {
            if (sortBy.equalsIgnoreCase(value.getCategoryName())) {
                return true;
            }
        }
        return false;
    }

}
