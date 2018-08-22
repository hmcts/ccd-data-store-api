package uk.gov.hmcts.ccd.domain.service.startevent;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventTrigger;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEvent;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.definition.DraftResponseToCaseDetailsBuilder;
import uk.gov.hmcts.ccd.domain.model.draft.CaseDraft;
import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.callbacks.EventTokenService;
import uk.gov.hmcts.ccd.domain.service.common.CaseService;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.stdapi.CallbackInvoker;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.newCaseDataContent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDetailsBuilder.newCaseDetails;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDraftBuilder.newCaseDraft;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseEventBuilder.anCaseEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeBuilder.newCaseType;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.DraftResponseBuilder.newDraftResponse;

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
    public static final String PRIVATE = SecurityClassification.PRIVATE.name();
    private static final Map<String, JsonNode> DATA = Maps.newHashMap();
    private static final Map<String, JsonNode> DATA_CLASSIFICATION = Maps.newHashMap();

    @Mock
    private EventTriggerService eventTriggerService;

    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;

    @Mock
    private CaseDetailsRepository caseDetailsRepository;

    @Mock
    private CaseService caseService;

    @Mock
    private CaseTypeService caseTypeService;

    @Mock
    private EventTokenService eventTokenService;

    @Mock
    private CallbackInvoker callbackInvoker;

    @Mock
    private DraftGateway draftGateway;

    @Mock
    private UIDService uidService;

    @Mock
    private DraftResponseToCaseDetailsBuilder draftResponseToCaseDetailsBuilder;

    private DefaultStartEventOperation defaultStartEventOperation;

    private final CaseDetails caseDetails = newCaseDetails().build();
    private final CaseType caseType = newCaseType().build();
    private final CaseEvent eventTrigger = anCaseEvent().build();
    private final CaseDataContent caseDataContent = newCaseDataContent()
        .withSecurityClassification(PRIVATE)
        .withData(DATA)
        .withDataClassification(DATA_CLASSIFICATION)
        .build();
    private final CaseDraft caseDraft = newCaseDraft()
        .withCaseTypeId(TEST_CASE_TYPE_ID)
        .withJurisdictionId(TEST_JURISDICTION_ID)
        .withCaseDataContent(caseDataContent)
        .build();
    private final DraftResponse draftResponse = newDraftResponse().withDocument(caseDraft).build();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        doReturn(caseType).when(caseDefinitionRepository).getCaseType(TEST_CASE_TYPE_ID);
        doReturn(eventTrigger).when(eventTriggerService).findCaseEvent(caseType, TEST_EVENT_TRIGGER_ID);
        doReturn(true).when(caseTypeService).isJurisdictionValid(TEST_JURISDICTION_ID, caseType);
        doNothing().when(callbackInvoker).invokeAboutToStartCallback(eventTrigger, caseType, caseDetails, IGNORE_WARNING);

        defaultStartEventOperation = new DefaultStartEventOperation(eventTokenService,
                                                                    caseDefinitionRepository,
                                                                    caseDetailsRepository,
                                                                    draftGateway,
                                                                    eventTriggerService,
                                                                    caseService,
                                                                    caseTypeService,
                                                                    callbackInvoker,
                                                                    uidService,
                                                                    draftResponseToCaseDetailsBuilder);
    }

    @Nested
    @DisplayName("case type tests")
    class StartEventTriggerForCaseType {

        @BeforeEach
        void setUp() {
            doReturn(caseDetails).when(caseService).createNewCaseDetails(eq(TEST_CASE_TYPE_ID), eq(TEST_JURISDICTION_ID), eq(Maps.newHashMap()));
            doReturn(true).when(eventTriggerService).isPreStateEmpty(eventTrigger);
            doReturn(TEST_EVENT_TOKEN).when(eventTokenService).generateToken(UID, eventTrigger, caseType.getJurisdiction(), caseType);
        }

        @Test
        @DisplayName("Should successfully trigger start")
        void shouldSuccessfullyTriggerStart() {

            StartEventTrigger actual = defaultStartEventOperation.triggerStartForCaseType(UID,
                                                                                          TEST_JURISDICTION_ID,
                                                                                          TEST_CASE_TYPE_ID,
                                                                                          TEST_EVENT_TRIGGER_ID,
                                                                                          IGNORE_WARNING);
            assertAll(
                () -> verify(caseDefinitionRepository).getCaseType(TEST_CASE_TYPE_ID),
                () -> verify(eventTriggerService).findCaseEvent(caseType, TEST_EVENT_TRIGGER_ID),
                () -> verify(caseTypeService).isJurisdictionValid(TEST_JURISDICTION_ID, caseType),
                () -> verify(caseService).createNewCaseDetails(eq(TEST_CASE_TYPE_ID), eq(TEST_JURISDICTION_ID), eq(Maps.newHashMap())),
                () -> verify(eventTriggerService).isPreStateEmpty(eventTrigger),
                () -> verify(eventTokenService).generateToken(UID, eventTrigger, caseType.getJurisdiction(), caseType),
                () -> verify(callbackInvoker).invokeAboutToStartCallback(eventTrigger, caseType, caseDetails, IGNORE_WARNING),
                () -> assertThat(actual.getCaseDetails(), is(equalTo(caseDetails))),
                () -> assertThat(actual.getToken(), is(equalTo(TEST_EVENT_TOKEN))),
                () -> assertThat(actual.getEventId(), is(equalTo(TEST_EVENT_TRIGGER_ID)))
            );
        }

        @Test
        @DisplayName("Should fail to trigger if case type not found")
        void shouldFailToTriggerIfCaseTypeNotFound() {
            doReturn(null).when(caseDefinitionRepository).getCaseType(TEST_CASE_TYPE_ID);

            final Exception exception = assertThrows(ResourceNotFoundException.class, () -> defaultStartEventOperation.triggerStartForCaseType(UID,
                                                                                                                                               TEST_JURISDICTION_ID,
                                                                                                                                               TEST_CASE_TYPE_ID,
                                                                                                                                               TEST_EVENT_TRIGGER_ID,
                                                                                                                                               IGNORE_WARNING)
            );
            assertThat(exception.getMessage(), startsWith("Cannot findCaseEvent case type definition for TestCaseTypeId"));
        }

        @Test
        @DisplayName("Should fail to trigger if event trigger not found")
        void shouldFailToTriggerIfEventTriggerNotFound() {

            doReturn(null).when(eventTriggerService).findCaseEvent(caseType, TEST_EVENT_TRIGGER_ID);

            Exception exception = assertThrows(ResourceNotFoundException.class, () -> defaultStartEventOperation.triggerStartForCaseType(UID,
                                                                                                                                         TEST_JURISDICTION_ID,
                                                                                                                                         TEST_CASE_TYPE_ID,
                                                                                                                                         TEST_EVENT_TRIGGER_ID,
                                                                                                                                         IGNORE_WARNING)
            );
            assertThat(exception.getMessage(), startsWith("Cannot findCaseEvent event TestEventTriggerId for case type TestCaseTypeId"));
        }

        @Test
        @DisplayName("Should fail to trigger if invalid jurisdiction")
        void shouldFailToTriggerIfInvalidJurisdiction() {
            doReturn(false).when(caseTypeService).isJurisdictionValid(TEST_JURISDICTION_ID, caseType);

            Exception exception = assertThrows(ValidationException.class, () -> defaultStartEventOperation.triggerStartForCaseType(UID,
                                                                                                                                   TEST_JURISDICTION_ID,
                                                                                                                                   TEST_CASE_TYPE_ID,
                                                                                                                                   TEST_EVENT_TRIGGER_ID,
                                                                                                                                   IGNORE_WARNING)
            );
            assertThat(exception.getMessage(), startsWith("TestCaseTypeId is not defined as a case type for TestJurisdictionId"));
        }

        @Test
        @DisplayName("Should fail to trigger if invalid event trigger")
        void shouldFailToTriggerIfInvalidEventTrigger() {

            doReturn(false).when(eventTriggerService).isPreStateEmpty(eventTrigger);

            Exception exception = assertThrows(ValidationException.class, () -> defaultStartEventOperation.triggerStartForCaseType(UID,
                                                                                                                                   TEST_JURISDICTION_ID,
                                                                                                                                   TEST_CASE_TYPE_ID,
                                                                                                                                   TEST_EVENT_TRIGGER_ID,
                                                                                                                                   IGNORE_WARNING)
            );
            assertThat(exception.getMessage(), startsWith("The case status did not qualify for the event"));
        }
    }

    @Nested
    @DisplayName("case draft tests")
    class StartEventTriggerForDraft {

        @BeforeEach
        void setUp() {
            doReturn(true).when(eventTriggerService).isPreStateEmpty(eventTrigger);
            doReturn(TEST_EVENT_TOKEN).when(eventTokenService).generateToken(UID, eventTrigger, caseType.getJurisdiction(), caseType);
            doReturn(draftResponse).when(draftGateway).get(TEST_DRAFT_ID);
            caseDetails.setCaseTypeId(TEST_CASE_TYPE_ID);
            caseDetails.setJurisdiction(TEST_JURISDICTION_ID);
            caseDetails.setData(DATA);
            caseDetails.setDataClassification(DATA_CLASSIFICATION);
            caseDetails.setSecurityClassification(SecurityClassification.PRIVATE);
            doReturn(caseDetails).when(draftResponseToCaseDetailsBuilder).build(draftResponse);
        }

        @Test
        @DisplayName("Should successfully trigger start")
        void shouldSuccessfullyTriggerStart() {

            StartEventTrigger actual = defaultStartEventOperation.triggerStartForDraft(UID,
                                                                                       TEST_JURISDICTION_ID,
                                                                                       TEST_CASE_TYPE_ID,
                                                                                       TEST_DRAFT_ID,
                                                                                       TEST_EVENT_TRIGGER_ID,
                                                                                       IGNORE_WARNING);
            assertAll(
                () -> verify(caseDefinitionRepository).getCaseType(TEST_CASE_TYPE_ID),
                () -> verify(eventTriggerService).findCaseEvent(caseType, TEST_EVENT_TRIGGER_ID),
                () -> verify(caseTypeService).isJurisdictionValid(TEST_JURISDICTION_ID, caseType),
                () -> verify(draftGateway).get(TEST_DRAFT_ID),
                () -> verify(eventTriggerService).isPreStateEmpty(eventTrigger),
                () -> verify(eventTokenService).generateToken(UID, eventTrigger, caseType.getJurisdiction(), caseType),
                () -> verify(callbackInvoker).invokeAboutToStartCallback(eq(eventTrigger), eq(caseType), any(CaseDetails.class), eq(IGNORE_WARNING)),
                () -> assertThat(actual.getCaseDetails(), hasProperty("securityClassification", is(SecurityClassification.PRIVATE))),
                () -> assertThat(actual.getCaseDetails(), hasProperty("data", is(DATA))),
                () -> assertThat(actual.getCaseDetails(), hasProperty("dataClassification", is(DATA_CLASSIFICATION))),
                () -> assertThat(actual.getCaseDetails(), hasProperty("caseTypeId", is(TEST_CASE_TYPE_ID))),
                () -> assertThat(actual.getCaseDetails(), hasProperty("jurisdiction", is(TEST_JURISDICTION_ID))),
                () -> assertThat(actual.getToken(), is(equalTo(TEST_EVENT_TOKEN))),
                () -> assertThat(actual.getEventId(), is(equalTo(TEST_EVENT_TRIGGER_ID)))
            );
        }

        @Test
        @DisplayName("Should fail to trigger if case type not found")
        void shouldFailToTriggerIfCaseTypeNotFound() {
            doReturn(null).when(caseDefinitionRepository).getCaseType(TEST_CASE_TYPE_ID);

            final Exception exception = assertThrows(ResourceNotFoundException.class,
                                                     () -> defaultStartEventOperation.triggerStartForDraft(UID,
                                                                                                           TEST_JURISDICTION_ID,
                                                                                                           TEST_CASE_TYPE_ID,
                                                                                                           TEST_DRAFT_ID,
                                                                                                           TEST_EVENT_TRIGGER_ID,
                                                                                                           IGNORE_WARNING)
            );
            assertThat(exception.getMessage(), startsWith("Cannot findCaseEvent case type definition for TestCaseTypeId"));
        }

        @Test
        @DisplayName("Should fail to trigger if event trigger not found")
        void shouldFailToTriggerIfEventTriggerNotFound() {

            doReturn(null).when(eventTriggerService).findCaseEvent(caseType, TEST_EVENT_TRIGGER_ID);

            Exception exception = assertThrows(ResourceNotFoundException.class, () -> defaultStartEventOperation.triggerStartForDraft(UID,
                                                                                                                                      TEST_JURISDICTION_ID,
                                                                                                                                      TEST_CASE_TYPE_ID,
                                                                                                                                      TEST_DRAFT_ID,
                                                                                                                                      TEST_EVENT_TRIGGER_ID,
                                                                                                                                      IGNORE_WARNING)
            );
            assertThat(exception.getMessage(), startsWith("Cannot findCaseEvent event TestEventTriggerId for case type TestCaseTypeId"));
        }

        @Test
        @DisplayName("Should fail to trigger if invalid jurisdiction")
        void shouldFailToTriggerIfInvalidJurisdiction() {
            doReturn(false).when(caseTypeService).isJurisdictionValid(TEST_JURISDICTION_ID, caseType);

            Exception exception = assertThrows(ValidationException.class, () -> defaultStartEventOperation.triggerStartForDraft(UID,
                                                                                                                                TEST_JURISDICTION_ID,
                                                                                                                                TEST_CASE_TYPE_ID,
                                                                                                                                TEST_DRAFT_ID,
                                                                                                                                TEST_EVENT_TRIGGER_ID,
                                                                                                                                IGNORE_WARNING)
            );
            assertThat(exception.getMessage(), startsWith("TestCaseTypeId is not defined as a case type for TestJurisdictionId"));
        }

        @Test
        @DisplayName("Should fail to trigger if invalid event trigger")
        void shouldFailToTriggerIfInvalidEventTrigger() {

            doReturn(false).when(eventTriggerService).isPreStateEmpty(eventTrigger);

            Exception exception = assertThrows(ValidationException.class, () -> defaultStartEventOperation.triggerStartForDraft(UID,
                                                                                                                                TEST_JURISDICTION_ID,
                                                                                                                                TEST_CASE_TYPE_ID,
                                                                                                                                TEST_DRAFT_ID,
                                                                                                                                TEST_EVENT_TRIGGER_ID,
                                                                                                                                IGNORE_WARNING)
            );
            assertThat(exception.getMessage(), startsWith("The case status did not qualify for the event"));
        }
    }

    @Nested
    @DisplayName("case tests")
    class StartEventTriggerForCase {


        @BeforeEach
        void setUp() {
            caseDetails.setState(TEST_CASE_STATE);
            doReturn(true).when(uidService).validateUID(TEST_CASE_REFERENCE);
            doReturn(caseDetails).when(caseDetailsRepository).findUniqueCase(TEST_JURISDICTION_ID, TEST_CASE_TYPE_ID, TEST_CASE_REFERENCE);
            doReturn(true).when(eventTriggerService).isPreStateValid(TEST_CASE_STATE, eventTrigger);
            doReturn(TEST_EVENT_TOKEN).when(eventTokenService).generateToken(UID, caseDetails, eventTrigger, caseType.getJurisdiction(), caseType);
        }

        @Test
        @DisplayName("Should successfully get event trigger")
        void shouldSuccessfullyGetEventTrigger() {

            StartEventTrigger actual = defaultStartEventOperation.triggerStartForCase(UID,
                                                                                      TEST_JURISDICTION_ID,
                                                                                      TEST_CASE_TYPE_ID,
                                                                                      TEST_CASE_REFERENCE,
                                                                                      TEST_EVENT_TRIGGER_ID,
                                                                                      IGNORE_WARNING);

            assertAll(
                () -> verify(caseDefinitionRepository).getCaseType(TEST_CASE_TYPE_ID),
                () -> verify(eventTriggerService).findCaseEvent(caseType, TEST_EVENT_TRIGGER_ID),
                () -> verify(caseTypeService).isJurisdictionValid(TEST_JURISDICTION_ID, caseType),
                () -> verify(uidService).validateUID(TEST_CASE_REFERENCE),
                () -> verify(caseDetailsRepository).findUniqueCase(TEST_JURISDICTION_ID, TEST_CASE_TYPE_ID, TEST_CASE_REFERENCE),
                () -> verify(eventTriggerService).isPreStateValid(TEST_CASE_STATE, eventTrigger),
                () -> verify(eventTokenService).generateToken(UID, caseDetails, eventTrigger, caseType.getJurisdiction(), caseType),
                () -> verify(callbackInvoker).invokeAboutToStartCallback(eventTrigger, caseType, caseDetails, IGNORE_WARNING),
                () -> assertThat(actual.getCaseDetails(), is(equalTo(caseDetails))),
                () -> assertThat(actual.getToken(), is(equalTo(TEST_EVENT_TOKEN))),
                () -> assertThat(actual.getEventId(), is(equalTo(TEST_EVENT_TRIGGER_ID)))
            );
        }


        @Test
        @DisplayName("Should fail to trigger if case type not found")
        void shouldFailToTriggerIfCaseTypeNotFound() {
            doReturn(null).when(caseDefinitionRepository).getCaseType(TEST_CASE_TYPE_ID);

            final Exception exception = assertThrows(ResourceNotFoundException.class, () -> defaultStartEventOperation.triggerStartForCase(UID,
                                                                                                                                           TEST_JURISDICTION_ID,
                                                                                                                                           TEST_CASE_TYPE_ID,
                                                                                                                                           TEST_CASE_REFERENCE,
                                                                                                                                           TEST_EVENT_TRIGGER_ID,
                                                                                                                                           IGNORE_WARNING)
            );
            assertThat(exception.getMessage(), startsWith("Cannot findCaseEvent case type definition for TestCaseTypeId"));
        }

        @Test
        @DisplayName("Should fail to trigger if event trigger not found")
        void shouldFailToTriggerIfEventTriggerNotFound() {
            doReturn(null).when(eventTriggerService).findCaseEvent(caseType, TEST_EVENT_TRIGGER_ID);

            Exception exception = assertThrows(ResourceNotFoundException.class, () -> defaultStartEventOperation.triggerStartForCase(UID,
                                                                                                                                     TEST_JURISDICTION_ID,
                                                                                                                                     TEST_CASE_TYPE_ID,
                                                                                                                                     TEST_CASE_REFERENCE,
                                                                                                                                     TEST_EVENT_TRIGGER_ID,
                                                                                                                                     IGNORE_WARNING)
            );
            assertThat(exception.getMessage(), startsWith("Cannot findCaseEvent event TestEventTriggerId for case type TestCaseTypeId"));
        }

        @Test
        @DisplayName("Should fail to trigger if invalid jurisdiction")
        void shouldFailToTriggerIfInvalidJurisdiction() {
            doReturn(false).when(caseTypeService).isJurisdictionValid(TEST_JURISDICTION_ID, caseType);

            Exception exception = assertThrows(ValidationException.class, () -> defaultStartEventOperation.triggerStartForCase(UID,
                                                                                                                               TEST_JURISDICTION_ID,
                                                                                                                               TEST_CASE_TYPE_ID,
                                                                                                                               TEST_CASE_REFERENCE,
                                                                                                                               TEST_EVENT_TRIGGER_ID,
                                                                                                                               IGNORE_WARNING)
            );
            assertThat(exception.getMessage(), startsWith("TestCaseTypeId is not defined as a case type for TestJurisdictionId"));
        }

        @Test
        @DisplayName("Should fail to trigger if invalid case reference")
        void shouldFailToTriggerIfInvalidCaseReference() {
            doReturn(false).when(uidService).validateUID(TEST_CASE_REFERENCE);

            Exception exception = assertThrows(BadRequestException.class, () -> defaultStartEventOperation.triggerStartForCase(UID,
                                                                                                                               TEST_JURISDICTION_ID,
                                                                                                                               TEST_CASE_TYPE_ID,
                                                                                                                               TEST_CASE_REFERENCE,
                                                                                                                               TEST_EVENT_TRIGGER_ID,
                                                                                                                               IGNORE_WARNING)
            );
            assertThat(exception.getMessage(), startsWith("Case reference is not valid"));
        }

        @Test
        @DisplayName("Should fail to trigger if no case found")
        void shouldFailToTriggerIfNoCaseFound() {
            doReturn(false).when(uidService).validateUID(TEST_CASE_REFERENCE);

            Exception exception = assertThrows(BadRequestException.class, () -> defaultStartEventOperation.triggerStartForCase(UID,
                                                                                                                               TEST_JURISDICTION_ID,
                                                                                                                               TEST_CASE_TYPE_ID,
                                                                                                                               TEST_CASE_REFERENCE,
                                                                                                                               TEST_EVENT_TRIGGER_ID,
                                                                                                                               IGNORE_WARNING)
            );
            assertThat(exception.getMessage(), startsWith("Case reference is not valid"));
        }

        @Test
        @DisplayName("Should fail to trigger if invalid event trigger")
        void shouldFailToTriggerIfInvalidEventTrigger() {
            doReturn(null).when(caseDetailsRepository).findUniqueCase(TEST_JURISDICTION_ID, TEST_CASE_TYPE_ID, TEST_CASE_REFERENCE);

            Exception exception = assertThrows(ResourceNotFoundException.class, () -> defaultStartEventOperation.triggerStartForCase(UID,
                                                                                                                                     TEST_JURISDICTION_ID,
                                                                                                                                     TEST_CASE_TYPE_ID,
                                                                                                                                     TEST_CASE_REFERENCE,
                                                                                                                                     TEST_EVENT_TRIGGER_ID,
                                                                                                                                     IGNORE_WARNING)
            );
            assertThat(exception.getMessage(), startsWith("No case exist with id=123456789012345"));
        }
    }
}
