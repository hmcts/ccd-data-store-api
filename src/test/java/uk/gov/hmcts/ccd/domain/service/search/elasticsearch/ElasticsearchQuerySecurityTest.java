package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import java.util.Collections;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
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
    private static final String QUERY = "{\"query\":{\"match_all\":{}}}";

    @Mock
    private ElasticsearchQueryParserFactory queryParserFactory;
    @Mock
    private CaseSearchFilterFactory caseSearchFilterFactory;

    private ElasticsearchQuerySecurity querySecurity;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        querySecurity = new ElasticsearchQuerySecurity(queryParserFactory, Collections.singletonList(caseSearchFilterFactory));
    }

    @Test
    @DisplayName("should parse and secure query with filters")
    void shouldSecureQuery() {
        ElasticsearchQueryParser parser = mock(ElasticsearchQueryParser.class);
        when(queryParserFactory.createParser(QUERY)).thenReturn(parser);
        when(parser.extractQueryClause()).thenReturn(QUERY);
        when(parser.getSearchQuery()).thenReturn(QUERY);
        when(caseSearchFilterFactory.create(CASE_TYPE_ID)).thenReturn(Optional.of(mock(QueryBuilder.class)));

        String result = querySecurity.secureQuery(CASE_TYPE_ID, QUERY);

        assertAll(
            () -> assertThat(result, is(QUERY)),
            () -> verify(queryParserFactory).createParser(QUERY),
            () -> verify(caseSearchFilterFactory).create(CASE_TYPE_ID),
            () -> verify(parser).extractQueryClause(),
            () -> verify(parser).setQueryClause(anyString()),
            () -> verify(parser).getSearchQuery());
    }
}
