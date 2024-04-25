package uk.gov.hmcts.ccd.domain.service.aggregated;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseHistoryView;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewTab;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.valueOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDetailsBuilder.newCaseDetails;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseEventBuilder.newCaseEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseHistoryViewBuilder.aCaseHistoryView;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeBuilder.newCaseType;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseViewEventBuilder.aCaseViewEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseViewFieldBuilder.aViewField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseViewTabBuilder.newCaseViewTab;

class AuthorisedGetCaseHistoryViewOperationTest {
    private static final String CASE_TYPE_ID = "Grant";
    private static final String CASE_REFERENCE = "1111222233334444";
    private static final String USER_ID = "26";
    private static final Long EVENT_ID = 100L;
    private static final String ROLE_IN_USER_ROLES = "caseworker-probate-loa1";
    private static final String ROLE_IN_USER_ROLES_2 = "caseworker-divorce-loa";
    private static final Set<String> USER_ROLES = newHashSet(ROLE_IN_USER_ROLES, ROLE_IN_USER_ROLES_2);

    private static final Set<AccessProfile> ACCESS_PROFILES = createAccessProfiles(USER_ROLES);
    private static final List<AccessControlList> ACCESS_CONTROL_LISTS = createAccessControlList(USER_ROLES);
    private static final String ROLE_NOT_IN_USER_ROLES = "caseworker-family-law";
    private static final String EVENT_ID_STRING = valueOf(EVENT_ID);
    private static final CaseEventDefinition CASE_EVENT = newCaseEvent().withId(EVENT_ID_STRING).build();
    private static final CaseDetails CASE_DETAILS = newCaseDetails().withId(CASE_REFERENCE)
            .withCaseTypeId(CASE_TYPE_ID).withReference(Long.valueOf(CASE_REFERENCE)).build();
    private static final CaseEventDefinition CASE_EVENT_2 = newCaseEvent().withId("event2").build();
    private static final CaseTypeDefinition TEST_CASE_TYPE = newCaseType().withEvent(CASE_EVENT)
            .withEvent(CASE_EVENT_2).build();
    private static final CaseViewEvent CASE_VIEW_EVENT = aCaseViewEvent().withId(EVENT_ID_STRING).build();
    private final List<String> caseRoles = Collections.emptyList();

    private static CaseViewField FIELD_1;
    private static CaseViewField FIELD_2;
    private static CaseViewField FIELD_3;

    private static CaseViewTab CASE_VIEW_TAB_WITH_ROLE_ALLOWED;
    private static CaseViewTab CASE_VIEW_TAB_WITH_ROLE_ALLOWED2;
    private static CaseViewTab CASE_VIEW_TAB_WITH_ROLE_NOT_ALLOWED;

    private static CaseHistoryView TEST_CASE_HISTORY_VIEW;

    @Mock
    private uk.gov.hmcts.ccd.domain.service.aggregated.GetCaseHistoryViewOperation getCaseHistoryViewOperation;
    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CaseUserRepository caseUserRepository;
    @Mock
    private CaseDetailsRepository caseDetailsRepository;
    @Mock
    private CaseDataAccessControl caseDataAccessControl;

    private uk.gov.hmcts.ccd.domain.service.aggregated.AuthorisedGetCaseHistoryViewOperation
            authorisedGetCaseHistoryViewOperation;

    @BeforeEach
    void resetStaticFields() {
        FIELD_1 = aViewField().withId("FIELD_1").build();
        FIELD_2 = aViewField().withId("FIELD_2").build();
        FIELD_3 = aViewField().withId("FIELD_3").build();

        CASE_VIEW_TAB_WITH_ROLE_ALLOWED = newCaseViewTab().withId("cvt3")
            .addCaseViewField(FIELD_1)
            .withRole(ROLE_IN_USER_ROLES_2)
            .build();

        CASE_VIEW_TAB_WITH_ROLE_ALLOWED2 = newCaseViewTab().withId("cvt3")
            .addCaseViewField(FIELD_2)
            .withRole(ROLE_IN_USER_ROLES_2)
            .build();

        CASE_VIEW_TAB_WITH_ROLE_NOT_ALLOWED = newCaseViewTab().withId("cvt4")
            .addCaseViewField(FIELD_3)
            .withRole(ROLE_NOT_IN_USER_ROLES)
            .build();

        TEST_CASE_HISTORY_VIEW = aCaseHistoryView()
            .addCaseHistoryViewTab(CASE_VIEW_TAB_WITH_ROLE_ALLOWED)
            .addCaseHistoryViewTab(CASE_VIEW_TAB_WITH_ROLE_ALLOWED2)
            .addCaseHistoryViewTab(CASE_VIEW_TAB_WITH_ROLE_NOT_ALLOWED)
            .withEvent(CASE_VIEW_EVENT)
            .build();
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        FIELD_1.setAccessControlLists(ACCESS_CONTROL_LISTS);
        FIELD_2.setAccessControlLists(ACCESS_CONTROL_LISTS);
        FIELD_3.setAccessControlLists(ACCESS_CONTROL_LISTS);

        doReturn(TEST_CASE_TYPE).when(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);
        doReturn(ACCESS_PROFILES).when(caseDataAccessControl)
            .generateAccessProfilesByCaseTypeId(CASE_TYPE_ID);
        doReturn(ACCESS_PROFILES).when(caseDataAccessControl)
            .generateAccessProfilesByCaseReference(anyString());
        doReturn(TEST_CASE_HISTORY_VIEW).when(getCaseHistoryViewOperation).execute(CASE_REFERENCE, EVENT_ID);
        doReturn(caseRoles).when(caseUserRepository).findCaseRoles(Long.valueOf(CASE_REFERENCE), USER_ID);
        doReturn(Optional.of(CASE_DETAILS)).when(caseDetailsRepository).findByReference(CASE_REFERENCE);

        authorisedGetCaseHistoryViewOperation =
                new uk.gov.hmcts.ccd.domain.service.aggregated.AuthorisedGetCaseHistoryViewOperation(
                    getCaseHistoryViewOperation, caseDefinitionRepository,
                    accessControlService, caseDetailsRepository, caseDataAccessControl);
    }

    private static Set<AccessProfile> createAccessProfiles(Set<String> userRoles) {
        return userRoles.stream()
            .map(userRole -> AccessProfile.builder().readOnly(false)
                .accessProfile(userRole)
                .build())
            .collect(Collectors.toSet());
    }

    private static List<AccessControlList> createAccessControlList(Set<String> userRoles) {
        return userRoles.stream()
            .map(userRole -> {
                AccessControlList controlList = new AccessControlList();
                controlList.setAccessProfile(userRole);
                controlList.setRead(true);
                return controlList;
            })
            .toList();
    }

    @Test
    @DisplayName("should return case history view")
    void shouldReturnCaseHistoryView() {
        doReturn(true).when(accessControlService).canAccessCaseTypeWithCriteria(TEST_CASE_TYPE,
            ACCESS_PROFILES, CAN_READ);
        doReturn(USER_ID).when(userRepository).getUserId();

        CaseHistoryView caseHistoryView = authorisedGetCaseHistoryViewOperation.execute(CASE_REFERENCE, EVENT_ID);

        assertThat(caseHistoryView, CoreMatchers.is(TEST_CASE_HISTORY_VIEW));
        verify(getCaseHistoryViewOperation).execute(CASE_REFERENCE, EVENT_ID);
        verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);
        verify(caseDetailsRepository).findByReference(CASE_REFERENCE);
        verify(caseDataAccessControl).generateAccessProfilesByCaseReference(CASE_REFERENCE);
        verify(accessControlService).canAccessCaseTypeWithCriteria(TEST_CASE_TYPE, ACCESS_PROFILES, CAN_READ);
    }

    @Test
    @DisplayName("should remove all tabs based on Tab Role)")
    void shouldRemoveAllTabsNotAllowedForUser() {
        doReturn(true).when(accessControlService)
            .canAccessCaseTypeWithCriteria(TEST_CASE_TYPE, ACCESS_PROFILES, CAN_READ);

        final CaseHistoryView actualCaseView = authorisedGetCaseHistoryViewOperation.execute(CASE_REFERENCE, EVENT_ID);

        assertAll(
            () -> verify(getCaseHistoryViewOperation).execute(CASE_REFERENCE, EVENT_ID),
            () -> assertThat(actualCaseView.getTabs().length, is(0))
        );
    }

    @Test
    @DisplayName("should remove not allowed tab based on Tab Role)")
    void shouldRemoveTabsNotAllowedForUser() {
        doReturn(true).when(accessControlService)
            .canAccessCaseTypeWithCriteria(TEST_CASE_TYPE, ACCESS_PROFILES, CAN_READ);

        doReturn(true).when(accessControlService)
            .canAccessCaseViewFieldWithCriteria(FIELD_1, ACCESS_PROFILES, CAN_READ);
        doReturn(true).when(accessControlService)
            .canAccessCaseViewFieldWithCriteria(FIELD_2, ACCESS_PROFILES, CAN_READ);
        doReturn(true).when(accessControlService)
            .canAccessCaseViewFieldWithCriteria(FIELD_3, ACCESS_PROFILES, CAN_READ);

        final CaseHistoryView actualCaseView = authorisedGetCaseHistoryViewOperation.execute(CASE_REFERENCE, EVENT_ID);

        assertAll(
            () -> verify(getCaseHistoryViewOperation).execute(CASE_REFERENCE, EVENT_ID),
            () -> assertThat(actualCaseView.getTabs().length, is(2)),
            () -> assertNotEquals(actualCaseView.getTabs()[0], CASE_VIEW_TAB_WITH_ROLE_NOT_ALLOWED),
            () -> assertNotEquals(actualCaseView.getTabs()[1], CASE_VIEW_TAB_WITH_ROLE_NOT_ALLOWED)
        );
    }

    @Test
    @DisplayName("should remove allowed tab based on not allowed fields)")
    void shouldRemoveTabsNotAllowedFields() {
        doReturn(true).when(accessControlService)
            .canAccessCaseTypeWithCriteria(TEST_CASE_TYPE, ACCESS_PROFILES, CAN_READ);

        doReturn(true).when(accessControlService)
            .canAccessCaseViewFieldWithCriteria(FIELD_1, ACCESS_PROFILES, CAN_READ);
        doReturn(false).when(accessControlService)
            .canAccessCaseViewFieldWithCriteria(FIELD_2, ACCESS_PROFILES, CAN_READ);
        doReturn(true).when(accessControlService)
            .canAccessCaseViewFieldWithCriteria(FIELD_3, ACCESS_PROFILES, CAN_READ);

        final CaseHistoryView actualCaseView = authorisedGetCaseHistoryViewOperation.execute(CASE_REFERENCE, EVENT_ID);

        assertAll(
            () -> verify(getCaseHistoryViewOperation).execute(CASE_REFERENCE, EVENT_ID),
            () -> assertThat(actualCaseView.getTabs().length, is(1)),
            () -> assertEquals(actualCaseView.getTabs()[0], CASE_VIEW_TAB_WITH_ROLE_ALLOWED)
        );
    }

    @Test
    @DisplayName("should throw exception when case type not found")
    void shouldThrowExceptionWhenCaseTypeNotFound() {
        doReturn(null).when(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);

        assertThrows(ValidationException.class,
            () -> authorisedGetCaseHistoryViewOperation.execute(CASE_REFERENCE, EVENT_ID));
    }
}
