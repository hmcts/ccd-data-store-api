package uk.gov.hmcts.ccd.datastore.tests.v2.internal;

import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.datastore.tests.AATHelper;
import uk.gov.hmcts.ccd.datastore.tests.BaseTest;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder.FullCase;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.Event;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.State;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.Tab;
import uk.gov.hmcts.ccd.v2.V2;

import static java.lang.Boolean.FALSE;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.CASE_TYPE;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.JURISDICTION;

@DisplayName("Get UI case view by reference")
class GetUICaseViewTest extends BaseTest {
    private static final String NOT_FOUND_CASE_REFERENCE = "1234123412341238";
    private static final String INVALID_CASE_REFERENCE = "1234123412341234";

    protected GetUICaseViewTest(AATHelper aat) {
        super(aat);
    }

    @Test
    @DisplayName("should retrieve case view when the case reference exists")
    void shouldRetrieveWhenExists() {
        // Prepare new case in known state
        final Long caseReference = Event.create()
                                        .as(asAutoTestCaseworker())
                                        .withData(FullCase.build())
                                        .submitAndGetReference();

        whenCallingGetCaseView(caseReference.toString())
            .then()
            .log().ifError()
            .statusCode(200)
            .assertThat()

            // Metadata
            .body("case_id", equalTo(caseReference.toString()))
            .body("case_type.id", equalTo(CASE_TYPE))
            .body("case_type.jurisdiction.id", equalTo(JURISDICTION))
            .body("metadataFields", hasSize(7))
            // Tabs
            .body("tabs", hasSize(Tab.values().length))
            // State
            .body("state.id", equalTo(State.TODO))
            // Triggers
            .body("triggers", hasSize(3))
            // Events
            .body("events", hasSize(1))
            .body("events[0].event_id", equalTo(Event.CREATE))

            .rootPath("_links")
            .body("self.href", equalTo(String.format("%s/internal/cases/%s", aat.getTestUrl(), caseReference)))
        ;
    }

    @Test
    @DisplayName("should get 404 when case reference does NOT exist")
    void should404WhenNotExists() {
        whenCallingGetCaseView(NOT_FOUND_CASE_REFERENCE)
            .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("should get 400 when case reference invalid")
    void should400WhenReferenceInvalid() {
        whenCallingGetCaseView(INVALID_CASE_REFERENCE)
            .then()
            .statusCode(400);
    }

    private Response whenCallingGetCaseView(String caseReference) {
        return asAutoTestCaseworker(FALSE)
            .get()
            .given()
            .pathParam("caseReference", caseReference)
            .accept(V2.MediaType.UI_CASE_VIEW)
            .header("experimental", "true")

            .when()
            .get("/internal/cases/{caseReference}");
    }
}
