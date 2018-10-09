package uk.gov.hmcts.ccd.datastore.tests.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.datastore.tests.AATHelper;
import uk.gov.hmcts.ccd.datastore.tests.BaseTest;
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
}
