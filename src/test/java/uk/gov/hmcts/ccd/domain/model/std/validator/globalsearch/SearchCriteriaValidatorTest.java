package uk.gov.hmcts.ccd.domain.model.std.validator.globalsearch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.search.global.SearchCriteria;

import javax.validation.ConstraintValidatorContext;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class SearchCriteriaValidatorTest {

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    @Mock
    ConstraintValidatorContext.ConstraintViolationBuilder constraintViolationBuilder;

    private final SearchCriteriaValidator validator = new SearchCriteriaValidator();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(constraintValidatorContext.buildConstraintViolationWithTemplate(anyString())).thenReturn(
            constraintViolationBuilder);
    }

    @DisplayName("Returns `false` when SearchCriteria is null")
    @Test
    void returnsFalseWhenSearchCriteriaIsNull() {
        assertFalse(validator.isValid(null, constraintValidatorContext));
    }

    @DisplayName("Returns `false` when SearchCriteria is empty")
    @Test
    void returnsFalseWhenSearchCriteriaIsEmpty() {
        SearchCriteria criteria = new SearchCriteria();

        assertFalse(validator.isValid(criteria, constraintValidatorContext));
    }

    @DisplayName("Returns `false` when SearchCriteria field is null")
    @Test
    void returnsFalseWhenSearchCriteriaFieldIsNull() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setCaseReferences(null);

        assertFalse(validator.isValid(criteria, constraintValidatorContext));
    }

    @DisplayName("Returns `false` when SearchCriteria field is empty")
    @Test
    void returnsFalseWhenSearchCriteriaFieldIsEmpty() {
        SearchCriteria criteria = new SearchCriteria();
        List<String> emptyList = new ArrayList<>();
        criteria.setCaseManagementRegionIds(emptyList);

        assertFalse(validator.isValid(criteria, constraintValidatorContext));
    }

    @ParameterizedTest(name = "Returns `false` when Jurisdiction and CaseType criteria fields are null or empty: {0}")
    @NullAndEmptySource
    void returnsFalseWhenSearchCriteriaJurisdictionAndCaseTypeAreNullOrEmpty(List<String> values) {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setCcdJurisdictionIds(values);
        criteria.setCcdCaseTypeIds(values);
        criteria.setCaseReferences(List.of("123456"));

        assertFalse(validator.isValid(criteria, constraintValidatorContext));
    }

    @DisplayName("Returns `true` when CaseType criteria populated")
    @Test
    void returnsTrueWhenSearchCriteriaCaseTypePopulated() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setCcdCaseTypeIds(List.of("test-case-type"));

        assertTrue(validator.isValid(criteria, constraintValidatorContext));
    }

    @DisplayName("Returns `true` when Jurisdiction criteria populated")
    @Test
    void returnsTrueWhenSearchCriteriaJurisdictionPopulated() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setCcdJurisdictionIds(List.of("test-jurisdiction"));

        assertTrue(validator.isValid(criteria, constraintValidatorContext));
    }

}
