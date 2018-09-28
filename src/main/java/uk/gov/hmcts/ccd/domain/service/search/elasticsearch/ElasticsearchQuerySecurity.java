package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ElasticsearchQuerySecurity implements CaseSearchQuerySecurity {

    private final ElasticsearchQueryParserFactory queryParserFactory;
    private final List<CaseSearchFilterFactory> caseFilterFactories;

    @Autowired
    public ElasticsearchQuerySecurity(ElasticsearchQueryParserFactory queryParserFactory, List<CaseSearchFilterFactory> caseFilterFactories) {
        this.queryParserFactory = queryParserFactory;
        this.caseFilterFactories = caseFilterFactories;
    }

    @Override
    public String secureQuery(String caseTypeId, String query) {
        ElasticsearchQueryParser parser = queryParserFactory.createParser(query);
        String queryClause = parser.extractQueryClause();
        parser.setQueryClause(addFiltersToQuery(caseTypeId, queryClause));
        String searchQuery = parser.getSearchQuery();

        log.debug("case search query with security filters: {}", searchQuery);

        return searchQuery;
    }

    private String addFiltersToQuery(String caseTypeId, String query) {
        log.debug("wrapping user query: {}", query);
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.must(QueryBuilders.wrapperQuery(query));

        caseFilterFactories.forEach(factory -> factory.create(caseTypeId).ifPresent(boolQueryBuilder::filter));

        return createQueryString(boolQueryBuilder);
    }

    private String createQueryString(QueryBuilder queryBuilder) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(queryBuilder);
        return searchSourceBuilder.toString();
    }

}
