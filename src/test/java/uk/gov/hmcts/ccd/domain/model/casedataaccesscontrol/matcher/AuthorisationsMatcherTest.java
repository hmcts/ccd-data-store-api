package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.RoleToAccessProfileDefinition;
import uk.gov.hmcts.ccd.domain.service.AuthorisationMapper;
import uk.gov.hmcts.ccd.domain.service.accessprofile.filter.BaseFilter;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthorisationsMatcherTest extends BaseFilter {

    private static final String AUTHORISATION_1 = "auth-1";
    private static final String AUTHORISATION_2 = "auth-2";
    private static final String AUTHORISATION_3 = "auth-3";

    private AuthorisationsMatcher classUnderTest;

    private CaseTypeService caseTypeService;

    @BeforeEach
    void setUp() {
        caseTypeService = mock(CaseTypeService.class);
        classUnderTest = new AuthorisationsMatcher(new AuthorisationMapper(caseTypeService));
    }

    @Test
    void shouldMatchWhenRoleAssignmentHasMatchingCaseWithDefinitionAuthorisations() {
        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_1, JURISDICTION_1,
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS),
            "PRIVATE", null, null);
        roleAssignment.setAuthorisations(Lists.newArrayList(AUTHORISATION_1));

        CaseTypeDefinition caseTypeDefinition = mockCaseTypeDefinition();

        List<RoleToAccessProfileDefinition> roleToAccessProfileDefinitions = mockRoleToAccessProfileDefinitions(
            ROLE_NAME_1,
            CASE_TYPE_ID_1,
            1,
            false,
            Lists.newArrayList(AUTHORISATION_1));
        when(caseTypeDefinition.getRoleToAccessProfiles()).thenReturn(roleToAccessProfileDefinitions);

        CaseDetails caseDetails = mockCaseDetails();
        when(caseTypeService.getCaseType(anyString())).thenReturn(caseTypeDefinition);
        assertTrue(classUnderTest.matchAttribute(roleAssignment, caseDetails));
    }

    @Test
    void shouldMatchWhenRoleAssignmentHasMatchingDefinitionAuthorisations() {
        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_1, JURISDICTION_1,
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS),
            "PRIVATE", null, null);
        roleAssignment.setAuthorisations(Lists.newArrayList(AUTHORISATION_1));

        CaseTypeDefinition caseTypeDefinition = mockCaseTypeDefinition();

        List<RoleToAccessProfileDefinition> roleToAccessProfileDefinitions = mockRoleToAccessProfileDefinitions(
            ROLE_NAME_1,
            CASE_TYPE_ID_1,
            1,
            false,
            Lists.newArrayList(AUTHORISATION_1));
        when(caseTypeDefinition.getRoleToAccessProfiles()).thenReturn(roleToAccessProfileDefinitions);
        assertTrue(classUnderTest.matchAttribute(roleAssignment, caseTypeDefinition));
    }

    @Test
    void shouldMatchWhenRoleAssignmentHasMatchingDefinitionAuthorisationsForRoleWithMultipleAccessProfiles() {
        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_1, JURISDICTION_1,
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS),
            "PRIVATE", null, null);
        roleAssignment.setAuthorisations(Lists.newArrayList(AUTHORISATION_1));

        CaseTypeDefinition caseTypeDefinition = mockCaseTypeDefinition();

        List<RoleToAccessProfileDefinition> roleToAccessProfileDefinitions = mockRoleToMultipleAccessProfileDefinitions(
            ROLE_NAME_1,
            CASE_TYPE_ID_1,
            false,
            Lists.newArrayList(AUTHORISATION_2),
            Lists.newArrayList(AUTHORISATION_1)
            );
        when(caseTypeDefinition.getRoleToAccessProfiles()).thenReturn(roleToAccessProfileDefinitions);
        assertTrue(classUnderTest.matchAttribute(roleAssignment, caseTypeDefinition));
    }

    @Test
    void shouldMatchWhenRoleAssignmentHasMatchingCaseWithDefinitionAuthorisationsForRoleWithMultipleAccessProfiles() {
        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_1, JURISDICTION_1,
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS),
            "PRIVATE", null, null);
        roleAssignment.setAuthorisations(Lists.newArrayList(AUTHORISATION_1));

        CaseTypeDefinition caseTypeDefinition = mockCaseTypeDefinition();

        List<RoleToAccessProfileDefinition> roleToAccessProfileDefinitions = mockRoleToMultipleAccessProfileDefinitions(
            ROLE_NAME_1,
            CASE_TYPE_ID_1,
            false,
            Lists.newArrayList(AUTHORISATION_2),
            Lists.newArrayList(AUTHORISATION_1));
        when(caseTypeDefinition.getRoleToAccessProfiles()).thenReturn(roleToAccessProfileDefinitions);

        CaseDetails caseDetails = mockCaseDetails();
        when(caseTypeService.getCaseType(anyString())).thenReturn(caseTypeDefinition);
        assertTrue(classUnderTest.matchAttribute(roleAssignment, caseDetails));
    }

    @Test
    void shouldNotMatchWhenRoleAssignmentHasMatchingDefinitionAuthorisationsForRoleWithMultipleAccessProfiles() {
        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_1, JURISDICTION_1,
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS),
            "PRIVATE", null, null);
        roleAssignment.setAuthorisations(Lists.newArrayList(AUTHORISATION_1));

        CaseTypeDefinition caseTypeDefinition = mockCaseTypeDefinition();

        List<RoleToAccessProfileDefinition> roleToAccessProfileDefinitions = mockRoleToMultipleAccessProfileDefinitions(
            ROLE_NAME_1,
            CASE_TYPE_ID_1,
            false,
            Lists.newArrayList(AUTHORISATION_2),
            Lists.newArrayList(AUTHORISATION_3)
        );
        when(caseTypeDefinition.getRoleToAccessProfiles()).thenReturn(roleToAccessProfileDefinitions);
        assertFalse(classUnderTest.matchAttribute(roleAssignment, caseTypeDefinition));
    }

    @Test
    void shouldNotMatchWhenRoleAssignmentHasMatchingCaseWithDefinitionAuthorisationsWithMultipleAccessProfilesToRole() {
        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_1, JURISDICTION_1,
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS),
            "PRIVATE", null, null);
        roleAssignment.setAuthorisations(Lists.newArrayList(AUTHORISATION_1));

        CaseTypeDefinition caseTypeDefinition = mockCaseTypeDefinition();

        List<RoleToAccessProfileDefinition> roleToAccessProfileDefinitions = mockRoleToMultipleAccessProfileDefinitions(
            ROLE_NAME_1,
            CASE_TYPE_ID_1,
            false,
            Lists.newArrayList(AUTHORISATION_2),
            Lists.newArrayList(AUTHORISATION_3));
        when(caseTypeDefinition.getRoleToAccessProfiles()).thenReturn(roleToAccessProfileDefinitions);

        CaseDetails caseDetails = mockCaseDetails();
        when(caseTypeService.getCaseType(anyString())).thenReturn(caseTypeDefinition);
        assertFalse(classUnderTest.matchAttribute(roleAssignment, caseDetails));
    }

    @Test
    void shouldNotMatchWhenRoleAssignmentHasEmptyAuthorisations() {
        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_1, JURISDICTION_1,
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS),
            "PRIVATE", null, null);
        roleAssignment.setAuthorisations(Lists.newArrayList());

        CaseTypeDefinition caseTypeDefinition = mockCaseTypeDefinition();
        List<RoleToAccessProfileDefinition> roleToAccessProfileDefinitions = mockRoleToAccessProfileDefinitions(
            ROLE_NAME_1,
            CASE_TYPE_ID_1,
            1,
            false,
            Lists.newArrayList(AUTHORISATION_1));
        when(caseTypeDefinition.getRoleToAccessProfiles()).thenReturn(roleToAccessProfileDefinitions);
        assertTrue(classUnderTest.matchAttribute(roleAssignment, caseTypeDefinition));
    }

    @Test
    void shouldNotMatchWhenRoleAssignmentHasNullAuthorisations() {
        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_1, JURISDICTION_1,
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS),
            "PRIVATE", null, null);
        roleAssignment.setAuthorisations(null);

        CaseTypeDefinition caseTypeDefinition = mockCaseTypeDefinition();
        List<RoleToAccessProfileDefinition> roleToAccessProfileDefinitions = mockRoleToAccessProfileDefinitions(
            ROLE_NAME_1,
            CASE_TYPE_ID_1,
            1,
            false,
            Lists.newArrayList(AUTHORISATION_1));
        when(caseTypeDefinition.getRoleToAccessProfiles()).thenReturn(roleToAccessProfileDefinitions);
        assertTrue(classUnderTest.matchAttribute(roleAssignment, caseTypeDefinition));
    }

    @Test
    void shouldNotMatchWhenDefinitionHasNoAuthorisations() {
        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_1, JURISDICTION_1,
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS),
            "PRIVATE", null, null);
        roleAssignment.setAuthorisations(Lists.newArrayList(AUTHORISATION_1));

        CaseTypeDefinition caseTypeDefinition = mockCaseTypeDefinition();
        List<RoleToAccessProfileDefinition> roleToAccessProfileDefinitions = mockRoleToAccessProfileDefinitions(
            ROLE_NAME_1,
            CASE_TYPE_ID_1,
            1,
            false,
            Lists.newArrayList());
        when(caseTypeDefinition.getRoleToAccessProfiles()).thenReturn(roleToAccessProfileDefinitions);
        assertTrue(classUnderTest.matchAttribute(roleAssignment, caseTypeDefinition));
    }

    @Test
    void shouldNotMatchWhenRoleAndDefinitionHasDifferentAuthorisations() {
        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_1, JURISDICTION_1,
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS),
            "PRIVATE", null, null);
        roleAssignment.setAuthorisations(Lists.newArrayList(AUTHORISATION_1));

        CaseTypeDefinition caseTypeDefinition = mockCaseTypeDefinition();

        List<RoleToAccessProfileDefinition> roleToAccessProfileDefinitions = mockRoleToAccessProfileDefinitions(
            ROLE_NAME_1,
            CASE_TYPE_ID_1,
            1,
            false,
            Lists.newArrayList(AUTHORISATION_2, AUTHORISATION_3));
        when(caseTypeDefinition.getRoleToAccessProfiles()).thenReturn(roleToAccessProfileDefinitions);
        assertFalse(classUnderTest.matchAttribute(roleAssignment, caseTypeDefinition));
    }


    private List<RoleToAccessProfileDefinition> mockRoleToAccessProfileDefinitions(String roleName,
                                                                                   String caseTypeId,
                                                                                   int numberOfAccessProfiles,
                                                                                   boolean disabled,
                                                                                   List<String> authorisations) {
        List<RoleToAccessProfileDefinition> roleToAccessProfileDefinitions = new ArrayList<>();
        for (int i = 0;  i < numberOfAccessProfiles; i++) {
            RoleToAccessProfileDefinition roleToAccessProfileDefinition = mock(RoleToAccessProfileDefinition.class);
            when(roleToAccessProfileDefinition.getDisabled()).thenReturn(disabled);
            when(roleToAccessProfileDefinition.getReadOnly()).thenReturn(false);
            when(roleToAccessProfileDefinition.getCaseTypeId()).thenReturn(caseTypeId);
            when(roleToAccessProfileDefinition.getRoleName()).thenReturn(roleName);
            when(roleToAccessProfileDefinition.getAccessProfileList()).thenReturn(Lists.newArrayList());
            when(roleToAccessProfileDefinition.getAuthorisationList()).thenReturn(authorisations);
            roleToAccessProfileDefinitions.add(roleToAccessProfileDefinition);
        }
        return roleToAccessProfileDefinitions;
    }

    private List<RoleToAccessProfileDefinition> mockRoleToMultipleAccessProfileDefinitions(String roleName,
                                                                                           String caseTypeId,
                                                                                           boolean disabled,
                                                                                           List<String> authorisations1,
                                                                                           List<String>
                                                                                               authorisations2) {
        RoleToAccessProfileDefinition roleToAccessProfileDefinition1 = mock(RoleToAccessProfileDefinition.class);
        when(roleToAccessProfileDefinition1.getDisabled()).thenReturn(disabled);
        when(roleToAccessProfileDefinition1.getReadOnly()).thenReturn(false);
        when(roleToAccessProfileDefinition1.getCaseTypeId()).thenReturn(caseTypeId);
        when(roleToAccessProfileDefinition1.getRoleName()).thenReturn(roleName);
        when(roleToAccessProfileDefinition1.getAccessProfileList()).thenReturn(Lists.newArrayList());
        when(roleToAccessProfileDefinition1.getAuthorisationList()).thenReturn(authorisations1);
        List<RoleToAccessProfileDefinition> roleToAccessProfileDefinitions = new ArrayList<>();
        roleToAccessProfileDefinitions.add(roleToAccessProfileDefinition1);

        RoleToAccessProfileDefinition roleToAccessProfileDefinition2 = mock(RoleToAccessProfileDefinition.class);
        when(roleToAccessProfileDefinition2.getDisabled()).thenReturn(disabled);
        when(roleToAccessProfileDefinition2.getReadOnly()).thenReturn(false);
        when(roleToAccessProfileDefinition2.getCaseTypeId()).thenReturn(caseTypeId);
        when(roleToAccessProfileDefinition2.getRoleName()).thenReturn(roleName);
        when(roleToAccessProfileDefinition2.getAccessProfileList()).thenReturn(Lists.newArrayList());
        when(roleToAccessProfileDefinition2.getAuthorisationList()).thenReturn(authorisations2);
        roleToAccessProfileDefinitions.add(roleToAccessProfileDefinition2);

        return roleToAccessProfileDefinitions;
    }
}
