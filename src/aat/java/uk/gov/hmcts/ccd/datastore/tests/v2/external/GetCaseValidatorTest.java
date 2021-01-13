package uk.gov.hmcts.ccd.datastore.tests.v2.external;

import static java.lang.Boolean.FALSE;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.CASE_TYPE;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.function.Supplier;

import io.restassured.specification.RequestSpecification;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.datastore.tests.AATHelper;
import uk.gov.hmcts.ccd.datastore.tests.BaseTest;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder.FullCase;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.CaseData;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.Event;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.v2.V2;

@DisplayName("Get UI start trigger by case type and event ids")
class GetCaseValidatorTest extends BaseTest {

    private static final String INVALID_CASE_TYPE_ID = "invalidCaseType";
    private static final String INVALID_EVENT_TRIGGER_ID = "invalidEvent";

    protected GetCaseValidatorTest(AATHelper aat) {
        super(aat);
    }

    @Nested
    @DisplayName("Start case trigger")
    class StartCaseTrigger {

        @Test
        @DisplayName("should get 422 when event not provided")
        void should422WhenEventNotProvided() throws JsonProcessingException {
            callCaseDataValidate(INVALID_CASE_TYPE_ID, getBody(null))
                .when()
                .post("/case-types/{caseTypeId}/validate")

                .then()
                .statusCode(422);
        }

        @Test
        @DisplayName("should get 422 when event trigger does not exist")
        void should422WhenEventTriggerDoesNotExist() throws JsonProcessingException {
            callCaseDataValidate(CASE_TYPE, getBody(INVALID_EVENT_TRIGGER_ID))
                .when()
                .post("/case-types/{caseTypeId}/validate")

                .then()
                .statusCode(422);
        }

        private RequestSpecification callCaseDataValidate(String caseTypeId, Supplier<String> supplier)
                                                          throws JsonProcessingException {
            return asAutoTestCaseworker(FALSE)
                .get()
                .given()
                .pathParam("caseTypeId", caseTypeId)
                .body(supplier.get())
                .contentType("application/json")
                .accept(V2.MediaType.CASE_DATA_VALIDATE)
                .header("experimental", "true");
        }

        private Supplier<String> getBody(String eventId) {
            return getBody(eventId, () -> FullCase.build());
        }

        private Supplier<String> getBody(String eventId, Supplier<CaseData> caseDataSupplier) {
            CaseDataContent caseDataContent = Event.create()
                .as(asAutoTestCaseworker())
                .withData(caseDataSupplier.get())
                .withEventId(eventId)
                .toCaseDataContent();
            return () -> JacksonUtils.convertValueJsonNode(caseDataContent).toString();
        }

    }
}
