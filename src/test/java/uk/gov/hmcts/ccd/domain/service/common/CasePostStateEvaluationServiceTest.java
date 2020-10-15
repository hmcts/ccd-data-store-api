package uk.gov.hmcts.ccd.domain.service.common;

import java.util.HashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.enablingcondition.EnablingConditionParser;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

class CasePostStateEvaluationServiceTest extends BaseStateReferenceTest {

    private CasePostStateEvaluationService casePostStateEvaluationService;

    @Mock
    private EnablingConditionParser enablingConditionParser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        this.casePostStateEvaluationService = new CasePostStateEvaluationService(this.enablingConditionParser);
    }

    @Test
    void shouldReturnDefaultStateReferenceWhenNoPostStateConditionIsValid() {
        String postStateReference = this.casePostStateEvaluationService
            .evaluatePostStateCondition(createPostStates(), new HashMap<>());
        assertEquals(false, postStateReference.isEmpty());
        assertEquals("Test125", postStateReference);
    }

    @Test
    void shouldReturnStateReferenceWhenPostStateConditionIsValid() {
        doReturn(true).when(this.enablingConditionParser)
            .evaluate(anyString(), anyMap());
        String postStateReference = this.casePostStateEvaluationService
            .evaluatePostStateCondition(createPostStates(), new HashMap<>());
        assertEquals(false, postStateReference.isEmpty());
        assertEquals("Test123", postStateReference);
    }

    @Test
    void shouldReturnDefaultStateReferenceWhenPostStateConditionsAreNotValid() {
        doReturn(false).when(this.enablingConditionParser)
            .evaluate(anyString(), anyMap());
        String postStateReference = this.casePostStateEvaluationService
            .evaluatePostStateCondition(createPostStates(), new HashMap<>());
        assertEquals(false, postStateReference.isEmpty());
        assertEquals("Test125", postStateReference);
    }

    @Test
    void shouldThrowExceptionWhenDefaultStateReferenceNotFound() {
        doReturn(false).when(this.enablingConditionParser)
            .evaluate(anyString(), anyMap());
        ServiceException serviceException = assertThrows(ServiceException.class,
            () -> this.casePostStateEvaluationService
            .evaluatePostStateCondition(createEventPostStates(), new HashMap<>()));
        assertNotNull(serviceException);
    }
}
