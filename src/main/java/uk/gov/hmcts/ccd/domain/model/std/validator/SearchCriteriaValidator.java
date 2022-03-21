package uk.gov.hmcts.ccd.domain.model.std.validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.ccd.domain.model.search.global.SearchCriteria;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class SearchCriteriaValidator implements ConstraintValidator<ValidSearchCriteria, SearchCriteria> {

    private static final Logger LOG = LoggerFactory.getLogger(SearchCriteriaValidator.class);

    @Override
    public boolean isValid(final SearchCriteria criteria, final ConstraintValidatorContext context) {
        if (criteria != null) {
            try {
                return criteria.getNonNullFields();
            } catch (IllegalAccessException e) {
                LOG.error(e.getMessage());
            }
        }
        return false;
    }
}
