package uk.gov.hmcts.ccd.datastore.befta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import uk.gov.hmcts.befta.BeftaMain;
import uk.gov.hmcts.befta.DefaultTestAutomationAdapter;
import uk.gov.hmcts.befta.data.UserData;
import uk.gov.hmcts.befta.exception.FunctionalTestException;

public class DataStoreTestAutomationAdapter extends DefaultTestAutomationAdapter {

    private Logger logger = LoggerFactory.getLogger(DataStoreTestAutomationAdapter.class);

    private static final String[] TEST_DEFINITIONS_NEEDED_FOR_TA = {
            "src/aat/resources/CCD_CNP_27.xlsx",
            "src/aat/resources/CCD_CNP_27_AUTOTEST1.xlsx",
            "src/aat/resources/CCD_CNP_27_AUTOTEST2.xlsx",
            "src/aat/resources/CCD_CNP_RDM5118.xlsx",

            "src/aat/resources/CCD_BEFTA_JURISDICTION1.xlsx",
            "src/aat/resources/CCD_BEFTA_JURISDICTION2.xlsx",
            "src/aat/resources/CCD_BEFTA_JURISDICTION3.xlsx"
    };

    @Override
    public void doLoadTestData() {
        importDefinitions();
    }

    private void importDefinitions() {
        for (String fileName : TEST_DEFINITIONS_NEEDED_FOR_TA) {
            try {
                logger.info("\n\nImporting {}...", fileName);
                importDefinition(fileName);
                logger.info("Imported {}.\n\n", fileName);
            } catch (Exception e) {
                logger.info("Couldn't import {} - Exception: {}.\n\n", fileName, e);
            }
        }
    }

    private void importDefinition(String file) {
        Response response = asAutoTestImporter().given().multiPart(new File(file)).when().post("/import");
        String message = "Import failed with response body: " + response.body().prettyPrint();
        message += "\nand http code: " + response.statusCode();
        if (response.getStatusCode() != 201) {
            throw new FunctionalTestException(message);
        }
    }

    private RequestSpecification asAutoTestImporter() {
        UserData caseworker = new UserData(BeftaMain.getConfig().getImporterAutoTestEmail(),
                BeftaMain.getConfig().getImporterAutoTestPassword());
        authenticate(caseworker);

        String s2sToken = getNewS2SToken();
        return RestAssured
                .given(new RequestSpecBuilder().setBaseUri(BeftaMain.getConfig().getDefinitionStoreUrl())
                        .build())
                .header("Authorization", "Bearer " + caseworker.getAccessToken())
                .header("ServiceAuthorization", s2sToken);
    }

}
