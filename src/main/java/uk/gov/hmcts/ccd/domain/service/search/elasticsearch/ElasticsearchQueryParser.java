package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import static java.util.Optional.ofNullable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import uk.gov.hmcts.ccd.domain.service.common.ObjectMapperService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

/**
 * Sample ES query. The parser extracts/sets value for query node.
 * {
 * 	"query": {
 * 		"bool": {
 * 			"filter": {
 * 				"match": { "state": "AwaitingPayment"}
 *                }
 *            }
 *       },
 * 	"sort": {
 * 		"id": { "order":"asc" }
 *       }
 * }
 */
class ElasticsearchQueryParser {

    private static final String QUERY_NAME = "query";

    private final ObjectMapperService objectMapperService;
    private final ObjectNode searchQueryObjectNode;

    ElasticsearchQueryParser(ObjectMapperService objectMapperService, String query) {
        this.objectMapperService = objectMapperService;
        this.searchQueryObjectNode = this.objectMapperService.convertStringToObject(query, ObjectNode.class);
    }

    String extractQueryClause() {
        return ofNullable(searchQueryObjectNode.get(QUERY_NAME))
            .map(JsonNode::toString)
            .orElseThrow(() -> new ValidationException("invalid search query"));
    }

    void setQueryClause(String queryClause) {
        ObjectNode queryClauseNode = objectMapperService.convertStringToObject(queryClause, ObjectNode.class);
        searchQueryObjectNode.set(QUERY_NAME, queryClauseNode.get(QUERY_NAME));
    }

    String getSearchQuery() {
        return searchQueryObjectNode.toString();
    }
}
