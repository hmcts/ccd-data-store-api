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
        String bearerToken = "eyJ0eXAiOiJKV1QiLCJ6aXAiOiJOT05FIiwia2lkIjoiYi9PNk92VnYxK3krV2dySDVVaTlXVGlvTHQwPSIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiJjY2QuYWMuc3VwZXJ1c2VyQGdtYWlsLmNvbSIsImF1dGhfbGV2ZWwiOjAsImF1ZGl0VHJhY2tpbmdJZCI6Ijc1NDQ2ZDczLWI1ZTMtNDEyZC1iM2JmLTM3MDIzMTM2YjYxYiIsImlzcyI6Imh0dHA6Ly9mci1hbTo4MDgwL29wZW5hbS9vYXV0aDIvaG1jdHMiLCJ0b2tlbk5hbWUiOiJhY2Nlc3NfdG9rZW4iLCJ0b2tlbl90eXBlIjoiQmVhcmVyIiwiYXV0aEdyYW50SWQiOiIyMjJjOTVkZC0wMzRkLTQzZDQtYmQxMC1iOTI3MjU4YmVjMzAiLCJhdWQiOiJjY2RfZ2F0ZXdheSIsIm5iZiI6MTYzMjE0OTUxMywiZ3JhbnRfdHlwZSI6ImF1dGhvcml6YXRpb25fY29kZSIsInNjb3BlIjpbIm9wZW5pZCIsInByb2ZpbGUiLCJyb2xlcyJdLCJhdXRoX3RpbWUiOjE2MzIxNDk1MTIwMDAsInJlYWxtIjoiL2htY3RzIiwiZXhwIjoxNjMyMTc4MzEzLCJpYXQiOjE2MzIxNDk1MTMsImV4cGlyZXNfaW4iOjI4ODAwLCJqdGkiOiI1ZTRiZTAxMC01Y2Y3LTQ2MmMtYmFiMy1jNDM0Y2MzZDNmMTkifQ.NUg02VrSihryyqS68zuc6Dapu6vdTmjz8ISUdZRJ9ZBuL2s3gwucXQ5wAXcXSkclQCuMh_xSk-LiPqAc7gi2o4NOteTVt_PchNez9lbNC-nrPhTeZ3v1OTgHLk6eZol0A0MVocHj0iajICaY6P1dcET7RW7VTqPEfD5uKKeohsemEwN7jfzF0HoUHy2VUerYOGaoJRlWAGYk_jDI8rknBxcW6Ip85aFWkm9Q7JPoXde13qLRRTV6PblSAUYYItgVVRbuN1oPQqsHvgZbOBibBjdH_zM7QfXd5hkBG31dg8mG5v-_LbThI4dEITn3yEdSS5o4t3ltSNe0hbcZ4vw2EA";
        String s2sToken = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJjY2RfZ3ciLCJleHAiOjE2MzIxNjM5MTh9.bdt_VbZF1T6O-W3gGXmD9z3S_91kOFZt5StEryZ_GvPuteClQjwpFmO1bmJtyZIjptPEhAZGKbWj4Ov-BNNYOg";
        return RestAssured.given(new RequestSpecBuilder()
            .setBaseUri("http://localhost:4096")
            .build()
            .header("Authorization", "Bearer " + bearerToken)
            .header("ServiceAuthorization", s2sToken)
        );
    }

}
