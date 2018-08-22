package uk.gov.hmcts.ccd.domain.service.aggregated;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseHistoryView;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewEvent;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEvent;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.valueOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseEventBuilder.anCaseEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseHistoryViewBuilder.aCaseHistoryView;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeBuilder.newCaseType;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseViewEventBuilder.aCaseViewEvent;

class AuthorisedGetCaseHistoryViewOperationTest {
    private static final String JURISDICTION_ID = "Probate";
    private static final String CASE_TYPE_ID = "Grant";
    private static final String CASE_REFERENCE = "1111222233334444";
    private static final Long EVENT_ID = 100L;
    private static final String ROLE_IN_USER_ROLES = "caseworker-probate-loa1";
    private static final String ROLE_IN_USER_ROLES_2 = "caseworker-divorce-loa";
    private static final Set<String> USER_ROLES = newHashSet(ROLE_IN_USER_ROLES, ROLE_IN_USER_ROLES_2);
    private static final String EVENT_ID_STRING = valueOf(EVENT_ID);
    private static final CaseEvent CASE_EVENT = anCaseEvent().withId(EVENT_ID_STRING).build();
    private static final CaseEvent CASE_EVENT_2 = anCaseEvent().withId("event2").build();
    private static final CaseType TEST_CASE_TYPE = newCaseType().withEvent(CASE_EVENT).withEvent(CASE_EVENT_2).build();
    private static final CaseViewEvent CASE_VIEW_EVENT = aCaseViewEvent().withId(EVENT_ID_STRING).build();
    private static final CaseHistoryView TEST_CASE_HISTORY_VIEW = aCaseHistoryView().withEvent(CASE_VIEW_EVENT).build();

    @Mock
    private GetCaseHistoryViewOperation getCaseHistoryViewOperation;
    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private UserRepository userRepository;

    private AuthorisedGetCaseHistoryViewOperation authorisedGetCaseHistoryViewOperation;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        doReturn(TEST_CASE_TYPE).when(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);
        doReturn(USER_ROLES).when(userRepository).getUserRoles();
        doReturn(TEST_CASE_HISTORY_VIEW).when(getCaseHistoryViewOperation).execute(JURISDICTION_ID, CASE_TYPE_ID,
            CASE_REFERENCE, EVENT_ID);

        authorisedGetCaseHistoryViewOperation = new AuthorisedGetCaseHistoryViewOperation(getCaseHistoryViewOperation,
            caseDefinitionRepository, accessControlService, userRepository);
    }

    @Test
    @DisplayName("should return case history view")
    void shouldReturnCaseHistoryView() {
        doReturn(true).when(accessControlService).canAccessCaseTypeWithCriteria(TEST_CASE_TYPE, USER_ROLES, CAN_READ);

        CaseHistoryView caseHistoryView = authorisedGetCaseHistoryViewOperation.execute(JURISDICTION_ID, CASE_TYPE_ID,
            CASE_REFERENCE, EVENT_ID);

        assertThat(caseHistoryView, CoreMatchers.is(TEST_CASE_HISTORY_VIEW));
        verify(getCaseHistoryViewOperation).execute(JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE, EVENT_ID);
        verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);
        verify(userRepository).getUserRoles();
        verify(accessControlService).canAccessCaseTypeWithCriteria(TEST_CASE_TYPE, USER_ROLES, CAN_READ);
    }

    @Test
    @DisplayName("should throw exception when case type not found")
    void shouldThrowExceptionWhenCaseTypeNotFound() {
        doReturn(null).when(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);

        assertThrows(ValidationException.class,
            () -> authorisedGetCaseHistoryViewOperation.execute(JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE,
                EVENT_ID));
    }

}
