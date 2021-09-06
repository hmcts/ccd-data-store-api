package uk.gov.hmcts.ccd.endpoint.std.validator;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchSortByCategory;
import uk.gov.hmcts.ccd.domain.model.std.validator.SortByValidator;

import javax.validation.ConstraintValidatorContext;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SortByValidatorTest {

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    @Mock
    ConstraintValidatorContext.ConstraintViolationBuilder constraintViolationBuilder;

    private final SortByValidator validator = new SortByValidator();

    @Test
    void returnsTrueWhenSortByNull() {
        assertTrue(validator.isValid(null, constraintValidatorContext));
    }

    @Test
    void returnsTrueWhenSortByIsValid() {
        assertTrue(validator.isValid(GlobalSearchSortByCategory.CASE_NAME.getCategoryName(),
            constraintValidatorContext));
    }

    @Test
    void returnsFalseWhenSortByInvalid() {
        assertFalse(validator.isValid("invalid", constraintValidatorContext));
    }
}
