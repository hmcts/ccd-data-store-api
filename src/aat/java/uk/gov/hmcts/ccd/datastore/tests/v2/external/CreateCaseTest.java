package uk.gov.hmcts.ccd.datastore.tests.v2.external;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.datastore.tests.AATHelper;
import uk.gov.hmcts.ccd.datastore.tests.BaseTest;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder.FullCase;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.Event;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.v2.V2;

import java.util.function.Supplier;

import static java.lang.Boolean.FALSE;
import static org.hamcrest.Matchers.equalTo;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.CASE_TYPE;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.Event.CREATE;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.JURISDICTION;

@DisplayName("Create case")
class CreateCaseTest extends BaseTest {
    private static final String NOT_FOUND_CASE_REFERENCE = "1234123412341238";
    private static final String INVALID_CASE_REFERENCE = "1234123412341234";
    private static final String INVALID_EVENT_TRIGGER_ID = "invalidEvent";
    private static final String EVENT_TOKEN = "someToken";

    protected CreateCaseTest(AATHelper aat) {
        super(aat);
    }

    @Test
    @DisplayName("should create case")
    void shouldCreateCase() {
        String eventToken = aat.getCcdHelper().generateTokenCreateCase(asAutoTestCaseworker(), JURISDICTION, CASE_TYPE, CREATE);

        callCreateCase(CASE_TYPE, getBody(CREATE, eventToken, FullCase::build))
            .when()
            .post("/case-types/{caseTypeId}/cases")

            .then()
            .log().ifError()
            .statusCode(201)
            .assertThat()

            // Metadata
            .body("jurisdiction", equalTo(JURISDICTION))
            .body("case_type", equalTo(CASE_TYPE))
            .body("state", equalTo(AATCaseType.State.TODO))
            .body("security_classification", equalTo("PUBLIC"))

            // Flexible data
            .rootPath("data")
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
            .body("self.href", equalTo(String.format("%s/case-types/%s/cases{?ignore-warning}", aat.getTestUrl(), CASE_TYPE)))
        ;
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
        return () -> JacksonUtils.convertValueJsonNode(caseDataContent).toString();
    }

}
