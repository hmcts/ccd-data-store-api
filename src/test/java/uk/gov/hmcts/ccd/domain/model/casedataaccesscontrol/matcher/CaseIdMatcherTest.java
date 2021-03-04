package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleMatchingResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.accessprofile.filter.BaseFilter;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CaseIdMatcherTest extends BaseFilter {

    private CaseIdMatcher classUnderTest;

    @BeforeEach
    void setUp() {
        classUnderTest = new CaseIdMatcher();
    }

    @Test
    void shouldMatchWhenCaseIdsAreSame() {
        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_1, JURISDICTION_1,
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS),
            "PRIVATE", null, null);

        Pair<RoleAssignment, RoleMatchingResult> result = Pair.of(roleAssignment,
            new RoleMatchingResult());

        CaseDetails caseDetails = mockCaseDetails();
        classUnderTest.matchAttribute(result, caseDetails);
        assertTrue(result.getRight().isCaseIdMatched());
    }

    @Test
    void shouldNotMatchWhenCaseIdsAreDifferent() {
        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_2, JURISDICTION_1,
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS),
            "PRIVATE", null, null);

        Pair<RoleAssignment, RoleMatchingResult> result = Pair.of(roleAssignment,
            new RoleMatchingResult());

        CaseDetails caseDetails = mockCaseDetails(SecurityClassification.RESTRICTED, JURISDICTION_2);
        classUnderTest.matchAttribute(result, caseDetails);
        assertFalse(result.getRight().isCaseIdMatched());
    }

    @Test
    void shouldMatchWhenCaseIdIsNullOnRoleAssignment() {
        RoleAssignment roleAssignment = createRoleAssignment(
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS),
            "PRIVATE", null,
            null, null, null);

        Pair<RoleAssignment, RoleMatchingResult> result = Pair.of(roleAssignment,
            new RoleMatchingResult());

        CaseDetails caseDetails = mockCaseDetails(SecurityClassification.RESTRICTED, JURISDICTION_2);
        classUnderTest.matchAttribute(result, caseDetails);
        assertTrue(result.getRight().isCaseIdMatched());
    }

    @Test
    void shouldNotMatchWhenCaseIdIsEmptyOnRoleAssignment() {
        RoleAssignment roleAssignment = createRoleAssignment(
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS),
            "PRIVATE", Optional.of(""),
            Optional.of(""), null, null);

        Pair<RoleAssignment, RoleMatchingResult> result = Pair.of(roleAssignment,
            new RoleMatchingResult());

        CaseDetails caseDetails = mockCaseDetails(SecurityClassification.RESTRICTED, JURISDICTION_2);
        classUnderTest.matchAttribute(result, caseDetails);
        assertFalse(result.getRight().isCaseIdMatched());
    }
}
