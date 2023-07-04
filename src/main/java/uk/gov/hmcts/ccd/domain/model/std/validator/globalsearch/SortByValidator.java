package uk.gov.hmcts.ccd.domain.model.std.validator.globalsearch;

import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchSortByCategory;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class SortByValidator implements ConstraintValidator<ValidSortBy, String> {

    @Override
    public boolean isValid(final String sortBy, final ConstraintValidatorContext context) {

        if (sortBy == null) {
            return true;
        }

        for (GlobalSearchSortByCategory value : GlobalSearchSortByCategory.values()) {
            if (sortBy.equalsIgnoreCase(value.getCategoryName())) {
                return true;
            }
        }
        return false;
    }

}
