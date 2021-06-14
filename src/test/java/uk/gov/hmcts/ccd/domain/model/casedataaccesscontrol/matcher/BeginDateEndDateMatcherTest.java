package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.accessprofile.filter.BaseFilter;

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


        CaseDetails caseDetails = mockCaseDetails();
        assertTrue(classUnderTest.matchAttribute(roleAssignment, caseDetails));
    }

    @Test
    void shouldNotMatchWhenBeginDateIsNull() {
        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_2, JURISDICTION_1, null,
            Instant.now().plus(2, ChronoUnit.DAYS),
            "PRIVATE", null, null);

        CaseDetails caseDetails = mockCaseDetails(SecurityClassification.RESTRICTED, JURISDICTION_2);
        assertFalse(classUnderTest.matchAttribute(roleAssignment, caseDetails));
    }

    @Test
    void shouldNotMatchWhenEndDateIsNull() {
        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_2, JURISDICTION_1,
            Instant.now().plus(1, ChronoUnit.DAYS),
            null,
            "PRIVATE", null, null);

        CaseDetails caseDetails = mockCaseDetails(SecurityClassification.RESTRICTED, JURISDICTION_2);
        classUnderTest.matchAttribute(roleAssignment, caseDetails);
    }

    @Test
    void shouldNotMatchWhenBeginDateAndEndDateAreBeforeCurrent() {
        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_2, JURISDICTION_1,
            Instant.now().minus(3, ChronoUnit.DAYS),
            Instant.now().minus(1, ChronoUnit.DAYS),
            "PRIVATE", null, null);

        CaseDetails caseDetails = mockCaseDetails(SecurityClassification.RESTRICTED, JURISDICTION_2);
        assertFalse(classUnderTest.matchAttribute(roleAssignment, caseDetails));
    }

    @Test
    void shouldNotMatchWhenBeginDateAndEndDateAreAfterCurrent() {
        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_2, JURISDICTION_1,
            Instant.now().plus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS),
            "PRIVATE", null, null);

        CaseDetails caseDetails = mockCaseDetails(SecurityClassification.RESTRICTED, JURISDICTION_2);
        assertFalse(classUnderTest.matchAttribute(roleAssignment, caseDetails));
    }
}
