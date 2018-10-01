package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.elasticsearch.index.query.QueryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ElasticsearchQuerySecurityTest {

    private static final String CASE_TYPE_ID = "caseType";

    @Mock
    private CaseSearchFilter caseSearchFilter;

    private ElasticsearchCaseSearchRequestSecurity querySecurity;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        querySecurity = new ElasticsearchCaseSearchRequestSecurity(Collections.singletonList(caseSearchFilter));
    }

    @Test
    @DisplayName("should parse and secure query with filters")
    void shouldSecureQuery() {
        CaseSearchRequest caseSearchRequest = mock(CaseSearchRequest.class);
        when(caseSearchRequest.getCaseTypeId()).thenReturn(CASE_TYPE_ID);
        when(caseSearchFilter.getFilter(CASE_TYPE_ID)).thenReturn(Optional.of(mock(QueryBuilder.class)));

        querySecurity.secureRequest(caseSearchRequest);

        assertAll(
            () -> verify(caseSearchRequest).getQueryValue(),
            () -> verify(caseSearchFilter).getFilter(CASE_TYPE_ID),
            () -> verify(caseSearchRequest).replaceQuery(anyString())
        );
    }
}
