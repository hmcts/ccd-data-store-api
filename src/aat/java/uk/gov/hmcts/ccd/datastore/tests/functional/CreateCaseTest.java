package uk.gov.hmcts.ccd.datastore.tests.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.datastore.tests.AATHelper;
import uk.gov.hmcts.ccd.datastore.tests.BaseTest;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder.EmptyCase;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.Event;

import static org.hamcrest.Matchers.equalTo;

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
    @DisplayName("should create a case if the caseworker has CREATE access on a CaseType")
    void shouldCreateCaseWithCreateAccessForCaseTypeACL() {
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

        Event.create("AAT_AUTH_1")
            .as(asAutoTestCaseworker())
            .withData(AATCaseBuilder.FullCase.build())
            .submit()

            .then()
            .log().ifError()
            .statusCode(201);

        Event.create("AAT_AUTH_5")
            .as(asAutoTestCaseworker())
            .withData(AATCaseBuilder.FullCase.build())
            .submit()

            .then()
            .log().ifError()
            .statusCode(201);

    }

    @Test
    @DisplayName("should not create a case if the caseworker doesn't have CREATE access on a CaseType")
    void shouldNotCreateCaseWithNoCreateAccess() {
        Event.create("AAT_AUTH_2")
            .as(asAutoTestCaseworker())
            .withData(AATCaseBuilder.FullCase.build())
            .submit()

            .then()
            .log().ifError()
            .statusCode(404);

        Event.create("AAT_AUTH_4")
            .as(asAutoTestCaseworker())
            .withData(AATCaseBuilder.FullCase.build())
            .submit()

            .then()
            .log().ifError()
            .statusCode(404);

        Event.create("AAT_AUTH_6")
            .as(asAutoTestCaseworker())
            .withData(AATCaseBuilder.FullCase.build())
            .submit()

            .then()
            .log().ifError()
            .statusCode(404);

        Event.create("AAT_AUTH_8")
            .as(asAutoTestCaseworker())
            .withData(AATCaseBuilder.FullCase.build())
            .submit()

            .then()
            .log().ifError()
            .statusCode(404);
    }
}
