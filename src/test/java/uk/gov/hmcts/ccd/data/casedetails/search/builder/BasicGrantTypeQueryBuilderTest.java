package uk.gov.hmcts.ccd.data.casedetails.search.builder;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
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

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        basicGrantTypeQueryBuilder = new BasicGrantTypeQueryBuilder(accessControlService, caseDataAccessControl);
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
}
