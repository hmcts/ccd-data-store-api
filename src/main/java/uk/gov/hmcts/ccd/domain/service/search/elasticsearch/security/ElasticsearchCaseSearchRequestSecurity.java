package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.security;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest;
import uk.gov.hmcts.ccd.domain.service.common.ObjectMapperService;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CaseSearchRequest;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CrossCaseTypeSearchRequest;

import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest.QUERY;

@Component
public class ElasticsearchCaseSearchRequestSecurity implements CaseSearchRequestSecurity {

    private final List<CaseSearchFilter> caseSearchFilters;
    private final ObjectMapperService objectMapperService;

    @Autowired
    public ElasticsearchCaseSearchRequestSecurity(List<CaseSearchFilter> caseSearchFilters,
                                                  ObjectMapperService objectMapperService) {
        this.caseSearchFilters = caseSearchFilters;
        this.objectMapperService = objectMapperService;
    }

    @Override
    public CaseSearchRequest createSecuredSearchRequest(CaseSearchRequest caseSearchRequest) {
        String queryClauseWithSecurityFilters = addFiltersToQuery(caseSearchRequest);
        return createNewCaseSearchRequest(caseSearchRequest, queryClauseWithSecurityFilters);
    }

    @Override
    public CrossCaseTypeSearchRequest createSecuredSearchRequest(CrossCaseTypeSearchRequest request) {
        String queryClauseWithSecurityFilters = addFiltersToQuery(request);
        return createNewCrossCaseTypeSearchRequest(request, queryClauseWithSecurityFilters);
    }

    private String addFiltersToQuery(CrossCaseTypeSearchRequest crossCaseTypeSearchRequest) {
        String query = crossCaseTypeSearchRequest.getElasticSearchRequest().getQuery().toString();

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.must(QueryBuilders.wrapperQuery(query));

        // At least one of these clauses must match. The equivalent of OR
        crossCaseTypeSearchRequest.getCaseTypeIds()
            .forEach(caseTypeId -> createCaseType(caseTypeId).ifPresent(boolQueryBuilder::should));

        boolQueryBuilder.minimumShouldMatch(1);

        return createQueryString(boolQueryBuilder);
    }

    private String addFiltersToQuery(CaseSearchRequest caseSearchRequest) {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.must(QueryBuilders.wrapperQuery(caseSearchRequest.getQueryValue()));

        caseSearchFilters.forEach(filter ->
            filter.getFilter(caseSearchRequest.getCaseTypeId()).ifPresent(boolQueryBuilder::filter));

        return createQueryString(boolQueryBuilder);
    }


    // return must object
    public Optional<QueryBuilder> createCaseType(String caseTypeId) {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        caseSearchFilters.forEach(filter ->
            filter.getFilter(caseTypeId).ifPresent(boolQueryBuilder::must));

        if (boolQueryBuilder.must().isEmpty()) {
            return Optional.empty();
        }

        boolQueryBuilder.must(QueryBuilders.termQuery("case_type_id", caseTypeId.toLowerCase()));

        return Optional.of(boolQueryBuilder);
    }

    private String createQueryString(QueryBuilder queryBuilder) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(queryBuilder);

        return searchSourceBuilder.toString();
    }

    private CaseSearchRequest createNewCaseSearchRequest(CaseSearchRequest caseSearchRequest,
                                                         String queryWithFilters) {
        ObjectNode searchRequestJsonNode =
            objectMapperService.convertStringToObject(caseSearchRequest.toJsonString(),
                ObjectNode.class);
        ObjectNode queryNode = objectMapperService.convertStringToObject(queryWithFilters, ObjectNode.class);
        searchRequestJsonNode.set(QUERY, queryNode.get(QUERY));

        return new CaseSearchRequest(caseSearchRequest.getCaseTypeId(),
            new ElasticsearchRequest(searchRequestJsonNode));
    }

    private CrossCaseTypeSearchRequest createNewCrossCaseTypeSearchRequest(CrossCaseTypeSearchRequest request,
                                                                           String queryWithFilters) {
        ObjectNode queryNode = objectMapperService.convertStringToObject(queryWithFilters, ObjectNode.class);

        return new CrossCaseTypeSearchRequest.Builder()
            .withCaseTypes(request.getCaseTypeIds())
            .withSearchRequest(new ElasticsearchRequest(queryNode))
            .withMultiCaseTypeSearch(request.isMultiCaseTypeSearch())
            .withSourceFilterAliasFields(request.getAliasFields())
            .build();
    }
}
