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

    private static final String BE_FTA_FILE_JURISDICTION_1 = "src/aat/resources/CCD_BEFTA_JURISDICTION1.xlsx";
    private static final String BE_FTA_FILE_JURISDICTION_2 = "src/aat/resources/CCD_BEFTA_JURISDICTION2.xlsx";
    private static final String BE_FTA_FILE_JURISDICTION_3 = "src/aat/resources/CCD_BEFTA_JURISDICTION3.xlsx";

    @Override
    public void doLoadTestData() {
        importDefinitions();
    }

    private void importDefinitions() {
        logger.info("Importing {}...", BE_FTA_FILE_JURISDICTION_1);
        importDefinition(BE_FTA_FILE_JURISDICTION_1);
        logger.info("Imported {}.", BE_FTA_FILE_JURISDICTION_1);

        logger.info("Importing {}...", BE_FTA_FILE_JURISDICTION_2);
        importDefinition(BE_FTA_FILE_JURISDICTION_2);
        logger.info("Imported {}.", BE_FTA_FILE_JURISDICTION_2);

        logger.info("Importing {}...", BE_FTA_FILE_JURISDICTION_3);
        importDefinition(BE_FTA_FILE_JURISDICTION_3);
        logger.info("Imported {}", BE_FTA_FILE_JURISDICTION_3);
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
