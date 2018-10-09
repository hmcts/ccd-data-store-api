package uk.gov.hmcts.ccd.domain.service.aggregated;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseView;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewTrigger;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewType;
import uk.gov.hmcts.ccd.domain.model.aggregated.ProfileCaseState;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEvent;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.valueOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_UPDATE;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseEventBuilder.anCaseEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeBuilder.newCaseType;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseViewBuilder.aCaseView;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseViewTriggerBuilder.aViewTrigger;

class AuthorisedGetCaseViewOperationTest {
    private static final String JURISDICTION_ID = "Probate";
    private static final String CASE_TYPE_ID = "Grant";
    private static final String CASE_REFERENCE = "1111222233334444";
    private static final Long EVENT_ID = 100L;
    private static final String STATE = "Plop";
    private static final ProfileCaseState caseState = new ProfileCaseState(STATE, STATE, STATE, STATE);

    private static final String ROLE_IN_USER_ROLES = "caseworker-probate-loa1";
    private static final String ROLE_IN_USER_ROLES_2 = "caseworker-divorce-loa";
    private static final Set<String> USER_ROLES = newHashSet(ROLE_IN_USER_ROLES, ROLE_IN_USER_ROLES_2);
    private static final String EVENT_ID_STRING = valueOf(EVENT_ID);
    private static final CaseViewTrigger[] EMPTY_TRIGGERS = new CaseViewTrigger[]{};
    private static final CaseEvent CASE_EVENT = anCaseEvent().withId(EVENT_ID_STRING).build();
    private static final CaseEvent CASE_EVENT_2 = anCaseEvent().withId("event2").build();
    private static final CaseViewTrigger CASE_VIEW_TRIGGER = aViewTrigger().withId(EVENT_ID_STRING).build();
    private static final CaseViewTrigger CASE_VIEW_TRIGGER_2 = aViewTrigger().withId("event2").build();
    private static final CaseViewTrigger[] AUTH_CASE_VIEW_TRIGGERS = new CaseViewTrigger[] {CASE_VIEW_TRIGGER};
    private static final CaseType TEST_CASE_TYPE = newCaseType().withEvent(CASE_EVENT).withEvent(CASE_EVENT_2).build();
    private static final CaseView TEST_CASE_VIEW = aCaseView().withState(caseState).withCaseViewTrigger(CASE_VIEW_TRIGGER).withCaseViewTrigger(CASE_VIEW_TRIGGER_2).build();

    @Mock
    private GetCaseViewOperation getCaseViewOperation;
    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private UserRepository userRepository;

    @Spy
    @InjectMocks
    private AuthorisedGetCaseViewOperation authorisedGetCaseViewOperation;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        doReturn(TEST_CASE_TYPE).when(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);
        doReturn(USER_ROLES).when(userRepository).getUserRoles();

        final CaseViewType caseViewType = new CaseViewType();
        caseViewType.setId(CASE_TYPE_ID);
        TEST_CASE_VIEW.setCaseType(caseViewType);

        doReturn(TEST_CASE_VIEW).when(getCaseViewOperation).execute(CASE_REFERENCE);
    }

    @Test
    @DisplayName("should call not-deprecated #execute(caseReference)")
    void shouldCallNotDeprecatedExecute() {
        final CaseView expectedCaseView = new CaseView();
        doReturn(expectedCaseView).when(authorisedGetCaseViewOperation).execute(CASE_REFERENCE);

        final CaseView actualCaseView = authorisedGetCaseViewOperation.execute(JURISDICTION_ID,
                                                                               CASE_TYPE_ID,
                                                                               CASE_REFERENCE);

        assertAll(
            () -> verify(authorisedGetCaseViewOperation).execute(CASE_REFERENCE),
            () -> assertThat(actualCaseView, sameInstance(expectedCaseView))
        );
    }

    @Test
    @DisplayName("should fail when no READ access type on case type")
    void shouldFailWhenWhenNoReadAccess() {
        doReturn(false).when(accessControlService).canAccessCaseTypeWithCriteria(TEST_CASE_TYPE, USER_ROLES, CAN_READ);

        assertThrows(ResourceNotFoundException.class, () -> authorisedGetCaseViewOperation.execute(CASE_REFERENCE));
    }

    @Test
    @DisplayName("should remove all case view triggers when no UPDATE access type on case type")
    void shouldRemoveCaseViewTriggersWhenNoUpdateAccessForCaseType() {
        doReturn(true).when(accessControlService).canAccessCaseTypeWithCriteria(TEST_CASE_TYPE, USER_ROLES, CAN_READ);
        doReturn(false).when(accessControlService).canAccessCaseTypeWithCriteria(TEST_CASE_TYPE, USER_ROLES, CAN_UPDATE);

        CaseView caseView = authorisedGetCaseViewOperation.execute(CASE_REFERENCE);

        assertThat(caseView.getTriggers(), arrayWithSize(0));
    }

    @Test
    @DisplayName("should remove all case view triggers when no UPDATE access type on case state")
    void shouldRemoveCaseViewTriggersWhenNoUpdateAccessForState() {
        doReturn(true).when(accessControlService).canAccessCaseTypeWithCriteria(TEST_CASE_TYPE, USER_ROLES, CAN_READ);
        doReturn(true).when(accessControlService).canAccessCaseTypeWithCriteria(TEST_CASE_TYPE, USER_ROLES, CAN_UPDATE);
        doReturn(false).when(accessControlService).canAccessCaseStateWithCriteria(STATE, TEST_CASE_TYPE, USER_ROLES, CAN_UPDATE);

        CaseView caseView = authorisedGetCaseViewOperation.execute(CASE_REFERENCE);

        assertThat(caseView.getTriggers(), arrayWithSize(0));
    }

    @Test
    @DisplayName("should return case view triggers when there is CREATE access for relevant events")
    void shouldReturnCaseViewTriggersAuthorisedByAccess() {
        doReturn(true).when(accessControlService).canAccessCaseTypeWithCriteria(TEST_CASE_TYPE, USER_ROLES, CAN_READ);
        doReturn(true).when(accessControlService).canAccessCaseTypeWithCriteria(TEST_CASE_TYPE, USER_ROLES, CAN_UPDATE);
        doReturn(true).when(accessControlService).canAccessCaseStateWithCriteria(STATE, TEST_CASE_TYPE, USER_ROLES, CAN_UPDATE);
        doReturn(AUTH_CASE_VIEW_TRIGGERS).when(accessControlService).filterCaseViewTriggersByCreateAccess(TEST_CASE_VIEW.getTriggers(), TEST_CASE_TYPE.getEvents(), USER_ROLES);

        CaseView caseView = authorisedGetCaseViewOperation.execute(CASE_REFERENCE);
        assertThat(caseView.getTriggers(), arrayWithSize(1));
        assertThat(caseView.getTriggers(), arrayContaining(CASE_VIEW_TRIGGER));
    }

    @Test
    @DisplayName("returns empty case view triggers when no CREATE access for relevant events")
    void shouldReturnEmptyCaseViewTriggersWhenNotAuthorisedByAccess() {
        doReturn(true).when(accessControlService).canAccessCaseTypeWithCriteria(TEST_CASE_TYPE, USER_ROLES, CAN_READ);
        doReturn(true).when(accessControlService).canAccessCaseTypeWithCriteria(TEST_CASE_TYPE, USER_ROLES, CAN_UPDATE);
        doReturn(EMPTY_TRIGGERS).when(accessControlService).filterCaseViewTriggersByCreateAccess(TEST_CASE_VIEW.getTriggers(), TEST_CASE_TYPE.getEvents(), USER_ROLES);

        CaseView caseView = authorisedGetCaseViewOperation.execute(CASE_REFERENCE);

        assertThat(caseView.getTriggers(), arrayWithSize(0));
    }
}
