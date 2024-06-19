package uk.gov.hmcts.ccd.data.casedetails.search.builder;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.RoleToAccessProfileDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BasicGrantTypeQueryBuilderTest extends GrantTypeQueryBuilderTest {

    private BasicGrantTypeQueryBuilder basicGrantTypeQueryBuilder;

    @Mock
    private AccessControlService accessControlService;

    @Mock
    private CaseDataAccessControl caseDataAccessControl;

    @Mock
    private CaseTypeDefinition caseTypeDefinition;

    @Mock
    private ApplicationParams applicationParams;

    protected static final String CASE_TYPE_ID_1 = "CASE_TYPE_ID_1";
    protected static final String ROLE_NAME_1 = "ROLE1";
    protected static final String ROLE_NAME_2 = "ROLE2";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        basicGrantTypeQueryBuilder = new BasicGrantTypeQueryBuilder(accessControlService, caseDataAccessControl,
            applicationParams);
        CaseStateDefinition caseStateDefinition = mock(CaseStateDefinition.class);
        when(caseStateDefinition.getId()).thenReturn("CaseCreated");
        when(accessControlService
            .filterCaseStatesByAccess(anyList(), anySet(), any(Predicate.class)))
            .thenReturn(Lists.newArrayList(caseStateDefinition));
    }

    @Test
    void shouldReturnQueryWhenRoleAssignmentHasClassifications() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC, "CASE", "ROLE1",
            "PRIVATE", "", "", null);
        String query = basicGrantTypeQueryBuilder
            .createQuery(Lists.newArrayList(roleAssignment), Maps.newHashMap(), caseTypeDefinition);

        assertNotNull(query);
        String expectedValue =  "( state in (:states_1_basic) "
            + "AND security_classification in (:classifications_1_basic) )";
        assertEquals(expectedValue, query);
    }

    @Test
    void shouldReturnQueryWhenRoleAssignmentHasCaseAccessGroupId() {
        when(applicationParams.getCaseGroupAccessFilteringEnabled()).thenReturn(true);
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC, "CASE", "ROLE1",
            "PRIVATE", "", "", null, null, "", "caseAccessGroupId");
        String query = basicGrantTypeQueryBuilder
            .createQuery(Lists.newArrayList(roleAssignment), Maps.newHashMap(), caseTypeDefinition);

        assertNotNull(query);
        String expectedValue =  "( data->'CaseAccessGroups' @> '[{\"value\":{\"caseAccessGroupId\": "
            + "\"caseAccessGroupId\"}}]' AND state in (:states_1_basic) "
            + "AND security_classification in (:classifications_1_basic) )";
        assertEquals(expectedValue, query);
    }

    @Test
    void shouldNotReturnCaseAccessGroupQueryWhenNotEnabled() {
        when(applicationParams.getCaseGroupAccessFilteringEnabled()).thenReturn(false);
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC, "CASE", "ROLE1",
            "PRIVATE", "", "", null, null, "", "caseAccessGroupId");
        String query = basicGrantTypeQueryBuilder
            .createQuery(Lists.newArrayList(roleAssignment), Maps.newHashMap(), caseTypeDefinition);

        assertNotNull(query);
        assertFalse(query.contains("caseAccessGroups"));
    }

    @Test
    void shouldReturnEmptyQueryWhenRoleAssignmentHasAuthorisations() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC,
            "CASE",
            "ROLE1", "PRIVATE", "", "",
            Lists.newArrayList("auth1"));
        String query = basicGrantTypeQueryBuilder
            .createQuery(Lists.newArrayList(roleAssignment), Maps.newHashMap(), caseTypeDefinition);

        assertNotNull(query);
        assertEquals("( state in (:states_1_basic) AND security_classification in (:classifications_1_basic) )", query);
    }

    @Test
    void shouldReturnEmptyQueryWhenRoleAssignmentHasNoBasicGrantType() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.CHALLENGED,
            "CASE",
            "ROLE1", "PRIVATE", "", "", null);
        String query = basicGrantTypeQueryBuilder
            .createQuery(Lists.newArrayList(roleAssignment), Maps.newHashMap(), caseTypeDefinition);

        assertNotNull(query);
        assertEquals("", query);
    }

    @Test
    void shouldReturnQueryWhenAtLeastOneRoleAssignmentHasNoAuthorisations() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC,
            "CASE",
            "ROLE1", "PUBLIC", "", "",
            Lists.newArrayList("auth1"));

        RoleAssignment roleAssignment2 = createRoleAssignment(GrantType.BASIC,
            "CASE",
            "ROLE2", "PRIVATE", "", "",
            Lists.newArrayList());
        String query = basicGrantTypeQueryBuilder.createQuery(
            Lists.newArrayList(roleAssignment, roleAssignment2),
            Maps.newHashMap(), caseTypeDefinition);

        assertNotNull(query);
        String expectedValue =  "( state in (:states_1_basic) "
            + "AND security_classification in (:classifications_1_basic) ) "
            + "OR ( state in (:states_2_basic) AND security_classification in (:classifications_2_basic) )";
        assertEquals(expectedValue, query);
    }

    @Test
    void shouldReturnQueryWhenMoreThanOneRoleAssignmentHasNoAuthorisations() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC,
            "CASE",
            "ROLE1", "PUBLIC", "", "",
            Lists.newArrayList("auth1"));

        RoleAssignment roleAssignment2 = createRoleAssignment(GrantType.BASIC,
            "CASE",
            "ROLE2", "PRIVATE", "", "",
            Lists.newArrayList());

        String query = basicGrantTypeQueryBuilder.createQuery(
            Lists.newArrayList(roleAssignment, roleAssignment2),
            Maps.newHashMap(), caseTypeDefinition);

        assertNotNull(query);
        String expectedValue =  "( state in (:states_1_basic) "
            + "AND security_classification in (:classifications_1_basic) )"
            + " OR ( state in (:states_2_basic) AND security_classification in (:classifications_2_basic) )";
        assertEquals(expectedValue, query);
    }

    @Test
    void shouldReturnQueryWhenCaseAccessCategoryExistsForCaseTypeDefinition() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC,
            "CASE",
            "ROLE1", "PUBLIC", "", "",
            Lists.newArrayList("auth1"));

        List<RoleToAccessProfileDefinition> roleToAccessProfileDefinitions = mockRoleToAccessProfileDefinitions(
            ROLE_NAME_1,
            CASE_TYPE_ID_1,
            1,
            false,
            null,
            "Civil/Standard");
        when(caseTypeDefinition.getRoleToAccessProfiles()).thenReturn(roleToAccessProfileDefinitions);

        AccessProfile accessProfile = mock(AccessProfile.class);
        when(accessProfile.getAccessProfile()).thenReturn(ROLE_NAME_1);
        when(accessProfile.getCaseAccessCategories()).thenReturn("Civil/Standard");

        when(caseDataAccessControl.filteredAccessProfiles(anyList(), any(CaseTypeDefinition.class), anyBoolean()))
            .thenReturn(Sets.newHashSet(accessProfile));

        String query = basicGrantTypeQueryBuilder.createQuery(
            Lists.newArrayList(roleAssignment),
            Maps.newHashMap(), caseTypeDefinition);

        assertNotNull(query);
        String expectedValue =  "( state in (:states_1_basic) "
            + "AND security_classification in (:classifications_1_basic) "
            + "AND ( data #>> '{CaseAccessCategory}' LIKE 'Civil/Standard%' ) )";


        assertEquals(expectedValue, query);
    }

    @Test
    void shouldReturnQueryWhenCaseAccessCategoryWithMultipleEntriesExistsForCaseTypeDefinition() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC,
            "CASE",
            "ROLE1", "PUBLIC", "", "",
            Lists.newArrayList("auth1"));

        RoleAssignment roleAssignment2 = createRoleAssignment(GrantType.BASIC,
            "CASE",
            "ROLE2", "PRIVATE", "", "",
            Lists.newArrayList());

        List<RoleToAccessProfileDefinition> roleToAccessProfileDefinitions = mockRoleToAccessProfileDefinitions(
            ROLE_NAME_1,
            CASE_TYPE_ID_1,
            1,
            false,
            null,
            "Civil/Standard,Crime/Standard");
        when(caseTypeDefinition.getRoleToAccessProfiles()).thenReturn(roleToAccessProfileDefinitions);
        AccessProfile accessProfile1 = mock(AccessProfile.class);
        when(accessProfile1.getAccessProfile()).thenReturn(ROLE_NAME_1);
        when(accessProfile1.getCaseAccessCategories()).thenReturn("Civil/Standard,Crime/Standard");

        AccessProfile accessProfile2 = mock(AccessProfile.class);
        when(accessProfile2.getAccessProfile()).thenReturn(ROLE_NAME_1);
        when(accessProfile2.getCaseAccessCategories()).thenReturn(null);

        when(caseDataAccessControl.filteredAccessProfiles(anyList(), any(CaseTypeDefinition.class), anyBoolean()))
            .thenReturn(Sets.newHashSet(accessProfile1))
            .thenReturn(Sets.newHashSet(accessProfile1))
            .thenReturn(Sets.newHashSet(accessProfile1))
            .thenReturn(Sets.newHashSet(accessProfile2));

        String query = basicGrantTypeQueryBuilder.createQuery(
            Lists.newArrayList(roleAssignment, roleAssignment2),
            Maps.newHashMap(), caseTypeDefinition);

        assertNotNull(query);
        String expectedValue =  "( state in (:states_1_basic) "
            + "AND security_classification in (:classifications_1_basic) "
            + "AND ( data #>> '{CaseAccessCategory}' LIKE 'Civil/Standard%' "
            + "OR data #>> '{CaseAccessCategory}' LIKE 'Crime/Standard%' ) ) "
            + "OR ( state in (:states_2_basic) AND security_classification in (:classifications_2_basic) )";


        assertEquals(expectedValue, query);
    }

    @Test
    void shouldNotReturnQueryWithCaseAccessCategoryWhenRtapDHasNullCaseAccessCategory() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC,
            "CASE",
            "ROLE1", "PUBLIC", "", "",
            Lists.newArrayList("auth1"));

        RoleAssignment roleAssignment2 = createRoleAssignment(GrantType.BASIC,
            "CASE",
            "ROLE2", "PRIVATE", "", "",
            Lists.newArrayList());

        List<RoleToAccessProfileDefinition> roleToAccessProfileDefinitions = mockRoleToAccessProfileDefinitions(
            ROLE_NAME_1,
            CASE_TYPE_ID_1,
            1,
            false,
            null,
            "Civil/Standard,Crime/Standard");

        List<RoleToAccessProfileDefinition> roleName2AccessProfilesDefinitions = mockRoleToAccessProfileDefinitions(
            ROLE_NAME_2,
            CASE_TYPE_ID_1,
            1,
            false,
            null,
            null);
        roleToAccessProfileDefinitions.addAll(roleName2AccessProfilesDefinitions);
        when(caseTypeDefinition.getRoleToAccessProfiles()).thenReturn(roleToAccessProfileDefinitions);

        String query = basicGrantTypeQueryBuilder.createQuery(
            Lists.newArrayList(roleAssignment, roleAssignment2),
            Maps.newHashMap(), caseTypeDefinition);

        assertNotNull(query);
        String expectedValue =  "( state in (:states_1_basic) "
            + "AND security_classification in (:classifications_1_basic) ) "
            + "OR ( state in (:states_2_basic) AND security_classification in (:classifications_2_basic) )";


        assertEquals(expectedValue, query);
    }
}
