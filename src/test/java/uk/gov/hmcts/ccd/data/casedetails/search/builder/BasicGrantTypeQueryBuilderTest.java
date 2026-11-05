package uk.gov.hmcts.ccd.data.casedetails.search.builder;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.ccd.data.casedetails.search.builder.SqlParamAssert.assertBoundParams;
import static uk.gov.hmcts.ccd.data.casedetails.search.builder.SqlParamAssert.assertParamsMatchQuery;
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

    private static final Set<String> STATES = Set.of("CaseCreated");
    private static final List<String> PUBLIC_ONLY = List.of("PUBLIC");
    private static final List<String> PUBLIC_AND_PRIVATE = List.of("PUBLIC", "PRIVATE");

    private Map<String, Object> params;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        params = Maps.newHashMap();
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
            .createQuery(Lists.newArrayList(roleAssignment), params, caseTypeDefinition);

        assertNotNull(query);
        String expectedValue =  "( state in (:abac$states_1_basic) "
            + "AND security_classification in (:abac$classifications_1_basic) )";
        assertEquals(expectedValue, query);
        assertBoundParams(query, params,
            "abac$states_1_basic", STATES,
            "abac$classifications_1_basic", PUBLIC_AND_PRIVATE);
    }

    @Test
    void shouldReturnQueryWhenRoleAssignmentHasCaseAccessGroupId() {
        when(applicationParams.getCaseGroupAccessFilteringEnabled()).thenReturn(true);
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC, "CASE", "ROLE1",
            "PRIVATE", "", "", null, null, "", "caseAccessGroupId");
        String query = basicGrantTypeQueryBuilder
            .createQuery(Lists.newArrayList(roleAssignment), params, caseTypeDefinition);

        assertNotNull(query);
        String expectedValue =  "( data->'CaseAccessGroups' @> jsonb_build_array(jsonb_build_object('value', "
            + "jsonb_build_object('caseAccessGroupId', CAST(:abac$case_access_group_id_1_basic AS text)))) "
            + "AND state in (:abac$states_1_basic) "
            + "AND security_classification in (:abac$classifications_1_basic) )";
        assertEquals(expectedValue, query);
        assertBoundParams(query, params,
            "abac$case_access_group_id_1_basic", "caseAccessGroupId",
            "abac$states_1_basic", STATES,
            "abac$classifications_1_basic", PUBLIC_AND_PRIVATE);
    }

    @Test
    void shouldNotReturnCaseAccessGroupQueryWhenNotEnabled() {
        when(applicationParams.getCaseGroupAccessFilteringEnabled()).thenReturn(false);
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC, "CASE", "ROLE1",
            "PRIVATE", "", "", null, null, "", "caseAccessGroupId");
        String query = basicGrantTypeQueryBuilder
            .createQuery(Lists.newArrayList(roleAssignment), params, caseTypeDefinition);

        assertNotNull(query);
        assertFalse(query.contains("caseAccessGroups"));
        assertParamsMatchQuery(query, params);
        assertFalse(params.containsKey("abac$case_access_group_id_1_basic"),
            "the disabled caseAccessGroupId predicate must not leave a bound param behind: " + params);
    }

    @Test
    void shouldReturnEmptyQueryWhenRoleAssignmentHasAuthorisations() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC,
            "CASE",
            "ROLE1", "PRIVATE", "", "",
            Lists.newArrayList("auth1"));
        String query = basicGrantTypeQueryBuilder
            .createQuery(Lists.newArrayList(roleAssignment), params, caseTypeDefinition);

        assertNotNull(query);
        assertEquals("( state in (:abac$states_1_basic) "
            + "AND security_classification in (:abac$classifications_1_basic) )", query);
        assertBoundParams(query, params,
            "abac$states_1_basic", STATES,
            "abac$classifications_1_basic", PUBLIC_AND_PRIVATE);
    }

    @Test
    void shouldReturnEmptyQueryWhenRoleAssignmentHasNoBasicGrantType() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.CHALLENGED,
            "CASE",
            "ROLE1", "PRIVATE", "", "", null);
        String query = basicGrantTypeQueryBuilder
            .createQuery(Lists.newArrayList(roleAssignment), params, caseTypeDefinition);

        assertNotNull(query);
        assertEquals("", query);
        assertTrue(params.isEmpty(), "an empty query must bind nothing, params were: " + params);
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
            params, caseTypeDefinition);

        assertNotNull(query);
        String expectedValue =  "( state in (:abac$states_1_basic) "
            + "AND security_classification in (:abac$classifications_1_basic) ) "
            + "OR ( state in (:abac$states_2_basic) AND security_classification in (:abac$classifications_2_basic) )";
        assertEquals(expectedValue, query);
        assertBoundParams(query, params,
            "abac$states_1_basic", STATES,
            "abac$classifications_1_basic", PUBLIC_AND_PRIVATE,
            "abac$states_2_basic", STATES,
            "abac$classifications_2_basic", PUBLIC_ONLY);
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
            params, caseTypeDefinition);

        assertNotNull(query);
        String expectedValue =  "( state in (:abac$states_1_basic) "
            + "AND security_classification in (:abac$classifications_1_basic) )"
            + " OR ( state in (:abac$states_2_basic) AND security_classification in (:abac$classifications_2_basic) )";
        assertEquals(expectedValue, query);
        assertBoundParams(query, params,
            "abac$states_1_basic", STATES,
            "abac$classifications_1_basic", PUBLIC_AND_PRIVATE,
            "abac$states_2_basic", STATES,
            "abac$classifications_2_basic", PUBLIC_ONLY);
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
            params, caseTypeDefinition);

        assertNotNull(query);
        String expectedValue =  "( state in (:abac$states_1_basic) "
            + "AND security_classification in (:abac$classifications_1_basic) "
            + "AND ( data #>> '{CaseAccessCategory}' LIKE :abac$case_access_category_1_1_basic ESCAPE '\\' ) )";


        assertEquals(expectedValue, query);
        assertBoundParams(query, params,
            "abac$states_1_basic", STATES,
            "abac$classifications_1_basic", PUBLIC_ONLY,
            "abac$case_access_category_1_1_basic", "Civil/Standard%");
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
            params, caseTypeDefinition);

        assertNotNull(query);
        String expectedValue =  "( state in (:abac$states_1_basic) "
            + "AND security_classification in (:abac$classifications_1_basic) "
            + "AND ( data #>> '{CaseAccessCategory}' LIKE :abac$case_access_category_1_1_basic ESCAPE '\\' "
            + "OR data #>> '{CaseAccessCategory}' LIKE :abac$case_access_category_1_2_basic ESCAPE '\\' ) ) "
            + "OR ( state in (:abac$states_2_basic) AND security_classification in (:abac$classifications_2_basic) )";


        assertEquals(expectedValue, query);
        assertParamsMatchQuery(query, params);
        assertEquals(Set.of("Civil/Standard%", "Crime/Standard%"),
            Set.of(params.get("abac$case_access_category_1_1_basic"),
                params.get("abac$case_access_category_1_2_basic")),
            "both caseAccessCategory entries must be bound as escaped LIKE patterns, params were: " + params);
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
            params, caseTypeDefinition);

        assertNotNull(query);
        String expectedValue =  "( state in (:abac$states_1_basic) "
            + "AND security_classification in (:abac$classifications_1_basic) ) "
            + "OR ( state in (:abac$states_2_basic) AND security_classification in (:abac$classifications_2_basic) )";


        assertEquals(expectedValue, query);
        assertBoundParams(query, params,
            "abac$states_1_basic", STATES,
            "abac$classifications_1_basic", PUBLIC_AND_PRIVATE,
            "abac$states_2_basic", STATES,
            "abac$classifications_2_basic", PUBLIC_ONLY);
    }
}
