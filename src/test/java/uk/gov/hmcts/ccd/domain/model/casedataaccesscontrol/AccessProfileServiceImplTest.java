package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.definition.RoleToAccessProfileDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.AccessProfileServiceImpl;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccessProfileServiceImplTest {

    private AccessProfileServiceImpl roleAssignmentMapper;

    @BeforeEach
    void setUp() {
        roleAssignmentMapper = new AccessProfileServiceImpl();
    }

    @Test
    void shouldNotReturnAccessProfilesWhenGrantTypeExcluded() {
        RoleAssignmentFilteringResult filteringResult = mockRoleAssignmentFilteringResultWithGrantTypeExcluded();
        List<AccessProfile> mappedAccessProfiles = roleAssignmentMapper
            .generateAccessProfiles(filteringResult, Collections.emptyList());

        assertNotNull(mappedAccessProfiles);
        assertEquals(0, mappedAccessProfiles.size());
    }

    @Test
    void shouldNotReturnAccessProfilesWhenRoleNamesDidNotMatch() {
        RoleAssignmentFilteringResult filteringResult = mockRoleAssignmentFilteringWithRoleNames();
        List<RoleToAccessProfileDefinition> roleToAccessProfileDefinitions =
            mockCaseTypeDefinitionWithRoleAssignments("TestName", "TestName1", "", "");
        List<AccessProfile> mappedAccessProfiles = roleAssignmentMapper
            .generateAccessProfiles(filteringResult, roleToAccessProfileDefinitions);

        assertNotNull(mappedAccessProfiles);
        assertEquals(0, mappedAccessProfiles.size());
    }

    @Test
    void shouldReturnAccessProfilesWhenAuthorisationsEmpty() {
        RoleAssignmentFilteringResult filteringResult = mockRoleAssignmentFilteringWithRoleNames();
        List<RoleToAccessProfileDefinition> roleToAccessProfileDefinitions =
            mockCaseTypeDefinitionWithRoleAssignments("RoleName", "RoleName1", "", "");
        List<AccessProfile> mappedAccessProfiles = roleAssignmentMapper
            .generateAccessProfiles(filteringResult, roleToAccessProfileDefinitions);

        assertNotNull(mappedAccessProfiles);
        assertEquals(4, mappedAccessProfiles.size());
    }

    @Test
    void shouldNotReturnAccessProfilesWhenAuthorisationsEmptyOnRoleAssignment() {
        RoleAssignmentFilteringResult filteringResult = mockRoleAssignmentFilteringWithAuthorisations(
            null, Lists.newArrayList());
        List<RoleToAccessProfileDefinition> roleToAccessProfileDefinitions =
            mockCaseTypeDefinitionWithRoleAssignments("RoleName", "RoleName1", "auth1,auth2", "auth3,auth4");
        List<AccessProfile> mappedAccessProfiles = roleAssignmentMapper
            .generateAccessProfiles(filteringResult, roleToAccessProfileDefinitions);

        assertNotNull(mappedAccessProfiles);
        assertEquals(0, mappedAccessProfiles.size());
    }

    @Test
    void shouldReturnAccessProfilesWhenAuthorisationsMatch() {
        RoleAssignmentFilteringResult filteringResult = mockRoleAssignmentFilteringWithAuthorisations(
            Lists.newArrayList("auth2"),
            Lists.newArrayList("auth3"));
        List<RoleToAccessProfileDefinition> roleToAccessProfileDefinitions =
            mockCaseTypeDefinitionWithRoleAssignments("RoleName", "RoleName1", "auth1,auth2", "auth3,auth4");
        List<AccessProfile> mappedAccessProfiles = roleAssignmentMapper
            .generateAccessProfiles(filteringResult, roleToAccessProfileDefinitions);

        assertNotNull(mappedAccessProfiles);
        assertEquals(4, mappedAccessProfiles.size());
    }

    @Test
    void shouldReturnAccessProfilesWhenAuthorisationsMatchWhenGrantTypeExcludedInRoleAssignments() {
        RoleAssignmentFilteringResult filteringResult = mockRoleAssignmentFilteringWithGrantTypeAndOtherGrantTypes(
            Lists.newArrayList("auth2"),
            Lists.newArrayList("auth3"));
        List<RoleToAccessProfileDefinition> roleToAccessProfileDefinitions =
            mockCaseTypeDefinitionWithRoleAssignments("RoleName", "RoleName1", "auth1,auth2", "auth3,auth4");
        List<AccessProfile> mappedAccessProfiles = roleAssignmentMapper
            .generateAccessProfiles(filteringResult, roleToAccessProfileDefinitions);

        assertNotNull(mappedAccessProfiles);
        assertEquals(4, mappedAccessProfiles.size());
    }

    @Test
    void shouldReturnAccessProfilesWhenReadOnlyAttributesSet() {
        RoleAssignmentFilteringResult filteringResult = mockRoleAssignmentFilteringWithAuthorisations(
            Lists.newArrayList("auth2"),
            Lists.newArrayList("auth53"));
        List<RoleToAccessProfileDefinition> roleToAccessProfileDefinitions =
            mockCaseTypeDefinitionWithRoleAssignments("RoleName", "RoleName1", "auth1,auth2", "auth3,auth4");
        List<AccessProfile> mappedAccessProfiles = roleAssignmentMapper
            .generateAccessProfiles(filteringResult, roleToAccessProfileDefinitions);

        assertNotNull(mappedAccessProfiles);
        assertEquals(2, mappedAccessProfiles.size());
        assertEquals(true, mappedAccessProfiles.get(0).getReadOnly());
    }

    @Test
    void shouldReturnAccessProfilesWhenReadOnlyAttributesAreFalse() {
        RoleAssignmentFilteringResult filteringResult = mockRoleAssignmentFilteringWithAuthorisations(
            Lists.newArrayList("auth22"),
            Lists.newArrayList("auth3"));
        List<RoleToAccessProfileDefinition> roleToAccessProfileDefinitions =
            mockCaseTypeDefinitionWithRoleAssignments("RoleName", "RoleName1", "auth1,auth2", "auth3,auth4");
        List<AccessProfile> mappedAccessProfiles = roleAssignmentMapper
            .generateAccessProfiles(filteringResult, roleToAccessProfileDefinitions);

        assertNotNull(mappedAccessProfiles);
        assertEquals(2, mappedAccessProfiles.size());
        assertEquals(false, mappedAccessProfiles.get(0).getReadOnly());
    }

    private RoleAssignmentFilteringResult mockRoleAssignmentFilteringResultWithGrantTypeExcluded() {

        RoleAssignment roleAssignment = mock(RoleAssignment.class);
        when(roleAssignment.getGrantType()).thenReturn(GrantType.EXCLUDED.name());

        RoleAssignment roleAssignment1 = mock(RoleAssignment.class);
        when(roleAssignment1.getGrantType()).thenReturn(GrantType.EXCLUDED.name());

        return new RoleAssignmentFilteringResult(Lists.newArrayList(
            Pair.of(roleAssignment, mock(RoleMatchingResult.class)),
            Pair.of(roleAssignment, mock(RoleMatchingResult.class))));
    }

    private RoleAssignmentFilteringResult mockRoleAssignmentFilteringWithRoleNames() {
        RoleAssignment roleAssignment = mock(RoleAssignment.class);
        when(roleAssignment.getGrantType()).thenReturn(GrantType.BASIC.name());
        when(roleAssignment.getRoleName()).thenReturn("RoleName");

        RoleAssignment roleAssignment1 = mock(RoleAssignment.class);
        when(roleAssignment1.getGrantType()).thenReturn(GrantType.SPECIFIC.name());
        when(roleAssignment1.getRoleName()).thenReturn("RoleName1");

        return new RoleAssignmentFilteringResult(Lists.newArrayList(
            Pair.of(roleAssignment, mock(RoleMatchingResult.class)),
            Pair.of(roleAssignment, mock(RoleMatchingResult.class))));
    }

    private List<RoleToAccessProfileDefinition> mockCaseTypeDefinitionWithRoleAssignments(String roleName1,
                                                                                          String roleName2,
                                                                                          String authorisation1,
                                                                                          String authorisation2) {
        RoleToAccessProfileDefinition roleToAccessProfileDefinition = mock(RoleToAccessProfileDefinition.class);
        when(roleToAccessProfileDefinition.getRoleName()).thenReturn(roleName1);
        when(roleToAccessProfileDefinition.getAuthorisations()).thenReturn(authorisation1);
        when(roleToAccessProfileDefinition.isReadOnly()).thenReturn(true);
        when(roleToAccessProfileDefinition.isDisabled()).thenReturn(false);
        when(roleToAccessProfileDefinition.getAuthorisationList()).thenCallRealMethod();
        when(roleToAccessProfileDefinition.getAccessProfiles()).thenReturn("citizen,caseworker-befta-solicitor");
        when(roleToAccessProfileDefinition.getAccessProfileList()).thenReturn(
            Lists.newArrayList("citizen", "caseworker-befta-solicitor"));

        RoleToAccessProfileDefinition roleToAccessProfileDefinition1 = mock(RoleToAccessProfileDefinition.class);
        when(roleToAccessProfileDefinition1.getRoleName()).thenReturn(roleName2);
        when(roleToAccessProfileDefinition1.getAuthorisations()).thenReturn(authorisation2);
        when(roleToAccessProfileDefinition1.isReadOnly()).thenReturn(false);
        when(roleToAccessProfileDefinition1.isDisabled()).thenReturn(false);
        when(roleToAccessProfileDefinition1.getAuthorisationList()).thenCallRealMethod();
        when(roleToAccessProfileDefinition1.getAccessProfiles()).thenReturn("citizen,caseworker-befta-solicitor");
        when(roleToAccessProfileDefinition1.getAccessProfileList()).thenReturn(
            Lists.newArrayList("citizen", "caseworker-befta-solicitor"));

        return Lists.newArrayList(
            roleToAccessProfileDefinition,
            roleToAccessProfileDefinition1);
    }

    private RoleAssignmentFilteringResult mockRoleAssignmentFilteringWithAuthorisations(List<String> authorisations1,
                                                                                        List<String> authorisations2) {
        RoleAssignment roleAssignment = mock(RoleAssignment.class);
        when(roleAssignment.getGrantType()).thenReturn(GrantType.BASIC.name());
        when(roleAssignment.getRoleName()).thenReturn("RoleName");
        when(roleAssignment.getAuthorisations()).thenReturn(authorisations1);
        when(roleAssignment.getReadOnly()).thenReturn(false);

        RoleAssignment roleAssignment1 = mock(RoleAssignment.class);
        when(roleAssignment1.getGrantType()).thenReturn(GrantType.SPECIFIC.name());
        when(roleAssignment1.getRoleName()).thenReturn("RoleName1");
        when(roleAssignment1.getAuthorisations()).thenReturn(authorisations2);
        when(roleAssignment1.getReadOnly()).thenReturn(false);

        return new RoleAssignmentFilteringResult(Lists.newArrayList(
            Pair.of(roleAssignment, mock(RoleMatchingResult.class)),
            Pair.of(roleAssignment1, mock(RoleMatchingResult.class))));
    }


    private RoleAssignmentFilteringResult mockRoleAssignmentFilteringWithGrantTypeAndOtherGrantTypes(
        List<String> authorisations1,
        List<String> authorisations2) {

        RoleAssignment roleAssignment = mock(RoleAssignment.class);
        when(roleAssignment.getGrantType()).thenReturn(GrantType.BASIC.name());
        when(roleAssignment.getRoleName()).thenReturn("RoleName");
        when(roleAssignment.getAuthorisations()).thenReturn(authorisations1);
        when(roleAssignment.getReadOnly()).thenReturn(false);

        RoleAssignment roleAssignment1 = mock(RoleAssignment.class);
        when(roleAssignment1.getGrantType()).thenReturn(GrantType.SPECIFIC.name());
        when(roleAssignment1.getRoleName()).thenReturn("RoleName1");
        when(roleAssignment1.getAuthorisations()).thenReturn(authorisations2);
        when(roleAssignment1.getReadOnly()).thenReturn(false);

        RoleAssignment roleAssignment2 = mock(RoleAssignment.class);
        when(roleAssignment2.getGrantType()).thenReturn(GrantType.EXCLUDED.name());
        when(roleAssignment2.getRoleName()).thenReturn("RoleName2");
        when(roleAssignment2.getAuthorisations()).thenReturn(authorisations1);
        when(roleAssignment2.getReadOnly()).thenReturn(false);

        return new RoleAssignmentFilteringResult(Lists.newArrayList(
            Pair.of(roleAssignment, mock(RoleMatchingResult.class)),
            Pair.of(roleAssignment1, mock(RoleMatchingResult.class)),
            Pair.of(roleAssignment2, mock(RoleMatchingResult.class))));
    }
}
