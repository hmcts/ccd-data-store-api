package uk.gov.hmcts.ccd.data.casedetails.search;

import com.google.common.collect.Maps;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.casedetails.search.builder.AccessControlGrantTypeQueryBuilder;
import uk.gov.hmcts.ccd.data.casedetails.search.builder.GrantTypeSqlQueryBuilder;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.security.AuthorisedCaseDefinitionDataService;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SearchQueryFactoryOperationTest {


    private static final String META_DATA_0_VALUE = "someValue";
    private static final String META_DATA_1_VALUE = "TESTJ";
    private static final String GRANTED_REGION = "GB-EAST";
    private static final String OTHER_REGION = "GB-WEST";

    private CriterionFactory criterionFactory = new CriterionFactory();

    @Mock
    private ApplicationParams applicationParam;

    @Mock
    private UserAuthorisation userAuthorisation;

    @Mock
    private SortOrderQueryBuilder sortOrderQueryBuilder;

    @Mock
    private AuthorisedCaseDefinitionDataService authorisedCaseDefinitionDataService;

    @Mock
    private EntityManager entityManager;

    @Mock
    private CaseTypeService caseTypeService;

    @Mock
    private AccessControlGrantTypeQueryBuilder accessControlGrantTypeQueryBuilder;

    @Mock
    private CaseDataAccessControl caseDataAccessControl;

    private SearchQueryFactoryOperation classUnderTest;

    @BeforeEach
    public void initMock() throws IOException {
        MockitoAnnotations.openMocks(this);
        CaseTypeDefinition caseTypeDefinition = mock(CaseTypeDefinition.class);
        when(caseTypeService.getCaseTypeForJurisdiction(anyString(), anyString())).thenReturn(caseTypeDefinition);
        classUnderTest = new SearchQueryFactoryOperation(criterionFactory,
            entityManager,
            applicationParam,
            userAuthorisation,
            sortOrderQueryBuilder,
            authorisedCaseDefinitionDataService,
            accessControlGrantTypeQueryBuilder,
            caseDataAccessControl,
            caseTypeService);
    }

    @Test
    void shouldUserCountWhenCountIsTrue() {
        MetaData metadata = new MetaData(META_DATA_0_VALUE, META_DATA_1_VALUE);

        TypedQuery query = mock(TypedQuery.class);
        when(authorisedCaseDefinitionDataService.getUserAuthorisedCaseStateIds(anyString(), anyString(), any()))
            .thenReturn(List.of("caseStateId_1"));
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);

        classUnderTest.build(metadata, Maps.newHashMap(), true);

        verify(sortOrderQueryBuilder).buildSortOrderClause(metadata);
        verify(entityManager).createNativeQuery(anyString());
    }

    @Test
    void shouldUserCountWhenCountIsFalse() {
        MetaData metadata = new MetaData(META_DATA_0_VALUE, META_DATA_1_VALUE);

        TypedQuery query = mock(TypedQuery.class);
        when(authorisedCaseDefinitionDataService.getUserAuthorisedCaseStateIds(anyString(), anyString(), any()))
            .thenReturn(List.of("caseStateId_1"));
        when(entityManager.createNativeQuery(anyString(), any(Class.class))).thenReturn(query);

        classUnderTest.build(metadata, Maps.newHashMap(), false);

        verify(sortOrderQueryBuilder).buildSortOrderClause(metadata);
        verify(entityManager).createNativeQuery(anyString(), any(Class.class));
    }

    @Test
    void shouldCallRoleAssignmentServiceWhenRAEnabled() {
        when(applicationParam.getEnableAttributeBasedAccessControl()).thenReturn(true);
        when(userAuthorisation.getUserId()).thenReturn("Test User");
        when(accessControlGrantTypeQueryBuilder.createQuery(anyList(), anyMap(), any(CaseTypeDefinition.class)))
            .thenReturn("Select * from case_data");
        when(caseDataAccessControl.generateRoleAssignments(any(CaseTypeDefinition.class)))
            .thenReturn(List.of(RoleAssignment.builder().build()));

        TypedQuery query = mock(TypedQuery.class);
        when(entityManager.createNativeQuery(anyString(), any(Class.class))).thenReturn(query);
        MetaData metadata = new MetaData(META_DATA_0_VALUE, META_DATA_1_VALUE);
        classUnderTest.build(metadata, Maps.newHashMap(), false);

        verify(sortOrderQueryBuilder).buildSortOrderClause(metadata);
        verify(caseDataAccessControl).generateRoleAssignments(any(CaseTypeDefinition.class));
        verify(entityManager).createNativeQuery(anyString(), any(Class.class));
    }

    @Test
    void shouldNotCallRoleAssignmentServiceWhenRANotEnabled() {
        when(applicationParam.getEnableAttributeBasedAccessControl()).thenReturn(false);
        when(userAuthorisation.getAccessLevel()).thenReturn(UserAuthorisation.AccessLevel.GRANTED);

        TypedQuery query = mock(TypedQuery.class);
        when(entityManager.createNativeQuery(anyString(), any(Class.class))).thenReturn(query);
        MetaData metadata = new MetaData(META_DATA_0_VALUE, META_DATA_1_VALUE);
        classUnderTest.build(metadata, Maps.newHashMap(), false);

        verify(sortOrderQueryBuilder).buildSortOrderClause(metadata);
        verify(userAuthorisation).getUserId();
        verify(entityManager).createNativeQuery(anyString(), any(Class.class));
    }

    @Test
    @DisplayName("A search field named after an ABAC parameter cannot overwrite the ABAC predicate value")
    void searchFieldMustNotOverwriteAccessControlParameterValue() {
        // "abac$region_1_basic" - what production emits for the first BASIC grant type group
        final String abacRegionParam = String.format(GrantTypeSqlQueryBuilder.REGION_PARAM, 1, "basic");
        // "region_1_basic" - what the search field `case.region_1_basic` sanitises down to
        final String searchField = abacRegionParam.substring(
            GrantTypeSqlQueryBuilder.ACCESS_CONTROL_PARAM_PREFIX.length());

        stubAbacQuery(params -> {
            params.put(abacRegionParam, GRANTED_REGION);
            return " AND ( data #>> '{caseManagementLocation,region}' = :" + abacRegionParam + " )";
        });

        Query query = mock(Query.class);
        when(entityManager.createNativeQuery(anyString(), any(Class.class))).thenReturn(query);

        Map<String, String> searchParams = new HashMap<>();
        searchParams.put(searchField, OTHER_REGION);

        classUnderTest.build(new MetaData(META_DATA_0_VALUE, META_DATA_1_VALUE), searchParams, false);

        verify(query).setParameter(abacRegionParam, GRANTED_REGION);
        verify(query, never()).setParameter(abacRegionParam, OTHER_REGION);
        // the search field still binds, it just no longer lands in the access control namespace
        verify(query).setParameter(searchField, OTHER_REGION);
    }

    @Test
    @DisplayName("Every ABAC parameter name sits in the reserved namespace no search field can reach")
    void accessControlParametersUseTheReservedNamespace() {
        assertTrue(GrantTypeSqlQueryBuilder.ACCESS_CONTROL_PARAM_PREFIX.contains("$"),
            "the prefix must contain a character the search field name validation rejects, otherwise it is "
                + "merely unlikely to collide rather than collision-proof");
        assertTrue(Stream.of(GrantTypeSqlQueryBuilder.CASE_STATES_PARAM,
                GrantTypeSqlQueryBuilder.CLASSIFICATIONS_PARAM,
                GrantTypeSqlQueryBuilder.REFERENCES_PARAM,
                GrantTypeSqlQueryBuilder.JURISDICTION_PARAM,
                GrantTypeSqlQueryBuilder.REGION_PARAM,
                GrantTypeSqlQueryBuilder.LOCATION_PARAM,
                GrantTypeSqlQueryBuilder.CASE_ACCESS_GROUP_ID_PARAM,
                GrantTypeSqlQueryBuilder.CASE_ACCESS_CATEGORY_PARAM,
                SearchQueryFactoryOperation.USER_ID_PARAM,
                SearchQueryFactoryOperation.CASE_STATES_PARAM)
                .allMatch(param -> param.startsWith(GrantTypeSqlQueryBuilder.ACCESS_CONTROL_PARAM_PREFIX)),
            "every access control parameter must be namespaced");
    }

    @Test
    @DisplayName("A search field name unreachable through sanitisation is still rejected by field validation")
    void reservedNamespaceIsUnreachableThroughFieldNameValidation() {
        Map<String, String> params = Map.of(
            "case." + GrantTypeSqlQueryBuilder.ACCESS_CONTROL_PARAM_PREFIX + "region_1_basic", OTHER_REGION);

        assertThrows(BadRequestException.class, () -> new FieldMapSanitizeOperation().execute(params),
            "a field name in the reserved namespace must never survive sanitisation");
    }

    @Test
    @DisplayName("A parameter collision fails closed instead of silently rebinding the access control value")
    void collidingAccessControlParameterIsRejected() {
        // simulates a regression in which an access control parameter loses its reserved prefix
        final String unprefixedParam = "region_1_basic";

        stubAbacQuery(params -> {
            params.put(unprefixedParam, GRANTED_REGION);
            return " AND ( data #>> '{caseManagementLocation,region}' = :" + unprefixedParam + " )";
        });

        Map<String, String> searchParams = new HashMap<>();
        searchParams.put(unprefixedParam, OTHER_REGION);
        MetaData metadata = new MetaData(META_DATA_0_VALUE, META_DATA_1_VALUE);

        assertThrows(BadRequestException.class, () -> classUnderTest.build(metadata, searchParams, false),
            "a collision must abort the search rather than bind the user's value into the ABAC predicate");
        verify(entityManager, never()).createNativeQuery(anyString(), any(Class.class));
    }

    private void stubAbacQuery(Function<Map<String, Object>, String> clauseBuilder) {
        when(applicationParam.getEnableAttributeBasedAccessControl()).thenReturn(true);
        when(caseDataAccessControl.generateRoleAssignments(any(CaseTypeDefinition.class)))
            .thenReturn(List.of(RoleAssignment.builder().build()));
        when(accessControlGrantTypeQueryBuilder.createQuery(anyList(), anyMap(), any(CaseTypeDefinition.class)))
            .thenAnswer(invocation -> clauseBuilder.apply(invocation.getArgument(1)));
    }
}
