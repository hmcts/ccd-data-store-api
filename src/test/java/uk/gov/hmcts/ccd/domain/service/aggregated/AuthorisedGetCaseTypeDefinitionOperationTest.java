package uk.gov.hmcts.ccd.domain.service.aggregated;

import java.util.Optional;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_CREATE;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_UPDATE;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.AccessControlListBuilder.anAcl;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseEventBuilder.newCaseEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseStateBuilder.newState;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeBuilder.newCaseType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

class AuthorisedGetCaseTypeDefinitionOperationTest {

    private static final String CASE_TYPE_ID = "caseTypeId";
    private static final String ROLE_IN_USER_ROLES = "caseworker-probate-loa1";
    private static final String ROLE_IN_USER_ROLES_2 = "caseworker-divorce-loa";
    private static final String ROLE_IN_USER_ROLES_3 = "caseworker-probate-loa3";
    private static final Set<String> USER_ROLES = newHashSet(ROLE_IN_USER_ROLES,
        ROLE_IN_USER_ROLES_3,
        ROLE_IN_USER_ROLES_2);
    private static final String STATE_ID_1_1 = "STATE_ID_1_1";
    private static final String STATE_ID_1_2 = "STATE_ID_1_2";
    private static final String STATE_ID_2_1 = "STATE_ID_2_1";
    private static final String STATE_ID_2_2 = "STATE_ID_2_2";
    private static final String STATE_ID_3_1 = "STATE_ID_3_1";
    private static final String STATE_ID_3_2 = "STATE_ID_3_2";
    private static final String EVENT_ID_1_1 = "EVENT_ID_1_1";
    private static final String EVENT_ID_1_2 = "EVENT_ID_1_2";
    private static final String EVENT_ID_1_3 = "EVENT_ID_1_3";
    private static final String EVENT_ID_2_1 = "EVENT_ID_2_1";
    private static final String EVENT_ID_2_2 = "EVENT_ID_2_2";
    private static final String EVENT_ID_2_3 = "EVENT_ID_2_3";
    private static final String EVENT_ID_3_1 = "EVENT_ID_3_1";
    private static final String EVENT_ID_3_2 = "EVENT_ID_3_2";
    private static final String EVENT_ID_3_3 = "EVENT_ID_3_3";
    private static final String CASE_FIELD_ID_1_1 = "CASE_FIELD_1_1";
    private static final String CASE_FIELD_ID_1_2 = "CASE_FIELD_1_2";
    private static final String CASE_FIELD_ID_1_3 = "CASE_FIELD_1_3";
    private static final String CASE_FIELD_ID_2_1 = "CASE_FIELD_2_1";
    private static final String CASE_FIELD_ID_2_2 = "CASE_FIELD_2_2";
    private static final String CASE_FIELD_ID_2_3 = "CASE_FIELD_2_3";
    private static final String CASE_FIELD_ID_3_1 = "CASE_FIELD_3_1";
    private static final String CASE_FIELD_ID_3_2 = "CASE_FIELD_3_2";
    private static final String CASE_FIELD_ID_3_3 = "CASE_FIELD_3_3";
    private static final CaseStateDefinition CASE_STATE_1_1 = newState().withId(STATE_ID_1_1).build();
    private static final CaseStateDefinition CASE_STATE_1_2 = newState().withId(STATE_ID_1_2).build();
    private static final CaseStateDefinition CASE_STATE_2_1 = newState().withId(STATE_ID_2_1).build();
    private static final CaseStateDefinition CASE_STATE_2_2 = newState().withId(STATE_ID_2_2).build();
    private static final CaseStateDefinition CASE_STATE_3_1 = newState().withId(STATE_ID_3_1).build();
    private static final CaseStateDefinition CASE_STATE_3_2 = newState().withId(STATE_ID_3_2).build();
    private static final CaseEventDefinition CASE_EVENT_1_1 = newCaseEvent().withId(EVENT_ID_1_1)
        .withAcl(anAcl()
            .withRole(ROLE_IN_USER_ROLES)
            .withCreate(true)
            .withRead(true)
            .build())
        .build();

    private static final CaseEventDefinition CASE_EVENT_1_3 = newCaseEvent().withId(EVENT_ID_1_3)
        .withAcl(anAcl()
            .withRole(ROLE_IN_USER_ROLES)
            .withUpdate(true)
            .withRead(true)
            .build())
        .build();
    private static final CaseEventDefinition CASE_EVENT_2_3 = newCaseEvent().withId(EVENT_ID_2_3)
        .withAcl(anAcl()
            .withRole(ROLE_IN_USER_ROLES)
            .withCreate(true)
            .withRead(true)
            .build())
        .build();
    private static final CaseEventDefinition CASE_EVENT_3_1 = newCaseEvent().withId(EVENT_ID_3_1)
        .withAcl(anAcl()
            .withRole(ROLE_IN_USER_ROLES)
            .withCreate(true)
            .withRead(true)
            .build())
        .build();
    private static final CaseEventDefinition CASE_EVENT_3_2 = newCaseEvent().withId(EVENT_ID_3_2)
        .withAcl(anAcl()
            .withRole(ROLE_IN_USER_ROLES)
            .withUpdate(true)
            .withRead(true)
            .build())
        .build();
    private static final CaseEventDefinition CASE_EVENT_3_3 = newCaseEvent().withId(EVENT_ID_3_3)
        .withAcl(anAcl()
            .withRole(ROLE_IN_USER_ROLES)
            .withCreate(true)
            .withRead(true)
            .build())
        .build();

    private static final CaseFieldDefinition CASE_FIELD_1_1 = newCaseField().withId(CASE_FIELD_ID_1_1)
        .withAcl(anAcl()
            .withRole(ROLE_IN_USER_ROLES)
            .build())
        .build();
    private static final CaseFieldDefinition CASE_FIELD_1_2 = newCaseField().withId(CASE_FIELD_ID_1_2)
        .withAcl(anAcl()
            .withRole(ROLE_IN_USER_ROLES)
            .withRead(true)
            .build())
        .build();
    private static final CaseFieldDefinition CASE_FIELD_1_3 = newCaseField().withId(CASE_FIELD_ID_1_3)
        .withAcl(anAcl()
            .withRole(ROLE_IN_USER_ROLES)
            .build())
        .build();
    private static final CaseFieldDefinition CASE_FIELD_2_3 = newCaseField().withId(CASE_FIELD_ID_2_3)
        .withAcl(anAcl()
            .withRole(ROLE_IN_USER_ROLES)
            .withCreate(true)
            .withRead(true)
            .build())
        .build();
    private static final CaseFieldDefinition CASE_FIELD_3_1 = newCaseField().withId(CASE_FIELD_ID_3_1)
        .withAcl(anAcl()
            .withRole(ROLE_IN_USER_ROLES)
            .withUpdate(true)
            .withRead(true)
            .build())
        .build();
    private static final CaseFieldDefinition CASE_FIELD_3_2 = newCaseField().withId(CASE_FIELD_ID_3_2)
        .withAcl(anAcl()
            .withRole(ROLE_IN_USER_ROLES)
            .withUpdate(true)
            .withCreate(true)
            .build())
        .build();
    private static final CaseFieldDefinition CASE_FIELD_3_3 = newCaseField().withId(CASE_FIELD_ID_3_3)
        .withAcl(anAcl()
            .withRole(ROLE_IN_USER_ROLES)
            .withRead(true)
            .build())
        .build();



    private CaseTypeDefinition testCaseTypeDefinition1;
    private CaseTypeDefinition testCaseTypeDefinition2;
    private CaseTypeDefinition testCaseTypeDefinition3;

    @Mock
    private AccessControlService accessControlService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private GetCaseTypeOperation getCaseTypeOperation;

    private AuthorisedGetCaseTypeOperation authorisedGetCaseTypeOperation;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        testCaseTypeDefinition1 = newCaseType()
            .withId(CASE_TYPE_ID)
            .withAcl(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withRead(true)
                .build())
            .withState(CASE_STATE_1_1)
            .withState(CASE_STATE_1_2)
            .withEvent(CASE_EVENT_1_1)
            .withEvent(newCaseEvent()
                .withId(EVENT_ID_1_2)
                .withAcl(anAcl()
                    .withRole(ROLE_IN_USER_ROLES)
                    .build())
                .build())
            .withEvent(CASE_EVENT_1_3)
            .withField(CASE_FIELD_1_1)
            .withField(CASE_FIELD_1_2)
            .withField(CASE_FIELD_1_3)
            .build();

        testCaseTypeDefinition2 = newCaseType()
            .withId(CASE_TYPE_ID)
            .withAcl(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withCreate(true)
                .build())
            .withState(CASE_STATE_2_1)
            .withState(CASE_STATE_2_2)
            .withEvent(newCaseEvent()
                .withId(EVENT_ID_2_1)
                .withAcl(anAcl()
                    .withRole(ROLE_IN_USER_ROLES)
                    .withRead(true)
                    .build())
                .build())
            .withEvent(newCaseEvent().withId(EVENT_ID_2_2)
                .withAcl(anAcl()
                    .withRole(ROLE_IN_USER_ROLES)
                    .withRead(true)
                    .build())
                .build())
            .withEvent(CASE_EVENT_2_3)
            .withField(newCaseField().withId(CASE_FIELD_ID_2_1)
                .withAcl(anAcl()
                    .withRole(ROLE_IN_USER_ROLES)
                    .withRead(true)
                    .build())
                .build())
            .withField(newCaseField().withId(CASE_FIELD_ID_2_2)
                .withAcl(anAcl()
                    .withRole(ROLE_IN_USER_ROLES)
                    .build())
                .build())
            .withField(CASE_FIELD_2_3)
            .build();

        testCaseTypeDefinition3 = newCaseType()
            .withId(CASE_TYPE_ID)
            .withAcl(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withCreate(true)
                .withRead(true)
                .withUpdate(true)
                .build())
            .withState(CASE_STATE_3_1)
            .withState(CASE_STATE_3_2)
            .withEvent(CASE_EVENT_3_1)
            .withEvent(CASE_EVENT_3_2)
            .withEvent(CASE_EVENT_3_3)
            .withField(CASE_FIELD_3_1)
            .withField(CASE_FIELD_3_2)
            .withField(CASE_FIELD_3_3)
            .build();

        doReturn(USER_ROLES).when(userRepository).getUserRoles();

        doReturn(false).when(accessControlService).canAccessCaseTypeWithCriteria(testCaseTypeDefinition1, USER_ROLES, CAN_CREATE);
        doReturn(false).when(accessControlService).canAccessCaseTypeWithCriteria(testCaseTypeDefinition1, USER_ROLES, CAN_UPDATE);
        doReturn(true).when(accessControlService).canAccessCaseTypeWithCriteria(testCaseTypeDefinition1, USER_ROLES, CAN_READ);
        doReturn(true).when(accessControlService).canAccessCaseTypeWithCriteria(testCaseTypeDefinition2, USER_ROLES, CAN_CREATE);
        doReturn(false).when(accessControlService).canAccessCaseTypeWithCriteria(testCaseTypeDefinition2, USER_ROLES, CAN_UPDATE);
        doReturn(false).when(accessControlService).canAccessCaseTypeWithCriteria(testCaseTypeDefinition2, USER_ROLES, CAN_READ);
        doReturn(true).when(accessControlService).canAccessCaseTypeWithCriteria(testCaseTypeDefinition3, USER_ROLES, CAN_CREATE);
        doReturn(true).when(accessControlService).canAccessCaseTypeWithCriteria(testCaseTypeDefinition3, USER_ROLES, CAN_UPDATE);
        doReturn(true).when(accessControlService).canAccessCaseTypeWithCriteria(testCaseTypeDefinition3, USER_ROLES, CAN_READ);
        authorisedGetCaseTypeOperation = new AuthorisedGetCaseTypeOperation(accessControlService,
            userRepository,
            getCaseTypeOperation);
    }

    @Nested
    @DisplayName("case type tests")
    class ReturnsCaseTypeDefinitionWithMatchingAccessCriteria {

        @Test
        @DisplayName("Should return case type that has matching access rights")
        void shouldReturnCreateAccessCaseType() {
            doReturn(Optional.of(testCaseTypeDefinition2)).when(getCaseTypeOperation).execute(CASE_TYPE_ID, CAN_CREATE);

            Optional<CaseTypeDefinition> caseTypeOpt = authorisedGetCaseTypeOperation.execute(CASE_TYPE_ID, CAN_CREATE);

            assertThat(caseTypeOpt.get(), equalTo(testCaseTypeDefinition2));
        }

        @Test
        @DisplayName("Should not return case type that hasn't matching access rights")
        void shouldNotReturnCaseTypeIfNoCreateAccess() {
            doReturn(Optional.of(testCaseTypeDefinition1)).when(getCaseTypeOperation).execute(CASE_TYPE_ID, CAN_CREATE);

            Optional<CaseTypeDefinition> caseTypeOpt = authorisedGetCaseTypeOperation.execute(CASE_TYPE_ID, CAN_CREATE);

            assertThat(caseTypeOpt, equalTo(Optional.empty()));
        }
    }

    @Nested
    @DisplayName("case state tests")
    class ReturnsStatesWithMatchingAccessCriteria {

        @Test
        @DisplayName("Should return case states that have matching access rights")
        void shouldReturnCorrectCaseStatesThatHaveAccessRights() {
            doReturn(Optional.of(testCaseTypeDefinition1)).when(getCaseTypeOperation).execute(CASE_TYPE_ID, CAN_READ);
            doReturn(newArrayList(CASE_STATE_1_1)).when(accessControlService).filterCaseStatesByAccess(testCaseTypeDefinition1.getStates(),
                USER_ROLES,
                CAN_READ);

            Optional<CaseTypeDefinition> caseTypeOpt = authorisedGetCaseTypeOperation.execute(CASE_TYPE_ID, CAN_READ);

            assertAll(
                () -> assertThat(caseTypeOpt.get(), is(testCaseTypeDefinition1)),
                () -> assertThat(caseTypeOpt.get().getStates(), hasItems(CASE_STATE_1_1)),
                () -> assertThat(caseTypeOpt.get().getStates(), not(hasItems(CASE_STATE_1_2)))
            );
        }

        @Test
        @DisplayName("Should return no states if no matching access rights")
        void shouldNotReturnCaseTypeIfNoStatesThatHaveAccessRights() {
            doReturn(Optional.of(testCaseTypeDefinition1)).when(getCaseTypeOperation).execute(CASE_TYPE_ID, CAN_READ);

            Optional<CaseTypeDefinition> caseTypeOpt = authorisedGetCaseTypeOperation.execute(CASE_TYPE_ID, CAN_READ);

            assertThat(caseTypeOpt.get().getStates(), hasSize(0));
        }
    }

    @Nested
    @DisplayName("events tests")
    class ReturnsEventsThatMatchAccessCriteria {

        @Test
        @DisplayName("Should return case events that have matching access rights")
        void shouldReturnEventsWithMatchingAccessRights() {
            doReturn(Optional.of(testCaseTypeDefinition3)).when(getCaseTypeOperation).execute(CASE_TYPE_ID, CAN_CREATE);
            doReturn(newArrayList(CASE_EVENT_3_1, CASE_EVENT_3_3)).when(accessControlService).filterCaseEventsByAccess(
                testCaseTypeDefinition3.getEvents(),
                USER_ROLES,
                CAN_CREATE);

            Optional<CaseTypeDefinition> caseTypeOpt = authorisedGetCaseTypeOperation.execute(CASE_TYPE_ID, CAN_CREATE);

            assertThat(caseTypeOpt.get().getEvents(), hasSize(2));
            assertThat(caseTypeOpt.get().getEvents(),
                hasItems(hasProperty("id", equalTo(EVENT_ID_3_1)), hasProperty("id", equalTo(EVENT_ID_3_3))));
        }

        @Test
        @DisplayName("Should return empty events if no event that have matching access rights")
        void shouldReturnNoEventsIfNoAccessRights() {
            doReturn(Optional.of(testCaseTypeDefinition3)).when(getCaseTypeOperation).execute(CASE_TYPE_ID, CAN_CREATE);

            Optional<CaseTypeDefinition> caseTypeOpt = authorisedGetCaseTypeOperation.execute(CASE_TYPE_ID, CAN_CREATE);

            assertThat(caseTypeOpt.get().getEvents(), hasSize(0));
        }
    }

    @Nested
    @DisplayName("fields tests")
    class ReturnsCaseTypeDefinitionWithFieldsThatMatchAccessCriteria {

        @Test
        @DisplayName("Should return case type with case fields that have matching access rights")
        void shouldReturnCaseTypeWithMatchingAccessFields() {
            doReturn(Optional.of(testCaseTypeDefinition2)).when(getCaseTypeOperation).execute(CASE_TYPE_ID, CAN_CREATE);
            doReturn(newArrayList(CASE_FIELD_2_3)).when(accessControlService).filterCaseFieldsByAccess(testCaseTypeDefinition2.getCaseFieldDefinitions(),
                USER_ROLES,
                CAN_CREATE);

            Optional<CaseTypeDefinition> caseTypeOpt = authorisedGetCaseTypeOperation.execute(CASE_TYPE_ID, CAN_CREATE);

            assertThat(caseTypeOpt.get().getCaseFieldDefinitions(), hasSize(1));
            assertThat(caseTypeOpt.get().getCaseFieldDefinitions(),
                hasItems(hasProperty("id", equalTo(CASE_FIELD_ID_2_3))));
        }
    }
}
