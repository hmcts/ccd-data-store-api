package uk.gov.hmcts.ccd.v2.external.resource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.Link;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventTrigger;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import java.util.Optional;

@DisplayName("CaseResource")
class StartTriggerResourceTest {
    private static final String TOKEN = "Token";
    private static final String CASE_TYPE_ID = "TestAddressBookCase";
    private static final String EVENT_ID = "createCase";
    private static final CaseDetails CASE_DETAILS = new CaseDetails();

    private StartEventTrigger startEventTrigger;
    private boolean ignoreWarning;

    @BeforeEach
    void setUp() {
        CASE_DETAILS.setCaseTypeId(CASE_TYPE_ID);
        startEventTrigger = aStartEventTrigger();
        ignoreWarning = true;
    }

    private StartEventTrigger aStartEventTrigger() {
        final StartEventTrigger startEventTrigger = new StartEventTrigger();

        startEventTrigger.setCaseDetails(CASE_DETAILS);
        startEventTrigger.setEventId(EVENT_ID);
        startEventTrigger.setToken(TOKEN);

        return startEventTrigger;
    }

    @Nested
    @DisplayName("Start case trigger")
    class StartTriggerForCase {
        private final String LINK_SELF_FOR_CASE = String.format("/case-types/%s/event-triggers/%s?ignore-warning=true", CASE_TYPE_ID, EVENT_ID);

        @Test
        @DisplayName("should copy case details")
        void shouldCopyCaseDetails() {
            final StartTriggerResource startTriggerResource = new StartTriggerResource(startEventTrigger, ignoreWarning, false);

            assertAll(
                () -> assertThat(startTriggerResource.getEventId(), equalTo(EVENT_ID.toString())),
                () -> assertThat(startTriggerResource.getToken(), equalTo(TOKEN)),
                () -> assertThat(startTriggerResource.getCaseDetails(), equalTo(CASE_DETAILS))
            );
        }

        @Test
        @DisplayName("should link to itself")
        void shouldLinkToSelf() {
            final StartTriggerResource startTriggerResource = new StartTriggerResource(startEventTrigger, ignoreWarning, false);

            Optional<Link> self = startTriggerResource.getLink("self");
            assertThat(self.get().getHref(), equalTo(LINK_SELF_FOR_CASE));
        }
    }

    @Nested
    @DisplayName("Start event trigger")
    class StartTriggerForEvent {
        private final Long CASE_REFERENCE = 1111222233334444L;
        private final String LINK_SELF_FOR_EVENT = String.format("/cases/%s/event-triggers/%s?ignore-warning=true", CASE_REFERENCE, EVENT_ID);

        @BeforeEach
        void setUp() {
            CASE_DETAILS.setReference(CASE_REFERENCE);
            startEventTrigger.setCaseDetails(CASE_DETAILS);
            CASE_DETAILS.setCaseTypeId(CASE_TYPE_ID);
            startEventTrigger = aStartEventTrigger();
            ignoreWarning = true;
        }

        @Test
        @DisplayName("should copy case details")
        void shouldCopyCaseDetails() {
            final StartTriggerResource startTriggerResource = new StartTriggerResource(startEventTrigger, ignoreWarning, true);

            assertAll(
                () -> assertThat(startTriggerResource.getEventId(), equalTo(EVENT_ID.toString())),
                () -> assertThat(startTriggerResource.getToken(), equalTo(TOKEN)),
                () -> assertThat(startTriggerResource.getCaseDetails(), equalTo(CASE_DETAILS))
            );
        }

        @Test
        @DisplayName("should link to itself")
        void shouldLinkToSelf() {
            final StartTriggerResource startTriggerResource = new StartTriggerResource(startEventTrigger, ignoreWarning, true);

            Optional<Link> self = startTriggerResource.getLink("self");
            assertThat(self.get().getHref(), equalTo(LINK_SELF_FOR_EVENT));
        }
    }
}
