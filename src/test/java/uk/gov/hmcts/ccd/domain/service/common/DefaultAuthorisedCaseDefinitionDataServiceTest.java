package uk.gov.hmcts.ccd.domain.service.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseState;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

class DefaultAuthorisedCaseDefinitionDataServiceTest {

    @Mock
    private CaseTypeService caseTypeService;
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DefaultAuthorisedCaseDefinitionDataService authorisedCaseDataService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Nested
    @DisplayName("Get user authorised case states")
    class GetUserAuthorisedCaseStates {

        @Test
        @DisplayName("Should return list of user authorised case states for a jurisdiction and case type")
        void shouldReturnAuthorisedCaseStates() {
            CaseType caseType = mock(CaseType.class);
            String caseTypeId = "caseTypeId";
            String jurisdiction = "jusrisdiction";
            Set<String> userRoles = new HashSet<>();
            List<CaseState> caseStates = Collections.singletonList(new CaseState());
            when(caseTypeService.getCaseTypeForJurisdiction(caseTypeId, jurisdiction)).thenReturn(caseType);
            when(userRepository.getUserRoles()).thenReturn(userRoles);
            when(accessControlService.filterCaseStatesByAccess(caseType.getStates(), userRoles, CAN_READ)).thenReturn(caseStates);

            List<CaseState> result = authorisedCaseDataService.getUserAuthorisedCaseStates(jurisdiction, caseTypeId, CAN_READ);

            assertAll(
                () -> assertThat(result, is(caseStates)),
                () -> verify(caseTypeService).getCaseTypeForJurisdiction(caseTypeId, jurisdiction),
                () -> verify(userRepository).getUserRoles(),
                () -> verify(accessControlService).filterCaseStatesByAccess(caseType.getStates(), userRoles, CAN_READ)
            );
        }
    }
}
