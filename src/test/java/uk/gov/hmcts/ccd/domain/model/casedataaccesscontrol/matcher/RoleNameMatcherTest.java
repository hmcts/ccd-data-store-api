package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.RoleToAccessProfileDefinition;
import uk.gov.hmcts.ccd.domain.service.accessprofile.filter.BaseFilter;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class RoleNameMatcherTest extends BaseFilter {

    public static final String ROLE_NAME1 = "roleName1";
    public static final String ROLE_NAME2 = "roleName2";

    private RoleNameMatcher classUnderTest;

    @BeforeEach
    void setUp() {
        classUnderTest = new RoleNameMatcher();
    }

    @Test
    void shouldAlwaysMatchRoleNameForMatchAttributeWithCaseDetails() {
        RoleAssignment roleAssignment = null;
        CaseDetails caseDetails = null;
        assertTrue(classUnderTest.matchAttribute(roleAssignment, caseDetails));
    }

    @Test
    void shouldMatchWhenRoleNamePresentInAccessProfiles() {
        RoleAssignment roleAssignment = createRoleAssignmentWithRoleName(ROLE_NAME1);

        CaseTypeDefinition caseTypeDefinition = mockCaseTypeDefinition();
        when(caseTypeDefinition.getRoleToAccessProfiles()).thenReturn(asList(
            RoleToAccessProfileDefinition.builder()
                .roleName(ROLE_NAME1)
                .build(),
            RoleToAccessProfileDefinition.builder()
                .roleName(ROLE_NAME2)
                .build()
        ));
        assertTrue(classUnderTest.matchAttribute(roleAssignment, caseTypeDefinition));
    }

    @Test
    void shouldMatchWhenAccessProfilesNull() {
        RoleAssignment roleAssignment = createRoleAssignmentWithRoleName(ROLE_NAME1);

        CaseTypeDefinition caseTypeDefinition = mockCaseTypeDefinition();
        when(caseTypeDefinition.getRoleToAccessProfiles()).thenReturn(null);
        assertTrue(classUnderTest.matchAttribute(roleAssignment, caseTypeDefinition));
    }

    @Test
    void shouldMatchWhenAccessProfilesIsEmpty() {
        RoleAssignment roleAssignment = createRoleAssignmentWithRoleName(ROLE_NAME1);

        CaseTypeDefinition caseTypeDefinition = mockCaseTypeDefinition();
        when(caseTypeDefinition.getRoleToAccessProfiles()).thenReturn(emptyList());
        assertTrue(classUnderTest.matchAttribute(roleAssignment, caseTypeDefinition));
    }

    @Test
    void shouldNotMatchWhenRoleNameNotPresentInAccessProfiles() {
        RoleAssignment roleAssignment = createRoleAssignmentWithRoleName(ROLE_NAME1);

        CaseTypeDefinition caseTypeDefinition = mockCaseTypeDefinition();
        when(caseTypeDefinition.getRoleToAccessProfiles()).thenReturn(singletonList(
            RoleToAccessProfileDefinition.builder()
                .roleName(ROLE_NAME2)
                .build()
        ));
        assertFalse(classUnderTest.matchAttribute(roleAssignment, caseTypeDefinition));
    }
}
