package uk.gov.hmcts.ccd.domain.model.std.validator.globalsearch;

import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.domain.model.search.global.SearchCriteria;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class SearchCriteriaValidator implements ConstraintValidator<ValidSearchCriteria, SearchCriteria> {

    @Override
    public boolean isValid(final SearchCriteria criteria, final ConstraintValidatorContext context) {
        if (criteria != null) {
            // rule: At least one jurisdiction or case type must be provided in the search criteria
            return !CollectionUtils.isEmpty(criteria.getCcdJurisdictionIds())
                || !CollectionUtils.isEmpty(criteria.getCcdCaseTypeIds());
        }
        return false;
    }
}
