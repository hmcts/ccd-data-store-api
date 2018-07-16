package uk.gov.hmcts.ccd.datastore.tests.functional;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.datastore.tests.AATHelper;
import uk.gov.hmcts.ccd.datastore.tests.BaseTest;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;

import java.util.function.Supplier;

import static uk.gov.hmcts.ccd.domain.model.std.CaseDataContentBuilder.aCaseDataContent;
import static uk.gov.hmcts.ccd.domain.model.std.EventBuilder.anEvent;

class UpdateCaseTest extends BaseTest {


    private static final String EVENT_UPDATE = "START_PROGRESS";
    private static final String JURISDICTION = "AUTOTEST1";
    private static final String CASE_TYPE = "AAT";

    protected UpdateCaseTest(AATHelper aat) { super(aat); }

    @Test
    @DisplayName("Update a case")


    public void shouldUpdateACase() {

        Long caseID = shouldCreateACase();

        String eventToken  = aat.getCcdHelper()
            .generateTokenUpdateCase(asAutoTestCaseworker(),JURISDICTION,CASE_TYPE,EVENT_UPDATE,caseID);

        String eventBody = createUpdateBody(eventToken).toString();


        Supplier<RequestSpecification> asUser = asAutoTestCaseworker();


        asUser.get()
            .given()
            .pathParam("jurisdiction", JURISDICTION)
            .pathParam("caseType", CASE_TYPE)
            .pathParam("caseID",caseID)
            .contentType(ContentType.JSON)
            .body(eventBody)
            .when()
            .post(
                "/caseworkers/{user}/jurisdictions/{jurisdiction}/case-types/{caseType}/cases/{caseID}/events")
            .then()
            .statusCode(201);

        }

        Long shouldCreateACase() {

        Long caseID= aat.getCcdHelper()
            .createCase(asAutoTestCaseworker(), JURISDICTION, CASE_TYPE, "CREATE", createEmptyCase())
            .then()
            .extract()
            .path("id");

        return caseID;
        }

        private CaseDataContent createEmptyCase() {
            final Event event = anEvent().build();
            event.setEventId("CREATE");

            final CaseDataContent caseData = aCaseDataContent().build();
            caseData.setEvent(event);

        return caseData;
        }

        private JSONObject createUpdateBody(String eventToken){
          JSONObject eventBody = new JSONObject();
             try {
                 eventBody.put("event_token", eventToken);
                 JSONObject event = new JSONObject();
                 event.put("description", "This is an update");
                 event.put("id", EVENT_UPDATE);
                 event.put("summary", "Well this is a summary");
                 eventBody.put("event", event);
                 }
             catch (JSONException e) { }

        return eventBody;
        }




}
