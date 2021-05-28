package uk.gov.hmcts.ccd.domain.service.aggregated;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.caseaccess.GlobalCaseRole;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseUpdateViewEvent;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_CREATE;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_UPDATE;

class AuthorisedGetEventTriggerOperationTest {

    private static final String CASEWORKER_PROBATE_LOA1 = "caseworker-probate-loa1";
    private static final String CASEWORKER_PROBATE_LOA3 = "caseworker-probate-loa3";
    private static final String CASEWORKER_DIVORCE = "caseworker-divorce-loa3";

    private static final String EVENT_TRIGGER_ID = "testEventTriggerId";
    private static final String CASE_REFERENCE = "1234567891012345";
    private static final String CASE_ID = "26";
    private static final Long CASE_REFERENCE_LONG = 1234567891012345L;
    private static final String CASE_TYPE_ID = "Grant";
    private static final String STATE = "CaseCreated";
    private static final Boolean IGNORE = Boolean.TRUE;

    @Mock
    private GetEventTriggerOperation getEventTriggerOperation;

    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;

    @Mock
    private CaseDetailsRepository caseDetailsRepository;

    @Mock
    private AccessControlService accessControlService;

    @Mock
    private CaseAccessService caseAccessService;

    @Mock
    private DraftGateway draftGateway;

    @Mock
    private EventTriggerService eventTriggerService;

    private AuthorisedGetEventTriggerOperation authorisedGetEventTriggerOperation;
    private CaseUpdateViewEvent caseEventTrigger;
    private final CaseDetails caseDetails = new CaseDetails();
    private final CaseTypeDefinition caseType = new CaseTypeDefinition();
    private final List<CaseFieldDefinition> caseFields = Lists.newArrayList();
    private final Set<String> userRoles = Sets.newHashSet(CASEWORKER_DIVORCE,
        CASEWORKER_PROBATE_LOA1,
        CASEWORKER_PROBATE_LOA3);
    private final Set<String> createCaseUserRoles = Sets.newHashSet(CASEWORKER_DIVORCE,
        CASEWORKER_PROBATE_LOA1,
        CASEWORKER_PROBATE_LOA3,
        GlobalCaseRole.CREATOR.getRole());
    private final List<CaseEventDefinition> events = Lists.newArrayList();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        authorisedGetEventTriggerOperation = new AuthorisedGetEventTriggerOperation(
            getEventTriggerOperation,
            caseDefinitionRepository,
            caseDetailsRepository,
            caseAccessService,
            accessControlService,
            eventTriggerService,
            draftGateway);
        caseEventTrigger = new CaseUpdateViewEvent();

        caseType.setId(CASE_TYPE_ID);
        caseType.setEvents(events);
        caseType.setCaseFieldDefinitions(caseFields);
        caseDetails.setReference(CASE_REFERENCE_LONG);
        caseDetails.setState(STATE);
        caseDetails.setCaseTypeId(CASE_TYPE_ID);
        caseDetails.setId(CASE_ID);
        when(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).thenReturn(caseType);
        when(caseAccessService.getUserRoles()).thenReturn(userRoles);
        when(caseAccessService.getCaseCreationRoles()).thenReturn(createCaseUserRoles);
        when(accessControlService.canAccessCaseTypeWithCriteria(eq(caseType),
                                                                eq(userRoles),
                                                                eq(CAN_CREATE))).thenReturn(true);
        when(accessControlService.canAccessCaseTypeWithCriteria(eq(caseType), eq(userRoles), eq(CAN_READ)))
            .thenReturn(true);
        when(accessControlService.canAccessCaseEventWithCriteria(eq(EVENT_TRIGGER_ID),
                                                                 eq(events),
                                                                 eq(userRoles),
                                                                 eq(CAN_CREATE))).thenReturn(true);
        when(accessControlService.canAccessCaseFieldsWithCriteria(any(JsonNode.class),
                                                                  eq(caseFields),
                                                                  eq(userRoles),
                                                                  eq(CAN_CREATE))).thenReturn(true);

        CaseEventDefinition caseEvent = new CaseEventDefinition();
        when(eventTriggerService.findCaseEvent(eq(caseType), eq(EVENT_TRIGGER_ID))).thenReturn(caseEvent);
        when(eventTriggerService.isPreStateValid(eq(STATE), eq(caseEvent))).thenReturn(true);
    }

    @Nested
    @DisplayName("for case type")
    class ForCaseType {

        @BeforeEach
        void setUp() {
            doReturn(caseEventTrigger).when(getEventTriggerOperation).executeForCaseType(CASE_TYPE_ID,
                                                                                         EVENT_TRIGGER_ID,
                                                                                         IGNORE);
            doReturn(true).when(accessControlService).canAccessCaseTypeWithCriteria(caseType,
                                                                                    createCaseUserRoles,
                                                                                    CAN_READ);
            doReturn(true).when(accessControlService).canAccessCaseTypeWithCriteria(caseType,
                                                                                    createCaseUserRoles,
                                                                                    CAN_CREATE);
            doReturn(true).when(accessControlService).canAccessCaseEventWithCriteria(EVENT_TRIGGER_ID,
                                                                                     events,
                                                                                     createCaseUserRoles,
                                                                                     CAN_CREATE);
            doReturn(caseEventTrigger).when(accessControlService)
                .setReadOnlyOnCaseViewFieldsIfNoAccess(caseEventTrigger, caseFields, createCaseUserRoles, CAN_CREATE);
            doReturn(caseEventTrigger).when(accessControlService).filterCaseViewFieldsByAccess(caseEventTrigger,
                                                                                               caseFields,
                                                                                               createCaseUserRoles,
                                                                                               CAN_CREATE);
            doReturn(caseEventTrigger).when(accessControlService)
                .updateCollectionDisplayContextParameterByAccess(caseEventTrigger, createCaseUserRoles);
        }

        @Test
        @DisplayName("should call decorated get event trigger operation as is")
        void shouldCallDecoratedGetEventTriggerOperation() {

            final CaseUpdateViewEvent output = authorisedGetEventTriggerOperation.executeForCaseType(CASE_TYPE_ID,
                                                                                                  EVENT_TRIGGER_ID,
                                                                                                  IGNORE);

            assertAll(
                () -> assertThat(output, sameInstance(caseEventTrigger)),
                () -> verify(getEventTriggerOperation).executeForCaseType(CASE_TYPE_ID,
                                                                          EVENT_TRIGGER_ID,
                                                                          IGNORE)
            );
        }

        @Test
        @DisplayName("should return event trigger and perform operations in order")
        void shouldReturnEventTriggerAndPerformOperationsInOrder() {

            final CaseUpdateViewEvent output = authorisedGetEventTriggerOperation.executeForCaseType(CASE_TYPE_ID,
                                                                                                  EVENT_TRIGGER_ID,
                                                                                                  IGNORE);

            InOrder inOrder = inOrder(caseDefinitionRepository,
                                      caseAccessService,
                                      accessControlService,
                                      getEventTriggerOperation);
            assertAll(
                () -> assertThat(output, sameInstance(caseEventTrigger)),
                () -> inOrder.verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID),
                () -> inOrder.verify(caseAccessService).getCaseCreationRoles(),
                () -> inOrder.verify(accessControlService).canAccessCaseTypeWithCriteria(eq(caseType),
                                                                                         eq(createCaseUserRoles),
                                                                                         eq(CAN_CREATE)),
                () -> inOrder.verify(accessControlService).canAccessCaseEventWithCriteria(eq(EVENT_TRIGGER_ID),
                                                                                          eq(caseType.getEvents()),
                                                                                          eq(createCaseUserRoles),
                                                                                          eq(CAN_CREATE)),
                () -> inOrder.verify(getEventTriggerOperation).executeForCaseType(CASE_TYPE_ID,
                                                                                  EVENT_TRIGGER_ID,
                                                                                  IGNORE),
                () -> inOrder.verify(accessControlService).filterCaseViewFieldsByAccess(eq(caseEventTrigger),
                                                                                        eq(caseFields),
                                                                                        eq(createCaseUserRoles),
                                                                                        eq(CAN_CREATE)),
                () -> inOrder.verify(accessControlService)
                    .updateCollectionDisplayContextParameterByAccess(eq(caseEventTrigger), eq(createCaseUserRoles))
            );
        }

        @Test
        @DisplayName("should fail if no read access on case type")
        void shouldFailIfNoReadAccessOnCaseType() {
            doReturn(false).when(accessControlService).canAccessCaseTypeWithCriteria(caseType,
                                                                                     createCaseUserRoles,
                                                                                     CAN_READ);

            assertThrows(
                ResourceNotFoundException.class, () ->
                    authorisedGetEventTriggerOperation.executeForCaseType(CASE_TYPE_ID, EVENT_TRIGGER_ID, IGNORE)
            );
        }

        @Test
        @DisplayName("should fail if no create access on case type")
        void shouldFailIfNoCreateAccessOnCaseType() {
            doReturn(false).when(accessControlService).canAccessCaseTypeWithCriteria(caseType,
                                                                                     createCaseUserRoles,
                                                                                     CAN_CREATE);

            assertThrows(
                ResourceNotFoundException.class, () ->
                    authorisedGetEventTriggerOperation.executeForCaseType(CASE_TYPE_ID, EVENT_TRIGGER_ID, IGNORE)
            );
        }

        @Test
        @DisplayName("should fail if no create access on case event")
        void shouldFailIfNoCreateAccessOnCaseEvent() {
            doReturn(false).when(accessControlService).canAccessCaseEventWithCriteria(EVENT_TRIGGER_ID,
                                                                                      events,
                                                                                      createCaseUserRoles,
                                                                                      CAN_CREATE);

            assertThrows(
                ResourceNotFoundException.class, () ->
                    authorisedGetEventTriggerOperation.executeForCaseType(CASE_TYPE_ID, EVENT_TRIGGER_ID, IGNORE)
            );
        }
    }

    @Nested
    @DisplayName("for case")
    class ForCase {

        @BeforeEach
        void setUp() {
            doReturn(Optional.of(caseDetails)).when(caseDetailsRepository).findByReference(CASE_REFERENCE);

            doReturn(caseEventTrigger).when(getEventTriggerOperation).executeForCase(CASE_REFERENCE,
                                                                                     EVENT_TRIGGER_ID,
                                                                                     IGNORE);
            doReturn(caseDetails).when(caseDetailsRepository).findByReference(CASE_REFERENCE_LONG);
            doReturn(true).when(accessControlService).canAccessCaseTypeWithCriteria(caseType,
                                                                                    userRoles,
                                                                                    CAN_READ);
            doReturn(true).when(accessControlService).canAccessCaseTypeWithCriteria(caseType,
                                                                                    userRoles,
                                                                                    CAN_UPDATE);
            doReturn(true).when(accessControlService).canAccessCaseEventWithCriteria(EVENT_TRIGGER_ID,
                                                                                     caseType.getEvents(),
                                                                                     userRoles,
                                                                                     CAN_CREATE);
            doReturn(true).when(accessControlService).canAccessCaseStateWithCriteria(eq(caseDetails.getState()),
                                                                                     eq(caseType),
                                                                                     eq(userRoles),
                                                                                     eq(CAN_UPDATE));
            doReturn(caseEventTrigger).when(accessControlService)
                .setReadOnlyOnCaseViewFieldsIfNoAccess(caseEventTrigger, caseFields, userRoles, CAN_UPDATE);
            doReturn(caseEventTrigger).when(accessControlService)
                .updateCollectionDisplayContextParameterByAccess(caseEventTrigger, userRoles);
        }

        @Test
        @DisplayName("should call decorated get event trigger operation as is")
        void shouldCallDecoratedGetEventTriggerOperation() {

            final CaseUpdateViewEvent output = authorisedGetEventTriggerOperation.executeForCase(CASE_REFERENCE,
                                                                                              EVENT_TRIGGER_ID,
                                                                                              IGNORE);

            assertAll(
                () -> assertThat(output, sameInstance(caseEventTrigger)),
                () -> verify(getEventTriggerOperation).executeForCase(CASE_REFERENCE,
                                                                      EVENT_TRIGGER_ID,
                                                                      IGNORE)
            );
        }

        @Test
        @DisplayName("should return event trigger and perform operations in order")
        void shouldReturnEventTriggerAndPerformOperationsInOrder() {

            final CaseUpdateViewEvent output = authorisedGetEventTriggerOperation.executeForCase(CASE_REFERENCE,
                                                                                              EVENT_TRIGGER_ID,
                                                                                              IGNORE);

            InOrder inOrder = inOrder(caseDefinitionRepository,
                                      caseAccessService,
                                      accessControlService,
                                      getEventTriggerOperation);
            assertAll(
                () -> assertThat(output, sameInstance(caseEventTrigger)),
                () -> inOrder.verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID),
                () -> inOrder.verify(caseAccessService).getUserRoles(),
                () -> inOrder.verify(accessControlService).canAccessCaseEventWithCriteria(eq(EVENT_TRIGGER_ID),
                                                                                          eq(caseType.getEvents()),
                                                                                          eq(userRoles),
                                                                                          eq(CAN_CREATE)),
                () -> inOrder.verify(accessControlService).canAccessCaseStateWithCriteria(eq(caseDetails.getState()),
                                                                                          eq(caseType),
                                                                                          eq(userRoles),
                                                                                          eq(CAN_UPDATE)),
                () -> inOrder.verify(getEventTriggerOperation).executeForCase(CASE_REFERENCE,
                                                                              EVENT_TRIGGER_ID,
                                                                              IGNORE),
                () -> inOrder.verify(accessControlService).setReadOnlyOnCaseViewFieldsIfNoAccess(eq(caseEventTrigger),
                                                                                                 eq(caseFields),
                                                                                                 eq(userRoles),
                                                                                                 eq(CAN_UPDATE)),
                () -> inOrder.verify(accessControlService)
                    .updateCollectionDisplayContextParameterByAccess(eq(caseEventTrigger), eq(userRoles))
            );
        }

        @Test
        @DisplayName("should fail if no read access on case type")
        void shouldFailIfNoReadAccessOnCaseType() {
            doReturn(false).when(accessControlService).canAccessCaseTypeWithCriteria(caseType,
                                                                                     userRoles,
                                                                                     CAN_READ);

            assertThrows(
                ResourceNotFoundException.class, () ->
                    authorisedGetEventTriggerOperation.executeForCase(CASE_REFERENCE, EVENT_TRIGGER_ID, IGNORE)
            );
        }

        @Test
        @DisplayName("should fail if no update access on case type")
        void shouldFailIfNoUpdateAccessOnCaseType() {
            doReturn(false).when(accessControlService).canAccessCaseTypeWithCriteria(caseType,
                                                                                     userRoles,
                                                                                     CAN_UPDATE);

            assertThrows(
                ResourceNotFoundException.class, () ->
                    authorisedGetEventTriggerOperation.executeForCase(CASE_REFERENCE, EVENT_TRIGGER_ID, IGNORE)
            );
        }

        @Test
        @DisplayName("should fail if no create access on case event")
        void shouldFailIfNoCreateAccessOnCaseEvent() {
            doReturn(false).when(accessControlService).canAccessCaseEventWithCriteria(EVENT_TRIGGER_ID,
                                                                                      caseType.getEvents(),
                                                                                      userRoles,
                                                                                      CAN_CREATE);

            assertThrows(
                ResourceNotFoundException.class, () ->
                    authorisedGetEventTriggerOperation.executeForCase(CASE_REFERENCE, EVENT_TRIGGER_ID, IGNORE)
            );
        }

        @Test
        @DisplayName("should fail if no create access on case event")
        void shouldFailIfNoUpdateAccessOnCaseState() {
            doReturn(false).when(accessControlService).canAccessCaseStateWithCriteria(caseDetails.getState(),
                                                                                      caseType,
                                                                                      userRoles,
                                                                                      CAN_UPDATE);

            assertThrows(
                ResourceNotFoundException.class, () ->
                    authorisedGetEventTriggerOperation.executeForCase(CASE_REFERENCE, EVENT_TRIGGER_ID, IGNORE)
            );
        }

        @Test
        @DisplayName("should fail if case reference is not found")
        void shouldThrowExceptionIfCaseReferenceNotFound() {
            doReturn(Optional.empty()).when(caseDetailsRepository).findByReference(CASE_REFERENCE);
            assertThrows(
                ResourceNotFoundException.class, () ->
                    authorisedGetEventTriggerOperation.executeForCase(CASE_REFERENCE, EVENT_TRIGGER_ID, IGNORE)
            );
        }

        @Test
        @DisplayName("should fail if case reference is invalid")
        void shouldThrowExceptionIfCaseReferenceInvalid() {
            doThrow(NumberFormatException.class).when(caseDetailsRepository).findByReference("invalidReference");
            assertThrows(
                BadRequestException.class, () ->
                    authorisedGetEventTriggerOperation.executeForCase("invalidReference", EVENT_TRIGGER_ID,
                        IGNORE)
            );
        }

    }
}
