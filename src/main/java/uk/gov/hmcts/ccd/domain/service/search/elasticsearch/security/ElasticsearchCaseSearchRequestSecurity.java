package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest;
import uk.gov.hmcts.ccd.domain.service.common.ObjectMapperService;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CaseSearchRequest;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CrossCaseTypeSearchRequest;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.builder.AccessControlGrantTypeESQueryBuilder;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
        Query securedQuery = buildSecuredQuery(caseSearchRequest);
        return createNewCaseSearchRequest(caseSearchRequest, securedQuery);
    }

    @Override
    public CrossCaseTypeSearchRequest createSecuredSearchRequest(CrossCaseTypeSearchRequest request) {
        Query securedQuery = buildSecuredQuery(request);
        return createNewCrossCaseTypeSearchRequest(request, securedQuery);
    }

    private Query buildSecuredQuery(CaseSearchRequest caseSearchRequest) {
        Query baseQuery = Query.of(q -> q
            .wrapper(w -> w
                .query(Base64.getEncoder().encodeToString(caseSearchRequest.getQueryValue().getBytes(
                    StandardCharsets.UTF_8)))
            )
        );
        List<Query> filters = caseSearchFilters.stream()
            .map(filter -> filter.getFilter(caseSearchRequest.getCaseTypeId()))
            .flatMap(Optional::stream)
            .collect(Collectors.toList());

        grantTypeESQueryBuilder.createQuery(caseSearchRequest.getCaseTypeId(), filters);

        return Query.of(q -> q.bool(b -> b
            .must(baseQuery)
            .filter(filters)
        ));
    }

    private Query buildSecuredQuery(CrossCaseTypeSearchRequest request) {
        String rawQueryJson = request.getElasticSearchRequest().getQuery().toString();
        String base64EncodedQuery = Base64.getEncoder().encodeToString(rawQueryJson.getBytes(StandardCharsets.UTF_8));

        Query baseQuery = Query.of(q -> q
            .wrapper(w -> w.query(base64EncodedQuery))
        );

        List<Query> shouldQueries = request.getCaseTypeIds().stream()
            .map(this::createFilterQueryForCaseType)
            .flatMap(Optional::stream)
            .collect(Collectors.toList());

        return Query.of(q -> q.bool(b -> b
            .must(baseQuery)
            .should(shouldQueries)
            .minimumShouldMatch("1")
        ));
    }

    private Optional<Query> createFilterQueryForCaseType(String caseTypeId) {
        List<Query> mustQueries = caseSearchFilters.stream()
            .map(filter -> filter.getFilter(caseTypeId))
            .flatMap(Optional::stream)
            .collect(Collectors.toList());

        grantTypeESQueryBuilder.createQuery(caseTypeId, mustQueries);

        if (mustQueries.isEmpty()) {
            return Optional.empty();
        }

        mustQueries.add(Query.of(q -> q.term(t -> t.field("case_type_id")
            .value(caseTypeId.toLowerCase()))));

        return Optional.of(Query.of(q -> q.bool(b -> b.must(mustQueries))));
    }

    private CaseSearchRequest createNewCaseSearchRequest(CaseSearchRequest caseSearchRequest, Query securedQuery) {
        ObjectNode updatedJson = injectQueryIntoSearchRequest(
            caseSearchRequest.toJsonString(),
            objectMapperService.convertObjectToString(securedQuery)
        );

        return new CaseSearchRequest(
            caseSearchRequest.getCaseTypeId(),
            new ElasticsearchRequest(updatedJson)
        );
    }

    private CrossCaseTypeSearchRequest createNewCrossCaseTypeSearchRequest(CrossCaseTypeSearchRequest request,
                                                                           Query securedQuery) {
        ObjectNode updatedJson = injectQueryIntoSearchRequest(
            request.getSearchRequestJsonNode().toString(),
            objectMapperService.convertObjectToString(securedQuery)
        );

        ElasticsearchRequest newRequest = createNewElasticsearchRequest(request.getElasticSearchRequest(), updatedJson);

        return new CrossCaseTypeSearchRequest.Builder(request)
            .withSearchRequest(newRequest)
            .build();
    }

    private ElasticsearchRequest createNewElasticsearchRequest(ElasticsearchRequest request,
                                                               JsonNode updatedSearchRequestQuery) {
        ObjectNode node;
        if (request.hasRequestedSupplementaryData()) {
            node = (ObjectNode) objectMapperService.createEmptyJsonNode();
            node.set(NATIVE_ES_QUERY, updatedSearchRequestQuery.deepCopy());
            node.set(SUPPLEMENTARY_DATA, request.getRequestedSupplementaryData());
        } else {
            node = updatedSearchRequestQuery.deepCopy();
        }
        return new ElasticsearchRequest(node);
    }

    private ObjectNode injectQueryIntoSearchRequest(String searchRequestJson, String queryJson) {
        ObjectNode originalRequest = objectMapperService.convertStringToObject(searchRequestJson, ObjectNode.class);
        ObjectNode newQueryNode = objectMapperService.convertStringToObject(queryJson, ObjectNode.class);
        originalRequest.set(QUERY, newQueryNode.get(QUERY));
        return originalRequest;
    }
}
