package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.accessprofile.filter.BaseFilter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        CaseDetails caseDetails = mockCaseDetails();
        assertTrue(classUnderTest.matchAttribute(roleAssignment, caseDetails));
    }

    @Test
    void shouldNotMatchWhenCaseIdsAreDifferent() {
        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_2, JURISDICTION_1,
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS),
            "PRIVATE", null, null);

        CaseDetails caseDetails = mockCaseDetails(SecurityClassification.RESTRICTED, JURISDICTION_2);
        assertFalse(classUnderTest.matchAttribute(roleAssignment, caseDetails));
    }

    @Test
    void shouldMatchWhenCaseIdIsNullOnRoleAssignment() {
        RoleAssignment roleAssignment = createRoleAssignment(
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS),
            "PRIVATE", null,
            null, null, null);

        CaseDetails caseDetails = mockCaseDetails(SecurityClassification.RESTRICTED, JURISDICTION_2);
        assertTrue(classUnderTest.matchAttribute(roleAssignment, caseDetails));
    }

    @Test
    void shouldNotMatchWhenCaseIdIsEmptyOnRoleAssignment() {
        RoleAssignment roleAssignment = createRoleAssignment(
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS),
            "PRIVATE", Optional.of(""),
            Optional.of(""), null, null);

        CaseDetails caseDetails = mockCaseDetails(SecurityClassification.RESTRICTED, JURISDICTION_2);
        assertFalse(classUnderTest.matchAttribute(roleAssignment, caseDetails));
    }
}
