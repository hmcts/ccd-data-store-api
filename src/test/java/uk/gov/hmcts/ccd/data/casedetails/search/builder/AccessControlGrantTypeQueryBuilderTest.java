package uk.gov.hmcts.ccd.data.casedetails.search.builder;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

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

    @BeforeEach
    void setUp() {
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
            Maps.newHashMap(), caseTypeDefinition);
        assertNotNull(query);
        assertTrue(StringUtils.isBlank(query));
    }

    @Test
    void shouldReturnBasicQueryWhenRoleAssignmentsWithBasicGrantTypeExists() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC, "CASE", "ROLE1", "PRIVATE", "", "", null);
        String query = accessControlGrantTypeQueryBuilder
            .createQuery(Lists.newArrayList(roleAssignment), Maps.newHashMap(),
                caseTypeDefinition);
        String expectedValue =  " AND ( ( state in (:states_1_basic) "
            + "AND security_classification in (:classifications_1_basic) ) )";
        assertNotNull(query);
        assertEquals(expectedValue, query);
    }


    @Test
    void shouldReturnNonOrganisationalQueryWhenRoleAssignmentsGrantTypeExists() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC,
            "CASE", "ROLE1", "PRIVATE", "", "", null);
        RoleAssignment specificRoleAssignment = createRoleAssignment(GrantType.SPECIFIC,
            "CASE", "ROLE2", "PRIVATE", "Test", "", "", null, "1111111111");
        String query = accessControlGrantTypeQueryBuilder
            .createQuery(Lists.newArrayList(roleAssignment,
                specificRoleAssignment),
                Maps.newHashMap(),
                caseTypeDefinition);
        String expectedValue = " AND ( ( ( state in (:states_1_basic) "
            + "AND security_classification in (:classifications_1_basic) ) OR ( jurisdiction='Test' "
            + "AND reference = ANY(ARRAY(SELECT unnest(CAST(:references_1_specific AS bigint[])))) "
            + "AND state in (:states_1_specific) "
            + "AND security_classification in (:classifications_1_specific) ) ) )";
        assertNotNull(query);
        assertEquals(expectedValue, query);
    }

    @Test
    void shouldReturnOrgAndNonOrganisationalQueryWhenRoleAssignmentsGrantTypeExists() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC,
            "CASE", "ROLE1", "PRIVATE", "", "", null);
        RoleAssignment specificRoleAssignment = createRoleAssignment(GrantType.SPECIFIC,
            "CASE", "ROLE2", "PRIVATE", "Test", "", "", null, "1111111111");

        RoleAssignment challengedRoleAssignment = createRoleAssignment(GrantType.CHALLENGED,
            "CASE", "ROLE3", "PRIVATE", "Test", "", "",
            Lists.newArrayList("auth1"), "1111111111");

        RoleAssignment standardRoleAssignment = createRoleAssignment(GrantType.STANDARD,
            "CASE", "ROLE4", "PRIVATE", "Test", "loc1", "reg1", null, "1111111111");
        String query = accessControlGrantTypeQueryBuilder.createQuery(Lists.newArrayList(roleAssignment,
            specificRoleAssignment, challengedRoleAssignment, standardRoleAssignment),
            Maps.newHashMap(), caseTypeDefinition);

        String expectedValue =  " AND ( ( ( state in (:states_1_basic) "
            + "AND security_classification in (:classifications_1_basic) ) OR ( jurisdiction='Test' "
            + "AND reference = ANY(ARRAY(SELECT unnest(CAST(:references_1_specific AS bigint[])))) "
            + "AND state in (:states_1_specific) "
            + "AND security_classification in (:classifications_1_specific) ) ) OR ( ( jurisdiction='Test' "
            + "AND data #>> '{caseManagementLocation,region}'='reg1' "
            + "AND data #>> '{caseManagementLocation,baseLocation}'='loc1' "
            + "AND reference = ANY(ARRAY(SELECT unnest(CAST(:references_1_standard AS bigint[])))) "
            + "AND state in (:states_1_standard) "
            + "AND security_classification in (:classifications_1_standard) ) OR ( jurisdiction='Test' "
            + "AND reference = ANY(ARRAY(SELECT unnest(CAST(:references_1_challenged AS bigint[])))) "
            + "AND state in (:states_1_challenged) "
            + "AND security_classification in (:classifications_1_challenged) ) ) )";

        assertNotNull(query);
        assertEquals(expectedValue, query);
    }

    @Test
    void shouldReturnOrgAndNonAndExcludedOrganisationalQueryWhenRoleAssignmentsGrantTypeExists() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC,
            "CASE", "ROLE1", "PRIVATE", "", "", null);
        RoleAssignment specificRoleAssignment = createRoleAssignment(GrantType.SPECIFIC,
            "CASE", "ROLE2", "PRIVATE", "Test", "", "", null, "1111111111");

        RoleAssignment challengedRoleAssignment = createRoleAssignment(GrantType.CHALLENGED,
            "CASE", "ROLE3", "PRIVATE", "Test", "", "",
            Lists.newArrayList("auth1"), "1111111111");

        RoleAssignment standardRoleAssignment = createRoleAssignment(GrantType.STANDARD,
            "CASE", "ROLE4", "PRIVATE", "Test", "loc1", "reg1", null, "1111111111");

        RoleAssignment excludedRoleAssignment = createRoleAssignment(GrantType.EXCLUDED,
            "CASE", "ROLE5", "PRIVATE", "Test", "loc1", "reg1", null, "1111111111");

        String query = accessControlGrantTypeQueryBuilder.createQuery(Lists.newArrayList(roleAssignment,
            specificRoleAssignment, challengedRoleAssignment,
            standardRoleAssignment, excludedRoleAssignment),
            Maps.newHashMap(),
            caseTypeDefinition);

        String expectedValue =  " AND ( ( ( state in (:states_1_basic) "
            + "AND security_classification in (:classifications_1_basic) ) "
            + "OR ( jurisdiction='Test' "
            + "AND reference = ANY(ARRAY(SELECT unnest(CAST(:references_1_specific AS bigint[])))) "
            + "AND state in (:states_1_specific) AND security_classification in (:classifications_1_specific) ) ) "
            + "OR ( ( ( jurisdiction='Test' AND data #>> '{caseManagementLocation,region}'='reg1' "
            + "AND data #>> '{caseManagementLocation,baseLocation}'='loc1' "
            + "AND reference = ANY(ARRAY(SELECT unnest(CAST(:references_1_standard AS bigint[])))) "
            + "AND state in (:states_1_standard) AND security_classification in (:classifications_1_standard) ) "
            + "OR ( jurisdiction='Test' "
            + "AND reference = ANY(ARRAY(SELECT unnest(CAST(:references_1_challenged AS bigint[])))) "
            + "AND state in (:states_1_challenged) AND security_classification in (:classifications_1_challenged) ) ) "
            + "AND NOT reference = ANY(ARRAY(SELECT unnest(CAST(:case_ids_excluded AS bigint[])))) ) )";

        assertNotNull(query);
        assertEquals(expectedValue, query);
    }

    @Test
    void shouldReturnChallengedAndExcludedOrganisationalQueryWhenRoleAssignmentsGrantTypeExists() {
        RoleAssignment challengedRoleAssignment = createRoleAssignment(GrantType.CHALLENGED,
            "CASE", "ROLE1", "PRIVATE", "Test", "", "",
            Lists.newArrayList("auth1"), "1111111111");

        RoleAssignment excludedRoleAssignment = createRoleAssignment(GrantType.EXCLUDED,
            "CASE", "ROLE2", "PRIVATE", "Test", "loc1", "reg1", null, "1111111111");

        String query = accessControlGrantTypeQueryBuilder
            .createQuery(Lists.newArrayList(challengedRoleAssignment,
                excludedRoleAssignment),
                Maps.newHashMap(),
                caseTypeDefinition);

        String expectedValue =  " AND ( ( ( jurisdiction='Test' "
            + "AND reference = ANY(ARRAY(SELECT unnest(CAST(:references_1_challenged AS bigint[])))) "
            + "AND state in (:states_1_challenged) AND security_classification in (:classifications_1_challenged) ) ) "
            + "AND NOT reference = ANY(ARRAY(SELECT unnest(CAST(:case_ids_excluded AS bigint[])))) )";

        assertNotNull(query);
        assertEquals(expectedValue, query);
    }

    @Test
    void shouldReturnBasicAndExcludedQueryWhenRoleAssignmentsGrantTypeExists() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC,
            "CASE", "ROLE1", "PRIVATE", "", "", null);

        RoleAssignment excludedRoleAssignment = createRoleAssignment(GrantType.EXCLUDED,
            "CASE", "ROLE2", "PRIVATE", "Test", "loc1", "reg1", null, "1111111111");

        String query = accessControlGrantTypeQueryBuilder
            .createQuery(Lists.newArrayList(roleAssignment, excludedRoleAssignment),
                Maps.newHashMap(),
                caseTypeDefinition);

        String expectedValue =  " AND ( ( state in (:states_1_basic) "
            + "AND security_classification in (:classifications_1_basic) ) "
            + "AND NOT reference = ANY(ARRAY(SELECT unnest(CAST(:case_ids_excluded AS bigint[])))) )";

        assertNotNull(query);
        assertEquals(expectedValue, query);
    }

    @Test
    void shouldReturnChallengedOrganisationalQueryWhenRoleAssignmentsGrantTypeExists() {
        RoleAssignment challengedRoleAssignment = createRoleAssignment(GrantType.CHALLENGED,
            "CASE", "ROLE1", "PRIVATE", "Test", "", "",
            Lists.newArrayList("auth1"), "1111111111");

        String query = accessControlGrantTypeQueryBuilder
            .createQuery(Lists.newArrayList(challengedRoleAssignment),
                Maps.newHashMap(),
                caseTypeDefinition);

        String expectedValue = " AND ( ( ( jurisdiction='Test' "
            + "AND reference = ANY(ARRAY(SELECT unnest(CAST(:references_1_challenged AS bigint[])))) "
            + "AND state in (:states_1_challenged) "
            + "AND security_classification in (:classifications_1_challenged) ) ) )";
        assertNotNull(query);
        assertEquals(expectedValue, query);
    }

    @Test
    void shouldReturnOnlyExcludedOrganisationalQuery() {
        RoleAssignment excludedRoleAssignment = createRoleAssignment(GrantType.EXCLUDED,
            "CASE", "ROLE1", "PRIVATE", "Test", "loc1", "reg1", null, "1111111111");

        String query = accessControlGrantTypeQueryBuilder
            .createQuery(Lists.newArrayList(excludedRoleAssignment),
                Maps.newHashMap(),
                caseTypeDefinition);

        String expectedValue =  " AND NOT reference = ANY(ARRAY(SELECT unnest(CAST(:case_ids_excluded AS bigint[]))))";

        assertNotNull(query);
        assertEquals(expectedValue, query);
    }

    @Test
    void shouldNotExceedPostgresMaxParametersWithLargeNumberOfRoleAssignments() {
        // Regression test for CCD-6129
        // When a user has a large number of specific case role assignments (e.g. from a bulk grant),
        // the generated SQL query must not exceed PostgreSQL's hard limit of 65,535 parameters.
        // Previously the IN clause expanded each case reference into an individual parameter,
        // causing PSQLException: PreparedStatement can have at most 65,535 parameters.
        List<RoleAssignment> roleAssignments = new ArrayList<>();
        for (int i = 0; i < 70000; i++) {
            roleAssignments.add(createRoleAssignment(
                GrantType.SPECIFIC,
                "CASE",
                "ROLE1",
                "PRIVATE",
                "Test",
                "",
                "",
                null,
                String.valueOf(i)
            ));
        }

        Map<String, Object> params = Maps.newHashMap();
        String result = accessControlGrantTypeQueryBuilder.createQuery(
            roleAssignments, params, caseTypeDefinition
        );

        assertThat(result).contains("reference = ANY");
        assertThat(result).doesNotContain("reference in (:");
    }
}
