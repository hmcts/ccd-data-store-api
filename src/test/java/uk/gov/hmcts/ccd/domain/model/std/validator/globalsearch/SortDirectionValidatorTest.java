package uk.gov.hmcts.ccd.domain.model.std.validator.globalsearch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchSortDirection;

import javax.validation.ConstraintValidatorContext;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class SortDirectionValidatorTest {

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    @Mock
    ConstraintValidatorContext.ConstraintViolationBuilder constraintViolationBuilder;

    private final SortDirectionValidator validator = new SortDirectionValidator();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(constraintValidatorContext.buildConstraintViolationWithTemplate(anyString())).thenReturn(
            constraintViolationBuilder);
    }

    @Test
    void returnsTrueWhenSortDirectionNull() {
        assertTrue(validator.isValid(null, constraintValidatorContext));
    }

    @Test
    void returnsTrueWhenSortDirectionIsValid() {
        assertTrue(validator.isValid(GlobalSearchSortDirection.ASCENDING.name(), constraintValidatorContext));
    }

    @Test
    void returnsFalseWhenSortDirectionInvalid() {
        assertFalse(validator.isValid("invalid", constraintValidatorContext));
    }
}
