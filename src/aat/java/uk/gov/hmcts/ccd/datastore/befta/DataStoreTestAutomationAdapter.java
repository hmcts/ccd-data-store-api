package uk.gov.hmcts.ccd.datastore.befta;


import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import uk.gov.hmcts.befta.DefaultTestAutomationAdapter;
import uk.gov.hmcts.befta.TestAutomationConfig;
import uk.gov.hmcts.befta.data.UserData;

public class DataStoreTestAutomationAdapter extends DefaultTestAutomationAdapter {

    private Logger logger = LoggerFactory.getLogger(DataStoreTestAutomationAdapter.class);

    private final String BE_FTA_FILE_JURISDICTION1 = "src/aat/resources/CCD_BEFTA_JURISDICTION1.xlsx";
    private final String BE_FTA_FILE_JURISDICTION2 = "src/aat/resources/CCD_BEFTA_JURISDICTION2.xlsx";
    private final String BE_FTA_FILE_JURISDICTION3 = "src/aat/resources/CCD_BEFTA_JURISDICTION3.xlsx";

    public DataStoreTestAutomationAdapter(TestAutomationConfig config) {
        super(config);
    }

    @Override
    public void doLoadTestData() {
        importDefinitions();
    }

    private void importDefinitions() {
        logger.info("Importing {}...", BE_FTA_FILE_JURISDICTION1);
        importDefinition(BE_FTA_FILE_JURISDICTION1);
        logger.info("Imported {}.", BE_FTA_FILE_JURISDICTION1);

        logger.info("Importing {}...", BE_FTA_FILE_JURISDICTION2);
        importDefinition(BE_FTA_FILE_JURISDICTION2);
        logger.info("Imported {}.", BE_FTA_FILE_JURISDICTION2);

        logger.info("Importing {}...", BE_FTA_FILE_JURISDICTION3);
        importDefinition(BE_FTA_FILE_JURISDICTION3);
        logger.info("Imported {}", BE_FTA_FILE_JURISDICTION3);
    }

    private void importDefinition(String file) {
        Response response = asAutoTestImporter().given().multiPart(new File(file)).when().post("/import");
        String message = "Import failed with response body: " + response.body().prettyPrint();
        message += "\nand http code: " + response.statusCode();
        Assert.assertEquals(message, 201, response.getStatusCode());
    }

    private RequestSpecification asAutoTestImporter() {
        UserData caseworker = authenticate(new UserData(getAutomationConfig().getImporterAutoTestEmail(),
                getAutomationConfig().getImporterAutoTestPassword()));

        String s2sToken = getNewS2SToken();
        return RestAssured
                .given(new RequestSpecBuilder().setBaseUri(getAutomationConfig().getDefinitionStoreUrl())
                        .build())
                .header("Authorization", "Bearer " + caseworker.getAccessToken())
                .header("ServiceAuthorization", s2sToken);
    }

}
