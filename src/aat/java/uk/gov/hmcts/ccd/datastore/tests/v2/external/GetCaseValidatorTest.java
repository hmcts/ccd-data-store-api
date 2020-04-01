package uk.gov.hmcts.ccd.datastore.tests.v2.external;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.datastore.tests.AATHelper;
import uk.gov.hmcts.ccd.datastore.tests.BaseTest;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder.CaseWithInvalidData;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder.FullCase;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.CaseData;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.Event;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.v2.V2;

import java.util.function.Supplier;

import static java.lang.Boolean.FALSE;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder.ADDRESS_COUNTRY;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder.ADDRESS_COUNTY;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder.ADDRESS_LINE_1;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder.ADDRESS_LINE_2;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder.ADDRESS_LINE_3;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder.ADDRESS_POSTCODE;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder.ADDRESS_POST_TOWN;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder.COLLECTION_VALUE_1;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder.COLLECTION_VALUE_2;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder.COMPLEX_FIXED_LIST;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder.COMPLEX_TEXT;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder.DATE;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder.DATE_TIME;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder.EMAIL;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder.FIXED_LIST;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder.MONEY_GBP;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder.MULTI_SELECT_LIST;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder.NUMBER;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder.PHONE_UK;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder.TEXT;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder.TEXT_AREA;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder.YES_OR_NO;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.CASE_TYPE;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.Event.CREATE;

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
        @DisplayName("should validate when the case type and event exists")
        void shouldRetrieveWhenExists() throws JsonProcessingException {
            callCaseDataValidate(CASE_TYPE, getBody(CREATE))
                .when()
                .post("/case-types/{caseTypeId}/validate")

                .then()
                .log().ifError()
                .statusCode(200)
                .assertThat()

                // Metadata
                .rootPath("data")
                .body("MoneyGBPField", equalTo(MONEY_GBP))
                .body("FixedListField", equalTo(FIXED_LIST))
                .body("AddressUKField.AddressLine1", equalTo(ADDRESS_LINE_1))
                .body("AddressUKField.AddressLine2", equalTo(ADDRESS_LINE_2))
                .body("AddressUKField.AddressLine3", equalTo(ADDRESS_LINE_3))
                .body("AddressUKField.PostTown", equalTo(ADDRESS_POST_TOWN))
                .body("AddressUKField.County", equalTo(ADDRESS_COUNTY))
                .body("AddressUKField.PostCode", equalTo(ADDRESS_POSTCODE))
                .body("AddressUKField.Country", equalTo(ADDRESS_COUNTRY))
                .body("ComplexField.ComplexTextField", equalTo(COMPLEX_TEXT))
                .body("ComplexField.ComplexFixedListField", equalTo(COMPLEX_FIXED_LIST))
                .body("DateTimeField", equalTo(DATE_TIME))
                .body("PhoneUKField", equalTo(PHONE_UK))
                .body("NumberField", equalTo(NUMBER))
                .body("MultiSelectListField", contains(MULTI_SELECT_LIST))
                .body("YesOrNoField", equalTo(YES_OR_NO))
                .body("EmailField", equalTo(EMAIL))
                .body("TextField", equalTo(TEXT))
                .body("DateField", equalTo(DATE))
                .body("TextAreaField", equalTo(TEXT_AREA))
                .body("CollectionField[0].value", equalTo(COLLECTION_VALUE_1))
                .body("CollectionField[1].value", equalTo(COLLECTION_VALUE_2))

                .rootPath("_links")
                .body("self.href", equalTo(String.format("%s/case-types/%s/validate{?pageId}", aat.getTestUrl(), CASE_TYPE)));
        }

        @Test
        @DisplayName("should get 404 when case type does not exist")
        void should404WhenCaseTypeDoesNotExist() throws JsonProcessingException {
            callCaseDataValidate(INVALID_CASE_TYPE_ID, getBody(CREATE))
                .when()
                .post("/case-types/{caseTypeId}/validate")

                .then()
                .statusCode(404);
        }

        @Test
        @DisplayName("should get 404 when event not provided")
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

        @Test
        @DisplayName("should get 422 when event trigger does not exist")
        void should422WhenCaseDataInvalid() throws JsonProcessingException {
            callCaseDataValidate(CASE_TYPE, getBody(CREATE, CaseWithInvalidData::build))
                .when()
                .post("/case-types/{caseTypeId}/validate")

                .then()
                .statusCode(422);
        }

        private RequestSpecification callCaseDataValidate(String caseTypeId, Supplier<String> supplier) throws JsonProcessingException {
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
