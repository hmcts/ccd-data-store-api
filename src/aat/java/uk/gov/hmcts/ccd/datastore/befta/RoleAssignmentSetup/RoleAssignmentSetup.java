package uk.gov.hmcts.ccd.datastore.befta.RoleAssignmentSetup;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Supplier;
import uk.gov.hmcts.ccd.datastore.tests.AATHelper;
import uk.gov.hmcts.ccd.datastore.tests.helper.idam.AuthenticatedUser;

public class RoleAssignmentSetup{

    public static final String SOLICITOR_1_ORG_ROLE_ASSIGNMENTS = "roleAssignmentSetup/OrganisationRoleAssignmentsSolicitor1.json";
    public static final String SUPERUSER_ORG_ROLE_ASSIGNMENTS = "roleAssignmentSetup/OrganisationRoleAssignmentsSuperUser.json";

    public static final String STAFF1_ROLE_ASSIGNMENTS = "roleAssignmentSetup/OrganisationRoleAssignmentsStaff1.json";
    public static final String STAFF2_ROLE_ASSIGNMENTS = "roleAssignmentSetup/OrganisationRoleAssignmentsStaff2.json";
    public static final String STAFF3_ROLE_ASSIGNMENTS = "roleAssignmentSetup/OrganisationRoleAssignmentsStaff3.json";
    public static final String STAFF5_ROLE_ASSIGNMENTS = "roleAssignmentSetup/OrganisationRoleAssignmentsStaff5.json";
    public static final String STAFF6_ROLE_ASSIGNMENTS = "roleAssignmentSetup/OrganisationRoleAssignmentsStaff6.json";
    public static final String STAFF7_ROLE_ASSIGNMENTS = "roleAssignmentSetup/OrganisationRoleAssignmentsStaff7.json";
    public static final String STAFF8_ROLE_ASSIGNMENTS = "roleAssignmentSetup/OrganisationRoleAssignmentsStaff8.json";
    public static final String OTHER1_ROLE_ASSIGNMENTS = "roleAssignmentSetup/OrganisationRoleAssignmentsOther1.json";

    public static void main(String[] args) {
        new RoleAssignmentSetup().setupOrganisationRoleAssignments();
    }

//    TODO REPLACE THE IDAM ID IN THE 2 JSON FILES LISTED ABOVE AS FINAL VARS WITH CORRECT IDAM IDS FROM YOUR LOCAL

    public void setupOrganisationRoleAssignments() {

        try {

            loadRoleAssignments(SOLICITOR_1_ORG_ROLE_ASSIGNMENTS);
            loadRoleAssignments(SUPERUSER_ORG_ROLE_ASSIGNMENTS);

            loadRoleAssignments(STAFF1_ROLE_ASSIGNMENTS);
            loadRoleAssignments(STAFF2_ROLE_ASSIGNMENTS);
            loadRoleAssignments(STAFF3_ROLE_ASSIGNMENTS);
            loadRoleAssignments(STAFF5_ROLE_ASSIGNMENTS);
            loadRoleAssignments(STAFF6_ROLE_ASSIGNMENTS);
            loadRoleAssignments(STAFF7_ROLE_ASSIGNMENTS);
            loadRoleAssignments(STAFF8_ROLE_ASSIGNMENTS);
            loadRoleAssignments(OTHER1_ROLE_ASSIGNMENTS);


        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

    }

    private void loadRoleAssignments(String superuserOrgRoleAssignments) throws IOException, URISyntaxException {
        String payload2 = new String(Files.readAllBytes(Paths.get(getClass()
            .getClassLoader()
            .getResource(superuserOrgRoleAssignments).toURI())));
        asRoleAssignemntSuperUser()
            .contentType("application/json")
            .body(payload2)
            .when()
            .post("/am/role-assignments")
            .prettyPeek()
            .then()
            .statusCode(201);
    }


    protected RequestSpecification asRoleAssignemntSuperUser() {
        return RestAssured.given(authenticateAndCreateRequestSpecification(
            "ccd.ac.superuser@gmail.com", "Pa55word11", true)
        .get());
    }


    private Supplier<RequestSpecification> authenticateAndCreateRequestSpecification(String username, String password,
                                                                                     Boolean withUserParam) {
        AATHelper aat = AATHelper.INSTANCE;
        AuthenticatedUser caseworker = aat.getIdamHelper().authenticate(username, password);
        String s2sToken = aat.getS2SHelper().getToken();

        return () -> {
            RequestSpecification request = RestAssured.given()
                .baseUri("http://localhost:4096")
                .header("Authorization", "Bearer " + caseworker.getAccessToken())
                .header("ServiceAuthorization", s2sToken);

            return withUserParam ? request.pathParam("user", caseworker.getId()) : request;
        };
    }

}
