package uk.gov.hmcts.ccd.domain.service.accessprofile.filter.matcher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentFilteringResult;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleMatchingResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.accessprofile.filter.BaseFilter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


class BeginDateEndDateMatcherTest extends BaseFilter {

    private BeginDateEndDateMatcher classUnderTest;

    @BeforeEach
    void setUp() {
        classUnderTest = new BeginDateEndDateMatcher();
    }

    @Test
    void shouldMatchWhenCurrentDateIsWithInBeginDateAndEndDate() {
        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_1, JURISDICTION_1,
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS),
            "PRIVATE", null, null);

        RoleAssignmentFilteringResult result = new RoleAssignmentFilteringResult(roleAssignment,
            new RoleMatchingResult());

        CaseDetails caseDetails = mockCaseDetails();
        boolean matched = classUnderTest.matchAttribute(result, caseDetails);
        assertTrue(matched);
        assertTrue(result.getRoleMatchingResult().isValidDate());
    }

    @Test
    void shouldNotMatchWhenBeginDateIsNull() {
        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_2, JURISDICTION_1,
           null,
            Instant.now().plus(2, ChronoUnit.DAYS),
            "PRIVATE", null, null);

        RoleAssignmentFilteringResult result = new RoleAssignmentFilteringResult(roleAssignment,
            new RoleMatchingResult());

        CaseDetails caseDetails = mockCaseDetails(SecurityClassification.RESTRICTED, JURISDICTION_2);
        boolean matched = classUnderTest.matchAttribute(result, caseDetails);
        assertFalse(matched);
        assertFalse(result.getRoleMatchingResult().isValidDate());
    }

    @Test
    void shouldNotMatchWhenEndDateIsNull() {
        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_2, JURISDICTION_1,
            Instant.now().plus(1, ChronoUnit.DAYS),
            null,
            "PRIVATE", null, null);

        RoleAssignmentFilteringResult result = new RoleAssignmentFilteringResult(roleAssignment,
            new RoleMatchingResult());

        CaseDetails caseDetails = mockCaseDetails(SecurityClassification.RESTRICTED, JURISDICTION_2);
        boolean matched = classUnderTest.matchAttribute(result, caseDetails);
        assertFalse(matched);
        assertFalse(result.getRoleMatchingResult().isValidDate());
    }

    @Test
    void shouldNotMatchWhenBeginDateAndEndDateAreBeforeCurrent() {
        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_2, JURISDICTION_1,
            Instant.now().minus(3, ChronoUnit.DAYS),
            Instant.now().minus(1, ChronoUnit.DAYS),
            "PRIVATE", null, null);

        RoleAssignmentFilteringResult result = new RoleAssignmentFilteringResult(roleAssignment,
            new RoleMatchingResult());

        CaseDetails caseDetails = mockCaseDetails(SecurityClassification.RESTRICTED, JURISDICTION_2);
        boolean matched = classUnderTest.matchAttribute(result, caseDetails);
        assertFalse(matched);
        assertFalse(result.getRoleMatchingResult().isValidDate());
    }

    @Test
    void shouldNotMatchWhenBeginDateAndEndDateAreAfterCurrent() {
        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_2, JURISDICTION_1,
            Instant.now().plus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS),
            "PRIVATE", null, null);

        RoleAssignmentFilteringResult result = new RoleAssignmentFilteringResult(roleAssignment,
            new RoleMatchingResult());

        CaseDetails caseDetails = mockCaseDetails(SecurityClassification.RESTRICTED, JURISDICTION_2);
        boolean matched = classUnderTest.matchAttribute(result, caseDetails);
        assertFalse(matched);
        assertFalse(result.getRoleMatchingResult().isValidDate());
    }
}
