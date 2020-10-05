package uk.gov.hmcts.ccd.domain.service.common;

import java.util.HashMap;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.casestate.EnablingConditionParser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

class StateReferenceServiceTest extends BaseStateReferenceTest {

    private StateReferenceService stateReferenceService;

    @Mock
    private EnablingConditionParser enablingConditionParser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        this.stateReferenceService = new StateReferenceService(this.enablingConditionParser);
    }

    @Test
    void shouldReturnEmptyReferenceWhenNullPostStateReferenceIsPassed() {
        Optional<String> postStateReference = this.stateReferenceService
            .evaluatePostStateCondition(null, new HashMap<>());
        assertEquals(true, postStateReference.isEmpty());
    }

    @Test
    void shouldReturnDefaultStateReferenceWhenNoPostStateConditionIsValid() {
        Optional<String> postStateReference = this.stateReferenceService
            .evaluatePostStateCondition(createPostStates(), new HashMap<>());
        assertEquals(false, postStateReference.isEmpty());
        assertEquals("Test125", postStateReference.get());
    }

    @Test
    void shouldReturnStateReferenceWhenPostStateConditionIsValid() {
        doReturn(true).when(this.enablingConditionParser)
            .evaluate(anyString(), anyMap());
        Optional<String> postStateReference = this.stateReferenceService
            .evaluatePostStateCondition(createPostStates(), new HashMap<>());
        assertEquals(false, postStateReference.isEmpty());
        assertEquals("Test123", postStateReference.get());
    }

    @Test
    void shouldReturnDefaultStateReferenceWhenPostStateConditionsAreNotValid() {
        doReturn(false).when(this.enablingConditionParser)
            .evaluate(anyString(), anyMap());
        Optional<String> postStateReference = this.stateReferenceService
            .evaluatePostStateCondition(createPostStates(), new HashMap<>());
        assertEquals(false, postStateReference.isEmpty());
        assertEquals("Test125", postStateReference.get());
    }
}
