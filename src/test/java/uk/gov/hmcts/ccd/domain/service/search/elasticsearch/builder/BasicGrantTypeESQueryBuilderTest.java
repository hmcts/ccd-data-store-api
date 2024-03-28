package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.builder;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.RoleToAccessProfileDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BasicGrantTypeESQueryBuilderTest extends GrantTypeESQueryBuilderTest {

    private BasicGrantTypeESQueryBuilder basicGrantTypeESQueryBuilder;

    @Mock
    private AccessControlService accessControlService;

    @Mock
    private CaseDataAccessControl caseDataAccessControl;

    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private CaseTypeDefinition caseTypeDefinition;

    protected static final String CASE_TYPE_ID_1 = "CASE_TYPE_ID_1";
    protected static final String ROLE_NAME_1 = "ROLE1";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        basicGrantTypeESQueryBuilder = new BasicGrantTypeESQueryBuilder(accessControlService, caseDataAccessControl,
            applicationParams);
    }

    @Test
    void shouldIncludeMustQueryWhenClassificationPresentInRoleAssignment() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC, "CASE", "PRIVATE", "", "", null);
        BoolQueryBuilder query = basicGrantTypeESQueryBuilder.createQuery(Lists.newArrayList(roleAssignment),
            caseTypeDefinition);
        assertNotNull(query);
    }


    @Test
    void shouldNotIncludeMustQueryWhenClassificationPresentInRoleAssignment() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC, "CASE", "", "", "", null);
        BoolQueryBuilder query = basicGrantTypeESQueryBuilder.createQuery(Lists.newArrayList(roleAssignment),
            caseTypeDefinition);
        assertNotNull(query);
    }

    @Test
    void shouldIncludeShouldQueryWhenCaseTypeContainsCaseAccessCategory() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC,
            "CASE", "", "",
            "", "", null, "", ROLE_NAME_1, null);

        Set<String> caseStates = Sets.newHashSet("STATE-1");
        CaseStateDefinition caseStateDefinition = mock(CaseStateDefinition.class);
        when(caseStateDefinition.getId()).thenReturn("STATE-1");
        List<CaseStateDefinition> caseStateDefinitions = Lists.newArrayList(caseStateDefinition);
        when(accessControlService
            .filterCaseStatesByAccess(anyList(), anySet(), any())).thenReturn(caseStateDefinitions);

        List<RoleToAccessProfileDefinition> roleToAccessProfileDefinitions = mockRoleToAccessProfileDefinitions(
            ROLE_NAME_1,
            CASE_TYPE_ID_1,
            1,
            false,
            null,
            "Civil/Standard,Crime/Standard");
        when(caseTypeDefinition.getRoleToAccessProfiles()).thenReturn(roleToAccessProfileDefinitions);

        BoolQueryBuilder query = basicGrantTypeESQueryBuilder.createQuery(Lists.newArrayList(roleAssignment),
            caseTypeDefinition);

        assertNotNull(query);
        assertTrue(query.hasClauses());
        assertEquals(1, (query.should().size()));
    }

    @Test
    void shouldIncludeShouldQueryWhenCaseTypeContainsCaseAccessGroup() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC,
            "CASE", "", "",
            "", "", null, "", ROLE_NAME_1, "caseAccesGroupId1");

        when(applicationParams.getCaseGroupAccessFilteringEnabled()).thenReturn(true);
        CaseStateDefinition caseStateDefinition = mock(CaseStateDefinition.class);
        when(caseStateDefinition.getId()).thenReturn("STATE-1");
        List<CaseStateDefinition> caseStateDefinitions = Lists.newArrayList(caseStateDefinition);
        when(accessControlService
            .filterCaseStatesByAccess(anyList(), anySet(), any())).thenReturn(caseStateDefinitions);

        List<RoleToAccessProfileDefinition> roleToAccessProfileDefinitions = mockRoleToAccessProfileDefinitions(
            ROLE_NAME_1,
            CASE_TYPE_ID_1,
            1,
            false,
            null,
            "Civil/Standard,Crime/Standard");
        when(caseTypeDefinition.getRoleToAccessProfiles()).thenReturn(roleToAccessProfileDefinitions);

        BoolQueryBuilder query = basicGrantTypeESQueryBuilder.createQuery(Lists.newArrayList(roleAssignment),
            caseTypeDefinition);

        assertNotNull(query);
        assertTrue(query.hasClauses());
        assertTrue(query.toString().contains("CaseAccessGroups"));
    }

    @Test
    void shouldNotIncludeCaseAccessGroupForFilteringDisabled() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC,
            "CASE", "", "",
            "", "", null, "", ROLE_NAME_1, "caseAccesGroupId1");

        CaseStateDefinition caseStateDefinition = mock(CaseStateDefinition.class);
        when(caseStateDefinition.getId()).thenReturn("STATE-1");
        List<CaseStateDefinition> caseStateDefinitions = Lists.newArrayList(caseStateDefinition);
        when(accessControlService
            .filterCaseStatesByAccess(anyList(), anySet(), any())).thenReturn(caseStateDefinitions);

        List<RoleToAccessProfileDefinition> roleToAccessProfileDefinitions = mockRoleToAccessProfileDefinitions(
            ROLE_NAME_1,
            CASE_TYPE_ID_1,
            1,
            false,
            null,
            "Civil/Standard,Crime/Standard");
        when(caseTypeDefinition.getRoleToAccessProfiles()).thenReturn(roleToAccessProfileDefinitions);

        BoolQueryBuilder query = basicGrantTypeESQueryBuilder.createQuery(Lists.newArrayList(roleAssignment),
            caseTypeDefinition);

        assertNotNull(query);
        assertTrue(query.hasClauses());
        assertFalse(query.toString().contains("CaseAccessGroups"));
    }
}
