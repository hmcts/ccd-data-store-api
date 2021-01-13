package uk.gov.hmcts.ccd.datastore.tests.v2.internal;

import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.datastore.tests.AATHelper;
import uk.gov.hmcts.ccd.datastore.tests.BaseTest;
import uk.gov.hmcts.ccd.v2.V2;

import static java.lang.Boolean.FALSE;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.CASE_TYPE;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.Event.CREATE;

@DisplayName("Get UI start trigger by case type and event ids")
class GetUIStartTriggerTest extends BaseTest {
    private static final String INVALID_CASE_TYPE_ID = "invalidCaseType";
    private static final String INVALID_EVENT_TRIGGER_ID = "invalidEvent";
    private static final String CREATE_NAME = "Create a new case";

    protected GetUIStartTriggerTest(AATHelper aat) {
        super(aat);
    }

    @Nested
    @DisplayName("Start case trigger")
    class StartCaseTrigger {

        @Test
        @DisplayName("should retrieve trigger when the case type and event exists")
        void shouldRetrieveWhenExists() {
            callCaseTypeUpdateViewEvent(CASE_TYPE, CREATE)
                .when()
                .get("/internal/case-types/{caseTypeId}/event-triggers/{triggerId}")

                .then()
                .log().ifError()
                .statusCode(200)
                .assertThat()

                // Metadata
                .body("id", equalTo(CREATE))
                .body("event_token", is(not(isEmptyString())))
                .body("name", is(CREATE_NAME))
                .body("description", is(nullValue()))
                .body("case_id", is(nullValue()))
                .body("show_summary", is(true))
                .body("show_event_notes", is(nullValue()))
                .body("end_button_label", is(nullValue()))
                .body("can_save_draft", is(nullValue()))

                // Flexible data
                .body("case_fields", hasSize(16))
                .body("wizard_pages", hasSize(3))

                .rootPath("_links")
                .body("self.href", equalTo(String.format("%s/internal/case-types/%s/event-triggers/%s{?ignore-warning}", aat.getTestUrl(), CASE_TYPE, CREATE)));
        }

        @Test
        @DisplayName("should get 404 when case type does not exist")
        void should404WhenCaseTypeDoesNotExist() {
            callCaseTypeUpdateViewEvent(INVALID_CASE_TYPE_ID, CREATE)
                .when()
                .get("/internal/case-types/{caseTypeId}/event-triggers/{triggerId}")

                .then()
                .statusCode(404);
        }

        @Test
        @DisplayName("should get 404 when event trigger does not exist")
        void should404WhenEventTriggerDoesNotExist() {
            callCaseTypeUpdateViewEvent(CASE_TYPE, INVALID_EVENT_TRIGGER_ID)
                .when()
                .get("/internal/case-types/{caseTypeId}/event-triggers/{triggerId}")

                .then()
                .statusCode(404);
        }

        private RequestSpecification callCaseTypeUpdateViewEvent(String caseTypeId, String eventId) {
            return asAutoTestCaseworker(FALSE)
                .get()
                .given()
                .pathParam("caseTypeId", caseTypeId)
                .pathParam("triggerId", eventId)
                .accept(V2.MediaType.CASE_TYPE_UPDATE_VIEW_EVENT)
                .header("experimental", "true");
        }
    }
}
