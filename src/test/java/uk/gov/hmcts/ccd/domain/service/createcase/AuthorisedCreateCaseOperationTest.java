package uk.gov.hmcts.ccd.domain.service.createcase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.assertj.core.util.Lists;
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
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.ccd.domain.model.std.EventBuilder.anEvent;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_CREATE;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.newCaseDataContent;

class AuthorisedCreateCaseOperationTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final String CASEWORKER_PROBATE_LOA1 = "caseworker-probate-loa1";
    private static final String CASEWORKER_PROBATE_LOA3 = "caseworker-probate-loa3";
    private static final String CASEWORKER_DIVORCE = "caseworker-divorce-loa3";

    private static final String UID = "123";
    private static final String JURISDICTION_ID = "Probate";
    private static final String CASE_TYPE_ID = "Grant";
    private static final Event EVENT = anEvent().build();
    private static final String EVENT_ID = "EVENT_ID";
    private static final Map<String, JsonNode> DATA = new HashMap<>();
    private static final String TOKEN = "JwtToken";
    private static final CaseDataContent EVENT_DATA = newCaseDataContent().withEvent(EVENT).withData(DATA).withToken(TOKEN).build();
    private static final Boolean IGNORE = Boolean.TRUE;
    private static final CaseDataContent NULL_EVENT_DATA = null;

    @Mock
    private CreateCaseOperation classifiedCreateCaseOperation;

    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;

    @Mock
    private AccessControlService accessControlService;

    @Mock
    private UserRepository userRepository;

    private AuthorisedCreateCaseOperation authorisedCreateCaseOperation;
    private CaseDetails classifiedCase;
    private final CaseType caseType = new CaseType();
    private final List<CaseField> caseFields = Lists.newArrayList();
    private final Set<String> userRoles = Sets.newHashSet(CASEWORKER_DIVORCE, CASEWORKER_PROBATE_LOA1, CASEWORKER_PROBATE_LOA3);
    private final List<CaseEvent> events = Lists.newArrayList();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        authorisedCreateCaseOperation = new AuthorisedCreateCaseOperation(
            classifiedCreateCaseOperation, caseDefinitionRepository, accessControlService, userRepository);
        classifiedCase = new CaseDetails();
        classifiedCase.setData(Maps.newHashMap());
        EVENT.setEventId(EVENT_ID);
        doReturn(classifiedCase).when(classifiedCreateCaseOperation).createCaseDetails(UID,
                                                                                       JURISDICTION_ID,
                                                                                       CASE_TYPE_ID,
                                                                                       EVENT_DATA,
                                                                                       IGNORE);
        JsonNode authorisedCaseNode = MAPPER.createObjectNode();
        caseType.setEvents(events);
        caseType.setCaseFields(caseFields);
        when(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).thenReturn(caseType);
        when(userRepository.getUserRoles()).thenReturn(userRoles);
        when(accessControlService.canAccessCaseTypeWithCriteria(eq(caseType), eq(userRoles), eq(CAN_CREATE))).thenReturn(true);
        when(accessControlService.canAccessCaseTypeWithCriteria(eq(caseType), eq(userRoles), eq(CAN_READ))).thenReturn(true);
        when(accessControlService.canAccessCaseEventWithCriteria(eq(EVENT_ID), eq(events), eq(userRoles), eq(CAN_CREATE))).thenReturn(true);
        when(accessControlService.canAccessCaseFieldsWithCriteria(any(JsonNode.class), eq(caseFields), eq(userRoles), eq(CAN_CREATE))).thenReturn(true);
        when(accessControlService.filterCaseFieldsByAccess(any(JsonNode.class), eq(caseFields), eq(userRoles), eq(CAN_READ))).thenReturn(authorisedCaseNode);
    }

    @Test
    @DisplayName("should call decorated operation")
    void shouldCallDecoratedOperation() {

        authorisedCreateCaseOperation.createCaseDetails(UID,
                                                        JURISDICTION_ID,
                                                        CASE_TYPE_ID,
                                                        EVENT_DATA,
                                                        IGNORE);

        verify(classifiedCreateCaseOperation).createCaseDetails(UID,
                                                                JURISDICTION_ID,
                                                                CASE_TYPE_ID,
                                                                EVENT_DATA,
                                                                IGNORE);
    }

    @Test
    @DisplayName("should return null when decorated operation returns null")
    void shouldReturnNullWhenOperationReturnsNull() {
        doReturn(null).when(classifiedCreateCaseOperation).createCaseDetails(UID,
                                                                             JURISDICTION_ID,
                                                                             CASE_TYPE_ID,
                                                                             EVENT_DATA,
                                                                             IGNORE);

        final CaseDetails output = authorisedCreateCaseOperation.createCaseDetails(UID,
                                                                                   JURISDICTION_ID,
                                                                                   CASE_TYPE_ID,
                                                                                   EVENT_DATA,
                                                                                   IGNORE);
        assertAll(
            () -> assertThat(output, is(nullValue())),
            () -> verify(accessControlService, never()).canAccessCaseTypeWithCriteria(eq(caseType), eq(userRoles), eq(CAN_READ)),
            () -> verify(accessControlService, never()).filterCaseFieldsByAccess(any(JsonNode.class), eq(caseFields), eq(userRoles), eq(CAN_READ))
        );
    }

    @Test
    @DisplayName("should return authorised case detail if relevant create and read access granted")
    void shouldReturnAuthorisedCaseDetailsIfCreateAndReadAccessGranted() {

        final CaseDetails output = authorisedCreateCaseOperation.createCaseDetails(UID,
                                                                                   JURISDICTION_ID,
                                                                                   CASE_TYPE_ID,
                                                                                   EVENT_DATA,
                                                                                   IGNORE);

        InOrder inOrder = inOrder(caseDefinitionRepository, userRepository, classifiedCreateCaseOperation, accessControlService);
        assertAll(
            () -> assertThat(output, sameInstance(classifiedCase)),
            () -> inOrder.verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID),
            () -> inOrder.verify(userRepository).getUserRoles(),
            () -> inOrder.verify(accessControlService).canAccessCaseTypeWithCriteria(eq(caseType), eq(userRoles), eq(CAN_CREATE)),
            () -> inOrder.verify(accessControlService).canAccessCaseEventWithCriteria(eq(EVENT_ID), eq(events), eq(userRoles), eq(CAN_CREATE)),
            () -> inOrder.verify(accessControlService).canAccessCaseFieldsWithCriteria(any(JsonNode.class), eq(caseFields), eq(userRoles), eq(CAN_CREATE)),
            () -> inOrder.verify(classifiedCreateCaseOperation).createCaseDetails(UID, JURISDICTION_ID, CASE_TYPE_ID, EVENT_DATA, IGNORE),
            () -> inOrder.verify(accessControlService).canAccessCaseTypeWithCriteria(eq(caseType), eq(userRoles), eq(CAN_READ)),
            () -> inOrder.verify(accessControlService, times(2)).filterCaseFieldsByAccess(any(JsonNode.class), eq(caseFields), eq(userRoles), eq(CAN_READ))
        );
    }

    @Test
    @DisplayName("should fail if null case type")
    void shouldFailIfNullCaseTypeFound() {

        doReturn(null).when(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);

        assertThrows(ValidationException.class, () -> authorisedCreateCaseOperation.createCaseDetails(UID,
                                                                                                      JURISDICTION_ID,
                                                                                                      CASE_TYPE_ID,
                                                                                                      EVENT_DATA,
                                                                                                      IGNORE));
    }

    @Test
    @DisplayName("should fail if case type not found")
    void shouldFailIfNoCaseTypeFound() {

        doThrow(ResourceNotFoundException.class).when(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);

        assertThrows(ResourceNotFoundException.class, () -> authorisedCreateCaseOperation.createCaseDetails(UID,
                                                                                                            JURISDICTION_ID,
                                                                                                            CASE_TYPE_ID,
                                                                                                            EVENT_DATA,
                                                                                                            IGNORE));
    }

    @Test
    @DisplayName("should fail if user roles not found")
    void shouldFailIfNoUserRolesFound() {

        doReturn(null).when(userRepository).getUserRoles();

        assertThrows(ValidationException.class, () -> authorisedCreateCaseOperation.createCaseDetails(UID,
                                                                                                      JURISDICTION_ID,
                                                                                                      CASE_TYPE_ID,
                                                                                                      EVENT_DATA,
                                                                                                      IGNORE));
    }

    @Test
    @DisplayName("should fail if no create case access")
    void shouldFailIfNoCreateCaseAccess() {

        when(accessControlService.canAccessCaseTypeWithCriteria(eq(caseType), eq(userRoles), eq(CAN_CREATE))).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> authorisedCreateCaseOperation.createCaseDetails(UID,
                                                                                                            JURISDICTION_ID,
                                                                                                            CASE_TYPE_ID,
                                                                                                            EVENT_DATA,
                                                                                                            IGNORE));
    }

    @Test
    @DisplayName("should fail if no data provided")
    void shouldFailIfNoDataProvided() {

        assertThrows(ValidationException.class, () -> authorisedCreateCaseOperation.createCaseDetails(UID,
                                                                                                            JURISDICTION_ID,
                                                                                                            CASE_TYPE_ID,
                                                                                                            NULL_EVENT_DATA,
                                                                                                            IGNORE));
    }

    @Test
    @DisplayName("should fail if no event provided")
    void shouldFailIfNoEventProvided() {
        assertThrows(ResourceNotFoundException.class, () -> authorisedCreateCaseOperation.createCaseDetails(UID,
                                                                                                            JURISDICTION_ID,
                                                                                                            CASE_TYPE_ID,
                                                                                                            newCaseDataContent().build(),
                                                                                                            IGNORE));
    }

    @Test
    @DisplayName("should fail if no create event access")
    void shouldFailIfNoCreateEventAccess() {

        when(accessControlService.canAccessCaseEventWithCriteria(eq(EVENT_ID), eq(events), eq(userRoles), eq(CAN_CREATE))).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> authorisedCreateCaseOperation.createCaseDetails(UID,
                                                                                                            JURISDICTION_ID,
                                                                                                            CASE_TYPE_ID,
                                                                                                            EVENT_DATA,
                                                                                                            IGNORE));
    }

    @Test
    @DisplayName("should fail if no create field access")
    void shouldFailIfNoCreateFieldAccess() {

        when(accessControlService.canAccessCaseFieldsWithCriteria(any(JsonNode.class), eq(caseFields), eq(userRoles), eq(CAN_CREATE))).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> authorisedCreateCaseOperation.createCaseDetails(UID,
                                                                                                            JURISDICTION_ID,
                                                                                                            CASE_TYPE_ID,
                                                                                                            EVENT_DATA,
                                                                                                            IGNORE));
    }

    @Test
    @DisplayName("should return empty case if no read case access")
    void shouldReturnEmptyIfNoCaseReadAccess() {

        when(accessControlService.canAccessCaseTypeWithCriteria(eq(caseType), eq(userRoles), eq(CAN_READ))).thenReturn(false);

        final CaseDetails caseDetails = authorisedCreateCaseOperation.createCaseDetails(UID,
                                                                                        JURISDICTION_ID,
                                                                                        CASE_TYPE_ID,
                                                                                        EVENT_DATA,
                                                                                        IGNORE);
        assertThat(caseDetails, is(nullValue()));
    }

}
