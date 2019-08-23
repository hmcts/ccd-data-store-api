package uk.gov.hmcts.ccd.datastore.tests.functional;

import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.hmcts.ccd.datastore.tests.AATHelper;
import uk.gov.hmcts.ccd.datastore.tests.BaseTest;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType;
import uk.gov.hmcts.ccd.datastore.tests.helper.CaseTestDataLoaderExtension;

import java.util.ArrayList;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.CASE_TYPE;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.JURISDICTION;

@ExtendWith(CaseTestDataLoaderExtension.class)
@DisplayName("Search cases")
class SearchCaseTest extends BaseTest {

    protected SearchCaseTest(AATHelper aat) {
        super(aat);
    }

    @Test
    @DisplayName("should succeed with 200")
    void shouldReturn200() {
        asAutoTestCaseworker()
            .get()
            .given()
            .pathParam("jurisdiction", JURISDICTION)
            .pathParam("caseType", CASE_TYPE)
            .contentType(ContentType.JSON)

            .when()
            .get("/caseworkers/{user}/jurisdictions/{jurisdiction}/case-types/{caseType}/cases")

            .then()
            .statusCode(200);
    }

    @Test
    @DisplayName("should retrieve when a case exists if caseworker has 'CRUD' access on CaseType")
    void shouldRetrieveCaseWhenExistsWithFullAccessForCaseType() {
        // Prepare new case in known state
        final Long caseReference = createFullCase("AAT_AUTH_15");

        asAutoTestCaseworker()
            .get()
            .given()
            .pathParam("jurisdiction", JURISDICTION)
            .pathParam("caseType", "AAT_AUTH_15")
            .contentType(ContentType.JSON)

            .when()
            .get("/caseworkers/{user}/jurisdictions/{jurisdiction}/case-types/{caseType}/cases")

            .then()
            .statusCode(200).assertThat().body("isEmpty()", Matchers.is(false));
    }

    @Test
    @DisplayName("should retrieve when a case exists if caseworker has 'CR' access on CaseType")
    void shouldRetrieveCaseWhenExistsWithCRAccessForCaseType() {
        // Prepare new case in known state
        final Long caseReference = createFullCase("AAT_AUTH_3");

        asAutoTestCaseworker()
            .get()
            .given()
            .pathParam("jurisdiction", JURISDICTION)
            .pathParam("caseType", "AAT_AUTH_3")
            .contentType(ContentType.JSON)

            .when()
            .get("/caseworkers/{user}/jurisdictions/{jurisdiction}/case-types/{caseType}/cases")

            .then()
            .statusCode(200).assertThat().body("isEmpty()", Matchers.is(false));
    }

    @Test
    @DisplayName("should retrieve when a case exists if caseworker has 'R' access on CaseType")
    void shouldRetrieveCaseWhenExistsWithRAccessForCaseType() {
        // Prepare new case in known state
        final Long caseReference = createFullCase("AAT_AUTH_2");

        asAutoTestCaseworker()
            .get()
            .given()
            .pathParam("jurisdiction", JURISDICTION)
            .pathParam("caseType", "AAT_AUTH_2")
            .contentType(ContentType.JSON)

            .when()
            .get("/caseworkers/{user}/jurisdictions/{jurisdiction}/case-types/{caseType}/cases")

            .then()
            .statusCode(200).assertThat().body("isEmpty()", Matchers.is(false));
    }

    @Test
    @DisplayName("should retrieve when a case exists if caseworker has 'RU' access on CaseType")
    void shouldRetrieveCaseWhenExistsWithRUAccessForCaseType() {
        // Prepare new case in known state
        final Long caseReference = createFullCase("AAT_AUTH_6");

        asAutoTestCaseworker()
            .get()
            .given()
            .pathParam("jurisdiction", JURISDICTION)
            .pathParam("caseType", "AAT_AUTH_6")
            .contentType(ContentType.JSON)

            .when()
            .get("/caseworkers/{user}/jurisdictions/{jurisdiction}/case-types/{caseType}/cases")

            .then()
            .statusCode(200).assertThat().body("isEmpty()", Matchers.is(false));
    }

    @Test
    @DisplayName("should retrieve empty result when a case exists if caseworker has 'CU' access on CaseType")
    void shouldRetrieveEmptyResultWhenExistsWithCRAccessForCaseType() {
        // Prepare new case in known state
        final Long caseReference = createFullCase("AAT_AUTH_5");

        asAutoTestCaseworker()
            .get()
            .given()
            .pathParam("jurisdiction", JURISDICTION)
            .pathParam("caseType", "AAT_AUTH_5")
            .contentType(ContentType.JSON)

            .when()
            .get("/caseworkers/{user}/jurisdictions/{jurisdiction}/case-types/{caseType}/cases")

            .then()
            .statusCode(200).assertThat().body("isEmpty()", Matchers.is(true));
    }

    @Test
    @DisplayName("should retrieve empty result when a case exists if caseworker has 'U' access on CaseType")
    void shouldRetrieveEmptyResultWhenExistsWithUAccessForCaseType() {
        // Prepare new case in known state
        final Long caseReference = createFullCase("AAT_AUTH_4");

        asAutoTestCaseworker()
            .get()
            .given()
            .pathParam("jurisdiction", JURISDICTION)
            .pathParam("caseType", "AAT_AUTH_4")
            .contentType(ContentType.JSON)

            .when()
            .get("/caseworkers/{user}/jurisdictions/{jurisdiction}/case-types/{caseType}/cases")

            .then()
            .statusCode(200).assertThat().body("isEmpty()", Matchers.is(true));
    }

    @Test
    @DisplayName("should retrieve empty result when a case exists if caseworker has 'D' access on CaseType")
    void shouldRetrieveEmptyResultWhenExistsWithDAccessForCaseType() {
        // Prepare new case in known state
        final Long caseReference = createFullCase("AAT_AUTH_8");

        asAutoTestCaseworker()
            .get()
            .given()
            .pathParam("jurisdiction", JURISDICTION)
            .pathParam("caseType", "AAT_AUTH_8")
            .contentType(ContentType.JSON)

            .when()
            .get("/caseworkers/{user}/jurisdictions/{jurisdiction}/case-types/{caseType}/cases")

            .then()
            .statusCode(200).assertThat().body("isEmpty()", Matchers.is(true));
    }

    @Test
    @DisplayName("should retrieve empty result when a case exists if caseworker has 'C' access on CaseType")
    void shouldRetrieveEmptyResultWhenExistsWithCAccessForCaseType() {
        // Prepare new case in known state
        final Long caseReference = createFullCase("AAT_AUTH_1");

        asAutoTestCaseworker()
            .get()
            .given()
            .pathParam("jurisdiction", JURISDICTION)
            .pathParam("caseType", "AAT_AUTH_1")
            .contentType(ContentType.JSON)

            .when()
            .get("/caseworkers/{user}/jurisdictions/{jurisdiction}/case-types/{caseType}/cases")

            .then()
            .statusCode(200).assertThat().body("isEmpty()", Matchers.is(true));
    }

    @Test
    @DisplayName("should read only cases he has created if Solicitor role has CRUD access on a case Type")
    void shouldReadCasesHasCreatedIfSolsRoleHasCrudAccessOnCaseType() {
        // Prepare new case in known state
        final Long caseReference  = createFullCaseSolicitor("AAT_AUTH_15");
        final Long caseReference1 = createFullCaseSolicitor1("AAT_AUTH_15");

         asPrivateCaseworkerSolicitor(true)
            .get()
            .given()
            .pathParam("jurisdiction", JURISDICTION)
            .pathParam("caseType", "AAT_AUTH_15")
            .contentType(ContentType.JSON)

            .when()
            .get("/caseworkers/{user}/jurisdictions/{jurisdiction}/case-types/{caseType}/cases")

            .then()
           // .statusCode(200).assertThat().body("isEmpty()", Matchers.is(false)).extract().path("id")
            .statusCode(200).assertThat()
            .body("id", hasItem( caseReference ),true);
    }

    @Test
    @DisplayName("should not read cases he has not created if Solicitor role has CRUD access on a case Type")
    void shouldNotReadCasesHasNotCreatedIfSolsRoleHasCrudAccessOnCaseType() {
        // Prepare new case in known state
        final Long caseReference  = createFullCaseSolicitor("AAT_AUTH_15");

        asPrivateCaseworkerSolicitor2(true)
            .get()
            .given()
            .pathParam("jurisdiction", JURISDICTION)
            .pathParam("caseType", "AAT_AUTH_15")
            .contentType(ContentType.JSON)

            .when()
            .get("/caseworkers/{user}/jurisdictions/{jurisdiction}/case-types/{caseType}/cases")

            .then()
            .statusCode(200).assertThat().body("isEmpty()", Matchers.is(true));
    }

    @Test
    @DisplayName("should read only cases he has created if LocalAuthority role has CR access on a case Type")
    void shouldReadCasesHasCreatedIfLaRoleHasCrAccessOnCaseType() {
        // Prepare new case in known state
        final Long caseReference  = createFullCaseLocalAuthority("AAT_AUTH_3");
        final Long caseReference1 = createFullCaseLocalAuthority1("AAT_AUTH_3");

        asPrivateCaseworkerLocalAuthority(true)
            .get()
            .given()
            .pathParam("jurisdiction", JURISDICTION)
            .pathParam("caseType", "AAT_AUTH_3")
            .contentType(ContentType.JSON)

            .when()
            .get("/caseworkers/{user}/jurisdictions/{jurisdiction}/case-types/{caseType}/cases")

            .then()
            .statusCode(200).assertThat()
            .body("id", hasItem( caseReference ),true);
    }

    @Test
    @DisplayName("should not read cases he has not created if LocalAuthority role has CR access on a case Type")
    void shouldNotReadCasesHasNotCreatedIfLaRoleHasCrAccessOnCaseType() {
        // Prepare new case in known state
        final Long caseReference  = createFullCaseLocalAuthority("AAT_AUTH_3");

        asPrivateCaseworkerLocalAuthority2(true)
            .get()
            .given()
            .pathParam("jurisdiction", JURISDICTION)
            .pathParam("caseType", "AAT_AUTH_3")
            .contentType(ContentType.JSON)

            .when()
            .get("/caseworkers/{user}/jurisdictions/{jurisdiction}/case-types/{caseType}/cases")

            .then()
            .statusCode(200).assertThat().body("isEmpty()", Matchers.is(true));
    }

    @Test
    @DisplayName("should not read cases he has created if PanelMember role has U access on a case Type")
    void shouldNotReadCasesIfPmRoleHasUAccessOnCaseType() {
        // Prepare new case in known state
        final Long caseReference  = createFullCasePanelMember("AAT_AUTH_15");

        asPrivateCaseworkerPanelMember1(true)
            .get()
            .given()
            .pathParam("jurisdiction", JURISDICTION)
            .pathParam("caseType", "AAT_AUTH_4")
            .contentType(ContentType.JSON)

            .when()
            .get("/caseworkers/{user}/jurisdictions/{jurisdiction}/case-types/{caseType}/cases")

            .then()
            .statusCode(200).assertThat().body("isEmpty()", Matchers.is(true));
    }


    @Test
    @DisplayName("should not read cases he has created if Citizen role has RU access on a case Type")
    void shouldNotReadCasesIfCtznRoleHasRuAccessOnCaseType() {
        // Prepare new case in known state
        final Long caseReference  = createFullCaseCitizen("AAT_AUTH_15");

        asPrivateCaseworkerCitizen(true)
            .get()
            .given()
            .pathParam("jurisdiction", JURISDICTION)
            .pathParam("caseType", "AAT_AUTH_6")
            .contentType(ContentType.JSON)

            .when()
            .get("/caseworkers/{user}/jurisdictions/{jurisdiction}/case-types/{caseType}/cases")

            .then()
            .statusCode(200).assertThat().body("isEmpty()", Matchers.is(true));
    }


    @Test
    @DisplayName("should not read cases if AnySingleAccess role has D access on a case Type")
    void shouldNotReadCasesIfSingleAccessRoleHasDAccessOnCaseType() {
        // Prepare new case in known state
        final Long caseReference  = createFullCasePanelMember("AAT_AUTH_15");

        asPrivateCaseworkerPanelMember(true)
            .get()
            .given()
            .pathParam("jurisdiction", JURISDICTION)
            .pathParam("caseType", "AAT_AUTH_8")
            .contentType(ContentType.JSON)

            .when()
            .get("/caseworkers/{user}/jurisdictions/{jurisdiction}/case-types/{caseType}/cases")

            .then()
            .statusCode(200).assertThat().body("isEmpty()", Matchers.is(true));
    }



    /*
       Method to create a Full Case with caseType param.
       This method uses 'privatecaseworker' as default user role
     */
    private Long createFullCase(String caseType) {

        return AATCaseType.Event.create(caseType)
            .as(asPrivateCaseworker(true))
            .withData(AATCaseBuilder.FullCase.build())
            .submitAndGetReference();
    }

    private Long createFullCaseSolicitor(String caseType) {

        return AATCaseType.Event.create(caseType)
            .as(asPrivateCaseworkerSolicitor(true))
            .withData(AATCaseBuilder.FullCase.build())
            .submitAndGetReference();
    }

    private Long createFullCaseSolicitor1(String caseType) {

        return AATCaseType.Event.create(caseType)
            .as(asPrivateCaseworkerSolicitor1(true))
            .withData(AATCaseBuilder.FullCase.build())
            .submitAndGetReference();
    }

    private Long createFullCaseLocalAuthority(String caseType) {

        return AATCaseType.Event.create(caseType)
            .as(asPrivateCaseworkerLocalAuthority(true))
            .withData(AATCaseBuilder.FullCase.build())
            .submitAndGetReference();
    }

    private Long createFullCaseLocalAuthority1(String caseType) {

        return AATCaseType.Event.create(caseType)
            .as(asPrivateCaseworkerLocalAuthority1(true))
            .withData(AATCaseBuilder.FullCase.build())
            .submitAndGetReference();
    }

    private Long createFullCasePanelMember(String caseType) {

        return AATCaseType.Event.create(caseType)
            .as(asPrivateCaseworkerPanelMember(true))
            .withData(AATCaseBuilder.FullCase.build())
            .submitAndGetReference();
    }

    private Long createFullCaseCitizen(String caseType) {

        return AATCaseType.Event.create(caseType)
            .as(asPrivateCaseworkerCitizen(true))
            .withData(AATCaseBuilder.FullCase.build())
            .submitAndGetReference();
    }

//    private Long createFullCaseGeneric(String caseType, String caseRole) {
//
//        return AATCaseType.Event.create(caseType)
//            .as(asPrivateCaseworkerGeneric(true,caseRole))
//            .withData(AATCaseBuilder.FullCase.build())
//            .submitAndGetReference();
//    }

}
