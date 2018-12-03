package uk.gov.hmcts.ccd.domain.service.aggregated;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseEventTrigger;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewFieldBuilder;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventTrigger;
import uk.gov.hmcts.ccd.domain.model.definition.*;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.domain.service.startevent.StartEventOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class DefaultGetEventTriggerOperationTest {

    private static final String EVENT_TRIGGER_ID = "testEventTriggerId";
    private static final String EVENT_TRIGGER_NAME = "testEventTriggerName";
    private static final String EVENT_TRIGGER_DESCRIPTION = "testEventTriggerDescription";
    private static final Boolean EVENT_TRIGGER_SHOW_SUMMARY = true;
    private static final Boolean EVENT_TRIGGER_SHOW_EVENT_NOTES = false;
    private static final String UID = "123";
    private static final String JURISDICTION_ID = "Probate";
    private static final String CASE_REFERENCE = "1234567891012345";
    private static final String DRAFT_ID = "DRAFT1";
    private static final String CASE_TYPE_ID = "Grant";
    private static final Boolean IGNORE = Boolean.TRUE;
    private static final String TOKEN = "testToken";
    private final CaseDetails caseDetails = new CaseDetails();
    private final CaseType caseType = new CaseType();
    private final CaseEvent caseEvent = new CaseEvent();
    private final List<CaseEvent> events = Lists.newArrayList();
    private final List<CaseEventField> eventFields = Lists.newArrayList();
    private final List<CaseViewField> viewFields = Lists.newArrayList();
    private final List<CaseField> caseFields = Lists.newArrayList();
    private final List<WizardPage> wizardPageCollection = Lists.newArrayList();

    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;

    @Mock
    private CaseDetailsRepository caseDetailsRepository;

    @Mock
    private CaseViewFieldBuilder caseViewFieldBuilder;

    @Mock
    private UIDefinitionRepository uiDefinitionRepository;

    @Mock
    private EventTriggerService eventTriggerService;

    @Mock
    private StartEventOperation startEventOperation;

    @Mock
    private UIDService uidService;

    private DefaultGetEventTriggerOperation defaultGetEventTriggerOperation;

    private StartEventTrigger startEventTrigger = new StartEventTrigger();

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);

        caseType.setId(CASE_TYPE_ID);
        caseType.setEvents(events);
        caseType.setCaseFields(caseFields);

        caseDetails.setCaseTypeId(CASE_TYPE_ID);
        startEventTrigger.setCaseDetails(caseDetails);
        startEventTrigger.setToken(TOKEN);

        defaultGetEventTriggerOperation = new DefaultGetEventTriggerOperation(
            caseDefinitionRepository,
            caseDetailsRepository,
            eventTriggerService,
            caseViewFieldBuilder,
            uiDefinitionRepository,
            uidService,
            startEventOperation);

        doReturn(true).when(uidService).validateUID(CASE_REFERENCE);
        doReturn(Optional.of(caseDetails)).when(caseDetailsRepository).findByReference(CASE_REFERENCE);

        when(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).thenReturn(caseType);
        when(startEventOperation.triggerStartForCase(CASE_REFERENCE,
                                                     EVENT_TRIGGER_ID,
                                                     IGNORE)).thenReturn(startEventTrigger);
        when(startEventOperation.triggerStartForDraft(UID,
                                                      JURISDICTION_ID,
                                                      CASE_TYPE_ID,
                                                      DRAFT_ID,
                                                      EVENT_TRIGGER_ID,
                                                      IGNORE)).thenReturn(startEventTrigger);
        when(eventTriggerService.findCaseEvent(caseType, EVENT_TRIGGER_ID)).thenReturn(caseEvent);
        when(uiDefinitionRepository.getWizardPageCollection(CASE_TYPE_ID, EVENT_TRIGGER_ID)).thenReturn(wizardPageCollection);
        when(caseViewFieldBuilder.build(caseFields, eventFields, caseDetails.getData())).thenReturn(viewFields);
    }

    @Nested
    @DisplayName("for case type")
    class ForCaseType {

        @BeforeEach
        public void setup() {
            when(startEventOperation.triggerStartForCaseType(CASE_TYPE_ID, EVENT_TRIGGER_ID, IGNORE)).thenReturn(startEventTrigger);
        }

        @Test
        @DisplayName("should fail if no case details")
        void shouldFailIfNoCaseDetails() {
            startEventTrigger.setCaseDetails(null);
            assertThrows(ResourceNotFoundException.class, () -> defaultGetEventTriggerOperation.executeForCaseType(CASE_TYPE_ID,
                                                                                                                   EVENT_TRIGGER_ID,
                                                                                                                   IGNORE));
        }

        @Test
        @DisplayName("should fail if no case type")
        void shouldFailIfNoCaseType() {
            when(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).thenReturn(null);

            assertThrows(ResourceNotFoundException.class, () -> defaultGetEventTriggerOperation.executeForCaseType(CASE_TYPE_ID,
                                                                                                                   EVENT_TRIGGER_ID,
                                                                                                                   IGNORE));
        }

        @Test
        @DisplayName("should fail if no event trigger")
        void shouldFailIfNoEventTrigger() {
            when(eventTriggerService.findCaseEvent(caseType, EVENT_TRIGGER_ID)).thenReturn(null);

            assertThrows(ResourceNotFoundException.class, () -> defaultGetEventTriggerOperation.executeForCaseType(CASE_TYPE_ID,
                                                                                                                   EVENT_TRIGGER_ID,
                                                                                                                   IGNORE));
        }

        @Test
        @DisplayName("should get trigger with all data set")
        void shouldGetTriggerWithAllDataSet() {
            caseEvent.setId(EVENT_TRIGGER_ID);
            caseEvent.setName(EVENT_TRIGGER_NAME);
            caseEvent.setDescription(EVENT_TRIGGER_DESCRIPTION);
            caseEvent.setShowSummary(EVENT_TRIGGER_SHOW_SUMMARY);
            caseEvent.setShowEventNotes(EVENT_TRIGGER_SHOW_EVENT_NOTES);

            caseEvent.setCaseFields(eventFields);

            CaseEventTrigger caseEventTrigger = defaultGetEventTriggerOperation.executeForCaseType(CASE_TYPE_ID,
                                                                                                   EVENT_TRIGGER_ID,
                                                                                                   IGNORE);
            assertAll(
                () -> assertThat(caseEventTrigger, hasProperty("id", equalTo(EVENT_TRIGGER_ID))),
                () -> assertThat(caseEventTrigger, hasProperty("name", equalTo(EVENT_TRIGGER_NAME))),
                () -> assertThat(caseEventTrigger, hasProperty("description", equalTo(EVENT_TRIGGER_DESCRIPTION))),
                () -> assertThat(caseEventTrigger, hasProperty("showSummary", equalTo(EVENT_TRIGGER_SHOW_SUMMARY))),
                () -> assertThat(caseEventTrigger, hasProperty("showEventNotes", equalTo(EVENT_TRIGGER_SHOW_EVENT_NOTES))),
                () -> assertThat(caseEventTrigger, hasProperty("eventToken", equalTo(TOKEN))),
                () -> assertThat(caseEventTrigger, hasProperty("caseId", is(nullValue()))),
                () -> assertThat(caseEventTrigger, hasProperty("caseFields", equalTo(viewFields))),
                () -> assertThat(caseEventTrigger, hasProperty("wizardPages", equalTo(wizardPageCollection)))
            );
        }
    }

    @Nested
    @DisplayName("for case")
    class ForCase {

        @Test
        @DisplayName("Should propagate bad request exception if case reference invalid")
        void shouldPropagateBadRequestExceptionIfCaseReferenceInvalid() {

            doReturn(false).when(uidService).validateUID(CASE_REFERENCE);

            final Exception exception = assertThrows(BadRequestException.class, () -> defaultGetEventTriggerOperation.executeForCase(CASE_REFERENCE,
                                                                                                         EVENT_TRIGGER_ID,
                                                                                                         IGNORE));
            assertThat(exception.getMessage(), startsWith("Case reference is not valid"));
        }

        @Test
        @DisplayName("Should propagate case not found exception if no case")
        void shouldPropagateCaseNotFoundExceptionIfCaseNotFound() {

            doReturn(true).when(uidService).validateUID(CASE_REFERENCE);
            doReturn(Optional.empty()).when(caseDetailsRepository).findByReference(CASE_REFERENCE);

            final Exception exception = assertThrows(CaseNotFoundException.class, () -> defaultGetEventTriggerOperation.executeForCase(CASE_REFERENCE,
                                                                                                           EVENT_TRIGGER_ID,
                                                                                                           IGNORE));
            assertThat(exception.getMessage(), startsWith("No case found for reference: " + CASE_REFERENCE));
        }

        @Test
        @DisplayName("should fail if no case details")
        void shouldFailIfNoCaseDetails() {
            startEventTrigger.setCaseDetails(null);

            final Exception exception = assertThrows(ResourceNotFoundException.class, () -> defaultGetEventTriggerOperation.executeForCase(CASE_REFERENCE,
                                                                                                               EVENT_TRIGGER_ID,
                                                                                                               IGNORE));
            assertThat(exception.getMessage(), startsWith("Case not found"));
        }

        @Test
        @DisplayName("should fail if no case type")
        void shouldFailIfNoCaseType() {
            when(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).thenReturn(null);

            final Exception exception = assertThrows(ResourceNotFoundException.class, () -> defaultGetEventTriggerOperation.executeForCase(CASE_REFERENCE,
                                                                                                               EVENT_TRIGGER_ID,
                                                                                                               IGNORE));
            assertThat(exception.getMessage(), startsWith("Case type not found"));
        }

        @Test
        @DisplayName("should fail if no event trigger")
        void shouldFailIfNoEventTrigger() {
            when(eventTriggerService.findCaseEvent(caseType, EVENT_TRIGGER_ID)).thenReturn(null);

            assertThrows(ResourceNotFoundException.class, () -> defaultGetEventTriggerOperation.executeForCase(CASE_REFERENCE,
                                                                                                               EVENT_TRIGGER_ID,
                                                                                                               IGNORE));
        }

        @Test
        @DisplayName("should get trigger with all data set")
        void shouldGetTriggerWithAllDataSet() {
            caseEvent.setId(EVENT_TRIGGER_ID);
            caseEvent.setName(EVENT_TRIGGER_NAME);
            caseEvent.setDescription(EVENT_TRIGGER_DESCRIPTION);
            caseEvent.setShowSummary(EVENT_TRIGGER_SHOW_SUMMARY);
            caseEvent.setShowEventNotes(EVENT_TRIGGER_SHOW_EVENT_NOTES);

            caseEvent.setCaseFields(eventFields);

            CaseEventTrigger caseEventTrigger = defaultGetEventTriggerOperation.executeForCase(CASE_REFERENCE,
                                                                                               EVENT_TRIGGER_ID,
                                                                                               IGNORE);

            InOrder inOrder = inOrder(caseDefinitionRepository,
                                      caseDetailsRepository,
                                      uiDefinitionRepository,
                                      eventTriggerService,
                                      caseViewFieldBuilder,
                                      startEventOperation,
                                      uidService);
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
                () -> inOrder.verify(uidService).validateUID(CASE_REFERENCE),
                () -> inOrder.verify(caseDetailsRepository).findByReference(CASE_REFERENCE),
                () -> inOrder.verify(startEventOperation).triggerStartForCase(CASE_REFERENCE, EVENT_TRIGGER_ID, IGNORE),
                () -> inOrder.verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID),
                () -> inOrder.verify(eventTriggerService).findCaseEvent(caseType, EVENT_TRIGGER_ID),
                () -> inOrder.verify(caseViewFieldBuilder).build(caseFields, eventFields, caseDetails.getCaseDataAndMetadata()),
                () -> inOrder.verify(uiDefinitionRepository).getWizardPageCollection(CASE_TYPE_ID, EVENT_TRIGGER_ID),
                () -> inOrder.verifyNoMoreInteractions()
                );
        }
    }

    @Nested
    @DisplayName("for draft")
    class ForDraft {

        @Test
        @DisplayName("should fail if no draft details")
        void shouldFailIfNoDraftDetails() {
            doThrow(ResourceNotFoundException.class).when(startEventOperation).triggerStartForDraft(UID,
                                                                                                    JURISDICTION_ID,
                                                                                                    CASE_TYPE_ID,
                                                                                                    DRAFT_ID,
                                                                                                    EVENT_TRIGGER_ID,
                                                                                                    IGNORE);
            assertThrows(ResourceNotFoundException.class, () -> defaultGetEventTriggerOperation.executeForDraft(UID,
                                                                                                                JURISDICTION_ID,
                                                                                                                CASE_TYPE_ID,
                                                                                                                DRAFT_ID,
                                                                                                                EVENT_TRIGGER_ID,
                                                                                                                IGNORE));
        }

        @Test
        @DisplayName("should fail if no case type")
        void shouldFailIfNoCaseType() {
            when(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).thenReturn(null);

            assertThrows(ResourceNotFoundException.class, () -> defaultGetEventTriggerOperation.executeForDraft(UID,
                                                                                                                JURISDICTION_ID,
                                                                                                                CASE_TYPE_ID,
                                                                                                                DRAFT_ID,
                                                                                                                EVENT_TRIGGER_ID,
                                                                                                                IGNORE));
        }

        @Test
        @DisplayName("should fail if no event trigger")
        void shouldFailIfNoEventTrigger() {
            when(eventTriggerService.findCaseEvent(caseType, EVENT_TRIGGER_ID)).thenReturn(null);

            assertThrows(ResourceNotFoundException.class, () -> defaultGetEventTriggerOperation.executeForDraft(UID,
                                                                                                                JURISDICTION_ID,
                                                                                                                CASE_TYPE_ID,
                                                                                                                DRAFT_ID,
                                                                                                                EVENT_TRIGGER_ID,
                                                                                                                IGNORE));
        }

        @Test
        @DisplayName("should get trigger with all data set")
        void shouldGetTriggerWithAllDataSet() {
            caseEvent.setId(EVENT_TRIGGER_ID);
            caseEvent.setName(EVENT_TRIGGER_NAME);
            caseEvent.setDescription(EVENT_TRIGGER_DESCRIPTION);
            caseEvent.setShowSummary(EVENT_TRIGGER_SHOW_SUMMARY);
            caseEvent.setShowEventNotes(EVENT_TRIGGER_SHOW_EVENT_NOTES);

            caseEvent.setCaseFields(eventFields);

            CaseEventTrigger caseEventTrigger = defaultGetEventTriggerOperation.executeForDraft(UID,
                                                                                                JURISDICTION_ID,
                                                                                                CASE_TYPE_ID,
                                                                                                DRAFT_ID,
                                                                                                EVENT_TRIGGER_ID,
                                                                                                IGNORE);
            assertAll(
                () -> assertThat(caseEventTrigger, hasProperty("id", equalTo(EVENT_TRIGGER_ID))),
                () -> assertThat(caseEventTrigger, hasProperty("name", equalTo(EVENT_TRIGGER_NAME))),
                () -> assertThat(caseEventTrigger, hasProperty("description", equalTo(EVENT_TRIGGER_DESCRIPTION))),
                () -> assertThat(caseEventTrigger, hasProperty("showSummary", equalTo(EVENT_TRIGGER_SHOW_SUMMARY))),
                () -> assertThat(caseEventTrigger, hasProperty("showEventNotes", equalTo(EVENT_TRIGGER_SHOW_EVENT_NOTES))),
                () -> assertThat(caseEventTrigger, hasProperty("eventToken", equalTo(TOKEN))),
                () -> assertThat(caseEventTrigger, hasProperty("caseId", equalTo(DRAFT_ID))),
                () -> assertThat(caseEventTrigger, hasProperty("caseFields", equalTo(viewFields))),
                () -> assertThat(caseEventTrigger, hasProperty("wizardPages", equalTo(wizardPageCollection)))
            );
        }
    }
}
