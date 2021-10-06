package uk.gov.hmcts.ccd.datastore.befta.RoleAssignmentSetup;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import uk.gov.hmcts.ccd.datastore.tests.AATHelper;
import uk.gov.hmcts.ccd.datastore.tests.helper.idam.AuthenticatedUser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class RoleAssignmentSetup{

    public static final String CCD_CASEWORKER_AUTOTEST_PASSWORD = "CCD_CASEWORKER_AUTOTEST_PASSWORD";
    public static final String SOLICITOR_1_ORG_ROLE_ASSIGNMENTS = "/Users/skynet/Desktop/mis_cosas/work/MoJ/repositories/ccd-data-store-api/src/aat/java/uk/gov/hmcts/ccd/datastore/befta/RoleAssignmentSetup/OrganisationRoleAssignmentsSolicitor1.json";
    public static final String SUPERUSER_ORG_ROLE_ASSIGNMENTS = "/Users/skynet/Desktop/mis_cosas/work/MoJ/repositories/ccd-data-store-api/src/aat/java/uk/gov/hmcts/ccd/datastore/befta/RoleAssignmentSetup/OrganisationRoleAssignmentsSuperUser.json";

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
    //          AuthenticatedUser caseworker = aat.getIdamHelper().authenticate("ccd.ac.solicitor1@gmail.com",
    //              "Pa55word");
    //          caseworker.getId();
    //          caseworker.getAccessToken();
    //
    //          String s2sToken = aat.getS2SHelper().getToken();

        //ccd.ac.solicitor1@gmail.com
        //TODO YOU NEED TO HARDCODE THE AUTH AND S2S TOKEN BELOW IN ORDER TO SUCCESSFULLY UPLOAD THE ROLEASSIGNMENTS
        String bearerToken = "eyJ0eXAiOiJKV1QiLCJ6aXAiOiJOT05FIiwia2lkIjoiYi9PNk92VnYxK3krV2dySDVVaTlXVGlvTHQwPSIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiJjY2QuYWMuc29saWNpdG9yMUBnbWFpbC5jb20iLCJhdXRoX2xldmVsIjowLCJhdWRpdFRyYWNraW5nSWQiOiJmZmEyODY0Mi0wNGE2LTQ5NmMtOTNhNS1kN2IzNGI1YTQ4ZTEiLCJpc3MiOiJodHRwOi8vZnItYW06ODA4MC9vcGVuYW0vb2F1dGgyL2htY3RzIiwidG9rZW5OYW1lIjoiYWNjZXNzX3Rva2VuIiwidG9rZW5fdHlwZSI6IkJlYXJlciIsImF1dGhHcmFudElkIjoiYTg1Nzk5ZTQtMzlmMy00NzQ4LTk3NzYtMDU3MjE0ZTE4OWVjIiwiYXVkIjoiY2NkX2dhdGV3YXkiLCJuYmYiOjE2MzEyNzA5MjcsImdyYW50X3R5cGUiOiJhdXRob3JpemF0aW9uX2NvZGUiLCJzY29wZSI6WyJvcGVuaWQiLCJwcm9maWxlIiwicm9sZXMiXSwiYXV0aF90aW1lIjoxNjMxMjcwOTIzMDAwLCJyZWFsbSI6Ii9obWN0cyIsImV4cCI6MTYzMTI5OTcyNywiaWF0IjoxNjMxMjcwOTI3LCJleHBpcmVzX2luIjoyODgwMCwianRpIjoiMDA5ZWQzZjQtYWEzMS00MDBkLTlkNGUtMGIwZWY5MGU2ZTg0In0.XgOOPa_XI_eB-QEIjW2N14F_M0HrGmoLJDoDZiTyIqDVsae0n6vHmzRthHZbz0E5IVx9iqGMF0_TVdRD2ZUbSkSvwK4osISq9nGI7FOX2uMNIuumJDnOoSlx4ZBUxcVuMLOg4tNSpsrMxPwypUb3vBOPejiTnTd7ATaVRNk6gs7Y9-9g3OpCr0oql9d_3yk9BW75l9q_KXGJTL61EUFsd6W-FI1NXWPk5ttv-kOilOxYC4xB7yDbtKjNTctX3RJjTwYOcTFeW6_yyZfsN0fQopmuVNrfby74AhVQDmL8yHsJ9KPfmfYbMpuyTFSqLZQ-BgR8tvaydZgSQ-Vc8tuV_A";
        String s2sToken = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJjY2RfZ3ciLCJleHAiOjE2MzEyODU0NTh9.vDup4liyfruc7PiFkrOT3chbkxjm5Aj6M28rr38SX_wLcuffXE-rxGx0e-1rYmTGgWvvAFrtARJ2_laQgLn2eQ";
        return RestAssured.given(new RequestSpecBuilder()
            .setBaseUri("http://localhost:4096")
            .build()
            .header("Authorization", "Bearer " + bearerToken)
            .header("ServiceAuthorization", s2sToken)
        );
    }

}
