package uk.gov.hmcts.ccd.test;

import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentAttributesResource;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentRequestResource;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentRequestResponse;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentResource;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentResponse;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.ActorIdType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.Classification;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.RoleCategory;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.RoleType;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.RoleToAccessProfileDefinition;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.ccd.TestFixtures.loadCaseTypeDefinition;

public class RoleAssignmentsHelper {
    public static final String GET_ROLE_ASSIGNMENTS_PREFIX = "/am/role-assignments/actors/";

    private static final Instant BEGIN_TIME = Instant.parse("2015-10-21T13:32:21.123Z");
    private static final Instant END_TIME = Instant.parse("2215-11-04T14:43:22.456Z");
    private static final Instant CREATED = Instant.parse("2020-12-04T15:54:23.789Z");

    private RoleAssignmentsHelper() {
    }

    public static String emptyRoleAssignmentResponseJson() {
        return "{ \"roleAssignmentResponse\" : []}";
    }

    public static String roleAssignmentResponseJson(String... roleAssignmentsAsJson) {
        return "{ \"roleAssignmentResponse\" : ["
            + String.join(",", roleAssignmentsAsJson)
            + "]}";
    }

    public static String roleAssignmentJson(String roleName, String jurisdiction, String caseType, String caseId) {
        return "        {"
               + "          \"id\": \"e6fc5ebb-63e3-4613-9cfc-b3f9b1559571\","
               + "          \"actorIdType\": \"IDAM\","
               + "          \"actorId\": \"123\","
               + "          \"roleType\": \"CASE\","
               + "          \"roleName\": \"" + roleName + "\","
               + "          \"classification\": \"PUBLIC\","
               + "          \"grantType\": \"STANDARD\","
               + "          \"roleCategory\": \"JUDICIAL\","
               + "          \"readOnly\": false,"
               + "          \"beginTime\": \"2021-02-01T00:00:00Z\","
               + "          \"endTime\": \"2122-01-01T00:00:00Z\","
               + "          \"created\": \"2020-12-23T06:37:58.096065Z\","
               + "          \"attributes\": {"
               + "            \"jurisdiction\": \"" + jurisdiction + "\","
               + "            \"caseType\": \"" + caseType + "\","
               + "            \"caseId\": \"" + caseId + "\""
               + "          },"
               + "          \"authorisations\": []"
               + "        }";
    }

    public static String organisationalRoleAssignmentJson(String roleName, String jurisdiction, String caseType,
                                                          String caseId) {
        return "        {"
               + "          \"id\": \"e6fc5ebb-63e3-4613-9cfc-b3f9b1559571\","
               + "          \"actorIdType\": \"IDAM\","
               + "          \"actorId\": \"123\","
               + "          \"roleType\": \"ORGANISATION\","
               + "          \"roleName\": \"" + roleName + "\","
               + "          \"classification\": \"PUBLIC\","
               + "          \"grantType\": \"STANDARD\","
               + "          \"roleCategory\": \"JUDICIAL\","
               + "          \"readOnly\": false,"
               + "          \"beginTime\": \"2021-02-01T00:00:00Z\","
               + "          \"endTime\": \"2122-01-01T00:00:00Z\","
               + "          \"created\": \"2020-12-23T06:37:58.096065Z\","
               + "          \"attributes\": {"
               + "            \"jurisdiction\": \"" + jurisdiction + "\","
               + "            \"caseType\": \"" + caseType + "\","
               + "            \"caseId\": \"" + caseId + "\""
               + "          },"
               + "          \"authorisations\": []"
               + "        }";
    }

    public static String userRoleAssignmentJson(String actorId, String roleName, String caseId) {
        return userRoleAssignmentJson(actorId, roleName, caseId, Classification.PUBLIC);
    }

    public static String userRoleAssignmentJson(String actorId, String roleName, String caseId,
                                                Classification classification) {
        return "        {"
               + "          \"id\": \"e6fc5ebb-63e3-4613-9cfc-b3f9b1559571\","
               + "          \"actorIdType\": \"IDAM\","
               + "          \"actorId\": \"" + actorId + "\","
               + "          \"roleType\": \"CASE\","
               + "          \"roleName\": \"" + roleName + "\","
               + "          \"classification\": \"" + classification.name() + "\","
               + "          \"grantType\": \"STANDARD\","
               + "          \"roleCategory\": \"JUDICIAL\","
               + "          \"readOnly\": false,"
               + "          \"beginTime\": \"2021-02-01T00:00:00Z\","
               + "          \"endTime\": \"2122-01-01T00:00:00Z\","
               + "          \"created\": \"2020-12-23T06:37:58.096065Z\","
               + "          \"attributes\": {"
               + "            \"jurisdiction\": \"PROBATE\","
               + "            \"caseType\": \"TestAddressBookCase\","
               + "            \"caseId\": \"" + caseId + "\""
               + "          },"
               + "          \"authorisations\": []"
               + "        }";
    }

    public static String caseTypeRoleAssignmentJson(String actorId, String roleName, String caseId, String jurisdiction,
                                                    String caseType, Classification classification,
                                                    GrantType grantType) {
        return "        {"
               + "          \"id\": \"e6fc5ebb-63e3-4613-9cfc-b3f9b1559571\","
               + "          \"actorIdType\": \"IDAM\","
               + "          \"actorId\": \"" + actorId + "\","
               + "          \"roleType\": \"CASE\","
               + "          \"roleName\": \"" + roleName + "\","
               + "          \"classification\": \"" + classification.name() + "\","
               + "          \"grantType\": \"" + grantType.name() + "\","
               + "          \"roleCategory\": \"JUDICIAL\","
               + "          \"readOnly\": false,"
               + "          \"beginTime\": \"2021-02-01T00:00:00Z\","
               + "          \"endTime\": \"2122-01-01T00:00:00Z\","
               + "          \"created\": \"2020-12-23T06:37:58.096065Z\","
               + "          \"attributes\": {"
               + "            \"jurisdiction\": \"" + jurisdiction + "\","
               + "            \"caseType\": \"" + caseType + "\","
               + "            \"caseId\": \"" + caseId + "\""
               + "          },"
               + "          \"authorisations\": []"
               + "        }";
    }

    public static String restrictedSecurityCTSpecificPublicUserRoleAssignmentJson(String actorId, String roleName,
                                                                                  String caseId) {
        return caseTypeRoleAssignmentJson(actorId, roleName, caseId, "AUTOTEST1", "RESTRICTED_SECURITY",
            Classification.PUBLIC, GrantType.SPECIFIC);
    }

    public static String mapperCTSpecificPublicUserRoleAssignmentJson(String actorId, String roleName, String caseId) {
        return caseTypeRoleAssignmentJson(actorId, roleName, caseId, "AUTOTEST2", "MAPPER",
            Classification.PUBLIC, GrantType.SPECIFIC);
    }

    public static String aatCTSpecificPublicUserRoleAssignmentJson(String actorId, String roleName, String caseId) {
        return caseTypeRoleAssignmentJson(actorId, roleName, caseId, "AUTOTEST1", "AAT",
            Classification.PUBLIC, GrantType.SPECIFIC);
    }

    public static String securityCTSpecificPublicUserRoleAssignmentJson(String actorId,
                                                                        String roleName,
                                                                        String caseId) {
        return caseTypeRoleAssignmentJson(actorId, roleName, caseId, "AUTOTEST1", "SECURITY",
            Classification.PUBLIC, GrantType.SPECIFIC);
    }

    public static String securityCTSpecificRestrictedUserRoleAssignmentJson(String actorId,
                                                                            String roleName,
                                                                            String caseId) {
        return caseTypeRoleAssignmentJson(actorId, roleName, caseId, "AUTOTEST1", "SECURITY",
            Classification.RESTRICTED, GrantType.SPECIFIC);
    }

    public static String securityCTSpecificPrivateUserRoleAssignmentJson(String actorId,
                                                                         String roleName,
                                                                         String caseId) {
        return caseTypeRoleAssignmentJson(actorId, roleName, caseId, "AUTOTEST1", "SECURITY",
            Classification.PRIVATE, GrantType.SPECIFIC);
    }

    public static RoleAssignmentRequestResponse createRoleAssignmentRequestResponse(
        List<RoleAssignmentResource> requestedRoles) {

        RoleAssignmentRequestResource roleAssignmentResponse = RoleAssignmentRequestResource
            .builder().requestedRoles(requestedRoles).build();

        return RoleAssignmentRequestResponse.builder()
            .roleAssignmentResponse(roleAssignmentResponse)
            .build();
    }

    public static RoleAssignmentResource createRoleAssignmentRecord(String id,
                                                                    String caseId,
                                                                    String roleName,
                                                                    String userId) {
        return createRoleAssignmentRecord(id, caseId, null, null, roleName,userId, true);
    }

    public static RoleAssignmentResource createRoleAssignmentRecord(String id,
                                                                    String caseId,
                                                                    String caseType,
                                                                    String jurisdiction,
                                                                    String roleName,
                                                                    String userId,
                                                                    boolean localised) {
        return RoleAssignmentResource.builder()
            .id(id)
            .actorIdType(ActorIdType.IDAM.name())
            .actorId(userId)
            .roleType(RoleType.CASE.name())
            .roleName(roleName)
            .classification(Classification.PUBLIC.name())
            .grantType(GrantType.SPECIFIC.name())
            .roleCategory(RoleCategory.JUDICIAL.name())
            .readOnly(false)
            .beginTime(BEGIN_TIME)
            .endTime(END_TIME)
            .created(CREATED)
            .authorisations(Collections.emptyList())
            .attributes(createRoleAssignmentRecordAttribute(caseId, caseType, jurisdiction, localised, null))
            .build();
    }

    public static RoleAssignmentResource createRoleAssignmentRecord(String id, String caseId) {
        return RoleAssignmentResource.builder()
            .id(id)
            .actorIdType(ActorIdType.IDAM.name())
            .actorId("aecfec12-1f9a-40cb-bd8c-7a9f3506e67c")
            .roleType(RoleType.CASE.name())
            .roleName("judiciary")
            .classification(Classification.PUBLIC.name())
            .grantType(GrantType.STANDARD.name())
            .roleCategory(RoleCategory.JUDICIAL.name())
            .readOnly(false)
            .beginTime(BEGIN_TIME)
            .endTime(END_TIME)
            .created(CREATED)
            .authorisations(Collections.emptyList())
            .attributes(createRoleAssignmentRecordAttribute(caseId, null, null, true, null))
            .build();
    }

    public static RoleAssignmentResource createRoleAssignmentRecord(String id, String caseId, String caseGroupId) {
        return RoleAssignmentResource.builder()
            .id(id)
            .actorIdType(ActorIdType.IDAM.name())
            .actorId("aecfec12-1f9a-40cb-bd8c-7a9f3506e67c")
            .roleType(RoleType.CASE.name())
            .roleName("judiciary")
            .classification(Classification.PUBLIC.name())
            .grantType(GrantType.STANDARD.name())
            .roleCategory(RoleCategory.JUDICIAL.name())
            .readOnly(false)
            .beginTime(BEGIN_TIME)
            .endTime(END_TIME)
            .created(CREATED)
            .authorisations(Collections.emptyList())
            .attributes(createRoleAssignmentRecordAttribute(caseId, null, null, true, caseGroupId))
            .build();
    }

    public static RoleAssignmentResponse createRoleAssignmentResponse(
        List<RoleAssignmentResource> roleAssignments) {
        return RoleAssignmentResponse.builder()
            .roleAssignments(roleAssignments)
            .build();
    }

    public static RoleToAccessProfileDefinition roleToAccessProfileDefinition(String caseRole) {
        return RoleToAccessProfileDefinition.builder()
            .disabled(false)
            .readOnly(false)
            .accessProfiles(caseRole)
            .roleName(caseRole).build();
    }

    public static CaseTypeDefinition enhanceGetCaseTypeStubWithAccessProfiles(
        String caseTypeJsonFile, RoleToAccessProfileDefinition... accessProfiles) {
        CaseTypeDefinition caseTypeDefinition = loadCaseTypeDefinition("mappings/" + caseTypeJsonFile);
        caseTypeDefinition.setRoleToAccessProfiles(Arrays.asList(accessProfiles.clone()));
        return caseTypeDefinition;
    }

    private static RoleAssignmentAttributesResource createRoleAssignmentRecordAttribute(String caseId,
                                                                                        String caseType,
                                                                                        String jurisdiction,
                                                                                        boolean localised,
                                                                                        String caseGroupId) {
        if (localised) {
            return RoleAssignmentAttributesResource.builder()
                .jurisdiction(Optional.of(jurisdiction == null ? "DIVORCE" : jurisdiction))
                .caseId(Optional.of(caseId))
                .caseType(Optional.of(caseType == null ? "FT_Tabs" : caseType))
                .region(Optional.of("Hampshire"))
                .location(Optional.of("Southampton"))
                .contractType(Optional.of("SALARIED")) // SALARIED, FEEPAY
                .caseAccessGroupId(Optional.ofNullable(caseGroupId))
                .build();
        } else {
            return RoleAssignmentAttributesResource.builder()
                .jurisdiction(Optional.of(jurisdiction == null ? "DIVORCE" : jurisdiction))
                .caseId(Optional.of(caseId))
                .caseType(Optional.of(caseType == null ? "FT_Tabs" : caseType))
                .contractType(Optional.of("SALARIED")) // SALARIED, FEEPAY
                .region(Optional.empty())
                .location(Optional.empty())
                .caseAccessGroupId(Optional.ofNullable(caseGroupId))
                .build();
        }
    }

    private static RoleAssignmentAttributesResource createRoleAssignmentRecordAttribute(String caseId) {
        return RoleAssignmentAttributesResource.builder()
            .jurisdiction(Optional.of("PROBATE"))
            .caseId(Optional.of(caseId))
            .caseType(Optional.of("TestAddressBookNoEventAccessToCaseRole"))
            .contractType(Optional.of("SALARIED")) // SALARIED, FEEPAY
            .build();
    }
}
