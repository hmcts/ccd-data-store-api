package uk.gov.hmcts.ccd.v2.external.resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventTrigger;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("CaseResource")
class StartTriggerResourceTest {
    private static final String TOKEN = "Token";
    private static final String CASE_TYPE_ID = "TestAddressBookCase";
    private static final String EVENT_ID = "createCase";
    private static final String LINK_SELF = String.format("/case-types/%s/event-triggers/%s?ignore-warning=true", CASE_TYPE_ID, EVENT_ID);
    private static final CaseDetails CASE_DETAILS = new CaseDetails();

    private StartEventTrigger startEventTrigger;
    private boolean ignoreWarning;

    @BeforeEach
    void setUp() {
        CASE_DETAILS.setCaseTypeId(CASE_TYPE_ID);
        startEventTrigger = aStartEventTrigger();
        ignoreWarning = true;
    }

    @Test
    @DisplayName("should copy case details")
    void shouldCopyCaseDetails() {
        final StartTriggerResource startTriggerResource = new StartTriggerResource(startEventTrigger, ignoreWarning);

        assertAll(
            () -> assertThat(startTriggerResource.getEventId(), equalTo(EVENT_ID.toString())),
            () -> assertThat(startTriggerResource.getToken(), equalTo(TOKEN)),
            () -> assertThat(startTriggerResource.getCaseDetails(), equalTo(CASE_DETAILS))
        );
    }

    @Test
    @DisplayName("should link to itself")
    void shouldLinkToSelf() {
        final StartTriggerResource startTriggerResource = new StartTriggerResource(startEventTrigger, ignoreWarning);

        assertThat(startTriggerResource.getLink("self").getHref(), equalTo(LINK_SELF));
    }

    private StartEventTrigger aStartEventTrigger() {
        final StartEventTrigger startEventTrigger = new StartEventTrigger();

        startEventTrigger.setCaseDetails(CASE_DETAILS);
        startEventTrigger.setEventId(EVENT_ID);
        startEventTrigger.setToken(TOKEN);

        return startEventTrigger;
    }
}
