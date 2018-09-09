package uk.gov.hmcts.ccd.datastore.tests.functional;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.datastore.tests.AATHelper;
import uk.gov.hmcts.ccd.datastore.tests.BaseTest;

import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.CASE_TYPE;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.JURISDICTION;

@Tag("smoke")
@DisplayName("Search cases")
class SearchCaseTest extends BaseTest {

    protected SearchCaseTest(AATHelper aat) {
        super(aat);
    }

    @Test
    @DisplayName("should succeed with 200")
    void shouldReturn200() {
        asAutoTestCaseworker()
            .get()
            .given()
            .pathParam("jurisdiction", JURISDICTION)
            .pathParam("caseType", CASE_TYPE)
            .contentType(ContentType.JSON)

            .when()
            .get("/caseworkers/{user}/jurisdictions/{jurisdiction}/case-types/{caseType}/cases")

            .then()
            .statusCode(200);
    }
}
