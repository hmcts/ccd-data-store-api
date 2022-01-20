package uk.gov.hmcts.ccd.endpoint.std.validator;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import uk.gov.hmcts.ccd.domain.model.search.global.Party;
import uk.gov.hmcts.ccd.domain.model.search.global.SearchCriteria;
import uk.gov.hmcts.ccd.domain.model.std.validator.SearchCriteriaValidator;

import javax.validation.ConstraintValidatorContext;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

class SearchCriteriaValidatorTest {
    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    @Mock
    ConstraintValidatorContext.ConstraintViolationBuilder constraintViolationBuilder;

    private final SearchCriteriaValidator validator = new SearchCriteriaValidator();

    @Test
    void returnsTrueWhenSearchCriteriaHasOneValidField() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setCaseReferences(null);
        List<String> emptyList = new ArrayList<>();
        criteria.setCaseManagementRegionIds(emptyList);
        List<Party> parties = new ArrayList<>();
        parties.add(new Party());
        Party party = new Party();
        party.setPartyName("name");
        criteria.setParties(parties);
        assertFalse(validator.isValid(criteria, constraintValidatorContext));
    }

    @Test
    void returnsFalseWhenSearchCriteriaIsNull() {
        assertFalse(validator.isValid(null, constraintValidatorContext));
    }

    @Test
    void returnsFalseWhenSearchCriteriaIsEmpty() {
        SearchCriteria criteria = new SearchCriteria();
        assertFalse(validator.isValid(criteria, constraintValidatorContext));
    }

    @Test
    void returnsFalseWhenSearchCriteriaFieldIsNull() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setCaseReferences(null);
        assertFalse(validator.isValid(criteria, constraintValidatorContext));
    }

    @Test
    void returnsFalseWhenSearchCriteriaFieldIsEmpty() {
        SearchCriteria criteria = new SearchCriteria();
        List<String> emptyList = new ArrayList<>();
        criteria.setCaseManagementRegionIds(emptyList);
        assertFalse(validator.isValid(criteria, constraintValidatorContext));
    }

    @Test
    void returnsFalseWhenSearchCriteriaPartiesIsEmpty() {
        SearchCriteria criteria = new SearchCriteria();
        List<Party> parties = new ArrayList<>();
        parties.add(new Party());
        criteria.setParties(parties);
        assertFalse(validator.isValid(criteria, constraintValidatorContext));
    }
}
