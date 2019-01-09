package uk.gov.hmcts.ccd.v2.internal.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseEventTriggerBuilder.newCaseEventTrigger;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseViewFieldBuilder.aViewField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.WizardPageBuilder.newWizardPage;

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
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseEventTrigger;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetEventTriggerOperation;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.v2.internal.resource.UIStartTriggerResource;

@DisplayName("UIStartTriggerControllerTest")

class UIStartTriggerControllerTest {
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

    private CaseEventTrigger caseEventTrigger = newCaseEventTrigger()
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
                                           .build())
                            .build())
        .withShowSummary(IS_SHOW_SUMMARY)
        .withShowEventNotes(IS_SHOW_EVENT_NOTES)
        .withEndButtonLabel(END_BUTTON_LABEL)
        .withCanSaveDraft(IS_SAVE_DRAFT)
        .build();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        when(getEventTriggerOperation.executeForCaseType(CASE_TYPE_ID, EVENT_TRIGGER_ID, IGNORE_WARNING)).thenReturn(caseEventTrigger);
        when(getEventTriggerOperation.executeForCase(CASE_ID, EVENT_TRIGGER_ID, IGNORE_WARNING)).thenReturn(caseEventTrigger);
        when(getEventTriggerOperation.executeForDraft(DRAFT_ID, IGNORE_WARNING)).thenReturn(caseEventTrigger);
        when(caseReferenceService.validateUID(CASE_ID)).thenReturn(true);
    }

    @Nested
    @DisplayName("GET /internal/case-types/{caseTypeId}/trigger/{triggerId}")
    class StartTriggerForCaseType {

        @Test
        @DisplayName("should return 200 when start trigger found")
        void startTriggerFound() {
            final ResponseEntity<UIStartTriggerResource> response =
                uiStartTriggerController.getStartCaseTrigger(CASE_TYPE_ID, EVENT_TRIGGER_ID, IGNORE_WARNING);

            assertAll(
                () -> assertThat(response.getStatusCode(), is(HttpStatus.OK)),
                () -> assertThat(response.getBody().getCaseEventTrigger().getId(), is(EVENT_TRIGGER_ID)),
                () -> assertThat(response.getBody().getCaseEventTrigger().getName(), is(NAME)),
                () -> assertThat(response.getBody().getCaseEventTrigger().getDescription(), is(DESCRIPTION)),
                () -> assertThat(response.getBody().getCaseEventTrigger().getCaseId(), is(CASE_ID)),
                () -> assertThat(response.getBody().getCaseEventTrigger().getCaseFields(), hasItems(hasProperty("id", CoreMatchers.is(FIELD_ID)))),
                () -> assertThat(response.getBody().getCaseEventTrigger().getEventToken(), equalTo(TOKEN)),
                () -> assertThat(response.getBody().getCaseEventTrigger().getWizardPages().get(0).getWizardPageFields().get(0),
                                 hasProperty("caseFieldId", CoreMatchers.is(FIELD_ID))),
                () -> assertThat(response.getBody().getCaseEventTrigger().getShowSummary(), equalTo(IS_SHOW_SUMMARY)),
                () -> assertThat(response.getBody().getCaseEventTrigger().getShowEventNotes(), equalTo(IS_SHOW_EVENT_NOTES)),
                () -> assertThat(response.getBody().getCaseEventTrigger().getEndButtonLabel(), equalTo(END_BUTTON_LABEL)),
                () -> assertThat(response.getBody().getCaseEventTrigger().getCanSaveDraft(), equalTo(IS_SAVE_DRAFT))
            );
        }

        @Test
        @DisplayName("should propagate exception")
        void shouldPropagateExceptionWhenThrown() {
            when(getEventTriggerOperation.executeForCaseType(CASE_TYPE_ID, EVENT_TRIGGER_ID, IGNORE_WARNING)).thenThrow(Exception.class);

            assertThrows(Exception.class, () -> uiStartTriggerController.getStartCaseTrigger(CASE_TYPE_ID, EVENT_TRIGGER_ID, IGNORE_WARNING));
        }

    }

    @Nested
    @DisplayName("GET /internal/cases/{caseTypeId}/trigger/{triggerId}")
    class StartTriggerForCase {

        @Test
        @DisplayName("should return 200 when start trigger found")
        void startTriggerFound() {
            final ResponseEntity<UIStartTriggerResource> response = uiStartTriggerController.getStartEventTrigger(CASE_ID, EVENT_TRIGGER_ID, IGNORE_WARNING);

            assertAll(
                () -> assertThat(response.getStatusCode(), is(HttpStatus.OK)),
                () -> assertThat(response.getBody().getCaseEventTrigger().getId(), is(EVENT_TRIGGER_ID)),
                () -> assertThat(response.getBody().getCaseEventTrigger().getName(), is(NAME)),
                () -> assertThat(response.getBody().getCaseEventTrigger().getDescription(), is(DESCRIPTION)),
                () -> assertThat(response.getBody().getCaseEventTrigger().getCaseId(), is(CASE_ID)),
                () -> assertThat(response.getBody().getCaseEventTrigger().getCaseFields(), hasItems(hasProperty("id", CoreMatchers.is(FIELD_ID)))),
                () -> assertThat(response.getBody().getCaseEventTrigger().getEventToken(), equalTo(TOKEN)),
                () -> assertThat(response.getBody().getCaseEventTrigger().getWizardPages().get(0).getWizardPageFields().get(0),
                                 hasProperty("caseFieldId", CoreMatchers.is(FIELD_ID))),
                () -> assertThat(response.getBody().getCaseEventTrigger().getShowSummary(), equalTo(IS_SHOW_SUMMARY)),
                () -> assertThat(response.getBody().getCaseEventTrigger().getShowEventNotes(), equalTo(IS_SHOW_EVENT_NOTES)),
                () -> assertThat(response.getBody().getCaseEventTrigger().getEndButtonLabel(), equalTo(END_BUTTON_LABEL)),
                () -> assertThat(response.getBody().getCaseEventTrigger().getCanSaveDraft(), equalTo(IS_SAVE_DRAFT))
            );
        }

        @Test
        @DisplayName("should propagate exception from downstream operation")
        void shouldPropagateExceptionFromOperationWhenThrown() {
            when(getEventTriggerOperation.executeForCase(CASE_ID, EVENT_TRIGGER_ID, IGNORE_WARNING)).thenThrow(Exception.class);

            assertThrows(Exception.class, () -> uiStartTriggerController.getStartEventTrigger(CASE_ID, EVENT_TRIGGER_ID, IGNORE_WARNING));
        }

        @Test
        @DisplayName("should fail with bad request exception if case reference invalid")
        void shouldFailWithBadRequestException() {
            when(caseReferenceService.validateUID(CASE_ID)).thenReturn(false);

            assertThrows(BadRequestException.class, () -> uiStartTriggerController.getStartEventTrigger(CASE_ID, EVENT_TRIGGER_ID, IGNORE_WARNING));
        }

    }

    @Nested
    @DisplayName("GET /internal/drafts/{draftId}/trigger/{triggerId}")
    class StartTriggerForDraft {

        @Test
        @DisplayName("should return 200 when start trigger found")
        void startTriggerFound() {
            final ResponseEntity<UIStartTriggerResource> response = uiStartTriggerController.getStartDraftTrigger(DRAFT_ID, IGNORE_WARNING);

            assertAll(
                () -> assertThat(response.getStatusCode(), is(HttpStatus.OK)),
                () -> assertThat(response.getBody().getCaseEventTrigger().getId(), is(EVENT_TRIGGER_ID)),
                () -> assertThat(response.getBody().getCaseEventTrigger().getName(), is(NAME)),
                () -> assertThat(response.getBody().getCaseEventTrigger().getDescription(), is(DESCRIPTION)),
                () -> assertThat(response.getBody().getCaseEventTrigger().getCaseId(), is(CASE_ID)),
                () -> assertThat(response.getBody().getCaseEventTrigger().getCaseFields(), hasItems(hasProperty("id", CoreMatchers.is(FIELD_ID)))),
                () -> assertThat(response.getBody().getCaseEventTrigger().getEventToken(), equalTo(TOKEN)),
                () -> assertThat(response.getBody().getCaseEventTrigger().getWizardPages().get(0).getWizardPageFields().get(0),
                                 hasProperty("caseFieldId", CoreMatchers.is(FIELD_ID))),
                () -> assertThat(response.getBody().getCaseEventTrigger().getShowSummary(), equalTo(IS_SHOW_SUMMARY)),
                () -> assertThat(response.getBody().getCaseEventTrigger().getShowEventNotes(), equalTo(IS_SHOW_EVENT_NOTES)),
                () -> assertThat(response.getBody().getCaseEventTrigger().getEndButtonLabel(), equalTo(END_BUTTON_LABEL)),
                () -> assertThat(response.getBody().getCaseEventTrigger().getCanSaveDraft(), equalTo(IS_SAVE_DRAFT))
            );
        }

        @Test
        @DisplayName("should propagate exception from downstream operation")
        void shouldPropagateExceptionFromOperationWhenThrown() {
            when(getEventTriggerOperation.executeForDraft(DRAFT_ID, IGNORE_WARNING)).thenThrow(Exception.class);

            assertThrows(Exception.class, () -> uiStartTriggerController.getStartDraftTrigger(DRAFT_ID, IGNORE_WARNING));
        }

    }

}
