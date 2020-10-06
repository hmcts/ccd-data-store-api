package uk.gov.hmcts.ccd.domain.service.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.casestate.EnablingConditionSorter;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.EventPostStateDefinition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doReturn;

class CaseStateUpdateServiceTest extends BaseStateReferenceTest {

    @Mock
    private StateReferenceService stateReferenceService;

    private CaseStateUpdateService caseStateUpdateService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        caseStateUpdateService = new CaseStateUpdateService(new EnablingConditionSorter(),
            this.stateReferenceService);
    }

    @Test
    void shouldNotUpdateCaseStateWhenPostStateConditionsAreEmpty() {
        CaseEventDefinition caseEventDefinition = mockCaseEventDefinition();
        doReturn(new ArrayList<>()).when(caseEventDefinition).getPostStates();
        CaseDetails caseDetails = mockCaseDetails();
        Optional<String> postState = this.caseStateUpdateService.retrieveCaseState(caseEventDefinition, caseDetails);
        assertTrue(postState.isEmpty());
    }

    @Test
    void shouldNotUpdateCaseStateWhenPostStateConditionHasSpecialCharStar() {
        CaseEventDefinition caseEventDefinition = mockCaseEventDefinition();
        doReturn(new ArrayList<>()).when(caseEventDefinition).getPostStates();
        CaseDetails caseDetails = mockCaseDetails();
        doReturn(Optional.of("*")).when(this.stateReferenceService).evaluatePostStateCondition(anyList(), anyMap());
        Optional<String> postState = this.caseStateUpdateService.retrieveCaseState(caseEventDefinition, caseDetails);
        assertTrue(postState.isPresent());
        assertNotNull(postState.get());
        assertEquals("*", postState.get());
    }

    @Test
    void shouldUpdateCaseStateWhenPostStateConditionHasValidReference() {
        CaseEventDefinition caseEventDefinition = mockCaseEventDefinition();
        doReturn(new ArrayList<>()).when(caseEventDefinition).getPostStates();
        CaseDetails caseDetails = new CaseDetails();
        doReturn(Optional.of("TestReference")).when(this.stateReferenceService)
            .evaluatePostStateCondition(anyList(), anyMap());
        Optional<String> postState = this.caseStateUpdateService.retrieveCaseState(caseEventDefinition, caseDetails);
        assertEquals("TestReference", postState.get());
    }

    @Test
    void shouldUpdateCaseStateWhenPostStatesExists() {
        CaseEventDefinition caseEventDefinition = mockCaseEventDefinition();
        List<EventPostStateDefinition> postStates = createPostStates();
        doReturn(postStates).when(caseEventDefinition).getPostStates();
        CaseDetails caseDetails = new CaseDetails();
        doReturn(Optional.of("Test125")).when(this.stateReferenceService)
            .evaluatePostStateCondition(anyList(), anyMap());
        Optional<String> postState = this.caseStateUpdateService.retrieveCaseState(caseEventDefinition, caseDetails);
        assertEquals("Test125", postState.get());
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
