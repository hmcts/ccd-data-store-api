package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.builder;

import com.google.common.collect.Lists;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.RoleAssignmentService;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
class AccessControlGrantTypeESQueryBuilderTest extends  GrantTypeESQueryBuilderTest {

    private static final String CASE_TYPE_ID = "FT_MasterCaseType";

    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;

    @Mock
    RoleAssignmentService roleAssignmentService;

    @Mock
    UserAuthorisation userAuthorisation;

    private AccessControlGrantTypeESQueryBuilder accessControlGrantTypeQueryBuilder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        accessControlGrantTypeQueryBuilder = new AccessControlGrantTypeESQueryBuilder(
            new BasicGrantTypeESQueryBuilder(),
            new SpecificGrantTypeESQueryBuilder(),
            new StandardGrantTypeESQueryBuilder(),
            new ChallengedGrantTypeESQueryBuilder(),
            new ExcludedGrantTypeESQueryBuilder(),
            caseDefinitionRepository,
            roleAssignmentService,
            userAuthorisation);
        CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        when(caseDefinitionRepository.getCaseType(anyString())).thenReturn(caseTypeDefinition);
        when(userAuthorisation.getUserId()).thenReturn("USER123");
    }

    @Test
    void shouldReturnEmptyQueryWhenNoRoleAssignmentsExists() {
        when(roleAssignmentService.getRoleAssignments(anyString(), any())).thenReturn(Lists.newArrayList());
        BoolQueryBuilder query = accessControlGrantTypeQueryBuilder.createQuery(CASE_TYPE_ID);
        assertNotNull(query);
        assertFalse(query.hasClauses());
    }

    @Test
    void shouldReturnBasicQueryWhenRoleAssignmentsWithBasicGrantTypeExists() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC, "CASE", "PRIVATE", "", "", null);
        when(roleAssignmentService.getRoleAssignments(anyString(), any()))
            .thenReturn(Lists.newArrayList(roleAssignment));
        BoolQueryBuilder query = accessControlGrantTypeQueryBuilder.createQuery(CASE_TYPE_ID);
        assertNotNull(query);
        assertEquals(1, query.should().size());
    }


    @Test
    void shouldReturnNonOrganisationalQueryWhenRoleAssignmentsGrantTypeExists() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC,
            "CASE", "PRIVATE", "", "", null);
        RoleAssignment specificRoleAssignment = createRoleAssignment(GrantType.SPECIFIC,
            "CASE", "PRIVATE", "Test", "", "", null, "caseId1");

        when(roleAssignmentService.getRoleAssignments(anyString(), any()))
            .thenReturn(Lists.newArrayList(roleAssignment, specificRoleAssignment));

        BoolQueryBuilder query = accessControlGrantTypeQueryBuilder.createQuery(CASE_TYPE_ID);
        assertNotNull(query);
        assertEquals(1, query.should().size());
        BoolQueryBuilder nonOrgQuery = (BoolQueryBuilder) query.should().get(0);
        assertNotNull(nonOrgQuery);
        assertEquals(2, nonOrgQuery.should().size());
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
        when(roleAssignmentService.getRoleAssignments(anyString(), any()))
            .thenReturn(Lists.newArrayList(roleAssignment, specificRoleAssignment,
            challengedRoleAssignment, standardRoleAssignment));


        BoolQueryBuilder query = accessControlGrantTypeQueryBuilder.createQuery(CASE_TYPE_ID);

        assertNotNull(query);
        assertEquals(2, query.should().size());
        BoolQueryBuilder nonOrgQuery = (BoolQueryBuilder) query.should().get(0);
        assertNotNull(nonOrgQuery);
        assertEquals(2, nonOrgQuery.should().size());

        BoolQueryBuilder orgQuery = (BoolQueryBuilder) query.should().get(1);
        assertNotNull(orgQuery);
        assertEquals(1, orgQuery.should().size());
        assertEquals(0, orgQuery.mustNot().size());
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

        when(roleAssignmentService.getRoleAssignments(anyString(), any()))
            .thenReturn(Lists.newArrayList(roleAssignment, specificRoleAssignment,
            challengedRoleAssignment, standardRoleAssignment, excludedRoleAssignment));


        BoolQueryBuilder query = accessControlGrantTypeQueryBuilder.createQuery(CASE_TYPE_ID);

        assertNotNull(query);
        assertEquals(2, query.should().size());
        BoolQueryBuilder nonOrgQuery = (BoolQueryBuilder) query.should().get(0);
        assertNotNull(nonOrgQuery);
        assertEquals(2, nonOrgQuery.should().size());

        BoolQueryBuilder orgQuery = (BoolQueryBuilder) query.should().get(1);
        assertNotNull(orgQuery);
        assertEquals(1, orgQuery.should().size());
        assertEquals(1, orgQuery.mustNot().size());
    }
}
