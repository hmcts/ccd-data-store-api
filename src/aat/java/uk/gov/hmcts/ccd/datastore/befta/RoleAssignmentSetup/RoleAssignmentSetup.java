package uk.gov.hmcts.ccd.datastore.befta.RoleAssignmentSetup;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import java.net.URISyntaxException;
import uk.gov.hmcts.ccd.datastore.tests.AATHelper;
import uk.gov.hmcts.ccd.datastore.tests.helper.idam.AuthenticatedUser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class RoleAssignmentSetup{

    public static final String CCD_CASEWORKER_AUTOTEST_PASSWORD = "CCD_CASEWORKER_AUTOTEST_PASSWORD";
    public static final String SOLICITOR_1_ORG_ROLE_ASSIGNMENTS = "roleAssignmentSetup/OrganisationRoleAssignmentsSolicitor1.json";
    public static final String SUPERUSER_ORG_ROLE_ASSIGNMENTS = "roleAssignmentSetup/OrganisationRoleAssignmentsSuperUser.json";

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

            String payload = new String(Files.readAllBytes(Paths.get(getClass()
                .getClassLoader()
                .getResource(SOLICITOR_1_ORG_ROLE_ASSIGNMENTS).toURI())));
            asRoleAssignemntCaseworker()
                .contentType("application/json")
                .body(payload)
            .when()
                .post("/am/role-assignments")
                .prettyPeek()
            .then()
                .statusCode(201);

            String payload2 = new String(Files.readAllBytes(Paths.get(getClass()
                .getClassLoader()
                .getResource(SUPERUSER_ORG_ROLE_ASSIGNMENTS).toURI())));
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
        } catch (URISyntaxException e) {
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
        String bearerToken = "eyJ0eXAiOiJKV1QiLCJ6aXAiOiJOT05FIiwia2lkIjoiYi9PNk92VnYxK3krV2dySDVVaTlXVGlvTHQwPSIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiJjY2QuYWMuc3VwZXJ1c2VyQGdtYWlsLmNvbSIsImF1dGhfbGV2ZWwiOjAsImF1ZGl0VHJhY2tpbmdJZCI6IjhmMGZhNDYxLTFmNWQtNDg1My04YzBjLWYyYWU0OGQ4ODYzOSIsImlzcyI6Imh0dHA6Ly9mci1hbTo4MDgwL29wZW5hbS9vYXV0aDIvaG1jdHMiLCJ0b2tlbk5hbWUiOiJhY2Nlc3NfdG9rZW4iLCJ0b2tlbl90eXBlIjoiQmVhcmVyIiwiYXV0aEdyYW50SWQiOiJiMGY1MDI2YS04ZjlkLTQwZmYtOWNiNS00ZmFhYTM2MGRmYmUiLCJhdWQiOiJjY2RfZ2F0ZXdheSIsIm5iZiI6MTYyOTk3ODY1NSwiZ3JhbnRfdHlwZSI6ImF1dGhvcml6YXRpb25fY29kZSIsInNjb3BlIjpbIm9wZW5pZCIsInByb2ZpbGUiLCJyb2xlcyJdLCJhdXRoX3RpbWUiOjE2Mjk5Nzg2NTUwMDAsInJlYWxtIjoiL2htY3RzIiwiZXhwIjoxNjMwMDA3NDU1LCJpYXQiOjE2Mjk5Nzg2NTUsImV4cGlyZXNfaW4iOjI4ODAwLCJqdGkiOiI4NTEyMjBmNS1kMTZkLTQxYTItOGE1Mi1lNzQwY2ZlZDJkMTAifQ.OsMuL6fhgtiPo_VJeYuPkzHfQDu1-2on5mhK3jFwY53TKZbXDNkeYVsEG-5hzOkEkSrqyxboU6AyAn5TblBr4P_2KWhq1fN-EpBaCWSiy4pv_FKq9dQ-1Orh_KytLBrvFRdPmmwneR2eaAtg2TZ6DnDjf_ZiasFe43SDZl6LysoZquw8d2L1j8fwGngyzoEUAmqSS83HCkpa9xEWN7TOWqKsmDjg5IgminK3Om_C-iIZGS46Xds2hvJs7YxZ6xfgqNjbxuf-uDAnE2-10WIGtmR6FOqaRDvRcP6N-CAA7LF-GvLGoZPC0vPcPq10tIWWTPO-DflIXzqEF30JFGP5xA";
        String s2sToken = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJjY2RfZ3ciLCJleHAiOjE2Mjk5OTQyMDh9.ircXHJ8uicTQIxocJnoZgl4NRHOJZV6dyaYlbjP-VDELHM4wlq7_L38bATbQEbpykC8ehf6dhD3K3AiyAdbNUQ";
        return RestAssured.given(new RequestSpecBuilder()
            .setBaseUri("http://localhost:4096")
            .build()
            .header("Authorization", "Bearer " + bearerToken)
            .header("ServiceAuthorization", s2sToken)
        );
    }

}
