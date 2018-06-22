package uk.gov.hmcts.ccd.datastore.tests.functional;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.datastore.tests.AATHelper;
import uk.gov.hmcts.ccd.datastore.tests.BaseTest;
import java.util.function.Supplier;

@Tag("smoke")
class SearchCaseTest extends BaseTest {

    private static final String JURISDICTION = "AUTOTEST1";
    private static final String CASE_TYPE = "AAT";

    protected SearchCaseTest(AATHelper aat) { super(aat); }

    @Test
    @DisplayName("Search for cases")


    void shouldUpdateACase() {

        Supplier<RequestSpecification> asUser = asAutoTestCaseworker();

        asUser.get()
            .given()
            .pathParam("jurisdiction", JURISDICTION)
            .pathParam("caseType", CASE_TYPE)
            .contentType(ContentType.JSON)
            .when()
            .get(
                "/caseworkers/{user}/jurisdictions/{jurisdiction}/case-types/{caseType}/cases")
            .then()
            .statusCode(200);

        }

}
