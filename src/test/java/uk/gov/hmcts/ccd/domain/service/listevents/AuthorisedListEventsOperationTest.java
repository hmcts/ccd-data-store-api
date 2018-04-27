package uk.gov.hmcts.ccd.domain.service.listevents;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEvent;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.util.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

class AuthorisedListEventsOperationTest {

    private static final String JURISDICTION_ID = "Probate";
    private static final String CASE_TYPE_ID = "CaseTypeId";
    private final static String CASE_REFERENCE = "999999";

    @Mock
    private ListEventsOperation listEventsOperation;

    @Mock
    private AccessControlService accessControlService;

    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;

    @Mock
    private UserRepository userRepository;

    private AuthorisedListEventsOperation authorisedOperation;
    private CaseDetails caseDetails;
    private List<AuditEvent> classifiedEvents;
    private List<AuditEvent> authorisedEvents;
    private CaseType caseType;
    private static final String CASEWORKER_PROBATE_LOA1 = "caseworker-probate-loa1";
    private static final String CASEWORKER_PROBATE_LOA3 = "caseworker-probate-loa3";
    private static final String CASEWORKER_DIVORCE = "caseworker-divorce-loa3";
    private static final Set<String> USER_ROLES = Sets.newHashSet(CASEWORKER_DIVORCE, CASEWORKER_PROBATE_LOA1, CASEWORKER_PROBATE_LOA3);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        caseType = new CaseType();
        List<CaseEvent> eventsDefinition = newArrayList();
        caseType.setEvents(eventsDefinition);
        caseDetails = new CaseDetails();
        caseDetails.setJurisdiction(JURISDICTION_ID);
        caseDetails.setCaseTypeId(CASE_TYPE_ID);
        classifiedEvents = newArrayList(new AuditEvent(), new AuditEvent());

        doReturn(caseType).when(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);
        doReturn(USER_ROLES).when(userRepository).getUserRoles();
        doReturn(classifiedEvents).when(listEventsOperation).execute(caseDetails);
        doReturn(classifiedEvents).when(listEventsOperation).execute(JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE);

        authorisedEvents = newArrayList(new AuditEvent());

        doReturn(true).when(accessControlService).canAccessCaseTypeWithCriteria(caseType, USER_ROLES, CAN_READ);
        doReturn(authorisedEvents).when(accessControlService).filterCaseAuditEventsByReadAccess(classifiedEvents, eventsDefinition, USER_ROLES);

        authorisedOperation = new AuthorisedListEventsOperation(listEventsOperation, caseDefinitionRepository, accessControlService, userRepository);
    }

    @Test
    @DisplayName("should call decorated implementation")
    void shouldCallDecoratedImplementation() {

        authorisedOperation.execute(caseDetails);

        verify(listEventsOperation).execute(caseDetails);
    }

    @Test
    @DisplayName("should return empty list when decorated implementation returns null")
    void shouldReturnEmptyListInsteadOfNull() {
        doReturn(null).when(listEventsOperation).execute(caseDetails);

        final List<AuditEvent> outputs = authorisedOperation.execute(caseDetails);

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

        assertThrows(ValidationException.class, () -> authorisedOperation.execute(caseDetails));
    }

    @Test
    @DisplayName("should fail if no user roles found")
    void shouldReturnEmptyCaseIfNoUserRolesFound() {
        doReturn(null).when(userRepository).getUserRoles();

        assertThrows(ValidationException.class, () -> authorisedOperation.execute(caseDetails));
    }

    @Test
    @DisplayName("should fail if no user roles found")
    void shouldReturnEmptyCaseIfEmptyUserRolesFound() {
        doReturn(Sets.newHashSet()).when(userRepository).getUserRoles();

        assertThrows(ValidationException.class, () -> authorisedOperation.execute(caseDetails));
    }

    @Test
    @DisplayName("should return empty list if no case read access")
    void shouldReturnEmptyListIfNoCaseReadAccess() {
        doReturn(false).when(accessControlService).canAccessCaseTypeWithCriteria(caseType, USER_ROLES, CAN_READ);

        final List<AuditEvent> outputs = authorisedOperation.execute(caseDetails);

        InOrder inOrder = inOrder(caseDefinitionRepository, userRepository, listEventsOperation, accessControlService);
        assertAll(
            () -> inOrder.verify(listEventsOperation).execute(caseDetails),
            () -> inOrder.verify(caseDefinitionRepository).getCaseType(caseDetails.getCaseTypeId()),
            () -> inOrder.verify(userRepository).getUserRoles(),
            () -> inOrder.verify(accessControlService).canAccessCaseTypeWithCriteria(caseType, USER_ROLES, CAN_READ),
            () -> inOrder.verify(accessControlService, never()).filterCaseAuditEventsByReadAccess(classifiedEvents, caseType.getEvents(), USER_ROLES),
            () -> assertThat(outputs, is(notNullValue())),
            () -> assertThat(outputs, hasSize(0))
        );
    }

    @Test
    @DisplayName("should apply authorization for case details")
    void shouldApplyAuthorisationForCaseDetails() {
        final List<AuditEvent> outputs = authorisedOperation.execute(caseDetails);

        InOrder inOrder = inOrder(caseDefinitionRepository, userRepository, listEventsOperation, accessControlService);
        assertAll(
            () -> inOrder.verify(listEventsOperation).execute(caseDetails),
            () -> inOrder.verify(caseDefinitionRepository).getCaseType(caseDetails.getCaseTypeId()),
            () -> inOrder.verify(userRepository).getUserRoles(),
            () -> inOrder.verify(accessControlService).canAccessCaseTypeWithCriteria(caseType, USER_ROLES, CAN_READ),
            () -> inOrder.verify(accessControlService).filterCaseAuditEventsByReadAccess(classifiedEvents, caseType.getEvents(), USER_ROLES),
            () -> assertThat(outputs, is(authorisedEvents))
        );
    }

    @Test
    @DisplayName("should apply authorization when jurisdiction, case type id and case reference is received")
    void shouldApplyAuthorisationForJurisdictionCaseTypeIdAndCaseReference() {
        final List<AuditEvent> outputs = authorisedOperation.execute(JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE);

        InOrder inOrder = inOrder(caseDefinitionRepository, userRepository, listEventsOperation, accessControlService);
        assertAll(
            () -> inOrder.verify(listEventsOperation).execute(JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE),
            () -> inOrder.verify(caseDefinitionRepository).getCaseType(caseDetails.getCaseTypeId()),
            () -> inOrder.verify(userRepository).getUserRoles(),
            () -> inOrder.verify(accessControlService).canAccessCaseTypeWithCriteria(caseType, USER_ROLES, CAN_READ),
            () -> inOrder.verify(accessControlService).filterCaseAuditEventsByReadAccess(classifiedEvents, caseType.getEvents(), USER_ROLES),
            () -> assertThat(outputs, is(authorisedEvents))
        );
    }
    
}
