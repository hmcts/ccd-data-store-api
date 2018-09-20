package uk.gov.hmcts.ccd.domain.service.createevent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.ccd.domain.model.std.EventBuilder.anEvent;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.*;

class AuthorisedCreateEventOperationTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference STRING_JSON_MAP = new TypeReference<HashMap<String, JsonNode>>() {
    };
    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final String CASEWORKER_PROBATE_LOA1 = "caseworker-probate-loa1";
    private static final String CASEWORKER_PROBATE_LOA3 = "caseworker-probate-loa3";
    private static final String CASEWORKER_DIVORCE = "caseworker-divorce-loa3";
    private static final Set<String> USER_ROLES = Sets.newHashSet(CASEWORKER_DIVORCE,
                                                                  CASEWORKER_PROBATE_LOA1,
                                                                  CASEWORKER_PROBATE_LOA3);

    private static final String UID = "123";
    private static final String JURISDICTION_ID = "Probate";
    private static final String CASE_TYPE_ID = "Grant";
    private static final String CASE_REFERENCE = "123456789012345";
    private static final String STATE_ID = "STATE_1";
    private static final Event EVENT = anEvent().build();
    private static final String EVENT_ID = "EVENT_ID";
    private static final Event NULL_EVENT = null;
    private static final Map<String, JsonNode> NEW_DATA = Maps.newHashMap();
    private static final String TOKEN = "JwtToken";
    private static final Boolean IGNORE = Boolean.TRUE;

    @Mock
    private CreateEventOperation createEventOperation;

    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;

    @Mock
    private GetCaseOperation getCaseOperation;

    @Mock
    private AccessControlService accessControlService;

    @Mock
    private UserRepository userRepository;

    private AuthorisedCreateEventOperation authorisedCreateEventOperation;
    private CaseDetails classifiedCase;
    private JsonNode authorisedCaseNode;
    private final CaseType caseType = new CaseType();
    private final List<CaseField> caseFields = Lists.newArrayList();
    private final List<CaseEvent> events = Lists.newArrayList();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        EVENT.setEventId(EVENT_ID);
        authorisedCreateEventOperation = new AuthorisedCreateEventOperation(
            createEventOperation, getCaseOperation, caseDefinitionRepository, accessControlService, userRepository);

        CaseDetails existingCase = new CaseDetails();
        Map<String, JsonNode> existingData = Maps.newHashMap();
        existingCase.setState(STATE_ID);
        existingCase.setData(existingData);
        when(getCaseOperation.execute(CASE_REFERENCE)).thenReturn(Optional.of(existingCase));

        classifiedCase = new CaseDetails();
        Map<String, JsonNode> classifiedData = Maps.newHashMap();
        classifiedCase.setData(classifiedData);
        doReturn(classifiedCase).when(createEventOperation).createCaseEvent(UID,
                                                                            JURISDICTION_ID,
                                                                            CASE_TYPE_ID,
                                                                            CASE_REFERENCE,
                                                                            EVENT,
                                                                            NEW_DATA,
                                                                            TOKEN,
                                                                            IGNORE);
        caseType.setEvents(events);
        caseType.setCaseFields(caseFields);
        when(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).thenReturn(caseType);
        when(userRepository.getUserRoles()).thenReturn(USER_ROLES);
        when(accessControlService.canAccessCaseTypeWithCriteria(eq(caseType), eq(USER_ROLES), eq(CAN_UPDATE))).thenReturn(
            true);
        when(accessControlService.canAccessCaseStateWithCriteria(eq(STATE_ID), eq(caseType), eq(USER_ROLES), eq(CAN_UPDATE))).thenReturn(
            true);
        when(accessControlService.canAccessCaseEventWithCriteria(eq(EVENT_ID),
                                                                 eq(events),
                                                                 eq(USER_ROLES),
                                                                 eq(CAN_CREATE))).thenReturn(true);
        when(accessControlService.canAccessCaseFieldsForUpsert(any(JsonNode.class),
                                                               any(JsonNode.class),
                                                               eq(caseFields),
                                                               eq(USER_ROLES))).thenReturn(true);
        when(accessControlService.canAccessCaseTypeWithCriteria(eq(caseType),
                                                                eq(USER_ROLES),
                                                                eq(CAN_READ))).thenReturn(true);
        authorisedCaseNode = MAPPER.createObjectNode();
        ((ObjectNode) authorisedCaseNode).set("testField", JSON_NODE_FACTORY.textNode("testValue"));
        when(accessControlService.filterCaseFieldsByAccess(any(JsonNode.class),
                                                           eq(caseFields),
                                                           eq(USER_ROLES),
                                                           eq(CAN_READ))).thenReturn(authorisedCaseNode);
    }

    @Test
    @DisplayName("should call decorated operation")
    void shouldCallDecoratedOperation() {

        authorisedCreateEventOperation.createCaseEvent(UID,
                                                       JURISDICTION_ID,
                                                       CASE_TYPE_ID,
                                                       CASE_REFERENCE,
                                                       EVENT,
                                                       NEW_DATA,
                                                       TOKEN,
                                                       IGNORE);

        verify(createEventOperation).createCaseEvent(UID,
                                                     JURISDICTION_ID,
                                                     CASE_TYPE_ID,
                                                     CASE_REFERENCE,
                                                     EVENT,
                                                     NEW_DATA,
                                                     TOKEN,
                                                     IGNORE);
    }

    @Test
    @DisplayName("should fail when no case found")
    void shouldFailWhenCaseNotFound() {
        doReturn(Optional.empty()).when(getCaseOperation).execute(CASE_REFERENCE);
        assertThrows(ResourceNotFoundException.class, () -> authorisedCreateEventOperation.createCaseEvent(UID,
                                                                                                           JURISDICTION_ID,
                                                                                                           CASE_TYPE_ID,
                                                                                                           CASE_REFERENCE,
                                                                                                           EVENT,
                                                                                                           NEW_DATA,
                                                                                                           TOKEN,
                                                                                                           IGNORE));

    }

    @Test
    @DisplayName("should return null when decorated operation returns null")
    void shouldReturnNullWhenOperationReturnsNull() {
        doReturn(null).when(createEventOperation).createCaseEvent(UID,
                                                                  JURISDICTION_ID,
                                                                  CASE_TYPE_ID,
                                                                  CASE_REFERENCE,
                                                                  EVENT,
                                                                  NEW_DATA,
                                                                  TOKEN,
                                                                  IGNORE);
        final CaseDetails output = authorisedCreateEventOperation.createCaseEvent(UID,
                                                                                  JURISDICTION_ID,
                                                                                  CASE_TYPE_ID,
                                                                                  CASE_REFERENCE,
                                                                                  EVENT,
                                                                                  NEW_DATA,
                                                                                  TOKEN,
                                                                                  IGNORE);

        assertThat(output, is(nullValue()));
    }

    @Test
    @DisplayName("should return authorised case detail if relevant create, update and read access granted")
    void shouldReturnAuthorisedCaseDetailsIfCreateEventAndCreateUpdateAndReadAccessGranted() {

        final CaseDetails output = authorisedCreateEventOperation.createCaseEvent(UID,
                                                                                  JURISDICTION_ID,
                                                                                  CASE_TYPE_ID,
                                                                                  CASE_REFERENCE,
                                                                                  EVENT,
                                                                                  NEW_DATA,
                                                                                  TOKEN,
                                                                                  IGNORE);
        InOrder inOrder = inOrder(caseDefinitionRepository, userRepository, getCaseOperation,
                                  createEventOperation, accessControlService);
        assertAll(
            () -> assertThat(output, sameInstance(classifiedCase)),
            () -> assertThat(output.getData(), is(equalTo(MAPPER.convertValue(authorisedCaseNode, STRING_JSON_MAP)))),
            () -> inOrder.verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID),
            () -> inOrder.verify(userRepository).getUserRoles(),
            () -> inOrder.verify(getCaseOperation).execute(CASE_REFERENCE),
            () -> inOrder.verify(accessControlService).canAccessCaseTypeWithCriteria(eq(caseType),
                                                                                     eq(USER_ROLES),
                                                                                     eq(CAN_UPDATE)),
            () -> inOrder.verify(accessControlService).canAccessCaseEventWithCriteria(eq(EVENT_ID),
                                                                                      eq(events),
                                                                                      eq(USER_ROLES),
                                                                                      eq(CAN_CREATE)),
            () -> inOrder.verify(accessControlService).canAccessCaseFieldsForUpsert(any(JsonNode.class),
                                                                                    any(JsonNode.class),
                                                                                    eq(caseFields),
                                                                                    eq(USER_ROLES)),
            () -> inOrder.verify(createEventOperation).createCaseEvent(UID,
                                                                       JURISDICTION_ID,
                                                                       CASE_TYPE_ID,
                                                                       CASE_REFERENCE,
                                                                       EVENT,
                                                                       NEW_DATA,
                                                                       TOKEN,
                                                                       IGNORE),
            () -> inOrder.verify(accessControlService).canAccessCaseTypeWithCriteria(eq(caseType),
                                                                                     eq(USER_ROLES),
                                                                                     eq(CAN_READ)),
            () -> inOrder.verify(accessControlService, times(2)).filterCaseFieldsByAccess(any(JsonNode.class),
                                                                                          eq(caseFields),
                                                                                          eq(USER_ROLES),
                                                                                          eq(CAN_READ))
        );
    }

    @Test
    @DisplayName("should return null when no classified case")
    void shouldReturnNullCaseDetailsWhenNoCaseTypeAccess() {

        doReturn(null).when(createEventOperation).createCaseEvent(UID,
                                                                  JURISDICTION_ID,
                                                                  CASE_TYPE_ID,
                                                                  CASE_REFERENCE,
                                                                  EVENT,
                                                                  NEW_DATA,
                                                                  TOKEN,
                                                                  IGNORE);


        final CaseDetails output = authorisedCreateEventOperation.createCaseEvent(UID,
                                                                                  JURISDICTION_ID,
                                                                                  CASE_TYPE_ID,
                                                                                  CASE_REFERENCE,
                                                                                  EVENT,
                                                                                  NEW_DATA,
                                                                                  TOKEN,
                                                                                  IGNORE);

        assertThat(output, is(nullValue()));
    }

    @Test
    @DisplayName("should fail if case type not found")
    void shouldFailIfNoCaseTypeFound() {

        doReturn(null).when(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);

        assertThrows(ValidationException.class, () -> authorisedCreateEventOperation.createCaseEvent(UID,
                                                                                                     JURISDICTION_ID,
                                                                                                     CASE_TYPE_ID,
                                                                                                     CASE_REFERENCE,
                                                                                                     EVENT,
                                                                                                     NEW_DATA,
                                                                                                     TOKEN,
                                                                                                     IGNORE));
    }

    @Test
    @DisplayName("should fail if user roles not found")
    void shouldFailIfNoUserRolesFound() {

        doReturn(null).when(userRepository).getUserRoles();

        assertThrows(ValidationException.class, () -> authorisedCreateEventOperation.createCaseEvent(UID,
                                                                                                     JURISDICTION_ID,
                                                                                                     CASE_TYPE_ID,
                                                                                                     CASE_REFERENCE,
                                                                                                     EVENT,
                                                                                                     NEW_DATA,
                                                                                                     TOKEN,
                                                                                                     IGNORE));
    }

    @Test
    @DisplayName("should fail if no update case access")
    void shouldFailIfNoUpdateCaseAccess() {

        when(accessControlService.canAccessCaseTypeWithCriteria(caseType, USER_ROLES, CAN_UPDATE)).thenReturn(
            false);

        assertThrows(ResourceNotFoundException.class, () -> authorisedCreateEventOperation.createCaseEvent(UID,
                                                                                                           JURISDICTION_ID,
                                                                                                           CASE_TYPE_ID,
                                                                                                           CASE_REFERENCE,
                                                                                                           EVENT,
                                                                                                           NEW_DATA,
                                                                                                           TOKEN,
                                                                                                           IGNORE));
    }

    @Test
    @DisplayName("should fail when user has no state update access")
    void shouldFailWhenUserCannotUpdateState() {
        when(accessControlService.canAccessCaseStateWithCriteria(eq(STATE_ID), eq(caseType), eq(USER_ROLES), eq(CAN_UPDATE))).thenReturn(
            false);
        assertThrows(ResourceNotFoundException.class, () -> authorisedCreateEventOperation.createCaseEvent(UID,
            JURISDICTION_ID,
            CASE_TYPE_ID,
            CASE_REFERENCE,
            EVENT,
            NEW_DATA,
            TOKEN,
            IGNORE));

    }

    @Test
    @DisplayName("should fail if no event provided")
    void shouldFailIfNoEventProvided() {

        assertThrows(ResourceNotFoundException.class, () -> authorisedCreateEventOperation.createCaseEvent(UID,
                                                                                                           JURISDICTION_ID,
                                                                                                           CASE_TYPE_ID,
                                                                                                           CASE_REFERENCE,
                                                                                                           NULL_EVENT,
                                                                                                           NEW_DATA,
                                                                                                           TOKEN,
                                                                                                           IGNORE));
    }

    @Test
    @DisplayName("should fail if no create event access")
    void shouldFailIfNoCreateEventAccess() {

        when(accessControlService.canAccessCaseEventWithCriteria(eq(EVENT_ID),
                                                                 eq(events),
                                                                 eq(USER_ROLES),
                                                                 eq(CAN_CREATE))).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> authorisedCreateEventOperation.createCaseEvent(UID,
                                                                                                           JURISDICTION_ID,
                                                                                                           CASE_TYPE_ID,
                                                                                                           CASE_REFERENCE,
                                                                                                           EVENT,
                                                                                                           NEW_DATA,
                                                                                                           TOKEN,
                                                                                                           IGNORE));
    }

    @Test
    @DisplayName("should fail if no create or update field access")
    void shouldFailIfNoCreateFieldAccess() {

        when(accessControlService.canAccessCaseFieldsForUpsert(any(JsonNode.class),
                                                               any(JsonNode.class),
                                                               eq(caseFields),
                                                               eq(USER_ROLES))).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> authorisedCreateEventOperation.createCaseEvent(UID,
                                                                                                           JURISDICTION_ID,
                                                                                                           CASE_TYPE_ID,
                                                                                                           CASE_REFERENCE,
                                                                                                           EVENT,
                                                                                                           NEW_DATA,
                                                                                                           TOKEN,
                                                                                                           IGNORE));
    }

    @Test
    @DisplayName("should return empty case if no read case access")
    void shouldFailIfNoCaseReadAccess() {

        when(accessControlService.canAccessCaseTypeWithCriteria(eq(caseType),
                                                                eq(USER_ROLES),
                                                                eq(CAN_READ))).thenReturn(false);

        final CaseDetails caseDetails = authorisedCreateEventOperation.createCaseEvent(UID,
                                                                                       JURISDICTION_ID,
                                                                                       CASE_TYPE_ID,
                                                                                       CASE_REFERENCE,
                                                                                       EVENT,
                                                                                       NEW_DATA,
                                                                                       TOKEN,
                                                                                       IGNORE);
        assertThat(caseDetails, is(nullValue()));
    }

}
