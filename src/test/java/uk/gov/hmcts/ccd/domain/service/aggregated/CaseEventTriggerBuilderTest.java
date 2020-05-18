package uk.gov.hmcts.ccd.domain.service.aggregated;

import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.model.callbacks.EventTokenProperties.CASE_TYPE_ID;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDetailsBuilder.newCaseDetails;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseEventBuilder.newCaseEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeBuilder.newCaseType;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.StartEventTriggerBuilder.newStartEventTrigger;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
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
import uk.gov.hmcts.ccd.domain.service.processor.FieldProcessorService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

class CaseEventTriggerBuilderTest {

    private static final String TOKEN = "testToken";
    private static final String EVENT_TRIGGER_ID = "testEventTriggerId";
    private static final String EVENT_TRIGGER_NAME = "testEventTriggerName";
    private static final String EVENT_TRIGGER_DESCRIPTION = "testEventTriggerDescription";
    private static final Boolean EVENT_TRIGGER_SHOW_SUMMARY = true;
    private static final Boolean EVENT_TRIGGER_SHOW_EVENT_NOTES = false;
    private static final String CASE_REFERENCE = "1234567891012345";
    private final CaseEvent caseEvent = newCaseEvent()
        .withId(EVENT_TRIGGER_ID)
        .withName(EVENT_TRIGGER_NAME)
        .withDescription(EVENT_TRIGGER_DESCRIPTION)
        .withShowSummary(EVENT_TRIGGER_SHOW_SUMMARY)
        .withShowEventNotes(EVENT_TRIGGER_SHOW_EVENT_NOTES)
        .build();
    private final List<CaseEvent> events = Lists.newArrayList(newCaseEvent().build());
    private final List<CaseField> caseFields = Lists.newArrayList(newCaseField().build());
    private final List<CaseEventField> eventFields = Lists.newArrayList();
    private final CaseType caseType = newCaseType().withCaseTypeId(CASE_TYPE_ID).withEvents(events).withCaseFields(caseFields).build();
    private final CaseDetails caseDetails = newCaseDetails().withCaseTypeId(CASE_TYPE_ID).build();
    private final StartEventTrigger startEventTrigger = newStartEventTrigger().withCaseDetails(caseDetails).withEventToken(TOKEN).build();
    private final List<WizardPage> wizardPageCollection = Lists.newArrayList();
    private final List<CaseViewField> viewFields = Lists.newArrayList();

    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;

    @Mock
    private UIDefinitionRepository uiDefinitionRepository;

    @Mock
    private EventTriggerService eventTriggerService;

    @Mock
    private CaseViewFieldBuilder caseViewFieldBuilder;

    @Mock
    private FieldProcessorService fieldProcessorService;

    private CaseEventTriggerBuilder caseEventTriggerBuilder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        when(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).thenReturn(caseType);
        when(eventTriggerService.findCaseEvent(caseType, EVENT_TRIGGER_ID)).thenReturn(caseEvent);
        when(uiDefinitionRepository.getWizardPageCollection(CASE_TYPE_ID, EVENT_TRIGGER_ID)).thenReturn(wizardPageCollection);
        when(caseViewFieldBuilder.build(caseFields, eventFields, caseDetails.getData())).thenReturn(viewFields);

        caseEventTriggerBuilder = new CaseEventTriggerBuilder(caseDefinitionRepository,
                                                              uiDefinitionRepository,
                                                              eventTriggerService,
                                                              caseViewFieldBuilder,
                                                              fieldProcessorService);
    }

    @Test
    @DisplayName("should build trigger")
    void shouldBuildTrigger() {

        final CaseEventTrigger caseEventTrigger = caseEventTriggerBuilder.build(startEventTrigger,
                                                                                CASE_TYPE_ID,
                                                                                EVENT_TRIGGER_ID,
                                                                                CASE_REFERENCE);
        InOrder inOrder = inOrder(caseDefinitionRepository,
                                  uiDefinitionRepository,
                                  eventTriggerService,
                                  caseViewFieldBuilder);
        assertAll(
            () -> assertThat(caseEventTrigger, hasProperty("id", equalTo(EVENT_TRIGGER_ID))),
            () -> assertThat(caseEventTrigger, hasProperty("name", equalTo(EVENT_TRIGGER_NAME))),
            () -> assertThat(caseEventTrigger, hasProperty("description", equalTo(EVENT_TRIGGER_DESCRIPTION))),
            () -> assertThat(caseEventTrigger, hasProperty("showSummary", equalTo(EVENT_TRIGGER_SHOW_SUMMARY))),
            () -> assertThat(caseEventTrigger, hasProperty("showEventNotes", equalTo(EVENT_TRIGGER_SHOW_EVENT_NOTES))),
            () -> assertThat(caseEventTrigger, hasProperty("eventToken", equalTo(TOKEN))),
            () -> assertThat(caseEventTrigger, hasProperty("caseId", equalTo(CASE_REFERENCE))),
            () -> assertThat(caseEventTrigger, hasProperty("caseFields", equalTo(viewFields))),
            () -> assertThat(caseEventTrigger, hasProperty("wizardPages", equalTo(wizardPageCollection))),
            () -> inOrder.verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID),
            () -> inOrder.verify(eventTriggerService).findCaseEvent(caseType, EVENT_TRIGGER_ID),
            () -> inOrder.verify(caseViewFieldBuilder).build(caseFields, eventFields, caseDetails.getCaseDataAndMetadata()),
            () -> inOrder.verify(uiDefinitionRepository).getWizardPageCollection(CASE_TYPE_ID, EVENT_TRIGGER_ID)

        );

    }

    @Test
    @DisplayName("should fail if no case type")
    void shouldFailIfNoCaseType() {
        when(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> caseEventTriggerBuilder.build(startEventTrigger,
                                                                                          CASE_TYPE_ID,
                                                                                          EVENT_TRIGGER_ID,
                                                                                          CASE_REFERENCE));
    }

    @Test
    @DisplayName("should fail if no event trigger")
    void shouldFailIfNoEventTrigger() {
        when(eventTriggerService.findCaseEvent(caseType, EVENT_TRIGGER_ID)).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> caseEventTriggerBuilder.build(startEventTrigger,
                                                                                          CASE_TYPE_ID,
                                                                                          EVENT_TRIGGER_ID,
                                                                                          CASE_REFERENCE));
    }

    @Test
    @DisplayName("should fail if no case details")
    void shouldFailIfNoCaseDetails() {
        startEventTrigger.setCaseDetails(null);

        final Exception exception = assertThrows(ResourceNotFoundException.class, () -> caseEventTriggerBuilder.build(startEventTrigger,
                                                                                                                      CASE_TYPE_ID,
                                                                                                                      EVENT_TRIGGER_ID,
                                                                                                                      CASE_REFERENCE));
        assertThat(exception.getMessage(), startsWith("Case not found"));
    }

}
