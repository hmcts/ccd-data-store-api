package uk.gov.hmcts.ccd.uk.gov.hmcts.ccd.validators;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.validators.UuIdValidator;

import javax.validation.ConstraintValidatorContext;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class UuIdValidatorTest extends ValidatorTest {

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    @InjectMocks
    private UuIdValidator uuIdValidatorValid;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        when(constraintValidatorContext.buildConstraintViolationWithTemplate(anyString()))
            .thenReturn(getConstraintViolationBuilder());
    }

    @Test
    @DisplayName("Should fail for null")
    void shouldFailForNullValues() {

        doTheTest(null, false, uuIdValidatorValid, constraintValidatorContext);
    }

    @Test
    @DisplayName("Should fail for not valid UuId")
    void shouldFailForNotValidUuId() {

        doTheTest("89au7ahubja89sd", false, uuIdValidatorValid, constraintValidatorContext);
    }

    @Test
    @DisplayName("Should pass for correct format.")
    void shouldPassForValidId() {

        doTheTest("da882fca-c5cb-48df-b787-cef2431ed051", true, uuIdValidatorValid, constraintValidatorContext);
    }

}
