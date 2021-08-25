package uk.gov.hmcts.ccd.test;

public class RoleAssignmentsHelper {
    public static final String GET_ROLE_ASSIGNMENTS_PREFIX = "/am/role-assignments/actors/";

    private RoleAssignmentsHelper() {
    }

    public static String roleAssignmentResponseJson(String... roleAssignmentsAsJson) {
        return "{ \"roleAssignmentResponse\" : ["
            + String.join(",", roleAssignmentsAsJson)
            + "]}";
    }

    public static String roleAssignmentJson(String roleName, String jurisdiction, String caseType, String caseId) {
        return "        {\n"
               + "          \"id\": \"e6fc5ebb-63e3-4613-9cfc-b3f9b1559571\",\n"
               + "          \"actorIdType\": \"IDAM\",\n"
               + "          \"actorId\": \"123\",\n"
               + "          \"roleType\": \"CASE\",\n"
               + "          \"roleName\": \"" + roleName + "\",\n"
               + "          \"classification\": \"PUBLIC\",\n"
               + "          \"grantType\": \"STANDARD\",\n"
               + "          \"roleCategory\": \"SPECIFIC\",\n"
               + "          \"readOnly\": false,\n"
               + "          \"beginTime\": \"2021-02-01T00:00:00Z\",\n"
               + "          \"endTime\": \"2122-01-01T00:00:00Z\",\n"
               + "          \"created\": \"2020-12-23T06:37:58.096065Z\",\n"
               + "          \"attributes\": {\n"
               + "            \"jurisdiction\": \"" + jurisdiction + "\",\n"
               + "            \"caseType\": \"" + caseType + "\",\n"
               + "            \"caseId\": \"" + caseId + "\"\n"
               + "          },\n"
               + "          \"authorisations\": []\n"
               + "        }";
    }
}
