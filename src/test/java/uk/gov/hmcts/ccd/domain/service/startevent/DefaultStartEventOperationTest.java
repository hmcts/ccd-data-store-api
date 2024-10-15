package uk.gov.hmcts.ccd.domain.service.startevent;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.common.collect.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventFieldComplexDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.draft.CaseDraft;
import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.callbacks.EventTokenService;
import uk.gov.hmcts.ccd.domain.service.casedeletion.TimeToLiveService;
import uk.gov.hmcts.ccd.domain.service.common.CaseDataService;
import uk.gov.hmcts.ccd.domain.service.common.CaseService;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.domain.service.stdapi.CallbackInvoker;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.ccd.config.JacksonUtils.MAPPER;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.newCaseDataContent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDetailsBuilder.newCaseDetails;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDraftBuilder.newCaseDraft;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseEventBuilder.newCaseEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseEventFieldDefinitionBuilder.newCaseEventField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeBuilder.newCaseType;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.DraftResponseBuilder.newDraftResponse;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.JurisdictionBuilder.newJurisdiction;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.formatLogMessage;

public class DefaultStartEventOperationTest {

    private static final String UID = "1";
    private static final String TEST_JURISDICTION_ID = "TestJurisdictionId";
    private static final String TEST_CASE_TYPE_ID = "TestCaseTypeId";
    private static final String TEST_EVENT_TRIGGER_ID = "TestEventTriggerId";
    private static final String TEST_EVENT_TOKEN = "TestEventToken";
    private static final boolean IGNORE_WARNING = false;
    private static final String TEST_DRAFT_ID = "1";
    private static final String TEST_CASE_REFERENCE = "123456789012345";
    private static final String TEST_CASE_STATE = "TestState";
    private static final String PRIVATE = SecurityClassification.PRIVATE.name();
    private static final Map<String, JsonNode> DATA_CLASSIFICATION = Maps.newHashMap();
    private static final Map<String, JsonNode> DEFAULT_VALUE_DATA = expectedDefaultValue();
    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final int TTL_INCREMENT = 20;

    private static final Map<String, JsonNode> NEW_FIELDS_CLASSIFICATION =
        Map.of("key", JSON_NODE_FACTORY.textNode("value"));

    private static final Map<String, JsonNode> NEW_FIELDS_CLASSIFICATION_TTL =
        Map.of("key", JSON_NODE_FACTORY.textNode("value_with_ttl"));

    static Map<String, JsonNode> expectedDefaultValue() {
        Map<String, JsonNode> data = new HashMap<>();
        try {
            JsonNode changeOrgDefaultValue = MAPPER.readTree(""
                                                                 + "{"
                                                                 + "  \"Reason\": \"Good reason\","
                                                                 + "  \"OrganisationToAdd\": {"
                                                                 + "    \"OrganisationID\": \"Sol Firm 1\""
                                                                 + "  }"
                                                                 + "}");

            data.put("ChangeOrganisationRequestField", changeOrgDefaultValue);

            JsonNode orgPolicyDefaultValue = MAPPER.readTree("{"
                                                                 + "  \"Organisation\": {"
                                                                 + "    \"OrganisationID\": \"Sol Firm 2\""
                                                                 + "  },"
                                                                 + "  \"OrgPolicyCaseAssignedRole\": \"[Claimant]\""
                                                                 + "}");
            data.put("OrganisationPolicyField", orgPolicyDefaultValue);

            data.put("TextField0",  MAPPER.getNodeFactory().textNode("Default text"));

            data.put("TextField1",  MAPPER.getNodeFactory().textNode("Should not replace existing text"));

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return data;
    }

    static Map<String, JsonNode> existingCaseData() {
        Map<String, JsonNode> data = new HashMap<>();
        try {
            JsonNode changeOrg = MAPPER.readTree(""
                                                     + "{"
                                                     + "  \"Reason\": \"Old reason\","
                                                     + "  \"NotesReason\": null,"
                                                     + "  \"OrganisationToAdd\": {"
                                                     + "    \"OrganisationID\": null"
                                                     + "  }"
                                                     + "}");
            data.put("ChangeOrganisationRequestField", changeOrg);

            JsonNode orgPolicy = MAPPER.readTree("{"
                                                     + "  \"Organisation\": {"
                                                     + "    \"OrganisationID\": null"
                                                     + "  },"
                                                     + "  \"OrgPolicyReference\": null,"
                                                     + "  \"OrgPolicyCaseAssignedRole\": null"
                                                     + "}");
            data.put("OrganisationPolicyField", orgPolicy);

            data.put("TextField1",  MAPPER.getNodeFactory().textNode("Existing text"));

            data.put("TextField0",  MAPPER.getNodeFactory().nullNode());


        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return data;
    }

    static Map<String, JsonNode> ttlCaseData(String systemTtl) {
        Map<String, JsonNode> data = new HashMap<>();

        try {
            JsonNode ttl = MAPPER.readTree("{"
                + "  \"SystemTTL\": \"" + systemTtl + "\","
                + "  \"OverrideTTL\": \"2017-02-13\","
                + "  \"Suspended\": \"No\""
                + "}");

            data.put("TTL", ttl);
        } catch (JsonProcessingException ex) {
            ex.printStackTrace();
        }

        return data;
    }

    static Map<String, JsonNode> expectedAfterMergeWithDefaultValue() {
        Map<String, JsonNode> data = new HashMap<>();
        try {
            JsonNode changeOrg = MAPPER.readTree(""
                                                     + "{"
                                                     + "  \"Reason\": \"Good reason\","
                                                     + "  \"NotesReason\": null,"
                                                     + "  \"OrganisationToAdd\": {"
                                                     + "    \"OrganisationID\": \"Sol Firm 1\""
                                                     + "  }"
                                                     + "}");
            data.put("ChangeOrganisationRequestField", changeOrg);

            JsonNode orgPolicy = MAPPER.readTree("{"
                                                     + "  \"Organisation\": {"
                                                     + "    \"OrganisationID\": \"Sol Firm 2\""
                                                     + "  },"
                                                     + "  \"OrgPolicyReference\": null,"
                                                     + "  \"OrgPolicyCaseAssignedRole\": \"[Claimant]\""
                                                     + "}");
            data.put("OrganisationPolicyField", orgPolicy);

            data.put("TextField0",  MAPPER.getNodeFactory().textNode("Default text"));

            data.put("TextField1",  MAPPER.getNodeFactory().textNode("Existing text"));

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return data;
    }

    @Mock
    private EventTriggerService eventTriggerService;

    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;

    @Mock
    private CaseDetailsRepository caseDetailsRepository;

    @Mock
    private CaseService caseService;

    @Mock
    private UserAuthorisation userAuthorisation;

    @Mock
    private EventTokenService eventTokenService;

    @Mock
    private CallbackInvoker callbackInvoker;

    @Mock
    private DraftGateway draftGateway;

    @Mock
    private UIDService uidService;
    @Mock
    private CaseDataService caseDataService;
    @Mock
    private TimeToLiveService timeToLiveService;

    private DefaultStartEventOperation defaultStartEventOperation;

    private final CaseDetails caseDetails = newCaseDetails().withData(null).build();
    private final CaseTypeDefinition caseTypeDefinition = newCaseType().withCaseTypeId(TEST_CASE_TYPE_ID)
        .withJurisdiction(newJurisdiction().withJurisdictionId(TEST_JURISDICTION_ID).build()).build();
    private final CaseEventDefinition caseEventDefinition = newCaseEvent().withTTLIncrement(TTL_INCREMENT)
        .withCaseFields(
            Arrays.asList(newCaseEventField()
                          .withCaseFieldId("ChangeOrganisationRequestField")
                          .addCaseEventFieldComplexDefinitions(CaseEventFieldComplexDefinition.builder()
                                                                   .reference("Reason")
                                                                   .defaultValue("Good reason")
                                                                   .build())
                          .addCaseEventFieldComplexDefinitions(CaseEventFieldComplexDefinition.builder()
                                                                   .reference("NotesReason")
                                                                   .defaultValue(null)
                                                                   .build())
                          .addCaseEventFieldComplexDefinitions(CaseEventFieldComplexDefinition.builder()
                                                                   .reference("OrganisationToAdd.OrganisationID")
                                                                   .defaultValue("Sol Firm 1")
                                                                   .build())
                          .build(),
                      newCaseEventField()
                          .withCaseFieldId("OrganisationPolicyField")
                          .addCaseEventFieldComplexDefinitions(CaseEventFieldComplexDefinition.builder()
                                                                   .reference("Organisation.OrganisationID")
                                                                   .defaultValue("Sol Firm 2")
                                                                   .build())
                          .addCaseEventFieldComplexDefinitions(CaseEventFieldComplexDefinition.builder()
                                                                   .reference("OrgPolicyReference")
                                                                   .defaultValue(null)
                                                                   .build())
                          .addCaseEventFieldComplexDefinitions(CaseEventFieldComplexDefinition.builder()
                                                                   .reference("OrgPolicyCaseAssignedRole")
                                                                   .defaultValue("[Claimant]")
                                                                   .build())
                          .build(),
                      newCaseEventField()
                          .withCaseFieldId("TTL")
                          .addCaseEventFieldComplexDefinitions(CaseEventFieldComplexDefinition.builder()
                                                                   .reference("SystemTTL")
                                                                   .build())
                          .addCaseEventFieldComplexDefinitions(CaseEventFieldComplexDefinition.builder()
                                                                    .reference("OverrideTTL")
                                                                    .build())
                          .addCaseEventFieldComplexDefinitions(CaseEventFieldComplexDefinition.builder()
                                                                    .reference("Suspended")
                                                                    .build())
                          .build(),
                      newCaseEventField()
                          .withCaseFieldId("TextField0")
                          .withDefaultValue("Default text")
                          .build(),
                      newCaseEventField()
                          .withCaseFieldId("TextField1")
                          .withDefaultValue("Should not replace existing text")
                          .build()


        )
    ).build();
    private final CaseDataContent caseDataContent = newCaseDataContent()
        .withSecurityClassification(PRIVATE)
        .withData(existingCaseData())
        .withDataClassification(DATA_CLASSIFICATION)
        .build();
    private final CaseDraft caseDraft = newCaseDraft()
        .withCaseTypeId(TEST_CASE_TYPE_ID)
        .withEventId(TEST_EVENT_TRIGGER_ID)
        .withJurisdictionId(TEST_JURISDICTION_ID)
        .withCaseDataContent(caseDataContent)
        .build();
    private final DraftResponse draftResponse = newDraftResponse().withDocument(caseDraft).build();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        doReturn(DEFAULT_VALUE_DATA).when(caseService)
            .buildJsonFromCaseFieldsWithDefaultValue(caseEventDefinition.getCaseFields());
        doReturn(caseTypeDefinition).when(caseDefinitionRepository).getCaseType(TEST_CASE_TYPE_ID);
        doReturn(caseEventDefinition).when(eventTriggerService).findCaseEvent(caseTypeDefinition,
            TEST_EVENT_TRIGGER_ID);

        defaultStartEventOperation = new DefaultStartEventOperation(eventTokenService,
                                                                    caseDefinitionRepository,
                                                                    caseDetailsRepository,
                                                                    draftGateway,
                                                                    eventTriggerService,
                                                                    caseService,
                                                                    userAuthorisation,
                                                                    callbackInvoker,
                                                                    uidService,
                                                                    caseDataService,
                                                                    timeToLiveService);
    }

    @Nested
    @DisplayName("case type tests")
    class StartEventResultForCaseTypeDefinition {

        @BeforeEach
        void setUp() {
            doReturn(newCaseDetails().withData(Maps.newHashMap()).build()).when(caseService)
                .createNewCaseDetails(eq(TEST_CASE_TYPE_ID), eq(TEST_JURISDICTION_ID), eq(Maps.newHashMap()));
            doReturn(true).when(eventTriggerService).isPreStateEmpty(caseEventDefinition);
            doReturn(UID).when(userAuthorisation).getUserId();
            doReturn(TEST_EVENT_TOKEN).when(eventTokenService).generateToken(
                UID, caseEventDefinition, caseTypeDefinition.getJurisdictionDefinition(), caseTypeDefinition);

            doReturn(NEW_FIELDS_CLASSIFICATION).when(caseDataService)
                .getDefaultSecurityClassifications(eq(caseTypeDefinition), any(Map.class), any(Map.class));
        }

        @Test
        @DisplayName("Should successfully trigger start")
        void shouldSuccessfullyTriggerStart() {

            StartEventResult actual = defaultStartEventOperation.triggerStartForCaseType(TEST_CASE_TYPE_ID,
                                                                                          TEST_EVENT_TRIGGER_ID,
                                                                                          IGNORE_WARNING);
            assertAll(
                () -> verify(caseDefinitionRepository).getCaseType(TEST_CASE_TYPE_ID),
                () -> verify(eventTriggerService).findCaseEvent(caseTypeDefinition, TEST_EVENT_TRIGGER_ID),
                () -> verify(caseService).createNewCaseDetails(eq(TEST_CASE_TYPE_ID), eq(TEST_JURISDICTION_ID),
                    eq(Maps.newHashMap())),
                () -> verify(eventTriggerService).isPreStateEmpty(caseEventDefinition),
                () -> verify(eventTokenService).generateToken(UID, caseEventDefinition, caseTypeDefinition
                    .getJurisdictionDefinition(), caseTypeDefinition),
                () -> verify(callbackInvoker).invokeAboutToStartCallback(caseEventDefinition, caseTypeDefinition,
                                                                         actual.getCaseDetails(), IGNORE_WARNING),
                () -> assertThat(actual.getCaseDetails().getData(), is(equalTo(DEFAULT_VALUE_DATA))),
                () -> assertThat(actual.getToken(), is(equalTo(TEST_EVENT_TOKEN))),
                () -> assertThat(actual.getEventId(), is(equalTo(TEST_EVENT_TRIGGER_ID))),
                () -> assertThat(actual.getCaseDetails().getDataClassification(),
                    is(equalTo(NEW_FIELDS_CLASSIFICATION)))
            );
        }

        @Test
        @DisplayName("Should fail to trigger if case type not found")
        void shouldFailToTriggerIfCaseTypeNotFound() {
            doReturn(null).when(caseDefinitionRepository).getCaseType(TEST_CASE_TYPE_ID);

            final Exception exception =
                assertThrows(ResourceNotFoundException.class, () ->
                    defaultStartEventOperation.triggerStartForCaseType(TEST_CASE_TYPE_ID, TEST_EVENT_TRIGGER_ID,
                        IGNORE_WARNING)
            );
            assertThat(exception.getMessage(), startsWith("Cannot find case type definition for TestCaseTypeId"));
        }

        @Test
        @DisplayName("Should fail to trigger if event trigger not found")
        void shouldFailToTriggerIfEventTriggerNotFound() {

            doReturn(null).when(eventTriggerService).findCaseEvent(caseTypeDefinition,
                TEST_EVENT_TRIGGER_ID);

            Exception exception = assertThrows(ResourceNotFoundException.class, () ->
                defaultStartEventOperation.triggerStartForCaseType(TEST_CASE_TYPE_ID, TEST_EVENT_TRIGGER_ID,
                    IGNORE_WARNING)
            );
            assertThat(exception.getMessage(), startsWith("Cannot find event TestEventTriggerId for case type "
                + "TestCaseTypeId"));
        }

        @Test
        @DisplayName("Should fail to trigger if invalid event trigger")
        void shouldFailToTriggerIfInvalidEventTrigger() {

            doReturn(false).when(eventTriggerService).isPreStateEmpty(caseEventDefinition);

            Exception exception = assertThrows(ValidationException.class, () ->
                defaultStartEventOperation.triggerStartForCaseType(TEST_CASE_TYPE_ID, TEST_EVENT_TRIGGER_ID,
                    IGNORE_WARNING)
            );
            assertThat(exception.getMessage(), startsWith("The case status did not qualify for the event"));
        }
    }

    @Nested
    @DisplayName("case draft tests")
    class StartEventResultForDraft {

        @BeforeEach
        void setUp() {
            doReturn(true).when(eventTriggerService).isPreStateEmpty(caseEventDefinition);
            doReturn(TEST_EVENT_TOKEN).when(eventTokenService).generateToken(
                UID, caseEventDefinition, caseTypeDefinition.getJurisdictionDefinition(), caseTypeDefinition);
            doReturn(caseDetails).when(draftGateway).getCaseDetails(TEST_DRAFT_ID);
            doReturn(draftResponse).when(draftGateway).get(TEST_DRAFT_ID);
            caseDetails.setCaseTypeId(TEST_CASE_TYPE_ID);
            caseDetails.setJurisdiction(TEST_JURISDICTION_ID);
            caseDetails.setData(existingCaseData());
            caseDetails.setDataClassification(DATA_CLASSIFICATION);
            caseDetails.setSecurityClassification(SecurityClassification.PRIVATE);
            doReturn(UID).when(userAuthorisation).getUserId();

            doReturn(NEW_FIELDS_CLASSIFICATION).when(caseDataService)
                .getDefaultSecurityClassifications(eq(caseTypeDefinition), any(Map.class), any(Map.class));
        }

        @Test
        @DisplayName("Should successfully trigger start")
        void shouldSuccessfullyTriggerStart() {

            caseDetails.setData(existingCaseData());
            StartEventResult actual = defaultStartEventOperation.triggerStartForDraft(TEST_DRAFT_ID,
                                                                                       IGNORE_WARNING);
            assertAll(
                () -> verify(caseDefinitionRepository).getCaseType(TEST_CASE_TYPE_ID),
                () -> verify(eventTriggerService).findCaseEvent(caseTypeDefinition, TEST_EVENT_TRIGGER_ID),
                () -> verify(draftGateway).getCaseDetails(TEST_DRAFT_ID),
                () -> verify(eventTriggerService).isPreStateEmpty(caseEventDefinition),
                () -> verify(eventTokenService).generateToken(UID, caseEventDefinition, caseTypeDefinition
                    .getJurisdictionDefinition(), caseTypeDefinition),
                () -> verify(callbackInvoker).invokeAboutToStartCallback(eq(caseEventDefinition),
                    eq(caseTypeDefinition), any(CaseDetails.class), eq(IGNORE_WARNING)),
                () -> assertThat(actual.getCaseDetails(), hasProperty("securityClassification",
                    is(SecurityClassification.PRIVATE))),
                () -> assertThat(actual.getCaseDetails(), hasProperty("data",
                                                                      is(expectedAfterMergeWithDefaultValue()))),
                () -> assertThat(actual.getCaseDetails(), hasProperty("dataClassification",
                    is(NEW_FIELDS_CLASSIFICATION))),
                () -> assertThat(actual.getCaseDetails(), hasProperty("caseTypeId",
                    is(TEST_CASE_TYPE_ID))),
                () -> assertThat(actual.getCaseDetails(), hasProperty("jurisdiction",
                    is(TEST_JURISDICTION_ID))),
                () -> assertThat(actual.getToken(), is(equalTo(TEST_EVENT_TOKEN))),
                () -> assertThat(actual.getEventId(), is(equalTo(TEST_EVENT_TRIGGER_ID)))
            );
        }

        @Test
        @DisplayName("Should fail to trigger if case type not found")
        void shouldFailToTriggerIfCaseTypeNotFound() {
            doReturn(null).when(caseDefinitionRepository).getCaseType(TEST_CASE_TYPE_ID);

            final Exception exception =
                assertThrows(ResourceNotFoundException.class, () ->
                    defaultStartEventOperation.triggerStartForDraft(TEST_DRAFT_ID, IGNORE_WARNING)
            );
            assertThat(exception.getMessage(), startsWith("Cannot find case type definition for TestCaseTypeId"));
        }

        @Test
        @DisplayName("Should fail to trigger if event trigger not found")
        void shouldFailToTriggerIfEventTriggerNotFound() {

            doReturn(null).when(eventTriggerService).findCaseEvent(caseTypeDefinition,
                TEST_EVENT_TRIGGER_ID);

            Exception exception = assertThrows(ResourceNotFoundException.class, () ->
                defaultStartEventOperation.triggerStartForDraft(TEST_DRAFT_ID, IGNORE_WARNING));
            assertThat(exception.getMessage(), startsWith("Cannot find event TestEventTriggerId for case type "
                + "TestCaseTypeId"));
        }

        @Test
        @DisplayName("Should fail to trigger if invalid event trigger")
        void shouldFailToTriggerIfInvalidEventTrigger() {

            doReturn(false).when(eventTriggerService).isPreStateEmpty(caseEventDefinition);

            Exception exception = assertThrows(ValidationException.class, () ->
                defaultStartEventOperation.triggerStartForDraft(TEST_DRAFT_ID, IGNORE_WARNING)
            );
            assertThat(exception.getMessage(), startsWith("The case status did not qualify for the event"));
        }
    }

    @Nested
    @DisplayName("case tests")
    class StartEventResultForCase {

        private Map<String, JsonNode> expectedData;

        @BeforeEach
        void setUp() {

            Map<String, JsonNode> ttlData = new HashMap<>(existingCaseData());
            ttlData.putAll(ttlCaseData("2021-03-04"));
            caseDetails.setData(ttlData);

            expectedData = new HashMap<>(expectedAfterMergeWithDefaultValue());
            LocalDate twentyDaysFromNow = LocalDate.now().plusDays(TTL_INCREMENT);
            expectedData.putAll(ttlCaseData(twentyDaysFromNow.toString()));

            doReturn(expectedData)
                .when(timeToLiveService).updateCaseDetailsWithTTL(
                    anyMap(), any(CaseEventDefinition.class), any(CaseTypeDefinition.class)
                );

            doReturn(NEW_FIELDS_CLASSIFICATION_TTL)
                .when(timeToLiveService).updateCaseDataClassificationWithTTL(
                    anyMap(),
                    eq(NEW_FIELDS_CLASSIFICATION),
                    any(CaseEventDefinition.class),
                    any(CaseTypeDefinition.class)
                );

            doReturn(NEW_FIELDS_CLASSIFICATION).when(caseDataService)
                .getDefaultSecurityClassifications(eq(caseTypeDefinition),
                    eq(caseDetails.getData()), eq(caseDetails.getDataClassification()));


            caseDetails.setData(existingCaseData());
            caseDetails.setState(TEST_CASE_STATE);
            caseDetails.setCaseTypeId(TEST_CASE_TYPE_ID);
            caseDetails.setDataClassification(DATA_CLASSIFICATION);
            doReturn(true).when(uidService).validateUID(TEST_CASE_REFERENCE);
            doReturn(true).when(eventTriggerService).isPreStateValid(TEST_CASE_STATE, caseEventDefinition);
            doReturn(TEST_EVENT_TOKEN).when(eventTokenService).generateToken(
                UID, caseDetails, caseEventDefinition,
                caseTypeDefinition.getJurisdictionDefinition(), caseTypeDefinition);
            doReturn(true).when(uidService).validateUID(TEST_CASE_REFERENCE);
            doReturn(Optional.of(caseDetails)).when(caseDetailsRepository).findByReference(TEST_CASE_REFERENCE);
            doReturn(UID).when(userAuthorisation).getUserId();

            doReturn(NEW_FIELDS_CLASSIFICATION).when(caseDataService)
                .getDefaultSecurityClassifications(eq(caseTypeDefinition),
                    eq(caseDetails.getData()), eq(caseDetails.getDataClassification()));
        }

        @Test
        @DisplayName("Should successfully get event trigger")
        void shouldSuccessfullyGetEventTrigger() {

            StartEventResult actual = defaultStartEventOperation.triggerStartForCase(TEST_CASE_REFERENCE,
                                                                                      TEST_EVENT_TRIGGER_ID,
                                                                                      IGNORE_WARNING);

            assertAll(
                () -> verify(caseDefinitionRepository).getCaseType(TEST_CASE_TYPE_ID),
                () -> verify(eventTriggerService).findCaseEvent(caseTypeDefinition, TEST_EVENT_TRIGGER_ID),
                () -> verify(uidService).validateUID(TEST_CASE_REFERENCE),
                () -> verify(caseDetailsRepository).findByReference(TEST_CASE_REFERENCE),
                () -> verify(eventTriggerService).isPreStateValid(TEST_CASE_STATE, caseEventDefinition),
                () -> verify(eventTokenService).generateToken(UID, caseDetails,
                    caseEventDefinition, caseTypeDefinition.getJurisdictionDefinition(), caseTypeDefinition),
                () -> verify(callbackInvoker).invokeAboutToStartCallback(caseEventDefinition, caseTypeDefinition,
                                                                         actual.getCaseDetails(), IGNORE_WARNING),
                () -> verify(timeToLiveService).updateCaseDetailsWithTTL(
                    any(), eq(caseEventDefinition), eq(caseTypeDefinition)
                ),
                () -> verify(timeToLiveService).updateCaseDataClassificationWithTTL(
                    any(), any(), eq(caseEventDefinition), eq(caseTypeDefinition)
                ),
                () -> assertThat(actual.getCaseDetails(), is(equalTo(caseDetails))),
                () -> assertThat(actual.getCaseDetails().getData(), is(equalTo(expectedData))),
                () -> assertThat(actual.getCaseDetails().getDataClassification(),
                    is(equalTo(NEW_FIELDS_CLASSIFICATION_TTL))),
                () -> assertThat(actual.getToken(), is(equalTo(TEST_EVENT_TOKEN))),
                () -> assertThat(actual.getEventId(), is(equalTo(TEST_EVENT_TRIGGER_ID)))
            );
        }

        @Test
        @DisplayName("Should fail to trigger if case reference invalid")
        void shouldFailToTriggerIfCaseReferenceInvalid() {
            doReturn(false).when(uidService).validateUID(TEST_CASE_REFERENCE);

            final Exception exception = assertThrows(BadRequestException.class, () ->
                defaultStartEventOperation.triggerStartForCase(TEST_CASE_REFERENCE, TEST_EVENT_TRIGGER_ID,
                    IGNORE_WARNING)
            );
            assertThat(exception.getMessage(), startsWith("Case reference is not valid"));
        }

        @Test
        @DisplayName("Should fail to trigger if no case")
        void shouldFailToTriggerIfCaseNotFound() {
            doReturn(true).when(uidService).validateUID(TEST_CASE_REFERENCE);
            doReturn(Optional.empty()).when(caseDetailsRepository).findByReference(TEST_CASE_REFERENCE);

            final Exception exception = assertThrows(CaseNotFoundException.class, () ->
                defaultStartEventOperation.triggerStartForCase(TEST_CASE_REFERENCE, TEST_EVENT_TRIGGER_ID,
                    IGNORE_WARNING)
            );
            assertThat(exception.getMessage(), startsWith("No case found for reference: " + TEST_CASE_REFERENCE));
        }

        @Test
        @DisplayName("Should fail to trigger if case type not found")
        void shouldFailToTriggerIfCaseTypeNotFound() {
            doReturn(null).when(caseDefinitionRepository).getCaseType(TEST_CASE_TYPE_ID);

            final Exception exception =
                assertThrows(ResourceNotFoundException.class, () ->
                    defaultStartEventOperation.triggerStartForCase(TEST_CASE_REFERENCE, TEST_EVENT_TRIGGER_ID,
                        IGNORE_WARNING)
            );
            assertThat(exception.getMessage(), startsWith("Cannot find case type definition for TestCaseTypeId"));
        }

        @Test
        @DisplayName("Should fail to trigger if event trigger not found")
        void shouldFailToTriggerIfEventTriggerNotFound() {
            doReturn(null).when(eventTriggerService).findCaseEvent(caseTypeDefinition,
                TEST_EVENT_TRIGGER_ID);

            Exception exception = assertThrows(ResourceNotFoundException.class, () ->
                defaultStartEventOperation.triggerStartForCase(TEST_CASE_REFERENCE, TEST_EVENT_TRIGGER_ID,
                    IGNORE_WARNING)
            );
            assertThat(exception.getMessage(), startsWith("Cannot find event TestEventTriggerId for case type "
                + "TestCaseTypeId"));
        }

        @Test
        @DisplayName("Should fail to trigger if invalid case reference")
        void shouldFailToTriggerIfInvalidCaseReference() {
            doReturn(false).when(uidService).validateUID(TEST_CASE_REFERENCE);

            Exception exception = assertThrows(BadRequestException.class, () ->
                defaultStartEventOperation.triggerStartForCase(TEST_CASE_REFERENCE, TEST_EVENT_TRIGGER_ID,
                    IGNORE_WARNING)
            );
            assertThat(exception.getMessage(), startsWith("Case reference is not valid"));
        }

        @Test
        @DisplayName("Should fail to trigger if no case found")
        void shouldFailToTriggerIfNoCaseFound() {
            doReturn(false).when(uidService).validateUID(TEST_CASE_REFERENCE);

            Exception exception = assertThrows(BadRequestException.class, () ->
                defaultStartEventOperation.triggerStartForCase(TEST_CASE_REFERENCE, TEST_EVENT_TRIGGER_ID,
                    IGNORE_WARNING)
            );
            assertThat(exception.getMessage(), startsWith("Case reference is not valid"));
        }

        @Test
        @DisplayName("Should fail to trigger if invalid event trigger")
        void shouldFailToTriggerIfInvalidEventTrigger() {
            Logger logger = (Logger) LoggerFactory.getLogger(DefaultStartEventOperation.class);
            logger.setLevel(Level.ERROR);
            ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
            listAppender.start();
            logger.addAppender(listAppender);

            doReturn(false).when(eventTriggerService).isPreStateValid(TEST_CASE_STATE, caseEventDefinition);

            Exception exception = assertThrows(ValidationException.class, () ->
                defaultStartEventOperation.triggerStartForCase(TEST_CASE_REFERENCE, TEST_EVENT_TRIGGER_ID,
                    IGNORE_WARNING)
            );
            assertThat(exception.getMessage(), startsWith("The case status did not qualify for the event"));

            assertEquals(
                formatLogMessage("eventId={} cannot be triggered on case={} with currentStatus={}",
                                    TEST_CASE_REFERENCE, TEST_EVENT_TRIGGER_ID, TEST_CASE_STATE),
                listAppender.list.get(0).getFormattedMessage());

            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            loggerContext.stop();
        }
    }
}
