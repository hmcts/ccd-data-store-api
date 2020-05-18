package uk.gov.hmcts.ccd.datastore.tests.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.hmcts.ccd.datastore.tests.AATHelper;
import uk.gov.hmcts.ccd.datastore.tests.BaseTest;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder.EmptyCase;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.Event;
import uk.gov.hmcts.ccd.datastore.tests.helper.CaseTestDataLoaderExtension;

import static org.hamcrest.Matchers.equalTo;

@ExtendWith(CaseTestDataLoaderExtension.class)
@DisplayName("Create case")
class CreateCaseTest extends BaseTest {

    protected CreateCaseTest(AATHelper aat) {
        super(aat);
    }

    @Test
    @DisplayName("should create a new empty case")
    void shouldCreateEmptyCase() {
        Event.create()
            .as(asAutoTestCaseworker())
            .withData(EmptyCase.build())
            .submit()

            .then()
            .log().ifError()
            .statusCode(201)

            .assertThat()
            .body("jurisdiction", equalTo(AATCaseType.JURISDICTION))
            .body("case_type_id", equalTo(AATCaseType.CASE_TYPE))
            .body("state", equalTo(AATCaseType.State.TODO));
    }

    @Test
    @DisplayName("should create a case if caseworker has 'CR' access on CaseType")
    void shouldCreateCaseWithCRAccessForCaseType() {
        //Case Type with "CR" access to the role autoTestCaseWorker
        Event.create("AAT_AUTH_3")
            .as(asAutoTestCaseworker())
            .withData(AATCaseBuilder.FullCase.build())
            .submit()

            .then()
            .log().ifError()
            .statusCode(201)

            .assertThat()
            .body("jurisdiction", equalTo(AATCaseType.JURISDICTION))
            .body("case_type_id", equalTo("AAT_AUTH_3"))
            .body("state", equalTo(AATCaseType.State.TODO));
    }

    @Test
    @DisplayName("should create a case if caseworker has 'CRUD' access on CaseType")
    void shouldCreateCaseWithFullAccessForCaseType() {
        //Case Type with "CRUD" access to the role autoTestCaseWorker
        Event.create("AAT_AUTH_15")
            .as(asAutoTestCaseworker())
            .withData(AATCaseBuilder.FullCase.build())
            .submit()

            .then()
            .log().ifError()
            .statusCode(201)

            .assertThat()
            .body("jurisdiction", equalTo(AATCaseType.JURISDICTION))
            .body("case_type_id", equalTo("AAT_AUTH_15"))
            .body("state", equalTo(AATCaseType.State.TODO));
    }

    @Test
    @DisplayName("should create a case if caseworker has 'C' access on CaseType")
    void shouldCreateCaseWithCAccessForCaseType() {
        //Case Type with "C" access to the role autoTestCaseWorker
        Event.create("AAT_AUTH_1")
            .as(asAutoTestCaseworker())
            .withData(AATCaseBuilder.FullCase.build())
            .submit()
            .then()
            .log().ifError()
            .statusCode(201);
    }

    @Test
    @DisplayName("should create a case if caseworker has 'CU' access on CaseType")
    void shouldCreateCaseWithCUAccessForCaseType() {
        //Case Type with "CU" access to the role autoTestCaseWorker
        Event.create("AAT_AUTH_5")
            .as(asAutoTestCaseworker())
            .withData(AATCaseBuilder.FullCase.build())
            .submit()
            .then()
            .log().ifError()
            .statusCode(201);
    }

    @Test
    @DisplayName("should not create a case if caseworker has 'R' on CaseType")
    void shouldNotCreateCaseWithRAccessForCaseType() {
        //Case Type with "R" access to the role autoTestCaseWorker
        Event.create("AAT_AUTH_2")
            .as(asAutoTestCaseworker())
            .withData(AATCaseBuilder.FullCase.build())
            .submit()
            .then()
            .log().ifError()
            .statusCode(404);
    }

    @Test
    @DisplayName("should not create a case if caseworker has 'U' on CaseType")
    void shouldNotCreateCaseWithUAccessForCaseType() {
        //Case Type with "U" access to the role autoTestCaseWorker
        Event.create("AAT_AUTH_4")
            .as(asAutoTestCaseworker())
            .withData(AATCaseBuilder.FullCase.build())
            .submit()
            .then()
            .log().ifError()
            .statusCode(404);
    }

    @Test
    @DisplayName("should not create a case if caseworker has 'RU' on CaseType")
    void shouldNotCreateCaseWithRUAccessForCaseType() {
        //Case Type with "RU" access to the role autoTestCaseWorker
        Event.create("AAT_AUTH_6")
            .as(asAutoTestCaseworker())
            .withData(AATCaseBuilder.FullCase.build())
            .submit()
            .then()
            .log().ifError()
            .statusCode(404);
    }

    @Test
    @DisplayName("should not create a case if caseworker has 'D' on CaseType")
    void shouldNotCreateCaseWithRDAccessForCaseType() {
        //Case Type with "D" access to the role autoTestCaseWorker
        Event.create("AAT_AUTH_8")
            .as(asAutoTestCaseworker())
            .withData(AATCaseBuilder.FullCase.build())
            .submit()
            .then()
            .log().ifError()
            .statusCode(404);
    }

}
