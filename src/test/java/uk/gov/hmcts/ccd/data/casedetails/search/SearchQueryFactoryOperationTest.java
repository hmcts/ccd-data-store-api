package uk.gov.hmcts.ccd.data.casedetails.search;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity;
import uk.gov.hmcts.ccd.domain.service.security.AuthorisedCaseDefinitionDataService;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation;

public class SearchQueryFactoryOperationTest {

    private static final String TEST_CASE_TYPE_VALUE = "TEST_CASE_TYPE";
    private static final String TEST_JURISDICTION_VALUE = "JURISDICTION";
    private static final String TEST_FIELD_NAME = "case.name";
    private static final String TEST_FIELD_VALUE = "Tim";

    private SearchQueryFactoryOperation subject;
    private SearchQueryFactoryOperation subjectWithUserAuthValues;
    private final CriterionFactory criterionFactory = new CriterionFactory();
    private SortOrderQueryBuilder sortOrderQueryBuilder = new SortOrderQueryBuilder();

    @Mock
    EntityManager em;

    @Mock
    Query mockQuery;

    @Mock
    private ApplicationParams mockApp;

    @Mock
    private UserAuthorisation userAuthorisation;

    @Mock
    private AuthorisedCaseDefinitionDataService authorisedCaseDefinitionDataService;

    private Map<String, String> params;

    private Set<String> roles = null;
    private UserAuthorisation userAuthorisationWithAccessLevel = new UserAuthorisation("2", UserAuthorisation.AccessLevel.GRANTED,roles);

    @Before
    public void prepare() {
        MockitoAnnotations.initMocks(this);
        subject = new SearchQueryFactoryOperation(
            criterionFactory,
            em,
            mockApp,
            userAuthorisation,
            sortOrderQueryBuilder, authorisedCaseDefinitionDataService);

        subjectWithUserAuthValues = new SearchQueryFactoryOperation(
            criterionFactory,
            em,
            mockApp,
            userAuthorisationWithAccessLevel,
            sortOrderQueryBuilder, authorisedCaseDefinitionDataService);

        params = new HashMap<>();
    }

    @Test
    public void searchFactorySqlGeneratedTestFromField() {
        MetaData metadata = new MetaData(null, null);
        params.put(TEST_FIELD_NAME, TEST_FIELD_VALUE);
        when(em.createNativeQuery(any(String.class), any(Class.class)))
                .thenReturn(mockQuery);
        Query result = subject.build(metadata, params, false);
        verify(em, times(1)).createNativeQuery("SELECT * FROM case_data WHERE TRIM( UPPER ( data #>> '{name}')) = TRIM( UPPER ( ?0)) ORDER BY created_date ASC", CaseDetailsEntity.class);
        verify(result, times(1)).setParameter(0, TEST_FIELD_VALUE);
    }

    @Test
    public void searchFactorySqlGeneratedTestFromMeta() {
        MetaData metadata = new MetaData(TEST_CASE_TYPE_VALUE, null);
        when(em.createNativeQuery(any(String.class), any(Class.class)))
                .thenReturn(mockQuery);
        Query result = subject.build(metadata, params, false);
        verify(em, times(1)).createNativeQuery("SELECT * FROM case_data WHERE case_type_id = ?0 ORDER BY created_date ASC", CaseDetailsEntity.class);
        verify(result, times(1)).setParameter(0, TEST_CASE_TYPE_VALUE);
    }

    @Test
    public void searchFactorySqlGeneratedTestFromMetaAndField() {
        MetaData metadata = new MetaData(TEST_CASE_TYPE_VALUE, null);
        params.put(TEST_FIELD_NAME, TEST_FIELD_VALUE);
        when(em.createNativeQuery(any(String.class), any(Class.class)))
                .thenReturn(mockQuery);
        Query result = subject.build(metadata, params, false);
        verify(em, times(1)).createNativeQuery("SELECT * FROM case_data WHERE TRIM( UPPER ( data #>> '{name}')) = TRIM( UPPER ( ?0)) AND case_type_id = ?1 ORDER BY created_date ASC",
                CaseDetailsEntity.class);
        verify(result, times(1)).setParameter(0, TEST_FIELD_VALUE);
        verify(result, times(1)).setParameter(1, TEST_CASE_TYPE_VALUE);
    }

    @Test
    public void searchFactorySqlGeneratedTestFromMetaAndFieldSortDescending() {
        MetaData metadata = new MetaData(TEST_CASE_TYPE_VALUE, null);
        metadata.setSortDirection(Optional.of("desc"));
        params.put(TEST_FIELD_NAME, TEST_FIELD_VALUE);
        when(em.createNativeQuery(any(String.class), any(Class.class)))
            .thenReturn(mockQuery);
        Query result = subject.build(metadata, params, false);
        verify(em, times(1)).createNativeQuery("SELECT * FROM case_data WHERE TRIM( UPPER ( data #>> '{name}')) = TRIM( UPPER ( ?0)) AND case_type_id = ?1 ORDER BY created_date DESC",
            CaseDetailsEntity.class);
        verify(result, times(1)).setParameter(0, TEST_FIELD_VALUE);
        verify(result, times(1)).setParameter(1, TEST_CASE_TYPE_VALUE);
    }

    @Test
    public void searchFactorySqlGeneratedTestFromMetaAndFieldSortDescendingIgnoresCase() {
        MetaData metadata = new MetaData(TEST_CASE_TYPE_VALUE, null);
        metadata.setSortDirection(Optional.of("DESC"));
        params.put(TEST_FIELD_NAME, TEST_FIELD_VALUE);
        when(em.createNativeQuery(any(String.class), any(Class.class)))
            .thenReturn(mockQuery);
        Query result = subject.build(metadata, params, false);
        verify(em, times(1)).createNativeQuery("SELECT * FROM case_data WHERE TRIM( UPPER ( data #>> '{name}')) = TRIM( UPPER ( ?0)) AND case_type_id = ?1 ORDER BY created_date DESC",
            CaseDetailsEntity.class);
        verify(result, times(1)).setParameter(0, TEST_FIELD_VALUE);
        verify(result, times(1)).setParameter(1, TEST_CASE_TYPE_VALUE);
    }

    @Test
    public void shouldAppendStatesClauseToQuery() {
        when(em.createNativeQuery(any(String.class), any(Class.class))).thenReturn(mockQuery);
        when(authorisedCaseDefinitionDataService.getUserAuthorisedCaseStateIds(TEST_JURISDICTION_VALUE, TEST_CASE_TYPE_VALUE, CAN_READ))
            .thenReturn(asList("STATE1", "STATE2"));
        MetaData metadata = new MetaData(TEST_CASE_TYPE_VALUE, TEST_JURISDICTION_VALUE);

        subject.build(metadata, params, false);

        assertAll(
            () -> verify(authorisedCaseDefinitionDataService).getUserAuthorisedCaseStateIds(TEST_JURISDICTION_VALUE, TEST_CASE_TYPE_VALUE, CAN_READ),
            () -> verify(em).createNativeQuery(
                "SELECT * FROM case_data WHERE case_type_id = ?0 AND jurisdiction = ?1 AND state IN ('STATE1','STATE2') ORDER BY created_date ASC",
                CaseDetailsEntity.class));
    }

    @Test
    public void searchFactorySqlGeneratedTestFromMetaAndFromCaseUsersEntity() {
        MetaData metadata = new MetaData(TEST_CASE_TYPE_VALUE, null);
        when(em.createNativeQuery(any(String.class), any(Class.class)))
            .thenReturn(mockQuery);
        Query result = subjectWithUserAuthValues.build(metadata, params, false);
        verify(em, times(1)).createNativeQuery("SELECT * FROM case_data WHERE case_type_id = ?0 AND id IN (SELECT cu.case_data_id FROM case_users AS cu WHERE user_id = '2') ORDER BY created_date ASC", CaseDetailsEntity.class);
        verify(result, times(1)).setParameter(0, TEST_CASE_TYPE_VALUE);
    }

    @Test
    public void searchFactorySqlGeneratedTestFromFieldWithQueryCountAndFromCaseUsersEntity() {
        MetaData metadata = new MetaData(null, null);
        params.put(TEST_FIELD_NAME, TEST_FIELD_VALUE);
        when(em.createNativeQuery(any(String.class)))
            .thenReturn(mockQuery);
        Query result = subjectWithUserAuthValues.build(metadata, params, true);
        verify(em, times(1)).createNativeQuery("SELECT count(*) FROM case_data WHERE TRIM( UPPER ( data #>> '{name}')) = TRIM( UPPER ( ?0)) AND id IN (SELECT cu.case_data_id FROM case_users AS cu WHERE user_id = '2')");
        verify(result, times(1)).setParameter(0, TEST_FIELD_VALUE);
    }

    @Test
    public void shouldGenerateOrderByWithSortQueryString() {
        MetaData metadata = new MetaData(TEST_CASE_TYPE_VALUE, null);
        SortOrderField sortOrderField = SortOrderField.sortOrderWith()
            .caseFieldId("[LAST_MODIFIED_DATE]")
            .metadata(true)
            .direction("DESC")
            .build();
        metadata.addSortOrderField(sortOrderField);
        when(em.createNativeQuery(any(String.class), any(Class.class))).thenReturn(mockQuery);

        subject.build(metadata, params, false);

        verify(em, times(1)).createNativeQuery("SELECT * FROM case_data WHERE case_type_id = ?0 ORDER BY last_modified DESC, created_date ASC", CaseDetailsEntity.class);
    }

}
