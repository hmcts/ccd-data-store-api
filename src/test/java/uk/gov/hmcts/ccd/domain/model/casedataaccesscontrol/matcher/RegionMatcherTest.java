package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentFilteringResult;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleMatchingResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.accessprofile.filter.BaseFilter;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RegionMatcherTest extends BaseFilter {

    private RegionMatcher classUnderTest;

    @BeforeEach
    void setUp() {
        classUnderTest = new RegionMatcher();
    }

    @Test
    void shouldMatchWhenRegionIsNull() {
        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_1, JURISDICTION_1,
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS),
            "PRIVATE", null, Optional.of(""));
        RoleAssignmentFilteringResult result = new RoleAssignmentFilteringResult(roleAssignment,
            new RoleMatchingResult());
        CaseDetails caseDetails = mockCaseDetails();
        classUnderTest.matchAttribute(result, caseDetails);
        assertTrue(result.getRoleMatchingResult().isRegionMatched());
    }

    @Test
    void shouldMatchWhenRegionIsEmptyOnCaseDetailsAndRoleAssignment() {
        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_1, JURISDICTION_1,
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS),
            "PRIVATE", Optional.of(""), Optional.of(""));
        RoleAssignmentFilteringResult result = new RoleAssignmentFilteringResult(roleAssignment,
            new RoleMatchingResult());
        CaseDetails caseDetails = mockCaseDetails();
        classUnderTest.matchAttribute(result, caseDetails);
        assertTrue(result.getRoleMatchingResult().isRegionMatched());
    }

    @Test
    void shouldNotMatchWhenRegionIsNotEmptyOnRoleAssignmentAndEmptyOnCaseDetails() {
        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_1, JURISDICTION_1,
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS),
            "PRIVATE", Optional.of("England"), Optional.of(""));
        RoleAssignmentFilteringResult result = new RoleAssignmentFilteringResult(roleAssignment,
            new RoleMatchingResult());
        CaseDetails caseDetails = mockCaseDetails();
        classUnderTest.matchAttribute(result, caseDetails);
        assertFalse(result.getRoleMatchingResult().isRegionMatched());
    }

}
