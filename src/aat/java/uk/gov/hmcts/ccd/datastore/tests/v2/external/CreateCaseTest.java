package uk.gov.hmcts.ccd.datastore.tests.v2.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.datastore.tests.AATHelper;
import uk.gov.hmcts.ccd.datastore.tests.BaseTest;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder.FullCase;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.Event;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.v2.V2;

import java.util.function.Supplier;

import static java.lang.Boolean.FALSE;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.CASE_TYPE;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.Event.CREATE;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.JURISDICTION;

@DisplayName("Create case")
class CreateCaseTest extends BaseTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String INVALID_EVENT_TRIGGER_ID = "invalidEvent";

    protected CreateCaseTest(AATHelper aat) {
        super(aat);
    }

    @Test
    @DisplayName("should get 404 when event trigger id invalid")
    void should404WhenEventTriggerIdInvalid() {
        String eventToken = aat.getCcdHelper().generateTokenCreateCase(asAutoTestCaseworker(), JURISDICTION, CASE_TYPE, CREATE);

        callCreateCase(CASE_TYPE, getBody(INVALID_EVENT_TRIGGER_ID, eventToken, FullCase::build))
            .when()
            .post("/case-types/{caseTypeId}/cases")

            .then()
            .statusCode(404);
    }

    private RequestSpecification callCreateCase(final String caseTypeId, Supplier<String> supplier) {
        return asAutoTestCaseworker(FALSE)
            .get()
            .given()
            .body(supplier.get())
            .contentType(ContentType.JSON)
            .log().all()
            .pathParam("caseTypeId", caseTypeId)
            .accept(V2.MediaType.CREATE_CASE)
            .header("experimental", "true");
    }

    private Supplier<String> getBody(String eventId, String eventToken, Supplier<AATCaseType.CaseData> caseDataSupplier) {
        CaseDataContent caseDataContent = Event.create()
            .as(asAutoTestCaseworker())
            .withData(caseDataSupplier.get())
            .withEventId(eventId)
            .withToken(eventToken)
            .toCaseDataContent();
        return () -> MAPPER.convertValue(caseDataContent, JsonNode.class).toString();
    }

}
