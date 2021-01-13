package uk.gov.hmcts.ccd.v2.internal.resource;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.junit.jupiter.api.Assertions.assertAll;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseViewFieldBuilder.aViewField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.WizardPageBuilder.newWizardPage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.Link;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseUpdateViewEvent;
import uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil;

import java.util.Optional;

@DisplayName("CaseResource")
class CaseUpdateViewEventResourceTest {
    private static final String NAME = "eventName";
    private static final String DESCRIPTION = "eventDescription";
    private static final String TOKEN = "Token";
    private static final String END_BUTTON_LABEL = "Submit";
    private static final boolean IS_SHOW_SUMMARY = true;
    private static final boolean IS_SHOW_EVENT_NOTES = false;
    private static final boolean IS_SAVE_DRAFT = true;
    private static final String CASE_ID = "1111222233334444";
    private static final String FIELD_ID = "PersonFirstName";
    private static final String CASE_TYPE_ID = "TestAddressBookCase";
    private static final String ID = "createCase";

    private CaseUpdateViewEvent caseUpdateViewEvent;
    private boolean ignoreWarning;

    @BeforeEach
    void setUp() {
        caseUpdateViewEvent = newCaseUpdateViewEvent();
        ignoreWarning = true;
    }


    private CaseUpdateViewEvent newCaseUpdateViewEvent() {
        return TestBuildersUtil.CaseUpdateViewEventBuilder.newCaseUpdateViewEvent()
            .withId(ID)
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
    }

    @Nested
    @DisplayName("Start case trigger")
    class StartTriggerForCase {
        private final String linkSelfForCase =
            String.format("/internal/case-types/%s/event-triggers/%s?ignore-warning=true", CASE_TYPE_ID, ID);

        @Test
        @DisplayName("should copy case event trigger")
        void shouldCopyCaseDetails() {
            final CaseUpdateViewEventResource caseUpdateViewEventResource =
                CaseUpdateViewEventResource.forCaseType(caseUpdateViewEvent, CASE_TYPE_ID, ignoreWarning);

            assertAll(
                () -> assertThat(caseUpdateViewEventResource.getCaseUpdateViewEvent().getId(), equalTo(ID)),
                () -> assertThat(caseUpdateViewEventResource.getCaseUpdateViewEvent().getName(), equalTo(NAME)),
                () -> assertThat(caseUpdateViewEventResource.getCaseUpdateViewEvent().getDescription(),
                    equalTo(DESCRIPTION)),
                () -> assertThat(caseUpdateViewEventResource.getCaseUpdateViewEvent().getCaseId(), equalTo(CASE_ID)),
                () -> assertThat(caseUpdateViewEventResource.getCaseUpdateViewEvent().getCaseFields(),
                    hasItems(hasProperty("id", is(FIELD_ID)))),
                () -> assertThat(caseUpdateViewEventResource.getCaseUpdateViewEvent().getEventToken(), equalTo(TOKEN)),
                () -> assertThat(caseUpdateViewEventResource.getCaseUpdateViewEvent().getWizardPages()
                        .get(0).getWizardPageFields().get(0),
                                 hasProperty("caseFieldId", is(FIELD_ID))),
                () -> assertThat(caseUpdateViewEventResource.getCaseUpdateViewEvent().getShowSummary(),
                    equalTo(IS_SHOW_SUMMARY)),
                () -> assertThat(caseUpdateViewEventResource.getCaseUpdateViewEvent().getShowEventNotes(),
                    equalTo(IS_SHOW_EVENT_NOTES)),
                () -> assertThat(caseUpdateViewEventResource.getCaseUpdateViewEvent().getEndButtonLabel(),
                    equalTo(END_BUTTON_LABEL)),
                () -> assertThat(caseUpdateViewEventResource.getCaseUpdateViewEvent().getCanSaveDraft(),
                    equalTo(IS_SAVE_DRAFT))
            );
        }

        @Test
        @DisplayName("should link to itself")
        void shouldLinkToSelf() {
            final CaseUpdateViewEventResource caseUpdateViewEventResource =
                CaseUpdateViewEventResource.forCaseType(caseUpdateViewEvent, CASE_TYPE_ID, ignoreWarning);

            Optional<Link> self = caseUpdateViewEventResource.getLink("self");
            assertThat(self.get().getHref(), equalTo(linkSelfForCase));
        }

    }

    @Nested
    @DisplayName("Start event trigger")
    class StartTriggerForEvent {
        private final Long caseReference = 1111222233334444L;
        private final String linkSelfForEvent =
            String.format("/internal/cases/%s/event-triggers/%s?ignore-warning=true", caseReference, ID);

        @Test
        @DisplayName("should copy case event trigger")
        void shouldCopyCaseDetails() {
            final CaseUpdateViewEventResource caseUpdateViewEventResource =
                CaseUpdateViewEventResource.forCase(caseUpdateViewEvent, caseReference.toString(), ignoreWarning);

            assertAll(
                () -> assertThat(caseUpdateViewEventResource.getCaseUpdateViewEvent().getId(), equalTo(ID)),
                () -> assertThat(caseUpdateViewEventResource.getCaseUpdateViewEvent().getName(), equalTo(NAME)),
                () -> assertThat(caseUpdateViewEventResource.getCaseUpdateViewEvent().getDescription(),
                    equalTo(DESCRIPTION)),
                () -> assertThat(caseUpdateViewEventResource.getCaseUpdateViewEvent().getCaseId(), equalTo(CASE_ID)),
                () -> assertThat(caseUpdateViewEventResource.getCaseUpdateViewEvent().getCaseFields(),
                    hasItems(hasProperty("id", is(FIELD_ID)))),
                () -> assertThat(caseUpdateViewEventResource.getCaseUpdateViewEvent().getEventToken(), equalTo(TOKEN)),
                () -> assertThat(caseUpdateViewEventResource.getCaseUpdateViewEvent().getWizardPages().get(0)
                        .getWizardPageFields().get(0), hasProperty("caseFieldId", is(FIELD_ID))),
                () -> assertThat(caseUpdateViewEventResource.getCaseUpdateViewEvent().getShowSummary(),
                    equalTo(IS_SHOW_SUMMARY)),
                () -> assertThat(caseUpdateViewEventResource.getCaseUpdateViewEvent().getShowEventNotes(),
                    equalTo(IS_SHOW_EVENT_NOTES)),
                () -> assertThat(caseUpdateViewEventResource.getCaseUpdateViewEvent().getEndButtonLabel(),
                    equalTo(END_BUTTON_LABEL)),
                () -> assertThat(caseUpdateViewEventResource.getCaseUpdateViewEvent().getCanSaveDraft(),
                    equalTo(IS_SAVE_DRAFT))
            );
        }

        @Test
        @DisplayName("should link to itself")
        void shouldLinkToSelf() {
            final CaseUpdateViewEventResource caseUpdateViewEventResource =
                CaseUpdateViewEventResource.forCase(caseUpdateViewEvent, caseReference.toString(), ignoreWarning);

            Optional<Link> self = caseUpdateViewEventResource.getLink("self");
            assertThat(self.get().getHref(), equalTo(linkSelfForEvent));
        }

    }

    @Nested
    @DisplayName("Start draft trigger")
    class StartTriggerForD {
        private final String draftReference = "DRAFT127";
        private final String linkSelfForEvent =
            String.format("/internal/drafts/%s/event-trigger?ignore-warning=true", draftReference, ID);

        @Test
        @DisplayName("should copy case event trigger")
        void shouldCopyCaseDetails() {
            final CaseUpdateViewEventResource caseUpdateViewEventResource =
                CaseUpdateViewEventResource.forDraft(caseUpdateViewEvent, draftReference, ignoreWarning);

            assertAll(
                () -> assertThat(caseUpdateViewEventResource.getCaseUpdateViewEvent().getId(), equalTo(ID)),
                () -> assertThat(caseUpdateViewEventResource.getCaseUpdateViewEvent().getName(), equalTo(NAME)),
                () -> assertThat(caseUpdateViewEventResource.getCaseUpdateViewEvent().getDescription(),
                    equalTo(DESCRIPTION)),
                () -> assertThat(caseUpdateViewEventResource.getCaseUpdateViewEvent().getCaseId(), equalTo(CASE_ID)),
                () -> assertThat(caseUpdateViewEventResource.getCaseUpdateViewEvent().getCaseFields(),
                    hasItems(hasProperty("id", is(FIELD_ID)))),
                () -> assertThat(caseUpdateViewEventResource.getCaseUpdateViewEvent().getEventToken(), equalTo(TOKEN)),
                () -> assertThat(caseUpdateViewEventResource.getCaseUpdateViewEvent().getWizardPages().get(0)
                        .getWizardPageFields().get(0), hasProperty("caseFieldId", is(FIELD_ID))),
                () -> assertThat(caseUpdateViewEventResource.getCaseUpdateViewEvent().getShowSummary(),
                    equalTo(IS_SHOW_SUMMARY)),
                () -> assertThat(caseUpdateViewEventResource.getCaseUpdateViewEvent().getShowEventNotes(),
                    equalTo(IS_SHOW_EVENT_NOTES)),
                () -> assertThat(caseUpdateViewEventResource.getCaseUpdateViewEvent().getEndButtonLabel(),
                    equalTo(END_BUTTON_LABEL)),
                () -> assertThat(caseUpdateViewEventResource.getCaseUpdateViewEvent().getCanSaveDraft(),
                    equalTo(IS_SAVE_DRAFT))
            );
        }

        @Test
        @DisplayName("should link to itself")
        void shouldLinkToSelf() {
            final CaseUpdateViewEventResource caseUpdateViewEventResource =
                CaseUpdateViewEventResource.forDraft(caseUpdateViewEvent, draftReference, ignoreWarning);

            Optional<Link> self = caseUpdateViewEventResource.getLink("self");
            assertThat(self.get().getHref(), equalTo(linkSelfForEvent));
        }

    }
}
