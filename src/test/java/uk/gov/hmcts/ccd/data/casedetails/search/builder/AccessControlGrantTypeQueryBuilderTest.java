package uk.gov.hmcts.ccd.data.casedetails.search.builder;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.GrantType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessControlGrantTypeQueryBuilderTest extends GrantTypeQueryBuilderTest {

    private AccessControlGrantTypeQueryBuilder accessControlGrantTypeQueryBuilder;

    @BeforeEach
    void setUp() {
        accessControlGrantTypeQueryBuilder = new AccessControlGrantTypeQueryBuilder(new BasicGrantTypeQueryBuilder(),
            new SpecificGrantTypeQueryBuilder(),
            new StandardGrantTypeQueryBuilder(),
            new ChallengedGrantTypeQueryBuilder(),
            new ExcludedGrantTypeQueryBuilder());
    }

    @Test
    void shouldReturnEmptyQueryWhenNoRoleAssignmentsExists() {
        String query = accessControlGrantTypeQueryBuilder.createQuery(Lists.newArrayList(), Maps.newHashMap());
        assertNotNull(query);
        assertTrue(StringUtils.isBlank(query));
    }

    @Test
    void shouldReturnBasicQueryWhenRoleAssignmentsWithBasicGrantTypeExists() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC, "CASE", "PRIVATE", "", "", null);
        String query = accessControlGrantTypeQueryBuilder
            .createQuery(Lists.newArrayList(roleAssignment), Maps.newHashMap());
        String expectedValue =  " AND ( ( ( security_classification in (:classifications) ) ) )";
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
            .createQuery(Lists.newArrayList(roleAssignment, specificRoleAssignment), Maps.newHashMap());
        String expectedValue =  " AND ( ( ( security_classification in (:classifications) ) "
            + "OR ( security_classification in (:classifications) "
            + "AND jurisdiction in (:jurisdictions) AND case_id in (:case_ids) ) ) )";
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
            specificRoleAssignment, challengedRoleAssignment, standardRoleAssignment), Maps.newHashMap());


        String expectedValue =  " AND ( ( ( security_classification in (:classifications) )"
            + " OR ( security_classification in (:classifications) "
            + "AND jurisdiction in (:jurisdictions) AND case_id in (:case_ids) ) )"
            + " OR ( ( security_classification in (:classifications) AND jurisdiction in (:jurisdictions) "
            + "AND region in (:regions) AND location in (:locations) ) OR "
            + "( security_classification in (:classifications) AND jurisdiction in (:jurisdictions) ) ) )";

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
            standardRoleAssignment, excludedRoleAssignment), Maps.newHashMap());


        String expectedValue =  " AND ( ( ( security_classification in (:classifications) ) "
            + "OR ( security_classification in (:classifications) AND jurisdiction in (:jurisdictions) "
            + "AND case_id in (:case_ids) ) ) "
            + "OR ( ( ( security_classification in (:classifications) AND jurisdiction in (:jurisdictions) "
            + "AND region in (:regions) AND location in (:locations) ) "
            + "OR ( security_classification in (:classifications) AND jurisdiction in (:jurisdictions) ) ) "
            + "AND NOT ( security_classification in (:classifications) AND case_id in (:case_ids) ) ) )";


        assertNotNull(query);
        assertEquals(expectedValue, query);
    }
}
