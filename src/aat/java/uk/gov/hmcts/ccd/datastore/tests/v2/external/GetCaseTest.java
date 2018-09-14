package uk.gov.hmcts.ccd.datastore.tests.v2.external;

import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.datastore.tests.AATHelper;
import uk.gov.hmcts.ccd.datastore.tests.BaseTest;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder.FullCase;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.Event;
import uk.gov.hmcts.ccd.v2.V2;

import static java.lang.Boolean.FALSE;
import static org.hamcrest.Matchers.equalTo;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.CASE_TYPE;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.JURISDICTION;

@DisplayName("Get case by reference")
class GetCaseTest extends BaseTest {
    private static final String NOT_FOUND_CASE_REFERENCE = "1234123412341238";
    private static final String INVALID_CASE_REFERENCE = "1234123412341234";

    protected GetCaseTest(AATHelper aat) {
        super(aat);
    }

    @Test
    @DisplayName("should retrieve case when the case reference exists")
    void shouldRetrieveWhenExists() {
        // Prepare new case in known state
        final Long caseReference = Event.create()
                                        .as(asAutoTestCaseworker())
                                        .withData(FullCase.build())
                                        .submitAndGetReference();

        callGetCase(caseReference.toString())
            .when()
            .get("/cases/{caseReference}")

            .then()
            .log().ifError()
            .statusCode(200)
            .assertThat()

            // Metadata
            .body("jurisdiction", equalTo(JURISDICTION))
            .body("case_type", equalTo(CASE_TYPE))
            .body("id", equalTo(caseReference.toString()))
            .body("state", equalTo(AATCaseType.State.TODO))
            .body("security_classification", equalTo("PUBLIC"))

            // Flexible data
            .rootPath("data")
            .body("TextField", equalTo(FullCase.TEXT))
            .body("NumberField", equalTo(FullCase.NUMBER))
            .body("YesOrNoField", equalTo(FullCase.YES_OR_NO))
            .body("PhoneUKField", equalTo(FullCase.PHONE_UK))
            .body("EmailField", equalTo(FullCase.EMAIL))
            .body("MoneyGBPField", equalTo(FullCase.MONEY_GBP))
            .body("DateField", equalTo(FullCase.DATE))
            .body("DateTimeField", equalTo(FullCase.DATE_TIME))
            .body("TextAreaField", equalTo(FullCase.TEXT_AREA))
            .body("FixedListField", equalTo(FullCase.FIXED_LIST))
            .body("MultiSelectListField[0]", equalTo(FullCase.MULTI_SELECT_LIST[0]))
            .body("MultiSelectListField[1]", equalTo(FullCase.MULTI_SELECT_LIST[1]))
            .body("CollectionField[0].value", equalTo(FullCase.COLLECTION_VALUE_1))
            .body("CollectionField[1].value", equalTo(FullCase.COLLECTION_VALUE_2))
            .body("ComplexField.ComplexTextField", equalTo(FullCase.COMPLEX_TEXT))
            .body("ComplexField.ComplexFixedListField", equalTo(FullCase.COMPLEX_FIXED_LIST))
            .body("AddressUKField.AddressLine1", equalTo(FullCase.ADDRESS_LINE_1))
            .body("AddressUKField.AddressLine2", equalTo(FullCase.ADDRESS_LINE_2))
            .body("AddressUKField.AddressLine3", equalTo(FullCase.ADDRESS_LINE_3))
            .body("AddressUKField.PostTown", equalTo(FullCase.ADDRESS_POST_TOWN))
            .body("AddressUKField.County", equalTo(FullCase.ADDRESS_COUNTY))
            .body("AddressUKField.PostCode", equalTo(FullCase.ADDRESS_POSTCODE))
            .body("AddressUKField.Country", equalTo(FullCase.ADDRESS_COUNTRY))

            .rootPath("_links")
            .body("self.href", equalTo(String.format("%s/cases/%s", aat.getTestUrl(), caseReference)))
        ;
    }

    @Test
    @DisplayName("should get 404 when case reference does NOT exist")
    void should404WhenNotExists() {
        callGetCase(NOT_FOUND_CASE_REFERENCE)
            .when()
            .get("/cases/{caseReference}")

            .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("should get 400 when case reference invalid")
    void should400WhenReferenceInvalid() {
        callGetCase(INVALID_CASE_REFERENCE)
            .when()
            .get("/cases/{caseReference}")

            .then()
            .statusCode(400);
    }

    private RequestSpecification callGetCase(String caseReference) {
        return asAutoTestCaseworker(FALSE)
            .get()
            .given()
            .pathParam("caseReference", caseReference)
            .accept(V2.MediaType.CASE)
            .header("experimental", "true");
    }
}
