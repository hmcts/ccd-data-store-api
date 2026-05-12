package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.accessprofile.filter.BaseFilter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JurisdictionMatcherTest extends BaseFilter {

    private static final String CROSS_JURISDICTION_ROLE = "caseworker-caa";

    private JurisdictionMatcher classUnderTest;

    @BeforeEach
    void setUp() {
        ApplicationParams applicationParams = mock(ApplicationParams.class);
        when(applicationParams.getCcdAccessControlCrossJurisdictionRoles())
            .thenReturn(List.of(CROSS_JURISDICTION_ROLE, "caseworker-approver", "next-hearing-date-admin"));
        classUnderTest = new JurisdictionMatcher(applicationParams);
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
    void shouldMatchWhenJurisdictionIsNullOnCrossJurisdictionalRole() {
        RoleAssignment roleAssignment = createRoleAssignment(
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS),
            "PRIVATE", Optional.of(CASE_ID_1),
            null, null, null);
        roleAssignment.setRoleName(CROSS_JURISDICTION_ROLE);

        CaseDetails caseDetails = mockCaseDetails(SecurityClassification.RESTRICTED, JURISDICTION_2);
        assertTrue(classUnderTest.matchAttribute(roleAssignment, caseDetails));
    }

    @Test
    void shouldNotMatchWhenJurisdictionIsNullOnNonCrossJurisdictionalRole() {
        RoleAssignment roleAssignment = createRoleAssignment(
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS),
            "PRIVATE", Optional.of(CASE_ID_1),
            null, null, null);
        // ROLE_NAME_1 ("RoleName1") is not in the cross-jurisdictional roles list

        CaseDetails caseDetails = mockCaseDetails(SecurityClassification.RESTRICTED, JURISDICTION_2);
        assertFalse(classUnderTest.matchAttribute(roleAssignment, caseDetails));
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
