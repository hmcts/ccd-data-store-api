package uk.gov.hmcts.ccd.domain.service.createevent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.TestFixtures;
import uk.gov.hmcts.ccd.data.casedetails.CaseAuditEventRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;

import uk.gov.hmcts.ccd.data.casedetails.DefaultCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.IdamUser;
import uk.gov.hmcts.ccd.domain.model.callbacks.SignificantItem;
import uk.gov.hmcts.ccd.domain.model.callbacks.SignificantItemType;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.callbacks.EventTokenService;
import uk.gov.hmcts.ccd.domain.service.casedeletion.TimeToLiveService;
import uk.gov.hmcts.ccd.domain.service.caselinking.CaseLinkService;
import uk.gov.hmcts.ccd.domain.service.common.CaseDataService;
import uk.gov.hmcts.ccd.domain.service.common.CasePostStateService;
import uk.gov.hmcts.ccd.domain.service.common.CaseService;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationServiceImpl;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.getcasedocument.CaseDocumentService;
import uk.gov.hmcts.ccd.domain.service.getcasedocument.CaseDocumentTimestampService;
import uk.gov.hmcts.ccd.domain.service.jsonpath.CaseDetailsJsonParser;
import uk.gov.hmcts.ccd.domain.service.message.CaseEventMessageService;
import uk.gov.hmcts.ccd.domain.service.processor.FieldProcessorService;
import uk.gov.hmcts.ccd.domain.service.processor.GlobalSearchProcessorService;
import uk.gov.hmcts.ccd.domain.service.stdapi.AboutToSubmitCallbackResponse;
import uk.gov.hmcts.ccd.domain.service.stdapi.CallbackInvoker;
import uk.gov.hmcts.ccd.domain.service.validate.CaseDataIssueLogger;
import uk.gov.hmcts.ccd.domain.service.validate.ValidateCaseFieldsOperation;
import uk.gov.hmcts.ccd.domain.types.sanitiser.CaseSanitiser;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation;

import javax.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.model.std.EventBuilder.anEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.newCaseDataContent;
import static uk.gov.hmcts.ccd.domain.service.getcasedocument.CaseDocumentUtils.DOCUMENT_BINARY_URL;
import static uk.gov.hmcts.ccd.domain.service.getcasedocument.CaseDocumentUtils.DOCUMENT_URL;
import static uk.gov.hmcts.ccd.domain.service.getcasedocument.CaseDocumentUtils.UPLOAD_TIMESTAMP;

class CreateCaseEventServiceTest extends TestFixtures {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String USER_ID = "123";
    private static final String TOKEN = "eygeyvcey12w2";
    private static final Boolean IGNORE_WARNING = Boolean.TRUE;
    private static final String EVENT_ID = "UpdateCase";
    private static final String PRE_STATE_ID = "Created";
    private static final LocalDateTime LAST_MODIFIED = LocalDateTime.of(2015, 12, 21, 15, 30);
    private static final int CASE_VERSION = 0;
    private static final String ATTRIBUTE_PATH = "DocumentField";
    private static final String CATEGORY_ID = "categoryId";
    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final String VALID_DOCUMENT_URL = "https://dm.reform.hmcts.net/documents/a1-2Z-3-x";
    protected static final String NON_EXISTENT_CASE_REFERENCE = "1234123412341289";

    @Mock
    private CallbackInvoker callbackInvoker;
    @Mock
    private CaseAuditEventRepository caseAuditEventRepository;
    @Mock
    private CaseDataIssueLogger caseDataIssueLogger;
    @Mock
    private CaseDataService caseDataService;
    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;
    @Mock
    private CaseDetailsRepository caseDetailsRepository;
    @Mock
    private CaseDetailsJsonParser caseDetailsJsonParser;
    @Mock
    private CaseDocumentService caseDocumentService;
    @Mock
    private CaseEventMessageService caseEventMessageService;
    @Mock
    private CaseLinkService caseLinkService;
    @Mock
    private CasePostStateService casePostStateService;
    @Mock
    private CaseSanitiser caseSanitiser;
    @Mock
    private CaseService caseService;
    @Mock
    private CaseTypeService caseTypeService;
    @Mock
    private Clock clock;
    @Mock
    private EventTokenService eventTokenService;
    @Mock
    private EventTriggerService eventTriggerService;
    @Mock
    private FieldProcessorService fieldProcessorService;
    @Mock
    private GlobalSearchProcessorService globalSearchProcessorService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private SecurityClassificationServiceImpl securityClassificationService;
    @Mock
    private TimeToLiveService timeToLiveService;
    @Mock
    private UserAuthorisation userAuthorisation;
    @Mock
    private ApplicationParams applicationParams;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UIDService uidService;
    @Mock
    private ValidateCaseFieldsOperation validateCaseFieldsOperation;

    @Spy
    private CaseDocumentTimestampService caseDocumentTimestampService =
        new CaseDocumentTimestampService(Clock.systemDefaultZone(), new ApplicationParams());

    @InjectMocks
    private CreateCaseEventService underTest;

    private final Instant timestamp = Instant.parse("2018-08-19T16:02:42.00Z");
    private final Clock fixedClock = Clock.fixed(timestamp, ZoneOffset.UTC);
    private final LocalDateTime lastModifiedTimestamp = LocalDateTime.ofInstant(timestamp, ZoneOffset.UTC);

    private Event event;

    private Map<String, JsonNode> data;
    private CaseTypeDefinition caseTypeDefinition;
    private CaseEventDefinition caseEventDefinition;
    private CaseDetails caseDetails;
    private CaseDetails caseDetailsFromDB;
    private CaseDetails caseDetailsBefore;
    private CaseDataContent caseDataContent;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        event = buildEvent();
        data = buildJsonNodeData();
        caseDataContent = newCaseDataContent()
            .withEvent(event)
            .withData(data)
            .withToken(TOKEN)
            .withIgnoreWarning(IGNORE_WARNING)
            .build();
        caseTypeDefinition = buildCaseTypeDefinition();
        caseEventDefinition = buildCaseEventDefinition();
        SignificantItem significantItem = new SignificantItem();
        significantItem.setUrl("http://www.yahoo.com");
        significantItem.setDescription("description");
        significantItem.setType(SignificantItemType.DOCUMENT.name());
        AboutToSubmitCallbackResponse aboutToSubmitCallbackResponse = new AboutToSubmitCallbackResponse();
        aboutToSubmitCallbackResponse.setSignificantItem(significantItem);
        aboutToSubmitCallbackResponse.setState(Optional.empty());

        caseDetails = new CaseDetails();
        caseDetails.setReference(Long.parseLong(CASE_REFERENCE));
        caseDetails.setCaseTypeId(CASE_TYPE_ID);
        caseDetails.setState(PRE_STATE_ID);
        caseDetails.setLastModified(LAST_MODIFIED);
        caseDetails.setLastStateModifiedDate(LAST_MODIFIED);
        caseDetailsBefore = caseDetails.shallowClone();

        CaseStateDefinition postState = new CaseStateDefinition();
        postState.setId(POST_STATE);
        IdamUser user = new IdamUser();
        user.setId(USER_ID);

        doReturn(fixedClock.instant()).when(clock).instant();
        doReturn(fixedClock.getZone()).when(clock).getZone();

        doReturn(caseTypeDefinition).when(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);
        doReturn(true).when(caseTypeService).isJurisdictionValid(JURISDICTION_ID, caseTypeDefinition);
        doReturn(caseEventDefinition).when(eventTriggerService).findCaseEvent(caseTypeDefinition, EVENT_ID);
        doReturn(true).when(uidService).validateUID(CASE_REFERENCE);
        doReturn(Optional.of(caseDetails)).when(caseDetailsRepository).findByReference(CASE_REFERENCE);

        doReturn(true).when(eventTriggerService).isPreStateValid(PRE_STATE_ID, caseEventDefinition);
        doReturn(caseDetails).when(caseDetailsRepository).set(caseDetails);

        doReturn(postState).when(caseTypeService).findState(caseTypeDefinition, POST_STATE);
        doReturn(user).when(userRepository).getUser();
        doReturn(user).when(userRepository).getUserByUserId(anyString());
        doReturn(user).when(userRepository).getUser(anyString());
        doReturn(caseDetailsBefore).when(caseService).clone(caseDetails);
        doReturn(data).when(fieldProcessorService).processData(any(), any(), any(CaseEventDefinition.class));
        given(callbackInvoker.invokeAboutToSubmitCallback(any(),
            any(),
            any(),
            any(),
            any())).willReturn(aboutToSubmitCallbackResponse);
        doReturn(POST_STATE).when(this.casePostStateService)
            .evaluateCaseState(any(CaseEventDefinition.class), any(CaseDetails.class));

        doReturn(emptyMap()).when(caseSanitiser).sanitise(any(CaseTypeDefinition.class), anyMap());
        doReturn(caseDetails).when(caseDocumentService).stripDocumentHashes(any(CaseDetails.class));

        when(caseDocumentTimestampService.isCaseTypeUploadTimestampFeatureEnabled(any())).thenReturn(false);
    }

    @Test
    void testShouldCreateUpdatedCaseDetails() throws Exception {
        // GIVEN
        final Map<String, JsonNode> dbData = fromFileAsMap("A-case-detail-in-database.json");
        final Map<String, JsonNode> dataUpdate = fromFileAsMap("A-case-detail-update.json");
        final Map<String, JsonNode> expectedData = fromFileAsMap("A-case-detail-after-update.json");

        final CaseDetails caseDetails = buildCaseDetails(dbData);
        final CaseEventDefinition caseEventDefinition = buildCaseEventDefinition();
        final CaseTypeDefinition caseTypeDefinition = buildCaseTypeDefinition();

        doReturn(caseDetails).when(caseService).clone(caseDetails);
        doReturn(dataUpdate).when(caseSanitiser).sanitise(any(CaseTypeDefinition.class), anyMap());
        doReturn(emptyMap()).when(caseDataService)
            .getDefaultSecurityClassifications(any(CaseTypeDefinition.class), anyMap(), anyMap());
        doReturn(POST_STATE).when(casePostStateService)
            .evaluateCaseState(any(CaseEventDefinition.class), any(CaseDetails.class));
        doReturn(fixedClock.instant()).when(clock).instant();
        doReturn(fixedClock.getZone()).when(clock).getZone();

        Map<String, JsonNode> clonedData = new HashMap<>(caseDetails.getData());
        clonedData.putAll(dataUpdate);
        doReturn(clonedData).when(globalSearchProcessorService)
            .populateGlobalSearchData(any(CaseTypeDefinition.class), anyMap());

        // WHEN
        final CaseDetails updatedCaseDetails = underTest.mergeUpdatedFieldsToCaseDetails(
            emptyMap(),
            caseDetails,
            caseEventDefinition,
            caseTypeDefinition
        );

        // THEN
        assertThat(updatedCaseDetails)
            .isNotNull()
            .satisfies(x -> {
                assertThat(x.getData()).isNotNull().containsExactlyInAnyOrderEntriesOf(expectedData);
                assertThat(x.getDataClassification()).isNotNull().isEmpty();
                assertThat(x.getLastModified()).isEqualTo(lastModifiedTimestamp);
                assertThat(x.getState()).isEqualTo(POST_STATE);
            });

        verify(caseService).clone(caseDetails);
        verify(caseSanitiser).sanitise(any(CaseTypeDefinition.class), anyMap());
        verify(caseDataService)
            .getDefaultSecurityClassifications(any(CaseTypeDefinition.class), anyMap(), anyMap());
        verify(casePostStateService)
            .evaluateCaseState(any(CaseEventDefinition.class), any(CaseDetails.class));
        verify(clock).instant();
        verify(clock).getZone();
    }

    @Test
    @DisplayName("should update Last state modified")
    void shouldUpdateLastStateModifiedWhenStateTransitionOccurred() {
        caseDetailsBefore.setLastStateModifiedDate(LAST_MODIFIED);
        caseDetailsBefore.setState(PRE_STATE_ID);

        final CreateCaseEventResult caseEventResult = underTest.createCaseEvent(CASE_REFERENCE, caseDataContent);

        assertThat(caseEventResult.getSavedCaseDetails().getState()).isEqualTo(POST_STATE);
        assertThat(caseEventResult.getSavedCaseDetails().getLastStateModifiedDate())
            .isEqualTo(LAST_MODIFIED);
    }

    @Test
    @DisplayName("should not update Last state modified")
    void shouldNotUpdateLastStateModifiedWhenStateTransitionNotOccurred() {
        caseDetailsBefore.setLastStateModifiedDate(LAST_MODIFIED);
        caseDetailsBefore.setState(PRE_STATE_ID);
        caseEventDefinition = new CaseEventDefinition();
        doReturn(PRE_STATE_ID).when(this.casePostStateService)
            .evaluateCaseState(any(CaseEventDefinition.class), any(CaseDetails.class));

        CaseStateDefinition state = new CaseStateDefinition();
        state.setId(PRE_STATE_ID);

        doReturn(caseEventDefinition).when(eventTriggerService).findCaseEvent(caseTypeDefinition, EVENT_ID);
        doReturn(true).when(eventTriggerService).isPreStateValid(PRE_STATE_ID, caseEventDefinition);
        doReturn(state).when(caseTypeService).findState(caseTypeDefinition, PRE_STATE_ID);

        final CreateCaseEventResult caseEventResult = underTest.createCaseEvent(CASE_REFERENCE, caseDataContent);

        assertThat(caseEventResult.getSavedCaseDetails().getState()).isEqualTo(PRE_STATE_ID);
        assertThat(caseEventResult.getSavedCaseDetails().getLastStateModifiedDate()).isEqualTo(LAST_MODIFIED);
    }

    @Test
    @DisplayName("should invoke about to submit callback")
    void shouldInvokeAboutToSubmitCallback() {
        createCaseEvent();

        verify(callbackInvoker).invokeAboutToSubmitCallback(caseEventDefinition,
            caseDetailsBefore,
            caseDetails,
            caseTypeDefinition,
            IGNORE_WARNING);
        verify(globalSearchProcessorService).populateGlobalSearchData(any(CaseTypeDefinition.class), anyMap());
    }

    @Test
    @DisplayName("should update Last state modified")
    void shouldUpdateUserDetailsWhenOnBehalfOfUserTokenIsPassed() {
        final String userToken = "Test_Token";

        caseDataContent = newCaseDataContent()
            .withEvent(event)
            .withData(data)
            .withToken(TOKEN)
            .withIgnoreWarning(IGNORE_WARNING)
            .withOnBehalfOfUserToken(userToken)
            .build();

        final CreateCaseEventResult caseEventResult = underTest.createCaseEvent(CASE_REFERENCE, caseDataContent);

        verify(userRepository).getUser(userToken);
        verify(userRepository).getUser();
        verify(globalSearchProcessorService).populateGlobalSearchData(any(CaseTypeDefinition.class), anyMap());
        assertThat(caseEventResult.getSavedCaseDetails().getState()).isEqualTo(POST_STATE);
        assertThat(caseEventResult.getSavedCaseDetails().getLastStateModifiedDate())
            .isEqualTo(LAST_MODIFIED);
    }

    @Test
    @DisplayName("Should insert case links when case is modified")
    void shouldInsertCaseLinks() {

        // GIVEN
        final String userToken = "Test_Token";

        caseDataContent = newCaseDataContent()
            .withEvent(event)
            .withData(data)
            .withToken(TOKEN)
            .withIgnoreWarning(IGNORE_WARNING)
            .withOnBehalfOfUserToken(userToken)
            .build();

        // WHEN
        underTest.createCaseEvent(CASE_REFERENCE, caseDataContent);

        // THEN
        // i.e. verify same caseDetails passed to both saveCase and updateCaseLinks
        verify(caseDetailsRepository).set(caseDetails);
        verify(caseLinkService).updateCaseLinks(caseDetails, caseTypeDefinition.getCaseFieldDefinitions());
    }

    @Test
    @DisplayName("Should verify calls to increment TTL are made if CaseType is using TTL")
    void shouldVerifyTTLIncrementCallsMadeIfCaseTypeUsingTTL() {

        // GIVEN
        final String userToken = "Test_Token";

        caseDataContent = newCaseDataContent()
            .withEvent(event)
            .withData(data)
            .withToken(TOKEN)
            .withIgnoreWarning(IGNORE_WARNING)
            .withOnBehalfOfUserToken(userToken)
            .build();

        doReturn(true).when(timeToLiveService).isCaseTypeUsingTTL(any());

        // WHEN
        underTest.createCaseEvent(CASE_REFERENCE, caseDataContent);

        // THEN
        verify(timeToLiveService).isCaseTypeUsingTTL(any());
        verify(timeToLiveService).updateCaseDetailsWithTTL(any(), any(), any());
        verify(timeToLiveService).updateCaseDataClassificationWithTTL(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should verify calls to increment TTL are not made if CaseType is not using TTL")
    void shouldVerifyTTLIncrementCallsNotMadeIfCaseTypeNotUsingTTL() {

        // GIVEN
        final String userToken = "Test_Token";

        caseDataContent = newCaseDataContent()
            .withEvent(event)
            .withData(data)
            .withToken(TOKEN)
            .withIgnoreWarning(IGNORE_WARNING)
            .withOnBehalfOfUserToken(userToken)
            .build();

        doReturn(false).when(timeToLiveService).isCaseTypeUsingTTL(any());

        // WHEN
        underTest.createCaseEvent(CASE_REFERENCE, caseDataContent);

        // THEN
        verify(timeToLiveService).isCaseTypeUsingTTL(any());
        verify(timeToLiveService, never()).updateCaseDetailsWithTTL(any(), any(), any());
        verify(timeToLiveService, never()).updateCaseDataClassificationWithTTL(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should verify calls to validate TTL Suspension changes and set Resolved TTL")
    void shouldVerifyTTLSuspensionValidateAndSetResolvedTTL() {

        // GIVEN
        final String userToken = "Test_Token";

        caseDataContent = newCaseDataContent()
            .withEvent(event)
            .withData(data)
            .withToken(TOKEN)
            .withIgnoreWarning(IGNORE_WARNING)
            .withOnBehalfOfUserToken(userToken)
            .build();

        LocalDate expectedResolvedTTL = LocalDate.now().plusDays(100);
        doReturn(expectedResolvedTTL).when(timeToLiveService).getUpdatedResolvedTTL(any());

        // WHEN
        underTest.createCaseEvent(CASE_REFERENCE, caseDataContent);

        // THEN
        verify(timeToLiveService).validateTTLChangeAgainstTTLGuard(any(), any());
        verify(timeToLiveService).getUpdatedResolvedTTL(any());

        // verify saved case details reflects resolved TTL
        ArgumentCaptor<CaseDetails> captor = ArgumentCaptor.forClass(CaseDetails.class);
        verify(caseDetailsRepository).set(captor.capture());
        assertThat(captor.getValue().getResolvedTTL()).isEqualTo(expectedResolvedTTL);
    }

    @Test
    @DisplayName("should update case category id")
    void shouldUpdateCaseDocumentCategoryId() {
        CaseStateDefinition state = new CaseStateDefinition();
        state.setId(POST_STATE);
        doReturn(state).when(caseTypeService).findState(caseTypeDefinition, POST_STATE);
        caseDetails.setState(POST_STATE);
        doReturn(caseDetails).when(caseDocumentService).stripDocumentHashes(any(CaseDetails.class));

        final CreateCaseEventResult caseEventResult = underTest.createCaseSystemEvent(CASE_REFERENCE,
            ATTRIBUTE_PATH,
            CATEGORY_ID,
            new Event());

        assertThat(caseEventResult.getSavedCaseDetails().getState()).isEqualTo(POST_STATE);
    }


    @Test
    @DisplayName("should use onBehalfOfId")
    void shouldUpdateUserDetailsWhenOnBehalfOfIdIsPassed() {
        String onBehalfOfId = UUID.randomUUID().toString();

        caseDataContent = newCaseDataContent()
            .withEvent(event)
            .withData(data)
            .withToken(TOKEN)
            .withIgnoreWarning(IGNORE_WARNING)
            .withOnBehalfOfId(onBehalfOfId)
            .build();

        final CreateCaseEventResult caseEventResult = underTest.createCaseEvent(CASE_REFERENCE, caseDataContent);

        verify(userRepository).getUserByUserId(onBehalfOfId);
        verify(userRepository).getUser();
        verify(globalSearchProcessorService).populateGlobalSearchData(any(CaseTypeDefinition.class), anyMap());
        assertThat(caseEventResult.getSavedCaseDetails().getState()).isEqualTo(POST_STATE);
        assertThat(caseEventResult.getSavedCaseDetails().getLastStateModifiedDate())
            .isEqualTo(LAST_MODIFIED);
    }

    @Test
    @DisplayName("should update case category id by sending full data content")
    void shouldUpdateCaseDocumentCategoryIdBySendingFullDataContent() throws Exception {
        caseDetailsFromDB = caseDetails.shallowClone();
        Map<String, JsonNode> data = Maps.newHashMap();
        data.put("dataKey1", JSON_NODE_FACTORY.textNode("dataValue1"));
        caseDetailsFromDB.setData(data);
        CaseDetailsRepository defaultCaseDetailsRepository = mock(CaseDetailsRepository.class);

        doReturn(Optional.of(caseDetailsFromDB)).when(defaultCaseDetailsRepository).findByReference(CASE_REFERENCE);
        doReturn(Optional.of(caseDetails)).when(caseDetailsRepository).findByReference(CASE_REFERENCE);

        CaseStateDefinition state = new CaseStateDefinition();
        state.setId(POST_STATE);
        doReturn(state).when(caseTypeService).findState(caseTypeDefinition, POST_STATE);
        caseDetailsFromDB.setState(POST_STATE);
        doReturn(caseDetailsFromDB).when(caseDetailsRepository).set(any(CaseDetails.class));


        final CreateCaseEventResult caseEventResult = underTest.createCaseSystemEvent(CASE_REFERENCE,
            ATTRIBUTE_PATH,
            CATEGORY_ID,
            new Event());

        assertThat(caseEventResult.getSavedCaseDetails().getData()).isEqualTo(data);
        assertThat(caseEventResult.getSavedCaseDetails().getState()).isEqualTo(POST_STATE);
    }

    @Test
    @DisplayName("should throw Resource Not Found Exception when no case reference found")
    void shouldThrowResourceNotFoundExceptionWhenNoCaseReferenceFound() throws Exception {
        CaseDetailsRepository defaultCaseDetailsRepository = mock(DefaultCaseDetailsRepository.class);
        doReturn(Optional.empty()).when(defaultCaseDetailsRepository).findByReference(NON_EXISTENT_CASE_REFERENCE);
        assertThrows(ResourceNotFoundException.class, () -> {
            underTest.createCaseSystemEvent(NON_EXISTENT_CASE_REFERENCE,
                ATTRIBUTE_PATH,
                CATEGORY_ID,
                new Event());
        });

        assertAll(
            () -> verify(caseTypeService, times(0)).findState(any(), any()),
            () -> verify(caseDetailsRepository, times(0)).set(any()),
            () -> verify(caseDefinitionRepository, times(0)).getCaseType(any()));


    }

    @Test
    @DisplayName("should add upload timestamp to a document")
    void shouldAddUploadTimestampToDocument() throws Exception  {
        caseDetailsFromDB = caseDetails.shallowClone();
        Map<String, JsonNode> data = Maps.newHashMap();
        ObjectNode objectNode = createBasicDoc();
        data.put("dataKey1", objectNode);
        caseDetails.setData(data);
        CaseDetailsRepository defaultCaseDetailsRepository = mock(CaseDetailsRepository.class);

        doReturn(Optional.of(caseDetailsFromDB)).when(defaultCaseDetailsRepository).findByReference(CASE_REFERENCE);
        doReturn(Optional.of(caseDetails)).when(caseDetailsRepository).findByReference(CASE_REFERENCE);
        when(caseDocumentTimestampService.isCaseTypeUploadTimestampFeatureEnabled(any())).thenReturn(true);

        final CreateCaseEventResult caseEventResult = underTest.createCaseEvent(CASE_REFERENCE, caseDataContent);

        Collection<JsonNode> nodes = caseDocumentTimestampService.findNodes(
            caseEventResult.getSavedCaseDetails().getData().values());

        AtomicInteger countChanges = new AtomicInteger();
        nodes.forEach(node -> {
            countChanges.getAndIncrement();
            System.out.println("node - " + node.get(DOCUMENT_URL).asText() + " : " + node.get(UPLOAD_TIMESTAMP));
            assertTrue(node.has(UPLOAD_TIMESTAMP));
        });
        assertEquals(1, countChanges.get());
    }

    @Test
    @DisplayName("should not replace upload timestamp in a document")
    void shouldNotReplaceUploadTimestampInDocument() throws Exception {
        Map<String, JsonNode> data = Maps.newHashMap();
        ObjectNode objectNode = createBasicDoc();
        final String uploadTimestamp = "2001-01-01T01:02:03.00Z";
        objectNode.put(UPLOAD_TIMESTAMP, new TextNode(uploadTimestamp));
        data.put("dataKey1", objectNode);
        caseDetails.setData(data);
        caseDetailsFromDB = caseDetails.shallowClone();
        CaseDetailsRepository defaultCaseDetailsRepository = mock(CaseDetailsRepository.class);

        doReturn(Optional.of(caseDetailsFromDB)).when(defaultCaseDetailsRepository).findByReference(CASE_REFERENCE);
        doReturn(Optional.of(caseDetails)).when(caseDetailsRepository).findByReference(CASE_REFERENCE);

        final CreateCaseEventResult caseEventResult = underTest.createCaseEvent(CASE_REFERENCE, caseDataContent);

        Collection<JsonNode> nodes = caseDocumentTimestampService.findNodes(
            caseEventResult.getSavedCaseDetails().getData().values());

        AtomicInteger countChanges = new AtomicInteger();
        nodes.forEach(node -> {
            countChanges.getAndIncrement();
            System.out.println("node - " + node.get(DOCUMENT_URL).asText() + " : " + node.get(UPLOAD_TIMESTAMP));
            assertTrue(node.has(UPLOAD_TIMESTAMP));
            assertEquals(uploadTimestamp, node.get(UPLOAD_TIMESTAMP).asText());
        });
        assertEquals(1, countChanges.get());
    }

    private static Event buildEvent() {
        final Event event = anEvent().build();
        event.setEventId(EVENT_ID);
        event.setSummary("Update case summary");
        event.setDescription("Update case description");
        return event;
    }

    private void createCaseEvent() {
        underTest.createCaseEvent(CASE_REFERENCE, caseDataContent);
    }

    private Map<String, JsonNode> buildJsonNodeData() {
        return new HashMap<>();
    }

    private ObjectNode createBasicDoc() {
        ObjectNode node = MAPPER.createObjectNode();
        node.put(DOCUMENT_URL, new TextNode(VALID_DOCUMENT_URL));
        node.put(DOCUMENT_BINARY_URL, new TextNode(VALID_DOCUMENT_URL + "/binary"));
        node.put("document_filename", new TextNode("test-a5.pdf"));

        return node;
    }

}
