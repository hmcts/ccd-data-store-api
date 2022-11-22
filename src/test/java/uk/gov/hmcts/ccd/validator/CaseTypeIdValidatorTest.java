package uk.gov.hmcts.ccd.validator;

import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.validation.ConstraintValidatorContext;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CaseTypeIdValidatorTest {

    private static final String CASE_TYPE_ID_INVALID_CHARACTER = "TEST#_CASE_TYPE_ID";
    private static final String CASE_TYPE_ID_VALID_UPPER_CASE = "TEST_CASE_TYPE_ID";
    private static final String CASE_TYPE_ID_VALID_LOWER_CASE = "test_case_type_id";
    private static final String CASE_TYPE_ID_VALID_NUMBERS = "TEST_CASE_TYPE_ID_1234567890";
    private static final String CASE_TYPE_ID_VALID_HYPHENS = "TEST-CASE-TYPE-ID";
    private static final String CASE_TYPE_ID_VALID_ALL = "TEST_case-TYPE_1_ID";

    private final CaseTypeIdValidator caseTypeIdValidator = new CaseTypeIdValidator();

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    private AutoCloseable autoCloseable;

    @BeforeEach
    public void openMocks() {
        autoCloseable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    public void releaseMocks() throws Exception {
        autoCloseable.close();
    }

    @Test
    public void failForNullCaseTypeId() {
        final boolean result = caseTypeIdValidator.isValid(null, constraintValidatorContext);
        assertFalse("Null Case Type Id should not be valid", result);
    }

    @Test
    public void failForBlankCaseTypeId() {
        final boolean result = caseTypeIdValidator.isValid("", constraintValidatorContext);
        assertFalse("Blank Case Type Id should not be valid", result);
    }

    @Test
    public void failForInvalidCharacter() {
        final boolean result = caseTypeIdValidator.isValid(CASE_TYPE_ID_INVALID_CHARACTER, constraintValidatorContext);
        assertFalse("Case Type Id " + CASE_TYPE_ID_INVALID_CHARACTER + " should not be valid", result);
    }

    @Test
    public void passForValidCaseTypeIdUpperCase() {
        final boolean result = caseTypeIdValidator.isValid(CASE_TYPE_ID_VALID_UPPER_CASE, constraintValidatorContext);
        assertTrue("Case Type Id " + CASE_TYPE_ID_VALID_UPPER_CASE + " should be valid", result);
    }

    @Test
    public void passForValidCaseTypeIdLowerCase() {
        final boolean result = caseTypeIdValidator.isValid(CASE_TYPE_ID_VALID_LOWER_CASE, constraintValidatorContext);
        assertTrue("Case Type Id " + CASE_TYPE_ID_VALID_LOWER_CASE + " should be valid", result);
    }

    @Test
    public void passForValidCaseTypeIdNumbers() {
        final boolean result = caseTypeIdValidator.isValid(CASE_TYPE_ID_VALID_NUMBERS, constraintValidatorContext);
        assertTrue("Case Type Id " + CASE_TYPE_ID_VALID_NUMBERS + " should be valid", result);
    }

    @Test
    public void passForValidCaseTypeIdHyphens() {
        final boolean result = caseTypeIdValidator.isValid(CASE_TYPE_ID_VALID_HYPHENS, constraintValidatorContext);
        assertTrue("Case Type Id " + CASE_TYPE_ID_VALID_HYPHENS + " should be valid", result);
    }

    @Test
    public void passForValidCaseTypeIdAll() {
        final boolean result = caseTypeIdValidator.isValid(CASE_TYPE_ID_VALID_ALL, constraintValidatorContext);
        assertTrue("Case Type Id " + CASE_TYPE_ID_VALID_ALL + " should be valid", result);
    }
}
