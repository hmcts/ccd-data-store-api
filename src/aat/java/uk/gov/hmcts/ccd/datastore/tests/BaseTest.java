package uk.gov.hmcts.ccd.datastore.tests;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.hmcts.ccd.datastore.tests.helper.idam.AuthenticatedUser;

import java.util.function.Supplier;

@ExtendWith(AATExtension.class)
public abstract class BaseTest {
    protected final AATHelper aat;

    protected BaseTest(AATHelper aat) {
        this.aat = aat;
        RestAssured.baseURI = aat.getTestUrl();
    }

    protected Supplier<RequestSpecification> asAutoTestCaseworker() {

        final AuthenticatedUser caseworker = aat.getIdamHelper()
                                                .authenticate(aat.getCaseworkerAutoTestEmail(),
                                                              aat.getCaseworkerAutoTestPassword());

        final String s2sToken = aat.getS2SHelper()
                                   .getToken(aat.getGatewayServiceName(), aat.getGatewayServiceSecret());

        return () -> RestAssured.given()
                          .header("Authorization", "Bearer " + caseworker.getAccessToken())
                          .header("ServiceAuthorization", s2sToken)
                          .pathParam("user", caseworker.getId());
    }
}
