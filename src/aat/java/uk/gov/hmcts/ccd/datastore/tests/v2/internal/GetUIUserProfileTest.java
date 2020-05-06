package uk.gov.hmcts.ccd.datastore.tests.v2.internal;

import static java.lang.Boolean.FALSE;

import static org.hamcrest.Matchers.*;

import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.datastore.tests.AATHelper;
import uk.gov.hmcts.ccd.datastore.tests.BaseTest;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType;
import uk.gov.hmcts.ccd.v2.V2;

@DisplayName("Get UI user profile")
class GetUIUserProfileTest extends BaseTest {

    protected GetUIUserProfileTest(AATHelper aat) {
        super(aat);
    }

    @Test
    @DisplayName("should retrieve user profile")
    void shouldRetrieveWhenExists() {

        whenCallingGetUserProfile()
            .then()
            .log().ifError()
            .statusCode(200)
            .assertThat()

            // commented out in commit f628cdc: "Comment out IDAM section to relax assert conditions and unblock non AAT environments" see PR-360
            //
            //    .body("user.idam.defaultService", equalTo("CCD"))
            //    .body("user.idam.roles", hasItems(equalTo("caseworker"),
            //                                      equalTo("caseworker-autotest1"),
            //                                      equalTo("caseworker-loa1"),
            //                                      equalTo("caseworker-autotest1-loa1")))
            //    .body("channels.id", is(nullValue()))

            .body("jurisdictions.find { it.id == 'AUTOTEST1' }.name", equalTo("Auto Test 1"))
            .body("jurisdictions.find { it.id == 'AUTOTEST1' }.description", equalTo("Content for the Test Jurisdiction."))
            .body("jurisdictions.find { it.id == 'AUTOTEST1' }.caseTypes.find { it.id == 'MAPPER' }.name", equalTo("Case type for Mapper"))
            .body("jurisdictions.find { it.id == 'AUTOTEST1' }.caseTypes.find { it.id == 'AAT' }.name", equalTo("Demo case"))
            .body("default.workbasket.jurisdiction_id", equalTo(AATCaseType.JURISDICTION))
            .body("default.workbasket.case_type_id", not(nullValue()))
            .body("default.workbasket.state_id", equalTo(AATCaseType.State.TODO))

            .rootPath("_links")
            .body("self.href", equalTo(String.format("%s/internal/profile", aat.getTestUrl())));
    }

    private Response whenCallingGetUserProfile() {
        return asAutoTestCaseworker(FALSE)
            .get()
            .accept(V2.MediaType.UI_USER_PROFILE)
            .header("experimental", "true")

            .when()
            .get("/internal/profile");
    }
}
