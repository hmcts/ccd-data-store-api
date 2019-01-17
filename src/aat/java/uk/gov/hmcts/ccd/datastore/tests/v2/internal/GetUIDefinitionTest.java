package uk.gov.hmcts.ccd.datastore.tests.v2.internal;

import static java.lang.Boolean.FALSE;
import static org.hamcrest.Matchers.equalTo;

import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.datastore.tests.AATHelper;
import uk.gov.hmcts.ccd.datastore.tests.BaseTest;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType;
import uk.gov.hmcts.ccd.v2.V2;

@DisplayName("Get UI definition")
class GetUIDefinitionTest extends BaseTest {

    protected GetUIDefinitionTest(AATHelper aat) {
        super(aat);
    }

    @Test
    @DisplayName("should retrieve workbasket inputs")
    void shouldRetrieveWorkbasketInputsWhenExist() {

        whenCallingGetWorkbasketInputs(AATCaseType.CASE_TYPE)
            .then()
            .log().ifError()
            .statusCode(200)
            .assertThat()

            .body("workbasketInputs.find  { it.order == 1 }.label", equalTo("Search `Text` field"))
            .body("workbasketInputs.find  { it.order == 1 }.field.id", equalTo("TextField"))
            .body("workbasketInputs.find  { it.order == 1 }.field.field_type.id", equalTo("Text"))
            .body("workbasketInputs.find  { it.order == 1 }.field.field_type.type", equalTo("Text"))

            .rootPath("_links")
            .body("self.href", equalTo(String.format("%s/internal/case-types/AAT/work-basket-inputs", aat.getTestUrl())));
    }

    private Response whenCallingGetWorkbasketInputs(String caseTypeId) {
        return asAutoTestCaseworker(FALSE)
            .get()
            .given()
            .pathParam("caseTypeId", caseTypeId)
            .accept(V2.MediaType.UI_WORKBASKET_INPUT_DETAILS)
            .header("experimental", "true")

            .when()
            .get("/internal/case-types/{caseTypeId}/work-basket-inputs");
    }
}
