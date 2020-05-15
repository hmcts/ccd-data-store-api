package uk.gov.hmcts.ccd.datastore.tests.v2.external;

import static java.lang.Boolean.FALSE;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.Event.CREATE;

import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.datastore.tests.AATHelper;
import uk.gov.hmcts.ccd.datastore.tests.BaseTest;
import uk.gov.hmcts.ccd.v2.V2;

@DisplayName("Get start trigger by case type and event ids")
class GetStartTriggerTest extends BaseTest {

    protected GetStartTriggerTest(AATHelper aat) {
        super(aat);
    }

    @Nested
    @DisplayName("Start event trigger")
    class StartEventResult {
        private static final String NOT_FOUND_CASE_REFERENCE = "1234123412341238";

        @Test
        @DisplayName("should get 404 when case does not exist")
        void should404WhenCaseDoesNotExist() {
            callGetStartEventTrigger(NOT_FOUND_CASE_REFERENCE, CREATE)
                .when()
                .get("/cases/{caseId}/event-triggers/{triggerId}")

                .then()
                .statusCode(404);
        }

    }

    private RequestSpecification callGetStartEventTrigger(String caseId, String eventId) {
        return asAutoTestCaseworker(FALSE)
            .get()
            .given()
            .pathParam("caseId", caseId)
            .pathParam("triggerId", eventId)
            .accept(V2.MediaType.START_EVENT)
            .header("experimental", "true");
    }
}
