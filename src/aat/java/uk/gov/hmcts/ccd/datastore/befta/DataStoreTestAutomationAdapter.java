package uk.gov.hmcts.ccd.datastore.befta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

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

    private static final String[][] CCD_ROLES_NEEDED_FOR_TA = {
            { "caseworker-autotest1", "PUBLIC" },
            { "caseworker-autotest1-private", "PRIVATE" },
            { "caseworker-autotest1-senior", "RESTRICTED" },
            { "caseworker-autotest1-solicitor", "PRIVATE" },

            { "caseworker-autotest2", "PUBLIC" },
            { "caseworker-autotest2-private", "PRIVATE" },
            { "caseworker-autotest2-senior", "RESTRICTED" },
            { "caseworker-autotest2-solicitor", "PRIVATE" },

            { "caseworker-befta_jurisdiction_1", "PUBLIC" },
            { "caseworker-befta_jurisdiction_2", "PUBLIC" },
            { "citizen", "PUBLIC" },
            { "caseworker-befta_jurisdiction_3", "PUBLIC" },
            { "caseworker-befta_jurisdiction_3-solicitor", "PUBLIC" }
    };

    @Override
    public void doLoadTestData() {
        addCcdRoles();
        importDefinitions();
    }

    private void addCcdRoles() {
        for (String[] roleInfo : CCD_ROLES_NEEDED_FOR_TA) {
            try {
                logger.info("\n\nAdding CCD Role {}, {}...", roleInfo[0], roleInfo[1]);
                addCcdRole(roleInfo[0], roleInfo[1]);
                logger.info("\n\nAdded CCD Role {}, {}...", roleInfo[0], roleInfo[1]);
            } catch (Exception e) {
                logger.info("\n\nCouldn't adding CCD Role {}, {} - Exception: {}.\\n\\n", roleInfo[0], roleInfo[1], e);
            }
        }
    }

    private void addCcdRole(String role, String classification) {
        Map<String, String> ccdRoleInfo = new HashMap<>();
        ccdRoleInfo.put("role", role);
        ccdRoleInfo.put("security_classification", classification);
        Response response = asAutoTestImporter().given()
                .header("Content-type", "application/json").body(ccdRoleInfo).when()
                .put("/api/user-role");
        if (response.getStatusCode() != 205 && response.getStatusCode() != 215) {
            String message = "Import failed with response body: " + response.body().prettyPrint();
            message += "\nand http code: " + response.statusCode();
            throw new FunctionalTestException(message);
        }
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
        if (response.getStatusCode() != 201) {
            String message = "Import failed with response body: " + response.body().prettyPrint();
            message += "\nand http code: " + response.statusCode();
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
