package uk.gov.hmcts.ccd.datastore.tests.v2.external;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.datastore.tests.AATHelper;
import uk.gov.hmcts.ccd.datastore.tests.BaseTest;
import uk.gov.hmcts.ccd.v2.V2;

import static java.lang.Boolean.FALSE;
import static org.hamcrest.Matchers.*;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.CASE_TYPE;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.Event.CREATE;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.JURISDICTION;

@DisplayName("Get start trigger by case type and event ids")
class GetStartTriggerTest extends BaseTest {
    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final ObjectNode EMPTY_OBJECT = JSON_NODE_FACTORY.objectNode();
    private static final String EMPTY_OBJECT_STRING = EMPTY_OBJECT.toString();
    private static final String INVALID_CASE_TYPE_ID = "invalidCaseType";
    private static final String INVALID_EVENT_TRIGGER_ID = "invalidEvent";

    protected GetStartTriggerTest(AATHelper aat) {
        super(aat);
    }

    @Test
    @DisplayName("should retrieve trigger when the case type and event exists")
    void shouldRetrieveWhenExists() {
        callGetStartTrigger(CASE_TYPE, CREATE)
            .when()
            .get("/case-types/{caseTypeId}/triggers/{triggerId}")

            .then()
            .log().ifError()
            .statusCode(200)
            .assertThat()

            // Metadata
            .body("event_id", equalTo(CREATE))
            .body("token", is(not(isEmptyString())))

            // Flexible data
            .rootPath("case_details")
            .body("id", is(nullValue()))
            .body("jurisdiction", equalTo(JURISDICTION))
            .body("state", is(nullValue()))
            .body("case_type_id", equalTo(CASE_TYPE))
            .body("created_date", is(nullValue()))
            .body("last_modified", is(nullValue()))
            .body("security_classification", is(nullValue()))
            .body("case_data", hasToString(EMPTY_OBJECT_STRING))
            .body("data_classification", hasToString(EMPTY_OBJECT_STRING))
            .body("after_submit_response_callback", is(nullValue()))
            .body("callback_response_status_code", is(nullValue()))
            .body("callback_response_status", is(nullValue()))
            .body("delete_draft_response_status_code", is(nullValue()))
            .body("delete_draft_response_status", is(nullValue()))
            .body("security_classifications", hasToString(EMPTY_OBJECT_STRING))

            .rootPath("_links")
            .body("self.href", equalTo(String.format("%s/case-types/%s/triggers/%s{?ignore-warning}", aat.getTestUrl(), CASE_TYPE, CREATE)))
        ;
    }

    @Test
    @DisplayName("should get 404 when case type does not exist")
    void should404WhenCaseTypeDoesNotExist() {
        callGetStartTrigger(INVALID_CASE_TYPE_ID, CREATE)
            .when()
            .get("/case-types/{caseTypeId}/triggers/{triggerId}")

            .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("should get 404 when event trigger does not exist")
    void should404WhenEventTriggerDoesNotExist() {
        callGetStartTrigger(CASE_TYPE, INVALID_EVENT_TRIGGER_ID)
            .when()
            .get("/case-types/{caseTypeId}/triggers/{triggerId}")

            .then()
            .statusCode(404);
    }

    private RequestSpecification callGetStartTrigger(String caseTypeId, String eventTriggerId) {
        return asAutoTestCaseworker(FALSE)
            .get()
            .given()
            .pathParam("caseTypeId", caseTypeId)
            .pathParam("triggerId", eventTriggerId)
            .accept(V2.MediaType.START_TRIGGER)
            .header("experimental", "true");
    }
}
