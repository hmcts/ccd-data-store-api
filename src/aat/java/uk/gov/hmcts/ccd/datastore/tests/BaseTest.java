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

    protected Supplier<RequestSpecification> asAutoTestCaseworker(final Boolean withUserParam) {

        final AuthenticatedUser caseworker = aat.getIdamHelper()
                                                .authenticate(aat.getCaseworkerAutoTestEmail(),
                                                              aat.getCaseworkerAutoTestPassword());

        final String s2sToken = aat.getS2SHelper()
                                   .getToken();

        return () -> {
            final RequestSpecification request = RestAssured.given()
                                                            .header("Authorization",
                                                                    "Bearer " + caseworker.getAccessToken())
                                                            .header("ServiceAuthorization", s2sToken);

            return withUserParam ? request.pathParam("user", caseworker.getId()) : request;
        };
    }

    protected RequestSpecification asAutoTestImporter() {

        AuthenticatedUser caseworker = aat.getIdamHelper()
            .authenticate(aat.getImporterAutoTestEmail(),
                          aat.getImporterAutoTestPassword());

        String s2sToken = aat.getS2SHelper().getToken();

        return RestAssured.given(new RequestSpecBuilder()
                                     .setBaseUri(aat.getDefinitionImportUrl())
                                     .build())
            .header("Authorization", "Bearer " + caseworker.getAccessToken())
            .header("ServiceAuthorization", s2sToken);
    }
}
