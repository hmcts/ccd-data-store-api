package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.searchbox.core.Search;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.ApplicationParams;

class ElasticsearchCaseSearchRequestFactoryTest {

    private static final String CASE_TYPE_ID = "CASE_TYPE";
    private static final String INDEX_NAME_FORMAT = "%s_cases";
    private static final String INDEX_TYPE = "case";

    @Mock
    private ApplicationParams applicationParams;
    @Mock
    private CaseSearchQuerySecurity caseSearchQuerySecurity;

    @InjectMocks
    private ElasticsearchCaseSearchRequestFactory factory;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        when(applicationParams.getCasesIndexNameFormat()).thenReturn(INDEX_NAME_FORMAT);
        when(applicationParams.getCasesIndexType()).thenReturn(INDEX_TYPE);
    }

    @Test
    @DisplayName("should secure and create a new search request from query string")
    void shouldCreateSearchRequest() {
        String query = "{}";

        Search searchRequest = factory.create(CASE_TYPE_ID, query);

        assertAll(
            () -> assertThat(searchRequest, is(notNullValue())),
            () -> assertThat(searchRequest.getIndex(), is("case_type_cases")),
            () -> assertThat(searchRequest.getType(), is(INDEX_TYPE)),
            () -> verify(caseSearchQuerySecurity).secureQuery(CASE_TYPE_ID, query),
            () -> verify(applicationParams).getCasesIndexNameFormat(),
            () -> verify(applicationParams).getCasesIndexType());
    }
}
