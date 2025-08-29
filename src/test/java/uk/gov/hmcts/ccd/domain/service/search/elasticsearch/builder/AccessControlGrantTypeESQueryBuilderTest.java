package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.builder;

import com.google.common.collect.Lists;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

import java.util.List;

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
    private AccessControlService accessControlService;

    @Mock
    private CaseDataAccessControl caseDataAccessControl;

    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private CaseStateDefinition caseStateDefinition;

    private AccessControlGrantTypeESQueryBuilder accessControlGrantTypeQueryBuilder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        accessControlGrantTypeQueryBuilder = new AccessControlGrantTypeESQueryBuilder(
            new BasicGrantTypeESQueryBuilder(accessControlService, caseDataAccessControl, applicationParams),
            new SpecificGrantTypeESQueryBuilder(accessControlService, caseDataAccessControl, applicationParams),
            new StandardGrantTypeESQueryBuilder(accessControlService, caseDataAccessControl, applicationParams),
            new ChallengedGrantTypeESQueryBuilder(accessControlService, caseDataAccessControl, applicationParams),
            new ExcludedGrantTypeESQueryBuilder(accessControlService, caseDataAccessControl, applicationParams),
            caseDefinitionRepository,
            caseDataAccessControl);
        CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        when(caseDefinitionRepository.getCaseType(anyString())).thenReturn(caseTypeDefinition);
        when(caseStateDefinition.getId()).thenReturn("TestState");
        when(accessControlService.filterCaseStatesByAccess(Mockito.anyList(), any(), any()))
            .thenReturn(List.of(caseStateDefinition));
    }

    @Test
    void shouldReturnEmptyQueryWhenNoRoleAssignmentsExists() {
        when(caseDataAccessControl.generateRoleAssignments(any(CaseTypeDefinition.class)))
            .thenReturn(Lists.newArrayList());
        BoolQueryBuilder query = QueryBuilders.boolQuery();
        accessControlGrantTypeQueryBuilder.createQuery(CASE_TYPE_ID, query);
        assertNotNull(query);
        assertFalse(query.hasClauses());
    }

    @Test
    void shouldReturnBasicQueryWhenRoleAssignmentsWithBasicGrantTypeExists() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC, "CASE", "PRIVATE", "", "", null);
        when(caseDataAccessControl.generateRoleAssignments(any(CaseTypeDefinition.class)))
            .thenReturn(Lists.newArrayList(roleAssignment));


        BoolQueryBuilder query = QueryBuilders.boolQuery();
        accessControlGrantTypeQueryBuilder.createQuery(CASE_TYPE_ID, query);
        assertNotNull(query);
        assertEquals(1, query.must().size());
    }

    @Test
    void shouldReturnBasicQueryWhenRoleAssignmentsWithCaseAccessGroupsExists() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC, null, null, null, null, null, null,
            null, null, "caseAccessGroupId");
        when(caseDataAccessControl.generateRoleAssignments(any(CaseTypeDefinition.class)))
            .thenReturn(Lists.newArrayList(roleAssignment));


        BoolQueryBuilder query = QueryBuilders.boolQuery();
        accessControlGrantTypeQueryBuilder.createQuery(CASE_TYPE_ID, query);
        assertNotNull(query);
        assertEquals(1, query.must().size());
    }


    @Test
    void shouldReturnNonOrganisationalQueryWhenRoleAssignmentsGrantTypeExists() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC,
            "CASE", "PRIVATE", "", "", null);
        RoleAssignment specificRoleAssignment = createRoleAssignment(GrantType.SPECIFIC,
            "CASE", "PRIVATE", "Test", "", "", null, "caseId1");

        when(caseDataAccessControl.generateRoleAssignments(any(CaseTypeDefinition.class)))
            .thenReturn(Lists.newArrayList(roleAssignment, specificRoleAssignment));


        BoolQueryBuilder query = QueryBuilders.boolQuery();
        accessControlGrantTypeQueryBuilder.createQuery(CASE_TYPE_ID, query);
        assertNotNull(query);
        assertEquals(1, query.must().size());
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

        when(caseDataAccessControl.generateRoleAssignments(any(CaseTypeDefinition.class)))
            .thenReturn(Lists.newArrayList(roleAssignment, specificRoleAssignment,
                challengedRoleAssignment, standardRoleAssignment));


        BoolQueryBuilder query = QueryBuilders.boolQuery();
        accessControlGrantTypeQueryBuilder.createQuery(CASE_TYPE_ID, query);

        assertNotNull(query);
        assertEquals(1, query.must().size());
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

        when(caseDataAccessControl.generateRoleAssignments(any(CaseTypeDefinition.class)))
            .thenReturn(Lists.newArrayList(roleAssignment, specificRoleAssignment,
                challengedRoleAssignment, standardRoleAssignment, excludedRoleAssignment));

        BoolQueryBuilder query = QueryBuilders.boolQuery();
        accessControlGrantTypeQueryBuilder.createQuery(CASE_TYPE_ID, query);

        assertNotNull(query);
        assertEquals(1, query.must().size());
    }
}
