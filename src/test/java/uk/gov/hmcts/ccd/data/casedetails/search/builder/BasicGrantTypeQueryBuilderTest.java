package uk.gov.hmcts.ccd.data.casedetails.search.builder;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BasicGrantTypeQueryBuilderTest extends GrantTypeQueryBuilderTest {

    private BasicGrantTypeQueryBuilder basicGrantTypeQueryBuilder;

    @Mock
    private AccessControlService accessControlService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        basicGrantTypeQueryBuilder = new BasicGrantTypeQueryBuilder(accessControlService);
    }

    @Test
    void shouldReturnQueryWhenRoleAssignmentHasClassifications() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC, "CASE", "PRIVATE", "", "", null);
        String query = basicGrantTypeQueryBuilder
            .createQuery(Lists.newArrayList(roleAssignment), Maps.newHashMap(), null, Sets.newHashSet());

        assertNotNull(query);
        String expectedValue =  "( security_classification in (:classifications_basic) )";
        assertEquals(expectedValue, query);
    }

    @Test
    void shouldReturnEmptyQueryWhenRoleAssignmentHasAuthorisations() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC,
            "CASE",
            "PRIVATE", "", "",
            Lists.newArrayList("auth1"));
        String query = basicGrantTypeQueryBuilder
            .createQuery(Lists.newArrayList(roleAssignment), Maps.newHashMap(), null, Sets.newHashSet());

        assertNotNull(query);
        assertEquals("( security_classification in (:classifications_basic) )", query);
    }

    @Test
    void shouldReturnEmptyQueryWhenRoleAssignmentHasNoBasicGrantType() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.CHALLENGED,
            "CASE",
            "PRIVATE", "", "", null);
        String query = basicGrantTypeQueryBuilder
            .createQuery(Lists.newArrayList(roleAssignment), Maps.newHashMap(), null, Sets.newHashSet());

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
            Maps.newHashMap(), null, Sets.newHashSet());

        assertNotNull(query);
        String expectedValue =  "( security_classification in (:classifications_basic) )";
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
            Maps.newHashMap(), null, Sets.newHashSet());

        assertNotNull(query);
        String expectedValue =  "( security_classification in (:classifications_basic) )";
        assertEquals(expectedValue, query);
    }
}
