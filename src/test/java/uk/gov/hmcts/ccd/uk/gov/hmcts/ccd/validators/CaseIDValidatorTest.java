package uk.gov.hmcts.ccd.uk.gov.hmcts.ccd.validators;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.validators.CaseIDValidator;

import javax.validation.ConstraintValidatorContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@DisplayName("CaseIDValidatorTest")
public class CaseIDValidatorTest extends ValidatorTest {

    @Mock
    private UIDService uidService;

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    @InjectMocks
    private CaseIDValidator caseIDValidator;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        when(constraintValidatorContext.buildConstraintViolationWithTemplate(anyString()))
            .thenReturn(getConstraintViolationBuilder());
    }

    @Test
    @DisplayName("Should fail for null")
    void shouldFailForNullValues() {

        doTheTest(null, false, caseIDValidator, constraintValidatorContext);
    }

    @Test
    @DisplayName("Should fail format.")
    void shouldFailForCheckSum() {

        when(uidService.validateUID(anyString()))
            .thenReturn(false);
        final boolean result = caseIDValidator.isValid("157831A1501829932", constraintValidatorContext);
        assertThat(result, is(false));
    }

    @Test
    @DisplayName("Should pass for correct format.")
    void shouldPassForValidId() {

        when(uidService.validateUID(anyString()))
            .thenReturn(true);
        final boolean result = caseIDValidator.isValid("1578311501829932", constraintValidatorContext);
        assertThat(result, is(true));
    }
}
