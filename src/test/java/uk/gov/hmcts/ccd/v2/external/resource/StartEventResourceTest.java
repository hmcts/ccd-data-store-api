package uk.gov.hmcts.ccd.v2.external.resource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.Link;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import java.util.Optional;

@DisplayName("CaseResource")
class StartEventResourceTest {
    private static final String TOKEN = "Token";
    private static final String CASE_TYPE_ID = "TestAddressBookCase";
    private static final String EVENT_ID = "createCase";
    private static final CaseDetails CASE_DETAILS = new CaseDetails();

    private static final String LINK_SELF_FOR_CASE = String.format("/case-types/%s/event-triggers/%s?ignore-warning=true", CASE_TYPE_ID, EVENT_ID);
    private static final Long CASE_REFERENCE = 1111222233334444L;
    private static final String LINK_SELF_FOR_EVENT = String.format("/cases/%s/event-triggers/%s?ignore-warning=true", CASE_REFERENCE, EVENT_ID);

    private StartEventResult startEventResult;
    private boolean ignoreWarning;

    @BeforeEach
    void setUp() {
        CASE_DETAILS.setCaseTypeId(CASE_TYPE_ID);
        startEventResult = startEventTrigger();
        ignoreWarning = true;
    }

    private StartEventResult startEventTrigger() {
        final StartEventResult startEventResult = new StartEventResult();

        startEventResult.setCaseDetails(CASE_DETAILS);
        startEventResult.setEventId(EVENT_ID);
        startEventResult.setToken(TOKEN);

        return startEventResult;
    }

    @Nested
    @DisplayName("Start case trigger")
    class StartTriggerForCase {

        @Test
        @DisplayName("should copy case details")
        void shouldCopyCaseDetails() {
            final StartEventResource startEventResource = new StartEventResource(startEventResult, ignoreWarning, false);

            assertAll(
                () -> assertThat(startEventResource.getEventId(), equalTo(EVENT_ID.toString())),
                () -> assertThat(startEventResource.getToken(), equalTo(TOKEN)),
                () -> assertThat(startEventResource.getCaseDetails(), equalTo(CASE_DETAILS))
            );
        }

        @Test
        @DisplayName("should link to itself")
        void shouldLinkToSelf() {
            final StartEventResource startEventResource = new StartEventResource(startEventResult, ignoreWarning, false);

            Optional<Link> self = startEventResource.getLink("self");
            assertThat(self.get().getHref(), equalTo(LINK_SELF_FOR_CASE));
        }
    }

    @Nested
    @DisplayName("Start event trigger")
    class StartTriggerForEvent {

        @BeforeEach
        void setUp() {
            CASE_DETAILS.setReference(CASE_REFERENCE);
            startEventResult.setCaseDetails(CASE_DETAILS);
            CASE_DETAILS.setCaseTypeId(CASE_TYPE_ID);
            startEventResult = startEventTrigger();
            ignoreWarning = true;
        }

        @Test
        @DisplayName("should copy case details")
        void shouldCopyCaseDetails() {
            final StartEventResource startEventResource = new StartEventResource(startEventResult, ignoreWarning, true);

            assertAll(
                () -> assertThat(startEventResource.getEventId(), equalTo(EVENT_ID.toString())),
                () -> assertThat(startEventResource.getToken(), equalTo(TOKEN)),
                () -> assertThat(startEventResource.getCaseDetails(), equalTo(CASE_DETAILS))
            );
        }

        @Test
        @DisplayName("should link to itself")
        void shouldLinkToSelf() {
            final StartEventResource startEventResource = new StartEventResource(startEventResult, ignoreWarning, true);

            Optional<Link> self = startEventResource.getLink("self");
            assertThat(self.get().getHref(), equalTo(LINK_SELF_FOR_EVENT));
        }
    }
}
