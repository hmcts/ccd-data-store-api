package uk.gov.hmcts.ccd.data.casedetails.search.builder;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.ccd.data.casedetails.search.builder.SqlParamAssert.assertBoundParams;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccessControlGrantTypeQueryBuilderTest extends GrantTypeQueryBuilderTest {

    private AccessControlGrantTypeQueryBuilder accessControlGrantTypeQueryBuilder;

    @Mock
    private AccessControlService accessControlService;

    @Mock
    private CaseDataAccessControl caseDataAccessControl;

    @Mock
    ApplicationParams applicationParams;

    @Mock
    private CaseTypeDefinition caseTypeDefinition;

    private static final Set<String> STATES = Set.of("CaseCreated");
    private static final List<String> PRIVATE_CLASSIFICATIONS = List.of("PUBLIC", "PRIVATE");

    private Map<String, Object> params;

    @BeforeEach
    void setUp() {
        params = Maps.newHashMap();
        MockitoAnnotations.openMocks(this);
        accessControlGrantTypeQueryBuilder = new AccessControlGrantTypeQueryBuilder(
            new BasicGrantTypeQueryBuilder(accessControlService, caseDataAccessControl, applicationParams),
            new SpecificGrantTypeQueryBuilder(accessControlService, caseDataAccessControl, applicationParams),
            new StandardGrantTypeQueryBuilder(accessControlService, caseDataAccessControl, applicationParams),
            new ChallengedGrantTypeQueryBuilder(accessControlService, caseDataAccessControl, applicationParams),
            new ExcludedGrantTypeQueryBuilder(accessControlService, caseDataAccessControl, applicationParams));

        CaseStateDefinition caseStateDefinition = mock(CaseStateDefinition.class);
        when(caseStateDefinition.getId()).thenReturn("CaseCreated");
        when(accessControlService
            .filterCaseStatesByAccess(anyList(), anySet(), any(Predicate.class)))
            .thenReturn(Lists.newArrayList(caseStateDefinition));
    }

    @Test
    void shouldReturnEmptyQueryWhenNoRoleAssignmentsExists() {
        String query = accessControlGrantTypeQueryBuilder.createQuery(Lists.newArrayList(),
            params, caseTypeDefinition);
        assertNotNull(query);
        assertTrue(StringUtils.isBlank(query));
        assertTrue(params.isEmpty(), "a blank query must bind nothing, params were: " + params);
    }

    @Test
    void shouldReturnBasicQueryWhenRoleAssignmentsWithBasicGrantTypeExists() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC, "CASE", "ROLE1",
            "PRIVATE", "", "", null);
        String query = accessControlGrantTypeQueryBuilder
            .createQuery(Lists.newArrayList(roleAssignment), params,
                caseTypeDefinition);
        String expectedValue =  " AND ( ( state in (:abac$states_1_basic) "
            + "AND security_classification in (:abac$classifications_1_basic) ) )";
        assertNotNull(query);
        assertEquals(expectedValue, query);
        assertBoundParams(query, params,
            "abac$states_1_basic", STATES,
            "abac$classifications_1_basic", PRIVATE_CLASSIFICATIONS);
    }


    @Test
    void shouldReturnNonOrganisationalQueryWhenRoleAssignmentsGrantTypeExists() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC,
            "CASE", "ROLE1", "PRIVATE", "", "", null);
        RoleAssignment specificRoleAssignment = createRoleAssignment(GrantType.SPECIFIC,
            "CASE", "ROLE2", "PRIVATE", "Test", "", "",
            null, "caseId1");
        String query = accessControlGrantTypeQueryBuilder
            .createQuery(Lists.newArrayList(roleAssignment,
                specificRoleAssignment),
                params,
                caseTypeDefinition);
        String expectedValue = """
             AND ( ( ( state in (:abac$states_1_basic) \
            AND security_classification in (:abac$classifications_1_basic) ) \
            OR ( jurisdiction = :abac$jurisdiction_1_specific \
            AND reference in (:abac$references_1_specific) AND state in (:abac$states_1_specific) \
            AND security_classification in (:abac$classifications_1_specific) ) ) )""";
        assertNotNull(query);
        assertEquals(expectedValue, query);
        assertBoundParams(query, params,
            "abac$states_1_basic", STATES,
            "abac$classifications_1_basic", PRIVATE_CLASSIFICATIONS,
            "abac$jurisdiction_1_specific", "Test",
            "abac$references_1_specific", List.of("caseId1"),
            "abac$states_1_specific", STATES,
            "abac$classifications_1_specific", PRIVATE_CLASSIFICATIONS);
    }

    @Test
    void shouldReturnOrgAndNonOrganisationalQueryWhenRoleAssignmentsGrantTypeExists() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC,
            "CASE", "ROLE1", "PRIVATE", "", "", null);
        RoleAssignment specificRoleAssignment = createRoleAssignment(GrantType.SPECIFIC,
            "CASE", "ROLE2", "PRIVATE", "Test", "", "",
            null, "caseId1");

        RoleAssignment challengedRoleAssignment = createRoleAssignment(GrantType.CHALLENGED,
            "CASE", "ROLE3", "PRIVATE", "Test", "", "",
            Lists.newArrayList("auth1"), "caseId1");

        RoleAssignment standardRoleAssignment = createRoleAssignment(GrantType.STANDARD,
            "CASE", "ROLE4", "PRIVATE", "Test", "loc1", "reg1",
            null, "caseId1");
        String query = accessControlGrantTypeQueryBuilder.createQuery(Lists.newArrayList(roleAssignment,
            specificRoleAssignment, challengedRoleAssignment, standardRoleAssignment),
            params, caseTypeDefinition);

        String expectedValue = """
             AND ( ( ( state in (:abac$states_1_basic) \
            AND security_classification in (:abac$classifications_1_basic) ) OR\
             ( jurisdiction = :abac$jurisdiction_1_specific \
            AND reference in (:abac$references_1_specific) AND state in (:abac$states_1_specific) \
            AND security_classification in (:abac$classifications_1_specific) ) ) OR ( ( \
            jurisdiction = :abac$jurisdiction_1_standard \
            AND data #>> '{caseManagementLocation,region}' = :abac$region_1_standard \
            AND data #>> '{caseManagementLocation,baseLocation}' = :abac$location_1_standard \
            AND reference in (:abac$references_1_standard) AND state in (:abac$states_1_standard) \
            AND security_classification in (:abac$classifications_1_standard) ) OR ( \
            jurisdiction = :abac$jurisdiction_1_challenged \
            AND reference in (:abac$references_1_challenged) AND state in (:abac$states_1_challenged) \
            AND security_classification in (:abac$classifications_1_challenged) ) ) )""";

        assertNotNull(query);
        assertEquals(expectedValue, query);
        assertBoundParams(query, params,
            "abac$states_1_basic", STATES,
            "abac$classifications_1_basic", PRIVATE_CLASSIFICATIONS,
            "abac$jurisdiction_1_specific", "Test",
            "abac$references_1_specific", List.of("caseId1"),
            "abac$states_1_specific", STATES,
            "abac$classifications_1_specific", PRIVATE_CLASSIFICATIONS,
            "abac$jurisdiction_1_standard", "Test",
            "abac$region_1_standard", "reg1",
            "abac$location_1_standard", "loc1",
            "abac$references_1_standard", List.of("caseId1"),
            "abac$states_1_standard", STATES,
            "abac$classifications_1_standard", PRIVATE_CLASSIFICATIONS,
            "abac$jurisdiction_1_challenged", "Test",
            "abac$references_1_challenged", List.of("caseId1"),
            "abac$states_1_challenged", STATES,
            "abac$classifications_1_challenged", PRIVATE_CLASSIFICATIONS);
    }

    @Test
    void shouldReturnOrgAndNonAndExcludedOrganisationalQueryWhenRoleAssignmentsGrantTypeExists() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC,
            "CASE", "ROLE1", "PRIVATE", "", "", null);
        RoleAssignment specificRoleAssignment = createRoleAssignment(GrantType.SPECIFIC,
            "CASE", "ROLE2", "PRIVATE", "Test", "", "",
            null, "caseId1");

        RoleAssignment challengedRoleAssignment = createRoleAssignment(GrantType.CHALLENGED,
            "CASE", "ROLE3", "PRIVATE", "Test", "", "",
            Lists.newArrayList("auth1"), "caseId1");

        RoleAssignment standardRoleAssignment = createRoleAssignment(GrantType.STANDARD,
            "CASE", "ROLE4", "PRIVATE", "Test", "loc1", "reg1",
            null, "caseId1");

        RoleAssignment excludedRoleAssignment = createRoleAssignment(GrantType.EXCLUDED,
            "CASE", "ROLE5", "PRIVATE", "Test", "loc1", "reg1",
            null, "caseId1");

        String query = accessControlGrantTypeQueryBuilder.createQuery(Lists.newArrayList(roleAssignment,
            specificRoleAssignment, challengedRoleAssignment,
            standardRoleAssignment, excludedRoleAssignment),
            params,
            caseTypeDefinition);

        String expectedValue =  " AND ( ( ( state in (:abac$states_1_basic) "
            + "AND security_classification in (:abac$classifications_1_basic) ) "
            + "OR ( jurisdiction = :abac$jurisdiction_1_specific AND reference in (:abac$references_1_specific) "
            + "AND state in (:abac$states_1_specific) "
            + "AND security_classification in (:abac$classifications_1_specific) ) ) "
            + "OR ( ( ( jurisdiction = :abac$jurisdiction_1_standard "
            + "AND data #>> '{caseManagementLocation,region}' = :abac$region_1_standard "
            + "AND data #>> '{caseManagementLocation,baseLocation}' = :abac$location_1_standard "
            + "AND reference in (:abac$references_1_standard) "
            + "AND state in (:abac$states_1_standard) "
            + "AND security_classification in (:abac$classifications_1_standard) ) "
            + "OR ( jurisdiction = :abac$jurisdiction_1_challenged AND reference in (:abac$references_1_challenged) "
            + "AND state in (:abac$states_1_challenged) "
            + "AND security_classification in (:abac$classifications_1_challenged) ) ) "
            + "AND NOT reference in (:abac$case_ids_excluded) ) )";

        assertNotNull(query);
        assertEquals(expectedValue, query);
        assertBoundParams(query, params,
            "abac$states_1_basic", STATES,
            "abac$classifications_1_basic", PRIVATE_CLASSIFICATIONS,
            "abac$jurisdiction_1_specific", "Test",
            "abac$references_1_specific", List.of("caseId1"),
            "abac$states_1_specific", STATES,
            "abac$classifications_1_specific", PRIVATE_CLASSIFICATIONS,
            "abac$jurisdiction_1_standard", "Test",
            "abac$region_1_standard", "reg1",
            "abac$location_1_standard", "loc1",
            "abac$references_1_standard", List.of("caseId1"),
            "abac$states_1_standard", STATES,
            "abac$classifications_1_standard", PRIVATE_CLASSIFICATIONS,
            "abac$jurisdiction_1_challenged", "Test",
            "abac$references_1_challenged", List.of("caseId1"),
            "abac$states_1_challenged", STATES,
            "abac$classifications_1_challenged", PRIVATE_CLASSIFICATIONS,
            "abac$case_ids_excluded", Set.of("caseId1"));
    }

    @Test
    void shouldReturnChallengedAndExcludedOrganisationalQueryWhenRoleAssignmentsGrantTypeExists() {
        RoleAssignment challengedRoleAssignment = createRoleAssignment(GrantType.CHALLENGED,
            "CASE", "ROLE1", "PRIVATE", "Test", "", "",
            Lists.newArrayList("auth1"), "caseId1");

        RoleAssignment excludedRoleAssignment = createRoleAssignment(GrantType.EXCLUDED,
            "CASE", "ROLE2", "PRIVATE", "Test", "loc1", "reg1",
            null, "caseId1");

        String query = accessControlGrantTypeQueryBuilder
            .createQuery(Lists.newArrayList(challengedRoleAssignment,
                excludedRoleAssignment),
                params,
                caseTypeDefinition);

        String expectedValue =  " AND ( ( ( jurisdiction = :abac$jurisdiction_1_challenged "
            + "AND reference in (:abac$references_1_challenged) "
            + "AND state in (:abac$states_1_challenged) "
            + "AND security_classification in (:abac$classifications_1_challenged) ) ) "
            + "AND NOT reference in (:abac$case_ids_excluded) )";

        assertNotNull(query);
        assertEquals(expectedValue, query);
        assertBoundParams(query, params,
            "abac$jurisdiction_1_challenged", "Test",
            "abac$references_1_challenged", List.of("caseId1"),
            "abac$states_1_challenged", STATES,
            "abac$classifications_1_challenged", PRIVATE_CLASSIFICATIONS,
            "abac$case_ids_excluded", Set.of("caseId1"));
    }

    @Test
    void shouldReturnBasicAndExcludedQueryWhenRoleAssignmentsGrantTypeExists() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC,
            "CASE", "ROLE1", "PRIVATE", "", "", null);

        RoleAssignment excludedRoleAssignment = createRoleAssignment(GrantType.EXCLUDED,
            "CASE", "ROLE2", "PRIVATE", "Test", "loc1", "reg1",
            null, "caseId1");

        String query = accessControlGrantTypeQueryBuilder
            .createQuery(Lists.newArrayList(roleAssignment, excludedRoleAssignment),
                params,
                caseTypeDefinition);

        String expectedValue =  " AND ( ( state in (:abac$states_1_basic) "
            + "AND security_classification in (:abac$classifications_1_basic) ) "
            + "AND NOT reference in (:abac$case_ids_excluded) )";

        assertNotNull(query);
        assertEquals(expectedValue, query);
        assertBoundParams(query, params,
            "abac$states_1_basic", STATES,
            "abac$classifications_1_basic", PRIVATE_CLASSIFICATIONS,
            "abac$case_ids_excluded", Set.of("caseId1"));
    }

    @Test
    void shouldReturnChallengedOrganisationalQueryWhenRoleAssignmentsGrantTypeExists() {
        RoleAssignment challengedRoleAssignment = createRoleAssignment(GrantType.CHALLENGED,
            "CASE", "ROLE1", "PRIVATE", "Test", "", "",
            Lists.newArrayList("auth1"), "caseId1");

        String query = accessControlGrantTypeQueryBuilder
            .createQuery(Lists.newArrayList(challengedRoleAssignment),
                params,
                caseTypeDefinition);

        String expectedValue = " AND ( ( ( jurisdiction = :abac$jurisdiction_1_challenged "
            + "AND reference in (:abac$references_1_challenged) "
            + "AND state in (:abac$states_1_challenged) "
            + "AND security_classification in (:abac$classifications_1_challenged) ) ) )";
        assertNotNull(query);
        assertEquals(expectedValue, query);
        assertBoundParams(query, params,
            "abac$jurisdiction_1_challenged", "Test",
            "abac$references_1_challenged", List.of("caseId1"),
            "abac$states_1_challenged", STATES,
            "abac$classifications_1_challenged", PRIVATE_CLASSIFICATIONS);
    }

    @Test
    void shouldReturnOnlyExcludedOrganisationalQuery() {
        RoleAssignment excludedRoleAssignment = createRoleAssignment(GrantType.EXCLUDED,
            "CASE", "ROLE1", "PRIVATE", "Test", "loc1", "reg1",
            null, "caseId1");

        String query = accessControlGrantTypeQueryBuilder
            .createQuery(Lists.newArrayList(excludedRoleAssignment),
                params,
                caseTypeDefinition);

        String expectedValue =  " AND NOT reference in (:abac$case_ids_excluded)";

        assertNotNull(query);
        assertEquals(expectedValue, query);
        assertBoundParams(query, params,
            "abac$case_ids_excluded", Set.of("caseId1"));
    }
}
