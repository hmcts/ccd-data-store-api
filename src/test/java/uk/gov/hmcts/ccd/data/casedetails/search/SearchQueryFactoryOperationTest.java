package uk.gov.hmcts.ccd.data.casedetails.search;

import com.google.common.collect.Maps;
import java.io.IOException;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.casedetails.search.builder.AccessControlGrantTypeQueryBuilder;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.security.AuthorisedCaseDefinitionDataService;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SearchQueryFactoryOperationTest {


    private static final String META_DATA_0_VALUE = "someValue";
    private static final String META_DATA_1_VALUE = "TESTJ";

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
        MockitoAnnotations.initMocks(this);
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
}
