package uk.gov.hmcts.ccd.datastore.befta.roleAssignmentSetup;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class RoleAssignmentSetup{

    public static final String CCD_CASEWORKER_AUTOTEST_PASSWORD = "CCD_CASEWORKER_AUTOTEST_PASSWORD";
    public static final String SOLICITOR_1_ORG_ROLE_ASSIGNMENTS = "/Users/skynet/Desktop/mis_cosas/work/MoJ/repositories" +
        "/ccd-data-store-api/src/aat/java/uk/gov/hmcts/ccd/datastore/befta/RoleAssignmentSetup" +
        "/OrganisationRoleAssignmentsSolicitor1.json";
    public static final String SUPERUSER_ORG_ROLE_ASSIGNMENTS = "/Users/skynet/Desktop/mis_cosas/work/MoJ/repositories" +
        "/ccd-data-store-api/src/aat/java/uk/gov/hmcts/ccd/datastore/befta/RoleAssignmentSetup/" +
        "OrganisationRoleAssignmentsSuperUser.json";

    public static void main(String[] args) {
        new RoleAssignmentSetup().setupOrganisationRoleAssignments();
    }

    //    TODO CHANGE THE PATH OF THE 2 JSON FILES IN THE STATIC VARS ABOVE TO THE CORRECT PATH FOR YOUR LOCAL MACHINE
    //    TODO REPLACE THE IDAM ID IN THE 2 JSON FILES LISTED ABOVE AS FINAL VARS WITH CORRECT IDAM IDS FROM YOUR LOCAL


    public void setupOrganisationRoleAssignments() {

        try {

            String payload = new String(Files.readAllBytes(Paths.get(SOLICITOR_1_ORG_ROLE_ASSIGNMENTS)));
            asRoleAssignemntCaseworker()
                .contentType("application/json")
                .body(payload)
            .when()
                .post("/am/role-assignments")
                .prettyPeek()
            .then()
                .statusCode(201);

            String payload2 = new String(Files.readAllBytes(Paths.get(SUPERUSER_ORG_ROLE_ASSIGNMENTS)));
            asRoleAssignemntCaseworker()
                .contentType("application/json")
                .body(payload2)
                .when()
                .post("/am/role-assignments")
                .prettyPeek()
                .then()
                .statusCode(201);


        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    protected RequestSpecification asRoleAssignemntCaseworker() {
        // ccd.ac.solicitor1@gmail.com
        // TODO YOU NEED TO HARDCODE THE AUTH AND S2S TOKEN BELOW IN ORDER TO SUCCESSFULLY UPLOAD THE ROLEASSIGNMENTS
        String bearerToken = "";
        String s2sToken = "";
        return RestAssured.given(new RequestSpecBuilder()
            .setBaseUri("http://localhost:4096")
            .build()
            .header("Authorization", "Bearer " + bearerToken)
            .header("ServiceAuthorization", s2sToken)
        );
    }

}
