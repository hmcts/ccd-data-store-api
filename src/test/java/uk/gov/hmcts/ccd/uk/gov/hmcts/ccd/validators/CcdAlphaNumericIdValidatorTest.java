package uk.gov.hmcts.ccd.uk.gov.hmcts.ccd.validators;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.validators.CcdAlphaNumericIdValidator;

import javax.validation.ConstraintValidatorContext;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@DisplayName("CcdAlphaNumericIdValidatorTest")
public class CcdAlphaNumericIdValidatorTest extends ValidatorTest {

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    @InjectMocks
    private CcdAlphaNumericIdValidator ccdAlphaNumericIdValidator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        when(constraintValidatorContext.buildConstraintViolationWithTemplate(anyString()))
            .thenReturn(getConstraintViolationBuilder());
    }

    @Test
    @DisplayName("Should fail for null")
    void shouldFailForNullValues() {

        doTheTest(null, false, ccdAlphaNumericIdValidator, constraintValidatorContext);
    }

    @Test
    @DisplayName("Should fail for non valid characters")
    void shouldFailForNotValidCharacters() {

        doTheTest("TY^^^^^****", false, ccdAlphaNumericIdValidator, constraintValidatorContext);
    }

    @Test
    @DisplayName("Should fail for spaces")
    void shouldFailForSpaces() {

        doTheTest("TE TE TE ", false, ccdAlphaNumericIdValidator, constraintValidatorContext);
    }

    @Test
    @DisplayName("Should pass for alpha numeric")
    void shouldPassForNumbers() {

        doTheTest("TE1", true, ccdAlphaNumericIdValidator, constraintValidatorContext);
    }

    @Test
    @DisplayName("Should pass for correct format.")
    void shouldPassForValidId() {

        doTheTest("TEST", true, ccdAlphaNumericIdValidator, constraintValidatorContext);
    }
}
