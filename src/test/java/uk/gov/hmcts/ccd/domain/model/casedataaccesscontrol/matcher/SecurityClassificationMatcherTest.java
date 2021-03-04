package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentFilteringResult;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleMatchingResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.accessprofile.filter.BaseFilter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

class SecurityClassificationMatcherTest extends BaseFilter {

    private SecurityClassificationMatcher classUnderTest;

    @BeforeEach
    void setUp() {
        classUnderTest = new SecurityClassificationMatcher();
    }

    @Test
    void shouldMatchSecurityClassifications() {
        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_1, JURISDICTION_1,
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS), "PRIVATE");
        Pair<RoleAssignment, RoleMatchingResult> result = Pair.of(roleAssignment,
            new RoleMatchingResult());
        CaseDetails caseDetails = mockCaseDetails();
        classUnderTest.matchAttribute(result, caseDetails);
        assertTrue(result.getRight().isClassificationMatched());
    }

    @Test
    void shouldNotMatchSecurityClassificationsWhenOneHasHigherClassfication() {
        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_1, JURISDICTION_1,
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS), "PUBLIC");
        Pair<RoleAssignment, RoleMatchingResult> result = Pair.of(roleAssignment,
            new RoleMatchingResult());
        CaseDetails caseDetails = mockCaseDetails(SecurityClassification.RESTRICTED);
        classUnderTest.matchAttribute(result, caseDetails);
        assertFalse(result.getRight().isClassificationMatched());
    }


    @Test
    void shouldNotMatchSecurityClassificationsOnInvalidClassification() {
        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_1, JURISDICTION_1,
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS), "INVALID");
        Pair<RoleAssignment, RoleMatchingResult> result = Pair.of(roleAssignment,
            new RoleMatchingResult());
        CaseDetails caseDetails = mockCaseDetails(SecurityClassification.RESTRICTED);
        classUnderTest.matchAttribute(result, caseDetails);
        assertFalse(result.getRight().isClassificationMatched());
    }

    @Test
    void shouldNotMatchSecurityClassificationsOnInvalidNullClassification() {
        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_1, JURISDICTION_1,
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS), null);
        Pair<RoleAssignment, RoleMatchingResult> result = Pair.of(roleAssignment,
            new RoleMatchingResult());
        CaseDetails caseDetails = mockCaseDetails(SecurityClassification.RESTRICTED);
        classUnderTest.matchAttribute(result, caseDetails);
        assertFalse(result.getRight().isClassificationMatched());
    }

}
