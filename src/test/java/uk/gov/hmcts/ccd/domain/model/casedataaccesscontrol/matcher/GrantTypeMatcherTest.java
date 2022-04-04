package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.accessprofile.filter.BaseFilter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrantTypeMatcherTest extends BaseFilter {

    private GrantTypeMatcher classUnderTest;

    @BeforeEach
    void setUp() {
        classUnderTest = new GrantTypeMatcher();
    }

    @Test
    void shouldAlwaysMatchGrantTapeForMatchAttributeWithCaseDetails() {
        RoleAssignment roleAssignment = null;
        CaseDetails caseDetails = null;
        assertTrue(classUnderTest.matchAttribute(roleAssignment, caseDetails));
    }

    @Test
    void shouldMatchWhenGrantTapeNotExcluded() {
        RoleAssignment roleAssignment = createRoleAssignmentWithGrantType(GrantType.BASIC.name());

        CaseTypeDefinition caseTypeDefinition = mockCaseTypeDefinition();
        assertTrue(classUnderTest.matchAttribute(roleAssignment, caseTypeDefinition));
    }

    @Test
    void shouldNotMatchWhenGrantTapeExcluded() {
        RoleAssignment roleAssignment = createRoleAssignmentWithGrantType(GrantType.EXCLUDED.name());

        CaseTypeDefinition caseTypeDefinition = mockCaseTypeDefinition();
        assertFalse(classUnderTest.matchAttribute(roleAssignment, caseTypeDefinition));
    }
}
