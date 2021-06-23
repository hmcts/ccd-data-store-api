package uk.gov.hmcts.ccd.domain.service.accessprofile.filter;

import com.google.common.collect.Lists;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentAttributes;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BaseFilter {

    protected static final String JURISDICTION_1 = "JURISDICTION_1";
    protected static final String JURISDICTION_2 = "JURISDICTION_2";
    protected static final String CASE_ID_1 = "CASE_ID_1";
    protected static final String CASE_ID_2 = "CASE_ID_2";
    protected static final String CASE_TYPE_ID_1 = "CASE_TYPE_ID_1";
    protected static final String CASE_TYPE_ID_2 = "CASE_TYPE_ID_2";
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
            "PUBLIC");
    }

    protected RoleAssignment createRoleAssignment(String caseId,
                                                  String jurisdiction,
                                                  Instant startDate,
                                                  Instant endDate,
                                                  String securityClassification) {
        return createRoleAssignment(caseId,
            jurisdiction,
            startDate,
            endDate,
            securityClassification,
            Optional.of(""),
            Optional.of(""));
    }

    protected RoleAssignment createRoleAssignment(String caseId,
                                                  String jurisdiction,
                                                  Instant startDate,
                                                  Instant endDate,
                                                  String securityClassification,
                                                  Optional<String> region,
                                                  Optional<String> location) {
        RoleAssignment roleAssignment = RoleAssignment.builder().build();

        roleAssignment.setActorId("Actor1");
        roleAssignment.setRoleName(ROLE_NAME_1);
        roleAssignment.setActorIdType("IDAM");
        roleAssignment.setRoleType("ORGANISATION");
        roleAssignment.setClassification(securityClassification);
        roleAssignment.setGrantType("BASIC");
        roleAssignment.setRoleCategory("JUDICIAL");
        roleAssignment.setReadOnly(false);
        roleAssignment.setBeginTime(startDate);
        roleAssignment.setEndTime(endDate);
        roleAssignment.setCreated(Instant.now());
        roleAssignment.setAuthorisations(Lists.newArrayList());

        // role assignment attributes
        RoleAssignmentAttributes roleAssignmentAttributes = createRoleAssignmentAttributes(Optional.of(caseId),
            Optional.of(jurisdiction),
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
                                                  Optional<String> location) {
        RoleAssignment roleAssignment = RoleAssignment.builder().build();

        roleAssignment.setActorId("Actor1");
        roleAssignment.setRoleName(ROLE_NAME_1);
        roleAssignment.setActorIdType("IDAM");
        roleAssignment.setRoleType("ORGANISATION");
        roleAssignment.setClassification(securityClassification);
        roleAssignment.setGrantType("BASIC");
        roleAssignment.setRoleCategory("JUDICIAL");
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

    protected CaseDetails mockCaseDetails() {
        return mockCaseDetails(SecurityClassification.PUBLIC);
    }

    protected CaseDetails mockCaseDetails(SecurityClassification securityClassification) {
        return mockCaseDetails(securityClassification, JURISDICTION_1);
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
}
