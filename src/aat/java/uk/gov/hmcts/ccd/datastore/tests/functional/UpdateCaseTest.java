package uk.gov.hmcts.ccd.datastore.tests.functional;

import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.datastore.tests.AATHelper;
import uk.gov.hmcts.ccd.datastore.tests.BaseTest;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder.FullCase;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.CaseData;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.Event;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.State;

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
    @DisplayName("should update a case with CRUD Access for a Case Type")
    void shouldUpdateCaseWithCRUDAccessCaseType() {
        // Prepare new case in known state
        final Long caseReference = Event.create("AAT_AUTH_15")
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
    @DisplayName("should not update a case with CR Access for a Case Type")
    void shouldNotUpdateCaseWithCRAccessCaseType() {
        // Prepare new case in known state
        final Long caseReference = Event.create("AAT_AUTH_3")
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
            .statusCode(404);
    }

    @Test
    @DisplayName("should not update a case with C Access for a Case Type")
    void shouldNotUpdateCaseWithCAccessCaseType() {
        // Prepare new case in known state
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
}
