package uk.gov.hmcts.ccd.domain.service.createevent;

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
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEvent;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.model.std.EventBuilder.anEvent;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_CREATE;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_UPDATE;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.newCaseDataContent;

class AuthorisedCreateEventOperationTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final String CASEWORKER_PROBATE_LOA1 = "caseworker-probate-loa1";
    private static final String CASEWORKER_PROBATE_LOA3 = "caseworker-probate-loa3";
    private static final String CASEWORKER_DIVORCE = "caseworker-divorce-loa3";
    private static final Set<String> USER_ROLES = Sets.newHashSet(CASEWORKER_DIVORCE,
        CASEWORKER_PROBATE_LOA1,
        CASEWORKER_PROBATE_LOA3);

    private static final String CASE_TYPE_ID = "Grant";
    private static final String CASE_ID = "26";
    private static final String CASE_REFERENCE = "123456789012345";
    private static final String STATE_ID = "STATE_1";
    private static final Event EVENT = anEvent().build();
    private static final String EVENT_ID = "EVENT_ID";
    private static final Event NULL_EVENT = null;
    private static final Map<String, JsonNode> NEW_DATA = Maps.newHashMap();
    private static final String TOKEN = "JwtToken";
    private static final Boolean IGNORE = Boolean.TRUE;
    public static final CaseDataContent INVALID_CASE_DATA_CONTENT = newCaseDataContent()
        .withEvent(NULL_EVENT)
        .withData(NEW_DATA)
        .withToken(TOKEN)
        .withIgnoreWarning(IGNORE)
        .build();
    public static final CaseDataContent CASE_DATA_CONTENT = newCaseDataContent()
        .withEvent(EVENT)
        .withData(NEW_DATA)
        .withToken(TOKEN)
        .withIgnoreWarning(IGNORE)
        .build();

    @Mock
    private CreateEventOperation createEventOperation;

    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;

    @Mock
    private GetCaseOperation getCaseOperation;

    @Mock
    private AccessControlService accessControlService;

    @Mock
    private CaseAccessService caseAccessService;

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
            createEventOperation,
            getCaseOperation,
            caseDefinitionRepository,
            accessControlService,
            caseAccessService);

        CaseDetails existingCase = new CaseDetails();
        Map<String, JsonNode> existingData = Maps.newHashMap();
        existingCase.setState(STATE_ID);
        existingCase.setData(existingData);
        existingCase.setId(CASE_ID);
        existingCase.setCaseTypeId(CASE_TYPE_ID);
        when(getCaseOperation.execute(CASE_REFERENCE)).thenReturn(Optional.of(existingCase));

        classifiedCase = new CaseDetails();
        Map<String, JsonNode> classifiedData = Maps.newHashMap();
        classifiedCase.setData(classifiedData);
        doReturn(classifiedCase).when(createEventOperation).createCaseEvent(CASE_REFERENCE,
            CASE_DATA_CONTENT);
        caseType.setEvents(events);
        caseType.setCaseFields(caseFields);
        when(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).thenReturn(caseType);
        when(caseAccessService.getUserRoles()).thenReturn(USER_ROLES);
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
            eq(CAN_READ),
            anyBoolean())).thenReturn(authorisedCaseNode);
    }

    @Test
    @DisplayName("should call decorated operation")
    void shouldCallDecoratedOperation() {

        authorisedCreateEventOperation.createCaseEvent(CASE_REFERENCE,
            CASE_DATA_CONTENT);

        verify(createEventOperation).createCaseEvent(CASE_REFERENCE,
            CASE_DATA_CONTENT);
    }

    @Test
    @DisplayName("should fail when no case found")
    void shouldFailWhenCaseNotFound() {
        doReturn(Optional.empty()).when(getCaseOperation).execute(CASE_REFERENCE);
        assertThrows(ResourceNotFoundException.class, () -> authorisedCreateEventOperation.createCaseEvent(CASE_REFERENCE,
            CASE_DATA_CONTENT));

    }

    @Test
    @DisplayName("should return null when decorated operation returns null")
    void shouldReturnNullWhenOperationReturnsNull() {
        doReturn(null).when(createEventOperation).createCaseEvent(CASE_REFERENCE,
            CASE_DATA_CONTENT);
        final CaseDetails output = authorisedCreateEventOperation.createCaseEvent(CASE_REFERENCE,
            CASE_DATA_CONTENT);

        assertThat(output, is(nullValue()));
    }

    @Test
    @DisplayName("should return authorised case detail if relevant create, update and read access granted")
    void shouldReturnAuthorisedCaseDetailsIfCreateEventAndCreateUpdateAndReadAccessGranted() {

        final CaseDetails output = authorisedCreateEventOperation.createCaseEvent(CASE_REFERENCE,
            CASE_DATA_CONTENT);
        InOrder inOrder = inOrder(caseDefinitionRepository, caseAccessService, getCaseOperation,
            createEventOperation, accessControlService);
        assertAll(
            () -> assertThat(output, sameInstance(classifiedCase)),
            () -> assertThat(output.getData(), is(equalTo(JacksonUtils.convertValue(authorisedCaseNode)))),
            () -> inOrder.verify(getCaseOperation).execute(CASE_REFERENCE),
            () -> inOrder.verify(caseAccessService).getUserRoles(),
            () -> inOrder.verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID),
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
            () -> inOrder.verify(createEventOperation).createCaseEvent(CASE_REFERENCE,
                CASE_DATA_CONTENT),
            () -> inOrder.verify(accessControlService).canAccessCaseTypeWithCriteria(eq(caseType),
                eq(USER_ROLES),
                eq(CAN_READ)),
            () -> inOrder.verify(accessControlService, times(2)).filterCaseFieldsByAccess(any(JsonNode.class),
                eq(caseFields),
                eq(USER_ROLES),
                eq(CAN_READ),
                anyBoolean())
        );
    }

    @Test
    @DisplayName("should return null when no classified case")
    void shouldReturnNullCaseDetailsWhenNoCaseTypeAccess() {

        doReturn(null).when(createEventOperation).createCaseEvent(CASE_REFERENCE,
            CASE_DATA_CONTENT);


        final CaseDetails output = authorisedCreateEventOperation.createCaseEvent(CASE_REFERENCE,
            CASE_DATA_CONTENT);

        assertThat(output, is(nullValue()));
    }

    @Test
    @DisplayName("should fail if case type not found")
    void shouldFailIfNoCaseTypeFound() {

        doReturn(null).when(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);

        assertThrows(ValidationException.class, () -> authorisedCreateEventOperation.createCaseEvent(CASE_REFERENCE,
            CASE_DATA_CONTENT));
    }

    @Test
    @DisplayName("should fail if user roles not found")
    void shouldFailIfNoUserRolesFound() {

        doReturn(Collections.EMPTY_SET).when(caseAccessService).getUserRoles();

        assertThrows(ValidationException.class, () -> authorisedCreateEventOperation.createCaseEvent(CASE_REFERENCE,
            CASE_DATA_CONTENT));
    }

    @Test
    @DisplayName("should fail if no update case access")
    void shouldFailIfNoUpdateCaseAccess() {

        when(accessControlService.canAccessCaseTypeWithCriteria(caseType, USER_ROLES, CAN_UPDATE)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> authorisedCreateEventOperation.createCaseEvent(CASE_REFERENCE,
            CASE_DATA_CONTENT));
    }

    @Test
    @DisplayName("should fail when user has no state update access")
    void shouldFailWhenUserCannotUpdateState() {
        when(accessControlService.canAccessCaseStateWithCriteria(eq(STATE_ID), eq(caseType), eq(USER_ROLES), eq(CAN_UPDATE))).thenReturn(
            false);
        assertThrows(ResourceNotFoundException.class, () -> authorisedCreateEventOperation.createCaseEvent(CASE_REFERENCE,
            CASE_DATA_CONTENT));

    }

    @Test
    @DisplayName("should fail if no event provided")
    void shouldFailIfNoEventProvided() {

        assertThrows(ResourceNotFoundException.class, () -> authorisedCreateEventOperation.createCaseEvent(CASE_REFERENCE,
            INVALID_CASE_DATA_CONTENT));
    }

    @Test
    @DisplayName("should fail if no create event access")
    void shouldFailIfNoCreateEventAccess() {

        when(accessControlService.canAccessCaseEventWithCriteria(eq(EVENT_ID),
            eq(events),
            eq(USER_ROLES),
            eq(CAN_CREATE))).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> authorisedCreateEventOperation.createCaseEvent(CASE_REFERENCE,
            CASE_DATA_CONTENT));
    }

    @Test
    @DisplayName("should fail if no create or update field access")
    void shouldFailIfNoCreateFieldAccess() {

        when(accessControlService.canAccessCaseFieldsForUpsert(any(JsonNode.class),
            any(JsonNode.class),
            eq(caseFields),
            eq(USER_ROLES))).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> authorisedCreateEventOperation.createCaseEvent(CASE_REFERENCE,
            CASE_DATA_CONTENT));
    }

    @Test
    @DisplayName("should return empty case if no read case access")
    void shouldFailIfNoCaseReadAccess() {

        when(accessControlService.canAccessCaseTypeWithCriteria(eq(caseType),
            eq(USER_ROLES),
            eq(CAN_READ))).thenReturn(false);

        final CaseDetails caseDetails = authorisedCreateEventOperation.createCaseEvent(CASE_REFERENCE,
            CASE_DATA_CONTENT);
        assertThat(caseDetails, is(nullValue()));
    }
}
