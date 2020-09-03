package uk.gov.hmcts.ccd.v2.internal.controller;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseUpdateViewEvent;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetEventTriggerOperation;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.v2.internal.resource.CaseUpdateViewEventResource;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseUpdateViewEventBuilder.newCaseUpdateViewEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseViewFieldBuilder.aViewField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.WizardPageBuilder.newWizardPage;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.WizardPageComplexFieldOverrideBuilder.newWizardPageComplexFieldOverride;

@DisplayName("UIStartTriggerControllerTest")

class UIStartEventControllerTest {
    private static final String NAME = "eventName";
    private static final String DESCRIPTION = "eventDescription";
    private static final String TOKEN = "Token";
    private static final String END_BUTTON_LABEL = "Submit";
    private static final boolean IS_SHOW_SUMMARY = true;
    private static final boolean IS_SHOW_EVENT_NOTES = false;
    private static final boolean IS_SAVE_DRAFT = true;
    private static final String CASE_ID = "1111222233334444";
    private static final String DRAFT_ID = "DRAFT127";
    private static final String FIELD_ID = "PersonFirstName";
    private static final String FIELD_LABEL = "Persion Name";
    private static final String FIELD_HINT_TEXT = "Please provide person name";
    private static final String CASE_TYPE_ID = "TestAddressBookCase";
    private static final String EVENT_ID = "createCase";
    private static final String EVENT_TRIGGER_ID = "createCase";
    private static final boolean IGNORE_WARNING = false;

    @Mock
    private GetEventTriggerOperation getEventTriggerOperation;
    @Mock
    private UIDService caseReferenceService;

    @InjectMocks
    private UIStartTriggerController uiStartTriggerController;

    private CaseUpdateViewEvent caseUpdateViewEvent = newCaseUpdateViewEvent()
        .withId(EVENT_ID)
        .withName(NAME)
        .withDescription(DESCRIPTION)
        .withCaseId(CASE_ID)
        .withField(aViewField()
            .withId(FIELD_ID).build())
        .withEventToken(TOKEN)
        .withWizardPage(newWizardPage()
            .withField(aViewField()
                    .withId(FIELD_ID)
                    .build(),
                singletonList(newWizardPageComplexFieldOverride()
                    .withComplexFieldId(FIELD_ID)
                    .withDisplayContext("MANDATORY")
                    .withLabel(FIELD_LABEL)
                    .withHintText(FIELD_HINT_TEXT)
                    .withShowCondition(null)
                    .build()))
            .build())
        .withShowSummary(IS_SHOW_SUMMARY)
        .withShowEventNotes(IS_SHOW_EVENT_NOTES)
        .withEndButtonLabel(END_BUTTON_LABEL)
        .withCanSaveDraft(IS_SAVE_DRAFT)
        .build();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        when(getEventTriggerOperation.executeForCaseType(CASE_TYPE_ID, EVENT_TRIGGER_ID, IGNORE_WARNING)).thenReturn(caseUpdateViewEvent);
        when(getEventTriggerOperation.executeForCase(CASE_ID, EVENT_TRIGGER_ID, IGNORE_WARNING)).thenReturn(caseUpdateViewEvent);
        when(getEventTriggerOperation.executeForDraft(DRAFT_ID, IGNORE_WARNING)).thenReturn(caseUpdateViewEvent);
        when(caseReferenceService.validateUID(CASE_ID)).thenReturn(true);
    }

    @Nested
    @DisplayName("GET /internal/case-types/{caseTypeId}/trigger/{triggerId}")
    class StartTriggerForCaseTypeDefinition {

        @Test
        @DisplayName("should return 200 when start trigger found")
        void startTriggerFound() {
            final ResponseEntity<CaseUpdateViewEventResource> response =
                uiStartTriggerController.getCaseUpdateViewEventByCaseType(CASE_TYPE_ID, EVENT_TRIGGER_ID, IGNORE_WARNING);

            assertAll(
                () -> assertThat(response.getStatusCode(), is(HttpStatus.OK)),
                () -> assertThat(response.getBody().getCaseUpdateViewEvent().getId(), is(EVENT_TRIGGER_ID)),
                () -> assertThat(response.getBody().getCaseUpdateViewEvent().getName(), is(NAME)),
                () -> assertThat(response.getBody().getCaseUpdateViewEvent().getDescription(), is(DESCRIPTION)),
                () -> assertThat(response.getBody().getCaseUpdateViewEvent().getCaseId(), is(CASE_ID)),
                () -> assertThat(response.getBody().getCaseUpdateViewEvent().getCaseFields(), hasItems(hasProperty("id", CoreMatchers.is(FIELD_ID)))),
                () -> assertThat(response.getBody().getCaseUpdateViewEvent().getEventToken(), equalTo(TOKEN)),
                () -> assertThat(response.getBody().getCaseUpdateViewEvent().getWizardPages().get(0).getWizardPageFields().get(0),
                                 hasProperty("caseFieldId", CoreMatchers.is(FIELD_ID))),
                () -> assertThat(response.getBody().getCaseUpdateViewEvent().getWizardPages().get(0).getWizardPageFields().get(0)
                        .getComplexFieldOverrides().get(0),
                    hasProperty("complexFieldElementId", CoreMatchers.is(FIELD_ID))),
                () -> assertThat(response.getBody().getCaseUpdateViewEvent().getWizardPages().get(0).getWizardPageFields().get(0)
                        .getComplexFieldOverrides().get(0),
                    hasProperty("displayContext", CoreMatchers.is("MANDATORY"))),
                () -> assertThat(response.getBody().getCaseUpdateViewEvent().getWizardPages().get(0).getWizardPageFields().get(0)
                        .getComplexFieldOverrides().get(0),
                    hasProperty("label", CoreMatchers.is(FIELD_LABEL))),
                () -> assertThat(response.getBody().getCaseUpdateViewEvent().getWizardPages().get(0).getWizardPageFields().get(0)
                        .getComplexFieldOverrides().get(0),
                    hasProperty("hintText", CoreMatchers.is(FIELD_HINT_TEXT))),
                () -> assertThat(response.getBody().getCaseUpdateViewEvent().getWizardPages().get(0).getWizardPageFields().get(0)
                        .getComplexFieldOverrides().get(0),
                    hasProperty("showCondition", CoreMatchers.nullValue())),
                () -> assertThat(response.getBody().getCaseUpdateViewEvent().getShowSummary(), equalTo(IS_SHOW_SUMMARY)),
                () -> assertThat(response.getBody().getCaseUpdateViewEvent().getShowEventNotes(), equalTo(IS_SHOW_EVENT_NOTES)),
                () -> assertThat(response.getBody().getCaseUpdateViewEvent().getEndButtonLabel(), equalTo(END_BUTTON_LABEL)),
                () -> assertThat(response.getBody().getCaseUpdateViewEvent().getCanSaveDraft(), equalTo(IS_SAVE_DRAFT))
            );
        }

        @Test
        @DisplayName("should propagate exception")
        void shouldPropagateExceptionWhenThrown() {
            when(getEventTriggerOperation.executeForCaseType(CASE_TYPE_ID, EVENT_TRIGGER_ID, IGNORE_WARNING)).thenThrow(RuntimeException.class);

            assertThrows(Exception.class, () -> uiStartTriggerController.getCaseUpdateViewEventByCaseType(CASE_TYPE_ID, EVENT_TRIGGER_ID, IGNORE_WARNING));
        }

    }

    @Nested
    @DisplayName("GET /internal/cases/{caseTypeId}/trigger/{triggerId}")
    class StartTriggerForCase {

        @Test
        @DisplayName("should return 200 when start trigger found")
        void startTriggerFound() {
            final ResponseEntity<CaseUpdateViewEventResource> response = uiStartTriggerController.getCaseUpdateViewEvent(
                CASE_ID, EVENT_TRIGGER_ID, IGNORE_WARNING);

            assertAll(
                () -> assertThat(response.getStatusCode(), is(HttpStatus.OK)),
                () -> assertThat(response.getBody().getCaseUpdateViewEvent().getId(), is(EVENT_TRIGGER_ID)),
                () -> assertThat(response.getBody().getCaseUpdateViewEvent().getName(), is(NAME)),
                () -> assertThat(response.getBody().getCaseUpdateViewEvent().getDescription(), is(DESCRIPTION)),
                () -> assertThat(response.getBody().getCaseUpdateViewEvent().getCaseId(), is(CASE_ID)),
                () -> assertThat(response.getBody().getCaseUpdateViewEvent().getCaseFields(), hasItems(hasProperty("id", CoreMatchers.is(FIELD_ID)))),
                () -> assertThat(response.getBody().getCaseUpdateViewEvent().getEventToken(), equalTo(TOKEN)),
                () -> assertThat(response.getBody().getCaseUpdateViewEvent().getWizardPages().get(0).getWizardPageFields().get(0),
                                 hasProperty("caseFieldId", CoreMatchers.is(FIELD_ID))),
                () -> assertThat(response.getBody().getCaseUpdateViewEvent().getShowSummary(), equalTo(IS_SHOW_SUMMARY)),
                () -> assertThat(response.getBody().getCaseUpdateViewEvent().getShowEventNotes(), equalTo(IS_SHOW_EVENT_NOTES)),
                () -> assertThat(response.getBody().getCaseUpdateViewEvent().getEndButtonLabel(), equalTo(END_BUTTON_LABEL)),
                () -> assertThat(response.getBody().getCaseUpdateViewEvent().getCanSaveDraft(), equalTo(IS_SAVE_DRAFT))
            );
        }

        @Test
        @DisplayName("should propagate exception from downstream operation")
        void shouldPropagateExceptionFromOperationWhenThrown() {
            when(getEventTriggerOperation.executeForCase(CASE_ID, EVENT_TRIGGER_ID, IGNORE_WARNING)).thenThrow(RuntimeException.class);

            assertThrows(Exception.class, () -> uiStartTriggerController.getCaseUpdateViewEvent(CASE_ID, EVENT_TRIGGER_ID, IGNORE_WARNING));
        }

        @Test
        @DisplayName("should fail with bad request exception if case reference invalid")
        void shouldFailWithBadRequestException() {
            when(caseReferenceService.validateUID(CASE_ID)).thenReturn(false);

            assertThrows(BadRequestException.class, () -> uiStartTriggerController.getCaseUpdateViewEvent(CASE_ID, EVENT_TRIGGER_ID, IGNORE_WARNING));
        }

    }

    @Nested
    @DisplayName("GET /internal/drafts/{draftId}/trigger/{triggerId}")
    class StartTriggerForDraft {

        @Test
        @DisplayName("should return 200 when start trigger found")
        void startTriggerFound() {
            final ResponseEntity<CaseUpdateViewEventResource> response = uiStartTriggerController.getStartDraftTrigger(DRAFT_ID, IGNORE_WARNING);

            assertAll(
                () -> assertThat(response.getStatusCode(), is(HttpStatus.OK)),
                () -> assertThat(response.getBody().getCaseUpdateViewEvent().getId(), is(EVENT_TRIGGER_ID)),
                () -> assertThat(response.getBody().getCaseUpdateViewEvent().getName(), is(NAME)),
                () -> assertThat(response.getBody().getCaseUpdateViewEvent().getDescription(), is(DESCRIPTION)),
                () -> assertThat(response.getBody().getCaseUpdateViewEvent().getCaseId(), is(CASE_ID)),
                () -> assertThat(response.getBody().getCaseUpdateViewEvent().getCaseFields(), hasItems(hasProperty("id", CoreMatchers.is(FIELD_ID)))),
                () -> assertThat(response.getBody().getCaseUpdateViewEvent().getEventToken(), equalTo(TOKEN)),
                () -> assertThat(response.getBody().getCaseUpdateViewEvent().getWizardPages().get(0).getWizardPageFields().get(0),
                                 hasProperty("caseFieldId", CoreMatchers.is(FIELD_ID))),
                () -> assertThat(response.getBody().getCaseUpdateViewEvent().getShowSummary(), equalTo(IS_SHOW_SUMMARY)),
                () -> assertThat(response.getBody().getCaseUpdateViewEvent().getShowEventNotes(), equalTo(IS_SHOW_EVENT_NOTES)),
                () -> assertThat(response.getBody().getCaseUpdateViewEvent().getEndButtonLabel(), equalTo(END_BUTTON_LABEL)),
                () -> assertThat(response.getBody().getCaseUpdateViewEvent().getCanSaveDraft(), equalTo(IS_SAVE_DRAFT))
            );
        }

        @Test
        @DisplayName("should propagate exception from downstream operation")
        void shouldPropagateExceptionFromOperationWhenThrown() {
            when(getEventTriggerOperation.executeForDraft(DRAFT_ID, IGNORE_WARNING)).thenThrow(RuntimeException.class);

            assertThrows(Exception.class, () -> uiStartTriggerController.getStartDraftTrigger(DRAFT_ID, IGNORE_WARNING));
        }

    }

}
