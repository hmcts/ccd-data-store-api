package uk.gov.hmcts.ccd.datastore.tests.functional;

import cucumber.api.Scenario;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import uk.gov.hmcts.ccd.datastore.tests.AATHelper;

import static org.hamcrest.core.IsEqual.equalTo;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.JURISDICTION;
import static uk.gov.hmcts.ccd.datastore.tests.util.CucumberHooks.getScenario;

public class CaseStepDef {

    Long caseReference;
    GetCaseTest getCaseTest = new GetCaseTest(AATHelper.INSTANCE);
    Response response;
    private Scenario scenario = getScenario();

    @Given("A case is available in system")
    public void aCaseIsAvailableInSystem() {
        caseReference = getCaseTest.createFullCase("AAT_AUTH_15");
        scenario.write("Created case reference : " + caseReference.toString());
    }

    @When("Send the request")
    public void sendTheRequest() {
        response = getCaseTest.asAutoTestCaseworker()
            .get()
            .given()
            .pathParam("jurisdiction", JURISDICTION)
            .pathParam("caseType", "AAT_AUTH_15")
            .pathParam("caseReference", caseReference)
            .contentType(ContentType.JSON)
            .when()
            .get("/caseworkers/{user}/jurisdictions/{jurisdiction}/case-types/{caseType}/cases/{caseReference}");
        scenario.write("status code: " + response.statusCode());
    }

    @Then("The case is returned")
    public void theCaseIsReturned() {
        scenario.write("response: " + response.prettyPrint());
        response.then()
            .log().ifError()
            .statusCode(200)
            .assertThat()

            // Metadata
            .body("jurisdiction", equalTo(JURISDICTION))
            .body("case_type_id", equalTo("AAT_AUTH_15"))
            .body("id", equalTo(caseReference));
    }

}
