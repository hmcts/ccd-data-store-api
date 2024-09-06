package uk.gov.hmcts.ccd.domain.service.accessprofile.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentAttributes;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.ActorIdType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.Classification;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.RoleCategory;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.RoleType;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessGroupUtils;
import uk.gov.hmcts.ccd.domain.service.common.CaseDataService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;

public class BaseFilter {

    protected static final String JURISDICTION_1 = "JURISDICTION_1";
    protected static final String JURISDICTION_2 = "JURISDICTION_2";
    protected static final String CASE_ID_1 = "CASE_ID_1";
    protected static final String CASE_ID_2 = "CASE_ID_2";
    protected static final String CASE_TYPE_ID_1 = "CASE_TYPE_ID_1";
    protected static final String ROLE_NAME_1 = "RoleName1";

    protected RoleAssignment createRoleAssignmentWithRoleName(String roleName) {
        RoleAssignment ra = createRoleAssignment(CASE_ID_1, JURISDICTION_1);
        ra.setRoleName(roleName);
        return ra;
    }

    protected RoleAssignment createRoleAssignmentWithGrantType(String grantType) {
        RoleAssignment ra = createRoleAssignment(CASE_ID_1, JURISDICTION_1);
        ra.setGrantType(grantType);
        return ra;
    }

    protected RoleAssignment createRoleAssignment(String caseId,
                                                  String jurisdiction) {
        return createRoleAssignment(caseId, jurisdiction,
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(1, ChronoUnit.DAYS),
            Classification.PUBLIC.name());
    }

    protected RoleAssignment createRoleAssignment(String caseId,
                                                  String jurisdiction,
                                                  Instant startDate,
                                                  Instant endDate,
                                                  String securityClassification) {
        return createRoleAssignment(
            caseId,
            jurisdiction,
            startDate,
            endDate,
            securityClassification,
            Optional.of(""),
            Optional.of("")
        );
    }

    protected RoleAssignment createRoleAssignment(String caseId,
                                                  String jurisdiction,
                                                  Instant startDate,
                                                  Instant endDate,
                                                  String securityClassification,
                                                  String caseAccessGroupId) {

        return createRoleAssignment(
            startDate,
            endDate,
            securityClassification,
            Optional.of(caseId),
            Optional.of(jurisdiction),
            Optional.of(""),
            Optional.of(""),
            Optional.of(caseAccessGroupId)
        );
    }


    protected RoleAssignment createRoleAssignment(String caseId,
                                                  String jurisdiction,
                                                  Instant startDate,
                                                  Instant endDate,
                                                  String securityClassification,
                                                  Optional<String> region,
                                                  Optional<String> location) {
        return createRoleAssignment(
            startDate,
            endDate,
            securityClassification,
            Optional.of(caseId),
            Optional.of(jurisdiction),
            region,
            location
        );
    }

    protected RoleAssignment createRoleAssignment(Instant startDate,
                                                  Instant endDate,
                                                  String securityClassification,
                                                  Optional<String> caseId,
                                                  Optional<String> jurisdiction,
                                                  Optional<String> region,
                                                  Optional<String> location) {
        RoleAssignment roleAssignment = RoleAssignment.builder().build();

        roleAssignment.setActorId("Actor1");
        roleAssignment.setRoleName(ROLE_NAME_1);
        roleAssignment.setActorIdType(ActorIdType.IDAM.name());
        roleAssignment.setRoleType(RoleType.ORGANISATION.name());
        roleAssignment.setClassification(securityClassification);
        roleAssignment.setGrantType(GrantType.BASIC.name());
        roleAssignment.setRoleCategory(RoleCategory.JUDICIAL.name());
        roleAssignment.setReadOnly(false);
        roleAssignment.setBeginTime(startDate);
        roleAssignment.setEndTime(endDate);
        roleAssignment.setCreated(Instant.now());
        roleAssignment.setAuthorisations(Lists.newArrayList());

        // role assignment attributes
        RoleAssignmentAttributes roleAssignmentAttributes = createRoleAssignmentAttributes(caseId,
            jurisdiction,
            region,
            location);

        roleAssignment.setAttributes(roleAssignmentAttributes);
        return roleAssignment;
    }

    protected RoleAssignment createRoleAssignment(Instant startDate,
                                                  Instant endDate,
                                                  String securityClassification,
                                                  Optional<String> caseId,
                                                  Optional<String> jurisdiction,
                                                  Optional<String> region,
                                                  Optional<String> location,
                                                  Optional<String> caseAccessGroupId) {
        RoleAssignment roleAssignment = RoleAssignment.builder().build();

        roleAssignment.setActorId("Actor1");
        roleAssignment.setRoleName(ROLE_NAME_1);
        roleAssignment.setActorIdType(ActorIdType.IDAM.name());
        roleAssignment.setRoleType(RoleType.ORGANISATION.name());
        roleAssignment.setClassification(securityClassification);
        roleAssignment.setGrantType(GrantType.BASIC.name());
        roleAssignment.setRoleCategory(RoleCategory.JUDICIAL.name());
        roleAssignment.setReadOnly(false);
        roleAssignment.setBeginTime(startDate);
        roleAssignment.setEndTime(endDate);
        roleAssignment.setCreated(Instant.now());
        roleAssignment.setAuthorisations(Lists.newArrayList());

        // role assignment attributes
        RoleAssignmentAttributes roleAssignmentAttributes = createRoleAssignmentAttributes(caseId,
            jurisdiction,
            region,
            location,
            caseAccessGroupId);

        roleAssignment.setAttributes(roleAssignmentAttributes);
        return roleAssignment;
    }

    private RoleAssignmentAttributes createRoleAssignmentAttributes(Optional<String> caseId,
                                                                    Optional<String> jurisdiction,
                                                                    Optional<String> region,
                                                                    Optional<String> location) {
        // role assignment attributes
        RoleAssignmentAttributes roleAssignmentAttributes = new RoleAssignmentAttributes();

        roleAssignmentAttributes.setCaseId(caseId);
        roleAssignmentAttributes.setJurisdiction(jurisdiction);
        roleAssignmentAttributes.setContractType(Optional.of("SALARIED"));
        roleAssignmentAttributes.setLocation(location);
        roleAssignmentAttributes.setRegion(region);
        roleAssignmentAttributes.setCaseType(Optional.of("TEST_CASE_TYPE"));
        return roleAssignmentAttributes;
    }

    private RoleAssignmentAttributes createRoleAssignmentAttributes(Optional<String> caseId,
                                                                    Optional<String> jurisdiction,
                                                                    Optional<String> region,
                                                                    Optional<String> location,
                                                                    Optional<String> caseAccessGroupId) {
        // role assignment attributes
        RoleAssignmentAttributes roleAssignmentAttributes = new RoleAssignmentAttributes();

        roleAssignmentAttributes.setCaseId(caseId);
        roleAssignmentAttributes.setJurisdiction(jurisdiction);
        roleAssignmentAttributes.setContractType(Optional.of("SALARIED"));
        roleAssignmentAttributes.setLocation(location);
        roleAssignmentAttributes.setRegion(region);
        roleAssignmentAttributes.setCaseType(Optional.of("TEST_CASE_TYPE"));
        roleAssignmentAttributes.setCaseAccessGroupId(caseAccessGroupId);
        return roleAssignmentAttributes;
    }

    protected CaseDetails mockCaseDetails() {
        return mockCaseDetails(SecurityClassification.PUBLIC);
    }

    protected CaseDetails mockCaseDetails(SecurityClassification securityClassification) {
        return mockCaseDetails(securityClassification, JURISDICTION_1);
    }


    protected CaseDetails mockCaseDetails(Map<String,JsonNode> caseAccessGroups, CaseDataService caseDataService,
                                          CaseTypeDefinition caseTypeDefinition, ObjectMapper objectMapper) {
        return mockCaseDetails(SecurityClassification.PUBLIC,caseAccessGroups, JURISDICTION_1,
            caseDataService, caseTypeDefinition, objectMapper);
    }

    protected CaseDetails mockCaseDetails(SecurityClassification securityClassification,
                                          Map<String,JsonNode> caseAccessGroups, CaseDataService caseDataService,
                                          CaseTypeDefinition caseTypeDefinition, ObjectMapper objectMapper) {
        CaseDetails caseDetails = mock(CaseDetails.class);

        CaseAccessGroupUtils caseAccessGroupUtils = new CaseAccessGroupUtils(caseDataService, objectMapper);
        Map<String, JsonNode> caseDataClassificationWithCaseAccessGroup =
            caseAccessGroupUtils.updateCaseDataClassificationWithCaseGroupAccess(
                caseDetails, caseTypeDefinition);

        when(caseDetails.getSecurityClassification()).thenReturn(securityClassification);
        when(caseDetails.getReferenceAsString()).thenReturn(CASE_ID_1);
        when(caseDetails.getCaseTypeId()).thenReturn("TEST_CASE_TYPE");
        when(caseDetails.getData()).thenReturn(caseAccessGroups);
        when(caseDetails.getDataClassification()).thenReturn(caseDataClassificationWithCaseAccessGroup);
        return caseDetails;
    }

    protected CaseDetails mockCaseDetails(SecurityClassification securityClassification,
                                          Map<String,JsonNode> caseAccessGroups, String jurisdiction,
                                          CaseDataService caseDataService, CaseTypeDefinition caseTypeDefinition,
                                          ObjectMapper objectMapper) {

        CaseDetails caseDetails = mock(CaseDetails.class);

        when(caseDetails.getSecurityClassification()).thenReturn(securityClassification);
        when(caseDetails.getReferenceAsString()).thenReturn(CASE_ID_1);
        when(caseDetails.getJurisdiction()).thenReturn(jurisdiction);
        when(caseDetails.getCaseTypeId()).thenReturn("TEST_CASE_TYPE");
        when(caseDetails.getData()).thenReturn(caseAccessGroups);
        when(caseDetails.getDataClassification()).thenReturn(caseAccessGroups);

        mockGetDefaultSecurityClassificationsResponse(caseDataService, caseDetails);

        CaseAccessGroupUtils caseAccessGroupUtils = new CaseAccessGroupUtils(caseDataService, objectMapper);
        Map<String, JsonNode> caseDataClassificationWithCaseAccessGroup =
            caseAccessGroupUtils.updateCaseDataClassificationWithCaseGroupAccess(
                caseDetails, caseTypeDefinition);

        when(caseDetails.getDataClassification()).thenReturn(caseDataClassificationWithCaseAccessGroup);
        return caseDetails;
    }

    protected CaseDetails mockCaseDetails(SecurityClassification securityClassification, String jurisdiction) {
        CaseDetails caseDetails = mock(CaseDetails.class);
        when(caseDetails.getSecurityClassification()).thenReturn(securityClassification);
        when(caseDetails.getReferenceAsString()).thenReturn(CASE_ID_1);
        when(caseDetails.getJurisdiction()).thenReturn(jurisdiction);
        when(caseDetails.getCaseTypeId()).thenReturn("TEST_CASE_TYPE");
        return caseDetails;
    }

    protected CaseTypeDefinition mockCaseTypeDefinition() {
        return mockCaseTypeDefinition(SecurityClassification.PUBLIC);
    }

    protected CaseTypeDefinition mockCaseTypeDefinition(SecurityClassification securityClassification) {
        return mockCaseTypeDefinition(securityClassification, JURISDICTION_1);
    }

    protected CaseTypeDefinition mockCaseTypeDefinition(SecurityClassification securityClassification,
                                                 String jurisdiction) {
        CaseTypeDefinition caseTypeDefinition = mock(CaseTypeDefinition.class);
        when(caseTypeDefinition.getSecurityClassification()).thenReturn(securityClassification);
        when(caseTypeDefinition.getId()).thenReturn(CASE_TYPE_ID_1);
        when(caseTypeDefinition.getJurisdictionId()).thenReturn(jurisdiction);
        return caseTypeDefinition;
    }

    private void mockGetDefaultSecurityClassificationsResponse(CaseDataService caseDataService,
                                                               CaseDetails caseDetails) {
        JsonNode caseAccessGroupJsonNode = caseDetails.getData().get(CaseAccessGroupUtils.CASE_ACCESS_GROUPS);
        Map<String, JsonNode> dataClassification = new HashMap<>();
        dataClassification.put(CaseAccessGroupUtils.CASE_ACCESS_GROUPS, caseAccessGroupJsonNode);

        doReturn(dataClassification).when(caseDataService).getDefaultSecurityClassifications(
            any(), any(), any()
        );
    }
}
