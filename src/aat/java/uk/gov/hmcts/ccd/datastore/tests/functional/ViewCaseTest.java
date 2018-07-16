package uk.gov.hmcts.ccd.datastore.tests.functional;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.datastore.tests.AATHelper;
import uk.gov.hmcts.ccd.datastore.tests.BaseTest;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import java.util.function.Supplier;

class ViewCaseTest extends BaseTest {


    private static final String JURISDICTION = "AUTOTEST1";
    private static final String CASE_TYPE = "AAT";

    protected ViewCaseTest(AATHelper aat) { super(aat); }

    @Test
    @DisplayName("View a case")


    public void shouldUpdateACase() {

        Long caseID = shouldCreateACase();

        Supplier<RequestSpecification> asUser = asAutoTestCaseworker();

        asUser.get()
            .given()
            .pathParam("jurisdiction", JURISDICTION)
            .pathParam("caseType", CASE_TYPE)
            .pathParam("caseID",caseID)
            .contentType(ContentType.JSON)
            .when()
            .get(
                "/caseworkers/{user}/jurisdictions/{jurisdiction}/case-types/{caseType}/cases/{caseID}")
            .then()
            .statusCode(200);

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
            final Event event = new Event();
            event.setEventId("CREATE");

            final CaseDataContent caseData = new CaseDataContent();
            caseData.setEvent(event);

        return caseData;
        }

}
