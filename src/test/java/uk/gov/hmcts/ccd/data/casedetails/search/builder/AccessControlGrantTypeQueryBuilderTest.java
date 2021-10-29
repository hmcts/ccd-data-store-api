package uk.gov.hmcts.ccd.data.casedetails.search.builder;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessControlGrantTypeQueryBuilderTest extends GrantTypeQueryBuilderTest {

    private AccessControlGrantTypeQueryBuilder accessControlGrantTypeQueryBuilder;

    @Mock
    private AccessControlService accessControlService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        accessControlGrantTypeQueryBuilder = new AccessControlGrantTypeQueryBuilder(
            new BasicGrantTypeQueryBuilder(accessControlService),
            new SpecificGrantTypeQueryBuilder(accessControlService),
            new StandardGrantTypeQueryBuilder(accessControlService),
            new ChallengedGrantTypeQueryBuilder(accessControlService),
            new ExcludedGrantTypeQueryBuilder(accessControlService));
    }

    @Test
    void shouldReturnEmptyQueryWhenNoRoleAssignmentsExists() {
        String query = accessControlGrantTypeQueryBuilder.createQuery(Lists.newArrayList(),
            Maps.newHashMap(),
            Lists.newArrayList(),
            Sets.newHashSet());
        assertNotNull(query);
        assertTrue(StringUtils.isBlank(query));
    }

    @Test
    void shouldReturnBasicQueryWhenRoleAssignmentsWithBasicGrantTypeExists() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC, "CASE", "PRIVATE", "", "", null);
        String query = accessControlGrantTypeQueryBuilder
            .createQuery(Lists.newArrayList(roleAssignment), Maps.newHashMap(),
                Lists.newArrayList(),
                Sets.newHashSet());
        String expectedValue =  " AND ( ( security_classification in (:classifications_basic) ) )";
        assertNotNull(query);
        assertEquals(expectedValue, query);
    }


    @Test
    void shouldReturnNonOrganisationalQueryWhenRoleAssignmentsGrantTypeExists() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC,
            "CASE", "PRIVATE", "", "", null);
        RoleAssignment specificRoleAssignment = createRoleAssignment(GrantType.SPECIFIC,
            "CASE", "PRIVATE", "Test", "", "", null, "caseId1");
        String query = accessControlGrantTypeQueryBuilder
            .createQuery(Lists.newArrayList(roleAssignment,
                specificRoleAssignment),
                Maps.newHashMap(),
                Lists.newArrayList(),
                Sets.newHashSet());
        String expectedValue =  " AND ( ( ( security_classification in (:classifications_basic) ) "
            + "OR ( security_classification in (:classifications_specific) "
            + "AND jurisdiction in (:jurisdictions_specific) AND reference in (:case_ids_specific) ) ) )";
        assertNotNull(query);
        assertEquals(expectedValue, query);
    }

    @Test
    void shouldReturnOrgAndNonOrganisationalQueryWhenRoleAssignmentsGrantTypeExists() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC,
            "CASE", "PRIVATE", "", "", null);
        RoleAssignment specificRoleAssignment = createRoleAssignment(GrantType.SPECIFIC,
            "CASE", "PRIVATE", "Test", "", "", null, "caseId1");

        RoleAssignment challengedRoleAssignment = createRoleAssignment(GrantType.CHALLENGED,
            "CASE", "PRIVATE", "Test", "", "",
            Lists.newArrayList("auth1"), "caseId1");

        RoleAssignment standardRoleAssignment = createRoleAssignment(GrantType.STANDARD,
            "CASE", "PRIVATE", "Test", "loc1", "reg1", null, "caseId1");
        String query = accessControlGrantTypeQueryBuilder.createQuery(Lists.newArrayList(roleAssignment,
            specificRoleAssignment, challengedRoleAssignment, standardRoleAssignment),
            Maps.newHashMap(), Lists.newArrayList(), Sets.newHashSet());

        String expectedValue =  " AND ( ( ( security_classification in (:classifications_basic) ) "
            + "OR ( security_classification in (:classifications_specific) "
            + "AND jurisdiction in (:jurisdictions_specific) "
            + "AND reference in (:case_ids_specific) ) ) "
            + "OR ( ( security_classification in (:classifications_standard)"
            + " AND ( ( jurisdiction='Test' AND data #>> '{caseManagementLocation,region}'='reg1' "
            + "AND data #>> '{caseManagementLocation,baseLocation}'='loc1' ) ) ) "
            + "OR ( security_classification in (:classifications_challenged) "
            + "AND jurisdiction in (:jurisdictions_challenged) ) ) )";

        assertNotNull(query);
        assertEquals(expectedValue, query);
    }

    @Test
    void shouldReturnOrgAndNonAndExcludedOrganisationalQueryWhenRoleAssignmentsGrantTypeExists() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC,
            "CASE", "PRIVATE", "", "", null);
        RoleAssignment specificRoleAssignment = createRoleAssignment(GrantType.SPECIFIC,
            "CASE", "PRIVATE", "Test", "", "", null, "caseId1");

        RoleAssignment challengedRoleAssignment = createRoleAssignment(GrantType.CHALLENGED,
            "CASE", "PRIVATE", "Test", "", "",
            Lists.newArrayList("auth1"), "caseId1");

        RoleAssignment standardRoleAssignment = createRoleAssignment(GrantType.STANDARD,
            "CASE", "PRIVATE", "Test", "loc1", "reg1", null, "caseId1");

        RoleAssignment excludedRoleAssignment = createRoleAssignment(GrantType.EXCLUDED,
            "CASE", "PRIVATE", "Test", "loc1", "reg1", null, "caseId1");

        String query = accessControlGrantTypeQueryBuilder.createQuery(Lists.newArrayList(roleAssignment,
            specificRoleAssignment, challengedRoleAssignment,
            standardRoleAssignment, excludedRoleAssignment),
            Maps.newHashMap(),
            Lists.newArrayList(),
            Sets.newHashSet());

        String expectedValue =  " AND ( ( ( security_classification in (:classifications_basic) ) "
            + "OR ( security_classification in (:classifications_specific) "
            + "AND jurisdiction in (:jurisdictions_specific) "
            + "AND reference in (:case_ids_specific) ) ) "
            + "OR ( ( ( security_classification in (:classifications_standard) "
            + "AND ( ( jurisdiction='Test' AND data #>> '{caseManagementLocation,region}'='reg1' "
            + "AND data #>> '{caseManagementLocation,baseLocation}'='loc1' ) ) ) "
            + "OR ( security_classification in (:classifications_challenged) "
            + "AND jurisdiction in (:jurisdictions_challenged) ) ) "
            + "AND NOT ( security_classification in (:classifications_excluded) "
            + "AND reference in (:case_ids_excluded) ) ) )";

        assertNotNull(query);
        assertEquals(expectedValue, query);
    }

    @Test
    void shouldReturnChallengedAndExcludedOrganisationalQueryWhenRoleAssignmentsGrantTypeExists() {
        RoleAssignment challengedRoleAssignment = createRoleAssignment(GrantType.CHALLENGED,
            "CASE", "PRIVATE", "Test", "", "",
            Lists.newArrayList("auth1"), "caseId1");

        RoleAssignment excludedRoleAssignment = createRoleAssignment(GrantType.EXCLUDED,
            "CASE", "PRIVATE", "Test", "loc1", "reg1", null, "caseId1");

        String query = accessControlGrantTypeQueryBuilder
            .createQuery(Lists.newArrayList(challengedRoleAssignment,
                excludedRoleAssignment),
                Maps.newHashMap(),
                Lists.newArrayList(),
                Sets.newHashSet());

        String expectedValue =  " AND ( ( ( security_classification in (:classifications_challenged) "
            + "AND jurisdiction in (:jurisdictions_challenged) ) ) "
            + "AND NOT ( security_classification in (:classifications_excluded) "
            + "AND reference in (:case_ids_excluded) ) )";

        assertNotNull(query);
        assertEquals(expectedValue, query);
    }

    @Test
    void shouldReturnBasicAndExcludedQueryWhenRoleAssignmentsGrantTypeExists() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC,
            "CASE", "PRIVATE", "", "", null);

        RoleAssignment excludedRoleAssignment = createRoleAssignment(GrantType.EXCLUDED,
            "CASE", "PRIVATE", "Test", "loc1", "reg1", null, "caseId1");

        String query = accessControlGrantTypeQueryBuilder
            .createQuery(Lists.newArrayList(roleAssignment, excludedRoleAssignment),
                Maps.newHashMap(),
                Lists.newArrayList(),
                Sets.newHashSet());

        String expectedValue =  " AND ( ( security_classification in (:classifications_basic) ) "
            + "AND NOT ( security_classification in (:classifications_excluded) "
            + "AND reference in (:case_ids_excluded) ) )";

        assertNotNull(query);
        assertEquals(expectedValue, query);
    }

    @Test
    void shouldReturnChallengedOrganisationalQueryWhenRoleAssignmentsGrantTypeExists() {
        RoleAssignment challengedRoleAssignment = createRoleAssignment(GrantType.CHALLENGED,
            "CASE", "PRIVATE", "Test", "", "",
            Lists.newArrayList("auth1"), "caseId1");

        String query = accessControlGrantTypeQueryBuilder
            .createQuery(Lists.newArrayList(challengedRoleAssignment),
                Maps.newHashMap(),
                Lists.newArrayList(),
                Sets.newHashSet());

        String expectedValue =  " AND ( ( ( security_classification in (:classifications_challenged)"
            + " AND jurisdiction in (:jurisdictions_challenged) ) ) )";
        assertNotNull(query);
        assertEquals(expectedValue, query);
    }

    @Test
    void shouldReturnOnlyExcludedOrganisationalQuery() {
        RoleAssignment excludedRoleAssignment = createRoleAssignment(GrantType.EXCLUDED,
            "CASE", "PRIVATE", "Test", "loc1", "reg1", null, "caseId1");

        String query = accessControlGrantTypeQueryBuilder
            .createQuery(Lists.newArrayList(excludedRoleAssignment),
                Maps.newHashMap(),
                Lists.newArrayList(),
                Sets.newHashSet());

        String expectedValue =  " AND NOT ( security_classification in "
            + "(:classifications_excluded) AND reference in (:case_ids_excluded) )";

        assertNotNull(query);
        assertEquals(expectedValue, query);
    }
}
