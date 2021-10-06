package uk.gov.hmcts.ccd.datastore.befta.roleassignmentsetup;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class RoleAssignmentSetup {

    public static final String CCD_CASEWORKER_AUTOTEST_PASSWORD = "CCD_CASEWORKER_AUTOTEST_PASSWORD";
    public static final String SOLICITOR_1_ORG_ROLE_ASSIGNMENTS = "";
    public static final String SUPERUSER_ORG_ROLE_ASSIGNMENTS = "";

    public static void main(String[] args) {
        new RoleAssignmentSetup().setupOrganisationRoleAssignments();
    }

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
