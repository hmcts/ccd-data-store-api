package uk.gov.hmcts.ccd.datastore.befta.roleassignmentsetup;

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

public class RoleAssignmentSetup {

    public static final String SOLICITOR_1_ORG_ROLE_ASSIGNMENTS =
        "roleAssignmentSetup/OrganisationRoleAssignmentsSolicitor1.json";
    public static final String SUPERUSER_ORG_ROLE_ASSIGNMENTS =
        "roleAssignmentSetup/OrganisationRoleAssignmentsSuperUser.json";

    public static final String STAFF1_ROLE_ASSIGNMENTS =
        "roleAssignmentSetup/OrganisationRoleAssignmentsStaff1.json";
    public static final String STAFF2_ROLE_ASSIGNMENTS =
        "roleAssignmentSetup/OrganisationRoleAssignmentsStaff2.json";
    public static final String STAFF3_ROLE_ASSIGNMENTS =
        "roleAssignmentSetup/OrganisationRoleAssignmentsStaff3.json";
    public static final String STAFF5_ROLE_ASSIGNMENTS =
        "roleAssignmentSetup/OrganisationRoleAssignmentsStaff5.json";
    public static final String STAFF6_ROLE_ASSIGNMENTS =
        "roleAssignmentSetup/OrganisationRoleAssignmentsStaff6.json";
    public static final String STAFF7_ROLE_ASSIGNMENTS =
        "roleAssignmentSetup/OrganisationRoleAssignmentsStaff7.json";
    public static final String STAFF8_ROLE_ASSIGNMENTS =
        "roleAssignmentSetup/OrganisationRoleAssignmentsStaff8.json";
    public static final String OTHER1_ROLE_ASSIGNMENTS =
        "roleAssignmentSetup/OrganisationRoleAssignmentsOther1.json";

    public static void main(String[] args) {
        new RoleAssignmentSetup().setupOrganisationRoleAssignments();
    }

    public void setupOrganisationRoleAssignments() {

        try {

            loadRoleAssignments("ccd.ac.solicitor1@gmail.com", SOLICITOR_1_ORG_ROLE_ASSIGNMENTS);
            loadRoleAssignments("ccd.ac.superuser@gmail.com", SUPERUSER_ORG_ROLE_ASSIGNMENTS);

            loadRoleAssignments("ccd.ac.staff1@gmail.com", STAFF1_ROLE_ASSIGNMENTS);
            loadRoleAssignments("ccd.ac.staff2@gmail.com", STAFF2_ROLE_ASSIGNMENTS);
            loadRoleAssignments("ccd.ac.staff3@gmail.com", STAFF3_ROLE_ASSIGNMENTS);
            loadRoleAssignments("ccd.ac.staff5@gmail.com", STAFF5_ROLE_ASSIGNMENTS);
            loadRoleAssignments("ccd.ac.staff6@gmail.com", STAFF6_ROLE_ASSIGNMENTS);
            loadRoleAssignments("ccd.ac.staff7@gmail.com", STAFF7_ROLE_ASSIGNMENTS);
            loadRoleAssignments("ccd.ac.staff8@gmail.com", STAFF8_ROLE_ASSIGNMENTS);
            loadRoleAssignments("ccd.ac.other1@gmail.com", OTHER1_ROLE_ASSIGNMENTS);


        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

    }

    private void loadRoleAssignments(String userName,
                                     String superuserOrgRoleAssignments) throws IOException, URISyntaxException {
        System.out.println("superuserOrgRoleAssignments << >> " + superuserOrgRoleAssignments);
        String payload2 = new String(Files.readAllBytes(Paths.get(getClass()
            .getClassLoader()
            .getResource(superuserOrgRoleAssignments).toURI())));
        asRoleAssignemntCaseworker()
            .contentType("application/json")
            .body(payload2)
            .when()
            .post("/am/role-assignments")
            .prettyPeek()
            .then()
            .statusCode(201);
    }


    protected RequestSpecification asRoleAssignemntSuperUser(String userName) {
        return RestAssured.given(authenticateAndCreateRequestSpecification(
            userName, "Pa55word11", true)
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

    @SuppressWarnings("checkstyle:LineLength")
    protected RequestSpecification asRoleAssignemntCaseworker() {
        String bearerToken = "eyJ0eXAiOiJKV1QiLCJ6aXAiOiJOT05FIiwia2lkIjoiYi9PNk92VnYxK3krV2dySDVVaTlXVGlvTHQwPSIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiJjY2QuYWMuc3VwZXJ1c2VyQGdtYWlsLmNvbSIsImF1dGhfbGV2ZWwiOjAsImF1ZGl0VHJhY2tpbmdJZCI6ImE5NjEwNTA0LTg4MGUtNDVkNC05NTMwLWU4Mjk4NGMyNjVjZiIsImlzcyI6Imh0dHA6Ly9mci1hbTo4MDgwL29wZW5hbS9vYXV0aDIvaG1jdHMiLCJ0b2tlbk5hbWUiOiJhY2Nlc3NfdG9rZW4iLCJ0b2tlbl90eXBlIjoiQmVhcmVyIiwiYXV0aEdyYW50SWQiOiIzYWRiMjIwZC05MGVlLTQzZmMtOWQ2Ny0zYTFkMDBjZTJkZmQiLCJhdWQiOiJjY2RfZ2F0ZXdheSIsIm5iZiI6MTYzMjI2MDM3MSwiZ3JhbnRfdHlwZSI6ImF1dGhvcml6YXRpb25fY29kZSIsInNjb3BlIjpbIm9wZW5pZCIsInByb2ZpbGUiLCJyb2xlcyJdLCJhdXRoX3RpbWUiOjE2MzIyNjAzNzAwMDAsInJlYWxtIjoiL2htY3RzIiwiZXhwIjoxNjMyMjg5MTcxLCJpYXQiOjE2MzIyNjAzNzEsImV4cGlyZXNfaW4iOjI4ODAwLCJqdGkiOiI5OWY4NjA1ZC05OGMwLTQzNTEtODg0Yi1jMWEyNTFkMmNhMDUifQ.Eg0DKEVXabE_0QZ34TFvK1dqugQRe_UhuJI2mrTRKP-JAlnF02ze365e_a7f1rH7-ehvtXW2kPUdgi-_zZJfx69zrdTzFA6QbTvBFbyFj7WWVqQ4rC_RarRsvPXwfpA_aLGoDM9yj6e3ZpndCF0Q9tygUhURA_9O8kTTPoiaXrPhGdCOXjX04Vgr0lO4Ud_lDa-3PnVA9XD8wh9VxIa-NhJSnFid3SF4svqc9_pTDr8ZvBzYcuf96HYKE_WXVfhmSPuKf61j5773afdJpS5Qy4gWcIwF5mdCYlvszakdoOrfPV35Xz3R9ac8lgubo9i_TBsXoRy9hGEil_yXBjRYhA";
        String s2sToken = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJjY2RfZ3ciLCJleHAiOjE2MzIyNzQ3Nzd9._qq3tabLsxdYkxk7PZed0JI9jH7K5053kBvq6g-k4EgQOrUrCysMGqwNIiTmIAFT5ttWlw2QV8QN5-R_1YTqsg";
        return RestAssured.given(new RequestSpecBuilder()
            .setBaseUri("http://localhost:4096")
            .build()
            .header("Authorization", "Bearer " + bearerToken)
            .header("ServiceAuthorization", s2sToken)
        );
    }

}
