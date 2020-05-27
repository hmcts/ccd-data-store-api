package uk.gov.hmcts.ccd.datastore.tests.functional;

import static org.hamcrest.Matchers.equalTo;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.CASE_TYPE;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.JURISDICTION;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.restassured.http.ContentType;
import uk.gov.hmcts.ccd.datastore.tests.AATHelper;
import uk.gov.hmcts.ccd.datastore.tests.BaseTest;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder.FullCase;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.Event;
import uk.gov.hmcts.ccd.datastore.tests.helper.CaseTestDataLoaderExtension;

@ExtendWith(CaseTestDataLoaderExtension.class)
@DisplayName("Get case by reference")
class GetCaseTest extends BaseTest {
    private static final String NOT_FOUND_CASE_REFERENCE = "1234123412341238";
    private static final String INVALID_CASE_REFERENCE = "1234123412341234";

    protected GetCaseTest(AATHelper aat) {
        super(aat);
    }

    @Test
    @DisplayName("should get 404 when case reference does NOT exist")
    void should404WhenNotExists() {
        asAutoTestCaseworker()
            .get()

            .given()
            .pathParam("jurisdiction", JURISDICTION)
            .pathParam("caseType", CASE_TYPE)
            .pathParam("caseReference", NOT_FOUND_CASE_REFERENCE)
            .contentType(ContentType.JSON)

            .when()
            .get("/caseworkers/{user}/jurisdictions/{jurisdiction}/case-types/{caseType}/cases/{caseReference}")

            .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("should get 400 when case reference invalid")
    void should400WhenReferenceInvalid() {
        asAutoTestCaseworker()
            .get()

            .given()
            .pathParam("jurisdiction", JURISDICTION)
            .pathParam("caseType", CASE_TYPE)
            .pathParam("caseReference", INVALID_CASE_REFERENCE)
            .contentType(ContentType.JSON)

            .when()
            .get("/caseworkers/{user}/jurisdictions/{jurisdiction}/case-types/{caseType}/cases/{caseReference}")

            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("should retrieve when a case reference exists if caseworker has 'CRUD' access on CaseType")
    void shouldRetrieveCaseWhenExistsWithFullAccessForCaseType() {
        // Prepare new case in known state
        final Long caseReference = createFullCase("AAT_AUTH_15");

        asAutoTestCaseworker()
            .get()

            .given()
            .pathParam("jurisdiction", JURISDICTION)
            .pathParam("caseType", "AAT_AUTH_15")
            .pathParam("caseReference", caseReference)
            .contentType(ContentType.JSON)

            .when()
            .get("/caseworkers/{user}/jurisdictions/{jurisdiction}/case-types/{caseType}/cases/{caseReference}")

            .then()
            .log().ifError()
            .statusCode(200)
            .assertThat()

            // Metadata
            .body("jurisdiction", equalTo(JURISDICTION))
            .body("case_type_id", equalTo("AAT_AUTH_15"))
            .body("id", equalTo(caseReference));
    }

    @Test
    @DisplayName("should retrieve when a case reference exists if caseworker has 'CR' access on CaseType")
    void shouldRetrieveCaseWhenExistsWithCRAccessForCaseType() {
        // Prepare new case in known state
        final Long caseReference = createFullCase("AAT_AUTH_3");

        asAutoTestCaseworker()
            .get()

            .given()
            .pathParam("jurisdiction", JURISDICTION)
            .pathParam("caseType", "AAT_AUTH_3")
            .pathParam("caseReference", caseReference)
            .contentType(ContentType.JSON)

            .when()
            .get("/caseworkers/{user}/jurisdictions/{jurisdiction}/case-types/{caseType}/cases/{caseReference}")

            .then()
            .log().ifError()
            .statusCode(200)
            .assertThat()

            // Metadata
            .body("jurisdiction", equalTo(JURISDICTION))
            .body("case_type_id", equalTo("AAT_AUTH_3"))
            .body("id", equalTo(caseReference));
    }

    @Test
    @DisplayName("should retrieve when a case reference exists if caseworker has 'RU' access on CaseType")
    void shouldRetrieveCaseWhenExistsWithRUAccessForCaseType() {
        // Prepare new case in known state
        final Long caseReference = createFullCase("AAT_AUTH_6");

        asAutoTestCaseworker()
            .get()

            .given()
            .pathParam("jurisdiction", JURISDICTION)
            .pathParam("caseType", "AAT_AUTH_6")
            .pathParam("caseReference", caseReference)
            .contentType(ContentType.JSON)

            .when()
            .get("/caseworkers/{user}/jurisdictions/{jurisdiction}/case-types/{caseType}/cases/{caseReference}")

            .then()
            .log().ifError()
            .statusCode(200)
            .assertThat()

            // Metadata
            .body("jurisdiction", equalTo(JURISDICTION))
            .body("case_type_id", equalTo("AAT_AUTH_6"))
            .body("id", equalTo(caseReference));
    }

    @Test
    @DisplayName("should retrieve when a case reference exists if caseworker has 'R' access on CaseType")
    void shouldRetrieveCaseWhenExistsWithRAccessForCaseType() {
        // Prepare new case in known state
        final Long caseReference = createFullCase("AAT_AUTH_2");

        asAutoTestCaseworker()
            .get()

            .given()
            .pathParam("jurisdiction", JURISDICTION)
            .pathParam("caseType", "AAT_AUTH_2")
            .pathParam("caseReference", caseReference)
            .contentType(ContentType.JSON)

            .when()
            .get("/caseworkers/{user}/jurisdictions/{jurisdiction}/case-types/{caseType}/cases/{caseReference}")

            .then()
            .log().ifError()
            .statusCode(200)
            .assertThat()

            // Metadata
            .body("jurisdiction", equalTo(JURISDICTION))
            .body("case_type_id", equalTo("AAT_AUTH_2"))
            .body("id", equalTo(caseReference));
    }

    @Test
    @DisplayName("should not retrieve when a case reference exists if caseworker has 'CU' access on CaseType")
    void shouldNotRetrieveCaseWhenExistsWithCUAccessForCaseType() {

        // Prepare new case in known state
        final Long caseReference = createFullCase("AAT_AUTH_5");

        asAutoTestCaseworker()
            .get()

            .given()
            .pathParam("jurisdiction", JURISDICTION)
            .pathParam("caseType", "AAT_AUTH_5")
            .pathParam("caseReference", caseReference)
            .contentType(ContentType.JSON)

            .when()
            .get("/caseworkers/{user}/jurisdictions/{jurisdiction}/case-types/{caseType}/cases/{caseReference}")

            .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("should not retrieve when a case reference exists if caseworker has 'C' access on CaseType")
    void shouldNotRetrieveCaseWhenExistsWithCAccessForCaseType() {

        // Prepare new case in known state
        final Long caseReference = createFullCase("AAT_AUTH_1");

        asAutoTestCaseworker()
            .get()

            .given()
            .pathParam("jurisdiction", JURISDICTION)
            .pathParam("caseType", "AAT_AUTH_1")
            .pathParam("caseReference", caseReference)
            .contentType(ContentType.JSON)

            .when()
            .get("/caseworkers/{user}/jurisdictions/{jurisdiction}/case-types/{caseType}/cases/{caseReference}")

            .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("should not retrieve when a case reference exists if caseworker has 'U' access on CaseType")
    void shouldNotRetrieveCaseWhenExistsWithUAccessForCaseType() {

        // Prepare new case in known state
        final Long caseReference = createFullCase("AAT_AUTH_4");

        asAutoTestCaseworker()
            .get()

            .given()
            .pathParam("jurisdiction", JURISDICTION)
            .pathParam("caseType", "AAT_AUTH_4")
            .pathParam("caseReference", caseReference)
            .contentType(ContentType.JSON)

            .when()
            .get("/caseworkers/{user}/jurisdictions/{jurisdiction}/case-types/{caseType}/cases/{caseReference}")

            .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("should not retrieve when a case reference exists if caseworker has 'D' access on CaseType")
    void shouldNotRetrieveCaseWhenExistsWithDAccessForCaseType() {

        // Prepare new case in known state
        final Long caseReference = createFullCase("AAT_AUTH_8");

        asAutoTestCaseworker()
            .get()

            .given()
            .pathParam("jurisdiction", JURISDICTION)
            .pathParam("caseType", "AAT_AUTH_8")
            .pathParam("caseReference", caseReference)
            .contentType(ContentType.JSON)

            .when()
            .get("/caseworkers/{user}/jurisdictions/{jurisdiction}/case-types/{caseType}/cases/{caseReference}")

            .then()
            .statusCode(404);
    }

    /*
       Method to create a Full Case with caseType param.
       This method uses 'privatecaseworker' as default user role
     */
    private Long createFullCase(String caseType) {

        return Event.create(caseType)
            .as(asPrivateCaseworker(true))
            .withData(FullCase.build())
            .submitAndGetReference();
    }
}
