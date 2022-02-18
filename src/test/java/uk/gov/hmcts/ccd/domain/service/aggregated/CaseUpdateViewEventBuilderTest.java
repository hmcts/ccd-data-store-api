package uk.gov.hmcts.ccd.domain.service.aggregated;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseUpdateViewEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseUpdateViewEventBuilder;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewFieldBuilder;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPage;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;
import uk.gov.hmcts.ccd.domain.service.processor.FieldProcessorService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.model.callbacks.EventTokenProperties.CASE_TYPE_ID;
import static uk.gov.hmcts.ccd.domain.service.aggregated.SearchResultUtil.buildData;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDetailsBuilder.newCaseDetails;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseEventBuilder.newCaseEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseStateBuilder.newState;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeBuilder.newCaseType;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.StartEventResultBuilder.newStartEventTrigger;

class CaseUpdateViewEventBuilderTest {

    private static final String TOKEN = "testToken";
    private static final String EVENT_TRIGGER_ID = "testEventTriggerId";
    private static final String EVENT_TRIGGER_NAME = "testEventTriggerName";
    private static final String EVENT_TRIGGER_DESCRIPTION = "testEventTriggerDescription";
    private static final Boolean EVENT_TRIGGER_SHOW_SUMMARY = true;
    private static final Boolean EVENT_TRIGGER_SHOW_EVENT_NOTES = false;
    private static final String CASE_REFERENCE = "1234567891012345";
    private final CaseEventDefinition caseEventDefinition = newCaseEvent()
        .withId(EVENT_TRIGGER_ID)
        .withName(EVENT_TRIGGER_NAME)
        .withDescription(EVENT_TRIGGER_DESCRIPTION)
        .withShowSummary(EVENT_TRIGGER_SHOW_SUMMARY)
        .withShowEventNotes(EVENT_TRIGGER_SHOW_EVENT_NOTES)
        .build();
    private final List<CaseEventDefinition> events = Lists.newArrayList(newCaseEvent().build());
    private final List<CaseFieldDefinition> caseFieldDefinitions = Lists.newArrayList(newCaseField().build());
    private final List<CaseEventFieldDefinition> eventFields = Lists.newArrayList();
    private final CaseTypeDefinition caseTypeDefinition = newCaseType()
                                                            .withCaseTypeId(CASE_TYPE_ID)
                                                            .withEvents(events)
                                                            .withCaseFields(caseFieldDefinitions).build();
    private final CaseDetails caseDetails = newCaseDetails().withCaseTypeId(CASE_TYPE_ID).build();
    private final StartEventResult startEventResult = newStartEventTrigger().withCaseDetails(caseDetails)
        .withEventToken(TOKEN).build();
    private final List<WizardPage> wizardPageCollection = Lists.newArrayList();
    private final List<CaseViewField> viewFields = Lists.newArrayList();
    private static final String STATE_ID = "STATE_ID";
    private static final String TITLE_DISPLAY = "titleDisplay";
    private final CaseStateDefinition caseStateDefinition = newState().withId(STATE_ID).build();

    private static final String SUPPLEMENTARY_DATA_FIELD_1 = "Supplementary data field 1";
    private static final String SUPPLEMENTARY_DATA_FIELD_2 = "Supplementary data field 2";

    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;

    @Mock
    private UIDefinitionRepository uiDefinitionRepository;

    @Mock
    private EventTriggerService eventTriggerService;

    @Mock
    private CaseViewFieldBuilder caseViewFieldBuilder;

    private CaseUpdateViewEventBuilder caseUpdateViewEventBuilder;
    @Mock
    private FieldProcessorService fieldProcessorService;
    @Mock
    private CaseTypeService caseTypeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        when(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).thenReturn(caseTypeDefinition);
        when(eventTriggerService.findCaseEvent(caseTypeDefinition, EVENT_TRIGGER_ID)).thenReturn(caseEventDefinition);
        when(uiDefinitionRepository.getWizardPageCollection(CASE_TYPE_ID, EVENT_TRIGGER_ID))
            .thenReturn(wizardPageCollection);
        when(caseViewFieldBuilder.build(caseFieldDefinitions, eventFields, caseDetails.getData()))
            .thenReturn(viewFields);
        caseDetails.setState(STATE_ID);
        caseStateDefinition.setTitleDisplay(TITLE_DISPLAY);
        when(caseTypeService.findState(caseTypeDefinition, STATE_ID)).thenReturn(caseStateDefinition);

        caseUpdateViewEventBuilder = new CaseUpdateViewEventBuilder(caseDefinitionRepository,
                                                              uiDefinitionRepository,
                                                              eventTriggerService,
                                                              caseViewFieldBuilder, fieldProcessorService,
                                                              caseTypeService);
    }

    @Test
    @DisplayName("should build trigger")
    void shouldBuildTrigger() {

        final CaseUpdateViewEvent caseUpdateViewEvent = caseUpdateViewEventBuilder.build(startEventResult,
                                                                                CASE_TYPE_ID,
                                                                                EVENT_TRIGGER_ID,
                                                                                CASE_REFERENCE);
        InOrder inOrder = inOrder(caseDefinitionRepository,
                                  uiDefinitionRepository,
                                  eventTriggerService,
                                  caseViewFieldBuilder);
        assertAll(
            () -> assertThat(caseUpdateViewEvent, hasProperty("id", equalTo(EVENT_TRIGGER_ID))),
            () -> assertThat(caseUpdateViewEvent, hasProperty("name", equalTo(EVENT_TRIGGER_NAME))),
            () -> assertThat(caseUpdateViewEvent, hasProperty("description", equalTo(EVENT_TRIGGER_DESCRIPTION))),
            () -> assertThat(caseUpdateViewEvent, hasProperty("showSummary", equalTo(EVENT_TRIGGER_SHOW_SUMMARY))),
            () -> assertThat(caseUpdateViewEvent, hasProperty("showEventNotes",
                equalTo(EVENT_TRIGGER_SHOW_EVENT_NOTES))),
            () -> assertThat(caseUpdateViewEvent, hasProperty("eventToken", equalTo(TOKEN))),
            () -> assertThat(caseUpdateViewEvent, hasProperty("caseId", equalTo(CASE_REFERENCE))),
            () -> assertThat(caseUpdateViewEvent, hasProperty("caseFields", equalTo(viewFields))),
            () -> assertThat(caseUpdateViewEvent, hasProperty("wizardPages", equalTo(wizardPageCollection))),
            () -> assertThat(caseUpdateViewEvent, hasProperty("titleDisplay", is(TITLE_DISPLAY))),
            () -> assertThat(caseUpdateViewEvent, hasProperty("supplementaryData", nullValue())),
            () -> inOrder.verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID),
            () -> inOrder.verify(eventTriggerService).findCaseEvent(caseTypeDefinition, EVENT_TRIGGER_ID),
            () -> inOrder.verify(caseViewFieldBuilder).build(caseFieldDefinitions, eventFields,
                caseDetails.getCaseDataAndMetadata()),
            () -> inOrder.verify(uiDefinitionRepository).getWizardPageCollection(CASE_TYPE_ID, EVENT_TRIGGER_ID)

        );

    }

    @Test
    @DisplayName("should build trigger supplementary data")
    void shouldBuildTriggerWithSupplementaryData() {

        Map<String, JsonNode> supplementaryData = buildData(SUPPLEMENTARY_DATA_FIELD_1, SUPPLEMENTARY_DATA_FIELD_2);
        caseDetails.setSupplementaryData(supplementaryData);
        final CaseUpdateViewEvent caseUpdateViewEvent = caseUpdateViewEventBuilder.build(startEventResult,
            CASE_TYPE_ID,
            EVENT_TRIGGER_ID,
            CASE_REFERENCE);
        InOrder inOrder = inOrder(caseDefinitionRepository,
            uiDefinitionRepository,
            eventTriggerService,
            caseViewFieldBuilder);
        assertAll(
            () -> assertThat(caseUpdateViewEvent, hasProperty("id", equalTo(EVENT_TRIGGER_ID))),
            () -> assertThat(caseUpdateViewEvent, hasProperty("name", equalTo(EVENT_TRIGGER_NAME))),
            () -> assertThat(caseUpdateViewEvent, hasProperty("description", equalTo(EVENT_TRIGGER_DESCRIPTION))),
            () -> assertThat(caseUpdateViewEvent, hasProperty("showSummary", equalTo(EVENT_TRIGGER_SHOW_SUMMARY))),
            () -> assertThat(caseUpdateViewEvent, hasProperty("showEventNotes",
                equalTo(EVENT_TRIGGER_SHOW_EVENT_NOTES))),
            () -> assertThat(caseUpdateViewEvent, hasProperty("eventToken", equalTo(TOKEN))),
            () -> assertThat(caseUpdateViewEvent, hasProperty("caseId", equalTo(CASE_REFERENCE))),
            () -> assertThat(caseUpdateViewEvent, hasProperty("caseFields", equalTo(viewFields))),
            () -> assertThat(caseUpdateViewEvent, hasProperty("wizardPages", equalTo(wizardPageCollection))),
            () -> assertThat(caseUpdateViewEvent, hasProperty("titleDisplay", is(TITLE_DISPLAY))),
            () -> assertThat(caseUpdateViewEvent.getSupplementaryData(), notNullValue()),
            () -> assertTrue(caseUpdateViewEvent.getSupplementaryData().containsKey(SUPPLEMENTARY_DATA_FIELD_1)),
            () -> assertTrue(caseUpdateViewEvent.getSupplementaryData().containsKey(SUPPLEMENTARY_DATA_FIELD_2)),
            () -> inOrder.verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID),
            () -> inOrder.verify(eventTriggerService).findCaseEvent(caseTypeDefinition, EVENT_TRIGGER_ID),
            () -> inOrder.verify(caseViewFieldBuilder).build(caseFieldDefinitions, eventFields,
                caseDetails.getCaseDataAndMetadata()),
            () -> inOrder.verify(uiDefinitionRepository).getWizardPageCollection(CASE_TYPE_ID, EVENT_TRIGGER_ID)

        );

    }

    @Test
    @DisplayName("should fail if no case type")
    void shouldFailIfNoCaseType() {
        when(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> caseUpdateViewEventBuilder.build(startEventResult,
                                                                                          CASE_TYPE_ID,
                                                                                          EVENT_TRIGGER_ID,
                                                                                          CASE_REFERENCE));
    }

    @Test
    @DisplayName("should fail if no event trigger")
    void shouldFailIfNoEventTrigger() {
        when(eventTriggerService.findCaseEvent(caseTypeDefinition, EVENT_TRIGGER_ID)).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> caseUpdateViewEventBuilder.build(startEventResult,
                                                                                          CASE_TYPE_ID,
                                                                                          EVENT_TRIGGER_ID,
                                                                                          CASE_REFERENCE));
    }

    @Test
    @DisplayName("should fail if no case details")
    void shouldFailIfNoCaseDetails() {
        startEventResult.setCaseDetails(null);

        final Exception exception = assertThrows(ResourceNotFoundException.class, () ->
            caseUpdateViewEventBuilder.build(startEventResult, CASE_TYPE_ID, EVENT_TRIGGER_ID, CASE_REFERENCE));
        assertThat(exception.getMessage(), startsWith("Case not found"));
    }

}
