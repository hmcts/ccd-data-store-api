package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.builder;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
class AccessControlGrantTypeESQueryBuilderTest extends GrantTypeESQueryBuilderTest {

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

        // Fixed: disambiguate overloaded method using argument types
        when(accessControlService.filterCaseStatesByAccess(
            any(CaseTypeDefinition.class),
            any(Set.class),
            any(Predicate.class))
        ).thenReturn(List.of(caseStateDefinition));
    }

    @Test
    void shouldReturnEmptyQueryListWhenNoRoleAssignmentsExists() {
        when(caseDataAccessControl.generateRoleAssignments(any())).thenReturn(List.of());

        List<Query> filters = new ArrayList<>();
        accessControlGrantTypeQueryBuilder.createQuery(CASE_TYPE_ID, filters);

        assertNotNull(filters);
        assertEquals(0, filters.size());
    }

    @Test
    void shouldAddBasicGrantTypeQuery() {
        RoleAssignment role = createRoleAssignment(GrantType.BASIC, "CASE", "PRIVATE", "",
            "", null);
        when(caseDataAccessControl.generateRoleAssignments(any())).thenReturn(List.of(role));

        List<Query> filters = new ArrayList<>();
        accessControlGrantTypeQueryBuilder.createQuery(CASE_TYPE_ID, filters);

        assertNotNull(filters);
        assertEquals(1, filters.size());
    }

    @Test
    void shouldAddQueryForCaseAccessGroup() {
        RoleAssignment role = createRoleAssignment(GrantType.BASIC, null, null, null,
            null, null, null,
            null, null, "caseAccessGroupId");
        when(caseDataAccessControl.generateRoleAssignments(any())).thenReturn(List.of(role));

        List<Query> filters = new ArrayList<>();
        accessControlGrantTypeQueryBuilder.createQuery(CASE_TYPE_ID, filters);

        assertNotNull(filters);
        assertEquals(1, filters.size());
    }

    @Test
    void shouldReturnQueryForMultipleNonOrganisationalGrantTypes() {
        RoleAssignment basic = createRoleAssignment(GrantType.BASIC, "CASE", "PRIVATE", "",
            "", null);
        RoleAssignment specific = createRoleAssignment(GrantType.SPECIFIC, "CASE", "PRIVATE",
            "Test", "", "", null, "caseId1");

        when(caseDataAccessControl.generateRoleAssignments(any())).thenReturn(List.of(basic, specific));

        List<Query> filters = new ArrayList<>();
        accessControlGrantTypeQueryBuilder.createQuery(CASE_TYPE_ID, filters);

        assertNotNull(filters);
        assertEquals(1, filters.size()); // Combined under one bool query
    }

    @Test
    void shouldReturnQueryForOrgAndNonOrgGrants() {
        RoleAssignment basic = createRoleAssignment(GrantType.BASIC, "CASE", "PRIVATE",
            "", "", null);
        RoleAssignment specific = createRoleAssignment(GrantType.SPECIFIC, "CASE", "PRIVATE",
            "Test", "", "", null, "caseId1");
        RoleAssignment challenged = createRoleAssignment(GrantType.CHALLENGED, "CASE", "PRIVATE",
            "Test", "", "",
            List.of("auth1"), "caseId1");
        RoleAssignment standard = createRoleAssignment(GrantType.STANDARD, "CASE", "PRIVATE",
            "Test", "loc1", "reg1", null, "caseId1");

        when(caseDataAccessControl.generateRoleAssignments(any())).thenReturn(List.of(basic, specific, challenged,
            standard));

        List<Query> filters = new ArrayList<>();
        accessControlGrantTypeQueryBuilder.createQuery(CASE_TYPE_ID, filters);

        assertNotNull(filters);
        assertEquals(1, filters.size());
    }

    @Test
    void shouldReturnQueryForOrgNonOrgAndExcludedGrants() {
        RoleAssignment basic = createRoleAssignment(GrantType.BASIC, "CASE", "PRIVATE", "",
            "", null);
        RoleAssignment specific = createRoleAssignment(GrantType.SPECIFIC, "CASE", "PRIVATE",
            "Test", "", "", null, "caseId1");
        RoleAssignment challenged = createRoleAssignment(GrantType.CHALLENGED, "CASE", "PRIVATE",
            "Test", "", "",
            List.of("auth1"), "caseId1");
        RoleAssignment standard = createRoleAssignment(GrantType.STANDARD, "CASE", "PRIVATE",
            "Test", "loc1", "reg1", null, "caseId1");
        RoleAssignment excluded = createRoleAssignment(GrantType.EXCLUDED, "CASE", "PRIVATE",
            "Test", "loc1", "reg1", null, "caseId1");

        when(caseDataAccessControl.generateRoleAssignments(any()))
            .thenReturn(List.of(basic, specific, challenged, standard, excluded));

        List<Query> filters = new ArrayList<>();
        accessControlGrantTypeQueryBuilder.createQuery(CASE_TYPE_ID, filters);

        assertNotNull(filters);
        assertEquals(1, filters.size());
    }
}
