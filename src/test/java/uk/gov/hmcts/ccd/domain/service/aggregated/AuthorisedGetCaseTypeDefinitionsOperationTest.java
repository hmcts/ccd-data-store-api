package uk.gov.hmcts.ccd.domain.service.aggregated;

import java.util.List;
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

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEvent;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseState;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

class AuthorisedGetCaseTypeDefinitionsOperationTest {

    private static final String JURISDICTION_ID = "TEST";
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
    private static final CaseState CASE_STATE_1_1 = newState().withId(STATE_ID_1_1).build();
    private static final CaseState CASE_STATE_1_2 = newState().withId(STATE_ID_1_2).build();
    private static final CaseState CASE_STATE_2_1 = newState().withId(STATE_ID_2_1).build();
    private static final CaseState CASE_STATE_2_2 = newState().withId(STATE_ID_2_2).build();
    private static final CaseState CASE_STATE_3_1 = newState().withId(STATE_ID_3_1).build();
    private static final CaseState CASE_STATE_3_2 = newState().withId(STATE_ID_3_2).build();
    private static final CaseEvent CASE_EVENT_1_1 = newCaseEvent().withId(EVENT_ID_1_1)
        .withAcl(anAcl()
                     .withRole(ROLE_IN_USER_ROLES)
                     .withCreate(true)
                     .withRead(true)
                     .build())
        .build();
    private static final CaseEvent CASE_EVENT_1_2 = newCaseEvent().withId(EVENT_ID_1_2)
        .withAcl(anAcl()
                     .withRole(ROLE_IN_USER_ROLES)
                     .withUpdate(true)
                     .withRead(true)
                     .build())
        .build();
    private static final CaseEvent CASE_EVENT_1_3 = newCaseEvent().withId(EVENT_ID_1_3)
        .withAcl(anAcl()
                     .withRole(ROLE_IN_USER_ROLES)
                     .withUpdate(true)
                     .withRead(true)
                     .build())
        .build();
    private static final CaseEvent CASE_EVENT_2_3 = newCaseEvent().withId(EVENT_ID_2_3)
        .withAcl(anAcl()
                     .withRole(ROLE_IN_USER_ROLES)
                     .withCreate(true)
                     .withRead(true)
                     .build())
        .build();
    private static final CaseEvent CASE_EVENT_3_1 = newCaseEvent().withId(EVENT_ID_3_1)
        .withAcl(anAcl()
                     .withRole(ROLE_IN_USER_ROLES)
                     .withCreate(true)
                     .withRead(true)
                     .build())
        .build();
    private static final CaseEvent CASE_EVENT_3_2 = newCaseEvent().withId(EVENT_ID_3_2)
        .withAcl(anAcl()
                     .withRole(ROLE_IN_USER_ROLES)
                     .withUpdate(true)
                     .withRead(true)
                     .build())
        .build();
    private static final CaseEvent CASE_EVENT_3_3 = newCaseEvent().withId(EVENT_ID_3_3)
        .withAcl(anAcl()
                     .withRole(ROLE_IN_USER_ROLES)
                     .withCreate(true)
                     .withRead(true)
                     .build())
        .build();

    private static final CaseField CASE_FIELD_1_1 = newCaseField().withId(CASE_FIELD_ID_1_1)
        .withAcl(anAcl()
                     .withRole(ROLE_IN_USER_ROLES)
                     .build())
        .build();
    private static final CaseField CASE_FIELD_1_2 = newCaseField().withId(CASE_FIELD_ID_1_2)
        .withAcl(anAcl()
                     .withRole(ROLE_IN_USER_ROLES)
                     .withRead(true)
                     .build())
        .build();
    private static final CaseField CASE_FIELD_1_3 = newCaseField().withId(CASE_FIELD_ID_1_3)
        .withAcl(anAcl()
                     .withRole(ROLE_IN_USER_ROLES)
                     .build())
        .build();
    private static final CaseField CASE_FIELD_2_3 = newCaseField().withId(CASE_FIELD_ID_2_3)
        .withAcl(anAcl()
                     .withRole(ROLE_IN_USER_ROLES)
                     .withCreate(true)
                     .withRead(true)
                     .build())
        .build();
    private static final CaseField CASE_FIELD_3_1 = newCaseField().withId(CASE_FIELD_ID_3_1)
        .withAcl(anAcl()
                     .withRole(ROLE_IN_USER_ROLES)
                     .withUpdate(true)
                     .withRead(true)
                     .build())
        .build();
    private static final CaseField CASE_FIELD_3_2 = newCaseField().withId(CASE_FIELD_ID_3_2)
        .withAcl(anAcl()
                     .withRole(ROLE_IN_USER_ROLES)
                     .withUpdate(true)
                     .withCreate(true)
                     .build())
        .build();
    private static final CaseField CASE_FIELD_3_3 = newCaseField().withId(CASE_FIELD_ID_3_3)
        .withAcl(anAcl()
                     .withRole(ROLE_IN_USER_ROLES)
                     .withRead(true)
                     .build())
        .build();


    private CaseTypeDefinition testCaseTypeDefinition1;
    private CaseTypeDefinition testCaseTypeDefinition2;
    private CaseTypeDefinition testCaseTypeDefinition3;
    private List<CaseTypeDefinition> testCaseTypeDefinitions;

    @Mock
    private AccessControlService accessControlService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private GetCaseTypesOperation getCaseTypesOperation;

    private AuthorisedGetCaseTypesOperation authorisedGetCaseTypesOperation;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        testCaseTypeDefinition1 = newCaseType()
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

        testCaseTypeDefinitions = Lists.newArrayList(testCaseTypeDefinition1, testCaseTypeDefinition2, testCaseTypeDefinition3);

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
        authorisedGetCaseTypesOperation = new AuthorisedGetCaseTypesOperation(accessControlService,
                                                                              userRepository,
                                                                              getCaseTypesOperation);
    }

    @Nested
    @DisplayName("case type tests")
    class ReturnsCaseTypeDefinitionsWithMatchingAccessCriteria {

        @Test
        @DisplayName("Should return case types that have matching create access rights")
        void shouldReturnCreateAccessCaseTypesForJurisdiction() {
            doReturn(testCaseTypeDefinitions).when(getCaseTypesOperation).execute(JURISDICTION_ID, CAN_CREATE);

            List<CaseTypeDefinition> caseTypeDefinitions = authorisedGetCaseTypesOperation.execute(JURISDICTION_ID, CAN_CREATE);

            assertThat(caseTypeDefinitions, hasItems(testCaseTypeDefinition2, testCaseTypeDefinition3));
        }

        @Test
        @DisplayName("Should return case types that have matching read access rights")
        void shouldReturnReadAccessCaseTypesForJurisdiction() {
            doReturn(testCaseTypeDefinitions).when(getCaseTypesOperation).execute(JURISDICTION_ID, CAN_READ);

            List<CaseTypeDefinition> caseTypeDefinitions = authorisedGetCaseTypesOperation.execute(JURISDICTION_ID, CAN_READ);

            assertThat(caseTypeDefinitions, hasItems(testCaseTypeDefinition1, testCaseTypeDefinition3));
        }

        @Test
        @DisplayName("Should return case types that have matching update access rights")
        void shouldReturnUpdateAccessCaseTypesForJurisdiction() {
            doReturn(testCaseTypeDefinitions).when(getCaseTypesOperation).execute(JURISDICTION_ID, CAN_UPDATE);

            List<CaseTypeDefinition> caseTypeDefinitions = authorisedGetCaseTypesOperation.execute(JURISDICTION_ID, CAN_UPDATE);

            assertThat(caseTypeDefinitions, hasItems(testCaseTypeDefinition3));
        }
    }

    @Nested
    @DisplayName("case state tests")
    class ReturnsCaseStatesWithMatchingAccessCriteria {

        @Test
        @DisplayName("Should return case states that have matching read access rights")
        void shouldReturnCorrectCaseStatesThatHaveReadAccess() {
            doReturn(testCaseTypeDefinitions).when(getCaseTypesOperation).execute(JURISDICTION_ID, CAN_READ);
            doReturn(newArrayList(CASE_STATE_1_1)).when(accessControlService).filterCaseStatesByAccess(testCaseTypeDefinition1.getStates(),
                USER_ROLES,
                CAN_READ);
            doReturn(newArrayList(CASE_STATE_3_1)).when(accessControlService).filterCaseStatesByAccess(testCaseTypeDefinition3.getStates(),
                USER_ROLES,
                CAN_READ);

            List<CaseTypeDefinition> caseTypeDefinitions = authorisedGetCaseTypesOperation.execute(JURISDICTION_ID, CAN_READ);

            assertAll(
                () -> assertThat(caseTypeDefinitions.size(), is(2)),
                () -> assertThat(caseTypeDefinitions, hasItems(testCaseTypeDefinition1, testCaseTypeDefinition3)),
                () -> assertThat(caseTypeDefinitions.get(0).getStates(), hasItems(CASE_STATE_1_1)),
                () -> assertThat(caseTypeDefinitions.get(0).getStates(), not(hasItems(CASE_STATE_1_2))),
                () -> assertThat(caseTypeDefinitions.get(1).getStates(), hasItems(CASE_STATE_3_1)),
                () -> assertThat(caseTypeDefinitions.get(1).getStates(), not(hasItems(CASE_STATE_3_2)))
            );
        }
    }

        @Nested
    @DisplayName("events tests")
    class ReturnsCaseTypeDefinitionsWithEventsThatMatchAccessCriteria {

        @Test
        @DisplayName("Should return case types with case events that have matching create access rights")
        void shouldReturnCaseTypesWithCreateAccessEventsForJurisdiction() {
            doReturn(testCaseTypeDefinitions).when(getCaseTypesOperation).execute(JURISDICTION_ID, CAN_CREATE);
            doReturn(newArrayList(CASE_EVENT_2_3)).when(accessControlService).filterCaseEventsByAccess(testCaseTypeDefinition2.getEvents(),
                                                                                                       USER_ROLES,
                                                                                                       CAN_CREATE);

            doReturn(newArrayList(CASE_EVENT_3_1, CASE_EVENT_3_3)).when(accessControlService).filterCaseEventsByAccess(
                testCaseTypeDefinition3.getEvents(),
                USER_ROLES,
                CAN_CREATE);

            List<CaseTypeDefinition> caseTypeDefinitions = authorisedGetCaseTypesOperation.execute(JURISDICTION_ID, CAN_CREATE);

            assertThat(caseTypeDefinitions.get(0).getEvents(), hasSize(1));
            assertThat(caseTypeDefinitions.get(0).getEvents(), hasItems(hasProperty("id", equalTo(EVENT_ID_2_3))));
            assertThat(caseTypeDefinitions.get(1).getEvents(), hasSize(2));
            assertThat(caseTypeDefinitions.get(1).getEvents(),
                       hasItems(hasProperty("id", equalTo(EVENT_ID_3_1)), hasProperty("id", equalTo(EVENT_ID_3_3))));
        }

        @Test
        @DisplayName("Should return case types with case events that have matching update access rights")
        void shouldReturnCaseTypesWithUpdateAccessEventsForJurisdiction() {
            doReturn(testCaseTypeDefinitions).when(getCaseTypesOperation).execute(JURISDICTION_ID, CAN_UPDATE);
            doReturn(newArrayList(CASE_EVENT_3_2)).when(accessControlService).filterCaseEventsByAccess(testCaseTypeDefinition3.getEvents(),
                                                                                                       USER_ROLES,
                                                                                                       CAN_UPDATE);

            List<CaseTypeDefinition> caseTypeDefinitions = authorisedGetCaseTypesOperation.execute(JURISDICTION_ID, CAN_UPDATE);

            assertThat(caseTypeDefinitions.get(0).getEvents(), hasSize(1));
            assertThat(caseTypeDefinitions.get(0).getEvents(), hasItems(hasProperty("id", equalTo(EVENT_ID_3_2))));
        }

        @Test
        @DisplayName("Should return case types with case events that have matching read access rights")
        void shouldReturnCaseTypesWithReadAccessEventsForJurisdiction() {
            doReturn(testCaseTypeDefinitions).when(getCaseTypesOperation).execute(JURISDICTION_ID, CAN_READ);
            doReturn(newArrayList(CASE_EVENT_1_1, CASE_EVENT_1_2)).when(accessControlService).filterCaseEventsByAccess(
                testCaseTypeDefinition1.getEvents(),
                USER_ROLES,
                CAN_READ);
            doReturn(newArrayList(CASE_EVENT_3_1,
                                  CASE_EVENT_3_2,
                                  CASE_EVENT_3_3)).when(accessControlService).filterCaseEventsByAccess(testCaseTypeDefinition3.getEvents(),
                                                                                                       USER_ROLES,
                                                                                                       CAN_READ);

            List<CaseTypeDefinition> caseTypeDefinitions = authorisedGetCaseTypesOperation.execute(JURISDICTION_ID, CAN_READ);

            assertThat(caseTypeDefinitions.get(0).getEvents(), hasSize(2));
            assertThat(caseTypeDefinitions.get(0).getEvents(),
                       hasItems(hasProperty("id", equalTo(EVENT_ID_1_1)), hasProperty("id", equalTo(EVENT_ID_1_2))));
            assertThat(caseTypeDefinitions.get(1).getEvents(), hasSize(3));
            assertThat(caseTypeDefinitions.get(1).getEvents(),
                       hasItems(hasProperty("id", equalTo(EVENT_ID_3_1)),
                                hasProperty("id", equalTo(EVENT_ID_3_2)),
                                hasProperty("id", equalTo(EVENT_ID_3_3))));
        }
    }

    @Nested
    @DisplayName("fields tests")
    class ReturnsCaseTypeDefinitionsWithFieldsThatMatchAccessCriteriaDefinition {

        @Test
        @DisplayName("Should return case types with case fields that have matching create access rights")
        void shouldReturnCaseTypesWithCreateAccessFieldsForJurisdiction() {
            doReturn(testCaseTypeDefinitions).when(getCaseTypesOperation).execute(JURISDICTION_ID, CAN_CREATE);
            doReturn(newArrayList(CASE_FIELD_2_3)).when(accessControlService).filterCaseFieldsByAccess(testCaseTypeDefinition2.getCaseFields(),
                                                                                                       USER_ROLES,
                                                                                                       CAN_CREATE);
            doReturn(newArrayList(CASE_FIELD_3_2)).when(accessControlService).filterCaseFieldsByAccess(testCaseTypeDefinition3.getCaseFields(),
                                                                                                       USER_ROLES,
                                                                                                       CAN_CREATE);

            List<CaseTypeDefinition> caseTypeDefinitions = authorisedGetCaseTypesOperation.execute(JURISDICTION_ID, CAN_CREATE);

            assertThat(caseTypeDefinitions.get(0).getCaseFields(), hasSize(1));
            assertThat(caseTypeDefinitions.get(0).getCaseFields(),
                       hasItems(hasProperty("id", equalTo(CASE_FIELD_ID_2_3))));
            assertThat(caseTypeDefinitions.get(1).getCaseFields(), hasSize(1));
            assertThat(caseTypeDefinitions.get(1).getCaseFields(),
                       hasItems(hasProperty("id", equalTo(CASE_FIELD_ID_3_2))));
        }

        @Test
        @DisplayName("Should return case types with case fields that have matching update access rights")
        void shouldReturnCaseTypesWithUpdateAccessFieldsForJurisdiction() {
            doReturn(testCaseTypeDefinitions).when(getCaseTypesOperation).execute(JURISDICTION_ID, CAN_UPDATE);
            doReturn(newArrayList(CASE_FIELD_3_1, CASE_FIELD_3_2)).when(accessControlService).filterCaseFieldsByAccess(
                testCaseTypeDefinition3.getCaseFields(),
                USER_ROLES,
                CAN_UPDATE);

            List<CaseTypeDefinition> caseTypeDefinitions = authorisedGetCaseTypesOperation.execute(JURISDICTION_ID, CAN_UPDATE);

            assertThat(caseTypeDefinitions.get(0).getCaseFields(), hasSize(2));
            assertThat(caseTypeDefinitions.get(0).getCaseFields(), hasItems(hasProperty("id", equalTo(CASE_FIELD_ID_3_1)), hasProperty("id", equalTo(CASE_FIELD_ID_3_2))));
        }

        @Test
        @DisplayName("Should return case types with case fields that have matching read access rights")
        void shouldReturnCaseTypesWithReadAccessFieldsForJurisdiction() {
            doReturn(testCaseTypeDefinitions).when(getCaseTypesOperation).execute(JURISDICTION_ID, CAN_READ);
            doReturn(newArrayList(CASE_FIELD_1_2)).when(accessControlService).filterCaseFieldsByAccess(testCaseTypeDefinition1.getCaseFields(),
                                                                                                       USER_ROLES,
                                                                                                       CAN_READ);
            doReturn(newArrayList(CASE_FIELD_3_1, CASE_FIELD_3_3)).when(accessControlService).filterCaseFieldsByAccess(
                testCaseTypeDefinition3.getCaseFields(),
                USER_ROLES,
                CAN_READ);

            List<CaseTypeDefinition> caseTypeDefinitions = authorisedGetCaseTypesOperation.execute(JURISDICTION_ID, CAN_READ);

            assertThat(caseTypeDefinitions.get(0).getCaseFields(), hasSize(1));
            assertThat(caseTypeDefinitions.get(0).getCaseFields(), hasItems(hasProperty("id", equalTo(CASE_FIELD_ID_1_2))));
            assertThat(caseTypeDefinitions.get(1).getCaseFields(), hasSize(2));
            assertThat(caseTypeDefinitions.get(1).getCaseFields(), hasItems(hasProperty("id", equalTo(CASE_FIELD_ID_3_1)),
                                                              hasProperty("id", equalTo(CASE_FIELD_ID_3_3))));
        }
    }

}
