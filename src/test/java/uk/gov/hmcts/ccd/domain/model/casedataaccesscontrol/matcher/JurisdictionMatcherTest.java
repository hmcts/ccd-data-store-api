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

        CaseDetails caseDetails = mockCaseDetails();
        assertTrue(classUnderTest.matchAttribute(roleAssignment, caseDetails));
    }

    @Test
    void shouldNotMatchWhenJurisdictionIsDifferent() {
        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_1, JURISDICTION_1,
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS),
            "PRIVATE", null, null);

        CaseDetails caseDetails = mockCaseDetails(SecurityClassification.RESTRICTED, JURISDICTION_2);
        assertFalse(classUnderTest.matchAttribute(roleAssignment, caseDetails));
    }

    @Test
    void shouldMatchWhenJurisdictionIsNullOnRoleAssignment() {
        RoleAssignment roleAssignment = createRoleAssignment(
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS),
            "PRIVATE", Optional.of(CASE_ID_1),
            null, null, null);

        CaseDetails caseDetails = mockCaseDetails(SecurityClassification.RESTRICTED, JURISDICTION_2);
        assertTrue(classUnderTest.matchAttribute(roleAssignment, caseDetails));
    }

    @Test
    void shouldNotMatchWhenJurisdictionIsEmptyOnRoleAssignment() {
        RoleAssignment roleAssignment = createRoleAssignment(
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS),
            "PRIVATE", Optional.of(CASE_ID_1),
            Optional.of(""), null, null);

        CaseDetails caseDetails = mockCaseDetails(SecurityClassification.RESTRICTED, JURISDICTION_2);
        assertFalse(classUnderTest.matchAttribute(roleAssignment, caseDetails));
    }
}
