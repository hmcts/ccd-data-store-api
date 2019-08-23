package uk.gov.hmcts.ccd.datastore.tests;

import java.util.function.Supplier;

import static java.lang.Boolean.TRUE;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.hmcts.ccd.datastore.tests.helper.idam.AuthenticatedUser;

@ExtendWith(AATExtension.class)
public abstract class BaseTest {
    protected final AATHelper aat;

    protected BaseTest(AATHelper aat) {
        this.aat = aat;
        RestAssured.baseURI = aat.getTestUrl();
        RestAssured.useRelaxedHTTPSValidation();
    }

    protected Supplier<RequestSpecification> asAutoTestCaseworker() {
        return asAutoTestCaseworker(TRUE);
    }

    protected Supplier<RequestSpecification> asAutoTestCaseworker(boolean withUserParam) {
        return authenticateAndCreateRequestSpecification(aat.getCaseworkerAutoTestEmail(), aat.getCaseworkerAutoTestPassword(), withUserParam);
    }

    protected Supplier<RequestSpecification> asPrivateCaseworker(boolean withUserParam) {
        return authenticateAndCreateRequestSpecification(aat.getPrivateCaseworkerEmail(), aat.getPrivateCaseworkerPassword(), withUserParam);
    }

    protected Supplier<RequestSpecification> asRestrictedCaseworker(boolean withUserParam) {
        return authenticateAndCreateRequestSpecification(aat.getRestrictedCaseworkerEmail(), aat.getRestrictedCaseworkerPassword(), withUserParam);
    }

    protected Supplier<RequestSpecification> asPrivateCaseworkerSolicitor(boolean withUserParam) {
        return authenticateAndCreateRequestSpecification(aat.getPrivateCaseworkerSolicitorEmail(), aat.getPrivateCaseworkerSolicitorPassword(), withUserParam);
    }

    protected Supplier<RequestSpecification> asPrivateCaseworkerSolicitor1(boolean withUserParam) {
        return authenticateAndCreateRequestSpecification(aat.getPrivateCaseworkerSolicitor1Email(), aat.getPrivateCaseworkerSolicitor1Password(), withUserParam);
    }

    protected Supplier<RequestSpecification> asPrivateCaseworkerSolicitor2(boolean withUserParam) {
        return authenticateAndCreateRequestSpecification(aat.getPrivateCaseworkerSolicitor2Email(), aat.getPrivateCaseworkerSolicitor2Password(), withUserParam);
    }

    protected Supplier<RequestSpecification> asPrivateCaseworkerLocalAuthority(boolean withUserParam) {
        return authenticateAndCreateRequestSpecification(aat.getPrivateCaseworkerLocalAuthorityEmail(), aat.getPrivateCaseworkerLocalAuthorityPassword(), withUserParam);
    }

    protected Supplier<RequestSpecification> asPrivateCaseworkerLocalAuthority1(boolean withUserParam) {
        return authenticateAndCreateRequestSpecification(aat.getPrivateCaseworkerLocalAuthority1Email(), aat.getPrivateCaseworkerLocalAuthority1Password(), withUserParam);
    }

    protected Supplier<RequestSpecification> asPrivateCaseworkerLocalAuthority2(boolean withUserParam) {
        return authenticateAndCreateRequestSpecification(aat.getPrivateCaseworkerLocalAuthority2Email(), aat.getPrivateCaseworkerLocalAuthority2Password(), withUserParam);
    }

    protected Supplier<RequestSpecification> asPrivateCaseworkerPanelMember(boolean withUserParam) {
        return authenticateAndCreateRequestSpecification(aat.getPrivateCaseworkerPanelMemberEmail(), aat.getPrivateCaseworkerPanelMemberPassword(), withUserParam);
    }

    protected Supplier<RequestSpecification> asPrivateCaseworkerPanelMember1(boolean withUserParam) {
        return authenticateAndCreateRequestSpecification(aat.getPrivateCaseworkerPanelMember1Email(), aat.getPrivateCaseworkerPanelMember1Password(), withUserParam);
    }

    protected Supplier<RequestSpecification> asPrivateCaseworkerCitizen(boolean withUserParam) {
        return authenticateAndCreateRequestSpecification(aat.getPrivateCaseworkerCitizenEmail(), aat.getPrivateCaseworkerCitizenPassword(), withUserParam);
    }

    protected Supplier<RequestSpecification> asPrivateCrossCaseTypeCaseworker(boolean withUserParam) {
        return authenticateAndCreateRequestSpecification(aat.getPrivateCrossCaseTypeCaseworkerEmail(), aat.getPrivateCrossCaseTypeCaseworkerPassword(),
                                                         withUserParam);
    }

    protected Supplier<RequestSpecification> asPrivateCrossCaseTypeSolicitor(boolean withUserParam) {
        return authenticateAndCreateRequestSpecification(aat.getPrivateCrossCaseTypeSolicitorEmail(), aat.getPrivateCrossCaseTypeSolicitorPassword(),
                                                         withUserParam);
    }

    protected Supplier<RequestSpecification> asRestrictedCrossCaseTypeCaseworker(boolean withUserParam) {
        return authenticateAndCreateRequestSpecification(aat.getRestrictedCrossCaseTypeCaseworkerEmail(), aat.getRestrictedCrossCaseTypeCaseworkerPassword(),
                                                         withUserParam);
    }

    //    protected Supplier<RequestSpecification> asPrivateCaseworkerGeneric(boolean withUserParam, String caseRole) {
    //
    //        switch(caseRole) {
    //            case "asPrivateCaseworkerSolicitor1":
    //                return authenticateAndCreateRequestSpecification("auto.test.cnp+solc1@gmail.com", aat.getPrivateCaseworkerSolicitor1Password(), withUserParam);
    //                break;
    //            case "asPrivateCaseworkerLocalAuthority":
    //                // code block
    //                break;
    //            default:
    //                // code block
    //        }
    ////        return authenticateAndCreateRequestSpecification(aat.getPrivateCaseworkerSolicitor1Email(), aat.getPrivateCaseworkerSolicitor1Password(), withUserParam);
    //        return authenticateAndCreateRequestSpecification("auto.test.cnp+solc1@gmail.com", aat.getPrivateCaseworkerSolicitorPassword(), withUserParam);
    //    }

    private Supplier<RequestSpecification> authenticateAndCreateRequestSpecification(String username, String password, Boolean withUserParam) {
        AuthenticatedUser caseworker = aat.getIdamHelper().authenticate(username, password);
        String s2sToken = aat.getS2SHelper().getToken();

        return () -> {
            RequestSpecification request = RestAssured.given()
                .header("Authorization", "Bearer " + caseworker.getAccessToken())
                .header("ServiceAuthorization", s2sToken);

            return withUserParam ? request.pathParam("user", caseworker.getId()) : request;
        };
    }

    protected RequestSpecification asAutoTestImporter() {
        AuthenticatedUser caseworker = aat.getIdamHelper().authenticate(aat.getImporterAutoTestEmail(),
                                                                        aat.getImporterAutoTestPassword());

        String s2sToken = aat.getS2SHelper().getToken();

        return RestAssured.given(new RequestSpecBuilder()
                                     .setBaseUri(aat.getDefinitionStoreUrl())
                                     .build())
            .header("Authorization", "Bearer " + caseworker.getAccessToken())
            .header("ServiceAuthorization", s2sToken);
    }
}
