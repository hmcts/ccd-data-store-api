package uk.gov.hmcts.ccd.domain.model.std.validator.globalsearch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchSortByCategory;

import javax.validation.ConstraintValidatorContext;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class SortByValidatorTest {

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    @Mock
    ConstraintValidatorContext.ConstraintViolationBuilder constraintViolationBuilder;

    private final SortByValidator validator = new SortByValidator();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(constraintValidatorContext.buildConstraintViolationWithTemplate(anyString())).thenReturn(
            constraintViolationBuilder);
    }

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
