package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.accessprofile.filter.BaseFilter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        CaseDetails caseDetails = mockCaseDetails();
        assertTrue(classUnderTest.matchAttribute(roleAssignment, caseDetails));
    }

    @Test
    void shouldMatchWhenRegionIsEmptyOnCaseDetailsAndRoleAssignment() {
        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_1, JURISDICTION_1,
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS),
            "PRIVATE", Optional.of(""), Optional.of(""));

        CaseDetails caseDetails = mockCaseDetails();
        assertTrue(classUnderTest.matchAttribute(roleAssignment, caseDetails));
    }

    @Test
    void shouldNotMatchWhenRegionIsNotEmptyOnRoleAssignmentAndEmptyOnCaseDetails() {
        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_1, JURISDICTION_1,
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS),
            "PRIVATE", Optional.of("England"), Optional.of(""));

        CaseDetails caseDetails = mockCaseDetails();
        assertFalse(classUnderTest.matchAttribute(roleAssignment, caseDetails));
    }

}
