package uk.gov.hmcts.ccd.endpoint.std.validator;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchSortDirection;
import uk.gov.hmcts.ccd.domain.model.std.validator.SortDirectionValidator;

import javax.validation.ConstraintValidatorContext;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SortDirectionValidatorTest {

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    @Mock
    ConstraintValidatorContext.ConstraintViolationBuilder constraintViolationBuilder;

    private final SortDirectionValidator validator = new SortDirectionValidator();

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
