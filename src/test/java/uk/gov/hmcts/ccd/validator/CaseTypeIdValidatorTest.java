package uk.gov.hmcts.ccd.validator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import jakarta.validation.ConstraintValidatorContext;

import static org.assertj.core.api.Assertions.assertThat;

public class CaseTypeIdValidatorTest {

    private static final String CASE_TYPE_ID_INVALID_CHARACTER = "TEST#_CASE_TYPE_ID";
    private static final String CASE_TYPE_ID_VALID_UPPER_CASE = "TEST_CASE_TYPE_ID";
    private static final String CASE_TYPE_ID_VALID_LOWER_CASE = "test_case_type_id";
    private static final String CASE_TYPE_ID_VALID_NUMBERS = "TEST_CASE_TYPE_ID_1234567890";
    private static final String CASE_TYPE_ID_VALID_HYPHENS = "TEST-CASE-TYPE-ID";
    private static final String CASE_TYPE_ID_VALID_ALL = "TEST_case-TYPE_1_ID";
    private static final String CASE_TYPE_ID_INVALID_NON_ASCII = "é_CASE_TYPE_ID";

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
        assertThat(result).isFalse();
    }

    @Test
    public void failForBlankCaseTypeId() {
        final boolean result = caseTypeIdValidator.isValid("", constraintValidatorContext);
        assertThat(result).isFalse();
    }

    @Test
    public void failForInvalidCharacter() {
        final boolean result = caseTypeIdValidator.isValid(CASE_TYPE_ID_INVALID_CHARACTER, constraintValidatorContext);
        assertThat(result).isFalse();
    }

    @Test
    public void failForNonAsciiLetter() {
        final boolean result = caseTypeIdValidator.isValid(CASE_TYPE_ID_INVALID_NON_ASCII, constraintValidatorContext);
        assertThat(result).isFalse();
    }

    @Test
    public void passForValidCaseTypeIdUpperCase() {
        final boolean result = caseTypeIdValidator.isValid(CASE_TYPE_ID_VALID_UPPER_CASE, constraintValidatorContext);
        assertThat(result).isTrue();
    }

    @Test
    public void passForValidCaseTypeIdLowerCase() {
        final boolean result = caseTypeIdValidator.isValid(CASE_TYPE_ID_VALID_LOWER_CASE, constraintValidatorContext);
        assertThat(result).isTrue();
    }

    @Test
    public void passForValidCaseTypeIdNumbers() {
        final boolean result = caseTypeIdValidator.isValid(CASE_TYPE_ID_VALID_NUMBERS, constraintValidatorContext);
        assertThat(result).isTrue();
    }

    @Test
    public void passForValidCaseTypeIdHyphens() {
        final boolean result = caseTypeIdValidator.isValid(CASE_TYPE_ID_VALID_HYPHENS, constraintValidatorContext);
        assertThat(result).isTrue();
    }

    @Test
    public void passForValidCaseTypeIdAll() {
        final boolean result = caseTypeIdValidator.isValid(CASE_TYPE_ID_VALID_ALL, constraintValidatorContext);
        assertThat(result).isTrue();
    }
}
