package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentFilteringResult;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleMatchingResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.accessprofile.filter.BaseFilter;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class JurisdictionMatcherTest extends BaseFilter {

    private JurisdictionMatcher classUnderTest;

    @BeforeEach
    void setUp() {
        classUnderTest = new JurisdictionMatcher();
    }

    @Test
    void shouldMatchWhenJurisdictionsAreSame() {
        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_1, JURISDICTION_1,
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS),
            "PRIVATE", null, null);

        RoleAssignmentFilteringResult result = new RoleAssignmentFilteringResult(roleAssignment,
            new RoleMatchingResult());

        CaseDetails caseDetails = mockCaseDetails();
        classUnderTest.matchAttribute(result, caseDetails);
        assertTrue(result.getRoleMatchingResult().isJurisdictionMatched());
    }

    @Test
    void shouldNotMatchWhenJurisdictionIsDifferent() {
        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_1, JURISDICTION_1,
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS),
            "PRIVATE", null, null);

        RoleAssignmentFilteringResult result = new RoleAssignmentFilteringResult(roleAssignment,
            new RoleMatchingResult());

        CaseDetails caseDetails = mockCaseDetails(SecurityClassification.RESTRICTED, JURISDICTION_2);
        classUnderTest.matchAttribute(result, caseDetails);
        assertFalse(result.getRoleMatchingResult().isJurisdictionMatched());
    }

    @Test
    void shouldMatchWhenJurisdictionIsNullOnRoleAssignment() {
        RoleAssignment roleAssignment = createRoleAssignment(
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS),
            "PRIVATE", Optional.of(CASE_ID_1),
            null, null, null);

        RoleAssignmentFilteringResult result = new RoleAssignmentFilteringResult(roleAssignment,
            new RoleMatchingResult());

        CaseDetails caseDetails = mockCaseDetails(SecurityClassification.RESTRICTED, JURISDICTION_2);
        classUnderTest.matchAttribute(result, caseDetails);
        assertTrue(result.getRoleMatchingResult().isJurisdictionMatched());
    }

    @Test
    void shouldNotMatchWhenJurisdictionIsEmptyOnRoleAssignment() {
        RoleAssignment roleAssignment = createRoleAssignment(
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS),
            "PRIVATE", Optional.of(CASE_ID_1),
            Optional.of(""), null, null);

        RoleAssignmentFilteringResult result = new RoleAssignmentFilteringResult(roleAssignment,
            new RoleMatchingResult());

        CaseDetails caseDetails = mockCaseDetails(SecurityClassification.RESTRICTED, JURISDICTION_2);
        classUnderTest.matchAttribute(result, caseDetails);
        assertFalse(result.getRoleMatchingResult().isJurisdictionMatched());
    }
}
