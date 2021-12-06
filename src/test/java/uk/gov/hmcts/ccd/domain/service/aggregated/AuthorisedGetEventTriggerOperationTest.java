package uk.gov.hmcts.ccd.domain.service.aggregated;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProcess;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.CaseAccessMetadata;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    @Mock
    private CaseDataAccessControl caseDataAccessControl;

    private AuthorisedGetEventTriggerOperation authorisedGetEventTriggerOperation;
    private CaseUpdateViewEvent caseEventTrigger;
    private final CaseDetails caseDetails = new CaseDetails();
    private final CaseTypeDefinition caseType = new CaseTypeDefinition();
    private final List<CaseFieldDefinition> caseFields = Lists.newArrayList();
    private final Set<AccessProfile> accessProfiles = createAccessProfiles(Sets.newHashSet(CASEWORKER_DIVORCE,
        CASEWORKER_PROBATE_LOA1,
        CASEWORKER_PROBATE_LOA3));
    private final Set<AccessProfile> createCaseAccessProfiles = createAccessProfiles(Sets.newHashSet(CASEWORKER_DIVORCE,
        CASEWORKER_PROBATE_LOA1,
        CASEWORKER_PROBATE_LOA3,
        GlobalCaseRole.CREATOR.getRole()));
    private final List<CaseEventDefinition> events = Lists.newArrayList();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        authorisedGetEventTriggerOperation = new AuthorisedGetEventTriggerOperation(
            getEventTriggerOperation,
            caseDefinitionRepository,
            caseDetailsRepository,
            caseAccessService,
            accessControlService,
            eventTriggerService,
            draftGateway,
            caseDataAccessControl);
        caseEventTrigger = new CaseUpdateViewEvent();
        caseEventTrigger.setCaseId(CASE_ID);

        caseType.setId(CASE_TYPE_ID);
        caseType.setEvents(events);
        caseType.setCaseFieldDefinitions(caseFields);
        caseDetails.setReference(CASE_REFERENCE_LONG);
        caseDetails.setState(STATE);
        caseDetails.setCaseTypeId(CASE_TYPE_ID);
        caseDetails.setId(CASE_ID);
        when(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).thenReturn(caseType);
        when(caseAccessService.getAccessProfiles(any())).thenReturn(accessProfiles);
        when(caseAccessService.getAccessProfilesByCaseReference(any())).thenReturn(accessProfiles);
        when(caseAccessService.getCaseCreationRoles(CASE_TYPE_ID)).thenReturn(createCaseAccessProfiles);
        when(accessControlService.canAccessCaseTypeWithCriteria(eq(caseType),
                                                                eq(accessProfiles),
                                                                eq(CAN_CREATE))).thenReturn(true);
        when(accessControlService.canAccessCaseTypeWithCriteria(eq(caseType), eq(accessProfiles), eq(CAN_READ)))
            .thenReturn(true);
        when(accessControlService.canAccessCaseEventWithCriteria(eq(EVENT_TRIGGER_ID),
                                                                 eq(events),
                                                                 eq(accessProfiles),
                                                                 eq(CAN_CREATE))).thenReturn(true);
        when(accessControlService.canAccessCaseFieldsWithCriteria(any(JsonNode.class),
                                                                  eq(caseFields),
                                                                  eq(accessProfiles),
                                                                  eq(CAN_CREATE))).thenReturn(true);

        CaseEventDefinition caseEvent = new CaseEventDefinition();
        when(eventTriggerService.findCaseEvent(eq(caseType), eq(EVENT_TRIGGER_ID))).thenReturn(caseEvent);
        when(eventTriggerService.isPreStateValid(eq(STATE), eq(caseEvent))).thenReturn(true);
    }

    private Set<AccessProfile> createAccessProfiles(Set<String> userRoles) {
        return userRoles.stream()
            .map(userRole -> AccessProfile.builder().readOnly(false)
                .accessProfile(userRole)
                .build())
            .collect(Collectors.toSet());
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
                createCaseAccessProfiles,
                CAN_READ);
            doReturn(true).when(accessControlService).canAccessCaseTypeWithCriteria(caseType,
                createCaseAccessProfiles,
                CAN_CREATE);
            doReturn(true).when(accessControlService)
                .canAccessCaseEventWithCriteria(EVENT_TRIGGER_ID,
                    events,
                    createCaseAccessProfiles,
                    CAN_CREATE);
            doReturn(caseEventTrigger).when(accessControlService)
                .filterCaseViewFieldsByAccess(caseEventTrigger,
                    caseFields,
                    createCaseAccessProfiles,
                    CAN_CREATE);

            doReturn(caseEventTrigger).when(accessControlService)
                .updateCollectionDisplayContextParameterByAccess(caseEventTrigger, createCaseAccessProfiles);

            doReturn(new CaseAccessMetadata())
                .when(caseDataAccessControl).generateAccessMetadataWithNoCaseId();
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
                () -> inOrder.verify(caseAccessService).getCaseCreationRoles(CASE_TYPE_ID),
                () -> inOrder.verify(accessControlService).canAccessCaseTypeWithCriteria(eq(caseType),
                                                                                         eq(createCaseAccessProfiles),
                                                                                         eq(CAN_CREATE)),
                () -> inOrder.verify(accessControlService).canAccessCaseEventWithCriteria(eq(EVENT_TRIGGER_ID),
                                                                                          eq(caseType.getEvents()),
                                                                                          eq(createCaseAccessProfiles),
                                                                                          eq(CAN_CREATE)),
                () -> inOrder.verify(getEventTriggerOperation).executeForCaseType(CASE_TYPE_ID,
                                                                                  EVENT_TRIGGER_ID,
                                                                                  IGNORE),
                () -> inOrder.verify(accessControlService).filterCaseViewFieldsByAccess(eq(caseEventTrigger),
                                                                                        eq(caseFields),
                                                                                        eq(createCaseAccessProfiles),
                                                                                        eq(CAN_CREATE)),
                () -> inOrder.verify(accessControlService)
                    .updateCollectionDisplayContextParameterByAccess(eq(caseEventTrigger), eq(createCaseAccessProfiles))
            );
        }

        @Test
        @DisplayName("should fail if no read access on case type")
        void shouldFailIfNoReadAccessOnCaseType() {
            doReturn(false).when(accessControlService).canAccessCaseTypeWithCriteria(caseType,
                createCaseAccessProfiles,
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
                createCaseAccessProfiles,
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
                createCaseAccessProfiles,
                                                                                      CAN_CREATE);

            assertThrows(
                ResourceNotFoundException.class, () ->
                    authorisedGetEventTriggerOperation.executeForCaseType(CASE_TYPE_ID, EVENT_TRIGGER_ID, IGNORE)
            );
        }

        @Test
        @DisplayName("should return Case Access metadata")
        void shouldReturnCaseAccessMetadata() throws JsonProcessingException {
            CaseAccessMetadata caseAccessMetadata = new CaseAccessMetadata();
            caseAccessMetadata.setAccessProcess(AccessProcess.NONE);
            caseAccessMetadata.setAccessGrants(List.of(GrantType.STANDARD));

            doReturn(caseAccessMetadata)
                .when(caseDataAccessControl).generateAccessMetadataWithNoCaseId();

            final CaseUpdateViewEvent output = authorisedGetEventTriggerOperation.executeForCaseType(CASE_TYPE_ID,
                EVENT_TRIGGER_ID,
                IGNORE);

            assertTrue(isCaseAccessMetadataPresentInJson(output));
            assertThat(output.getAccessGrants(), is(GrantType.STANDARD.name()));
            assertThat(output.getAccessProcess(), is(AccessProcess.NONE.name()));
        }

        private boolean isCaseAccessMetadataPresentInJson(CaseUpdateViewEvent caseUpdateViewEvent)
            throws JsonProcessingException {
            ObjectMapper objMapper = new ObjectMapper();
            String jsonString = objMapper.writeValueAsString(caseUpdateViewEvent);
            return jsonString.contains("access_granted") || jsonString.contains("access_process");
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
                accessProfiles,
                                                                                    CAN_READ);
            doReturn(true).when(accessControlService).canAccessCaseTypeWithCriteria(caseType,
                accessProfiles,
                                                                                    CAN_UPDATE);
            doReturn(true).when(accessControlService).canAccessCaseEventWithCriteria(EVENT_TRIGGER_ID,
                                                                                     caseType.getEvents(),
                accessProfiles,
                                                                                     CAN_CREATE);
            doReturn(true).when(accessControlService).canAccessCaseStateWithCriteria(eq(caseDetails.getState()),
                                                                                     eq(caseType),
                                                                                     eq(accessProfiles),
                                                                                     eq(CAN_UPDATE));
            doReturn(caseEventTrigger).when(accessControlService)
                .setReadOnlyOnCaseViewFieldsIfNoAccess(CASE_TYPE_ID,CASE_REFERENCE, EVENT_TRIGGER_ID, caseEventTrigger,
                    caseFields, accessProfiles, CAN_UPDATE);
            doReturn(caseEventTrigger).when(accessControlService)
                .updateCollectionDisplayContextParameterByAccess(caseEventTrigger, accessProfiles);
            doReturn(new CaseAccessMetadata()).when(caseDataAccessControl).generateAccessMetadata(anyString());
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
                () -> inOrder.verify(caseAccessService).getAccessProfilesByCaseReference(CASE_REFERENCE),
                () -> inOrder.verify(accessControlService).canAccessCaseEventWithCriteria(eq(EVENT_TRIGGER_ID),
                                                                                          eq(caseType.getEvents()),
                                                                                          eq(accessProfiles),
                                                                                          eq(CAN_CREATE)),
                () -> inOrder.verify(accessControlService).canAccessCaseStateWithCriteria(eq(caseDetails.getState()),
                                                                                          eq(caseType),
                                                                                          eq(accessProfiles),
                                                                                          eq(CAN_UPDATE)),
                () -> inOrder.verify(getEventTriggerOperation).executeForCase(CASE_REFERENCE,
                                                                              EVENT_TRIGGER_ID,
                                                                              IGNORE),
                () -> inOrder.verify(accessControlService).setReadOnlyOnCaseViewFieldsIfNoAccess(
                    eq(CASE_TYPE_ID),
                    eq(CASE_REFERENCE),
                    eq(EVENT_TRIGGER_ID),
                    eq(caseEventTrigger),
                    eq(caseFields),
                    eq(accessProfiles),
                    eq(CAN_UPDATE)),
                () -> inOrder.verify(accessControlService)
                    .updateCollectionDisplayContextParameterByAccess(eq(caseEventTrigger), eq(accessProfiles))
            );
        }

        @Test
        @DisplayName("should return Case Access metadata")
        void shouldReturnCaseAccessMetadata() throws JsonProcessingException {
            CaseAccessMetadata caseAccessMetadata = new CaseAccessMetadata();
            caseAccessMetadata.setAccessGrants(List.of(GrantType.STANDARD, GrantType.SPECIFIC, GrantType.CHALLENGED));
            caseAccessMetadata.setAccessProcess(AccessProcess.NONE);
            doReturn(caseAccessMetadata).when(caseDataAccessControl).generateAccessMetadata(anyString());

            final CaseUpdateViewEvent output = authorisedGetEventTriggerOperation.executeForCase(CASE_REFERENCE,
                EVENT_TRIGGER_ID,
                IGNORE);

            assertTrue(isCaseAccessMetadataPresentInJson(output));
            assertThat(output.getAccessGrants(),
                is(GrantType.CHALLENGED.name() + "," + GrantType.SPECIFIC + "," + GrantType.STANDARD));
            assertThat(output.getAccessProcess(), is(AccessProcess.NONE.name()));
        }

        @Test
        @DisplayName("should not set CaseUpdateViewEvent json fields if there is no Case Access metadata")
        void shouldNotSetCaseAccessMetadataJson() throws JsonProcessingException {
            final CaseUpdateViewEvent output = authorisedGetEventTriggerOperation.executeForCase(CASE_REFERENCE,
                EVENT_TRIGGER_ID,
                IGNORE);

            assertFalse(isCaseAccessMetadataPresentInJson(output));
            assertNull(output.getAccessGrants());
            assertNull(output.getAccessProcess());
        }

        private boolean isCaseAccessMetadataPresentInJson(CaseUpdateViewEvent caseUpdateViewEvent)
            throws JsonProcessingException {
            ObjectMapper objMapper = new ObjectMapper();
            String jsonString = objMapper.writeValueAsString(caseUpdateViewEvent);
            return jsonString.contains("access_granted") || jsonString.contains("access_process");
        }

        @Test
        @DisplayName("should fail if no read access on case type")
        void shouldFailIfNoReadAccessOnCaseType() {
            doReturn(false).when(accessControlService).canAccessCaseTypeWithCriteria(caseType,
                accessProfiles,
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
                accessProfiles,
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
                accessProfiles,
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
                accessProfiles,
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
