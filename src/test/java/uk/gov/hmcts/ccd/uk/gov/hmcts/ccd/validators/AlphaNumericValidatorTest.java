package uk.gov.hmcts.ccd.uk.gov.hmcts.ccd.validators;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.validators.AlphaNumericValidator;

import javax.validation.ConstraintValidatorContext;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@DisplayName("AlphaNumericValidatorTest")
public class AlphaNumericValidatorTest extends ValidatorTest {

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    @InjectMocks
    private AlphaNumericValidator alphaNumericValidator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        when(constraintValidatorContext.buildConstraintViolationWithTemplate(anyString()))
            .thenReturn(getConstraintViolationBuilder());
    }

    @Test
    @DisplayName("Should fail for null")
    void shouldFailForNullValues() {

        doTheTest(null, false, alphaNumericValidator, constraintValidatorContext);
    }

    @Test
    @DisplayName("Should fail for non valid characters")
    void shouldFailForNotValidCharacters() {

        doTheTest("TY^^^^^****", false, alphaNumericValidator, constraintValidatorContext);
    }

    @Test
    @DisplayName("Should fail for spaces")
    void shouldFailForSpaces() {

        doTheTest("TE TE TE ", false, alphaNumericValidator, constraintValidatorContext);
    }

    @Test
    @DisplayName("Should pass for alpha numeric")
    void shouldPassForNumbers() {

        doTheTest("TE1", true, alphaNumericValidator, constraintValidatorContext);
    }

    @Test
    @DisplayName("Should pass for correct format.")
    void shouldPassForValidId() {

        doTheTest("TEST", true, alphaNumericValidator, constraintValidatorContext);
    }
}
