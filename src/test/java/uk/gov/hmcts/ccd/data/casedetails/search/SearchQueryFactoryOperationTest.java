package uk.gov.hmcts.ccd.data.casedetails.search;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Matchers.any;
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
    private final CriterionFactory criterionFactory = new CriterionFactory();

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

    @Before
    public void prepare() {
        MockitoAnnotations.initMocks(this);
        subject = new SearchQueryFactoryOperation(
            criterionFactory,
            em,
            mockApp,
            userAuthorisation,
            authorisedCaseDefinitionDataService);

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
}
