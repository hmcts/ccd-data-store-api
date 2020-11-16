package uk.gov.hmcts.ccd.domain.service.common;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.enablingcondition.PrioritiseEnablingCondition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.EventPostStateDefinition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doReturn;

class CasePostStateServiceTest extends BaseStateReferenceTest {

    @Mock
    private CasePostStateEvaluationService casePostStateEvaluationService;

    private CasePostStateService casePostStateService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        casePostStateService = new CasePostStateService(new PrioritiseEnablingCondition(),
            this.casePostStateEvaluationService);
    }

    @Test
    void shouldNotUpdateCaseStateWhenPostStateConditionHasSpecialCharStar() {
        CaseEventDefinition caseEventDefinition = mockCaseEventDefinition();
        doReturn(new ArrayList<>()).when(caseEventDefinition).getPostStates();
        CaseDetails caseDetails = mockCaseDetails();
        doReturn("*").when(this.casePostStateEvaluationService)
            .evaluatePostStateCondition(anyList(), anyMap());
        String postState = this.casePostStateService.evaluateCaseState(caseEventDefinition, caseDetails);
        assertNotNull(postState);
        assertEquals("*", postState);
    }

    @Test
    void shouldUpdateCaseStateWhenPostStateConditionHasValidReference() {
        CaseEventDefinition caseEventDefinition = mockCaseEventDefinition();
        doReturn(new ArrayList<>()).when(caseEventDefinition).getPostStates();
        CaseDetails caseDetails = new CaseDetails();
        doReturn("TestReference").when(this.casePostStateEvaluationService)
            .evaluatePostStateCondition(anyList(), anyMap());
        String postState = this.casePostStateService.evaluateCaseState(caseEventDefinition, caseDetails);
        assertEquals("TestReference", postState);
    }

    @Test
    void shouldUpdateCaseStateWhenPostStatesExists() {
        CaseEventDefinition caseEventDefinition = mockCaseEventDefinition();
        List<EventPostStateDefinition> postStates = createPostStates();
        doReturn(postStates).when(caseEventDefinition).getPostStates();
        CaseDetails caseDetails = new CaseDetails();
        doReturn("Test125").when(this.casePostStateEvaluationService)
            .evaluatePostStateCondition(anyList(), anyMap());
        String postState = this.casePostStateService.evaluateCaseState(caseEventDefinition, caseDetails);
        assertEquals("Test125", postState);
    }

    private CaseDetails mockCaseDetails() {
        CaseDetails caseDetails = Mockito.mock(CaseDetails.class);
        return caseDetails;
    }

    private CaseEventDefinition mockCaseEventDefinition() {
        CaseEventDefinition caseEventDefinition = Mockito.mock(CaseEventDefinition.class);
        return caseEventDefinition;
    }
}
