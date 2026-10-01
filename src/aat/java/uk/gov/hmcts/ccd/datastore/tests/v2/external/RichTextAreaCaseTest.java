package uk.gov.hmcts.ccd.datastore.tests.v2.external;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.datastore.tests.AATHelper;
import uk.gov.hmcts.ccd.datastore.tests.BaseTest;
import uk.gov.hmcts.ccd.v2.V2;

import java.util.function.Supplier;

import static java.lang.Boolean.FALSE;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@DisplayName("RichTextArea case data")
class RichTextAreaCaseTest extends BaseTest {

    private static final String CASE_TYPE = "AllDataTypes2";
    private static final String CREATE_EVENT = "createCase";
    private static final String UPDATE_EVENT = "updateRichTextArea";
    private static final String RICH_TEXT_AREA_FIELD = "RichTextAreaField";
    private static final String RICH_TEXT_AREA_COMPLEX_FIELD = "RichTextAreaComplexField";
    private static final String RICH_TEXT_AREA_COMPLEX_ELEMENT = "RichTextAreaElement";
    private static final String CREATED_RICH_TEXT = "<p><strong>Order</strong> created for CCD-7988</p>";
    private static final String CREATED_COMPLEX_RICH_TEXT =
        "<p><em>Complex order text created for CCD-7988</em></p>";
    private static final String UPDATED_RICH_TEXT = "<p><strong>Order</strong> updated for CCD-7988</p>";
    private static final String UPDATED_COMPLEX_RICH_TEXT =
        "<p><em>Complex order text updated for CCD-7988</em></p>";

    protected RichTextAreaCaseTest(AATHelper aat) {
        super(aat);
    }

    @Test
    @DisplayName("should create, update and retrieve RichTextArea fields")
    void shouldCreateUpdateAndRetrieveRichTextAreaFields() {
        String createToken = getStartCaseToken(CREATE_EVENT);

        String caseReference = createCase(createToken)
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("data." + RICH_TEXT_AREA_FIELD, equalTo(CREATED_RICH_TEXT))
            .body("data." + RICH_TEXT_AREA_COMPLEX_FIELD + "." + RICH_TEXT_AREA_COMPLEX_ELEMENT,
                  equalTo(CREATED_COMPLEX_RICH_TEXT))
            .extract()
            .path("id");

        String updateToken = getStartEventToken(caseReference, UPDATE_EVENT);

        updateCase(caseReference, updateToken)
            .then()
            .statusCode(201)
            .body("id", equalTo(caseReference))
            .body("data." + RICH_TEXT_AREA_FIELD, equalTo(UPDATED_RICH_TEXT))
            .body("data." + RICH_TEXT_AREA_COMPLEX_FIELD + "." + RICH_TEXT_AREA_COMPLEX_ELEMENT,
                  equalTo(UPDATED_COMPLEX_RICH_TEXT));

        getCase(caseReference)
            .then()
            .statusCode(200)
            .body("id", equalTo(caseReference))
            .body("data." + RICH_TEXT_AREA_FIELD, equalTo(UPDATED_RICH_TEXT))
            .body("data." + RICH_TEXT_AREA_COMPLEX_FIELD + "." + RICH_TEXT_AREA_COMPLEX_ELEMENT,
                  equalTo(UPDATED_COMPLEX_RICH_TEXT));
    }

    private String getStartCaseToken(String eventId) {
        return asV2AutoTestCaseworker()
            .get()
            .given()
            .pathParam("caseTypeId", CASE_TYPE)
            .pathParam("triggerId", eventId)
            .accept(V2.MediaType.START_CASE_EVENT)
            .when()
            .get("/case-types/{caseTypeId}/event-triggers/{triggerId}?ignore-warning=true")
            .then()
            .statusCode(200)
            .extract()
            .path("token");
    }

    private String getStartEventToken(String caseReference, String eventId) {
        return asV2AutoTestCaseworker()
            .get()
            .given()
            .pathParam("caseId", caseReference)
            .pathParam("triggerId", eventId)
            .accept(V2.MediaType.START_EVENT)
            .when()
            .get("/cases/{caseId}/event-triggers/{triggerId}?ignore-warning=true")
            .then()
            .statusCode(200)
            .extract()
            .path("token");
    }

    private Response createCase(String token) {
        return asV2AutoTestCaseworker()
            .get()
            .given()
            .pathParam("caseTypeId", CASE_TYPE)
            .contentType(V2.MediaType.CREATE_CASE)
            .accept(V2.MediaType.CREATE_CASE)
            .body(caseDataContent(CREATE_EVENT, token, CREATED_RICH_TEXT, CREATED_COMPLEX_RICH_TEXT))
            .when()
            .post("/case-types/{caseTypeId}/cases?ignore-warning=true");
    }

    private Response updateCase(String caseReference, String token) {
        return asV2AutoTestCaseworker()
            .get()
            .given()
            .pathParam("caseId", caseReference)
            .contentType(V2.MediaType.CREATE_EVENT)
            .accept(V2.MediaType.CREATE_EVENT)
            .body(caseDataContent(UPDATE_EVENT, token, UPDATED_RICH_TEXT, UPDATED_COMPLEX_RICH_TEXT))
            .when()
            .post("/cases/{caseId}/events");
    }

    private Response getCase(String caseReference) {
        return asV2AutoTestCaseworker()
            .get()
            .given()
            .pathParam("caseId", caseReference)
            .accept(V2.MediaType.CASE)
            .when()
            .get("/cases/{caseId}");
    }

    private Supplier<RequestSpecification> asV2AutoTestCaseworker() {
        return () -> asAutoTestCaseworker(FALSE)
            .get()
            .given()
            .header("experimental", "true");
    }

    private String caseDataContent(String eventId, String token, String richText, String complexRichText) {
        return """
            {
              "event_token": "%s",
              "event": {
                "id": "%s",
                "summary": "CCD-7988 RichTextArea",
                "description": "RichTextArea AAT"
              },
              "data": {
                "%s": "%s",
                "%s": {
                  "%s": "%s"
                }
              }
            }
            """.formatted(
                token,
                eventId,
                RICH_TEXT_AREA_FIELD,
                richText,
                RICH_TEXT_AREA_COMPLEX_FIELD,
                RICH_TEXT_AREA_COMPLEX_ELEMENT,
                complexRichText
            );
    }
}
