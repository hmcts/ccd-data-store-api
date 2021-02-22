package uk.gov.hmcts.ccd.domain.service.accessprofile.filter;

import com.google.common.collect.Lists;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentAttributes;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignments;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FilterRoleAssignmentsImplTest {

    private static final String JURISDICTION_1 = "JURISDICTION_1";
    private static final String JURISDICTION_2 = "JURISDICTION_2";
    private static final String CASE_ID_1 = "CASE_ID_1";
    private static final String CASE_ID_2 = "CASE_ID_2";

    private FilterRoleAssignmentsImpl classUnderTest;

    @BeforeEach
    void setUp() {
        classUnderTest = new FilterRoleAssignmentsImpl();
    }

    @Test
    void shouldFilerBasedOnDateCaseIDJurisdiction() {
        RoleAssignments roleAssignments = mockRoleAssignments();
        CaseDetails caseDetails = mockCaseDetails();
        RoleAssignments filteredRoleAssignments = classUnderTest.filter(roleAssignments, caseDetails);
        assertEquals(1, filteredRoleAssignments.getRoleAssignments().size());
    }


    @Test
    void shouldFilerBasedOnSecurityClassificationWhenCaseClassificationIsLess() {
        RoleAssignments roleAssignments = mockRoleAssignmentsOnSecurityClassification();
        CaseDetails caseDetails = mockCaseDetails();
        RoleAssignments filteredRoleAssignments = classUnderTest.filter(roleAssignments, caseDetails);
        assertEquals(2, filteredRoleAssignments.getRoleAssignments().size());
    }

    @Test
    void shouldFilerBasedOnSecurityClassificationWhenCaseClassificationIsMore() {
        RoleAssignments roleAssignments = mockRoleAssignmentsOnSecurityClassification();
        CaseDetails caseDetails = mockCaseDetails(SecurityClassification.PRIVATE);
        RoleAssignments filteredRoleAssignments = classUnderTest.filter(roleAssignments, caseDetails);
        assertEquals(2, filteredRoleAssignments.getRoleAssignments().size());
    }

    @Test
    void shouldFilerBasedOnSecurityClassificationWhenCaseClassificationIsRestricted() {
        RoleAssignments roleAssignments = mockRoleAssignmentsOnSecurityClassification();
        CaseDetails caseDetails = mockCaseDetails(SecurityClassification.RESTRICTED);
        RoleAssignments filteredRoleAssignments = classUnderTest.filter(roleAssignments, caseDetails);
        assertEquals(1, filteredRoleAssignments.getRoleAssignments().size());
    }

    @Test
    void shouldFilerBasedOnStartDateAndEndDate() {
        RoleAssignments roleAssignments = mockRoleAssignmentsDatesNotMatching();
        CaseDetails caseDetails = mockCaseDetails();
        RoleAssignments filteredRoleAssignments = classUnderTest.filter(roleAssignments, caseDetails);
        assertEquals(0, filteredRoleAssignments.getRoleAssignments().size());
    }

    private CaseDetails mockCaseDetails() {
        return mockCaseDetails(SecurityClassification.PUBLIC);
    }

    private CaseDetails mockCaseDetails(SecurityClassification securityClassification) {
        CaseDetails caseDetails = mock(CaseDetails.class);
        when(caseDetails.getSecurityClassification()).thenReturn(securityClassification);
        when(caseDetails.getReferenceAsString()).thenReturn(CASE_ID_1);
        when(caseDetails.getJurisdiction()).thenReturn(JURISDICTION_1);
        return caseDetails;
    }

    private RoleAssignments mockRoleAssignments() {
        RoleAssignments roleAssignments = mock(RoleAssignments.class);

        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_1, JURISDICTION_1);
        RoleAssignment roleAssignment2 = createRoleAssignment(CASE_ID_2, JURISDICTION_2);

        List<RoleAssignment> roleAssignmentList = Lists.newArrayList(roleAssignment, roleAssignment2);

        when(roleAssignments.getRoleAssignments()).thenReturn(roleAssignmentList);

        return roleAssignments;
    }

    private RoleAssignments mockRoleAssignmentsOnSecurityClassification() {
        RoleAssignments roleAssignments = mock(RoleAssignments.class);

        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_1, JURISDICTION_1,
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS), "PRIVATE");
        RoleAssignment roleAssignment2 = createRoleAssignment(CASE_ID_1, JURISDICTION_1,
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS), "RESTRICTED");

        List<RoleAssignment> roleAssignmentList = Lists.newArrayList(roleAssignment, roleAssignment2);

        when(roleAssignments.getRoleAssignments()).thenReturn(roleAssignmentList);

        return roleAssignments;
    }

    private RoleAssignments mockRoleAssignmentsDatesNotMatching() {
        RoleAssignments roleAssignments = mock(RoleAssignments.class);

        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_1, JURISDICTION_1,
            Instant.now().plus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS), "PUBLIC");
        RoleAssignment roleAssignment2 = createRoleAssignment(CASE_ID_2, JURISDICTION_2,
            Instant.now().plus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS)
            , "PUBLIC");

        List<RoleAssignment> roleAssignmentList = Lists.newArrayList(roleAssignment, roleAssignment2);

        when(roleAssignments.getRoleAssignments()).thenReturn(roleAssignmentList);

        return roleAssignments;
    }

    private RoleAssignment createRoleAssignment(String caseId, String jurisdiction) {
        return createRoleAssignment(caseId, jurisdiction,
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(1, ChronoUnit.DAYS),
            "PUBLIC");
    }

    private RoleAssignment createRoleAssignment(String caseId, String jurisdiction,
                                                Instant startDate,
                                                Instant endDate,
                                                String securityClassification) {
        RoleAssignment roleAssignment = new RoleAssignment();

        roleAssignment.setActorId("Actor1");
        roleAssignment.setRoleName("RoleName1");
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
        RoleAssignmentAttributes roleAssignmentAttributes = new RoleAssignmentAttributes();

        roleAssignmentAttributes.setCaseId(caseId);
        roleAssignmentAttributes.setJurisdiction(jurisdiction);
        roleAssignmentAttributes.setContractType("SALARIED");
        roleAssignmentAttributes.setLocation("");
        roleAssignmentAttributes.setRegion("");
        roleAssignment.setAttributes(roleAssignmentAttributes);

        return roleAssignment;
    }
}
