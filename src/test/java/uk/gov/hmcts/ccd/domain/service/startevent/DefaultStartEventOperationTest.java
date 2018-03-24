package uk.gov.hmcts.ccd.domain.service.startevent;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseEventTrigger;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewFieldBuilder;
import uk.gov.hmcts.ccd.domain.model.definition.*;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetEventTriggerOperation;
import uk.gov.hmcts.ccd.domain.service.callbacks.EventTokenService;
import uk.gov.hmcts.ccd.domain.service.common.CaseService;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.stdapi.CallbackInvoker;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

public class DefaultStartEventOperationTest {

//    private static final int UID = 1;
//    private static final String TEST_JURISDICTION_ID = "TestJurisdictionId";
//    private static final String TEST_CASE_TYPE_ID = "TestCaseTypeId";
//    private static final String TEST_EVENT_TRIGGER_ID = "TestEventTriggerId";
//    private static final String TEST_EVENT_TOKEN = "TestEventToken";
//    private static final boolean IGNORE_WARNING = false;
//    private static final String TEST_CASE_REFERENCE = "123456789012345";
//    private static final Long TEST_CASE_REFERENCE_L = Long.valueOf(TEST_CASE_REFERENCE);
//    private static final String CASE_STATE = "TestState";
//
//    @Mock
//    private GetCaseOperation getCaseOperation;
//
//    @Mock
//    private SecurityClassificationService classificationService;
//
//    @Mock
//    private EventTriggerService eventTriggerService;
//
//    @Mock
//    private CaseDefinitionRepository caseDefinitionRepository;
//
//    @Mock
//    private UIDefinitionRepository uiDefinitionRepository;
//
//    @Mock
//    private CaseViewFieldBuilder caseViewFieldBuilder;
//
//    @Mock
//    private CaseService caseService;
//
//    @Mock
//    private EventTokenService eventTokenService;
//
//    @Mock
//    private CallbackInvoker callbackInvoker;
//
//    @Mock
//    private UIDService uidService;
//
//    private GetEventTriggerOperation getEventTriggerOperation;
//
//    private final CaseDetails defaultCaseDetails = new CaseDetails();
//    private final Map<String, JsonNode> defaultData = Maps.newHashMap();
//    private Optional<CaseDetails> defaultCaseDetailsOpt;
//    private final CaseDetails classifiedCaseDetails = new CaseDetails();
//    private final Map<String, JsonNode> classifiedData = Maps.newHashMap();
//    private Optional<CaseDetails> classifiedCaseDetailsOpt;
//    private final CaseType caseType = new CaseType();
//    private final CaseEvent eventTrigger = new CaseEvent();
//    private final Jurisdiction jurisdiction = new Jurisdiction();
//    private final List<WizardPage> wizardPageCollection = newArrayList();
//    private final List<CaseViewField> caseViewFields = newArrayList();
//
//    @BeforeEach
//    void setUp() {
//        MockitoAnnotations.initMocks(this);
//        jurisdiction.setId(TEST_JURISDICTION_ID);
//        caseType.setJurisdiction(jurisdiction);
//        doReturn(caseType).when(caseDefinitionRepository).getCaseType(TEST_CASE_TYPE_ID);
//        doReturn(eventTrigger).when(eventTriggerService).findCaseEvent(caseType, TEST_EVENT_TRIGGER_ID);
//        doReturn(TEST_EVENT_TOKEN).when(eventTokenService).generateToken(UID, eventTrigger, jurisdiction, caseType);
//        defaultCaseDetails.setData(defaultData);
//        doReturn(defaultCaseDetails).when(caseService).createNewCaseDetails(eq(TEST_CASE_TYPE_ID),
//                                                                            eq(TEST_JURISDICTION_ID),
//                                                                            any(Map.class));
//        doNothing().when(callbackInvoker).invokeAboutToStartCallback(eventTrigger,
//                                                                     caseType,
//                                                                     defaultCaseDetails,
//                                                                     IGNORE_WARNING);
//        doReturn(true).when(eventTriggerService).isPreStateEmpty(eventTrigger);
//        doReturn(wizardPageCollection).when(uiDefinitionRepository).getWizardPageCollection(TEST_CASE_TYPE_ID,
//                                                                                            TEST_EVENT_TRIGGER_ID);
//        doReturn(caseViewFields).when(caseViewFieldBuilder).build(any(List.class), any(List.class), eq(defaultData));
//
//        getEventTriggerOperation = new GetEventTriggerOperation(getCaseOperation,
//                                                                caseDefinitionRepository,
//                                                                eventTriggerService,
//                                                                caseViewFieldBuilder,
//                                                                eventTokenService,
//                                                                callbackInvoker,
//                                                                caseService,
//                                                                uidService,
//                                                                uiDefinitionRepository,
//                                                                classificationService);
//    }
//
//    @Nested
//    @DisplayName("case type tests")
//    class getEventTriggerForCaseType {
//
//        @Test
//        @DisplayName("Should successfully get event trigger")
//        void shouldSuccessfullyGetEventTrigger() {
//
//            CaseEventTrigger caseEventTrigger = getEventTriggerOperation.executeForCaseType(UID,
//                                                                                            TEST_JURISDICTION_ID,
//                                                                                            TEST_CASE_TYPE_ID,
//                                                                                            TEST_EVENT_TRIGGER_ID,
//                                                                                            IGNORE_WARNING);
//            assertAll(
//                () -> assertThat(caseEventTrigger.getEventToken(), is(equalTo(TEST_EVENT_TOKEN))),
//                () -> assertThat(caseEventTrigger.getCaseFields(), is(equalTo(caseViewFields))),
//                () -> assertThat(caseEventTrigger.getWizardPages(), is(equalTo(wizardPageCollection)))
//            );
//        }
//
//        @Test
//        @DisplayName("Should fail to get event trigger if case type not found")
//        void shouldFailToGetEventTriggerIfCaseTypeNotFound() {
//
//            setupForCase();
//            doReturn(null).when(caseDefinitionRepository).getCaseType(TEST_CASE_TYPE_ID);
//
//            assertThrows(ResourceNotFoundException.class, () -> getEventTriggerOperation.executeForCaseType(UID,
//                                                                                                            TEST_JURISDICTION_ID,
//                                                                                                            TEST_CASE_TYPE_ID,
//                                                                                                            TEST_EVENT_TRIGGER_ID,
//                                                                                                            IGNORE_WARNING)
//            );
//        }
//
//        @Test
//        @DisplayName("Should fail to get event trigger if event trigger not found")
//        void shouldFailToGetEventTriggerIfEventTriggerNotFound() {
//
//            doReturn(null).when(eventTriggerService).findCaseEvent(caseType, TEST_EVENT_TRIGGER_ID);
//
//            assertThrows(ResourceNotFoundException.class, () -> getEventTriggerOperation.executeForCaseType(UID,
//                                                                                                            TEST_JURISDICTION_ID,
//                                                                                                            TEST_CASE_TYPE_ID,
//                                                                                                            TEST_EVENT_TRIGGER_ID,
//                                                                                                            IGNORE_WARNING)
//            );
//        }
//
//        @Test
//        @DisplayName("Should fail to get event trigger if invalid event trigger")
//        void shouldFailToGetEventTriggerIfInvalidEventTrigger() {
//
//            setupForCase();
//            doReturn(false).when(eventTriggerService).isPreStateEmpty(eventTrigger);
//
//            assertThrows(ValidationException.class, () -> getEventTriggerOperation.executeForCaseType(UID,
//                                                                                                      TEST_JURISDICTION_ID,
//                                                                                                      TEST_CASE_TYPE_ID,
//                                                                                                      TEST_EVENT_TRIGGER_ID,
//                                                                                                      IGNORE_WARNING)
//            );
//        }
//    }
//
//    @Nested
//    @DisplayName("case tests")
//    class getEventTriggerForCase {
//        @Test
//        @DisplayName("Should successfully get event trigger")
//        void shouldSuccessfullyGetEventTrigger() {
//
//            setupForCase();
//
//            CaseEventTrigger caseEventTrigger = getEventTriggerOperation.executeForCase(UID,
//                                                                                        TEST_CASE_REFERENCE,
//                                                                                        TEST_EVENT_TRIGGER_ID,
//                                                                                        IGNORE_WARNING);
//
//            assertAll(
//                () -> assertThat(caseEventTrigger.getEventToken(), is(equalTo(TEST_EVENT_TOKEN))),
//                () -> assertThat(caseEventTrigger.getCaseId(), is(equalTo(TEST_CASE_REFERENCE))),
//                () -> assertThat(caseEventTrigger.getCaseFields(), is(equalTo(caseViewFields))),
//                () -> assertThat(caseEventTrigger.getWizardPages(), is(equalTo(wizardPageCollection)))
//            );
//        }
//
//        @Test
//        @DisplayName("Should fail to get event trigger if default case not found")
//        void shouldFailToGetEventTriggerIfDefaultCaseNotFound() {
//
//            setupForCase();
//            doReturn(Optional.empty()).when(getCaseOperation).execute(TEST_CASE_REFERENCE);
//
//            assertThrows(ResourceNotFoundException.class, () -> getEventTriggerOperation.executeForCase(UID,
//                                                                                                        TEST_CASE_REFERENCE,
//                                                                                                        TEST_EVENT_TRIGGER_ID,
//                                                                                                        IGNORE_WARNING)
//            );
//        }
//
//        @Test
//        @DisplayName("Should fail to get event trigger if classified case not found")
//        void shouldFailToGetEventTriggerIfClassifiedCaseNotFound() {
//
//            setupForCase();
//            doReturn(Optional.empty()).when(classificationService).apply(defaultCaseDetails);
//
//            assertThrows(ResourceNotFoundException.class, () -> getEventTriggerOperation.executeForCase(UID,
//                                                                                                        TEST_CASE_REFERENCE,
//                                                                                                        TEST_EVENT_TRIGGER_ID,
//                                                                                                        IGNORE_WARNING)
//            );
//        }
//
//        @Test
//        @DisplayName("Should fail to get event trigger if UID not valid")
//        void shouldFailToGetEventTriggerIfUIDNotValid() {
//
//            setupForCase();
//            doReturn(false).when(uidService).validateUID(TEST_CASE_REFERENCE);
//
//            assertThrows(BadRequestException.class, () -> getEventTriggerOperation.executeForCase(UID,
//                                                                                                  TEST_CASE_REFERENCE,
//                                                                                                  TEST_EVENT_TRIGGER_ID,
//                                                                                                  IGNORE_WARNING)
//            );
//        }
//
//        @Test
//        @DisplayName("Should fail to get event trigger if case type not found")
//        void shouldFailToGetEventTriggerIfCaseTypeNotFound() {
//
//            setupForCase();
//            doReturn(null).when(caseDefinitionRepository).getCaseType(TEST_CASE_TYPE_ID);
//
//            assertThrows(ResourceNotFoundException.class, () -> getEventTriggerOperation.executeForCase(UID,
//                                                                                                        TEST_CASE_REFERENCE,
//                                                                                                        TEST_EVENT_TRIGGER_ID,
//                                                                                                        IGNORE_WARNING)
//            );
//        }
//
//        @Test
//        @DisplayName("Should fail to get event trigger if event trigger not found")
//        void shouldFailToGetEventTriggerIfEventTriggerNotFound() {
//
//            setupForCase();
//            doReturn(null).when(eventTriggerService).findCaseEvent(caseType, TEST_EVENT_TRIGGER_ID);
//
//            assertThrows(ResourceNotFoundException.class, () -> getEventTriggerOperation.executeForCase(UID,
//                                                                                                        TEST_CASE_REFERENCE,
//                                                                                                        TEST_EVENT_TRIGGER_ID,
//                                                                                                        IGNORE_WARNING)
//            );
//        }
//
//        @Test
//        @DisplayName("Should fail to get event trigger if invalid event trigger")
//        void shouldFailToGetEventTriggerIfInvalidEventTrigger() {
//
//            setupForCase();
//            doReturn(false).when(eventTriggerService).isPreStateValid(CASE_STATE, eventTrigger);
//
//            assertThrows(ValidationException.class, () -> getEventTriggerOperation.executeForCase(UID,
//                                                                                                  TEST_CASE_REFERENCE,
//                                                                                                  TEST_EVENT_TRIGGER_ID,
//                                                                                                  IGNORE_WARNING)
//            );
//        }
//    }
//
//    private void setupForCase() {
//        doReturn(true).when(uidService).validateUID(TEST_CASE_REFERENCE);
//        defaultCaseDetailsOpt = Optional.of(defaultCaseDetails);
//        defaultCaseDetails.setCaseTypeId(TEST_CASE_TYPE_ID);
//        doReturn(defaultCaseDetailsOpt).when(getCaseOperation).execute(TEST_CASE_REFERENCE);
//
//        classifiedCaseDetails.setState(CASE_STATE);
//        classifiedCaseDetails.setData(classifiedData);
//        classifiedCaseDetails.setReference(TEST_CASE_REFERENCE_L);
//        classifiedCaseDetailsOpt = Optional.of(classifiedCaseDetails);
//        doReturn(classifiedCaseDetailsOpt).when(classificationService).apply(defaultCaseDetails);
//        doReturn(true).when(eventTriggerService).isPreStateValid(CASE_STATE, eventTrigger);
//        doReturn(TEST_EVENT_TOKEN).when(eventTokenService).generateToken(UID, defaultCaseDetails, eventTrigger, jurisdiction, caseType);
//        doReturn(caseViewFields).when(caseViewFieldBuilder).build(any(List.class), any(List.class), eq(classifiedData));
//    }
}
