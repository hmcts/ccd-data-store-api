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
    public static final String SOLICITOR_1_ORG_ROLE_ASSIGNMENTS = "/Users/ashleynoronha/code/ccd/ccd-data-store-api/src/aat/java/uk/gov/hmcts/ccd/datastore/befta/RoleAssignmentSetup/OrganisationRoleAssignmentsSolicitor1.json";
    public static final String SUPERUSER_ORG_ROLE_ASSIGNMENTS = "/Users/ashleynoronha/code/ccd/ccd-data-store-api/src/aat/java/uk/gov/hmcts/ccd/datastore/befta/RoleAssignmentSetup/OrganisationRoleAssignmentsSuperUser.json";

    public static void main(String[] args) {
        new RoleAssignmentSetup().setupOrganisationRoleAssignments();
    }

//    TODO CHANGE THE PATH OF THE 2 JSON FILES IN THE STATIC VARS ABOVE TO THE CORRECT PATH FOR YOUR LOCAL MACHINE
//    TODO REPLACE THE IDAM ID IN THE 2 JSON FILES LISTED ABOVE AS FINAL VARS WITH CORRECT IDAM IDS FROM YOUR LOCAL

        //NO LONGER REQUIRED AS WE CAN JUST POST ASSIGNMENTS WITH THE 'REPLACE EXISTING' FLAG
    //    public ArrayList<RoleAssignment> getRoleAssignements(){
    //         ArrayList<LinkedHashMap> roleAssingments = asRoleAssignemntCaseworker()
    //            .get("/am/role-assignments/actors/82853c08-73dd-4bd3-a50e-7dae355da504")
    //            .then()Befta_Default_Token_Creation_Data_For_Case_Creation
    //            .extract().path("roleAssignmentResponse");
    //
    //         ArrayList<RoleAssignment> raList = (ArrayList<RoleAssignment>) roleAssingments.stream().map(ra ->
    //             RoleAssignment.builder()
    //             .roleName((String) ra.get("roleName"))
    //             .roleType((String) ra.get("roleType"))
    //             .grantType((String) ra.get("grantType"))
    //             .roleCategory((String) ra.get("roleCategory"))
    //             .classification((String) ra.get("classification"))
    //             .readOnly((Boolean) ra.get("readOnly"))
    //             .attributes(
    //                 RoleAssignmentAttributes.builder()
    //                     .jurisdiction(Optional.ofNullable((String) ra.get("attributes.jurisdiction")))
    //                     .caseType(Optional.ofNullable((String) ra.get("attributes.caseType")))
    //                     .build())
    //             .build()).collect(Collectors.toList());
    //
    //
    //        System.out.println(roleAssingments.get(0));
    //        System.out.println(raList.get(0));
    //
    //        return raList;
    //    }


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
        String bearerToken = "eyJ0eXAiOiJKV1QiLCJ6aXAiOiJOT05FIiwia2lkIjoiYi9PNk92VnYxK3krV2dySDVVaTlXVGlvTHQwPSIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiJjY2QuYWMuc29saWNpdG9yMUBnbWFpbC5jb20iLCJhdXRoX2xldmVsIjowLCJhdWRpdFRyYWNraW5nSWQiOiI4YTA1MTU5Yy1iNWFmLTRlOGYtYWMxZC1mNDMzYjlkYTYzNWEiLCJpc3MiOiJodHRwOi8vZnItYW06ODA4MC9vcGVuYW0vb2F1dGgyL2htY3RzIiwidG9rZW5OYW1lIjoiYWNjZXNzX3Rva2VuIiwidG9rZW5fdHlwZSI6IkJlYXJlciIsImF1dGhHcmFudElkIjoiOGFlNTU1YjgtMWVkYS00YTZiLTk2YWEtZWIxZTVjN2RiOWQyIiwiYXVkIjoiY2NkX2dhdGV3YXkiLCJuYmYiOjE2MjkyOTEzMjgsImdyYW50X3R5cGUiOiJhdXRob3JpemF0aW9uX2NvZGUiLCJzY29wZSI6WyJvcGVuaWQiLCJwcm9maWxlIiwicm9sZXMiXSwiYXV0aF90aW1lIjoxNjI5MjkxMzI3MDAwLCJyZWFsbSI6Ii9obWN0cyIsImV4cCI6MTYyOTMyMDEyOCwiaWF0IjoxNjI5MjkxMzI4LCJleHBpcmVzX2luIjoyODgwMCwianRpIjoiN2QwOGU2ZjktZjRiYi00NGNlLTliZjctZDY4OTQ0NjVhY2JjIn0.RqtK3o-fWiqorscZamA0TKxW1uJZieL07yxnbj-oC41yepMh40sjpC1i_sZRTCT8d5RPz0VJ30N_c7tdkj6_RMqJXEPbkpZfOkUGLVMRoOZWhlJTjSx0N3k3vw14cWazA4X2Jj18fBVOZ8zDbZOVC73mSg7QtyJYuxC85QUIA145MVqYJGD78s-K12C7H3owKW4eKyQA0A0HE4m0BWZBwcQw1bL5v0oSUrqvrV5RwC9HRw_UOi_1tdwT-KS7hFS2c2ey3e6C2_LQ_5uWSp3dEYGGY1uiGT1GbviIBJNN6sZJAd3TYX0ALBnxmY_Uo7yJTg3uM9q1hP7nVs1xG8hE4A";
        String s2sToken = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJjY2RfZ3ciLCJleHAiOjE2MjkzMDU3NzR9.u5EGm7k8H6Dz8yVdqcvRJrE680-7yhpQXlZdq5HPgfdYliOf1Nb4zROuHaIpQbXr-NHsrcw0KEAtJ7qs5BW-YQ";
        return RestAssured.given(new RequestSpecBuilder()
            .setBaseUri("http://localhost:4096")
            .build()
            .header("Authorization", "Bearer " + bearerToken)
            .header("ServiceAuthorization", s2sToken)
        );
    }

}
