package uk.gov.hmcts.ccd.domain.service.aggregated;

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
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventTrigger;
import uk.gov.hmcts.ccd.domain.model.definition.*;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;
import uk.gov.hmcts.ccd.domain.service.startevent.StartEventOperation;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;

public class GetEventTriggerOperationTest {

    private static final String UID = "1";
    private static final String TEST_JURISDICTION_ID = "TestJurisdictionId";
    private static final String TEST_CASE_TYPE_ID = "TestCaseTypeId";
    private static final String TEST_EVENT_TRIGGER_ID = "TestEventTriggerId";
    private static final String TEST_EVENT_TOKEN = "TestEventToken";
    private static final boolean IGNORE_WARNING = false;
    private static final String TEST_CASE_REFERENCE = "123456789012345";
    private static final Long TEST_CASE_REFERENCE_L = Long.valueOf(TEST_CASE_REFERENCE);
    private static final String CASE_STATE = "TestState";

    @Mock
    private StartEventOperation startEventOperation;

    @Mock
    private EventTriggerService eventTriggerService;

    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;

    @Mock
    private UIDefinitionRepository uiDefinitionRepository;

    @Mock
    private CaseViewFieldBuilder caseViewFieldBuilder;

    private GetEventTriggerOperation getEventTriggerOperation;

    private final CaseDetails caseDetails = new CaseDetails();
    private final Map<String, JsonNode> data = Maps.newHashMap();
    private final CaseType caseType = new CaseType();
    private final CaseEvent eventTrigger = new CaseEvent();
    private final Jurisdiction jurisdiction = new Jurisdiction();
    private final List<WizardPage> wizardPageCollection = newArrayList();
    private final List<CaseViewField> caseViewFields = newArrayList();
    private final StartEventTrigger startEventTrigger = new StartEventTrigger();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        jurisdiction.setId(TEST_JURISDICTION_ID);
        caseType.setJurisdiction(jurisdiction);
        doReturn(caseType).when(caseDefinitionRepository).getCaseType(TEST_CASE_TYPE_ID);
        doReturn(eventTrigger).when(eventTriggerService).findCaseEvent(caseType, TEST_EVENT_TRIGGER_ID);
        caseDetails.setData(data);
        doReturn(wizardPageCollection).when(uiDefinitionRepository).getWizardPageCollection(TEST_CASE_TYPE_ID,
                                                                                            TEST_EVENT_TRIGGER_ID);
        doReturn(caseViewFields).when(caseViewFieldBuilder).build(any(List.class), any(List.class), eq(data));

        startEventTrigger.setCaseDetails(caseDetails);
        startEventTrigger.setEventId(TEST_EVENT_TRIGGER_ID);
        startEventTrigger.setToken(TEST_EVENT_TOKEN);
        doReturn(startEventTrigger).when(startEventOperation).triggerStartForCaseType(UID, TEST_JURISDICTION_ID, TEST_CASE_TYPE_ID, TEST_EVENT_TRIGGER_ID, IGNORE_WARNING);
        getEventTriggerOperation = new DefaultGetEventTriggerOperation(caseDefinitionRepository,
                                                                       eventTriggerService,
                                                                       caseViewFieldBuilder,
                                                                       uiDefinitionRepository,
                                                                       startEventOperation);
    }

    @Nested
    @DisplayName("case type tests")
    class getEventTriggerForCaseType {

        @Test
        @DisplayName("Should successfully get event trigger")
        void shouldSuccessfullyGetEventTrigger() {

            CaseEventTrigger caseEventTrigger = getEventTriggerOperation.executeForCaseType(UID,
                                                                                            TEST_JURISDICTION_ID,
                                                                                            TEST_CASE_TYPE_ID,
                                                                                            TEST_EVENT_TRIGGER_ID,
                                                                                            IGNORE_WARNING);
            assertAll(
                () -> assertThat(caseEventTrigger.getEventToken(), is(equalTo(TEST_EVENT_TOKEN))),
                () -> assertThat(caseEventTrigger.getCaseFields(), is(equalTo(caseViewFields))),
                () -> assertThat(caseEventTrigger.getWizardPages(), is(equalTo(wizardPageCollection)))
            );
        }
    }

    @Nested
    @DisplayName("case tests")
    class getEventTriggerForCase {
        @Test
        @DisplayName("Should successfully get event trigger")
        void shouldSuccessfullyGetEventTrigger() {

            setupForCase();

            CaseEventTrigger caseEventTrigger = getEventTriggerOperation.executeForCase(UID,
                                                                                        TEST_JURISDICTION_ID,
                                                                                        TEST_CASE_TYPE_ID,
                                                                                        TEST_CASE_REFERENCE,
                                                                                        TEST_EVENT_TRIGGER_ID,
                                                                                        IGNORE_WARNING);

            assertAll(
                () -> assertThat(caseEventTrigger.getEventToken(), is(equalTo(TEST_EVENT_TOKEN))),
                () -> assertThat(caseEventTrigger.getCaseId(), is(equalTo(TEST_CASE_REFERENCE))),
                () -> assertThat(caseEventTrigger.getCaseFields(), is(equalTo(caseViewFields))),
                () -> assertThat(caseEventTrigger.getWizardPages(), is(equalTo(wizardPageCollection)))
            );
        }
    }

    private void setupForCase() {

        caseDetails.setState(CASE_STATE);
        caseDetails.setData(data);
        caseDetails.setReference(TEST_CASE_REFERENCE_L);

        doReturn(startEventTrigger).when(startEventOperation).triggerStartForCase(UID, TEST_JURISDICTION_ID, TEST_CASE_TYPE_ID, TEST_CASE_REFERENCE, TEST_EVENT_TRIGGER_ID, IGNORE_WARNING);

        doReturn(true).when(eventTriggerService).isPreStateValid(CASE_STATE, eventTrigger);
        doReturn(caseViewFields).when(caseViewFieldBuilder).build(any(List.class), any(List.class), eq(data));
    }
}
