package uk.gov.hmcts.ccd.datastore.tests.v2.external;

import static java.lang.Boolean.FALSE;
import static org.hamcrest.Matchers.*;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.CASE_TYPE;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.Event.CREATE;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.Event.START_PROGRESS;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.Event.create;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.JURISDICTION;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.State.TODO;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.datastore.tests.AATHelper;
import uk.gov.hmcts.ccd.datastore.tests.BaseTest;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder.FullCase;
import uk.gov.hmcts.ccd.v2.V2;

@DisplayName("Get start trigger by case type and event ids")
class GetStartTriggerTest extends BaseTest {
    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final ObjectNode EMPTY_OBJECT = JSON_NODE_FACTORY.objectNode();
    private static final String EMPTY_OBJECT_STRING = EMPTY_OBJECT.toString();
    private static final String INVALID_CASE_TYPE_ID = "invalidCaseType";
    private static final String INVALID_EVENT_TRIGGER_ID = "invalidEvent";

    protected GetStartTriggerTest(AATHelper aat) {
        super(aat);
    }

    @Nested
    @DisplayName("Start case trigger")
    class StartCaseTrigger {

        // @Tag("smoke")
        // @Test
        // @DisplayName("should retrieve trigger when the case type and event exists")
        void shouldRetrieveWhenExists() {
            callGetStartCaseTrigger(CASE_TYPE, CREATE)
                .when()
                .get("/case-types/{caseTypeId}/event-triggers/{triggerId}")

                .then()
                .log().ifError()
                .statusCode(200)
                .assertThat()

                // Metadata
                .body("event_id", equalTo(CREATE))
                .body("token", is(not(isEmptyString())))

                // Flexible data
                .rootPath("case_details")
                .body("id", is(nullValue()))
                .body("jurisdiction", equalTo(JURISDICTION))
                .body("state", is(nullValue()))
                .body("case_type_id", equalTo(CASE_TYPE))
                .body("created_date", is(nullValue()))
                .body("last_modified", is(nullValue()))
                .body("security_classification", is(nullValue()))
                .body("case_data", hasToString(EMPTY_OBJECT_STRING))
                .body("data_classification", hasToString(EMPTY_OBJECT_STRING))
                .body("after_submit_response_callback", is(nullValue()))
                .body("callback_response_status_code", is(nullValue()))
                .body("callback_response_status", is(nullValue()))
                .body("delete_draft_response_status_code", is(nullValue()))
                .body("delete_draft_response_status", is(nullValue()))
                .body("security_classifications", hasToString(EMPTY_OBJECT_STRING))

                .rootPath("_links")
                .body("self.href", equalTo(String.format("%s/case-types/%s/event-triggers/%s{?ignore-warning}", aat.getTestUrl(), CASE_TYPE, CREATE)))
            ;
        }

        @Test
        @DisplayName("should get 404 when case type does not exist")
        void should404WhenCaseTypeDoesNotExist() {
            callGetStartCaseTrigger(INVALID_CASE_TYPE_ID, CREATE)
                .when()
                .get("/case-types/{caseTypeId}/event-triggers/{triggerId}")

                .then()
                .statusCode(404);
        }

        @Test
        @DisplayName("should get 404 when event trigger does not exist")
        void should404WhenEventTriggerDoesNotExist() {
            callGetStartCaseTrigger(CASE_TYPE, INVALID_EVENT_TRIGGER_ID)
                .when()
                .get("/case-types/{caseTypeId}/event-triggers/{triggerId}")

                .then()
                .statusCode(404);
        }
    }

    @Nested
    @DisplayName("Start event trigger")
    class StartEventTrigger {
        private static final String INVALID_CASE_REFERENCE = "1234123412341234";
        private static final String NOT_FOUND_CASE_REFERENCE = "1234123412341238";

        @Test
        @DisplayName("should retrieve trigger when the case and event exists")
        void shouldRetrieveWhenExists() {
            // Prepare new case in known state
            final Long caseReference = create()
                .as(asAutoTestCaseworker())
                .withData(FullCase.build())
                .submitAndGetReference();

            callGetStartEventTrigger(String.valueOf(caseReference), START_PROGRESS)
                .when()
                .get("/cases/{caseId}/event-triggers/{triggerId}")

                .then()
                .log().ifError()
                .statusCode(200)
                .assertThat()

                // Metadata
                .body("event_id", equalTo(START_PROGRESS))
                .body("token", is(not(isEmptyString())))

                // Flexible data
                .rootPath("case_details")
                .body("id", equalTo(caseReference))
                .body("jurisdiction", equalTo(JURISDICTION))
                .body("state", equalTo(TODO))
                .body("case_type_id", equalTo(CASE_TYPE))
                .body("created_date", is(not(nullValue())))
                .body("last_modified", is(not(nullValue())))
                .body("security_classification", equalTo("PUBLIC"))
                .body("data_classification", is(not(nullValue())))
                .body("after_submit_response_callback", is(nullValue()))
                .body("callback_response_status_code", is(nullValue()))
                .body("callback_response_status", is(nullValue()))
                .body("delete_draft_response_status_code", is(nullValue()))
                .body("delete_draft_response_status", is(nullValue()))
                .body("security_classifications", is(not(nullValue())))

                .rootPath("case_details.case_data")
                .body("TextField", equalTo(AATCaseBuilder.TEXT))
                .body("NumberField", equalTo(AATCaseBuilder.NUMBER))
                .body("YesOrNoField", equalTo(AATCaseBuilder.YES_OR_NO))
                .body("PhoneUKField", equalTo(AATCaseBuilder.PHONE_UK))
                .body("EmailField", equalTo(AATCaseBuilder.EMAIL))
                .body("MoneyGBPField", equalTo(AATCaseBuilder.MONEY_GBP))
                .body("DateField", equalTo(AATCaseBuilder.DATE))
                .body("DateTimeField", equalTo(AATCaseBuilder.DATE_TIME))
                .body("TextAreaField", equalTo(AATCaseBuilder.TEXT_AREA))
                .body("FixedListField", equalTo(AATCaseBuilder.FIXED_LIST))
                .body("MultiSelectListField[0]", equalTo(AATCaseBuilder.MULTI_SELECT_LIST[0]))
                .body("MultiSelectListField[1]", equalTo(AATCaseBuilder.MULTI_SELECT_LIST[1]))
                .body("CollectionField[0].value", equalTo(AATCaseBuilder.COLLECTION_VALUE_1))
                .body("CollectionField[1].value", equalTo(AATCaseBuilder.COLLECTION_VALUE_2))
                .body("ComplexField.ComplexTextField", equalTo(AATCaseBuilder.COMPLEX_TEXT))
                .body("ComplexField.ComplexFixedListField", equalTo(AATCaseBuilder.COMPLEX_FIXED_LIST))
                .body("AddressUKField.AddressLine1", equalTo(AATCaseBuilder.ADDRESS_LINE_1))
                .body("AddressUKField.AddressLine2", equalTo(AATCaseBuilder.ADDRESS_LINE_2))
                .body("AddressUKField.AddressLine3", equalTo(AATCaseBuilder.ADDRESS_LINE_3))
                .body("AddressUKField.PostTown", equalTo(AATCaseBuilder.ADDRESS_POST_TOWN))
                .body("AddressUKField.County", equalTo(AATCaseBuilder.ADDRESS_COUNTY))
                .body("AddressUKField.PostCode", equalTo(AATCaseBuilder.ADDRESS_POSTCODE))
                .body("AddressUKField.Country", equalTo(AATCaseBuilder.ADDRESS_COUNTRY))

                .rootPath("_links")
                .body("self.href", equalTo(String.format("%s/cases/%s/event-triggers/%s{?ignore-warning}", aat.getTestUrl(), caseReference, START_PROGRESS)));
        }

        @Test
        @DisplayName("should get 400 when case reference invalid")
        void should400WhenCaseReferenceInvalid() {
            callGetStartEventTrigger(INVALID_CASE_REFERENCE, CREATE)
                .when()
                .get("/cases/{caseId}/event-triggers/{triggerId}")

                .then()
                .statusCode(400);
        }

        @Test
        @DisplayName("should get 404 when case does not exist")
        void should404WhenCaseDoesNotExist() {
            callGetStartEventTrigger(NOT_FOUND_CASE_REFERENCE, CREATE)
                .when()
                .get("/cases/{caseId}/event-triggers/{triggerId}")

                .then()
                .statusCode(404);
        }

        @Test
        @DisplayName("should get 404 when event trigger does not exist")
        void should404WhenEventTriggerDoesNotExist() {
            // Prepare new case in known state
            final Long caseReference = create()
                .as(asAutoTestCaseworker())
                .withData(FullCase.build())
                .submitAndGetReference();

            callGetStartEventTrigger(String.valueOf(caseReference), INVALID_EVENT_TRIGGER_ID)
                .when()
                .get("/cases/{caseId}/event-triggers/{triggerId}")

                .then()
                .statusCode(404);
        }
    }

    private RequestSpecification callGetStartCaseTrigger(String caseTypeId, String eventTriggerId) {
        return asAutoTestCaseworker(FALSE)
            .get()
            .given()
            .pathParam("caseTypeId", caseTypeId)
            .pathParam("triggerId", eventTriggerId)
            .accept(V2.MediaType.START_CASE_TRIGGER)
            .header("experimental", "true");
    }

    private RequestSpecification callGetStartEventTrigger(String caseId, String eventTriggerId) {
        return asAutoTestCaseworker(FALSE)
            .get()
            .given()
            .pathParam("caseId", caseId)
            .pathParam("triggerId", eventTriggerId)
            .accept(V2.MediaType.START_EVENT_TRIGGER)
            .header("experimental", "true");
    }
}
