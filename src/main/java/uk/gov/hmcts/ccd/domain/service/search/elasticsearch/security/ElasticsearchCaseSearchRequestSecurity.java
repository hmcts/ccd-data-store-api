package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
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
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.builder.AccessControlGrantTypeESQueryBuilder;

import java.util.Optional;

import static uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest.NATIVE_ES_QUERY;
import static uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest.QUERY;
import static uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest.SUPPLEMENTARY_DATA;

@Component
@Slf4j
public class ElasticsearchCaseSearchRequestSecurity implements CaseSearchRequestSecurity {

    private final List<CaseSearchFilter> caseSearchFilters;
    private final ObjectMapperService objectMapperService;
    private final AccessControlGrantTypeESQueryBuilder grantTypeESQueryBuilder;

    @Autowired
    public ElasticsearchCaseSearchRequestSecurity(List<CaseSearchFilter> caseSearchFilters,
                                                  ObjectMapperService objectMapperService,
                                                  AccessControlGrantTypeESQueryBuilder grantTypeESQueryBuilder) {
        this.caseSearchFilters = caseSearchFilters;
        this.objectMapperService = objectMapperService;
        this.grantTypeESQueryBuilder = grantTypeESQueryBuilder;
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
            .forEach(caseTypeId -> createFilterQueryForCaseType(caseTypeId).ifPresent(boolQueryBuilder::should));

        boolQueryBuilder.minimumShouldMatch(1);

        return createQueryString(boolQueryBuilder);
    }

    private String addFiltersToQuery(CaseSearchRequest caseSearchRequest) {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.must(QueryBuilders.wrapperQuery(caseSearchRequest.getQueryValue()));
        grantTypeESQueryBuilder.createQuery(caseSearchRequest.getCaseTypeId(), boolQueryBuilder);

        caseSearchFilters.forEach(filter ->
            filter.getFilter(caseSearchRequest.getCaseTypeId()).ifPresent(boolQueryBuilder::filter));

        return createQueryString(boolQueryBuilder);
    }


    private Optional<QueryBuilder> createFilterQueryForCaseType(String caseTypeId) {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        grantTypeESQueryBuilder.createQuery(caseTypeId, boolQueryBuilder);

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

        String queryString = searchSourceBuilder.toString();
        log.debug("[[ES Query ]] : " + queryString);
        return queryString;
    }

    private CaseSearchRequest createNewCaseSearchRequest(CaseSearchRequest caseSearchRequest,
                                                         String queryWithFilters) {

        ObjectNode searchRequestJsonNode =
            updateSearchRequestQueryInJson(caseSearchRequest.toJsonString(), queryWithFilters);

        return new CaseSearchRequest(caseSearchRequest.getCaseTypeId(),
            new ElasticsearchRequest(searchRequestJsonNode));
    }

    private CrossCaseTypeSearchRequest createNewCrossCaseTypeSearchRequest(CrossCaseTypeSearchRequest request,
                                                                           String queryWithFilters) {

        ObjectNode searchRequestJsonNode =
            updateSearchRequestQueryInJson(request.getSearchRequestJsonNode().toString(), queryWithFilters);

        ElasticsearchRequest newElasticsearchRequest =
            createNewElasticsearchRequest(request.getElasticSearchRequest(), searchRequestJsonNode);

        // clone CCT search request object :: then replace ES search request
        return new CrossCaseTypeSearchRequest
            .Builder(request)
            .withSearchRequest(newElasticsearchRequest)
            .build();
    }

    private ElasticsearchRequest createNewElasticsearchRequest(ElasticsearchRequest request,
                                                               JsonNode updatedSearchRequestQuery) {

        ObjectNode searchRequestObjectNode;

        if (request.hasRequestedSupplementaryData()) {
            searchRequestObjectNode = (ObjectNode)objectMapperService.createEmptyJsonNode();
            searchRequestObjectNode.set(NATIVE_ES_QUERY, updatedSearchRequestQuery.deepCopy());
            searchRequestObjectNode.set(SUPPLEMENTARY_DATA, request.getRequestedSupplementaryData());
        } else {
            searchRequestObjectNode = updatedSearchRequestQuery.deepCopy();
        }

        return new ElasticsearchRequest(searchRequestObjectNode);
    }

    private ObjectNode updateSearchRequestQueryInJson(String searchRequest, String queryWithFilters) {

        ObjectNode searchRequestJsonNode = objectMapperService.convertStringToObject(searchRequest, ObjectNode.class);
        ObjectNode queryNode = objectMapperService.convertStringToObject(queryWithFilters, ObjectNode.class);

        // update query value with replacement
        searchRequestJsonNode.set(QUERY, queryNode.get(QUERY));

        return searchRequestJsonNode;
    }

}
