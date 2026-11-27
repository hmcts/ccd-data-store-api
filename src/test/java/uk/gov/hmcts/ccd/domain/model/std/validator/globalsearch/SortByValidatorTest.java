package uk.gov.hmcts.ccd.domain.model.std.validator.globalsearch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchSortByCategory;

import jakarta.validation.ConstraintValidatorContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class SortByValidatorTest {

    private final SortByValidator validator = new SortByValidator();
    @Mock
    ConstraintValidatorContext.ConstraintViolationBuilder constraintViolationBuilder;
    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(constraintValidatorContext.buildConstraintViolationWithTemplate(anyString())).thenReturn(
            constraintViolationBuilder);
    }

    @Test
    void returnsTrueWhenSortByNull() {
        assertThat(validator.isValid(null, constraintValidatorContext)).isTrue();
    }

    @Test
    void returnsTrueWhenSortByIsValid() {
        assertThat(validator.isValid(GlobalSearchSortByCategory.CASE_NAME.getCategoryName(),
            constraintValidatorContext)).isTrue();
        assertThat(validator.isValid(GlobalSearchSortByCategory.NEXT_HEARING_DATE.getCategoryName(),
            constraintValidatorContext)).isTrue();
    }

    @Test
    void returnsFalseWhenSortByInvalid() {
        assertThat(validator.isValid("invalid", constraintValidatorContext)).isFalse();
    }
}
