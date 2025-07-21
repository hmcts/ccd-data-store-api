package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.builder;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.RoleToAccessProfileDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
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
        basicGrantTypeESQueryBuilder =
            new BasicGrantTypeESQueryBuilder(accessControlService, caseDataAccessControl, applicationParams);
    }

    @Test
    void shouldIncludeMustQueryWhenClassificationPresentInRoleAssignment() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC, "CASE", "PRIVATE", "", "", null);

        Query query = basicGrantTypeESQueryBuilder.createQuery(List.of(roleAssignment), caseTypeDefinition);

        assertThat(query).isNotNull();
    }

    @Test
    void shouldNotIncludeMustQueryWhenClassificationMissingInRoleAssignment() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC, "CASE", "", "", "", null);

        Query query = basicGrantTypeESQueryBuilder.createQuery(List.of(roleAssignment), caseTypeDefinition);

        assertThat(query).isNotNull();
    }

    @Test
    void shouldIncludeShouldQueryWhenCaseTypeContainsCaseAccessCategory() {
        RoleAssignment roleAssignment = createRoleAssignment(
            GrantType.BASIC, "CASE", "", "", "", "", null, "", ROLE_NAME_1, null
        );

        CaseStateDefinition caseStateDefinition = mock(CaseStateDefinition.class);
        when(caseStateDefinition.getId()).thenReturn("STATE-1");
        List<CaseStateDefinition> caseStateDefinitions = List.of(caseStateDefinition);
        when(accessControlService.filterCaseStatesByAccess(anyList(), anySet(), any()))
            .thenReturn(caseStateDefinitions);

        List<RoleToAccessProfileDefinition> roleToAccessProfileDefinitions = mockRoleToAccessProfileDefinitions(
            ROLE_NAME_1,
            CASE_TYPE_ID_1,
            1,
            false,
            null,
            "Civil/Standard,Crime/Standard"
        );
        when(caseTypeDefinition.getRoleToAccessProfiles()).thenReturn(roleToAccessProfileDefinitions);

        Query query = basicGrantTypeESQueryBuilder.createQuery(List.of(roleAssignment), caseTypeDefinition);

        assertThat(query).isNotNull();
        assertThat(query.toString()).contains("caseAccessCategory");
    }

    @Test
    void shouldIncludeShouldQueryWhenCaseTypeContainsCaseAccessGroup() {
        RoleAssignment roleAssignment = createRoleAssignment(
            GrantType.BASIC, "CASE", "", "", "", "", null, "", ROLE_NAME_1, "caseAccesGroupId1"
        );

        when(applicationParams.getCaseGroupAccessFilteringEnabled()).thenReturn(true);

        CaseStateDefinition caseStateDefinition = mock(CaseStateDefinition.class);
        when(caseStateDefinition.getId()).thenReturn("STATE-1");
        List<CaseStateDefinition> caseStateDefinitions = List.of(caseStateDefinition);
        when(accessControlService.filterCaseStatesByAccess(anyList(), anySet(), any()))
            .thenReturn(caseStateDefinitions);

        List<RoleToAccessProfileDefinition> roleToAccessProfileDefinitions = mockRoleToAccessProfileDefinitions(
            ROLE_NAME_1,
            CASE_TYPE_ID_1,
            1,
            false,
            null,
            "Civil/Standard,Crime/Standard"
        );
        when(caseTypeDefinition.getRoleToAccessProfiles()).thenReturn(roleToAccessProfileDefinitions);

        Query query = basicGrantTypeESQueryBuilder.createQuery(List.of(roleAssignment), caseTypeDefinition);

        assertThat(query).isNotNull();
        assertThat(query.toString()).contains("CaseAccessGroups");
    }

    @Test
    void shouldNotIncludeCaseAccessGroupWhenFilteringDisabled() {
        RoleAssignment roleAssignment = createRoleAssignment(
            GrantType.BASIC, "CASE", "", "", "", "", null, "", ROLE_NAME_1, "caseAccesGroupId1"
        );

        when(applicationParams.getCaseGroupAccessFilteringEnabled()).thenReturn(false);

        CaseStateDefinition caseStateDefinition = mock(CaseStateDefinition.class);
        when(caseStateDefinition.getId()).thenReturn("STATE-1");
        List<CaseStateDefinition> caseStateDefinitions = List.of(caseStateDefinition);
        when(accessControlService.filterCaseStatesByAccess(anyList(), anySet(), any()))
            .thenReturn(caseStateDefinitions);

        List<RoleToAccessProfileDefinition> roleToAccessProfileDefinitions = mockRoleToAccessProfileDefinitions(
            ROLE_NAME_1,
            CASE_TYPE_ID_1,
            1,
            false,
            null,
            "Civil/Standard,Crime/Standard"
        );
        when(caseTypeDefinition.getRoleToAccessProfiles()).thenReturn(roleToAccessProfileDefinitions);

        Query query = basicGrantTypeESQueryBuilder.createQuery(List.of(roleAssignment), caseTypeDefinition);

        assertThat(query).isNotNull();
        assertThat(query.toString()).doesNotContain("CaseAccessGroups");
    }
}
