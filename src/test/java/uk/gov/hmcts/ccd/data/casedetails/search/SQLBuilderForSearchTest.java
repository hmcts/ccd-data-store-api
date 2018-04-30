package uk.gov.hmcts.ccd.data.casedetails.search;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class SQLBuilderForSearchTest {

    private static final String TEST_CASE_TYPE_VALUE = "TEST_CASE_TYPE";
    private static final String TEST_FIELD_NAME = "case.name";
    private static final String TEST_FIELD_VALUE = "Tim";
    @Captor
    ArgumentCaptor<EntityManager> emCaptor;
    SearchQueryFactoryOperation subject;
    CriterionFactory criterionFactory = new CriterionFactory();

    @Mock
    EntityManager em;
    @Mock
    Query mockQuery;

    @Mock
    private ApplicationParams mockApp;

    @Mock
    private UserAuthorisation userAuthorisation;

    private Map<String, String> params;

    public SQLBuilderForSearchTest() {

    }

    @Before
    public void prepare() {
        MockitoAnnotations.initMocks(this);
        subject = new SearchQueryFactoryOperation(
                criterionFactory,
                em,
                mockApp,
                userAuthorisation);

        params = new HashMap<>();

    }

    @Test
    public void searchFactorysqlGeneratedTestFromField() {
        MetaData metadata = new MetaData(null, null);
        params.put(TEST_FIELD_NAME, TEST_FIELD_VALUE);
        when(em.createNativeQuery(any(String.class), any(Class.class)))
                .thenReturn(mockQuery);
        Query result = subject.build(metadata, params, false);
        verify(em, times(1)).createNativeQuery("SELECT * FROM case_data WHERE TRIM( UPPER ( data #>> '{name}')) = TRIM( UPPER ( ?0)) ORDER BY created_date ASC", CaseDetailsEntity.class);
        verify(result, times(1)).setParameter(0, TEST_FIELD_VALUE);
    }

    @Test
    public void searchFactorysqlGeneratedTestFromMeta() {
        MetaData metadata = new MetaData(TEST_CASE_TYPE_VALUE, null);
        when(em.createNativeQuery(any(String.class), any(Class.class)))
                .thenReturn(mockQuery);
        Query result = subject.build(metadata, params, false);
        verify(em, times(1)).createNativeQuery("SELECT * FROM case_data WHERE case_type_id = ?0 ORDER BY created_date ASC", CaseDetailsEntity.class);
        verify(result, times(1)).setParameter(0, TEST_CASE_TYPE_VALUE);
    }

    @Test
    public void searchFactorysqlGeneratedTestFromMetaandField() {
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

}
