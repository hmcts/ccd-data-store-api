package uk.gov.hmcts.ccd.domain.service.createevent;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.documentdata.CollectionData;
import uk.gov.hmcts.ccd.data.documentdata.DocumentData;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CategoryDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.AccessTypeRoleDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.AccessTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.casedeletion.TimeToLiveService;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;
import uk.gov.hmcts.ccd.domain.service.common.CaseDataService;
import uk.gov.hmcts.ccd.domain.service.common.CaseService;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.jsonpath.CaseDetailsJsonParser;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
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
    private static final Set<AccessProfile> USER_ROLES = createAccessProfiles(Sets.newHashSet(CASEWORKER_DIVORCE,
        CASEWORKER_PROBATE_LOA1,
        CASEWORKER_PROBATE_LOA3));

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

    @Mock
    private CaseDetailsJsonParser caseDetailsJsonParser;

    @Mock
    private CaseDataService caseDataService;

    @Mock
    private EventTriggerService eventTriggerService;

    @Mock
    private TimeToLiveService timeToLiveService;

    @InjectMocks
    private CaseService caseService;

    private AuthorisedCreateEventOperation authorisedCreateEventOperation;
    private CaseDetails classifiedCase;
    private CaseDetails documentFieldsCase;
    private JsonNode authorisedCaseNode;
    private final CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
    private final List<CaseFieldDefinition> caseFieldDefinitions = Lists.newArrayList();
    private final List<CaseEventDefinition> events = Lists.newArrayList();
    private final String categoryId = "categoryId";

    private final String accessTypeRole = "accessTypeRole";

    private final Map<String, JsonNode> documentData = new HashMap<>();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        EVENT.setEventId(EVENT_ID);
        authorisedCreateEventOperation = new AuthorisedCreateEventOperation(
            createEventOperation,
            getCaseOperation,
            caseDefinitionRepository,
            accessControlService,
            caseAccessService,
            caseDetailsJsonParser,
            getCaseOperation,
            caseService,
            eventTriggerService,
            timeToLiveService);

        mockExistingCaseDetails(Maps.newHashMap());

        classifiedCase = new CaseDetails();
        Map<String, JsonNode> classifiedData = Maps.newHashMap();
        classifiedCase.setData(classifiedData);
        doReturn(classifiedCase).when(createEventOperation).createCaseEvent(CASE_REFERENCE,
            CASE_DATA_CONTENT);
        caseTypeDefinition.setEvents(events);
        CategoryDefinition categoryDefinition = new CategoryDefinition();
        categoryDefinition.setCategoryId(categoryId);
        caseTypeDefinition.setCategories(Lists.newArrayList(categoryDefinition));

        AccessTypeRoleDefinition accessTypeRolesDefinition = new AccessTypeRoleDefinition();
        accessTypeRolesDefinition.setCaseAssignedRoleField("caseAssignedroleField");
        accessTypeRolesDefinition.setGroupRoleName("groupRoleName");
        accessTypeRolesDefinition.setCaseAssignedRoleField("caseAssignedRoleField");

        caseTypeDefinition.setAccessTypeRoleDefinitions(Lists.newArrayList(accessTypeRolesDefinition));

        AccessTypeDefinition accessTypeDefinition = new AccessTypeDefinition();
        accessTypeDefinition.setDescription("description");
        accessTypeDefinition.setDisplayOrder(10);
        accessTypeDefinition.setOrganisationProfileId("OrganisationProfileId");

        caseTypeDefinition.setAccessTypeDefinitions(Lists.newArrayList(accessTypeDefinition));

        caseTypeDefinition.setCaseFieldDefinitions(caseFieldDefinitions);
        when(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).thenReturn(caseTypeDefinition);
        when(caseAccessService.getAccessProfiles(anyString())).thenReturn(USER_ROLES);
        when(caseAccessService.getAccessProfilesByCaseReference(anyString())).thenReturn(USER_ROLES);
        when(accessControlService.canAccessCaseTypeWithCriteria(eq(caseTypeDefinition), eq(USER_ROLES),
            eq(CAN_UPDATE))).thenReturn(true);
        when(accessControlService.canAccessCaseStateWithCriteria(eq(STATE_ID), eq(caseTypeDefinition), eq(USER_ROLES),
            eq(CAN_UPDATE))).thenReturn(true);
        when(accessControlService.canAccessCaseEventWithCriteria(eq(EVENT_ID),
            eq(events),
            eq(USER_ROLES),
            eq(CAN_CREATE))).thenReturn(true);
        when(accessControlService.canAccessCaseFieldsForUpsert(any(JsonNode.class),
            any(JsonNode.class),
            eq(caseFieldDefinitions),
            eq(USER_ROLES))).thenReturn(true);
        when(accessControlService.canAccessCaseTypeWithCriteria(eq(caseTypeDefinition),
            eq(USER_ROLES),
            eq(CAN_READ))).thenReturn(true);
        authorisedCaseNode = MAPPER.createObjectNode();
        ((ObjectNode) authorisedCaseNode).set("testField", JSON_NODE_FACTORY.textNode("testValue"));
        when(accessControlService.filterCaseFieldsByAccess(any(JsonNode.class),
            eq(caseFieldDefinitions),
            eq(USER_ROLES),
            eq(CAN_READ),
            anyBoolean())).thenReturn(authorisedCaseNode);

        when(caseDetailsJsonParser.read(any(CaseDetails.class), anyString())).thenCallRealMethod();
        when(caseDetailsJsonParser.containsDocumentUrl(any(CaseDetails.class), anyString())).thenCallRealMethod();
        doCallRealMethod().when(caseDetailsJsonParser)
            .updateCaseDocumentData(anyString(), anyString(), any(CaseDetails.class));
        doCallRealMethod().when(caseDetailsJsonParser).compiledPath(anyString());
        doCallRealMethod().when(caseDetailsJsonParser).compiledPath(anyString(), anyBoolean());
        when(caseDataService.cloneDataMap(anyMap())).thenCallRealMethod();
    }

    private void mockExistingCaseDetails(Map<String, JsonNode> existingData) {
        CaseDetails existingCase = new CaseDetails();
        existingCase.setState(STATE_ID);
        existingCase.setData(existingData);
        existingCase.setId(CASE_ID);
        existingCase.setCaseTypeId(CASE_TYPE_ID);
        when(getCaseOperation.execute(CASE_REFERENCE)).thenReturn(Optional.of(existingCase));
    }

    private static Set<AccessProfile> createAccessProfiles(Set<String> userRoles) {
        return userRoles.stream()
            .map(userRole -> AccessProfile.builder().readOnly(false)
                .accessProfile(userRole)
                .build())
            .collect(Collectors.toSet());
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
    @DisplayName("should make calls to increment TTL if CaseType is using TTL and event found")
    void shouldMakeCallsToIncrementTtlIfCaseTypeUsingTtlAndEventFound() {

        // GIVEN
        doReturn(true).when(timeToLiveService).isCaseTypeUsingTTL(any());
        doReturn(new CaseEventDefinition()).when(eventTriggerService).findCaseEvent(any(), any());

        // WHEN
        authorisedCreateEventOperation.createCaseEvent(CASE_REFERENCE,
            CASE_DATA_CONTENT);

        // THEN
        verify(timeToLiveService).isCaseTypeUsingTTL(any());
        verify(timeToLiveService).updateCaseDetailsWithTTL(any(), any(), any());
        verify(timeToLiveService).updateCaseDataClassificationWithTTL(any(), any(), any(), any());
    }

    @Test
    @DisplayName("should not make calls to increment TTL if CaseType is not using TTL")
    void shouldNotMakeCallsToIncrementTtlIfCaseTypeNotUsingTtl() {

        // GIVEN
        doReturn(false).when(timeToLiveService).isCaseTypeUsingTTL(any());

        // WHEN
        authorisedCreateEventOperation.createCaseEvent(CASE_REFERENCE,
            CASE_DATA_CONTENT);

        // THEN
        verify(timeToLiveService).isCaseTypeUsingTTL(any());
        verify(eventTriggerService, never()).findCaseEvent(any(), any());
        verify(timeToLiveService, never()).updateCaseDetailsWithTTL(any(), any(), any());
        verify(timeToLiveService, never()).updateCaseDataClassificationWithTTL(any(), any(), any(), any());
    }

    @Test
    @DisplayName("should not make calls to increment TTL if event not found")
    void shouldNotMakeCallsToIncrementTtlIfEventNotFound() {

        // GIVEN
        doReturn(true).when(timeToLiveService).isCaseTypeUsingTTL(any());
        doReturn(null).when(eventTriggerService).findCaseEvent(any(), any());

        // WHEN
        authorisedCreateEventOperation.createCaseEvent(CASE_REFERENCE,
            CASE_DATA_CONTENT);

        // THEN
        verify(timeToLiveService).isCaseTypeUsingTTL(any());
        verify(timeToLiveService, never()).updateCaseDetailsWithTTL(any(), any(), any());
        verify(timeToLiveService, never()).updateCaseDataClassificationWithTTL(any(), any(), any(), any());
    }

    @Test
    @DisplayName("should fail when no case found")
    void shouldFailWhenCaseNotFound() {
        doReturn(Optional.empty()).when(getCaseOperation).execute(CASE_REFERENCE);
        assertThrows(ResourceNotFoundException.class, () ->
            authorisedCreateEventOperation.createCaseEvent(CASE_REFERENCE, CASE_DATA_CONTENT));

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
            () -> inOrder.verify(caseAccessService).getAccessProfilesByCaseReference(CASE_REFERENCE),
            () -> inOrder.verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID),
            () -> inOrder.verify(accessControlService).canAccessCaseTypeWithCriteria(eq(caseTypeDefinition),
                eq(USER_ROLES),
                eq(CAN_UPDATE)),
            () -> inOrder.verify(accessControlService).canAccessCaseEventWithCriteria(eq(EVENT_ID),
                eq(events),
                eq(USER_ROLES),
                eq(CAN_CREATE)),
            () -> inOrder.verify(accessControlService).canAccessCaseFieldsForUpsert(any(JsonNode.class),
                any(JsonNode.class),
                eq(caseFieldDefinitions),
                eq(USER_ROLES)),
            () -> inOrder.verify(createEventOperation).createCaseEvent(CASE_REFERENCE,
                CASE_DATA_CONTENT),
            () -> inOrder.verify(accessControlService).canAccessCaseTypeWithCriteria(eq(caseTypeDefinition),
                eq(USER_ROLES),
                eq(CAN_READ)),
            () -> inOrder.verify(accessControlService, times(2)).filterCaseFieldsByAccess(any(JsonNode.class),
                eq(caseFieldDefinitions),
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

        doReturn(Collections.EMPTY_SET).when(caseAccessService).getAccessProfilesByCaseReference(anyString());

        assertThrows(ValidationException.class, () -> authorisedCreateEventOperation.createCaseEvent(CASE_REFERENCE,
            CASE_DATA_CONTENT));
    }

    @Test
    @DisplayName("should fail if no update case access")
    void shouldFailIfNoUpdateCaseAccess() {

        when(accessControlService.canAccessCaseTypeWithCriteria(caseTypeDefinition, USER_ROLES, CAN_UPDATE))
            .thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () ->
            authorisedCreateEventOperation.createCaseEvent(CASE_REFERENCE, CASE_DATA_CONTENT));
    }

    @Test
    @DisplayName("should fail when user has no state update access")
    void shouldFailWhenUserCannotUpdateState() {
        when(accessControlService.canAccessCaseStateWithCriteria(eq(STATE_ID), eq(caseTypeDefinition), eq(USER_ROLES),
            eq(CAN_UPDATE))).thenReturn(
            false);
        assertThrows(ResourceNotFoundException.class, () ->
            authorisedCreateEventOperation.createCaseEvent(CASE_REFERENCE, CASE_DATA_CONTENT));

    }

    @Test
    @DisplayName("should fail if no event provided")
    void shouldFailIfNoEventProvided() {
        assertCreateEventLogs(INVALID_CASE_DATA_CONTENT);
    }

    @Test
    @DisplayName("should fail if eventId is null")
    void shouldFailIfEventIdIsNull() {
        Event event = new Event();
        event.setEventId(null);
        INVALID_CASE_DATA_CONTENT.setEvent(event);
        assertCreateEventLogs(INVALID_CASE_DATA_CONTENT);
        assertNull(INVALID_CASE_DATA_CONTENT.getEvent().getEventId());
    }

    @Test
    @DisplayName("should fail if eventId is empty")
    void shouldFailIfEventIdIsEmpty() {
        Event event = new Event();
        event.setEventId("");
        INVALID_CASE_DATA_CONTENT.setEvent(event);
        assertCreateEventLogs(INVALID_CASE_DATA_CONTENT);
        assertTrue(INVALID_CASE_DATA_CONTENT.getEvent().getEventId().isEmpty());
    }

    private void assertCreateEventLogs(CaseDataContent caseDataContent) {
        Logger logger = (Logger) LoggerFactory.getLogger(AuthorisedCreateEventOperation.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        logger.setLevel(Level.ERROR);
        assertThrows(ResourceNotFoundException.class, () ->
                authorisedCreateEventOperation.createCaseEvent(CASE_REFERENCE, caseDataContent));

        List<ILoggingEvent> loggingEventList = listAppender.list;
        assertAll(
                () -> assertThat(loggingEventList.get(0).getLevel(), is(Level.ERROR)),
                () -> assertThat(loggingEventList.get(0).getFormattedMessage(), is("EventId is not supplied"))
        );
        listAppender.stop();
        logger.detachAndStopAllAppenders();
    }

    @Test
    @DisplayName("should fail if no create event access")
    void shouldFailIfNoCreateEventAccess() {

        when(accessControlService.canAccessCaseEventWithCriteria(eq(EVENT_ID),
            eq(events),
            eq(USER_ROLES),
            eq(CAN_CREATE))).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () ->
            authorisedCreateEventOperation.createCaseEvent(CASE_REFERENCE, CASE_DATA_CONTENT));
    }

    @Test
    @DisplayName("should fail if no create or update field access")
    void shouldFailIfNoCreateFieldAccess() {

        when(accessControlService.canAccessCaseFieldsForUpsert(any(JsonNode.class),
            any(JsonNode.class),
            eq(caseFieldDefinitions),
            eq(USER_ROLES))).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () ->
            authorisedCreateEventOperation.createCaseEvent(CASE_REFERENCE, CASE_DATA_CONTENT));
    }

    @Test
    @DisplayName("should return empty case if no read case access")
    void shouldFailIfNoCaseReadAccess() {

        when(accessControlService.canAccessCaseTypeWithCriteria(eq(caseTypeDefinition),
            eq(USER_ROLES),
            eq(CAN_READ))).thenReturn(false);

        final CaseDetails caseDetails = authorisedCreateEventOperation.createCaseEvent(CASE_REFERENCE,
            CASE_DATA_CONTENT);
        assertThat(caseDetails, is(nullValue()));
    }

    @Test
    void shouldSuccessfullyReturnCaseDetailsWhenTopLevelDocumentFieldIdentifiedByAttributePathWithAllCorrectDetails() {
        Event event = new Event();
        event.setEventId("DocumentUpdated");
        CaseDetails existingCase = new CaseDetails();
        existingCase.setVersion(1);
        existingCase.setCaseTypeId(CASE_TYPE_ID);
        existingCase.setState(STATE_ID);
        DocumentData document = new DocumentData();
        document.setFilename("filename");
        document.setUrl("someurl");
        String attributeField = "DocumentField1";
        documentData.put(attributeField, MAPPER.convertValue(document, JsonNode.class));
        existingCase.setData(documentData);
        String caseReference = "1122334455";
        when(getCaseOperation.execute(caseReference)).thenReturn(Optional.of(existingCase));
        documentFieldsCase = new CaseDetails();
        documentFieldsCase.setVersion(2);
        doReturn(documentFieldsCase).when(createEventOperation).createCaseSystemEvent(caseReference,
            1, attributeField, categoryId);

        CaseDetails updatedCaseDetails = authorisedCreateEventOperation.createCaseSystemEvent(caseReference,
            1, attributeField, categoryId);

        assertEquals(documentFieldsCase, updatedCaseDetails);
    }

    @Test
    void shouldSuccessfullyReturnCaseDetailsWhenComplexDocumentFieldHasMatchingDetails() {
        Event event = new Event();
        event.setEventId("DocumentUpdated");
        CaseDetails existingCase = new CaseDetails();
        existingCase.setVersion(1);
        existingCase.setCaseTypeId(CASE_TYPE_ID);
        existingCase.setState(STATE_ID);
        DocumentData document = new DocumentData();
        document.setFilename("filename");
        document.setUrl("someurl");
        documentData.put("AnotherField", MAPPER.convertValue(document, JsonNode.class));
        Map<String, JsonNode> map = new HashMap<>();
        map.put("DocumentField1", MAPPER.convertValue(documentData, JsonNode.class));
        existingCase.setData(map);
        String caseReference = "1122334455";
        when(getCaseOperation.execute(caseReference)).thenReturn(Optional.of(existingCase));
        documentFieldsCase = new CaseDetails();
        documentFieldsCase.setVersion(2);
        String attributeField = "DocumentField1.AnotherField";
        doReturn(documentFieldsCase).when(createEventOperation).createCaseSystemEvent(caseReference,
            1, attributeField, categoryId);

        CaseDetails updatedCaseDetails = authorisedCreateEventOperation.createCaseSystemEvent(caseReference,
            1, attributeField, categoryId);

        assertEquals(documentFieldsCase, updatedCaseDetails);
    }

    @Test
    void shouldSuccessfullyReturnCaseDetailsWhenCollectionDocumentFieldHasMatchingDetails() {
        Event event = new Event();
        event.setEventId("DocumentUpdated");
        CaseDetails existingCase = new CaseDetails();
        existingCase.setVersion(1);
        existingCase.setCaseTypeId(CASE_TYPE_ID);
        existingCase.setState(STATE_ID);
        Map<String, String> collectionMap = new HashMap<>();
        CollectionData collectionData = new CollectionData();
        collectionData.setId("12345");
        collectionMap.put("document_url", "someUrl");
        collectionData.setValue(collectionMap);
        List<CollectionData> collectionDataList = new ArrayList<>();
        collectionDataList.add(collectionData);
        Map<String, JsonNode> dataMap = new HashMap<>();
        dataMap.put("DocumentField1", MAPPER.convertValue(collectionDataList, JsonNode.class));
        existingCase.setData(dataMap);
        String caseReference = "1122334455";
        when(getCaseOperation.execute(caseReference)).thenReturn(Optional.of(existingCase));
        documentFieldsCase = new CaseDetails();
        documentFieldsCase.setVersion(2);
        String attributeField = "DocumentField1[12345]";
        doReturn(documentFieldsCase).when(createEventOperation).createCaseSystemEvent(caseReference,
            1, attributeField, categoryId);

        CaseDetails updatedCaseDetails = authorisedCreateEventOperation.createCaseSystemEvent(caseReference,
            1, attributeField, categoryId);

        assertEquals(documentFieldsCase, updatedCaseDetails);
    }

    @Test
    void shouldSuccessfullyReturnCaseDetailsWhenComplexCollectionDocumentFieldHasMatchingDetails() {
        Event event = new Event();
        event.setEventId("DocumentUpdated");
        CaseDetails existingCase = new CaseDetails();
        existingCase.setVersion(1);
        existingCase.setCaseTypeId(CASE_TYPE_ID);
        existingCase.setState(STATE_ID);
        Map<String, JsonNode> map1 = new HashMap<>();
        DocumentData documentData = new DocumentData();
        documentData.setUrl("someurl");
        map1.put("Document", MAPPER.convertValue(documentData, JsonNode.class));
        Map<String, JsonNode> map2 = new HashMap<>();
        map2.put("id", MAPPER.convertValue("12345", JsonNode.class));
        map2.put("value", MAPPER.convertValue(map1, JsonNode.class));
        List<Map<String, JsonNode>> collectionDataList = new ArrayList<>();
        collectionDataList.add(map2);
        Map<String, JsonNode> map3 = new HashMap<>();
        map3.put("DocumentField1", MAPPER.convertValue(collectionDataList, JsonNode.class));
        Map<String, JsonNode> map4 = new HashMap<>();
        map4.put("Complex", MAPPER.convertValue(map3, JsonNode.class));
        existingCase.setData(map4);
        String caseReference = "1122334455";
        when(getCaseOperation.execute(caseReference)).thenReturn(Optional.of(existingCase));
        documentFieldsCase = new CaseDetails();
        documentFieldsCase.setVersion(2);
        String attributeField = "Complex.DocumentField1[12345].Document";
        doReturn(documentFieldsCase).when(createEventOperation).createCaseSystemEvent(caseReference,
            1, attributeField, categoryId);

        CaseDetails updatedCaseDetails = authorisedCreateEventOperation.createCaseSystemEvent(caseReference,
            1, attributeField, categoryId);

        assertEquals(documentFieldsCase, updatedCaseDetails);
    }

    @Test
    void shouldSuccessfullyReturnCaseDetailsWhenCollectionComplexDocumentFieldHasMatchingDetails() {
        Event event = new Event();
        event.setEventId("DocumentUpdated");
        CaseDetails existingCase = new CaseDetails();
        existingCase.setVersion(1);
        existingCase.setCaseTypeId(CASE_TYPE_ID);
        existingCase.setState(STATE_ID);
        Map<String, JsonNode> map1 = new HashMap<>();
        DocumentData documentData = new DocumentData();
        documentData.setUrl("someurl");
        map1.put("Document", MAPPER.convertValue(documentData, JsonNode.class));
        Map<String, JsonNode> map2 = new HashMap<>();
        map2.put("id", MAPPER.convertValue("12345", JsonNode.class));
        map2.put("value", MAPPER.convertValue(map1, JsonNode.class));
        List<Map<String, JsonNode>> collectionDataList = new ArrayList<>();
        collectionDataList.add(map2);
        Map<String, JsonNode> map3 = new HashMap<>();
        map3.put("DocumentField1", MAPPER.convertValue(collectionDataList, JsonNode.class));
        existingCase.setData(map3);
        String caseReference = "1122334455";
        when(getCaseOperation.execute(caseReference)).thenReturn(Optional.of(existingCase));
        documentFieldsCase = new CaseDetails();
        documentFieldsCase.setVersion(2);
        String attributeField = "DocumentField1[12345].Document";
        doReturn(documentFieldsCase).when(createEventOperation).createCaseSystemEvent(caseReference,
            1, attributeField, categoryId);

        CaseDetails updatedCaseDetails = authorisedCreateEventOperation.createCaseSystemEvent(caseReference,
            1, attributeField, categoryId);

        assertEquals(documentFieldsCase, updatedCaseDetails);
    }

    @Test
    void shouldThrowBadRequestExceptionWhenCollectionFieldDoesNotMatchAttributePath() {
        Event event = new Event();
        event.setEventId("DocumentUpdated");
        CaseDetails existingCase = new CaseDetails();
        existingCase.setVersion(1);
        existingCase.setCaseTypeId(CASE_TYPE_ID);
        existingCase.setState(STATE_ID);
        Map<String, String> collectionMap = new HashMap<>();
        CollectionData collectionData = new CollectionData();
        collectionData.setId("12345");
        collectionMap.put("document_url", "someUrl");
        collectionData.setValue(collectionMap);
        List<CollectionData> collectionDataList = new ArrayList<>();
        collectionDataList.add(collectionData);
        Map<String, JsonNode> dataMap = new HashMap<>();
        dataMap.put("DocumentField2", MAPPER.convertValue(collectionDataList, JsonNode.class));
        existingCase.setData(dataMap);
        String caseReference = "1122334455";
        when(getCaseOperation.execute(caseReference)).thenReturn(Optional.of(existingCase));
        documentFieldsCase = new CaseDetails();
        documentFieldsCase.setVersion(2);
        String attributeField = "DocumentField1[12345]";
        doReturn(documentFieldsCase).when(createEventOperation).createCaseSystemEvent(caseReference,
            1, attributeField, categoryId);

        assertThrows(BadRequestException.class, () -> authorisedCreateEventOperation
            .createCaseSystemEvent(caseReference, 1, attributeField, categoryId));
    }

    @Test
    @DisplayName("should return case details if case data match with case access categories")
    void shouldReturnCaseDetailsIfCaseAccessCategoriesMatchCaseData() {
        Set<AccessProfile> accessProfiles =
            createAccessProfilesWithCaseAccessCategories(Sets.newHashSet("Civil/Standard"));
        mockAccess(accessProfiles);

        Map<String, JsonNode> data = getCaseAccessCategoriesData("Civil/Standard/Test");
        mockExistingCaseDetails(data);

        CaseDataContent newCaseDataContent = createNewCaseDataContent("Civil/Standard/Test");
        CaseDetails classifiedCase = new CaseDetails();
        Map<String, JsonNode> classifiedData = Maps.newHashMap();
        classifiedCase.setData(classifiedData);
        doReturn(classifiedCase).when(createEventOperation).createCaseEvent(CASE_REFERENCE,
            newCaseDataContent);

        final CaseDetails caseDetails = authorisedCreateEventOperation.createCaseEvent(CASE_REFERENCE,
            newCaseDataContent);
        assertNotNull(caseDetails);
    }

    private void mockAccess(Set<AccessProfile> accessProfiles) {
        when(caseAccessService.getAccessProfiles(anyString())).thenReturn(accessProfiles);
        when(caseAccessService.getAccessProfilesByCaseReference(anyString())).thenReturn(accessProfiles);

        when(accessControlService.canAccessCaseTypeWithCriteria(eq(caseTypeDefinition),
            eq(accessProfiles), eq(CAN_UPDATE)))
            .thenReturn(true);
        when(accessControlService.canAccessCaseTypeWithCriteria(eq(caseTypeDefinition),
            eq(accessProfiles),
            eq(CAN_UPDATE))).thenReturn(true);
        when(accessControlService.canAccessCaseStateWithCriteria(eq(STATE_ID),
            eq(caseTypeDefinition), eq(accessProfiles),
            eq(CAN_UPDATE))).thenReturn(true);

        when(accessControlService.canAccessCaseEventWithCriteria(eq(EVENT_ID),
            eq(events),
            eq(accessProfiles),
            eq(CAN_CREATE))).thenReturn(true);

        when(accessControlService.canAccessCaseEventWithCriteria(eq(EVENT_ID),
            eq(events),
            eq(accessProfiles),
            eq(CAN_CREATE))).thenReturn(true);

        when(accessControlService.canAccessCaseFieldsForUpsert(any(JsonNode.class),
            any(JsonNode.class),
            eq(caseFieldDefinitions),
            eq(accessProfiles))).thenReturn(true);
        when(accessControlService.canAccessCaseTypeWithCriteria(eq(caseTypeDefinition),
            eq(accessProfiles),
            eq(CAN_READ))).thenReturn(true);
    }

    private static Set<AccessProfile> createAccessProfilesWithCaseAccessCategories(Set<String> caseAccessCategories) {
        return caseAccessCategories.stream()
            .map(caseAccessCategory -> AccessProfile.builder().readOnly(false)
                .caseAccessCategories(caseAccessCategory)
                .build())
            .collect(Collectors.toSet());
    }

    private Map<String, JsonNode> getCaseAccessCategoriesData(String caseAccessCategoryValue) {
        JsonNode caseAccessCategory = new TextNode(caseAccessCategoryValue);
        Map<String, JsonNode> data = new HashMap<>();
        data.put("CaseAccessCategory", caseAccessCategory);
        return data;
    }

    private CaseDataContent createNewCaseDataContent(String caseAccessCategoryValue) {
        JsonNode caseAccessCategory = new TextNode(caseAccessCategoryValue);
        Map<String, JsonNode> data = new HashMap<>();
        data.put("CaseAccessCategory", caseAccessCategory);

        CaseDataContent caseDataContent = newCaseDataContent()
            .withEvent(EVENT)
            .withData(data)
            .withToken(TOKEN)
            .withIgnoreWarning(IGNORE)
            .build();
        return caseDataContent;
    }
}
