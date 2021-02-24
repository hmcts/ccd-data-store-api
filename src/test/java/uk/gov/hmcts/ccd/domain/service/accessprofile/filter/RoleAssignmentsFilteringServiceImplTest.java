package uk.gov.hmcts.ccd.domain.service.accessprofile.filter;

import com.google.common.collect.Lists;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentFilteringResult;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignments;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.accessprofile.filter.matcher.AttributeMatcher;
import uk.gov.hmcts.ccd.domain.service.accessprofile.filter.matcher.BeginDateEndDateMatcher;
import uk.gov.hmcts.ccd.domain.service.accessprofile.filter.matcher.CaseIdMatcher;
import uk.gov.hmcts.ccd.domain.service.accessprofile.filter.matcher.JurisdictionMatcher;
import uk.gov.hmcts.ccd.domain.service.accessprofile.filter.matcher.LocationMatcher;
import uk.gov.hmcts.ccd.domain.service.accessprofile.filter.matcher.RegionMatcher;
import uk.gov.hmcts.ccd.domain.service.accessprofile.filter.matcher.SecurityClassificationMatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoleAssignmentsFilteringServiceImplTest extends BaseFilter {

    private RoleAssignmentsFilteringServiceImpl classUnderTest;

    @BeforeEach
    void setUp() {
        List<AttributeMatcher> attributeMatchers = Lists.newArrayList(new BeginDateEndDateMatcher(),
            new CaseIdMatcher(),
            new JurisdictionMatcher(),
            new LocationMatcher(),
            new RegionMatcher(),
            new SecurityClassificationMatcher());
        classUnderTest = new RoleAssignmentsFilteringServiceImpl(attributeMatchers);
    }

    @Test
    void shouldFilerBasedOnDateCaseIDJurisdiction() {
        RoleAssignments roleAssignments = mockRoleAssignments();
        CaseDetails caseDetails = mockCaseDetails();
        List<RoleAssignmentFilteringResult> filteredRoleAssignments = classUnderTest
            .filter(roleAssignments, caseDetails);
        assertEquals(1, filteredRoleAssignments.size());
    }


    @Test
    void shouldFilerBasedOnSecurityClassificationWhenCaseClassificationIsLess() {
        RoleAssignments roleAssignments = mockRoleAssignmentsOnSecurityClassification();
        CaseDetails caseDetails = mockCaseDetails();
        List<RoleAssignmentFilteringResult> filteredRoleAssignments = classUnderTest
            .filter(roleAssignments, caseDetails);
        assertEquals(2, filteredRoleAssignments.size());
    }

    @Test
    void shouldFilerBasedOnSecurityClassificationWhenCaseClassificationIsMore() {
        RoleAssignments roleAssignments = mockRoleAssignmentsOnSecurityClassification();
        CaseDetails caseDetails = mockCaseDetails(SecurityClassification.PRIVATE);
        List<RoleAssignmentFilteringResult> filteredRoleAssignments = classUnderTest
            .filter(roleAssignments, caseDetails);
        assertEquals(2, filteredRoleAssignments.size());
    }

    @Test
    void shouldFilerBasedOnSecurityClassificationWhenCaseClassificationIsRestricted() {
        RoleAssignments roleAssignments = mockRoleAssignmentsOnSecurityClassification();
        CaseDetails caseDetails = mockCaseDetails(SecurityClassification.RESTRICTED);
        List<RoleAssignmentFilteringResult> filteredRoleAssignments = classUnderTest
            .filter(roleAssignments, caseDetails);
        assertEquals(1, filteredRoleAssignments.size());
    }

    @Test
    void shouldFilerBasedOnStartDateAndEndDate() {
        RoleAssignments roleAssignments = mockRoleAssignmentsDatesNotMatching();
        CaseDetails caseDetails = mockCaseDetails();
        List<RoleAssignmentFilteringResult> filteredRoleAssignments = classUnderTest
            .filter(roleAssignments, caseDetails);
        assertEquals(0, filteredRoleAssignments.size());
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
            Instant.now().plus(2, ChronoUnit.DAYS), "PUBLIC");

        List<RoleAssignment> roleAssignmentList = Lists.newArrayList(roleAssignment, roleAssignment2);

        when(roleAssignments.getRoleAssignments()).thenReturn(roleAssignmentList);

        return roleAssignments;
    }
}
