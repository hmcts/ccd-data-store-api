package uk.gov.hmcts.ccd.data.casedetails.search.builder;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.GrantType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BasicGrantTypeQueryBuilderTest extends GrantTypeQueryBuilderTest {

    private BasicGrantTypeQueryBuilder basicGrantTypeQueryBuilder;

    @BeforeEach
    void setUp() {
        basicGrantTypeQueryBuilder = new BasicGrantTypeQueryBuilder();
    }

    @Test
    void shouldReturnQueryWhenRoleAssignmentHasClassifications() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC, "CASE", "PRIVATE", "", "", null);
        String query = basicGrantTypeQueryBuilder
            .createQuery(Lists.newArrayList(roleAssignment), Maps.newHashMap());

        assertNotNull(query);
        String expectedValue =  "( security_classification in (:classifications) )";
        assertEquals(expectedValue, query);
    }

    @Test
    void shouldReturnEmptyQueryWhenRoleAssignmentHasAuthorisations() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC,
            "CASE",
            "PRIVATE", "", "",
            Lists.newArrayList("auth1"));
        String query = basicGrantTypeQueryBuilder
            .createQuery(Lists.newArrayList(roleAssignment), Maps.newHashMap());

        assertNotNull(query);
        assertEquals("", query);
    }

    @Test
    void shouldReturnEmptyQueryWhenRoleAssignmentHasNoBasicGrantType() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.CHALLENGED,
            "CASE",
            "PRIVATE", "", "", null);
        String query = basicGrantTypeQueryBuilder
            .createQuery(Lists.newArrayList(roleAssignment), Maps.newHashMap());

        assertNotNull(query);
        assertEquals("", query);
    }

    @Test
    void shouldReturnQueryWhenAtLeastOneRoleAssignmentHasNoAuthorisations() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC,
            "CASE",
            "PRIVATE", "", "",
            Lists.newArrayList("auth1"));

        RoleAssignment roleAssignment2 = createRoleAssignment(GrantType.BASIC,
            "CASE",
            "PRIVATE", "", "",
            Lists.newArrayList());
        String query = basicGrantTypeQueryBuilder.createQuery(
            Lists.newArrayList(roleAssignment, roleAssignment2),
            Maps.newHashMap());

        assertNotNull(query);
        String expectedValue =  "( security_classification in (:classifications) )";
        assertEquals(expectedValue, query);
    }

    @Test
    void shouldReturnQueryWhenMoreThanOneRoleAssignmentHasNoAuthorisations() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC,
            "CASE",
            "PRIVATE", "", "",
            Lists.newArrayList("auth1"));

        RoleAssignment roleAssignment2 = createRoleAssignment(GrantType.BASIC,
            "CASE",
            "PRIVATE", "", "",
            Lists.newArrayList());

        RoleAssignment roleAssignment3 = createRoleAssignment(GrantType.BASIC,
            "CASE",
            "PUBLIC", "", "",
            Lists.newArrayList());

        String query = basicGrantTypeQueryBuilder.createQuery(
            Lists.newArrayList(roleAssignment, roleAssignment2),
            Maps.newHashMap());

        assertNotNull(query);
        String expectedValue =  "( security_classification in (:classifications) )";
        assertEquals(expectedValue, query);
    }
}
