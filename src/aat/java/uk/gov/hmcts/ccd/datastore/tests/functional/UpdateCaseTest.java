package uk.gov.hmcts.ccd.datastore.tests.functional;

import static org.hamcrest.Matchers.equalTo;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.JURISDICTION;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.hmcts.ccd.datastore.tests.AATHelper;
import uk.gov.hmcts.ccd.datastore.tests.BaseTest;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder.FullCase;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.CaseData;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.Event;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.State;
import uk.gov.hmcts.ccd.datastore.tests.helper.CaseTestDataLoaderExtension;

@ExtendWith(CaseTestDataLoaderExtension.class)
@DisplayName("Update case")
class UpdateCaseTest extends BaseTest {

    private static final String UPDATED_NUMBER = "4732";

    protected UpdateCaseTest(AATHelper aat) {
        super(aat);
    }

    @Test
    @DisplayName("should progress case state")
    void shouldProgressCaseState() {
        // Prepare new case in known state
        final Long caseReference = Event.create()
                                        .as(asAutoTestCaseworker())
                                        .withData(AATCaseBuilder.EmptyCase.build())
                                        .submitAndGetReference();

        Event.startProgress(caseReference)
             .as(asAutoTestCaseworker())
             .submit()
             .then()
             .statusCode(201)
             .assertThat()
             .body("state", equalTo(State.IN_PROGRESS));
    }

    @Test
    @DisplayName("should update a single case field")
    void shouldUpdateSingleField() {
        // Prepare new case in known state
        final Long caseReference = Event.create()
                                        .as(asAutoTestCaseworker())
                                        .withData(FullCase.build())
                                        .submitAndGetReference();

        Event.update(caseReference)
             .as(asAutoTestCaseworker())
             .withData(
                 CaseData.builder()
                         .numberField(UPDATED_NUMBER)
                         .build()
             )
             .submit()

             .then()
             .statusCode(201)
             .assertThat()
             .rootPath("case_data")

             // Field updated
             .body("NumberField", equalTo(UPDATED_NUMBER))

             // Other fields not updated
             .body("TextField", equalTo(AATCaseBuilder.TEXT));
    }

    @Test
    @DisplayName("should update a case if the caseworker has 'CRUD' access on CaseType")
    void shouldUpdateCaseWithFullAccessForCaseType() {
        // Case Type with "CRUD" access to the role autoTestCaseWorker
        final Long caseReference = Event.create("AAT_AUTH_15")
            .as(asPrivateCaseworker(true))
            .withData(FullCase.build())
            .submitAndGetReference();

        Event.update(caseReference)
            .as(asAutoTestCaseworker())
            .withData(
                CaseData.builder()
                    .numberField(UPDATED_NUMBER)
                    .build()
            )
            .submit()

            .then()
            .statusCode(201)
            .assertThat()
            .rootPath("case_data")
            .body("NumberField", equalTo(UPDATED_NUMBER))
            .body("TextField", equalTo(AATCaseBuilder.TEXT));
    }

    @Test
    @DisplayName("should update a case if the caseworker has 'U' access on CaseType")
    void shouldUpdateCaseWithUAccessForCaseType() {
        // Case Type with "U" access to the role autoTestCaseWorker
        final Long caseReference = Event.create("AAT_AUTH_4")
            .as(asPrivateCaseworker(true))
            .withData(FullCase.build())
            .submitAndGetReference();

        Event.update(caseReference)
            .as(asAutoTestCaseworker())
            .withData(
                CaseData.builder()
                    .numberField(UPDATED_NUMBER)
                    .build()
            )
            .submit()

            .then()
            .statusCode(201);

        //read the case data using private case worker and verify that the case was successfully updated.
        asPrivateCaseworker(true).get().given()
            .pathParam("jurisdiction", JURISDICTION)
            .pathParam("caseType", "AAT_AUTH_4")
            .pathParam("caseReference", caseReference)
            .contentType(ContentType.JSON).when()
            .get("/caseworkers/{user}/jurisdictions/{jurisdiction}/case-types/{caseType}/cases/{caseReference}")
            .then().log().ifError().statusCode(200)
            .assertThat()
            .rootPath("case_data")
            .body("NumberField", equalTo(UPDATED_NUMBER))
            .body("TextField", equalTo(AATCaseBuilder.TEXT));
    }

    @Test
    @DisplayName("should update a case if the caseworker has 'CU' access on CaseType")
    void shouldUpdateCaseWithCUAccessForCaseType() {
        // Case Type with "CU" access to the role autoTestCaseWorker
        final Long caseReference = Event.create("AAT_AUTH_5")
            .as(asPrivateCaseworker(true))
            .withData(FullCase.build())
            .submitAndGetReference();

        Event.update(caseReference)
            .as(asAutoTestCaseworker())
            .withData(
                CaseData.builder()
                    .numberField(UPDATED_NUMBER)
                    .build()
            )
            .submit()

            .then()
            .statusCode(201);

        //read the case data using private case worker and verify that the case was successfully updated.
        asPrivateCaseworker(true).get().given()
            .pathParam("jurisdiction", JURISDICTION)
            .pathParam("caseType", "AAT_AUTH_5")
            .pathParam("caseReference", caseReference)
            .contentType(ContentType.JSON).when()
            .get("/caseworkers/{user}/jurisdictions/{jurisdiction}/case-types/{caseType}/cases/{caseReference}")
            .then().log().ifError().statusCode(200)
            .assertThat()
            .rootPath("case_data")
            .body("NumberField", equalTo(UPDATED_NUMBER))
            .body("TextField", equalTo(AATCaseBuilder.TEXT));
    }

    @Test
    @DisplayName("should update a case if the caseworker has 'RU' access on CaseType")
    void shouldUpdateCaseWithRUAccessForCaseType() {
        // Case Type with "RU" access to the role autoTestCaseWorker
        final Long caseReference = Event.create("AAT_AUTH_6")
            .as(asPrivateCaseworker(true))
            .withData(FullCase.build())
            .submitAndGetReference();

        Event.update(caseReference)
            .as(asAutoTestCaseworker())
            .withData(
                CaseData.builder()
                    .numberField(UPDATED_NUMBER)
                    .build()
            )
            .submit()

            .then()
            .statusCode(201)
            .assertThat()
            .rootPath("case_data")
            .body("NumberField", equalTo(UPDATED_NUMBER))
            .body("TextField", equalTo(AATCaseBuilder.TEXT));
    }

    @Test
    @DisplayName("should not update a case if the caseworker has 'CR' access on CaseType")
    void shouldNotUpdateCaseWithCRAccessForCaseType() {
        // Case Type with "CR" access to the role autoTestCaseWorker
        final Long caseReference = Event.create("AAT_AUTH_3")
            .as(asPrivateCaseworker(true))
            .withData(FullCase.build())
            .submitAndGetReference();

        Event.update(caseReference)
            .as(asAutoTestCaseworker())
            .withData(
                CaseData.builder()
                    .numberField(UPDATED_NUMBER)
                    .build()
            )
            .submit()

            .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("should not update a case if the caseworker has 'C' access on CaseType")
    void shouldNotUpdateCaseWithCAccessForCaseType() {
        // Case Type with "C" access to the role autoTestCaseWorker
        final Long caseReference = Event.create("AAT_AUTH_1")
            .as(asPrivateCaseworker(true))
            .withData(FullCase.build())
            .submitAndGetReference();

        Event.update(caseReference)
            .as(asAutoTestCaseworker())
            .withData(
                CaseData.builder()
                    .numberField(UPDATED_NUMBER)
                    .build()
            )
            .submit()

            .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("should not update a case if the caseworker has 'R' access on CaseType")
    void shouldNotUpdateCaseWithRAccessForCaseType() {
        // Case Type with "R" access to the role autoTestCaseWorker
        final Long caseReference = Event.create("AAT_AUTH_2")
            .as(asPrivateCaseworker(true))
            .withData(FullCase.build())
            .submitAndGetReference();

        Event.update(caseReference)
            .as(asAutoTestCaseworker())
            .withData(
                CaseData.builder()
                    .numberField(UPDATED_NUMBER)
                    .build()
            )
            .submit()

            .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("should not update a case if the caseworker has 'D' access on CaseType")
    void shouldNotUpdateCaseWithDAccessForCaseType() {
        // Case Type with "D" access to the role autoTestCaseWorker
        final Long caseReference = Event.create("AAT_AUTH_8")
            .as(asPrivateCaseworker(true))
            .withData(FullCase.build())
            .submitAndGetReference();

        Event.update(caseReference)
            .as(asAutoTestCaseworker())
            .withData(
                CaseData.builder()
                    .numberField(UPDATED_NUMBER)
                    .build()
            )
            .submit()

            .then()
            .statusCode(404);
    }
}
