package uk.gov.hmcts.ccd.domain.service.getevents;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import static java.util.Collections.singletonList;
import static org.assertj.core.util.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.anyListOf;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

class AuthorisedGetEventsOperationTest {

    private static final String JURISDICTION_ID = "Probate";
    private static final String CASE_TYPE_ID = "CaseTypeId";
    private static final String CASE_REFERENCE = "999999";
    private static final String CASE_REFERENCE_INVALID = "9999991";
    private static final String CASE_REFERENCE_INVALID_NULL = "9999992";
    private static final Long EVENT_ID = 100L;
    private static final String CASE_ID = "12345";

    @Mock
    private GetEventsOperation getEventsOperation;

    @Mock
    private AccessControlService accessControlService;

    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;

    @Mock
    private CaseAccessService caseAccessService;


    private AuthorisedGetEventsOperation authorisedOperation;
    private CaseDetails caseDetails;
    private List<AuditEvent> classifiedEvents;
    private List<AuditEvent> authorisedEvents;
    private CaseTypeDefinition caseType;
    private AuditEvent event;
    private static final String CASEWORKER_PROBATE_LOA1 = "caseworker-probate-loa1";
    private static final String CASEWORKER_PROBATE_LOA3 = "caseworker-probate-loa3";
    private static final String CASEWORKER_DIVORCE = "caseworker-divorce-loa3";
    private static final Set<String> USER_ROLES = Sets.newHashSet(CASEWORKER_DIVORCE, CASEWORKER_PROBATE_LOA1, CASEWORKER_PROBATE_LOA3);
    private static final Set<String> CASE_ROLES = Sets.newHashSet("[LASOLICITOR]");
    private static final Set<String> CASE_USER_ROLES = Sets.union(USER_ROLES, CASE_ROLES.stream().collect(Collectors.toSet()));


    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        caseType = new CaseTypeDefinition();
        List<CaseEventDefinition> eventsDefinition = new ArrayList<>();
        caseType.setEvents(eventsDefinition);
        caseDetails = new CaseDetails();
        caseDetails.setJurisdiction(JURISDICTION_ID);
        caseDetails.setCaseTypeId(CASE_TYPE_ID);
        caseDetails.setId(CASE_ID);
        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setCaseDataId(CASE_ID);
        auditEvent.setCaseTypeId(CASE_TYPE_ID);
        AuditEvent auditEvent1 = new AuditEvent();
        auditEvent1.setCaseDataId(CASE_ID);
        auditEvent1.setCaseTypeId(CASE_TYPE_ID);
        classifiedEvents = newArrayList(auditEvent, auditEvent1);
        event = new AuditEvent();
        event.setCaseDataId(CASE_ID);

        doReturn(caseType).when(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);
        doReturn(CASE_USER_ROLES).when(caseAccessService).getAccessRoles(anyString());
        doReturn(classifiedEvents).when(getEventsOperation).getEvents(caseDetails);
        doReturn(classifiedEvents).when(getEventsOperation).getEvents(JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE);
        doReturn(classifiedEvents).when(getEventsOperation).getEvents(CASE_REFERENCE);
        doReturn(Lists.newArrayList()).when(getEventsOperation).getEvents(CASE_REFERENCE_INVALID);
        doReturn(Lists.newArrayList()).when(getEventsOperation).getEvents(CASE_REFERENCE_INVALID_NULL);

        authorisedEvents = newArrayList(new AuditEvent());

        doReturn(true).when(accessControlService).canAccessCaseTypeWithCriteria(caseType, CASE_USER_ROLES, CAN_READ);
        doReturn(authorisedEvents).when(accessControlService).filterCaseAuditEventsByReadAccess(classifiedEvents, eventsDefinition, CASE_USER_ROLES);

        authorisedOperation = new AuthorisedGetEventsOperation(getEventsOperation, caseDefinitionRepository,
            accessControlService, caseAccessService);
    }

    @Test
    @DisplayName("should call decorated implementation")
    void shouldCallDecoratedImplementation() {

        authorisedOperation.getEvents(caseDetails);

        verify(getEventsOperation).getEvents(caseDetails);
    }

    @Test
    @DisplayName("should return empty list when decorated implementation returns null")
    void shouldReturnEmptyListInsteadOfNull() {
        doReturn(null).when(getEventsOperation).getEvents(caseDetails);

        final List<AuditEvent> outputs = authorisedOperation.getEvents(caseDetails);

        assertAll(
            () -> assertThat(outputs, is(notNullValue())),
            () -> assertThat(outputs, hasSize(0)),
            () -> verify(accessControlService, never()).filterCaseAuditEventsByReadAccess(any(), any(), any())
        );
    }


    @Test
    @DisplayName("should fail if no case type found")
    void shouldReturnEmptyCaseIfNoCaseTypeFound() {
        doReturn(null).when(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);

        assertThrows(ValidationException.class, () -> authorisedOperation.getEvents(caseDetails));
    }

    @Test
    @DisplayName("should fail if no user roles and case roles found")
    void shouldReturnEmptyCaseIfNoUserRolesFound() {
        doReturn(Sets.newHashSet()).when(caseAccessService).getAccessRoles(CASE_ID);

        assertThrows(ValidationException.class, () -> authorisedOperation.getEvents(caseDetails));
    }

    @Test
    @DisplayName("should fail if no user roles found")
    void shouldReturnEmptyCaseIfEmptyUserRolesFound() {
        doReturn(null).when(caseAccessService).getAccessRoles(CASE_ID);

        assertThrows(ValidationException.class, () -> authorisedOperation.getEvents(caseDetails));
    }

    @Test
    @DisplayName("should return empty list if no case read access")
    void shouldReturnEmptyListIfNoCaseReadAccess() {
        doReturn(false).when(accessControlService).canAccessCaseTypeWithCriteria(caseType, CASE_USER_ROLES, CAN_READ);

        final List<AuditEvent> outputs = authorisedOperation.getEvents(caseDetails);

        InOrder inOrder = inOrder(caseDefinitionRepository, getEventsOperation, accessControlService, caseAccessService);
        assertAll(() -> inOrder.verify(getEventsOperation).getEvents(caseDetails),
            () -> inOrder.verify(caseDefinitionRepository).getCaseType(caseDetails.getCaseTypeId()),
            () -> inOrder.verify(caseAccessService).getAccessRoles(CASE_ID),
            () -> inOrder.verify(accessControlService).canAccessCaseTypeWithCriteria(caseType, CASE_USER_ROLES, CAN_READ),
            () -> inOrder.verify(accessControlService, never()).filterCaseAuditEventsByReadAccess(classifiedEvents, caseType.getEvents(), USER_ROLES),
            () -> assertThat(outputs, is(notNullValue())),
            () -> assertThat(outputs, hasSize(0))
        );
    }

    @Test
    @DisplayName("should apply authorization for case details")
    void shouldApplyAuthorisationForCaseDetails() {
        final List<AuditEvent> outputs = authorisedOperation.getEvents(caseDetails);

        InOrder inOrder = inOrder(caseDefinitionRepository, getEventsOperation, accessControlService, caseAccessService);
        assertAll(() -> inOrder.verify(getEventsOperation).getEvents(caseDetails),
            () -> inOrder.verify(caseDefinitionRepository).getCaseType(caseDetails.getCaseTypeId()),
            () -> inOrder.verify(caseAccessService).getAccessRoles(CASE_ID),
            () -> inOrder.verify(accessControlService).canAccessCaseTypeWithCriteria(caseType, CASE_USER_ROLES, CAN_READ),
            () -> inOrder.verify(accessControlService).filterCaseAuditEventsByReadAccess(classifiedEvents, caseType.getEvents(), CASE_USER_ROLES),
            () -> assertThat(outputs, is(authorisedEvents))
        );
    }

    @Test
    @DisplayName("should apply authorization when jurisdiction, case type id and case reference is received")
    void shouldApplyAuthorisationForJurisdictionCaseTypeIdAndCaseReference() {
        final List<AuditEvent> outputs = authorisedOperation.getEvents(JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE);

        InOrder inOrder = inOrder(caseDefinitionRepository, getEventsOperation, accessControlService, caseAccessService);
        assertAll(() -> inOrder.verify(getEventsOperation).getEvents(JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE),
            () -> inOrder.verify(caseDefinitionRepository).getCaseType(caseDetails.getCaseTypeId()),
            () -> inOrder.verify(caseAccessService).getAccessRoles(CASE_ID),
            () -> inOrder.verify(accessControlService).canAccessCaseTypeWithCriteria(caseType, CASE_USER_ROLES, CAN_READ),
            () -> inOrder.verify(accessControlService).filterCaseAuditEventsByReadAccess(classifiedEvents, caseType.getEvents(), CASE_USER_ROLES),
            () -> assertThat(outputs, is(authorisedEvents))
        );
    }

    @Test
    @DisplayName("should apply authorization when case reference is received")
    void shouldApplyAuthorisationForCaseReference() {
        final List<AuditEvent> outputs = authorisedOperation.getEvents(CASE_REFERENCE);

        InOrder inOrder = inOrder(caseDefinitionRepository, getEventsOperation, accessControlService, caseAccessService);
        assertAll(() -> inOrder.verify(getEventsOperation).getEvents(CASE_REFERENCE),
            () -> inOrder.verify(caseDefinitionRepository).getCaseType(caseDetails.getCaseTypeId()),
            () -> inOrder.verify(caseAccessService).getAccessRoles(CASE_ID),
            () -> inOrder.verify(accessControlService).canAccessCaseTypeWithCriteria(caseType, CASE_USER_ROLES, CAN_READ),
            () -> inOrder.verify(accessControlService).filterCaseAuditEventsByReadAccess(classifiedEvents, caseType.getEvents(), CASE_USER_ROLES),
            () -> assertThat(outputs, is(authorisedEvents))
        );
    }

    @Test
    @DisplayName("should return empty events list when no event data found")
    void shouldReturnEmptyListForCaseReference() {
        final List<AuditEvent> outputs = authorisedOperation.getEvents(CASE_REFERENCE_INVALID);

        assertAll(
            () -> assertThat(outputs, is(notNullValue())),
            () -> assertThat(outputs.size(), is(0))
        );
    }

    @Test
    @DisplayName("should return empty events list when event data returns null")
    void shouldReturnEmptyListWhenGetEventsReturnsNull() {
        final List<AuditEvent> outputs = authorisedOperation.getEvents(CASE_REFERENCE_INVALID_NULL);

        assertAll(
            () -> assertThat(outputs, is(notNullValue())),
            () -> assertThat(outputs.size(), is(0))
        );
    }

    @Test
    @DisplayName("should apply authorization when jurisdiction, case type id and event id is received")
    void shouldApplyAuthorisationForJurisdictionCaseTypeIdAndEvent() {
        doReturn(Optional.of(event)).when(getEventsOperation).getEvent(JURISDICTION_ID, CASE_TYPE_ID, EVENT_ID);
        doReturn(singletonList(event)).when(accessControlService)
            .filterCaseAuditEventsByReadAccess(anyListOf(AuditEvent.class), anyListOf(CaseEventDefinition.class), eq(CASE_USER_ROLES));

        Optional<AuditEvent> optionalAuditEvent = authorisedOperation.getEvent(JURISDICTION_ID, CASE_TYPE_ID, EVENT_ID);

        assertThat(optionalAuditEvent.isPresent(), is(true));
        AuditEvent output = optionalAuditEvent.get();
        InOrder inOrder = inOrder(caseDefinitionRepository, getEventsOperation, accessControlService, caseAccessService);
        assertAll(
            () -> inOrder.verify(getEventsOperation).getEvent(JURISDICTION_ID, CASE_TYPE_ID, EVENT_ID),
            () -> inOrder.verify(caseDefinitionRepository).getCaseType(caseDetails.getCaseTypeId()),
            () -> inOrder.verify(caseAccessService).getAccessRoles(CASE_ID),
            () -> inOrder.verify(accessControlService).canAccessCaseTypeWithCriteria(caseType, CASE_USER_ROLES, CAN_READ),
            () -> inOrder.verify(accessControlService)
                .filterCaseAuditEventsByReadAccess(anyListOf(AuditEvent.class), eq(caseType.getEvents()), eq(CASE_USER_ROLES)),
            () -> assertThat(output, is(event))
        );
    }
}
